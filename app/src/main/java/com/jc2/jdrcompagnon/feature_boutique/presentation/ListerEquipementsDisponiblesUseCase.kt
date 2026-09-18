package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference

/** Expose le catalogue SRD au ViewModel sans lui faire connaître EquipementSourcePort/le port directement. */
class ListerEquipementsDisponiblesUseCase(
    private val equipementSource: EquipementSourcePort
) {
    suspend operator fun invoke(monde: String): List<EquipementReference> =
        equipementSource.getEquipementsDisponibles(monde)
}
