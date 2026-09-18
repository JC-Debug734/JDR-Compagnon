package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.EvenementsViewModelFactory
import com.jc2.jdrcompagnon.feature_carte.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire
import com.jc2.jdrcompagnon.feature_carte.presentation.EvenementsViewModel
import com.jc2.jdrcompagnon.ui.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvenementsAleatoiresScreen(
    campagneId: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    viewModel: EvenementsViewModel = viewModel(factory = EvenementsViewModelFactory(campagneId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dernierTirage by viewModel.dernierTirage.collectAsStateWithLifecycle()
    var afficherAjout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ÉVÉNEMENTS ALÉATOIRES") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onTirerEvenement() },
                icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                text = { Text("Tirer un événement") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Personnalisés", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { afficherAjout = true }) { Icon(Icons.Default.Add, contentDescription = "Ajouter un événement") }
                }
            }
            if (uiState.personnalises.isEmpty()) {
                item { Text("Aucun événement personnalisé", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(uiState.personnalises, key = { it.id }) { evenement ->
                EvenementCard(evenement, onSupprimer = { viewModel.onSupprimerEvenement(evenement.id) })
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { Text("Génériques", style = MaterialTheme.typography.titleMedium) }
            items(uiState.generiques, key = { it.id }) { evenement ->
                EvenementCard(evenement, onSupprimer = null)
            }
        }
    }

    if (afficherAjout) {
        AjouterEvenementDialog(
            onDismiss = { afficherAjout = false },
            onConfirmer = { titre, description, effets ->
                viewModel.onAjouterEvenement(titre, description, effets)
                afficherAjout = false
            }
        )
    }

    dernierTirage?.let { evenement ->
        ResultatTirageDialog(
            evenement = evenement,
            groupes = uiState.groupes,
            onAppliquerReputation = { groupeId, factionNom, delta -> viewModel.onAppliquerReputation(groupeId, factionNom, delta) },
            onDismiss = { viewModel.clearDernierTirage() }
        )
    }
}

@Composable
internal fun EvenementCard(evenement: EvenementAleatoire, onSupprimer: (() -> Unit)?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(evenement.titre, style = MaterialTheme.typography.titleSmall)
                Text(evenement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                evenement.effets.forEach { effet ->
                    Text(
                        text = when (effet) {
                            is EffetEvenement.GainReputation -> "Réputation : ${effet.factionNom} ${if (effet.delta >= 0) "+" else ""}${effet.delta}"
                            is EffetEvenement.Information -> "Info : ${effet.texte}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (onSupprimer != null) {
                IconButton(onClick = onSupprimer) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
            }
        }
    }
}

@Composable
internal fun AjouterEvenementDialog(
    onDismiss: () -> Unit,
    onConfirmer: (titre: String, description: String, effets: List<EffetEvenement>) -> Unit
) {
    var titre by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var factionNom by remember { mutableStateOf("") }
    var deltaTexte by remember { mutableStateOf("") }
    var infoTexte by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel événement") },
        text = {
            Column {
                OutlinedTextField(value = titre, onValueChange = { titre = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Effet réputation (optionnel)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = factionNom, onValueChange = { factionNom = it }, label = { Text("Faction") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = deltaTexte,
                    onValueChange = { deltaTexte = it.filter { c -> c.isDigit() || c == '-' } },
                    label = { Text("Variation (ex: 5 ou -5)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Effet info (optionnel)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = infoTexte, onValueChange = { infoTexte = it }, label = { Text("Texte") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val effets = buildList {
                        val delta = deltaTexte.toIntOrNull()
                        if (factionNom.isNotBlank() && delta != null) add(EffetEvenement.GainReputation(factionNom, delta))
                        if (infoTexte.isNotBlank()) add(EffetEvenement.Information(infoTexte))
                    }
                    onConfirmer(titre, description, effets)
                },
                enabled = titre.isNotBlank()
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
internal fun ResultatTirageDialog(
    evenement: EvenementAleatoire,
    groupes: List<GameState.MjGroup>,
    onAppliquerReputation: (groupeId: String, factionNom: String, delta: Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(evenement.titre) },
        text = {
            Column {
                Text(evenement.description)
                evenement.effets.forEach { effet ->
                    Spacer(Modifier.height(8.dp))
                    when (effet) {
                        is EffetEvenement.Information -> Text("Info : ${effet.texte}")
                        is EffetEvenement.GainReputation -> {
                            Text("Réputation : ${effet.factionNom} ${if (effet.delta >= 0) "+" else ""}${effet.delta}")
                            if (groupes.isEmpty()) {
                                Text("Aucun groupe créé pour appliquer cet effet.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                var menuOuvert by remember { mutableStateOf(false) }
                                androidx.compose.foundation.layout.Box {
                                    TextButton(onClick = { menuOuvert = true }) { Text("Appliquer à un groupe") }
                                    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                                        groupes.forEach { groupe ->
                                            DropdownMenuItem(
                                                text = { Text(groupe.name) },
                                                onClick = {
                                                    menuOuvert = false
                                                    onAppliquerReputation(groupe.id, effet.factionNom, effet.delta)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
