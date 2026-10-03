package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.feature_group.domain.usecase.SuggestBalancedEncounterUseCase
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Contenu détaillé d'un groupe d'aventuriers — tout ce qui le concerne, localisé : lieu du groupe
 * et trésor commun, répartition par lieu, membres (PJ, PNJ, créatures), PNJ de la campagne,
 * montures et animaux, véhicules, réputation par faction, inventaire commun, biens, et
 * calculateur de rencontre équilibrée. Affiché sur son propre écran (GroupDetailScreen).
 *
 * Les quêtes validées alimentent automatiquement trésor, réputation, montures, véhicules,
 * inventaire, biens et PNJ (voir ValiderQueteUseCase) ; tout reste modifiable à la main ici.
 * L'attitude des PNJ envers le groupe est réglée sur leur fiche (onglet PNJ) et récapitulée ici.
 *
 * La mutation des membres repasse par [onMemberToggle] (GroupDetailScreen) ; le reste passe
 * directement par GameState (updateMjGroup, assignMountRider, harnessMount...).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailContent(
    group: GameState.MjGroup,
    availableCharacters: List<Character>,
    onMemberToggle: (String) -> Unit,
    onOpenBestiaryDetail: (String) -> Unit,
    onOpenEquipmentDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCharacter: (Character) -> Unit = {},
    onTablePlayersChange: (List<GameState.TablePlayer>) -> Unit = {},
) {
    val members = availableCharacters.filter { it.id in group.memberIds }
    // Le calculateur de rencontre ne compte que les combattants "du côté" du groupe (PJ et
    // PNJ compagnons, joueurs sans fiche), pas les créatures qui l'accompagnent.
    val memberLevels = members.filter { it.type == "PJ" || it.type == "PNJ" }.map { it.level } +
        group.tablePlayers.map { it.level }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SubsectionCard { GroupLocationSubsection(group) }
        SubsectionCard { WhereIsWhatSubsection(group, members) }
        SubsectionCard { TablePlayersSubsection(group.tablePlayers, onTablePlayersChange) }
        MemberCategory.entries.forEach { category ->
            SubsectionCard {
                MembersSubsection(group, category, availableCharacters, onMemberToggle, onOpenCharacter)
            }
        }
        SubsectionCard { CampaignPnjsSubsection(group, onOpenCharacter) }
        SubsectionCard { PnjAttitudesSubsection(group, availableCharacters) }
        SubsectionCard { MountsSubsection(group, members, onOpenBestiaryDetail) }
        SubsectionCard { TransportsSubsection(group, onOpenEquipmentDetail) }
        SubsectionCard { ReputationSubsection(group) }
        SubsectionCard { InventorySubsection(group, members, onOpenEquipmentDetail) }
        SubsectionCard { AssetsSubsection(group) }
        SubsectionCard { EncounterCalculatorSubsection(memberLevels) }
    }
}

@Composable
internal fun SubsectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
internal fun SubsectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
}

// --- Membres : aventuriers (PJ), PNJ et créatures du groupe ---

private enum class MemberCategory(val title: String, val emptyLabel: String, val types: Set<String>) {
    AVENTURIERS("Aventuriers (PJ)", "Aucun PJ dans ce groupe.", setOf("PJ")),
    PNJ("PNJ du groupe", "Aucun PNJ dans ce groupe.", setOf("PNJ")),
    CREATURES("Créatures du groupe", "Aucune créature dans ce groupe.", setOf("Monstre", "Boss", "Créature", "Familier")),
}

@Composable
private fun MembersSubsection(
    group: GameState.MjGroup,
    category: MemberCategory,
    availableCharacters: List<Character>,
    onMemberToggle: (String) -> Unit,
    onOpenCharacter: (Character) -> Unit,
) {
    val ofCategory = availableCharacters.filter { it.type in category.types }
    val members = ofCategory.filter { it.id in group.memberIds }
    val candidates = ofCategory.filter { it.id !in group.memberIds }
    var showAddMenu by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf<Character?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SubsectionTitle("${category.title} — ${members.size}") }
            Box {
                TextButton(onClick = { showAddMenu = true }, enabled = candidates.isNotEmpty()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Ajouter")
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    candidates.forEach { character ->
                        DropdownMenuItem(
                            text = { Text("${character.name} (${character.type})") },
                            onClick = {
                                onMemberToggle(character.id)
                                showAddMenu = false
                            }
                        )
                    }
                }
            }
        }
        if (members.isEmpty()) {
            Text(
                if (ofCategory.isEmpty()) "Aucune fiche de ce type dans ce monde." else category.emptyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
        members.forEach { character ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCharacter(character) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(character.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        listOf(character.characterClass, character.race, "niv.${character.level}", "PV ${character.currentHitPoints}/${character.maxHitPoints}", "VIT ${character.speed} m")
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    val monture = group.mounts.firstOrNull { it.riderCharacterId == character.id && it.kind == com.jc2.jdrcompagnon.feature_group.domain.model.MountKind.MONTURE }
                    Text(
                        listOfNotNull(
                            "📍 ${lieuDe(character.location, group)}",
                            monture?.let { "🐎 ${it.name}" },
                        ).joinToString("  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { locating = character }) {
                    Icon(Icons.Default.Place, contentDescription = "Changer le lieu", tint = Color.White)
                }
                IconButton(onClick = { onMemberToggle(character.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer du groupe", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    locating?.let { character ->
        LocationDialog(
            title = "Où se trouve ${character.name} ?",
            initial = character.location,
            hint = "Vide = avec le groupe",
            onDismiss = { locating = null },
            onConfirm = { GameState.setCharacterLocation(character.id, it); locating = null },
        )
    }
}

// --- Joueurs sans fiche (personnes jouant sans l'application) ---

@Composable
private fun TablePlayersSubsection(
    players: List<GameState.TablePlayer>,
    onPlayersChange: (List<GameState.TablePlayer>) -> Unit,
) {
    var editing by remember { mutableStateOf<GameState.TablePlayer?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SubsectionTitle("Joueurs sans fiche — ${players.size}") }
            TextButton(onClick = {
                val level = players.lastOrNull()?.level ?: 1
                onPlayersChange(
                    players + GameState.TablePlayer(
                        name = "Joueur ${players.size + 1}",
                        level = level,
                        maxHitPoints = GameState.TablePlayer.estimatedHitPoints(level),
                    )
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Ajouter")
            }
        }
        if (players.isEmpty()) {
            Text(
                "Pour jouer avec des personnes sans l'application : ajoutez-les ici avec leur niveau.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
        players.forEach { player ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editing = player },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(player.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "niv.${player.level} • CA ${player.armorClass} • PV ${player.maxHitPoints}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { onPlayersChange(players.filterNot { it.id == player.id }) }) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer du groupe", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    editing?.let { player ->
        var name by remember(player.id) { mutableStateOf(player.name) }
        var level by remember(player.id) { mutableStateOf(player.level) }
        var armorClass by remember(player.id) { mutableStateOf(player.armorClass) }
        var hitPoints by remember(player.id) { mutableStateOf(player.maxHitPoints) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Joueur sans fiche") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom") },
                        singleLine = true,
                    )
                    NumberStepper("Niveau", level, 1..20) { newLevel ->
                        // PV encore à l'estimation par défaut : on les suit le niveau.
                        if (hitPoints == GameState.TablePlayer.estimatedHitPoints(level)) {
                            hitPoints = GameState.TablePlayer.estimatedHitPoints(newLevel)
                        }
                        level = newLevel
                    }
                    NumberStepper("CA", armorClass, 1..30) { armorClass = it }
                    NumberStepper("PV max", hitPoints, 1..400) { hitPoints = it }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val updated = player.copy(name = name.trim(), level = level, armorClass = armorClass, maxHitPoints = hitPoints)
                        onPlayersChange(players.map { if (it.id == player.id) updated else it })
                        editing = null
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Annuler") } }
        )
    }
}

/** Sélecteur numérique compact (− valeur +), utilisé pour le nombre et le niveau des joueurs. */
@Composable
internal fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onValueChange((value - 1).coerceIn(range)) }, enabled = value > range.first) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuer")
        }
        Text("$value", fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onValueChange((value + 1).coerceIn(range)) }, enabled = value < range.last) {
            Icon(Icons.Default.Add, contentDescription = "Augmenter")
        }
    }
}

// --- Attitude des PNJ envers le groupe (réglée dans l'onglet PNJ de leur fiche) ---

@Composable
private fun PnjAttitudesSubsection(group: GameState.MjGroup, availableCharacters: List<Character>) {
    val attitudes = availableCharacters
        .filter { it.type == "PNJ" && group.id in it.groupReputations }
        .sortedByDescending { it.groupReputations[group.id] }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubsectionTitle("Attitude des PNJ envers le groupe")
        if (attitudes.isEmpty()) {
            Text(
                "Aucun PNJ n'a d'avis sur ce groupe (à régler dans l'onglet PNJ de leur fiche).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
        attitudes.forEach { pnj ->
            val score = pnj.groupReputations[group.id] ?: 0
            Text("${pnj.name} — ${ReputationScale.labelFor(score)} ($score)", color = Color.White)
        }
    }
}

// --- Calculateur de rencontre ---

@Composable
private fun EncounterCalculatorSubsection(memberLevels: List<Int>) {
    var selectedDifficulty by remember { mutableStateOf(EncounterDifficulty.MOYENNE) }
    val suggestBalancedEncounter = remember { SuggestBalancedEncounterUseCase() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubsectionTitle("Rencontre équilibrée")
        if (memberLevels.isEmpty()) {
            Text(
                "Ajoutez des PJ/PNJ ou des joueurs sans fiche ci-dessus pour calculer une rencontre.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            return
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EncounterDifficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    selected = selectedDifficulty == difficulty,
                    onClick = { selectedDifficulty = difficulty },
                    shape = SegmentedButtonDefaults.itemShape(index, EncounterDifficulty.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        activeBorderColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text(difficulty.label) }
            }
        }

        val suggestion = suggestBalancedEncounter(memberLevels, selectedDifficulty)
        Text(
            "Budget d'XP recommandé : ${suggestion.totalXpBudget}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Nombre de monstres conseillé : ${suggestion.recommendedMonsterCount.first} " +
                    "à ${suggestion.recommendedMonsterCount.last}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}