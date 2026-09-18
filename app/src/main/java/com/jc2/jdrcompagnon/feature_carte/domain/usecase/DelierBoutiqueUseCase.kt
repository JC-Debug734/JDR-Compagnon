package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret

class DelierBoutiqueUseCase(
    private val boutiqueRepository: BoutiqueRepository,
    private val carteRepository: CarteRepository
) {
    suspend operator fun invoke(ville: PointInteret, boutique: Boutique) {
        boutiqueRepository.sauvegarderBoutique(boutique.copy(villeId = null))
        carteRepository.sauvegarderPoint(ville.copy(boutiqueIds = ville.boutiqueIds - boutique.id))
    }
}
