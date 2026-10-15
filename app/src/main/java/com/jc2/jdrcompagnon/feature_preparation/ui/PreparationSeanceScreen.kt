package com.jc2.jdrcompagnon.feature_preparation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.domain.model.CarnetMonde
import com.jc2.jdrcompagnon.feature_preparation.domain.model.PreparationSeance
import com.jc2.jdrcompagnon.feature_preparation.domain.model.SecretIndice
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import java.text.DateFormat
import java.util.Date

/**
 * Carnet du MJ pour le monde courant, indépendant des campagnes. En haut, la préparation de la
 * prochaine séance (méthode « Lazy DM ») : où on s'est arrêté, ouverture, agenda (scénarios,
 * combats, épreuves, lieux, boutiques, PNJ...), secrets à placer, récompenses, notes. En bas, les
 * secrets et indices et les fils en suspens du monde (filtrables par campagne), et les séances
 * jouées. Tout est enregistré à la saisie.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreparationSeanceScreen(
    onBack: () -> Unit,
    navigation: NavigationAgenda,
) {
    val monde by GameState.currentWorld.collectAsState()
    val worldId = monde?.id ?: ""
    val carnets by CarnetMj.carnets.collectAsState()
    val carnet = carnets[worldId] ?: CarnetMonde()
    val prep = carnet.preparation
    fun maj(nouvelle: PreparationSeance) = CarnetMj.enregistrerPreparation(worldId, nouvelle)
    val ownerKey = "preparation:$worldId"
    var cloture by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Préparation — séance ${prep.numero}", fontWeight = FontWeight.Bold)
                        monde?.name?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = PrepHintColor) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CollapsibleSectionCard(ownerKey = ownerKey, title = "Où on s'est arrêté") {
                ChampPrep(prep.ouOnSEstArrete, "Situation du groupe à la fin de la dernière séance") { maj(prep.copy(ouOnSEstArrete = it)) }
            }

            CollapsibleSectionCard(ownerKey = ownerKey, title = "Ouverture") {
                ChampPrep(prep.ouverture, "Première scène : une entrée forte, au cœur de l'action") { maj(prep.copy(ouverture = it)) }
            }

            CollapsibleSectionCard(ownerKey = ownerKey, title = "Agenda", count = prep.agenda.count { !it.fait }) {
                AgendaContent(worldId, prep.agenda, navigation, modifiable = true)
            }

            SecretsAPlacer(worldId, carnet, ownerKey)

            CollapsibleSectionCard(ownerKey = ownerKey, title = "Récompenses", count = prep.recompensesPrevues.size) {
                RecompensesContent(worldId, prep)
            }

            CollapsibleSectionCard(ownerKey = ownerKey, title = "Notes") {
                ChampPrep(prep.notes, "Rappels, et notes prises en jeu depuis le menu latéral") { maj(prep.copy(notes = it)) }
                NotesAudioListe(worldId, prep.notesAudio)
            }

            Button(onClick = { cloture = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Clôturer la séance ${prep.numero}")
            }

            // --- Carnet du monde : secrets et fils, filtrables par campagne ---
            val campagnes = rememberCampagnesMonde(worldId)
            var filtre by rememberSaveable { mutableStateOf<String?>(null) }
            if (campagnes.none { it.id == filtre }) filtre = null
            Text(
                "CARNET DU MONDE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = ForcedDarkPalette.AccentGold,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (campagnes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val couleurs = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = ForcedDarkPalette.AccentGold)
                    FilterChip(selected = filtre == null, onClick = { filtre = null }, label = { Text("Toutes les campagnes") }, colors = couleurs)
                    campagnes.forEach { c ->
                        FilterChip(selected = filtre == c.id, onClick = { filtre = c.id }, label = { Text(c.title) }, colors = couleurs)
                    }
                }
            }

            CollapsibleSectionCard(
                ownerKey = ownerKey,
                title = "Secrets et indices",
                count = carnet.secrets.count { !it.decouvert && (filtre == null || it.campagneId == filtre) },
            ) {
                SecretsContent(worldId, carnet, filtre)
            }

            CollapsibleSectionCard(
                ownerKey = ownerKey,
                title = "Fils en suspens",
                count = carnet.fils.count { !it.resolu && (filtre == null || it.campagneId == filtre) },
            ) {
                FilsContent(worldId, carnet, filtre)
            }

            if (carnet.seances.isNotEmpty()) {
                CollapsibleSectionCard(ownerKey = ownerKey, title = "Séances jouées", count = carnet.seances.size, defaultExpanded = false) {
                    val format = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
                    carnet.seances.sortedByDescending { it.numero }.forEach { s ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("Séance ${s.numero} — ${format.format(Date(s.date))}", color = ForcedDarkPalette.AccentGold, fontWeight = FontWeight.Bold)
                            if (s.resume.isNotBlank()) Text(s.resume, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            if (s.recompenses.isNotBlank()) Text("🎁 ${s.recompenses}", color = PrepHintColor, style = MaterialTheme.typography.bodySmall)
                            if (s.notes.isNotBlank()) Text(s.notes, color = PrepHintColor, style = MaterialTheme.typography.bodySmall)
                            NotesAudioListe(worldId, s.notesAudio)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (cloture) {
            ClotureDialog(
                numero = prep.numero,
                secretsDecouverts = carnet.secrets.count { it.decouvert && it.decouvertSeance == prep.numero },
                onDismiss = { cloture = false },
                onValider = { resume -> CarnetMj.cloturerSeance(worldId, resume); cloture = false },
            )
        }
    }
}

@Composable
private fun ChampPrep(valeur: String, aide: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        placeholder = { Text(aide, color = PrepHintColor.copy(alpha = 0.6f)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = ForcedDarkPalette.AccentGold,
            unfocusedBorderColor = PrepHintColor.copy(alpha = 0.5f),
        ),
    )
}

/**
 * Secrets étoilés pour cette séance, puis (dépliable) les autres secrets non découverts pour en
 * ajouter. Cocher un secret le marque découvert pendant la séance.
 */
@Composable
private fun SecretsAPlacer(worldId: String, carnet: CarnetMonde, ownerKey: String) {
    val pnjs = rememberPnjsMonde(worldId)
    var voirAutres by remember { mutableStateOf(false) }
    val ids = carnet.preparation.secretIds
    val retenus = ids.mapNotNull { id -> carnet.secrets.firstOrNull { it.id == id } }
    val autres = carnet.secrets.filter { !it.decouvert && it.id !in ids }
    fun detail(s: SecretIndice) =
        listOfNotNull(
            s.pnjIds.mapNotNull { id -> pnjs.firstOrNull { it.id == id }?.name }.joinToString(", ").ifBlank { null }?.let { "👤 $it" },
            s.lieu.ifBlank { null }?.let { "📍 $it" },
        ).joinToString(" · ")

    CollapsibleSectionCard(ownerKey = ownerKey, title = "Secrets à placer", count = retenus.count { !it.decouvert }) {
        if (retenus.isEmpty()) {
            Text(
                "Étoilez une dizaine de secrets que les joueurs pourraient découvrir. Peu importe comment : ils viendront d'un PNJ, d'un lieu ou d'un objet selon ce que font les joueurs.",
                style = MaterialTheme.typography.bodyMedium,
                color = PrepHintColor,
            )
        }
        retenus.forEach { s ->
            SecretRow(s, detail(s), aPlacer = true, onBasculerAPlacer = { CarnetMj.basculerAPlacer(worldId, s.id) }, onClick = null)
        }
        if (autres.isNotEmpty()) {
            TextButton(onClick = { voirAutres = !voirAutres }) {
                Text((if (voirAutres) "Masquer" else "Ajouter parmi") + " les ${autres.size} autres secrets non découverts", color = ForcedDarkPalette.AccentGold)
            }
            if (voirAutres) {
                autres.forEach { s ->
                    SecretRow(s, detail(s), aPlacer = false, onBasculerAPlacer = { CarnetMj.basculerAPlacer(worldId, s.id) }, onClick = null)
                }
            }
        } else if (carnet.secrets.isEmpty()) {
            Text("Ajoutez vos secrets dans « Secrets et indices », plus bas.", style = MaterialTheme.typography.bodySmall, color = PrepHintColor)
        }
    }
}

@Composable
private fun ClotureDialog(numero: Int, secretsDecouverts: Int, onDismiss: () -> Unit, onValider: (String) -> Unit) {
    var resume by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clôturer la séance $numero") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Le résumé devient le « où on s'est arrêté » de la séance ${numero + 1}. Les entrées d'agenda non cochées passent à la séance suivante ; secrets non découverts et fils ouverts sont conservés." +
                        if (secretsDecouverts > 0) "\n$secretsDecouverts secret(s) découvert(s) pendant cette séance." else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = resume,
                    onValueChange = { resume = it },
                    label = { Text("Ce qui s'est passé, où en est le groupe") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onValider(resume) }) { Text("Clôturer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
