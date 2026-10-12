package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.StopKind
import com.nicoarbio.mercier.engine.Fixtures.matrix
import com.nicoarbio.mercier.engine.Fixtures.merchant
import com.nicoarbio.mercier.engine.Fixtures.order
import com.nicoarbio.mercier.engine.Fixtures.point
import kotlin.test.Test
import kotlin.test.assertEquals

class NearestNeighborRouterTest {

	private val router = NearestNeighborRouter()

	private val m1 = merchant("m1", point(1))
	private val m2 = merchant("m2", point(2))
	private val a = point(10)
	private val b = point(11)
	private val points = listOf(m1.location, m2.location, a, b)

	@Test
	fun `picks up at every merchant, then delivers, then returns to the first merchant`() {
		val routing = matrix(points, default = 100.0, distances = emptyMap())
		val o1 = order("o1", m1, a)
		val o2 = order("o2", m2, b)

		val stops = router.route(listOf(o1, o2), routing)

		assertEquals(
			listOf(StopKind.PICKUP, StopKind.PICKUP, StopKind.DELIVERY, StopKind.DELIVERY, StopKind.RETURN),
			stops.map { it.kind },
		)
		assertEquals(m1.location, stops.first().location)
		assertEquals(m1.location, stops.last().location)
	}

	@Test
	fun `starts at the merchant that yields the shortest route`() {
		// One-way block: m2 -> m1 is a few meters, m1 -> m2 forces a loop around the block.
		val routing = matrix(points, default = 100.0, distances = mapOf(
			(m1.location to m2.location) to 1000.0,
			(m2.location to m1.location) to 20.0,
		))
		val o1 = order("o1", m1, a)
		val o2 = order("o2", m2, b)

		val pickups = router.route(listOf(o1, o2), routing).filter { it.kind == StopKind.PICKUP }

		assertEquals(listOf(m2, m1), pickups.map { it.merchant })
	}

	@Test
	fun `delivers to the nearest pending destination first, using directed distances`() {
		// From m1, b is closer than a only in the m1 -> b direction (one-way street effect).
		val routing = matrix(points, distances = mapOf(
			(m1.location to a) to 300.0, (m1.location to b) to 200.0,
			(b to m1.location) to 900.0,
			(b to a) to 150.0, (a to b) to 150.0,
		))
		val toA = order("toA", m1, a)
		val toB = order("toB", m1, b)

		val deliveries = router.route(listOf(toA, toB), routing).filter { it.kind == StopKind.DELIVERY }

		assertEquals(listOf(toB, toA), deliveries.map { it.order })
	}
}
