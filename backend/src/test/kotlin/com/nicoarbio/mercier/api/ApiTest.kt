package com.nicoarbio.mercier.api

import com.nicoarbio.mercier.TestcontainersConfiguration
import com.nicoarbio.mercier.engine.CostAllocator
import com.nicoarbio.mercier.engine.CostModel
import com.nicoarbio.mercier.engine.NearestNeighborRouter
import com.nicoarbio.mercier.engine.OrderGrouper
import com.nicoarbio.mercier.engine.RoundPlanner
import com.nicoarbio.mercier.persistence.MerchantEntity
import com.nicoarbio.mercier.persistence.MerchantRepository
import com.nicoarbio.mercier.routing.DistanceMatrix
import com.nicoarbio.mercier.routing.DistanceMatrixProvider
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs
import kotlin.test.Test

/**
 * End-to-end through HTTP, against PostgreSQL (Testcontainers). Distances are fake so OSRM is not needed:
 * a city grid where distance = (|Δlon| + |Δlat|) × 100,000 m and driving speed is 5 m/s. "Now" is fixed at 18:00.
 *
 * Shops: bakery at (0, 0), kiosk at (0.0001, 0) — 10 m apart. Destinations 300 m and 200 m away.
 * Best route starts at the bakery: pickup bakery → pickup kiosk (10 m, 2 s) → deliver kiosk order (200 m, 40 s)
 * → deliver bakery order (110 m, 22 s) → return (300 m, 60 s). With 2 min per stop, arrivals are at
 * +0, +122, +282, +424 and +604 s after departure.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, ApiTest.Fakes::class)
class ApiTest @Autowired constructor(
	private val mvc: MockMvc,
	private val merchants: MerchantRepository,
	private val jdbc: JdbcTemplate,
) {

	@TestConfiguration(proxyBeanMethods = false)
	class Fakes {
		@Bean
		@Primary
		fun fixedClock(): Clock = Clock.fixed(
			LocalDateTime.of(2026, 10, 13, 18, 0).atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant(),
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

	private lateinit var bakery: UUID
	private lateinit var kiosk: UUID

	@BeforeEach
	fun cleanDatabase() {
		jdbc.execute("truncate round_share, round_stop, delivery_order, round, merchant cascade")
		bakery = merchants.save(MerchantEntity(UUID.randomUUID(), "bakery", 0.0, 0.0)).id
		kiosk = merchants.save(MerchantEntity(UUID.randomUUID(), "kiosk", 0.0001, 0.0)).id
	}

	private fun orderJson(merchant: UUID, lon: Double, lat: Double, start: String = "19:00", end: String = "19:30") = """
		{"merchantId": "$merchant", "destination": {"lon": $lon, "lat": $lat}, "address": "Somewhere 123",
		 "windowStart": "2026-10-13T$start:00", "windowEnd": "2026-10-13T$end:00"}
	"""

	private fun submit(merchant: UUID, lon: Double, lat: Double) =
		mvc.post("/api/orders") { contentType = MediaType.APPLICATION_JSON; content = orderJson(merchant, lon, lat) }

	private fun roundId(): String =
		Regex("\"id\":\"([^\"]+)\"").find(mvc.get("/api/rounds").andReturn().response.contentAsString)!!.groupValues[1]

	@Test
	fun `submitting an order returns it planned, with the merchant's share and estimated delivery`() {
		submit(bakery, 0.0, 0.003).andExpect {
			status { isCreated() }
			header { string("Location", containsString("/api/orders/")) }
			jsonPath("$.status") { value("PLANNED") }
			jsonPath("$.round.roundStatus") { value("PLANNED") }
			jsonPath("$.round.merchantShare") { exists() }
		}
	}

	@Test
	fun `a merchant sees each order's estimated delivery, from the window start along the round timeline`() {
		submit(bakery, 0.0, 0.003)
		submit(kiosk, 0.0001, 0.002)

		mvc.get("/api/merchants/$bakery/orders").andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].round.estimatedDelivery") { value(startsWith("2026-10-13T19:07:0")) } // 19:00 + 424 s
		}
		mvc.get("/api/merchants/$kiosk/orders").andExpect {
			jsonPath("$[0].round.estimatedDelivery") { value(startsWith("2026-10-13T19:04:4")) } // 19:00 + 282 s
		}
	}

	@Test
	fun `the courier gets the ordered stops with times and a Google Maps link`() {
		submit(bakery, 0.0, 0.003)
		submit(kiosk, 0.0001, 0.002)

		mvc.get("/api/rounds/${roundId()}").andExpect {
			status { isOk() }
			jsonPath("$.stops.length()") { value(5) }
			jsonPath("$.stops[*].kind") { value(contains("PICKUP", "PICKUP", "DELIVERY", "DELIVERY", "RETURN")) }
			jsonPath("$.stops[0].merchantName") { value("bakery") }
			jsonPath("$.stops[2].address") { value("Somewhere 123") }
			jsonPath("$.departureAt") { value("2026-10-13T19:00:00") }
			jsonPath("$.googleMapsUrl") { value(startsWith("https://www.google.com/maps/dir/?api=1")) }
			jsonPath("$.shares.length()") { value(2) }
		}
	}

	@Test
	fun `dispatching freezes the round at the actual departure and keeps it out of re-planning`() {
		submit(bakery, 0.0, 0.003)
		val dispatched = roundId()

		mvc.post("/api/rounds/$dispatched/dispatch").andExpect {
			status { isOk() }
			jsonPath("$.status") { value("DISPATCHED") }
			jsonPath("$.departureAt") { value("2026-10-13T18:00:00") } // the fixed "now"
		}
		mvc.post("/api/rounds/$dispatched/dispatch").andExpect { status { isConflict() } }

		// A new order nearby cannot join a round that already left: it gets a new one.
		submit(kiosk, 0.0001, 0.002)
		mvc.get("/api/rounds?status=DISPATCHED").andExpect { jsonPath("$.length()") { value(1) } }
		mvc.get("/api/rounds?status=PLANNED").andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].deliveries") { value(1) }
		}
	}

	@Test
	fun `invalid input is rejected with problem details`() {
		mvc.post("/api/orders") { contentType = MediaType.APPLICATION_JSON; content = orderJson(bakery, 0.0, 0.003, start = "19:30", end = "19:00") }
			.andExpect { status { isBadRequest() }; content { contentType(MediaType.APPLICATION_PROBLEM_JSON) } }
		mvc.post("/api/orders") { contentType = MediaType.APPLICATION_JSON; content = orderJson(bakery, 0.0, 95.0) }
			.andExpect { status { isBadRequest() } }
		mvc.post("/api/orders") { contentType = MediaType.APPLICATION_JSON; content = orderJson(UUID.randomUUID(), 0.0, 0.003) }
			.andExpect { status { isNotFound() }; jsonPath("$.detail") { value(containsString("Merchant")) } }
		mvc.get("/api/rounds/${UUID.randomUUID()}").andExpect { status { isNotFound() } }
	}

	@Test
	fun `OpenAPI description is published`() {
		mvc.get("/v3/api-docs").andExpect {
			status { isOk() }
			jsonPath("$.paths['/api/orders']") { exists() }
		}
	}
}
