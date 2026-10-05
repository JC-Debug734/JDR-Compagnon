package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.ui.LancerCombatDialog
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Profil (monstre ou PNJ) présent dans une scène, déduit de ses liens internes. */
data class SceneProfile(val name: String, val isPnj: Boolean)

/**
 * Profils cités dans le texte d'une scène : liens #monster, #pnj/#npc, #event (discussion avec un
 * PNJ) et chaque créature d'une composition #combat. Un nom qui correspond à une fiche non-PJ est
 * traité comme un PNJ, même cité par #monster ou dans un #combat.
 */
fun sceneProfiles(content: String, characters: List<Character>): List<SceneProfile> {
    val pnjNames = characters.filter { it.type != "PJ" }.map { it.name.lowercase() }.toSet()
    val raw = extractInternalLinks(content).flatMap { (type, name) ->
        when (type) {
            "monster" -> listOf(SceneProfile(name, name.lowercase() in pnjNames))
            "pnj", "npc", "event" -> listOf(SceneProfile(name, true))
            "combat" -> CompositionCombat.parser(name).map { ligne ->
                SceneProfile(ligne.monstreNom, ligne.monstreNom.lowercase() in pnjNames)
            }
            else -> emptyList()
        }
    }
    return raw.distinctBy { it.name.lowercase() }
}

/**
 * Demande d'action sur un profil de scène (petit menu combat / dialogue / fiche). Émise depuis le
 * menu latéral MJ ou le lecteur de scénario, traitée par [SceneProfileActionsHost] posé une seule
 * fois au niveau de la navigation : le menu reste ainsi utilisable après fermeture du tiroir.
 */
object SceneProfileRequests {
    data class Request(
        val profile: SceneProfile,
        val worldId: String?,
        val discussion: GameState.PnjDiscussion? = null,
    )

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    fun open(request: Request) { _request.value = request }
    fun clear() { _request.value = null }
}

/** Ouvre le menu d'actions pour un profil de la scène donnée. */
fun openSceneProfileMenu(profile: SceneProfile, scene: GameState.MjScene?, worldId: String?) {
    val discussion = scene?.discussions?.firstOrNull { it.pnjName.equals(profile.name, ignoreCase = true) }
    SceneProfileRequests.open(SceneProfileRequests.Request(profile, worldId, discussion))
}

/**
 * Menu d'actions d'un profil de scène : lancer un combat contre lui (LancerCombatDialog), ouvrir
 * une interaction de type dialogue (PnjBriefingOverlay, PNJ uniquement) ou voir sa fiche.
 */
@Composable
fun SceneProfileActionsHost(onOpenInternalLink: (type: String, name: String) -> Unit) {
    val request by SceneProfileRequests.request.collectAsState()
    var combatComposition by remember { mutableStateOf<String?>(null) }
    var combatWorldId by remember { mutableStateOf<String?>(null) }
    var briefing by remember { mutableStateOf<Pair<Character, GameState.PnjDiscussion?>?>(null) }
    var ajoutGroupe by remember { mutableStateOf<Pair<SceneProfile, String?>?>(null) }
    var butin by remember { mutableStateOf<Pair<SceneProfile, String?>?>(null) }
    val groupeId by GameState.currentGroupId.collectAsState()

    request?.let { req ->
        val profile = req.profile
        val pnj = if (profile.isPnj) {
            GameState.characters.value.firstOrNull { it.type != "PJ" && it.name.equals(profile.name, ignoreCase = true) }
        } else null
        AlertDialog(
            onDismissRequest = { SceneProfileRequests.clear() },
            title = { Text(profile.name, fontWeight = FontWeight.Bold) },
            text = { Text(if (profile.isPnj) "PNJ — que faire ?" else "Monstre — que faire ?") },
            confirmButton = {},
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        SceneProfileRequests.clear()
                        combatWorldId = req.worldId
                        combatComposition = profile.name
                    }) { Text("⚔ Lancer un combat") }
                    if (profile.isPnj) {
                        TextButton(onClick = {
                            SceneProfileRequests.clear()
                            briefing = (pnj ?: pnjPourConversation(profile.name, req.worldId)) to req.discussion
                        }) { Text("💬 Conversation") }
                    }
                    TextButton(onClick = {
                        SceneProfileRequests.clear()
                        butin = profile to req.worldId
                    }) { Text(if (profile.isPnj) "💰 Possessions / échange" else "💰 Butin") }
                    TextButton(onClick = {
                        SceneProfileRequests.clear()
                        ajoutGroupe = profile to req.worldId
                    }) { Text("Ajouter au groupe") }
                    TextButton(onClick = {
                        SceneProfileRequests.clear()
                        onOpenInternalLink(if (profile.isPnj) "pnj" else "monster", profile.name)
                    }) { Text("Voir la fiche") }
                }
            }
        )
    }

    combatComposition?.let { composition ->
        LancerCombatDialog(
            composition = composition,
            worldId = combatWorldId,
            onDismiss = { combatComposition = null },
            onLance = {
                combatComposition = null
                onOpenInternalLink("combat", composition)
            }
        )
    }

    butin?.let { (profile, worldId) ->
        if (profile.isPnj) {
            val pnj = remember(profile.name) { pnjPourConversation(profile.name, worldId) }
            com.jc2.jdrcompagnon.feature_butin.ui.ButinPersonnageDialog(
                character = pnj,
                onDismiss = { butin = null },
            )
        } else {
            com.jc2.jdrcompagnon.feature_butin.ui.ButinMonstreDialog(
                nom = profile.name,
                cle = "profil:$worldId:${profile.name}",
                worldId = worldId,
                onDismiss = { butin = null },
            )
        }
    }

    ajoutGroupe?.let { (profile, worldId) ->
        AjouterAuGroupeDialog(
            type = if (profile.isPnj) "pnj" else "monster",
            nom = profile.name,
            worldId = worldId,
            onDismiss = { ajoutGroupe = null },
        )
    }

    briefing?.let { (character, discussion) ->
        PnjBriefingOverlay(
            character = character,
            discussion = discussion,
            reputationGroupe = groupeId?.let { character.groupReputations[it] },
            onDismiss = { briefing = null }
        )
    }
}

/** Liste verticale compacte des profils (menu latéral MJ). */
@Composable
fun SceneProfilesColumn(
    profiles: List<SceneProfile>,
    contentColor: Color,
    onClick: (SceneProfile) -> Unit,
) {
    profiles.forEach { profile ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(profile) }
                .padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (profile.isPnj) Icons.Default.Person else Icons.Default.Pets,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(profile.name, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
    }
}
