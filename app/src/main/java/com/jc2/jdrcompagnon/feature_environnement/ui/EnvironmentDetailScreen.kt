package com.jc2.jdrcompagnon.feature_environnement.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementsLiesSection
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.components.forgetCollapsibleSections
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.echantillonnerSansRemise
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TERRAINS_SRD
import com.jc2.jdrcompagnon.feature_environnement.domain.model.rencontrableDans
import com.jc2.jdrcompagnon.feature_environnement.domain.model.terrainsEffectifs
import com.jc2.jdrcompagnon.feature_environnement.domain.model.LootEntry
import com.jc2.jdrcompagnon.feature_environnement.domain.usecase.catalogueEpreuves
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironmentImageStore
import com.jc2.jdrcompagnon.feature_environnement.presentation.EnvironmentDetailViewModel
import com.jc2.jdrcompagnon.feature_environnement.presentation.EpreuveSession
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import com.jc2.jdrcompagnon.ui.screens.mj.challengeSortKey
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.monsterChallenge
import com.jc2.jdrcompagnon.ui.screens.mj.monsterChallengeLabel
import kotlinx.coroutines.launch
import kotlin.random.Random

// Getter (et non valeur figée) : inclut les musiques importées depuis l'écran Musique.
private val MusicTracks: List<Pair<String, String>>
    get() = listOf("Aucune" to "") + availableLoopTracks.map { it.displayName to it.id }

/**
 * Écran d'édition d'un environnement : nom/description, image, musique, événements, tables
 * aléatoires, épreuves environnementales, bestiaire (suggéré + ajout manuel) et table de butin
 * (avec tirage pondéré).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EnvironmentDetailScreen(
    viewModel: EnvironmentDetailViewModel,
    worldId: String?,
    onOpenBestiaryDetail: (String) -> Unit,
    onOpenEquipmentDetail: (String) -> Unit,
    onOuvrirEpreuve: () -> Unit = {},
    onOpenTableAleatoire: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val environnement by viewModel.environnement.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val choisirImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val current = environnement ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val fileName = EnvironmentImageStore.copier(context, uri, current.id)
            if (fileName != null) viewModel.mettreAJour(current.copy(imageFileName = fileName))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(environnement?.nom ?: "Environnement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val current = environnement
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                Text("Chargement...", color = Color.White)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = current.nom,
                onValueChange = { viewModel.mettreAJour(current.copy(nom = it)) },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = environmentFieldColors()
            )
            OutlinedTextField(
                value = current.description,
                onValueChange = { viewModel.mettreAJour(current.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = environmentFieldColors()
            )

            SubsectionCard(current.id, "Image") {
                val bitmap = current.imageFileName?.let { fileName ->
                    remember(fileName) {
                        runCatching {
                            val file = EnvironmentImageStore.fichier(context, fileName)
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                        }.getOrNull()
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Image de l'environnement",
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                var galerieOuverte by remember { mutableStateOf(false) }
                if (galerieOuverte) {
                    GalerieImagesDialog(
                        onChoisir = { assetName ->
                            galerieOuverte = false
                            val fileName = EnvironmentImageStore.copierDepuisAsset(context, assetName, current.id, current.imageFileName)
                            if (fileName != null) viewModel.mettreAJour(current.copy(imageFileName = fileName))
                        },
                        onDismiss = { galerieOuverte = false }
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { galerieOuverte = true }) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galerie")
                    }
                    OutlinedButton(onClick = { choisirImageLauncher.launch("image/*") }) {
                        Text(if (current.imageFileName == null) "Depuis l'appareil" else "Changer l'image")
                    }
                    if (current.imageFileName != null) {
                        OutlinedButton(onClick = { viewModel.mettreAJour(current.copy(imageFileName = null)) }) {
                            Text("Retirer")
                        }
                    }
                }
            }

            SubsectionCard(current.id, "Musique") {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = MusicTracks.find { it.second == current.musicTrackId }?.first ?: "Aucune",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Piste d'ambiance") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MusicTracks.forEach { (label, trackId) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.mettreAJour(current.copy(musicTrackId = trackId.ifBlank { null }))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Rumeurs, rencontres, périls... du lieu : événements de la bibliothèque (les anciennes
            // rumeurs et rencontres en texte y ont été converties, voir EnvironnementRepositoryImpl).
            SubsectionCard(current.id, "Événements") {
                EvenementsLiesSection(
                    worldId = worldId ?: current.worldId,
                    campagneId = null,
                    evenementIds = current.evenementIds,
                    onChanger = { viewModel.mettreAJour(current.copy(evenementIds = it)) },
                    titre = null
                )
            }

            SubsectionCard(current.id, "Tables aléatoires") {
                TablesAleatoiresSection(
                    environnement = current,
                    worldId = worldId,
                    onEnvironnementChanged = viewModel::mettreAJour,
                    onOpenTable = onOpenTableAleatoire
                )
            }

            SubsectionCard(current.id, "Épreuves environnementales") {
                EpreuvesSection(
                    epreuves = current.epreuves,
                    onEpreuvesChanged = { viewModel.mettreAJour(current.copy(epreuves = it)) },
                    onOuvrirEpreuve = onOuvrirEpreuve
                )
            }

            SubsectionCard(current.id, "Bestiaire") {
                BestiaireSection(
                    environnement = current,
                    worldId = worldId,
                    onOpenBestiaryDetail = onOpenBestiaryDetail,
                    onEnvironnementChanged = viewModel::mettreAJour
                )
            }

            SubsectionCard(current.id, "Table de butin") {
                LootSection(
                    tableButin = current.tableButin,
                    worldId = worldId,
                    onOpenEquipmentDetail = onOpenEquipmentDetail,
                    onTableButinChanged = { viewModel.mettreAJour(current.copy(tableButin = it)) },
                    onTirage = { resultat ->
                        coroutineScope.launch { snackbarHostState.showSnackbar("Butin tiré : $resultat") }
                    }
                )
            }
        }
    }
}

/** Oublie la configuration des cartes d'un environnement supprimé. */
internal fun forgetEnvironmentSections(context: android.content.Context, environmentId: String) =
    forgetCollapsibleSections(context, "environnement:$environmentId")

/**
 * Carte de section de l'environnement (voir [CollapsibleSectionCard]) : état replié propre à
 * chaque environnement, persisté.
 */
@Composable
private fun SubsectionCard(environmentId: String, title: String, content: @Composable () -> Unit) =
    CollapsibleSectionCard(ownerKey = "environnement:$environmentId", title = title) { content() }

/** Champs de saisie de l'environnement : fond translucide (comme les cartes), texte blanc. */
@Composable
private fun environmentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    cursorColor = Color.White,
)

@Composable
internal fun SubsectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
}

@Composable
private fun EpreuvesSection(
    epreuves: List<EpreuveEnvironnementale>,
    onEpreuvesChanged: (List<EpreuveEnvironnementale>) -> Unit,
    onOuvrirEpreuve: () -> Unit,
) {
    val epreuveActive by EpreuveSession.etat.collectAsState()
    var afficherCreation by remember { mutableStateOf(false) }
    var afficherCatalogue by remember { mutableStateOf(false) }
    var epreuveALancer by remember { mutableStateOf<EpreuveEnvironnementale?>(null) }
    var epreuveEditee by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        epreuveActive?.let { active ->
            OutlinedButton(onClick = onOuvrirEpreuve, modifier = Modifier.fillMaxWidth()) {
                Text(
                    (if (active.issue == null) "Épreuve en cours : " else "Épreuve terminée : ") +
                        "${active.epreuve.nom} (${active.progres}/${active.progresMax})"
                )
            }
        }
        if (epreuves.isEmpty()) {
            Text(
                "Aucune épreuve. Ajoutez-en depuis le catalogue ou créez la vôtre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        epreuves.forEachIndexed { index, epreuve ->
            EpreuveCard(
                epreuve = epreuve,
                onLancer = { epreuveALancer = epreuve },
                onModifier = { epreuveEditee = index },
                onSupprimer = { onEpreuvesChanged(epreuves.filterIndexed { i, _ -> i != index }) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { afficherCatalogue = true }, modifier = Modifier.weight(1f)) { Text("Catalogue") }
            Button(onClick = { afficherCreation = true }, modifier = Modifier.weight(1f)) { Text("Créer") }
        }
    }

    if (afficherCreation) {
        CreerEpreuveDialog(
            onDismiss = { afficherCreation = false },
            onConfirmer = { onEpreuvesChanged(epreuves + it); afficherCreation = false }
        )
    }

    epreuveEditee?.let { index ->
        CreerEpreuveDialog(
            initiale = epreuves[index],
            onDismiss = { epreuveEditee = null },
            onConfirmer = { modifiee ->
                onEpreuvesChanged(epreuves.mapIndexed { i, e -> if (i == index) modifiee else e })
                epreuveEditee = null
            }
        )
    }

    if (afficherCatalogue) {
        var apercu by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { afficherCatalogue = false },
            title = { Text("Catalogue d'épreuves") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    catalogueEpreuves.forEach { epreuve ->
                        val ouvert = apercu == epreuve.nom
                        val dejaPresente = epreuves.any { it.nom == epreuve.nom }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { apercu = if (ouvert) null else epreuve.nom }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(epreuve.nom, style = MaterialTheme.typography.titleSmall)
                            if (ouvert) {
                                EpreuveContenu(epreuve)
                                if (dejaPresente) {
                                    Text(
                                        "Déjà dans cet environnement : la mise à jour remplace ta version (tes modifications seront perdues).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(onClick = {
                                        onEpreuvesChanged(epreuves.map { if (it.nom == epreuve.nom) epreuve else it })
                                        afficherCatalogue = false
                                    }) { Text("Mettre à jour depuis le catalogue") }
                                } else {
                                    Button(onClick = { onEpreuvesChanged(epreuves + epreuve); afficherCatalogue = false }) {
                                        Text("Ajouter à l'environnement")
                                    }
                                }
                            } else {
                                Text(
                                    "${epreuve.type.label} · ${epreuve.capacites.size} capacités" +
                                        (if (dejaPresente) " · déjà présente" else "") + " · toucher pour voir",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { afficherCatalogue = false }) { Text("Fermer") } }
        )
    }

    epreuveALancer?.let { epreuve ->
        LancerEpreuveDialog(
            epreuve = epreuve,
            onDismiss = { epreuveALancer = null },
            onLancee = { epreuveALancer = null; onOuvrirEpreuve() }
        )
    }
}

/** Tranches de FP proposées pour affiner le bestiaire suggéré (mêmes paliers que les tables d'exemple). */
private val TranchesFp: List<Pair<String, ClosedFloatingPointRange<Double>>> = listOf(
    "FP 0-1" to 0.0..1.0,
    "FP 2-4" to 2.0..4.0,
    "FP 5-10" to 5.0..10.0,
    "FP 11-16" to 11.0..16.0,
    "FP 17+" to 17.0..99.0,
)

private const val SuggestionsVisiblesParDefaut = 8

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BestiaireSection(
    environnement: Environnement,
    worldId: String?,
    onOpenBestiaryDetail: (String) -> Unit,
    onEnvironnementChanged: (Environnement) -> Unit,
) {
    val context = LocalContext.current
    val monstresManuels = environnement.monstresIds
    val terrains = environnement.terrainsEffectifs
    val terrainsDevines = environnement.terrains.isEmpty() && terrains.isNotEmpty()
    var tousLesMonstres by remember(worldId) { mutableStateOf<List<SrdEntry>>(emptyList()) }
    var tranchesFp by remember { mutableStateOf(setOf<String>()) }
    var toutAfficher by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(worldId) {
        tousLesMonstres = SrdRepository.loadMonsters(context, worldId)
    }
    LaunchedEffect(query, worldId) {
        results = if (query.length < 2) emptyList() else {
            SrdRepository.searchMonsters(context, query, worldId).map { it.name }.take(6)
        }
    }

    // Monstres du SRD rencontrables sur les terrains choisis, filtrés par tranche de FP puis triés
    // du plus faible au plus fort ; ceux déjà ajoutés à la main ne sont pas répétés.
    val suggestions = remember(tousLesMonstres, terrains, tranchesFp, monstresManuels) {
        val plages = TranchesFp.filter { it.first in tranchesFp }.map { it.second }
        tousLesMonstres
            .filter { it.rencontrableDans(terrains) }
            .filter { m -> monstresManuels.none { it.equals(m.name, ignoreCase = true) } }
            .map { it to challengeSortKey(monsterChallenge(it)?.first ?: "") }
            .filter { (_, fp) -> plages.isEmpty() || plages.any { fp in it } }
            .sortedWith(compareBy({ it.second }, { it.first.name }))
            .map { it.first }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (terrainsDevines) "Terrains devinés depuis le nom — touchez pour ajuster :" else "Terrains de rencontre :",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TERRAINS_SRD.forEach { terrain ->
                val selectionne = terrain in terrains
                FilterChip(
                    selected = selectionne,
                    onClick = {
                        onEnvironnementChanged(
                            environnement.copy(terrains = if (selectionne) terrains - terrain else terrains + terrain)
                        )
                    },
                    label = { Text(terrain) }
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TranchesFp.forEach { (label, _) ->
                val selectionne = label in tranchesFp
                FilterChip(
                    selected = selectionne,
                    onClick = { tranchesFp = if (selectionne) tranchesFp - label else tranchesFp + label },
                    label = { Text(label) }
                )
            }
        }

        if (terrains.isEmpty()) {
            Text(
                "Choisissez au moins un terrain pour obtenir des suggestions de monstres du SRD.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (suggestions.isEmpty() && tousLesMonstres.isNotEmpty()) {
            Text(
                "Aucun monstre du SRD ne correspond à ces filtres.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (suggestions.isNotEmpty()) {
            Text(
                "${suggestions.size} monstre(s) suggéré(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val visibles = if (toutAfficher) suggestions else suggestions.take(SuggestionsVisiblesParDefaut)
        visibles.forEach { monstre ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenBestiaryDetail(monstre.name) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(monstre.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
                monsterChallengeLabel(monstre)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (suggestions.size > SuggestionsVisiblesParDefaut) {
            TextButton(onClick = { toutAfficher = !toutAfficher }) {
                Text(if (toutAfficher) "Réduire" else "Afficher tout (${suggestions.size})")
            }
        }

        if (monstresManuels.isNotEmpty()) {
            Text(
                "Ajoutés manuellement :",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        monstresManuels.forEachIndexed { index, name ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f).clickable { onOpenBestiaryDetail(name) }
                )
                IconButton(onClick = {
                    onEnvironnementChanged(environnement.copy(monstresIds = monstresManuels.filterIndexed { i, _ -> i != index }))
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer")
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Ajouter un autre monstre (recherche bestiaire)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        results.forEach { name ->
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEnvironnementChanged(environnement.copy(monstresIds = monstresManuels + name))
                        query = ""
                        results = emptyList()
                    }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LootSection(
    tableButin: List<LootEntry>,
    worldId: String?,
    onOpenEquipmentDetail: (String) -> Unit,
    onTableButinChanged: (List<LootEntry>) -> Unit,
    onTirage: (String) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var poids by remember { mutableStateOf("1") }

    LaunchedEffect(query, worldId) {
        results = if (query.length < 2) emptyList() else {
            SrdRepository.loadEquipmentList(context, worldId)
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { it.name }
                .take(6)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tableButin.forEachIndexed { index, entry ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.nomObjet} (poids ${entry.poids})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f).clickable { onOpenEquipmentDetail(entry.nomObjet) }
                )
                IconButton(onClick = { onTableButinChanged(tableButin.filterIndexed { i, _ -> i != index }) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Rechercher un objet (équipement SRD)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = poids,
                onValueChange = { poids = it.filter(Char::isDigit) },
                label = { Text("Poids") },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )
        }
        results.forEach { name ->
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onTableButinChanged(tableButin + LootEntry(name, poids.toIntOrNull() ?: 1))
                        query = ""
                        results = emptyList()
                        poids = "1"
                    }
                    .padding(vertical = 4.dp)
            )
        }
        if (tableButin.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    val tirage = tableButin.echantillonnerSansRemise(
                        n = minOf(3, tableButin.size),
                        poids = { it.poids.toDouble() },
                        random = Random(System.nanoTime())
                    )
                    onTirage(tirage.joinToString(", ") { it.nomObjet })
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tirer un butin")
            }
        }
    }
}

/** Libellé lisible d'une image prédéfinie : "marais_maudit.webp" → "Marais maudit". */
private fun libelleImage(assetName: String): String =
    assetName.substringBeforeLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }

/** Galerie des images d'environnement fournies avec l'app (assets/dnd/environnements), réutilisée par l'outil ÉPREUVES. */
@Composable
internal fun GalerieImagesDialog(onChoisir: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val images = remember { EnvironmentImageStore.imagesPredefinies(context) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Galerie d'environnements") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (images.isEmpty()) Text("Aucune image disponible.")
                images.forEach { assetName ->
                    // Vignette sous-échantillonnée pour ne pas charger l'image pleine taille.
                    val vignette = remember(assetName) {
                        runCatching {
                            context.assets.open("${EnvironmentImageStore.DOSSIER_ASSETS}/$assetName").use {
                                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                                    ?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                    Column(modifier = Modifier.fillMaxWidth().clickable { onChoisir(assetName) }) {
                        if (vignette != null) {
                            Image(
                                bitmap = vignette,
                                contentDescription = libelleImage(assetName),
                                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(libelleImage(assetName), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
