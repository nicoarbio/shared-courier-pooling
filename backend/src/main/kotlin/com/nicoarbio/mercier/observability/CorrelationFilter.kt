package com.nicoarbio.mercier.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Puts correlation ids in the logging context (MDC) for the whole request, so every log line it produces,
 * including calls to OSRM and Nominatim, can be traced back to it:
 * - requestId: from the `X-Request-Id` header, or generated; echoed back in the response.
 * - sessionId: from the `X-Session-Id` header the UI sends (one per browser tab); groups a merchant's actions.
 * - merchantId: added later by the code that knows it (see [RequestContext.merchant]).
 *
 * Incoming ids are accepted only if they look like ids, so client input cannot forge or break log lines.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationFilter : OncePerRequestFilter() {

	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
		val requestId = request.getHeader(REQUEST_ID_HEADER)?.takeIf { it.matches(SAFE_ID) } ?: UUID.randomUUID().toString()
		val start = System.nanoTime()
		MDC.put(RequestContext.REQUEST_ID, requestId)
		request.getHeader(SESSION_ID_HEADER)?.takeIf { it.matches(SAFE_ID) }?.let { MDC.put(RequestContext.SESSION_ID, it) }
		response.setHeader(REQUEST_ID_HEADER, requestId)
		try {
			chain.doFilter(request, response)
		} finally {
			log.info("{} {} -> {} in {} ms", request.method, request.requestURI, response.status, (System.nanoTime() - start) / 1_000_000)
			MDC.clear()
		}
	}

	companion object {
		const val REQUEST_ID_HEADER = "X-Request-Id"
		const val SESSION_ID_HEADER = "X-Session-Id"
		private val SAFE_ID = Regex("^[A-Za-z0-9_-]{1,64}$")
		private val log = LoggerFactory.getLogger(CorrelationFilter::class.java)
	}
}

/** Correlation keys shared by the filter and the code that enriches the context. */
object RequestContext {
	const val REQUEST_ID = "requestId"
	const val SESSION_ID = "sessionId"
	const val MERCHANT_ID = "merchantId"

	/** Tags the rest of the current request's log lines with the merchant acting in it. */
	fun merchant(id: UUID) = MDC.put(MERCHANT_ID, id.toString())
}
