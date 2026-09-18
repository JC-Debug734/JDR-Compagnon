package com.jc2.jdrcompagnon.feature_group.domain.model

import kotlinx.serialization.Serializable

/** Réputation du groupe auprès d'une faction — propre au groupe, pas à un personnage. */
@Serializable
data class Reputation(
    val factionId: String = java.util.UUID.randomUUID().toString(),
    val factionName: String,
    val score: Int = 0 // borné à -100..100 par l'UI
)

/** Libellé qualitatif dérivé du score — logique pure, réutilisable partout où score est affiché. */
object ReputationScale {
    fun labelFor(score: Int): String = when {
        score <= -51 -> "Hostile"
        score <= -11 -> "Méfiant"
        score <= 10 -> "Neutre"
        score <= 50 -> "Amical"
        else -> "Allié"
    }
}
