package com.nicoarbio.mercier.scenario

import com.nicoarbio.mercier.demo.CaseStudyOrders
import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Merchant
import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.TimeWindow
import java.time.LocalDateTime

/**
 * Case study scenario: four shops on the same block of Yerbal (between F. García Lorca and
 * Félix Lora, Caballito, CABA) receive five orders (CaseStudyOrders) on a Tuesday between 19:00 and 19:30.
 */
object CaseStudy {

	val bakery = Merchant("bakery", "Panadería San Martín", GeoPoint(-58.44470, -34.62046))
	val greengrocer = Merchant("greengrocer", "Verdulería La Estación", GeoPoint(-58.44452, -34.62039))
	val grocery = Merchant("grocery", "Almacén Don Pedro", GeoPoint(-58.44433, -34.62032))
	val kiosk = Merchant("kiosk", "Kiosco Sarmiento", GeoPoint(-58.44415, -34.62025))

	val merchants = listOf(bakery, greengrocer, grocery, kiosk)

	private val tuesday7pm = LocalDateTime.of(2026, 10, 13, 19, 0)
	private val window = TimeWindow(tuesday7pm, tuesday7pm.plusMinutes(30))

	// Same orders as the demo (CaseStudyOrders), with readable merchant ids for the report.
	private val merchantsById = mapOf(
		CaseStudyOrders.BAKERY to bakery,
		CaseStudyOrders.GREENGROCER to greengrocer,
		CaseStudyOrders.GROCERY to grocery,
		CaseStudyOrders.KIOSK to kiosk,
	)

	val orders = CaseStudyOrders.orders.map { Order(it.key, merchantsById.getValue(it.merchantId), it.destination, window) }

	/** Price of a dedicated courier trip for a single order, as stated by the case study. */
	const val DEDICATED_TRIP_PRICE = 2500
}
