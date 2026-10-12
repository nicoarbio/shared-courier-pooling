package com.nicoarbio.mercier.domain

import java.time.Duration
import java.time.LocalDateTime

data class Merchant(val id: String, val name: String, val location: GeoPoint)

/** Delivery time window requested for an order. */
data class TimeWindow(val start: LocalDateTime, val end: LocalDateTime) {
	init {
		require(!end.isBefore(start)) { "Window end ($end) is before start ($start)" }
	}

	/** Two windows are compatible when their start times differ by at most [tolerance]. */
	fun isCompatibleWith(other: TimeWindow, tolerance: Duration): Boolean =
		Duration.between(start, other.start).abs() <= tolerance
}

data class Order(
	val id: String,
	val merchant: Merchant,
	val destination: GeoPoint,
	val window: TimeWindow,
)
