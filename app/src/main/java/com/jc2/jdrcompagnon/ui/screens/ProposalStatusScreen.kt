package com.jc2.jdrcompagnon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.SessionRole

/**
 * Écran "Proposition en cours", commun MJ/joueur (comme ScenarioClockScreen) : liste les
 * propositions de groupe encore non résolues (au moins un joueur sans décision, ou qui a passé),
 * cf. point 2 du plan réseau. Accessible depuis le menu latéral tant qu'il en reste une, via
 * NetworkSessionManager.hasPendingGroupProposals (MJ) / activeProposalState (joueur).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalStatusScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val role by NetworkSessionManager.role.collectAsState()
    val isMj = role == SessionRole.HOST
    val mjProposals by NetworkSessionManager.activeGroupProposals.collectAsState()
    val playerProposals by NetworkSessionManager.activeProposalState.collectAsState()
    val proposals = if (isMj) mjProposals.values.toList() else playerProposals.values.toList()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Proposition en cours", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (proposals.isEmpty()) {
                        Text("Aucune proposition en cours pour le moment.")
                    } else {
                        proposals.forEach { proposal ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(proposal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (proposal.description.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(proposal.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (proposal.rewardLabel.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Récompense : ${proposal.rewardLabel}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    proposal.pendingClientIds.forEach { clientId ->
                                        val name = proposal.clientNames[clientId] ?: "Joueur"
                                        val decision = proposal.decisions[clientId] ?: NetworkSessionManager.ProposalDecision.PENDING
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(name, style = MaterialTheme.typography.bodyMedium)
                                            Text(decisionLabel(decision), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (isMj) {
                                        Spacer(Modifier.height(12.dp))
                                        OutlinedButton(onClick = { NetworkSessionManager.closeGroupProposal(proposal.id) }) {
                                            Text("Clôturer")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decisionLabel(decision: NetworkSessionManager.ProposalDecision): String = when (decision) {
    NetworkSessionManager.ProposalDecision.PENDING -> "En attente"
    NetworkSessionManager.ProposalDecision.ACCEPTED -> "A accepté"
    NetworkSessionManager.ProposalDecision.DECLINED -> "A décliné"
    NetworkSessionManager.ProposalDecision.PASSED -> "A passé"
}
