package com.jc2.jdrcompagnon.feature_carte.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "points_interet")
data class PointInteretEntity(
    @PrimaryKey val id: String,
    val campagneId: String,
    val nom: String,
    val type: String,          // nom de l'enum TypePointInteret
    val x: Int,
    val y: Int,
    val description: String,
    val boutiqueIds: String,   // ids séparés par des virgules, vide = aucune boutique liée
    val scenarioIds: String = "", // ids séparés par des virgules, vide = aucun scénario lié
    val iconKey: String? = null,
    val couleurArgb: Int? = null,
    // Carte sur laquelle le MJ a placé ce lieu (version 15). Null = lieu non placé (les points
    // antérieurs aux cartes multiples ont été rattachés à la carte historique, cf. MIGRATION_15_16).
    val carteId: String? = null,
    // Position libre du centre de l'icône en fraction de la carte (version 17), null = ancien
    // point placé sur la case (x, y). visibleJoueurs : lieu montré aux joueurs en exploration.
    val fx: Float? = null,
    val fy: Float? = null,
    val visibleJoueurs: Boolean = false,
    // Événements de la bibliothèque (feature_evenement) rattachés à ce lieu (version 18), ids
    // séparés par des virgules comme boutiqueIds ; vide = aucun.
    val evenementIds: String = ""
)

/**
 * Plusieurs cartes par campagne depuis la version 15 de la base. La carte existante d'une
 * campagne a été migrée avec id = campagneId ("Carte principale"), supprimable comme les autres.
 */
@Entity(tableName = "cartes_campagne", indices = [Index("campagneId")])
data class CarteCampagneEntity(
    @PrimaryKey val id: String,
    val campagneId: String,
    val nom: String,
    val largeurCases: Int,
    val hauteurCases: Int,
    val echelleKmParCase: Int,
    val imageFileName: String? = null
)

/**
 * Ancien stockage des événements de campagne/ville, remplacé par la bibliothèque
 * (feature_evenement) en version 18 : conservé uniquement pour convertir les données existantes
 * (ConversionEvenementsCarte), la table se vide à la première ouverture.
 */
@Entity(tableName = "evenements_aleatoires")
data class EvenementAleatoireEntity(
    @PrimaryKey val id: String,
    val campagneId: String,
    val villeId: String? = null,
    val titre: String,
    val description: String,
    val effetsJson: String     // liste d'EffetEvenement sérialisée en JSON (kotlinx.serialization)
)

@Entity(
    tableName = "lieux_notables",
    foreignKeys = [
        ForeignKey(
            entity = PointInteretEntity::class,
            parentColumns = ["id"],
            childColumns = ["villeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LieuNotableEntity(
    @PrimaryKey val id: String,
    val villeId: String,
    val nom: String,
    val description: String
)
