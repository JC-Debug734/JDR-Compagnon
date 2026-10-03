package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.di.VilleDetailViewModelFactory
import com.jc2.jdrcompagnon.feature_carte.data.VilleImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementsLiesSection
import com.jc2.jdrcompagnon.feature_carte.presentation.VilleDetailUiState
import com.jc2.jdrcompagnon.feature_carte.presentation.VilleDetailViewModel
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.rememberImageFichier
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Nom du lieu par lequel les joueurs arrivent toujours dans une ville (voir VisiteVille). */
const val ENTREE_DE_LA_VILLE = "Entrée de la ville"

/**
 * Détail d'une ville : image d'illustration, description, puis une carte par catégorie
 * (boutiques, lieux à visiter, scénarios, événements), chacune repliable et identifiée par son
 * icône. Une boutique se modifie depuis sa ligne ([onOpenBoutique], écran de la boutique).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VilleDetailScreen(
    campagneId: String,
    villeId: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    // Côté joueur : consultation seule (pas d'édition/liaison/suppression, réservé au MJ).
    readOnly: Boolean = false,
    onOpenBoutique: (boutiqueId: String) -> Unit = {},
    viewModel: VilleDetailViewModel = viewModel(factory = VilleDetailViewModelFactory(campagneId, villeId)),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var afficherEdition by remember { mutableStateOf(false) }
    var afficherAjoutBoutique by remember { mutableStateOf(false) }
    var afficherAjoutScenario by remember { mutableStateOf(false) }
    var afficherAjoutLieu by remember { mutableStateOf(false) }
    var lieuEdite by remember { mutableStateOf<LieuNotable?>(null) }
    // Change à chaque image ajoutée, retirée ou reçue du MJ : le fichier garde le même nom.
    val versionImage by VilleImageStore.version.collectAsState()
    val choisirImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) VilleImageStore.copier(context, uri, villeId)
    }

    val titre = (uiState as? VilleDetailUiState.Success)?.ville?.nom?.uppercase() ?: "VILLE"
    val ownerKey = "ville:$villeId"

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text(titre) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    actions = {
                        if (uiState is VilleDetailUiState.Success && !readOnly) {
                            IconButton(onClick = { afficherEdition = true }) { Icon(Icons.Default.Edit, contentDescription = "Modifier") }
                        }
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
        containerColor = Color.Transparent,
    ) { padding ->
        when (val state = uiState) {
            is VilleDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is VilleDetailUiState.Introuvable -> Box(Modifier.fillMaxSize().padding(padding)) {
                Text("Ville introuvable", modifier = Modifier.align(Alignment.Center), color = Color.White)
            }
            is VilleDetailUiState.Success -> {
                val ville = state.ville
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- Image et description ---
                    val fichierImage = remember(versionImage) { VilleImageStore.fichier(context, villeId).takeIf { it.exists() } }
                    val image = rememberImageFichier(fichierImage)
                    if (image != null || ville.description.isNotBlank() || !readOnly) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                if (image != null) {
                                    Image(
                                        bitmap = image,
                                        contentDescription = "Image de ${ville.nom}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large)
                                    )
                                }
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (ville.description.isNotBlank()) {
                                        Text(ville.description, style = MaterialTheme.typography.bodyMedium)
                                    } else if (!readOnly) {
                                        Text("Aucune description (bouton ✎ en haut).", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                                    }
                                    if (!readOnly) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { choisirImage.launch("image/*") }) {
                                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(if (image == null) "Ajouter une image" else "Changer l'image")
                                            }
                                            if (image != null) {
                                                OutlinedButton(onClick = { VilleImageStore.supprimer(context, villeId) }) { Text("Retirer") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Boutiques ---
                    val boutiquesLiees = state.toutesLesBoutiques.filter { it.id in ville.boutiqueIds || it.villeId == ville.id }
                    CollapsibleSectionCard(
                        ownerKey = ownerKey,
                        title = "🏪 Boutiques (${boutiquesLiees.size})",
                        headerActions = {
                            if (!readOnly) {
                                IconButton(onClick = { afficherAjoutBoutique = true }) { Icon(Icons.Default.Add, contentDescription = "Lier une boutique") }
                            }
                        }
                    ) {
                        if (boutiquesLiees.isEmpty()) VideHint("Aucune boutique liée")
                        boutiquesLiees.forEach { boutique ->
                            ElementVille(
                                icone = Icons.Default.Storefront,
                                titre = boutique.nom,
                                detail = listOf(boutique.type.label, boutique.marchand.nom).filter { it.isNotBlank() }.joinToString(" · "),
                                onClick = if (readOnly) null else ({ onOpenBoutique(boutique.id) }),
                                actions = {
                                    if (!readOnly) {
                                        IconButton(onClick = { onOpenBoutique(boutique.id) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Modifier ${boutique.nom}")
                                        }
                                        IconButton(onClick = { viewModel.onDelierBoutique(ville, boutique) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Délier ${boutique.nom}")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // --- Lieux à visiter : l'entrée de la ville d'abord (point d'arrivée des joueurs) ---
                    CollapsibleSectionCard(
                        ownerKey = ownerKey,
                        title = "📍 Lieux à visiter (${state.lieuxNotables.size + 1})",
                        headerActions = {
                            if (!readOnly) {
                                IconButton(onClick = { afficherAjoutLieu = true }) { Icon(Icons.Default.Add, contentDescription = "Ajouter un lieu") }
                            }
                        }
                    ) {
                        ElementVille(
                            icone = Icons.Default.MeetingRoom,
                            titre = ENTREE_DE_LA_VILLE,
                            detail = "Point d'arrivée des joueurs dans la ville",
                        )
                        state.lieuxNotables.forEach { lieu ->
                            ElementVille(
                                icone = Icons.Default.Place,
                                titre = lieu.nom,
                                detail = lieu.description,
                                onClick = if (readOnly) null else ({ lieuEdite = lieu }),
                                actions = {
                                    if (!readOnly) {
                                        IconButton(onClick = { viewModel.onSupprimerLieu(lieu.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer ${lieu.nom}", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // --- Scénarios ---
                    val scenariosAttaches = state.scenariosMonde.filter { it.id in ville.scenarioIds }
                    CollapsibleSectionCard(
                        ownerKey = ownerKey,
                        title = "📜 Scénarios attachés (${scenariosAttaches.size})",
                        headerActions = {
                            if (!readOnly) {
                                IconButton(onClick = { afficherAjoutScenario = true }) { Icon(Icons.Default.Add, contentDescription = "Attacher un scénario") }
                            }
                        }
                    ) {
                        if (scenariosAttaches.isEmpty()) VideHint("Aucun scénario attaché")
                        scenariosAttaches.forEach { scenario ->
                            ElementVille(
                                icone = Icons.AutoMirrored.Filled.MenuBook,
                                titre = scenario.title,
                                actions = {
                                    if (!readOnly) {
                                        IconButton(onClick = { viewModel.onDetacherScenario(ville, scenario.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Détacher ${scenario.title}")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // --- Événements (bibliothèque) : préparation du MJ, masquée côté joueur ---
                    if (!readOnly) {
                        CollapsibleSectionCard(ownerKey = ownerKey, title = "🎲 Événements de cette ville") {
                            EvenementsLiesSection(
                                worldId = EvenementDependencies.mondeDeCampagne(campagneId),
                                campagneId = campagneId,
                                evenementIds = ville.evenementIds,
                                onChanger = viewModel::onChangerEvenements,
                                titre = null
                            )
                        }
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
                                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
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

                if (afficherAjoutLieu || lieuEdite != null) {
                    val initial = lieuEdite
                    var nom by remember(initial) { mutableStateOf(initial?.nom.orEmpty()) }
                    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
                    val fermer = { afficherAjoutLieu = false; lieuEdite = null }
                    AlertDialog(
                        onDismissRequest = fermer,
                        title = { Text(if (initial == null) "Nouveau lieu à visiter" else "Modifier le lieu") },
                        text = {
                            Column {
                                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom (ex: Auberge du Dragon)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (initial == null) viewModel.onAjouterLieu(nom, description)
                                    else viewModel.onModifierLieu(initial.copy(nom = nom, description = description))
                                    fermer()
                                },
                                enabled = nom.isNotBlank()
                            ) { Text(if (initial == null) "Ajouter" else "Enregistrer") }
                        },
                        dismissButton = { TextButton(onClick = fermer) { Text("Annuler") } }
                    )
                }
            }
        }
    }
}

private val TexteSecondaire = Color.White.copy(alpha = 0.75f)

@Composable
private fun VideHint(texte: String) {
    Text(texte, style = MaterialTheme.typography.bodyMedium, color = TexteSecondaire)
}

/** Élément d'une catégorie de la ville (boutique, lieu, scénario) : icône dorée, nom, détail. */
@Composable
private fun ElementVille(
    icone: ImageVector,
    titre: String,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.06f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icone, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!detail.isNullOrBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            actions()
        }
    }
}
