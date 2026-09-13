package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Règles de calcul de la Classe d'Armure (CA) pour D&D 5e.
 * Séparées de GameState pour rester testables et réutilisables.
 *
 * V7 (slot-based) : la CA se calcule UNIQUEMENT depuis equippedSlots.
 * - TORSO occupé par une armure reconnue → base armure + mod Dex (max)
 * - TORSO vide → 10 + mod Dex
 * - OFF_HAND contient "bouclier" → +2
 */
object ArmorRules {

    /**
     * Détail du calcul de CA : valeur totale, texte explicatif et
     * indicateurs sur l'armure/bouclier équipés.
     */
    data class AcBreakdown(
        val total: Int,
        val label: String,
        val detail: String,
        val hasArmor: Boolean,
        val hasShield: Boolean,
        val armorName: String? = null,
        val maxDex: Int? = null
    )

    /**
     * Calcule la CA détaillée d'un personnage D&D depuis equippedSlots.
     */
    fun computeAc(character: Character): AcBreakdown {
        val dexMod = abilityModifier(character.dexterity)
        val torsoItem = character.equippedSlots[EquipmentSlot.TORSO]
        val armorSpec = torsoItem?.let { armorSpec(it) }
        val baseAc = armorSpec?.baseAc ?: 10
        val maxDex = armorSpec?.maxDexBonus

        val appliedDex = when {
            armorSpec == null -> dexMod
            armorSpec.category == ArmorCategory.HEAVY -> 0
            maxDex != null -> dexMod.coerceAtMost(maxDex)
            else -> dexMod
        }

        val offHandItem = character.equippedSlots[EquipmentSlot.OFF_HAND]
        val hasShield = offHandItem != null && offHandItem.contains("bouclier", ignoreCase = true)
        val shieldBonus = if (hasShield) 2 else 0
        val total = baseAc + appliedDex + shieldBonus

        val detail = buildString {
            val parts = mutableListOf<String>()
            if (armorSpec != null) {
                parts.add(armorSpec.displayName)
            }
            if (appliedDex != 0 || armorSpec == null) {
                parts.add(if (armorSpec != null && maxDex != null && dexMod > maxDex) "Dex (max +$maxDex)" else "Dex")
            }
            if (hasShield) {
                parts.add("Bouclier")
            }
            append(if (parts.isEmpty()) "Base" else parts.joinToString(" + "))
        }

        return AcBreakdown(
            total = total,
            label = "CA $total",
            detail = "CA $total ($detail)",
            hasArmor = armorSpec != null,
            hasShield = hasShield,
            armorName = armorSpec?.displayName ?: torsoItem,
            maxDex = maxDex
        )
    }


    private enum class ArmorCategory { LIGHT, MEDIUM, HEAVY }

    private data class ArmorSpec(
        val displayName: String,
        val baseAc: Int,
        val category: ArmorCategory,
        val maxDexBonus: Int? = null
    )

    private fun abilityModifier(score: Int): Int = (score - 10) / 2

    internal fun abilityModifierPublic(score: Int): Int = abilityModifier(score)

    /**
     * Renvoie la spécification d'une armure reconnue, ou null si ce n'est pas une armure.
     * Le nom affiché est normalisé pour un rendu plus propre.
     */
    private fun armorSpec(itemName: String): ArmorSpec? {
        return when {
            itemName.contains("harnois", ignoreCase = true) ->
                ArmorSpec("Harnois", 18, ArmorCategory.HEAVY)
            itemName.contains("demi-plate", ignoreCase = true) ||
                    itemName.contains("demi plate", ignoreCase = true) ->
                ArmorSpec("Demi-plate", 15, ArmorCategory.HEAVY)
            itemName.contains("clibanion", ignoreCase = true) ->
                ArmorSpec("Clibanion", 17, ArmorCategory.HEAVY)
            itemName.contains("cotte de mailles", ignoreCase = true) ->
                ArmorSpec("Cotte de mailles", 16, ArmorCategory.HEAVY)
            itemName.contains("broigne", ignoreCase = true) ->
                ArmorSpec("Broigne", 14, ArmorCategory.HEAVY)
            itemName.contains("plastron", ignoreCase = true) ||
                    itemName.contains("armure d'écailles", ignoreCase = true) ||
                    itemName.contains("armure d'ecailles", ignoreCase = true) ||
                    itemName.contains("armure d'anneaux", ignoreCase = true) ->
                ArmorSpec("Plastron", 14, ArmorCategory.HEAVY)
            itemName.contains("chemise de mailles", ignoreCase = true) ->
                ArmorSpec("Chemise de mailles", 13, ArmorCategory.MEDIUM, maxDexBonus = 2)
            itemName.contains("cotte de clous", ignoreCase = true) ->
                ArmorSpec("Cotte de clous", 13, ArmorCategory.MEDIUM, maxDexBonus = 2)
            itemName.contains("armure de peau", ignoreCase = true) ->
                ArmorSpec("Armure de peau", 12, ArmorCategory.MEDIUM, maxDexBonus = 2)
            itemName.contains("armure de cuir clouté", ignoreCase = true) ->
                ArmorSpec("Armure de cuir clouté", 13, ArmorCategory.LIGHT)
            itemName.contains("armure de cuir", ignoreCase = true) ->
                ArmorSpec("Armure de cuir", 11, ArmorCategory.LIGHT)
            itemName.contains("armure matelassée", ignoreCase = true) ||
                    itemName.contains("matelassée", ignoreCase = true) ||
                    itemName.contains("matelassee", ignoreCase = true) ->
                ArmorSpec("Armure matelassée", 11, ArmorCategory.LIGHT)
            else -> null
        }
    }

    /**
     * Détermine le slot naturel d'un item pour la gestion d'équipement V7.
     * Retourne null si l'item n'a pas de slot reconnu (place dans le sac).
     */
    fun slotForItem(itemName: String): EquipmentSlot? {
        val lower = itemName.lowercase()
        return when {
            armorSpec(itemName) != null -> EquipmentSlot.TORSO
            lower.contains("bouclier") -> EquipmentSlot.OFF_HAND
            isTwoHandedWeapon(itemName) -> EquipmentSlot.MAIN_HAND
            lower.contains("arc") ||
                    lower.contains("hallebarde") ||
                    lower.contains("pique") ||
                    lower.contains("gourdin") ||
                    lower.contains("dague") ||
                    lower.contains("hache") ||
                    lower.contains("épée") ||
                    lower.contains("epee") ||
                    lower.contains("lance") ||
                    lower.contains("marteau") ||
                    lower.contains("rapière") ||
                    lower.contains("arbalette") ||
                    lower.contains("masse") ||
                    lower.contains("serpe") ||
                    lower.contains("trident") ||
                    lower.contains("couteau") ||
                    lower.contains("fléau") ||
                    lower.contains("fleau") ||
                    lower.contains("fléchette") ||
                    lower.contains("flechette") ||
                    lower.contains("javeline") ||
                    lower.contains("sarbacane") ||
                    lower.contains("filet") ||
                    lower.contains("bâton") ||
                    lower.contains("baton") ||
                    lower.contains("maillet") ||
                    lower.contains("morgenstern") ||
                    lower.contains("pic de guerre") ||
                    lower.contains("fouet") -> EquipmentSlot.MAIN_HAND
            lower.contains("casque") ||
                    lower.contains("chapeau") ||
                    lower.contains("bandeau") ||
                    lower.contains("diadème") ||
                    lower.contains("couronne") -> EquipmentSlot.HEAD
            lower.contains("cape") ||
                    lower.contains("manteau") ||
                    lower.contains("tapis") ||
                    lower.contains("sac à dos") ||
                    lower.contains("carquois") -> EquipmentSlot.BACK
            lower.contains("anneau") ||
                    lower.contains("amulette") ||
                    lower.contains("collier") ||
                    lower.contains("bracelet") ||
                    lower.contains("ceinture") ||
                    lower.contains("gant") ||
                    lower.contains("botte") ||
                    lower.contains("bottine") -> EquipmentSlot.ACCESSORY
            else -> null
        }
    }

    private fun isTwoHandedWeapon(itemName: String): Boolean {
        val lower = itemName.lowercase()
        val twoHandedKeywords = listOf("deux mains", "2 mains", "2m", "hallebarde", "arc", "arbalette", "pique")
        return twoHandedKeywords.any { lower.contains(it) }
    }

    /**
     * Retourne true si l'item est une arme à deux mains.
     */
    fun twoHanded(itemName: String): Boolean = isTwoHandedWeapon(itemName)

    /**
     * Poids temporaires pour les tests sans contexte Android.
     */
    private val testWeightOverrides = mutableMapOf<String, Double>()

    /**
     * Remplace le poids d'un item pour les tests unitaires.
     */
    fun setTestWeight(itemName: String, pounds: Double) {
        testWeightOverrides[itemName] = pounds
    }

    /**
     * Efface tous les overrides de test.
     */
    fun clearTestWeights() {
        testWeightOverrides.clear()
    }

    /**
     * Point d'entrée public pour effacer les caches de tests.
     */
    fun clearCache() {
        SrdRepository.clearCache()
        clearTestWeights()
    }

    /**
     * Alias public de computeAc pour les tests et GameState.
     */
    fun effectiveArmorClass(character: Character): Int = computeAc(character).total

    /**
     * Alias public de computeAc pour le détail.
     */
    fun armorClassBreakdown(character: Character): AcBreakdown = computeAc(character)

    /**
     * Migre equippedItems legacy vers equippedSlots.
     */
    fun migrateEquippedItemsToSlots(character: Character): Character {
        val migratedSlots = character.equippedItems.mapNotNull { item ->
            slotForItem(item)?.let { slot -> slot to item }
        }.toMap()
        return character.copy(
            equippedSlots = character.equippedSlots + migratedSlots,
            equippedItems = character.equippedItems.filter { item ->
                val slot = slotForItem(item)
                slot == null || character.equippedSlots[slot] == item
            }
        )
    }

    /**
     * Retourne le poids en livres d'un item connu, ou null.
     */
    fun weightInPounds(itemName: String): Double? {
        testWeightOverrides[itemName]?.let { return it }
        val item = SrdRepository.findEquipmentItemCached(itemName)
        return item?.weight?.let { parseWeight(it) }
    }

    private fun parseWeight(weight: String): Double? {
        return weight.trim()
            .replace(" lb", "", ignoreCase = true)
            .replace(" kg", "", ignoreCase = true)
            .replace("1/4", "0.25")
            .replace("1/2", "0.5")
            .replace(",", ".")
            .toDoubleOrNull()
            ?.let { if (weight.contains("kg", ignoreCase = true)) it / 0.453592 else it }
    }
}
