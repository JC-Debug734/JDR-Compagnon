package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import kotlinx.coroutines.launch

/**
 * Événements de la bibliothèque rattachés à un élément (ville, lieu de la carte, environnement,
 * scène) : liste par type, tirage parmi eux (dé), ajout depuis la bibliothèque, création à la
 * volée (enregistrée dans la bibliothèque), modification (tap, répercutée partout) et retrait
 * (l'événement reste dans la bibliothèque). Le propriétaire ne stocke que les ids :
 * [onChanger] reçoit la nouvelle liste complète, à persister. Les ids d'événements supprimés de
 * la bibliothèque sont ignorés.
 */
@Composable
fun EvenementsLiesSection(
    worldId: String,
    campagneId: String?,
    evenementIds: List<String>,
    onChanger: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    titre: String? = "Événements",
) {
    val flux = remember(worldId) { EvenementDependencies.repository.observerEvenements(worldId) }
    val bibliotheque by flux.collectAsState(initial = emptyList())
    val parId = bibliotheque.associateBy { it.id }
    val lies = evenementIds.mapNotNull { parId[it] }
        .sortedWith(compareBy({ it.type.ordinal }, { it.titre.lowercase() }))
    val scope = rememberCoroutineScope()

    var afficherSelecteur by remember { mutableStateOf(false) }
    var afficherCreation by remember { mutableStateOf(false) }
    var enEdition by remember { mutableStateOf<Evenement?>(null) }
    var tirage by remember { mutableStateOf<Evenement?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (titre != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(titre, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { tirage = lies.randomOrNull() }, enabled = lies.isNotEmpty()) {
                    Icon(Icons.Default.Casino, contentDescription = "Tirer un événement")
                }
            }
        } else if (lies.isNotEmpty()) {
            OutlinedButton(onClick = { tirage = lies.randomOrNull() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tirer un événement")
            }
        }
        if (lies.isEmpty()) {
            Text(
                if (readOnly) "Aucun événement." else "Aucun événement lié. Ajoutez-en depuis la bibliothèque ou créez-en un.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        lies.forEach { evenement ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (readOnly) tirage = evenement else enEdition = evenement },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeEvenementBadge(evenement.type, taille = 28.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        evenement.titre,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(evenement.type.label, style = MaterialTheme.typography.labelSmall, color = evenement.type.couleur)
                }
                if (!readOnly) {
                    IconButton(onClick = { onChanger(evenementIds - evenement.id) }) {
                        Icon(Icons.Default.LinkOff, contentDescription = "Retirer ${evenement.titre}")
                    }
                }
            }
        }
        if (!readOnly) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { afficherSelecteur = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bibliothèque")
                }
                Button(onClick = { afficherCreation = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Créer")
                }
            }
        }
    }

    if (afficherSelecteur) {
        EvenementPickerDialog(
            worldId = worldId,
            campagneId = campagneId,
            exclus = evenementIds.toSet(),
            onDismiss = { afficherSelecteur = false },
            onValider = { choisis ->
                onChanger(evenementIds + choisis.map { it.id })
                afficherSelecteur = false
            }
        )
    }
    if (afficherCreation) {
        EvenementEditorDialog(
            initial = null,
            worldId = worldId,
            campagneParDefaut = campagneId,
            onDismiss = { afficherCreation = false },
            onSave = { evenement ->
                afficherCreation = false
                scope.launch {
                    EvenementDependencies.repository.sauvegarder(evenement)
                    onChanger(evenementIds + evenement.id)
                }
            }
        )
    }
    enEdition?.let { evenement ->
        EvenementEditorDialog(
            initial = evenement,
            worldId = worldId,
            onDismiss = { enEdition = null },
            onSave = { modifie ->
                enEdition = null
                scope.launch { EvenementDependencies.repository.sauvegarder(modifie) }
            }
        )
    }
    tirage?.let { evenement ->
        EventResultDialog(evenement = evenement, worldId = worldId, onDismiss = { tirage = null })
    }
}
