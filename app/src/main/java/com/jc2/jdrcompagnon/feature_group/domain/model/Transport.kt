package com.jc2.jdrcompagnon.feature_group.domain.model

import kotlinx.serialization.Serializable

/** Moyen de transport du groupe — purement descriptif (chariot, barque, calèche...). */
@Serializable
data class Transport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String = "",
    val capacity: Int = 0
)
