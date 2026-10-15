package com.jc2.jdrcompagnon.feature_carte.domain.model

/**
 * Lieu notable au sein d'une ville (auberge, forge, place du marché...), sans coordonnées propres.
 *
 * [fonction] : ce que les joueurs peuvent y faire (prier, consulter le panneau de quêtes...), avec
 * ses réglages : [chancePourcent] (bénédiction, gain au jeu), [prixPc] (offrande, nuit, soins,
 * mise ; en pièces de cuivre), [texte] (bénédiction accordée, rumeurs une par ligne) et
 * [queteIds] (contrats affichés sur un panneau de quêtes), [dd] (jet de compétence) et [valeur]
 * (gain de l'arène en po, PX de l'entraînement). [visibleJoueurs] faux : le lieu reste
 * réservé au MJ (jamais envoyé aux joueurs).
 */
data class LieuNotable(
    val id: String,
    val villeId: String,
    val nom: String,
    val description: String = "",
    val fonction: FonctionLieu = FonctionLieu.AUCUNE,
    val chancePourcent: Int = 50,
    val prixPc: Int = 0,
    val texte: String = "",
    val queteIds: List<String> = emptyList(),
    val visibleJoueurs: Boolean = true,
    // DD du jet de compétence (bibliothèque, arène) et valeur de la fonction : prix de l'arène
    // en po, PX de l'entraînement, frais de la banque en %.
    val dd: Int = 12,
    val valeur: Int = 0,
)

/**
 * Fonction d'un lieu de ville, utilisée par les joueurs depuis l'écran du lieu (menu latéral
 * joueur). Les effets sont appliqués à la fiche du personnage (relayée au MJ en partie réseau).
 */
enum class FonctionLieu(val label: String, val emoji: String, val aide: String) {
    AUCUNE("Aucune", "📍", "Simple lieu à visiter."),
    TEMPLE("Temple / sanctuaire", "🙏", "Prier : chance (en %) de recevoir une bénédiction (inspiration héroïque), contre une offrande éventuelle."),
    PANNEAU_QUETES("Panneau de quêtes", "📜", "Affiche les contrats de quête choisis par le MJ ; les cocher les rend visibles des joueurs."),
    AUBERGE("Auberge", "🛏️", "Passer la nuit : repos long, contre le prix de la chambre."),
    GUERISSEUR("Guérisseur", "⚕️", "Soins payants : points de vie au maximum et état Empoisonné retiré."),
    TAVERNE("Taverne / rumeurs", "🍺", "Écouter les rumeurs : une rumeur tirée au hasard dans la liste du MJ (une par ligne), contre une tournée."),
    TRIPOT("Tripot / jeux", "🎲", "Parier la mise : chance (en %) de doubler la mise, sinon elle est perdue."),
    ECURIE("Écurie", "🐎", "Louer une monture parmi celles listées par le MJ (une par ligne), au prix de la location."),
    BANQUE("Banque", "🏦", "Déposer ou retirer de l'or dans le coffre du personnage (frais en % sur chaque dépôt)."),
    BIBLIOTHEQUE("Bibliothèque", "📚", "Faire des recherches : jet d'Investigation contre le DD ; réussite = un savoir tiré dans la liste du MJ (un par ligne)."),
    ARENE("Arène", "⚔️", "Combat d'exhibition : jet d'Athlétisme contre le DD ; victoire = prix en po, défaite = inscription perdue."),
    ORACLE("Oracle / diseuse", "🔮", "Consulter l'oracle : une prophétie tirée dans la liste du MJ (une par ligne)."),
    THERMES("Thermes / bains", "♨️", "Se délasser : retire un niveau d'épuisement."),
    ENTRAINEMENT("Maître d'armes", "🏋️", "Leçon payante : gain de PX fixé par le MJ (une fois par visite)."),
    ;

    companion object {
        fun depuis(nom: String?): FonctionLieu = entries.firstOrNull { it.name == nom } ?: AUCUNE
    }
}
