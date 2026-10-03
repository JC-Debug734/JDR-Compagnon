package com.jc2.jdrcompagnon.feature_epreuve.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpreuveDao {

    @Query("SELECT * FROM epreuves WHERE worldId = :worldId ORDER BY nom ASC")
    fun observerEpreuves(worldId: String): Flow<List<EpreuveEntity>>

    @Query("SELECT * FROM epreuves WHERE id = :id")
    suspend fun getEpreuveParId(id: String): EpreuveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarder(epreuve: EpreuveEntity)

    @Query("DELETE FROM epreuves WHERE id = :id")
    suspend fun supprimer(id: String)
}
