package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import kotlin.random.Random

/**
 * Tirage pondéré d'un standing (option MJ "Aléatoire" dans CreerBoutiqueDialog).
 * Pondération définie sur StandingBoutique.poidsTirageAleatoire.
 */
class GenererStandingAleatoireUseCase {

    operator fun invoke(random: Random = Random.Default): StandingBoutique {
        val poids = StandingBoutique.poidsTirageAleatoire
        val total = poids.values.sum()
        var tirage = random.nextInt(total)

        for ((standing, poidsStanding) in poids) {
            if (tirage < poidsStanding) return standing
            tirage -= poidsStanding
        }
        // Filet de sécurité si les poids ne sommaient pas exactement à `total`
        return poids.keys.first()
    }
}