package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.engine.Fixtures.merchant
import com.nicoarbio.mercier.engine.Fixtures.order
import com.nicoarbio.mercier.engine.Fixtures.point
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CostTest {

	@Test
	fun `cost is price per km times distance driven`() {
		assertEquals(BigDecimal("2499.08"), CostModel(BigDecimal(1266)).cost(1974.0))
	}

	@Test
	fun `cost is split proportionally to delivery stops`() {
		val bakery = merchant("bakery", point(1))
		val kiosk = merchant("kiosk", point(2))
		val orders = listOf(
			order("b1", bakery, point(10)),
			order("k1", kiosk, point(11)),
			order("k2", kiosk, point(12)),
		)

		val shares = CostAllocator().allocate(BigDecimal("3000.00"), orders)

		assertEquals(BigDecimal("1000.00"), shares[bakery])
		assertEquals(BigDecimal("2000.00"), shares[kiosk])
	}

	@Test
	fun `rounded shares add up exactly to the round cost`() {
		val merchants = (1..3).map { merchant("m$it", point(it)) }
		val orders = merchants.mapIndexed { i, m -> order("o$i", m, point(10 + i)) }

		val shares = CostAllocator().allocate(BigDecimal("100.00"), orders)

		assertEquals(listOf("33.33", "33.33", "33.34").map(::BigDecimal), shares.values.toList())
		assertEquals(BigDecimal("100.00"), shares.values.fold(BigDecimal.ZERO, BigDecimal::add))
	}
}
