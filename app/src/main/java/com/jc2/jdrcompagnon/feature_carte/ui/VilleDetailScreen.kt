package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import com.jc2.jdrcompagnon.di.VilleDetailViewModelFactory
import com.jc2.jdrcompagnon.feature_carte.presentation.VilleDetailUiState
import com.jc2.jdrcompagnon.feature_carte.presentation.VilleDetailViewModel
import com.jc2.jdrcompagnon.ui.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VilleDetailScreen(
    campagneId: String,
    villeId: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    viewModel: VilleDetailViewModel = viewModel(factory = VilleDetailViewModelFactory(campagneId, villeId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dernierTirage by viewModel.dernierTirage.collectAsStateWithLifecycle()
    val groupes by GameState.mjGroups.collectAsStateWithLifecycle()
    var afficherEdition by remember { mutableStateOf(false) }
    var afficherAjoutBoutique by remember { mutableStateOf(false) }
    var afficherAjoutScenario by remember { mutableStateOf(false) }
    var afficherAjoutLieu by remember { mutableStateOf(false) }
    var afficherAjoutEvenement by remember { mutableStateOf(false) }

    val titre = (uiState as? VilleDetailUiState.Success)?.ville?.nom?.uppercase() ?: "VILLE"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titre) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                actions = {
                    if (uiState is VilleDetailUiState.Success) {
                        IconButton(onClick = { afficherEdition = true }) { Icon(Icons.Default.Edit, contentDescription = "Modifier") }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        when (val state = uiState) {
            is VilleDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is VilleDetailUiState.Introuvable -> Box(Modifier.fillMaxSize().padding(padding)) {
                Text("Ville introuvable", modifier = Modifier.align(Alignment.Center))
            }
            is VilleDetailUiState.Success -> {
                val ville = state.ville
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    if (ville.description.isNotBlank()) {
                        Text(ville.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                    }

                    // --- Boutiques ---
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Boutiques", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { afficherAjoutBoutique = true }) { Icon(Icons.Default.Add, contentDescription = "Lier une boutique") }
                    }
                    val boutiquesLiees = state.toutesLesBoutiques.filter { it.id in ville.boutiqueIds }
                    if (boutiquesLiees.isEmpty()) {
                        Text("Aucune boutique liée", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    boutiquesLiees.forEach { boutique ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(boutique.nom, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.onDelierBoutique(ville, boutique) }) {
                                Icon(Icons.Default.Close, contentDescription = "Délier ${boutique.nom}")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    // --- Scénarios ---
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Scénarios attachés", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { afficherAjoutScenario = true }) { Icon(Icons.Default.Add, contentDescription = "Attacher un scénario") }
                    }
                    val scenariosAttaches = state.scenariosMonde.filter { it.id in ville.scenarioIds }
                    if (scenariosAttaches.isEmpty()) {
                        Text("Aucun scénario attaché", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    scenariosAttaches.forEach { scenario ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(scenario.title, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.onDetacherScenario(ville, scenario.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Détacher ${scenario.title}")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    // --- Lieux notables ---
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Lieux notables", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { afficherAjoutLieu = true }) { Icon(Icons.Default.Add, contentDescription = "Ajouter un lieu") }
                    }
                    if (state.lieuxNotables.isEmpty()) {
                        Text("Aucun lieu notable", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.lieuxNotables.forEach { lieu ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lieu.nom)
                                if (lieu.description.isNotBlank()) {
                                    Text(lieu.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.onSupprimerLieu(lieu.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer ${lieu.nom}")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    // --- Événements ---
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Événements de cette ville", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.onTirerEvenement() }) { Icon(Icons.Default.Casino, contentDescription = "Tirer un événement") }
                        IconButton(onClick = { afficherAjoutEvenement = true }) { Icon(Icons.Default.Add, contentDescription = "Ajouter un événement") }
                    }
                    if (state.evenements.isEmpty()) {
                        Text("Aucun événement personnalisé pour cette ville (le tirage pioche aussi dans les événements génériques)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.evenements.forEach { evenement ->
                        EvenementCard(evenement, onSupprimer = { viewModel.onSupprimerEvenement(evenement.id) })
                    }

                    Spacer(Modifier.height(32.dp))
                }

                if (afficherEdition) {
                    var nom by remember { mutableStateOf(ville.nom) }
                    var description by remember { mutableStateOf(ville.description) }
                    AlertDialog(
                        onDismissRequest = { afficherEdition = false },
                        title = { Text("Modifier la ville") },
                        text = {
                            Column {
                                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.onRenommer(ville, nom, description); afficherEdition = false },
                                enabled = nom.isNotBlank()
                            ) { Text("Enregistrer") }
                        },
                        dismissButton = { TextButton(onClick = { afficherEdition = false }) { Text("Annuler") } }
                    )
                }

                if (afficherAjoutBoutique) {
                    val nonLiees = state.toutesLesBoutiques.filter { it.villeId == null }
                    AlertDialog(
                        onDismissRequest = { afficherAjoutBoutique = false },
                        title = { Text("Lier une boutique") },
                        text = {
                            if (nonLiees.isEmpty()) {
                                Text("Aucune boutique disponible (déjà toutes liées à une ville, ou aucune boutique créée).")
                            } else {
                                Column {
                                    nonLiees.forEach { boutique ->
                                        TextButton(
                                            onClick = { viewModel.onLierBoutique(ville, boutique); afficherAjoutBoutique = false },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text(boutique.nom) }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = { afficherAjoutBoutique = false }) { Text("Fermer") } }
                    )
                }

                if (afficherAjoutScenario) {
                    val disponibles = state.scenariosMonde.filter { it.id !in ville.scenarioIds }
                    AlertDialog(
                        onDismissRequest = { afficherAjoutScenario = false },
                        title = { Text("Attacher un scénario") },
                        text = {
                            if (disponibles.isEmpty()) {
                                Text("Aucun scénario disponible dans ce monde.")
                            } else {
                                Column {
                                    disponibles.forEach { scenario ->
                                        TextButton(
                                            onClick = { viewModel.onAttacherScenario(ville, scenario.id); afficherAjoutScenario = false },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text(scenario.title) }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = { afficherAjoutScenario = false }) { Text("Fermer") } }
                    )
                }

                if (afficherAjoutLieu) {
                    var nom by remember { mutableStateOf("") }
                    var description by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { afficherAjoutLieu = false },
                        title = { Text("Nouveau lieu notable") },
                        text = {
                            Column {
                                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom (ex: Auberge du Dragon)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { viewModel.onAjouterLieu(nom, description); afficherAjoutLieu = false },
                                enabled = nom.isNotBlank()
                            ) { Text("Ajouter") }
                        },
                        dismissButton = { TextButton(onClick = { afficherAjoutLieu = false }) { Text("Annuler") } }
                    )
                }

                if (afficherAjoutEvenement) {
                    AjouterEvenementDialog(
                        onDismiss = { afficherAjoutEvenement = false },
                        onConfirmer = { titre2, description, effets ->
                            viewModel.onAjouterEvenement(titre2, description, effets)
                            afficherAjoutEvenement = false
                        }
                    )
                }

                dernierTirage?.let { evenement ->
                    ResultatTirageDialog(
                        evenement = evenement,
                        groupes = groupes,
                        onAppliquerReputation = { groupeId, factionNom, delta -> viewModel.onAppliquerReputation(groupeId, factionNom, delta) },
                        onDismiss = { viewModel.clearDernierTirage() }
                    )
                }
            }
        }
    }
}
