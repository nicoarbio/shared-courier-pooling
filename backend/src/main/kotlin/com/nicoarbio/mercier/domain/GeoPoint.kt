package com.nicoarbio.mercier.domain

/** WGS84 coordinate. Longitude first, matching OSRM's `lon,lat` convention. */
data class GeoPoint(val lon: Double, val lat: Double) {
	override fun toString() = "$lon,$lat"
}
