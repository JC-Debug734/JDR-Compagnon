package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MarkdownSectionParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Sous-classe de Guerrier importée (format de sousclasses_guerrier_tasha.md) : runes à choisir
 * (choix-effet sur plusieurs niveaux), et listes d'effets conservées dans la description d'une
 * aptitude automatique.
 */
class ChevalierRuniqueImportTest {
    private val fichierImport = """
        ## Sous-classe : Chevalier runique
        <!-- type: sousclasse -->

        <!-- id: chevalier-runique -->
        - classe : Guerrier

        #### Niveau 3 : Puissance des géants
        <!-- id: puissance-des-geants; type: automatique -->
        Action Bonus : pendant 1 minute, vous bénéficiez des effets suivants.
        - **Taille G** : vous devenez de taille G.
        - **Coup géant** : 1d6 dégâts supplémentaires une fois par tour.

        #### Niveau 3 : Sculpteur de runes
        <!-- id: sculpteur-de-runes; type: choix-effet -->
        Première rune connue.
        - **Rune du feu** <!-- id: rune-du-feu --> : chaînes ardentes.
        - **Rune des collines** <!-- id: rune-des-collines --> : (niv. 7+) résistance.

        #### Niveau 3, 7, 10, 15 : Rune supplémentaire
        <!-- id: rune-supplementaire; type: choix-effet -->
        Une rune de plus.
        - **Rune du feu** <!-- id: rune-du-feu --> : chaînes ardentes.
        - **Rune des collines** <!-- id: rune-des-collines --> : (niv. 7+) résistance.
    """.trimIndent()

    private val classesMd = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))
    private val importe = CustomContentParser.parse(fichierImport)
    private val chevalier = ClasseParser.parse(
        SrdRepository.fusionnerSousClasses(MarkdownSectionParser.parseFlat(classesMd, category = "Classe"), importe.sousClasses)
            .joinToString("\n\n") { it.rawMarkdown }
    ).first { it.nom == "Guerrier" }.sousClasses.first { it.nom == "Chevalier runique" }

    @Test
    fun `sous-classe rattachee au guerrier`() {
        assertEquals(1, importe.summary.sousClasses)
        assertEquals("Guerrier", importe.sousClasses.single().category)
    }

    @Test
    fun `runes a choisir au niveau 3 (deux) puis aux niveaux 7, 10 et 15`() {
        val choix = chevalier.aptitudes.filter { it.type == "choix-effet" }
        assertEquals(listOf(listOf(3), listOf(3, 7, 10, 15)), choix.map { it.niveaux })
        assertTrue(choix.all { apt -> apt.options.map { it.id } == listOf("rune-du-feu", "rune-des-collines") })
        // Aptitude à choix : les options ne sont pas répétées dans la description.
        assertTrue(choix.none { it.description.contains("Rune du feu") })
    }

    @Test
    fun `aptitude automatique, la liste de ses effets reste dans la description`() {
        val puissance = chevalier.aptitudes.first { it.nom == "Puissance des géants" }
        assertTrue(puissance.description.contains("Taille G"))
        assertTrue(puissance.description.contains("Coup géant"))
    }
}
