package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference

/**
 * Port (interface) permettant au Domain d'accéder aux équipements du SRD
 * sans dépendre du module SRD directement (Domain ne dépend d'aucun framework externe).
 * Implémenté dans data/ par un adaptateur qui enveloppe SrdRepository.
 */
interface EquipementSourcePort {
    suspend fun getEquipementsDisponibles(monde: String): List<EquipementReference>
}