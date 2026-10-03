package com.jc2.jdrcompagnon.feature_carte.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import com.jc2.jdrcompagnon.feature_exploration.ExplorationSession
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.CarteCampagneViewModelFactory
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteGrillePrefs
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.VitesseDeplacement
import com.jc2.jdrcompagnon.feature_carte.presentation.CarteCampagneViewModel
import com.jc2.jdrcompagnon.feature_carte.presentation.CarteUiState
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementsLiesSection
import com.jc2.jdrcompagnon.feature_exploration.LieuExploration
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.collectAsState
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.PlanTrajet
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.PlanifierTrajet
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.jc2.jdrcompagnon.feature_carte.data.CartePointPrefs
import com.jc2.jdrcompagnon.feature_carte.data.PositionTexte
import com.jc2.jdrcompagnon.feature_carte.data.ReglagePoint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Écritures lancées depuis l'exploration, qui peut rester ouverte après avoir quitté la carte. */
private val scopePersistance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Palette fixe proposée pour la couleur des traits de la grille (Int ARGB, même format que android.graphics.Color). */
private val PALETTE_GRILLE: List<Int> = listOf(
    0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFEF5350.toInt(), 0xFF42A5F5.toInt(), 0xFFFFCA28.toInt(), 0xFF66BB6A.toInt()
)

/**
 * Taille (sans zoom) d'une case de carte sans image de fond, et taille de base des icônes :
 * fixe, indépendante de la taille des cases de la grille réglée par le MJ.
 */
private val TAILLE_CASE_REFERENCE = 36.dp

/** Étape d'un tracé en mode mesure : un lieu ([pointId]) ou un point libre, en fractions de la carte. */
private data class EtapeTrace(val pointId: String?, val fx: Float, val fy: Float)

/** [EtapeTrace.pointId] d'une étape posée sur l'icône du groupe. */
private const val ETAPE_GROUPE = "__groupe__"

/**
 * Groupe montré sur la carte : côté MJ le groupe sélectionné, côté joueur celui diffusé par le
 * MJ (sinon, hors réseau, le groupe local du personnage choisi).
 */
@Composable
private fun rememberGroupeCarte(readOnly: Boolean): com.jc2.jdrcompagnon.network.GroupeJoueurData? {
    val networkGroup by NetworkSessionManager.networkGroup.collectAsState()
    val groups by com.jc2.jdrcompagnon.ui.GameState.mjGroups.collectAsState()
    val characters by com.jc2.jdrcompagnon.ui.GameState.characters.collectAsState()
    val currentGroupId by com.jc2.jdrcompagnon.ui.GameState.currentGroupId.collectAsState()
    val selectedCharacterId by com.jc2.jdrcompagnon.ui.GameState.selectedCharacterId.collectAsState()
    return remember(readOnly, networkGroup, groups, characters, currentGroupId, selectedCharacterId) {
        val local = if (readOnly) {
            com.jc2.jdrcompagnon.ui.GameState.groupForCharacter(selectedCharacterId, com.jc2.jdrcompagnon.ui.GameState.currentWorldId())
        } else {
            groups.firstOrNull { it.id == currentGroupId }
        }
        (if (readOnly) networkGroup?.details else null)
            ?: local?.let { com.jc2.jdrcompagnon.feature_group.domain.GroupeJoueur.depuis(it, characters) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarteCampagneScreen(
    campagneId: String,
    // Carte à afficher parmi celles de la campagne (null = première carte de la campagne).
    carteId: String? = null,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    // Côté joueur, la carte ne sert qu'à mesurer les distances : pas de mode édition, pas de
    // réglage de grille, pas d'ajout/déplacement de lieu, et seuls les lieux révélés par le MJ
    // sont affichés (réservé au MJ).
    readOnly: Boolean = false,
    // Autre carte de la même campagne choisie dans la barre de titre.
    onOuvrirCarte: (carteId: String) -> Unit = {},
    viewModel: CarteCampagneViewModel = viewModel(key = "carte:${carteId ?: campagneId}", factory = CarteCampagneViewModelFactory(campagneId, carteId)),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Réglages de grille et images propres à chaque carte, indexés par l'id de la carte réellement
    // affichée (la carte historique a pour id le campagneId, donc garde ses réglages existants).
    val cleCarte = (uiState as? CarteUiState.Success)?.carte?.id ?: carteId ?: campagneId
    var modeMesure by remember { mutableStateOf(readOnly) }
    val groupeCarte = rememberGroupeCarte(readOnly)
    // MJ : prochain toucher sur la carte (ou un lieu) = nouvelle position du groupe.
    var placementGroupe by remember { mutableStateOf(false) }
    // Trajet : vitesse choisie (null = celle du groupe) et heures de marche max par jour.
    var choixVitesse by remember { mutableStateOf<VitesseDeplacement?>(null) }
    var heuresMax by remember { mutableStateOf(CarteGrillePrefs.heuresMaxParJour(context)) }
    var evenementContexte by remember { mutableStateOf<String?>(null) }
    var reposLieu by remember { mutableStateOf<String?>(null) }
    val cartesCampagne by remember(campagneId) { CarteDependencies.repository.observerCartes(campagneId) }
        .collectAsState(initial = emptyList())
    // Tracé en mode mesure : étapes successives (lieux ou points libres touchés sur la carte).
    var trace by remember { mutableStateOf(listOf<EtapeTrace>()) }
    var traceTermine by remember { mutableStateOf(false) }
    fun ajouterEtape(etape: EtapeTrace) {
        when {
            // Un nouveau toucher après « Fin de tracé » commence un nouveau tracé.
            traceTermine -> { trace = listOf(etape); traceTermine = false }
            // Même lieu touché deux fois de suite : ignoré.
            etape.pointId != null && trace.lastOrNull()?.pointId == etape.pointId -> Unit
            else -> trace = trace + etape
        }
    }
    // Voyage partagé (route validée par « Prêt au départ », voir VoyageSession).
    val voyage by com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession.etat.collectAsState()
    val nomJoueurReseau by NetworkSessionManager.nomJoueurReseau.collectAsState()
    val roleReseau by NetworkSessionManager.role.collectAsState()
    val playerName by com.jc2.jdrcompagnon.ui.GameState.playerName.collectAsState()
    // Nom sous lequel ce joueur (ou le MJ) apparaît dans la liste des « prêts ».
    val nomMoi = if (readOnly) nomJoueurReseau ?: playerName ?: "Joueur" else com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession.NOM_MJ
    // MJ : choix de l'icône et de la couleur du groupe.
    var iconeGroupeOuverte by remember { mutableStateOf(false) }
    var pointEnDetail by remember { mutableStateOf<PointInteret?>(null) }
    // Position libre (fractions de la carte) du centre de l'icône du lieu à créer ou placer.
    var nouveauPointPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var menuImageOuvert by remember { mutableStateOf(false) }
    var showGrilleDialog by remember { mutableStateOf(false) }
    var caseSizeDp by remember(cleCarte) { mutableStateOf(CarteGrillePrefs.tailleCaseDp(context, cleCarte)) }
    var couleurGrilleArgb by remember(cleCarte) { mutableStateOf(CarteGrillePrefs.couleurGrille(context, cleCarte)) }
    var epaisseurLigneDp by remember(cleCarte) { mutableStateOf(CarteGrillePrefs.epaisseurLigneDp(context, cleCarte)) }
    var afficherNoms by remember(cleCarte) { mutableStateOf(CarteGrillePrefs.afficherNoms(context, cleCarte)) }
    var afficherGrille by remember(cleCarte) { mutableStateOf(CarteGrillePrefs.afficherGrille(context, cleCarte)) }
    var iconesVerrouillees by remember(cleCarte) { mutableStateOf(CarteGrillePrefs.iconesVerrouillees(context, cleCarte)) }
    // Réglages d'affichage modifiés pendant la session ; les autres sont relus dans les préférences.
    var reglagesPoints by remember(cleCarte) { mutableStateOf(mapOf<String, ReglagePoint>()) }
    fun reglageDe(pointId: String): ReglagePoint = reglagesPoints[pointId] ?: CartePointPrefs.lire(context, pointId)
    fun majReglage(pointId: String, reglage: ReglagePoint) {
        reglagesPoints = reglagesPoints + (pointId to reglage)
        CartePointPrefs.ecrire(context, pointId, reglage)
    }
    var zoom by remember(cleCarte) { mutableStateOf(1f) }
    val caseSize = caseSizeDp.dp
    val couleurTraitPersonnalisee = couleurGrilleArgb?.let { Color(it) }
    val density = LocalDensity.current

    val carteActuelle = (uiState as? CarteUiState.Success)?.carte
    val imageFileName = carteActuelle?.imageFileName
    val imageFondBitmap = remember(imageFileName) {
        imageFileName?.let {
            runCatching {
                BitmapFactory.decodeFile(CarteImageStore.fichier(context, it).absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }
    // Taille de la carte sans zoom : celle de l'image de fond, sinon la grille enregistrée avec
    // une case de référence fixe — la taille des cases réglée par le MJ ne redimensionne donc
    // pas la carte et ne déplace pas les icônes.
    val largeurBase: Dp = imageFondBitmap?.let { with(density) { it.width.toDp() } } ?: (TAILLE_CASE_REFERENCE * (carteActuelle?.largeurCases ?: 20))
    val hauteurBase: Dp = imageFondBitmap?.let { with(density) { it.height.toDp() } } ?: (TAILLE_CASE_REFERENCE * (carteActuelle?.hauteurCases ?: 15))

    // Décalage (px) de la carte zoomée dans la zone visible, borné pour ne pas sortir de la carte.
    var decalageX by remember(cleCarte) { mutableFloatStateOf(0f) }
    var decalageY by remember(cleCarte) { mutableFloatStateOf(0f) }
    var tailleVue by remember { mutableStateOf(IntSize.Zero) }
    val tailleBasePx by rememberUpdatedState(with(density) { largeurBase.toPx() to hauteurBase.toPx() })
    val scopeCarte = rememberCoroutineScope()
    var elanCarte by remember { mutableStateOf<Job?>(null) }
    fun bornerDecalage(x: Float, y: Float) {
        val (l, h) = tailleBasePx
        decalageX = x.coerceIn(0f, (l * zoom - tailleVue.width).coerceAtLeast(0f))
        decalageY = y.coerceIn(0f, (h * zoom - tailleVue.height).coerceAtLeast(0f))
    }
    LaunchedEffect(tailleVue, tailleBasePx) { bornerDecalage(decalageX, decalageY) }

    /** Centre de l'icône en fractions de la carte (un ancien point est centré sur sa case). */
    fun positionDe(point: PointInteret): Pair<Float, Float> =
        if (point.fx != null && point.fy != null) point.fx to point.fy
        else ((point.x + 0.5f) * TAILLE_CASE_REFERENCE.value / largeurBase.value).coerceIn(0f, 1f) to
            ((point.y + 0.5f) * TAILLE_CASE_REFERENCE.value / hauteurBase.value).coerceIn(0f, 1f)

    // Cases sur la largeur/hauteur de la carte, base des distances : imposées par le MJ côté joueur
    // (mêmes mesures quel que soit l'écran), sinon déduites de la taille des cases réglée.
    val casesEnLargeur = remember(cleCarte, caseSizeDp, largeurBase) {
        CarteGrillePrefs.casesEnLargeur(context, cleCarte) ?: (largeurBase.value / caseSize.value)
    }
    val casesEnHauteur = casesEnLargeur * hauteurBase.value / largeurBase.value

    /** Case (colonne, ligne) de la grille qui contient la position (fractions de la carte). */
    fun caseDe(fx: Float, fy: Float): Pair<Int, Int> = (fx * casesEnLargeur).toInt() to (fy * casesEnHauteur).toInt()

    // Lieux transmis à l'exploration : les joueurs voient ceux que le MJ y rend visibles.
    val pointsCarte = (uiState as? CarteUiState.Success)?.points.orEmpty()
    // Côté joueur, un lieu non révélé par le MJ n'apparaît pas.
    val pointsAffiches = if (readOnly) pointsCarte.filter { it.visibleJoueurs } else pointsCarte

    // Position du groupe sur cette carte : placée par le MJ, sinon sur le lieu où il se trouve.
    val positionGroupe: Pair<Float, Float>? = groupeCarte?.let { g ->
        if (g.carteId == carteActuelle?.id && g.carteFx != null && g.carteFy != null) g.carteFx to g.carteFy
        else pointsAffiches.firstOrNull { g.location.isNotBlank() && it.nom.equals(g.location, ignoreCase = true) }?.let { positionDe(it) }
    }
    // Lieu où se trouve le groupe (son icône est posée dessus) : le groupe y est alors montré en
    // petit, en haut à droite de l'icône du lieu, plutôt que par-dessus.
    val lieuDuGroupe: PointInteret? = positionGroupe?.let { (gx, gy) ->
        pointsAffiches.firstOrNull { p ->
            val (px, py) = positionDe(p)
            val dx = (px - gx) * largeurBase.value
            val dy = (py - gy) * hauteurBase.value
            dx * dx + dy * dy <= (TAILLE_CASE_REFERENCE.value * 0.5f).let { it * it }
        }
    }
    val iconeGroupe = IconesPointInteret.iconeGroupe(groupeCarte?.iconKey)
    val couleurGroupe = Color(groupeCarte?.couleurArgb ?: IconesPointInteret.COULEUR_GROUPE_DEFAUT)

    /** MJ : place le groupe sélectionné sur cette carte (et à ce lieu, si c'en est un). */
    fun placerGroupe(fx: Float, fy: Float, lieu: String?) {
        val g = com.jc2.jdrcompagnon.ui.GameState.mjGroups.value
            .firstOrNull { it.id == com.jc2.jdrcompagnon.ui.GameState.currentGroupId.value } ?: return
        com.jc2.jdrcompagnon.ui.GameState.updateMjGroup(
            g.copy(carteId = carteActuelle?.id, carteFx = fx, carteFy = fy, location = lieu ?: g.location)
        )
        placementGroupe = false
    }

    /** Toucher l'icône du groupe : étape du tracé en mesure ; MJ en édition : choix de son icône. */
    fun toucherGroupe(gx: Float, gy: Float) {
        when {
            modeMesure -> ajouterEtape(EtapeTrace(ETAPE_GROUPE, gx, gy))
            !readOnly && groupeCarte != null -> iconeGroupeOuverte = true
        }
    }
    val lieuxExploration = pointsCarte.map { p ->
        val (fx, fy) = positionDe(p)
        LieuExploration(p.id, p.nom, fx, fy, IconesPointInteret.emojiPour(p.type, p.iconKey), p.couleurArgb, p.visibleJoueurs)
    }
    LaunchedEffect(lieuxExploration, carteActuelle?.id) {
        carteActuelle?.let { ExplorationSession.majLieux(context, "carte:${it.id}", lieuxExploration) }
    }

    val choisirImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = CarteImageStore.copier(context, uri, cleCarte)
            if (fileName != null) viewModel.onDefinirImageFond(fileName)
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    actions = {
                        if (!readOnly) {
                            IconButton(onClick = { showGrilleDialog = true }) {
                                Icon(Icons.Default.GridOn, contentDescription = "Réglages de la grille")
                            }
                        }
                        // Exploration (brouillard de guerre sur la page table) : seulement pour le
                        // MJ, et sur une carte qui a une image de fond à dévoiler.
                        if (!readOnly && carteActuelle?.imageFileName != null) {
                            IconButton(onClick = {
                                val fichier = CarteImageStore.fichier(context, carteActuelle.imageFileName!!)
                                ExplorationSession.ouvrirEcran(
                                    context, "carte:${carteActuelle.id}", carteActuelle.nom, fichier,
                                    lieux = lieuxExploration,
                                    // Hors du ViewModel : l'exploration reste ouverte après avoir quitté la carte.
                                    onVisibiliteLieu = { id, visible ->
                                        scopePersistance.launch { CarteDependencies.repository.definirVisibiliteJoueurs(id, visible) }
                                    },
                                )
                            }) {
                                Icon(Icons.Default.Explore, contentDescription = "Exploration (brouillard sur la table)")
                            }
                        }
                        if (!readOnly) {
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
                                            text = { Text("Envoyer à l'écran de table") },
                                            leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) },
                                            onClick = { menuImageOuvert = false; envoyerCarteALaTable(context, carteActuelle) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Retirer l'image de fond") },
                                            onClick = { menuImageOuvert = false; viewModel.onDefinirImageFond(null) }
                                        )
                                    }
                                }
                            }
                        }
                        // Joueur : toujours en mesure, pas de mode édition.
                        if (!readOnly) {
                            FilterChip(
                                selected = modeMesure,
                                onClick = {
                                    modeMesure = !modeMesure
                                    placementGroupe = false
                                    trace = emptyList()
                                    traceTermine = false
                                },
                                label = { Text(if (modeMesure) "Mesurer" else "Éditer") },
                                leadingIcon = { Icon(Icons.Default.NearMe, contentDescription = null) },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    // Titre de la carte, à droite de la flèche retour : menu des autres cartes de la campagne.
                    var menuCartes by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            (carteActuelle?.nom ?: "Carte de campagne") + if (cartesCampagne.size > 1) " ▾" else "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = cartesCampagne.size > 1) { menuCartes = true }
                        )
                        DropdownMenu(expanded = menuCartes, onDismissRequest = { menuCartes = false }) {
                            cartesCampagne.filter { it.id != carteActuelle?.id }.forEach { autre ->
                                DropdownMenuItem(text = { Text(autre.nom) }, onClick = { menuCartes = false; onOuvrirCarte(autre.id) })
                            }
                        }
                    }
                    // MJ : placer l'icône d'un groupe sur la carte. Sans groupe sélectionné (ou pour en
                    // changer), un menu propose les groupes du monde ; le choix devient le groupe courant.
                    if (!readOnly && !modeMesure) {
                        val groupesMonde by com.jc2.jdrcompagnon.ui.GameState.mjGroups.collectAsState()
                        val groupesDuMonde = groupesMonde.filter { it.worldId == com.jc2.jdrcompagnon.ui.GameState.currentWorldId() }
                        var menuGroupes by remember { mutableStateOf(false) }
                        if (groupesDuMonde.isNotEmpty()) {
                            Box {
                                IconButton(onClick = {
                                    when {
                                        placementGroupe -> placementGroupe = false
                                        groupeCarte == null || groupesDuMonde.size > 1 -> menuGroupes = true
                                        else -> placementGroupe = true
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = "Placer le groupe sur la carte",
                                        tint = if (placementGroupe) Color(0xFFFFD54F) else androidx.compose.material3.LocalContentColor.current
                                    )
                                }
                                DropdownMenu(expanded = menuGroupes, onDismissRequest = { menuGroupes = false }) {
                                    Text(
                                        "Placer quel groupe ?",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                    groupesDuMonde.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text(g.name, fontWeight = if (g.id == groupeCarte?.id) FontWeight.Bold else null) },
                                            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                            onClick = {
                                                menuGroupes = false
                                                com.jc2.jdrcompagnon.ui.GameState.setCurrentGroupId(g.id)
                                                placementGroupe = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Verrouillage de l'emplacement de toutes les icônes de la carte.
                    if (!modeMesure && !readOnly) {
                        IconButton(onClick = {
                            iconesVerrouillees = !iconesVerrouillees
                            CarteGrillePrefs.setIconesVerrouillees(context, cleCarte, iconesVerrouillees)
                        }) {
                            Icon(
                                if (iconesVerrouillees) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (iconesVerrouillees) "Déverrouiller les icônes" else "Verrouiller les icônes"
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!modeMesure && !readOnly) {
                ExtendedFloatingActionButton(
                    onClick = { nouveauPointPosition = 0.5f to 0.5f },
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
                // Trajet planifié (vitesse du groupe ou choisie, pauses de fin de journée).
                val vitesseGroupeM = groupeCarte?.vitesseM
                val kmParHeure = if (choixVitesse == null && vitesseGroupeM != null) PlanifierTrajet.kmParHeure(vitesseGroupeM)
                else (choixVitesse ?: VitesseDeplacement.A_PIED).kmParJour / 8.0
                val plan = if (modeMesure && trace.size >= 2) {
                    PlanifierTrajet.planifier(trace.map { it.fx to it.fy }, casesEnLargeur, casesEnHauteur, state.carte.echelleKmParCase, kmParHeure, heuresMax)
                } else null
                val nomEtape = { e: EtapeTrace ->
                    when (e.pointId) {
                        ETAPE_GROUPE -> "le groupe"
                        null -> caseDe(e.fx, e.fy).let { (c, l) -> "case ${c + 1}, ${l + 1}" }
                        else -> state.points.firstOrNull { it.id == e.pointId }?.nom ?: "lieu"
                    }
                }

                // ── Voyage partagé ──
                val session = com.jc2.jdrcompagnon.feature_carte.presentation.VoyageSession
                val voyageIci = voyage?.takeIf { it.carteId == state.carte.id }
                // Seul un tracé qui part du groupe (son icône, ou le lieu où il se trouve) est un
                // voyage : partant d'ailleurs, ce n'est qu'une estimation de distance.
                val partDuGroupe = trace.firstOrNull()?.pointId.let { depart ->
                    depart == ETAPE_GROUPE || (depart != null && depart == lieuDuGroupe?.id)
                }
                /** Route tracée ici depuis le groupe, terminée (« Fin de tracé »), sous forme de voyage à partager. */
                fun routeActuelle(): com.jc2.jdrcompagnon.network.VoyageData? {
                    val p = plan ?: return null
                    if (!traceTermine || trace.size < 2 || !partDuGroupe) return null
                    val fin = trace.last()
                    return com.jc2.jdrcompagnon.network.VoyageData(
                        id = java.util.UUID.randomUUID().toString(),
                        campagneId = campagneId,
                        carteId = state.carte.id,
                        etapes = trace.map { com.jc2.jdrcompagnon.network.EtapeVoyageData(it.pointId, it.fx, it.fy) },
                        vitesse = choixVitesse?.name,
                        heuresMax = heuresMax,
                        distanceKm = p.distanceKm,
                        kmParHeure = p.kmParHeure,
                        arrets = p.arrets.map { com.jc2.jdrcompagnon.network.ArretVoyageData(it.numero, it.fx, it.fy, it.kmParcourus) },
                        arriveeNom = nomEtape(fin),
                        arriveePointId = fin.pointId?.takeIf { it != ETAPE_GROUPE },
                        proposePar = nomMoi,
                    )
                }
                // Route partagée reçue ou modifiée : affichée telle quelle sur la carte.
                LaunchedEffect(voyageIci?.id, voyageIci?.version) {
                    val v = voyageIci ?: return@LaunchedEffect
                    trace = v.etapes.map { EtapeTrace(it.pointId, it.fx, it.fy) }
                    traceTermine = true
                    choixVitesse = v.vitesse?.let { n -> VitesseDeplacement.entries.firstOrNull { it.name == n } }
                    heuresMax = v.heuresMax
                    modeMesure = true
                }
                // MJ : changer la route, l'allure ou les heures de marche par jour (les haltes)
                // modifie le voyage partagé ; chacun doit alors le revalider.
                LaunchedEffect(trace, traceTermine, choixVitesse, heuresMax) {
                    if (readOnly) return@LaunchedEffect
                    val v = voyageIci ?: return@LaunchedEffect
                    val r = routeActuelle() ?: return@LaunchedEffect
                    if (!v.enRoute && !v.memeTrajet(r)) session.proposer(r, par = session.NOM_MJ, pretNom = null, mjPret = false)
                }
                val maRoute = routeActuelle()
                val routeDifferente = maRoute != null && (voyageIci == null || !voyageIci.memeTrajet(maRoute))
                // La carte montre bien la route partagée (et pas une mesure faite à côté).
                val afficheRoutePartagee = voyageIci != null &&
                    trace.map { com.jc2.jdrcompagnon.network.EtapeVoyageData(it.pointId, it.fx, it.fy) } == voyageIci.etapes
                val dejaPret = voyageIci != null && !routeDifferente &&
                    (if (readOnly) nomMoi in voyageIci.prets else voyageIci.mjPret)
                fun pretAuDepart() {
                    val proposition = maRoute?.takeIf { routeDifferente }
                    when {
                        // Joueur connecté : le MJ fait foi.
                        readOnly && roleReseau == com.jc2.jdrcompagnon.network.SessionRole.PLAYER ->
                            NetworkSessionManager.voyagePret(proposition)
                        proposition != null -> session.proposer(proposition, par = nomMoi, pretNom = nomMoi.takeIf { readOnly }, mjPret = !readOnly)
                        readOnly -> session.marquerPret(nomMoi)
                        else -> session.marquerMjPret()
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (placementGroupe) {
                        Text(
                            "Touchez la carte ou un lieu pour y placer le groupe « ${groupeCarte?.name.orEmpty()} ».",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (modeMesure) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            // Route terminée (« Fin de tracé ») ou route partagée : on se déclare prêt.
                            Button(
                                onClick = { pretAuDepart() },
                                // Un tracé qui ne part pas du groupe n'est qu'une mesure : pas de départ.
                                enabled = !dejaPret && voyageIci?.enRoute != true && (maRoute != null || afficheRoutePartagee),
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (dejaPret) "Prêt ✔" else "Prêt au départ")
                            }
                            voyageIci?.let { v ->
                                Text(
                                    if (v.enRoute) "En route vers ${v.arriveeNom}"
                                    else "Prêts ${(if (v.mjPret) 1 else 0) + v.prets.size}/${v.attendus.size + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            // Départ depuis la position du groupe.
                            if (positionGroupe != null && (trace.isEmpty() || traceTermine)) {
                                TextButton(onClick = {
                                    trace = listOf(EtapeTrace(ETAPE_GROUPE, positionGroupe.first, positionGroupe.second))
                                    traceTermine = false
                                }) { Text("Partir du groupe") }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .onSizeChanged { tailleVue = it }
                            // Déplacement à un doigt (avec inertie) et zoom à deux doigts centré
                            // sur les doigts. Un glissement déjà pris par une icône (déplacement
                            // d'un lieu) ne fait pas défiler la carte ; un simple toucher reste un
                            // tap (placer un point) tant qu'il ne dépasse pas le seuil de glissement.
                            .pointerInput(cleCarte) {
                                val decay = splineBasedDecay<Float>(this)
                                awaitEachGesture {
                                    val premier = awaitFirstDown(requireUnconsumed = false)
                                    elanCarte?.cancel()
                                    val suivi = VelocityTracker()
                                    suivi.addPointerInputChange(premier)
                                    var cumul = Offset.Zero
                                    var glisse = false
                                    var pincement = false
                                    var abandon = false
                                    do {
                                        val event = awaitPointerEvent()
                                        if (event.changes.count { it.pressed } >= 2) {
                                            pincement = true
                                            // Le point de la carte sous les doigts y reste : le
                                            // décalage suit le changement d'échelle autour du centre.
                                            val centre = event.calculateCentroid(useCurrent = false)
                                            val pan = event.calculatePan()
                                            val ancienZoom = zoom
                                            val nouveauZoom = (zoom * event.calculateZoom()).coerceIn(0.5f, 4f)
                                            val f = nouveauZoom / ancienZoom
                                            zoom = nouveauZoom
                                            bornerDecalage(
                                                (decalageX + centre.x) * f - centre.x - pan.x,
                                                (decalageY + centre.y) * f - centre.y - pan.y,
                                            )
                                            event.changes.forEach { it.consume() }
                                        } else if (!pincement && !abandon) {
                                            val change = event.changes.firstOrNull { it.id == premier.id } ?: event.changes.first()
                                            if (change.isConsumed) {
                                                if (!glisse) abandon = true
                                            } else {
                                                suivi.addPointerInputChange(change)
                                                val delta = change.positionChange()
                                                if (!glisse) {
                                                    cumul += delta
                                                    glisse = cumul.getDistance() > viewConfiguration.touchSlop
                                                }
                                                if (glisse) {
                                                    bornerDecalage(decalageX - delta.x, decalageY - delta.y)
                                                    change.consume()
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                    if (glisse && !pincement) {
                                        val vitesse = suivi.calculateVelocity()
                                        elanCarte = scopeCarte.launch {
                                            launch {
                                                Animatable(decalageX).animateDecay(-vitesse.x, decay) { bornerDecalage(value, decalageY) }
                                            }
                                            launch {
                                                Animatable(decalageY).animateDecay(-vitesse.y, decay) { bornerDecalage(decalageX, value) }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        val caseSizeZoome = caseSize * zoom

                        // La carte garde la taille réelle de son image de fond (pas d'étirement
                        // imposé par la grille) ; sans image, on retombe sur la taille en cases.
                        // Le pincement multiplie ces tailles par [zoom] : la carte est réellement
                        // agrandie (pas un simple zoom visuel), puis décalée de [decalageX]/[decalageY].
                        val largeurBox = largeurBase * zoom
                        val hauteurBox = hauteurBase * zoom

                        Box(
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .offset { androidx.compose.ui.unit.IntOffset(-decalageX.roundToInt(), -decalageY.roundToInt()) }
                                .size(largeurBox, hauteurBox)
                        ) {
                            if (imageFondBitmap != null) {
                                Image(
                                    bitmap = imageFondBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(largeurBox, hauteurBox),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                            GrilleCarte(
                                largeurBox = largeurBox,
                                hauteurBox = hauteurBox,
                                caseSize = caseSizeZoome,
                                epaisseurTrait = epaisseurLigneDp.dp,
                                couleurTrait = couleurTraitPersonnalisee,
                                // Masquable en édition ; jamais tracée en mode mesure (le calcul
                                // des distances reste basé sur les cases).
                                afficher = !modeMesure && afficherGrille,
                                // Étapes du tracé, reliées dans l'ordre par un trait.
                                trajet = if (modeMesure) trace.map { it.fx to it.fy } else emptyList(),
                                onTap = { fx, fy ->
                                    when {
                                        modeMesure -> ajouterEtape(EtapeTrace(null, fx, fy))
                                        placementGroupe -> placerGroupe(fx, fy, null)
                                        !readOnly -> nouveauPointPosition = fx to fy
                                    }
                                }
                            )
                            pointsAffiches.forEach { point ->
                                val (fx, fy) = positionDe(point)
                                val reglage = reglageDe(point.id)
                                PointSurCarte(
                                    point = point,
                                    fx = fx,
                                    fy = fy,
                                    largeurBox = largeurBox,
                                    hauteurBox = hauteurBox,
                                    tailleBase = TAILLE_CASE_REFERENCE * zoom,
                                    // Numéro de la (première) étape du tracé sur ce lieu.
                                    marqueMesure = if (modeMesure) {
                                        trace.indexOfFirst { it.pointId == point.id }.takeIf { it >= 0 }?.let { "${it + 1}" }
                                    } else null,
                                    modeMesure = modeMesure,
                                    afficherNoms = afficherNoms && reglage.afficherTexte,
                                    echelleIcone = reglage.taille,
                                    positionTexte = reglage.positionTexte,
                                    // Groupe présent dans ce lieu : sa petite icône en haut à droite.
                                    badgeGroupe = if (point.id == lieuDuGroupe?.id && groupeCarte != null) {
                                        BadgeGroupe(iconeGroupe, couleurGroupe) { toucherGroupe(fx, fy) }
                                    } else null,
                                    onTap = {
                                        when {
                                            modeMesure -> ajouterEtape(EtapeTrace(point.id, fx, fy))
                                            placementGroupe -> placerGroupe(fx, fy, point.nom)
                                            else -> pointEnDetail = point
                                        }
                                    },
                                    deplacable = !readOnly && !iconesVerrouillees && !reglage.verrouille,
                                    onDeplacer = { nouveauFx, nouveauFy -> viewModel.onDeplacerPoint(point, nouveauFx, nouveauFy) }
                                )
                            }
                            // Haltes de fin de journée du trajet.
                            plan?.arrets?.forEach { arret ->
                                MarqueurSurCarte(
                                    fx = arret.fx,
                                    fy = arret.fy,
                                    largeurBox = largeurBox,
                                    hauteurBox = hauteurBox,
                                    taille = 26.dp,
                                    couleur = Color(0xFF3949AB),
                                    icone = Icons.Default.Bedtime,
                                    libelle = "Halte ${arret.numero}",
                                )
                            }
                            // Icône du groupe (sans son nom), par-dessus les lieux — sauf quand il est
                            // dans un lieu : elle est alors en petit sur l'icône du lieu (badgeGroupe).
                            positionGroupe?.takeIf { lieuDuGroupe == null }?.let { (gx, gy) ->
                                MarqueurSurCarte(
                                    fx = gx,
                                    fy = gy,
                                    largeurBox = largeurBox,
                                    hauteurBox = hauteurBox,
                                    taille = TAILLE_CASE_REFERENCE * zoom * 1.1f,
                                    couleur = couleurGroupe,
                                    icone = iconeGroupe,
                                    libelle = groupeCarte?.name ?: "Groupe",
                                    afficherLibelle = false,
                                    onTap = { toucherGroupe(gx, gy) },
                                    // MJ en édition : le groupe se glisse directement sur la carte ;
                                    // lâché sur un lieu, il y est installé (nom du lieu du groupe).
                                    onDeplacer = if (!readOnly && !modeMesure) { nfx, nfy ->
                                        val lieu = pointsAffiches.firstOrNull { p ->
                                            val (px, py) = positionDe(p)
                                            val dx = (px - nfx) * largeurBase.value
                                            val dy = (py - nfy) * hauteurBase.value
                                            dx * dx + dy * dy <= (TAILLE_CASE_REFERENCE.value * 0.75f).let { it * it }
                                        }
                                        if (lieu != null) positionDe(lieu).let { (lx, ly) -> placerGroupe(lx, ly, lieu.nom) }
                                        else placerGroupe(nfx, nfy, null)
                                    } else null,
                                )
                            }
                        }
                    }

                    if (modeMesure && trace.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (trace.size == 1) "Départ : ${nomEtape(trace.first())}"
                                        else "${nomEtape(trace.first())} → ${nomEtape(trace.last())} · ${trace.size - 1} segment(s)",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!traceTermine) {
                                        TextButton(onClick = { trace = trace.dropLast(1) }) { Text("Annuler") }
                                    }
                                }
                                if (trace.size >= 2 && !partDuGroupe) {
                                    Text(
                                        "Estimation de distance seulement : pour voyager, commencez le tracé sur le groupe" +
                                            (lieuDuGroupe?.let { " (ou sur ${it.nom})" } ?: "") + ".",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (voyageIci != null && !afficheRoutePartagee) {
                                    TextButton(onClick = {
                                        trace = voyageIci.etapes.map { EtapeTrace(it.pointId, it.fx, it.fy) }
                                        traceTermine = true
                                        choixVitesse = voyageIci.vitesse?.let { n -> VitesseDeplacement.entries.firstOrNull { it.name == n } }
                                        heuresMax = voyageIci.heuresMax
                                    }) { Text("Revoir la route du groupe") }
                                }
                                voyageIci?.let { v ->
                                    val prets = listOfNotNull("MJ".takeIf { v.mjPret }) + v.prets
                                    val enAttente = listOfNotNull("MJ".takeIf { !v.mjPret }) + v.attendus.filter { it !in v.prets }
                                    Text(
                                        "Route sélectionnée par ${v.proposePar}" + if (routeDifferente) " (votre tracé diffère : « Prêt au départ » le proposera)" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Prêts : ${prets.joinToString().ifBlank { "personne" }}" +
                                            if (enAttente.isNotEmpty()) " · en attente : ${enAttente.joinToString()}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (!readOnly && !v.enRoute) {
                                        TextButton(onClick = { session.annuler() }) { Text("Annuler le voyage") }
                                    }
                                }
                                if (!traceTermine && trace.size >= 2) {
                                    Button(onClick = { traceTermine = true }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Flag, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Fin de tracé")
                                    }
                                }
                                if (plan != null) {
                                    TrajetResultat(
                                        plan = plan,
                                        groupe = groupeCarte,
                                        choixVitesse = choixVitesse,
                                        onChoixVitesse = { choixVitesse = it },
                                        heuresMax = heuresMax,
                                        onHeuresMax = { h ->
                                            heuresMax = h
                                            CarteGrillePrefs.setHeuresMaxParJour(context, h)
                                        },
                                        arrivee = nomEtape(trace.last()),
                                        outilsMj = !readOnly,
                                        onEvenement = { contexte -> evenementContexte = contexte },
                                        onRepos = { lieu -> reposLieu = lieu },
                                        onDeplacerGroupe = if (!readOnly && groupeCarte != null) {
                                            {
                                                val fin = trace.last()
                                                placerGroupe(fin.fx, fin.fy, fin.pointId?.let { id -> state.points.firstOrNull { it.id == id }?.nom })
                                            }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }

                evenementContexte?.let { contexte ->
                    EvenementTrajetDialog(campagneId = campagneId, contexte = contexte, onDismiss = { evenementContexte = null })
                }
                reposLieu?.let { lieu ->
                    ReposGroupeDialog(lieu = lieu, onDismiss = { reposLieu = null })
                }
                if (iconeGroupeOuverte && groupeCarte != null) {
                    GroupeIconeSheet(
                        nom = groupeCarte.name,
                        iconKey = groupeCarte.iconKey,
                        couleurArgb = groupeCarte.couleurArgb,
                        onChoix = { iconKey, couleurArgb ->
                            val g = com.jc2.jdrcompagnon.ui.GameState.mjGroups.value.firstOrNull { it.id == groupeCarte.id }
                            if (g != null) com.jc2.jdrcompagnon.ui.GameState.updateMjGroup(g.copy(iconKey = iconKey, couleurArgb = couleurArgb))
                        },
                        onDismiss = { iconeGroupeOuverte = false },
                    )
                }

                nouveauPointPosition?.let { (fx, fy) ->
                    NouveauPointDialog(
                        pointsNonPlaces = state.pointsNonPlaces,
                        onDismiss = { nouveauPointPosition = null },
                        onPlacerExistant = { point ->
                            viewModel.onPlacerPoint(point, fx, fy)
                            nouveauPointPosition = null
                        },
                        onConfirmer = { nom, type, description ->
                            viewModel.onCreerPoint(nom, type, fx, fy, description)
                            nouveauPointPosition = null
                        }
                    )
                }

                pointEnDetail?.let { point ->
                    PointDetailBottomSheet(
                        point = point,
                        toutesLesBoutiques = state.toutesLesBoutiques,
                        readOnly = readOnly,
                        onDismiss = { pointEnDetail = null },
                        onSupprimer = { viewModel.onSupprimerPoint(point.id); pointEnDetail = null },
                        onRetirerDeLaCarte = { viewModel.onRetirerPointDeLaCarte(point); pointEnDetail = null },
                        onLierBoutique = { boutique -> viewModel.onLierBoutique(point, boutique) },
                        onDelierBoutique = { boutique -> viewModel.onDelierBoutique(point, boutique) },
                        onPersonnaliser = { iconKey, couleurArgb ->
                            viewModel.onPersonnaliserPoint(point, iconKey, couleurArgb)
                            pointEnDetail = point.copy(iconKey = iconKey, couleurArgb = couleurArgb)
                        },
                        onVisibiliteJoueurs = { visible ->
                            viewModel.onBasculerVisibiliteJoueurs(point, visible)
                            pointEnDetail = point.copy(visibleJoueurs = visible)
                        },
                        onChangerEvenements = { ids ->
                            viewModel.onDefinirEvenements(point, ids)
                            pointEnDetail = point.copy(evenementIds = ids)
                        },
                        reglage = reglageDe(point.id),
                        onReglage = { majReglage(point.id, it) }
                    )
                }
            }
        }

        if (showGrilleDialog) {
            GrilleReglagesDialog(
                tailleActuelle = caseSizeDp,
                epaisseurActuelle = epaisseurLigneDp,
                couleurActuelle = couleurGrilleArgb,
                echelleActuelle = carteActuelle?.echelleKmParCase ?: 10,
                afficherNomsActuel = afficherNoms,
                afficherGrilleActuel = afficherGrille,
                onAfficherGrilleChangee = { nouvelleValeur ->
                    afficherGrille = nouvelleValeur
                    CarteGrillePrefs.setAfficherGrille(context, cleCarte, nouvelleValeur)
                },
                onDismiss = { showGrilleDialog = false },
                onTailleChanged = { nouvelleTaille ->
                    caseSizeDp = nouvelleTaille
                    CarteGrillePrefs.setTailleCaseDp(context, cleCarte, nouvelleTaille)
                },
                onEpaisseurChangee = { nouvelleEpaisseur ->
                    epaisseurLigneDp = nouvelleEpaisseur
                    CarteGrillePrefs.setEpaisseurLigneDp(context, cleCarte, nouvelleEpaisseur)
                },
                onCouleurChangee = { nouvelleCouleur ->
                    couleurGrilleArgb = nouvelleCouleur
                    CarteGrillePrefs.setCouleurGrille(context, cleCarte, nouvelleCouleur)
                },
                onEchelleChangee = onEchelleChangee@{ nouvelleEchelle ->
                    val carte = carteActuelle ?: return@onEchelleChangee
                    viewModel.onRedimensionnerCarte(carte.largeurCases, carte.hauteurCases, nouvelleEchelle)
                },
                onAfficherNomsChangee = { nouvelleValeur ->
                    afficherNoms = nouvelleValeur
                    CarteGrillePrefs.setAfficherNoms(context, cleCarte, nouvelleValeur)
                }
            )
        }
    }
}

@Composable
private fun GrilleReglagesDialog(
    tailleActuelle: Int,
    epaisseurActuelle: Int,
    couleurActuelle: Int?,
    echelleActuelle: Int,
    afficherNomsActuel: Boolean,
    afficherGrilleActuel: Boolean,
    onAfficherGrilleChangee: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTailleChanged: (Int) -> Unit,
    onEpaisseurChangee: (Int) -> Unit,
    onCouleurChangee: (Int) -> Unit,
    onEchelleChangee: (Int) -> Unit,
    onAfficherNomsChangee: (Boolean) -> Unit
) {
    var echelleTexte by remember { mutableStateOf(echelleActuelle.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Réglages de la carte", modifier = Modifier.weight(1f))
                IconButton(onClick = { onAfficherGrilleChangee(!afficherGrilleActuel) }) {
                    Icon(
                        if (afficherGrilleActuel) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = if (afficherGrilleActuel) "Masquer la grille" else "Afficher la grille"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Taille des cases : ${tailleActuelle}dp")
                Slider(
                    value = tailleActuelle.toFloat(),
                    onValueChange = { onTailleChanged(it.roundToInt()) },
                    valueRange = 8f..72f,
                    steps = 15 // pas de 4dp entre 8 et 72
                )
                Spacer(Modifier.height(12.dp))
                Text("Épaisseur des traits : ${epaisseurActuelle}dp")
                Slider(
                    value = epaisseurActuelle.toFloat(),
                    onValueChange = { onEpaisseurChangee(it.roundToInt()) },
                    valueRange = 1f..6f,
                    steps = 4
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("Échelle de la carte", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Distance réelle représentée par une case de la grille. Sert au calcul du temps de trajet entre deux points.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = echelleTexte,
                    onValueChange = { valeur ->
                        val filtre = valeur.filter { it.isDigit() }
                        echelleTexte = filtre
                        filtre.toIntOrNull()?.takeIf { it > 0 }?.let(onEchelleChangee)
                    },
                    label = { Text("Kilomètres par case") },
                    suffix = { Text("km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("Couleur des traits")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETTE_GRILLE.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(argb), CircleShape)
                                .border(
                                    width = if (couleurActuelle == argb) 3.dp else 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onCouleurChangee(argb) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Afficher le nom des points", modifier = Modifier.weight(1f))
                    Switch(checked = afficherNomsActuel, onCheckedChange = onAfficherNomsChangee)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

/**
 * Grille dessinée par-dessus l'image de fond (Canvas placé après l'Image dans le Box parent, donc
 * peint après = au-dessus). [largeurBox]/[hauteurBox] correspondent exactement à la taille réelle
 * de la carte (image ou repli en cases) : la grille ne peut donc jamais la dépasser.
 */
@Composable
private fun GrilleCarte(
    largeurBox: Dp,
    hauteurBox: Dp,
    caseSize: Dp,
    epaisseurTrait: Dp,
    couleurTrait: Color?,
    // Faux : quadrillage masqué (la zone reste touchable pour placer un lieu).
    afficher: Boolean,
    // Mode mesure : étapes du tracé (fractions de la carte), reliées dans l'ordre.
    trajet: List<Pair<Float, Float>>,
    // Toucher hors d'un lieu : position libre, en fractions de la carte.
    onTap: (Float, Float) -> Unit
) {
    val couleur = couleurTrait ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val surlignage = MaterialTheme.colorScheme.primary
    val onTapActuel by rememberUpdatedState(onTap)
    Canvas(
        modifier = Modifier
            .size(largeurBox, hauteurBox)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTapActuel((offset.x / size.width).coerceIn(0f, 1f), (offset.y / size.height).coerceIn(0f, 1f))
                }
            }
    ) {
        val caseTaillePx = caseSize.toPx()
        val largeurPx = largeurBox.toPx()
        val hauteurPx = hauteurBox.toPx()
        val epaisseurPx = epaisseurTrait.toPx()
        val etapes = trajet.map { (fx, fy) -> Offset(fx * largeurPx, fy * hauteurPx) }
        val pointille = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 6.dp.toPx()))
        etapes.zipWithNext { depart, arrivee ->
            drawLine(surlignage, depart, arrivee, strokeWidth = 3.dp.toPx(), pathEffect = pointille)
        }
        etapes.forEach { etape ->
            drawCircle(Color.White, radius = 6.dp.toPx(), center = etape)
            drawCircle(surlignage, radius = 4.dp.toPx(), center = etape)
        }
        if (!afficher) return@Canvas
        var x = 0f
        while (x <= largeurPx) {
            drawLine(couleur, Offset(x, 0f), Offset(x, hauteurPx), strokeWidth = epaisseurPx)
            x += caseTaillePx
        }
        var y = 0f
        while (y <= hauteurPx) {
            drawLine(couleur, Offset(0f, y), Offset(largeurPx, y), strokeWidth = epaisseurPx)
            y += caseTaillePx
        }
    }
}

@Composable
private fun PointSurCarte(
    point: PointInteret,
    // Centre de l'icône en fractions de la carte : placement libre, sans lien avec la grille.
    fx: Float,
    fy: Float,
    largeurBox: Dp,
    hauteurBox: Dp,
    // Taille de base de l'icône (zoom compris), indépendante de la taille des cases de la grille.
    tailleBase: Dp,
    // Mode mesure : numéro de l'étape du tracé si ce lieu en fait partie, sinon null.
    marqueMesure: String?,
    modeMesure: Boolean,
    afficherNoms: Boolean,
    // Multiple de la taille d'une case, réglé par point.
    echelleIcone: Float,
    positionTexte: PositionTexte,
    // Faux côté joueur (carte en lecture seule) ou icône verrouillée : le point ne se glisse pas.
    deplacable: Boolean,
    onTap: () -> Unit,
    onDeplacer: (Float, Float) -> Unit,
    // Groupe présent dans ce lieu : sa petite icône, en haut à droite de celle du lieu.
    badgeGroupe: BadgeGroupe? = null,
) {
    // Décalage du glissement en cours : remis à zéro quand la nouvelle position enregistrée
    // arrive (pas avant, sinon le point revient un instant à son ancienne place).
    var dragOffset by remember(point.id) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(fx, fy) { dragOffset = Offset.Zero }
    val density = LocalDensity.current
    val selectionne = marqueMesure != null
    // Icône de taille fixe × réglage du point (agrandie quand elle fait partie du tracé) ; zone de
    // toucher d'au moins 44dp centrée dessus, même quand la carte est dézoomée.
    val tailleIcone = tailleBase * echelleIcone * (if (selectionne) 1.3f else 1f)
    val cible = maxOf(tailleIcone, 44.dp)
    val demiCiblePx = with(density) { cible.toPx() } / 2f
    val largeurPx = with(density) { largeurBox.toPx() }
    val hauteurPx = with(density) { hauteurBox.toPx() }
    val centreX = fx * largeurPx
    val centreY = fy * hauteurPx
    // Les gestes ci-dessous sont installés une seule fois (pointerInput) : ils lisent ces valeurs
    // à jour plutôt que celles du premier affichage — position, taille de la carte après un zoom,
    // et callbacks liés au point courant.
    val centreActuel by rememberUpdatedState(Offset(centreX, centreY))
    val tailleCarte by rememberUpdatedState(largeurPx to hauteurPx)
    val onTapActuel by rememberUpdatedState(onTap)
    val onDeplacerActuel by rememberUpdatedState(onDeplacer)

    val couleur = when {
        point.couleurArgb != null -> Color(point.couleurArgb)
        point.type == TypePointInteret.VILLE -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val icone = IconesPointInteret.iconePour(point.type, point.iconKey)
    val or = Color(0xFFFFD54F)

    // La zone de toucher (centrée sur la position du point) reste l'ancre ; le nom est posé
    // autour de l'icône sans décaler celle-ci, quel que soit le côté choisi.
    Box(
        modifier = Modifier
            .size(cible)
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    (centreX - demiCiblePx + dragOffset.x).roundToInt(),
                    (centreY - demiCiblePx + dragOffset.y).roundToInt()
                )
            }
            .pointerInput(point.id, modeMesure) {
                detectTapGestures { onTapActuel() }
            }
            .pointerInput(point.id, modeMesure, deplacable) {
                if (!modeMesure && deplacable) {
                    detectDragGestures(
                        onDragEnd = {
                            val (l, h) = tailleCarte
                            val c = centreActuel + dragOffset
                            if (dragOffset == Offset.Zero) return@detectDragGestures
                            onDeplacerActuel((c.x / l).coerceIn(0f, 1f), (c.y / h).coerceIn(0f, 1f))
                        },
                        onDragCancel = { dragOffset = Offset.Zero }
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(tailleIcone)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(couleur, CircleShape)
                        // Lieu choisi en mesure : anneau doré bien visible.
                        .then(if (selectionne) Modifier.border(3.dp, or, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icone,
                        contentDescription = point.nom,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(tailleIcone * 0.6f)
                    )
                }
                badgeGroupe?.let { badge ->
                    val tailleBadge = maxOf(tailleIcone * 0.5f, 18.dp)
                    val onTapBadge by rememberUpdatedState(badge.onTap)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = tailleBadge / 4, y = -tailleBadge / 4)
                            .size(tailleBadge)
                            .background(badge.couleur, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                            .pointerInput(point.id) { detectTapGestures { onTapBadge() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(badge.icone, contentDescription = "Groupe", tint = Color.White, modifier = Modifier.size(tailleBadge * 0.65f))
                    }
                }
            }
            marqueMesure?.let { lettre ->
                Box(
                    modifier = Modifier
                        // Coin haut droit pris par l'icône du groupe : numéro d'étape à gauche.
                        .align(if (badgeGroupe != null) Alignment.TopStart else Alignment.TopEnd)
                        .size(18.dp)
                        .background(or, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lettre, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
        if (afficherNoms || selectionne) {
            val largeurMaxPx = with(density) { maxOf(tailleBase * 3, 96.dp).roundToPx() }
            val demiIconePx = with(density) { (tailleIcone / 2).roundToPx() }
            Text(
                text = point.nom,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    // Taille nulle, centrée sur l'icône : le texte est placé à côté sans
                    // agrandir la zone de toucher ni déplacer l'icône.
                    .layout { measurable, _ ->
                        val p = measurable.measure(Constraints(maxWidth = largeurMaxPx))
                        layout(0, 0) {
                            val (x, y) = when (positionTexte) {
                                PositionTexte.BAS -> -p.width / 2 to demiIconePx
                                PositionTexte.HAUT -> -p.width / 2 to -demiIconePx - p.height
                                PositionTexte.GAUCHE -> -demiIconePx - p.width to -p.height / 2
                                PositionTexte.DROITE -> demiIconePx to -p.height / 2
                            }
                            p.place(x, y)
                        }
                    }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * Tap sur une case vide : soit placer ici un lieu existant non placé (ville créée depuis la
 * liste des villes, lieu d'une carte supprimée...), soit créer un nouveau point d'intérêt.
 */
@Composable
private fun NouveauPointDialog(
    pointsNonPlaces: List<PointInteret>,
    onDismiss: () -> Unit,
    onPlacerExistant: (PointInteret) -> Unit,
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (pointsNonPlaces.isNotEmpty()) {
                    Text("Placer ici un lieu existant", style = MaterialTheme.typography.titleSmall)
                    pointsNonPlaces.forEach { point ->
                        TextButton(onClick = { onPlacerExistant(point) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${point.nom} · ${point.type.label}", modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Ou créer un nouveau lieu", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                }
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PointDetailBottomSheet(
    point: PointInteret,
    toutesLesBoutiques: List<Boutique>,
    readOnly: Boolean = false,
    onDismiss: () -> Unit,
    onSupprimer: () -> Unit,
    onRetirerDeLaCarte: () -> Unit = {},
    onLierBoutique: (Boutique) -> Unit,
    onDelierBoutique: (Boutique) -> Unit,
    onPersonnaliser: (iconKey: String?, couleurArgb: Int?) -> Unit = { _, _ -> },
    onVisibiliteJoueurs: (Boolean) -> Unit = {},
    onChangerEvenements: (List<String>) -> Unit = {},
    reglage: ReglagePoint = ReglagePoint(),
    onReglage: (ReglagePoint) -> Unit = {}
) {
    var afficherSelectionBoutique by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(point.nom, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (!readOnly) {
                    // Retirer de la carte : le lieu est conservé (non placé), réutilisable ailleurs.
                    IconButton(onClick = onRetirerDeLaCarte) { Icon(Icons.Default.LocationOff, contentDescription = "Retirer de la carte") }
                    IconButton(onClick = onSupprimer) { Icon(Icons.Default.Delete, contentDescription = "Supprimer le point") }
                }
            }
            Text(point.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (point.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(point.description)
            }

            if (!readOnly) {
                Spacer(Modifier.height(12.dp))
                // Aussi réglable depuis l'exploration (barre « Lieux »).
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visible des joueurs", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Affiché sur la carte d'exploration (écran de table).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = point.visibleJoueurs, onCheckedChange = onVisibiliteJoueurs)
                }
                Spacer(Modifier.height(16.dp))
                Text("Icône", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconesPointInteret.PALETTE.forEach { (cle, icone) ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (point.iconKey == cle) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .clickable { onPersonnaliser(cle, point.couleurArgb) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icone, contentDescription = IconesPointInteret.LIBELLES[cle] ?: cle, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Couleur", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETTE_GRILLE.forEach { argb ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(argb), CircleShape)
                                .border(
                                    width = if (point.couleurArgb == argb) 3.dp else 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onPersonnaliser(point.iconKey, argb) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Taille de l'icône : ${(reglage.taille * 100).roundToInt()} %", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = reglage.taille,
                    onValueChange = { onReglage(reglage.copy(taille = (it * 4).roundToInt() / 4f)) },
                    valueRange = 0.5f..3f,
                    steps = 9 // pas de 25 %
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Verrouiller l'emplacement", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = reglage.verrouille, onCheckedChange = { onReglage(reglage.copy(verrouille = it)) })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Afficher le nom", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = reglage.afficherTexte, onCheckedChange = { onReglage(reglage.copy(afficherTexte = it)) })
                }
                if (reglage.afficherTexte) {
                    Text("Position du nom", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PositionTexte.entries.forEach { position ->
                            FilterChip(
                                selected = reglage.positionTexte == position,
                                onClick = { onReglage(reglage.copy(positionTexte = position)) },
                                label = { Text(position.label) }
                            )
                        }
                    }
                }
            }

            if (point.type == TypePointInteret.VILLE) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Boutiques liées", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (!readOnly) {
                        IconButton(onClick = { afficherSelectionBoutique = true }) { Icon(Icons.Default.Add, contentDescription = "Lier une boutique") }
                    }
                }
                val boutiquesLiees = toutesLesBoutiques.filter { it.id in point.boutiqueIds }
                if (boutiquesLiees.isEmpty()) {
                    Text("Aucune boutique liée", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                boutiquesLiees.forEach { boutique ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(boutique.nom, modifier = Modifier.weight(1f))
                        if (!readOnly) {
                            IconButton(onClick = { onDelierBoutique(boutique) }) { Icon(Icons.Default.Close, contentDescription = "Délier ${boutique.nom}") }
                        }
                    }
                }
            }

            // Événements de la bibliothèque rattachés au lieu, tous types de lieux confondus
            // (préparation du MJ : non montrés aux joueurs).
            if (!readOnly) {
                Spacer(Modifier.height(16.dp))
                EvenementsLiesSection(
                    worldId = EvenementDependencies.mondeDeCampagne(point.campagneId),
                    campagneId = point.campagneId,
                    evenementIds = point.evenementIds,
                    onChanger = onChangerEvenements
                )
            }
        }
    }

    if (afficherSelectionBoutique && !readOnly) {
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

/** Petite icône du groupe posée sur le lieu où il se trouve (voir PointSurCarte). */
private data class BadgeGroupe(val icone: androidx.compose.ui.graphics.vector.ImageVector, val couleur: Color, val onTap: () -> Unit)

/** Icône et couleur du groupe sur la carte, choisies comme pour un lieu. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GroupeIconeSheet(
    nom: String,
    iconKey: String?,
    couleurArgb: Int?,
    onChoix: (iconKey: String?, couleurArgb: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Reflet immédiat du choix (le groupe mis à jour revient ensuite par GameState).
    var cle by remember { mutableStateOf(iconKey) }
    var couleur by remember { mutableStateOf(couleurArgb) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text(nom, style = MaterialTheme.typography.titleLarge)
            Text("Icône du groupe sur la carte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("Icône", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconesPointInteret.PALETTE_GROUPE.forEach { (k, icone) ->
                    val choisie = (cle ?: "groupe") == k
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (choisie) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .clickable { cle = k; onChoix(k, couleur) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icone, contentDescription = IconesPointInteret.LIBELLES[k] ?: k, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Couleur", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf(IconesPointInteret.COULEUR_GROUPE_DEFAUT) + PALETTE_GRILLE).forEach { argb ->
                    val choisie = (couleur ?: IconesPointInteret.COULEUR_GROUPE_DEFAUT) == argb
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(argb), CircleShape)
                            .border(if (choisie) 3.dp else 1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { couleur = argb; onChoix(cle, argb) }
                    )
                }
            }
        }
    }
}

/**
 * Marqueur rond centré en ([fx], [fy]) (fractions de la carte) avec une icône et un libellé
 * dessous : icône « compagnie » du groupe, haltes d'un trajet.
 */
@Composable
private fun MarqueurSurCarte(
    fx: Float,
    fy: Float,
    largeurBox: Dp,
    hauteurBox: Dp,
    taille: Dp,
    couleur: Color,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    libelle: String,
    // Faux : [libelle] ne sert que de description d'accessibilité (icône du groupe, sans son nom).
    afficherLibelle: Boolean = true,
    onTap: (() -> Unit)? = null,
    // Non null : le marqueur se glisse ; reçoit la nouvelle position (fractions de la carte).
    onDeplacer: ((Float, Float) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val demiPx = with(density) { taille.toPx() } / 2f
    val largeurPx = with(density) { largeurBox.toPx() }
    val hauteurPx = with(density) { hauteurBox.toPx() }
    val x = fx * largeurPx
    val y = fy * hauteurPx
    val onTapActuel by rememberUpdatedState(onTap)
    val onDeplacerActuel by rememberUpdatedState(onDeplacer)
    val centreActuel by rememberUpdatedState(Offset(x, y))
    val tailleCarte by rememberUpdatedState(largeurPx to hauteurPx)
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .size(taille)
            .offset { androidx.compose.ui.unit.IntOffset((x - demiPx + dragOffset.x).roundToInt(), (y - demiPx + dragOffset.y).roundToInt()) }
            .then(if (onTap != null) Modifier.pointerInput(Unit) { detectTapGestures { onTapActuel?.invoke() } } else Modifier)
            .then(
                if (onDeplacer != null) Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragOffset == Offset.Zero) return@detectDragGestures
                            val (l, h) = tailleCarte
                            val c = centreActuel + dragOffset
                            onDeplacerActuel?.invoke((c.x / l).coerceIn(0f, 1f), (c.y / h).coerceIn(0f, 1f))
                            // Position du groupe mise à jour de façon synchrone (GameState) : pas
                            // d'attente comme pour un lieu, et un lâcher sur la même place revient bien.
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = { dragOffset = Offset.Zero }
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    }
                } else Modifier
            )
            .background(couleur, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icone, contentDescription = libelle, tint = Color.White, modifier = Modifier.size(taille * 0.6f))
        if (!afficherLibelle) return@Box
        val demiTaillePx = with(density) { (taille / 2).roundToPx() }
        Text(
            libelle,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .layout { measurable, _ ->
                    val p = measurable.measure(Constraints())
                    layout(0, 0) { p.place(-p.width / 2, demiTaillePx + 2) }
                }
                .background(couleur.copy(alpha = 0.85f))
                .padding(horizontal = 3.dp)
        )
    }
}

/**
 * Résultat d'un tracé : distance, vitesse (celle du membre le plus lent du groupe, ou une allure
 * choisie), heures de marche max par jour, et haltes de fin de journée. Pour le MJ, chaque halte
 * et l'arrivée proposent un événement et un repos long ; l'arrivée permet d'y déplacer le groupe.
 */
@Composable
private fun TrajetResultat(
    plan: PlanTrajet,
    groupe: com.jc2.jdrcompagnon.network.GroupeJoueurData?,
    choixVitesse: VitesseDeplacement?,
    onChoixVitesse: (VitesseDeplacement?) -> Unit,
    heuresMax: Int,
    onHeuresMax: (Int) -> Unit,
    arrivee: String,
    outilsMj: Boolean,
    onEvenement: (String) -> Unit,
    onRepos: (String) -> Unit,
    onDeplacerGroupe: (() -> Unit)?,
) {
    fun f1(v: Double) = String.format("%.1f", v)
    var menuVitesse by remember { mutableStateOf(false) }
    val vitesseGroupe = groupe?.vitesseM
    Box {
        TextButton(onClick = { menuVitesse = true }) {
            Text(
                if (choixVitesse == null && vitesseGroupe != null)
                    "Vitesse du groupe : ${f1(plan.kmParHeure)} km/h (le plus lent : ${groupe.plusLent}, $vitesseGroupe m)"
                else "Allure : ${(choixVitesse ?: VitesseDeplacement.A_PIED).label} (${f1(plan.kmParHeure)} km/h)"
            )
        }
        DropdownMenu(expanded = menuVitesse, onDismissRequest = { menuVitesse = false }) {
            if (vitesseGroupe != null) {
                DropdownMenuItem(text = { Text("Vitesse du groupe (membre le plus lent)") }, onClick = { onChoixVitesse(null); menuVitesse = false })
            }
            VitesseDeplacement.entries.forEach { v ->
                DropdownMenuItem(text = { Text("${v.label} (${v.kmParJour} km/j)") }, onClick = { onChoixVitesse(v); menuVitesse = false })
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Marche max par jour : $heuresMax h", modifier = Modifier.weight(1f))
        TextButton(onClick = { onHeuresMax((heuresMax - 1).coerceAtLeast(1)) }, enabled = heuresMax > 1) { Text("−") }
        TextButton(onClick = { onHeuresMax((heuresMax + 1).coerceAtMost(16)) }, enabled = heuresMax < 16) { Text("+") }
    }
    Text(
        "${plan.distanceKm.roundToInt()} km · ${f1(plan.heures)} h de marche · ${plan.jours} jour(s) (${plan.kmParJour.roundToInt()} km/jour)",
        fontWeight = FontWeight.Bold
    )
    Text(
        when (plan.arrets.size) {
            0 -> "Aucune halte : arrivée dans la journée."
            1 -> "1 halte pour la nuit en chemin."
            else -> "${plan.arrets.size} haltes pour la nuit en chemin."
        }
    )
    plan.arrets.forEach { arret ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🌙 Halte ${arret.numero} · fin du jour ${arret.numero} · ${arret.kmParcourus.roundToInt()} km parcourus",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (outilsMj) {
                TextButton(onClick = { onEvenement("Halte ${arret.numero} (fin du jour ${arret.numero}, ${arret.kmParcourus.roundToInt()} km parcourus)") }) { Text("Événement") }
                TextButton(onClick = { onRepos("halte ${arret.numero}") }) { Text("Repos") }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🏁 Arrivée : $arrivee", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (outilsMj) {
            TextButton(onClick = { onEvenement("Arrivée : $arrivee") }) { Text("Événement") }
            TextButton(onClick = { onRepos(arrivee) }) { Text("Repos") }
        }
    }
    if (onDeplacerGroupe != null) {
        OutlinedButtonCompat(text = "Déplacer le groupe à l'arrivée", onClick = onDeplacerGroupe)
    }
}

@Composable
private fun OutlinedButtonCompat(text: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Groups, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

/**
 * Affiche l'image de fond de [carte] sur la page d'affichage table (MjWebServer), comme une photo
 * envoyée par le MJ : elle remplace ce qui y était montré. Un message indique le résultat.
 */
internal fun envoyerCarteALaTable(context: android.content.Context, carte: CarteCampagne) {
    val message = when {
        carte.imageFileName == null -> "Cette carte n'a pas d'image à envoyer."
        !NetworkSessionManager.isTableDisplayOnline -> "Écran de table hors ligne : lancez la partie (Connexion) pour l'afficher."
        NetworkSessionManager.sendImageFileToWeb(
            CarteImageStore.fichier(context, carte.imageFileName),
            carte.nom.ifBlank { "Carte" },
        ) -> "Carte « ${carte.nom.ifBlank { "Carte" }} » envoyée à l'écran de table."
        else -> "Impossible d'envoyer la carte à l'écran de table."
    }
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}
