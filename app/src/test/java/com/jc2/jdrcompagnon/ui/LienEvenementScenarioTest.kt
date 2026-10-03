package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.scenario.extractInternalLinks
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.internalLinkLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class LienEvenementScenarioTest {

    @Test
    fun `un lien evenement est distingue d'une discussion avec un PNJ`() {
        val texte = "Au pont, #evenement:[Gué en crue] puis #event:[Bram le passeur] et #evenement:Éboulement."
        assertEquals(
            listOf("evenement" to "Gué en crue", "event" to "Bram le passeur", "evenement" to "Éboulement."),
            extractInternalLinks(texte)
        )
    }

    @Test
    fun `un lien evenement s'affiche avec un de`() {
        assertEquals("🎲 Gué en crue", internalLinkLabel("evenement", "Gué en crue"))
    }
}
