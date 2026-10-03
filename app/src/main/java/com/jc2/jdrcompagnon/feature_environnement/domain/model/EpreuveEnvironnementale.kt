package com.jc2.jdrcompagnon.feature_environnement.domain.model

import kotlinx.serialization.Serializable

/**
 * Épreuve environnementale, adaptée des "environnements" de Daggerheart pour D&D 5e : une fiche
 * de lieu/situation hostile avec deux comptes à rebours (Progrès à remplir par le groupe, Menace
 * qui s'épuise à chaque échec) et des capacités Passives/Réactions/Actions.
 *
 * Toutes les valeurs chiffrées (DD, dégâts, longueur du Progrès) sont exprimées en termes
 * relatifs et résolues au lancement selon le niveau et la taille du groupe (voir
 * [EchelleEpreuve]) : une même fiche sert du niveau 1 au niveau 20.
 *
 * Rattachée à un [Environnement] (liste sérialisée en JSON dans la colonne epreuvesJson), et
 * lançable depuis un scénario via le lien interne `#epreuve:[Nom]`.
 */
@Serializable
data class EpreuveEnvironnementale(
    val nom: String,
    val type: TypeEpreuve = TypeEpreuve.TRAVERSEE,
    val description: String = "",
    // Ce que l'environnement "veut" (isoler, épuiser, égarer…) : guide le MJ pour improviser.
    val pulsions: List<String> = emptyList(),
    val competences: List<String> = emptyList(),
    val duree: DureeEpreuve = DureeEpreuve.STANDARD,
    val menaceMax: Int = 4,
    val capacites: List<CapaciteEpreuve> = emptyList(),
    // Noms de créatures du bestiaire SRD susceptibles d'intervenir.
    val adversaires: List<String> = emptyList()
)

@Serializable
data class CapaciteEpreuve(
    val nom: String,
    val type: TypeCapacite,
    val description: String,
    // Caractéristique du jet de sauvegarde ("DEX", "CON"…), null = test de compétence ou pas de jet.
    val sauvegarde: String? = null,
    val difficulte: DifficulteRelative? = null,
    val degats: GraviteDegats? = null,
    val typeDegats: String? = null,
    // Question posée au MJ/aux joueurs pour ancrer la capacité dans la fiction.
    val question: String? = null
)

@Serializable
enum class TypeEpreuve(val label: String) {
    EXPLORATION("Exploration"),
    TRAVERSEE("Traversée"),
    SOCIALE("Sociale"),
    EVENEMENT("Événement")
}

/**
 * PASSIVE : toujours active. REACTION : se déclenche sur un échec lié. ACTION : le MJ peut la
 * jouer après un échec (la "tension" d'une épreuve naît uniquement des échecs du groupe).
 */
@Serializable
enum class TypeCapacite(val label: String) {
    PASSIVE("Passive"),
    REACTION("Réaction"),
    ACTION("Action")
}

@Serializable
enum class DifficulteRelative(val label: String) {
    FACILE("Facile"),
    MOYEN("Moyen"),
    DIFFICILE("Difficile"),
    EXTREME("Extrême")
}

@Serializable
enum class GraviteDegats(val label: String) {
    REVERS("Revers"),
    DANGEREUX("Dangereux"),
    MORTEL("Mortel")
}

@Serializable
enum class DureeEpreuve(val label: String) {
    COURTE("Courte"),
    STANDARD("Standard"),
    LONGUE("Longue")
}
