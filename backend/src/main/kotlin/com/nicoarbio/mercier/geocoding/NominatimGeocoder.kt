package com.nicoarbio.mercier.geocoding

import com.fasterxml.jackson.annotation.JsonProperty
import com.nicoarbio.mercier.domain.GeoPoint
import org.springframework.http.HttpHeaders
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body

/**
 * Self-hosted Nominatim search, bounded to the given area.
 * See https://nominatim.org/release-docs/latest/api/Search/
 */
class NominatimGeocoder(private val restClient: RestClient) : Geocoder {

	override fun search(query: String, area: SearchArea, limit: Int): List<GeocodeCandidate> {
		val start = System.nanoTime()
		val candidates = try {
			fetch(query, area, limit)
		} catch (e: RestClientException) {
			log.warn("Nominatim search failed after {} ms: {}", (System.nanoTime() - start) / 1_000_000, e.message)
			throw e
		}
		// The query is a customer address (personal data): only at DEBUG.
		log.debug("Nominatim search '{}' returned {} candidates in {} ms", query, candidates.size, (System.nanoTime() - start) / 1_000_000)
		return candidates
	}

	private fun fetch(query: String, area: SearchArea, limit: Int): List<GeocodeCandidate> =
		restClient.get()
			.uri { uri ->
				uri.path("/search")
					.queryParam("q", query)
					.queryParam("format", "jsonv2")
					.queryParam("addressdetails", 1)
					.queryParam("limit", limit)
					.queryParam("viewbox", "${area.minLon},${area.maxLat},${area.maxLon},${area.minLat}")
					.queryParam("bounded", 1)
					.build()
			}
			.header(HttpHeaders.ACCEPT_ENCODING, "identity")
			.retrieve()
			.body<List<Place>>()
			.orEmpty()
			.map { it.toCandidate() }

	private fun Place.toCandidate(): GeocodeCandidate {
		val street = address["road"] ?: address["pedestrian"]
		val houseNumber = address["house_number"]
		val neighbourhood = address["suburb"] ?: address["neighbourhood"] ?: address["city_district"]
		val label = listOfNotNull(listOfNotNull(street, houseNumber).joinToString(" ").ifBlank { null } ?: name, neighbourhood)
			.joinToString(", ")
			.ifBlank { displayName }
		return GeocodeCandidate(label, street, houseNumber, neighbourhood, GeoPoint(lon.toDouble(), lat.toDouble()))
	}

	private data class Place(
		val lat: String,
		val lon: String,
		val name: String?,
		@JsonProperty("display_name") val displayName: String,
		val address: Map<String, String> = emptyMap(),
	)
}

private val log = LoggerFactory.getLogger(NominatimGeocoder::class.java)
