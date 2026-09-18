package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import kotlin.random.Random

/**
 * Génère une courte description de caractère (marchand ou employé), pour donner au MJ une
 * accroche de jeu de rôle immédiate sans avoir à improviser (ex. "gentil et peu enclin à la
 * négociation").
 */
class GenererTraitCaractereUseCase {

    private companion object {
        val TRAITS = listOf(
            "gentil et peu enclin à la négociation",
            "bourru mais honnête",
            "cupide et toujours prêt à marchander",
            "chaleureux avec les habitués, froid avec les étrangers",
            "bavard et curieux de tout",
            "discret et économe de mots",
            "prudent, méfiant envers les inconnus",
            "jovial et généreux",
            "sérieux et pointilleux sur les comptes",
            "nerveux, sursaute au moindre bruit",
            "fier de son savoir-faire",
            "fatigué mais consciencieux",
            "malin, toujours à l'affût d'une bonne affaire",
            "naïf et facile à convaincre",
            "aime raconter des histoires de voyageurs",
            "impatient, n'aime pas perdre de temps",
            "calme et posé en toute circonstance",
            "superstitieux, garde toujours un porte-bonheur sur lui"
        )
    }

    operator fun invoke(random: Random = Random.Default): String = TRAITS.random(random)
}
