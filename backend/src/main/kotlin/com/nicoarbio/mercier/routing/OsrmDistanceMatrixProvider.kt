package com.nicoarbio.mercier.routing

import com.nicoarbio.mercier.domain.GeoPoint
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Distance matrix from an OSRM `table` service: a single HTTP call for all point pairs.
 * See https://project-osrm.org/docs/v5.24.0/api/#table-service
 */
class OsrmDistanceMatrixProvider(private val restClient: RestClient) : DistanceMatrixProvider {

	override fun matrix(points: List<GeoPoint>): DistanceMatrix {
		val unique = points.distinct()
		val coordinates = unique.joinToString(";")
		val response = restClient.get()
			.uri("/table/v1/driving/{coordinates}?annotations=distance,duration", coordinates)
			// OSRM's compressed responses fail to inflate on the JDK client; payloads are small anyway.
			.header(HttpHeaders.ACCEPT_ENCODING, "identity")
			.retrieve()
			.body<OsrmTableResponse>()
			?: error("Empty response from OSRM")
		check(response.code == "Ok") { "OSRM table failed: ${response.code}" }
		return DistanceMatrix(unique, response.distances.requireRoutes(), response.durations.requireRoutes())
	}

	// OSRM returns null for pairs with no route between them.
	private fun List<List<Double?>>.requireRoutes(): List<List<Double>> =
		map { row -> row.map { it ?: error("OSRM found no route between some points") } }

	private data class OsrmTableResponse(
		val code: String,
		val distances: List<List<Double?>>,
		val durations: List<List<Double?>>,
	)
}
