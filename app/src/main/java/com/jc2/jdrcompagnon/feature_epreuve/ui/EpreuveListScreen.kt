package com.jc2.jdrcompagnon.feature_epreuve.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EpreuveDependencies
import com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveImageStore
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveListViewModel
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Outil ÉPREUVES : liste des épreuves environnementales du monde courant. Toucher une carte la
 * modifie, le bouton « Lancer » démarre sa résolution (EpreuveResolutionScreen). Une épreuve en
 * cours est rappelée en haut de la liste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpreuveListScreen(
    viewModel: EpreuveListViewModel,
    worldId: String,
    onOuvrirResolution: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    val epreuves by remember(worldId) { viewModel.observerEpreuves(worldId) }.collectAsState(initial = emptyList())
    val enCours by EpreuveOutilSession.etat.collectAsState()

    LaunchedEffect(worldId) { EpreuveDependencies.seedExamplesIfNeeded(context, worldId) }

    // null = éditeur fermé ; EditeurEpreuve(null) = création.
    var edition by remember { mutableStateOf<EditeurEpreuve?>(null) }
    var menuPourId by remember { mutableStateOf<String?>(null) }
    var aSupprimer by remember { mutableStateOf<Epreuve?>(null) }
    var aLancer by remember { mutableStateOf<Epreuve?>(null) }

    fun lancer(epreuve: Epreuve) {
        val actuelle = EpreuveOutilSession.etat.value
        if (actuelle != null && !actuelle.terminee && actuelle.epreuve.id != epreuve.id) {
            aLancer = epreuve
        } else {
            // Même épreuve encore en cours : on la reprend au lieu de remettre le compteur à zéro.
            val reprise = actuelle != null && !actuelle.terminee && actuelle.epreuve.id == epreuve.id
            if (!reprise) EpreuveOutilSession.demarrer(epreuve)
            onOuvrirResolution()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Épreuves", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { edition = EditeurEpreuve(null) },
                containerColor = ForcedDarkPalette.AccentGold,
                contentColor = ForcedDarkPalette.Background,
            ) { Icon(Icons.Default.Add, contentDescription = "Nouvelle épreuve") }
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 170.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            enCours?.let { actif ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onOuvrirResolution),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
                        border = BorderStroke(2.dp, ForcedDarkPalette.AccentGold),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (actif.terminee) "ÉPREUVE TERMINÉE" else "ÉPREUVE EN COURS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = ForcedDarkPalette.AccentGold,
                                )
                                Text(actif.epreuve.nom, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "${actif.reussites} / ${actif.epreuve.reussitesRequises} réussites · ${actif.echecs} échec(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                )
                            }
                            TextButton(onClick = onOuvrirResolution) { Text("Reprendre") }
                        }
                    }
                }
            }
            if (epreuves.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EtatVide() }
            } else {
                items(epreuves, key = { it.id }) { epreuve ->
                    EpreuveCarte(
                        epreuve = epreuve,
                        active = enCours?.epreuve?.id == epreuve.id && enCours?.terminee == false,
                        menuOuvert = menuPourId == epreuve.id,
                        onClick = { edition = EditeurEpreuve(epreuve) },
                        onLancer = { lancer(epreuve) },
                        onMenu = { menuPourId = epreuve.id },
                        onFermerMenu = { menuPourId = null },
                        onSupprimer = { aSupprimer = epreuve },
                    )
                }
            }
        }
    }

    edition?.let { (initiale) ->
        EpreuveEditorDialog(
            initiale = initiale,
            worldId = worldId,
            onSave = { epreuve ->
                viewModel.sauvegarder(epreuve)
                edition = null
            },
            onDismiss = { edition = null },
        )
    }

    aSupprimer?.let { epreuve ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer l'épreuve « ${epreuve.nom} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    // L'image reste utilisée tant que l'épreuve est affichée sur la table.
                    val affichee = EpreuveOutilSession.etat.value?.epreuve?.id == epreuve.id
                    if (affichee) EpreuveOutilSession.fermer()
                    viewModel.supprimer(epreuve) { EpreuveImageStore.supprimer(context, epreuve.imageFileName) }
                    aSupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } },
        )
    }

    aLancer?.let { epreuve ->
        AlertDialog(
            onDismissRequest = { aLancer = null },
            title = { Text("Épreuve déjà en cours") },
            text = { Text("« ${enCours?.epreuve?.nom} » n'est pas terminée. La remplacer par « ${epreuve.nom} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    EpreuveOutilSession.demarrer(epreuve)
                    aLancer = null
                    onOuvrirResolution()
                }) { Text("Remplacer") }
            },
            dismissButton = { TextButton(onClick = { aLancer = null }) { Text("Annuler") } },
        )
    }
}

/** Éditeur ouvert : [epreuve] null = création. */
private data class EditeurEpreuve(val epreuve: Epreuve?)

@Composable
private fun EpreuveCarte(
    epreuve: Epreuve,
    active: Boolean,
    menuOuvert: Boolean,
    onClick: () -> Unit,
    onLancer: () -> Unit,
    onMenu: () -> Unit,
    onFermerMenu: () -> Unit,
    onSupprimer: () -> Unit,
) {
    val vignette = rememberImageEpreuve(epreuve.imageFileName, echantillonnage = 4)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        border = if (active) BorderStroke(2.dp, ForcedDarkPalette.AccentGold) else null,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(90.dp).background(ForcedDarkPalette.Indicator)) {
            if (vignette != null) {
                Image(
                    bitmap = vignette,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Default.Hiking,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp).align(Alignment.Center),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = onFermerMenu) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { onFermerMenu(); onClick() },
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { onFermerMenu(); onSupprimer() },
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                epreuve.nom,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${epreuve.reussitesRequises} réussite(s) · ${epreuve.complications.size} complication(s)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onLancer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForcedDarkPalette.AccentGold, contentColor = ForcedDarkPalette.Background),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (active) "Reprendre" else "Lancer")
            }
        }
    }
}

@Composable
private fun EtatVide() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Hiking, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.5f))
        Text("Aucune épreuve pour ce monde", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text("Appuyez sur + pour en créer une.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
    }
}
