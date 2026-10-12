package com.nicoarbio.mercier.routing

import com.nicoarbio.mercier.domain.GeoPoint

/**
 * Pairwise road-network distances (meters) and durations (seconds) between [points].
 * Entry `[i][j]` is the trip from `points[i]` to `points[j]`; it may differ from `[j][i]`.
 */
class DistanceMatrix(
	val points: List<GeoPoint>,
	private val distances: List<List<Double>>,
	private val durations: List<List<Double>>,
) {
	private val index: Map<GeoPoint, Int> = points.withIndex().associate { (i, p) -> p to i }

	init {
		require(distances.size == points.size && distances.all { it.size == points.size }) { "Distance matrix is not ${points.size}x${points.size}" }
		require(durations.size == points.size && durations.all { it.size == points.size }) { "Duration matrix is not ${points.size}x${points.size}" }
	}

	fun distance(from: GeoPoint, to: GeoPoint): Double = distances[indexOf(from)][indexOf(to)]

	fun duration(from: GeoPoint, to: GeoPoint): Double = durations[indexOf(from)][indexOf(to)]

	private fun indexOf(point: GeoPoint) = index[point] ?: throw IllegalArgumentException("Point $point is not in the matrix")
}

fun interface DistanceMatrixProvider {
	fun matrix(points: List<GeoPoint>): DistanceMatrix
}
