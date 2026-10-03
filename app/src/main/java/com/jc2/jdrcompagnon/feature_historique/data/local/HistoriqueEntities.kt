package com.jc2.jdrcompagnon.feature_historique.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une entrée du journal d'un personnage (montée de niveau, +1 caractéristique, équipement
 * gagné/perdu, sort appris, etc.). personnageId référence Character.id, qui n'est pas une
 * entité Room (Character est persisté en JSON dans SharedPreferences via GameState) : pas de
 * ForeignKey possible ici, le lien se fait uniquement par égalité d'id.
 */
@Entity(tableName = "historique_personnage", indices = [Index("personnageId")])
data class HistoriqueEntreeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personnageId: String,
    val timestamp: Long,
    val type: String,
    val description: String
)
