package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneCompositionCombat
import com.jc2.jdrcompagnon.feature_combat.ui.LancerCombatDialog
import com.jc2.jdrcompagnon.ui.navigation.DemandesLienInterne
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.InternalLinkDetailDialog
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.SendItemToCharacterDialog
import com.jc2.jdrcompagnon.ui.components.SendItemToGroupDialog

/**
 * Résolution d'un événement : description, ce qui est attendu des joueurs, effets immédiats,
 * puis choix de l'issue obtenue (réussite, échec...) qui révèle ce qui se passe et ses propres
 * effets. Chaque effet a un bouton d'action quand une action automatisée a un sens — objet
 * gagné (envoi comme un loot), XP/or (application au personnage), réputation (application au
 * groupe). Une perte d'objet ou un effet "Autre" reste purement informatif.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventResultDialog(
    evenement: Evenement?,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    var envoyerObjetPersonnage by remember { mutableStateOf<String?>(null) }
    var envoyerObjetGroupe by remember { mutableStateOf<String?>(null) }
    var appliquerXpOr by remember { mutableStateOf<EffetEvenement?>(null) }
    var appliquerReputation by remember { mutableStateOf<EffetEvenement?>(null) }
    var issueChoisie by remember(evenement?.id) { mutableStateOf<IssueEvenement?>(null) }
    var monstreEnDetail by remember { mutableStateOf<String?>(null) }
    // Composition (format CompositionCombat) et difficulté visée, ajustée au niveau du groupe.
    var combatALancer by remember { mutableStateOf<Pair<String, EncounterDifficulty>?>(null) }

    @Composable
    fun BoutonCombat(evenement: Evenement, issue: IssueEvenement? = null) {
        val difficulte = difficulteDuCombat(issue)
        OutlinedButton(
            onClick = {
                combatALancer = CompositionCombat.formater(evenement.profils.map { LigneCompositionCombat(it.nom, it.quantite) }) to difficulte
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SportsMartialArts, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "Lancer le combat · ${difficulte.label}" + if (issue?.nature == NatureIssue.ECHEC) " (test raté)" else "",
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }

    @Composable
    fun LigneEffet(effet: EffetEvenement) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(effetLabel(effet), modifier = Modifier.weight(1f))
            when {
                effet.categorie == CategorieEffet.OBJET && effet.gain -> {
                    IconButton(onClick = { envoyerObjetPersonnage = "${effet.quantite}x ${effet.cible}" }) {
                        Icon(Icons.Default.Person, contentDescription = "Envoyer à un personnage")
                    }
                    IconButton(onClick = { envoyerObjetGroupe = "${effet.quantite}x ${effet.cible}" }) {
                        Icon(Icons.Default.Group, contentDescription = "Proposer à un groupe")
                    }
                }
                effet.categorie == CategorieEffet.EXPERIENCE || effet.categorie == CategorieEffet.OR -> {
                    IconButton(onClick = { appliquerXpOr = effet }) {
                        Icon(Icons.Default.Person, contentDescription = "Appliquer à un personnage")
                    }
                }
                effet.categorie == CategorieEffet.REPUTATION -> {
                    IconButton(onClick = { appliquerReputation = effet }) {
                        Icon(Icons.Default.Group, contentDescription = "Appliquer à un groupe")
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = evenement?.let { { TypeEvenementBadge(it.type) } },
        title = {
            // Centré même sur plusieurs lignes, comme un titre court sous l'icône.
            Text(evenement?.titre ?: "Événement", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (evenement == null) {
                    Text("Aucun événement disponible.")
                } else {
                    Text(evenement.type.label, style = MaterialTheme.typography.labelMedium, color = evenement.type.couleur)
                    if (evenement.description.isNotBlank()) {
                        Text(evenement.description)
                    }
                    if (evenement.profils.isNotEmpty()) {
                        // Profils impliqués : monstre = fiche du bestiaire en fenêtre, PNJ = sa
                        // fiche complète (plein écran) ; ensemble, ils composent le combat.
                        PastillesCompactes {
                            evenement.profils.forEach { profil ->
                                AssistChip(
                                    onClick = {
                                        if (profil.type == TypeProfil.MONSTRE) {
                                            monstreEnDetail = profil.nom
                                        } else {
                                            DemandesLienInterne.ouvrir("pnj", profil.nom)
                                            onDismiss()
                                        }
                                    },
                                    label = { Text(profil.libelle) },
                                    leadingIcon = { Icon(profil.type.icone, contentDescription = profil.type.label, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                    if (evenement.objectif.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = evenement.type.couleur.copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Attendu", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = evenement.type.couleur)
                                ObjectifEnListe(evenement.objectif)
                            }
                        }
                    }
                    if (evenement.effets.isNotEmpty()) {
                        if (evenement.issues.isNotEmpty()) {
                            Text("Effets immédiats", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        evenement.effets.forEach { LigneEffet(it) }
                    }
                    if (evenement.issues.isNotEmpty()) {
                        Text("Issue", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        PastillesCompactes {
                            evenement.issues.forEach { issue ->
                                FilterChip(
                                    selected = issueChoisie?.id == issue.id,
                                    onClick = { issueChoisie = if (issueChoisie?.id == issue.id) null else issue },
                                    label = { Text(issue.libelle) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = issue.nature.couleur,
                                        selectedContainerColor = issue.nature.couleur.copy(alpha = 0.3f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        val choisie = issueChoisie
                        if (choisie == null) {
                            Text(
                                "Choisissez l'issue obtenue par les joueurs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = choisie.nature.couleur.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, choisie.nature.couleur.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(choisie.nature.label, style = MaterialTheme.typography.labelMedium, color = choisie.nature.couleur)
                                    if (choisie.description.isNotBlank()) Text(choisie.description)
                                    choisie.effets.forEach { LigneEffet(it) }
                                    // L'issue qui mène au combat le lance avec les profils de l'événement.
                                    if (choisie.declencheCombat && evenement.profils.isNotEmpty()) {
                                        BoutonCombat(evenement, choisie)
                                    }
                                }
                            }
                        }
                    }
                    // Aucune issue ne mène explicitement au combat : le bouton reste disponible,
                    // en bas, tant que l'événement a des profils.
                    if (evenement.profils.isNotEmpty() && evenement.issues.none { it.declencheCombat }) {
                        BoutonCombat(evenement)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )

    envoyerObjetPersonnage?.let { itemName ->
        SendItemToCharacterDialog(itemName = itemName, worldId = worldId, onDismiss = { envoyerObjetPersonnage = null })
    }
    envoyerObjetGroupe?.let { itemName ->
        SendItemToGroupDialog(itemName = itemName, worldId = worldId, onDismiss = { envoyerObjetGroupe = null })
    }
    appliquerXpOr?.let { effet ->
        ApplyXpOrDialog(effet = effet, worldId = worldId, onDismiss = { appliquerXpOr = null })
    }
    appliquerReputation?.let { effet ->
        ApplyReputationDialog(effet = effet, worldId = worldId, onDismiss = { appliquerReputation = null })
    }
    monstreEnDetail?.let { nom ->
        InternalLinkDetailDialog(type = "monster", name = nom, worldId = worldId, onDismiss = { monstreEnDetail = null })
    }
    combatALancer?.let { (composition, difficulte) ->
        LancerCombatDialog(
            composition = composition,
            worldId = worldId,
            onDismiss = { combatALancer = null },
            onLance = {
                combatALancer = null
                DemandesLienInterne.ouvrir("combat", composition)
                onDismiss()
            },
            difficulteInitiale = difficulte
        )
    }
}


/** Applique un effet XP/or à un personnage choisi (délta signé selon [EffetEvenement.gain]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyXpOrDialog(
    effet: EffetEvenement,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    val characters by GameState.characters.collectAsState()
    val worldCharacters = remember(characters, worldId) {
        characters.filter { it.worldId == worldId && it.type == "PJ" }
    }
    val delta = if (effet.gain) effet.quantite else -effet.quantite
    val label = if (effet.categorie == CategorieEffet.EXPERIENCE) "XP" else "po"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appliquer ${effetSigneCourt(delta)} $label à…", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (worldCharacters.isEmpty()) {
                    Text("Aucun personnage joueur dans ce monde.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                worldCharacters.forEach { character ->
                    ListItem(
                        headlineContent = { Text(character.name) },
                        modifier = Modifier.clickable {
                            if (effet.categorie == CategorieEffet.EXPERIENCE) {
                                GameState.addExperience(character.id, delta)
                            } else {
                                GameState.addGold(character.id, delta)
                            }
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/** Applique un effet de réputation à un groupe choisi (faction = [EffetEvenement.cible]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyReputationDialog(
    effet: EffetEvenement,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    val mjGroups by GameState.mjGroups.collectAsState()
    val worldGroups = remember(mjGroups, worldId) {
        mjGroups.filter { it.worldId == worldId }
    }
    val delta = if (effet.gain) effet.quantite else -effet.quantite

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appliquer ${effetSigneCourt(delta)} réputation (${effet.cible}) à…", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (worldGroups.isEmpty()) {
                    Text("Aucun groupe dans ce monde.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                worldGroups.forEach { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        modifier = Modifier.clickable {
                            val existante = group.reputations.find { it.factionName.equals(effet.cible, ignoreCase = true) }
                            val nouveauScore = ((existante?.score ?: 0) + delta).coerceIn(-100, 100)
                            val reputations = if (existante != null) {
                                group.reputations.map { if (it.factionId == existante.factionId) it.copy(score = nouveauScore) else it }
                            } else {
                                group.reputations + com.jc2.jdrcompagnon.feature_group.domain.model.Reputation(
                                    factionName = effet.cible,
                                    score = nouveauScore
                                )
                            }
                            GameState.updateMjGroup(group.copy(reputations = reputations))
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun effetSigneCourt(delta: Int): String = if (delta >= 0) "+$delta" else "$delta"

/**
 * Pastilles (profils, issues) en rangées serrées : sans la zone tactile minimale de 48 dp, qui
 * espaçait fortement les lignes quand les pastilles passaient à la ligne.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PastillesCompactes(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { content() }
    }
}

/**
 * "Ce qui est attendu" : une option par ligne ("Payer : 20 po", "Négocier : Charisme
 * (Persuasion) DD 13"...) affichée en liste à puces, l'action en gras ; un texte d'une seule
 * ligne reste un simple paragraphe.
 */
@Composable
private fun ObjectifEnListe(objectif: String) {
    val lignes = objectif.lines().map { it.trim().removePrefix("-").removePrefix("•").trim() }.filter { it.isNotBlank() }
    if (lignes.size <= 1) {
        Text(objectif.trim(), style = MaterialTheme.typography.bodyMedium)
        return
    }
    lignes.forEach { ligne ->
        Row(verticalAlignment = Alignment.Top) {
            Text("•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 6.dp))
            val separateur = ligne.indexOf(" : ")
            Text(
                buildAnnotatedString {
                    if (separateur > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(ligne.substring(0, separateur)) }
                        append(ligne.substring(separateur))
                    } else {
                        append(ligne)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Difficulté visée par le combat d'un événement, ajustée au niveau du groupe par
 * LancerCombatDialog : Moyenne par défaut ; un cran au-dessus quand le combat vient d'un test
 * raté (issue d'échec), les adversaires ayant alors l'avantage.
 */
internal fun difficulteDuCombat(issue: IssueEvenement?): EncounterDifficulty {
    val normale = EncounterDifficulty.MOYENNE
    if (issue?.nature != NatureIssue.ECHEC) return normale
    return EncounterDifficulty.entries.getOrElse(normale.ordinal + 1) { normale }
}
