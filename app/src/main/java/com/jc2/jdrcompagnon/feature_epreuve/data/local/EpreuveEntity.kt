package com.jc2.jdrcompagnon.feature_epreuve.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Complications stockées en JSON dans une colonne texte (même choix que
 * EnvironnementEntity.epreuvesJson) : petite liste toujours lue/écrite avec son épreuve.
 * Table ajoutée en version 21 de la base (AppDatabase.MIGRATION_20_21).
 */
@Entity(tableName = "epreuves", indices = [Index("worldId")])
data class EpreuveEntity(
    @PrimaryKey val id: String,
    val worldId: String,
    val nom: String,
    val description: String = "",
    val reussitesRequises: Int = 3,
    val imageFileName: String? = null,
    val complicationsJson: String = "[]",
    // Ajoutée en version 22 (AppDatabase.MIGRATION_21_22).
    val musicTrackId: String? = null,
    // Ajoutée en version 23 (AppDatabase.MIGRATION_22_23) : nom de DifficulteEpreuve, null = réussites fixes.
    val difficulte: String? = null,
)
