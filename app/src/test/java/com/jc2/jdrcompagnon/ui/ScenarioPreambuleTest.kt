package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioPreambuleTest {

    private val fichier = """
        # Ferme Crâne-beurré

        {mlieu: nom=Ferme Crâne-beurré; image=carte-ferme}
        {mpnj: nom=Grand Al Kalazorn; for=16}

        ## Aperçu du lieu

        #perso:[Alfonse Kalazorn] fut autrefois le shérif de #lieu:[Troisangliers].

        ## Objectifs

        - Sauver le Grand Al.

        # SCÈNE — La quête

        {mscenemeta: music=}

        Le panneau des quêtes affiche...
    """.trimIndent()

    @Test
    fun `l'introduction avant la premiere scene est gardee pour le MJ, sans titre ni lignes techniques`() {
        val preambule = GameState.preambuleScenario(fichier)
        assertTrue(preambule.startsWith("## Aperçu du lieu"))
        assertTrue("## Objectifs" in preambule)
        assertFalse("{mpnj" in preambule)
        assertFalse("{mlieu" in preambule)
        assertFalse("# Ferme" in preambule.lines().first())
        assertFalse("SCÈNE" in preambule)
    }

    @Test
    fun `l'introduction survit a la reecriture du fichier par l'application`() {
        val (titre, scenes) = GameState.parseScenarioMarkdown(fichier)
        val scenario = GameState.MjScenario(title = titre, description = GameState.preambuleScenario(fichier), scenes = scenes)
        val reecrit = GameState.scenarioToMarkdown(scenario)
        assertEquals(scenario.description, GameState.preambuleScenario(reecrit))
        assertEquals(1, GameState.parseScenarioMarkdown(reecrit).second.size)
    }

    @Test
    fun `sans scene, pas d'introduction separee`() {
        assertEquals("", GameState.preambuleScenario("# Titre\n\nTexte libre."))
    }
}
