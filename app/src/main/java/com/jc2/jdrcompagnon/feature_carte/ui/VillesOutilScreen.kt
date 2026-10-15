package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.VillesOutilViewModelFactory
import com.jc2.jdrcompagnon.feature_carte.data.VilleEmblemeStore
import com.jc2.jdrcompagnon.feature_carte.data.VilleImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.presentation.VilleOutil
import com.jc2.jdrcompagnon.feature_carte.presentation.VillesOutilViewModel
import com.jc2.jdrcompagnon.ui.components.VignetteImage
import com.jc2.jdrcompagnon.ui.components.forgetCollapsibleSections
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Outil VILLES : bibliothèque de villes réutilisables d'une campagne à l'autre (une campagne les
 * ajoute depuis son éditeur, carte Villes), et villes déjà rattachées aux campagnes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillesOutilScreen(
    onOpenVille: (campagneId: String, villeId: String) -> Unit,
    onOpenMenu: () -> Unit = {},
    viewModel: VillesOutilViewModel = viewModel(factory = VillesOutilViewModelFactory()),
) {
    val context = LocalContext.current
    val villes by viewModel.villes.collectAsStateWithLifecycle()
    var afficherCreation by remember { mutableStateOf(false) }
    var villeASupprimer by remember { mutableStateOf<PointInteret?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("VILLES", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                actions = {
                    IconButton(onClick = { afficherCreation = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nouvelle ville", tint = ForcedDarkPalette.AccentGold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (villes.isEmpty()) {
                Text(
                    "Aucune ville pour l'instant. Créez-en une avec le bouton + : elle pourra ensuite être ajoutée à une campagne.",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(villes, key = { it.ville.id }) { item ->
                        VilleOutilCard(
                            item = item,
                            onClick = { onOpenVille(item.ville.campagneId, item.ville.id) },
                            onSupprimer = { villeASupprimer = item.ville }
                        )
                    }
                }
            }
        }
    }

    if (afficherCreation) {
        var nom by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { afficherCreation = false },
            title = { Text("Nouvelle ville") },
            text = {
                Column {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onCreerVille(nom, description); afficherCreation = false },
                    enabled = nom.isNotBlank()
                ) { Text("Créer") }
            },
            dismissButton = { TextButton(onClick = { afficherCreation = false }) { Text("Annuler") } }
        )
    }

    villeASupprimer?.let { ville ->
        AlertDialog(
            onDismissRequest = { villeASupprimer = null },
            title = { Text("Confirmer la suppression") },
            text = {
                Text(
                    "Supprimer la ville « ${ville.nom} » ?" +
                        if (ville.campagneId != PointInteret.SANS_CAMPAGNE) " Elle sera retirée de sa campagne et de ses cartes." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onSupprimerVille(ville.id)
                    VilleEmblemeStore.supprimer(context, ville.embleme)
                    VilleImageStore.supprimer(context, ville.id)
                    forgetCollapsibleSections(context, "ville:${ville.id}")
                    villeASupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { villeASupprimer = null }) { Text("Annuler") } }
        )
    }
}

/** Carte d'une ville : même fond translucide arrondi que le reste de l'app, texte blanc. */
@Composable
private fun VilleOutilCard(item: VilleOutil, onClick: () -> Unit, onSupprimer: () -> Unit) {
    val ville = item.ville
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
        ) {
            EmblemeVille(ville, Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ville.nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    item.nomCampagne?.let { "Campagne : $it" } ?: "Bibliothèque (aucune campagne)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${ville.boutiqueIds.size} boutiques · ${ville.scenarioIds.size} scénarios",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer ${ville.nom}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Emblème de la ville, ou l'icône de ville par défaut quand elle n'en a pas. */
@Composable
fun EmblemeVille(ville: PointInteret, modifier: Modifier = Modifier) {
    val embleme = ville.embleme
    if (embleme != null && (VilleEmblemeStore.predefinie(embleme) != null ||
            VilleEmblemeStore.fichier(LocalContext.current, embleme)?.exists() == true)
    ) {
        VignetteImage(VilleEmblemeStore, embleme, modifier, contentDescription = ville.nom)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocationCity, contentDescription = null, tint = ForcedDarkPalette.AccentGold)
        }
    }
}
