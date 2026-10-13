package com.nicoarbio.mercier.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Maps errors to RFC 9457 problem details. Spring MVC's own errors (validation, malformed JSON, missing parameters,
 * ResponseStatusException) are handled by the base class with their proper 4xx status.
 *
 * Client errors (4xx) are logged at DEBUG: they are the caller's problem, not the system's. Server errors are
 * logged here, while the request's correlation ids are still in the logging context.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

	@ExceptionHandler(NoSuchElementException::class)
	fun notFound(e: NoSuchElementException): ProblemDetail {
		log.debug("Not found: {}", e.message)
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Not found")
	}

	/** An operation that is not valid in the current state, e.g. dispatching a round that already left. */
	@ExceptionHandler(IllegalStateException::class)
	fun conflict(e: IllegalStateException): ProblemDetail {
		log.debug("Conflict: {}", e.message)
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Conflict")
	}

	/** A backing service (OSRM, Nominatim) failed or is unreachable; the client that called it already logged the details. */
	@ExceptionHandler(RestClientException::class)
	fun upstreamFailure(e: RestClientException): ProblemDetail {
		log.warn("Request failed because a backing service is unavailable: {}", e.message)
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "A backing service is unavailable")
	}

	/** Anything unexpected: a bug. Logged with its stack trace; the client only gets the request id to report it. */
	@ExceptionHandler(Exception::class)
	fun unexpected(e: Exception): ProblemDetail {
		log.error("Unexpected error", e)
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
	}

	/** Spring MVC errors handled by the base class (all 4xx here). */
	override fun handleExceptionInternal(
		ex: Exception,
		body: Any?,
		headers: HttpHeaders,
		statusCode: HttpStatusCode,
		request: WebRequest,
	): ResponseEntity<Any>? {
		log.debug("{}: {}", statusCode, ex.message)
		return super.handleExceptionInternal(ex, body, headers, statusCode, request)
	}
}

private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
