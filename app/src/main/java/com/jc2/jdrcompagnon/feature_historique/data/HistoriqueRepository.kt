package com.jc2.jdrcompagnon.feature_historique.data

import com.jc2.jdrcompagnon.feature_historique.data.local.HistoriqueDao
import com.jc2.jdrcompagnon.feature_historique.data.local.HistoriqueEntreeEntity
import com.jc2.jdrcompagnon.feature_historique.domain.TypeEvenementHistorique
import kotlinx.coroutines.flow.Flow

interface HistoriqueRepository {
    suspend fun ajouterEntree(
        personnageId: String,
        type: TypeEvenementHistorique,
        description: String,
        timestamp: Long = System.currentTimeMillis()
    )
    fun observerHistorique(personnageId: String): Flow<List<HistoriqueEntreeEntity>>
    suspend fun supprimerHistoriqueDePersonnage(personnageId: String)
}

class HistoriqueRepositoryImpl(private val dao: HistoriqueDao) : HistoriqueRepository {

    override suspend fun ajouterEntree(personnageId: String, type: TypeEvenementHistorique, description: String, timestamp: Long) {
        dao.inserer(
            HistoriqueEntreeEntity(
                personnageId = personnageId,
                timestamp = timestamp,
                type = type.name,
                description = description
            )
        )
    }

    override fun observerHistorique(personnageId: String): Flow<List<HistoriqueEntreeEntity>> =
        dao.observerHistorique(personnageId)

    override suspend fun supprimerHistoriqueDePersonnage(personnageId: String) {
        dao.supprimerHistoriqueDePersonnage(personnageId)
    }
}
