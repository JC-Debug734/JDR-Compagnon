package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.theme.MysticAlert
import com.jc2.jdrcompagnon.ui.theme.MysticSuccess

/**
 * Palette "fiche sombre" partagée entre la fiche de personnage
 * (CharacterSheetScreen) et les écrans d'équipement (CharacterEquipment,
 * EquipmentSlotsLayout), pour garder un style cohérent partout.
 *
 * Ces valeurs sont désormais pilotées par [JdrCompagnonTheme] via
 * [updateSheetTheme] (appelée à chaque changement de monde ou de mode
 * clair/sombre), plutôt que figées en dur : la fiche suit ainsi le thème
 * actif. Les noms et types restent inchangés (toujours de simples `Color`)
 * pour ne rien casser dans les écrans qui les consomment.
 */
var SheetSurface: Color by mutableStateOf(Color(0xFF232C40))
    private set
var SheetSurfaceLight: Color by mutableStateOf(Color(0xFF2B3550))
    private set
var SheetBorder: Color by mutableStateOf(Color(0xFF3A4560))
    private set
var SheetTextPrimary: Color by mutableStateOf(Color(0xFFF5F6FA))
    private set
var SheetTextSecondary: Color by mutableStateOf(Color(0xFFA6B0C3))
    private set

/**
 * Met à jour la palette de la fiche pour qu'elle suive le [MaterialTheme]
 * actif. Appelée depuis [JdrCompagnonTheme] (SideEffect) à chaque
 * recomposition du thème racine, donc à chaque changement de monde ou de
 * mode clair/sombre.
 */
fun updateSheetTheme(
    surface: Color,
    surfaceLight: Color,
    border: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    SheetSurface = surface
    SheetSurfaceLight = surfaceLight
    SheetBorder = border
    SheetTextPrimary = textPrimary
    SheetTextSecondary = textSecondary
}

/** Vert/rouge cohérents avec la palette par défaut de l'appli (barre de dé, connexion). */
val SheetAccentGood = MysticSuccess
val SheetAccentBad = MysticAlert

/**
 * Conteneur de carte réutilisé sur toute la fiche et les écrans
 * d'équipement pour garder un style uniforme (cadre sombre, coins arrondis).
 */
@Composable
fun SheetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SheetSurface,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * Couleurs de champ de texte adaptées à la palette sombre de la fiche.
 */
@Composable
fun sheetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SheetTextPrimary,
    unfocusedTextColor = SheetTextPrimary,
    focusedBorderColor = SheetTextSecondary,
    unfocusedBorderColor = SheetBorder,
    focusedLabelColor = SheetTextSecondary,
    unfocusedLabelColor = SheetTextSecondary,
    cursorColor = SheetTextPrimary
)