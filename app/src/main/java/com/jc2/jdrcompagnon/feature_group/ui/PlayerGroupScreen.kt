package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.domain.GroupeJoueur
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState

private val Hint = Color.White.copy(alpha = 0.8f)

/**
 * Groupe vu par un joueur (menu latéral) : membres (avec leur lieu), montures (et leurs sacoches,
 * où ranger des objets du sac du personnage),
 * véhicules, réputation, inventaire commun et biens. Connecté à une partie, tout vient du MJ
 * (TYPE_GROUP_INFO) et se met à jour en direct ; sinon, du groupe local du personnage choisi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerGroupScreen(onOpenMenu: () -> Unit) {
    val networkGroup by NetworkSessionManager.networkGroup.collectAsState()
    val groups by GameState.mjGroups.collectAsState()
    val characters by GameState.characters.collectAsState()
    val selectedCharacterId by GameState.selectedCharacterId.collectAsState()
    val groupe = networkGroup?.details ?: remember(groups, characters, selectedCharacterId) {
        GameState.groupForCharacter(selectedCharacterId, GameState.currentWorldId())?.let { GroupeJoueur.depuis(it, characters) }
    }
    // Personnage du joueur (celui qu'il incarne en partie, sinon celui choisi) : son sac
    // s'échange avec les sacoches des montures.
    val claimedId by NetworkSessionManager.claimedCharacterId.collectAsState()
    val personnage = characters.firstOrNull { it.id == (claimedId ?: selectedCharacterId) }
    var montureOuverte by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(groupe?.name ?: "Groupe", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        if (groupe == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Vous ne faites partie d'aucun groupe pour le moment.", color = Color.White)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SubsectionCard {
                SubsectionTitle("Membres — ${groupe.membres.size}")
                groupe.membres.forEach { m ->
                    Text(m.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    // Lieu de chaque membre : le sien, sinon celui du groupe qu'il accompagne.
                    Text(
                        "📍 " + m.location.ifBlank { groupe.location }.ifBlank { "Lieu non précisé" },
                        color = Color.White, style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        listOf(m.type, m.detail, "VIT ${m.speed} m").filter { it.isNotBlank() }.joinToString(" • "),
                        color = Hint, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (groupe.montures.isNotEmpty()) {
                SubsectionCard {
                    SubsectionTitle("Montures et animaux")
                    groupe.montures.forEach { m ->
                        // Un toucher ouvre ses sacoches (échange d'objets avec le sac du personnage).
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = m.id.isNotBlank()) { montureOuverte = m.id }
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                "${m.name} (${m.kind})" + if (m.bagages.isNotEmpty()) " · 🎒 ${m.bagages.size}" else "",
                                color = Color.White, fontWeight = FontWeight.Bold,
                            )
                            Text(
                                listOfNotNull(m.species.ifBlank { null }, m.speed.takeIf { it > 0 }?.let { "VIT $it m" }, m.rider?.let { "avec $it" }, m.location.ifBlank { null }?.let { "📍 $it" })
                                    .joinToString(" • "),
                                color = Hint, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (groupe.vehicules.isNotEmpty()) {
                SubsectionCard {
                    SubsectionTitle("Véhicules")
                    groupe.vehicules.forEach { v ->
                        Text(v.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            listOfNotNull(
                                v.type.ifBlank { null },
                                if (v.speed == null) "immobile" else "VIT ${v.speed} m",
                                v.tirePar.takeIf { it.isNotEmpty() }?.let { "tiré par ${it.joinToString()}" },
                                v.location.ifBlank { null }?.let { "📍 $it" },
                            ).joinToString(" • "),
                            color = Hint, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (groupe.reputations.isNotEmpty()) {
                SubsectionCard {
                    SubsectionTitle("Réputation")
                    groupe.reputations.sortedByDescending { it.score }.forEach { r ->
                        Text("${r.faction} — ${ReputationScale.labelFor(r.score)} (${r.score})", color = Color.White)
                    }
                }
            }
            SubsectionCard {
                SubsectionTitle("Inventaire du groupe")
                if (groupe.inventaire.isEmpty()) Text("Vide.", color = Hint)
                groupe.inventaire.forEach { o ->
                    Text(
                        o.name + (if (o.quantity > 1) " ×${o.quantity}" else "") + (o.location.ifBlank { null }?.let { " · 📍 $it" } ?: ""),
                        color = Color.White,
                    )
                }
            }
            if (groupe.biens.isNotEmpty()) {
                SubsectionCard {
                    SubsectionTitle("Biens")
                    groupe.biens.forEach { b ->
                        Text(b.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        Text(listOfNotNull(b.detail.ifBlank { null }, b.location.ifBlank { null }?.let { "📍 $it" }).joinToString(" • "), color = Hint, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        groupe.montures.firstOrNull { it.id == montureOuverte }?.let { monture ->
            SacochesMontureDialog(
                monture = monture,
                personnage = personnage,
                onTransfert = { objet, versMonture ->
                    val perso = personnage ?: return@SacochesMontureDialog
                    NetworkSessionManager.transfererBagage(groupe.id, perso.id, monture.id, objet, versMonture)
                },
                onDismiss = { montureOuverte = null },
            )
        }
    }
}

/**
 * Sacoches d'une monture ou d'un animal : ce qu'elle porte, et le sac du personnage du joueur.
 * « Ranger » déplace un objet du sac vers les sacoches, « Reprendre » l'inverse.
 */
@Composable
private fun SacochesMontureDialog(
    monture: com.jc2.jdrcompagnon.network.MontureJoueurData,
    personnage: com.jc2.jdrcompagnon.ui.Character?,
    onTransfert: (objet: String, versMonture: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sacoches de ${monture.name}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Dans les sacoches", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (monture.bagages.isEmpty()) Text("Vide.", style = MaterialTheme.typography.bodySmall)
                monture.bagages.groupingBy { it }.eachCount().forEach { (objet, n) ->
                    LigneObjet(objet, n, action = "Reprendre", actif = personnage != null) { onTransfert(objet, false) }
                }
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    personnage?.let { "Sac de ${it.name}" } ?: "Aucun personnage sélectionné",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                )
                if (personnage != null && personnage.backpackItems.isEmpty()) Text("Vide.", style = MaterialTheme.typography.bodySmall)
                personnage?.backpackItems?.groupingBy { it }?.eachCount()?.forEach { (objet, n) ->
                    LigneObjet(objet, n, action = "Ranger", actif = true) { onTransfert(objet, true) }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun LigneObjet(objet: String, quantite: Int, action: String, actif: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(objet + if (quantite > 1) " ×$quantite" else "", modifier = Modifier.weight(1f))
        androidx.compose.material3.TextButton(onClick = onClick, enabled = actif) { Text(action) }
    }
}
