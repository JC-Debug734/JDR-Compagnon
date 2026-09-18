package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import kotlin.random.Random

/**
 * Détermine la somme d'argent (en po) dont dispose la boutique pour acheter du matériel,
 * selon son standing. Sert de valeur suggérée pour ApprovisionnerBoutiqueUseCase — le MJ
 * peut toujours l'ajuster à la main dans le formulaire.
 */
class DeterminerBudgetAchatUseCase {

    private companion object {
        val BUDGET_PAR_STANDING: Map<StandingBoutique, IntRange> = mapOf(
            StandingBoutique.MODESTE to 20..50,
            StandingBoutique.CORRECT to 50..150,
            StandingBoutique.PROSPERE to 150..500,
            StandingBoutique.LUXUEUX to 500..2000
        )
    }

    operator fun invoke(standing: StandingBoutique, random: Random = Random.Default): Int {
        val plage = BUDGET_PAR_STANDING.getValue(standing)
        return random.nextInt(plage.first, plage.last + 1)
    }
}
