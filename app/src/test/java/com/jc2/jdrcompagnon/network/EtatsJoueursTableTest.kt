package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.ui.Character
import org.junit.Assert.assertEquals
import org.junit.Test

class EtatsJoueursTableTest {

    @Test
    fun `les etats d'un joueur viennent de sa fiche et de son combattant, sans concentration ni monstres`() {
        val aria = Character(id = "a", name = "Aria", type = "PJ", condition = "Empoisonnée, Ivre")
        val pnj = Character(id = "p", name = "Toblen", type = "PNJ", condition = "Étourdi")
        val combattants = listOf(
            Combattant(id = "c1", nom = "Aria", estMonstre = false, characterId = "a", ca = 12, pvMax = 10, pv = 10, bonusInitiative = 0,
                conditions = setOf(ConditionCombat.A_TERRE, ConditionCombat.CONCENTRATION)),
            Combattant(id = "c2", nom = "Orc", estMonstre = true, ca = 13, pvMax = 15, pv = 15, bonusInitiative = 1, conditions = setOf(ConditionCombat.ETOURDI)),
        )
        val etats = NetworkSessionManager.etatsDesJoueurs(listOf(aria, pnj), combattants)
        assertEquals(setOf("Aria"), etats.keys)
        assertEquals(setOf(ConditionCombat.EMPOISONNE, ConditionCombat.A_TERRE), etats["Aria"])
    }
}
