package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import com.jc2.jdrcompagnon.ui.components.WorldBackground

/**
 * Écran plein écran de lecture d'un scénario MJ (route Route.ScenarioReader).
 * Ne fait que poser un Scaffold + bouton retour autour de [ScenarioReaderContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioReaderScreen(
    scenarioId: String,
    onBack: () -> Unit,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
) {
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val scenario = remember(scenarioId, mjScenarios) {
        mjScenarios.firstOrNull { it.id == scenarioId }
    }

    WorldBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            ScenarioReaderContent(
                scenario = scenario,
                onOpenInternalLink = onOpenInternalLink,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

/**
 * Contenu de lecture d'un scénario, scène par scène : réutilisable en plein
 * écran (ScenarioReaderScreen) ou intégré directement dans un autre écran
 * (MjHomeScreen) sans navigation — c'est cette dernière utilisation qui
 * justifie l'extraction hors de ScenarioReaderScreen.
 *
 * - Affiche le scénario scène par scène avec animation de transition.
 * - Joue automatiquement la musique associée à la scène via MusicManager.
 * - Navigation via les touches fléchées gauche/droite (clavier / télécommande).
 * - Navigation via les gestes swipe horizontal sur écran tactile.
 * - Rendu des balises couleur, liens internes cliquables, markdown simple.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioReaderContent(
    scenario: GameState.MjScenario?,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val scenes = scenario?.scenes?.ifEmpty {
        listOf(GameState.MjScene(title = scenario.title, markdownContent = scenario.markdownContent))
    } ?: listOf()

    var currentIndex by remember(scenario?.id) { mutableIntStateOf(0) }
    val currentScene = scenes.getOrNull(currentIndex)
    var direction by remember(scenario?.id) { mutableIntStateOf(1) }
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showScenePanel by remember { mutableStateOf(false) }
    var showLinksPanel by remember { mutableStateOf(false) }

    // Auto-play musique de la scène
    LaunchedEffect(currentScene) {
        currentScene?.musicTrackId?.let { trackId ->
            playSceneMusic(context, trackId)
        }
    }

    fun goToScene(newIndex: Int) {
        if (newIndex in scenes.indices) {
            direction = if (newIndex > currentIndex) 1 else -1
            currentIndex = newIndex
        }
    }

    if (scenario == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Scénario introuvable", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = modifier) {
        // En-tête : titre du scénario, scène courante, action musique
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(scenario.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                currentScene?.title?.let {
                    Text(
                        text = "Scène ${currentIndex + 1}/${scenes.size} — $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            currentScene?.musicTrackId?.let { trackId ->
                val trackName = availableLoopTracks.firstOrNull { it.id.equals(trackId, ignoreCase = true) }?.displayName ?: trackId
                IconButton(onClick = { playSceneMusic(context, trackId) }) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Musique : $trackName")
                }
            }
            if (scenes.size > 1) {
                IconButton(onClick = { showScenePanel = true }) {
                    Icon(Icons.Default.List, contentDescription = "Toutes les scènes")
                }
            }
            IconButton(onClick = { showLinksPanel = true }) {
                Icon(Icons.Default.Groups, contentDescription = "Profils et liens du scénario")
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.isFocused }
                .onKeyEvent { keyEvent ->
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                goToScene(currentIndex - 1)
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                goToScene(currentIndex + 1)
                            }
                            true
                        }
                        else -> false
                    }
                }
                .pointerInput(scenes.size, currentIndex) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val threshold = 80f
                        when {
                            dragAmount < -threshold -> goToScene(currentIndex + 1)
                            dragAmount > threshold -> goToScene(currentIndex - 1)
                        }
                    }
                }
                .background(Color.Transparent)
        ) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            if (currentScene == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune scène à afficher", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        val enter = slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth })
                        val exit = slideOutHorizontally(targetOffsetX = { fullWidth -> -direction * fullWidth })
                        enter togetherWith exit
                    },
                    label = "scene_transition"
                ) { index ->
                    val scene = scenes[index]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PlainScenarioRenderer(
                            content = scene.markdownContent,
                            onLinkClick = onOpenInternalLink,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val links = remember(scene.markdownContent) {
                            extractInternalLinks(scene.markdownContent)
                        }
                        if (links.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Liens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            links.distinct().forEach { (type, name) ->
                                Surface(
                                    onClick = { onOpenInternalLink(type, name) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF2196F3).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3)),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (type) {
                                            "monster" -> "🐲 $name"
                                            "pnj", "npc" -> "👤 $name"
                                            "equipment" -> "⚔️ $name"
                                            "spell" -> "✨ $name"
                                            "rule" -> "📜 $name"
                                            else -> "$type:$name"
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = Color(0xFF2196F3),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (scenes.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { goToScene(currentIndex - 1) },
                    enabled = currentIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Scène précédente")
                }
                Text("${currentIndex + 1} / ${scenes.size}", style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = { goToScene(currentIndex + 1) },
                    enabled = currentIndex < scenes.size - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Scène suivante")
                }
            }
        }
    }

    if (showScenePanel) {
        ModalBottomSheet(onDismissRequest = { showScenePanel = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Scènes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                scenes.forEachIndexed { index, scene ->
                    ListItem(
                        headlineContent = { Text(scene.title) },
                        leadingContent = if (index == currentIndex) {
                            @Composable { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        modifier = Modifier.clickable {
                            goToScene(index)
                            showScenePanel = false
                        }
                    )
                }
            }
        }
    }

    if (showLinksPanel) {
        val allLinks = remember(scenario.id, scenes) {
            scenes.flatMap { extractInternalLinks(it.markdownContent) }.distinct()
        }
        ModalBottomSheet(onDismissRequest = { showLinksPanel = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Profils et liens du scénario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (allLinks.isEmpty()) {
                    Text(
                        "Aucun lien (monstre, PNJ, équipement...) dans ce scénario pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allLinks.forEach { (type, name) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = {
                                Text(
                                    when (type) {
                                        "monster" -> "Monstre"
                                        "pnj", "npc" -> "PNJ"
                                        "equipment" -> "Équipement"
                                        "spell" -> "Sort"
                                        "rule" -> "Règle"
                                        else -> type
                                    }
                                )
                            },
                            leadingContent = {
                                Text(
                                    when (type) {
                                        "monster" -> "🐲"
                                        "pnj", "npc" -> "👤"
                                        "equipment" -> "⚔️"
                                        "spell" -> "✨"
                                        "rule" -> "📜"
                                        else -> "🔗"
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            modifier = Modifier.clickable {
                                showLinksPanel = false
                                onOpenInternalLink(type, name)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun playSceneMusic(context: Context, trackId: String) {
    val track = availableLoopTracks.firstOrNull { it.id.equals(trackId, ignoreCase = true) }
    if (track != null) {
        MusicManager.play(context, track.resId, track.displayName)
    }
}
