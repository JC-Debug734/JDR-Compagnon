package com.jc2.jdrcompagnon.feature_carte.domain.model

import kotlinx.serialization.Serializable

/**
 * Carte quadrillée d'une campagne. Une campagne peut en avoir plusieurs (région, donjon...),
 * toutes supprimables. La carte historique d'une campagne a pour id le campagneId. Les lieux
 * (PointInteret) existent indépendamment des cartes : PointInteret.carteId désigne la carte où
 * le MJ l'a placé, null = lieu non placé (jamais placé, ou sa carte a été supprimée).
 */
@Serializable
data class CarteCampagne(
    val campagneId: String,
    val largeurCases: Int = 20,
    val hauteurCases: Int = 15,
    val echelleKmParCase: Int = 10,
    val imageFileName: String? = null, // image de fond choisie par le MJ (copiée en stockage interne), null = grille nue
    val id: String = campagneId,
    val nom: String = NOM_CARTE_PRINCIPALE,
) {
    val estPrincipale: Boolean get() = id == campagneId

    companion object {
        const val NOM_CARTE_PRINCIPALE = "Carte principale"
    }
}
