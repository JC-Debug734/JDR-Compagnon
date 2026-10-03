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
    val networkQuests by NetworkSessionManager.networkQuests.collectAsState()
    val campaigns by GameState.mjCampaigns.collectAsState()
    val campaignId by GameState.currentCampaignId.collectAsState()
    val characters by GameState.characters.collectAsState()
    val quests = networkQuests ?: remember(campaigns, campaignId, characters) { QuetesJoueur.locales() }
    var tab by rememberSaveable { mutableStateOf(0) }
    val actives = setOf(QuestStatus.EN_COURS.name, QuestStatus.EN_ATTENTE.name)
    val enCours = quests.filter { it.status in actives }
    val finies = quests.filter { it.status !in actives }
    val affichees = if (tab == 0) enCours else finies

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
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = Color.White) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("En cours (${enCours.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Terminées (${finies.size})") })
            }
            if (affichees.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (tab == 0) "Aucune quête en cours pour le moment." else "Aucune quête terminée.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(affichees, key = { it.id }) { quest -> PlayerQuestCard(quest) }
                }
            }
        }
    }
}

@Composable
private fun PlayerQuestCard(quest: QuestJoueurData) {
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
            }
        }
    }
}
