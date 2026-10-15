package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Contenu détaillé d'un groupe d'aventuriers — tout ce qui le concerne, localisé : lieu du groupe
 * et trésor commun, répartition par lieu, membres (PJ, PNJ, créatures), montures et animaux,
 * véhicules, réputation par faction, inventaire commun et biens. Affiché sur son propre écran
 * (GroupDetailScreen). Chaque section se replie (flèche, nombre d'entrées en pastille), et cet
 * état est mémorisé pour chaque groupe.
 *
 * Les quêtes validées alimentent automatiquement trésor, réputation, montures, véhicules,
 * inventaire, biens et PNJ (voir ValiderQueteUseCase) ; tout reste modifiable à la main ici.
 * L'attitude d'un PNJ envers le groupe se règle sur sa fiche.
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
) {
    val members = availableCharacters.filter { it.id in group.memberIds }
    val cle = "groupe:${group.id}"

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CollapsibleSectionCard(cle, "Position et trésor") { GroupLocationSubsection(group) }
        CollapsibleSectionCard(cle, "Où se trouve quoi") { WhereIsWhatSubsection(group, members) }
        MemberCategory.entries.forEach { category ->
            val nombre = availableCharacters.count { it.type in category.types && it.id in group.memberIds }
            CollapsibleSectionCard(cle, category.title, count = nombre) {
                MembersSubsection(group, category, availableCharacters, onMemberToggle, onOpenCharacter)
            }
        }
        CollapsibleSectionCard(cle, "Montures et animaux", count = group.mounts.size) { MountsSubsection(group, members, onOpenBestiaryDetail) }
        CollapsibleSectionCard(cle, "Véhicules", count = group.transports.size) { TransportsSubsection(group, onOpenEquipmentDetail) }
        CollapsibleSectionCard(cle, "Réputation", count = group.reputations.size) { ReputationSubsection(group) }
        CollapsibleSectionCard(cle, "Inventaire du groupe", count = group.inventory.sumOf { it.quantity }) {
            InventorySubsection(group, members, onOpenEquipmentDetail)
        }
        CollapsibleSectionCard(cle, "Biens", count = group.assets.size) { AssetsSubsection(group) }
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
            Spacer(Modifier.weight(1f))
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

