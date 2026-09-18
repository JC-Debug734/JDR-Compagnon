package com.jc2.jdrcompagnon.feature_group.domain

/**
 * Seuils d'XP par niveau de personnage (barème D&D 5e).
 * Chaque valeur = budget d'XP qu'un unique combattant de ce niveau absorbe
 * pour une rencontre de la difficulté donnée.
 */
object XpThresholdTable {

    private data class Thresholds(val easy: Int, val medium: Int, val hard: Int, val deadly: Int)

    private val table: Map<Int, Thresholds> = mapOf(
        1 to Thresholds(25, 50, 75, 100),
        2 to Thresholds(50, 100, 150, 200),
        3 to Thresholds(75, 150, 225, 400),
        4 to Thresholds(125, 250, 375, 500),
        5 to Thresholds(250, 500, 750, 1100),
        6 to Thresholds(300, 600, 900, 1400),
        7 to Thresholds(350, 750, 1100, 1700),
        8 to Thresholds(450, 900, 1400, 2100),
        9 to Thresholds(550, 1100, 1600, 2400),
        10 to Thresholds(600, 1200, 1900, 2800),
        11 to Thresholds(800, 1600, 2400, 3600),
        12 to Thresholds(1000, 2000, 3000, 4500),
        13 to Thresholds(1100, 2200, 3400, 5100),
        14 to Thresholds(1250, 2500, 3800, 5700),
        15 to Thresholds(1400, 2800, 4300, 6400),
        16 to Thresholds(1600, 3200, 4800, 7200),
        17 to Thresholds(2000, 3900, 5900, 8800),
        18 to Thresholds(2100, 4200, 6300, 9500),
        19 to Thresholds(2400, 4900, 7300, 10900),
        20 to Thresholds(2800, 5700, 8500, 12700)
    )

    /** Seuil d'XP pour un unique combattant de ce niveau et cette difficulté. */
    fun thresholdFor(level: Int, difficulty: EncounterDifficulty): Int {
        val t = table.getValue(level.coerceIn(1, 20))
        return when (difficulty) {
            EncounterDifficulty.FACILE -> t.easy
            EncounterDifficulty.MOYENNE -> t.medium
            EncounterDifficulty.DIFFICILE -> t.hard
            EncounterDifficulty.MORTELLE -> t.deadly
        }
    }
}
