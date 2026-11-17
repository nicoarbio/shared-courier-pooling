package com.nicoarbio.mercier.demo

import com.nicoarbio.mercier.TestcontainersConfiguration
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
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoResetTest {

	/** Default configuration (any regular deployment): the demo endpoints do not exist. */
	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@Import(TestcontainersConfiguration::class)
	inner class Disabled @Autowired constructor(private val mvc: MockMvc) {

		@Test
		fun `reset is not available unless enabled`() {
			mvc.get("/api/demo").andExpect { status { isNotFound() } }
			mvc.post("/api/demo/reset").andExpect { status { isNotFound() } }
		}
	}

	/**
	 * Demo enabled, with fake distances (no OSRM: a city grid, |Δlon| + |Δlat| × 100,000 m) and "now" fixed at
	 * 18:52 in Buenos Aires, so the case study's window is known: 18:55 to 19:25.
	 */
	@Nested
	@SpringBootTest(properties = ["mercier.demo.reset-enabled=true"])
	@AutoConfigureMockMvc
	@Import(TestcontainersConfiguration::class, DemoResetTest.Fakes::class)
	inner class Enabled @Autowired constructor(
		private val mvc: MockMvc,
		private val merchants: MerchantRepository,
		private val orders: OrderRepository,
		private val rounds: RoundRepository,
		private val startupLoader: DemoStartupLoader,
		private val jdbc: JdbcTemplate,
	) {

		@BeforeEach
		fun caseStudyShopsOnly() {
			jdbc.execute("truncate round_share, round_stop, delivery_order, round, merchant cascade")
			listOf(
				CaseStudyOrders.BAKERY to "Panadería San Martín",
				CaseStudyOrders.GREENGROCER to "Verdulería La Estación",
				CaseStudyOrders.GROCERY to "Almacén Don Pedro",
				CaseStudyOrders.KIOSK to "Kiosco Sarmiento",
			).forEachIndexed { i, (id, name) -> merchants.save(MerchantEntity(id, name, -58.44470 + i * 0.00018, -34.62046 + i * 0.00007)) }
		}

		@Test
		fun `reset loads the case study already planned, with a window starting at the next 5 minutes`() {
			mvc.get("/api/demo").andExpect { status { isOk() } }

			mvc.post("/api/demo/reset").andExpect {
				status { isOk() }
				jsonPath("$.loadedOrders") { value(5) }
			}
			// Again, now over a planned round: its delivery stops reference the orders (the case that once failed).
			mvc.post("/api/demo/reset").andExpect {
				status { isOk() }
				jsonPath("$.deletedOrders") { value(5) }
				jsonPath("$.loadedOrders") { value(5) }
			}

			val all = orders.findAll()
			assertEquals(5, all.size)
			assertTrue(all.all { it.status == OrderStatus.PLANNED })
			assertTrue(all.all { it.windowStart == LocalDateTime.of(2026, 10, 13, 18, 55) && it.windowEnd == LocalDateTime.of(2026, 10, 13, 19, 25) })
			assertEquals(CaseStudyOrders.orders.map { it.address }.toSet(), all.map { it.address }.toSet())
			assertTrue(rounds.count() >= 1)
			assertEquals(4, merchants.count())
		}

		@Test
		fun `startup loads the case study only when there are no orders`() {
			startupLoader.run(DefaultApplicationArguments())
			assertEquals(5, orders.count())

			startupLoader.run(DefaultApplicationArguments())
			assertEquals(5, orders.count(), "existing orders are left alone")
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	class Fakes {
		@Bean
		@Primary
		fun fixedClock(): Clock = Clock.fixed(
			LocalDateTime.of(2026, 10, 13, 18, 52).atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant(),
			ZoneId.of("America/Argentina/Buenos_Aires"),
		)

		@Bean
		@Primary
		fun fakeRoundPlanner(): RoundPlanner {
			val grid = DistanceMatrixProvider { points ->
				val d = points.map { a -> points.map { b -> (abs(a.lon - b.lon) + abs(a.lat - b.lat)) * 100_000 } }
				DistanceMatrix(points, d, d.map { row -> row.map { it / 5 } })
			}
			return RoundPlanner(
				OrderGrouper(500.0, Duration.ofMinutes(15)), NearestNeighborRouter(), CostModel(BigDecimal(1000)),
				CostAllocator(), proximity = grid, routing = grid, serviceTime = Duration.ofMinutes(2),
			)
		}
	}
}
