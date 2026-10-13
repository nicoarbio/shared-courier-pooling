package com.nicoarbio.mercier.config

import com.nicoarbio.mercier.engine.CostAllocator
import com.nicoarbio.mercier.engine.CostModel
import com.nicoarbio.mercier.engine.NearestNeighborRouter
import com.nicoarbio.mercier.engine.OrderGrouper
import com.nicoarbio.mercier.engine.RoundPlanner
import com.nicoarbio.mercier.routing.OsrmDistanceMatrixProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

/** Wires the engine. Engine classes are plain Kotlin (no Spring) so they can be unit-tested in isolation. */
@Configuration
@EnableConfigurationProperties(PoolingProperties::class, OsrmProperties::class)
class EngineConfig {

	@Bean
	fun roundPlanner(pooling: PoolingProperties, osrm: OsrmProperties, restClient: RestClient.Builder) = RoundPlanner(
		grouper = OrderGrouper(pooling.radiusMeters, pooling.windowTolerance),
		router = NearestNeighborRouter(),
		costModel = CostModel(pooling.pricePerKm),
		allocator = CostAllocator(),
		proximity = OsrmDistanceMatrixProvider("proximity", restClient.clone().baseUrl(osrm.proximityUrl).build()),
		routing = OsrmDistanceMatrixProvider("routing", restClient.clone().baseUrl(osrm.routingUrl).build()),
		serviceTime = pooling.stopServiceTime,
	)
}
