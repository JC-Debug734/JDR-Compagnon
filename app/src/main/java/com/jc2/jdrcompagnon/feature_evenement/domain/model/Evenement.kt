package com.jc2.jdrcompagnon.feature_evenement.domain.model

import java.util.UUID

/**
 * Les sept familles d'événements de la bibliothèque :
 * - Rencontre : réaction immédiate (combat, négociation, fuite), entame les ressources du groupe.
 * - Découverte : exploration pacifique (Investigation/Survie), lieux remarquables, butin caché.
 * - Rumeur / Indice : annonce une menace à venir, guide les personnages.
 * - Péril : menace sans adversaire, résolue par jets de sauvegarde ou de compétence.
 * - Complication : rebondissement qui change la situation, coûte du temps ou des options.
 * - Opportunité : un PNJ ou une situation propose quelque chose (accroche de quête).
 * - Répit : rend des ressources au groupe (repos sûr, bénédiction), équilibre le rythme.
 */
enum class TypeEvenement(val label: String) {
    RENCONTRE("Rencontre"),
    DECOUVERTE("Découverte"),
    RUMEUR_INDICE("Rumeur / Indice"),
    PERIL("Péril"),
    COMPLICATION("Complication"),
    OPPORTUNITE("Opportunité"),
    REPIT("Répit")
}

/** Nature d'un effet d'événement — détermine quelle action "Envoyer" est proposée au tirage. */
enum class CategorieEffet(val label: String) {
    EXPERIENCE("Expérience"),
    OR("Or"),
    REPUTATION("Réputation"),
    OBJET("Objet"),
    AUTRE("Autre")
}

/**
 * Un gain ou une perte associé à un événement (ex. "+50 XP", "-10 réputation auprès des Gardes",
 * "trouve une dague"). [quantite] est toujours positif, [gain] porte le signe — évite d'avoir à se
 * souvenir si une quantité négative représente une perte ou une simple erreur de saisie. [cible]
 * précise la faction (RÉPUTATION) ou le nom de l'objet (OBJET) ; ignoré pour EXPERIENCE/OR/AUTRE.
 * [description] est le texte libre affiché au MJ, obligatoire pour AUTRE (aucune action
 * automatique n'existe pour cette catégorie, purement informative).
 */
data class EffetEvenement(
    val id: String = UUID.randomUUID().toString(),
    val categorie: CategorieEffet = CategorieEffet.AUTRE,
    val gain: Boolean = true,
    val quantite: Int = 1,
    val cible: String = "",
    val description: String = ""
)

/** Nature d'une issue d'événement : détermine sa couleur et son ordre d'affichage. */
enum class NatureIssue(val label: String) {
    REUSSITE("Réussite"),
    PARTIELLE("Réussite partielle"),
    ECHEC("Échec"),
    AUTRE("Autre")
}

/**
 * Une issue possible d'un événement (ex. "Réussite : le villageois les guide jusqu'au gué",
 * "Échec : la roue casse, une demi-journée perdue"), avec ses propres effets. [titre] vide =
 * le libellé de [nature] est affiché.
 */
data class IssueEvenement(
    val id: String = UUID.randomUUID().toString(),
    val nature: NatureIssue = NatureIssue.REUSSITE,
    val titre: String = "",
    val description: String = "",
    val effets: List<EffetEvenement> = emptyList(),
    // Issue qui mène au combat : son encadré propose "Lancer le combat" avec les profils.
    val declencheCombat: Boolean = false
) {
    val libelle: String get() = titre.ifBlank { nature.label }
    val estVide: Boolean get() = titre.isBlank() && description.isBlank() && effets.isEmpty()
}

/** Origine d'un profil impliqué dans un événement. */
enum class TypeProfil(val label: String) {
    MONSTRE("Monstre"),   // fiche du bestiaire (SrdRepository), par nom
    PNJ("PNJ")            // fiche de personnage (GameState.characters), par nom
}

/**
 * Un profil impliqué dans un événement (les bandits d'une embuscade, le PNJ qui demande de
 * l'aide), affiché pendant la résolution pour ouvrir sa fiche ; les monstres peuvent lancer un
 * combat (format CompositionCombat : "Bandit x4, Chef de bande").
 */
data class ProfilEvenement(
    val type: TypeProfil = TypeProfil.MONSTRE,
    val nom: String,
    val quantite: Int = 1
) {
    val libelle: String get() = if (quantite > 1) "$nom ×$quantite" else nom
}

/**
 * Un événement de la bibliothèque du monde [worldId]. Écrit une seule fois puis référencé par id
 * depuis les tables aléatoires, villes, lieux, environnements et scènes (plutôt que recopié dans
 * chacun). [campagneId] non null = événement propre à cette campagne ; null = commun au monde.
 * [objectif] : ce qui est attendu des joueurs (action, jet et DD...) ; [effets] : effets
 * immédiats, dès que l'événement survient ; [issues] : résultats possibles selon ce que font
 * les joueurs, chacun avec ses effets, choisis par le MJ au moment de la résolution.
 */
data class Evenement(
    val id: String = UUID.randomUUID().toString(),
    val worldId: String,
    val campagneId: String? = null,
    val type: TypeEvenement = TypeEvenement.RENCONTRE,
    val titre: String,
    val description: String = "",
    val effets: List<EffetEvenement> = emptyList(),
    val objectif: String = "",
    val issues: List<IssueEvenement> = emptyList(),
    val profils: List<ProfilEvenement> = emptyList()
)
