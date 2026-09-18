package com.jc2.jdrcompagnon.feature_carte.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.CarteCampagneViewModelFactory
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.VitesseDeplacement
import com.jc2.jdrcompagnon.feature_carte.presentation.CarteCampagneViewModel
import com.jc2.jdrcompagnon.feature_carte.presentation.CarteUiState
import kotlin.math.roundToInt

private val TAILLE_CASE = 36.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarteCampagneScreen(
    campagneId: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    viewModel: CarteCampagneViewModel = viewModel(factory = CarteCampagneViewModelFactory(campagneId)),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var modeMesure by remember { mutableStateOf(false) }
    var pointSelectionneA by remember { mutableStateOf<PointInteret?>(null) }
    var pointSelectionneB by remember { mutableStateOf<PointInteret?>(null) }
    var vitesse by remember { mutableStateOf(VitesseDeplacement.A_PIED) }
    var pointEnDetail by remember { mutableStateOf<PointInteret?>(null) }
    var nouveauPointPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var menuImageOuvert by remember { mutableStateOf(false) }

    val choisirImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = CarteImageStore.copier(context, uri, campagneId)
            if (fileName != null) viewModel.onDefinirImageFond(fileName)
        }
    }

    val carteActuelle = (uiState as? CarteUiState.Success)?.carte

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CARTE DE CAMPAGNE") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuImageOuvert = true }) {
                            Icon(Icons.Default.ImageIcon, contentDescription = "Image de fond de la carte")
                        }
                        DropdownMenu(expanded = menuImageOuvert, onDismissRequest = { menuImageOuvert = false }) {
                            DropdownMenuItem(
                                text = { Text(if (carteActuelle?.imageFileName == null) "Choisir une image de fond" else "Changer l'image de fond") },
                                onClick = { menuImageOuvert = false; choisirImageLauncher.launch("image/*") }
                            )
                            if (carteActuelle?.imageFileName != null) {
                                DropdownMenuItem(
                                    text = { Text("Retirer l'image de fond") },
                                    onClick = { menuImageOuvert = false; viewModel.onDefinirImageFond(null) }
                                )
                            }
                        }
                    }
                    FilterChip(
                        selected = modeMesure,
                        onClick = {
                            modeMesure = !modeMesure
                            pointSelectionneA = null
                            pointSelectionneB = null
                        },
                        label = { Text(if (modeMesure) "Mesurer" else "Éditer") },
                        leadingIcon = { Icon(Icons.Default.NearMe, contentDescription = null) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (!modeMesure) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val carte = carteActuelle
                        nouveauPointPosition = if (carte != null) (carte.largeurCases / 2) to (carte.hauteurCases / 2) else 0 to 0
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nouveau point") }
                )
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is CarteUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is CarteUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Text(
                        text = if (modeMesure)
                            "Touchez deux points pour calculer le temps de trajet entre eux."
                        else
                            "Touchez une case vide pour y placer un point, ou utilisez le bouton « Nouveau point ». Glissez un point pour le déplacer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier.size(TAILLE_CASE * state.carte.largeurCases, TAILLE_CASE * state.carte.hauteurCases)
                        ) {
                            val imageFileName = state.carte.imageFileName
                            val imageFondBitmap = remember(imageFileName) {
                                imageFileName?.let {
                                    runCatching {
                                        BitmapFactory.decodeFile(CarteImageStore.fichier(context, it).absolutePath)?.asImageBitmap()
                                    }.getOrNull()
                                }
                            }
                            if (imageFondBitmap != null) {
                                Image(
                                    bitmap = imageFondBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(TAILLE_CASE * state.carte.largeurCases, TAILLE_CASE * state.carte.hauteurCases),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                            GrilleCarte(
                                largeurCases = state.carte.largeurCases,
                                hauteurCases = state.carte.hauteurCases,
                                modeMesure = modeMesure,
                                onTapCaseVide = { x, y -> nouveauPointPosition = x to y }
                            )
                            state.points.forEach { point ->
                                PointSurCarte(
                                    point = point,
                                    selectionne = point.id == pointSelectionneA?.id || point.id == pointSelectionneB?.id,
                                    modeMesure = modeMesure,
                                    largeurCases = state.carte.largeurCases,
                                    hauteurCases = state.carte.hauteurCases,
                                    onTap = {
                                        if (modeMesure) {
                                            when {
                                                pointSelectionneA == null -> pointSelectionneA = point
                                                pointSelectionneB == null && point.id != pointSelectionneA?.id -> pointSelectionneB = point
                                                else -> { pointSelectionneA = point; pointSelectionneB = null }
                                            }
                                        } else {
                                            pointEnDetail = point
                                        }
                                    },
                                    onDeplacer = { nouveauX, nouveauY -> viewModel.onDeplacerPoint(point, nouveauX, nouveauY) }
                                )
                            }
                        }
                    }

                    val a = pointSelectionneA
                    val b = pointSelectionneB
                    if (modeMesure && a != null && b != null) {
                        val resultat = viewModel.calculerTrajet(a, b, vitesse)
                        Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${a.nom} → ${b.nom}", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                var menuVitesseOuvert by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { menuVitesseOuvert = true }) { Text("Vitesse : ${vitesse.label} (${vitesse.kmParJour} km/j)") }
                                    DropdownMenu(expanded = menuVitesseOuvert, onDismissRequest = { menuVitesseOuvert = false }) {
                                        VitesseDeplacement.entries.forEach { v ->
                                            DropdownMenuItem(text = { Text(v.label) }, onClick = { vitesse = v; menuVitesseOuvert = false })
                                        }
                                    }
                                }
                                if (resultat != null) {
                                    Text("${resultat.distanceEnKm.roundToInt()} km · ${String.format("%.1f", resultat.joursDeTrajet)} jours de trajet")
                                }
                            }
                        }
                    }
                }

                nouveauPointPosition?.let { (x, y) ->
                    NouveauPointDialog(
                        onDismiss = { nouveauPointPosition = null },
                        onConfirmer = { nom, type, description ->
                            viewModel.onCreerPoint(nom, type, x, y, description)
                            nouveauPointPosition = null
                        }
                    )
                }

                pointEnDetail?.let { point ->
                    PointDetailBottomSheet(
                        point = point,
                        toutesLesBoutiques = state.toutesLesBoutiques,
                        onDismiss = { pointEnDetail = null },
                        onSupprimer = { viewModel.onSupprimerPoint(point.id); pointEnDetail = null },
                        onLierBoutique = { boutique -> viewModel.onLierBoutique(point, boutique) },
                        onDelierBoutique = { boutique -> viewModel.onDelierBoutique(point, boutique) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GrilleCarte(
    largeurCases: Int,
    hauteurCases: Int,
    modeMesure: Boolean,
    onTapCaseVide: (Int, Int) -> Unit
) {
    val couleurTrait = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Canvas(
        modifier = Modifier
            .size(TAILLE_CASE * largeurCases, TAILLE_CASE * hauteurCases)
            .pointerInput(modeMesure, largeurCases, hauteurCases) {
                if (!modeMesure) {
                    detectTapGestures { offset ->
                        val x = (offset.x / TAILLE_CASE.toPx()).toInt().coerceIn(0, largeurCases - 1)
                        val y = (offset.y / TAILLE_CASE.toPx()).toInt().coerceIn(0, hauteurCases - 1)
                        onTapCaseVide(x, y)
                    }
                }
            }
    ) {
        val caseTaillePx = TAILLE_CASE.toPx()
        for (i in 0..largeurCases) {
            drawLine(couleurTrait, Offset(i * caseTaillePx, 0f), Offset(i * caseTaillePx, hauteurCases * caseTaillePx))
        }
        for (j in 0..hauteurCases) {
            drawLine(couleurTrait, Offset(0f, j * caseTaillePx), Offset(largeurCases * caseTaillePx, j * caseTaillePx))
        }
    }
}

@Composable
private fun PointSurCarte(
    point: PointInteret,
    selectionne: Boolean,
    modeMesure: Boolean,
    largeurCases: Int,
    hauteurCases: Int,
    onTap: () -> Unit,
    onDeplacer: (Int, Int) -> Unit
) {
    var dragOffset by remember(point.id, point.x, point.y) { mutableStateOf(Offset.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val caseTaillePx = with(density) { TAILLE_CASE.toPx() }
    val baseX = point.x * caseTaillePx
    val baseY = point.y * caseTaillePx

    val couleur = when {
        selectionne -> MaterialTheme.colorScheme.primary
        point.type == TypePointInteret.VILLE -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Box(
        modifier = Modifier
            .offset { Offset(baseX + dragOffset.x, baseY + dragOffset.y).let { androidx.compose.ui.unit.IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
            .size(TAILLE_CASE)
            .background(couleur, CircleShape)
            .pointerInput(point.id, modeMesure) {
                detectTapGestures { onTap() }
            }
            .pointerInput(point.id, modeMesure, largeurCases, hauteurCases) {
                if (!modeMesure) {
                    detectDragGestures(
                        onDragEnd = {
                            val nouveauX = ((baseX + dragOffset.x) / caseTaillePx).roundToInt().coerceIn(0, largeurCases - 1)
                            val nouveauY = ((baseY + dragOffset.y) / caseTaillePx).roundToInt().coerceIn(0, hauteurCases - 1)
                            onDeplacer(nouveauX, nouveauY)
                            dragOffset = Offset.Zero
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = point.nom.take(2).uppercase(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun NouveauPointDialog(
    onDismiss: () -> Unit,
    onConfirmer: (nom: String, type: TypePointInteret, description: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypePointInteret.VILLE) }
    var description by remember { mutableStateOf("") }
    var menuOuvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau point d'intérêt") },
        text = {
            Column {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Box {
                    TextButton(onClick = { menuOuvert = true }) { Text("Type : ${type.label}") }
                    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                        TypePointInteret.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { type = t; menuOuvert = false })
                        }
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmer(nom, type, description) }, enabled = nom.isNotBlank()) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointDetailBottomSheet(
    point: PointInteret,
    toutesLesBoutiques: List<Boutique>,
    onDismiss: () -> Unit,
    onSupprimer: () -> Unit,
    onLierBoutique: (Boutique) -> Unit,
    onDelierBoutique: (Boutique) -> Unit
) {
    var afficherSelectionBoutique by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(point.nom, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onSupprimer) { Icon(Icons.Default.Delete, contentDescription = "Supprimer le point") }
            }
            Text(point.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (point.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(point.description)
            }

            if (point.type == TypePointInteret.VILLE) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Boutiques liées", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { afficherSelectionBoutique = true }) { Icon(Icons.Default.Add, contentDescription = "Lier une boutique") }
                }
                val boutiquesLiees = toutesLesBoutiques.filter { it.id in point.boutiqueIds }
                if (boutiquesLiees.isEmpty()) {
                    Text("Aucune boutique liée", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                boutiquesLiees.forEach { boutique ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(boutique.nom, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelierBoutique(boutique) }) { Icon(Icons.Default.Close, contentDescription = "Délier ${boutique.nom}") }
                    }
                }
            }
        }
    }

    if (afficherSelectionBoutique) {
        val nonLiees = toutesLesBoutiques.filter { it.villeId == null }
        AlertDialog(
            onDismissRequest = { afficherSelectionBoutique = false },
            title = { Text("Lier une boutique") },
            text = {
                if (nonLiees.isEmpty()) {
                    Text("Aucune boutique disponible (déjà toutes liées à une ville, ou aucune boutique créée).")
                } else {
                    Column {
                        nonLiees.forEach { boutique ->
                            TextButton(
                                onClick = { onLierBoutique(boutique); afficherSelectionBoutique = false },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(boutique.nom) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { afficherSelectionBoutique = false }) { Text("Fermer") } }
        )
    }
}
