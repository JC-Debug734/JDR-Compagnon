package com.jc2.jdrcompagnon.ui.screens.mj

import kotlin.math.roundToInt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.MusicSettings
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import com.jc2.jdrcompagnon.ui.ImportedMusicStore
import com.jc2.jdrcompagnon.ui.LoopTrack
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    currentWorld: WorldState? = null,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val musicSettings by GameState.musicSettings.collectAsState()
    var volume by remember { mutableStateOf(musicSettings.volume) }

    LaunchedEffect(musicSettings) {
        volume = musicSettings.volume
    }

    // Debounce persistance du volume (300ms) pour éviter les écritures disque excessives.
    LaunchedEffect(volume) {
        delay(300)
        GameState.saveMusicSettings(musicSettings.copy(volume = volume))
    }

    // Relu à chaque import/suppression (availableLoopTracks inclut les pistes importées).
    val importedTracks by ImportedMusicStore.tracks.collectAsState()
    val musicTracks = remember(importedTracks) { availableLoopTracks }

    val currentTrack by MusicManager.currentTrack.collectAsState()
    val isPlaying by MusicManager.isPlaying.collectAsState()
    var selectedTrack by remember { mutableStateOf(currentTrack ?: musicTracks.first().displayName) }

    // Nom saisi dans la boîte de renommage.
    var pendingName by remember { mutableStateOf("") }
    var trackToRename by remember { mutableStateOf<LoopTrack?>(null) }
    var trackToDelete by remember { mutableStateOf<LoopTrack?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Musique", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (currentTrack != null) {
                        IconButton(onClick = { if (isPlaying) MusicManager.pauseByUser() else MusicManager.resume() }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Lecture"
                            )
                        }
                        IconButton(onClick = { MusicManager.stopByUser() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Arrêter la musique")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Volume", fontWeight = FontWeight.Bold)
                        Text("${(volume * 100).roundToInt()} %")
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it
                            MusicManager.setVolume(it)
                        },
                        valueRange = 0f..1f
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(onClick = { MusicManager.pauseByUser() }) {
                            Icon(Icons.Default.Pause, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Pause")
                        }
                        OutlinedButton(onClick = { MusicManager.stopByUser() }) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Playlists d'ambiance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                // Import d'un fichier audio : via l'outil IMPORT (renommage possible ici ensuite).
                Button(onClick = { com.jc2.jdrcompagnon.feature_import.ImportNavigation.ouvrir() }) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Importer")
                }
            }

            musicTracks.forEach { track ->
                val selected = selectedTrack == track.displayName
                val active = currentTrack == track.displayName && isPlaying
                Surface(
                    onClick = {
                        selectedTrack = track.displayName
                        if (active) {
                            MusicManager.pauseByUser()
                        } else {
                            MusicManager.play(context, track)
                            MusicManager.setVolume(volume)
                        }
                        GameState.saveMusicSettings(musicSettings.copy(lastTrackName = track.displayName))
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (active) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = track.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (active) {
                            Text(
                                text = "LECTURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (track.isImported) {
                            IconButton(onClick = { trackToRename = track; pendingName = track.displayName }) {
                                Icon(Icons.Default.Edit, contentDescription = "Renommer")
                            }
                            IconButton(onClick = { trackToDelete = track }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Text(
                "Les musiques importées sont copiées dans l'application et utilisables dans les scènes et les environnements.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }

    trackToRename?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToRename = null },
            title = { Text("Renommer la musique") },
            text = {
                OutlinedTextField(
                    value = pendingName,
                    onValueChange = { pendingName = it },
                    label = { Text("Nom affiché") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(enabled = pendingName.isNotBlank(), onClick = {
                    ImportedMusicStore.rename(context, track.id, pendingName)
                    trackToRename = null
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { trackToRename = null }) { Text("Annuler") } }
        )
    }

    trackToDelete?.let { track ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            title = { Text("Supprimer la musique") },
            text = { Text("Supprimer « ${track.displayName} » ? Les scènes qui l'utilisent n'auront plus de musique.") },
            confirmButton = {
                TextButton(onClick = {
                    if (currentTrack == track.displayName) MusicManager.stop()
                    ImportedMusicStore.remove(context, track.id)
                    trackToDelete = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { trackToDelete = null }) { Text("Annuler") } }
        )
    }
}