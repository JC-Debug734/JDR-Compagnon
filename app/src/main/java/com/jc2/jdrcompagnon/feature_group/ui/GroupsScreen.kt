package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.SelectableListCard
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.launch

/**
 * Liste des groupes du monde courant (SelectableListCard, comme Scénarios et Campagnes) : rond à
 * gauche pour sélectionner le groupe actif, clic sur la carte pour le modifier (GroupDetailScreen),
 * corbeille à droite pour le supprimer. Bouton "+" flottant pour créer un groupe
 * (nom demandé dans une petite boîte de dialogue).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentWorld: WorldState?,
    onOpenGroupDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val mjGroups by GameState.mjGroups.collectAsState()
    val worldId = currentWorld?.id
    val groups = remember(worldId, mjGroups) { GameState.groupsForWorld(worldId) }
    val worldName = currentWorld?.name ?: "ce monde"

    var showCreateDialog by remember { mutableStateOf(false) }
    val selectedGroupId by GameState.currentGroupId.collectAsState()
    var itemToDelete by remember { mutableStateOf<GameState.MjGroup?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Groupes", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nouveau groupe", tint = ForcedDarkPalette.AccentGold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (groups.isEmpty()) {
                item { EmptyGroupsState(worldName) }
            } else {
                items(groups, key = { it.id }) { group ->
                    SelectableListCard(
                        title = group.name,
                        subtitle = "${group.memberCount} membre(s)" +
                                (if (group.mounts.isNotEmpty() || group.transports.isNotEmpty())
                                    " · ${group.mounts.size} monture(s)/animal(aux) · ${group.transports.size} véhicule(s)"
                                else "") +
                                (group.location.takeIf { it.isNotBlank() }?.let { " · 📍 $it" } ?: ""),
                        selected = selectedGroupId == group.id,
                        onToggleSelect = {
                            GameState.setCurrentGroupId(if (selectedGroupId == group.id) null else group.id)
                        },
                        onEdit = { onOpenGroupDetail(group.id) },
                        onDelete = { itemToDelete = group }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var newGroupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nouveau groupe") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Nom du groupe") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            val group = GameState.MjGroup(name = newGroupName, worldId = worldId ?: "")
                            GameState.addMjGroup(group)
                            showCreateDialog = false
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Veuillez saisir un nom de groupe")
                            }
                        }
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    itemToDelete?.let { group: GameState.MjGroup ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer le groupe « ${group.name} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GameState.removeMjGroup(group.id)
                        itemToDelete = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Groupe supprimé") }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun EmptyGroupsState(worldName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun groupe pour $worldName",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Appuyez sur + en haut à droite pour créer un groupe.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
