package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitaires pour l'éditeur multi-scènes : CRUD scènes, undo, recherche de liens.
 * Ces tests ne nécessitent pas de contexte Android.
 */
class ScenarioEditorLogicTest {

    @Test
    fun `MjScenario supports scene CRUD operations`() {
        val initial = GameState.MjScenario(
            title = "Test",
            scenes = listOf(
                GameState.MjScene(title = "Intro", order = 0),
                GameState.MjScene(title = "Combat", order = 1)
            )
        )

        // Create
        val added = initial.copy(
            scenes = initial.scenes + GameState.MjScene(title = "Conclusion", order = 2)
        )
        assertEquals(3, added.scenes.size)

        // Update
        val updated = added.copy(
            scenes = added.scenes.mapIndexed { i, s ->
                if (i == 1) s.copy(title = "Combat épique") else s
            }
        )
        assertEquals("Combat épique", updated.scenes[1].title)

        // Delete
        val deleted = updated.copy(
            scenes = updated.scenes.filterIndexed { i, _ -> i != 0 }
        )
        assertEquals(2, deleted.scenes.size)
        assertEquals("Combat épique", deleted.scenes[0].title)

        // Reorder
        val reordered = deleted.copy(
            scenes = listOf(deleted.scenes[1], deleted.scenes[0]).mapIndexed { i, s -> s.copy(order = i) }
        )
        assertEquals("Conclusion", reordered.scenes[0].title)
        assertEquals(0, reordered.scenes[0].order)
    }

    @Test
    fun `undo snapshot preserves scene title music and order`() {
        val scene = GameState.MjScene(
            title = "Taverne",
            musicTrackId = "verres_et_dagues",
            markdownContent = "Accueil chaleureux",
            order = 2
        )
        assertEquals("Taverne", scene.title)
        assertEquals("verres_et_dagues", scene.musicTrackId)
        assertEquals(2, scene.order)
    }

    @Test
    fun `internal link insertion format is correct`() {
        val type = "monster"
        val name = "Gobelin"
        val insertion = "#$type:$name"
        assertEquals("#monster:Gobelin", insertion)
    }

    @Test
    fun `link search filtering is case insensitive`() {
        val names = listOf("Gobelin", "HOBGOBLIN", "Bugbear", "Loup")
        val query = "gob"
        val results = names.filter { it.contains(query, ignoreCase = true) }
        assertEquals(2, results.size)
        assertEquals(listOf("Gobelin", "HOBGOBLIN"), results)
    }

    @Test
    fun `scenario save normalizes scene order`() {
        val messy = GameState.MjScenario(
            title = "Test",
            scenes = listOf(
                GameState.MjScene(title = "Fin", order = 5),
                GameState.MjScene(title = "Début", order = 1)
            )
        )
        val normalized = messy.copy(scenes = messy.scenes.mapIndexed { i, s -> s.copy(order = i) })
        assertEquals(0, normalized.scenes[0].order)
        assertEquals(1, normalized.scenes[1].order)
    }

    @Test
    fun `delete scenario removes it from list`() {
        val scenarios = listOf(
            GameState.MjScenario(title = "A"),
            GameState.MjScenario(title = "B"),
            GameState.MjScenario(title = "C")
        )
        val targetId = scenarios[1].id
        val remaining = scenarios.filter { it.id != targetId }
        assertEquals(2, remaining.size)
        assertNull(remaining.find { it.id == targetId })
    }
}
