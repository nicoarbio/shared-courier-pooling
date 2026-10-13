package com.nicoarbio.mercier.api

import com.nicoarbio.mercier.domain.GeoPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds a Google Maps directions link with the round's stops preloaded, in order.
 * Uses the public "Maps URLs" format: no API key, no cost. See https://developers.google.com/maps/documentation/urls/get-started
 */
object GoogleMapsLink {

	/** Maps URLs accept at most this many intermediate waypoints. */
	const val MAX_WAYPOINTS = 9

	/** [route] includes origin and destination. Returns null when it has more intermediate stops than Maps accepts. */
	fun directions(route: List<GeoPoint>): String? {
		require(route.size >= 2) { "A route needs at least an origin and a destination" }
		val waypoints = route.subList(1, route.size - 1)
		if (waypoints.size > MAX_WAYPOINTS) return null
		val params = buildList {
			add("api=1")
			add("travelmode=driving")
			add("origin=" + route.first().latLng())
			add("destination=" + route.last().latLng())
			if (waypoints.isNotEmpty()) add("waypoints=" + encode(waypoints.joinToString("|") { it.latLng() }))
		}
		return "https://www.google.com/maps/dir/?" + params.joinToString("&")
	}

	// Google Maps expects lat,lng (OSRM and GeoPoint use lon,lat).
	private fun GeoPoint.latLng() = "$lat,$lon"

	private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
