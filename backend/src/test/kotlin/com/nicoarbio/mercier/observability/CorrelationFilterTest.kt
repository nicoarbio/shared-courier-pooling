package com.nicoarbio.mercier.observability

import jakarta.servlet.FilterChain
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CorrelationFilterTest {

	private val filter = CorrelationFilter()

	/** Runs the filter and returns the MDC as the downstream code saw it. */
	private fun run(request: MockHttpServletRequest, response: MockHttpServletResponse = MockHttpServletResponse()): Map<String, String> {
		var seen: Map<String, String> = emptyMap()
		filter.doFilter(request, response, FilterChain { _, _ ->
			RequestContext.merchant(UUID.fromString("00000000-0000-0000-0000-000000000004"))
			seen = MDC.getCopyOfContextMap()
		})
		return seen
	}

	@Test
	fun `incoming request and session ids are used, echoed back, and cleared afterwards`() {
		val request = MockHttpServletRequest("GET", "/api/rounds").apply {
			addHeader("X-Request-Id", "req-123")
			addHeader("X-Session-Id", "tab_42")
		}
		val response = MockHttpServletResponse()

		val seen = run(request, response)

		assertEquals("req-123", seen[RequestContext.REQUEST_ID])
		assertEquals("tab_42", seen[RequestContext.SESSION_ID])
		assertEquals("00000000-0000-0000-0000-000000000004", seen[RequestContext.MERCHANT_ID])
		assertEquals("req-123", response.getHeader("X-Request-Id"))
		assertNull(MDC.getCopyOfContextMap()?.takeIf { it.isNotEmpty() }, "context must not leak to the next request on this thread")
	}

	@Test
	fun `a request id is generated when missing or unsafe`() {
		val generated = run(MockHttpServletRequest("GET", "/api/rounds"))[RequestContext.REQUEST_ID]!!
		assertTrue(generated.matches(Regex("[0-9a-f-]{36}")), generated)

		val unsafe = MockHttpServletRequest("GET", "/api/rounds").apply { addHeader("X-Request-Id", "evil\nINFO fake log line") }
		val replaced = run(unsafe)[RequestContext.REQUEST_ID]!!
		assertNotEquals("evil\nINFO fake log line", replaced)
		assertTrue(replaced.matches(Regex("[0-9a-f-]{36}")), replaced)
	}
}
