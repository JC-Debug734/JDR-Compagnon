package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.ReferenceEntryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReferenceEntryParserTest {

    @Test
    fun `une entree par titre, categorie = section, sous-titres gardes dans le contenu`() {
        val md = """
            ---
            title: SRD
            ---
            # Aventures
            Intro du chapitre.
            ## Mouvement
            Texte du mouvement.
            ### Vitesse
            Texte vitesse.
            #### Rythme de voyage
            Détail du rythme.
            ## Environnement
            ### Chute
            On tombe.
        """.trimIndent()

        val entries = ReferenceEntryParser.parse(md)

        assertEquals(listOf("Aventures", "Mouvement", "Vitesse", "Chute"), entries.map { it.name })
        assertEquals(listOf("Aventures", "Mouvement", "Mouvement", "Environnement"), entries.map { it.category })
        assertTrue(entries[2].rawMarkdown.contains("#### Rythme de voyage"))
        // "## Environnement" sans texte propre : pas d'entrée vide.
        assertFalse(entries.any { it.name == "Environnement" })
    }

    @Test
    fun `noms en double completes par leur categorie`() {
        val md = "## Utiliser chaque capacité\n### Force\nA\n## Compétences\n### Force\nB\n"
        assertEquals(
            listOf("Force (Utiliser chaque capacité)", "Force (Compétences)"),
            ReferenceEntryParser.parse(md).map { it.name }
        )
    }

    @Test
    fun `fichiers embarques - noms uniques et pas d'en-tete YAML`() {
        listOf("src/main/assets/dnd/rules.md", "src/main/assets/dnd/glossary.md").forEach { path ->
            val entries = ReferenceEntryParser.parse(File(path).readText())
            assertTrue("$path : trop peu d'entrées (${entries.size})", entries.size > 10)
            assertEquals("$path : noms en double", entries.size, entries.map { it.name.lowercase() }.distinct().size)
            assertFalse(entries.any { it.name.contains("linespread") || it.rawMarkdown.startsWith("title:") })
            println("$path : ${entries.size} entrées, ${entries.map { it.category }.distinct().size} catégories")
        }
    }
}
