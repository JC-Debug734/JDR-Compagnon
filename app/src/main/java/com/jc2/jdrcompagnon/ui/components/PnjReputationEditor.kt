package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.domain.model.Reputation
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import kotlin.math.roundToInt

/**
 * Réputation d'un PNJ : attitude envers des factions (libres) et envers chaque groupe
 * d'aventuriers du monde — le score d'un groupe vaut automatiquement pour tous ses membres
 * (cf. GameState.pnjReputationToward). Partagé entre la fiche (CharacterSheetScreen, carte
 * "Réputation" côté MJ) et l'onglet PNJ de CharacterEditScreen.
 *
 * Les curseurs ne publient leur valeur qu'au relâchement ([onUpdate] une seule fois par geste),
 * pour ne pas sauvegarder la fiche à chaque pixel quand [onUpdate] écrit dans GameState.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PnjReputationEditor(
    character: Character,
    onUpdate: (Character) -> Unit,
    textColor: Color,
    sectionTitle: @Composable (String) -> Unit,
) {
    val mjGroups by GameState.mjGroups.collectAsState()
    val allCharacters by GameState.characters.collectAsState()
    val groups = mjGroups.filter { it.worldId == character.worldId }
    var nouvelleFaction by remember { mutableStateOf("") }

    // Factions déjà connues dans ce monde (réputations des groupes et des autres PNJ),
    // proposées en raccourci pour éviter de retaper les mêmes noms.
    val factionsConnues = remember(mjGroups, allCharacters, character.factionReputations) {
        val dejaAjoutees = character.factionReputations.map { it.factionName.lowercase() }.toSet()
        (groups.flatMap { g -> g.reputations.map { it.factionName } } +
            allCharacters.filter { it.worldId == character.worldId }
                .flatMap { c -> c.factionReputations.map { it.factionName } })
            .filter { it.isNotBlank() && it.lowercase() !in dejaAjoutees }
            .distinctBy { it.lowercase() }
    }

    fun ajouterFaction(nom: String) {
        val propre = nom.trim()
        if (propre.isBlank() || character.factionReputations.any { it.factionName.equals(propre, true) }) return
        onUpdate(character.copy(factionReputations = character.factionReputations + Reputation(factionName = propre)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sectionTitle("Réputation — factions")
        if (character.factionReputations.isEmpty()) {
            Text("Aucune faction pour l'instant.", style = MaterialTheme.typography.bodySmall, color = textColor)
        }
        character.factionReputations.forEach { rep ->
            ReputationSliderRow(
                titre = rep.factionName,
                score = rep.score,
                textColor = textColor,
                onScoreChange = { score ->
                    onUpdate(character.copy(factionReputations = character.factionReputations.map {
                        if (it.factionId == rep.factionId) it.copy(score = score) else it
                    }))
                },
                onRemove = {
                    onUpdate(character.copy(factionReputations = character.factionReputations.filterNot { it.factionId == rep.factionId }))
                }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nouvelleFaction,
                onValueChange = { nouvelleFaction = it },
                label = { Text("Nouvelle faction") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = textColor,
                    unfocusedLabelColor = textColor.copy(alpha = 0.7f),
                )
            )
            IconButton(
                enabled = nouvelleFaction.isNotBlank(),
                onClick = { ajouterFaction(nouvelleFaction); nouvelleFaction = "" }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter la faction", tint = textColor)
            }
        }
        if (factionsConnues.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                factionsConnues.forEach { nom ->
                    AssistChip(onClick = { ajouterFaction(nom) }, label = { Text("+ $nom", color = textColor) })
                }
            }
        }

        sectionTitle("Réputation — groupes")
        if (groups.isEmpty()) {
            Text("Aucun groupe dans ce monde.", style = MaterialTheme.typography.bodySmall, color = textColor)
        }
        groups.forEach { group ->
            val score = character.groupReputations[group.id]
            val membres = allCharacters.filter { it.id in group.memberIds && it.id != character.id }
            Column {
                ReputationSliderRow(
                    titre = group.name,
                    score = score ?: 0,
                    nonDefini = score == null,
                    textColor = textColor,
                    onScoreChange = { onUpdate(character.copy(groupReputations = character.groupReputations + (group.id to it))) },
                    onRemove = if (score != null) {
                        { onUpdate(character.copy(groupReputations = character.groupReputations - group.id)) }
                    } else null
                )
                Text(
                    if (membres.isEmpty()) "Aucun membre dans ce groupe."
                    else "S'applique automatiquement à : " + membres.joinToString { it.name },
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ReputationSliderRow(
    titre: String,
    score: Int,
    textColor: Color,
    onScoreChange: (Int) -> Unit,
    onRemove: (() -> Unit)?,
    nonDefini: Boolean = false,
) {
    // Valeur locale pendant le glissement, publiée au relâchement.
    var enCours by remember(score) { mutableFloatStateOf(score.toFloat()) }
    val affiche = enCours.roundToInt()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titre, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
            Text(
                if (nonDefini && affiche == score) "Non défini" else "${ReputationScale.labelFor(affiche)} ($affiche)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Slider(
            value = enCours,
            onValueChange = { enCours = it },
            onValueChangeFinished = { onScoreChange(enCours.roundToInt()) },
            valueRange = -100f..100f
        )
    }
}
