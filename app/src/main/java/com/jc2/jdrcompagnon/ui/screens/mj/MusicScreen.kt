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
            currentTrack?.let { track -> MusicProgressCard(track, isPlaying) }

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
                        // Pause garde la position : « Reprendre » relance la piste au même endroit.
                        OutlinedButton(
                            onClick = { if (isPlaying) MusicManager.pauseByUser() else MusicManager.resume() },
                            enabled = currentTrack != null,
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPlaying) "Pause" else "Reprendre")
                        }
                        OutlinedButton(onClick = { MusicManager.stopByUser() }) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }

            // L'import de fichiers audio passe par l'outil IMPORT (renommage possible ici ensuite).
            Text(
                text = "Playlists d'ambiance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

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
/**
 * Progression de la piste en cours : position / durée, rafraîchies deux fois par seconde, et
 * curseur pour se déplacer dans la piste. Une piste YouTube (lecture externe, durée inconnue)
 * n'affiche que son nom.
 */
@Composable
private fun MusicProgressCard(trackName: String, isPlaying: Boolean) {
    var position by remember { mutableStateOf(MusicManager.positionMs()) }
    var duration by remember { mutableStateOf(MusicManager.durationMs()) }
    // Position choisie au curseur, tant que le doigt n'est pas relevé (sinon le rafraîchissement
    // ramènerait le curseur en arrière pendant le geste).
    var seeking by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(trackName, isPlaying) {
        while (true) {
            position = MusicManager.positionMs()
            duration = MusicManager.durationMs()
            if (!isPlaying) break
            delay(500)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isPlaying) Icons.Default.MusicNote else Icons.Default.Pause,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(trackName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (isPlaying) "EN LECTURE" else "EN PAUSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (duration > 0) {
                val shown = seeking ?: (position.toFloat() / duration).coerceIn(0f, 1f)
                Slider(
                    value = shown,
                    onValueChange = { seeking = it },
                    onValueChangeFinished = {
                        seeking?.let { fraction ->
                            val target = (fraction * duration).toInt()
                            MusicManager.seekTo(target)
                            position = target
                        }
                        seeking = null
                    },
                    valueRange = 0f..1f,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMusicTime((shown * duration).toInt()), style = MaterialTheme.typography.bodySmall)
                    Text(formatMusicTime(duration), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatMusicTime(ms: Int): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
