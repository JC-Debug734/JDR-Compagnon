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
import androidx.compose.material3.FilterChip
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
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.SceneProfilesColumn
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.openSceneProfileMenu
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.sceneProfiles
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Menu latéral partagé côté MJ, utilisé de la même façon sur tous les écrans
 * concernés (accueil MJ, sélection de personnage, fiche de personnage) pour
 * garder un menu identique quel que soit l'écran d'où on l'ouvre — avant
 * cette extraction, chaque écran réinventait son propre contenu de tiroir
 * (certains se limitaient à un simple "Retour"), ce qui donnait l'impression
 * de perdre l'accès aux outils MJ en changeant d'écran.
 *
 * Sélecteurs Campagne/Scénario branchés sur l'état global persistant
 * (GameState.currentCampaignId / GameState.lastScenarioId) plutôt que sur un
 * état local à un écran, pour que le choix fait ici reste cohérent partout,
 * y compris sur le tableau de bord MJ.
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
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { GameState.syncScenariosFromDisk(context) }
    // Scénarios et campagnes limités au monde courant : après un changement d'univers, le
    // scénario sélectionné d'un autre univers ne doit plus être accessible.
    val allScenarios by GameState.mjScenarios.collectAsState()
    val allCampaigns by GameState.mjCampaigns.collectAsState()
    val mjScenarios = allScenarios.filter { it.worldId == (currentWorld?.id ?: "") }
    val mjCampaigns = allCampaigns.filter { it.worldId == (currentWorld?.id ?: "") }
    val mjGroups by GameState.mjGroups.collectAsState()
    val characters by GameState.characters.collectAsState()
    val selectedCampaignId by GameState.currentCampaignId.collectAsState()
    val selectedScenarioId by GameState.lastScenarioId.collectAsState()
    val selectedGroupId by GameState.currentGroupId.collectAsState()
    var quickEditCharacter by remember { mutableStateOf<Character?>(null) }
    var questToValidate by remember { mutableStateOf<com.jc2.jdrcompagnon.feature_quete.domain.model.Quest?>(null) }
    val hasPendingGroupProposals by NetworkSessionManager.hasPendingGroupProposals.collectAsState()

    val selectedCampaign = selectedCampaignId?.let { id -> mjCampaigns.firstOrNull { it.id == id } }
    val visibleScenarios = if (selectedCampaign != null) {
        mjScenarios.filter { it.id in selectedCampaign.scenarioIds }
    } else {
        mjScenarios.filter { com.jc2.jdrcompagnon.ui.PorteeCampagne.scenarioVisible(it) }
    }
    val visibleGroups = mjGroups.filter { it.worldId == (currentWorld?.id ?: "") }
    val selectedGroup = selectedGroupId?.let { id -> visibleGroups.firstOrNull { it.id == id } }
    val selectedScenario = mjScenarios.firstOrNull { it.id == selectedScenarioId }

    ModalDrawerSheet(
        drawerContainerColor = ForcedDarkPalette.Surface,
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
            ScenarioClockRow()
            HorizontalDivider(color = ForcedDarkPalette.Indicator)

        // --- CAMPAGNE (affichage seul : la sélection se fait dans l'outil CAMPAGNES) : section
        // entièrement masquée tant qu'aucune campagne n'est sélectionnée. Le contenu réel
        // s'affiche directement (pas juste des titres cliquables vers un écran séparé). ---
        if (selectedCampaign != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Checklist, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CAMPAGNE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = ForcedDarkPalette.AccentGold
                )
            }
            Text(
                text = selectedCampaign.title,
                style = MaterialTheme.typography.bodySmall,
                color = ForcedDarkPalette.Content
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Quêtes en cours : un toucher ouvre la validation (choix du groupe, récompenses).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = ForcedDarkPalette.Content, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quêtes en cours", style = MaterialTheme.typography.labelMedium, color = ForcedDarkPalette.Content)
            }
            val quetesEnCours = selectedCampaign.quests.filter { it.status == com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_COURS }
            if (quetesEnCours.isEmpty()) {
                Text("Aucune quête en cours", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.Content, modifier = Modifier.padding(start = 26.dp))
            } else {
                quetesEnCours.forEach { quest ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { questToValidate = quest }
                            .padding(start = 26.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            quest.title + (quest.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = ForcedDarkPalette.Content,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Valider", tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = ForcedDarkPalette.Indicator)
        }

        // --- SCÉNARIOS (affichage seul : la sélection se fait dans l'outil SCÉNARIOS) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SCÉNARIOS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = ForcedDarkPalette.AccentGold
            )
        }
        // Seul le scénario sélectionné est affiché (même quand une campagne en compte plusieurs).
        Text(
            text = selectedScenario?.title ?: when {
                selectedCampaign != null && visibleScenarios.isEmpty() -> "Aucun scénario dans cette campagne"
                mjScenarios.isEmpty() -> "Aucun scénario"
                else -> "Aucun scénario sélectionné"
            },
            style = MaterialTheme.typography.bodySmall,
            color = ForcedDarkPalette.Content
        )

        // Profils présents dans la scène en cours du scénario sélectionné : un clic ouvre le
        // petit menu combat / dialogue / fiche (SceneProfileActionsHost, au niveau navigation).
        if (selectedScenario != null) {
            val sceneIndexes by GameState.currentSceneIndexByScenario.collectAsState()
            val scenes = selectedScenario.scenes.ifEmpty {
                listOf(GameState.MjScene(title = selectedScenario.title, markdownContent = selectedScenario.markdownContent))
            }
            val sceneIndex = (sceneIndexes[selectedScenario.id] ?: GameState.currentSceneIndex(selectedScenario.id))
                .coerceIn(0, scenes.size - 1)
            val currentScene = scenes[sceneIndex]
            val profiles = remember(currentScene.markdownContent, characters) {
                sceneProfiles(currentScene.markdownContent, characters)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TheaterComedy, contentDescription = null, tint = ForcedDarkPalette.Content, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Scène ${sceneIndex + 1} — ${currentScene.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForcedDarkPalette.Content
                )
            }
            if (profiles.isEmpty()) {
                Text(
                    "Aucun profil dans cette scène",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                    modifier = Modifier.padding(start = 26.dp)
                )
            } else {
                SceneProfilesColumn(
                    profiles = profiles,
                    contentColor = ForcedDarkPalette.Content,
                    onClick = { profile ->
                        onCloseDrawer()
                        openSceneProfileMenu(profile, currentScene, selectedScenario.worldId)
                    }
                )
            }
        }

        // --- GROUPE (affichage seul : la sélection se fait dans l'outil GROUPES) ---
        HorizontalDivider(color = ForcedDarkPalette.Indicator)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
        }
        Text(
            text = selectedGroup?.name ?: "Aucun groupe sélectionné",
            style = MaterialTheme.typography.bodySmall,
            color = ForcedDarkPalette.Content
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
                        // Inspiration et conditions visibles d'un coup d'œil ; un clic ouvre l'édition rapide.
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.Content)
                            val conditionsMembre = conditionsDe(member)
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
            HorizontalDivider(color = ForcedDarkPalette.Indicator)
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
        QuickEditCharacterDialog(
            character = liveMember,
            onDismiss = { quickEditCharacter = null },
            onOpenEtat = { nom ->
                quickEditCharacter = null
                onCloseDrawer()
                onOpenEtat(nom)
            },
        )
    }

    val questCampaign = selectedCampaign
    val quest = questToValidate
    if (quest != null && questCampaign != null) {
        com.jc2.jdrcompagnon.feature_quete.ui.ValidateQuestDialog(
            campaign = questCampaign,
            quest = quest,
            onDismiss = { questToValidate = null },
        )
    }
}

/** Conditions d'un personnage : texte libre séparé par des virgules (Character.condition). */
private fun conditionsDe(character: Character): List<String> =
    character.condition.split(",").map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Ajustements rapides MJ (inspiration, XP, conditions, or, fatigue) depuis le tiroir, sans passer
 * par la fiche complète. Les changements passent par GameState (source unique de vérité,
 * persistée) : si ce personnage est réclamé par un joueur connecté, NetworkSessionManager les
 * relaie automatiquement à ce client dès que GameState.characters change (voir son init).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickEditCharacterDialog(character: Character, onDismiss: () -> Unit, onOpenEtat: (String) -> Unit) {
    var xpDelta by rememberSaveable(character.id) { mutableStateOf("") }
    var goldDelta by rememberSaveable(character.id) { mutableStateOf("") }
    var autreCondition by rememberSaveable(character.id) { mutableStateOf("") }
    val claimedCharacters by NetworkSessionManager.claimedCharacters.collectAsState()
    val isConnectedToPlayer = character.id in claimedCharacters
    // Le champ condition peut contenir plusieurs états (« Paralysé, Empoisonné ») et des mentions libres.
    val conditions = conditionsDe(character)
    val etats = Etats.lire(character.condition)
    val autres = Etats.autres(character.condition)

    fun basculerEtat(etat: ConditionCombat) {
        GameState.setCondition(character.id, Etats.ecrire(character.condition, if (etat in etats) etats - etat else etats + etat))
    }

    fun retirerAutre(libre: String) {
        GameState.setCondition(character.id, Etats.ecrire(autres.filterNot { it == libre }.joinToString(", "), etats))
    }

    /** Un état saisi à la main (« paralysée ») est reconnu et coché ; sinon il reste une mention libre. */
    fun ajouterLibre(saisie: String) {
        val libelle = saisie.trim().takeIf { it.isNotEmpty() } ?: return
        val reconnu = Etats.reconnaitre(libelle)
        when {
            reconnu != null -> if (reconnu !in etats) basculerEtat(reconnu)
            autres.none { it.equals(libelle, ignoreCase = true) } ->
                GameState.setCondition(character.id, Etats.ecrire((autres + libelle).joinToString(", "), etats))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(character.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isConnectedToPlayer) {
                    Text(
                        "Connecté : les changements sont envoyés immédiatement au joueur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForcedDarkPalette.AccentGold,
                    )
                }

                // --- INSPIRATION HÉROÏQUE (règles 2024 : on l'a ou pas) ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (character.heroicInspiration) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = ForcedDarkPalette.AccentGold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (character.heroicInspiration) "Inspiration héroïque" else "Pas d'inspiration",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (character.heroicInspiration) {
                        OutlinedButton(onClick = { GameState.setHeroicInspiration(character.id, false) }) { Text("Retirer") }
                    } else {
                        Button(onClick = { GameState.setHeroicInspiration(character.id, true) }) { Text("Donner") }
                    }
                }

                // --- ÉTATS (catalogue Etats, le même qu'en combat et dans le livre États) ---
                Text(
                    "États : " + conditions.joinToString(", ").ifEmpty { "aucun" },
                    style = MaterialTheme.typography.labelMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etats.choisissables.forEach { etat ->
                        FilterChip(
                            selected = etat in etats,
                            onClick = { basculerEtat(etat) },
                            label = { Text(etat.label) },
                        )
                    }
                    // Mentions libres (hors catalogue) : affichées aussi, pour pouvoir les retirer.
                    autres.forEach { libre ->
                        FilterChip(selected = true, onClick = { retirerAutre(libre) }, label = { Text(libre) })
                    }
                }
                // Ce que les états cochés imposent (en combat : actions bloquées, vitesse 0…).
                com.jc2.jdrcompagnon.feature_combat.ui.ResumeEffetsEtats(etats)
                if (etats.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        etats.sortedBy { it.ordinal }.forEach { etat ->
                            TextButton(onClick = { onOpenEtat(etat.label) }) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(etat.label)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = autreCondition,
                        onValueChange = { autreCondition = it.replace(",", "") },
                        label = { Text("Autre état") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    TextButton(
                        onClick = {
                            ajouterLibre(autreCondition)
                            autreCondition = ""
                        },
                        enabled = autreCondition.isNotBlank(),
                    ) { Text("Ajouter") }
                }
                if (conditions.isNotEmpty()) {
                    TextButton(onClick = { GameState.setCondition(character.id, "") }) { Text("Retirer tous les états") }
                }

                // --- XP ---
                Text("Expérience : ${character.experience} (niveau ${character.level})", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(25, 50, 100, 250, 500).forEach { montant ->
                        AssistChip(onClick = { GameState.addExperience(character.id, montant) }, label = { Text("+$montant") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = xpDelta,
                        onValueChange = { xpDelta = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantité") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        xpDelta.toIntOrNull()?.let { GameState.addExperience(character.id, -it) }
                    }) { Text("−") }
                    TextButton(onClick = {
                        xpDelta.toIntOrNull()?.let { GameState.addExperience(character.id, it) }
                    }) { Text("+") }
                }

                // --- OR ---
                Text("Or : ${character.gold} po", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = goldDelta,
                        onValueChange = { goldDelta = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantité") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        goldDelta.toIntOrNull()?.let { GameState.addGold(character.id, -it) }
                    }) { Text("−") }
                    TextButton(onClick = {
                        goldDelta.toIntOrNull()?.let { GameState.addGold(character.id, it) }
                    }) { Text("+") }
                }

                // --- FATIGUE (épuisement) ---
                Text("Fatigue : niveau ${character.exhaustionLevel} / 6", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { GameState.setExhaustionLevel(character.id, character.exhaustionLevel - 1) },
                        enabled = character.exhaustionLevel > 0,
                    ) { Text("−") }
                    Text("${character.exhaustionLevel}", modifier = Modifier.padding(horizontal = 12.dp))
                    TextButton(
                        onClick = { GameState.setExhaustionLevel(character.id, character.exhaustionLevel + 1) },
                        enabled = character.exhaustionLevel < 6,
                    ) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
    )
}
