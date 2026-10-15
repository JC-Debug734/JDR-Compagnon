package com.jc2.jdrcompagnon.feature_epreuve

import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.DifficulteEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.GroupeEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ReglesEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpreuveOutilTest {

    private val sables = ComplicationEpreuve("Sables mouvants")
    private val sangsues = ComplicationEpreuve("Sangsues")
    private val marais = Epreuve(nom = "Marais", reussitesRequises = 2, complications = listOf(sables, sangsues))

    @After
    fun nettoyer() = EpreuveOutilSession.fermer()

    @Test
    fun `le DD varie de -2 a +2 au lancement et a chaque reussite`() {
        val froid = Epreuve(nom = "Froid", reussitesRequises = 20, description = "DD {DD}", difficulte = DifficulteEpreuve.MOYENNE)
        val ddNormal = ReglesEpreuve.ddPour(DifficulteEpreuve.MOYENNE, 1)
        val variations = mutableSetOf<Int>()
        repeat(30) { graine ->
            val random = Random(graine)
            EpreuveOutilSession.demarrer(froid, random)
            repeat(5) {
                val etat = EpreuveOutilSession.etat.value!!
                assertTrue(etat.variationDd in ReglesEpreuve.PLAGE_VARIATION_DD)
                // Le DD de la description suit l'écart tiré.
                assertEquals("DD ${ReglesEpreuve.ddPour(DifficulteEpreuve.MOYENNE, etat.groupe.niveau, etat.variationDd)}", etat.epreuve.description)
                variations += etat.variationDd
                EpreuveOutilSession.reussite(null, null, random)
            }
        }
        assertEquals(ReglesEpreuve.PLAGE_VARIATION_DD.toSet(), variations)
        assertEquals(ddNormal - 2, ReglesEpreuve.ddPour(DifficulteEpreuve.MOYENNE, 1, -2))
    }

    @Test
    fun `aucune complication si la liste est vide`() {
        assertNull(ReglesEpreuve.tirerComplication(emptyList(), null))
        assertNull(ReglesEpreuve.tirerComplication(listOf(ComplicationEpreuve("  ")), null))
    }

    @Test
    fun `le tirage evite de repeter la complication precedente`() {
        repeat(50) { graine ->
            assertEquals(sangsues, ReglesEpreuve.tirerComplication(listOf(sables, sangsues), sables, Random(graine)))
        }
    }

    @Test
    fun `une seule complication peut se repeter`() {
        assertEquals(sables, ReglesEpreuve.tirerComplication(listOf(sables), sables))
    }

    @Test
    fun `chaque echec avant le dernier tire une complication`() {
        EpreuveOutilSession.demarrer(marais.copy(echecsMax = 6))
        repeat(5) { EpreuveOutilSession.echec("Aria", "saute", Random(it)) }
        val etat = EpreuveOutilSession.etat.value!!
        assertEquals(5, etat.echecs)
        assertTrue(etat.tentatives.all { it.complication != null })
        assertEquals(5, etat.numeroComplication)
        assertFalse(etat.terminee)
        // Jamais deux fois la même d'affilée quand il y a le choix.
        etat.tentatives.zipWithNext().forEach { (a, b) -> assertNotEquals(a.complication, b.complication) }
    }

    @Test
    fun `l'epreuve se termine quand les reussites requises sont atteintes`() {
        EpreuveOutilSession.demarrer(marais)
        EpreuveOutilSession.reussite("Aria", "  ")
        assertFalse(EpreuveOutilSession.etat.value!!.terminee)
        assertNull(EpreuveOutilSession.etat.value!!.tentatives.last().action)
        EpreuveOutilSession.reussite(null, null)
        val etat = EpreuveOutilSession.etat.value!!
        assertTrue(etat.terminee)
        assertTrue(etat.reussie)
        // Plus aucune tentative n'est comptée une fois l'épreuve terminée.
        EpreuveOutilSession.echec(null, null)
        assertEquals(0, EpreuveOutilSession.etat.value!!.echecs)
    }

    @Test
    fun `trop d'echecs font perdre l'epreuve sans complication finale`() {
        EpreuveOutilSession.demarrer(marais.copy(echecsMax = 2, issueEchec = "Le tertre surgit", combatEchec = "Tertre errant x1"))
        EpreuveOutilSession.echec(null, null, Random(1))
        assertFalse(EpreuveOutilSession.etat.value!!.terminee)
        EpreuveOutilSession.echec(null, null, Random(2))
        val etat = EpreuveOutilSession.etat.value!!
        assertTrue(etat.terminee)
        assertTrue(etat.echouee)
        assertFalse(etat.reussie)
        assertNull(etat.tentatives.last().complication)
        assertEquals(1, etat.numeroComplication)
        assertEquals("Tertre errant x1", etat.epreuve.combatEchec)
        // Annuler le dernier échec rouvre l'épreuve.
        EpreuveOutilSession.annulerDerniere()
        assertFalse(EpreuveOutilSession.etat.value!!.terminee)
        assertFalse(EpreuveOutilSession.etat.value!!.echouee)
    }

    @Test
    fun `annuler la derniere tentative rouvre l'epreuve`() {
        EpreuveOutilSession.demarrer(marais)
        EpreuveOutilSession.reussite(null, null)
        EpreuveOutilSession.reussite(null, null)
        EpreuveOutilSession.annulerDerniere()
        val etat = EpreuveOutilSession.etat.value!!
        assertFalse(etat.terminee)
        assertEquals(1, etat.reussites)
    }

    @Test
    fun `relancer change la complication du dernier echec`() {
        EpreuveOutilSession.demarrer(marais)
        EpreuveOutilSession.echec(null, null, Random(1))
        val premiere = EpreuveOutilSession.etat.value!!.derniereComplication
        assertNotNull(premiere)
        EpreuveOutilSession.relancerComplication(Random(2))
        val etat = EpreuveOutilSession.etat.value!!
        assertEquals(1, etat.echecs)
        assertNotEquals(premiere, etat.derniereComplication)
        assertEquals(2, etat.numeroComplication)
    }

    @Test
    fun `les reussites suivent le nombre de joueurs et la difficulte`() {
        assertEquals(4, ReglesEpreuve.reussitesPour(DifficulteEpreuve.MOYENNE, 4))
        assertEquals(6, ReglesEpreuve.reussitesPour(DifficulteEpreuve.DIFFICILE, 4))
        assertEquals(8, ReglesEpreuve.reussitesPour(DifficulteEpreuve.DIFFICILE, 5))
        assertEquals(2, ReglesEpreuve.reussitesPour(DifficulteEpreuve.FACILE, 1))
    }

    @Test
    fun `les echecs toleres diminuent en proportion avec la difficulte`() {
        assertEquals(3, ReglesEpreuve.echecsPour(DifficulteEpreuve.FACILE, 4))
        assertEquals(4, ReglesEpreuve.echecsPour(DifficulteEpreuve.MOYENNE, 4))
        assertEquals(4, ReglesEpreuve.echecsPour(DifficulteEpreuve.DIFFICILE, 4))
        assertEquals(5, ReglesEpreuve.echecsPour(DifficulteEpreuve.MORTELLE, 4))
        assertEquals(6, ReglesEpreuve.echecsPour(DifficulteEpreuve.MORTELLE, 5))
        assertEquals(2, ReglesEpreuve.echecsPour(DifficulteEpreuve.FACILE, 1))
        DifficulteEpreuve.entries.zipWithNext().forEach { (a, b) -> assertTrue(a.echecsParReussite > b.echecsParReussite) }
    }

    @Test
    fun `les variables sont remplacees selon le niveau du groupe`() {
        val col = Epreuve(
            nom = "Col",
            difficulte = DifficulteEpreuve.DIFFICILE,
            complications = listOf(ComplicationEpreuve("Froid", "JS DD {DD} ou {degats}, attaque {attaque}, DD {DD+2}, {po} po")),
            issueEchec = "Chute : {degats_lourd}",
        )
        val niv1 = ReglesEpreuve.adapter(col, GroupeEpreuve(joueurs = 3, niveau = 1))
        assertEquals(5, niv1.reussitesRequises)
        assertEquals(4, niv1.echecsMax)
        assertEquals("JS DD 14 ou 2d10, attaque +6, DD 16, 75 po", niv1.complications.single().description)
        assertEquals("Chute : 4d10", niv1.issueEchec)
        val niv9 = ReglesEpreuve.adapter(col, GroupeEpreuve(joueurs = 4, niveau = 9))
        assertEquals(6, niv9.reussitesRequises)
        assertEquals("JS DD 16 ou 4d10, attaque +8, DD 18, 675 po", niv9.complications.single().description)
    }

    @Test
    fun `une epreuve sans difficulte garde ses reussites fixes`() {
        assertEquals(2, ReglesEpreuve.adapter(marais, GroupeEpreuve(joueurs = 6, niveau = 12)).reussitesRequises)
        assertEquals(3, ReglesEpreuve.adapter(marais, GroupeEpreuve(joueurs = 6, niveau = 12)).echecsMax)
    }
}
