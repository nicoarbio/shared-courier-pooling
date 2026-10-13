package com.nicoarbio.mercier.geocoding

import com.nicoarbio.mercier.domain.GeoPoint
import kotlin.math.cos

/** One match for a searched address. [label] is a short human-readable form, e.g. "Yerbal 1550, Caballito". */
data class GeocodeCandidate(
	val label: String,
	val street: String?,
	val houseNumber: String?,
	val neighbourhood: String?,
	val location: GeoPoint,
)

/** Axis-aligned search area: only results inside it are returned. */
data class SearchArea(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double) {
	companion object {
		private const val METERS_PER_DEGREE_LAT = 111_320.0

		/** Square of roughly [halfSideMeters] in every direction around [center]. */
		fun around(center: GeoPoint, halfSideMeters: Double): SearchArea {
			val dLat = halfSideMeters / METERS_PER_DEGREE_LAT
			val dLon = halfSideMeters / (METERS_PER_DEGREE_LAT * cos(Math.toRadians(center.lat)))
			return SearchArea(center.lon - dLon, center.lat - dLat, center.lon + dLon, center.lat + dLat)
		}
	}
}

/** Address → coordinates. Implemented by Nominatim; a port so the provider can change without touching the API. */
fun interface Geocoder {
	fun search(query: String, area: SearchArea, limit: Int): List<GeocodeCandidate>
}
