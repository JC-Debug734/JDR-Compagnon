package com.jc2.jdrcompagnon.feature_boutique.data

import com.jc2.jdrcompagnon.feature_boutique.data.local.BoutiqueDao
import com.jc2.jdrcompagnon.feature_boutique.data.mapper.toDomain
import com.jc2.jdrcompagnon.feature_boutique.data.mapper.toEntity
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Point de vérité unique pour les boutiques. Le ViewModel ne connaît que cette interface. */
interface BoutiqueRepository {
    fun observerToutesLesBoutiques(): Flow<List<Boutique>>
    suspend fun getBoutique(id: String): Boutique?
    suspend fun sauvegarderBoutique(boutique: Boutique)
    suspend fun supprimerBoutique(id: String)
}

class BoutiqueRepositoryImpl(
    private val dao: BoutiqueDao
) : BoutiqueRepository {

    override fun observerToutesLesBoutiques(): Flow<List<Boutique>> =
        dao.observerToutesLesBoutiques().map { liste -> liste.map { it.toDomain() } }

    override suspend fun getBoutique(id: String): Boutique? =
        dao.getBoutiqueParId(id)?.toDomain()

    override suspend fun sauvegarderBoutique(boutique: Boutique) {
        dao.remplacerBoutiqueComplete(
            boutique = boutique.toEntity(),
            articles = boutique.inventaire.map { it.toEntity(boutique.id) },
            employes = boutique.employes.map { it.toEntity(boutique.id) },
            services = boutique.services.map { it.toEntity(boutique.id) }
        )
    }

    override suspend fun supprimerBoutique(id: String) {
        dao.supprimerBoutique(id)
    }
}