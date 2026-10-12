package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.engine.Fixtures.matrix
import com.nicoarbio.mercier.engine.Fixtures.merchant
import com.nicoarbio.mercier.engine.Fixtures.order
import com.nicoarbio.mercier.engine.Fixtures.point
import com.nicoarbio.mercier.engine.Fixtures.symmetric
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderGrouperTest {

	private val grouper = OrderGrouper(radiusMeters = 500.0, windowTolerance = Duration.ofMinutes(15))

	private val m1 = merchant("m1", point(1))
	private val m2 = merchant("m2", point(2))
	private val a = point(10)
	private val b = point(11)
	private val c = point(12)
	private val points = listOf(m1.location, m2.location, a, b, c)

	@Test
	fun `orders whose destinations are within radius of every merchant share a round`() {
		val proximity = matrix(points, distances = symmetric(
			(m1.location to a) to 300.0, (m1.location to b) to 400.0,
			(m2.location to a) to 350.0, (m2.location to b) to 450.0,
		))
		val o1 = order("o1", m1, a)
		val o2 = order("o2", m2, b)

		assertEquals(listOf(listOf(o1, o2)), grouper.group(listOf(o1, o2), proximity))
	}

	@Test
	fun `a destination beyond the radius of another merchant in the round opens a new round`() {
		val proximity = matrix(points, distances = symmetric(
			(m1.location to a) to 300.0, (m1.location to b) to 400.0,
			(m2.location to a) to 600.0, (m2.location to b) to 450.0, // a is too far from m2
		))
		val o1 = order("o1", m1, a)
		val o2 = order("o2", m2, b)

		assertEquals(listOf(listOf(o1), listOf(o2)), grouper.group(listOf(o1, o2), proximity))
	}

	@Test
	fun `windows starting more than the tolerance apart are not grouped`() {
		val proximity = matrix(points, distances = symmetric((m1.location to a) to 100.0, (m1.location to b) to 100.0))
		val o1 = order("o1", m1, a, startMinute = 0)
		val o2 = order("o2", m1, b, startMinute = 16)

		assertEquals(listOf(listOf(o1), listOf(o2)), grouper.group(listOf(o1, o2), proximity))
	}

	@Test
	fun `complete-linkage - joining requires compatibility with every order already in the round`() {
		// o3 is within the window tolerance of o2 (10 min) but not of o1 (20 min): it cannot join their round.
		val proximity = matrix(points, distances = symmetric(
			(m1.location to a) to 100.0, (m1.location to b) to 100.0, (m1.location to c) to 100.0,
		))
		val o1 = order("o1", m1, a, startMinute = 0)
		val o2 = order("o2", m1, b, startMinute = 10)
		val o3 = order("o3", m1, c, startMinute = 20)

		assertEquals(listOf(listOf(o1, o2), listOf(o3)), grouper.group(listOf(o3, o1, o2), proximity))
	}

	@Test
	fun `an order beyond the radius of its own merchant always goes alone`() {
		val proximity = matrix(points, distances = symmetric((m1.location to a) to 100.0, (m1.location to b) to 900.0))
		val near = order("near", m1, a)
		val far = order("far", m1, b)

		assertEquals(listOf(listOf(near), listOf(far)), grouper.group(listOf(near, far), proximity))
	}
}
