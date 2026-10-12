package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.routing.DistanceMatrix
import java.time.Duration

/**
 * Groups orders into rounds (complete-linkage, greedy).
 *
 * Orders are visited by window start; each one joins the first round where, once added:
 * - every destination is within [radiusMeters] of every merchant in the round, and
 * - every pair of windows starts within [windowTolerance] of each other.
 * Otherwise it opens a new round. A single order always forms a valid round.
 *
 * Distances come from the proximity matrix (road network, one-way streets ignored):
 * grouping measures geographic closeness; the actual driving is measured later by the router.
 */
class OrderGrouper(
	private val radiusMeters: Double,
	private val windowTolerance: Duration,
) {

	fun group(orders: List<Order>, proximity: DistanceMatrix): List<List<Order>> {
		val rounds = mutableListOf<MutableList<Order>>()
		for (order in orders.sortedBy { it.window.start }) {
			val round = rounds.firstOrNull { isValidRound(it + order, proximity) }
			if (round != null) round += order else rounds += mutableListOf(order)
		}
		return rounds
	}

	private fun isValidRound(orders: List<Order>, proximity: DistanceMatrix): Boolean {
		val merchants = orders.map { it.merchant.location }.distinct()
		val withinRadius = orders.all { order ->
			merchants.all { merchant -> proximity.distance(merchant, order.destination) <= radiusMeters }
		}
		val compatibleWindows = orders.all { a ->
			orders.all { b -> a.window.isCompatibleWith(b.window, windowTolerance) }
		}
		return withinRadius && compatibleWindows
	}
}
