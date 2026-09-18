package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference

/**
 * Ajout d'un article au catalogue de la boutique. Le prix est pré-rempli avec le prix
 * suggéré (ajusté au standing) dès qu'un objet est choisi, mais reste modifiable par le MJ.
 */
@Composable
fun AjouterArticleDialog(
    equipementsDisponibles: List<EquipementReference>,
    prixSuggere: (EquipementReference) -> Int,
    onDismiss: () -> Unit,
    onConfirmer: (equipement: EquipementReference, prix: Int, quantite: Int, toujoursDisponible: Boolean) -> Unit
) {
    var recherche by remember { mutableStateOf("") }
    var equipementSelectionne by remember { mutableStateOf<EquipementReference?>(null) }
    var prixTexte by remember { mutableStateOf("") }
    var quantiteTexte by remember { mutableStateOf("1") }
    var toujoursDisponible by remember { mutableStateOf(false) }

    val resultats = remember(recherche, equipementsDisponibles) {
        val filtres = if (recherche.isBlank()) {
            equipementsDisponibles
        } else {
            equipementsDisponibles.filter { it.nom.contains(recherche, ignoreCase = true) }
        }
        filtres.take(30)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un article") },
        text = {
            Column {
                val choisi = equipementSelectionne
                if (choisi == null) {
                    OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        label = { Text("Rechercher un objet") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (equipementsDisponibles.isEmpty()) {
                        Text(
                            "Chargement du catalogue…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (resultats.isEmpty()) {
                                Text(
                                    "Aucun résultat",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            resultats.forEach { equipement ->
                                Text(
                                    text = "${equipement.nom} — ${equipement.coutBaseEnPo} po",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            equipementSelectionne = equipement
                                            prixTexte = prixSuggere(equipement).toString()
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(choisi.nom, style = MaterialTheme.typography.titleMedium)
                    Text(
                        choisi.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { equipementSelectionne = null }) { Text("Changer d'objet") }
                    OutlinedTextField(
                        value = prixTexte,
                        onValueChange = { prixTexte = it.filter(Char::isDigit) },
                        label = { Text("Prix (po)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quantiteTexte,
                        onValueChange = { quantiteTexte = it.filter(Char::isDigit) },
                        label = { Text("Quantité en stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = toujoursDisponible, onCheckedChange = { toujoursDisponible = it })
                        Text("Toujours disponible (ne sera jamais retiré lors d'une visite)")
                    }
                }
            }
        },
        confirmButton = {
            val choisi = equipementSelectionne
            TextButton(
                onClick = {
                    if (choisi != null) {
                        onConfirmer(choisi, prixTexte.toIntOrNull() ?: 0, quantiteTexte.toIntOrNull() ?: 1, toujoursDisponible)
                    }
                },
                enabled = choisi != null && prixTexte.toIntOrNull() != null && quantiteTexte.toIntOrNull() != null
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
