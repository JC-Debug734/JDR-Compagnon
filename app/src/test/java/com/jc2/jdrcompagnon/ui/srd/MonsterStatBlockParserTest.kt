package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.CapaciteMonstre
import com.jc2.jdrcompagnon.ui.screens.mj.library.CaracMonstre
import com.jc2.jdrcompagnon.ui.screens.mj.library.MonsterStatBlockParser
import org.junit.Assert.assertEquals
import org.junit.Test

class MonsterStatBlockParserTest {

    @Test
    fun `caracteristiques avec signe moins typographique`() {
        val caracs = MonsterStatBlockParser.caracteristiques(
            "For 21 +5 +5 Dex 9 −1 +3 Con 15 +2 +6 / Int 18 +4 +8 Sag 15 +2 +6 Cha 18 +4 +4"
        )
        assertEquals(6, caracs.size)
        assertEquals(CaracMonstre("DEX", "9", "-1", "+3"), caracs[1])
    }

    @Test
    fun `CA et initiative separees`() {
        assertEquals("17" to "+7 (17)", MonsterStatBlockParser.caEtInitiative("17 Initiative +7 (17)"))
        assertEquals("12" to null, MonsterStatBlockParser.caEtInitiative("12"))
    }

    @Test
    fun `sections, noms de capacites et paragraphe libre`() {
        val sections = MonsterStatBlockParser.sections(
            """
            ## Actions
            Souffle de feu (recharge 5–6). JS Dextérité : DD 21, chaque créature dans un Cône de 18 m.

            ## Actions Légendaires
            Utilisations d’action Légendaire : 3 (4 dans son antre). Aussitôt après le tour d’une autre créature.
            Assaut incisif. Le dragon se déplace.
            """.trimIndent()
        )
        assertEquals(listOf("Actions", "Actions Légendaires"), sections.map { it.titre })
        assertEquals("Souffle de feu (recharge 5–6)", sections[0].capacites.single().nom)
        assertEquals(null, sections[1].capacites[0].nom)
        assertEquals(CapaciteMonstre("Assaut incisif", "Le dragon se déplace."), sections[1].capacites[1])
    }
}
