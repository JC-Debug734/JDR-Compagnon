package com.jc2.jdrcompagnon.feature_faction.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.feature_faction.data.FactionsStore
import com.jc2.jdrcompagnon.feature_faction.domain.model.Faction
import com.jc2.jdrcompagnon.feature_faction.domain.model.TypeFaction
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

internal val FactionHint = Color.White.copy(alpha = 0.8f)

@Composable
internal fun chipCouleurs() = FilterChipDefaults.filterChipColors(
    labelColor = Color.White,
    selectedLabelColor = Color.Black,
    selectedContainerColor = ForcedDarkPalette.AccentGold,
)

/** Outil FACTIONS : factions du monde courant, recherche et filtre par type. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FactionsScreen(onBack: () -> Unit, onOpenFaction: (String) -> Unit) {
    val monde by GameState.currentWorld.collectAsState()
    val worldId = monde?.id ?: ""
    val toutes by FactionsStore.factions.collectAsState()
    val factions = toutes.filter { it.worldId == worldId }.sortedBy { it.nom.lowercase() }
    var recherche by rememberSaveable { mutableStateOf("") }
    var filtre by rememberSaveable { mutableStateOf<TypeFaction?>(null) }
    var creation by remember { mutableStateOf(false) }

    val q = recherche.trim().lowercase()
    val affichees = factions.filter { f ->
        (filtre == null || f.type == filtre) &&
            (q.isEmpty() || listOf(f.nom, f.devise, f.description, f.siege).any { it.lowercase().contains(q) } ||
                f.membres.any { it.nom.lowercase().contains(q) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Factions", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } },
                actions = { IconButton(onClick = { creation = true }) { Icon(Icons.Default.Add, contentDescription = "Nouvelle faction", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = recherche,
                onValueChange = { recherche = it },
                label = { Text("Rechercher (nom, devise, siège, membre)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val typesPresents = factions.map { it.type }.distinct()
            if (typesPresents.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filtre == null, onClick = { filtre = null }, label = { Text("Toutes") }, colors = chipCouleurs())
                    typesPresents.sortedBy { it.ordinal }.forEach { t ->
                        FilterChip(selected = filtre == t, onClick = { filtre = t }, label = { Text("${t.emoji} ${t.label}") }, colors = chipCouleurs())
                    }
                }
            }
            if (factions.isEmpty()) {
                Text(
                    "Aucune faction dans ce monde. Créez-en une : membres, intérêts, fêtes, alliances, projets en cours...",
                    color = FactionHint,
                )
                Button(onClick = { creation = true }, modifier = Modifier.fillMaxWidth()) { Text("Nouvelle faction") }
            }
            affichees.forEach { f -> CarteFaction(f) { onOpenFaction(f.id) } }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (creation) {
        NouvelleFactionDialog(
            onDismiss = { creation = false },
            onCreer = { nom, type ->
                val f = Faction(worldId = worldId, nom = nom, type = type)
                FactionsStore.ajouter(f)
                creation = false
                onOpenFaction(f.id)
            },
        )
    }
}

@Composable
private fun CarteFaction(f: Faction, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(f.type.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.nom, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (f.devise.isNotBlank()) {
                    Text("« ${f.devise} »", fontStyle = FontStyle.Italic, color = FactionHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val chef = f.membres.firstOrNull { it.chef }
                Text(
                    listOfNotNull(
                        f.type.label,
                        chef?.let { "chef : ${it.nom}" },
                        f.membres.size.takeIf { it > 0 }?.let { "$it membre(s)" },
                        f.projets.count { it.avancement < it.total }.takeIf { it > 0 }?.let { "$it projet(s) en cours" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionHint,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NouvelleFactionDialog(onDismiss: () -> Unit, onCreer: (String, TypeFaction) -> Unit) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeFaction.AUTRE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle faction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeFaction.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text("${t.emoji} ${t.label}") })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = nom.isNotBlank(), onClick = { onCreer(nom.trim(), type) }) { Text("Créer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
