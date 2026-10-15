package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.EnvironmentDependencies
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.ui.EventResultDialog
import com.jc2.jdrcompagnon.feature_evenement.ui.TypeEvenementBadge
import com.jc2.jdrcompagnon.feature_evenement.ui.couleur
import com.jc2.jdrcompagnon.feature_evenement.ui.effetLabel
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.screens.joueur.effectuerReposLong
import kotlinx.coroutines.flow.first

/** Événement tiré pour un trajet, avec sa provenance (environnement, campagne, bibliothèque). */
private data class EvenementPropose(val source: String, val evenement: Evenement)

/**
 * Événement proposé au MJ pendant un trajet (à une halte ou à l'arrivée), affiché directement.
 * Tiré dans l'environnement de la campagne (ses événements liés, voir MjCampaign.environnementId)
 * quand il en a, sinon dans la bibliothèque d'événements : ceux de la campagne et les communs du
 * monde. Un petit bouton en bas en tire un autre.
 */
@Composable
fun EvenementTrajetDialog(campagneId: String, contexte: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val campagnes by GameState.mjCampaigns.collectAsState()
    val campagne = campagnes.firstOrNull { it.id == campagneId }
    val worldId = EvenementDependencies.mondeDeCampagne(campagneId)
    var environnements by remember { mutableStateOf<List<Environnement>>(emptyList()) }
    var bibliotheque by remember { mutableStateOf<List<Evenement>>(emptyList()) }
    var charge by remember { mutableStateOf(false) }
    LaunchedEffect(campagneId, worldId) {
        EvenementDependencies.seedSiNecessaire(context.applicationContext, worldId)
        environnements = runCatching { EnvironmentDependencies.repository.observerEnvironnements(worldId).first() }.getOrDefault(emptyList())
        bibliotheque = runCatching { EvenementDependencies.repository.observerEvenements(worldId).first() }.getOrDefault(emptyList())
        charge = true
    }
    val environnement = environnements.firstOrNull { it.id == campagne?.environnementId }
    var tirage by remember { mutableStateOf<EvenementPropose?>(null) }
    var afficherEffets by remember { mutableStateOf(false) }

    fun tirer() {
        val parId = bibliotheque.associateBy { it.id }
        val deLEnvironnement = environnement?.let { env ->
            env.evenementIds.mapNotNull { parId[it] }.map { EvenementPropose("Environnement · ${env.nom}", it) }
        }.orEmpty()
        val autres = bibliotheque
            .filter { it.campagneId == null || it.campagneId == campagneId }
            .map { EvenementPropose(if (it.campagneId != null) "Événement de campagne" else "Bibliothèque", it) }
        // L'environnement de la campagne l'emporte quand il a de quoi proposer : ses événements
        // ont lieu à chaque étape (ex. les apparitions du dragon d'une campagne).
        val pool = deLEnvironnement.ifEmpty { autres }
        // Un nouveau tirage évite de reproposer le même événement quand c'est possible.
        tirage = (pool.filter { it.evenement.id != tirage?.evenement?.id }.ifEmpty { pool }).randomOrNull()
    }
    LaunchedEffect(charge) { if (charge) tirer() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Événement proposé") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(contexte, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                val t = tirage
                if (t == null) {
                    Text(if (charge) "Aucun événement disponible." else "Chargement…")
                } else {
                    Text(t.source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeEvenementBadge(t.evenement.type, taille = 32.dp)
                        Column {
                            Text(t.evenement.titre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(t.evenement.type.label, style = MaterialTheme.typography.labelSmall, color = t.evenement.type.couleur)
                        }
                    }
                    if (t.evenement.description.isNotBlank()) com.jc2.jdrcompagnon.feature_evenement.ui.DescriptionEvenement(t.evenement.description)
                    t.evenement.effets.forEach { effet ->
                        Text(effetLabel(effet), style = MaterialTheme.typography.bodySmall)
                    }
                    if (t.evenement.objectif.isNotBlank()) {
                        Text("Attendu : ${t.evenement.objectif}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (t.evenement.effets.isNotEmpty() || t.evenement.issues.isNotEmpty() || t.evenement.profils.isNotEmpty()) {
                        TextButton(onClick = { afficherEffets = true }) {
                            Text(if (t.evenement.issues.isNotEmpty() || t.evenement.profils.isNotEmpty()) "Résoudre (profils, issues, effets)…" else "Appliquer les effets…")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        dismissButton = {
            TextButton(onClick = { tirer() }, enabled = charge) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Autre événement", style = MaterialTheme.typography.labelSmall)
            }
        },
    )

    if (afficherEffets) {
        tirage?.let { t -> EventResultDialog(evenement = t.evenement, worldId = worldId, onDismiss = { afficherEffets = false }) }
    }
}

/**
 * Halte pour un repos long (8 h) : l'horloge avance une fois pour tout le groupe, les joueurs
 * connectés reçoivent la proposition (chacun choisit de se reposer), et les membres que personne
 * n'incarne en réseau (PNJ, PJ gérés par le MJ) sont reposés directement.
 */
@Composable
fun ReposGroupeDialog(lieu: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groups by GameState.mjGroups.collectAsState()
    val groupId by GameState.currentGroupId.collectAsState()
    val characters by GameState.characters.collectAsState()
    val claimed by NetworkSessionManager.claimedCharacters.collectAsState()
    val sent by NetworkSessionManager.sentCharacterByClient.collectAsState()
    val group = groups.firstOrNull { it.id == groupId }
    val incarnes = claimed.keys + sent.values
    val membres = characters.filter { group != null && it.id in group.memberIds && (it.type == "PJ" || it.type == "PNJ") }
    val directs = membres.filter { it.id !in incarnes }
    var fait by remember { mutableStateOf(false) }
    // Membres dont le repos long est refusé (moins de 24 h depuis le précédent, 0 PV).
    var refuses by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repos long du groupe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fait) {
                    Text("Repos long lancé : horloge avancée de 8 h.")
                    val reposes = directs.filter { it.name !in refuses }
                    if (reposes.isNotEmpty()) Text("Reposés : ${reposes.joinToString { it.name }}.")
                    if (refuses.isNotEmpty()) Text("Sans effet (un seul repos long par 24 h, 1 PV minimum) : ${refuses.joinToString()}.", color = MaterialTheme.colorScheme.error)
                    if (incarnes.isNotEmpty()) Text("Les joueurs connectés ont reçu la proposition.")
                } else {
                    Text("Halte : $lieu. Le groupe se repose 8 heures.")
                    if (group == null) Text("Aucun groupe sélectionné : seule l'horloge avancera.", color = MaterialTheme.colorScheme.error)
                    if (directs.isNotEmpty()) Text("Reposés directement : ${directs.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                    val connectes = membres.filter { it.id in incarnes }
                    if (connectes.isNotEmpty()) Text("Proposé aux joueurs de : ${connectes.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (fait) {
                TextButton(onClick = onDismiss) { Text("Fermer") }
            } else {
                TextButton(onClick = {
                    ScenarioClockState.advanceManually(8L * 60)
                    refuses = directs.filterNot { effectuerReposLong(context, scope, it, avancerHorloge = false) }.map { it.name }
                    NetworkSessionManager.proposerReposLong("Le groupe fait halte ($lieu) pour la nuit.")
                    fait = true
                }) { Text("Lancer le repos") }
            }
        },
        dismissButton = { if (!fait) TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
