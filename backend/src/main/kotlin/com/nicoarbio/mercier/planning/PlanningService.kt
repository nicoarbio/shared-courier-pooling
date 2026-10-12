package com.nicoarbio.mercier.planning

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Round
import com.nicoarbio.mercier.domain.TimeWindow
import com.nicoarbio.mercier.engine.RoundPlanner
import com.nicoarbio.mercier.persistence.MerchantRepository
import com.nicoarbio.mercier.persistence.OrderEntity
import com.nicoarbio.mercier.persistence.OrderRepository
import com.nicoarbio.mercier.persistence.OrderStatus
import com.nicoarbio.mercier.persistence.RoundEntity
import com.nicoarbio.mercier.persistence.RoundRepository
import com.nicoarbio.mercier.persistence.RoundStopEmbeddable
import com.nicoarbio.mercier.persistence.toDomain
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Keeps the planned rounds in sync with the orders that have not left yet.
 *
 * Every new order triggers a full re-plan of all PENDING and PLANNED orders: their current rounds are
 * discarded and the engine groups and routes them again. Whether a new order fits an existing round
 * is decided there, by the engine's grouping rules. Runs in a single transaction, so readers never
 * see a half-replaced plan.
 */
@Service
class PlanningService(
	private val planner: RoundPlanner,
	private val merchants: MerchantRepository,
	private val orders: OrderRepository,
	private val rounds: RoundRepository,
	private val clock: Clock,
) {

	@Transactional
	fun submitOrder(merchantId: UUID, destination: GeoPoint, address: String?, window: TimeWindow): OrderEntity {
		val merchant = merchants.findById(merchantId).orElseThrow { NoSuchElementException("Merchant $merchantId not found") }
		val order = orders.save(
			OrderEntity(
				id = UUID.randomUUID(),
				merchant = merchant,
				destLon = destination.lon,
				destLat = destination.lat,
				address = address,
				windowStart = window.start,
				windowEnd = window.end,
				status = OrderStatus.PENDING,
				round = null,
				createdAt = Instant.now(clock),
			),
		)
		replan()
		return order
	}

	@Transactional
	fun replan(): List<RoundEntity> {
		val open = orders.findAllByStatusIn(listOf(OrderStatus.PENDING, OrderStatus.PLANNED))

		// Detach orders from their current rounds before deleting them (Hibernate flushes updates before deletes).
		val previousRounds = open.mapNotNull { it.round }.toSet()
		open.forEach { it.round = null; it.status = OrderStatus.PENDING }
		rounds.deleteAll(previousRounds)

		val byDomainId = open.associateBy { it.id.toString() }
		val plannedAt = Instant.now(clock)
		return planner.plan(open.map { it.toDomain() }).map { round ->
			val entity = rounds.save(round.toEntity(plannedAt))
			round.orders.forEach { order ->
				byDomainId.getValue(order.id).apply {
					this.round = entity
					status = OrderStatus.PLANNED
				}
			}
			entity
		}
	}

	private fun Round.toEntity(plannedAt: Instant) = RoundEntity(
		id = UUID.randomUUID(),
		plannedAt = plannedAt,
		distanceMeters = distanceMeters,
		durationSeconds = durationSeconds,
		cost = cost,
		pricePerKm = planner.pricePerKm,
		radiusMeters = planner.radiusMeters,
		stops = stops.mapTo(mutableListOf()) { stop ->
			RoundStopEmbeddable(
				kind = stop.kind,
				merchantId = UUID.fromString(stop.merchant.id),
				orderId = stop.order?.let { UUID.fromString(it.id) },
				lon = stop.location.lon,
				lat = stop.location.lat,
			)
		},
		shares = costByMerchant.entries.associateTo(mutableMapOf()) { (merchant, amount) -> UUID.fromString(merchant.id) to amount },
	)
}
