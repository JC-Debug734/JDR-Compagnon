package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import java.util.UUID
import kotlinx.coroutines.flow.first

/**
 * Ajoute une ville existante (outil VILLES) à une campagne :
 * - ville de la bibliothèque (sans campagne) : elle est rattachée à la campagne, avec ses boutiques ;
 * - ville d'une autre campagne : elle y reste, la campagne en reçoit une copie (lieux à visiter et
 *   boutiques compris), non placée sur ses cartes.
 * Les scénarios attachés et la position sur la carte, propres à une campagne, ne sont pas repris.
 * [copierImages] duplique les fichiers d'images (illustration, emblème) vers la copie et retourne
 * l'emblème de la copie ; [dupliquerImageBoutique] fait de même pour l'enseigne d'une boutique
 * copiée. Retourne l'id de la ville de la campagne.
 */
class AjouterVilleACampagneUseCase(
    private val carteRepository: CarteRepository,
    private val boutiqueRepository: BoutiqueRepository,
) {
    suspend operator fun invoke(
        ville: PointInteret,
        campagneId: String,
        copierImages: (source: PointInteret, nouvelId: String) -> String?,
        dupliquerImageBoutique: (image: String?) -> String?,
    ): String {
        if (ville.campagneId == campagneId) return ville.id
        val boutiques = boutiqueRepository.observerToutesLesBoutiques().first()
            .filter { it.villeId == ville.id || it.id in ville.boutiqueIds }

        if (ville.campagneId == PointInteret.SANS_CAMPAGNE) {
            carteRepository.sauvegarderPoint(
                ville.copy(campagneId = campagneId, carteId = null, fx = null, fy = null, scenarioIds = emptyList())
            )
            boutiques.forEach { boutiqueRepository.sauvegarderBoutique(it.copy(campagneId = campagneId)) }
            return ville.id
        }

        val nouvelId = UUID.randomUUID().toString()
        val copiesBoutiques = boutiques.map {
            it.copy(id = UUID.randomUUID().toString(), villeId = nouvelId, campagneId = campagneId, image = dupliquerImageBoutique(it.image))
        }
        carteRepository.sauvegarderPoint(
            ville.copy(
                id = nouvelId,
                campagneId = campagneId,
                carteId = null,
                fx = null,
                fy = null,
                visibleJoueurs = false,
                scenarioIds = emptyList(),
                boutiqueIds = copiesBoutiques.map { it.id },
                embleme = copierImages(ville, nouvelId),
            )
        )
        copiesBoutiques.forEach { boutiqueRepository.sauvegarderBoutique(it) }
        carteRepository.observerLieuxNotables(ville.id).first().forEach { lieu ->
            carteRepository.sauvegarderLieuNotable(
                LieuNotable(UUID.randomUUID().toString(), nouvelId, lieu.nom, lieu.description)
            )
        }
        return nouvelId
    }
}
