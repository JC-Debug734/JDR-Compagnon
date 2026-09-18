package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_group.domain.model.Mount
import com.jc2.jdrcompagnon.feature_group.domain.model.Reputation
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.feature_group.domain.model.Transport
import com.jc2.jdrcompagnon.feature_group.domain.usecase.SuggestBalancedEncounterUseCase
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Carte "groupe d'aventuriers" — regroupe en un seul endroit : membres, montures, moyens de
 * transport, réputation par faction, et calculateur de rencontre équilibrée.
 *
 * Montures et moyens de transport se choisissent dans la bibliothèque SRD (bestiaire pour les
 * montures, équipement pour les véhicules) plutôt qu'en texte libre : la recherche reste
 * tolérante (aucun résultat trouvé n'empêche pas de valider un nom personnalisé), pour ne pas
 * bloquer un MJ qui invente sa propre monture/véhicule hors bestiaire.
 *
 * Vue "idiote" (Règle A) : toute mutation du groupe repasse par [onGroupChanged] /
 * [onMemberToggle], appelés depuis GroupsScreen qui les relaie à GameState.updateMjGroup.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(
    group: GameState.MjGroup,
    availableCharacters: List<Character>,
    worldId: String?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onMemberToggle: (String) -> Unit,
    onGroupChanged: (GameState.MjGroup) -> Unit,
    onOpenBestiaryDetail: (String) -> Unit,
    onOpenEquipmentDetail: (String) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val members = availableCharacters.filter { it.id in group.memberIds }
    val memberLevels = members.map { it.level }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onToggleExpand, onLongClick = onLongClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${members.size} membre(s)" +
                                if (group.mounts.isNotEmpty() || group.transports.isNotEmpty())
                                    " · ${group.mounts.size} monture(s) · ${group.transports.size} transport(s)"
                                else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Développer"
                )
            }

            if (expanded) {
                HorizontalDivider()
                MembersSubsection(group, availableCharacters, onMemberToggle)
                HorizontalDivider()
                MountsSubsection(group, worldId, onGroupChanged, onOpenBestiaryDetail)
                HorizontalDivider()
                TransportsSubsection(group, worldId, onGroupChanged, onOpenEquipmentDetail)
                HorizontalDivider()
                ReputationSubsection(group, onGroupChanged)
                HorizontalDivider()
                EncounterCalculatorSubsection(memberLevels)
            }
        }
    }
}

@Composable
private fun SubsectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
}

// --- Membres (PJ/PNJ) ---

@Composable
private fun MembersSubsection(
    group: GameState.MjGroup,
    availableCharacters: List<Character>,
    onMemberToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubsectionTitle("Membres (PJ / PNJ)")
        if (availableCharacters.isEmpty()) {
            Text(
                "Aucun PJ ou PNJ créé pour ce monde.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        availableCharacters.forEach { character ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = character.id in group.memberIds,
                    onCheckedChange = { onMemberToggle(character.id) }
                )
                Text(
                    "${character.name} — ${character.characterClass} niv.${character.level} (${character.type})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Champ "nom personnalisé" + recherche dans la bibliothèque, avec résultats affichés juste en
 * dessous. Composant partagé par montures (bestiaire) et transport (équipement) : seule la
 * fonction de recherche [search] change entre les deux usages.
 */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    results: List<String>,
    onResultPicked: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        results.forEach { result ->
            Text(
                text = result,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResultPicked(result) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

// --- Montures (recherche dans le bestiaire SRD) ---

@Composable
private fun MountsSubsection(
    group: GameState.MjGroup,
    worldId: String?,
    onGroupChanged: (GameState.MjGroup) -> Unit,
    onOpenBestiaryDetail: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember(group.id) { mutableStateOf("") }
    var speciesQuery by remember(group.id) { mutableStateOf("") }
    var speciesResults by remember(group.id) { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(speciesQuery, worldId) {
        speciesResults = if (speciesQuery.length < 2) {
            emptyList()
        } else {
            SrdRepository.searchMonsters(context, speciesQuery, worldId).map { it.name }.take(6)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubsectionTitle("Montures")
        group.mounts.forEach { mount ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = mount.species.isNotBlank()) { onOpenBestiaryDetail(mount.species) }
                ) {
                    Text(mount.name)
                    if (mount.species.isNotBlank()) {
                        Text(
                            mount.species,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = { onGroupChanged(group.copy(mounts = group.mounts - mount)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer")
                }
            }
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nom donné à la monture (ex: Bucéphale)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        LibrarySearchField(
            query = speciesQuery,
            onQueryChange = { speciesQuery = it },
            label = "Rechercher une espèce dans le bestiaire (ex: cheval)",
            results = speciesResults,
            onResultPicked = { picked -> speciesQuery = picked; speciesResults = emptyList() }
        )
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                onGroupChanged(group.copy(mounts = group.mounts + Mount(name = name, species = speciesQuery)))
                name = ""; speciesQuery = ""; speciesResults = emptyList()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ajouter") }
    }
}

// --- Moyens de transport (recherche dans l'équipement SRD) ---

@Composable
private fun TransportsSubsection(
    group: GameState.MjGroup,
    worldId: String?,
    onGroupChanged: (GameState.MjGroup) -> Unit,
    onOpenEquipmentDetail: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember(group.id) { mutableStateOf("") }
    var typeQuery by remember(group.id) { mutableStateOf("") }
    var typeResults by remember(group.id) { mutableStateOf<List<String>>(emptyList()) }
    var capacity by remember(group.id) { mutableStateOf("") }

    LaunchedEffect(typeQuery, worldId) {
        typeResults = if (typeQuery.length < 2) {
            emptyList()
        } else {
            SrdRepository.loadEquipmentList(context, worldId)
                .filter { it.name.contains(typeQuery, ignoreCase = true) }
                .map { it.name }
                .take(6)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubsectionTitle("Moyens de transport")
        group.transports.forEach { transport ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = transport.type.isNotBlank()) { onOpenEquipmentDetail(transport.type) }
                ) {
                    Text(transport.name)
                    Text(
                        "${transport.type} (capacité ${transport.capacity})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onGroupChanged(group.copy(transports = group.transports - transport)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer")
                }
            }
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nom donné au véhicule (ex: La Mouette)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        LibrarySearchField(
            query = typeQuery,
            onQueryChange = { typeQuery = it },
            label = "Rechercher un véhicule dans l'équipement (ex: chariot)",
            results = typeResults,
            onResultPicked = { picked -> typeQuery = picked; typeResults = emptyList() }
        )
        OutlinedTextField(
            value = capacity, onValueChange = { capacity = it.filter(Char::isDigit) },
            label = { Text("Capacité") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                onGroupChanged(
                    group.copy(
                        transports = group.transports + Transport(
                            name = name, type = typeQuery, capacity = capacity.toIntOrNull() ?: 0
                        )
                    )
                )
                name = ""; typeQuery = ""; typeResults = emptyList(); capacity = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ajouter") }
    }
}

// --- Réputation ---

@Composable
private fun ReputationSubsection(group: GameState.MjGroup, onGroupChanged: (GameState.MjGroup) -> Unit) {
    var newFactionName by remember(group.id) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubsectionTitle("Réputation")
        group.reputations.forEach { reputation ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${reputation.factionName} — ${ReputationScale.labelFor(reputation.score)} (${reputation.score})")
                Row {
                    IconButton(onClick = { onGroupChanged(adjustReputation(group, reputation, -5)) }) { Text("−5") }
                    IconButton(onClick = { onGroupChanged(adjustReputation(group, reputation, +5)) }) { Text("+5") }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newFactionName, onValueChange = { newFactionName = it },
                label = { Text("Nouvelle faction") }, modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = newFactionName.isNotBlank(),
                onClick = {
                    onGroupChanged(group.copy(reputations = group.reputations + Reputation(factionName = newFactionName)))
                    newFactionName = ""
                }
            ) { Text("Ajouter") }
        }
    }
}

private fun adjustReputation(group: GameState.MjGroup, reputation: Reputation, delta: Int): GameState.MjGroup {
    val updated = reputation.copy(score = (reputation.score + delta).coerceIn(-100, 100))
    return group.copy(reputations = group.reputations.map { if (it.factionId == reputation.factionId) updated else it })
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
                "Cochez des PJ/PNJ ci-dessus pour calculer une rencontre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EncounterDifficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    selected = selectedDifficulty == difficulty,
                    onClick = { selectedDifficulty = difficulty },
                    shape = SegmentedButtonDefaults.itemShape(index, EncounterDifficulty.entries.size)
                ) { Text(difficulty.label) }
            }
        }

        val suggestion = suggestBalancedEncounter(memberLevels, selectedDifficulty)
        Text(
            "Budget d'XP recommandé : ${suggestion.totalXpBudget}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Nombre de monstres conseillé : ${suggestion.recommendedMonsterCount.first} " +
                    "à ${suggestion.recommendedMonsterCount.last}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}