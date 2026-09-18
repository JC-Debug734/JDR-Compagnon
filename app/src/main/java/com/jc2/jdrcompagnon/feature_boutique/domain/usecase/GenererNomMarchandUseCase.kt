package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import kotlin.random.Random

/** Génère un nom de marchand plausible, pour le bouton "nom aléatoire" du formulaire. */
class GenererNomMarchandUseCase {
    operator fun invoke(random: Random = Random.Default): String = NomsFantastiques.PRENOMS.random(random)
}
