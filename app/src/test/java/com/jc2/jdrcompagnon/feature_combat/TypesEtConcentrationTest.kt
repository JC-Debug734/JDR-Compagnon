package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.DefensesDegats
import com.jc2.jdrcompagnon.feature_combat.domain.model.EffetDefense
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypesCreature
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.random.Random

class TypesEtConcentrationTest {

    private class DesFixes(private vararg val valeurs: Int) : Random() {
        private var i = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(from: Int, until: Int): Int = valeurs[i++ % valeurs.size].coerceIn(from, until - 1)
    }

    private fun pj(nom: String, defenses: DefensesDegats = DefensesDegats(), conditions: Set<ConditionCombat> = emptySet()) =
        Combattant(id = nom, nom = nom, estMonstre = false, ca = 14, pvMax = 40, pv = 40, bonusInitiative = 0, initiative = 20,
            defenses = defenses, conditions = conditions, typeCreature = "Humanoïde")

    private fun monstre(nom: String) =
        Combattant(id = nom, nom = nom, estMonstre = true, monstreNom = nom, ca = 12, pvMax = 99, pv = 99, bonusInitiative = 0, initiative = 5)

    @After
    fun nettoyer() {
        CombatSession.fermer()
        CombatSession.aleatoire = Random
    }

    @Test
    fun `chaque creature du bestiaire a un type reconnu`() {
        val monstres = MonsterParser.parse(Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/monster_srd521.md")))
        assertTrue(monstres.size > 300)
        val sansType = monstres.filter { TypesCreature.depuisFiche(it.rawMarkdown) == null }.map { it.name }
        assertTrue("Sans type : $sansType", sansType.isEmpty())
        val feuFollet = monstres.first { it.name == "Feu follet" }
        assertEquals("Mort-vivant", TypesCreature.depuisFiche(feuFollet.rawMarkdown))
        val d = TypesCreature.defensesDepuisFiche(feuFollet.rawMarkdown)
        assertEquals(EffetDefense.IMMUNITE, d.effet("foudre"))
        assertEquals(EffetDefense.RESISTANCE, d.effet("nécrotiques"))
        assertEquals(EffetDefense.AUCUN, d.effet("radiants"))
        assertTrue(ConditionCombat.PARALYSE in d.etats)
        assertEquals("Bête", TypesCreature.normaliser("Nuée de taille M de Bêtes"))
        assertEquals("Mort-vivant", TypesCreature.normaliser("Morts-vivants"))
        assertEquals("Fée", TypesCreature.normaliser("Fée (archifée)"))
    }

    @Test
    fun `armes efficaces contre un type`() {
        val tueuse = TypesCreature.bonusArme("Épée longue, tueuse de dragons")!!
        assertEquals("3d6", tueuse.formule)
        assertTrue("Dragon" in tueuse.types)
        val livre = TypesCreature.bonusArme("Lame sacrée", "Arme +1. +2d6 dégâts radiants contre les Fiélons/Morts-vivants.")!!
        assertEquals(setOf("Fiélon", "Mort-vivant"), livre.types)
        assertEquals("radiants", livre.typeDegats)
        assertNull(TypesCreature.bonusArme("Épée longue"))
    }

    @Test
    fun `immunite, resistance et vulnerabilite aux degats d'un type`() {
        CombatSession.aleatoire = DesFixes(5)
        val souffle = AttaqueMonstre("Souffle", TypeAttaqueMonstre.SAUVEGARDE, formuleDegats = "2d6", degatsMoyens = 7, typeDegats = "feu", sauvegarde = "Dextérité", dd = 30)
        CombatSession.demarrer("Test", listOf(
            pj("Immunise", DefensesDegats(immunites = setOf("feu"))),
            pj("Resistant", DefensesDegats(resistances = setOf("feu"))),
            pj("Vulnerable", DefensesDegats(vulnerabilites = setOf("feu"))),
            monstre("Dragon"),
        ))
        CombatSession.commencer()
        CombatSession.modifierDeclaration(DeclarationAction("Dragon", "Souffle", cibleId = "Immunise", attaque = souffle.copy(zone = true)))
        CombatSession.resoudreAttaqueSauvegarde("Dragon", totalChoisi = 10)
        val e = CombatSession.etat.value!!
        assertEquals(40, e.combattants.first { it.id == "Immunise" }.pv)
        assertEquals(35, e.combattants.first { it.id == "Resistant" }.pv)
        assertEquals(20, e.combattants.first { it.id == "Vulnerable" }.pv)
    }

    @Test
    fun `immunite a un etat`() {
        CombatSession.demarrer("Test", listOf(pj("Fee", DefensesDegats(etats = setOf(ConditionCombat.CHARME))), monstre("Gob")))
        CombatSession.appliquerCondition("Fee", ConditionCombat.CHARME, "test")
        val fee = CombatSession.etat.value!!.combattants.first { it.id == "Fee" }
        assertFalse(ConditionCombat.CHARME in fee.conditions)
        assertTrue(CombatSession.etat.value!!.journal.any { "immunisé" in it })
    }

    @Test
    fun `concentration rompue par des degats sur un JS rate, gardee sinon`() {
        CombatSession.aleatoire = DesFixes(2)
        CombatSession.demarrer("Test", listOf(pj("Mage", conditions = setOf(ConditionCombat.CONCENTRATION)), monstre("Gob")))
        CombatSession.appliquerPv("Mage", 30)
        var mage = CombatSession.etat.value!!.combattants.first { it.id == "Mage" }
        assertFalse(ConditionCombat.CONCENTRATION in mage.conditions)
        assertTrue(CombatSession.etat.value!!.journal.any { "concentration" in it && "DD 15" in it })
        CombatSession.fermer()

        CombatSession.aleatoire = DesFixes(19)
        CombatSession.demarrer("Test", listOf(pj("Mage", conditions = setOf(ConditionCombat.CONCENTRATION)), monstre("Gob")))
        CombatSession.appliquerPv("Mage", 8)
        mage = CombatSession.etat.value!!.combattants.first { it.id == "Mage" }
        assertTrue(ConditionCombat.CONCENTRATION in mage.conditions)
    }
}
