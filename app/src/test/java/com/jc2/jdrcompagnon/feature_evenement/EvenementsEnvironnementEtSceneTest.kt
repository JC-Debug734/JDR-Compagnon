package com.jc2.jdrcompagnon.feature_evenement

import com.jc2.jdrcompagnon.feature_environnement.data.evenementDepuisTexte
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.ui.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvenementsEnvironnementEtSceneTest {

    @Test
    fun `une ancienne rumeur garde son texte en description et son debut en titre`() {
        val texte = "Un dragon rôde dans les hauteurs, disent les bergers."
        val evenement = evenementDepuisTexte("monde", TypeEvenement.RUMEUR_INDICE, texte)
        assertEquals(TypeEvenement.RUMEUR_INDICE, evenement.type)
        assertEquals("Un dragon rôde dans les hauteurs", evenement.titre)
        assertEquals(texte, evenement.description)
    }

    @Test
    fun `un titre trop long est coupe sur un mot`() {
        val texte = "Une meute de loups affamés suit le groupe à distance depuis plusieurs heures sans relâche"
        val titre = evenementDepuisTexte("monde", TypeEvenement.RENCONTRE, texte).titre
        assertTrue(titre.length <= 49)
        assertTrue(titre.endsWith("…"))
        assertTrue(texte.startsWith(titre.removeSuffix("…")))
    }

    @Test
    fun `les evenements d'une scene survivent a l'export markdown`() {
        val scene = GameState.MjScene(title = "Le pont", markdownContent = "Texte", boutiqueId = "b1", evenementIds = listOf("e1", "e2"))
        val markdown = GameState.scenarioToMarkdown(GameState.MjScenario(title = "Test", scenes = listOf(scene)))
        val relue = GameState.parseScenarioMarkdown(markdown).second.single()
        assertEquals(listOf("e1", "e2"), relue.evenementIds)
        assertEquals("b1", relue.boutiqueId)
    }

    @Test
    fun `une ancienne ligne de meta sans evenements reste lisible`() {
        val markdown = """
            # Test

            # SCÈNE — Le pont

            {mscenemeta: music=; environment=env1; table=; loot=; boutique=b1}

            Texte
        """.trimIndent()
        val scene = GameState.parseScenarioMarkdown(markdown).second.single()
        assertEquals("b1", scene.boutiqueId)
        assertEquals("env1", scene.environmentId)
        assertEquals(emptyList<String>(), scene.evenementIds)
    }
}
