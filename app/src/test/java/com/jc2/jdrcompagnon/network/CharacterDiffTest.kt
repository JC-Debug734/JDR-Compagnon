package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.ui.Character
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterDiffTest {

    private val base = Character(id = "pj-1", name = "Aria", type = "PJ")

    @Test
    fun `liste - seuls les objets divergents sont proposes`() {
        val local = base.copy(backpackItems = listOf("Corde", "Torche", "Rations", "Potion de soins"))
        val remote = base.copy(backpackItems = listOf("Torche", "Corde", "Rations", "Pied-de-biche"))

        val diff = diffCharacters(local, remote).single()

        assertTrue(diff.isListDiff)
        assertEquals("Potion de soins", diff.localValue)
        assertEquals("Pied-de-biche", diff.remoteValue)
    }

    @Test
    fun `liste - un simple changement d'ordre n'est pas une difference`() {
        val local = base.copy(equippedItems = listOf("Épée", "Bouclier"))
        val remote = base.copy(equippedItems = listOf("Bouclier", "Épée"))

        assertTrue(diffCharacters(local, remote).isEmpty())
    }

    @Test
    fun `liste - doublons comptes`() {
        val local = base.copy(backpackItems = listOf("Torche", "Torche"))
        val remote = base.copy(backpackItems = listOf("Torche"))

        val diff = diffCharacters(local, remote).single()

        assertEquals("Torche", diff.localValue)
        assertEquals("—", diff.remoteValue)
    }

    @Test
    fun `equipement texte - compare objet par objet`() {
        val local = base.copy(equipment = "Lance, cotte de mailles, bouclier")
        val remote = base.copy(equipment = "Lance, cotte de mailles, arc long")

        val diff = diffCharacters(local, remote).single()

        assertEquals("bouclier", diff.localValue)
        assertEquals("arc long", diff.remoteValue)
    }

    @Test
    fun `champ simple - valeurs completes`() {
        val diff = diffCharacters(base.copy(gold = 12), base.copy(gold = 30)).single()

        assertEquals("Or", diff.fieldLabel)
        assertEquals("12", diff.localValue)
        assertEquals("30", diff.remoteValue)
    }
}
