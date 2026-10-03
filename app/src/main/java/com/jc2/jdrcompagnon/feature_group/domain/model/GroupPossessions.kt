package com.jc2.jdrcompagnon.feature_group.domain.model

import kotlinx.serialization.Serializable

/** Objet de l'inventaire commun du groupe (butin de quête, équipement non réparti...). */
@Serializable
data class GroupItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    // Lieu où l'objet est entreposé, vide = porté par le groupe.
    val location: String = "",
)

/** Bien du groupe : maison, taverne, terrain, part d'une affaire... */
@Serializable
data class GroupAsset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val location: String = "",
)
