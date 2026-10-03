package com.jc2.jdrcompagnon.feature_historique.data

import kotlinx.serialization.Serializable

/**
 * Version sérialisable (JSON) d'une entrée d'historique, sans l'id Room autogénéré — utilisée
 * uniquement pour l'export/import d'un personnage (voir GameState.CharacterExport), jamais
 * pour la persistance locale (qui reste HistoriqueEntreeEntity/Room).
 */
@Serializable
data class HistoriqueEntreeExport(
    val timestamp: Long,
    val type: String,
    val description: String
)
