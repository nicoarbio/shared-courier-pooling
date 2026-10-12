package com.nicoarbio.mercier.engine

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Merchant
import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.TimeWindow
import com.nicoarbio.mercier.routing.DistanceMatrix
import java.time.LocalDateTime

/** Hand-written distance matrices, so each engine rule can be checked without OSRM. */
object Fixtures {

	val T0: LocalDateTime = LocalDateTime.of(2026, 10, 13, 19, 0)

	/** Distinct dummy coordinates; only identity matters, distances come from the matrix. */
	fun point(n: Int) = GeoPoint(n.toDouble(), 0.0)

	fun merchant(id: String, location: GeoPoint) = Merchant(id, id, location)

	fun order(id: String, merchant: Merchant, destination: GeoPoint, startMinute: Long = 0) =
		Order(id, merchant, destination, TimeWindow(T0.plusMinutes(startMinute), T0.plusMinutes(startMinute + 30)))

	/** Matrix where unspecified pairs default to [default] and every self-distance is 0. Durations = distance / 10. */
	fun matrix(points: List<GeoPoint>, default: Double = 10_000.0, distances: Map<Pair<GeoPoint, GeoPoint>, Double>): DistanceMatrix {
		val d = points.map { a -> points.map { b -> if (a == b) 0.0 else distances[a to b] ?: default } }
		return DistanceMatrix(points, d, d.map { row -> row.map { it / 10 } })
	}

	/** Same distance in both directions for each listed pair. */
	fun symmetric(vararg pairs: Pair<Pair<GeoPoint, GeoPoint>, Double>): Map<Pair<GeoPoint, GeoPoint>, Double> =
		pairs.flatMap { (p, d) -> listOf(p to d, (p.second to p.first) to d) }.toMap()
}
