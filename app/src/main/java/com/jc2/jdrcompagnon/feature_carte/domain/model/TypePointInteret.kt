package com.jc2.jdrcompagnon.feature_carte.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TypePointInteret(val label: String) {
    VILLE(label = "Ville"),
    DONJON(label = "Donjon"),
    CAMPEMENT(label = "Campement"),
    RUINE(label = "Ruine"),
    AUTRE(label = "Autre")
}
