package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.Merchant
import com.nicoarbio.mercier.domain.Order
import java.math.BigDecimal
import java.math.RoundingMode

/** Cost of a trip: a flat price per kilometer driven. */
class CostModel(val pricePerKm: BigDecimal) {

	fun cost(distanceMeters: Double): BigDecimal =
		pricePerKm.multiply(BigDecimal.valueOf(distanceMeters)).divide(METERS_PER_KM, 2, RoundingMode.HALF_EVEN)

	private companion object {
		val METERS_PER_KM = BigDecimal(1000)
	}
}

/**
 * Splits a round's cost among its merchants proportionally to their number of delivery stops.
 * Shares are rounded to cents; the last merchant absorbs the rounding difference so shares add up exactly.
 */
class CostAllocator {

	fun allocate(cost: BigDecimal, orders: List<Order>): Map<Merchant, BigDecimal> {
		require(orders.isNotEmpty()) { "Cannot allocate cost of an empty round" }
		val stopsByMerchant = orders.groupingBy { it.merchant }.eachCount()
		val totalStops = BigDecimal(orders.size)

		val shares = LinkedHashMap<Merchant, BigDecimal>()
		for ((merchant, stops) in stopsByMerchant.entries.toList().dropLast(1)) {
			shares[merchant] = cost.multiply(BigDecimal(stops)).divide(totalStops, 2, RoundingMode.HALF_EVEN)
		}
		val last = stopsByMerchant.keys.last()
		shares[last] = cost - shares.values.fold(BigDecimal.ZERO, BigDecimal::add)
		return shares
	}
}
