package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.Round
import com.nicoarbio.mercier.domain.ScheduledStop
import com.nicoarbio.mercier.domain.Stop
import com.nicoarbio.mercier.domain.StopKind
import com.nicoarbio.mercier.routing.DistanceMatrix
import com.nicoarbio.mercier.routing.DistanceMatrixProvider
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Runs the full pipeline: group → route → schedule → cost → allocate.
 *
 * Two distance sources on the same street network:
 * - [proximity] (one-way streets ignored) decides which orders are close enough to share a round;
 * - [routing] (one-way streets respected) measures what is actually driven, and therefore paid.
 *
 * [serviceTime] is the time spent at each pickup and delivery (handing over the order); it delays the arrival
 * at the following stops but is not charged, since the cost model is per kilometer.
 */
class RoundPlanner(
	private val grouper: OrderGrouper,
	private val router: NearestNeighborRouter,
	private val costModel: CostModel,
	private val allocator: CostAllocator,
	private val proximity: DistanceMatrixProvider,
	private val routing: DistanceMatrixProvider,
	val serviceTime: Duration,
) {

	/** Parameters this planner prices and groups with; stored with each round so it can be explained later. */
	val pricePerKm get() = costModel.pricePerKm
	val radiusMeters get() = grouper.radiusMeters

	fun plan(orders: List<Order>): List<Round> {
		if (orders.isEmpty()) return emptyList()
		val points = (orders.map { it.merchant.location } + orders.map { it.destination }).distinct()
		val proximityMatrix = proximity.matrix(points)
		val routingMatrix = routing.matrix(points)

		return grouper.group(orders, proximityMatrix).map { group ->
			val stops = schedule(router.route(group, routingMatrix), routingMatrix)
			val distance = stops.sumOf { it.legDistanceMeters }
			val cost = costModel.cost(distance)
			log.debug(
				"Round of {} orders: {} ({} m, {} s total, \${})",
				group.size, stops.joinToString(" -> ") { "${it.kind}:${it.merchant.id}" }, distance.toLong(), stops.last().arrivalOffsetSeconds.toLong(), cost,
			)
			Round(group, stops, distance, stops.sumOf { it.legDurationSeconds }, cost, allocator.allocate(cost, group))
		}
	}

	/** Places each stop on the timeline: drive from the previous stop, plus service time at every pickup and delivery. */
	private fun schedule(route: List<Stop>, routing: DistanceMatrix): List<ScheduledStop> {
		val service = serviceTime.toSeconds().toDouble()
		var offset = 0.0
		return route.mapIndexed { i, stop ->
			if (i == 0) return@mapIndexed ScheduledStop(stop, 0.0, 0.0, 0.0)
			val previous = route[i - 1]
			val legDistance = routing.distance(previous.location, stop.location)
			val legDuration = routing.duration(previous.location, stop.location)
			offset += (if (previous.kind == StopKind.RETURN) 0.0 else service) + legDuration
			ScheduledStop(stop, legDistance, legDuration, offset)
		}
	}
}

private val log = LoggerFactory.getLogger(RoundPlanner::class.java)
