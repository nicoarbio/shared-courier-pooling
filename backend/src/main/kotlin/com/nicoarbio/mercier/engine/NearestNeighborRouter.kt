package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Merchant
import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.Stop
import com.nicoarbio.mercier.domain.StopKind
import com.nicoarbio.mercier.routing.DistanceMatrix

/**
 * Orders the stops of a round with the nearest-neighbor heuristic, in two phases:
 * 1. Pickup: from the starting merchant, visit the closest pending merchant each time.
 * 2. Delivery: from the last merchant, visit the closest pending destination each time.
 * The route ends by returning to the starting merchant (the courier goes back to the pickup zone).
 *
 * Multi-start: every merchant of the round is tried as the starting point and the shortest
 * resulting route is kept. With one-way streets the starting point matters a lot (e.g. starting
 * at the wrong end of a one-way block forces a loop around it just to reach the next shop).
 *
 * Distances come from the routing matrix (directed: respects one-way streets).
 * Ties are broken by input order, so results are deterministic.
 */
class NearestNeighborRouter {

	fun route(orders: List<Order>, routing: DistanceMatrix): List<Stop> {
		require(orders.isNotEmpty()) { "Cannot route an empty round" }
		val merchants = orders.map { it.merchant }.distinct()
		return merchants
			.map { start -> routeFrom(start, merchants - start, orders, routing) }
			.minBy { stops -> stops.zipWithNext().sumOf { (a, b) -> routing.distance(a.location, b.location) } }
	}

	private fun routeFrom(start: Merchant, otherMerchants: List<Merchant>, orders: List<Order>, routing: DistanceMatrix): List<Stop> {
		val stops = mutableListOf(Stop(StopKind.PICKUP, start.location, start))

		val pickups = nearestNeighborOrder(start.location, otherMerchants, routing) { it.location }
		pickups.mapTo(stops) { Stop(StopKind.PICKUP, it.location, it) }

		val deliveries = nearestNeighborOrder(stops.last().location, orders, routing) { it.destination }
		deliveries.mapTo(stops) { Stop(StopKind.DELIVERY, it.destination, it.merchant, it) }

		stops += Stop(StopKind.RETURN, start.location, start)
		return stops
	}

	private fun <T> nearestNeighborOrder(
		from: GeoPoint,
		pending: List<T>,
		routing: DistanceMatrix,
		locationOf: (T) -> GeoPoint,
	): List<T> {
		val left = pending.toMutableList()
		val visited = mutableListOf<T>()
		var current = from
		while (left.isNotEmpty()) {
			val next = left.minBy { routing.distance(current, locationOf(it)) }
			left.remove(next)
			visited += next
			current = locationOf(next)
		}
		return visited
	}
}
