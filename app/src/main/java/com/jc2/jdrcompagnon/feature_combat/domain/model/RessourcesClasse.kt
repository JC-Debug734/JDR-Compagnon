package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character

/**
 * Ressources de classe à utilisations limitées de toutes les classes suivies par
 * Character.compteursUtilises (Guerrier, Roublard…) : lecture et récupération au repos court.
 */
object RessourcesClasse {

    fun toutes(c: Character): List<RessourceClasse> = Guerrier.ressources(c) + Roublard.ressources(c) + Dons.ressources(c)

    fun ressource(c: Character, cle: String): RessourceClasse? = toutes(c).firstOrNull { it.cle == cle }

    fun restantes(c: Character, r: RessourceClasse): Int = (r.max - (c.compteursUtilises[r.cle] ?: 0)).coerceIn(0, r.max)

    fun restantes(c: Character, cle: String): Int = ressource(c, cle)?.let { restantes(c, it) } ?: 0

    /** Compteurs après un repos court : ressources « repos court » remises à zéro, « une au repos court » −1. */
    fun apresReposCourt(c: Character): Map<String, Int> {
        val parCle = toutes(c).associateBy { it.cle }
        return c.compteursUtilises.mapNotNull { (cle, utilises) ->
            when (parCle[cle]?.recuperation) {
                Recuperation.REPOS_COURT -> null
                Recuperation.UNE_AU_REPOS_COURT -> (utilises - 1).takeIf { it > 0 }?.let { cle to it }
                else -> cle to utilises
            }
        }.toMap()
    }

    /** Dés d'Énergie psionique (Soldat psi, Âme acérée) selon le niveau de classe : (nombre, dé). */
    fun desEnergiePsionique(niveau: Int): Pair<Int, String> = when {
        niveau >= 17 -> 12 to "d12"
        niveau >= 13 -> 10 to "d10"
        niveau >= 11 -> 8 to "d10"
        niveau >= 9 -> 8 to "d8"
        niveau >= 5 -> 6 to "d8"
        else -> 4 to "d6"
    }
}
