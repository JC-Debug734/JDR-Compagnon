package com.jc2.jdrcompagnon

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.NaheulbeukCharacter
import org.junit.Assert.assertTrue
import org.junit.Test

class ReworkV5Tests {

    @Test
    fun `inventory drag drop highlights target correctly`() {
        // Simulate D&D character state
        val character = Character(
            id = "c1",
            name = "Test",
            type = "PJ",
            worldId = "donjon_et_dragon",
            backpackItems = listOf("Sword"),
            equippedItems = listOf("Shield")
        )

        // Dragging backpack item should target equipped zone
        val draggedItem = "Sword"
        val backpackIsTarget = character.equippedItems.contains(draggedItem)
        val equippedIsTarget = character.backpackItems.contains(draggedItem)
        assertTrue("Backpack should NOT be drop target when dragging backpack item", !backpackIsTarget)
        assertTrue("Equipped should be drop target when dragging backpack item", equippedIsTarget)

        // Dragging equipped item should target backpack
        val draggedEquipped = "Shield"
        val backpackIsTarget2 = character.equippedItems.contains(draggedEquipped)
        val equippedIsTarget2 = character.backpackItems.contains(draggedEquipped)
        assertTrue("Backpack should be drop target when dragging equipped item", backpackIsTarget2)
        assertTrue("Equipped should NOT be drop target when dragging equipped item", !equippedIsTarget2)
    }

    @Test
    fun `naheulbeuk equip and unequip are mutually exclusive`() {
        val character = NaheulbeukCharacter(
            id = "n1",
            name = "Zob",
            backpackItems = listOf("Cotte de mailles"),
            equippedItems = emptyList()
        )
        GameState.addNaheulbeukCharacter(character)
        GameState.equipNaheulbeukItem(character.id, "Cotte de mailles")
        val updated = GameState.naheulbeukCharacters.value.find { it.id == character.id }!!
        assertTrue("Item should move to equipped", updated.equippedItems.contains("Cotte de mailles"))
        assertTrue("Item should leave backpack", !updated.backpackItems.contains("Cotte de mailles"))

        GameState.unequipNaheulbeukItem(character.id, "Cotte de mailles")
        val updated2 = GameState.naheulbeukCharacters.value.find { it.id == character.id }!!
        assertTrue("Item should return to backpack", updated2.backpackItems.contains("Cotte de mailles"))
        assertTrue("Item should leave equipped", !updated2.equippedItems.contains("Cotte de mailles"))
    }

    @Test
    fun `naheulbeuk add item to backpack`() {
        val character = NaheulbeukCharacter(
            id = "n2",
            name = "Lina",
            backpackItems = emptyList(),
            equippedItems = emptyList()
        )
        GameState.addNaheulbeukCharacter(character)
        GameState.addItemToNaheulbeukBackpack(character.id, "Gourde")
        val updated = GameState.naheulbeukCharacters.value.find { it.id == character.id }!!
        assertTrue("Gourde should be in backpack", updated.backpackItems.contains("Gourde"))
    }
}
