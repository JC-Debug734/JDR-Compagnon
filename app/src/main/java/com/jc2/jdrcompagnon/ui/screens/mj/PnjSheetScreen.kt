package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_preparation.ui.ApparitionsPnjContent
import com.jc2.jdrcompagnon.feature_preparation.ui.SecretsConnusDuPnj
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.components.PnjReputationEditor
import com.jc2.jdrcompagnon.ui.components.rememberCharacterPortraitPainter
import com.jc2.jdrcompagnon.ui.screens.mj.library.PnjStatBlock
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.InfosComportementalesPnj
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.PnjBriefingOverlay
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.PortraitPlaceholder
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val FicheHint = Color.White.copy(alpha = 0.8f)
private const val OWNER_KEY = "fiche-pnj"

/**
 * Fiche d'un PNJ côté MJ, sur une seule page et sans rien d'une fiche de joueur : qui il est,
 * comment l'incarner, ce qu'il sait et où il apparaît, puis ses statistiques en bloc de bestiaire.
 * La fiche complète (équipement par emplacement, effets...) reste accessible par le menu ⋮.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnjSheetScreen(
    characterId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenFicheComplete: () -> Unit,
    onOpenScenario: (String) -> Unit,
    onOpenCampaign: (String) -> Unit,
    onOpenPreparation: () -> Unit,
    onOpenFaction: (String) -> Unit = {},
) {
    val characters by GameState.characters.collectAsState()
    val pnj = characters.firstOrNull { it.id == characterId } ?: return
    var menu by remember { mutableStateOf(false) }
    var suppression by remember { mutableStateOf(false) }
    var conversation by remember { mutableStateOf(false) }
    var possessions by remember { mutableStateOf(false) }
    var choixPortrait by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pnj.name, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color.White) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Plus", tint = Color.White) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Fiche complète (équipement, effets…)") }, onClick = { menu = false; onOpenFicheComplete() })
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; suppression = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EnTete(pnj, onPortrait = { choixPortrait = true })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { conversation = true }, modifier = Modifier.weight(1f)) {
                    Text("💬 Conversation", color = Color.White)
                }
                OutlinedButton(onClick = { possessions = true }, modifier = Modifier.weight(1f)) {
                    Text("💰 Possessions", color = Color.White)
                }
            }

            // Comportement, intentions, objectif, secrets... (ou personnalité improvisée si la fiche est maigre).
            InfosComportementalesPnj(pnj)

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Ce qu'il sait, où il apparaît") {
                SecretsConnusDuPnj(pnj)
                ApparitionsPnjContent(pnj, onOpenScenario, onOpenCampaign, onOpenPreparation, onOpenFaction)
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Statistiques", defaultExpanded = false) {
                PnjStatBlock(pnj, modifier = Modifier.clip(RoundedCornerShape(8.dp)))
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Réputation", defaultExpanded = false) {
                PnjReputationEditor(
                    character = pnj,
                    onUpdate = { GameState.updateCharacter(it) },
                    textColor = Color.White,
                    sectionTitle = { titre -> Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White) },
                )
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Notes") {
                OutlinedTextField(
                    value = pnj.notes,
                    onValueChange = { GameState.updateCharacterNotes(pnj.id, it) },
                    placeholder = { Text("Ce qui s'est passé avec lui, ce qu'il pense du groupe…", color = FicheHint.copy(alpha = 0.6f)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ForcedDarkPalette.AccentGold,
                        unfocusedBorderColor = FicheHint.copy(alpha = 0.5f),
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (conversation) {
        val groupeId by GameState.currentGroupId.collectAsState()
        PnjBriefingOverlay(
            character = pnj,
            reputationGroupe = groupeId?.let { pnj.groupReputations[it] },
            onDismiss = { conversation = false },
        )
    }
    if (possessions) {
        com.jc2.jdrcompagnon.feature_butin.ui.ButinPersonnageDialog(pnj, onDismiss = { possessions = false })
    }
    if (choixPortrait) {
        com.jc2.jdrcompagnon.ui.screens.joueur.PortraitPickerDialog(
            currentPortrait = pnj.portrait,
            pnj = true,
            onSelect = { GameState.setCharacterPortrait(pnj.id, it); choixPortrait = false },
            onDismiss = { choixPortrait = false },
        )
    }
    if (suppression) {
        AlertDialog(
            onDismissRequest = { suppression = false },
            title = { Text("Supprimer ${pnj.name} ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = { suppression = false; GameState.removeCharacter(pnj.id); onBack() }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { suppression = false }) { Text("Annuler") } },
        )
    }
}

/** Portrait, identité en une ligne, lieu, et PV ajustables d'un appui (−/+) pendant le jeu. */
@Composable
private fun EnTete(pnj: Character, onPortrait: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val portrait = rememberCharacterPortraitPainter(pnj.portrait)
            if (portrait != null) {
                Image(
                    painter = portrait,
                    contentDescription = "Portrait (appui pour changer)",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onPortrait),
                )
            } else {
                androidx.compose.foundation.layout.Box(Modifier.clickable(onClick = onPortrait)) { PortraitPlaceholder(size = 96.dp) }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val identite = listOf(pnj.race, pnj.characterClass).filter { it.isNotBlank() }.joinToString(" · ")
                if (identite.isNotBlank()) Text(identite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                pnj.location.takeIf { it.isNotBlank() }?.let { Text("📍 $it", style = MaterialTheme.typography.bodySmall, color = FicheHint) }
                pnj.alignment.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = FicheHint) }
                pnj.appearance.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = FicheHint, maxLines = 3, modifier = Modifier.heightIn(max = 60.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { GameState.updateCharacter(pnj.copy(currentHitPoints = (pnj.currentHitPoints - 1).coerceAtLeast(0))) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Retirer 1 PV", tint = Color.White)
                    }
                    Text(
                        "PV ${pnj.currentHitPoints}/${pnj.maxHitPoints}",
                        fontWeight = FontWeight.Bold,
                        color = if (pnj.currentHitPoints <= pnj.maxHitPoints / 4) MaterialTheme.colorScheme.error else Color.White,
                    )
                    IconButton(onClick = { GameState.updateCharacter(pnj.copy(currentHitPoints = (pnj.currentHitPoints + 1).coerceAtMost(pnj.maxHitPoints))) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter 1 PV", tint = Color.White)
                    }
                    Text("  CA ${GameState.caCombat(pnj)}", color = FicheHint, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
