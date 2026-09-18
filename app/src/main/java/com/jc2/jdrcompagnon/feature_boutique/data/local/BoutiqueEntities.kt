package com.jc2.jdrcompagnon.feature_boutique.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "boutiques")
data class BoutiqueEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val standing: String,      // nom de l'enum StandingBoutique
    val nomMarchand: String,
    val descriptionMarchand: String,
    val type: String = "MARCHAND",       // nom de l'enum TypeBoutique
    val argentDisponibleEnPo: Int = 0,
    val traitMarchand: String = "",
    val villeId: String? = null
)

@Entity(
    tableName = "articles_en_vente",
    foreignKeys = [
        ForeignKey(
            entity = BoutiqueEntity::class,
            parentColumns = ["id"],
            childColumns = ["boutiqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ArticleEnVenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boutiqueId: String,
    val equipementNom: String,     // référence par nom vers le SRD, pas de duplication du prix de base
    val equipementType: String,
    val coutBaseEnPo: Int,         // dupliqué en cache d'affichage uniquement, jamais source de vérité
    val prixApplique: Int,
    val quantiteStock: Int,
    val toujoursDisponible: Boolean = false
)

@Entity(
    tableName = "services_boutique",
    foreignKeys = [
        ForeignKey(
            entity = BoutiqueEntity::class,
            parentColumns = ["id"],
            childColumns = ["boutiqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boutiqueId: String,
    val nom: String,
    val description: String,
    val prixEnPo: Int,
    val quantiteDisponible: Int?,
    val actif: Boolean = true
)

@Entity(
    tableName = "employes_boutique",
    foreignKeys = [
        ForeignKey(
            entity = BoutiqueEntity::class,
            parentColumns = ["id"],
            childColumns = ["boutiqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EmployeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val boutiqueId: String,
    val nom: String,
    val role: String,           // nom de l'enum RoleEmploye
    val trait: String = ""
)