package com.jc2.jdrcompagnon.di

/**
 * Anciennes formulations de "ce qui est attendu" des exemples, par titre, avant leur passage en
 * liste (une option par ligne). Un exemple dont l'objectif est encore l'une d'elles n'a pas été
 * retouché par le MJ : il reçoit la nouvelle formulation (voir
 * EvenementDependencies.enrichirDepuisModele).
 */
internal val ANCIENS_OBJECTIFS: Map<String, Set<String>> = mapOf(
    // Bibliothèque
    "Animal sauvage" to setOf("Apaiser la bête : Sagesse (Dressage) DD 12, ou l'éviter : Dextérité (Discrétion) DD 13."),
    "Incident maladroit" to setOf("Apaiser les habitants : Charisme (Persuasion) DD 13, ou dédommager (5 po)."),
    "Aide à un PNJ" to setOf(
        "Aider : Force (Athlétisme) DD 12 pour la roue, Sagesse (Survie) pour l'animal, Sagesse (Médecine) pour le blessé."
    ),
    "Troc improvisé" to setOf("Négocier : Charisme (Persuasion) DD 12, ou Sagesse (Perspicacité) DD 12 pour repérer l'arnaque."),
    "Marque de confiance" to setOf("Rendre le service sans être vu : Dextérité (Discrétion) ou Charisme (Tromperie) DD 13."),
    // Tables d'exemple
    "Bande de gobelins" to setOf(
        "Repérer l'embuscade : Sagesse (Perception) DD 12, ou négocier le passage (les gobelins adorent les babioles)."
    ),
    "Loups affamés" to setOf("Tenir la meute à distance : feu, nourriture jetée, ou Sagesse (Dressage) DD 13."),
    "Bandits de grand chemin" to setOf(
        "Trois choix : payer les 20 po, négocier (Charisme (Persuasion) DD 13) ou intimider " +
            "(Charisme (Intimidation) DD 15). Un refus ou un jet raté mène au combat.",
        "Payer (20 po), négocier : Charisme (Persuasion) DD 13, ou intimider : Charisme (Intimidation) DD 15."
    ),
    "Sanglier enragé" to setOf("Esquiver la charge : Dextérité DD 12, ou le calmer : Sagesse (Dressage) DD 14."),
    "Patrouille de hobgobelins" to setOf("Se cacher : Dextérité (Discrétion) DD 14, ou parlementer : Charisme (Intimidation) DD 15."),
    "Ogre solitaire" to setOf("Le berner : Charisme (Tromperie) DD 12, ou l'amadouer avec de la nourriture."),
    "Nid d'araignées géantes" to setOf("Traverser les toiles : Dextérité (Acrobaties) DD 13, ou les brûler."),
    "Mercenaires en maraude" to setOf(
        "Payer 50 po, ou les convaincre de passer leur chemin (Charisme (Persuasion) DD 15). " +
            "Un refus ou un jet raté mène au combat.",
        "Les convaincre de passer leur chemin : Charisme (Persuasion) DD 15, ou les acheter (50 po)."
    ),
    "Culte secret" to setOf("Le faire parler : Sagesse (Perspicacité) DD 13, puis Charisme (Persuasion) DD 14."),
    "Garde du corps d'un seigneur de guerre" to setOf(
        "Obtenir une audience : Charisme (Persuasion) DD 16, ou passer inaperçu : Dextérité (Discrétion) DD 16."
    ),
    "Chimère" to setOf("La voir venir : Sagesse (Perception) DD 15, puis trouver un couvert."),
    "Élémentaire déchaîné" to setOf("Le renvoyer : Intelligence (Arcanes) DD 16 sur le cercle d'invocation, ou le vaincre."),
    "Sanctuaire ancien scellé" to setOf(
        "Désamorcer les pièges : outils de voleur DD 17, ou briser le sceau : Intelligence (Arcanes) DD 17."
    ),
    "Bibliothèque engloutie" to setOf("Plonger et chercher : Constitution DD 14 (apnée), puis Intelligence (Investigation) DD 15."),
    "Dragon adulte" to setOf("Négocier : Charisme (Persuasion) DD 20, ou fuir : Dextérité (Discrétion) DD 18."),
    "Avatar d'une divinité mineure" to setOf("Accepter l'épreuve de la divinité, ou y résister : Sagesse DD 18."),
    "Armée démoniaque" to setOf("Refermer la brèche : Intelligence (Arcanes) DD 20, pendant que les autres tiennent la ligne."),
    "Faille vers un autre plan" to setOf("Sceller la faille : Intelligence (Arcanes) DD 20, ou un sacrifice."),
)
