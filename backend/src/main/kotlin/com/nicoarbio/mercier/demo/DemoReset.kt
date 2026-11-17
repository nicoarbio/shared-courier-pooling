package com.nicoarbio.mercier.demo

import com.nicoarbio.mercier.persistence.OrderRepository
import com.nicoarbio.mercier.persistence.RoundRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Demo support: puts the system back to its initial state (the merchants, no orders, no rounds) without
 * redeploying. Only exists when `mercier.demo.reset-enabled=true` (set for the Docker Compose demo); otherwise
 * these endpoints are not registered and answer 404, so a regular deployment cannot be wiped through the API.
 */
@Service
@ConditionalOnBooleanProperty("mercier.demo.reset-enabled")
class DemoResetService(
	private val orders: OrderRepository,
	private val rounds: RoundRepository,
	private val jdbc: JdbcTemplate,
) {

	/**
	 * Deletes every order and round, with their stops and cost shares; merchants stay.
	 * Orders reference rounds and delivery stops reference orders, so no table-by-table order works: a single
	 * TRUNCATE of the four tables lets PostgreSQL drop them together (and it is transactional).
	 */
	@Transactional
	fun reset(): DemoResetResult {
		val result = DemoResetResult(deletedOrders = orders.count(), deletedRounds = rounds.count())
		jdbc.execute("truncate table round_share, round_stop, delivery_order, round")
		log.info("Demo reset: deleted {} orders and {} rounds", result.deletedOrders, result.deletedRounds)
		return result
	}
}

data class DemoResetResult(val deletedOrders: Long, val deletedRounds: Long)

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo")
@ConditionalOnBooleanProperty("mercier.demo.reset-enabled")
class DemoController(private val demo: DemoResetService) {

	@GetMapping
	@Operation(summary = "Answers 200 when demo features are enabled (the UI shows the reset button), 404 otherwise")
	fun status() = mapOf("resetEnabled" to true)

	@PostMapping("/reset")
	@Operation(summary = "Back to the initial state: deletes all orders and rounds, keeps the merchants")
	fun reset() = demo.reset()
}

private val log = LoggerFactory.getLogger(DemoResetService::class.java)
