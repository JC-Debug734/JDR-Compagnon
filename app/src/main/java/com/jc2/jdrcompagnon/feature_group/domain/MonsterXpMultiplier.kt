package com.jc2.jdrcompagnon.feature_group.domain

/**
 * Multiplicateur appliqué à la somme des XP des monstres d'une rencontre,
 * pour refléter le danger accru qu'apporte un nombre élevé d'adversaires.
 */
object MonsterXpMultiplier {

    private val multipliers = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0)

    /**
     * @param monsterCount nombre de monstres/adversaires dans la rencontre
     * @param combatantCount taille du groupe de combattants (PJ + PNJ/montures combattants)
     */
    fun multiplierFor(monsterCount: Int, combatantCount: Int): Double {
        val bracket = when {
            monsterCount <= 1 -> 0
            monsterCount == 2 -> 1
            monsterCount in 3..6 -> 2
            monsterCount in 7..10 -> 3
            monsterCount in 11..14 -> 4
            else -> 5
        }
        // Un petit groupe (<3) subit une rencontre plus dure à nombre égal de monstres ;
        // un grand groupe (>5) l'absorbe plus facilement : on décale le palier en conséquence.
        val adjustedBracket = when {
            combatantCount < 3 -> (bracket + 1).coerceAtMost(5)
            combatantCount > 5 -> (bracket - 1).coerceAtLeast(0)
            else -> bracket
        }
        return multipliers[adjustedBracket]
    }
}
