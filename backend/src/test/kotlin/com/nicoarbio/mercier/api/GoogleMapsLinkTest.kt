package com.nicoarbio.mercier.api

import com.nicoarbio.mercier.domain.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoogleMapsLinkTest {

	@Test
	fun `stops are passed in order, as lat,lng, with intermediate ones as waypoints`() {
		val url = GoogleMapsLink.directions(listOf(GeoPoint(-58.1, -34.1), GeoPoint(-58.2, -34.2), GeoPoint(-58.3, -34.3), GeoPoint(-58.1, -34.1)))

		assertEquals(
			"https://www.google.com/maps/dir/?api=1&travelmode=driving&origin=-34.1,-58.1&destination=-34.1,-58.1" +
				"&waypoints=-34.2%2C-58.2%7C-34.3%2C-58.3",
			url,
		)
	}

	@Test
	fun `no link when the route exceeds the waypoint limit`() {
		val route = (0..GoogleMapsLink.MAX_WAYPOINTS + 2).map { GeoPoint(it.toDouble(), 0.0) } // 10 intermediate stops

		assertNull(GoogleMapsLink.directions(route))
	}
}
