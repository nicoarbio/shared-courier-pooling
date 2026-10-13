package com.nicoarbio.mercier.config

import com.nicoarbio.mercier.geocoding.Geocoder
import com.nicoarbio.mercier.geocoding.NominatimGeocoder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(GeocodingProperties::class)
class GeocodingConfig {

	@Bean
	fun geocoder(properties: GeocodingProperties, restClient: RestClient.Builder): Geocoder =
		NominatimGeocoder(restClient.clone().baseUrl(properties.nominatimUrl).build())
}
