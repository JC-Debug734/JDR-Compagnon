package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioImportTest {

    private val markdown = """
        # Ferme test

        {mlieu: nom=La ferme; image=carte}
        {mpnj: nom=Grand Al; race=Humain; for=16; dex=13; pvmax=58; pv=9; ca=11; competences=Athlétisme, Perception; capacites=Ligne 1\nLigne 2; objectif=Garder sa ferme\; tuer les orcs}

        # SCÈNE — Arrivée

        {mscenemeta: music=}

        {image:carte}

        {lire}
        Texte à lire.
        {/lire}

        {mimage: nom=carte; data=QUJD}
    """.trimIndent()

    @Test
    fun `les lignes d'en-tete sont extraites et absentes du texte des scenes`() {
        val extras = ScenarioImport.extraireExtras(markdown)
        assertEquals(mapOf("carte" to "QUJD"), extras.images)
        assertEquals("La ferme", extras.lieuNom)
        assertEquals("carte", extras.lieuImage)
        assertEquals(1, extras.pnjs.size)

        val (_, scenes) = GameState.parseScenarioMarkdown(markdown)
        val contenu = scenes.single().markdownContent
        assertFalse(contenu.contains("{mimage:"))
        assertFalse(contenu.contains("{mpnj:"))
        assertTrue(contenu.contains("{image:carte}"))
        assertTrue(contenu.contains("{lire}"))
    }

    @Test
    fun `les zones d'exploration du fichier sont lues et converties en cases`() {
        val md = markdown + "\n{mzone: image=carte; colonnes=4; nom=Coin; rects=0,0,50,50|80,80,20,20}"
        val zone = ScenarioImport.extraireExtras(md).zones.single()
        assertEquals("carte", zone.image)
        assertEquals("Coin", zone.nom)
        assertEquals(2, zone.rectangles.size)
        val (colonnes, zones) = ScenarioImport.zonesPourImage(listOf(zone), 100, 100)
        assertEquals(4, colonnes)
        assertEquals(setOf(0, 1, 4, 5, 15), zones.single().cases)
        // La ligne {mzone:} n'apparaît pas dans le texte des scènes.
        assertFalse(GameState.parseScenarioMarkdown(md).second.single().markdownContent.contains("{mzone:"))
    }

    @Test
    fun `les images sont remplacees par leur fichier stocke`() {
        val contenu = "Avant\n{image:carte}\n{image:inconnue}\nAprès"
        val resultat = ScenarioImport.remplacerImages(contenu, mapOf("carte" to "abc_carte.img"))
        assertEquals("Avant\n{image:abc_carte.img}\n{image:inconnue}\nAprès", resultat)
    }

    @Test
    fun `la ligne mpnj donne une fiche PNJ complete`() {
        val fiche = ScenarioImport.pnjVersCharacter(ScenarioImport.extraireExtras(markdown).pnjs.single(), "monde")
        assertEquals("Grand Al", fiche.name)
        assertEquals("PNJ", fiche.type)
        assertEquals("monde", fiche.worldId)
        assertEquals(16, fiche.strength)
        assertEquals(58, fiche.maxHitPoints)
        assertEquals(9, fiche.currentHitPoints)
        assertEquals(11, fiche.armorClass)
        assertEquals(listOf("Athlétisme", "Perception"), fiche.skillProficiencies)
        assertEquals("Ligne 1\nLigne 2", fiche.classFeatures)
        assertEquals("Garder sa ferme; tuer les orcs", fiche.objectif)
    }
}
