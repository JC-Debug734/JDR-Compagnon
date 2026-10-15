package com.jc2.jdrcompagnon.feature_faction.domain.model

import kotlinx.serialization.Serializable

/** Nature d'une faction. */
@Serializable
enum class TypeFaction(val label: String, val emoji: String) {
    RELIGIEUSE("Religieuse", "⛪"),
    NOBLESSE("Noblesse", "👑"),
    POLITIQUE("Politique", "🏛️"),
    MILITAIRE("Militaire", "🛡️"),
    GUILDE("Guilde", "⚒️"),
    MARCHANDE("Marchande", "💰"),
    CRIMINELLE("Criminelle", "🗡️"),
    ARCANIQUE("Arcanique", "🔮"),
    SECRETE("Société secrète", "🕯️"),
    TRIBALE("Tribu / clan", "🪶"),
    AUTRE("Autre", "🏳️"),
}

/** Attitude d'une faction envers une autre. */
@Serializable
enum class AttitudeFaction(val label: String) {
    ALLIEE("Alliée"),
    AMICALE("Amicale"),
    NEUTRE("Neutre"),
    RIVALE("Rivale"),
    ENNEMIE("Ennemie"),
}

/** Membre d'une faction : un PNJ de l'appli ([pnjId]) ou un nom libre. */
@Serializable
data class MembreFaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String,
    val pnjId: String? = null,
    val rang: String = "",
    val note: String = "",
    val chef: Boolean = false,
)

/** Fête ou célébration propre à la faction. [quand] : date ou période en texte libre. */
@Serializable
data class FeteFaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String,
    val quand: String = "",
    val description: String = "",
)

/** Relation avec une autre faction du même monde. */
@Serializable
data class RelationFaction(
    val factionId: String,
    val attitude: AttitudeFaction = AttitudeFaction.NEUTRE,
    val note: String = "",
)

/**
 * Projet en cours de la faction, suivi comme une horloge : [avancement] cases remplies sur
 * [total] ; quand elle est pleine, [consequence] arrive dans le monde.
 */
@Serializable
data class ProjetFaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String,
    val avancement: Int = 0,
    val total: Int = 6,
    val consequence: String = "",
)

/**
 * Faction d'un monde (outil FACTIONS). Son [nom] fait le lien avec la réputation des groupes et
 * des PNJ (Reputation.factionName). [description] est ce qu'on en sait publiquement ; [secrets]
 * reste réservé au MJ.
 */
@Serializable
data class Faction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val worldId: String = "",
    val nom: String,
    val type: TypeFaction = TypeFaction.AUTRE,
    val devise: String = "",
    val description: String = "",
    val symbole: String = "",
    val interets: String = "",
    val methodes: String = "",
    val ressources: String = "",
    val siege: String = "",
    val territoires: String = "",
    val secrets: String = "",
    val notes: String = "",
    val membres: List<MembreFaction> = emptyList(),
    val fetes: List<FeteFaction> = emptyList(),
    val relations: List<RelationFaction> = emptyList(),
    val projets: List<ProjetFaction> = emptyList(),
    // Campagnes où la faction intervient (étiquettes facultatives, pour filtrer).
    val campagneIds: List<String> = emptyList(),
)
