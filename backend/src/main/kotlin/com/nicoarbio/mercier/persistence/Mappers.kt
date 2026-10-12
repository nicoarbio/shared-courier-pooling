package com.nicoarbio.mercier.persistence

import com.nicoarbio.mercier.domain.GeoPoint
import com.nicoarbio.mercier.domain.Merchant
import com.nicoarbio.mercier.domain.Order
import com.nicoarbio.mercier.domain.TimeWindow

/* Entity -> domain. Domain ids are the entities' UUIDs as strings. */

fun MerchantEntity.toDomain() = Merchant(id.toString(), name, GeoPoint(lon, lat))

fun OrderEntity.toDomain() = Order(
	id = id.toString(),
	merchant = merchant.toDomain(),
	destination = GeoPoint(destLon, destLat),
	window = TimeWindow(windowStart, windowEnd),
)
