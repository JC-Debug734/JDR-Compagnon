package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import kotlin.random.Random

/** Génère un nom d'enseigne plausible, pour le bouton "nom aléatoire" du formulaire. */
class GenererNomBoutiqueUseCase {

    private companion object {
        val NOMS = listOf(
            "Le Chaudron Doré", "L'Enclume Runique", "Au Bon Aventurier",
            "La Rose des Vents", "Le Sac Sans Fond", "L'Échoppe du Corbeau",
            "Chez Grigan", "La Lanterne Bleue", "Au Coffre Ouvert",
            "Le Marteau et l'Enclume", "La Bourse Pleine", "Aux Trois Lunes",
            "Le Comptoir du Voyageur", "L'Étoile du Marchand", "La Malle Curieuse",
            "Au Vieux Grimoire", "Le Repos du Nain", "La Forge d'Argent",
            "Chez Belladone", "Le Tonneau Percé", "L'Auberge des Reliques",
            "La Corne d'Abondance", "Au Loup Blanc", "Le Sceau d'Or"
        )
    }

    operator fun invoke(random: Random = Random.Default): String = NOMS.random(random)
}
