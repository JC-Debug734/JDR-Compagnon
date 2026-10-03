package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmesPersonnage
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ObjetsACharges
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ObjetsAChargesTest {

    private val baton = EquipmentParser.parse(
        """
        ### Bâton des chants d'oiseaux
        **Type** Bâtons magiques
        **Dégâts** 1d6 contondants
        **Propriétés** Polyvalente (1d8)
        **Charges** 10
        **Recharge** 1d6 + 4
        **Pouvoir** Chant d'oiseau : action, 1 charge.
        **Destruction** 1
        **Description** Bâton sculpté.
        ---
        """.trimIndent()
    ).single()

    @Test
    fun `les champs de charges du livre sont lus`() {
        assertEquals(10, baton.charges)
        assertEquals("1d6 + 4", baton.recharge)
        assertEquals("Chant d'oiseau : action, 1 charge.", baton.pouvoir)
        assertEquals(1, baton.destruction)
        assertEquals("1d6 contondants", baton.damage)
    }

    @Test
    fun `un objet sans charges enregistrees a toutes ses charges`() {
        val perso = Character(name = "Test", type = "PJ")
        assertEquals(10, ObjetsACharges.restantes(perso, baton))
        assertEquals(3, ObjetsACharges.restantes(perso.copy(itemCharges = mapOf(baton.name to 3)), baton))
    }

    @Test
    fun `seule la derniere charge fait lancer le d20 de destruction`() {
        val normale = ObjetsACharges.depenser(5, destruction = 1)
        assertEquals(4, normale.restantes)
        assertNull(normale.jetD20)
        assertFalse(normale.detruit)

        // Sur 1000 tirages de la dernière charge, le d20 reste dans 1..20 et détruit sur 1 seulement.
        val rnd = Random(42)
        repeat(1000) {
            val derniere = ObjetsACharges.depenser(1, destruction = 1, aleatoire = rnd)
            val jet = derniere.jetD20!!
            assertTrue(jet in 1..20)
            assertEquals(jet == 1, derniere.detruit)
            assertEquals(0, derniere.restantes)
        }
    }

    @Test
    fun `la recharge 1d6 + 4 donne entre 5 et 10 charges`() {
        val rnd = Random(7)
        val jets = (1..500).map { ObjetsACharges.jetRecharge("1d6 + 4", rnd)!! }
        assertTrue(jets.all { it in 5..10 })
        assertEquals(setOf(5, 6, 7, 8, 9, 10), jets.toSet())
        assertEquals(3, ObjetsACharges.jetRecharge("3"))
    }

    @Test
    fun `le bonus magique d'une arme s'ajoute a l'attaque et aux degats`() {
        val attaque = ArmesPersonnage.attaques(
            nomsArmes = listOf("Hache d'armes +1"),
            armes = listOf(ArmeSrd("Hache d'armes +1", "1d8 tranchants (+1)", "Polyvalente (1d10)", aDistance = false)),
            force = 14, dexterite = 10, maitrise = 2,
        ).single()
        assertEquals(5, attaque.bonusToucher) // +2 For, +1 magique, +2 maîtrise
        assertEquals("1d8 + 3", attaque.formuleDegats)
    }
}
