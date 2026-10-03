package com.jc2.jdrcompagnon.feature_environnement.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Terrain
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EnvironmentDependencies
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.presentation.EnvironmentViewModel
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.launch

/**
 * Liste des environnements du monde courant : même logique que GroupsScreen — carte cliquable
 * pour ouvrir le détail, menu "..." pour modifier/supprimer, FAB pour créer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentListScreen(
    viewModel: EnvironmentViewModel,
    worldId: String,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val environnements by viewModel.observerEnvironnements(worldId).collectAsState(initial = emptyList())

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(worldId) {
        EnvironmentDependencies.seedExamplesIfNeeded(context.applicationContext, worldId)
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var menuForId by remember { mutableStateOf<String?>(null) }
    var itemToDelete by remember { mutableStateOf<Environnement?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Environnements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                Icon(Icons.Default.Add, contentDescription = "Nouvel environnement")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (environnements.isEmpty()) {
                item { EmptyEnvironmentsState() }
            } else {
                items(environnements, key = { it.id }) { environnement ->
                    EnvironmentListItem(
                        environnement = environnement,
                        onClick = { onOpenDetail(environnement.id) },
                        onMenuClick = { menuForId = environnement.id },
                        menuExpanded = menuForId == environnement.id,
                        onDismissMenu = { menuForId = null },
                        onEdit = { onOpenDetail(environnement.id) },
                        onDelete = { itemToDelete = environnement }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nouvel environnement") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom (ex: Forêt profonde)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.creer(newName, worldId)
                            showCreateDialog = false
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Veuillez saisir un nom d'environnement")
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

    itemToDelete?.let { environnement ->
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer l'environnement « ${environnement.nom} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.supprimer(environnement.id)
                        forgetEnvironmentSections(context, environnement.id)
                        itemToDelete = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Environnement supprimé") }
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
private fun EnvironmentListItem(
    environnement: Environnement,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terrain,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = environnement.nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    val resume = listOfNotNull(
                        environnement.evenementIds.size.takeIf { it > 0 }?.let { "$it événement(s)" },
                        environnement.tablesAleatoiresIds.size.takeIf { it > 0 }?.let { "$it table(s) aléatoire(s)" },
                    ).joinToString(" · ")
                    if (resume.isNotBlank()) {
                        Text(text = resume, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
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
private fun EmptyEnvironmentsState() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Terrain,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Aucun environnement pour ce monde",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Appuyez sur + pour en créer un.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
