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
import com.nicoarbio.mercier.persistence.RoundStatus
import com.nicoarbio.mercier.persistence.RoundStopEmbeddable
import com.nicoarbio.mercier.persistence.toDomain
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Keeps the planned rounds in sync with the orders that have not left yet.
 *
 * Every new order triggers a full re-plan of all PENDING and PLANNED orders (DISPATCHED ones already left): their current rounds are
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
		log.info("Order {} submitted, window {} - {}", order.id, window.start, window.end)
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
		val now = LocalDateTime.now(clock)
		val start = System.nanoTime()
		val planned = planner.plan(open.map { it.toDomain() })
		log.info(
			"Re-planned {} open orders into {} rounds ({} m, \${}) in {} ms",
			open.size, planned.size, planned.sumOf { it.distanceMeters }.toLong(),
			planned.fold(BigDecimal.ZERO) { acc, r -> acc + r.cost }, (System.nanoTime() - start) / 1_000_000,
		)
		return planned.map { round ->
			// Estimated departure: the earliest delivery window of the round, or now if it already started.
			val departure = maxOf(round.orders.minOf { it.window.start }, now)
			val entity = rounds.save(round.toEntity(plannedAt, departure))
			round.orders.forEach { order ->
				byDomainId.getValue(order.id).apply {
					this.round = entity
					status = OrderStatus.PLANNED
				}
			}
			entity
		}
	}

	/**
	 * The courier leaves with the round: it becomes DISPATCHED with its orders, is no longer re-planned,
	 * and its departure becomes the actual one, so every estimated arrival shifts accordingly.
	 */
	@Transactional
	fun dispatch(roundId: UUID): RoundEntity {
		val round = rounds.findById(roundId).orElseThrow { NoSuchElementException("Round $roundId not found") }
		check(round.status == RoundStatus.PLANNED) { "Round $roundId is already ${round.status}" }
		round.status = RoundStatus.DISPATCHED
		round.departureAt = LocalDateTime.now(clock)
		orders.findAllByRound(round).forEach { it.status = OrderStatus.DISPATCHED }
		log.info("Round {} dispatched at {}", round.id, round.departureAt)
		return round
	}

	private fun Round.toEntity(plannedAt: Instant, departure: LocalDateTime) = RoundEntity(
		id = UUID.randomUUID(),
		plannedAt = plannedAt,
		distanceMeters = distanceMeters,
		drivingSeconds = drivingSeconds,
		cost = cost,
		pricePerKm = planner.pricePerKm,
		radiusMeters = planner.radiusMeters,
		serviceTimeSeconds = planner.serviceTime.toSeconds().toDouble(),
		status = RoundStatus.PLANNED,
		departureAt = departure,
		stops = stops.mapTo(mutableListOf()) { stop ->
			RoundStopEmbeddable(
				kind = stop.kind,
				merchantId = UUID.fromString(stop.merchant.id),
				orderId = stop.order?.let { UUID.fromString(it.id) },
				lon = stop.location.lon,
				lat = stop.location.lat,
				legDistanceMeters = stop.legDistanceMeters,
				legDurationSeconds = stop.legDurationSeconds,
				arrivalOffsetSeconds = stop.arrivalOffsetSeconds,
			)
		},
		shares = costByMerchant.entries.associateTo(mutableMapOf()) { (merchant, amount) -> UUID.fromString(merchant.id) to amount },
	)
}

private val log = LoggerFactory.getLogger(PlanningService::class.java)
