package com.nicoarbio.mercier.api

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.persistence.MerchantEntity
import com.nicoarbio.mercier.persistence.MerchantRepository
import com.nicoarbio.mercier.persistence.OrderEntity
import com.nicoarbio.mercier.persistence.OrderRepository
import com.nicoarbio.mercier.persistence.RoundEntity
import com.nicoarbio.mercier.persistence.RoundRepository
import com.nicoarbio.mercier.persistence.RoundStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** Read side of the API: assembles responses from entities inside a read-only transaction. */
@Service
@Transactional(readOnly = true)
class Views(
	private val merchants: MerchantRepository,
	private val orders: OrderRepository,
	private val rounds: RoundRepository,
) {

	fun merchants(): List<MerchantResponse> =
		merchants.findAll().sortedBy { it.name }.map { MerchantResponse(it.id, it.name, Coordinates(it.lon, it.lat)) }

	fun ordersOf(merchantId: UUID): List<OrderResponse> {
		if (!merchants.existsById(merchantId)) throw NoSuchElementException("Merchant $merchantId not found")
		return orders.findAllByMerchantIdOrderByWindowStartDesc(merchantId).map { order(it) }
	}

	fun order(id: UUID): OrderResponse =
		order(orders.findById(id).orElseThrow { NoSuchElementException("Order $id not found") })

	fun rounds(status: RoundStatus): List<RoundSummaryResponse> =
		rounds.findAllByStatusOrderByDepartureAt(status).map { round ->
			RoundSummaryResponse(
				id = round.id,
				status = round.status,
				departureAt = round.departureAt,
				estimatedReturn = round.arrivalAt(round.stops.last()),
				deliveries = round.stops.count { it.orderId != null },
				distanceMeters = round.distanceMeters,
				cost = round.cost,
			)
		}

	fun round(id: UUID): RoundResponse {
		val round = rounds.findById(id).orElseThrow { NoSuchElementException("Round $id not found") }
		val names = merchants.findAllById(round.stops.map { it.merchantId } + round.shares.keys).associate { it.id to it.name }
		val addresses = orders.findAllById(round.stops.mapNotNull { it.orderId }).associate { it.id to it.address }
		return RoundResponse(
			id = round.id,
			status = round.status,
			departureAt = round.departureAt,
			estimatedReturn = round.arrivalAt(round.stops.last()),
			distanceMeters = round.distanceMeters,
			drivingSeconds = round.drivingSeconds,
			cost = round.cost,
			pricePerKm = round.pricePerKm,
			stops = round.stops.mapIndexed { i, stop ->
				StopResponse(
					seq = i,
					kind = stop.kind,
					merchantId = stop.merchantId,
					merchantName = names.getValue(stop.merchantId),
					orderId = stop.orderId,
					address = stop.orderId?.let { addresses[it] },
					location = Coordinates(stop.lon, stop.lat),
					estimatedArrival = round.arrivalAt(stop),
				)
			},
			shares = round.shares.map { (merchantId, amount) -> ShareResponse(merchantId, names.getValue(merchantId), amount) }
				.sortedBy { it.merchantName },
			googleMapsUrl = GoogleMapsLink.directions(round.stops.map { GeoPoint(it.lon, it.lat) }),
		)
	}

	private fun order(order: OrderEntity): OrderResponse = OrderResponse(
		id = order.id,
		merchantId = order.merchant.id,
		address = order.address,
		destination = Coordinates(order.destLon, order.destLat),
		windowStart = order.windowStart,
		windowEnd = order.windowEnd,
		status = order.status,
		round = order.round?.let { round -> roundInfo(round, order.merchant, order.id) },
	)

	private fun roundInfo(round: RoundEntity, merchant: MerchantEntity, orderId: UUID) = OrderRoundInfo(
		roundId = round.id,
		roundStatus = round.status,
		merchantShare = round.shares.getValue(merchant.id),
		estimatedDelivery = round.arrivalAt(round.stops.single { it.orderId == orderId }),
	)
}
