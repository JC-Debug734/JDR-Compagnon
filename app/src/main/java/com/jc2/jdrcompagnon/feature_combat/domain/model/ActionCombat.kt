package com.jc2.jdrcompagnon.feature_combat.domain.model

/** Les 4 familles d'actions de combat regroupées à l'écran Actions de Combat. */
enum class CategorieActionCombat(val label: String) {
    OFFENSIVE_MAGIE("Offensive & Magie"),
    MOUVEMENT_POSTURE("Mouvement & Posture"),
    TACTIQUE_COMPETENCES("Tactique & Compétences"),
    ENVIRONNEMENT_EQUIPEMENT("Environnement & Équipement")
}

/** Une action de combat de référence : nom + description courte affichée au tap. */
data class ActionCombat(
    val id: String,
    val categorie: CategorieActionCombat,
    val nom: String,
    val description: String
)

/**
 * Catalogue statique des actions de combat courantes (règles 2024) — outil de référence pur
 * (pas de persistance, pas de tirage), donc pas de couche data/repository ni de DI : une simple
 * liste constante suffit, au même titre que les tuiles fixes d'un menu.
 */
object ActionsCombat {

    /**
     * Rappel affiché en tête d'écran : ce qui reste disponible en plus de l'action principale
     * choisie ci-dessous (déplacement, interaction gratuite, action bonus/réaction).
     */
    const val RAPPEL_ACTION_PRINCIPALE =
        "En plus de cette action, vous disposez toujours de votre déplacement, d'une interaction " +
            "gratuite avec un objet pendant votre déplacement ou votre attaque (dégainer une arme, " +
            "ouvrir une porte), et — si une capacité ou un sort vous le permet — d'une action bonus " +
            "ou d'une réaction."

    val toutes: List<ActionCombat> = listOf(
        ActionCombat(
            id = "attaquer",
            categorie = CategorieActionCombat.OFFENSIVE_MAGIE,
            nom = "Attaquer",
            description = "Vous frappez avec une arme ou effectuez une frappe à mains nues. Empoignade (Grapple) et " +
                "Bousculade (Shove) sont des formes de frappe à mains nues : la cible doit réussir un jet de " +
                "sauvegarde pour l'éviter (et non plus un jet d'opposition)."
        ),
        ActionCombat(
            id = "magie",
            categorie = CategorieActionCombat.OFFENSIVE_MAGIE,
            nom = "Magie",
            description = "Vous lancez un sort dont le temps d'incantation est une action, ou vous activez un objet magique."
        ),
        ActionCombat(
            id = "dash",
            categorie = CategorieActionCombat.MOUVEMENT_POSTURE,
            nom = "Se précipiter",
            description = "Vous gagnez un déplacement supplémentaire pour ce tour, égal à votre vitesse actuelle."
        ),
        ActionCombat(
            id = "disengage",
            categorie = CategorieActionCombat.MOUVEMENT_POSTURE,
            nom = "Se désengager",
            description = "Jusqu'à la fin de votre tour, vos déplacements ne provoquent pas d'attaques d'opportunité."
        ),
        ActionCombat(
            id = "dodge",
            categorie = CategorieActionCombat.MOUVEMENT_POSTURE,
            nom = "Esquiver",
            description = "Jusqu'au début de votre prochain tour, tous les jets d'attaque contre vous subissent le " +
                "désavantage (si vous voyez l'attaquant), et vous obtenez l'avantage à tous vos jets de sauvegarde " +
                "de Dextérité."
        ),
        ActionCombat(
            id = "hide",
            categorie = CategorieActionCombat.MOUVEMENT_POSTURE,
            nom = "Se cacher",
            description = "Vous effectuez un test de Dextérité (Discrétion) contre un DD de base de 15 (ou la " +
                "Perception passive d'un ennemi si elle est supérieure). En cas de réussite, vous obtenez la " +
                "condition Invisible."
        ),
        ActionCombat(
            id = "help",
            categorie = CategorieActionCombat.TACTIQUE_COMPETENCES,
            nom = "Aider",
            description = "Vous aidez un allié à accomplir une tâche : il obtient l'avantage à son prochain test de " +
                "caractéristique, ou à son prochain jet d'attaque contre une créature précise s'il attaque avant le " +
                "début de votre prochain tour."
        ),
        ActionCombat(
            id = "ready",
            categorie = CategorieActionCombat.TACTIQUE_COMPETENCES,
            nom = "Se tenir prêt",
            description = "Vous choisissez une action à faire et une condition de déclenchement. Quand le déclencheur " +
                "se produit, vous utilisez votre réaction pour agir."
        ),
        ActionCombat(
            id = "search",
            categorie = CategorieActionCombat.TACTIQUE_COMPETENCES,
            nom = "Chercher",
            description = "Vous consacrez votre attention à trouver quelque chose de dissimulé : test de Sagesse " +
                "(Perception) ou d'Intelligence (Investigation) selon la nature de la recherche."
        ),
        ActionCombat(
            id = "study",
            categorie = CategorieActionCombat.TACTIQUE_COMPETENCES,
            nom = "Étudier",
            description = "Vous utilisez votre Intelligence (Arcanes, Histoire, Nature, Religion ou Investigation) " +
                "pour vous remémorer ou déduire des informations utiles sur un monstre, un piège ou une énigme en " +
                "plein combat."
        ),
        ActionCombat(
            id = "influence",
            categorie = CategorieActionCombat.TACTIQUE_COMPETENCES,
            nom = "Influencer",
            description = "Vous utilisez votre Charisme (Intimidation, Persuasion, Tromperie) ou votre Sagesse " +
                "(Dressage) pour demander quelque chose à une créature ou modifier son attitude à votre égard."
        ),
        ActionCombat(
            id = "utilize",
            categorie = CategorieActionCombat.ENVIRONNEMENT_EQUIPEMENT,
            nom = "Utiliser",
            description = "Vous interagissez avec un objet matériel — boire une potion, crocheter une serrure avec " +
                "des outils de voleur, utiliser une trousse de soins."
        ),
        ActionCombat(
            id = "improvise",
            categorie = CategorieActionCombat.ENVIRONNEMENT_EQUIPEMENT,
            nom = "Improviser",
            description = "Toute autre action que vous souhaitez tenter (vous balancer à un lustre, renverser une " +
                "table...). Le MJ détermine le test de caractéristique nécessaire."
        ),
    )

    fun parCategorie(categorie: CategorieActionCombat): List<ActionCombat> =
        toutes.filter { it.categorie == categorie }
}
