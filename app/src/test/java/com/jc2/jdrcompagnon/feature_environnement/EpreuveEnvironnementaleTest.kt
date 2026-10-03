package com.jc2.jdrcompagnon.feature_environnement

import com.jc2.jdrcompagnon.feature_environnement.data.EnvironnementRepositoryImpl
import com.jc2.jdrcompagnon.feature_evenement.FakeEvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_environnement.data.local.EnvironnementDao
import com.jc2.jdrcompagnon.feature_environnement.data.local.EnvironnementEntity
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.domain.usecase.catalogueEpreuves
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import com.jc2.jdrcompagnon.feature_environnement.domain.model.CapaciteEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DifficulteRelative
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DureeEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EchelleEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import com.jc2.jdrcompagnon.feature_environnement.domain.model.GraviteDegats
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeCapacite
import com.jc2.jdrcompagnon.feature_environnement.presentation.EpreuveSession
import com.jc2.jdrcompagnon.feature_environnement.presentation.IssueEpreuve
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpreuveEnvironnementaleTest {

    private val epreuve = EpreuveEnvironnementale(
        nom = "Test",
        menaceMax = 3,
        capacites = listOf(
            CapaciteEpreuve("Chute", TypeCapacite.REACTION, "", sauvegarde = "DEX", difficulte = DifficulteRelative.MOYEN, degats = GraviteDegats.DANGEREUX, typeDegats = "contondant")
        )
    )

    @After
    fun tearDown() = EpreuveSession.fermer()

    @Test
    fun `tier suit les paliers officiels`() {
        assertEquals(listOf(1, 1, 2, 2, 3, 3, 4, 4), listOf(1, 4, 5, 10, 11, 16, 17, 20).map { EchelleEpreuve.tier(it) })
    }

    @Test
    fun `DD et degats montent avec le tier`() {
        assertEquals(13, EchelleEpreuve.dd(1, DifficulteRelative.MOYEN))
        assertEquals(25, EchelleEpreuve.dd(4, DifficulteRelative.EXTREME))
        assertEquals("JS DEX DD 15 · 4d10 contondant", EchelleEpreuve.resoudre(epreuve.capacites.single(), 2))
    }

    @Test
    fun `progres proportionnel au nombre de joueurs`() {
        assertEquals(6, EchelleEpreuve.progresMax(DureeEpreuve.COURTE, 4))
        assertEquals(8, EchelleEpreuve.progresMax(DureeEpreuve.STANDARD, 4))
        assertEquals(12, EchelleEpreuve.progresMax(DureeEpreuve.LONGUE, 5))
    }

    @Test
    fun `echecs epuisent la menace et ouvrent une reaction`() {
        EpreuveSession.demarrer(epreuve, niveau = 3, nbJoueurs = 4, niveauAuto = false)
        EpreuveSession.echec()
        val apresEchec = EpreuveSession.etat.value!!
        assertEquals(2, apresEchec.menace)
        assertTrue(apresEchec.echecEnAttente)

        EpreuveSession.declencher(epreuve.capacites.single())
        assertFalse(EpreuveSession.etat.value!!.echecEnAttente)

        EpreuveSession.echec(critique = true)
        assertEquals(IssueEpreuve.ECHEC, EpreuveSession.etat.value!!.issue)
    }

    @Test
    fun `progres complet donne la reussite et fige l'epreuve`() {
        EpreuveSession.demarrer(epreuve, niveau = 1, nbJoueurs = 1, niveauAuto = false, duree = DureeEpreuve.COURTE)
        EpreuveSession.reussite(critique = true)
        EpreuveSession.reussite()
        val fin = EpreuveSession.etat.value!!
        assertEquals(IssueEpreuve.REUSSITE, fin.issue)
        EpreuveSession.echec()
        assertEquals(fin, EpreuveSession.etat.value)
    }

    /** DAO en mémoire : vérifie le mapping réel du repository (colonne epreuvesJson). */
    private class FakeDao : EnvironnementDao {
        val lignes = MutableStateFlow<Map<String, EnvironnementEntity>>(emptyMap())
        override fun observerEnvironnements(worldId: String): Flow<List<EnvironnementEntity>> =
            lignes.map { it.values.filter { e -> e.worldId == worldId } }
        override suspend fun getEnvironnementParId(id: String) = lignes.value[id]
        override suspend fun sauvegarder(environnement: EnvironnementEntity) {
            lignes.value = lignes.value + (environnement.id to environnement)
        }
        override suspend fun supprimer(id: String) {
            lignes.value = lignes.value - id
        }
        override suspend fun compterPourMonde(worldId: String) = lignes.value.values.count { it.worldId == worldId }
    }

    @Test
    fun `epreuves d'un environnement survivent a l'aller-retour en base`() = runBlocking {
        val dao = FakeDao()
        val repository = EnvironnementRepositoryImpl(dao, FakeEvenementRepository())
        repository.sauvegarder(Environnement(id = "env", nom = "Forêt", worldId = "w", epreuves = listOf(epreuve)))
        assertEquals(listOf(epreuve), repository.getEnvironnementParId("env")!!.epreuves)
    }

    @Test
    fun `environnement d'avant la migration n'a aucune epreuve`() = runBlocking {
        val dao = FakeDao()
        dao.sauvegarder(EnvironnementEntity(id = "old", nom = "Ancien", worldId = "w"))
        assertTrue(EnvironnementRepositoryImpl(dao, FakeEvenementRepository()).getEnvironnementParId("old")!!.epreuves.isEmpty())
    }

    @Test
    fun `anciennes rumeurs et rencontres deviennent des evenements lies une seule fois`() = runBlocking {
        val dao = FakeDao()
        val bibliotheque = FakeEvenementRepository()
        dao.sauvegarder(
            EnvironnementEntity(
                id = "old", nom = "Forêt", worldId = "w",
                rumeursJson = """["Un dragon rôde."]""",
                rencontresJson = """["Des loups.", "Un druide."]"""
            )
        )
        val repository = EnvironnementRepositoryImpl(dao, bibliotheque)

        val converti = repository.getEnvironnementParId("old")!!
        assertEquals(3, converti.evenementIds.size)
        val types = converti.evenementIds.map { bibliotheque.evenements.value.getValue(it).type }
        assertEquals(listOf(TypeEvenement.RUMEUR_INDICE, TypeEvenement.RENCONTRE, TypeEvenement.RENCONTRE), types)
        assertEquals("[]", dao.lignes.value.getValue("old").rumeursJson)

        // Relire ne doit rien recréer.
        repository.getEnvironnementParId("old")
        assertEquals(3, bibliotheque.evenements.value.size)
    }

    @Test
    fun `catalogue contient les huit epreuves avec des noms uniques`() {
        assertEquals(8, catalogueEpreuves.size)
        assertEquals(8, catalogueEpreuves.map { it.nom }.toSet().size)
    }
}
