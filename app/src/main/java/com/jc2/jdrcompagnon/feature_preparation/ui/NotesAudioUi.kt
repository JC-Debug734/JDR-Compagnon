package com.jc2.jdrcompagnon.feature_preparation.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.data.NotesAudio
import com.jc2.jdrcompagnon.feature_preparation.domain.model.NoteAudio
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun duree(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Note rapide prise en jeu (menu latéral) : texte, et/ou enregistrement audio au micro. Une note
 * audio est rangée dans la séance dès l'arrêt de l'enregistrement ; fermer la fenêtre pendant un
 * enregistrement le garde aussi.
 */
@Composable
fun NoteRapideDialog(worldId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var texte by remember { mutableStateOf("") }
    var enregistre by remember { mutableStateOf(false) }
    var debut by remember { mutableLongStateOf(0L) }
    var ecoule by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }

    fun demarrer() {
        if (NotesAudio.demarrer(context)) {
            enregistre = true
            debut = System.currentTimeMillis()
            message = null
        } else {
            message = "Micro indisponible."
        }
    }

    fun arreter() {
        enregistre = false
        val resultat = NotesAudio.arreterEnregistrement()
        message = if (resultat == null) {
            "Enregistrement trop court, non gardé."
        } else {
            val (fichier, dureeMs) = resultat
            CarnetMj.ajouterNoteAudio(worldId, NoteAudio(fichier = fichier, date = System.currentTimeMillis(), dureeMs = dureeMs))
            "✔ Note audio gardée (${duree(dureeMs)})"
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        if (accordee) demarrer() else message = "Autorisez le micro pour enregistrer des notes audio."
    }

    LaunchedEffect(enregistre) {
        while (enregistre) {
            ecoule = System.currentTimeMillis() - debut
            delay(250)
        }
    }
    // Fenêtre fermée en cours d'enregistrement : la note est gardée.
    DisposableEffect(Unit) { onDispose { if (enregistre) arreter() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note rapide") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Texte") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        when {
                            enregistre -> arreter()
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> demarrer()
                            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = if (enregistre) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(if (enregistre) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (enregistre) "Arrêter (${duree(ecoule)})" else "Note audio")
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (enregistre) arreter()
                CarnetMj.noteRapide(worldId, texte)
                onDismiss()
            }) { Text(if (texte.isBlank()) "Fermer" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Notes audio écoutables (une à la fois) ; [supprimable] : croix pour en retirer une. */
@Composable
fun NotesAudioListe(worldId: String, notes: List<NoteAudio>, supprimable: Boolean = true) {
    if (notes.isEmpty()) return
    val context = LocalContext.current
    var enLecture by remember { mutableStateOf<String?>(null) }
    val heure = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    DisposableEffect(Unit) { onDispose { NotesAudio.arreterLecture() } }
    Column {
        notes.forEach { note ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (enLecture == note.id) {
                        NotesAudio.arreterLecture()
                        enLecture = null
                    } else {
                        enLecture = note.id
                        NotesAudio.lire(context, note.fichier) { if (enLecture == note.id) enLecture = null }
                    }
                }) {
                    Icon(
                        if (enLecture == note.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (enLecture == note.id) "Arrêter" else "Écouter",
                        tint = ForcedDarkPalette.AccentGold,
                    )
                }
                Text(
                    "🎙 ${heure.format(Date(note.date))} · ${duree(note.dureeMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (supprimable) {
                    IconButton(onClick = {
                        if (enLecture == note.id) NotesAudio.arreterLecture()
                        CarnetMj.supprimerNoteAudio(context, worldId, note.id)
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Supprimer la note audio", tint = PrepHintColor)
                    }
                }
            }
        }
    }
}
