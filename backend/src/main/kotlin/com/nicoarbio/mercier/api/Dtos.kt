package com.nicoarbio.mercier.api

import com.nicoarbio.mercier.domain.StopKind
import com.nicoarbio.mercier.persistence.OrderStatus
import com.nicoarbio.mercier.persistence.RoundStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/* API contract. Kept apart from the JPA entities so the database model can change without breaking clients. */

data class Coordinates(
	@field:NotNull @field:DecimalMin("-180") @field:DecimalMax("180") val lon: Double?,
	@field:NotNull @field:DecimalMin("-90") @field:DecimalMax("90") val lat: Double?,
)

data class CreateOrderRequest(
	@field:NotNull val merchantId: UUID?,
	@field:NotNull @field:Valid val destination: Coordinates?,
	/** Free text shown to the courier; coordinates are what the engine uses. */
	val address: String?,
	@field:NotNull val windowStart: LocalDateTime?,
	@field:NotNull val windowEnd: LocalDateTime?,
) {
	@get:AssertTrue(message = "windowEnd must not be before windowStart")
	val isWindowValid: Boolean
		get() = windowStart == null || windowEnd == null || !windowEnd.isBefore(windowStart)
}

data class MerchantResponse(val id: UUID, val name: String, val location: Coordinates)

/** An order as its merchant sees it: where it is in the plan, what the merchant pays and when it arrives. */
data class OrderResponse(
	val id: UUID,
	val merchantId: UUID,
	val address: String?,
	val destination: Coordinates,
	val windowStart: LocalDateTime,
	val windowEnd: LocalDateTime,
	val status: OrderStatus,
	val round: OrderRoundInfo?,
)

data class OrderRoundInfo(
	val roundId: UUID,
	val roundStatus: RoundStatus,
	/** What this order's merchant pays for the whole round (all its stops in it). */
	val merchantShare: BigDecimal,
	val estimatedDelivery: LocalDateTime,
)

data class RoundSummaryResponse(
	val id: UUID,
	val status: RoundStatus,
	val departureAt: LocalDateTime,
	val estimatedReturn: LocalDateTime,
	val deliveries: Int,
	val distanceMeters: Double,
	val cost: BigDecimal,
)

data class RoundResponse(
	val id: UUID,
	val status: RoundStatus,
	val departureAt: LocalDateTime,
	val estimatedReturn: LocalDateTime,
	val distanceMeters: Double,
	val drivingSeconds: Double,
	val cost: BigDecimal,
	val pricePerKm: BigDecimal,
	val stops: List<StopResponse>,
	val shares: List<ShareResponse>,
	/** Turn-by-turn navigation is delegated to Google Maps; null if the route exceeds its waypoint limit. */
	val googleMapsUrl: String?,
)

data class StopResponse(
	val seq: Int,
	val kind: StopKind,
	val merchantId: UUID,
	val merchantName: String,
	val orderId: UUID?,
	val address: String?,
	val location: Coordinates,
	val estimatedArrival: LocalDateTime,
)

data class ShareResponse(val merchantId: UUID, val merchantName: String, val amount: BigDecimal)
