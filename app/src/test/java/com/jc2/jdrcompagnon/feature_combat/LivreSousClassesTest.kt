package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MarkdownSectionParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Livre « sousclasses_2024.md » (Bureau de l'utilisateur) lu comme l'application l'importe :
 * chaque sous-classe rattachée à sa classe, ses aptitudes et ses sorts toujours préparés lus.
 */
class LivreSousClassesTest {

    private val livre = File("C:/Users/Jean/Desktop/sousclasses_2024.md")

    @Test
    fun `toutes les sous-classes du livre sont rattachees, avec aptitudes et sorts`() {
        assumeTrue(livre.isFile)
        val sousClasses = CustomContentParser.parse(livre.readText()).sousClasses
        assertEquals(48, sousClasses.size)
        val officielles = MarkdownSectionParser.parseFlat(
            Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md")), category = "Classe"
        )
        val classes = ClasseParser.parse(SrdRepository.fusionnerSousClasses(officielles, sousClasses).joinToString("\n\n") { it.rawMarkdown })
        val problemes = mutableListOf<String>()
        sousClasses.forEach { sc ->
            val classe = classes.firstOrNull { it.nom.equals(sc.category, ignoreCase = true) }
            val lue = classe?.sousClasses?.firstOrNull { it.nom.equals(sc.name, ignoreCase = true) }
            if (lue == null) { problemes += "${sc.name} (${sc.category}) : non rattachée"; return@forEach }
            if (lue.aptitudes.size < 3) problemes += "${sc.name} : ${lue.aptitudes.size} aptitude(s) seulement"
            // Une aptitude « Sorts … » doit donner des sorts toujours préparés.
            val annonceSorts = lue.aptitudes.any { it.nom.startsWith("Sorts ") && it.description.contains("**Niveau") }
            if (annonceSorts && lue.sortsJusquAu(20).isEmpty()) problemes += "${sc.name} : sorts toujours préparés non lus"
        }
        assertTrue(problemes.joinToString("\n"), problemes.isEmpty())
        val grandAncien = classes.first { it.nom == "Occultiste" }.sousClasses.first { it.nom == "Protecteur Grand Ancien" }
        assertTrue("Maléfice" in grandAncien.sortsJusquAu(10))
        assertTrue("Maléfice" !in grandAncien.sortsJusquAu(9))
    }
}
