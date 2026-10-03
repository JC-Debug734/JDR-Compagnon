package com.jc2.jdrcompagnon.feature_table_aleatoire.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Les entrées sont stockées en JSON dans des colonnes texte plutôt qu'en table relationnelle
 * séparée (même choix que EnvironnementEntity.rumeursJson/butinJson) : petites listes sans
 * recherche/filtre SQL dessus, toujours lues/écrites en bloc avec leur table parente. Seule
 * l'une des deux colonnes ([entreesEvenementsJson] ou [entreesLootJson]) est réellement peuplée,
 * selon [type].
 */
@Entity(tableName = "tables_aleatoires")
data class TableAleatoireEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val worldId: String = "",
    val type: String = "EVENEMENTS",
    val intervalleHeures: Int = 4,
    val active: Boolean = true,
    val derniereDeclenchementMinutes: Long? = null,
    val entreesEvenementsJson: String = "[]",
    val entreesLootJson: String = "[]"
)
