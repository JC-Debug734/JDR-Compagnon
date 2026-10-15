package com.jc2.jdrcompagnon.feature_preparation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.domain.model.CarnetMonde
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Aide de séance, ouverte depuis le menu latéral pendant la partie : l'essentiel de la
 * préparation de la séance en cours, en lecture rapide. Seules les rubriques remplies sont
 * affichées ; les entrées de l'agenda s'ouvrent ou se lancent d'un appui, et se cochent comme les
 * secrets et les fils.
 */
@Composable
fun AideSeanceDialog(
    onDismiss: () -> Unit,
    navigation: NavigationAgenda,
    onOpenPreparation: () -> Unit,
) {
    val monde by GameState.currentWorld.collectAsState()
    val worldId = monde?.id ?: ""
    val carnets by CarnetMj.carnets.collectAsState()
    val carnet = carnets[worldId] ?: CarnetMonde()
    val prep = carnet.preparation
    val pnjs = rememberPnjsMonde(worldId)

    val secrets = prep.secretIds.mapNotNull { id -> carnet.secrets.firstOrNull { it.id == id } }
    val fils = carnet.fils.filterNot { it.resolu }
    val vide = listOf(prep.ouOnSEstArrete, prep.ouverture, prep.notes).all { it.isBlank() } && prep.notesAudio.isEmpty() &&
        prep.agenda.isEmpty() && secrets.isEmpty() && fils.isEmpty() && prep.recompensesPrevues.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Séance ${prep.numero}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (vide) {
                    Text("Rien n'est encore préparé pour cette séance.", color = PrepHintColor)
                }
                Texte("Où on s'est arrêté", prep.ouOnSEstArrete)
                Texte("Ouverture", prep.ouverture)

                if (prep.agenda.isNotEmpty()) {
                    Rubrique("Agenda")
                    AgendaContent(worldId, prep.agenda, navigation, modifiable = false)
                }

                if (secrets.isNotEmpty()) {
                    Rubrique("Secrets à placer (${secrets.count { !it.decouvert }})")
                    secrets.sortedBy { it.decouvert }.forEach { s ->
                        val connu = s.pnjIds.mapNotNull { id -> pnjs.firstOrNull { it.id == id }?.name }.joinToString(", ")
                        SecretRow(s, if (connu.isBlank()) "" else "👤 $connu", aPlacer = null, onBasculerAPlacer = {}, onClick = null)
                    }
                }

                if (fils.isNotEmpty()) {
                    Rubrique("Fils en suspens (${fils.size})")
                    fils.forEach { fil ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = { CarnetMj.basculerResolu(worldId, fil.id) },
                                colors = CheckboxDefaults.colors(uncheckedColor = Color.White),
                            )
                            Text(fil.texte, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (prep.recompensesPrevues.isNotEmpty()) {
                    Rubrique("Récompenses" + if (prep.recompensesDistribuees.isNotBlank()) " (données)" else "")
                    prep.recompensesPrevues.forEach { Text("• ${it.resume}", color = Color.White, style = MaterialTheme.typography.bodyMedium) }
                }

                Texte("Notes", prep.notes)
                if (prep.notesAudio.isNotEmpty()) {
                    if (prep.notes.isBlank()) Rubrique("Notes")
                    NotesAudioListe(worldId, prep.notesAudio, supprimable = false)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer", color = ForcedDarkPalette.AccentGold) } },
        dismissButton = { TextButton(onClick = onOpenPreparation) { Text("Préparation complète", color = ForcedDarkPalette.AccentGold) } },
    )
}

@Composable
private fun Rubrique(titre: String) {
    Text(
        titre.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = ForcedDarkPalette.AccentGold,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun ColumnScope.Texte(titre: String, texte: String) {
    if (texte.isBlank()) return
    Rubrique(titre)
    Text(texte, color = Color.White, style = MaterialTheme.typography.bodyMedium)
}
