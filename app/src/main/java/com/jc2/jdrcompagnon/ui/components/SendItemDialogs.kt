package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Dialogues d'envoi d'un objet (nommé librement, ex. "3x Potion de soins") à un personnage ou à
 * un groupe du monde donné — extraits de ScenarioReaderScreen (liens #equipment:) pour être
 * réutilisés partout où un objet peut être remis à la table (tirage de table de loot, notamment),
 * sans dupliquer la logique de sélection ni le protocole réseau (NetworkSessionManager).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendItemToCharacterDialog(
    itemName: String,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    val characters by GameState.characters.collectAsState()
    val claimedCharacters by NetworkSessionManager.claimedCharacters.collectAsState()
    val worldCharacters = remember(characters, worldId) {
        characters.filter { it.worldId == worldId && (it.type == "PJ" || it.type == "PNJ") }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Envoyer « $itemName » à…", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (worldCharacters.isEmpty()) {
                    Text("Aucun personnage dans ce monde.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                worldCharacters.forEach { character ->
                    val clientId = claimedCharacters[character.id]
                    ListItem(
                        headlineContent = { Text(character.name) },
                        supportingContent = if (clientId != null) {
                            @Composable { Text("Connecté", color = MaterialTheme.colorScheme.primary) }
                        } else null,
                        modifier = Modifier.clickable {
                            if (clientId != null) {
                                NetworkSessionManager.sendItemToClient(clientId, itemName)
                            } else {
                                GameState.addItemToBackpack(character.id, itemName)
                            }
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendItemToGroupDialog(
    itemName: String,
    worldId: String?,
    onDismiss: () -> Unit,
) = SendItemsToGroupDialog(listOf(itemName), worldId, onDismiss)

/**
 * Propose un ou plusieurs objets à un groupe : une offre "je le veux" par objet, que chaque
 * joueur reçoit à la suite (file d'attente côté joueur).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendItemsToGroupDialog(
    itemNames: List<String>,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    val mjGroups by GameState.mjGroups.collectAsState()
    val worldGroups = remember(mjGroups, worldId) {
        mjGroups.filter { it.worldId == worldId }
    }
    val titre = itemNames.singleOrNull()?.let { "Proposer « $it » à…" } ?: "Proposer ${itemNames.size} objets à…"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (worldGroups.isEmpty()) {
                    Text("Aucun groupe dans ce monde.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                worldGroups.forEach { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        modifier = Modifier.clickable {
                            itemNames.forEach { NetworkSessionManager.sendLootOfferToGroup(group.id, it) }
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
