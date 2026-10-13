package com.nicoarbio.mercier.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.time.Duration

@ConfigurationProperties("mercier.pooling")
data class PoolingProperties(
	/** Max road distance (one-way streets ignored) from every merchant of a round to every destination. */
	val radiusMeters: Double,
	/** Max difference between the start times of the delivery windows in a round. */
	val windowTolerance: Duration,
	/** Price paid to the courier per kilometer driven. */
	val pricePerKm: BigDecimal,
	/** Time spent at each pickup and delivery; used for estimated arrival times. */
	val stopServiceTime: Duration,
)

@ConfigurationProperties("mercier.geocoding")
data class GeocodingProperties(
	/** Self-hosted Nominatim instance. */
	val nominatimUrl: String,
	/** Half side of the square around the merchant where addresses are searched. */
	val searchRadiusMeters: Double,
	/** Max candidates returned to the UI. */
	val maxResults: Int,
)

@ConfigurationProperties("mercier.osrm")
data class OsrmProperties(
	/** OSRM instance that respects one-way streets: routes and costs. */
	val routingUrl: String,
	/** OSRM instance with one-way handling disabled: grouping by proximity. */
	val proximityUrl: String,
)
