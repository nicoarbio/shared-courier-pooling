package com.nicoarbio.mercier.persistence

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MerchantRepository : JpaRepository<MerchantEntity, UUID>

interface OrderRepository : JpaRepository<OrderEntity, UUID> {
	@EntityGraph(attributePaths = ["merchant", "round"])
	fun findAllByStatusIn(statuses: Collection<OrderStatus>): List<OrderEntity>
}

interface RoundRepository : JpaRepository<RoundEntity, UUID>
