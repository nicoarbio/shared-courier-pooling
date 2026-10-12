package com.nicoarbio.mercier.domain

import java.math.BigDecimal

enum class StopKind { PICKUP, DELIVERY, RETURN }

/** One stop of a round. [order] is set only for deliveries. */
data class Stop(
	val kind: StopKind,
	val location: GeoPoint,
	val merchant: Merchant,
	val order: Order? = null,
)

/**
 * A group of orders served by a single courier trip.
 *
 * [stops] is the ordered route: pickups, deliveries and the final return to the pickup zone.
 * [distanceMeters] and [durationSeconds] are measured on the real (directed) road network.
 */
data class Round(
	val orders: List<Order>,
	val stops: List<Stop>,
	val distanceMeters: Double,
	val durationSeconds: Double,
	val cost: BigDecimal,
	val costByMerchant: Map<Merchant, BigDecimal>,
)
