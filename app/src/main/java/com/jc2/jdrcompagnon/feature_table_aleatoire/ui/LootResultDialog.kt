package com.jc2.jdrcompagnon.feature_table_aleatoire.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.ui.components.SendItemToCharacterDialog
import com.jc2.jdrcompagnon.ui.components.SendItemsToGroupDialog

/**
 * Résultat d'un tirage de loot, avec la possibilité d'envoyer chaque objet tiré à un personnage
 * ou à un groupe — même geste que pour un objet lié dans un scénario (#equipment:). Partagé entre
 * TableAleatoireDetailScreen (outil dédié) et ScenarioReaderContent (tirage rapide en lecture)
 * pour ne pas dupliquer la logique d'envoi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LootResultDialog(
    titre: String,
    resultats: List<ResultatLoot>,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    var envoyerPersonnageItem by remember { mutableStateOf<String?>(null) }
    // Un objet (icône groupe d'une ligne) ou tous les objets tirés ("Tout envoyer au groupe").
    var envoyerGroupeItems by remember { mutableStateOf<List<String>?>(null) }
    val tousLesLabels = resultats.map { "${it.quantite}x ${it.entree.equipementNom}" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre, fontWeight = FontWeight.Bold) },
        text = {
            if (resultats.isEmpty()) {
                Text("Aucun objet disponible dans cette table.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    resultats.forEach { resultat ->
                        val label = "${resultat.quantite}x ${resultat.entree.equipementNom}"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            IconButton(onClick = { envoyerPersonnageItem = label }) {
                                Icon(Icons.Default.Person, contentDescription = "Envoyer « $label » à un personnage")
                            }
                            IconButton(onClick = { envoyerGroupeItems = listOf(label) }) {
                                Icon(Icons.Default.Group, contentDescription = "Proposer « $label » à un groupe")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
        dismissButton = {
            if (tousLesLabels.size > 1) {
                TextButton(onClick = { envoyerGroupeItems = tousLesLabels }) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Text("  Tout envoyer au groupe")
                }
            }
        }
    )

    envoyerPersonnageItem?.let { itemName ->
        SendItemToCharacterDialog(itemName = itemName, worldId = worldId, onDismiss = { envoyerPersonnageItem = null })
    }
    envoyerGroupeItems?.let { items ->
        SendItemsToGroupDialog(itemNames = items, worldId = worldId, onDismiss = { envoyerGroupeItems = null })
    }
}
