package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onOpenAccueil: () -> Unit,
    onChooseCharacter: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onClose: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = ForcedDarkPalette.Surface,
        drawerContentColor = ForcedDarkPalette.Content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Header with role icon (clickable to change role) and settings ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Role icon (clickable to return to role selection)
                IconButton(
                    onClick = {
                        onClose()
                        GameState.requestRoleChange()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = "Joueur",
                        tint = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "MENU",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForcedDarkPalette.AccentGold,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        onClose()
                        onOpenSettings()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Réglages",
                        tint = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = ForcedDarkPalette.Indicator)

            // TODO: Display current group when available
            Text(
                text = "Groupe : Non sélectionné",
                style = MaterialTheme.typography.labelSmall,
                color = ForcedDarkPalette.Content,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Choose character
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

            HorizontalDivider(color = ForcedDarkPalette.Indicator)

            // Change role
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.SwapHoriz, null, tint = ForcedDarkPalette.Content) },
                label = { Text("Changer de rôle", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = { GameState.requestRoleChange() },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content,
                ),
            )
        }
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