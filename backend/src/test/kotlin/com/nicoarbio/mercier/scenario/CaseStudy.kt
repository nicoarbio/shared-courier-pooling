package com.nicoarbio.mercier.scenario

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Merchant
import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.TimeWindow
import java.time.LocalDateTime

/**
 * Case study scenario: four shops on the same block of Yerbal (between F. García Lorca and
 * Félix Lora, Caballito, CABA) receive five orders on a Tuesday between 19:00 and 19:30.
 *
 * Destinations are street corners at the block distance stated by the case study,
 * spread in different directions around the shops.
 */
object CaseStudy {

	val bakery = Merchant("bakery", "Panadería San Martín", GeoPoint(-58.44470, -34.62046))
	val greengrocer = Merchant("greengrocer", "Verdulería La Estación", GeoPoint(-58.44452, -34.62039))
	val grocery = Merchant("grocery", "Almacén Don Pedro", GeoPoint(-58.44433, -34.62032))
	val kiosk = Merchant("kiosk", "Kiosco Sarmiento", GeoPoint(-58.44415, -34.62025))

	val merchants = listOf(bakery, greengrocer, grocery, kiosk)

	private val tuesday7pm = LocalDateTime.of(2026, 10, 13, 19, 0)
	private val window = TimeWindow(tuesday7pm, tuesday7pm.plusMinutes(30))

	val orders = listOf(
		Order("bakery-3", bakery, GeoPoint(-58.44763, -34.62170), window),           // 3 blocks W: Yerbal & Paysandú
		Order("greengrocer-2", greengrocer, GeoPoint(-58.44649, -34.62122), window), // 2 blocks W (same way): Yerbal & Espinosa
		Order("grocery-4", grocery, GeoPoint(-58.44345, -34.62402), window),         // 4 blocks S: Emilio Mitre & Hualfin
		Order("kiosk-1", kiosk, GeoPoint(-58.44333, -34.61990), window),             // 1 block E: Yerbal & Dr. N. Repetto
		Order("kiosk-3", kiosk, GeoPoint(-58.44074, -34.61873), window),             // 3 blocks E: Yerbal & Colpayo
	)

	/** Price of a dedicated courier trip for a single order, as stated by the case study. */
	const val DEDICATED_TRIP_PRICE = 2500
}
