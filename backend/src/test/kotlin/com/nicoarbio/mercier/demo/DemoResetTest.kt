package com.nicoarbio.mercier.demo

import com.nicoarbio.mercier.TestcontainersConfiguration
import com.nicoarbio.mercier.persistence.MerchantEntity
import com.nicoarbio.mercier.persistence.MerchantRepository
import com.nicoarbio.mercier.persistence.OrderRepository
import com.nicoarbio.mercier.persistence.RoundRepository
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoResetTest {

	/** Default configuration (any regular deployment): the reset endpoints do not exist. */
	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@Import(TestcontainersConfiguration::class)
	inner class Disabled @Autowired constructor(private val mvc: MockMvc) {

		@Test
		fun `reset is not available unless enabled`() {
			mvc.get("/api/demo").andExpect { status { isNotFound() } }
			mvc.post("/api/demo/reset").andExpect { status { isNotFound() } }
		}
	}

	@Nested
	@SpringBootTest(properties = ["mercier.demo.reset-enabled=true"])
	@AutoConfigureMockMvc
	@Import(TestcontainersConfiguration::class)
	inner class Enabled @Autowired constructor(
		private val mvc: MockMvc,
		private val merchants: MerchantRepository,
		private val orders: OrderRepository,
		private val rounds: RoundRepository,
		private val jdbc: JdbcTemplate,
	) {

		@Test
		fun `reset deletes orders and rounds but keeps the merchants`() {
			jdbc.execute("truncate round_share, round_stop, delivery_order, round, merchant cascade")
			val shop = merchants.save(MerchantEntity(UUID.randomUUID(), "shop", 0.0, 0.0))
			val round = UUID.randomUUID()
			jdbc.update(
				"insert into round (id, planned_at, distance_m, duration_s, cost, price_per_km, radius_m, departure_at) " +
					"values (?, now(), 100, 10, 100, 1000, 500, now())",
				round,
			)
			jdbc.update("insert into round_stop (round_id, seq, kind, merchant_id, lon, lat) values (?, 0, 'PICKUP', ?, 0, 0)", round, shop.id)
			jdbc.update("insert into round_share (round_id, merchant_id, amount) values (?, ?, 100)", round, shop.id)
			jdbc.update(
				"insert into delivery_order (id, merchant_id, dest_lon, dest_lat, window_start, window_end, status, round_id, created_at) " +
					"values (?, ?, 0, 0.001, now(), now(), 'PLANNED', ?, now())",
				UUID.randomUUID(), shop.id, round,
			)

			mvc.get("/api/demo").andExpect { status { isOk() } }
			mvc.post("/api/demo/reset").andExpect {
				status { isOk() }
				jsonPath("$.deletedOrders") { value(1) }
				jsonPath("$.deletedRounds") { value(1) }
			}

			assertEquals(0, orders.count())
			assertEquals(0, rounds.count())
			assertEquals(0, jdbc.queryForObject("select count(*) from round_stop", Int::class.java))
			assertEquals(listOf("shop"), merchants.findAll().map { it.name })
		}
	}
}
