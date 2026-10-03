package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    // Les deux mains partagent la couleur des armes : c'est là qu'elles s'équipent
    // (armes à une main, à deux mains, ou bouclier en main secondaire) — les 3 emplacements
    // d'armes de dos partagent la même couleur, ce sont aussi des armes.
    EquipmentSlot.MAIN_HAND,
    EquipmentSlot.OFF_HAND,
    EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3 -> Color(0xFFC0524D)
    EquipmentSlot.TORSO -> Color(0xFF4A7FB5)       // Armure
    EquipmentSlot.HEAD -> Color(0xFF4A7FB5)        // Casque (même famille que l'armure)
    EquipmentSlot.CLOTHING -> Color(0xFF4A7FB5)    // Vêtements (même famille que l'armure/casque)
    EquipmentSlot.BACK -> Color(0xFFB08A4E)        // Cape / carquois
    EquipmentSlot.BACKPACK -> Color(0xFFB08A4E)    // Sac à dos (même famille que le dos)
    EquipmentSlot.ACCESSORY -> Color(0xFF9B6BC7)   // Anneau, amulette...
    EquipmentSlot.BELT_POUCH_1, EquipmentSlot.BELT_POUCH_2,
    EquipmentSlot.BACK_ACCESSORY_1, EquipmentSlot.BACK_ACCESSORY_2,
    EquipmentSlot.BACK_ACCESSORY_3, EquipmentSlot.BACK_ACCESSORY_4 -> Color(0xFF5FA37A) // Bourses / potions
    null -> Color(0xFF8A93A6)                       // Objet divers
}

/** Libellé français affiché à côté de la pastille de couleur. */
fun EquipmentSlot?.slotLabel(): String = when (this) {
    EquipmentSlot.MAIN_HAND -> "Arme"
    EquipmentSlot.OFF_HAND -> "Bouclier"
    EquipmentSlot.TORSO -> "Armure"
    EquipmentSlot.HEAD -> "Casque"
    EquipmentSlot.CLOTHING -> "Vêtements"
    EquipmentSlot.BACK -> "Sac / Dos"
    EquipmentSlot.BACKPACK -> "Sac à dos"
    EquipmentSlot.ACCESSORY -> "Accessoire"
    EquipmentSlot.BELT_POUCH_1, EquipmentSlot.BELT_POUCH_2,
    EquipmentSlot.BACK_ACCESSORY_1, EquipmentSlot.BACK_ACCESSORY_2,
    EquipmentSlot.BACK_ACCESSORY_3, EquipmentSlot.BACK_ACCESSORY_4 -> "Utilitaire"
    EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3 -> "Arme (dos)"
    null -> "Objet"
}

/** Icône associée à chaque emplacement — une seule source pour toute la silhouette (cohérence). */
fun EquipmentSlot.slotIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    EquipmentSlot.HEAD -> Icons.Default.Face
    EquipmentSlot.TORSO -> Icons.Default.Security
    EquipmentSlot.MAIN_HAND, EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3 -> Icons.Default.Build
    EquipmentSlot.OFF_HAND -> Icons.Default.Shield
    EquipmentSlot.CLOTHING -> Icons.Default.Checkroom
    EquipmentSlot.BACK -> Icons.Default.DryCleaning
    EquipmentSlot.BACKPACK -> Icons.Default.Backpack
    EquipmentSlot.ACCESSORY -> Icons.Default.Star
    // Emplacements utilitaires : tous la même icône (ils sont interchangeables).
    EquipmentSlot.BELT_POUCH_1, EquipmentSlot.BELT_POUCH_2,
    EquipmentSlot.BACK_ACCESSORY_1, EquipmentSlot.BACK_ACCESSORY_2,
    EquipmentSlot.BACK_ACCESSORY_3, EquipmentSlot.BACK_ACCESSORY_4 -> Icons.Default.Inventory2
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
fun HandsIcons(count: Int, tint: Color, iconSize: Dp = 14.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(count) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = "Main requise",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Pastille des mains requises, à superposer en bas à droite de l'icône d'une arme
 * (`Modifier.align(Alignment.BottomEnd)` dans un Box) : fond sombre pour rester lisible
 * quelle que soit la couleur de l'icône en dessous.
 */
@Composable
fun HandsBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 4.dp, y = 4.dp)
            .background(Color(0xE6202020), RoundedCornerShape(6.dp))
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        HandsIcons(count, Color.White, iconSize = 9.dp)
    }
}