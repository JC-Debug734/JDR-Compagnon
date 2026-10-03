package com.jc2.jdrcompagnon.feature_historique.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoriqueDao {

    @Insert
    suspend fun inserer(entree: HistoriqueEntreeEntity)

    @Query("SELECT * FROM historique_personnage WHERE personnageId = :personnageId ORDER BY timestamp DESC")
    fun observerHistorique(personnageId: String): Flow<List<HistoriqueEntreeEntity>>

    @Query("DELETE FROM historique_personnage WHERE personnageId = :personnageId")
    suspend fun supprimerHistoriqueDePersonnage(personnageId: String)
}
