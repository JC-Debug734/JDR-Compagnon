package com.jc2.jdrcompagnon.feature_environnement.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Les listes (rumeurs, rencontres, monstres ajoutés manuellement, table de butin) sont stockées
 * en JSON dans de simples colonnes texte plutôt qu'en tables relationnelles séparées (contrairement
 * à BoutiqueEntity/ArticleEnVenteEntity) : ce sont des listes de texte libre sans recherche/filtre
 * SQL dessus, une table par liste serait un surcoût sans bénéfice ici.
 */
@Entity(tableName = "environnements")
data class EnvironnementEntity(
    @PrimaryKey val id: String,
    val nom: String,
    val description: String = "",
    val worldId: String = "",
    val imageFileName: String? = null,
    val musicTrackId: String? = null,
    val rumeursJson: String = "[]",
    val rencontresJson: String = "[]",
    val monstresJson: String = "[]",
    val butinJson: String = "[]",
    // Ajoutée en version 12 de la base (AppDatabase.MIGRATION_11_12).
    val epreuvesJson: String = "[]",
    // Ajoutées en version 13 de la base (AppDatabase.MIGRATION_12_13).
    val terrainsJson: String = "[]",
    val tablesAleatoiresJson: String = "[]",
    // Ajoutée en version 18 (AppDatabase.MIGRATION_17_18) : ids d'événements de la bibliothèque.
    val evenementsJson: String = "[]"
)
