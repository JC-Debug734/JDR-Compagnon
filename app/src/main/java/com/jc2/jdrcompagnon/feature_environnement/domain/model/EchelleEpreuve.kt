package com.jc2.jdrcompagnon.feature_environnement.domain.model

/**
 * Dosage d'une épreuve environnementale selon le groupe :
 * - tier D&D officiel déduit du niveau moyen (1 : niv. 1-4, 2 : 5-10, 3 : 11-16, 4 : 17-20) ;
 * - DD calibrés pour ~55-65 % de réussite d'un personnage compétent sur un jet "Moyen" ;
 * - dégâts issus de la table des dégâts improvisés du DMG ;
 * - longueur du Progrès proportionnelle au nombre de joueurs (chacun doit pouvoir contribuer).
 */
object EchelleEpreuve {

    fun tier(niveau: Int): Int = when {
        niveau >= 17 -> 4
        niveau >= 11 -> 3
        niveau >= 5 -> 2
        else -> 1
    }

    fun niveauxDuTier(tier: Int): String = when (tier) {
        1 -> "niv. 1-4"
        2 -> "niv. 5-10"
        3 -> "niv. 11-16"
        else -> "niv. 17-20"
    }

    private val dd = mapOf(
        1 to listOf(10, 13, 15, 18),
        2 to listOf(12, 15, 17, 20),
        3 to listOf(14, 17, 19, 22),
        4 to listOf(16, 19, 21, 25)
    )

    private val degats = mapOf(
        1 to listOf("1d10", "2d10", "4d10"),
        2 to listOf("2d10", "4d10", "10d10"),
        3 to listOf("4d10", "10d10", "18d10"),
        4 to listOf("10d10", "18d10", "24d10")
    )

    fun dd(tier: Int, difficulte: DifficulteRelative): Int =
        dd.getValue(tier.coerceIn(1, 4))[difficulte.ordinal]

    fun degats(tier: Int, gravite: GraviteDegats): String =
        degats.getValue(tier.coerceIn(1, 4))[gravite.ordinal]

    fun progresMax(duree: DureeEpreuve, nbJoueurs: Int): Int {
        val n = nbJoueurs.coerceAtLeast(1)
        return when (duree) {
            DureeEpreuve.COURTE -> n + 2
            DureeEpreuve.STANDARD -> n + 4
            DureeEpreuve.LONGUE -> n * 2 + 2
        }
    }

    /** Ligne mécanique indépendante du groupe : "JS DEX Moyen · dégâts Dangereux contondant". */
    fun libelleRelatif(capacite: CapaciteEpreuve): String? {
        val parties = buildList {
            capacite.difficulte?.let { diff ->
                add(if (capacite.sauvegarde.isNullOrBlank()) "DD ${diff.label}" else "JS ${capacite.sauvegarde} ${diff.label}")
            }
            capacite.degats?.let { gravite ->
                add(listOfNotNull("dégâts ${gravite.label}", capacite.typeDegats?.takeIf { it.isNotBlank() }).joinToString(" "))
            }
        }
        return parties.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** Ligne mécanique d'une capacité résolue pour le tier : "JS DEX DD 13 · 2d10 contondant". */
    fun resoudre(capacite: CapaciteEpreuve, tier: Int): String? {
        val parties = buildList {
            capacite.difficulte?.let { diff ->
                val valeur = dd(tier, diff)
                add(if (capacite.sauvegarde.isNullOrBlank()) "DD $valeur" else "JS ${capacite.sauvegarde} DD $valeur")
            }
            capacite.degats?.let { gravite ->
                add(listOfNotNull(degats(tier, gravite), capacite.typeDegats?.takeIf { it.isNotBlank() }).joinToString(" "))
            }
        }
        return parties.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
