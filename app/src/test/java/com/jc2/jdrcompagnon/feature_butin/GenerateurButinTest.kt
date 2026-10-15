package com.jc2.jdrcompagnon.feature_butin

import com.jc2.jdrcompagnon.feature_butin.domain.Bourse
import com.jc2.jdrcompagnon.feature_butin.domain.FamilleCreature
import com.jc2.jdrcompagnon.feature_butin.domain.GenerateurButin
import com.jc2.jdrcompagnon.ui.Character
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateurButinTest {

    @Test
    fun `facteur de puissance lu depuis le champ FP`() {
        assertEquals(0.25, GenerateurButin.facteurPuissance("1/4 (50 PX ; BM +2)"), 0.0001)
        assertEquals(5.0, GenerateurButin.facteurPuissance("5 (1 800 PX ; BM +3)"), 0.0001)
        assertEquals(0.0, GenerateurButin.facteurPuissance(null), 0.0001)
    }

    @Test
    fun `famille deduite du type du bestiaire`() {
        assertEquals(FamilleCreature.HUMANOIDE, FamilleCreature.depuisType("Fée (Gobelinoïde)"))
        assertEquals(FamilleCreature.BETE, FamilleCreature.depuisType("Nuée de taille M de Bêtes"))
        assertEquals(FamilleCreature.DRAGON, FamilleCreature.depuisType("Dragon (Chromatique)"))
    }

    @Test
    fun `valeur de la bourse en pieces d or`() {
        assertEquals(11, Bourse(pc = 150, pa = 20, po = 8).valeurPo)
        // Les restes de chaque type de pièce s'additionnent avant l'arrondi.
        assertEquals(1, Bourse(pc = 50, pa = 5).valeurPo)
        assertEquals(10, Bourse(pp = 1).valeurPo)
    }

    @Test
    fun `une bete ne porte jamais de pieces mais donne de la viande`() {
        repeat(50) { graine ->
            val butin = GenerateurButin.pourMonstre("Loup", "Bête", "1/4 (50 PX)", "M", random = Random(graine))
            assertTrue(butin.bourse.vide)
            assertTrue(butin.objets.any { it.nom == "Rations (1 jour)" })
            assertTrue(butin.objets.any { it.nom.startsWith("Peau de loup") })
        }
    }

    @Test
    fun `l equipement de la fiche est recuperable avec ses munitions`() {
        val butin = GenerateurButin.pourMonstre(
            "Chef gobelin", "Fée (Gobelinoïde)", "1 (200 PX ; BM +2)", "P",
            equipement = "arc court, bouclier, chemise de mailles, cimeterre",
            random = Random(1),
        )
        val noms = butin.objets.map { it.nom }
        assertTrue(noms.containsAll(listOf("Arc court", "Bouclier", "Chemise de mailles", "Cimeterre", "Flèches")))
    }

    @Test
    fun `meme graine meme butin`() {
        val a = GenerateurButin.pourMonstre("Bandit", "Humanoïde", "1/8", "M", "cimeterre", Random(42))
        val b = GenerateurButin.pourMonstre("Bandit", "Humanoïde", "1/8", "M", "cimeterre", Random(42))
        assertEquals(a, b)
    }

    @Test
    fun `un pnj donne ce qu il porte et son or`() {
        val pnj = Character(
            name = "Garde", type = "PNJ", gold = 7,
            weapons = listOf("Lance"), armor = listOf("Armure de cuir"), backpackItems = listOf("Torche", "Torche"),
        )
        val butin = GenerateurButin.pourPersonnage(pnj, Random(3))
        assertEquals(7, butin.bourse.po)
        assertTrue(butin.objets.any { it.nom == "Lance" && it.possede })
        assertTrue(butin.objets.any { it.nom == "Torche" && it.quantite == 2 && it.possede })
    }
}
