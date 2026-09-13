package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.GroupCard
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    currentWorld: WorldState?,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val characters by GameState.characters.collectAsState()
    val availableCharacters = characters.filter { it.type == "PJ" || it.type == "PNJ" }
    val mjGroups by GameState.mjGroups.collectAsState()
    val worldId = currentWorld?.id
    val groups = remember(worldId, mjGroups) { GameState.groupsForWorld(worldId) }
    val worldName = currentWorld?.name ?: "ce monde"

    var newGroupName by remember { mutableStateOf("") }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var groupMenuForId by remember { mutableStateOf<String?>(null) }
    var groupToRename by remember { mutableStateOf<GameState.MjGroup?>(null) }
    var itemToDelete by remember { mutableStateOf<GameState.MjGroup?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Groupes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Créer un groupe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nom du groupe") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newGroupName.isNotBlank()) {
                                val group = GameState.MjGroup(name = newGroupName, worldId = worldId ?: "")
                                GameState.addMjGroup(group)
                                expandedGroupId = group.id
                                newGroupName = ""
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Veuillez saisir un nom de groupe")
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Créer")
                    }
                }
            }

            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Aucun groupe créé pour $worldName.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                groups.forEach { group ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        GroupCard(
                            group = group,
                            availableCharacters = availableCharacters,
                            expanded = expandedGroupId == group.id,
                            onToggleExpand = { expandedGroupId = if (expandedGroupId == group.id) null else group.id },
                            onMemberToggle = { characterId ->
                                val updated = if (group.memberIds.contains(characterId)) {
                                    group.copy(memberIds = group.memberIds - characterId)
                                } else {
                                    group.copy(memberIds = group.memberIds + characterId)
                                }
                                GameState.updateMjGroup(updated)
                            },
                            onLongClick = { groupMenuForId = group.id }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = groupMenuForId == group.id,
                            onDismissRequest = { groupMenuForId = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Renommer") },
                                onClick = {
                                    groupMenuForId = null
                                    groupToRename = group
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer") },
                                onClick = {
                                    groupMenuForId = null
                                    itemToDelete = group
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { group: GameState.MjGroup ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer le groupe « ${group.name} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GameState.removeMjGroup(group.id)
                        if (expandedGroupId == group.id) expandedGroupId = null
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

    groupToRename?.let { group: GameState.MjGroup ->
        var newName by remember(group.id) { mutableStateOf(group.name) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { groupToRename = null },
            title = { Text("Renommer le groupe") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom du groupe") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            GameState.updateMjGroup(group.copy(name = newName))
                            groupToRename = null
                        } else {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Veuillez saisir un nom") }
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToRename = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}