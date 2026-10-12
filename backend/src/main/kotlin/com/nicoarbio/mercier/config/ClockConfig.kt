package com.nicoarbio.mercier.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** Injectable clock, so time-dependent code can be tested with a fixed instant. */
@Configuration
class ClockConfig {

	@Bean
	fun clock(): Clock = Clock.systemDefaultZone()
}
