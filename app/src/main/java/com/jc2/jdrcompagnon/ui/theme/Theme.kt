package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.updateSheetTheme
import com.jc2.jdrcompagnon.ui.theme.Type.JdrTypography

@Composable
fun JdrCompagnonTheme(
    // L'app a une identité visuelle volontairement toujours sombre (voir
    // ForcedDarkPalette) : on ignore le mode clair/sombre du système pour
    // ne plus jamais retomber sur les variantes *LightColors, quel que
    // soit le réglage du téléphone. Le paramètre est conservé pour ne pas
    // casser d'éventuels appels existants, mais n'est plus utilisé.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val currentWorld by GameState.currentWorld.collectAsState()

    val colors = when (currentWorld?.id) {
        "donjon_et_dragon" -> DndDarkColors
        "naheulbeuk" -> NaheulDarkColors
        else -> DefaultDarkColors
    }

    // Fait suivre la palette des fiches de personnage (SheetTheme.kt) au
    // monde sélectionné, à chaque changement de `colors` ci-dessus.
    SideEffect {
        updateSheetTheme(
            surface = colors.surface,
            surfaceLight = colors.surfaceVariant,
            border = colors.outline,
            textPrimary = colors.onSurface,
            textSecondary = colors.onSurfaceVariant
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = JdrTypography,
        shapes = JdrShapes,
        content = content
    )
}

/**
 * Custom shape system for JDR Compagnon
 */
val JdrShapes = androidx.compose.material3.Shapes(
    extraSmall = Shapes.ExtraSmall,
    small = Shapes.Small,
    medium = Shapes.Medium,
    large = Shapes.Large,
    extraLarge = Shapes.ExtraLarge
)

/**
 * Palette bleu-nuit forcée, indépendante du thème Material sélectionné
 * (clair/sombre, ni du monde D&D/Naheulbeuk). Source unique de vérité pour
 * les zones qui ne doivent JAMAIS suivre `JdrCompagnonTheme` : fond racine
 * de l'app (MainActivity) et barre de navigation du bas (AppBottomBar).
 *
 * Couleurs alignées sur celles déjà utilisées pour les fiches de
 * personnage, afin de garder une cohérence visuelle sur toute l'app.
 */
object ForcedDarkPalette {
    /** Fond d'écran principal. */
    val Background = Color(0xFF232C40)
    /** Surface légèrement plus claire que le fond (cartes, barre du bas). */
    val Surface = Color(0xFF2B3550)
    /** Texte/icônes par défaut sur fond sombre. */
    val Content = Color(0xFFAAB4CC)
    /** Accent doré (cadre de portrait, éléments sélectionnés/mis en avant). */
    val AccentGold = Color(0xFFD4AF37)
    /** Pastille/indicateur de sélection, un ton au-dessus de Surface. */
    val Indicator = Color(0xFF3A4568)
}