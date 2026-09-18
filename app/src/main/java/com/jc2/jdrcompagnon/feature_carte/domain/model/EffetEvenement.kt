package com.jc2.jdrcompagnon.feature_carte.domain.model

sealed interface EffetEvenement {
    data class GainReputation(val factionNom: String, val delta: Int) : EffetEvenement
    data class Information(val texte: String) : EffetEvenement
}
