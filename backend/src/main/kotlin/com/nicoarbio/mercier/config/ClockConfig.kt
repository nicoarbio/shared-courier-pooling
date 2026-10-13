package com.nicoarbio.mercier.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

/**
 * Injectable clock, so time-dependent code can be tested with a fixed instant.
 * Delivery windows are city wall-clock times, so "now" is read in the city's time zone, not the server's.
 */
@Configuration
class ClockConfig {

	@Bean
	fun clock(@Value("\${mercier.time-zone}") zone: String): Clock = Clock.system(ZoneId.of(zone))
}
