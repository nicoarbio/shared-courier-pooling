package com.nicoarbio.mercier.scenario

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.Round
import com.nicoarbio.mercier.domain.StopKind
import com.nicoarbio.mercier.engine.CostAllocator
import com.nicoarbio.mercier.engine.CostModel
import com.nicoarbio.mercier.engine.NearestNeighborRouter
import com.nicoarbio.mercier.engine.OrderGrouper
import com.nicoarbio.mercier.engine.RoundPlanner
import com.nicoarbio.mercier.routing.OsrmDistanceMatrixProvider
import org.junit.jupiter.api.Tag
import org.springframework.web.client.RestClient
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs the engine on the case study against the real OSRM services and writes a report.
 *
 * Requires `docker compose up -d osrm-routing osrm-proximity`. Run with: `./mvnw test -Pscenario`.
 * Report: printed to stdout and written to target/scenario-report.md.
 */
@Tag("scenario")
class CaseStudyScenarioTest {

	private val routing = osrm(System.getenv("OSRM_ROUTING_URL") ?: "http://localhost:5000")
	private val proximity = osrm(System.getenv("OSRM_PROXIMITY_URL") ?: "http://localhost:5001")

	@Test
	fun `case study report`() {
		val report = StringBuilder()
		val orders = CaseStudy.orders

		// Baseline: one dedicated round trip (shop -> customer -> shop) per order.
		val points = (CaseStudy.merchants.map { it.location } + orders.map { it.destination }).distinct()
		val matrix = routing.matrix(points)
		val dedicated = orders.associateWith { o ->
			matrix.distance(o.merchant.location, o.destination) + matrix.distance(o.destination, o.merchant.location)
		}
		val dedicatedTotal = dedicated.values.sum()
		val averageKm = dedicatedTotal / orders.size / 1000
		val pricePerKm = BigDecimal(CaseStudy.DEDICATED_TRIP_PRICE).divide(BigDecimal.valueOf(averageKm), 2, RoundingMode.HALF_EVEN)
		val costModel = CostModel(pricePerKm)

		report.appendLine("# Case study — engine run").appendLine()
		report.appendLine("## Baseline: dedicated trips").appendLine()
		report.appendLine("| Order | Shop → customer → shop (m) | Straight line one way (m) |")
		report.appendLine("|---|---:|---:|")
		dedicated.forEach { (o, d) -> report.appendLine("| ${o.id} | ${d.m()} | ${haversine(o.merchant.location, o.destination).m()} |") }
		report.appendLine()
		report.appendLine("- Total dedicated distance: ${dedicatedTotal.m()} m (${orders.size} trips × $${CaseStudy.DEDICATED_TRIP_PRICE} = $${orders.size * CaseStudy.DEDICATED_TRIP_PRICE})")
		report.appendLine("- Calibrated price: $${CaseStudy.DEDICATED_TRIP_PRICE} / ${"%.3f".format(averageKm)} km = **$$pricePerKm per km**").appendLine()

		// Engine run at the paper's radius, plus a sensitivity analysis.
		for (radius in listOf(300.0, 400.0, 500.0)) {
			val rounds = planner(radius, costModel).plan(orders)
			assertEquals(orders.toSet(), rounds.flatMap { it.orders }.toSet(), "every order is assigned to exactly one round")
			report.appendRun(radius, rounds, dedicated, costModel)
		}

		println(report)
		File("target/scenario-report.md").writeText(report.toString())
	}

	private fun planner(radiusMeters: Double, costModel: CostModel) = RoundPlanner(
		grouper = OrderGrouper(radiusMeters, Duration.ofMinutes(15)),
		router = NearestNeighborRouter(),
		costModel = costModel,
		allocator = CostAllocator(),
		proximity = proximity,
		routing = routing,
		serviceTime = Duration.ofMinutes(2),
	)

	/**
	 * Per-shop comparison against two baselines:
	 * - flat: what the shop pays today ($2,500 per order, as stated by the case study);
	 * - same $/km: the shop's own dedicated trips priced with the calibrated tariff, i.e. the same cost model as the round.
	 * Both baselines add up to the same total; they differ per shop because real trips are not all the same length.
	 */
	private fun StringBuilder.appendRun(radius: Double, rounds: List<Round>, dedicated: Map<Order, Double>, costModel: CostModel) {
		val dedicatedTotal = dedicated.values.sum()
		val distance = rounds.sumOf { it.distanceMeters }
		val cost = rounds.fold(BigDecimal.ZERO) { acc, r -> acc + r.cost }
		appendLine("## Radius ${radius.m()} m — ${rounds.size} round(s)").appendLine()
		rounds.forEachIndexed { i, r ->
			appendLine("### Round ${i + 1}: ${r.distanceMeters.m()} m, ${"%.1f".format(r.drivingSeconds / 60)} min driving (${"%.1f".format(r.totalSeconds / 60)} min with 2 min per stop), $${r.cost}").appendLine()
			appendLine("Route: " + r.stops.joinToString(" → ") { s ->
				when (s.kind) {
					StopKind.PICKUP -> "pickup ${s.merchant.id}"
					StopKind.DELIVERY -> "deliver ${s.order!!.id}"
					StopKind.RETURN -> "return"
				}
			}).appendLine()
			appendLine("| Shop | Stops | Pays in round | Dedicated, flat | Dedicated, same $/km |")
			appendLine("|---|---:|---:|---:|---:|")
			r.costByMerchant.forEach { (m, share) ->
				val own = r.orders.filter { it.merchant == m }
				val sameRate = costModel.cost(own.sumOf { dedicated.getValue(it) })
				appendLine("| ${m.name} | ${own.size} | $$share | $${own.size * CaseStudy.DEDICATED_TRIP_PRICE} | $$sameRate |")
			}
			appendLine()
		}
		appendLine("**Total: ${distance.m()} m, $$cost — savings vs dedicated: ${"%.0f".format((1 - distance / dedicatedTotal) * 100)}%**").appendLine()
	}

	private fun Double.m() = "%.0f".format(this)

	private fun haversine(a: GeoPoint, b: GeoPoint): Double {
		val r = 6_371_000.0
		val dLat = Math.toRadians(b.lat - a.lat)
		val dLon = Math.toRadians(b.lon - a.lon)
		val h = Math.sin(dLat / 2).let { it * it } +
			Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat)) * Math.sin(dLon / 2).let { it * it }
		return 2 * r * Math.asin(Math.sqrt(h))
	}

	private fun osrm(url: String) = OsrmDistanceMatrixProvider(RestClient.builder().baseUrl(url).build())
}
