package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique

/**
 * Modifie une boutique existante (nom, marchand, standing, type).
 * L'inventaire, les services et les employés ne sont régénérés QUE si le standing ou le type
 * changent — renommer une boutique ne doit jamais faire disparaître son stock ou son personnel
 * actuels.
 */
class ModifierBoutiqueUseCase(
    private val genererStandingAleatoire: GenererStandingAleatoireUseCase,
    private val genererInventaire: GenererInventaireUseCase,
    private val genererEmployes: GenererEmployesUseCase,
    private val genererServices: GenererServicesUseCase
) {
    suspend operator fun invoke(
        boutiqueExistante: Boutique,
        nom: String,
        nomMarchand: String,
        standingChoisi: StandingBoutique?,
        type: TypeBoutique,
        monde: String
    ): Boutique {
        val nouveauStanding = standingChoisi ?: genererStandingAleatoire()
        val standingOuTypeAChange = nouveauStanding != boutiqueExistante.standing || type != boutiqueExistante.type

        return boutiqueExistante.copy(
            nom = nom,
            marchand = boutiqueExistante.marchand.copy(nom = nomMarchand),
            standing = nouveauStanding,
            type = type,
            inventaire = when {
                !standingOuTypeAChange -> boutiqueExistante.inventaire
                type == TypeBoutique.MARCHAND -> genererInventaire(nouveauStanding, monde)
                else -> emptyList()
            },
            services = when {
                !standingOuTypeAChange -> boutiqueExistante.services
                type == TypeBoutique.MARCHAND -> boutiqueExistante.services.filter { it.personnalise }
                else -> boutiqueExistante.services.filter { it.personnalise } + genererServices(type, nouveauStanding)
            },
            employes = if (standingOuTypeAChange) genererEmployes(nouveauStanding) else boutiqueExistante.employes
        )
    }
}
