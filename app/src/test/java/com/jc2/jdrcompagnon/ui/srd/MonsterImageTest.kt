package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterImages
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class MonsterImageTest {

    @Test
    fun `champ Image lu et non affiche dans la fiche`() {
        val md = """
            ### Gobelin
            Type: Fée
            Environnement: Forêt, Souterrain
            Image: gobelin_guerrier.png
            FP: 1/4 (50 PX ; BM +2)

            ## Actions
            Cimeterre. Corps à corps : +4.
        """.trimIndent()

        val gobelin = MonsterParser.parse(md).single()

        assertEquals("gobelin_guerrier.png", gobelin.image)
        assertEquals(listOf("Forêt", "Souterrain"), gobelin.environments)
        assertFalse(gobelin.rawMarkdown.contains("gobelin_guerrier.png"))
    }

    @Test
    fun `sans champ Image - pas d'image declaree`() {
        assertNull(MonsterParser.parse("### Loup\nType: Bête\n").single().image)
    }

    @Test
    fun `nom de fichier automatique sans accents ni espaces`() {
        assertEquals("dragon_rouge_adulte", MonsterImages.slug("Dragon rouge adulte"))
        assertEquals("elementaire_de_l_eau", MonsterImages.slug("Élémentaire de l’eau"))
        assertEquals("bete_eclipsante", MonsterImages.slug("Bête éclipsante"))
    }

    @Test
    fun `le bestiaire embarque porte bien les environnements`() {
        val file = File("src/main/assets/dnd/monster_srd521.md")
        val monstres = MonsterParser.parse(file.readText())
        assertEquals(318, monstres.size)
        assertEquals(listOf("Aquatique", "Souterrain"), monstres.first { it.name == "Aboleth" }.environments)
    }
}
