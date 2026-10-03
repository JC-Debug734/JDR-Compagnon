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
 * Chaîne complète d'une sous-classe importée : reconnue par CustomContentParser
 * (`<!-- type: sousclasse -->`), rattachée à sa classe par SrdRepository.fusionnerSousClasses,
 * puis lue par ClasseParser (aptitudes et sorts toujours préparés).
 */
class SousClasseImportTest {
    private val fichierImport = """
        # Sous-classes de Paladin

        ## Sous-classe : Serment des Anciens
        <!-- type: sousclasse -->
        <!-- id: serment-des-anciens -->
        - classe : Paladin
        - devise : Préservez la vie et la lumière en ce bas monde

        #### Sorts du Serment des Anciens

        | Niveau de Paladin | Sorts |
        |---|---|
        | 3 | communication avec les animaux, frappe piégeuse |
        | 5 | foulée brumeuse, rayon de lune |

        #### Niveau 3 : Courroux de la nature
        <!-- id: courroux-de-la-nature; type: automatique -->
        Lianes spectrales.

        #### Niveau 7 : Aura de garde
        <!-- id: aura-de-garde; type: automatique -->
        Résistances.

        ---

        ## Sous-classe : Serment de Dévotion
        <!-- type: sousclasse -->
        - classe : Paladin

        #### Niveau 20 : Nimbe sacré
        <!-- id: nimbe-sacre; type: automatique -->
        Aura renforcée.
    """.trimIndent()
    private val classesMd = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))

    private val importe = CustomContentParser.parse(fichierImport)
    private val paladin = ClasseParser.parse(
        SrdRepository.fusionnerSousClasses(MarkdownSectionParser.parseFlat(classesMd, category = "Classe"), importe.sousClasses)
            .joinToString("\n\n") { it.rawMarkdown }
    ).first { it.nom == "Paladin" }

    @Test
    fun `le fichier est reconnu comme sous-classes de Paladin`() {
        assertEquals(2, importe.summary.sousClasses)
        assertTrue(importe.sousClasses.all { it.category == "Paladin" })
    }

    @Test
    fun `les sous-classes importees remplacent celle du SRD sans doublon`() {
        assertEquals(listOf("Serment de Dévotion", "Serment des Anciens"), paladin.sousClasses.map { it.nom }.sorted())
    }

    @Test
    fun `aptitudes et sorts de sous-classe sont lus`() {
        val anciens = paladin.sousClasses.first { it.nom == "Serment des Anciens" }
        assertEquals(listOf(3, 7), anciens.aptitudes.flatMap { it.niveaux }.sorted())
        assertEquals(listOf("Communication avec les animaux", "Frappe piégeuse"), anciens.sortsJusquAu(3))
        assertEquals(4, anciens.sortsJusquAu(5).size)
        assertTrue(anciens.description.startsWith("Préservez la vie"))
        val devotion = paladin.sousClasses.first { it.nom == "Serment de Dévotion" }
        assertTrue(devotion.aptitudes.any { it.nom == "Nimbe sacré" && 20 in it.niveaux })
    }

    @Test
    fun `sorts toujours prepares lus depuis une liste sans table`() {
        val fichier = """
            ## Sous-classe : Domaine de la Vie
            <!-- type: sousclasse -->
            - classe : Clerc

            #### Niveau 3 : Sorts du Domaine de la Vie
            <!-- id: sorts-du-domaine-de-la-vie; type: automatique -->
            Sorts toujours préparés selon votre niveau de Clerc :
            - **Niveau 3** : aide, bénédiction
            - **Niveau 5** : retour à la vie

            #### Niveau 6 : Guérisseur béni
            <!-- id: guerisseur-beni; type: automatique -->
            Soins.
        """.trimIndent()
        val clerc = ClasseParser.parse(
            SrdRepository.fusionnerSousClasses(
                MarkdownSectionParser.parseFlat(classesMd, category = "Classe"),
                CustomContentParser.parse(fichier).sousClasses,
            ).joinToString("\n\n") { it.rawMarkdown }
        ).first { it.nom == "Clerc" }
        val vie = clerc.sousClasses.first { it.nom == "Domaine de la Vie" }
        assertEquals(listOf("Aide", "Bénédiction"), vie.sortsJusquAu(3))
        assertEquals(listOf("Aide", "Bénédiction", "Retour à la vie"), vie.sortsJusquAu(5))
        assertEquals(listOf(3, 6), vie.aptitudes.flatMap { it.niveaux }.sorted())
    }
}
