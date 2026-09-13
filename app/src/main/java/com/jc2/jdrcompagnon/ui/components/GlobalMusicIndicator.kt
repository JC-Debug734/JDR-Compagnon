package com.jc2.jdrcompagnon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jc2.jdrcompagnon.ui.MusicManager

/**
 * Indicateur global de musique pour la TopAppBar.
 * - Masqué si aucune piste active.
 * - Icône Pause si lecture, MusicNote si pause.
 * - Ouvre l'écran musique au clic.
 */
@Composable
fun GlobalMusicIndicator(
    onOpenMusic: () -> Unit
) {
    val currentTrack by MusicManager.currentTrack.collectAsState()
    val isPlaying by MusicManager.isPlaying.collectAsState()

    if (currentTrack == null) return

    IconButton(onClick = onOpenMusic) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.MusicNote,
            contentDescription = "Musique : $currentTrack",
        )
    }
}
