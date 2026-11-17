package com.nicoarbio.mercier.demo

import com.nicoarbio.mercier.domain.GeoPoint
import java.util.UUID

/**
 * The case study of the paper, as data: four shops on the same block of Yerbal (between F. García Lorca and
 * Félix Lora, Caballito, CABA; seeded by db/seed) and five orders at street corners at the block distance the case
 * study states, spread in different directions around the shops. Used to prepare the demo and by the scenario test.
 */
object CaseStudyOrders {

	val BAKERY: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
	val GREENGROCER: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
	val GROCERY: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
	val KIOSK: UUID = UUID.fromString("00000000-0000-0000-0000-000000000004")

	data class CaseStudyOrder(val key: String, val merchantId: UUID, val destination: GeoPoint, val address: String)

	val orders = listOf(
		CaseStudyOrder("bakery-3", BAKERY, GeoPoint(-58.44763, -34.62170), "Yerbal y Paysandú"),           // 3 blocks W
		CaseStudyOrder("greengrocer-2", GREENGROCER, GeoPoint(-58.44649, -34.62122), "Yerbal y Espinosa"), // 2 blocks W (same way)
		CaseStudyOrder("grocery-4", GROCERY, GeoPoint(-58.44345, -34.62402), "Emilio Mitre y Hualfin"),    // 4 blocks S
		CaseStudyOrder("kiosk-1", KIOSK, GeoPoint(-58.44333, -34.61990), "Yerbal y Dr. N. Repetto"),       // 1 block E
		CaseStudyOrder("kiosk-3", KIOSK, GeoPoint(-58.44074, -34.61873), "Yerbal y Colpayo"),              // 3 blocks E
	)
}
