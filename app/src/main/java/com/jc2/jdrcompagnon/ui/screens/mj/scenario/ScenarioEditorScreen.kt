package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import com.jc2.jdrcompagnon.ui.components.WorldBackground


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

private val MusicTracks: List<Pair<String, String>> =
    listOf("Aucune" to "") + availableLoopTracks.map { it.displayName to it.id }

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
) {
    val context = LocalContext.current
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val existingScenario = scenarioId?.let { id -> mjScenarios.firstOrNull { it.id == id } }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var scenes by remember { mutableStateOf(listOf(GameState.MjScene(title = "Scène 1"))) }
    var selectedSceneIndex by remember { mutableStateOf(0) }
    var markdownContent by remember { mutableStateOf(TextFieldValue("")) }
    var loaded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkDialogInitialType by remember { mutableStateOf("monster") }
    var showDeleteSceneDialog by remember { mutableStateOf(false) }
    var undoStack by remember { mutableStateOf(listOf<ScenarioSnapshot>()) }

    LaunchedEffect(scenarioId, mjScenarios) {
        if (!loaded) {
            existingScenario?.let { scenario ->
                title = scenario.title
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

    fun playSceneMusic(trackId: String?) {
        if (trackId.isNullOrBlank()) {
            MusicManager.stop()
            return
        }
        val track = availableLoopTracks.firstOrNull { it.id == trackId }
        if (track != null) {
            MusicManager.play(context, track.resId, track.displayName)
        }
    }

    fun saveScenario() {
        if (title.isNotBlank()) {
            val finalScenes = scenes.mapIndexed { i, scene -> scene.copy(order = i) }
            val updated = existingScenario?.copy(
                title = title,
                markdownContent = "",
                scenes = finalScenes
            ) ?: GameState.MjScenario(
                title = title,
                scenes = finalScenes,
                worldId = GameState.currentWorldId() ?: ""
            )
            if (existingScenario != null) GameState.updateMjScenario(updated, context)
            else GameState.addMjScenario(updated, context)
            onSaved()
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Le titre est obligatoire") }
        }
    }

    WorldBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Scénario", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
            containerColor = Color.Transparent,
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

                // Sélecteur de scènes avec réordonnancement et suppression
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(scenes.size, key = { "scene-chip-$it" }) { index ->
                        val scene = scenes[index]
                        FilterChip(
                            selected = selectedSceneIndex == index,
                            onClick = { selectedSceneIndex = index },
                            label = { Text(scene.title) },
                            leadingIcon = if (selectedSceneIndex == index) {
                                @Composable {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            } else null,
                            trailingIcon = if (selectedSceneIndex == index && scenes.size > 1) {
                                @Composable {
                                    IconButton(
                                        onClick = { showDeleteSceneDialog = true },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Supprimer la scène",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else null
                        )
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

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = MusicTracks.find { it.second == scene.musicTrackId }?.first ?: "Aucune",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Musique de la scène") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            MusicTracks.forEach { (label, trackId) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        pushUndo()
                                        scenes = scenes.mapIndexed { index, s ->
                                            if (index == selectedSceneIndex) s.copy(musicTrackId = trackId) else s
                                        }
                                        playSceneMusic(trackId)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
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
                    item { IconButton(onClick = { showLinkDialog = true }) { Icon(Icons.Default.Link, contentDescription = "Lien interne (monstre, équipement, PNJ...)") } }
                    item { IconButton(onClick = { insertLinePrefix("- ") }) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Liste") } }
                    item { IconButton(onClick = { insertLinePrefix("1. ") }) { Icon(Icons.Default.FormatListNumbered, contentDescription = "Liste numérotée") } }
                }

                OutlinedTextField(
                    value = markdownContent,
                    onValueChange = { updateCurrentScene(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 400.dp)
                        .padding(16.dp),
                    placeholder = { Text("Saisissez le contenu de la scène en markdown...") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                )
            }
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
            onSelect = { type, name -> insertInternalLink(type, name) },
            initialType = linkDialogInitialType
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
