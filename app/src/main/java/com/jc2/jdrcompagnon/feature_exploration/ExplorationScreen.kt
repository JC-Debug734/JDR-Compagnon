package com.jc2.jdrcompagnon.feature_exploration

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlin.math.roundToInt

/**
 * Écran d'exploration du MJ, affiché par-dessus toute l'app quand [ExplorationSession.ecranOuvert]
 * (ouvert par ExplorationSession.ouvrirEcran depuis une carte de campagne ou une image de
 * scénario). Monté une seule fois dans le NavGraph, comme DiceOverlay : une fenêtre de dialogue
 * plein écran était mesurée sur toute la hauteur de l'écran tout en étant placée sous la barre
 * d'état, ce qui faisait passer la barre des zones sous le bord de l'écran.
 */
@Composable
fun ExplorationOverlay() {
    val ouvert by ExplorationSession.ecranOuvert.collectAsState()
    if (!ouvert) return
    BackHandler { ExplorationSession.fermerEcran() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C14))
            // Absorbe les touchers : rien ne doit atteindre l'écran situé dessous.
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ExplorationContent(onClose = { ExplorationSession.fermerEcran() })
    }
}

/** Couleur des cases de la zone en cours de préparation (vue MJ uniquement). */
private val CouleurZoneEdition = Color(0xFFD4AF37)
/** Cases déjà rattachées à une autre zone, pour repérer ce qui reste à découper. */
private val CouleurAutresZones = Color(0xFF64B5F6)

@Composable
private fun ExplorationContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val etat by ExplorationSession.etat.collectAsState()
    val e = etat ?: return
    var menuOuvert by remember { mutableStateOf(false) }
    var reglageGrille by remember { mutableStateOf(false) }
    // Zone en cours de préparation (création ou modification) : les touchers ajoutent ou retirent
    // ses cases au lieu de révéler la carte. null = mode jeu.
    var zoneEdition by remember(e.cle) { mutableStateOf<ZoneExploration?>(null) }
    var nomNouvelleZone by remember { mutableStateOf<String?>(null) }
    var zoneRenommee by remember { mutableStateOf<ZoneExploration?>(null) }
    // Toucher une case : dévoile toute sa zone (par défaut) ou seulement la case.
    var parZone by remember { mutableStateOf(true) }
    val tableEnLigne = NetworkSessionManager.isTableDisplayOnline
    val titreSurTable by ExplorationSession.titreSurTable.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0C0C14))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White) }
            Column(modifier = Modifier.weight(1f)) {
                Text(e.titre, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${e.revelees.size} / ${e.colonnes * e.lignes} cases révélées",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            FilterChip(
                selected = e.afficheeSurTable,
                onClick = { ExplorationSession.afficherSurTable(context, !e.afficheeSurTable) },
                label = { Text(if (e.afficheeSurTable) "Sur la table" else "Envoyer à la table") },
                leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = Color.White,
                    iconColor = Color.White,
                    selectedContainerColor = ForcedDarkPalette.AccentGold,
                    selectedLabelColor = ForcedDarkPalette.Background,
                    selectedLeadingIconColor = ForcedDarkPalette.Background,
                )
            )
            Box {
                IconButton(onClick = { menuOuvert = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White) }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(text = { Text("Tout révéler") }, onClick = { ExplorationSession.toutReveler(context); menuOuvert = false })
                    DropdownMenuItem(text = { Text("Tout masquer") }, onClick = { ExplorationSession.toutMasquer(context); menuOuvert = false })
                    DropdownMenuItem(text = { Text("Taille des cases…") }, onClick = { reglageGrille = true; menuOuvert = false })
                }
            }
        }

        val edition = zoneEdition
        e.zones.firstOrNull { it.id == e.zoomZoneId }?.let { zoomee ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔍 Table zoomée sur « ${zoomee.nom} » (double appui sur une zone révélée pour zoomer)",
                    color = ForcedDarkPalette.AccentGold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { ExplorationSession.zoomerSurZone(context, null) }) { Text("Dézoomer", color = Color.White) }
            }
        }
        if (edition != null) {
            // Bandeau de préparation d'une zone.
            Surface(color = CouleurZoneEdition.copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Zone « ${edition.nom} » : touchez les cases à inclure (${edition.cases.size}). Appui long + glisser pour en peindre plusieurs.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { zoneEdition = null }) { Text("Annuler", color = Color.White) }
                    Button(
                        onClick = { ExplorationSession.enregistrerZone(context, edition); zoneEdition = null },
                        enabled = edition.cases.isNotEmpty()
                    ) { Text("Enregistrer") }
                }
            }
        } else if (!e.afficheeSurTable && titreSurTable != null) {
            // Une autre carte est sur la table : l'envoyer la remplace par celle-ci (et son brouillard).
            Text(
                "Sur la table : « $titreSurTable ». Touchez « Envoyer à la table » pour la remplacer par cette carte, sous son brouillard.",
                color = ForcedDarkPalette.AccentGold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        } else if (!tableEnLigne && e.afficheeSurTable) {
            // Seul message conservé : sans lui, le MJ croirait la carte visible des joueurs.
            Text(
                "La partie n'est pas hébergée : la carte apparaîtra sur la table dès que le serveur sera lancé (Connexion).",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        CarteBrouillard(
            e = e,
            casesZoneEdition = edition?.cases,
            casesAutresZones = e.zones.filter { it.id != edition?.id }.flatMap { it.cases }.toSet(),
            onToucherCase = { index ->
                val z = zoneEdition
                val zoneDeLaCase = if (parZone) ExplorationGrille.zonePourCase(e.zones, index) else null
                when {
                    z != null -> zoneEdition = z.copy(cases = if (index in z.cases) z.cases - index else z.cases + index)
                    // Mode « par zone » : toucher n'importe quelle case d'une zone la dévoile entière
                    // (un toucher ne remasque jamais : menu de la zone ou appui long + glisser).
                    zoneDeLaCase != null -> ExplorationSession.revelerZone(context, zoneDeLaCase.id)
                    else -> ExplorationSession.revelerCase(context, index)
                }
            },
            onDoubleToucherCase = { index ->
                // Double appui sur une zone révélée : la page table zoome dessus (nouveau double
                // appui : retour à la carte entière).
                val zone = ExplorationGrille.zonePourCase(e.zones, index)
                when {
                    zoneEdition != null -> Unit
                    zone != null && e.estRevelee(zone) -> ExplorationSession.zoomerSurZone(context, zone.id)
                    e.zoomZoneId != null -> ExplorationSession.zoomerSurZone(context, null)
                }
            },
            onPeindre = { index, ajouter ->
                val z = zoneEdition
                if (z != null) {
                    zoneEdition = z.copy(cases = if (ajouter) z.cases + index else z.cases - index)
                } else {
                    ExplorationSession.definir(context, listOf(index), ajouter)
                }
            },
            estActive = { index ->
                val z = zoneEdition
                if (z != null) index in z.cases else index in (ExplorationSession.etat.value?.revelees ?: emptySet())
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        if (edition == null && e.lieux.isNotEmpty()) {
            BarreLieux(lieux = e.lieux, onBasculer = { ExplorationSession.basculerLieu(context, it.id) })
        }

        BarreZones(
            e = e,
            enEdition = edition != null,
            parZone = parZone,
            onParZone = { parZone = it },
            onBasculer = { ExplorationSession.revelerZone(context, it.id) },
            onRemasquer = { ExplorationSession.remasquerZone(context, it.id) },
            onZoomer = { ExplorationSession.zoomerSurZone(context, it.id) },
            onNouvelle = { nomNouvelleZone = "Zone ${e.zones.size + 1}" },
            onModifierCases = { zoneEdition = it },
            onRenommer = { zoneRenommee = it },
            onSupprimer = { ExplorationSession.supprimerZone(context, it.id) },
        )
    }

    nomNouvelleZone?.let { nomInitial ->
        NomZoneDialog(
            titre = "Nouvelle zone",
            nomInitial = nomInitial,
            onDismiss = { nomNouvelleZone = null },
            onValider = { nom ->
                nomNouvelleZone = null
                zoneEdition = ZoneExploration(nom = nom, cases = emptySet())
            }
        )
    }

    zoneRenommee?.let { zone ->
        NomZoneDialog(
            titre = "Renommer la zone",
            nomInitial = zone.nom,
            onDismiss = { zoneRenommee = null },
            onValider = { nom ->
                zoneRenommee = null
                ExplorationSession.enregistrerZone(context, zone.copy(nom = nom))
            }
        )
    }

    if (reglageGrille) {
        val plage = ExplorationSession.PLAGE_COLONNES
        // Saisie libre du nombre de cases en largeur, avec boutons −/+ pour ajuster d'une unité.
        var saisie by remember { mutableStateOf(e.colonnes.toString()) }
        val colonnes = saisie.toIntOrNull()
        val valide = colonnes != null && colonnes in plage
        AlertDialog(
            onDismissRequest = { reglageGrille = false },
            title = { Text("Taille des cases") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { saisie = ((colonnes ?: e.colonnes) - 1).coerceIn(plage).toString() },
                            enabled = (colonnes ?: e.colonnes) > plage.first
                        ) { Text("−") }
                        OutlinedTextField(
                            value = saisie,
                            onValueChange = { v -> saisie = v.filter { it.isDigit() }.take(3) },
                            label = { Text("Cases en largeur") },
                            singleLine = true,
                            isError = !valide,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { saisie = ((colonnes ?: e.colonnes) + 1).coerceIn(plage).toString() },
                            enabled = (colonnes ?: e.colonnes) < plage.last
                        ) { Text("+") }
                    }
                    Text(
                        "Entre ${plage.first} et ${plage.last} cases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (valide) Color.Unspecified else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Les cases révélées et les zones sont adaptées à la nouvelle grille.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        colonnes?.let { ExplorationSession.changerColonnes(context, it) }
                        reglageGrille = false
                    },
                    enabled = valide
                ) { Text("Appliquer") }
            },
            dismissButton = { TextButton(onClick = { reglageGrille = false }) { Text("Annuler") } }
        )
    }
}

/**
 * Zones préparées, en bas de l'écran : un appui dévoile la zone entière (ou la remasque si elle
 * était déjà visible), un appui long ouvre ses options. Dorée = zone visible des joueurs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarreZones(
    e: ExplorationEtat,
    enEdition: Boolean,
    parZone: Boolean,
    onParZone: (Boolean) -> Unit,
    onBasculer: (ZoneExploration) -> Unit,
    onRemasquer: (ZoneExploration) -> Unit,
    onZoomer: (ZoneExploration) -> Unit,
    onNouvelle: () -> Unit,
    onModifierCases: (ZoneExploration) -> Unit,
    onRenommer: (ZoneExploration) -> Unit,
    onSupprimer: (ZoneExploration) -> Unit,
) {
    var menuZone by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14141E))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!enEdition && e.zones.isNotEmpty()) {
            // Ce que fait un toucher sur la carte : toute la zone de la case, ou la case seule.
            FilterChip(
                selected = parZone,
                onClick = { onParZone(!parZone) },
                label = { Text(if (parZone) "Toucher : zone entière" else "Toucher : case seule") },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = Color.White,
                    selectedContainerColor = Color.White.copy(alpha = 0.2f),
                    selectedLabelColor = Color.White,
                )
            )
        }
        if (e.zones.isEmpty()) {
            Text(
                "Aucune zone : créez-en pour tout dévoiler d'un clic →",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        e.zones.forEach { zone ->
            val revelee = e.estRevelee(zone)
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (revelee) ForcedDarkPalette.AccentGold else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (revelee) ForcedDarkPalette.AccentGold else Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.combinedClickable(
                        enabled = !enEdition,
                        onClick = { onBasculer(zone) },
                        onLongClick = { menuZone = zone.id }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (revelee) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (revelee) "Zone dévoilée" else "Zone cachée",
                            tint = if (revelee) ForcedDarkPalette.Background else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(zone.nom, color = if (revelee) ForcedDarkPalette.Background else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                DropdownMenu(expanded = menuZone == zone.id, onDismissRequest = { menuZone = null }) {
                    if (revelee) {
                        DropdownMenuItem(
                            text = { Text(if (e.zoomZoneId == zone.id) "Dézoomer la table" else "Zoomer la table dessus") },
                            onClick = { menuZone = null; onZoomer(zone) }
                        )
                        DropdownMenuItem(text = { Text("Remettre le brouillard") }, onClick = { menuZone = null; onRemasquer(zone) })
                    }
                    DropdownMenuItem(text = { Text("Modifier les cases") }, onClick = { menuZone = null; onModifierCases(zone) })
                    DropdownMenuItem(text = { Text("Renommer") }, onClick = { menuZone = null; onRenommer(zone) })
                    DropdownMenuItem(
                        text = { Text("Supprimer la zone", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuZone = null; onSupprimer(zone) }
                    )
                }
            }
        }
        if (!enEdition) {
            OutlinedButton(onClick = onNouvelle) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Zone", color = Color.White)
            }
        }
    }
}

/**
 * Lieux de la carte de campagne : un appui montre le lieu aux joueurs sur la table (doré, œil
 * ouvert) ou le leur cache.
 */
@Composable
private fun BarreLieux(lieux: List<LieuExploration>, onBasculer: (LieuExploration) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14141E))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Lieux", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
        lieux.forEach { lieu ->
            Surface(
                onClick = { onBasculer(lieu) },
                shape = RoundedCornerShape(20.dp),
                color = if (lieu.visible) ForcedDarkPalette.AccentGold else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (lieu.visible) ForcedDarkPalette.AccentGold else Color.White.copy(alpha = 0.4f)),
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (lieu.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (lieu.visible) "Visible des joueurs" else "Caché aux joueurs",
                        tint = if (lieu.visible) ForcedDarkPalette.Background else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        lieu.emoji + " " + lieu.nom,
                        color = if (lieu.visible) ForcedDarkPalette.Background else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NomZoneDialog(titre: String, nomInitial: String, onDismiss: () -> Unit, onValider: (String) -> Unit) {
    var nom by remember { mutableStateOf(nomInitial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre) },
        text = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom (ex : Cuisine, Étang, Cave...)") },
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onValider(nom.trim()) }, enabled = nom.isNotBlank()) { Text("Valider") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * Carte vue par le MJ : les cases masquées sont assombries (le MJ voit à travers, les joueurs
 * non), la grille est tracée en léger. En préparation de zone, les cases de la zone sont dorées
 * et celles des autres zones bleutées. Zoom et déplacement à deux doigts.
 */
@Composable
private fun CarteBrouillard(
    e: ExplorationEtat,
    casesZoneEdition: Set<Int>?,
    casesAutresZones: Set<Int>,
    onToucherCase: (Int) -> Unit,
    onDoubleToucherCase: (Int) -> Unit,
    onPeindre: (index: Int, ajouter: Boolean) -> Unit,
    estActive: (Int) -> Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val bitmap = remember(e.imageFile) {
        runCatching { BitmapFactory.decodeFile(e.imageFile.absolutePath)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Image introuvable", color = Color.White) }
        return
    }
    var zoom by remember(e.cle) { mutableFloatStateOf(1f) }
    var decalage by remember(e.cle) { mutableStateOf(Offset.Zero) }
    // Les gestes restent installés d'un changement d'état à l'autre : ils lisent toujours la
    // dernière version des callbacks (mode jeu / préparation de zone).
    val toucher by rememberUpdatedState(onToucherCase)
    val doubleToucher by rememberUpdatedState(onDoubleToucherCase)
    val peindre by rememberUpdatedState(onPeindre)
    val active by rememberUpdatedState(estActive)

    BoxWithConstraints(
        modifier = modifier.pointerInput(e.cle) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                zoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                decalage = if (zoom == 1f) Offset.Zero else decalage + pan
            }
        },
        contentAlignment = Alignment.Center
    ) {
        // Taille de la carte ajustée à l'écran, proportions de l'image conservées.
        val ratio = bitmap.width.toFloat() / bitmap.height
        val maxL = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val largeurPx = if (maxL / ratio <= maxH) maxL else maxH * ratio
        val hauteurPx = largeurPx / ratio
        val taille = with(density) { DpSize(largeurPx.toDp(), hauteurPx.toDp()) }

        fun caseSous(position: Offset): Int {
            val c = (position.x / largeurPx * e.colonnes).toInt().coerceIn(0, e.colonnes - 1)
            val l = (position.y / hauteurPx * e.lignes).toInt().coerceIn(0, e.lignes - 1)
            return l * e.colonnes + c
        }

        Box(
            modifier = Modifier
                .size(taille)
                .graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = decalage.x, translationY = decalage.y)
                .pointerInput(e.cle, e.colonnes, e.lignes) {
                    detectTapGestures(
                        onDoubleTap = { pos -> doubleToucher(caseSous(pos)) },
                        onTap = { pos -> toucher(caseSous(pos)) },
                    )
                }
                .pointerInput(e.cle, e.colonnes, e.lignes) {
                    // Glissé après appui long : même action (ajouter ou retirer) que sur la
                    // première case touchée, appliquée à toutes les cases parcourues.
                    var ajouter = true
                    detectDragGesturesAfterLongPress(
                        onDragStart = { pos ->
                            val i = caseSous(pos)
                            ajouter = !active(i)
                            peindre(i, ajouter)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            peindre(caseSous(change.position), ajouter)
                        }
                    )
                }
        ) {
            Image(bitmap = bitmap, contentDescription = e.titre, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width / e.colonnes
                val ch = size.height / e.lignes
                val voile = Color.Black.copy(alpha = 0.6f)
                for (l in 0 until e.lignes) {
                    // Cases masquées consécutives d'une ligne dessinées d'un seul rectangle : la
                    // grille peut compter des centaines de colonnes.
                    var debutVoile = -1
                    for (c in 0..e.colonnes) {
                        val masquee = c < e.colonnes && e.index(c, l) !in e.revelees
                        if (masquee && debutVoile < 0) debutVoile = c
                        if (!masquee && debutVoile >= 0) {
                            drawRect(voile, topLeft = Offset(debutVoile * cw, l * ch), size = Size((c - debutVoile) * cw, ch))
                            debutVoile = -1
                        }
                    }
                    if (casesZoneEdition == null) continue
                    for (c in 0 until e.colonnes) {
                        val i = e.index(c, l)
                        val coin = Offset(c * cw, l * ch)
                        val taillecase = Size(cw, ch)
                        when {
                            i in casesZoneEdition -> drawRect(CouleurZoneEdition.copy(alpha = 0.45f), topLeft = coin, size = taillecase)
                            i in casesAutresZones -> drawRect(CouleurAutresZones.copy(alpha = 0.25f), topLeft = coin, size = taillecase)
                        }
                    }
                }
                val trait = Color.White.copy(alpha = 0.2f)
                // Cases minuscules (grille très fine) : les traits cacheraient toute la carte.
                if (cw >= 6f) for (c in 1 until e.colonnes) drawLine(trait, Offset(c * cw, 0f), Offset(c * cw, size.height), strokeWidth = 1f)
                if (ch >= 6f) for (l in 1 until e.lignes) drawLine(trait, Offset(0f, l * ch), Offset(size.width, l * ch), strokeWidth = 1f)
            }
            // Lieux de la carte : bien visibles quand les joueurs les voient, estompés sinon.
            val diametre = 28.dp
            e.lieux.forEach { lieu ->
                Box(
                    modifier = Modifier
                        .offset {
                            val r = diametre.roundToPx() / 2
                            androidx.compose.ui.unit.IntOffset((lieu.fx * largeurPx).roundToInt() - r, (lieu.fy * hauteurPx).roundToInt() - r)
                        }
                        .size(diametre)
                        .graphicsLayer(alpha = if (lieu.visible) 1f else 0.45f)
                        .background(lieu.couleurArgb?.let { Color(it) } ?: Color(0xFF8D6E63), CircleShape)
                        .border(2.dp, if (lieu.visible) ForcedDarkPalette.AccentGold else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lieu.emoji, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
