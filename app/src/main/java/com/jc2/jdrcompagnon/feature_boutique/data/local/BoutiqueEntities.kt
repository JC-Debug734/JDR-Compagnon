package com.jc2.jdrcompagnon.feature_boutique.data.local

import androidx.room.ColumnInfo
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
    // Montants en pièces de cuivre depuis la version 29 (MIGRATION_28_29) ; colonnes gardées.
    @ColumnInfo(name = "argentDisponibleEnPo") val argentDisponibleEnPc: Int = 0,
    val traitMarchand: String = "",
    val villeId: String? = null,
    // Ajoutée en version 24 de la base (AppDatabase.MIGRATION_23_24).
    val campagneId: String? = null,
    // Ajoutée en version 27 de la base (AppDatabase.MIGRATION_26_27).
    val image: String? = null
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
    // En pièces de cuivre depuis la version 29 (MIGRATION_28_29) ; colonnes gardées.
    @ColumnInfo(name = "coutBaseEnPo") val coutBaseEnPc: Int, // dupliqué en cache d'affichage uniquement, jamais source de vérité
    @ColumnInfo(name = "prixApplique") val prixEnPc: Int,
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
    // En pièces de cuivre depuis la version 29 (MIGRATION_28_29) ; colonne gardée.
    @ColumnInfo(name = "prixEnPo") val prixEnPc: Int,
    val quantiteDisponible: Int?,
    val actif: Boolean = true,
    // Ajoutée en version 14 de la base (AppDatabase.MIGRATION_13_14).
    val personnalise: Boolean = false,
    // Effet du service (nom de EffetService), depuis la version 32.
    val effet: String = "AUTO",
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