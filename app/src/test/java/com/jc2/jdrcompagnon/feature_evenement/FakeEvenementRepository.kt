package com.jc2.jdrcompagnon.feature_evenement

import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Bibliothèque d'événements en mémoire, pour les tests des repositories qui la référencent. */
class FakeEvenementRepository : EvenementRepository {
    val evenements = MutableStateFlow<Map<String, Evenement>>(emptyMap())

    override fun observerEvenements(worldId: String): Flow<List<Evenement>> =
        evenements.map { it.values.filter { e -> e.worldId == worldId } }

    override suspend fun getEvenement(id: String) = evenements.value[id]

    override suspend fun getEvenements(ids: List<String>) = ids.mapNotNull { evenements.value[it] }

    override suspend fun sauvegarder(evenement: Evenement) {
        evenements.value = evenements.value + (evenement.id to evenement)
    }

    override suspend fun sauvegarderTous(evenements: List<Evenement>) = evenements.forEach { sauvegarder(it) }

    override suspend fun supprimer(id: String) {
        evenements.value = evenements.value - id
    }

    override suspend fun compterPourMonde(worldId: String) = evenements.value.values.count { it.worldId == worldId }
}
