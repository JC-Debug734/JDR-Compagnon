package com.jc2.jdrcompagnon.feature_quete.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.jc2.jdrcompagnon.feature_quete.domain.QuetesJoueur
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.QuestJoueurData
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Journal de quêtes côté joueur (menu latéral) : quêtes en cours et terminées que le MJ a rendues
 * visibles. Connecté à une partie, la liste vient du MJ (TYPE_QUESTS_STATE) ; sinon, de la
 * campagne sélectionnée sur cet appareil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerQuestsScreen(onOpenMenu: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quêtes", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        PlayerQuestsContent(Modifier.fillMaxSize().padding(innerPadding), PaddingValues(16.dp))
    }
}

/**
 * Journal de quêtes en fenêtre, ouvert depuis le menu latéral du joueur (comme la liste des lieux
 * d'une ville) : on le consulte sans quitter l'écran en cours.
 */
@Composable
fun PlayerQuestsDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quêtes") },
        text = { PlayerQuestsContent(Modifier.fillMaxWidth().heightIn(max = 480.dp), PaddingValues(vertical = 12.dp)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/** Onglets « En cours » / « Terminées » et cartes de quêtes (écran comme fenêtre). */
@Composable
private fun PlayerQuestsContent(modifier: Modifier, listPadding: PaddingValues) {
    val networkQuests by NetworkSessionManager.networkQuests.collectAsState()
    val campaigns by GameState.mjCampaigns.collectAsState()
    val campaignId by GameState.currentCampaignId.collectAsState()
    val characters by GameState.characters.collectAsState()
    // Personnage du joueur : celui qu'il incarne en partie réseau, sinon celui sélectionné.
    val claimedId by NetworkSessionManager.claimedCharacterId.collectAsState()
    val selectedId by GameState.selectedCharacterId.collectAsState()
    val personnage = characters.firstOrNull { it.id == (claimedId ?: selectedId) }
    val quests = networkQuests ?: remember(campaigns, campaignId, characters) { QuetesJoueur.locales() }
    var tab by rememberSaveable { mutableStateOf(0) }
    val actives = setOf(QuestStatus.EN_COURS.name, QuestStatus.EN_ATTENTE.name)
    // Quête abandonnée par ce personnage : rangée avec les terminées.
    fun abandonnee(q: QuestJoueurData) = personnage != null && q.abandonneePar.any { it.equals(personnage.name, ignoreCase = true) }
    val enCours = quests.filter { it.status in actives && !abandonnee(it) }
    val finies = quests.filter { it.status !in actives || abandonnee(it) }
    val affichees = if (tab == 0) enCours else finies

    Column(modifier) {
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = Color.White) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("En cours (${enCours.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Terminées (${finies.size})") })
        }
        if (affichees.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (tab == 0) "Aucune quête en cours pour le moment." else "Aucune quête terminée.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(affichees, key = { it.id }) { quest -> PlayerQuestCard(quest, personnage) }
            }
        }
    }
}

@Composable
private fun PlayerQuestCard(quest: QuestJoueurData, personnage: com.jc2.jdrcompagnon.ui.Character?) {
    var confirmerAbandon by remember(quest.id) { mutableStateOf(false) }
    // Quête en cours (acceptée sur un panneau ou confiée par le MJ) : ce personnage peut l'abandonner.
    val abandonneeParMoi = personnage != null && quest.abandonneePar.any { it.equals(personnage.name, ignoreCase = true) }
    val abandonnable = personnage != null && !abandonneeParMoi &&
        (quest.status == QuestStatus.EN_COURS.name || quest.status == QuestStatus.EN_ATTENTE.name)
    var expanded by rememberSaveable(quest.id) { mutableStateOf(quest.status == QuestStatus.EN_COURS.name) }
    val hint = Color.White.copy(alpha = 0.8f)
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (quest.status) {
                        QuestStatus.TERMINEE.name -> Icons.Default.CheckCircle
                        QuestStatus.ECHOUEE.name -> Icons.Default.Close
                        QuestStatus.EN_ATTENTE.name -> Icons.Default.HourglassEmpty
                        else -> Icons.Default.Flag
                    },
                    contentDescription = null,
                    tint = if (quest.status == QuestStatus.ECHOUEE.name) MaterialTheme.colorScheme.error else ForcedDarkPalette.AccentGold,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    quest.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.White)
            }
            val meta = listOfNotNull(
                quest.location.ifBlank { null }?.let { "📍 $it" },
                quest.giverName?.let { "Confiée par $it" },
                "Abandonnée".takeIf { abandonneeParMoi },
                QuestStatus.entries.firstOrNull { it.name == quest.status }?.takeIf { it != QuestStatus.EN_COURS }?.label,
            ).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, color = hint, style = MaterialTheme.typography.bodySmall)
            if (expanded) {
                if (quest.description.isNotBlank()) {
                    Text(quest.description, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
                if (quest.rewards.isNotEmpty()) {
                    Text("Récompenses", color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.labelLarge)
                    quest.rewards.forEach { Text("• $it", color = Color.White, style = MaterialTheme.typography.bodySmall) }
                }
                if (quest.accepteePar.isNotEmpty()) {
                    Text("✔ Acceptée par ${quest.accepteePar.joinToString(", ")}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                if (abandonnable) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirmerAbandon = true },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Abandonner la quête", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }

    if (confirmerAbandon && personnage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmerAbandon = false },
            title = { Text("Abandonner la quête ?") },
            text = { Text("Voulez-vous vraiment abandonner la quête « ${quest.title} » ? Le MJ en sera averti.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    NetworkSessionManager.abandonnerQuete(quest.id, personnage.id, personnage.name)
                    confirmerAbandon = false
                }) { Text("Abandonner", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmerAbandon = false }) { Text("Annuler") } },
        )
    }
}
