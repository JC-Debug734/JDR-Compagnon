package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Écrans de combat (MJ et joueur) : tout le texte en blanc, y compris le texte secondaire (gris
 * dans le thème) et le texte sur les conteneurs dorés ou les boutons (noir dans le thème D&D).
 * Les fenêtres ouvertes depuis ces écrans en héritent. Le thème de l'appli étant toujours sombre,
 * le blanc reste lisible partout.
 */
@Composable
internal fun TexteBlancCombat(content: @Composable () -> Unit) {
    val blanc = Color.White
    val couleurs = MaterialTheme.colorScheme.copy(
        onPrimary = blanc,
        onPrimaryContainer = blanc,
        onSecondary = blanc,
        onSecondaryContainer = blanc,
        onTertiary = blanc,
        onTertiaryContainer = blanc,
        onBackground = blanc,
        onSurface = blanc,
        onSurfaceVariant = blanc,
    )
    MaterialTheme(colorScheme = couleurs, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(LocalContentColor provides blanc, content = content)
    }
}

