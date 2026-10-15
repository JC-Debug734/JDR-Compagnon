package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Espèce d'un livre importé : écrite sans « ### Traits spéciaux » (un "### " couperait l'entrée,
 * cf. CustomContentParser), ses traits ne doivent pas être recomptés comme champs de base.
 */
class EspeceImporteeTest {

    private val livre = """
        # Règles de Krynn

        ## Kender
        <!-- type: espece -->

        <!-- id: kender -->

        - Type de créature : Humanoïde
        - Catégorie de taille : P (petite, entre 90 cm et 1,20 m)
        <!-- id: taille; taille: Petite -->
        - Vitesse : 9 m
        <!-- id: savoir-faire; choix: 1; effet: competences; options: Discrétion, Escamotage, Survie -->

        Petits humanoïdes curieux et intrépides. Ils amassent quantité d'objets étranges.

        #### Intrépide
        <!-- id: intrepide -->
        Avantage contre l'état Effrayé.

        #### Provocation
        <!-- id: provocation; choix: 1; effet: note; options: Intelligence, Sagesse, Charisme -->
        Par une action Bonus, vous provoquez une créature.
    """.trimIndent()

    private val kender = EspeceParser.parse(CustomContentParser.parse(livre).especes.joinToString("\n\n") { it.rawMarkdown }).single()

    @Test
    fun `chaque choix n'est demande qu'une fois`() {
        assertEquals(listOf("savoir-faire", "provocation"), choixEspece(kender, emptyList()).map { it.id })
    }

    @Test
    fun `la presentation de l'espece est lue`() {
        assertTrue(kender.description.startsWith("Petits humanoïdes curieux et intrépides."))
        assertEquals(listOf("Intrépide", "Provocation"), kender.traits.filterNot { it.base }.map { it.nom })
    }
}
