package com.jc2.jdrcompagnon.feature_import

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CampagneArchiveTest {

    private fun dossier(): File = Files.createTempDirectory("campagne").toFile()

    @Test
    fun `lit campagne json et l image de carte a cote`() {
        val racine = dossier()
        val sous = File(racine, "Campagne").apply { mkdirs() }
        File(sous, "carte.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(sous, "campagne.json").writeText(
            """
            { "titre": "Dragon", "calendrier": "harptos", "champInconnu": 1,
              "scenarios": ["Phandalin"],
              "carte": { "nom": "Côte", "image": "carte.jpg", "largeurCases": 35, "hauteurCases": 48, "echelleKmParCase": 5 },
              "lieux": [ { "nom": "Phandalin", "type": "VILLE", "x": 20, "y": 37,
                           "lieuxNotables": [ { "nom": "Auberge" } ],
                           "boutiques": [ { "nom": "Barthen", "articles": [ { "nom": "Corde", "prix": 1 } ] } ] } ] }
            """.trimIndent()
        )
        val contenu = CampagneArchive.lire(racine)!!
        assertEquals("Dragon", contenu.dto.titre)
        assertEquals(35, contenu.dto.carte!!.largeurCases)
        val lieu = contenu.dto.lieux.single()
        assertEquals("VILLE", lieu.type)
        assertEquals("Auberge", lieu.lieuxNotables.single().nom)
        assertEquals("Corde", lieu.boutiques.single().articles.single().nom)
        assertArrayEquals(byteArrayOf(1, 2, 3), contenu.imageCarte)
        racine.deleteRecursively()
    }

    @Test
    fun `sans campagne json ou json invalide aucune campagne`() {
        val racine = dossier()
        assertNull(CampagneArchive.lire(racine))
        File(racine, "campagne.json").writeText("{ pas du json")
        assertNull(CampagneArchive.lire(racine))
        racine.deleteRecursively()
    }
}
