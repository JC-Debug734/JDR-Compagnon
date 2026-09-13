package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioReaderContent
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.SessionRole
import kotlinx.coroutines.launch

data class MjTool(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

typealias MjGroup = GameState.MjGroup

private sealed class DeleteTarget {
    data class Scenario(val id: String, val title: String) : DeleteTarget()
    data class Group(val id: String, val name: String) : DeleteTarget()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MjHomeScreen(
    currentWorld: WorldState?,
    onCreateCharacter: () -> Unit,
    onViewCharacters: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenScenarioEditor: (String?) -> Unit,
    onOpenCampaigns: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenLanHost: (groupId: String?, campaignTitle: String?) -> Unit,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val serverRole by NetworkSessionManager.role.collectAsState()
    val isServerRunning = serverRole == SessionRole.HOST
    val serverStatusColor = if (isServerRunning) Color(0xFF2E7D32) else Color(0xFFC62828)

    val tools = listOf(
        MjTool("characters", "FICHES", "Voir et modifier", Icons.Default.People, MaterialTheme.colorScheme.secondary),
        MjTool("bestiary", "BIBLIOTHÈQUE", "Monstres & Sorts", Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.secondary),
        MjTool("campaigns", "CAMPAGNES", "Suivi d'objectifs", Icons.Default.Checklist, MaterialTheme.colorScheme.tertiary),
        MjTool("groups", "GROUPES", "Gérer les groupes", Icons.Default.Group, MaterialTheme.colorScheme.tertiary),
        MjTool("music", "MUSIQUE", "Ambiance sonore", Icons.Default.MusicNote, MaterialTheme.colorScheme.primary),
        MjTool(
            "session",
            "PARTIE",
            if (isServerRunning) "Serveur actif" else "Lancer le serveur réseau",
            Icons.Default.Wifi,
            serverStatusColor
        ),
    )

    var newGroupName by remember { mutableStateOf("") }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var scenarioMenuForId by remember { mutableStateOf<String?>(null) }
    var showScenarioPickerMenu by remember { mutableStateOf(false) }
    var showCampaignPickerMenu by remember { mutableStateOf(false) }
    var groupMenuForId by remember { mutableStateOf<String?>(null) }
    var groupToRename by remember { mutableStateOf<GameState.MjGroup?>(null) }
    var itemToDelete by remember { mutableStateOf<DeleteTarget?>(null) }
    var showGroupsPanel by remember { mutableStateOf(false) }
    var showReader by remember { mutableStateOf(false) }
    var showScenarioFilesPanel by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            GameState.syncScenariosFromDisk(context)
        }
    }
    val characters by GameState.characters.collectAsState()
    val availableCharacters = characters.filter { it.type == "PJ" || it.type == "PNJ" }
    val mjGroups by GameState.mjGroups.collectAsState()
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val mjCampaigns by GameState.mjCampaigns.collectAsState()
    val lastScenarioId by GameState.lastScenarioId.collectAsState()

    var selectedScenarioId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCampaignId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedCampaign = selectedCampaignId?.let { id -> mjCampaigns.firstOrNull { it.id == id } }
    val visibleScenarios = if (selectedCampaign != null) {
        mjScenarios.filter { it.id in selectedCampaign.scenarioIds }
    } else {
        mjScenarios
    }

    LaunchedEffect(mjCampaigns) {
        val currentCampaign = selectedCampaignId?.let { id -> mjCampaigns.firstOrNull { it.id == id }?.id }
        selectedCampaignId = currentCampaign
    }

    LaunchedEffect(visibleScenarios, mjGroups, lastScenarioId, selectedCampaignId) {
        val currentScenario = selectedScenarioId?.let { id -> visibleScenarios.firstOrNull { it.id == id }?.id }
        selectedScenarioId = currentScenario ?: lastScenarioId?.let { id -> visibleScenarios.firstOrNull { it.id == id }?.id } ?: visibleScenarios.firstOrNull()?.id

        val currentGroup = selectedGroupId?.let { id -> mjGroups.firstOrNull { it.id == id }?.id }
        selectedGroupId = currentGroup ?: mjGroups.firstOrNull()?.id
    }

    val selectedScenario = selectedScenarioId?.let { id -> mjScenarios.firstOrNull { it.id == id } }
    val selectedGroup = selectedGroupId?.let { id -> mjGroups.firstOrNull { it.id == id } }

    LaunchedEffect(selectedScenarioId) {
        if (selectedScenarioId == null) showReader = false
    }

    // Gestion du retour arrière physique
    BackHandler {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            onBack()
        }
    }

    fun handleToolClick(toolId: String) {
        when (toolId) {
            "characters" -> onViewCharacters()
            "bestiary" -> onOpenLibrary()
            "campaigns" -> onOpenCampaigns()
            "groups" -> showGroupsPanel = true
            "music" -> onOpenMusic()
            "session" -> onOpenLanHost(selectedGroupId, selectedCampaign?.title)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "MENU MJ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    currentWorld?.name?.let { worldName ->
                        Text(
                            text = "Univers : $worldName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // --- CAMPAGNES (filtre les scénarios disponibles ci-dessous) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .clickable { showCampaignPickerMenu = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CAMPAGNE",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showCampaignPickerMenu,
                                onDismissRequest = { showCampaignPickerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Toutes (aucun filtre)") },
                                    onClick = {
                                        showCampaignPickerMenu = false
                                        selectedCampaignId = null
                                    },
                                    leadingIcon = if (selectedCampaignId == null) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                                mjCampaigns.forEach { campaign ->
                                    DropdownMenuItem(
                                        text = { Text(campaign.title) },
                                        onClick = {
                                            showCampaignPickerMenu = false
                                            selectedCampaignId = campaign.id
                                        },
                                        leadingIcon = if (selectedCampaignId == campaign.id) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onOpenCampaigns() }) {
                            Icon(Icons.Default.Add, contentDescription = "Gérer les campagnes")
                        }
                    }
                    Text(
                        text = selectedCampaign?.title ?: "Toutes les campagnes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // --- SCÉNARIOS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .clickable(enabled = visibleScenarios.isNotEmpty()) { showScenarioPickerMenu = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SCÉNARIOS",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showScenarioPickerMenu,
                                onDismissRequest = { showScenarioPickerMenu = false }
                            ) {
                                visibleScenarios.forEach { scenario ->
                                    DropdownMenuItem(
                                        text = { Text(scenario.title) },
                                        onClick = {
                                            showScenarioPickerMenu = false
                                            selectedScenarioId = scenario.id
                                            GameState.setLastScenarioId(scenario.id)
                                            coroutineScope.launch { drawerState.close() }
                                        },
                                        leadingIcon = if (selectedScenarioId == scenario.id) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showScenarioFilesPanel = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Explorer les fichiers .md sur le téléphone")
                        }
                        IconButton(onClick = { onOpenScenarioEditor(null) }) {
                            Icon(Icons.Default.Add, contentDescription = "Nouveau scénario")
                        }
                    }
                    if (visibleScenarios.isEmpty()) {
                        Text(
                            text = if (selectedCampaign != null) "Aucun scénario dans cette campagne" else "Aucun scénario",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        visibleScenarios.forEach { scenario ->
                            val selected = selectedScenarioId == scenario.id
                            Box(modifier = Modifier.fillMaxWidth()) {
                                DrawerSelectableItem(
                                    label = scenario.title,
                                    selected = selected,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        onOpenScenarioEditor(scenario.id)
                                    },
                                    onLongClick = { scenarioMenuForId = scenario.id }
                                )
                                DropdownMenu(
                                    expanded = scenarioMenuForId == scenario.id,
                                    onDismissRequest = { scenarioMenuForId = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sélectionner") },
                                        onClick = {
                                            scenarioMenuForId = null
                                            selectedScenarioId = scenario.id
                                            GameState.setLastScenarioId(scenario.id)
                                            coroutineScope.launch { drawerState.close() }
                                        },
                                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Modifier") },
                                        onClick = {
                                            scenarioMenuForId = null
                                            coroutineScope.launch { drawerState.close() }
                                            onOpenScenarioEditor(scenario.id)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Supprimer") },
                                        onClick = {
                                            scenarioMenuForId = null
                                            itemToDelete = DeleteTarget.Scenario(scenario.id, scenario.title)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // --- OUTILS ---
                    DrawerSectionTitle("OUTILS", Icons.Default.Construction)
                    tools.forEach { tool ->
                        DrawerToolItem(
                            tool = tool,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                handleToolClick(tool.id)
                            }
                        )
                    }
                }
            }
        }
    ) {
        // Dialogues
        itemToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Confirmer la suppression") },
                text = {
                    Text(
                        when (target) {
                            is DeleteTarget.Scenario -> "Supprimer le scénario « ${target.title} » ?"
                            is DeleteTarget.Group -> "Supprimer le groupe « ${target.name} » ?"
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            when (target) {
                                is DeleteTarget.Scenario -> {
                                    if (selectedScenarioId == target.id) {
                                        GameState.setLastScenarioId(null)
                                    }
                                    GameState.removeMjScenario(target.id)
                                }
                                is DeleteTarget.Group -> {
                                    GameState.removeMjGroup(target.id)
                                    if (expandedGroupId == target.id) expandedGroupId = null
                                }
                            }
                            itemToDelete = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    when (target) {
                                        is DeleteTarget.Scenario -> "Scénario supprimé"
                                        is DeleteTarget.Group -> "Groupe supprimé"
                                    }
                                )
                            }
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

        groupToRename?.let { group ->
            var newName by remember(group.id) { mutableStateOf(group.name) }
            AlertDialog(
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
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Veuillez saisir un nom")
                                }
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

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (showReader && selectedScenario != null) selectedScenario.title else "MAÎTRE DU JEU",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (showReader) {
                            IconButton(onClick = { showReader = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour au tableau de bord")
                            }
                        } else {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Ouvrir le menu")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            if (showReader && selectedScenario != null) {
                ScenarioReaderContent(
                    scenario = selectedScenario,
                    onOpenInternalLink = onOpenInternalLink,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Carte "scénario en cours" (équivalent réel de "prochaine partie") ---
                    if (selectedScenario != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SCÉNARIO EN COURS",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = selectedScenario.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentWorld?.name ?: "Monde inconnu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (selectedScenario.scenes.size > 1) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${selectedScenario.scenes.size} scènes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showReader = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Reprendre la lecture")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Aucun scénario sélectionné",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Choisissez un scénario existant ou créez-en un nouveau.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Charger un scénario")
                                    }
                                    OutlinedButton(onClick = { onOpenScenarioEditor(null) }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Nouveau")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MES OUTILS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    tools.chunked(2).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTools.forEach { tool ->
                                DashboardToolCard(
                                    tool = tool,
                                    modifier = Modifier.weight(1f),
                                    onClick = { handleToolClick(tool.id) }
                                )
                            }
                            if (rowTools.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showGroupsPanel) {
            ModalBottomSheet(onDismissRequest = { showGroupsPanel = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Groupes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
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
                                        val group = GameState.MjGroup(name = newGroupName)
                                        GameState.addMjGroup(group)
                                        selectedGroupId = group.id
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

                    if (mjGroups.isEmpty()) {
                        Text("Aucun groupe créé pour le moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        mjGroups.forEach { group ->
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
                                DropdownMenu(
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
                                            itemToDelete = DeleteTarget.Group(group.id, group.name)
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

        if (showScenarioFilesPanel) {
            val scenarioFiles = remember(showScenarioFilesPanel) { GameState.listScenarioFiles(context) }
            val scenariosFolderPath = remember { GameState.scenariosDirectoryPath(context) }
            val srdDndFiles = remember(showScenarioFilesPanel) {
                com.jc2.jdrcompagnon.ui.PublicFilesStore.list(context, "SRD/dnd", "md").map { "dnd/${it.name}" to it }
            }
            val srdNaheulbeukFiles = remember(showScenarioFilesPanel) {
                com.jc2.jdrcompagnon.ui.PublicFilesStore.list(context, "SRD/naheulbeuk", "md").map { "naheulbeuk/${it.name}" to it }
            }
            val srdFolderPath = remember { com.jc2.jdrcompagnon.ui.PublicFilesStore.directoryLabel("SRD") }
            ModalBottomSheet(onDismissRequest = { showScenarioFilesPanel = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text("Fichiers scénarios (.md)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dossier : $scenariosFolderPath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Copiez ou éditez ces fichiers avec un explorateur de fichiers (via USB ou une appli comme " +
                                "\"Mes fichiers\"/\"Files\") en suivant ce chemin. Toute modification externe sera reprise à la prochaine synchronisation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (scenarioFiles.isEmpty()) {
                        Text(
                            "Aucun fichier .md pour l'instant. Crée ou enregistre un scénario pour qu'il apparaisse ici.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        scenarioFiles.forEach { entry ->
                            ListItem(
                                headlineContent = { Text(entry.name) },
                                supportingContent = {
                                    val sizeKb = (entry.sizeBytes / 1024).coerceAtLeast(1)
                                    Text("$sizeKb Ko")
                                },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Fichiers SRD (bestiaire, sorts, équipement, règles)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dossier : $srdFolderPath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Copiés automatiquement depuis l'app au premier chargement de la Bibliothèque. Modifiez-les pour changer/ajouter des monstres, sorts, objets ou règles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (srdDndFiles.isEmpty() && srdNaheulbeukFiles.isEmpty()) {
                        Text(
                            "Aucun fichier SRD copié pour l'instant. Ouvrez la Bibliothèque (bestiaire, sorts...) une première fois pour qu'ils apparaissent ici.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        (srdDndFiles + srdNaheulbeukFiles).forEach { (label, entry) ->
                            ListItem(
                                headlineContent = { Text(label) },
                                supportingContent = {
                                    val sizeKb = (entry.sizeBytes / 1024).coerceAtLeast(1)
                                    Text("$sizeKb Ko")
                                },
                                leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            GameState.syncScenariosFromDisk(context)
                            com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository.clearCache()
                            showScenarioFilesPanel = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Synchronisation effectuée") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resynchroniser maintenant")
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DrawerSelectableItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardToolCard(
    tool: MjTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tool.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tool.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerToolItem(
    tool: MjTool,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tool.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: MjGroup,
    availableCharacters: List<com.jc2.jdrcompagnon.ui.Character>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onMemberToggle: (String) -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    Text("${group.memberIds.size} membres", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (expanded) "Réduire" else "Modifier", color = MaterialTheme.colorScheme.primary)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Associer des PJ/PNJ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                if (availableCharacters.isEmpty()) {
                    Text("Aucun personnage PJ/PNJ disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    availableCharacters.forEach { character ->
                        val selected = group.memberIds.contains(character.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMemberToggle(character.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selected, onCheckedChange = { onMemberToggle(character.id) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(character.name, fontWeight = FontWeight.SemiBold)
                                Text(character.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}