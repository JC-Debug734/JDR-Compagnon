package com.jc2.jdrcompagnon.ui.screens.mj

import com.jc2.jdrcompagnon.feature_evenement.ui.icone
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_group.ui.LocationField
import com.jc2.jdrcompagnon.feature_group.ui.rememberLieuxConnus
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus
import com.jc2.jdrcompagnon.feature_quete.ui.CampaignQuestsContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.CampagneFileStore
import com.jc2.jdrcompagnon.ui.CampagneImageStore
import com.jc2.jdrcompagnon.ui.rememberImageFichier
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.components.DatePickerRow
import com.jc2.jdrcompagnon.ui.components.SelectableListCard
import com.jc2.jdrcompagnon.ui.components.forgetCollapsibleSections
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    onOpenCampaignEditor: (String?) -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val campaigns by GameState.mjCampaigns.collectAsState()
    val worldCampaigns = remember(campaigns, currentWorld) {
        campaigns.filter { it.worldId == currentWorld?.id }
    }
    val selectedCampaignId by GameState.currentCampaignId.collectAsState()
    var campaignToDelete by remember { mutableStateOf<GameState.MjCampaign?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campagnes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // Dossier de campagne : via l'outil IMPORT.
                    IconButton(onClick = { com.jc2.jdrcompagnon.feature_import.ImportNavigation.ouvrir() }) {
                        Icon(Icons.Default.DriveFolderUpload, contentDescription = "Importer une campagne")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenCampaignEditor(null) },
                containerColor = ForcedDarkPalette.AccentGold,
                contentColor = ForcedDarkPalette.Background
            ) {
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
                    val enCours = campaign.quests.count { it.status == QuestStatus.EN_COURS }
                    val terminees = campaign.quests.count { it.status == QuestStatus.TERMINEE }
                    SelectableListCard(
                        title = campaign.title,
                        subtitle = if (campaign.quests.isEmpty()) "Aucune quête"
                        else "$enCours quête(s) en cours · $terminees terminée(s)",
                        selected = campaign.id == selectedCampaignId,
                        onToggleSelect = {
                            GameState.setCurrentCampaignId(if (selectedCampaignId == campaign.id) null else campaign.id)
                        },
                        onEdit = { onOpenCampaignEditor(campaign.id) },
                        onDelete = { campaignToDelete = campaign }
                    )
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
                        CampagneImageStore.nettoyer(context, campaign.id)
                        forgetCampaignSections(context, campaign.id)
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
                text = "Appuyez sur + pour créer une campagne et suivre ses quêtes.",
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
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    onOpenCarte: (campagneId: String, carteId: String?) -> Unit = { _, _ -> },
    onOpenEvenements: (String) -> Unit = {},
    onOpenVilles: (String) -> Unit = {},
    onOpenVilleDetail: (campagneId: String, villeId: String) -> Unit = { _, _ -> },
    onOpenBestiaryDetail: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
    var monsterIds by remember(existing) { mutableStateOf(existing?.monsterIds ?: emptyList()) }
    var calendarId by remember(existing) { mutableStateOf(existing?.calendarId) }
    // Id connu dès l'ouverture (y compris pour une nouvelle campagne) pour nommer son image.
    val effectiveCampaignId = remember { campaignId ?: java.util.UUID.randomUUID().toString() }
    var imageFileName by remember(existing) { mutableStateOf(existing?.imageFileName) }
    val choisirImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            CampagneImageStore.copier(context, uri, effectiveCampaignId)?.let { imageFileName = it }
        }
    }
    // Cartes repliées/dépliées : configuration propre à chaque campagne.
    val sectionKey = campaignId ?: "nouvelle"
    var showAttachDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var hasAttemptedSave by remember { mutableStateOf(false) }
    val isTitleError = hasAttemptedSave && title.isBlank()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (campaignId == null) "Nouvelle campagne" else "Éditer la campagne", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                                monsterIds = monsterIds,
                                calendarId = calendarId,
                                imageFileName = imageFileName
                            ) ?: GameState.CampaignData(
                                id = effectiveCampaignId,
                                title = title,
                                worldId = currentWorldId ?: "",
                                scenarioIds = attachedIds,
                                monsterIds = monsterIds,
                                calendarId = calendarId
                            ).toMjCampaign().copy(imageFileName = imageFileName)
                            if (existing != null) GameState.updateMjCampaign(updated)
                            else GameState.addMjCampaign(updated)
                            CampagneImageStore.nettoyer(context, updated.id, garder = imageFileName)
                            coroutineScope.launch {
                                CampagneFileStore.exporterCampagne(context, updated)
                                onBack()
                            }
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
                colors = campaignFieldColors(),
                supportingText = {
                    if (isTitleError) {
                        Text("Le titre est obligatoire", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            CampaignSectionCard(sectionKey, "Image de la campagne") {
                Text(
                    "Affichée sur l'accueil MJ quand la campagne est sélectionnée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CampaignHintColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                val image = rememberImageFichier(imageFileName?.let { CampagneImageStore.fichier(context, it) })
                if (image != null) {
                    androidx.compose.foundation.Image(
                        bitmap = image,
                        contentDescription = "Image de la campagne",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { choisirImageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (imageFileName == null) "Choisir une image" else "Changer l'image")
                    }
                    if (imageFileName != null) {
                        OutlinedButton(onClick = { imageFileName = null }) { Text("Retirer") }
                    }
                }
            }

            CampaignSectionCard(sectionKey, "Calendrier de la campagne") {
                    Text(
                        "Choisi parmi les calendriers créés dans l'outil Horloge (dates et jours défilent pour tout le monde, seul le calendrier affiché change ici).",
                        style = MaterialTheme.typography.bodySmall,
                        color = CampaignHintColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val clock by ScenarioClockState.state.collectAsState()
                    val calendars by ScenarioClockState.calendars.collectAsState()
                    val selectedCalendar = remember(calendars, calendarId) { ScenarioClockState.calendarById(calendarId) }

                    var calendarMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = calendarMenuExpanded,
                        onExpandedChange = { calendarMenuExpanded = !calendarMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedCalendar.nom,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Calendrier") },
                            colors = campaignFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(calendarMenuExpanded) },
                        )
                        ExposedDropdownMenu(expanded = calendarMenuExpanded, onDismissRequest = { calendarMenuExpanded = false }) {
                            calendars.forEach { calendar ->
                                DropdownMenuItem(
                                    text = { Text(calendar.nom) },
                                    onClick = { calendarId = calendar.id; calendarMenuExpanded = false },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        ScenarioClockState.formattedCalendarDate(clock.scenarioMinutes, selectedCalendar),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DatePickerRow(
                        config = selectedCalendar,
                        initialMinutes = clock.scenarioMinutes,
                        onSetDate = { year, month, day -> ScenarioClockState.setCurrentDate(year, month, day, selectedCalendar.id) },
                    )
            }

            CampaignSectionCard(sectionKey, "Scénarios attachés") {
                    if (attachedIds.isEmpty()) {
                        Text(
                            "Aucun scénario attaché.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CampaignHintColor
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

            BestiaireAttacheCard(
                monsterIds = monsterIds,
                worldId = currentWorldId,
                onMonsterIdsChanged = { monsterIds = it },
                sectionKey = sectionKey,
                onOpenBestiaryDetail = onOpenBestiaryDetail
            )

            // Villes, carte et événements de campagne : une carte chacun, qui liste ce qui a déjà
            // été créé (mis à jour en direct depuis la base) en plus du bouton d'accès.
            CampaignVillesCard(
                sectionKey = sectionKey,
                campagneId = existing?.id,
                onOpenVilles = onOpenVilles,
                onOpenVilleDetail = onOpenVilleDetail
            )
            CampaignCartesSection(
                sectionKey = sectionKey,
                campagneId = existing?.id,
                onOpenCarte = onOpenCarte
            )
            CampaignEvenementsCard(
                sectionKey = sectionKey,
                campagneId = existing?.id,
                onOpenEvenements = onOpenEvenements
            )
            CampaignEnvironnementCard(sectionKey = sectionKey, campaign = existing)

            CampaignSectionCard(sectionKey, "Quêtes") {
                if (existing == null) {
                    CampagneNonEnregistreeHint("ses quêtes")
                } else {
                    CampaignQuestsContent(existing)
                }
            }

            CampaignPnjCard(sectionKey = sectionKey, campaign = existing)

            if (existing != null) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Réinitialiser la campagne")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Réinitialiser la campagne") },
            text = {
                Text(
                    "Remettre « ${existing.title} » à zéro pour la rejouer ?\n\n" +
                        "• toutes les quêtes repassent « En cours » et pourront de nouveau être récompensées ;\n" +
                        "• les scènes des scénarios attachés ne sont plus validées ;\n" +
                        "• le brouillard d'exploration des cartes est remis.\n\n" +
                        "Le contenu préparé (quêtes, récompenses, scénarios, PNJ, cartes, villes) est conservé. " +
                        "Les récompenses déjà distribuées aux personnages ne leur sont pas retirées."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    GameState.resetMjCampaignProgress(existing.id, context)
                    coroutineScope.launch {
                        CarteDependencies.repository.observerCartes(existing.id).first().forEach { carte ->
                            com.jc2.jdrcompagnon.feature_exploration.ExplorationSession
                                .reinitialiserBrouillard(context, "carte:${carte.id}")
                        }
                        snackbarHostState.showSnackbar("Campagne réinitialisée")
                    }
                }) { Text("Réinitialiser", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Annuler") } }
        )
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

/**
 * Carte de section de l'éditeur de campagne (voir [CollapsibleSectionCard]) : état replié
 * propre à chaque campagne, persisté.
 */
@Composable
private fun CampaignSectionCard(
    campaignKey: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) = CollapsibleSectionCard(ownerKey = "campagne:$campaignKey", title = title, content = content)

/** Message commun aux cartes Villes/Carte/Événements tant que la campagne n'est pas enregistrée. */
@Composable
private fun CampagneNonEnregistreeHint(quoi: String) {
    Text(
        "Enregistrez d'abord la campagne (bouton « Enregistrer » en haut) pour accéder à $quoi.",
        style = MaterialTheme.typography.bodySmall,
        color = CampaignHintColor
    )
}

/** Ligne cliquable d'un élément créé (ville, lieu, événement) dans les cartes de campagne. */
@Composable
private fun CampaignItemRow(icon: androidx.compose.ui.graphics.vector.ImageVector, titre: String, detail: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titre, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!detail.isNullOrBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = CampaignHintColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CampaignVillesCard(
    sectionKey: String,
    campagneId: String?,
    onOpenVilles: (String) -> Unit,
    onOpenVilleDetail: (campagneId: String, villeId: String) -> Unit,
) {
    CampaignSectionCard(sectionKey, "Villes") {
        if (campagneId == null) {
            CampagneNonEnregistreeHint("ses villes")
            return@CampaignSectionCard
        }
        val points by remember(campagneId) { CarteDependencies.repository.observerPoints(campagneId) }
            .collectAsState(initial = emptyList())
        val villes = points.filter { it.type == TypePointInteret.VILLE }
        if (villes.isEmpty()) {
            Text("Aucune ville créée.", style = MaterialTheme.typography.bodyMedium, color = CampaignHintColor)
        } else {
            villes.forEach { ville ->
                CampaignItemRow(
                    icon = Icons.Default.LocationCity,
                    titre = ville.nom,
                    detail = listOfNotNull(
                        "non placée".takeIf { ville.carteId == null },
                        ville.boutiqueIds.size.takeIf { it > 0 }?.let { "$it boutique(s)" },
                        ville.scenarioIds.size.takeIf { it > 0 }?.let { "$it scénario(s)" },
                    ).joinToString(" · ").ifBlank { ville.description }
                ) { onOpenVilleDetail(campagneId, ville.id) }
            }
        }
        OutlinedButton(onClick = { onOpenVilles(campagneId) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LocationCity, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Gérer les villes")
        }
    }
}

/**
 * Cartes géographiques de la campagne : une carte d'écran par carte, qui liste son contenu
 * (aperçu de l'image, grille, lieux placés dessus), puis les lieux placés sur aucune carte et
 * un bouton pour créer une carte. Toutes les cartes sont supprimables : leurs lieux ne sont pas
 * perdus, ils passent dans "Lieux non placés" (et les villes restent dans la carte Villes).
 */
@Composable
private fun CampaignCartesSection(
    sectionKey: String,
    campagneId: String?,
    onOpenCarte: (campagneId: String, carteId: String?) -> Unit,
) {
    if (campagneId == null) {
        CampaignSectionCard(sectionKey, "Cartes de la campagne") { CampagneNonEnregistreeHint("ses cartes") }
        return
    }
    val coroutineScope = rememberCoroutineScope()
    val cartes by remember(campagneId) { CarteDependencies.repository.observerCartes(campagneId) }
        .collectAsState(initial = emptyList())
    val points by remember(campagneId) { CarteDependencies.repository.observerPoints(campagneId) }
        .collectAsState(initial = emptyList())
    var showNouvelleCarte by remember { mutableStateOf(false) }
    var carteASupprimer by remember { mutableStateOf<CarteCampagne?>(null) }

    if (cartes.isEmpty()) {
        CampaignSectionCard(sectionKey, "Cartes de la campagne") {
            Text("Aucune carte. Créez-en une pour y placer vos lieux.", style = MaterialTheme.typography.bodyMedium, color = CampaignHintColor)
        }
    }
    cartes.forEach { carte ->
        CampaignCarteCard(
            sectionKey = sectionKey,
            carte = carte,
            points = points.filter { it.carteId == carte.id },
            onOpen = { onOpenCarte(campagneId, carte.id) },
            onDelete = { carteASupprimer = carte }
        )
    }
    val nonPlaces = points.filter { it.carteId == null }
    if (nonPlaces.isNotEmpty()) {
        CampaignSectionCard(sectionKey, "Lieux non placés") {
            Text(
                "Pour placer un lieu, ouvrez une carte et touchez une case vide.",
                style = MaterialTheme.typography.bodySmall,
                color = CampaignHintColor
            )
            nonPlaces.forEach { point ->
                CampaignItemRow(
                    icon = if (point.type == TypePointInteret.VILLE) Icons.Default.LocationCity else Icons.Default.Place,
                    titre = point.nom,
                    detail = point.type.label
                ) { cartes.firstOrNull()?.let { onOpenCarte(campagneId, it.id) } }
            }
        }
    }
    OutlinedButton(
        onClick = { showNouvelleCarte = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            contentColor = Color.White
        )
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Nouvelle carte")
    }

    if (showNouvelleCarte) {
        var nom by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNouvelleCarte = false },
            title = { Text("Nouvelle carte") },
            text = {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom (ex : Région du Nord, Donjon...)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nom.isNotBlank(),
                    onClick = {
                        val nouvelle = CarteCampagne(
                            campagneId = campagneId,
                            id = java.util.UUID.randomUUID().toString(),
                            nom = nom.trim()
                        )
                        coroutineScope.launch { CarteDependencies.repository.sauvegarderCarte(nouvelle) }
                        showNouvelleCarte = false
                    }
                ) { Text("Créer") }
            },
            dismissButton = { TextButton(onClick = { showNouvelleCarte = false }) { Text("Annuler") } }
        )
    }

    carteASupprimer?.let { carte ->
        AlertDialog(
            onDismissRequest = { carteASupprimer = null },
            title = { Text("Supprimer la carte") },
            text = { Text("Supprimer « ${carte.nom} » ? Ses lieux ne sont pas perdus : ils sont retirés de la carte et restent dans la campagne, prêts à être placés sur une autre carte.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch { CarteDependencies.repository.supprimerCarte(carte) }
                    carteASupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { carteASupprimer = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun CampaignCarteCard(
    sectionKey: String,
    carte: CarteCampagne,
    points: List<com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret>,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    CollapsibleSectionCard(
        ownerKey = "campagne:$sectionKey",
        title = "🗺 ${carte.nom}",
        headerActions = {
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer la carte", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) {
        // Aperçu de l'image de fond choisie par le MJ, s'il y en a une.
        val apercu = remember(carte.imageFileName) {
            carte.imageFileName?.let { nom ->
                runCatching {
                    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                    android.graphics.BitmapFactory.decodeFile(CarteImageStore.fichier(context, nom).absolutePath, options)?.asImageBitmap()
                }.getOrNull()
            }
        }
        apercu?.let {
            androidx.compose.foundation.Image(
                bitmap = it,
                contentDescription = "Aperçu de la carte",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onOpen)
            )
        }
        Text(
            "${carte.largeurCases} × ${carte.hauteurCases} cases · ${carte.echelleKmParCase} km par case",
            style = MaterialTheme.typography.bodySmall,
            color = CampaignHintColor
        )
        if (points.isEmpty()) {
            Text("Aucun lieu placé sur cette carte.", style = MaterialTheme.typography.bodyMedium, color = CampaignHintColor)
        } else {
            points.forEach { point ->
                CampaignItemRow(
                    icon = if (point.type == TypePointInteret.VILLE) Icons.Default.LocationCity else Icons.Default.Place,
                    titre = point.nom,
                    detail = point.type.label
                ) { onOpen() }
            }
        }
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ouvrir la carte")
        }
    }
}

@Composable
private fun CampaignEvenementsCard(
    sectionKey: String,
    campagneId: String?,
    onOpenEvenements: (String) -> Unit,
) {
    CampaignSectionCard(sectionKey, "Événements de campagne") {
        if (campagneId == null) {
            CampagneNonEnregistreeHint("ses événements")
            return@CampaignSectionCard
        }
        // Événements de la bibliothèque marqués pour cette campagne (les communs du monde sont
        // visibles depuis l'écran Événements de campagne).
        val evenements by remember(campagneId) {
            com.jc2.jdrcompagnon.di.EvenementDependencies.repository.observerEvenements(
                com.jc2.jdrcompagnon.di.EvenementDependencies.mondeDeCampagne(campagneId)
            )
        }.collectAsState(initial = emptyList())
        val deCampagne = evenements.filter { it.campagneId == campagneId }
        val coroutineScope = rememberCoroutineScope()
        var showPicker by remember { mutableStateOf(false) }
        Text(
            "Sélectionnez des événements de la bibliothèque pour les réserver à cette campagne (ils sont tirés pendant les trajets et sur l'écran Événements de campagne).",
            style = MaterialTheme.typography.bodySmall,
            color = CampaignHintColor
        )
        if (deCampagne.isEmpty()) {
            Text("Aucun événement propre à cette campagne.", style = MaterialTheme.typography.bodyMedium, color = CampaignHintColor)
        } else {
            deCampagne.forEach { evenement ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        CampaignItemRow(
                            icon = evenement.type.icone,
                            titre = evenement.titre,
                            detail = evenement.type.label + if (evenement.description.isNotBlank()) " · ${evenement.description}" else ""
                        ) { onOpenEvenements(campagneId) }
                    }
                    // Retiré de la campagne : redevient commun au monde (rien n'est supprimé).
                    IconButton(onClick = {
                        coroutineScope.launch {
                            com.jc2.jdrcompagnon.di.EvenementDependencies.repository.sauvegarder(evenement.copy(campagneId = null))
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Retirer ${evenement.titre} de la campagne")
                    }
                }
            }
        }
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LibraryAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sélectionner dans la bibliothèque")
        }
        OutlinedButton(onClick = { onOpenEvenements(campagneId) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Gérer / tirer les événements")
        }
        if (showPicker) {
            com.jc2.jdrcompagnon.feature_evenement.ui.EvenementPickerDialog(
                worldId = com.jc2.jdrcompagnon.di.EvenementDependencies.mondeDeCampagne(campagneId),
                exclus = deCampagne.map { it.id }.toSet(),
                campagneId = campagneId,
                onDismiss = { showPicker = false },
                onValider = { choisis ->
                    coroutineScope.launch {
                        com.jc2.jdrcompagnon.di.EvenementDependencies.repository
                            .sauvegarderTous(choisis.map { it.copy(campagneId = campagneId) })
                    }
                    showPicker = false
                }
            )
        }
    }
}

/**
 * Environnement par défaut de la campagne (forêt, montagnes...) : ses événements liés sont
 * proposés au MJ pendant les trajets tracés sur la carte.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CampaignEnvironnementCard(sectionKey: String, campaign: GameState.MjCampaign?) {
    CampaignSectionCard(sectionKey, "Environnement de la campagne") {
        if (campaign == null) {
            CampagneNonEnregistreeHint("son environnement")
            return@CampaignSectionCard
        }
        val environnements by remember(campaign.worldId) {
            com.jc2.jdrcompagnon.di.EnvironmentDependencies.repository.observerEnvironnements(campaign.worldId)
        }.collectAsState(initial = emptyList())
        Text(
            "Sert aux événements proposés pendant les trajets (événements liés à l'environnement).",
            style = MaterialTheme.typography.bodySmall,
            color = CampaignHintColor
        )
        if (environnements.isEmpty()) {
            Text("Aucun environnement dans ce monde (outil Environnements).", style = MaterialTheme.typography.bodyMedium, color = CampaignHintColor)
            return@CampaignSectionCard
        }
        val choisi = environnements.firstOrNull { it.id == campaign.environnementId }
        var showChoix by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showChoix = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Landscape, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(choisi?.nom ?: "Choisir l'environnement", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        if (showChoix) {
            AlertDialog(
                onDismissRequest = { showChoix = false },
                title = { Text("Environnement de la campagne") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        item {
                            EnvironnementChoixRow("Aucun", null, selected = choisi == null) {
                                GameState.updateMjCampaign(campaign.copy(environnementId = null))
                                showChoix = false
                            }
                        }
                        items(environnements, key = { it.id }) { env ->
                            EnvironnementChoixRow(env.nom, env.description, selected = env.id == choisi?.id) {
                                GameState.updateMjCampaign(campaign.copy(environnementId = env.id))
                                showChoix = false
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showChoix = false }) { Text("Annuler") } }
            )
        }
    }
}

/** Ligne du choix d'environnement : bouton radio, nom et début de description. */
@Composable
private fun EnvironnementChoixRow(nom: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(nom, fontWeight = FontWeight.Bold)
            if (!description.isNullOrBlank()) {
                Text(description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Oublie la configuration des cartes d'une campagne supprimée. */
private fun forgetCampaignSections(context: android.content.Context, campaignId: String) =
    forgetCollapsibleSections(context, "campagne:$campaignId")

/** Champs de saisie de l'éditeur : fond translucide (comme les cartes), texte blanc. */
@Composable
private fun campaignFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    errorContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Color.White,
    cursorColor = Color.White,
)

/** Texte secondaire des cartes de l'éditeur (explications, listes vides). */
private val CampaignHintColor = Color.White.copy(alpha = 0.8f)

/**
 * PNJ et créatures affectés à la campagne (MjCampaign.pnjIds, voir PorteeCampagne), chacun avec
 * son lieu (« Phandalin »). Enregistré immédiatement, comme les quêtes.
 */
@Composable
private fun CampaignPnjCard(sectionKey: String, campaign: GameState.MjCampaign?) {
    CampaignSectionCard(sectionKey, "PNJ de la campagne") {
        if (campaign == null) {
            CampagneNonEnregistreeHint("ses PNJ")
            return@CampaignSectionCard
        }
        val characters by GameState.characters.collectAsState()
        val duMonde = remember(characters, campaign.worldId) {
            characters.filter { it.type != "PJ" && (it.worldId == campaign.worldId || it.worldId.isBlank()) }
                .sortedBy { it.name.lowercase() }
        }
        val affectes = duMonde.filter { it.id in campaign.pnjIds }
        val candidats = duMonde.filter { it.id !in campaign.pnjIds }
        var showAdd by remember { mutableStateOf(false) }
        var lieuEdite by remember { mutableStateOf<com.jc2.jdrcompagnon.ui.Character?>(null) }
        val lieux = rememberLieuxConnus(campaign.id)

        Text(
            "Affectés à la campagne, ils n'apparaissent dans les listes que lorsqu'elle est sélectionnée. Touchez un PNJ pour changer son lieu.",
            style = MaterialTheme.typography.bodySmall,
            color = CampaignHintColor
        )
        if (affectes.isEmpty()) {
            Text("Aucun PNJ affecté.", style = MaterialTheme.typography.bodyMedium, color = CampaignHintColor)
        }
        // Regroupés par lieu : « Phandalin », « Neverwinter »... puis ceux sans lieu.
        affectes.groupBy { it.location.ifBlank { "Lieu non précisé" } }
            .toSortedMap(compareBy<String> { it == "Lieu non précisé" }.thenBy { it.lowercase() })
            .forEach { (lieu, pnjs) ->
                Text("📍 $lieu", color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                pnjs.forEach { pnj ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { lieuEdite = pnj },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pnj.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(pnj.type, pnj.race, pnj.characterClass).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = CampaignHintColor
                            )
                        }
                        IconButton(onClick = { GameState.setCampaignPnj(campaign.id, pnj.id, attached = false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Retirer de la campagne")
                        }
                    }
                }
            }
        Box {
            OutlinedButton(onClick = { showAdd = true }, enabled = candidats.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (duMonde.isEmpty()) "Aucune fiche PNJ dans ce monde" else "Affecter un PNJ")
            }
            DropdownMenu(expanded = showAdd, onDismissRequest = { showAdd = false }) {
                candidats.forEach { pnj ->
                    DropdownMenuItem(
                        text = { Text("${pnj.name} (${pnj.type})") },
                        onClick = {
                            GameState.setCampaignPnj(campaign.id, pnj.id, attached = true)
                            showAdd = false
                            lieuEdite = pnj
                        }
                    )
                }
            }
        }

        lieuEdite?.let { pnj ->
            var lieu by remember(pnj.id) { mutableStateOf(pnj.location) }
            AlertDialog(
                onDismissRequest = { lieuEdite = null },
                title = { Text("Où se trouve ${pnj.name} ?") },
                text = { LocationField(value = lieu, onValueChange = { lieu = it }, lieux = lieux) },
                confirmButton = {
                    TextButton(onClick = {
                        GameState.setCharacterLocation(pnj.id, lieu)
                        lieuEdite = null
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { lieuEdite = null }) { Text("Annuler") } }
            )
        }
    }
}

/** Monstres du bestiaire SRD attachés à la campagne (identifiés par nom), même principe que
 * les montures dans feature_group/ui/GroupCard.kt (MountsSubsection). */
@Composable
private fun BestiaireAttacheCard(
    monsterIds: List<String>,
    worldId: String?,
    onMonsterIdsChanged: (List<String>) -> Unit,
    onOpenBestiaryDetail: (String) -> Unit,
    sectionKey: String,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(query, worldId) {
        results = if (query.length < 2) {
            emptyList()
        } else {
            SrdRepository.searchMonsters(context, query, worldId).map { it.name }.take(6)
        }
    }

    CampaignSectionCard(sectionKey, "Bestiaire attaché") {
            if (monsterIds.isEmpty()) {
                Text(
                    "Aucun monstre attaché.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CampaignHintColor
                )
            } else {
                monsterIds.forEach { monsterName ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            monsterName,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenBestiaryDetail(monsterName) },
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { onMonsterIdsChanged(monsterIds - monsterName) }) {
                            Icon(Icons.Default.Close, contentDescription = "Détacher")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Rechercher un monstre du bestiaire") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = campaignFieldColors()
            )
            results.filter { it !in monsterIds }.forEach { monsterName ->
                Text(
                    text = monsterName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMonsterIdsChanged(monsterIds + monsterName)
                            query = ""
                        }
                        .padding(vertical = 4.dp)
                )
            }
    }
}