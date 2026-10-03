package com.jc2.jdrcompagnon.feature_table_aleatoire.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.TableAleatoireDependencies
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_table_aleatoire.presentation.TableAleatoireListViewModel
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.launch

/**
 * Liste des tables aléatoires (événements ou loot) du monde courant : même logique
 * qu'EnvironmentListScreen — carte cliquable pour ouvrir le détail, menu "..." pour
 * modifier/supprimer, FAB pour créer. Cartes compactes (titre seul) en grille. Une table dont
 * l'intervalle est écoulé en temps de lecture de scénario (voir LectureScenarioState) est
 * entourée d'or.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableAleatoireListScreen(
    viewModel: TableAleatoireListViewModel,
    worldId: String,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val tables by remember(worldId) { viewModel.observerTables(worldId) }.collectAsState(initial = emptyList())
    val minutesLecture by viewModel.minutesLecture.collectAsState()

    LaunchedEffect(worldId) {
        TableAleatoireDependencies.seedExamplesIfNeeded(context, worldId)
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var menuForId by remember { mutableStateOf<String?>(null) }
    var itemToDelete by remember { mutableStateOf<TableAleatoire?>(null) }

    // Filtres (bouton en haut à droite) : type de table, tables prêtes, tables actives.
    var showFilterMenu by remember { mutableStateOf(false) }
    var filtreType by rememberSaveable { mutableStateOf<TypeTable?>(null) }
    var filtrePretes by rememberSaveable { mutableStateOf(false) }
    var filtreActives by rememberSaveable { mutableStateOf(false) }
    val filtreActif = filtreType != null || filtrePretes || filtreActives
    val tablesFiltrees = tables.filter { table ->
        (filtreType == null || table.type == filtreType) &&
            (!filtrePretes || viewModel.estDue(table, minutesLecture)) &&
            (!filtreActives || table.active)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Table aléatoire", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtrer",
                                tint = if (filtreActif) ForcedDarkPalette.AccentGold else Color.White
                            )
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            Text(
                                "Type",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            (listOf<TypeTable?>(null) + TypeTable.entries).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type?.label ?: "Toutes") },
                                    leadingIcon = {
                                        RadioButton(selected = filtreType == type, onClick = null)
                                    },
                                    onClick = { filtreType = type }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Prêtes à se déclencher") },
                                leadingIcon = { Checkbox(checked = filtrePretes, onCheckedChange = null) },
                                onClick = { filtrePretes = !filtrePretes }
                            )
                            DropdownMenuItem(
                                text = { Text("Actives uniquement") },
                                leadingIcon = { Checkbox(checked = filtreActives, onCheckedChange = null) },
                                onClick = { filtreActives = !filtreActives }
                            )
                            if (filtreActif) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Réinitialiser les filtres") },
                                    onClick = {
                                        filtreType = null
                                        filtrePretes = false
                                        filtreActives = false
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ForcedDarkPalette.AccentGold,
                contentColor = ForcedDarkPalette.Background
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle table")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        // Grille compacte (titre seul) pour afficher un maximum de tables à l'écran.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tables.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyTablesState() }
            } else if (tablesFiltrees.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Aucune table ne correspond aux filtres.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            } else {
                items(tablesFiltrees, key = { it.id }) { table ->
                    TableAleatoireListItem(
                        table = table,
                        due = viewModel.estDue(table, minutesLecture),
                        onClick = { onOpenDetail(table.id) },
                        onMenuClick = { menuForId = table.id },
                        menuExpanded = menuForId == table.id,
                        onDismissMenu = { menuForId = null },
                        onEdit = { onOpenDetail(table.id) },
                        onDelete = { itemToDelete = table }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        var newType by remember { mutableStateOf(TypeTable.EVENEMENTS) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nouvelle table aléatoire") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nom (ex: Route de la forêt)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeTable.entries.forEach { type ->
                            val selected = newType == type
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { newType = type },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) ForcedDarkPalette.AccentGold else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = type.label,
                                    modifier = Modifier.padding(12.dp),
                                    color = if (selected) ForcedDarkPalette.Background else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.creer(newName, worldId, newType)
                            showCreateDialog = false
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Veuillez saisir un nom de table")
                            }
                        }
                    }
                ) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Annuler") }
            }
        )
    }

    itemToDelete?.let { table ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer la table « ${table.nom} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.supprimer(table.id)
                        itemToDelete = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Table supprimée") }
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableAleatoireListItem(
    table: TableAleatoire,
    due: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Titre seul : le type se lit à l'icône, une table prête est entourée d'or, une table
    // désactivée est grisée. Le détail (intervalle, entrées) reste dans l'écran de la table.
    val icon = if (table.type == TypeTable.LOOT) Icons.Default.Backpack else Icons.Default.Casino
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        border = if (due) BorderStroke(2.dp, ForcedDarkPalette.AccentGold) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = table.type.label,
                    tint = if (due) ForcedDarkPalette.AccentGold else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = table.nom,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (table.active) Color.White else Color.White.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text("Modifier") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { onDismissMenu(); onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Supprimer") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { onDismissMenu(); onDelete() }
                )
            }
        }
    }
}

@Composable
private fun EmptyTablesState() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Aucune table aléatoire pour ce monde",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Appuyez sur + pour en créer une.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
