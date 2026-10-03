package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement

/**
 * Choix d'un ou plusieurs événements de la bibliothèque du monde [worldId], pour les rattacher à
 * une table, une ville, un lieu, un environnement ou une scène. [exclus] = ids déjà rattachés,
 * masqués de la liste. [campagneId] non null = seuls les événements communs et ceux de cette
 * campagne sont proposés.
 */
@Composable
fun EvenementPickerDialog(
    worldId: String,
    exclus: Set<String>,
    onDismiss: () -> Unit,
    onValider: (List<Evenement>) -> Unit,
    campagneId: String? = null,
) {
    val context = LocalContext.current
    LaunchedEffect(worldId) { EvenementDependencies.seedExamplesIfNeeded(context, worldId) }
    val flux = remember(worldId) { EvenementDependencies.repository.observerEvenements(worldId) }
    val evenements by flux.collectAsState(initial = emptyList())

    var recherche by remember { mutableStateOf("") }
    var filtreType by remember { mutableStateOf<TypeEvenement?>(null) }
    var selection by remember { mutableStateOf(setOf<String>()) }

    val disponibles = evenements.filter { evt ->
        evt.id !in exclus &&
            (campagneId == null || evt.campagneId == null || evt.campagneId == campagneId)
    }
    val filtres = disponibles.filter { evt ->
        (filtreType == null || evt.type == filtreType) &&
            (recherche.isBlank() || evt.titre.contains(recherche, ignoreCase = true) || evt.description.contains(recherche, ignoreCase = true))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter des événements") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    placeholder = { Text("Rechercher") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = filtreType == null, onClick = { filtreType = null }, label = { Text("Tous") })
                    TypeEvenement.entries.filter { type -> disponibles.any { it.type == type } }.forEach { type ->
                        FilterChip(
                            selected = filtreType == type,
                            onClick = { filtreType = if (filtreType == type) null else type },
                            label = { Text(type.label) },
                            leadingIcon = { Icon(type.icone, contentDescription = null, tint = type.couleur, modifier = Modifier.size(18.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = type.couleur.copy(alpha = 0.3f))
                        )
                    }
                }
                if (filtres.isEmpty()) {
                    Text(
                        if (disponibles.isEmpty()) "Aucun autre événement dans la bibliothèque. Créez-en un nouveau."
                        else "Aucun événement ne correspond.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtres, key = { it.id }) { evt ->
                        val coche = evt.id in selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selection = if (coche) selection - evt.id else selection + evt.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(checked = coche, onCheckedChange = null)
                            TypeEvenementBadge(evt.type, taille = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(evt.titre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (evt.description.isNotBlank()) {
                                    Text(
                                        evt.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selection.isNotEmpty(),
                onClick = { onValider(evenements.filter { it.id in selection }) }
            ) { Text(if (selection.isEmpty()) "Ajouter" else "Ajouter (${selection.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
