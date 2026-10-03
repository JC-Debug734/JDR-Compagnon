package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Consommables
import com.jc2.jdrcompagnon.ui.screens.joueur.character.DureeEffet
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EffetsEtTempsDeCombatTest {

    @After
    fun nettoyer() {
        CombatSession.fermer()
    }

    @Test
    fun `duree des sorts lue sur la ligne Duree`() {
        val concentration = DureeEffet.depuisSort("Durée: Concentration, jusqu’à 1 minute\n\nTexte du sort.")!!
        assertEquals(60L, concentration.secondes)
        assertTrue(concentration.concentration)
        assertEquals(8 * 3600L, DureeEffet.depuisSort("Durée: 8 heures")!!.secondes)
        assertNull(DureeEffet.depuisSort("Durée: instantanée"))
    }

    @Test
    fun `duree et soins des consommables lus dans leur description`() {
        val objets = EquipmentParser.parse(
            """
            ### Potion de respiration aquatique
            **Type** Potions
            **Consommable** Oui
            **Description** Objet magique, peu courant. Vous pouvez respirer sous l'eau pendant 1 heure après avoir bu cette potion.
            ---
            ### Potion de guérison
            **Type** Potions
            **Consommable** Oui
            **Description** Objet magique, courant. Vous regagnez 2d4 + 2 points de vie en buvant cette potion.
            ---
            ### Poussière de disparition
            **Type** Objets merveilleux
            **Consommable** Oui
            **Description** Vous devenez invisibles pendant 2d4 minutes.
            ---
            """.trimIndent()
        )
        val (respiration, guerison, poussiere) = objets
        assertTrue(objets.all { it.consommable })
        assertEquals(3600L, DureeEffet.depuisDescription(respiration.rawMarkdown)!!.secondes)
        assertNull(Consommables.formuleSoin(respiration))
        assertEquals("2d4+2", Consommables.formuleSoin(guerison))
        assertNull(DureeEffet.depuisDescription(guerison.rawMarkdown))
        val rnd = Random(3)
        repeat(200) { assertTrue(DureeEffet.depuisDescription(poussiere.rawMarkdown, rnd)!!.secondes in 120L..480L) }
        assertEquals("effet 2d4 minute(s)", Consommables.resume(poussiere))
    }

    @Test
    fun `l'horloge avance a la seconde pres et change de minute`() {
        val avant = ScenarioClockState.totalSeconds()
        ScenarioClockState.advanceSeconds(65)
        assertEquals(avant + 65, ScenarioClockState.totalSeconds())
        assertTrue(ScenarioClockState.state.value.scenarioSeconds in 0..59)
    }

    @Test
    fun `pendant un combat l'horloge est en pause et chaque round dure 6 secondes`() {
        val heros = Combattant(id = "h", nom = "Héros", estMonstre = false, ca = 12, pvMax = 10, pv = 10, bonusInitiative = 0, initiative = 10)
        CombatSession.demarrer("Test", listOf(heros))
        assertTrue(ScenarioClockState.pauseCombat.value)

        val debut = ScenarioClockState.totalSeconds()
        CombatSession.commencer()
        assertEquals("la préparation et le round 1 ne font pas avancer le temps", debut, ScenarioClockState.totalSeconds())

        CombatSession.passerEnResolution()
        CombatSession.tourSuivant() // fin du round 1 → round 2
        assertEquals(2, CombatSession.etat.value!!.round)
        assertEquals(debut + 6, ScenarioClockState.totalSeconds())

        CombatSession.fermer() // le round 2 entamé compte aussi
        assertEquals(debut + 12, ScenarioClockState.totalSeconds())
        assertFalse(ScenarioClockState.pauseCombat.value)
    }
}
