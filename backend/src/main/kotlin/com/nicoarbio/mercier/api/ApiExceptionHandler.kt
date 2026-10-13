package com.nicoarbio.mercier.api

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Maps domain errors to RFC 9457 problem details. Validation errors are already handled by Spring (400). */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(NoSuchElementException::class)
	fun notFound(e: NoSuchElementException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Not found")

	/** An operation that is not valid in the current state, e.g. dispatching a round that already left. */
	@ExceptionHandler(IllegalStateException::class)
	fun conflict(e: IllegalStateException): ProblemDetail =
		ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Conflict")
}
