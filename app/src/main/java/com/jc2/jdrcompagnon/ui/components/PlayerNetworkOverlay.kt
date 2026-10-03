package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.network.CharacterFieldDiff
import com.jc2.jdrcompagnon.network.EpreuveJoueurData
import com.jc2.jdrcompagnon.network.diffCharacters
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.NetworkSessionManager.ProposalDecision
import com.jc2.jdrcompagnon.network.SessionRole
import com.jc2.jdrcompagnon.ui.screens.joueur.LootOfferCard

/**
 * Point unique d'affichage des messages réseau qui attendent une réaction du joueur (briefing
 * PNJ, offre de butin, proposition de groupe, dernier appel avant perte de récompense, conflit de
 * version de personnage) : monté une seule fois au niveau du NavGraph (comme DiceOverlay), donc
 * visible quel que soit l'écran affiché — plutôt que chaque écran affichant son propre dialog
 * local, invisible dès qu'on navigue ailleurs.
 *
 * Un seul dialog à la fois, par ordre de priorité (le conflit de personnage bloque l'accès à la
 * fiche tant qu'il n'est pas résolu, donc passe en premier).
 */
@Composable
fun PlayerNetworkOverlay() {
    val networkRole by NetworkSessionManager.role.collectAsState()
    if (networkRole != SessionRole.PLAYER) return

    val conflict by NetworkSessionManager.pendingCharacterVersionConflict.collectAsState()
    val finalCall by NetworkSessionManager.pendingProposalFinalCall.collectAsState()
    val pnjBriefing by NetworkSessionManager.pendingPnjBriefing.collectAsState()
    val lootOffer by NetworkSessionManager.pendingLootOffer.collectAsState()
    val groupProposal by NetworkSessionManager.pendingGroupProposal.collectAsState()
    val epreuve by NetworkSessionManager.epreuveEnCours.collectAsState()
    val epreuveMasquee by NetworkSessionManager.epreuveMasquee.collectAsState()
    val restOffer by NetworkSessionManager.pendingRestOffer.collectAsState()
    val voyageAnnonce by NetworkSessionManager.voyageAnnonce.collectAsState()

    when {
        conflict != null -> CharacterConflictDialog(
            conflict = conflict!!,
            onKeepHostVersion = { NetworkSessionManager.resolveCharacterConflictKeepHost() },
            onProposeMine = { NetworkSessionManager.resolveCharacterConflictProposeMine() },
        )
        finalCall != null -> ProposalFinalCallDialog(
            title = finalCall!!.title,
            onDismiss = { NetworkSessionManager.dismissProposalFinalCall() },
        )
        pnjBriefing != null -> PnjDiscussionPlayerDialog(
            name = pnjBriefing!!.name,
            portraitId = pnjBriefing!!.portraitId,
            onLeave = { characterName -> NetworkSessionManager.leavePnjDiscussion(characterName) },
        )
        lootOffer != null -> Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = true)) {
            LootOfferCard(
                itemName = lootOffer!!.itemName,
                onWantIt = { NetworkSessionManager.respondToLootOffer(lootOffer!!.offerId, wants = true) },
                onPass = { NetworkSessionManager.respondToLootOffer(lootOffer!!.offerId, wants = false) },
            )
        }
        groupProposal != null -> GroupProposalDialog(
            title = groupProposal!!.title,
            description = groupProposal!!.description,
            rewardLabel = groupProposal!!.rewardLabel,
            onDecision = { decision -> NetworkSessionManager.respondToGroupProposal(groupProposal!!.proposalId, decision) },
        )
        epreuve != null && epreuve!!.id != epreuveMasquee -> EpreuvePlayerDialog(
            epreuve = epreuve!!,
            onDismiss = { NetworkSessionManager.masquerEpreuve() },
        )
        restOffer != null -> RestOfferDialog(message = restOffer!!, onDismiss = { NetworkSessionManager.dismissRestOffer() })
        voyageAnnonce != null -> VoyageAnnonceDialog(
            voyage = voyageAnnonce!!,
            onPret = {
                NetworkSessionManager.voyagePret(null)
                NetworkSessionManager.dismissVoyageAnnonce()
            },
            onDismiss = { NetworkSessionManager.dismissVoyageAnnonce() },
        )
    }
}

/**
 * Route de voyage choisie par un joueur ou le MJ (« Route sélectionnée ») : le joueur peut se
 * déclarer prêt au départ tout de suite, ou plus tard depuis la carte. Une fois tout le monde
 * prêt, annonce du départ.
 */
@Composable
private fun VoyageAnnonceDialog(
    voyage: com.jc2.jdrcompagnon.network.VoyageData,
    onPret: () -> Unit,
    onDismiss: () -> Unit,
) {
    fun f1(v: Double) = String.format("%.1f", v)
    val jours = if (voyage.heures <= 0.0) 0 else kotlin.math.ceil(voyage.heures / voyage.heuresMax).toInt()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (voyage.enRoute) "En route !" else "Route sélectionnée") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                if (voyage.enRoute) {
                    Text("Tout le monde est prêt : le groupe part vers ${voyage.arriveeNom}.")
                } else {
                    Text("Route proposée par ${voyage.proposePar}, vers ${voyage.arriveeNom}.")
                }
                Text(
                    "${voyage.distanceKm.toInt()} km · ${f1(voyage.heures)} h de marche · $jours jour(s) · ${voyage.arrets.size} halte(s)",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!voyage.enRoute) {
                    Text(
                        "Prêts : ${(listOfNotNull("MJ".takeIf { voyage.mjPret }) + voyage.prets).joinToString().ifBlank { "personne" }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            if (voyage.enRoute) TextButton(onClick = onDismiss) { Text("OK") }
            else Button(onClick = onPret) { Text("Prêt au départ") }
        },
        dismissButton = { if (!voyage.enRoute) TextButton(onClick = onDismiss) { Text("Plus tard") } },
    )
}

/**
 * Halte décidée par le MJ sur un trajet : le joueur prend (ou non) un repos long pour son
 * personnage. L'horloge a déjà été avancée de 8 h par le MJ pour tout le groupe.
 */
@Composable
private fun RestOfferDialog(message: String, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val characters by GameState.characters.collectAsState()
    val claimedId by NetworkSessionManager.claimedCharacterId.collectAsState()
    val selectedId by GameState.selectedCharacterId.collectAsState()
    val character = characters.firstOrNull { it.id == (claimedId ?: selectedId) }
    var resultat by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Halte du groupe", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Repos long (8 h)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
                resultat?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (resultat == null) {
                    Button(
                        onClick = {
                            character?.let { c ->
                                com.jc2.jdrcompagnon.ui.screens.joueur.effectuerReposLong(context, scope, c, avancerHorloge = false) { resultat = it }
                            }
                        },
                        enabled = character != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(character?.let { "Repos long pour ${it.name}" } ?: "Aucun personnage") }
                    // Repos court, harmonisation, sorts préparés, repas... : l'écran de repos complet.
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            com.jc2.jdrcompagnon.ui.screens.joueur.ReposNavigation.ouvrir()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Ouvrir l'écran de repos") }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Ne pas se reposer") }
                } else {
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Fermer") }
                }
            }
        }
    }
}

/**
 * Pendant côté MJ de [PlayerNetworkOverlay] : une proposition de fiche envoyée par un joueur
 * (personnage perso, ou sa version locale après un conflit) s'affiche quel que soit l'écran du
 * MJ — avant, elle n'apparaissait que dans l'écran serveur et passait inaperçue. "Plus tard" la
 * masque ici ; elle reste traitable depuis l'écran serveur.
 */
@Composable
fun HostNetworkOverlay() {
    val networkRole by NetworkSessionManager.role.collectAsState()
    if (networkRole != SessionRole.HOST) return

    val proposals by NetworkSessionManager.pendingProposals.collectAsState()
    val clients by NetworkSessionManager.connectedClients.collectAsState()
    val characters by GameState.characters.collectAsState()
    var postponed by remember { mutableStateOf(emptyMap<String, Character>()) }

    val (clientId, proposed) = proposals.entries.firstOrNull { postponed[it.key] != it.value }?.toPair() ?: return
    val clientName = clients.find { it.id == clientId }?.displayName ?: "Un joueur"
    val existing = characters.find { it.id == proposed.id }
    val diffs = remember(existing, proposed) { existing?.let { diffCharacters(proposed, it) } }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Proposition de fiche", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (existing != null) {
                        "$clientName propose sa version de \"${proposed.name}\", différente de la vôtre."
                    } else {
                        "$clientName propose son personnage \"${proposed.name}\" (${proposed.race} ${proposed.characterClass}, niv. ${proposed.level})."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!diffs.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        LazyColumn(modifier = Modifier.padding(12.dp).heightIn(max = 260.dp)) {
                            items(diffs) { diff ->
                                CharacterDiffRow(diff, localLabel = "Version du joueur", remoteLabel = "Votre version")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { NetworkSessionManager.acceptProposal(clientId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Accepter")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { NetworkSessionManager.rejectProposal(clientId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (existing != null) "Refuser (garder ma version)" else "Refuser")
                }
                TextButton(onClick = { postponed = postponed + (clientId to proposed) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Plus tard")
                }
            }
        }
    }
}

@Composable
private fun EpreuvePlayerDialog(epreuve: EpreuveJoueurData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Épreuve · ${epreuve.type}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(epreuve.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (epreuve.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(epreuve.description, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Progrès du groupe : ${epreuve.progres} / ${epreuve.progresMax}", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { epreuve.progres.toFloat() / epreuve.progresMax.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (epreuve.competences.isNotEmpty() && epreuve.issue == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Compétences utiles : ${epreuve.competences.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                }
                epreuve.issue?.let { issue ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(issue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(if (epreuve.issue != null) "Fermer" else "Masquer") }
            }
        }
    }
}

@Composable
private fun ProposalFinalCallDialog(title: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Dernier appel !", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Le MJ a changé de scène et votre décision manque encore pour : \"$title\". Faites votre choix maintenant, sinon la récompense sera perdue.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Compris") }
            }
        }
    }
}

@Composable
private fun GroupProposalDialog(
    title: String,
    description: String,
    rewardLabel: String,
    onDecision: (ProposalDecision) -> Unit,
) {
    Dialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Proposition du MJ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
                if (rewardLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Récompense : $rewardLabel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecision(ProposalDecision.ACCEPTED) }, modifier = Modifier.weight(1f)) { Text("Accepter") }
                    OutlinedButton(onClick = { onDecision(ProposalDecision.DECLINED) }, modifier = Modifier.weight(1f)) { Text("Décliner") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onDecision(ProposalDecision.PASSED) }, modifier = Modifier.fillMaxWidth()) { Text("Passer") }
            }
        }
    }
}

/**
 * Une ligne de différence : pour une liste (équipement, sac...), n'affiche que les éléments
 * propres à chaque version plutôt que la liste entière.
 */
@Composable
private fun CharacterDiffRow(diff: CharacterFieldDiff, localLabel: String, remoteLabel: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(diff.fieldLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (diff.isListDiff) {
            Text("Seulement dans ${localLabel.lowercase()} : ${diff.localValue}", style = MaterialTheme.typography.bodySmall)
            Text("Seulement dans ${remoteLabel.lowercase()} : ${diff.remoteValue}", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("$localLabel : ${diff.localValue}", style = MaterialTheme.typography.bodySmall)
            Text("$remoteLabel : ${diff.remoteValue}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CharacterConflictDialog(
    conflict: NetworkSessionManager.CharacterVersionConflict,
    onKeepHostVersion: () -> Unit,
    onProposeMine: () -> Unit,
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Version différente détectée", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Votre fiche de \"${conflict.local.name}\" a été modifiée hors connexion et diffère de celle du MJ. " +
                        "La version du MJ fait référence : choisissez ci-dessous.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (conflict.diffs.isEmpty()) {
                    Text("Aucune différence détaillée disponible.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        LazyColumn(modifier = Modifier.padding(12.dp).height(220.dp)) {
                            items(conflict.diffs) { diff ->
                                CharacterDiffRow(diff, localLabel = "Votre version", remoteLabel = "Version du MJ")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onKeepHostVersion, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer ma version et adopter celle du MJ")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onProposeMine, modifier = Modifier.fillMaxWidth()) {
                    Text("Proposer ma version au MJ")
                }
            }
        }
    }
}
