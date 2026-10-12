package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.engine.Fixtures.matrix
import com.nicoarbio.mercier.engine.Fixtures.merchant
import com.nicoarbio.mercier.engine.Fixtures.order
import com.nicoarbio.mercier.engine.Fixtures.point
import com.nicoarbio.mercier.engine.Fixtures.symmetric
import com.nicoarbio.mercier.routing.DistanceMatrixProvider
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundPlannerTest {

	private val shop = merchant("shop", point(1))
	private val a = point(10)
	private val b = point(11)
	private val points = listOf(shop.location, a, b)

	// Proximity: both destinations are close to the shop (one-way streets ignored).
	private val proximity = DistanceMatrixProvider { matrix(points, distances = symmetric((shop.location to a) to 100.0, (shop.location to b) to 200.0, (a to b) to 150.0)) }

	// Routing: reaching a takes a long detour because of a one-way street.
	private val routing = DistanceMatrixProvider {
		matrix(points, distances = mapOf(
			(shop.location to a) to 1200.0, (shop.location to b) to 200.0,
			(b to a) to 150.0, (a to shop.location) to 100.0,
		))
	}

	private val planner = RoundPlanner(
		grouper = OrderGrouper(500.0, Duration.ofMinutes(15)),
		router = NearestNeighborRouter(),
		costModel = CostModel(BigDecimal(1000)),
		allocator = CostAllocator(),
		proximity = proximity,
		routing = routing,
	)

	@Test
	fun `groups by proximity but measures and charges the directed route`() {
		val rounds = planner.plan(listOf(order("o1", shop, a), order("o2", shop, b)))

		assertEquals(1, rounds.size) // grouped despite the 1200 m directed detour to a
		val round = rounds.single()
		// shop -> b (200) -> a (150) -> shop (100)
		assertEquals(450.0, round.distanceMeters)
		assertEquals(BigDecimal("450.00"), round.cost)
		assertEquals(BigDecimal("450.00"), round.costByMerchant[shop])
	}
}
