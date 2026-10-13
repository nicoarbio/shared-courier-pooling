package com.nicoarbio.mercier.domain

import java.math.BigDecimal

enum class StopKind { PICKUP, DELIVERY, RETURN }

/** One stop of a route. [order] is set only for deliveries. */
data class Stop(
	val kind: StopKind,
	val location: GeoPoint,
	val merchant: Merchant,
	val order: Order? = null,
)

/**
 * A stop placed on the round's timeline.
 *
 * [legDistanceMeters] and [legDurationSeconds] are the drive from the previous stop (0 for the first one), as measured
 * by the routing engine. [arrivalOffsetSeconds] is the time from departure until arriving at this stop: driving plus
 * the service time spent at every previous pickup and delivery.
 */
data class ScheduledStop(
	val stop: Stop,
	val legDistanceMeters: Double,
	val legDurationSeconds: Double,
	val arrivalOffsetSeconds: Double,
) {
	val kind get() = stop.kind
	val location get() = stop.location
	val merchant get() = stop.merchant
	val order get() = stop.order
}

/**
 * A group of orders served by a single courier trip.
 *
 * [stops] is the ordered route: pickups, deliveries and the final return to the pickup zone.
 * [distanceMeters] and [drivingSeconds] are measured on the real (directed) road network;
 * [totalSeconds] adds the service time at each stop.
 */
data class Round(
	val orders: List<Order>,
	val stops: List<ScheduledStop>,
	val distanceMeters: Double,
	val drivingSeconds: Double,
	val cost: BigDecimal,
	val costByMerchant: Map<Merchant, BigDecimal>,
) {
	val totalSeconds get() = stops.last().arrivalOffsetSeconds
}
