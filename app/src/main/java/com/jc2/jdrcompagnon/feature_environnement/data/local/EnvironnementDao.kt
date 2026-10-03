package com.jc2.jdrcompagnon.feature_environnement.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironnementDao {

    @Query("SELECT * FROM environnements WHERE worldId = :worldId ORDER BY nom ASC")
    fun observerEnvironnements(worldId: String): Flow<List<EnvironnementEntity>>

    @Query("SELECT * FROM environnements WHERE id = :id")
    suspend fun getEnvironnementParId(id: String): EnvironnementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarder(environnement: EnvironnementEntity)

    @Query("DELETE FROM environnements WHERE id = :id")
    suspend fun supprimer(id: String)

    @Query("SELECT COUNT(*) FROM environnements WHERE worldId = :worldId")
    suspend fun compterPourMonde(worldId: String): Int
}
