package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EvenementDependencies
import kotlinx.coroutines.flow.first
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LinkTypes = listOf(
    "monster" to "Monstre",
    "pnj" to "PNJ",
    "npc" to "PNJ (alias)",
    "equipment" to "Équipement",
    "spell" to "Sort",
    "rule" to "Règle",
    "event" to "Discussion avec un PNJ",
    "epreuve" to "Épreuve environnementale",
    "evenement" to "Événement (bibliothèque)"
)

private val typeDisplayNames = mapOf(
    "monster" to "Monstre",
    "pnj" to "PNJ",
    "npc" to "PNJ",
    "equipment" to "Équipement",
    "spell" to "Sort",
    "rule" to "Règle",
    "event" to "Discussion avec un PNJ",
    "epreuve" to "Épreuve",
    "evenement" to "Événement"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityLinkPickerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelect: (type: String, name: String) -> Unit,
    initialType: String = "monster",
    // Monstres attachés à la campagne du scénario : listés en tête pour le type "Monstre".
    bestiaireCampagne: List<String> = emptyList(),
) {
    if (!visible) return
    val context = LocalContext.current
    val currentWorldId = GameState.currentWorldId()
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(initialType) {
        mutableStateOf(LinkTypes.find { it.first == initialType } ?: LinkTypes.first())
    }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selected, query, currentWorldId) {
        isLoading = true
        results = withContext(Dispatchers.IO) {
            val raw = when (selected.first) {
                "monster" -> SrdRepository.loadMonsters(context, currentWorldId).map { it.name }
                "equipment" -> SrdRepository.loadEquipmentList(context, currentWorldId).map { it.name }
                "spell" -> SrdRepository.loadSpells(context, currentWorldId).map { it.name }
                "rule" -> SrdRepository.loadRuleEntries(context, currentWorldId).map { it.name }
                "pnj", "npc" -> GameState.characters.value
                    .filter { it.type == "PNJ" || it.type == "Monstre" }
                    .map { it.name }
                "event" -> GameState.characters.value
                    .filter { it.type == "PNJ" }
                    .map { it.name }
                "evenement" -> currentWorldId?.let { monde ->
                    EvenementDependencies.seedSiNecessaire(context.applicationContext, monde)
                    EvenementDependencies.repository.observerEvenements(monde).first().map { it.titre }.distinct()
                }.orEmpty()
                "epreuve" -> com.jc2.jdrcompagnon.di.EpreuveDependencies.epreuvesDuMonde(currentWorldId).map { it.nom }.distinct()
                else -> emptyList()
            }
            val q = query.trim()
            if (q.isBlank()) {
                raw.take(50)
            } else {
                raw.filter { it.contains(q, ignoreCase = true) }.take(50)
            }
        }
        isLoading = false
    }

    val campagneFiltres = if (selected.first != "monster") emptyList() else
        bestiaireCampagne.filter { query.isBlank() || it.contains(query.trim(), ignoreCase = true) }
    val autresResultats = results.filterNot { nom -> campagneFiltres.any { it.equals(nom, ignoreCase = true) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insérer un lien interne", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selected.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        LinkTypes.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selected = type to (typeDisplayNames[type] ?: type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Rechercher") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (campagneFiltres.isEmpty() && autresResultats.isEmpty()) {
                    Text(
                        "Aucun résultat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        if (campagneFiltres.isNotEmpty()) {
                            item {
                                Text(
                                    "Bestiaire de la campagne",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(campagneFiltres) { name ->
                                TextButton(
                                    onClick = { onSelect(selected.first, name) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🐲 $name")
                                }
                            }
                            if (autresResultats.isNotEmpty()) {
                                item {
                                    Text(
                                        "Tout le bestiaire",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                        items(autresResultats) { name ->
                            TextButton(
                                onClick = { onSelect(selected.first, name) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
