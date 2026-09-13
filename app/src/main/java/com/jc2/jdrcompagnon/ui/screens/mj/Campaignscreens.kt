package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.WorldBackground

// ─────────────────────────────────────────────────────────────
// Gestion des campagnes : liste (CampaignListScreen) + édition
// (CampaignEditorScreen), fusionnées dans un seul fichier.
// Câblage inchangé côté NavGraph.kt : les deux fonctions gardent
// leur nom, Route.Campaigns -> CampaignListScreen,
// Route.CampaignEditor -> CampaignEditorScreen.
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignListScreen(
    currentWorld: WorldState?,
    onBack: () -> Unit,
    onOpenCampaignEditor: (String?) -> Unit
) {
    val campaigns by GameState.mjCampaigns.collectAsState()
    val worldCampaigns = remember(campaigns, currentWorld) {
        campaigns.filter { it.worldId == currentWorld?.id }
    }
    var campaignMenuId by remember { mutableStateOf<String?>(null) }
    var campaignToDelete by remember { mutableStateOf<GameState.MjCampaign?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    WorldBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Campagnes", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { onOpenCampaignEditor(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle campagne")
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
                if (worldCampaigns.isEmpty()) {
                    item {
                        EmptyCampaignState()
                    }
                } else {
                    items(worldCampaigns, key = { it.id }) { campaign ->
                        CampaignListItem(
                            campaign = campaign,
                            onClick = { onOpenCampaignEditor(campaign.id) },
                            onMenuClick = { campaignMenuId = campaign.id },
                            menuExpanded = campaignMenuId == campaign.id,
                            onDismissMenu = { campaignMenuId = null },
                            onDelete = { campaignToDelete = campaign }
                        )
                    }
                }
            }
        }
    }

    campaignToDelete?.let { campaign ->
        AlertDialog(
            onDismissRequest = { campaignToDelete = null },
            title = { Text("Supprimer la campagne") },
            text = { Text("Supprimer « ${campaign.title} » ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GameState.removeMjCampaign(campaign.id)
                        campaignToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { campaignToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampaignListItem(
    campaign: GameState.MjCampaign,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onDelete: () -> Unit
) {
    val checkedCount = campaign.checklistItems.count { it.checked }
    val totalCount = campaign.checklistItems.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            totalCount == 0 -> "Aucun objectif"
                            checkedCount == totalCount -> "$checkedCount/$totalCount objectifs réalisés"
                            else -> "$checkedCount/$totalCount objectifs"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text("Supprimer") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        onDismissMenu()
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyCampaignState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Checklist,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucune campagne",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Appuyez sur + pour créer une campagne et suivre ses objectifs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignEditorScreen(
    campaignId: String?,
    onBack: () -> Unit
) {
    val mjCampaigns by GameState.mjCampaigns.collectAsState()
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val existing = campaignId?.let { id -> mjCampaigns.firstOrNull { it.id == id } }
    val currentWorldId = GameState.currentWorldId()
    val worldScenarios = remember(mjScenarios, currentWorldId) {
        mjScenarios.filter { it.worldId == currentWorldId }
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var attachedIds by remember(existing) { mutableStateOf(existing?.scenarioIds ?: emptyList()) }
    var checklist by remember(existing) { mutableStateOf(existing?.checklistItems ?: emptyList()) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var hasAttemptedSave by remember { mutableStateOf(false) }
    val isTitleError = hasAttemptedSave && title.isBlank()
    val snackbarHostState = remember { SnackbarHostState() }

    WorldBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (campaignId == null) "Nouvelle campagne" else "Éditer la campagne", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                hasAttemptedSave = true
                                if (title.isBlank()) return@TextButton
                                val updated = existing?.copy(
                                    title = title,
                                    scenarioIds = attachedIds,
                                    checklistItems = checklist
                                ) ?: GameState.CampaignData(
                                    title = title,
                                    worldId = currentWorldId ?: "",
                                    scenarioIds = attachedIds,
                                    checklistItems = checklist
                                ).toMjCampaign()
                                if (existing != null) GameState.updateMjCampaign(updated)
                                else GameState.addMjCampaign(updated)
                                onBack()
                            }
                        ) {
                            Text("Enregistrer")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; hasAttemptedSave = false },
                    label = { Text("Titre de la campagne *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isTitleError,
                    supportingText = {
                        if (isTitleError) {
                            Text("Le titre est obligatoire", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Scénarios attachés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (attachedIds.isEmpty()) {
                            Text(
                                "Aucun scénario attaché.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            attachedIds.forEach { id ->
                                val scenario = worldScenarios.find { it.id == id }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        scenario?.title ?: "Scénario inconnu",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    IconButton(onClick = { attachedIds = attachedIds - id }) {
                                        Icon(Icons.Default.Close, contentDescription = "Détacher")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAttachDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Attacher un scénario")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Fiche de suivi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (checklist.isEmpty()) {
                            Text(
                                "Aucun objectif. Ajoutez des éléments à cocher pour suivre la campagne.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            checklist.forEachIndexed { index, item ->
                                ChecklistItemRow(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        checklist = checklist.mapIndexed { i, it ->
                                            if (i == index) it.copy(checked = checked) else it
                                        }
                                    },
                                    onLabelChange = { value ->
                                        checklist = checklist.mapIndexed { i, it ->
                                            if (i == index) it.copy(label = value) else it
                                        }
                                    },
                                    onDelete = {
                                        checklist = checklist.filterIndexed { i, _ -> i != index }
                                    }
                                )
                                if (index < checklist.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                checklist = checklist + GameState.CampaignChecklistItem(label = "")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ajouter un suivi")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAttachDialog) {
        val available = worldScenarios.filter { it.id !in attachedIds }
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = { Text("Attacher un scénario") },
            text = {
                if (available.isEmpty()) {
                    Text("Aucun scénario disponible dans ce monde.")
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(available) { scenario ->
                            TextButton(
                                onClick = {
                                    attachedIds = attachedIds + scenario.id
                                    showAttachDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(scenario.title)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachDialog = false }) { Text("Fermer") }
            }
        )
    }
}

@Composable
private fun ChecklistItemRow(
    item: GameState.CampaignChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onLabelChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = onCheckedChange
        )
        OutlinedTextField(
            value = item.label,
            onValueChange = onLabelChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Objectif / événement") }
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
        }
    }
}