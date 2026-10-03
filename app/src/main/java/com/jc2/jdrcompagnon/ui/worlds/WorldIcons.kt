package com.jc2.jdrcompagnon.ui.worlds

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icônes Material sélectionnables pour un univers importé via son `reference.md` (champ
 * `Icone:`), reconnues par leur nom (insensible à la casse/accents) — voir
 * [CustomWorldsRepository]/[ReferenceMdParser]. Une icône inconnue ou absente retombe sur
 * [Icons.Filled.Public].
 */
object WorldIcons {
    private val byName: Map<String, ImageVector> = mapOf(
        "shield" to Icons.Filled.Shield,
        "landscape" to Icons.Filled.Landscape,
        "public" to Icons.Filled.Public,
        "darkmode" to Icons.Filled.DarkMode,
        "autoawesome" to Icons.Filled.AutoAwesome,
        "star" to Icons.Filled.Star,
        "forest" to Icons.Filled.Forest,
        "castle" to Icons.Filled.Castle,
        "localfiredepartment" to Icons.Filled.LocalFireDepartment,
        "feu" to Icons.Filled.LocalFireDepartment,
        "bolt" to Icons.Filled.Bolt,
        "nightlight" to Icons.Filled.Nightlight,
        "pets" to Icons.Filled.Pets,
        "menubook" to Icons.AutoMirrored.Filled.MenuBook,
        "wbsunny" to Icons.Filled.WbSunny,
        "soleil" to Icons.Filled.WbSunny,
        "acunit" to Icons.Filled.AcUnit,
        "glace" to Icons.Filled.AcUnit,
        "diamond" to Icons.Filled.Diamond,
        "sailing" to Icons.Filled.Sailing,
        "waterdrop" to Icons.Filled.WaterDrop,
        "eau" to Icons.Filled.WaterDrop,
    )

    fun forName(name: String?): ImageVector {
        if (name.isNullOrBlank()) return Icons.Filled.Public
        val key = name.trim().lowercase()
            .replace(" ", "")
            .replace("è", "e").replace("é", "e").replace("ê", "e")
        return byName[key] ?: Icons.Filled.Public
    }
}
