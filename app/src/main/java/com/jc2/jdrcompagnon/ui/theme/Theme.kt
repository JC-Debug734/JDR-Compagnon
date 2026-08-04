package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.jc2.jdrcompagnon.ui.GameState

// Schéma par défaut (Mystic)
private val DefaultDarkColors = darkColorScheme(
    primary = MysticPurple,
    onPrimary = Color.White,
    secondary = RadiantCyan,
    background = DeepBlack,
    surface = SurfaceDark,
    onBackground = Color(0xFFE0E0E6),
    onSurface = Color.White,
)

// Schéma Donjon et Dragon
private val DndDarkColors = darkColorScheme(
    primary = DndRed,
    onPrimary = DndOnPrimary,
    secondary = DndGold,
    background = DeepBlack,
    surface = Color(0xFF1A1212), // Teinte rouge très sombre
    onBackground = Color(0xFFF5E6E6),
    onSurface = Color.White,
    primaryContainer = DndRedDark,
    onPrimaryContainer = Color.White
)

// Schéma Naheulbeuk
private val NaheulDarkColors = darkColorScheme(
    primary = NaheulGreen,
    onPrimary = NaheulOnPrimary,
    secondary = NaheulAmber,
    tertiary = NaheulBrown,
    background = DeepBlack,
    surface = Color(0xFF121A12), // Teinte verte très sombre
    onBackground = Color(0xFFE6F5E6),
    onSurface = Color.White,
    primaryContainer = NaheulGreenDark,
    onPrimaryContainer = Color.White
)

@Composable
fun JdrCompagnonTheme(
    @Suppress("unused") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val currentWorld by GameState.currentWorld.collectAsState()
    
    val colors = when (currentWorld?.id) {
        "donjon_et_dragon" -> DndDarkColors
        "naheulbeuk" -> NaheulDarkColors
        else -> DefaultDarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
