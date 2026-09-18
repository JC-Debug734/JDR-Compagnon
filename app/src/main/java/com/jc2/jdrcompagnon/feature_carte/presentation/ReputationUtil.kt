package com.jc2.jdrcompagnon.feature_carte.presentation

import com.jc2.jdrcompagnon.feature_group.domain.model.Reputation
import com.jc2.jdrcompagnon.ui.GameState

/** Applique un gain/perte de réputation à un groupe (même logique que GroupCard.adjustReputation), partagée entre EvenementsViewModel et VilleDetailViewModel. */
internal fun appliquerReputationAuGroupe(groupeId: String, factionNom: String, delta: Int) {
    val groupe = GameState.mjGroups.value.firstOrNull { it.id == groupeId } ?: return
    val reputationExistante = groupe.reputations.firstOrNull { it.factionName == factionNom }
    val reputations = if (reputationExistante != null) {
        groupe.reputations.map {
            if (it.factionId == reputationExistante.factionId) it.copy(score = (it.score + delta).coerceIn(-100, 100)) else it
        }
    } else {
        groupe.reputations + Reputation(factionName = factionNom, score = delta.coerceIn(-100, 100))
    }
    GameState.updateMjGroup(groupe.copy(reputations = reputations))
}
