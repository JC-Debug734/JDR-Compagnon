package com.jc2.jdrcompagnon.feature_carte.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarteDao {

    @Query("SELECT * FROM points_interet WHERE campagneId = :campagneId ORDER BY nom ASC")
    fun observerPoints(campagneId: String): Flow<List<PointInteretEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderPoint(point: PointInteretEntity)

    @Query("DELETE FROM points_interet WHERE id = :id")
    suspend fun supprimerPoint(id: String)

    @Query("SELECT * FROM cartes_campagne WHERE campagneId = :campagneId")
    suspend fun getCarte(campagneId: String): CarteCampagneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderCarte(carte: CarteCampagneEntity)

    @Query("SELECT * FROM evenements_aleatoires WHERE campagneId = :campagneId ORDER BY titre ASC")
    fun observerEvenementsCustom(campagneId: String): Flow<List<EvenementAleatoireEntity>>

    @Query("SELECT * FROM evenements_aleatoires WHERE villeId = :villeId ORDER BY titre ASC")
    fun observerEvenementsDeVille(villeId: String): Flow<List<EvenementAleatoireEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderEvenement(evenement: EvenementAleatoireEntity)

    @Query("DELETE FROM evenements_aleatoires WHERE id = :id")
    suspend fun supprimerEvenement(id: String)

    @Query("SELECT * FROM lieux_notables WHERE villeId = :villeId ORDER BY nom ASC")
    fun observerLieuxNotables(villeId: String): Flow<List<LieuNotableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderLieuNotable(lieu: LieuNotableEntity)

    @Query("DELETE FROM lieux_notables WHERE id = :id")
    suspend fun supprimerLieuNotable(id: String)
}
