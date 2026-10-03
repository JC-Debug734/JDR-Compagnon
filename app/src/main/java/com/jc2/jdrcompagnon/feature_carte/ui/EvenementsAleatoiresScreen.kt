package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.di.EvenementsViewModelFactory
import com.jc2.jdrcompagnon.feature_carte.presentation.EvenementsViewModel
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementCard
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementEditorDialog
import com.jc2.jdrcompagnon.feature_evenement.ui.EventResultDialog
import com.jc2.jdrcompagnon.feature_evenement.ui.couleur
import com.jc2.jdrcompagnon.feature_evenement.ui.icone
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Événements de campagne : ceux de la bibliothèque propres à cette campagne (créables ici), puis
 * les événements communs du monde. Le tirage (bouton en bas) pioche dans les deux, filtré par le
 * type sélectionné. Tap sur une carte = modifier (dans la bibliothèque, donc partout).
 */
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
    val context = LocalContext.current
    LaunchedEffect(viewModel.worldId) { EvenementDependencies.seedExamplesIfNeeded(context, viewModel.worldId) }

    var filtreType by rememberSaveable { mutableStateOf<TypeEvenement?>(null) }
    var creation by remember { mutableStateOf(false) }
    var enEdition by remember { mutableStateOf<Evenement?>(null) }
    var aSupprimer by remember { mutableStateOf<Evenement?>(null) }

    val propres = uiState.propres.filter { filtreType == null || it.type == filtreType }
    val communs = uiState.communs.filter { filtreType == null || it.type == filtreType }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("ÉVÉNEMENTS DE CAMPAGNE") },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onTirerEvenement(filtreType) },
                icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                text = { Text(filtreType?.let { "Tirer : ${it.label}" } ?: "Tirer un événement") },
                containerColor = ForcedDarkPalette.AccentGold,
                contentColor = ForcedDarkPalette.Background
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = filtreType == null, onClick = { filtreType = null }, label = { Text("Tous") })
                    TypeEvenement.entries.forEach { type ->
                        FilterChip(
                            selected = filtreType == type,
                            onClick = { filtreType = if (filtreType == type) null else type },
                            label = { Text(type.label) },
                            leadingIcon = { Icon(type.icone, contentDescription = null, tint = type.couleur, modifier = Modifier.size(18.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = type.couleur.copy(alpha = 0.3f))
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Propres à la campagne (${propres.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { creation = true }) { Icon(Icons.Default.Add, contentDescription = "Nouvel événement de campagne") }
                }
            }
            if (propres.isEmpty()) {
                item { Text("Aucun événement propre à cette campagne.", color = Color.White.copy(alpha = 0.7f)) }
            }
            items(propres, key = { it.id }) { evenement ->
                EvenementCard(
                    evenement = evenement,
                    campagneNom = null,
                    onClick = { enEdition = evenement },
                    onDelete = { aSupprimer = evenement }
                )
            }
            item {
                Text(
                    "Communs au monde (${communs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            items(communs, key = { it.id }) { evenement ->
                // Suppression réservée à la bibliothèque : un événement commun sert ailleurs.
                EvenementCard(evenement = evenement, campagneNom = null, onClick = { enEdition = evenement }, onDelete = null)
            }
        }
    }

    if (creation || enEdition != null) {
        EvenementEditorDialog(
            initial = enEdition,
            worldId = viewModel.worldId,
            typeParDefaut = filtreType ?: TypeEvenement.RENCONTRE,
            campagneParDefaut = campagneId,
            onDismiss = { creation = false; enEdition = null },
            onSave = { evenement ->
                viewModel.onSauvegarder(evenement)
                creation = false
                enEdition = null
            }
        )
    }

    aSupprimer?.let { evenement ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer l'événement « ${evenement.titre} » de la bibliothèque ?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onSupprimer(evenement.id)
                    aSupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } }
        )
    }

    dernierTirage?.let { evenement ->
        EventResultDialog(evenement = evenement, worldId = viewModel.worldId, onDismiss = { viewModel.clearDernierTirage() })
    }
}
