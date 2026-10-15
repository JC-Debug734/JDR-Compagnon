package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionInfligee
import com.jc2.jdrcompagnon.feature_combat.domain.model.ModeJet
import com.jc2.jdrcompagnon.feature_combat.domain.model.Sauvegardes
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SauvegardesAutoTest {

    /** Dés imposés : chaque tirage renvoie la valeur suivante (en boucle). */
    private class DesFixes(private vararg val valeurs: Int) : Random() {
        private var i = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(from: Int, until: Int): Int = valeurs[i++ % valeurs.size].coerceIn(from, until - 1)
    }

    private fun pj(nom: String, pv: Int = 20, initiative: Int = 20, sauvegardes: Map<String, Int> = emptyMap()) =
        Combattant(id = nom, nom = nom, estMonstre = false, ca = 14, pvMax = 20, pv = pv,
            bonusInitiative = 0, initiative = initiative, sauvegardes = sauvegardes)

    private fun monstre(nom: String, pv: Int = 10, initiative: Int = 5) =
        Combattant(id = nom, nom = nom, estMonstre = true, monstreNom = nom, ca = 12, pvMax = pv, pv = pv,
            bonusInitiative = 0, initiative = initiative)

    @After
    fun nettoyer() {
        CombatSession.fermer()
        CombatSession.aleatoire = Random
    }

    @Test
    fun `bonus de sauvegarde lus sur la fiche du bestiaire`() {
        val fiche = "**Caractéristiques :** For 27 +8 +8 Dex 10 +0 +6 Con 25 +7 +7 / Int 16 +3 +3 Sag 13 +1 +7 Cha 23 −6 −6"
        val js = Sauvegardes.depuisFiche(fiche)
        assertEquals(6, js["Dextérité"])
        assertEquals(7, js["Sagesse"])
        assertEquals(-6, js["Charisme"])
        assertEquals("Dextérité", Sauvegardes.normaliser("Dex"))
        assertEquals("Constitution", Sauvegardes.normaliser("constitution"))
    }

    @Test
    fun `etats qui font rater ou donnent le desavantage`() {
        val paralyse = pj("A", sauvegardes = mapOf("Dextérité" to 10)).copy(conditions = setOf(ConditionCombat.PARALYSE))
        val r = Sauvegardes.lancer(paralyse, "Dextérité", 5, DesFixes(20))
        assertFalse(r.reussi)
        assertNull(r.d20)

        val entrave = pj("B", sauvegardes = mapOf("Dextérité" to 2)).copy(conditions = setOf(ConditionCombat.ENTRAVE))
        val r2 = Sauvegardes.lancer(entrave, "Dex", 12, DesFixes(18, 4))
        assertEquals(ModeJet.DESAVANTAGE, r2.mode)
        assertEquals(4, r2.d20)
        assertFalse(r2.reussi)

        // « JS Force ou Dextérité » : la cible prend sa meilleure sauvegarde.
        val agile = pj("C", sauvegardes = mapOf("Force" to 0, "Dextérité" to 5))
        val r3 = Sauvegardes.lancer(agile, null, 15, DesFixes(10), choix = listOf("Force", "Dextérité"))
        assertEquals("Dextérité", r3.caracteristique)
        assertTrue(r3.reussi)
    }

    @Test
    fun `JS d'un sort lu dans le resume de la declaration`() {
        val boule = Sauvegardes.duResume("JS Dextérité DD 15 (${Sauvegardes.MOITIE_SI_REUSSI}) · 8d6 feu")!!
        assertEquals("Dextérité", boule.caracteristique)
        assertEquals(15, boule.dd)
        assertTrue(boule.moitieSiReussi)
        assertFalse(Sauvegardes.duResume("JS Dextérité DD 13 · 1d8 radiant")!!.moitieSiReussi)
        assertNull(Sauvegardes.duResume("Épée longue +5 · 1d8 + 3"))
    }

    @Test
    fun `action a jet de sauvegarde d'un monstre resolue par l'application`() {
        CombatSession.aleatoire = DesFixes(3)
        val souffle = AttaqueMonstre(
            "Souffle", TypeAttaqueMonstre.SAUVEGARDE, formuleDegats = "2d6", degatsMoyens = 7, sauvegarde = "Constitution", dd = 30,
            conditions = listOf(ConditionInfligee(ConditionCombat.EMPOISONNE, "Constitution", 30)),
        )
        CombatSession.demarrer("Test", listOf(pj("Aria"), monstre("Wyrm", pv = 50)))
        CombatSession.commencer()
        CombatSession.modifierDeclaration(DeclarationAction("Wyrm", "Souffle", cibleId = "Aria", attaque = souffle))
        CombatSession.resoudreAttaqueSauvegarde("Wyrm")
        val etat = CombatSession.etat.value!!
        val aria = etat.combattants.first { it.id == "Aria" }
        // JS impossible (DD 30) : dégâts entiers (2d6 = 3 + 3) et état infligé.
        assertEquals(14, aria.pv)
        assertTrue(ConditionCombat.EMPOISONNE in aria.conditions)
        assertTrue(etat.journal.any { "JS Constitution" in it && "raté" in it })
        assertTrue(etat.declarations["Wyrm"]!!.degatsAppliques)
        // Une seule résolution par déclaration.
        CombatSession.resoudreAttaqueSauvegarde("Wyrm")
        assertEquals(14, CombatSession.etat.value!!.combattants.first { it.id == "Aria" }.pv)
    }

    @Test
    fun `jet contre la mort lance par l appli a la demande du MJ`() {
        CombatSession.aleatoire = DesFixes(5)
        CombatSession.demarrer("Test", listOf(pj("Aria", pv = 0, initiative = 20), monstre("Gob", initiative = 1)))
        CombatSession.commencer()
        // Le jet attend le MJ : « Lancer pour lui ».
        CombatSession.noterJetContreLaMort("Aria", null)
        val aria = CombatSession.etat.value!!.combattants.first { it.id == "Aria" }
        assertEquals(1, aria.echecsMort)
        // Frappé à terre : un échec de plus.
        CombatSession.appliquerPv("Aria", 3)
        assertEquals(2, CombatSession.etat.value!!.combattants.first { it.id == "Aria" }.echecsMort)
        CombatSession.appliquerPv("Aria", 3)
        assertTrue(CombatSession.etat.value!!.combattants.first { it.id == "Aria" }.mort)
    }

    @Test
    fun `un 20 naturel au jet contre la mort rend 1 PV`() {
        CombatSession.aleatoire = DesFixes(20)
        CombatSession.demarrer("Test", listOf(pj("Aria", pv = 0, initiative = 20), monstre("Gob", initiative = 1)))
        CombatSession.commencer()
        CombatSession.noterJetContreLaMort("Aria", null)
        assertEquals(1, CombatSession.etat.value!!.combattants.first { it.id == "Aria" }.pv)
    }

    @Test
    fun `le combat se termine quand le dernier ennemi tombe`() {
        CombatSession.demarrer("Test", listOf(pj("Aria"), monstre("Gob1"), monstre("Gob2")))
        CombatSession.appliquerPv("Gob1", 99)
        CombatSession.appliquerPv("Gob2", 99)
        // Pas encore commencé : pas de fin automatique pendant la préparation.
        assertFalse(CombatSession.etat.value!!.termine)
        CombatSession.fermer()

        CombatSession.demarrer("Test", listOf(pj("Aria"), monstre("Gob1"), monstre("Gob2")))
        CombatSession.commencer()
        CombatSession.appliquerPv("Gob1", 99)
        assertFalse(CombatSession.etat.value!!.termine)
        CombatSession.appliquerPv("Gob2", 99)
        assertTrue(CombatSession.etat.value!!.termine)
    }
}
