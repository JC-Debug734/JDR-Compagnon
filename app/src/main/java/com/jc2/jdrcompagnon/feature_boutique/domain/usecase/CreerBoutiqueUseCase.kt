package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Marchand
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import java.util.UUID

/**
 * Orchestre la création complète d'une boutique.
 * Si `standingChoisi` est null, le MJ a choisi l'option "Aléatoire" -> tirage pondéré.
 * MARCHAND génère un inventaire d'objets SRD ; les autres types génèrent un catalogue de
 * services (voir GenererServicesUseCase).
 */
class CreerBoutiqueUseCase(
    private val genererStandingAleatoire: GenererStandingAleatoireUseCase,
    private val genererInventaire: GenererInventaireUseCase,
    private val genererEmployes: GenererEmployesUseCase,
    private val genererServices: GenererServicesUseCase,
    private val determinerBudgetAchat: DeterminerBudgetAchatUseCase,
    private val genererTraitCaractere: GenererTraitCaractereUseCase
) {
    suspend operator fun invoke(
        nom: String,
        nomMarchand: String,
        standingChoisi: StandingBoutique?,
        type: TypeBoutique,
        monde: String
    ): Boutique {
        val standing = standingChoisi ?: genererStandingAleatoire()

        return Boutique(
            id = UUID.randomUUID().toString(),
            nom = nom,
            standing = standing,
            marchand = Marchand(nom = nomMarchand, trait = genererTraitCaractere()),
            employes = genererEmployes(standing),
            inventaire = if (type == TypeBoutique.MARCHAND) genererInventaire(standing, monde) else emptyList(),
            type = type,
            services = if (type == TypeBoutique.MARCHAND) emptyList() else genererServices(type, standing),
            argentDisponibleEnPo = determinerBudgetAchat(standing)
        )
    }
}