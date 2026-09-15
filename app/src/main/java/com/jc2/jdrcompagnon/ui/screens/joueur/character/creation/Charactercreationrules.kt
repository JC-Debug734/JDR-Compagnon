package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

/**
 * Règles de calcul pures du SRD 5.2.1 qui ne sont PAS des listes de choix issues
 * d'un fichier de données. Les classes, historiques, espèces et dons sont lus
 * depuis classes_srd521.md / historiques_srd521.md / especes_srd521.md /
 * dons_srd521.md via SrdCreationParsers.kt — rien de tout ça n'est dupliqué ici.
 */

enum class Caracteristique(val label: String) {
    FORCE("Force"),
    DEXTERITE("Dextérité"),
    CONSTITUTION("Constitution"),
    INTELLIGENCE("Intelligence"),
    SAGESSE("Sagesse"),
    CHARISME("Charisme")
}

enum class Alignement(val code: String, val label: String) {
    LOYAL_BON("LB", "Loyal Bon"),
    NEUTRE_BON("NB", "Neutre Bon"),
    CHAOTIQUE_BON("CB", "Chaotique Bon"),
    LOYAL_NEUTRE("LN", "Loyal Neutre"),
    NEUTRE("N", "Neutre"),
    CHAOTIQUE_NEUTRE("CN", "Chaotique Neutre"),
    LOYAL_MAUVAIS("LM", "Loyal Mauvais"),
    NEUTRE_MAUVAIS("NM", "Neutre Mauvais"),
    CHAOTIQUE_MAUVAIS("CM", "Chaotique Mauvais");
    // Le SRD précise : par défaut les PJ ne sont pas d'alignement mauvais,
    // sauf accord du MJ. Le wizard avertit (sans bloquer) sur LM/NM/CM.
    val estMauvais: Boolean get() = this in listOf(LOYAL_MAUVAIS, NEUTRE_MAUVAIS, CHAOTIQUE_MAUVAIS)
}

enum class MethodeGenerationCaracteristiques(val label: String) {
    VALEURS_STANDARD("Valeurs standard (15, 14, 13, 12, 10, 8)"),
    GENERATION_ALEATOIRE("Génération aléatoire (4d6, garder les 3 meilleurs, x6)"),
    ACQUISITION_PAR_POINTS("Acquisition par points (27 points)")
}

object TablesCaracteristiques {
    /** Table "Coût des valeurs de caractéristique" (méthode Acquisition par points). */
    val coutParValeur: Map<Int, Int> = mapOf(
        8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9
    )
    const val BUDGET_POINTS = 27

    val valeursStandard: List<Int> = listOf(15, 14, 13, 12, 10, 8)

    /** Table "Valeurs et modificateurs de caractéristique". */
    fun modificateur(valeur: Int): Int = when (valeur) {
        3 -> -4
        4, 5 -> -3
        6, 7 -> -2
        8, 9 -> -1
        10, 11 -> 0
        12, 13 -> 1
        14, 15 -> 2
        16, 17 -> 3
        18, 19 -> 4
        20 -> 5
        else -> Math.floorDiv(valeur - 10, 2) // extrapolation au-delà de la table
    }
}

/** Bonus de maîtrise au niveau 1, table "Progression des personnages". */
const val BONUS_MAITRISE_NIVEAU_1 = 2