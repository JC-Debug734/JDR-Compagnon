package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementsLiesSection
import com.jc2.jdrcompagnon.di.EnvironmentDependencies
import com.jc2.jdrcompagnon.di.TableAleatoireDependencies
import com.jc2.jdrcompagnon.feature_combat.ui.ComposerCombatDialog
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.ui.GameState


import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val NamedColors = mapOf(
    "Rouge" to "red", "Vert" to "green", "Bleu" to "blue",
    "Jaune" to "yellow", "Violet" to "purple", "Orange" to "orange"
)

private val NamedColorValues = mapOf(
    "red" to Color(0xFFF44336), "green" to Color(0xFF4CAF50),
    "blue" to Color(0xFF2196F3), "yellow" to Color(0xFFFFEB3B),
    "purple" to Color(0xFF9C27B0), "orange" to Color(0xFFFF9800)
)

/** Snapshot complet pour undo (scènes + index + titre). */
private data class ScenarioSnapshot(
    val title: String,
    val scenesJson: String,
    val selectedIndex: Int,
    val markdownText: String,
    val cursorPosition: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioEditorScreen(
    scenarioId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    onOpenMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val existingScenario = scenarioId?.let { id -> mjScenarios.firstOrNull { it.id == id } }

    // Bestiaire des campagnes auxquelles ce scénario est attaché : proposé en tête des outils
    // "lien monstre" et "combat", pour piocher directement dans les monstres de la campagne.
    val mjCampaigns by GameState.mjCampaigns.collectAsState()
    val bestiaireCampagne = remember(mjCampaigns, scenarioId) {
        if (scenarioId == null) emptyList()
        else mjCampaigns.filter { scenarioId in it.scenarioIds }
            .flatMap { it.monsterIds }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    // Environnements disponibles pour le monde courant, pour le sélecteur "Environnement de la
    // scène" — pas de ViewModel dédié ici, l'écran n'en utilise déjà pas pour le reste.
    val environments by remember {
        EnvironmentDependencies.repository.observerEnvironnements(GameState.currentWorldId() ?: "donjon_et_dragon")
    }.collectAsState(initial = emptyList())
    val environmentOptions: List<Pair<String, String>> =
        listOf("Aucun" to "") + environments.map { it.nom to it.id }

    // Tables aléatoires disponibles pour le monde courant : événements pour les sélecteurs
    // "Table d'événements" (scène + repli scénario entier), loot pour le sélecteur "Table de
    // loot" de la scène — même approche que le sélecteur d'environnement ci-dessus.
    val tablesAleatoires by remember {
        TableAleatoireDependencies.repository.observerTables(GameState.currentWorldId() ?: "donjon_et_dragon")
    }.collectAsState(initial = emptyList())
    val tableEvenementsOptions: List<Pair<String, String>> =
        listOf("Aucune" to "") + tablesAleatoires.filter { it.type == TypeTable.EVENEMENTS }.map { it.nom to it.id }
    val tableLootOptions: List<Pair<String, String>> =
        listOf("Aucune" to "") + tablesAleatoires.filter { it.type == TypeTable.LOOT }.map { it.nom to it.id }

    // Boutiques (feature_boutique) associables à une scène : leurs services et tarifs sont
    // consultables depuis la lecture de la scène.
    val boutiques by remember {
        com.jc2.jdrcompagnon.di.BoutiqueDependencies.repository.observerToutesLesBoutiques()
    }.collectAsState(initial = emptyList())
    val boutiqueOptions: List<Pair<String, String>> =
        listOf("Aucune" to "") + boutiques.map { "${it.nom} (${it.type.label})" to it.id }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Id stable dès l'ouverture de l'éditeur (même pour un nouveau scénario) : nécessaire pour
    // associer immédiatement une image de lieu via ScenarioImageStore avant le premier
    // enregistrement.
    val effectiveScenarioId = existingScenario?.id ?: rememberSaveable { java.util.UUID.randomUUID().toString() }

    var title by rememberSaveable { mutableStateOf("") }
    var lieuNom by rememberSaveable { mutableStateOf("") }
    var lieuImageFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var tableEvenementsId by rememberSaveable { mutableStateOf<String?>(null) }
    // Chapitre et numéro du scénario (ordre de jeu de la campagne, voir ChapitresScenarios),
    // saisis en texte puis convertis en nombres à l'enregistrement.
    var chapitreNumero by rememberSaveable { mutableStateOf("") }
    var chapitreTitre by rememberSaveable { mutableStateOf("") }
    var numero by rememberSaveable { mutableStateOf("") }
    var scenes by remember { mutableStateOf(listOf(GameState.MjScene(title = "Scène 1"))) }
    var selectedSceneIndex by remember { mutableStateOf(0) }
    var markdownContent by remember { mutableStateOf(TextFieldValue("")) }
    var loaded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkDialogInitialType by remember { mutableStateOf("monster") }
    // PNJ dont on remplit la fiche de discussion (attitude, désirs, peurs, ligne rouge).
    var discussionAFicher by remember { mutableStateOf<String?>(null) }
    var showDeleteSceneDialog by remember { mutableStateOf(false) }
    var showCombatDialog by remember { mutableStateOf(false) }
    var undoStack by remember { mutableStateOf(listOf<ScenarioSnapshot>()) }

    LaunchedEffect(scenarioId, mjScenarios) {
        if (!loaded) {
            existingScenario?.let { scenario ->
                title = scenario.title
                lieuNom = scenario.lieuNom
                lieuImageFileName = scenario.lieuImageFileName
                tableEvenementsId = scenario.tableEvenementsId
                chapitreNumero = scenario.chapitreNumero?.toString().orEmpty()
                chapitreTitre = scenario.chapitreTitre
                numero = scenario.numero?.toString().orEmpty()
                scenes = scenario.scenes.ifEmpty {
                    listOf(GameState.MjScene(title = "Scène 1", markdownContent = scenario.markdownContent))
                }
                selectedSceneIndex = 0
                markdownContent = TextFieldValue(scenes.firstOrNull()?.markdownContent ?: "")
            }
            loaded = true
        }
    }

    LaunchedEffect(selectedSceneIndex) {
        val scene = scenes.getOrNull(selectedSceneIndex)
        markdownContent = TextFieldValue(scene?.markdownContent ?: "")
    }

    fun currentSnapshot(): ScenarioSnapshot {
        val json = Json { ignoreUnknownKeys = true }
        return ScenarioSnapshot(
            title = title,
            scenesJson = json.encodeToString(scenes),
            selectedIndex = selectedSceneIndex,
            markdownText = markdownContent.text,
            cursorPosition = markdownContent.selection.start.coerceIn(0, markdownContent.text.length)
        )
    }

    fun applySnapshot(snapshot: ScenarioSnapshot) {
        val json = Json { ignoreUnknownKeys = true }
        title = snapshot.title
        scenes = try {
            json.decodeFromString<List<GameState.MjScene>>(snapshot.scenesJson)
        } catch (_: Exception) {
            scenes
        }
        selectedSceneIndex = snapshot.selectedIndex.coerceIn(0, scenes.size.coerceAtLeast(1) - 1)
        markdownContent = TextFieldValue(
            text = snapshot.markdownText,
            selection = TextRange(snapshot.cursorPosition.coerceIn(0, snapshot.markdownText.length))
        )
    }

    fun pushUndo() {
        undoStack = (listOf(currentSnapshot()) + undoStack).take(30)
    }

    fun popUndo(): Boolean {
        val previous = undoStack.firstOrNull() ?: return false
        undoStack = undoStack.drop(1)
        applySnapshot(previous)
        return true
    }

    fun updateCurrentScene(newContent: TextFieldValue) {
        scenes = scenes.mapIndexed { index, scene ->
            if (index == selectedSceneIndex) scene.copy(markdownContent = newContent.text) else scene
        }
        markdownContent = newContent
    }

    fun updateText(newText: String, newCursor: Int) {
        pushUndo()
        updateCurrentScene(TextFieldValue(
            text = newText,
            selection = TextRange(newCursor.coerceIn(0, newText.length))
        ))
    }

    fun insertMarkdown(prefix: String, suffix: String = prefix) {
        val current = markdownContent
        val start = current.selection.start.coerceIn(0, current.text.length)
        val end = current.selection.end.coerceIn(start, current.text.length)
        val text = current.text
        val selectedText = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        updateText(newText, start + prefix.length + selectedText.length + suffix.length)
    }

    fun insertLinePrefix(prefix: String) {
        val current = markdownContent
        val cursor = current.selection.start.coerceIn(0, current.text.length)
        val text = current.text
        val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        updateText(newText, cursor + prefix.length)
    }

    fun insertColorTag(colorName: String) {
        val current = markdownContent
        val start = current.selection.start.coerceIn(0, current.text.length)
        val end = current.selection.end.coerceIn(start, current.text.length)
        val text = current.text
        val selectedText = text.substring(start, end)
        val lowerName = colorName.lowercase().replace("_", "")
        val prefix = "{color:$lowerName}"
        val suffix = "{/color}"
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        updateText(newText, if (selectedText.isEmpty()) start + prefix.length else start + prefix.length + selectedText.length + suffix.length)
        showColorPicker = false
    }

    /** Zone "à lire aux joueurs" (description, paroles d'un PNJ) : fond vert + synthèse vocale en lecture. */
    fun insertReadAloudZone() {
        val current = markdownContent
        val start = current.selection.start.coerceIn(0, current.text.length)
        val end = current.selection.end.coerceIn(start, current.text.length)
        val text = current.text
        val selectedText = text.substring(start, end)
        val prefix = "{lire}\n"
        val suffix = "\n{/lire}"
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        updateText(newText, start + prefix.length + selectedText.length)
    }

    fun insertInternalLink(type: String, name: String) {
        val current = markdownContent
        val cursor = current.selection.start.coerceIn(0, current.text.length)
        val text = current.text
        val insertion = "#$type:[$name]"
        val newText = text.substring(0, cursor) + insertion + text.substring(cursor)
        updateText(newText, cursor + insertion.length)
        showLinkDialog = false
    }

    fun addScene() {
        pushUndo()
        scenes = scenes + GameState.MjScene(
            title = "Scène ${scenes.size + 1}",
            order = scenes.size
        )
        selectedSceneIndex = scenes.size - 1
    }

    fun deleteScene(index: Int) {
        pushUndo()
        scenes = scenes.filterIndexed { i, _ -> i != index }
        if (selectedSceneIndex >= scenes.size) {
            selectedSceneIndex = (scenes.size - 1).coerceAtLeast(0)
        }
    }

    fun moveScene(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in scenes.indices || toIndex !in scenes.indices) return
        pushUndo()
        val mutable = scenes.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        scenes = mutable.mapIndexed { i, scene -> scene.copy(order = i) }
        selectedSceneIndex = toIndex
    }

    val choisirLieuImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val fileName = ScenarioImageStore.copier(context, uri, effectiveScenarioId)
            if (fileName != null) lieuImageFileName = fileName
        }
    }

    fun saveScenario() {
        if (title.isNotBlank()) {
            val finalScenes = scenes.mapIndexed { i, scene -> scene.copy(order = i) }
            val updated = existingScenario?.copy(
                title = title,
                markdownContent = "",
                scenes = finalScenes,
                lieuNom = lieuNom,
                lieuImageFileName = lieuImageFileName,
                tableEvenementsId = tableEvenementsId,
                chapitreNumero = chapitreNumero.trim().toIntOrNull(),
                chapitreTitre = chapitreTitre.trim(),
                numero = numero.trim().toIntOrNull(),
            ) ?: GameState.MjScenario(
                id = effectiveScenarioId,
                title = title,
                scenes = finalScenes,
                worldId = GameState.currentWorldId() ?: "",
                lieuNom = lieuNom,
                lieuImageFileName = lieuImageFileName,
                tableEvenementsId = tableEvenementsId,
                chapitreNumero = chapitreNumero.trim().toIntOrNull(),
                chapitreTitre = chapitreTitre.trim(),
                numero = numero.trim().toIntOrNull(),
            )
            if (existingScenario != null) GameState.updateMjScenario(updated, context)
            else GameState.addMjScenario(updated, context)
            onSaved()
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Le titre est obligatoire") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scénario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!popUndo()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Rien à annuler")
                                }
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
                    }
                    IconButton(onClick = { saveScenario() }) {
                        Icon(Icons.Default.Save, contentDescription = "Enregistrer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Fond uni (et non transparent) : le fond d'écran global de l'app rendait les
        // champs et le texte illisibles par-dessus.
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre du scénario") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )

            // Chapitre et numéro : rangent la liste des scénarios dans l'ordre de jeu (« 3.2 »).
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = chapitreNumero,
                    onValueChange = { v -> chapitreNumero = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Chapitre") },
                    placeholder = { Text("3") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = numero,
                    onValueChange = { v -> numero = v.filter { it.isDigit() }.take(3) },
                    label = { Text("N° scénario") },
                    placeholder = { Text("2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = chapitreTitre,
                onValueChange = { chapitreTitre = it },
                label = { Text("Titre du chapitre (ex : Quand la maison brûle)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true,
            )

            // Lieu associé au scénario : affiché en tête de la première page du lecteur
            // (nom + image), pour planter le décor avant la première scène.
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("Lieu du scénario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lieuNom,
                    onValueChange = { lieuNom = it },
                    label = { Text("Nom du lieu (ex: Fondcombe)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                val lieuBitmap = lieuImageFileName?.let { fileName ->
                    remember(fileName) {
                        runCatching {
                            val file = ScenarioImageStore.fichier(context, fileName)
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                        }.getOrNull()
                    }
                }
                if (lieuBitmap != null) {
                    Image(
                        bitmap = lieuBitmap,
                        contentDescription = "Image du lieu",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { choisirLieuImageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (lieuImageFileName == null) "Choisir une image" else "Changer l'image")
                    }
                    if (lieuImageFileName != null) {
                        OutlinedButton(onClick = { lieuImageFileName = null }) {
                            Text("Retirer")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Table d'événements (feature_table_aleatoire, type EVENEMENTS) utilisée pour tout
                // le scénario — repli quand une scène n'a pas sa propre table (voir
                // MjScene.tableAleatoireId et MjScenario.tableEvenementsId). Champ propre à
                // l'app, au même titre que lieuNom.
                var tableEvenementsExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = tableEvenementsExpanded,
                    onExpandedChange = { tableEvenementsExpanded = !tableEvenementsExpanded },
                ) {
                    OutlinedTextField(
                        value = tableEvenementsOptions.find { it.second == tableEvenementsId }?.first ?: "Aucune",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Table d'événements du scénario (repli)") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tableEvenementsExpanded) }
                    )
                    ExposedDropdownMenu(expanded = tableEvenementsExpanded, onDismissRequest = { tableEvenementsExpanded = false }) {
                        tableEvenementsOptions.forEach { (label, id) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    tableEvenementsId = id.ifBlank { null }
                                    tableEvenementsExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Sélecteur de scènes avec réordonnancement et suppression. Cartes pleines (fond uni
            // de thème) avec bordure de sélection, même méthodologie que GroupsScreen (outil
            // Groupes) — remplace les FilterChip Material3 par défaut, dont le style "selected"
            // provoquait un affichage cassé (étiquette illisible/mal dimensionnée) avec le thème
            // de l'app.
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(scenes.size, key = { "scene-chip-$it" }) { index ->
                    val scene = scenes[index]
                    val selected = selectedSceneIndex == index
                    Card(
                        modifier = Modifier
                            .clickable { selectedSceneIndex = index }
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                                } else {
                                    Modifier
                                }
                            ),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = if (selected && scenes.size > 1) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = scene.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (selected && scenes.size > 1) {
                                IconButton(
                                    onClick = { showDeleteSceneDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Supprimer la scène",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    IconButton(onClick = { addScene() }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une scène")
                    }
                }
            }

            // Contrôles de réordonnancement de la scène active
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scène ${selectedSceneIndex + 1}/${scenes.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { moveScene(selectedSceneIndex, selectedSceneIndex - 1) },
                    enabled = selectedSceneIndex > 0
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Déplacer à gauche")
                }
                IconButton(
                    onClick = { moveScene(selectedSceneIndex, selectedSceneIndex + 1) },
                    enabled = selectedSceneIndex < scenes.size - 1
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Déplacer à droite")
                }
            }

            // Titre et musique de la scène active
            scenes.getOrNull(selectedSceneIndex)?.let { scene ->
                OutlinedTextField(
                    value = scene.title,
                    onValueChange = { newTitle ->
                        pushUndo()
                        scenes = scenes.mapIndexed { index, s ->
                            if (index == selectedSceneIndex) s.copy(title = newTitle) else s
                        }
                    },
                    label = { Text("Titre de la scène") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true
                )

                // Pas de sélecteur de musique manuel ici : la musique de la scène provient de
                // l'environnement qui lui est rattaché (voir sélecteur "Environnement de la
                // scène" ci-dessous et ScenarioReaderContent, qui joue Environnement.musicTrackId).
                var environmentExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = environmentExpanded,
                    onExpandedChange = { environmentExpanded = !environmentExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = environmentOptions.find { it.second == scene.environmentId }?.first ?: "Aucun",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Environnement de la scène") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(environmentExpanded) }
                    )
                    ExposedDropdownMenu(expanded = environmentExpanded, onDismissRequest = { environmentExpanded = false }) {
                        environmentOptions.forEach { (label, environmentId) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    pushUndo()
                                    scenes = scenes.mapIndexed { index, s ->
                                        if (index == selectedSceneIndex) s.copy(environmentId = environmentId.ifBlank { null }) else s
                                    }
                                    environmentExpanded = false
                                }
                            )
                        }
                    }
                }

                // Table aléatoire d'événements (feature_table_aleatoire, type EVENEMENTS) rattachée
                // à cette scène — même sélecteur que l'environnement ci-dessus.
                var tableAleatoireExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = tableAleatoireExpanded,
                    onExpandedChange = { tableAleatoireExpanded = !tableAleatoireExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = tableEvenementsOptions.find { it.second == scene.tableAleatoireId }?.first ?: "Aucune",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Table aléatoire de la scène") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tableAleatoireExpanded) }
                    )
                    ExposedDropdownMenu(expanded = tableAleatoireExpanded, onDismissRequest = { tableAleatoireExpanded = false }) {
                        tableEvenementsOptions.forEach { (label, tableId) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    pushUndo()
                                    scenes = scenes.mapIndexed { index, s ->
                                        if (index == selectedSceneIndex) s.copy(tableAleatoireId = tableId.ifBlank { null }) else s
                                    }
                                    tableAleatoireExpanded = false
                                }
                            )
                        }
                    }
                }

                // Table de loot (feature_table_aleatoire, type LOOT) rattachée à cette scène —
                // le butin est propre à chaque scène (pas de repli scénario entier, contrairement
                // à la table d'événements ci-dessus).
                var tableLootExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = tableLootExpanded,
                    onExpandedChange = { tableLootExpanded = !tableLootExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = tableLootOptions.find { it.second == scene.tableLootId }?.first ?: "Aucune",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Table de loot de la scène") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tableLootExpanded) }
                    )
                    ExposedDropdownMenu(expanded = tableLootExpanded, onDismissRequest = { tableLootExpanded = false }) {
                        tableLootOptions.forEach { (label, tableId) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    pushUndo()
                                    scenes = scenes.mapIndexed { index, s ->
                                        if (index == selectedSceneIndex) s.copy(tableLootId = tableId.ifBlank { null }) else s
                                    }
                                    tableLootExpanded = false
                                }
                            )
                        }
                    }
                }

                // Boutique présente dans la scène (icône à côté du loot en lecture).
                var boutiqueExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = boutiqueExpanded,
                    onExpandedChange = { boutiqueExpanded = !boutiqueExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = boutiqueOptions.find { it.second == scene.boutiqueId }?.first ?: "Aucune",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Boutique de la scène") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boutiqueExpanded) }
                    )
                    ExposedDropdownMenu(expanded = boutiqueExpanded, onDismissRequest = { boutiqueExpanded = false }) {
                        boutiqueOptions.forEach { (label, boutiqueId) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    pushUndo()
                                    scenes = scenes.mapIndexed { index, s ->
                                        if (index == selectedSceneIndex) s.copy(boutiqueId = boutiqueId.ifBlank { null }) else s
                                    }
                                    boutiqueExpanded = false
                                }
                            )
                        }
                    }
                }

                // Événements de la bibliothèque rattachés à la scène (tirables en lecture).
                EvenementsLiesSection(
                    worldId = GameState.currentWorldId() ?: "donjon_et_dragon",
                    campagneId = GameState.currentCampaignId.collectAsState().value,
                    evenementIds = scene.evenementIds,
                    onChanger = { ids ->
                        pushUndo()
                        scenes = scenes.mapIndexed { index, s ->
                            if (index == selectedSceneIndex) s.copy(evenementIds = ids) else s
                        }
                    },
                    titre = "Événements de la scène",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item { ToolbarTextButton("H1") { insertLinePrefix("# ") } }
                item { ToolbarTextButton("H2") { insertLinePrefix("## ") } }
                item { ToolbarTextButton("H3") { insertLinePrefix("### ") } }
                item { IconButton(onClick = { insertMarkdown("**") }) { Icon(Icons.Default.FormatBold, contentDescription = "Gras") } }
                item { IconButton(onClick = { insertMarkdown("*") }) { Icon(Icons.Default.FormatItalic, contentDescription = "Italique") } }
                item { IconButton(onClick = { insertMarkdown("`") }) { Icon(Icons.Default.Code, contentDescription = "Code") } }
                item { IconButton(onClick = { showColorPicker = true }) { Icon(Icons.Default.FormatColorFill, contentDescription = "Couleur") } }
                item {
                    IconButton(onClick = { insertReadAloudZone() }) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Zone à lire aux joueurs (description, paroles de PNJ)", tint = ReadAloudGreen)
                    }
                }
                item {
                    IconButton(onClick = { linkDialogInitialType = "pnj"; showLinkDialog = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Lien vers un PNJ")
                    }
                }
                item {
                    IconButton(onClick = { linkDialogInitialType = "equipment"; showLinkDialog = true }) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Lien vers un équipement")
                    }
                }
                item {
                    IconButton(onClick = { linkDialogInitialType = "event"; showLinkDialog = true }) {
                        Icon(Icons.Default.Forum, contentDescription = "Discussion avec un PNJ (fiche : attitude, désirs, peurs)")
                    }
                }
                item {
                    IconButton(onClick = { linkDialogInitialType = "epreuve"; showLinkDialog = true }) {
                        Icon(Icons.Default.Terrain, contentDescription = "Lien vers une épreuve environnementale")
                    }
                }
                item {
                    IconButton(onClick = { linkDialogInitialType = "evenement"; showLinkDialog = true }) {
                        Icon(Icons.Default.AutoStories, contentDescription = "Lien vers un événement de la bibliothèque (lancé depuis la lecture)")
                    }
                }
                item {
                    IconButton(onClick = { showCombatDialog = true }) {
                        Icon(Icons.Default.SportsMartialArts, contentDescription = "Insérer un combat (un ou plusieurs monstres)")
                    }
                }
                item {
                    IconButton(onClick = { linkDialogInitialType = "monster"; showLinkDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = "Autre lien interne (monstre, sort, règle...)")
                    }
                }
                item { IconButton(onClick = { insertLinePrefix("- ") }) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Liste") } }
                item { IconButton(onClick = { insertLinePrefix("1. ") }) { Icon(Icons.Default.FormatListNumbered, contentDescription = "Liste numérotée") } }
            }

            // Discussions présentes dans le texte de la scène : un crayon par PNJ pour rouvrir sa
            // fiche (attitude, désirs, peurs, ligne rouge) sans devoir réinsérer le lien.
            val discussionsDuTexte = remember(markdownContent.text) {
                Regex("""#event:\[([^\]\n]+)\]""").findAll(markdownContent.text)
                    .map { it.groupValues[1].trim() }
                    .distinctBy { it.lowercase() }
                    .toList()
            }
            if (discussionsDuTexte.isNotEmpty()) {
                val fichesScene = scenes.getOrNull(selectedSceneIndex)?.discussions.orEmpty()
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    discussionsDuTexte.forEach { pnjName ->
                        val fiche = fichesScene.firstOrNull { it.pnjName.equals(pnjName, ignoreCase = true) }
                        AssistChip(
                            onClick = { discussionAFicher = pnjName },
                            label = {
                                Text(
                                    "💬 $pnjName" + if (fiche == null || fiche.estVide) " · fiche vide" else "",
                                    color = Color.White
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier la discussion avec $pnjName", tint = Color.White)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                            ),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = markdownContent,
                onValueChange = { updateCurrentScene(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp)
                    .padding(16.dp),
                placeholder = { Text("Saisissez le contenu de la scène en markdown...") },
                visualTransformation = ReadAloudHighlightTransformation,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
            )
        }
    }

    if (showColorPicker) {
        ColorPickerBottomSheet(
            onDismiss = { showColorPicker = false },
            onColorSelected = { name -> insertColorTag(name) }
        )
    }

    if (showLinkDialog) {
        EntityLinkPickerDialog(
            visible = true,
            onDismiss = { showLinkDialog = false },
            onSelect = { type, name ->
                if (type == "event") {
                    // Discussion : on remplit d'abord sa fiche (ce que le PNJ attend).
                    showLinkDialog = false
                    discussionAFicher = name
                } else {
                    insertInternalLink(type, name)
                }
            },
            initialType = linkDialogInitialType,
            bestiaireCampagne = bestiaireCampagne
        )
    }

    discussionAFicher?.let { pnjName ->
        val scene = scenes.getOrNull(selectedSceneIndex)
        DiscussionFicheDialog(
            pnjName = pnjName,
            initial = scene?.discussions?.firstOrNull { it.pnjName.equals(pnjName, ignoreCase = true) },
            onDismiss = { discussionAFicher = null },
            onSave = { fiche ->
                pushUndo()
                scenes = scenes.mapIndexed { index, s ->
                    if (index != selectedSceneIndex) s
                    else s.copy(discussions = s.discussions.filterNot { it.pnjName.equals(pnjName, ignoreCase = true) } + fiche)
                }
                // Lien déjà présent dans la scène : on met seulement sa fiche à jour.
                val dejaPresent = markdownContent.text.contains("#event:[$pnjName]", ignoreCase = true)
                if (!dejaPresent) insertInternalLink("event", pnjName)
                discussionAFicher = null
            }
        )
    }

    if (showCombatDialog) {
        ComposerCombatDialog(
            // Toutes les scènes (celle en cours d'édition incluse, via scenes à jour).
            scenarioContent = scenes.joinToString("\n") { it.markdownContent },
            // Bestiaire de l'environnement choisi pour la scène en cours, proposé en plus.
            environnement = scenes.getOrNull(selectedSceneIndex)?.environmentId
                ?.let { id -> environments.firstOrNull { it.id == id } },
            bestiaireCampagne = bestiaireCampagne,
            onDismiss = { showCombatDialog = false },
            onInserer = { composition ->
                insertInternalLink("combat", composition)
                showCombatDialog = false
            }
        )
    }

    if (showDeleteSceneDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSceneDialog = false },
            title = { Text("Supprimer la scène") },
            text = { Text("Supprimer la scène « ${scenes.getOrNull(selectedSceneIndex)?.title} » ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteScene(selectedSceneIndex)
                        showDeleteSceneDialog = false
                        coroutineScope.launch { snackbarHostState.showSnackbar("Scène supprimée") }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSceneDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ToolbarTextButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerBottomSheet(
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var customHex by remember { mutableStateOf("#2196F3") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Choisir une couleur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NamedColors.forEach { (colorName, colorKey) ->
                    val color = NamedColorValues[colorKey] ?: Color.Gray
                    FilterChip(
                        selected = false,
                        onClick = { onColorSelected(colorKey) },
                        label = { Text(colorName) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(16.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customHex,
                    onValueChange = { customHex = it },
                    label = { Text("Hex #RRGGBB") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = {
                    if (customHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        val rawHex = customHex.removePrefix("#")
                        onColorSelected("custom$rawHex")
                    }
                }) {
                    Text("OK")
                }
            }
        }
    }
}
/**
 * Surligne en vert, dans le champ d'édition, les zones "à lire aux joueurs" ({lire}…{/lire}),
 * y compris une zone pas encore refermée pendant la saisie.
 */
private object ReadAloudHighlightTransformation : androidx.compose.ui.text.input.VisualTransformation {
    private val zoneRegex = Regex("""\{lire\}.*?(\{/lire\}|\z)""", RegexOption.DOT_MATCHES_ALL)
    private val zoneStyle = androidx.compose.ui.text.SpanStyle(
        background = ReadAloudGreen.copy(alpha = 0.35f),
        color = Color(0xFFC8E6C9)
    )

    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val zones = zoneRegex.findAll(text.text).toList()
        if (zones.isEmpty()) {
            return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        }
        val styled = androidx.compose.ui.text.buildAnnotatedString {
            append(text)
            zones.forEach { addStyle(zoneStyle, it.range.first, it.range.last + 1) }
        }
        return androidx.compose.ui.text.input.TransformedText(styled, androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}
