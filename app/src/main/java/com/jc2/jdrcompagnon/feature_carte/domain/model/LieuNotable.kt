package com.jc2.jdrcompagnon.feature_carte.domain.model

/** Lieu notable au sein d'une ville (auberge, forge, place du marché...), sans coordonnées propres. */
data class LieuNotable(
    val id: String,
    val villeId: String,
    val nom: String,
    val description: String = ""
)
