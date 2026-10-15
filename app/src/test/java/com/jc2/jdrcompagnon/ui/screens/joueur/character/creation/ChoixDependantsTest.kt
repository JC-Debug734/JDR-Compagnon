package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Balises "liste: <classe>", "selon: <choix>" + "options-<valeur>", et descriptions en puces. */
class ChoixDependantsTest {

    private val don = DonParser.depuisEntrees(
        listOf(
            "Initié de Haute Sorcellerie" to """
                <!-- id: ihs; categorie: Origines -->
                <!-- id: ihs-lune; choix: 1; effet: note; options: Nuitari, Solinari -->
                <!-- id: ihs-mineur; choix: 1; effet: sorts; options: sorts-mineurs; liste: Magicien -->
                <!-- id: ihs-sorts; choix: 2; effet: sorts; selon: ihs-lune; options-nuitari: Maléfice, Simulacre de vie; options-solinari: Bouclier, Compréhension des langues -->

                - **Nuitari** — La lune noire des Robes Noires.
                - **Solinari** — La lune blanche des Robes Blanches.
            """.trimIndent()
        )
    ).single()

    private val ctx = ContexteChoix(
        sorts = listOf(
            SortChoisissable("Trait de feu", 0, listOf("Magicien", "Ensorceleur"), "Mineur d'évocation"),
            SortChoisissable("Flamme sacrée", 0, listOf("Clerc"), "Mineur d'évocation"),
            SortChoisissable("Bouclier", 1, listOf("Magicien"), "1er niveau d'abjuration"),
        )
    )

    private fun choix(valeurs: Map<String, List<String>> = emptyMap()) =
        don.balises.mapNotNull { construireChoix(it, don.nom, don.description, ctx, valeursAutres = valeurs) }

    @Test
    fun `les lunes ont une description`() {
        val lune = choix().first { it.id == "ihs-lune" }
        assertEquals("La lune noire des Robes Noires.", lune.descriptions["Nuitari"])
    }

    @Test
    fun `liste imposee sans choix intermediaire`() {
        val mineur = choix().first { it.id == "ihs-mineur" }
        assertEquals(listOf("Trait de feu"), mineur.options)
        assertNull(mineur.enAttente)
    }

    @Test
    fun `les sorts dependent de la lune choisie`() {
        assertNotNull(choix().first { it.id == "ihs-sorts" }.enAttente)
        val solinari = choix(mapOf("ihs-lune" to listOf("Solinari"))).first { it.id == "ihs-sorts" }
        assertEquals(listOf("Bouclier", "Compréhension des langues"), solinari.options)
        assertEquals("1er niveau d'abjuration", solinari.descriptions["Bouclier"])
        assertTrue(choix(mapOf("ihs-lune" to listOf("Nuitari"))).first { it.id == "ihs-sorts" }.options.contains("Maléfice"))
    }
}
