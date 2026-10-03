package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Écran du groupe : membres, PNJ, montures, véhicules, réputation, inventaire, biens (tout
 * localisé) et calculateur de rencontre — ouvert depuis GroupsScreen via un tap sur la carte du
 * groupe. Voir GroupDetailContent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onOpenBestiaryDetail: (String) -> Unit,
    onOpenEquipmentDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCharacter: (Character) -> Unit = {},
) {
    val characters by GameState.characters.collectAsState()
    val mjGroups by GameState.mjGroups.collectAsState()
    val group = remember(mjGroups, groupId) { mjGroups.firstOrNull { it.id == groupId } }
    // Toutes les fiches (PJ, PNJ, créatures) du monde du groupe : GroupDetailContent les
    // répartit par catégorie.
    val campagnes by GameState.mjCampaigns.collectAsState()
    val campagneCourante by GameState.currentCampaignId.collectAsState()
    val availableCharacters = remember(characters, group, campagnes, campagneCourante) {
        val duMonde = characters.filter { it.worldId == group?.worldId }
        // Fiches d'une campagne non sélectionnée masquées, sauf celles déjà membres du groupe.
        val visibles = com.jc2.jdrcompagnon.ui.PorteeCampagne.personnagesVisibles(duMonde, group?.worldId).map { it.id }.toSet()
        duMonde.filter { it.id in visibles || it.id in group?.memberIds.orEmpty() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(group?.name ?: "Groupe", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (group == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                Text("Groupe introuvable.", color = Color.White)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var name by remember(group.id) { mutableStateOf(group.name) }
            LaunchedEffect(group.id, group.name) { name = group.name }
            OutlinedTextField(
                value = name,
                onValueChange = { value ->
                    name = value
                    if (value.isNotBlank()) GameState.updateMjGroup(group.copy(name = value))
                },
                label = { Text("Nom du groupe") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            GroupDetailContent(
                group = group,
                availableCharacters = availableCharacters,
                onMemberToggle = { characterId ->
                    if (group.memberIds.contains(characterId)) {
                        // Un membre qui quitte le groupe descend de sa monture (et retrouve sa vitesse).
                        group.mounts.filter { it.riderCharacterId == characterId }
                            .forEach { GameState.assignMountRider(group.id, it.id, null) }
                        val fresh = GameState.mjGroups.value.firstOrNull { it.id == group.id } ?: group
                        GameState.updateMjGroup(fresh.copy(memberIds = fresh.memberIds - characterId))
                    } else {
                        GameState.updateMjGroup(group.copy(memberIds = group.memberIds + characterId))
                    }
                },
                onOpenBestiaryDetail = onOpenBestiaryDetail,
                onOpenEquipmentDetail = onOpenEquipmentDetail,
                onOpenCharacter = onOpenCharacter,
                onTablePlayersChange = { players -> GameState.updateMjGroup(group.copy(tablePlayers = players)) },
            )
        }
    }
}
