package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.Round
import com.nicoarbio.mercier.routing.DistanceMatrixProvider

/**
 * Runs the full pipeline: group → route → cost → allocate.
 *
 * Two distance sources on the same street network:
 * - [proximity] (one-way streets ignored) decides which orders are close enough to share a round;
 * - [routing] (one-way streets respected) measures what is actually driven, and therefore paid.
 */
class RoundPlanner(
	private val grouper: OrderGrouper,
	private val router: NearestNeighborRouter,
	private val costModel: CostModel,
	private val allocator: CostAllocator,
	private val proximity: DistanceMatrixProvider,
	private val routing: DistanceMatrixProvider,
) {

	fun plan(orders: List<Order>): List<Round> {
		if (orders.isEmpty()) return emptyList()
		val points = (orders.map { it.merchant.location } + orders.map { it.destination }).distinct()
		val proximityMatrix = proximity.matrix(points)
		val routingMatrix = routing.matrix(points)

		return grouper.group(orders, proximityMatrix).map { group ->
			val stops = router.route(group, routingMatrix)
			val legs = stops.zipWithNext()
			val distance = legs.sumOf { (a, b) -> routingMatrix.distance(a.location, b.location) }
			val duration = legs.sumOf { (a, b) -> routingMatrix.duration(a.location, b.location) }
			val cost = costModel.cost(distance)
			Round(group, stops, distance, duration, cost, allocator.allocate(cost, group))
		}
	}
}
