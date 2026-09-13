package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitaires pour la gestion des scènes dans MjScenario et la navigation.
 * Ces tests ne nécessitent pas de contexte Android.
 */
class ScenarioSceneNavigationTest {

    @Test
    fun `MjScenario exposes ordered scene list`() {
        val scenario = GameState.MjScenario(
            title = "Test",
            scenes = listOf(
                GameState.MjScene(title = "A", order = 0),
                GameState.MjScene(title = "B", order = 1),
                GameState.MjScene(title = "C", order = 2)
            )
        )
        assertEquals(3, scenario.scenes.size)
        assertEquals("A", scenario.scenes[0].title)
        assertEquals("B", scenario.scenes[1].title)
        assertEquals(2, scenario.scenes[2].order)
    }

    @Test
    fun `MjScene supports music track id`() {
        val scene = GameState.MjScene(
            title = "Taverne",
            musicTrackId = "verres_et_dagues",
            markdownContent = "Contenu"
        )
        assertEquals("verres_et_dagues", scene.musicTrackId)
    }

    @Test
    fun `navigation helper respects bounds`() {
        val scenes = listOf(
            GameState.MjScene(title = "Intro"),
            GameState.MjScene(title = "Milieu"),
            GameState.MjScene(title = "Fin")
        )

        fun goToScene(current: Int, delta: Int): Int? {
            val target = current + delta
            return if (target in scenes.indices) target else null
        }

        assertEquals(1, goToScene(0, 1))
        assertEquals(2, goToScene(1, 1))
        assertNull(goToScene(2, 1))
        assertEquals(1, goToScene(2, -1))
        assertEquals(0, goToScene(1, -1))
        assertNull(goToScene(0, -1))
    }

    @Test
    fun `legacy markdown only scenario maps to single scene`() {
        val scenario = GameState.MjScenario(
            title = "Legacy",
            markdownContent = "# Intro\nTexte",
            scenes = emptyList()
        )
        val displayScenes = scenario.scenes.ifEmpty {
            listOf(GameState.MjScene(title = scenario.title, markdownContent = scenario.markdownContent))
        }
        assertEquals(1, displayScenes.size)
        assertEquals("Legacy", displayScenes.first().title)
    }
}
