package com.nicoarbio.mercier

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

/** Throwaway PostgreSQL in Docker for tests; Spring wires the datasource to it automatically. Requires Docker. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun postgres() = PostgreSQLContainer("postgres:17")
}
