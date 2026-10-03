package com.jc2.jdrcompagnon.feature_carte.data

import com.jc2.jdrcompagnon.feature_carte.data.local.CarteDao
import com.jc2.jdrcompagnon.feature_carte.data.mapper.toDomain
import com.jc2.jdrcompagnon.feature_carte.data.mapper.toEntity
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CarteRepository {
    fun observerPoints(campagneId: String): Flow<List<PointInteret>>
    suspend fun sauvegarderPoint(point: PointInteret)
    suspend fun supprimerPoint(id: String)
    /** Position libre du centre de l'icône, en fraction de la carte (voir PointInteret.fx). */
    suspend fun deplacerPoint(id: String, fx: Float, fy: Float)
    suspend fun definirVisibiliteJoueurs(id: String, visible: Boolean)
    /** [carteId] null = carte principale de la campagne (créée avec des valeurs par défaut si absente). */
    suspend fun getCarte(campagneId: String, carteId: String? = null): CarteCampagne
    fun observerCartes(campagneId: String): Flow<List<CarteCampagne>>
    suspend fun sauvegarderCarte(carte: CarteCampagne)
    suspend fun supprimerCarte(carte: CarteCampagne)
    /** Événements de la bibliothèque (feature_evenement) rattachés au lieu, par id. */
    suspend fun definirEvenementsDuPoint(id: String, evenementIds: List<String>)
    fun observerLieuxNotables(villeId: String): Flow<List<LieuNotable>>
    fun observerLieuxNotablesCampagne(campagneId: String): Flow<List<LieuNotable>>
    suspend fun sauvegarderLieuNotable(lieu: LieuNotable)
    suspend fun supprimerLieuNotable(id: String)
}

class CarteRepositoryImpl(private val dao: CarteDao) : CarteRepository {

    override fun observerPoints(campagneId: String): Flow<List<PointInteret>> =
        dao.observerPoints(campagneId).map { liste -> liste.map { it.toDomain() } }

    override suspend fun sauvegarderPoint(point: PointInteret) {
        dao.sauvegarderPoint(point.toEntity())
    }

    override suspend fun supprimerPoint(id: String) {
        dao.supprimerPoint(id)
    }

    override suspend fun deplacerPoint(id: String, fx: Float, fy: Float) {
        dao.deplacerPoint(id, fx, fy)
    }

    override suspend fun definirVisibiliteJoueurs(id: String, visible: Boolean) {
        dao.definirVisibiliteJoueurs(id, visible)
    }

    override suspend fun getCarte(campagneId: String, carteId: String?): CarteCampagne {
        val id = carteId ?: campagneId
        return dao.getCarte(id)?.toDomain() ?: CarteCampagne(campagneId = campagneId, id = id)
    }

    override fun observerCartes(campagneId: String): Flow<List<CarteCampagne>> =
        dao.observerCartes(campagneId).map { liste -> liste.map { it.toDomain() } }

    override suspend fun sauvegarderCarte(carte: CarteCampagne) {
        dao.sauvegarderCarte(carte.toEntity())
    }

    override suspend fun supprimerCarte(carte: CarteCampagne) {
        // Aucun lieu n'est perdu : ils redeviennent "non placés" et restent listés dans la
        // campagne (cartes Villes / Lieux non placés), prêts à être placés sur une autre carte.
        dao.retirerPointsDeCarte(carte.id)
        dao.supprimerCarte(carte.id)
    }

    override suspend fun definirEvenementsDuPoint(id: String, evenementIds: List<String>) {
        dao.definirEvenementsDuPoint(id, evenementIds.distinct().joinToString(","))
    }

    override fun observerLieuxNotables(villeId: String): Flow<List<LieuNotable>> =
        dao.observerLieuxNotables(villeId).map { liste -> liste.map { it.toDomain() } }

    override fun observerLieuxNotablesCampagne(campagneId: String): Flow<List<LieuNotable>> =
        dao.observerLieuxNotablesCampagne(campagneId).map { liste -> liste.map { it.toDomain() } }

    override suspend fun sauvegarderLieuNotable(lieu: LieuNotable) {
        dao.sauvegarderLieuNotable(lieu.toEntity())
    }

    override suspend fun supprimerLieuNotable(id: String) {
        dao.supprimerLieuNotable(id)
    }
}
