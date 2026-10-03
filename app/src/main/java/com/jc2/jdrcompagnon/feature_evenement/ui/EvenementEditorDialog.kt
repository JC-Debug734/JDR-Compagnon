package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Création / modification d'un événement de la bibliothèque. [initial] null = nouvel événement
 * du monde [worldId] ([typeParDefaut] présélectionné). Réutilisable partout où l'on veut créer un
 * événement à la volée (table, ville, scène) : l'appelant l'enregistre via [onSave].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EvenementEditorDialog(
    initial: Evenement?,
    worldId: String,
    onDismiss: () -> Unit,
    onSave: (Evenement) -> Unit,
    typeParDefaut: TypeEvenement = TypeEvenement.RENCONTRE,
    campagneParDefaut: String? = null,
) {
    var titre by remember { mutableStateOf(initial?.titre ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: typeParDefaut) }
    var campagneId by remember { mutableStateOf(initial?.campagneId ?: campagneParDefaut) }
    var effets by remember { mutableStateOf(initial?.effets ?: emptyList()) }
    var objectif by remember { mutableStateOf(initial?.objectif ?: "") }
    var profils by remember { mutableStateOf(initial?.profils ?: emptyList()) }
    // Nouvel événement : une réussite et un échec à compléter (les issues laissées vides sont
    // ignorées à l'enregistrement).
    var issues by remember {
        mutableStateOf(initial?.issues ?: listOf(IssueEvenement(nature = NatureIssue.REUSSITE), IssueEvenement(nature = NatureIssue.ECHEC)))
    }
    var porteeExpanded by remember { mutableStateOf(false) }

    val campagnes = GameState.mjCampaigns.collectAsState().value.filter { it.worldId == worldId }
    val porteeLabel = campagnes.firstOrNull { it.id == campagneId }?.title ?: "Commun à tout le monde"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvel événement" else "Modifier l'événement") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    label = { Text("Titre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeEvenement.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) },
                            leadingIcon = { Icon(t.icone, contentDescription = null, tint = t.couleur, modifier = Modifier.size(18.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = t.couleur.copy(alpha = 0.3f))
                        )
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = porteeExpanded, onExpandedChange = { porteeExpanded = !porteeExpanded }) {
                    OutlinedTextField(
                        value = porteeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Portée") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(porteeExpanded) }
                    )
                    ExposedDropdownMenu(expanded = porteeExpanded, onDismissRequest = { porteeExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Commun à tout le monde") },
                            onClick = { campagneId = null; porteeExpanded = false }
                        )
                        campagnes.forEach { campagne ->
                            DropdownMenuItem(
                                text = { Text("Campagne : ${campagne.title}") },
                                onClick = { campagneId = campagne.id; porteeExpanded = false }
                            )
                        }
                    }
                }
                ProfilsEditor(profils = profils, worldId = worldId, onProfilsChanged = { profils = it })
                OutlinedTextField(
                    value = objectif,
                    onValueChange = { objectif = it },
                    label = { Text("Ce qui est attendu : une option par ligne (action : jet)") },
                    placeholder = { Text("Payer : 20 po\nNégocier : Charisme (Persuasion) DD 13\nRefus : combat") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                EffetsEditor(effets = effets, onEffetsChanged = { effets = it }, modifier = Modifier, titre = "Effets immédiats (dès que l'événement survient)")
                IssuesEditor(issues = issues, onIssuesChanged = { issues = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = titre.isNotBlank(),
                onClick = {
                    val base = initial ?: Evenement(worldId = worldId, titre = "")
                    onSave(
                        base.copy(
                            titre = titre.trim(),
                            description = description.trim(),
                            type = type,
                            campagneId = campagneId,
                            effets = effets,
                            objectif = objectif.trim(),
                            profils = profils,
                            issues = issues.filterNot { it.estVide }
                        )
                    )
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
