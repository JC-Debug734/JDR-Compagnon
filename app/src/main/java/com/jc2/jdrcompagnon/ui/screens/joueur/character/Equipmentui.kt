package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules

/**
 * Point unique pour tout ce qui concerne l'affichage d'un équipement
 * (couleur d'emplacement, poids, nombre de mains). S'appuie sur ArmorRules,
 * qui reste la seule source de vérité pour les données SRD par nom d'objet.
 */

/** Couleur fixe associée à chaque emplacement. `null` = objet sans emplacement reconnu (sac, divers...). */
fun EquipmentSlot?.slotColor(): Color = when (this) {
    EquipmentSlot.MAIN_HAND -> Color(0xFFC0524D)   // Arme
    EquipmentSlot.OFF_HAND -> Color(0xFF5B9B6B)    // Bouclier / main secondaire
    EquipmentSlot.TORSO -> Color(0xFF4A7FB5)       // Armure
    EquipmentSlot.HEAD -> Color(0xFF4A7FB5)        // Casque (même famille que l'armure)
    EquipmentSlot.BACK -> Color(0xFFB08A4E)        // Cape / sac
    EquipmentSlot.ACCESSORY -> Color(0xFF9B6BC7)   // Anneau, amulette...
    null -> Color(0xFF8A93A6)                       // Objet divers
}

/** Libellé français affiché à côté de la pastille de couleur. */
fun EquipmentSlot?.slotLabel(): String = when (this) {
    EquipmentSlot.MAIN_HAND -> "Arme"
    EquipmentSlot.OFF_HAND -> "Bouclier"
    EquipmentSlot.TORSO -> "Armure"
    EquipmentSlot.HEAD -> "Casque"
    EquipmentSlot.BACK -> "Sac / Dos"
    EquipmentSlot.ACCESSORY -> "Accessoire"
    null -> "Objet"
}

/** Emplacement déduit du nom de l'objet (délègue à ArmorRules, qui lit la base SRD). */
fun equipmentSlotFor(itemName: String): EquipmentSlot? = ArmorRules.slotForItem(itemName)

/** Poids affiché pour un objet, ou null si le nom ne correspond à rien de connu. */
fun equipmentWeightLabel(itemName: String): String? {
    val lbs = ArmorRules.weightInPounds(itemName) ?: return null
    val kg = lbs / 2.20462
    return if (kg == kg.toLong().toDouble()) "${kg.toLong()} kg" else "%.1f kg".format(kg)
}

/** Nombre de mains requises si l'objet est une arme (slot MAIN_HAND), sinon null. */
fun equipmentHandsRequired(itemName: String): Int? {
    val slot = ArmorRules.slotForItem(itemName) ?: return null
    if (slot != EquipmentSlot.MAIN_HAND) return null
    return if (ArmorRules.twoHanded(itemName)) 2 else 1
}

/** Rangée de N icônes "main" représentant le nombre de mains requises par une arme. */
@Composable
fun HandsIcons(count: Int, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(count) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = "Main requise",
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}