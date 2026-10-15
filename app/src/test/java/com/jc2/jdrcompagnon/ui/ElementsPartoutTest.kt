package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.scenario.extraireElementsPartout
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.sansElementsPartout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementsPartoutTest {

    private val texte = """
        ## Intro

        {partout: ⚡ Le dragon se réveille}
        {lire}
        Un rugissement.
        {/lire}

        ⚔️ **Combat de boss :** #combat:[Cryovain]
        {/partout}

        {partout: 👥 Qui est qui}
        - Syleen : cheffe.
        {/partout}
        Suite de la page.
    """.trimIndent()

    @Test
    fun `les blocs partout sont extraits avec leur titre et leur contenu`() {
        val elements = extraireElementsPartout(texte)
        assertEquals(listOf("⚡ Le dragon se réveille", "👥 Qui est qui"), elements.map { it.titre })
        assertTrue(elements[0].contenu.startsWith("{lire}"))
        assertTrue(elements[0].contenu.endsWith("#combat:[Cryovain]"))
        assertEquals("- Syleen : cheffe.", elements[1].contenu)
    }

    @Test
    fun `les blocs partout disparaissent du texte de la page`() {
        val page = sansElementsPartout(texte)
        assertFalse(page.contains("partout"))
        assertFalse(page.contains("Rugissement", ignoreCase = true))
        assertTrue(page.contains("## Intro"))
        assertTrue(page.contains("Suite de la page."))
    }

    @Test
    fun `un texte sans bloc partout reste inchange`() {
        val simple = "# Titre\n\nTexte."
        assertEquals(simple, sansElementsPartout(simple))
        assertTrue(extraireElementsPartout(simple).isEmpty())
    }
}
