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

    /** UPDATE plutôt que REPLACE : un REPLACE supprimerait d'abord la ligne (lieux notables en cascade). */
    @Query("UPDATE points_interet SET fx = :fx, fy = :fy WHERE id = :id")
    suspend fun deplacerPoint(id: String, fx: Float, fy: Float)

    @Query("UPDATE points_interet SET visibleJoueurs = :visible WHERE id = :id")
    suspend fun definirVisibiliteJoueurs(id: String, visible: Boolean)

    @Query("SELECT * FROM cartes_campagne WHERE id = :carteId")
    suspend fun getCarte(carteId: String): CarteCampagneEntity?

    /** Carte historique en premier (id = campagneId), puis les autres par nom. */
    @Query("SELECT * FROM cartes_campagne WHERE campagneId = :campagneId ORDER BY (id != campagneId), nom ASC")
    fun observerCartes(campagneId: String): Flow<List<CarteCampagneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderCarte(carte: CarteCampagneEntity)

    @Query("DELETE FROM cartes_campagne WHERE id = :carteId")
    suspend fun supprimerCarte(carteId: String)

    /** Lieux d'une carte supprimée : conservés, simplement retirés de la carte (non placés). */
    @Query("UPDATE points_interet SET carteId = NULL WHERE carteId = :carteId")
    suspend fun retirerPointsDeCarte(carteId: String)

    /** UPDATE plutôt que REPLACE, comme deplacerPoint (lieux notables en cascade). */
    @Query("UPDATE points_interet SET evenementIds = :evenementIds WHERE id = :id")
    suspend fun definirEvenementsDuPoint(id: String, evenementIds: String)

    @Query("SELECT evenementIds FROM points_interet WHERE id = :id")
    suspend fun getEvenementsDuPoint(id: String): String?

    // Anciens événements de campagne/ville, antérieurs à la bibliothèque (feature_evenement) :
    // lus une seule fois pour être convertis (voir ConversionEvenementsCarte), puis supprimés.
    @Query("SELECT * FROM evenements_aleatoires")
    suspend fun anciensEvenements(): List<EvenementAleatoireEntity>

    @Query("DELETE FROM evenements_aleatoires WHERE id = :id")
    suspend fun supprimerAncienEvenement(id: String)

    @Query("SELECT * FROM lieux_notables WHERE villeId = :villeId ORDER BY nom ASC")
    fun observerLieuxNotables(villeId: String): Flow<List<LieuNotableEntity>>

    /** Lieux notables de toutes les villes de la campagne (partage réseau, menu joueur). */
    @Query("SELECT l.* FROM lieux_notables l INNER JOIN points_interet p ON l.villeId = p.id WHERE p.campagneId = :campagneId ORDER BY l.nom ASC")
    fun observerLieuxNotablesCampagne(campagneId: String): Flow<List<LieuNotableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun sauvegarderLieuNotable(lieu: LieuNotableEntity)

    @Query("DELETE FROM lieux_notables WHERE id = :id")
    suspend fun supprimerLieuNotable(id: String)
}
