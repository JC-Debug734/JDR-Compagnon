package com.jc2.jdrcompagnon.feature_evenement.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bibliothèque d'événements (version 18 de la base, AppDatabase.MIGRATION_17_18). Les effets sont
 * stockés en JSON (même choix que TableAleatoireEntity) : petite liste toujours lue/écrite avec
 * son événement, sans filtre SQL dessus.
 */
@Entity(tableName = "evenements", indices = [Index("worldId")])
data class EvenementEntity(
    @PrimaryKey val id: String,
    val worldId: String,
    val campagneId: String? = null,
    val type: String,           // nom de l'enum TypeEvenement
    val titre: String,
    val description: String = "",
    val effetsJson: String = "[]",
    // Ajoutées en version 19 (AppDatabase.MIGRATION_18_19) : ce qui est attendu et issues possibles.
    val objectif: String = "",
    val issuesJson: String = "[]",
    // Ajoutée en version 20 (AppDatabase.MIGRATION_19_20) : monstres et PNJ impliqués.
    val profilsJson: String = "[]"
)
