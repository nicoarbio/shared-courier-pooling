package com.nicoarbio.mercier.planning

import com.nicoarbio.mercier.TestcontainersConfiguration
import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.StopKind
import com.nicoarbio.mercier.domain.TimeWindow
import com.nicoarbio.mercier.engine.CostAllocator
import com.nicoarbio.mercier.engine.CostModel
import com.nicoarbio.mercier.engine.NearestNeighborRouter
import com.nicoarbio.mercier.engine.OrderGrouper
import com.nicoarbio.mercier.engine.RoundPlanner
import com.nicoarbio.mercier.persistence.MerchantEntity
import com.nicoarbio.mercier.persistence.MerchantRepository
import com.nicoarbio.mercier.persistence.OrderRepository
import com.nicoarbio.mercier.persistence.OrderStatus
import com.nicoarbio.mercier.persistence.RoundRepository
import com.nicoarbio.mercier.routing.DistanceMatrix
import com.nicoarbio.mercier.routing.DistanceMatrixProvider
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Planning against a real PostgreSQL (Testcontainers). Distances are fake so OSRM is not needed:
 * points are on a plane where 1 unit of lon or lat = 1 meter, and distance is |Δlon| + |Δlat| (a city grid).
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class, PlanningServiceTest.FakeDistances::class)
class PlanningServiceTest @Autowired constructor(
	private val planning: PlanningService,
	private val merchants: MerchantRepository,
	private val orders: OrderRepository,
	private val rounds: RoundRepository,
	private val jdbc: JdbcTemplate,
) {

	@TestConfiguration(proxyBeanMethods = false)
	class FakeDistances {
		@Bean
		@Primary
		fun fakeRoundPlanner(): RoundPlanner {
			val grid = DistanceMatrixProvider { points ->
				val d = points.map { a -> points.map { b -> abs(a.lon - b.lon) + abs(a.lat - b.lat) } }
				DistanceMatrix(points, d, d.map { row -> row.map { it / 5 } })
			}
			return RoundPlanner(
				OrderGrouper(500.0, Duration.ofMinutes(15)), NearestNeighborRouter(),
				CostModel(BigDecimal(1000)), CostAllocator(), proximity = grid, routing = grid,
			)
		}
	}

	private val window = TimeWindow(LocalDateTime.of(2026, 10, 13, 19, 0), LocalDateTime.of(2026, 10, 13, 19, 30))
	private lateinit var bakery: MerchantEntity
	private lateinit var kiosk: MerchantEntity

	@BeforeEach
	fun cleanDatabase() {
		jdbc.execute("truncate round_share, round_stop, delivery_order, round, merchant cascade")
		bakery = merchants.save(MerchantEntity(UUID.randomUUID(), "bakery", 0.0, 0.0))
		kiosk = merchants.save(MerchantEntity(UUID.randomUUID(), "kiosk", 10.0, 0.0))
	}

	@Test
	fun `a new order close to a planned round joins it`() {
		planning.submitOrder(bakery.id, GeoPoint(0.0, 300.0), "A", window)
		planning.submitOrder(kiosk.id, GeoPoint(10.0, 200.0), "B", window)

		val all = rounds.findAll()
		assertEquals(1, all.size)
		val round = all.single()
		assertEquals(listOf(StopKind.PICKUP, StopKind.PICKUP, StopKind.DELIVERY, StopKind.DELIVERY, StopKind.RETURN), round.stops.map { it.kind })
		assertEquals(setOf(bakery.id, kiosk.id), round.shares.keys)
		assertEquals(round.cost, round.shares.values.fold(BigDecimal.ZERO, BigDecimal::add))
		assertEquals(BigDecimal("1000.00"), round.pricePerKm.setScale(2))
		orders.findAll().forEach { assertEquals(OrderStatus.PLANNED, it.status) }
	}

	@Test
	fun `a new order beyond the radius gets its own round`() {
		planning.submitOrder(bakery.id, GeoPoint(0.0, 300.0), "near", window)
		planning.submitOrder(bakery.id, GeoPoint(0.0, 900.0), "far", window)

		assertEquals(2, rounds.count())
	}

	@Test
	fun `re-planning replaces previous rounds instead of accumulating them`() {
		planning.submitOrder(bakery.id, GeoPoint(0.0, 300.0), "A", window)
		planning.submitOrder(kiosk.id, GeoPoint(10.0, 200.0), "B", window)
		val before = rounds.findAll().map { it.id }

		planning.replan()

		val after = rounds.findAll().map { it.id }
		assertEquals(1, after.size)
		assert(after.none { it in before }) { "previous rounds should have been deleted" }
		orders.findAll().forEach { assertEquals(after.single(), it.round!!.id) }
	}
}
