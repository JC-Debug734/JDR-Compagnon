package com.jc2.jdrcompagnon.feature_table_aleatoire.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TableAleatoireDao {

    @Query("SELECT * FROM tables_aleatoires WHERE worldId = :worldId ORDER BY nom ASC")
    fun observerTables(worldId: String): Flow<List<TableAleatoireEntity>>

    @Query("SELECT * FROM tables_aleatoires WHERE worldId = :worldId AND type = :type ORDER BY nom ASC")
    fun observerTablesParType(worldId: String, type: String): Flow<List<TableAleatoireEntity>>

    @Query("SELECT * FROM tables_aleatoires WHERE id = :id")
    suspend fun getTableParId(id: String): TableAleatoireEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarder(table: TableAleatoireEntity)

    @Query("DELETE FROM tables_aleatoires WHERE id = :id")
    suspend fun supprimer(id: String)

    /** Ouverture d'un scénario en lecture : toutes les tables repartent de zéro (voir LectureScenarioState). */
    @Query("UPDATE tables_aleatoires SET derniereDeclenchementMinutes = NULL")
    suspend fun reinitialiserDeclenchements()

    @Query("SELECT COUNT(*) FROM tables_aleatoires WHERE worldId = :worldId")
    suspend fun compterPourMonde(worldId: String): Int
}
