package com.jc2.jdrcompagnon.feature_evenement

import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepositoryImpl
import com.jc2.jdrcompagnon.feature_evenement.data.local.EvenementDao
import com.jc2.jdrcompagnon.feature_evenement.data.local.EvenementEntity
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvenementRepositoryTest {

    private class FakeDao : EvenementDao {
        val lignes = MutableStateFlow<Map<String, EvenementEntity>>(emptyMap())
        override fun observerEvenements(worldId: String): Flow<List<EvenementEntity>> =
            lignes.map { it.values.filter { e -> e.worldId == worldId } }
        override suspend fun getParId(id: String) = lignes.value[id]
        override suspend fun getParIds(ids: List<String>) = ids.mapNotNull { lignes.value[it] }
        override suspend fun sauvegarder(evenement: EvenementEntity) {
            lignes.value = lignes.value + (evenement.id to evenement)
        }
        override suspend fun sauvegarderTous(evenements: List<EvenementEntity>) = evenements.forEach { sauvegarder(it) }
        override suspend fun supprimer(id: String) {
            lignes.value = lignes.value - id
        }
        override suspend fun compterPourMonde(worldId: String) = lignes.value.values.count { it.worldId == worldId }
    }

    @Test
    fun `objectif et issues avec leurs effets survivent a l'aller-retour en base`() = runBlocking {
        val repository = EvenementRepositoryImpl(FakeDao())
        val evenement = Evenement(
            worldId = "w",
            type = TypeEvenement.OPPORTUNITE,
            titre = "Aide à un PNJ",
            objectif = "Force (Athlétisme) DD 12",
            issues = listOf(
                IssueEvenement(
                    nature = NatureIssue.REUSSITE,
                    description = "Il offre le gîte.",
                    effets = listOf(EffetEvenement(categorie = CategorieEffet.REPUTATION, quantite = 5, cible = "Habitants locaux"))
                ),
                IssueEvenement(nature = NatureIssue.ECHEC, titre = "Trop tard", description = "La roue casse.")
            )
        )
        repository.sauvegarder(evenement)
        assertEquals(evenement, repository.getEvenement(evenement.id))
    }

    @Test
    fun `un evenement d'avant les issues se lit sans objectif ni issue`() = runBlocking {
        val dao = FakeDao()
        dao.sauvegarder(EvenementEntity(id = "old", worldId = "w", type = "PERIL", titre = "Éboulement"))
        val lu = EvenementRepositoryImpl(dao).getEvenement("old")!!
        assertEquals("", lu.objectif)
        assertTrue(lu.issues.isEmpty())
    }

    @Test
    fun `une issue sans titre affiche le libelle de sa nature`() {
        assertEquals("Réussite partielle", IssueEvenement(nature = NatureIssue.PARTIELLE).libelle)
        assertEquals("Trop tard", IssueEvenement(nature = NatureIssue.ECHEC, titre = "Trop tard").libelle)
    }
}
