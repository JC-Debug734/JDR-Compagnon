package com.jc2.jdrcompagnon.feature_preparation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.domain.model.PreparationSeance
import com.jc2.jdrcompagnon.feature_quete.domain.ValiderQueteUseCase
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestReward
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestRewardType
import com.jc2.jdrcompagnon.feature_quete.ui.RewardEditorDialog
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Groupes du monde, le groupe sélectionné (menu latéral) en premier. */
@Composable
private fun rememberGroupesMonde(worldId: String): List<GameState.MjGroup> {
    val groupes by GameState.mjGroups.collectAsState()
    val courant by GameState.currentGroupId.collectAsState()
    return groupes.filter { it.worldId == worldId }.sortedBy { if (it.id == courant) 0 else 1 }
}

/**
 * XP pour que chaque joueur du groupe gagne un niveau : écart entre son niveau et le suivant,
 * additionné sur tous les joueurs (PJ et joueurs sans fiche), puisque l'XP est partagée.
 */
private fun xpUnNiveau(groupe: GameState.MjGroup): Int {
    val pj = GameState.characters.value.filter { it.id in groupe.memberIds && it.type == "PJ" }
    val niveaux = pj.map { it.level } + groupe.tablePlayers.map { it.level }
    return niveaux.sumOf { n -> (com.jc2.jdrcompagnon.ui.CharacterProgression.xpForLevel(n + 1) - com.jc2.jdrcompagnon.ui.CharacterProgression.xpForLevel(n)).coerceAtLeast(0) }
}

/**
 * Récompenses prévues pour la séance (même modèle que les quêtes : objets du SRD, XP, or,
 * réputation...). XP libre, avec un raccourci « 1 niveau » calculé d'après le groupe ; une fois
 * données au groupe, le compte rendu remplace les boutons.
 */
@Composable
fun RecompensesContent(worldId: String, prep: PreparationSeance) {
    val pnjs = rememberPnjsMonde(worldId)
    val groupes = rememberGroupesMonde(worldId)
    var edition by remember { mutableStateOf<QuestReward?>(null) }
    var creation by remember { mutableStateOf(false) }
    var calculXp by remember { mutableStateOf(false) }
    var distribution by remember { mutableStateOf(false) }
    val distribuees = prep.recompensesDistribuees.isNotBlank()

    if (prep.recompensesPrevues.isEmpty()) {
        Text("Objets, XP, or, réputation... à donner pendant ou à la fin de la séance.", style = MaterialTheme.typography.bodyMedium, color = PrepHintColor)
    }
    prep.recompensesPrevues.forEach { r ->
        Row(
            modifier = Modifier.fillMaxWidth().then(if (distribuees) Modifier else Modifier.clickable { edition = r }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("• ${r.resume}", color = Color.White, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
            if (!distribuees) {
                IconButton(onClick = { CarnetMj.supprimerRecompense(worldId, r.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer", tint = PrepHintColor)
                }
            }
        }
    }
    if (distribuees) {
        Text("✔ Données", color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.labelLarge)
        Text(prep.recompensesDistribuees, color = PrepHintColor, style = MaterialTheme.typography.bodySmall)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { creation = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Ajouter", color = Color.White)
        }
        OutlinedButton(onClick = { calculXp = true }, enabled = groupes.isNotEmpty(), modifier = Modifier.weight(1f)) {
            Text("XP : 1 niveau", color = Color.White)
        }
    }
    if (prep.recompensesPrevues.isNotEmpty()) {
        OutlinedButton(onClick = { distribution = true }, enabled = groupes.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Donner au groupe", color = ForcedDarkPalette.AccentGold)
        }
    }

    if (creation || edition != null) {
        RewardEditorDialog(
            initial = edition,
            worldId = worldId,
            pnjs = pnjs,
            onDismiss = { creation = false; edition = null },
            onSave = { CarnetMj.enregistrerRecompense(worldId, it); creation = false; edition = null },
        )
    }
    if (calculXp) {
        ChoixGroupeDialog(
            titre = "XP pour 1 niveau",
            groupes = groupes,
            libelleValider = "Utiliser",
            apercu = { g -> "${xpUnNiveau(g)} XP au total, partagés entre les joueurs du groupe (un niveau chacun)." },
            onDismiss = { calculXp = false },
            onValider = { g ->
                val xp = xpUnNiveau(g)
                val existante = prep.recompensesPrevues.firstOrNull { it.type == QuestRewardType.XP }
                CarnetMj.enregistrerRecompense(worldId, (existante ?: QuestReward(type = QuestRewardType.XP)).copy(amount = xp))
                calculXp = false
            },
        )
    }
    if (distribution) {
        val characters by GameState.characters.collectAsState()
        ChoixGroupeDialog(
            titre = "Donner les récompenses",
            groupes = groupes,
            libelleValider = "Donner",
            apercu = { g ->
                val rep = ValiderQueteUseCase.repartition(prep.recompensesPrevues, g, characters)
                listOfNotNull(
                    "${rep.parts} joueur(s)",
                    rep.xpParJoueur.takeIf { it > 0 }?.let { "$it XP chacun" },
                    rep.orParJoueur.takeIf { it > 0 }?.let { "$it po chacun" },
                ).joinToString(" · ")
            },
            onDismiss = { distribution = false },
            onValider = { g -> CarnetMj.distribuerRecompenses(worldId, g.id); distribution = false },
        )
    }
}

@Composable
private fun ChoixGroupeDialog(
    titre: String,
    groupes: List<GameState.MjGroup>,
    libelleValider: String,
    apercu: (GameState.MjGroup) -> String,
    onDismiss: () -> Unit,
    onValider: (GameState.MjGroup) -> Unit,
) {
    var choisi by remember { mutableStateOf(groupes.firstOrNull()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groupes.forEach { g ->
                    Row(Modifier.fillMaxWidth().clickable { choisi = g }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = choisi?.id == g.id, onClick = { choisi = g })
                        Text(g.name)
                    }
                }
                choisi?.let {
                    HorizontalDivider()
                    Text(apercu(it), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(enabled = choisi != null, onClick = { choisi?.let(onValider) }) { Text(libelleValider) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
