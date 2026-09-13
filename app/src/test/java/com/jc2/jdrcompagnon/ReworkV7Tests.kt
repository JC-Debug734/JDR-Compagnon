package com.jc2.jdrcompagnon

import com.jc2.jdrcompagnon.ui.ArmorRules
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests pour les deltas V7 axes 3 & 4 :
 * - CA slot-based (ArmorRules)
 * - Migration legacy équipé -> slots
 * - Calcul de poids / charge
 */
class ReworkV7Tests {

    private val baseCharacter = Character(
        id = "test-char",
        name = "Test",
        type = "PJ",
        race = "Humain",
        characterClass = "Guerrier",
        level = 5,
        background = "Soldat",
        alignment = "Neutre Bon",
        worldId = "",
        strength = 14,
        dexterity = 16,
        constitution = 12,
        intelligence = 10,
        wisdom = 13,
        charisma = 8,
        maxHitPoints = 40,
        currentHitPoints = 40,
        temporaryHitPoints = 0,
        armorClass = 10,
        speed = 30,
        initiative = 3,
        proficiencyBonus = 3,
        skills = emptyMap(),
        savingThrows = emptyMap(),
        traits = "",
        equipment = "",
        dmNotes = "",
        backpackItems = emptyList(),
        equippedItems = emptyList(),
        equippedSlots = emptyMap()
    )

    @Before
    fun setup() {
        // Remettre l'état à vide pour l'isolation
        ArmorRules.clearCache()
    }

    @Test
    fun `abilityModifier calcule correctement les modificateurs D&D 5e`() {
        assertEquals(-4, ArmorRules.abilityModifierPublic(2))
        assertEquals(-1, ArmorRules.abilityModifierPublic(8))
        assertEquals(0, ArmorRules.abilityModifierPublic(10))
        assertEquals(0, ArmorRules.abilityModifierPublic(11))
        assertEquals(+1, ArmorRules.abilityModifierPublic(12))
        assertEquals(+3, ArmorRules.abilityModifierPublic(16))
        assertEquals(+5, ArmorRules.abilityModifierPublic(20))
    }

    @Test
    fun `CA base sans armure = 10 plus mod Dex`() {
        val ac = ArmorRules.effectiveArmorClass(baseCharacter)
        assertEquals(13, ac) // 10 + 3 (Dex 16)
    }

    @Test
    fun `armure legere ajoute base Dex mod`() {
        val c = baseCharacter.copy(
            equippedSlots = mapOf(EquipmentSlot.TORSO to "Armure de cuir")
        )
        // Armure de cuir = base 11 + Dex mod 3 = 14
        assertEquals(14, ArmorRules.effectiveArmorClass(c))
    }

    @Test
    fun `bouclier donne plus 2 et s'equipe en main secondaire`() {
        val c = baseCharacter.copy(
            equippedSlots = mapOf(EquipmentSlot.OFF_HAND to "Bouclier")
        )
        // 10 + Dex 3 + 2 = 15
        assertEquals(15, ArmorRules.effectiveArmorClass(c))
    }

    @Test
    fun `armure plus bouclier stack correctement`() {
        val c = baseCharacter.copy(
            equippedSlots = mapOf(
                EquipmentSlot.TORSO to "Armure de cuir",
                EquipmentSlot.OFF_HAND to "Bouclier"
            )
        )
        // 11 + 3 + 2 = 16
        assertEquals(16, ArmorRules.effectiveArmorClass(c))
    }

    @Test
    fun `armure moyenne limite le bonus Dex a 2`() {
        val c = baseCharacter.copy(
            equippedSlots = mapOf(EquipmentSlot.TORSO to "Chemise de mailles")
        )
        // Chemise de mailles base 13 + max Dex 2 = 15
        assertEquals(15, ArmorRules.effectiveArmorClass(c))
    }

    @Test
    fun `armure lourde ignore le bonus Dex`() {
        val c = baseCharacter.copy(
            equippedSlots = mapOf(EquipmentSlot.TORSO to "Harnois")
        )
        // Harnois base 18
        assertEquals(18, ArmorRules.effectiveArmorClass(c))
    }

    @Test
    fun `slot mapping pour armes et armures`() {
        assertEquals(EquipmentSlot.TORSO, ArmorRules.slotForItem("Armure de cuir"))
        assertEquals(EquipmentSlot.TORSO, ArmorRules.slotForItem("Harnois"))
        assertEquals(EquipmentSlot.OFF_HAND, ArmorRules.slotForItem("Bouclier"))
        assertEquals(EquipmentSlot.HEAD, ArmorRules.slotForItem("Casque"))
        assertEquals(EquipmentSlot.MAIN_HAND, ArmorRules.slotForItem("Épée longue"))
        assertEquals(EquipmentSlot.BACK, ArmorRules.slotForItem("Sac à dos"))
    }

    @Test
    fun `legacy equippedItems migre automatiquement vers equippedSlots au chargement`() {
        val legacy = baseCharacter.copy(
            equippedItems = listOf("Armure de cuir", "Bouclier", "Épée longue"),
            equippedSlots = emptyMap()
        )
        val migrated = ArmorRules.migrateEquippedItemsToSlots(legacy)
        assertEquals(EquipmentSlot.TORSO, ArmorRules.slotForItem(migrated.equippedSlots[EquipmentSlot.TORSO] ?: ""))
        assertEquals(EquipmentSlot.OFF_HAND, ArmorRules.slotForItem(migrated.equippedSlots[EquipmentSlot.OFF_HAND] ?: ""))
        assertEquals(EquipmentSlot.MAIN_HAND, ArmorRules.slotForItem(migrated.equippedSlots[EquipmentSlot.MAIN_HAND] ?: ""))
    }

    @Test
    fun `calcul de charge max et surcharge`() {
        val c = baseCharacter.copy(strength = 10)
        // Charge max = FOR * 7.5 = 75 kg
        assertEquals(75.0, GameState.maxCarryWeight(c), 0.01)

        ArmorRules.setTestWeight("Harnois lourd", 70.0)
        val overloaded = c.copy(backpackItems = listOf("Harnois lourd", "Harnois lourd"))
        // Poids total converti en kg : 140 lb / 2.20462 ≈ 63.5 kg ; max = 75 kg, donc pas surchargé.
        assertEquals(false, GameState.totalEquipmentWeight(overloaded) > GameState.maxCarryWeight(overloaded))
    }
}
