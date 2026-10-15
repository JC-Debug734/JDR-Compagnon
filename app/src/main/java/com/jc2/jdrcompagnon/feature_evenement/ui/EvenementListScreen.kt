package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
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
 * catégorie (menu déroulant) et recherche texte.
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

    val context = LocalContext.current
    LaunchedEffect(worldId) {
        EvenementDependencies.seedExamplesIfNeeded(context, worldId)
    }

    var recherche by rememberSaveable { mutableStateOf("") }
    var filtreType by rememberSaveable { mutableStateOf<TypeEvenement?>(null) }
    var enEdition by remember { mutableStateOf<Evenement?>(null) }
    var creation by remember { mutableStateOf(false) }
    var aSupprimer by remember { mutableStateOf<Evenement?>(null) }

    val filtres = evenements.filter { evt ->
        (filtreType == null || evt.type == filtreType) &&
            (recherche.isBlank() || evt.titre.contains(recherche, ignoreCase = true) || evt.description.contains(recherche, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Événements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                actions = {
                    IconButton(onClick = { creation = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nouvel événement", tint = ForcedDarkPalette.AccentGold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Recherche et filtres regroupés sur une carte translucide.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        contentColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = recherche,
                            onValueChange = { recherche = it },
                            placeholder = { Text("Rechercher un événement") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FiltreCategorie(
                            filtre = filtreType,
                            total = evenements.size,
                            nombre = { type -> evenements.count { it.type == type } },
                            onChoix = { filtreType = it }
                        )
                    }
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
                    ApercuDescriptionEvenement(evenement.description, lignes = 2, couleur = Color.White.copy(alpha = 0.8f))
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

/** Filtre de catégorie : menu déroulant (toutes, ou un type d'événement avec son nombre). */
@Composable
internal fun FiltreCategorie(
    filtre: TypeEvenement?,
    total: Int,
    nombre: (TypeEvenement) -> Int,
    onChoix: (TypeEvenement?) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { ouvert = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(
                filtre?.icone ?: Icons.Default.FilterList,
                contentDescription = null,
                tint = filtre?.couleur ?: Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "  Catégorie : " + (filtre?.let { "${it.label} (${nombre(it)})" } ?: "Toutes ($total)"),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            DropdownMenuItem(
                text = { Text("Toutes ($total)") },
                onClick = { onChoix(null); ouvert = false },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
            )
            TypeEvenement.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text("${type.label} (${nombre(type)})", fontWeight = if (filtre == type) FontWeight.Bold else null) },
                    onClick = { onChoix(type); ouvert = false },
                    leadingIcon = { Icon(type.icone, contentDescription = null, tint = type.couleur) }
                )
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
