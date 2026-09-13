package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentParserTest {

    @Test
    fun `each weapon in SRD table becomes a distinct entry`() {
        val snippet = """
            ## Armes

            Nom                 Coût   Dégâts       Poids    Propriétés

            *Armes courantes de mêlée*

            gourdin             1 sp   1d4          2 lb.    Lumière
                                       contondant

            Dague               2 gp   1d4          1 lb.    Finesse, lumière, lancé
                                       perforant             (portée 20/60)

            Massue              2 sp   1d8          10 lb.   À deux mains
                                       contondant

            *Armes courantes de portée simple*

            Arc court           25 gp  1d6          2 lb.    Munitions (portée
                                       perforant             80/320), à deux mains.
        """.trimIndent()

        val entries = EquipmentParser.parse(snippet)
            .filter { it.category == "Armes" }
            .map { it.name }

        assertEquals(
            "Les armes du tableau doivent être des entrées distinctes",
            listOf("Gourdin", "Dague", "Massue", "Arc court"),
            entries
        )
    }

    @Test
    fun `weapons include multi line properties`() {
        val snippet = """
            ## Armes

            Dague               2 gp   1d4          1 lb.    Finesse, lumière, lancé
                                       perforant             (portée 20/60)
        """.trimIndent()

        val dagger = EquipmentParser.parse(snippet).single { it.name == "Dague" }

        assertTrue("Le contenu de la Dague doit inclure la ligne de propriétés", dagger.rawMarkdown.contains("perforant"))
        assertTrue("Le contenu de la Dague doit inclure la portée", dagger.rawMarkdown.contains("portée 20/60"))
    }
}
