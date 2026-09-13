package com.jc2.jdrcompagnon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Menu latéral partagé côté Joueur, utilisé de la même façon sur les 3
 * écrans concernés (accueil joueur, sélection de personnage, fiche de
 * personnage) pour garder une navigation cohérente.
 *
 * Deux choix : "Choisir un personnage" et "Configuration". Ce dernier
 * n'ouvre pour l'instant qu'une boîte de dialogue permettant de changer
 * le pseudo du joueur (GameState.playerName) ; d'autres réglages
 * pourront y être ajoutés plus tard.
 *
 * Palette forcée sur ForcedDarkPalette (la même que la barre de
 * navigation du bas) plutôt que sur MaterialTheme brut ou SheetTheme,
 * pour que le tiroir soit visuellement identique à cette barre.
 */
@Composable
fun JoueurDrawer(
    onChooseCharacter: () -> Unit,
    onClose: () -> Unit,
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    AppDrawer {
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Group, null, tint = ForcedDarkPalette.Content) },
            label = { Text("Choisir un personnage", color = ForcedDarkPalette.Content) },
            selected = false,
            onClick = {
                onClose()
                onChooseCharacter()
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = ForcedDarkPalette.Surface,
                unselectedIconColor = ForcedDarkPalette.Content,
                unselectedTextColor = ForcedDarkPalette.Content
            )
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, null, tint = ForcedDarkPalette.Content) },
            label = { Text("Configuration", color = ForcedDarkPalette.Content) },
            selected = false,
            onClick = { showSettingsDialog = true },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = ForcedDarkPalette.Surface,
                unselectedIconColor = ForcedDarkPalette.Content,
                unselectedTextColor = ForcedDarkPalette.Content
            )
        )
    }

    if (showSettingsDialog) {
        PlayerSettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}

/**
 * Boîte de dialogue de configuration côté joueur. Pour l'instant, permet
 * uniquement de changer le pseudo (GameState.playerName) ; d'autres
 * réglages viendront s'y ajouter par la suite.
 */
@Composable
private fun PlayerSettingsDialog(onDismiss: () -> Unit) {
    val currentPlayerName by GameState.playerName.collectAsState()
    var pseudo by remember(currentPlayerName) { mutableStateOf(currentPlayerName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration") },
        text = {
            OutlinedTextField(
                value = pseudo,
                onValueChange = { pseudo = it },
                label = { Text("Pseudo") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pseudo.isNotBlank()) {
                        GameState.setPlayerName(pseudo.trim())
                    }
                    onDismiss()
                },
                enabled = pseudo.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}