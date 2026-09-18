package com.jc2.jdrcompagnon.feature_carte.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
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
    val scenarioIds: String = "" // ids séparés par des virgules, vide = aucun scénario lié
)

@Entity(tableName = "cartes_campagne")
data class CarteCampagneEntity(
    @PrimaryKey val campagneId: String,
    val largeurCases: Int,
    val hauteurCases: Int,
    val echelleKmParCase: Int,
    val imageFileName: String? = null
)

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
