package com.jc2.jdrcompagnon.feature_carte.data

import com.jc2.jdrcompagnon.feature_carte.data.local.CarteDao
import com.jc2.jdrcompagnon.feature_carte.data.mapper.toDomain
import com.jc2.jdrcompagnon.feature_carte.data.mapper.toEntity
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CarteRepository {
    fun observerPoints(campagneId: String): Flow<List<PointInteret>>
    suspend fun sauvegarderPoint(point: PointInteret)
    suspend fun supprimerPoint(id: String)
    suspend fun getCarte(campagneId: String): CarteCampagne
    suspend fun sauvegarderCarte(carte: CarteCampagne)
    fun observerEvenementsCustom(campagneId: String): Flow<List<EvenementAleatoire>>
    fun observerEvenementsDeVille(villeId: String): Flow<List<EvenementAleatoire>>
    suspend fun sauvegarderEvenement(evenement: EvenementAleatoire)
    suspend fun supprimerEvenement(id: String)
    fun observerLieuxNotables(villeId: String): Flow<List<LieuNotable>>
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

    override suspend fun getCarte(campagneId: String): CarteCampagne =
        dao.getCarte(campagneId)?.toDomain() ?: CarteCampagne(campagneId = campagneId)

    override suspend fun sauvegarderCarte(carte: CarteCampagne) {
        dao.sauvegarderCarte(carte.toEntity())
    }

    override fun observerEvenementsCustom(campagneId: String): Flow<List<EvenementAleatoire>> =
        dao.observerEvenementsCustom(campagneId).map { liste -> liste.map { it.toDomain() } }

    override fun observerEvenementsDeVille(villeId: String): Flow<List<EvenementAleatoire>> =
        dao.observerEvenementsDeVille(villeId).map { liste -> liste.map { it.toDomain() } }

    override suspend fun sauvegarderEvenement(evenement: EvenementAleatoire) {
        dao.sauvegarderEvenement(evenement.toEntity())
    }

    override suspend fun supprimerEvenement(id: String) {
        dao.supprimerEvenement(id)
    }

    override fun observerLieuxNotables(villeId: String): Flow<List<LieuNotable>> =
        dao.observerLieuxNotables(villeId).map { liste -> liste.map { it.toDomain() } }

    override suspend fun sauvegarderLieuNotable(lieu: LieuNotable) {
        dao.sauvegarderLieuNotable(lieu.toEntity())
    }

    override suspend fun supprimerLieuNotable(id: String) {
        dao.supprimerLieuNotable(id)
    }
}
