package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportement
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportements
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.IaMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.StrategieCible
import com.jc2.jdrcompagnon.feature_combat.domain.model.StyleCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComportementsTest {

    private val epee = AttaqueMonstre("Épée", TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = 5, formuleDegats = "1d8 + 3", degatsMoyens = 7)

    private fun pj(nom: String, pv: Int = 20, joue: Boolean = false, profil: ProfilIA? = null) =
        Combattant(id = nom, nom = nom, estMonstre = false, characterId = "c-$nom", ca = 14, pvMax = 20, pv = pv,
            bonusInitiative = 0, initiative = 10, attaques = listOf(epee), profilIA = profil, joueParJoueur = joue)

    private fun gobelin(nom: String, pv: Int = 20) =
        Combattant(id = nom, nom = nom, estMonstre = true, monstreNom = "Gobelin", ca = 12, pvMax = 20, pv = pv,
            bonusInitiative = 0, initiative = 15, attaques = listOf(epee), profilIA = ProfilIA.BRUTE)

    @After
    fun nettoyer() {
        CombatSession.fermer()
        CombatSession.personnagesJoues = { emptySet() }
    }

    @Test
    fun `la grille donne le comportement par classe, sans tenir compte des accents ni de la casse`() {
        assertEquals(ProfilIA.DUELLISTE.name, Comportements.pourClasse("Guerrier").id)
        assertEquals(ProfilIA.TIREUR.name, Comportements.pourClasse("rodeur").id)
        assertEquals(ProfilIA.ARTILLEUR.name, Comportements.pourClasse("MAGICIEN").id)
        // Classe non citée : ligne « Autre classe ».
        assertEquals(ProfilIA.BRUTE.name, Comportements.pourClasse("Artificier").id)
    }

    @Test
    fun `un personnage joue par un joueur n'est jamais pilote par l'IA`() {
        assertTrue(pj("Aria", profil = ProfilIA.DUELLISTE).piloteParIa)
        assertFalse(pj("Aria", profil = ProfilIA.DUELLISTE, joue = true).piloteParIa)
        assertFalse(pj("Aria").piloteParIa)
    }

    @Test
    fun `un comportement cree par le MJ est applique par l'IA`() {
        val chasseur = Comportement(
            id = "chasseur", nom = "Chasseur", cible = StrategieCible.PLUS_BLESSE, style = StyleCombat.MELEE,
        )
        val monstre = gobelin("Gob").copy(comportementId = "chasseur")
        // Le comportement n'est pas enregistré : retombe sur son profil (Brute).
        assertEquals("Brute", monstre.comportement.nom)
        Comportements.enregistrer(chasseur)
        try {
            assertEquals("Chasseur", monstre.comportement.nom)
            val decision = IaMonstre.decider(
                monstre,
                listOf(pj("Sain"), pj("Blesse", pv = 3)),
                distances = mapOf("Sain" to Distance.CONTACT, "Blesse" to Distance.CONTACT),
                aleatoire = Random(1),
            )
            assertEquals("Blesse", decision.cibleId)
            assertEquals("Chasseur", decision.comportementNom)
        } finally {
            Comportements.supprimerOuReinitialiser("chasseur")
        }
    }

    @Test
    fun `un joueur qui reprend son personnage efface la decision de l'IA`() {
        CombatSession.demarrer("Test", listOf(pj("Aria", profil = ProfilIA.DUELLISTE), pj("Bran", joue = true), gobelin("Gob")))
        CombatSession.commencer()
        // Le gobelin (initiative 15) joue d'abord, puis l'IA décide pour Aria à son tour.
        assertEquals(PhaseCombat.EN_COURS, CombatSession.etat.value!!.phase)
        assertEquals("Gob", CombatSession.etat.value!!.actif?.id)
        CombatSession.tourSuivant()
        assertEquals("Aria", CombatSession.etat.value!!.actif?.id)
        assertTrue(CombatSession.etat.value!!.declarations["Aria"]?.parIa == true)
        // Son joueur se connecte pendant son tour : la décision de l'IA est retirée, il choisit lui-même.
        CombatSession.marquerJoues(setOf("c-Aria"))
        val etat = CombatSession.etat.value!!
        assertTrue(etat.combattants.first { it.id == "Aria" }.joueParJoueur)
        assertNull(etat.declarations["Aria"])
    }
}
