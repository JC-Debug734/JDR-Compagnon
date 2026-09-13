package com.jc2.jdrcompagnon.ui.screens.mj

import kotlin.math.roundToInt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    currentWorld: WorldState? = null,
    onBack: () -> Unit
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

    val musicTracks = availableLoopTracks

    val currentTrack by MusicManager.currentTrack.collectAsState()
    val isPlaying by MusicManager.isPlaying.collectAsState()
    var selectedTrack by remember { mutableStateOf(currentTrack ?: musicTracks.first().displayName) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Musique", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (currentTrack != null) {
                        IconButton(onClick = { if (isPlaying) MusicManager.pause() else MusicManager.resume() }) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Lecture"
                            )
                        }
                        IconButton(onClick = { MusicManager.stop() }) {
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
            Text(
                text = "Réglages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

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
                        OutlinedButton(onClick = { MusicManager.pause() }) {
                            Icon(Icons.Default.Pause, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Pause")
                        }
                        OutlinedButton(onClick = { MusicManager.stop() }) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }

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
                            MusicManager.pause()
                        } else {
                            MusicManager.play(context, track.resId, track.displayName)
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
                    }
                }
            }
        }
    }
}