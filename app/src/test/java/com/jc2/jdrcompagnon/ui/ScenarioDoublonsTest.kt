package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenarioDoublonsTest {

    private fun scenario(id: String, titre: String, texte: String, monde: String = "dnd") = GameState.MjScenario(
        id = id,
        title = titre,
        worldId = monde,
        scenes = listOf(GameState.MjScene(title = "Scène 1", markdownContent = texte)),
    )

    @Test
    fun `les copies identiques sont detectees et rattachees au premier scenario`() {
        val original = scenario("11111111-1111-1111-1111-111111111111", "Ferme", "{image:11111111-1111-1111-1111-111111111111_carte.img}\nTexte")
        // Copie importée à nouveau : ses images portent son propre id, mais le contenu est le même.
        val copie = scenario("22222222-2222-2222-2222-222222222222", "Ferme", "{image:22222222-2222-2222-2222-222222222222_carte.img}\nTexte")
        val autre = scenario("33333333-3333-3333-3333-333333333333", "Ferme", "Texte modifié")
        val autreMonde = scenario("44444444-4444-4444-4444-444444444444", "Ferme", "{image:44444444-4444-4444-4444-444444444444_carte.img}\nTexte", monde = "autre")

        val tri = GameState.scenariosSansCopies(listOf(original, copie, autre, autreMonde))

        assertEquals(listOf(original, autre, autreMonde), tri.gardes)
        assertEquals(listOf(copie), tri.copies)
        assertEquals(mapOf(copie.id to original.id), tri.versGarde)
    }
}
