package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_carte.domain.model.FonctionLieu
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus
import com.jc2.jdrcompagnon.ui.GameState
import kotlin.math.roundToInt

/** Prix saisi en po (« 0,5 », « 2 ») converti en pièces de cuivre ; vide ou illisible = 0. */
private fun poVersPc(saisie: String): Int =
    ((saisie.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).roundToInt().coerceAtLeast(0)

private fun pcVersPo(pc: Int): String =
    if (pc % 100 == 0) (pc / 100).toString() else (pc / 100.0).toString().replace('.', ',')

/**
 * Création ou modification d'un lieu à visiter d'une ville (outil VILLES) : nom, description,
 * visibilité pour les joueurs et fonction du lieu avec ses réglages. Pour un panneau de quêtes,
 * les contrats cochés sont rendus visibles des joueurs à l'enregistrement (validation du MJ).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditeurLieuVilleDialog(
    initial: LieuNotable?,
    villeId: String,
    campagneId: String,
    onEnregistrer: (LieuNotable) -> Unit,
    onDismiss: () -> Unit,
) {
    var nom by remember(initial) { mutableStateOf(initial?.nom.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var visible by remember(initial) { mutableStateOf(initial?.visibleJoueurs ?: true) }
    var fonction by remember(initial) { mutableStateOf(initial?.fonction ?: FonctionLieu.AUCUNE) }
    var chance by remember(initial) { mutableIntStateOf(initial?.chancePourcent ?: 50) }
    var prix by remember(initial) { mutableStateOf(initial?.prixPc?.takeIf { it > 0 }?.let(::pcVersPo).orEmpty()) }
    var texte by remember(initial) { mutableStateOf(initial?.texte.orEmpty()) }
    var queteIds by remember(initial) { mutableStateOf(initial?.queteIds.orEmpty().toSet()) }
    var dd by remember(initial) { mutableIntStateOf(initial?.dd ?: 12) }
    var valeur by remember(initial) { mutableStateOf(initial?.valeur?.takeIf { it > 0 }?.toString().orEmpty()) }

    val campagnes by GameState.mjCampaigns.collectAsState()
    val campagne = campagnes.firstOrNull { it.id == campagneId }
    // Contrats possibles : quêtes de la campagne pas encore terminées (ou déjà sur ce panneau).
    val quetes = campagne?.quests.orEmpty().filter {
        it.status == QuestStatus.EN_COURS || it.status == QuestStatus.EN_ATTENTE || it.id in queteIds
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau lieu à visiter" else "Modifier le lieu") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom (ex: Auberge du Dragon)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visible des joueurs", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (visible) "Les joueurs peuvent s'y rendre." else "Caché : réservé au MJ.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = visible, onCheckedChange = { visible = it })
                }

                Text("Fonction du lieu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FonctionLieu.entries.forEach { f ->
                        FilterChip(
                            selected = f == fonction,
                            onClick = { fonction = f },
                            label = { Text("${f.emoji} ${f.label}") },
                        )
                    }
                }
                Text(fonction.aide, style = MaterialTheme.typography.bodySmall)

                if (fonction == FonctionLieu.TEMPLE || fonction == FonctionLieu.TRIPOT) {
                    Text(
                        (if (fonction == FonctionLieu.TEMPLE) "Chance de bénédiction" else "Chance de gagner") + " : $chance %",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Slider(value = chance.toFloat(), onValueChange = { chance = it.roundToInt() }, valueRange = 0f..100f, steps = 19)
                }
                val libellePrix = when (fonction) {
                    FonctionLieu.TEMPLE -> "Offrande (po, vide = gratuit)"
                    FonctionLieu.AUBERGE -> "Prix de la nuit (po)"
                    FonctionLieu.GUERISSEUR -> "Prix des soins (po)"
                    FonctionLieu.TAVERNE -> "Prix de la tournée (po, vide = gratuit)"
                    FonctionLieu.TRIPOT -> "Mise (po)"
                    FonctionLieu.ECURIE -> "Prix de la location (po)"
                    FonctionLieu.BIBLIOTHEQUE -> "Droit d'entrée (po, vide = gratuit)"
                    FonctionLieu.ARENE -> "Inscription (po, vide = gratuite)"
                    FonctionLieu.ORACLE -> "Prix de la consultation (po)"
                    FonctionLieu.THERMES -> "Prix des bains (po)"
                    FonctionLieu.ENTRAINEMENT -> "Prix de la leçon (po)"
                    else -> null
                }
                if (libellePrix != null) {
                    OutlinedTextField(
                        value = prix,
                        onValueChange = { prix = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text(libellePrix) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (fonction == FonctionLieu.BIBLIOTHEQUE || fonction == FonctionLieu.ARENE) {
                    Text(
                        (if (fonction == FonctionLieu.BIBLIOTHEQUE) "DD du jet d'Investigation" else "DD du jet d'Athlétisme") + " : $dd",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Slider(value = dd.toFloat(), onValueChange = { dd = it.roundToInt() }, valueRange = 5f..30f, steps = 24)
                }
                val libelleValeur = when (fonction) {
                    FonctionLieu.ARENE -> "Prix du vainqueur (po)"
                    FonctionLieu.ENTRAINEMENT -> "PX gagnés par leçon"
                    FonctionLieu.BANQUE -> "Frais sur chaque dépôt (%)"
                    else -> null
                }
                if (libelleValeur != null) {
                    OutlinedTextField(
                        value = valeur,
                        onValueChange = { valeur = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(libelleValeur) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                when (fonction) {
                    FonctionLieu.TEMPLE -> OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Bénédiction accordée (texte lu par le joueur)") },
                        placeholder = { Text("La déesse de la chance vous sourit…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    FonctionLieu.TAVERNE -> OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Rumeurs (une par ligne)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    FonctionLieu.ECURIE -> OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Montures à louer (une par ligne)") },
                        placeholder = { Text("Vide : cheval de selle, poney, mule, cheval de trait") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    FonctionLieu.BIBLIOTHEQUE -> OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Savoirs à découvrir (un par ligne)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    FonctionLieu.ORACLE -> OutlinedTextField(
                        value = texte,
                        onValueChange = { texte = it },
                        label = { Text("Prophéties (une par ligne)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    FonctionLieu.PANNEAU_QUETES -> {
                        Text("Contrats affichés", style = MaterialTheme.typography.labelMedium)
                        if (campagne == null) {
                            Text("Aucune campagne : impossible de choisir des quêtes.", style = MaterialTheme.typography.bodySmall)
                        } else if (quetes.isEmpty()) {
                            Text("Aucune quête en cours dans la campagne « ${campagne.title} ».", style = MaterialTheme.typography.bodySmall)
                        }
                        quetes.forEach { q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { queteIds = if (q.id in queteIds) queteIds - q.id else queteIds + q.id },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = q.id in queteIds, onCheckedChange = null, modifier = Modifier.padding(end = 8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(q.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        q.status.label + if (q.visibleToPlayers) " · déjà visible des joueurs" else " · sera rendue visible",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = initial ?: LieuNotable(id = java.util.UUID.randomUUID().toString(), villeId = villeId, nom = nom)
                    val lieu = base.copy(
                        nom = nom.trim(),
                        description = description,
                        visibleJoueurs = visible,
                        fonction = fonction,
                        chancePourcent = chance,
                        dd = dd,
                        valeur = valeur.toIntOrNull()?.let { if (fonction == FonctionLieu.BANQUE) it.coerceAtMost(100) else it } ?: 0,
                        prixPc = poVersPc(prix),
                        texte = texte,
                        queteIds = if (fonction == FonctionLieu.PANNEAU_QUETES) queteIds.toList() else emptyList(),
                    )
                    // Validation du MJ : les contrats affichés deviennent visibles des joueurs.
                    if (campagne != null && lieu.fonction == FonctionLieu.PANNEAU_QUETES) {
                        campagne.quests.filter { it.id in lieu.queteIds && !it.visibleToPlayers }
                            .forEach { GameState.upsertQuest(campagne.id, it.copy(visibleToPlayers = true)) }
                    }
                    onEnregistrer(lieu)
                    onDismiss()
                },
                enabled = nom.isNotBlank(),
            ) { Text(if (initial == null) "Ajouter" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
