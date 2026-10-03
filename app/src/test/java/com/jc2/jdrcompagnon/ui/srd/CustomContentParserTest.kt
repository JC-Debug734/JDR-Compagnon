package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomContentParserTest {

    // Format documenté dans CustomContentParser : balise de type juste sous le titre.
    private val livre = """
        ## Nouveaux monstres
        ### Gnome des cavernes
        <!-- type: monstre -->
        Catégorie: Gnomes
        Type: Humanoïde (gnome)
        Taille: P
        CA: 13 (armure de cuir) Initiative +2 (12)
        Pv: 16 (3d6 + 6)
        FP: 1/2 (100 PX)

        ## Actions
        Dague. Corps à corps : +4, allonge 1,50 m, une cible. Touché : 4 (1d4 + 2) dégâts perforants.
    """.trimIndent()

    @Test
    fun `la balise de type sous le titre n'empeche pas la lecture des champs du monstre`() {
        val result = CustomContentParser.parse(livre)

        assertEquals(1, result.monsters.size)
        assertTrue(result.unrecognized.isEmpty())
        val monstre = result.monsters.single()
        assertEquals("Gnomes", monstre.category)
        assertTrue("<!-- type" !in monstre.rawMarkdown)

        val profil = ProfilCombatMonstre.depuisFiche(monstre.rawMarkdown)
        assertEquals(13, profil.ca)
        assertEquals(16, profil.pvMoyens)
        assertEquals("3d6 + 6", profil.formulePv)
        assertEquals(2, profil.bonusInitiative)
    }

    @Test
    fun `une entree pnj est lue a part et n'entre pas dans le bestiaire`() {
        val result = CustomContentParser.parse(
            livre + "\n\n" + """
                ## PNJ
                ### Don-Jon Raskin
                <!-- type: pnj -->
                Catégorie: PNJ
                Type: Humanoïde (humain)
                Taille: M
                CA: 10 Initiative +0 (10)
                Pv: 44 (8d8 + 8)
                Caractéristiques: For 11 +0 +0 Dex 10 +0 +2 Con 13 +1 +3 / Int 12 +1 +1 Sag 10 +0 +0 Cha 14 +2 +2
                Image: images/don_jon_raskin.webp

                ## Actions
                Dague. Corps à corps : +2, allonge 1,50 m, une cible.
            """.trimIndent()
        )

        assertEquals(listOf("Gnome des cavernes"), result.monsters.map { it.name })
        assertEquals(listOf("Don-Jon Raskin"), result.pnjs.map { it.name })
        val pnj = result.pnjs.single()
        assertEquals("images/don_jon_raskin.webp", pnj.image)
        assertEquals("44 (8d8 + 8)", pnj.fields["Pv"])
        assertTrue("Dague" in pnj.body)
        assertEquals(1, result.summary.pnjs)
    }
}
