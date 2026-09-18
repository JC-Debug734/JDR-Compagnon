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

/** Descriptions reprises du SRD 5.2.1, chapitre "Les neuf alignements" (p.23). */
enum class Alignement(val code: String, val label: String, val description: String) {
    LOYAL_BON(
        "LB", "Loyal Bon",
        "Les créatures Loyales Bonnes s'efforcent de faire le bien selon les attentes de la société. Une personne qui combat l'injustice et protège les innocents sans hésiter sera probablement Loyale Bonne."
    ),
    NEUTRE_BON(
        "NB", "Neutre Bon",
        "Les créatures Neutres Bonnes font de leur mieux en s'adaptant aux règles, sans pour autant s'y contraindre strictement. Une personne bienveillante qui aide les autres selon leurs besoins sera probablement Neutre Bonne."
    ),
    CHAOTIQUE_BON(
        "CB", "Chaotique Bon",
        "Les créatures Chaotiques Bonnes agissent selon ce que leur conscience leur dicte, sans se soucier des attentes de la société. Un rebelle qui intercepte les collecteurs d'impôts d'un baron cruel pour reverser l'or saisi aux indigents sera probablement Chaotique Bon."
    ),
    LOYAL_NEUTRE(
        "LN", "Loyal Neutre",
        "Les individus Loyaux Neutres se conduisent en accord avec la loi, les traditions ou leur code personnel. Une personne qui adhère à une discipline de vie sans se laisser influencer par les affres des nécessiteux ni les tentations du mal sera probablement Loyale Neutre."
    ),
    NEUTRE(
        "N", "Neutre",
        "L'alignement Neutre est celui des gens qui préfèrent éviter les questions morales et ne pas prendre parti, pour simplement agir au cas par cas, du mieux possible. Une personne que les débats moraux ennuient sera probablement Neutre."
    ),
    CHAOTIQUE_NEUTRE(
        "CN", "Chaotique Neutre",
        "Les créatures Chaotiques Neutres agissent comme bon leur semble, leur propre liberté important plus que tout le reste. Une canaille qui erre dans la nature en vivant d'expédients sera probablement Chaotique Neutre."
    ),
    LOYAL_MAUVAIS(
        "LM", "Loyal Mauvais",
        "Les créatures Loyales Mauvaises s'approprient méthodiquement ce qu'elles désirent tout en restant dans le cadre de la loyauté, de la tradition ou de l'ordre. Un aristocrate qui exploite la population en intrigant pour le pouvoir sera probablement Loyal Mauvais."
    ),
    NEUTRE_MAUVAIS(
        "NM", "Neutre Mauvais",
        "Neutre Mauvais est l'alignement des personnes qui n'ont cure du mal qu'elles peuvent causer pour assouvir leurs désirs. Un criminel qui vole et tue comme bon lui semble sera probablement Neutre Mauvais."
    ),
    CHAOTIQUE_MAUVAIS(
        "CM", "Chaotique Mauvais",
        "La violence gratuite des créatures Chaotiques Mauvaises est motivée par la haine ou la soif de sang. Un scélérat dont les desseins sont animés par la vengeance et les ravages sera probablement Chaotique Mauvais."
    );
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

/**
 * Questions d'aide à la rédaction du passé du personnage, reprises du SRD 5.2.1,
 * section "Imaginez son passé et son présent" (p.21) — affichées comme guide sur
 * l'étape Historique du wizard.
 */
val QUESTIONS_PASSE_PERSONNAGE = listOf(
    "Qui a fait votre éducation ?",
    "Parmi vos amis d'enfance, qui était le ou la plus proche ?",
    "Avez-vous grandi avec un animal de compagnie ?",
    "Le sentiment amoureux vous est-il familier ? Le cas échéant, qui furent le ou les objets de votre amour ?",
    "Avez-vous été membre d'une organisation, telle qu'une guilde ou un culte religieux ? Le cas échéant, cette affiliation est-elle toujours d'actualité ?",
    "Quels sont les aspects de votre passé qui vous inspirent dans vos aventures d'aujourd'hui ?"
)