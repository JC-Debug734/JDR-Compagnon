package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironmentImageStore
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.WeatherSoundManager
import kotlin.math.roundToInt

/**
 * Ligne affichée sous le titre de la scène : miniature (ou icône) et nom de l'environnement de
 * la scène, ou "Pas d'environnement" s'il n'y en a pas.
 */
@Composable
internal fun SceneEnvironmentLine(environment: Environnement?) {
    val context = LocalContext.current
    val thumbnail = environment?.imageFileName?.let { fileName ->
        remember(fileName) {
            runCatching {
                val file = EnvironmentImageStore.fichier(context, fileName)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Terrain,
                contentDescription = null,
                tint = if (environment != null) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = environment?.let { "Environnement : ${it.nom}" } ?: "Pas d'environnement",
            style = MaterialTheme.typography.bodySmall,
            color = if (environment != null) Color.White else Color.White.copy(alpha = 0.6f),
        )
    }
}

/**
 * Barre d'outils de la scène en lecture : table d'événements, table de loot, boutique et
 * événements liés (bibliothèque) en
 * icônes côte à côte (appui long = nom), puis à droite liste des scènes, profils/liens,
 * validation de la scène, et l'icône son toujours visible en dernier — appui simple
 * = musique de la scène on/off, double appui = réglage du volume (musique + effets météo).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SceneToolsBar(
    tableEvenementsNom: String?,
    tableLootNom: String?,
    onTirerEvenement: () -> Unit,
    onTirerLoot: () -> Unit,
    musicPlaying: Boolean,
    musicTrackName: String?,
    onToggleMusic: () -> Unit,
    // null : scénario d'une seule scène, pas de liste à ouvrir.
    onOpenScenes: (() -> Unit)? = null,
    onOpenLinks: () -> Unit = {},
    // null : pas de scène courante (rien à valider).
    sceneValidated: Boolean? = null,
    onToggleValidated: () -> Unit = {},
    // null : aucune boutique associée à la scène (pas d'icône).
    boutiqueNom: String? = null,
    onOpenBoutique: () -> Unit = {},
    // 0 : aucun événement de la bibliothèque lié à la scène ni à son environnement (pas d'icône).
    nombreEvenements: Int = 0,
    onOpenEvenements: () -> Unit = {},
) {
    val context = LocalContext.current
    var showVolumeDialog by remember { mutableStateOf(false) }
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tableEvenementsNom?.let { nom ->
            ToolIcon(
                icon = Icons.Default.Casino,
                description = "Table d'événements : $nom (appuyer pour tirer)",
                onClick = onTirerEvenement,
                onLongClick = { toast("Table d'événements : $nom — appuyer pour tirer") },
            )
        }
        tableLootNom?.let { nom ->
            ToolIcon(
                icon = Icons.Default.Backpack,
                description = "Table de loot : $nom (appuyer pour tirer)",
                onClick = onTirerLoot,
                onLongClick = { toast("Table de loot : $nom — appuyer pour tirer") },
            )
        }
        boutiqueNom?.let { nom ->
            ToolIcon(
                icon = Icons.Default.Storefront,
                description = "Boutique : $nom (appuyer pour voir services et tarifs)",
                onClick = onOpenBoutique,
                onLongClick = { toast("Boutique : $nom — appuyer pour voir services et tarifs") },
            )
        }
        if (nombreEvenements > 0) {
            ToolIcon(
                icon = Icons.Default.AutoStories,
                description = "Événements de la scène ($nombreEvenements)",
                onClick = onOpenEvenements,
                onLongClick = { toast("Événements de la scène et de son environnement ($nombreEvenements)") },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        onOpenScenes?.let { open ->
            ToolIcon(
                icon = Icons.AutoMirrored.Filled.List,
                description = "Toutes les scènes",
                onClick = open,
                onLongClick = { toast("Toutes les scènes") },
            )
        }
        ToolIcon(
            icon = Icons.Default.Groups,
            description = "Profils et liens du scénario",
            onClick = onOpenLinks,
            onLongClick = { toast("Profils et liens du scénario") },
        )
        sceneValidated?.let { validated ->
            ToolIcon(
                icon = if (validated) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                description = if (validated) "Scène validée (suivi) — retirer la validation" else "Marquer la scène comme validée (suivi)",
                onClick = onToggleValidated,
                onLongClick = { toast(if (validated) "Scène validée — appuyer pour retirer" else "Marquer la scène comme validée") },
            )
        }
        ToolIcon(
            icon = if (musicPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            description = (if (musicPlaying) "Couper la musique" else "Lancer la musique") +
                (musicTrackName?.let { " : $it" } ?: "") + " — double appui : volume",
            highlighted = musicPlaying,
            onClick = onToggleMusic,
            onDoubleClick = { showVolumeDialog = true },
            onLongClick = { toast(musicTrackName?.let { "Musique : $it — double appui : volume" } ?: "Aucune musique — double appui : volume") },
        )
    }

    if (showVolumeDialog) {
        SoundVolumeDialog(onDismiss = { showVolumeDialog = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolIcon(
    description: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent)
            .combinedClickable(
                onClickLabel = description,
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content()
        else if (icon != null) Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/**
 * Réglage rapide du son depuis la lecture de scénario : volume de la musique et des effets
 * sonores météo, appliqués en direct et enregistrés au relâchement du curseur.
 */
@Composable
private fun SoundVolumeDialog(onDismiss: () -> Unit) {
    val musicSettings by GameState.musicSettings.collectAsState()
    val weatherSettings by GameState.weatherSoundSettings.collectAsState()
    var musicVolume by remember { mutableFloatStateOf(musicSettings.volume) }
    var weatherVolume by remember { mutableFloatStateOf(weatherSettings.volume) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Volume", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                VolumeSlider(
                    icon = Icons.Default.MusicNote,
                    label = "Musique",
                    value = musicVolume,
                    onValueChange = {
                        musicVolume = it
                        MusicManager.setVolume(it)
                    },
                    onValueChangeFinished = { GameState.saveMusicSettings(musicSettings.copy(volume = musicVolume)) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                VolumeSlider(
                    icon = Icons.Default.Thunderstorm,
                    label = "Effets météo",
                    value = weatherVolume,
                    onValueChange = {
                        weatherVolume = it
                        WeatherSoundManager.setVolume(it)
                    },
                    onValueChangeFinished = {
                        GameState.saveWeatherSoundSettings(weatherSettings.copy(volume = weatherVolume))
                        ScenarioClockState.refreshAmbientSound()
                    },
                )
                if (!weatherSettings.enabled) {
                    Text(
                        "Effets météo désactivés (horloge de scénario).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun VolumeSlider(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("${(value * 100).roundToInt()} %", style = MaterialTheme.typography.bodySmall)
    }
    Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished)
}
