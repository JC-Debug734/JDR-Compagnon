package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Menu latéral partagé côté MJ, utilisé de la même façon sur tous les écrans
 * concernés (accueil MJ, sélection de personnage, fiche de personnage) pour
 * garder un menu identique quel que soit l'écran d'où on l'ouvre — avant
 * cette extraction, chaque écran réinventait son propre contenu de tiroir
 * (certains se limitaient à un simple "Retour"), ce qui donnait l'impression
 * de perdre l'accès aux outils MJ en changeant d'écran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MjDrawer(
    currentWorld: WorldState?,
    onOpenAccueil: () -> Unit,
    onOpenBoutiques: () -> Unit,
    onOpenEnvironnements: () -> Unit = {},
    onOpenTableAleatoire: () -> Unit = {},
    onOpenCombatActions: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenProposalStatus: () -> Unit = {},
    // Fiche d'un état dans le livre États de la bibliothèque (depuis l'édition rapide d'un personnage).
    onOpenEtat: (String) -> Unit = {},
    // Fiche du groupe sélectionné (appui sur la section GROUPE).
    onOpenGroupe: (groupId: String) -> Unit = {},
    // Retour à une activité quittée en cours de route (section « EN COURS »).
    onOpenCombatEnCours: () -> Unit = {},
    onOpenEpreuveEnCours: () -> Unit = {},
    // Aide de séance (section SÉANCE) : ouverture des entrées de l'agenda, préparation complète.
    navigationAgenda: com.jc2.jdrcompagnon.feature_preparation.ui.NavigationAgenda? = null,
    onOpenPreparation: () -> Unit = {},
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { GameState.syncScenariosFromDisk(context) }
    val mjGroups by GameState.mjGroups.collectAsState()
    val characters by GameState.characters.collectAsState()
    val selectedGroupId by GameState.currentGroupId.collectAsState()
    var quickEditCharacter by remember { mutableStateOf<Character?>(null) }
    // Appui sur l'horloge : réglage de l'heure, de la date et de la météo manuelle.
    var reglageHorloge by remember { mutableStateOf(false) }
    if (reglageHorloge) ReglageHorlogeDialog(onDismiss = { reglageHorloge = false })
    var aideSeance by remember { mutableStateOf(false) }
    if (aideSeance && navigationAgenda != null) {
        // Ouvrir un élément ferme l'aide et le menu ; lancer un combat ou une épreuve se fait dans l'aide.
        fun puis(action: () -> Unit) { aideSeance = false; onCloseDrawer(); action() }
        com.jc2.jdrcompagnon.feature_preparation.ui.AideSeanceDialog(
            onDismiss = { aideSeance = false },
            navigation = com.jc2.jdrcompagnon.feature_preparation.ui.NavigationAgenda(
                ouvrirScenario = { puis { navigationAgenda.ouvrirScenario(it) } },
                ouvrirPnj = { puis { navigationAgenda.ouvrirPnj(it) } },
                ouvrirEnvironnement = { puis { navigationAgenda.ouvrirEnvironnement(it) } },
                ouvrirVille = { c, v -> puis { navigationAgenda.ouvrirVille(c, v) } },
                ouvrirBoutique = { puis { navigationAgenda.ouvrirBoutique(it) } },
                ouvrirCombat = { puis { navigationAgenda.ouvrirCombat() } },
            ),
            onOpenPreparation = { puis(onOpenPreparation) },
        )
    }
    var noteRapide by remember { mutableStateOf(false) }
    if (noteRapide) {
        com.jc2.jdrcompagnon.feature_preparation.ui.NoteRapideDialog(
            worldId = currentWorld?.id ?: "",
            onDismiss = { noteRapide = false },
        )
    }
    val hasPendingGroupProposals by NetworkSessionManager.hasPendingGroupProposals.collectAsState()

    val visibleGroups = mjGroups.filter { it.worldId == (currentWorld?.id ?: "") }
    val selectedGroup = selectedGroupId?.let { id -> visibleGroups.firstOrNull { it.id == id } }

    // Joueur connecté qui incarne un personnage, et lieu où se trouve ce personnage : ville et
    // lieu visité signalés par son joueur (positionsVille), sinon le lieu du groupe.
    val claimedCharacters by NetworkSessionManager.claimedCharacters.collectAsState()
    val connectedClients by NetworkSessionManager.connectedClients.collectAsState()
    val positionsVille by NetworkSessionManager.positionsVille.collectAsState()
    val villes by remember {
        com.jc2.jdrcompagnon.di.CarteDependencies.repository.observerToutesLesVilles()
    }.collectAsState(initial = emptyList())
    fun joueurDe(c: Character): String? =
        claimedCharacters[c.id]?.let { clientId -> connectedClients.firstOrNull { it.id == clientId }?.displayName }
    fun lieuDe(c: Character): String? {
        val clientId = claimedCharacters[c.id]
        val position = positionsVille.firstOrNull { clientId != null && it.clientId == clientId }
            ?: positionsVille.firstOrNull { it.personnage.isNotBlank() && it.personnage.equals(c.name, ignoreCase = true) }
            // Pas de position réseau : lieu écrit sur la fiche par le menu joueur (même téléphone
            // ou fiche synchronisée), sinon le lieu du groupe.
            ?: return c.lieuVille.takeIf { it.isNotBlank() } ?: selectedGroup?.location?.takeIf { it.isNotBlank() }
        val ville = villes.firstOrNull { it.id == position.villeId }?.nom ?: selectedGroup?.location.orEmpty()
        val endroit = position.lieuNom.ifBlank { com.jc2.jdrcompagnon.feature_carte.ui.ENTREE_DE_LA_VILLE }
        return listOf(ville, endroit).filter { it.isNotBlank() }.joinToString(" · ")
    }

    ModalDrawerSheet(
        // Liseré doré sur le flanc droit du menu, comme celui du haut de la barre du bas
        // (en retrait des coins arrondis de la feuille).
        modifier = Modifier.drawWithContent {
            drawContent()
            val x = size.width - 1.dp.toPx()
            drawLine(
                color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore,
                start = androidx.compose.ui.geometry.Offset(x, 16.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(x, size.height - 16.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        },
        drawerContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
        drawerContentColor = ForcedDarkPalette.Content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Header with role icon (clickable to change role) and settings ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Role icon (clickable to return to role selection)
                IconButton(
                    onClick = {
                        onCloseDrawer()
                        GameState.requestRoleChange()
                    },
                    modifier = Modifier.size(70.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_mj),
                        contentDescription = "Maître du Jeu — changer de rôle",
                        modifier = Modifier.size(70.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Maître du Jeu",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForcedDarkPalette.AccentGold,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        onCloseDrawer()
                        onOpenSettings()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Réglages",
                        tint = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            currentWorld?.name?.let {
                Text(
                    text = "Univers : $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                )
            }
            ScenarioClockRow(onClick = { reglageHorloge = true })
            HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)

            // --- EN COURS : combat, discussion PNJ ou épreuve quittés sans être terminés ---
            val combatEnCours by com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession.etat.collectAsState()
            val discussionEnCours by com.jc2.jdrcompagnon.ui.screens.mj.scenario.DiscussionEnCours.enCours.collectAsState()
            val epreuveEnCours by com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession.etat.collectAsState()
            if (combatEnCours != null || discussionEnCours != null || epreuveEnCours != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EN COURS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = ForcedDarkPalette.AccentGold
                    )
                }
                if (combatEnCours != null) {
                    RetourEnCoursRow(Icons.Default.SportsMartialArts, "Retourner au combat") {
                        onCloseDrawer()
                        onOpenCombatEnCours()
                    }
                }
                discussionEnCours?.let { d ->
                    RetourEnCoursRow(Icons.Default.Forum, "Retourner à la discussion : ${d.character.name}") {
                        onCloseDrawer()
                        com.jc2.jdrcompagnon.ui.screens.mj.scenario.DiscussionEnCours.rouvrir()
                    }
                }
                epreuveEnCours?.let { e ->
                    RetourEnCoursRow(Icons.Default.Terrain, "Retourner à l'épreuve : ${e.epreuve.nom}") {
                        onCloseDrawer()
                        onOpenEpreuveEnCours()
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)
            }

        // --- SÉANCE : raccourci vers l'aide de séance (préparation du carnet MJ du monde) ---
        val carnets by com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj.carnets.collectAsState()
        val carnet = carnets[currentWorld?.id ?: ""] ?: com.jc2.jdrcompagnon.feature_preparation.domain.model.CarnetMonde()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { aideSeance = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.EventNote, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SÉANCE ${carnet.preparation.numero}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = ForcedDarkPalette.AccentGold
                )
                val secretsAPlacer = carnet.preparation.secretIds.count { id -> carnet.secrets.any { it.id == id && !it.decouvert } }
                val filsOuverts = carnet.fils.count { !it.resolu }
                Text(
                    listOfNotNull(
                        carnet.preparation.agenda.count { !it.fait }.takeIf { it > 0 }?.let { "$it à l'agenda" },
                        secretsAPlacer.takeIf { it > 0 }?.let { "$it secret(s) à placer" },
                        filsOuverts.takeIf { it > 0 }?.let { "$it fil(s) en suspens" },
                    ).joinToString(" · ").ifBlank { "Aide de séance" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Ouvrir l'aide de séance", tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
        }
        RetourEnCoursRow(Icons.Default.EditNote, "Note rapide") { noteRapide = true }
        HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)

        // Pas de section SCÉNARIOS ici : scénario et profils de la scène font doublon avec
        // l'icône « Profils et liens du scénario » de la lecture.

        // --- GROUPE (la sélection se fait dans l'outil GROUPES ; appui = fiche du groupe) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (selectedGroup != null) Modifier.clickable { onOpenGroupe(selectedGroup.id) } else Modifier)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GROUPE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = ForcedDarkPalette.AccentGold
            )
            if (selectedGroup != null) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = "Voir le groupe", tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = selectedGroup?.name ?: "Aucun groupe sélectionné",
            style = MaterialTheme.typography.bodySmall,
            color = ForcedDarkPalette.Content,
            modifier = if (selectedGroup != null) Modifier.clickable { onOpenGroupe(selectedGroup.id) } else Modifier,
        )
        if (selectedGroup != null) {
            val groupCharacters = characters.filter { it.id in selectedGroup.memberIds }
            if (groupCharacters.isEmpty() && selectedGroup.tablePlayers.isEmpty()) {
                Text(
                    "Aucun personnage dans ce groupe",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                    modifier = Modifier.padding(start = 26.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                groupCharacters.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { quickEditCharacter = member }
                            .padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ForcedDarkPalette.Content, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        // Joueur connecté, lieu, inspiration et conditions visibles d'un coup d'œil ;
                        // un clic ouvre les ajustements rapides (PersonnageMjDialog).
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                member.name + (joueurDe(member)?.let { " ($it)" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ForcedDarkPalette.Content,
                            )
                            // Points de vie : texte coloré ; un appui sur la ligne permet de les modifier.
                            Text(
                                "❤ " + com.jc2.jdrcompagnon.ui.components.textePv(member),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.jc2.jdrcompagnon.ui.components.couleurPv(member.currentHitPoints, member.maxHitPoints),
                            )
                            lieuDe(member)?.let { lieu ->
                                Text("📍 $lieu", style = MaterialTheme.typography.labelSmall, color = ForcedDarkPalette.Content)
                            }
                            // Conditions avec la fatigue (épuisement) et son niveau.
                            val conditionsMembre = conditionsDe(member) + listOfNotNull(com.jc2.jdrcompagnon.ui.screens.joueur.character.libelleEpuisement(member.exhaustionLevel))
                            if (conditionsMembre.isNotEmpty()) {
                                Text(
                                    conditionsMembre.joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (member.heroicInspiration) {
                            Icon(Icons.Default.Star, contentDescription = "Inspiration héroïque", tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                // Joueurs sans fiche : affichage seul (modifiables depuis l'outil GROUPES).
                selectedGroup.tablePlayers.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ForcedDarkPalette.Content.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${player.name} · niv.${player.level} (sans fiche)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForcedDarkPalette.Content
                        )
                    }
                }
            }
        }

        // --- PROPOSITION EN COURS (visible seulement tant qu'une proposition de groupe
        // n'est pas résolue : décision manquante, ou joueur ayant passé). ---
        if (hasPendingGroupProposals) {
            HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCloseDrawer()
                        onOpenProposalStatus()
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HowToVote, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROPOSITION EN COURS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = ForcedDarkPalette.AccentGold
                )
            }
        }

        }
    }

    quickEditCharacter?.let { member ->
        val liveMember = characters.firstOrNull { it.id == member.id } ?: member
        PersonnageMjDialog(
            character = liveMember,
            joueur = joueurDe(liveMember),
            lieu = lieuDe(liveMember),
            onDismiss = { quickEditCharacter = null },
            onOpenEtat = { nom ->
                quickEditCharacter = null
                onCloseDrawer()
                onOpenEtat(nom)
            },
        )
    }
}

/** Ligne de la section « EN COURS » du menu latéral MJ : retour à une activité non terminée. */
@Composable
private fun RetourEnCoursRow(icon: androidx.compose.ui.graphics.vector.ImageVector, texte: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 18.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ForcedDarkPalette.Content, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(texte, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.Content)
    }
}
