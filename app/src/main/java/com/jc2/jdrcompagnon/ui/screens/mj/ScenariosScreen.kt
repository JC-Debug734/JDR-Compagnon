package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.WorldBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScenariosScreen(
    currentWorld: WorldState?,
    onBack: () -> Unit,
    onOpenScenarioEditor: (String?) -> Unit,
    onOpenScenarioReader: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val worldId = currentWorld?.id
    val scenarios = remember(worldId, mjScenarios) { GameState.scenariosForWorld(worldId) }

    LaunchedEffect(Unit) {
        GameState.syncScenariosFromDisk(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var scenarioMenuForId by remember { mutableStateOf<String?>(null) }
    var scenarioToDelete by remember { mutableStateOf<GameState.MjScenario?>(null) }

    val filtered = remember(searchQuery, scenarios) {
        if (searchQuery.isBlank()) scenarios else scenarios.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.markdownContent.contains(searchQuery, ignoreCase = true)
        }
    }

    val worldName = currentWorld?.name ?: "ce monde"

    WorldBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher un scénario...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(onClick = { onOpenScenarioEditor(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Nouveau scénario")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                if (scenarios.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Aucun scénario pour $worldName.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onOpenScenarioEditor(null) }) {
                                Text("Créer un scénario")
                            }
                        }
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Aucun scénario trouvé pour « $searchQuery ».",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { onOpenScenarioEditor(null) }) {
                                Text("Créer un scénario")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { scenario ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                GameState.setLastScenarioId(scenario.id)
                                                onBack()
                                            },
                                            onLongClick = { scenarioMenuForId = scenario.id }
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = scenario.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = scenarioMenuForId == scenario.id,
                                    onDismissRequest = { scenarioMenuForId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Lire") },
                                        onClick = {
                                            scenarioMenuForId = null
                                            GameState.setLastScenarioId(scenario.id)
                                            onOpenScenarioReader(scenario.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Modifier") },
                                        onClick = {
                                            scenarioMenuForId = null
                                            onOpenScenarioEditor(scenario.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Supprimer") },
                                        onClick = {
                                            scenarioMenuForId = null
                                            scenarioToDelete = scenario
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    scenarioToDelete?.let { scenario ->
        AlertDialog(
            onDismissRequest = { scenarioToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer le scénario « ${scenario.title} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GameState.removeMjScenario(scenario.id)
                        scenarioToDelete = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Scénario supprimé") }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scenarioToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}