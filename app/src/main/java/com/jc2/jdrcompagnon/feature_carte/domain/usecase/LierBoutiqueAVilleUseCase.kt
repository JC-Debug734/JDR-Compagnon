package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret

/** Lie une boutique existante (feature_boutique) à une ville de la carte : met à jour les deux côtés de la relation. */
class LierBoutiqueAVilleUseCase(
    private val boutiqueRepository: BoutiqueRepository,
    private val carteRepository: CarteRepository
) {
    suspend operator fun invoke(ville: PointInteret, boutique: Boutique) {
        boutiqueRepository.sauvegarderBoutique(boutique.copy(villeId = ville.id))
        if (boutique.id !in ville.boutiqueIds) {
            carteRepository.sauvegarderPoint(ville.copy(boutiqueIds = ville.boutiqueIds + boutique.id))
        }
    }
}
