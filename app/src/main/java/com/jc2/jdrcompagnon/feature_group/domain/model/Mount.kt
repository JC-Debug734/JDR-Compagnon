package com.jc2.jdrcompagnon.feature_group.domain.model

import kotlinx.serialization.Serializable

/**
 * Monture du groupe — purement descriptive (aucun impact mécanique/combat), sur le même
 * principe que les employés de feature_boutique. `riderCharacterId` référence optionnellement
 * un membre du groupe (Character.id) qui la monte.
 */
@Serializable
data class Mount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val species: String = "",
    val riderCharacterId: String? = null
)
