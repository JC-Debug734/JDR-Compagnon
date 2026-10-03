package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Service

/**
 * Ajout ou paramétrage d'un service de boutique : nom, description, tarif, nombre de places
 * (vide = illimité) et disponibilité.
 */
@Composable
fun ServiceFormDialog(
    initial: Service?,
    onDismiss: () -> Unit,
    onConfirmer: (Service) -> Unit,
) {
    var nom by remember { mutableStateOf(initial?.nom ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var prix by remember { mutableStateOf(initial?.prixEnPo?.toString() ?: "") }
    var quantite by remember { mutableStateOf(initial?.quantiteDisponible?.toString() ?: "") }
    var actif by remember { mutableStateOf(initial?.actif ?: true) }
    val prixValide = prix.toIntOrNull()?.let { it >= 0 } == true
    val quantiteValide = quantite.isBlank() || quantite.toIntOrNull()?.let { it >= 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau service" else "Modifier le service") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom (ex : Réparation d'armure)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prix,
                    onValueChange = { prix = it.filter(Char::isDigit) },
                    label = { Text("Tarif (po)") },
                    isError = prix.isNotEmpty() && !prixValide,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it.filter(Char::isDigit) },
                    label = { Text("Places disponibles (vide = illimité)") },
                    isError = !quantiteValide,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = actif, onCheckedChange = { actif = it })
                    Text("Proposé actuellement")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmer(
                        Service(
                            nom = nom.trim(),
                            description = description.trim(),
                            prixEnPo = prix.toInt(),
                            quantiteDisponible = quantite.toIntOrNull(),
                            actif = actif,
                            personnalise = true,
                        )
                    )
                },
                enabled = nom.isNotBlank() && prixValide && quantiteValide
            ) { Text(if (initial == null) "Ajouter" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
