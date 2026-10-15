package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.ui.Monnaie

/**
 * Ajout de toute une catégorie d'équipements à l'inventaire d'une boutique, avec des contraintes
 * facultatives : prix de base minimum et maximum (« 5 pa », « 10 po »), mot-clé dans le nom et
 * quantité par article. L'aperçu montre les équipements retenus (ceux déjà en vente exclus).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AjouterCategorieDialog(
    equipementsDisponibles: List<EquipementReference>,
    categorieInitiale: String?,
    dejaEnVente: Set<String>,
    onDismiss: () -> Unit,
    onConfirmer: (equipements: List<EquipementReference>, quantite: Int) -> Unit,
) {
    val categories = remember(equipementsDisponibles) { equipementsDisponibles.map { it.type }.distinct().sorted() }
    var categorie by remember { mutableStateOf(categorieInitiale) }
    var prixMin by remember { mutableStateOf("") }
    var prixMax by remember { mutableStateOf("") }
    var motCle by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("1") }

    val minPc = Monnaie.parser(prixMin)
    val maxPc = Monnaie.parser(prixMax)
    val retenus = equipementsDisponibles.filter { e ->
        e.type == categorie &&
            e.nom !in dejaEnVente &&
            (minPc == null || e.coutBaseEnPc >= minPc) &&
            (maxPc == null || e.coutBaseEnPc <= maxPc) &&
            (motCle.isBlank() || e.nom.contains(motCle.trim(), ignoreCase = true))
    }.distinctBy { it.nom }.sortedBy { it.nom }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une catégorie") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (equipementsDisponibles.isEmpty()) {
                    Text("Chargement du catalogue…", style = MaterialTheme.typography.bodySmall)
                }
                Text("Catégorie", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        FilterChip(selected = c == categorie, onClick = { categorie = c }, label = { Text(c) })
                    }
                }
                Text("Contraintes (facultatives)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prixMin, onValueChange = { prixMin = it },
                        label = { Text("Prix min") }, placeholder = { Text("5 pa") },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = prixMax, onValueChange = { prixMax = it },
                        label = { Text("Prix max") }, placeholder = { Text("50 po") },
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = motCle, onValueChange = { motCle = it },
                    label = { Text("Le nom contient") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = quantite, onValueChange = { quantite = it.filter(Char::isDigit).take(3) },
                    label = { Text("Quantité par article") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (categorie != null) {
                    Text("${retenus.size} équipement(s) à ajouter :", style = MaterialTheme.typography.labelMedium)
                    Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        retenus.forEach { e ->
                            Text("${e.nom} — ${Monnaie.libelle(e.coutBaseEnPc)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmer(retenus, quantite.toIntOrNull() ?: 1) },
                enabled = retenus.isNotEmpty(),
            ) { Text("Ajouter tout (${retenus.size})") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
