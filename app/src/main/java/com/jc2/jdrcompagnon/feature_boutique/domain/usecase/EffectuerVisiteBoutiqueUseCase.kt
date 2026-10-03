package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import kotlin.random.Random

/**
 * Simule le passage du groupe dans une boutique : le stock (ou les services) tourne, l'argent
 * disponible est réinitialisé. Les articles marqués "toujours disponibles" sont conservés tels
 * quels, le reste de l'inventaire est régénéré pour compléter jusqu'au nombre d'articles actuel.
 */
class EffectuerVisiteBoutiqueUseCase(
    private val genererInventaire: GenererInventaireUseCase,
    private val genererServices: GenererServicesUseCase,
    private val determinerBudgetAchat: DeterminerBudgetAchatUseCase
) {
    suspend operator fun invoke(
        boutique: Boutique,
        monde: String,
        random: Random = Random.Default
    ): Boutique {
        val nouvelInventaire = if (boutique.type == TypeBoutique.MARCHAND) {
            val articlesConserves = boutique.inventaire.filter { it.toujoursDisponible }
            val nbARenouveler = (boutique.inventaire.size - articlesConserves.size).coerceAtLeast(0)
            val nouveauxArticles = genererInventaire(
                standing = boutique.standing,
                monde = monde,
                nbArticles = nbARenouveler,
                exclureNoms = articlesConserves.map { it.equipement.nom }.toSet(),
                random = random
            )
            articlesConserves + nouveauxArticles
        } else {
            boutique.inventaire
        }

        val nouveauxServices = if (boutique.type == TypeBoutique.MARCHAND) {
            boutique.services
        } else {
            boutique.services.filter { it.personnalise } + genererServices(boutique.type, boutique.standing, random)
        }

        return boutique.copy(
            inventaire = nouvelInventaire,
            services = nouveauxServices,
            argentDisponibleEnPo = determinerBudgetAchat(boutique.standing, random)
        )
    }
}
