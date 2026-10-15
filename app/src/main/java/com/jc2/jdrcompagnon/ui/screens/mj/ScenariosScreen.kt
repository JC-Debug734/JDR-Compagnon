package com.jc2.jdrcompagnon.ui.screens.mj

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.SelectableListCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImport

/**
 * Liste des scénarios du monde courant (SelectableListCard, comme Groupes et Campagnes) : rond à
 * gauche pour sélectionner le scénario, clic sur la carte pour le modifier, "Lire" puis
 * corbeille à droite. Jamais d'image en fond de carte ni d'appui long, qui posaient un bug
 * d'affichage (voile/fond d'image corrompu par endroits).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenariosScreen(
    currentWorld: WorldState?,
    onBack: () -> Unit,
    onOpenScenarioEditor: (String?) -> Unit,
    onOpenScenarioReader: (String) -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val worldId = currentWorld?.id
    val campagnes by GameState.mjCampaigns.collectAsState()
    val campagneCourante by GameState.currentCampaignId.collectAsState()
    // Les scénarios d'une campagne non sélectionnée sont masqués (voir PorteeCampagne).
    val scenarios = remember(worldId, mjScenarios, campagnes, campagneCourante) {
        GameState.scenariosForWorld(worldId).filter { com.jc2.jdrcompagnon.ui.PorteeCampagne.scenarioVisible(it) }
    }
    val lastScenarioId by GameState.lastScenarioId.collectAsState()

    LaunchedEffect(Unit) {
        GameState.syncScenariosFromDisk(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var scenarioToDelete by remember { mutableStateOf<GameState.MjScenario?>(null) }

    val filtered = remember(searchQuery, scenarios) {
        if (searchQuery.isBlank()) scenarios else scenarios.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.markdownContent.contains(searchQuery, ignoreCase = true)
        }
    }

    val worldName = currentWorld?.name ?: "ce monde"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scénarios", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenScenarioEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Nouveau scénario", tint = ForcedDarkPalette.AccentGold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Fond transparent : le fond d'écran global de l'app doit rester visible ; seules les
        // cartes de scénario (couleur unie) doivent être lisibles, même principe que GroupsScreen.
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher un scénario...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
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
                // Rangés par chapitre puis par numéro (ordre de jeu), avec un en-tête par chapitre
                // dès qu'un scénario en porte un (voir ChapitresScenarios).
                val groupes = remember(filtered) { ChapitresScenarios.grouper(filtered) }
                val avecEnTetes = remember(filtered) { ChapitresScenarios.aDesChapitres(filtered) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupes.forEach { (chapitre, scenariosDuChapitre) ->
                        if (avecEnTetes) {
                            item(key = "chapitre-${chapitre ?: "aucun"}") {
                                Text(
                                    text = chapitre ?: "Sans chapitre",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
                                )
                            }
                        }
                        items(scenariosDuChapitre, key = { it.id }) { scenario ->
                            SelectableListCard(
                                title = ChapitresScenarios.titreNumerote(scenario),
                                subtitle = if (scenario.scenes.size > 1) "${scenario.scenes.size} scènes" else null,
                                selected = scenario.id == lastScenarioId,
                                onToggleSelect = {
                                    GameState.setLastScenarioId(
                                        if (lastScenarioId == scenario.id) null else scenario.id
                                    )
                                },
                                onEdit = { onOpenScenarioEditor(scenario.id) },
                                onDelete = { scenarioToDelete = scenario }
                            )
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
                        GameState.removeMjScenario(scenario.id, context)
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

