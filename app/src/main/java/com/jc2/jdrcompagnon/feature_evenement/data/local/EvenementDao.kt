package com.jc2.jdrcompagnon.feature_evenement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EvenementDao {

    @Query("SELECT * FROM evenements WHERE worldId = :worldId ORDER BY titre COLLATE NOCASE ASC")
    fun observerEvenements(worldId: String): Flow<List<EvenementEntity>>

    @Query("SELECT * FROM evenements WHERE id = :id")
    suspend fun getParId(id: String): EvenementEntity?

    @Query("SELECT * FROM evenements WHERE id IN (:ids)")
    suspend fun getParIds(ids: List<String>): List<EvenementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarder(evenement: EvenementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderTous(evenements: List<EvenementEntity>)

    @Query("DELETE FROM evenements WHERE id = :id")
    suspend fun supprimer(id: String)

    @Query("SELECT COUNT(*) FROM evenements WHERE worldId = :worldId")
    suspend fun compterPourMonde(worldId: String): Int
}
