package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue

/**
 * Éditeur des issues possibles d'un événement : pour chacune, sa nature (réussite, partielle,
 * échec, autre), un titre facultatif, ce qui se passe, et ses effets (dépliables).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IssuesEditor(
    issues: List<IssueEvenement>,
    onIssuesChanged: (List<IssueEvenement>) -> Unit,
) {
    // Issue dont les effets sont dépliés : une seule à la fois pour garder le dialogue lisible.
    var effetsDeplies by remember { mutableStateOf<String?>(null) }

    fun modifier(issue: IssueEvenement) = onIssuesChanged(issues.map { if (it.id == issue.id) issue else it })

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Issues possibles", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        if (issues.isEmpty()) {
            Text(
                "Aucune issue : l'événement n'a qu'un seul déroulement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        issues.forEach { issue ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = issue.nature.couleur.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, issue.nature.couleur.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            NatureIssue.entries.forEach { nature ->
                                FilterChip(
                                    selected = issue.nature == nature,
                                    onClick = { modifier(issue.copy(nature = nature)) },
                                    label = { Text(nature.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = nature.couleur.copy(alpha = 0.35f))
                                )
                            }
                        }
                        IconButton(onClick = { onIssuesChanged(issues - issue) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer l'issue")
                        }
                    }
                    OutlinedTextField(
                        value = issue.titre,
                        onValueChange = { modifier(issue.copy(titre = it)) },
                        label = { Text("Titre (optionnel, ex. « Le villageois les guide »)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = issue.description,
                        onValueChange = { modifier(issue.copy(description = it)) },
                        label = { Text("Ce qui se passe") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { modifier(issue.copy(declencheCombat = !issue.declencheCombat)) }
                    ) {
                        Checkbox(checked = issue.declencheCombat, onCheckedChange = null)
                        Text(
                            "Mène au combat (bouton « Lancer le combat » avec les profils)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    TextButton(onClick = { effetsDeplies = if (effetsDeplies == issue.id) null else issue.id }) {
                        Text(
                            if (issue.effets.isEmpty()) "Ajouter des effets"
                            else issue.effets.joinToString(" · ") { effetLabel(it) }
                        )
                    }
                    if (effetsDeplies == issue.id) {
                        EffetsEditor(
                            effets = issue.effets,
                            onEffetsChanged = { modifier(issue.copy(effets = it)) },
                            modifier = Modifier,
                            titre = "Effets de cette issue"
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                // Propose la nature qui manque le plus naturellement : réussite, échec, puis partielle.
                val nature = listOf(NatureIssue.REUSSITE, NatureIssue.ECHEC, NatureIssue.PARTIELLE)
                    .firstOrNull { n -> issues.none { it.nature == n } } ?: NatureIssue.AUTRE
                onIssuesChanged(issues + IssueEvenement(nature = nature))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Ajouter une issue", modifier = Modifier.padding(start = 6.dp))
        }
    }
}
