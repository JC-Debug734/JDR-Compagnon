package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire
import kotlin.random.Random

class TirerEvenementAleatoireUseCase(
    private val genererEvenementsGeneriques: GenererEvenementsGeneriquesUseCase
) {
    operator fun invoke(
        evenementsCustom: List<EvenementAleatoire>,
        random: Random = Random.Default
    ): EvenementAleatoire? {
        val pool = genererEvenementsGeneriques() + evenementsCustom
        return pool.randomOrNull(random)
    }
}
