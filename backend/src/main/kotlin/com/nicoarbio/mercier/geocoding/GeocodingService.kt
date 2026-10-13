package com.nicoarbio.mercier.geocoding

import com.nicoarbio.mercier.config.GeocodingProperties
import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.persistence.MerchantRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Searches an address within a merchant's delivery area.
 *
 * Bounding the search around the merchant avoids same-name streets elsewhere in the city (e.g. "Espinosa 400"
 * would otherwise match "Arzobispo Espinosa" in another neighbourhood). Anything outside the area could not join
 * a round with that merchant anyway. The UI still shows the point on a map so the merchant confirms it.
 */
@Service
class GeocodingService(
	private val geocoder: Geocoder,
	private val merchants: MerchantRepository,
	private val properties: GeocodingProperties,
) {

	@Transactional(readOnly = true)
	fun search(query: String, merchantId: UUID): List<GeocodeCandidate> {
		val merchant = merchants.findById(merchantId).orElseThrow { NoSuchElementException("Merchant $merchantId not found") }
		val area = SearchArea.around(GeoPoint(merchant.lon, merchant.lat), properties.searchRadiusMeters)
		val candidates = geocoder.search(query.trim(), area, properties.maxResults)
		log.info("Address search returned {} candidates ({} with house number)", candidates.size, candidates.count { it.houseNumber != null })
		return candidates
	}
}

private val log = LoggerFactory.getLogger(GeocodingService::class.java)
