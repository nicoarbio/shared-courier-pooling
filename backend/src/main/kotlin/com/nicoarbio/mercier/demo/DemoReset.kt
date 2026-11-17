package com.nicoarbio.mercier.demo

import com.nicoarbio.mercier.domain.TimeWindow
import com.nicoarbio.mercier.persistence.OrderRepository
import com.nicoarbio.mercier.persistence.RoundRepository
import com.nicoarbio.mercier.planning.NewOrder
import com.nicoarbio.mercier.planning.PlanningService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Demo support: puts the system in its demo state, the paper's case study already planned (the four shops, their
 * five orders and the resulting round), ready to show the screens and dispatch the round. Only exists when
 * `mercier.demo.reset-enabled=true` (set for the Docker Compose demo); otherwise these beans are not created and the
 * endpoints answer 404, so a regular deployment cannot be wiped through the API.
 */
@Service
@ConditionalOnBooleanProperty("mercier.demo.reset-enabled")
class DemoResetService(
	private val orders: OrderRepository,
	private val rounds: RoundRepository,
	private val planning: PlanningService,
	private val jdbc: JdbcTemplate,
	private val clock: Clock,
) {

	/**
	 * Deletes every order and round (with their stops and cost shares; merchants stay) and loads the case study.
	 * Orders reference rounds and delivery stops reference orders, so no table-by-table order works: a single
	 * TRUNCATE of the four tables lets PostgreSQL drop them together. All in one transaction: if planning fails
	 * (e.g. OSRM is down), nothing is deleted.
	 */
	@Transactional
	fun reset(): DemoResetResult {
		val deletedOrders = orders.count()
		val deletedRounds = rounds.count()
		jdbc.execute("truncate table round_share, round_stop, delivery_order, round")
		val loaded = loadCaseStudy()
		log.info("Demo reset: deleted {} orders and {} rounds; loaded the case study", deletedOrders, deletedRounds)
		return DemoResetResult(deletedOrders, deletedRounds, loaded, rounds.count())
	}

	/**
	 * Submits the case study's five orders. The window is relative to now (from the next multiple of 5 minutes,
	 * lasting 30), so the round always shows up about to leave, whenever the demo is run.
	 */
	@Transactional
	fun loadCaseStudy(): Int {
		val start = nextFiveMinutes(LocalDateTime.now(clock))
		val window = TimeWindow(start, start.plusMinutes(30))
		val submitted = planning.submitOrders(
			CaseStudyOrders.orders.map { NewOrder(it.merchantId, it.destination, it.address, window) },
		)
		return submitted.size
	}

	private fun nextFiveMinutes(now: LocalDateTime): LocalDateTime {
		val minute = now.truncatedTo(ChronoUnit.MINUTES)
		return minute.plusMinutes((5 - minute.minute % 5).toLong())
	}
}

data class DemoResetResult(val deletedOrders: Long, val deletedRounds: Long, val loadedOrders: Int, val plannedRounds: Long)

/**
 * A fresh deployment starts in the demo state too: if there are no orders at startup, load the case study.
 * Best effort: if the routing engine is not reachable yet, the app still starts and "reset demo" loads it later.
 */
@Component
@ConditionalOnBooleanProperty("mercier.demo.reset-enabled")
class DemoStartupLoader(private val demo: DemoResetService, private val orders: OrderRepository) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		if (orders.count() > 0) return
		try {
			val loaded = demo.loadCaseStudy()
			log.info("Demo: loaded the case study ({} orders) on startup", loaded)
		} catch (e: Exception) {
			log.warn("Demo: could not load the case study on startup ({}); use 'reset demo' once the services are up", e.message)
		}
	}
}

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo")
@ConditionalOnBooleanProperty("mercier.demo.reset-enabled")
class DemoController(private val demo: DemoResetService) {

	@GetMapping
	@Operation(summary = "Answers 200 when demo features are enabled (the UI shows the reset button), 404 otherwise")
	fun status() = mapOf("resetEnabled" to true)

	@PostMapping("/reset")
	@Operation(summary = "Back to the demo state: deletes all orders and rounds and loads the case study, already planned")
	fun reset() = demo.reset()
}

private val log = LoggerFactory.getLogger(DemoResetService::class.java)
