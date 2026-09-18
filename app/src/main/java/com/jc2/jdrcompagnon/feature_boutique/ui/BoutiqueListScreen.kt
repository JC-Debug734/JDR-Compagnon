package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueListUiState
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueViewModel

/**
 * Vue "idiote" : observe l'état du ViewModel, route les événements, ne décide de rien.
 * Style à harmoniser avec les écrans existants (SheetTheme / ForcedDarkPalette) —
 * voir le TODO sur BoutiqueCard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutiqueListScreen(
    viewModel: BoutiqueViewModel,
    onBoutiqueClick: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var afficherDialogCreation by remember { mutableStateOf(false) }
    var boutiqueAModifier by remember { mutableStateOf<Boutique?>(null) }
    var boutiqueASupprimer by remember { mutableStateOf<Boutique?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BOUTIQUES") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { afficherDialogCreation = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvelle boutique") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is BoutiqueListUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is BoutiqueListUiState.Error -> Text(
                    text = "Erreur : ${state.message}",
                    modifier = Modifier.align(Alignment.Center)
                )
                is BoutiqueListUiState.Success -> BoutiqueListContent(
                    boutiques = state.boutiques,
                    onBoutiqueClick = onBoutiqueClick,
                    onModifierClick = { boutiqueAModifier = it },
                    onSupprimerClick = { boutiqueASupprimer = it }
                )
            }
        }
    }

    if (afficherDialogCreation) {
        BoutiqueFormDialog(
            onGenererNomBoutique = viewModel::genererNomBoutiqueAleatoire,
            onGenererNomMarchand = viewModel::genererNomMarchandAleatoire,
            onDismiss = { afficherDialogCreation = false },
            onConfirmer = { nom, nomMarchand, standing, type ->
                viewModel.onCreerBoutique(nom, nomMarchand, standing, type)
                afficherDialogCreation = false
            }
        )
    }

    boutiqueAModifier?.let { boutique ->
        BoutiqueFormDialog(
            boutiqueExistante = boutique,
            onGenererNomBoutique = viewModel::genererNomBoutiqueAleatoire,
            onGenererNomMarchand = viewModel::genererNomMarchandAleatoire,
            onDismiss = { boutiqueAModifier = null },
            onConfirmer = { nom, nomMarchand, standing, type ->
                viewModel.onModifierBoutique(boutique, nom, nomMarchand, standing, type)
                boutiqueAModifier = null
            }
        )
    }

    boutiqueASupprimer?.let { boutique ->
        AlertDialog(
            onDismissRequest = { boutiqueASupprimer = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer la boutique « ${boutique.nom} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onSupprimerBoutique(boutique.id)
                    boutiqueASupprimer = null
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { boutiqueASupprimer = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun BoutiqueListContent(
    boutiques: List<Boutique>,
    onBoutiqueClick: (String) -> Unit,
    onModifierClick: (Boutique) -> Unit,
    onSupprimerClick: (Boutique) -> Unit
) {
    if (boutiques.isEmpty()) {
        Text(
            text = "Aucune boutique pour l'instant. Créez-en une avec le bouton +.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(boutiques, key = { it.id }) { boutique ->
            BoutiqueCard(
                boutique = boutique,
                onClick = { onBoutiqueClick(boutique.id) },
                onModifierClick = { onModifierClick(boutique) },
                onSupprimerClick = { onSupprimerClick(boutique) }
            )
        }
    }
}

@Composable
private fun BoutiqueCard(
    boutique: Boutique,
    onClick: () -> Unit,
    onModifierClick: () -> Unit,
    onSupprimerClick: () -> Unit
) {
    // TODO : reprendre le style de carte existant du projet (SheetSurface, bordures dorées, etc.)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(Modifier.clickable(onClick = onClick))
        ) {
            Text(text = boutique.nom)
            Text(text = "${boutique.type.label} · ${boutique.standing.label} · ${boutique.marchand.nom}")
            Text(
                text = if (boutique.type == TypeBoutique.MARCHAND)
                    "${boutique.inventaire.size} articles · ${boutique.employes.size} employés · ${boutique.argentDisponibleEnPo} po"
                else
                    "${boutique.services.size} services · ${boutique.employes.size} employés · ${boutique.argentDisponibleEnPo} po"
            )
        }
        IconButton(onClick = onModifierClick) {
            Icon(Icons.Default.Edit, contentDescription = "Modifier ${boutique.nom}")
        }
        IconButton(onClick = onSupprimerClick) {
            Icon(Icons.Default.Delete, contentDescription = "Supprimer ${boutique.nom}")
        }
    }
}