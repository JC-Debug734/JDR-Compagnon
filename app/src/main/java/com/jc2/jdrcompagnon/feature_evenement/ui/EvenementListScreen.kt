package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.presentation.EvenementListViewModel
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Bibliothèque d'événements du monde courant : un événement y est écrit une fois, puis
 * référencé depuis les tables aléatoires, villes, lieux, environnements et scènes. Filtres par
 * type (pastilles colorées), par portée (communs / campagne en cours) et recherche texte.
 * Tap sur une carte = modifier, corbeille à droite = supprimer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvenementListScreen(
    viewModel: EvenementListViewModel,
    worldId: String,
    onOpenMenu: () -> Unit = {},
) {
    val evenements by viewModel.observerEvenements(worldId).collectAsState(initial = emptyList())
    val campagnes = GameState.mjCampaigns.collectAsState().value.filter { it.worldId == worldId }
    val campagneCouranteId by GameState.currentCampaignId.collectAsState()
    val campagneCourante = campagnes.firstOrNull { it.id == campagneCouranteId }

    val context = LocalContext.current
    LaunchedEffect(worldId) {
        EvenementDependencies.seedExamplesIfNeeded(context, worldId)
    }

    var recherche by rememberSaveable { mutableStateOf("") }
    var filtreType by rememberSaveable { mutableStateOf<TypeEvenement?>(null) }
    var filtreCampagne by rememberSaveable { mutableStateOf(false) }
    var enEdition by remember { mutableStateOf<Evenement?>(null) }
    var creation by remember { mutableStateOf(false) }
    var aSupprimer by remember { mutableStateOf<Evenement?>(null) }

    val filtres = evenements.filter { evt ->
        (filtreType == null || evt.type == filtreType) &&
            (!filtreCampagne || campagneCourante == null || evt.campagneId == null || evt.campagneId == campagneCourante.id) &&
            (recherche.isBlank() || evt.titre.contains(recherche, ignoreCase = true) || evt.description.contains(recherche, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Événements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { creation = true },
                containerColor = ForcedDarkPalette.AccentGold,
                contentColor = ForcedDarkPalette.Background
            ) { Icon(Icons.Default.Add, contentDescription = "Nouvel événement") }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    placeholder = { Text("Rechercher un événement") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filtreType == null,
                        onClick = { filtreType = null },
                        label = { Text("Tous (${evenements.size})") }
                    )
                    TypeEvenement.entries.forEach { type ->
                        FilterChip(
                            selected = filtreType == type,
                            onClick = { filtreType = if (filtreType == type) null else type },
                            label = { Text("${type.label} (${evenements.count { it.type == type }})") },
                            leadingIcon = { Icon(type.icone, contentDescription = null, tint = type.couleur, modifier = Modifier.size(18.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = type.couleur.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            if (campagneCourante != null) {
                item {
                    FilterChip(
                        selected = filtreCampagne,
                        onClick = { filtreCampagne = !filtreCampagne },
                        label = { Text("Communs + campagne « ${campagneCourante.title} »") }
                    )
                }
            }

            when {
                evenements.isEmpty() -> item { EtatVide("Aucun événement pour ce monde", "Appuyez sur + pour en créer un.") }
                filtres.isEmpty() -> item { EtatVide("Aucun événement ne correspond aux filtres.", null) }
                else -> items(filtres, key = { it.id }) { evt ->
                    EvenementCard(
                        evenement = evt,
                        campagneNom = evt.campagneId?.let { id -> campagnes.firstOrNull { it.id == id }?.title },
                        onClick = { enEdition = evt },
                        onDelete = { aSupprimer = evt }
                    )
                }
            }
        }
    }

    if (creation || enEdition != null) {
        EvenementEditorDialog(
            initial = enEdition,
            worldId = worldId,
            typeParDefaut = filtreType ?: TypeEvenement.RENCONTRE,
            onDismiss = { creation = false; enEdition = null },
            onSave = { evt ->
                viewModel.sauvegarder(evt)
                creation = false
                enEdition = null
            }
        )
    }

    aSupprimer?.let { evt ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer l'événement « ${evt.titre} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.supprimer(evt.id)
                    aSupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } }
        )
    }
}

/** Carte d'un événement : pastille du type, titre, type/portée/effets, début de description. */
@Composable
fun EvenementCard(
    evenement: Evenement,
    campagneNom: String?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TypeEvenementBadge(evenement.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    evenement.titre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val details = buildList {
                    add(evenement.type.label)
                    campagneNom?.let { add(it) }
                    if (evenement.effets.isNotEmpty()) add("${evenement.effets.size} effet(s)")
                    if (evenement.issues.isNotEmpty()) add("${evenement.issues.size} issue(s)")
                    if (evenement.profils.isNotEmpty()) add(evenement.profils.joinToString(", ") { it.libelle })
                }.joinToString(" · ")
                Text(details, style = MaterialTheme.typography.labelSmall, color = evenement.type.couleur)
                if (evenement.description.isNotBlank()) {
                    Text(
                        evenement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun EtatVide(titre: String, sousTitre: String?) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
            Text(titre, style = MaterialTheme.typography.titleMedium, color = Color.White)
            sousTitre?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f)) }
        }
    }
}
