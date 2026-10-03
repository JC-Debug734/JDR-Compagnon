package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DonsParserTest {

    private val source = """
        # Dons

        ### Sauvagerie martiale

        <!-- id: sauvagerie-martiale; categorie: Origines -->

        **Type** Don
        **Prérequis** -
        **Répétable** Non

        #### Sauvagerie martiale

        **Catégorie** Actif
        **Coût** Aucun
        **Limitation** Une fois par tour
        **Effet** Vous pouvez lancer deux fois les dés de dégâts de l'arme.

        ---

        ### Défense

        **Type** Don
        **Prérequis** Aptitude Style de combat
    """.trimIndent()

    @Test
    fun `categorie lue dans la balise, structure unique`() {
        val (sauvagerie, defense) = DonsParser.parse(source)
        assertEquals("Origines", sauvagerie.category)
        assertEquals("Général", defense.category)
        val md = sauvagerie.rawMarkdown
        assertTrue(md.contains("*Don — Origines*"))
        assertTrue(md.contains("- **Répétable** : Non"))
        assertFalse(md.contains("Prérequis")) // « - » omis
        assertTrue(md.contains("### Utilisation"))
        assertTrue(md.contains("- **Type** : Actif"))
        assertTrue(md.contains("- **Limitation** : Une fois par tour"))
        assertTrue(md.contains("<!-- id: sauvagerie-martiale; categorie: Origines -->"))
    }
}
