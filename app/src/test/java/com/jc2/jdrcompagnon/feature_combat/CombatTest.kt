package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneCompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.bornesFormuleDes
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerFormuleDes
import com.jc2.jdrcompagnon.feature_combat.domain.model.pvSelonTranche
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CombatTest {

    @Test
    fun `composition avec quantites`() {
        assertEquals(
            listOf(LigneCompositionCombat("Gobelin", 3), LigneCompositionCombat("Loup sanguinaire", 1), LigneCompositionCombat("Ogre", 2)),
            CompositionCombat.parser("Gobelin x3, Loup sanguinaire ; Ogre ×2")
        )
    }

    @Test
    fun `composition aller-retour`() {
        val lignes = listOf(LigneCompositionCombat("Gobelin", 3), LigneCompositionCombat("Loup", 1))
        val texte = CompositionCombat.formater(lignes)
        assertEquals("Gobelin x3, Loup", texte)
        assertEquals(lignes, CompositionCombat.parser(texte))
    }

    @Test
    fun `profil extrait de la fiche parsee`() {
        val md = """
            ### Loup
            Catégorie: Animaux
            Type: Bête
            Taille: M
            CA: 12 Initiative +2 (12)
            Pv: 11 (2d8 + 2)
            FP: 1/4 (50 PX ; BM +2)
        """.trimIndent()
        val profil = ProfilCombatMonstre.depuisFiche(MonsterParser.parse(md).single().rawMarkdown)
        assertEquals(ProfilCombatMonstre(12, 11, "2d8 + 2", 2), profil)
    }

    @Test
    fun `initiative negative avec signe typographique`() {
        val profil = ProfilCombatMonstre.depuisFiche("**CA :** 9 Initiative −1 (9)\n**Pv :** 22 (5d8)")
        assertEquals(-1, profil.bonusInitiative)
        assertEquals(9, profil.ca)
        assertEquals(22, profil.pvMoyens)
    }

    @Test
    fun `formule de des`() {
        val r = Random(42)
        repeat(50) {
            val v = lancerFormuleDes("2d8 + 2", r)!!
            assertTrue(v in 4..18)
        }
        assertTrue(lancerFormuleDes("4d8 − 4", r)!! >= 1)
        assertNull(lancerFormuleDes("n'importe quoi", r))
    }

    @Test
    fun `pv selon la difficulte en 4 tranches`() {
        assertEquals(2..7, bornesFormuleDes("1d6 + 1"))
        assertEquals(listOf(2, 4, 5, 7), (0..3).map { pvSelonTranche("1d6 + 1", it, 4) })
        assertEquals(listOf(19, 89, 158, 228), (0..3).map { pvSelonTranche("19d12", it, 4) })
        assertNull(pvSelonTranche("n'importe quoi", 0, 4))
    }
}
