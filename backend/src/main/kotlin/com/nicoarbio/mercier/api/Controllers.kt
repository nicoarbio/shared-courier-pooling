package com.nicoarbio.mercier.api

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.TimeWindow
import com.nicoarbio.mercier.persistence.RoundStatus
import com.nicoarbio.mercier.planning.PlanningService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("/api/merchants")
@Tag(name = "Merchants")
class MerchantController(private val views: Views) {

	@GetMapping
	@Operation(summary = "List merchants (the UI picks one; there is no login)")
	fun list() = views.merchants()

	@GetMapping("/{id}/orders")
	@Operation(summary = "A merchant's orders with their round, the merchant's share and the estimated delivery time")
	fun orders(@PathVariable id: UUID) = views.ordersOf(id)
}

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders")
class OrderController(private val planning: PlanningService, private val views: Views) {

	@PostMapping
	@Operation(summary = "Submit an order; every order that has not left yet is re-planned")
	fun create(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
		val order = planning.submitOrder(
			merchantId = request.merchantId!!,
			destination = GeoPoint(request.destination!!.lon!!, request.destination.lat!!),
			address = request.address,
			window = TimeWindow(request.windowStart!!, request.windowEnd!!),
		)
		val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(order.id).toUri()
		return ResponseEntity.created(location).body(views.order(order.id))
	}

	@GetMapping("/{id}")
	@Operation(summary = "An order with its round, the merchant's share and the estimated delivery time")
	fun get(@PathVariable id: UUID) = views.order(id)
}

@RestController
@RequestMapping("/api/rounds")
@Tag(name = "Rounds")
class RoundController(private val planning: PlanningService, private val views: Views) {

	@GetMapping
	@Operation(summary = "Rounds by status (PLANNED: ready to leave)")
	fun list(@RequestParam(defaultValue = "PLANNED") status: RoundStatus) = views.rounds(status)

	@GetMapping("/{id}")
	@Operation(summary = "A round for the courier: ordered stops with estimated times and a Google Maps link")
	fun get(@PathVariable id: UUID) = views.round(id)

	@PostMapping("/{id}/dispatch")
	@Operation(summary = "The courier leaves: the round is frozen (no more re-planning) and its times use the actual departure")
	fun dispatch(@PathVariable id: UUID): RoundResponse {
		planning.dispatch(id)
		return views.round(id)
	}
}
