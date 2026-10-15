package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapitresScenariosTest {

    private fun scenario(titre: String, chapitre: Int? = null, titreChapitre: String = "", numero: Int? = null) =
        GameState.MjScenario(title = titre, chapitreNumero = chapitre, chapitreTitre = titreChapitre, numero = numero)

    @Test
    fun `la ligne mchapitre est lue et reecrite a l'identique`() {
        val md = """
            # Trahison à Grandcolline
            {mchapitre: numero=3; titre=Quand la maison brûle; scenario=3}

            # SCÈNE — La parade
        """.trimIndent()
        val c = ChapitresScenarios.lire(md)!!
        assertEquals(3, c.numero)
        assertEquals("Quand la maison brûle", c.titre)
        assertEquals(3, c.scenario)
        val s = scenario("Trahison à Grandcolline", 3, "Quand la maison brûle", 3)
        assertEquals(c, ChapitresScenarios.lire("# x\n" + ChapitresScenarios.ligne(s)))
    }

    @Test
    fun `un scenario sans chapitre n'ecrit pas de ligne`() {
        assertNull(ChapitresScenarios.ligne(scenario("Libre")))
        assertNull(ChapitresScenarios.lire("# Libre\n\nTexte"))
    }

    @Test
    fun `le titre du chapitre echappe les points-virgules`() {
        val s = scenario("A", 1, "Ombres; et lumières", 1)
        assertEquals("Ombres; et lumières", ChapitresScenarios.lire(ChapitresScenarios.ligne(s)!!)!!.titre)
    }

    @Test
    fun `les scenarios sont ranges par chapitre puis par numero, sans chapitre en dernier`() {
        val liste = listOf(
            scenario("Libre"),
            scenario("Invasion", 3, "Quand la maison brûle", 5),
            scenario("Silence brisé", 2, "Prélude à la guerre", 3),
            scenario("Derniers adieux", 3, "Quand la maison brûle", 1),
            scenario("Écailles", 2, "Prélude à la guerre", 1),
        )
        val groupes = ChapitresScenarios.grouper(liste)
        assertEquals(listOf("Chapitre 2 — Prélude à la guerre", "Chapitre 3 — Quand la maison brûle", null), groupes.map { it.first })
        assertEquals(listOf("Écailles", "Silence brisé", "Derniers adieux", "Invasion", "Libre"), ChapitresScenarios.trier(liste).map { it.title })
        assertTrue(ChapitresScenarios.aDesChapitres(liste))
        assertFalse(ChapitresScenarios.aDesChapitres(listOf(scenario("Libre"))))
    }

    @Test
    fun `le numero affiche precede le titre une seule fois`() {
        assertEquals("3.2 · Fête", ChapitresScenarios.titreNumerote(scenario("Fête", 3, "", 2)))
        assertEquals("3.2 — Fête", ChapitresScenarios.titreNumerote(scenario("3.2 — Fête", 3, "", 2)))
        assertEquals("Fête", ChapitresScenarios.titreNumerote(scenario("Fête")))
    }
}
