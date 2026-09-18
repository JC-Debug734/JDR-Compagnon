package com.jc2.jdrcompagnon.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private data class ScenarioToDelete(val id: String, val title: String)

/**
 * Menu latéral partagé côté MJ, utilisé de la même façon sur tous les écrans
 * concernés (accueil MJ, sélection de personnage, fiche de personnage) pour
 * garder un menu identique quel que soit l'écran d'où on l'ouvre — avant
 * cette extraction, chaque écran réinventait son propre contenu de tiroir
 * (certains se limitaient à un simple "Retour"), ce qui donnait l'impression
 * de perdre l'accès aux outils MJ en changeant d'écran.
 *
 * Sélecteurs Campagne/Scénario branchés sur l'état global persistant
 * (GameState.currentCampaignId / GameState.lastScenarioId) plutôt que sur un
 * état local à un écran, pour que le choix fait ici reste cohérent partout,
 * y compris sur le tableau de bord MJ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MjDrawer(
    currentWorld: WorldState?,
    onOpenAccueil: () -> Unit,
    onOpenCampaigns: () -> Unit,
    onOpenScenarioEditor: (String?) -> Unit,
    onOpenBoutiques: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { GameState.syncScenariosFromDisk(context) }
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val mjCampaigns by GameState.mjCampaigns.collectAsState()
    val selectedCampaignId by GameState.currentCampaignId.collectAsState()
    val selectedScenarioId by GameState.lastScenarioId.collectAsState()

    val selectedCampaign = selectedCampaignId?.let { id -> mjCampaigns.firstOrNull { it.id == id } }
    val visibleScenarios = if (selectedCampaign != null) {
        mjScenarios.filter { it.id in selectedCampaign.scenarioIds }
    } else {
        mjScenarios
    }

    var scenarioMenuForId by remember { mutableStateOf<String?>(null) }
    var showScenarioPickerMenu by remember { mutableStateOf(false) }
    var showCampaignPickerMenu by remember { mutableStateOf(false) }
    var scenarioToDelete by remember { mutableStateOf<ScenarioToDelete?>(null) }
    var showScenarioFilesPanel by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = ForcedDarkPalette.Surface,
        drawerContentColor = ForcedDarkPalette.Content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Header with role icon (clickable to change role) and settings ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Role icon (clickable to return to role selection)
                IconButton(
                    onClick = {
                        onCloseDrawer()
                        GameState.requestRoleChange()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Maître du Jeu",
                        tint = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "MENU MJ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForcedDarkPalette.AccentGold,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        onCloseDrawer()
                        onOpenSettings()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Réglages",
                        tint = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            currentWorld?.name?.let {
                Text(
                    text = "Univers : $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                )
            }
            HorizontalDivider(color = ForcedDarkPalette.Indicator)

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
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAMPAGNE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = ForcedDarkPalette.AccentGold
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
                            GameState.setCurrentCampaignId(null)
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
                                GameState.setCurrentCampaignId(campaign.id)
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
            color = ForcedDarkPalette.Content
        )

        // --- CAMPAIGN ITEMS (visible only if campaign is selected) ---
        if (selectedCampaign != null) {
            Spacer(modifier = Modifier.height(8.dp))

            // Fiche de suivi
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = ForcedDarkPalette.Content) },
                label = { Text("Fiche de suivi", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = { /* TODO: Open campaign tracking */ },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content,
                ),
            )

            // Lieux
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Place, contentDescription = null, tint = ForcedDarkPalette.Content) },
                label = { Text("Lieux", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = { /* TODO: Open locations */ },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content,
                ),
            )

            // Cartes
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Map, contentDescription = null, tint = ForcedDarkPalette.Content) },
                label = { Text("Cartes", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = { /* TODO: Open maps */ },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content,
                ),
            )

            HorizontalDivider(color = ForcedDarkPalette.Indicator)
        }

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
                    Icon(Icons.Default.Description, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SCÉNARIOS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = ForcedDarkPalette.AccentGold
                    )
                }
                DropdownMenu(
                    expanded = showScenarioPickerMenu,
                    onDismissRequest = { showScenarioPickerMenu = false }
                ) {
                    visibleScenarios.forEach { scenario ->
                        Box {
                            DropdownMenuItem(
                                text = { Text(scenario.title) },
                                onClick = {
                                    showScenarioPickerMenu = false
                                    GameState.setLastScenarioId(scenario.id)
                                    onCloseDrawer()
                                },
                                leadingIcon = if (selectedScenarioId == scenario.id) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                trailingIcon = {
                                    IconButton(onClick = { scenarioMenuForId = scenario.id }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Actions sur ${scenario.title}")
                                    }
                                }
                            )
                            DropdownMenu(
                                expanded = scenarioMenuForId == scenario.id,
                                onDismissRequest = { scenarioMenuForId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Modifier") },
                                    onClick = {
                                        scenarioMenuForId = null
                                        showScenarioPickerMenu = false
                                        onCloseDrawer()
                                        onOpenScenarioEditor(scenario.id)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Supprimer") },
                                    onClick = {
                                        scenarioMenuForId = null
                                        showScenarioPickerMenu = false
                                        scenarioToDelete = ScenarioToDelete(scenario.id, scenario.title)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
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
                color = ForcedDarkPalette.Content
            )
        }

        // --- BOUTIQUES ---
        HorizontalDivider(color = ForcedDarkPalette.Indicator)
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = ForcedDarkPalette.Content) },
            label = { Text("Boutiques", color = ForcedDarkPalette.Content) },
            selected = false,
            onClick = {
                onCloseDrawer()
                onOpenBoutiques()
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = ForcedDarkPalette.Surface,
                unselectedIconColor = ForcedDarkPalette.Content,
                unselectedTextColor = ForcedDarkPalette.Content,
            ),
        )

        // --- Change role (at bottom) ---
        HorizontalDivider(color = ForcedDarkPalette.Indicator)
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.SwapHoriz, null, tint = ForcedDarkPalette.Content) },
            label = { Text("Changer de rôle", color = ForcedDarkPalette.Content) },
            selected = false,
            onClick = { GameState.requestRoleChange() },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = ForcedDarkPalette.Surface,
                unselectedIconColor = ForcedDarkPalette.Content,
                unselectedTextColor = ForcedDarkPalette.Content,
            ),
        )
        }
    }

    scenarioToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { scenarioToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer le scénario « ${target.title} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedScenarioId == target.id) {
                            GameState.setLastScenarioId(null)
                        }
                        GameState.removeMjScenario(target.id, context)
                        scenarioToDelete = null
                        Toast.makeText(context, "Scénario supprimé", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Synchronisation effectuée", Toast.LENGTH_SHORT).show()
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
