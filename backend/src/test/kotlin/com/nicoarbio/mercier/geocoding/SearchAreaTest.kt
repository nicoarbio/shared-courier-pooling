package com.nicoarbio.mercier.geocoding

import com.nicoarbio.mercier.domain.GeoPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchAreaTest {

	@Test
	fun `area around a point spans the requested meters in both axes at Buenos Aires latitude`() {
		val shop = GeoPoint(-58.44415, -34.62025)
		val area = SearchArea.around(shop, 2000.0)

		// 1 degree of latitude ≈ 111.3 km everywhere; 1 degree of longitude ≈ 91.6 km at -34.6°.
		val northSouthMeters = (area.maxLat - area.minLat) * 111_320
		val eastWestMeters = (area.maxLon - area.minLon) * 111_320 * Math.cos(Math.toRadians(shop.lat))
		assertTrue(abs(northSouthMeters - 4000) < 1, "north-south side: $northSouthMeters")
		assertTrue(abs(eastWestMeters - 4000) < 1, "east-west side: $eastWestMeters")
		assertTrue(area.minLon < shop.lon && shop.lon < area.maxLon && area.minLat < shop.lat && shop.lat < area.maxLat)
	}
}
