package com.jc2.jdrcompagnon.feature_preparation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.ui.LocationField
import com.jc2.jdrcompagnon.feature_group.ui.rememberLieuxConnus
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.domain.ApparitionsPnj
import com.jc2.jdrcompagnon.feature_preparation.domain.model.CarnetMonde
import com.jc2.jdrcompagnon.feature_preparation.domain.model.FilEnSuspens
import com.jc2.jdrcompagnon.feature_preparation.domain.model.SecretIndice
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

internal val PrepHintColor = Color.White.copy(alpha = 0.8f)

/** PNJ du monde, pour les choix de PNJ des secrets, fils et séances. */
@Composable
internal fun rememberPnjsMonde(worldId: String): List<Character> {
    val characters by GameState.characters.collectAsState()
    return remember(characters, worldId) { CarnetMj.pnjsDuMonde(worldId, characters) }
}

/** Campagnes et scénarios du monde (étiquettes facultatives des secrets et des fils). */
@Composable
internal fun rememberCampagnesMonde(worldId: String): List<GameState.MjCampaign> {
    val campagnes by GameState.mjCampaigns.collectAsState()
    return remember(campagnes, worldId) { campagnes.filter { it.worldId == worldId }.sortedBy { it.title.lowercase() } }
}

@Composable
internal fun rememberScenariosMonde(worldId: String): List<GameState.MjScenario> {
    val scenarios by GameState.mjScenarios.collectAsState()
    return remember(scenarios, worldId) { ChapitresScenarios.trier(scenarios.filter { it.worldId == worldId }) }
}

private fun nomsPnj(ids: List<String>, pnjs: List<Character>): String =
    ids.mapNotNull { id -> pnjs.firstOrNull { it.id == id }?.name }.joinToString(", ")

// ---------------------------------------------------------------------------------------------
// Secrets et indices
// ---------------------------------------------------------------------------------------------

/**
 * Secrets et indices du monde ([filtreCampagneId] non null : seulement ceux de cette campagne).
 * À découvrir d'abord, découverts repliés. L'étoile ajoute le secret aux secrets « à placer » de
 * la prochaine séance.
 */
@Composable
fun ColumnScope.SecretsContent(worldId: String, carnet: CarnetMonde, filtreCampagneId: String?) {
    val pnjs = rememberPnjsMonde(worldId)
    val campagnes = rememberCampagnesMonde(worldId)
    val scenarios = rememberScenariosMonde(worldId)
    var edition by remember { mutableStateOf<SecretIndice?>(null) }
    var creation by remember { mutableStateOf(false) }
    var coller by remember { mutableStateOf(false) }
    var voirDecouverts by remember { mutableStateOf(false) }

    val secrets = carnet.secrets.filter { filtreCampagneId == null || it.campagneId == filtreCampagneId }
    val aDecouvrir = secrets.filterNot { it.decouvert }
    val decouverts = secrets.filter { it.decouvert }
    fun detail(s: SecretIndice) = detailSecret(s, pnjs, campagnes.takeIf { filtreCampagneId == null }.orEmpty(), scenarios)

    if (secrets.isEmpty()) {
        Text(
            "Notez ici ce que les joueurs peuvent découvrir, relié aux PNJ qui le connaissent. Cochez-le quand ils l'apprennent.",
            style = MaterialTheme.typography.bodyMedium,
            color = PrepHintColor,
        )
    }
    aDecouvrir.forEach { secret ->
        SecretRow(
            secret = secret,
            detail = detail(secret),
            aPlacer = secret.id in carnet.preparation.secretIds,
            onBasculerAPlacer = { CarnetMj.basculerAPlacer(worldId, secret.id) },
            onClick = { edition = secret },
        )
    }
    if (decouverts.isNotEmpty()) {
        TextButton(onClick = { voirDecouverts = !voirDecouverts }) {
            Text((if (voirDecouverts) "Masquer" else "Voir") + " les découverts (${decouverts.size})", color = ForcedDarkPalette.AccentGold)
        }
        if (voirDecouverts) {
            decouverts.forEach { secret ->
                SecretRow(secret = secret, detail = detail(secret), aPlacer = null, onBasculerAPlacer = {}, onClick = { edition = secret })
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { creation = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ajouter", color = Color.White)
        }
        OutlinedButton(onClick = { coller = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ContentPaste, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Coller une liste", color = Color.White)
        }
    }

    if (creation || edition != null) {
        SecretEditorDialog(
            worldId = worldId,
            initial = edition ?: SecretIndice(texte = "", campagneId = filtreCampagneId),
            nouveau = edition == null,
            pnjs = pnjs,
            campagnes = campagnes,
            scenarios = scenarios,
            onDismiss = { creation = false; edition = null },
        )
    }
    if (coller) {
        CollerSecretsDialog(
            onDismiss = { coller = false },
            onValider = { CarnetMj.ajouterSecrets(worldId, it, filtreCampagneId); coller = false },
        )
    }
}

private fun detailSecret(
    secret: SecretIndice,
    pnjs: List<Character>,
    campagnes: List<GameState.MjCampaign>,
    scenarios: List<GameState.MjScenario>,
): String = listOfNotNull(
    nomsPnj(secret.pnjIds, pnjs).ifBlank { null }?.let { "👤 $it" },
    secret.campagneId?.let { id -> campagnes.firstOrNull { it.id == id } }?.let { "🗺 ${it.title}" },
    secret.scenarioId?.let { id -> scenarios.firstOrNull { it.id == id } }?.let { "📖 ${ChapitresScenarios.titreNumerote(it)}" },
    secret.lieu.ifBlank { null }?.let { "📍 $it" },
    secret.decouvertSeance?.takeIf { secret.decouvert }?.let { "découvert en séance $it" },
).joinToString(" · ")

/**
 * Ligne d'un secret : case « découvert », texte, détails, et étoile « à placer » ([aPlacer] null
 * = pas d'étoile, pour un secret déjà découvert).
 */
@Composable
internal fun SecretRow(
    secret: SecretIndice,
    detail: String,
    aPlacer: Boolean?,
    onBasculerAPlacer: () -> Unit,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = secret.decouvert,
            onCheckedChange = { CarnetMj.basculerDecouvert(secret.id) },
            colors = CheckboxDefaults.colors(checkedColor = ForcedDarkPalette.AccentGold, uncheckedColor = Color.White),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                secret.texte,
                color = if (secret.decouvert) PrepHintColor else Color.White,
                textDecoration = if (secret.decouvert) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = PrepHintColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (aPlacer != null) {
            IconButton(onClick = onBasculerAPlacer) {
                Icon(
                    if (aPlacer) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (aPlacer) "Retirer de la prochaine séance" else "À placer à la prochaine séance",
                    tint = if (aPlacer) ForcedDarkPalette.AccentGold else PrepHintColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecretEditorDialog(
    worldId: String,
    initial: SecretIndice,
    nouveau: Boolean,
    pnjs: List<Character>,
    campagnes: List<GameState.MjCampaign>,
    scenarios: List<GameState.MjScenario>,
    onDismiss: () -> Unit,
) {
    var texte by remember { mutableStateOf(initial.texte) }
    var pnjIds by remember { mutableStateOf(initial.pnjIds) }
    var campagneId by remember { mutableStateOf(initial.campagneId) }
    var scenarioId by remember { mutableStateOf(initial.scenarioId) }
    var lieu by remember { mutableStateOf(initial.lieu) }
    var choixPnj by remember { mutableStateOf(false) }
    // Scénarios proposés : ceux de la campagne choisie, sinon tous ceux du monde.
    val scenariosProposes = campagneId?.let { id -> campagnes.firstOrNull { it.id == id } }
        ?.let { c -> scenarios.filter { it.id in c.scenarioIds } } ?: scenarios

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (nouveau) "Nouveau secret" else "Modifier le secret") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Ce que les joueurs peuvent découvrir *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                PnjChips(pnjIds = pnjIds, pnjs = pnjs, titre = "Qui le connaît ?", onRetirer = { pnjIds = pnjIds - it }, onAjouter = { choixPnj = true })
                ChoixListe(
                    label = "Campagne (facultatif)",
                    valeur = campagneId,
                    options = campagnes.map { it.id to it.title },
                    onChoix = { campagneId = it },
                )
                ChoixListe(
                    label = "Scénario (facultatif)",
                    valeur = scenarioId,
                    options = scenariosProposes.map { it.id to ChapitresScenarios.titreNumerote(it) },
                    onChoix = { scenarioId = it },
                )
                LocationField(
                    value = lieu,
                    onValueChange = { lieu = it },
                    label = "Où le découvrir",
                    lieux = rememberLieuxConnus(campagneId),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = texte.isNotBlank(),
                onClick = {
                    CarnetMj.enregistrerSecret(
                        worldId,
                        initial.copy(texte = texte.trim(), pnjIds = pnjIds, campagneId = campagneId, scenarioId = scenarioId, lieu = lieu.trim()),
                    )
                    onDismiss()
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (!nouveau) {
                    TextButton(onClick = { CarnetMj.supprimerSecret(worldId, initial.id); onDismiss() }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
    if (choixPnj) {
        ChoixPnjDialog(pnjs = pnjs, selection = pnjIds, onDismiss = { choixPnj = false }, onValider = { pnjIds = it; choixPnj = false })
    }
}

/** Liste déroulante « Aucun(e) + options » (id → libellé). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoixListe(label: String, valeur: String?, options: List<Pair<String, String>>, onChoix: (String?) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = ouvert, onExpandedChange = { ouvert = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == valeur }?.second ?: "Aucun",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ouvert) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            DropdownMenuItem(text = { Text("Aucun") }, onClick = { onChoix(null); ouvert = false })
            options.forEach { (id, libelle) ->
                DropdownMenuItem(text = { Text(libelle) }, onClick = { onChoix(id); ouvert = false })
            }
        }
    }
}

@Composable
private fun CollerSecretsDialog(onDismiss: () -> Unit, onValider: (String) -> Unit) {
    var texte by remember { mutableStateOf("") }
    val nombre = texte.lines().count { it.trim().trimStart('-', '•', '*').isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coller une liste de secrets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Une ligne = un secret. Les tirets et puces en début de ligne sont retirés.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = nombre > 0, onClick = { onValider(texte) }) { Text(if (nombre > 0) "Ajouter $nombre secret(s)" else "Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ---------------------------------------------------------------------------------------------
// Fils en suspens
// ---------------------------------------------------------------------------------------------

/** Fils en suspens du monde ([filtreCampagneId] : seulement ceux de cette campagne). Ouverts d'abord, résolus repliés. */
@Composable
fun ColumnScope.FilsContent(worldId: String, carnet: CarnetMonde, filtreCampagneId: String?) {
    val pnjs = rememberPnjsMonde(worldId)
    val campagnes = rememberCampagnesMonde(worldId)
    var edition by remember { mutableStateOf<FilEnSuspens?>(null) }
    var creation by remember { mutableStateOf(false) }
    var voirResolus by remember { mutableStateOf(false) }
    val fils = carnet.fils.filter { filtreCampagneId == null || it.campagneId == filtreCampagneId }
    val ouverts = fils.filterNot { it.resolu }
    val resolus = fils.filter { it.resolu }
    val campagnesAffichees = campagnes.takeIf { filtreCampagneId == null }.orEmpty()

    if (fils.isEmpty()) {
        Text(
            "Promesses faites, PNJ vexés, pistes laissées de côté : tout ce qui doit revenir plus tard.",
            style = MaterialTheme.typography.bodyMedium,
            color = PrepHintColor,
        )
    }
    ouverts.forEach { fil -> FilRow(worldId, fil, pnjs, campagnesAffichees) { edition = fil } }
    if (resolus.isNotEmpty()) {
        TextButton(onClick = { voirResolus = !voirResolus }) {
            Text((if (voirResolus) "Masquer" else "Voir") + " les résolus (${resolus.size})", color = ForcedDarkPalette.AccentGold)
        }
        if (voirResolus) resolus.forEach { fil -> FilRow(worldId, fil, pnjs, campagnesAffichees) { edition = fil } }
    }
    OutlinedButton(onClick = { creation = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Nouveau fil", color = Color.White)
    }
    if (creation || edition != null) {
        FilEditorDialog(
            worldId = worldId,
            initial = edition ?: FilEnSuspens(texte = "", campagneId = filtreCampagneId),
            nouveau = edition == null,
            pnjs = pnjs,
            campagnes = campagnes,
            onDismiss = { creation = false; edition = null },
        )
    }
}

@Composable
private fun FilRow(worldId: String, fil: FilEnSuspens, pnjs: List<Character>, campagnes: List<GameState.MjCampaign>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = fil.resolu,
            onCheckedChange = { CarnetMj.basculerResolu(worldId, fil.id) },
            colors = CheckboxDefaults.colors(checkedColor = ForcedDarkPalette.AccentGold, uncheckedColor = Color.White),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                fil.texte,
                color = if (fil.resolu) PrepHintColor else Color.White,
                textDecoration = if (fil.resolu) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.bodyMedium,
            )
            val detail = listOfNotNull(
                nomsPnj(fil.pnjIds, pnjs).ifBlank { null }?.let { "👤 $it" },
                fil.campagneId?.let { id -> campagnes.firstOrNull { it.id == id } }?.let { "🗺 ${it.title}" },
                fil.ouvertSeance?.let { "depuis la séance $it" },
            ).joinToString(" · ")
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = PrepHintColor)
        }
    }
}

@Composable
private fun FilEditorDialog(
    worldId: String,
    initial: FilEnSuspens,
    nouveau: Boolean,
    pnjs: List<Character>,
    campagnes: List<GameState.MjCampaign>,
    onDismiss: () -> Unit,
) {
    var texte by remember { mutableStateOf(initial.texte) }
    var pnjIds by remember { mutableStateOf(initial.pnjIds) }
    var campagneId by remember { mutableStateOf(initial.campagneId) }
    var choixPnj by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (nouveau) "Nouveau fil en suspens" else "Modifier le fil") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texte,
                    onValueChange = { texte = it },
                    label = { Text("Ce qui doit revenir *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                PnjChips(pnjIds = pnjIds, pnjs = pnjs, titre = "PNJ concernés", onRetirer = { pnjIds = pnjIds - it }, onAjouter = { choixPnj = true })
                ChoixListe(
                    label = "Campagne (facultatif)",
                    valeur = campagneId,
                    options = campagnes.map { it.id to it.title },
                    onChoix = { campagneId = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = texte.isNotBlank(),
                onClick = {
                    CarnetMj.enregistrerFil(worldId, initial.copy(texte = texte.trim(), pnjIds = pnjIds, campagneId = campagneId))
                    onDismiss()
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (!nouveau) {
                    TextButton(onClick = { CarnetMj.supprimerFil(worldId, initial.id); onDismiss() }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
    if (choixPnj) {
        ChoixPnjDialog(pnjs = pnjs, selection = pnjIds, onDismiss = { choixPnj = false }, onValider = { pnjIds = it; choixPnj = false })
    }
}

// ---------------------------------------------------------------------------------------------
// Choix des PNJ
// ---------------------------------------------------------------------------------------------

/** PNJ choisis, en pastilles (appui = retirer, ou ouvrir sa fiche si [onOuvrir]), et bouton d'ajout. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PnjChips(
    pnjIds: List<String>,
    pnjs: List<Character>,
    titre: String,
    onRetirer: (String) -> Unit,
    onAjouter: () -> Unit,
    onOuvrir: ((String) -> Unit)? = null,
) {
    Text(titre, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        pnjIds.forEach { id ->
            val nom = pnjs.firstOrNull { it.id == id }?.name ?: return@forEach
            AssistChip(
                onClick = { onOuvrir?.invoke(id) ?: onRetirer(id) },
                label = { Text(nom) },
                trailingIcon = if (onOuvrir == null) {
                    { Text("✕") }
                } else null,
                colors = AssistChipDefaults.assistChipColors(labelColor = Color.White),
            )
        }
        AssistChip(
            onClick = onAjouter,
            label = { Text(if (pnjIds.isEmpty()) "Choisir" else "Modifier") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            colors = AssistChipDefaults.assistChipColors(labelColor = ForcedDarkPalette.AccentGold),
        )
    }
}

/** Sélection de PNJ (cases à cocher) avec recherche par nom. */
@Composable
internal fun ChoixPnjDialog(
    pnjs: List<Character>,
    selection: List<String>,
    onDismiss: () -> Unit,
    onValider: (List<String>) -> Unit,
) {
    var choix by remember { mutableStateOf(selection.toSet()) }
    var recherche by remember { mutableStateOf("") }
    val filtres = pnjs.filter { recherche.isBlank() || it.name.contains(recherche.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir les PNJ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pnjs.isEmpty()) {
                    Text("Aucun PNJ dans ce monde. Créez-en depuis la liste des personnages.", style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    filtres.forEach { pnj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { choix = if (pnj.id in choix) choix - pnj.id else choix + pnj.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = pnj.id in choix, onCheckedChange = { choix = if (it) choix + pnj.id else choix - pnj.id })
                            Column {
                                Text(pnj.name)
                                val detail = listOf(pnj.race, pnj.characterClass, pnj.location).filter { it.isNotBlank() }.joinToString(" · ")
                                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        // Ordre de la liste conservé pour les PNJ déjà choisis, nouveaux à la suite.
        confirmButton = { TextButton(onClick = { onValider(selection.filter { it in choix } + pnjs.map { it.id }.filter { it in choix && it !in selection }) }) { Text("Valider") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ---------------------------------------------------------------------------------------------
// Côté PNJ : secrets connus (pendant une discussion) et liens inverses (fiche)
// ---------------------------------------------------------------------------------------------

/**
 * Secrets que connaît ce PNJ, à cocher quand les joueurs les apprennent : affichés pendant une
 * discussion (PnjBriefingOverlay) et sur sa fiche. Rien n'est affiché s'il n'en connaît aucun.
 */
@Composable
fun SecretsConnusDuPnj(character: Character, modifier: Modifier = Modifier) {
    val carnets by CarnetMj.carnets.collectAsState()
    val secrets = carnets.values.flatMap { c -> c.secrets.filter { character.id in it.pnjIds } }
    if (secrets.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("🔑 Ce qu'il sait", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
        secrets.sortedBy { it.decouvert }.forEach { secret ->
            SecretRow(secret = secret, detail = "", aPlacer = null, onBasculerAPlacer = {}, onClick = null)
        }
    }
}

/**
 * Liens inverses d'un PNJ (voir [ApparitionsPnj]) : un appui ouvre le scénario, la campagne de la
 * quête, ou le carnet de préparation (secrets, fils, séance).
 */
@Composable
fun ApparitionsPnjContent(
    character: Character,
    onOpenScenario: ((String) -> Unit)?,
    onOpenCampaign: ((String) -> Unit)?,
    onOpenPreparation: (() -> Unit)?,
    onOpenFaction: ((String) -> Unit)? = null,
) {
    val campagnes by GameState.mjCampaigns.collectAsState()
    val scenarios by GameState.mjScenarios.collectAsState()
    val carnets by CarnetMj.carnets.collectAsState()
    val factions by com.jc2.jdrcompagnon.feature_faction.data.FactionsStore.factions.collectAsState()
    val apparitions = remember(character.id, character.name, character.worldId, campagnes, scenarios, carnets, factions) {
        ApparitionsPnj.trouver(character, campagnes, scenarios, carnets, factions)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text("🔗 Apparaît dans", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
        if (apparitions.isEmpty()) {
            Text(
                "Nulle part pour l'instant. Citez-le dans un scénario avec #pnj:[${character.name}], ou reliez-le à une quête, un secret ou un fil en suspens.",
                style = MaterialTheme.typography.bodySmall,
                color = PrepHintColor,
            )
        }
        val ouvrirCarnet = onOpenPreparation?.let { { it() } }
        apparitions.forEach { a ->
            when (a) {
                is ApparitionsPnj.Apparition.Scene -> ApparitionRow(
                    icone = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp)) },
                    titre = a.scenarioTitre,
                    detail = (a.sceneTitre ?: "Introduction") + if (a.parLien) "" else " · nom cité",
                    onClick = onOpenScenario?.let { { it(a.scenarioId) } },
                )
                is ApparitionsPnj.Apparition.Quete -> ApparitionRow(
                    icone = { Icon(Icons.Default.Flag, null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp)) },
                    titre = a.titre,
                    detail = a.role.replaceFirstChar { it.uppercase() },
                    onClick = onOpenCampaign?.let { { it(a.campagneId) } },
                )
                is ApparitionsPnj.Apparition.Secret -> ApparitionRow(
                    icone = { Icon(Icons.Default.Key, null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp)) },
                    titre = a.secret.texte,
                    detail = if (a.secret.decouvert) "Secret découvert" else "Secret qu'il connaît",
                    onClick = ouvrirCarnet,
                )
                is ApparitionsPnj.Apparition.Fil -> ApparitionRow(
                    icone = { Text("🧵") },
                    titre = a.fil.texte,
                    detail = "Fil en suspens",
                    onClick = ouvrirCarnet,
                )
                is ApparitionsPnj.Apparition.Seance -> ApparitionRow(
                    icone = { Icon(Icons.Default.EventNote, null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp)) },
                    titre = "Prévu pour la séance ${a.numero}",
                    detail = "Préparation de séance",
                    onClick = ouvrirCarnet,
                )
                is ApparitionsPnj.Apparition.Faction -> ApparitionRow(
                    icone = { Text("🏳️") },
                    titre = a.nom,
                    detail = if (a.chef) "Chef de la faction" else a.rang.ifBlank { "Membre de la faction" },
                    onClick = onOpenFaction?.let { { it(a.factionId) } },
                )
            }
        }
    }
}

@Composable
private fun ApparitionRow(icone: @Composable () -> Unit, titre: String, detail: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icone()
        Column(modifier = Modifier.weight(1f)) {
            Text(titre, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            Text(detail, color = PrepHintColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Scénarios de la séance
// ---------------------------------------------------------------------------------------------

private fun detailScenario(scenario: GameState.MjScenario, campagnes: List<GameState.MjCampaign>): String = listOfNotNull(
    campagnes.filter { scenario.id in it.scenarioIds }.joinToString(", ") { it.title }.ifBlank { null }?.let { "🗺 $it" },
    scenario.scenes.size.takeIf { it > 1 }?.let { n -> "${scenario.scenes.count { it.validated }}/$n scènes jouées" },
    scenario.lieuNom.ifBlank { null }?.let { "📍 $it" },
).joinToString(" · ")

/**
 * Choix des scénarios de la séance parmi ceux du monde : recherche (titre, chapitre, lieu,
 * description) et filtre par campagne, « hors campagne » ou « déjà choisis ».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoixScenariosDialog(
    worldId: String,
    selection: List<String>,
    onDismiss: () -> Unit,
    onValider: (List<String>) -> Unit,
) {
    val scenarios = rememberScenariosMonde(worldId)
    val campagnes = rememberCampagnesMonde(worldId)
    var choix by remember { mutableStateOf(selection.toSet()) }
    var recherche by remember { mutableStateOf("") }
    // null = tous ; "hors" = sans campagne ; "choisis" = sélection ; sinon id de campagne.
    var filtre by remember { mutableStateOf<String?>(null) }
    val q = recherche.trim().lowercase()
    val affiches = scenarios.filter { s ->
        val okFiltre = when (filtre) {
            null -> true
            "hors" -> campagnes.none { s.id in it.scenarioIds }
            "choisis" -> s.id in choix
            else -> campagnes.firstOrNull { it.id == filtre }?.let { s.id in it.scenarioIds } ?: true
        }
        val okRecherche = q.isEmpty() || listOf(s.title, s.chapitreTitre, s.lieuNom, s.description).any { it.lowercase().contains(q) }
        okFiltre && okRecherche
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scénarios de la séance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.FilterChip(selected = filtre == null, onClick = { filtre = null }, label = { Text("Tous") })
                    campagnes.forEach { c ->
                        androidx.compose.material3.FilterChip(selected = filtre == c.id, onClick = { filtre = c.id }, label = { Text(c.title) })
                    }
                    androidx.compose.material3.FilterChip(selected = filtre == "hors", onClick = { filtre = "hors" }, label = { Text("Hors campagne") })
                    androidx.compose.material3.FilterChip(selected = filtre == "choisis", onClick = { filtre = "choisis" }, label = { Text("Choisis (${choix.size})") })
                }
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (affiches.isEmpty()) Text("Aucun scénario.", style = MaterialTheme.typography.bodySmall)
                    affiches.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { choix = if (s.id in choix) choix - s.id else choix + s.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = s.id in choix, onCheckedChange = { choix = if (it) choix + s.id else choix - s.id })
                            Column {
                                Text(ChapitresScenarios.titreNumerote(s), fontWeight = FontWeight.Bold)
                                val detail = detailScenario(s, campagnes)
                                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        // Ordre de jeu (chapitres, numéros).
        confirmButton = { TextButton(onClick = { onValider(scenarios.map { it.id }.filter { it in choix }) }) { Text("Valider") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
