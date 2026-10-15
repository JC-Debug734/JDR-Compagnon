package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Monnaie : prix en pièces de cuivre, libellés, saisie et paiement avec rendu de monnaie. */
class MonnaieTest {

    private fun perso(pp: Int = 0, po: Int = 0, pe: Int = 0, pa: Int = 0, pc: Int = 0) = Character(
        name = "Lia", type = "PJ", platinum = pp, gold = po, electrum = pe, silver = pa, copper = pc,
    )

    @Test
    fun `libelle dans la monnaie correspondante`() {
        assertEquals("3 pa", Monnaie.libelle(30))
        assertEquals("5 pc", Monnaie.libelle(5))
        assertEquals("1 po 5 pa", Monnaie.libelle(150))
        assertEquals("12 po", Monnaie.libelle(1200))
        assertEquals("Gratuit", Monnaie.libelle(0))
    }

    @Test
    fun `saisie de montants`() {
        assertEquals(50, Monnaie.parser("5 pa"))
        assertEquals(150, Monnaie.parser("1,5 po"))
        assertEquals(230, Monnaie.parser("2 po 3 pa"))
        assertEquals(1200, Monnaie.parser("12"))
        assertEquals(1050, Monnaie.parser("1 pp 1 pe"))
        assertNull(Monnaie.parser("—"))
        // Un libellé se relit tel quel.
        assertEquals(150, Monnaie.parser(Monnaie.libelle(150)))
    }

    @Test
    fun `paiement avec rendu de monnaie`() {
        val apres = Monnaie.payer(perso(po = 10, pc = 5), 30)!!
        assertEquals(1005 - 30, Monnaie.totalPc(apres))
        // Les 5 pc partent d'abord, puis une po : 7 pa 5 pc rendus.
        assertEquals(9, apres.gold)
        assertEquals(7, apres.silver)
        assertEquals(5, apres.copper)
    }

    @Test
    fun `paiement impossible sans assez de pieces`() {
        assertNull(Monnaie.payer(perso(pa = 2), 30))
    }

    @Test
    fun `toutes les sortes de pieces comptent`() {
        val p = perso(pp = 1, po = 2, pe = 1, pa = 3, pc = 4)
        assertEquals(1000 + 200 + 50 + 30 + 4, Monnaie.totalPc(p))
        assertEquals("1 pp, 2 po, 1 pe, 3 pa, 4 pc", Monnaie.libelleBourse(p))
        val apres = Monnaie.payer(p, 1284)!!
        assertEquals(0, Monnaie.totalPc(apres))
    }
}
