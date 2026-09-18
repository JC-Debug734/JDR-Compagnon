package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.FiltresApprovisionnement

/**
 * Réapprovisionnement en masse : le MJ choisit une ou plusieurs catégories, un prix
 * plafond par objet, un nombre d'articles max et un budget total — la boutique pioche
 * dans le catalogue SRD en respectant ces contraintes plutôt que de chercher objet par objet.
 */
@Composable
fun ApprovisionnerBoutiqueDialog(
    equipementsDisponibles: List<EquipementReference>,
    budgetSuggereEnPo: Int,
    onDismiss: () -> Unit,
    onConfirmer: (filtres: FiltresApprovisionnement, budgetEnPo: Int) -> Unit
) {
    val categories = remember(equipementsDisponibles) {
        equipementsDisponibles.map { it.type }.distinct().sorted()
    }
    var categoriesSelectionnees by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuCategorieOuvert by remember { mutableStateOf(false) }
    var prixMaxTexte by remember { mutableStateOf("") }
    var nombreMaxTexte by remember { mutableStateOf("8") }
    var budgetTexte by remember { mutableStateOf(budgetSuggereEnPo.toString()) }

    val libelleCategories = if (categoriesSelectionnees.isEmpty()) {
        "Toutes catégories"
    } else {
        categoriesSelectionnees.joinToString(", ")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Réapprovisionner") },
        text = {
            Column {
                TextButton(onClick = { menuCategorieOuvert = true }) { Text("Catégories : $libelleCategories") }
                DropdownMenu(expanded = menuCategorieOuvert, onDismissRequest = { menuCategorieOuvert = false }) {
                    categories.forEach { categorie ->
                        val selectionnee = categorie in categoriesSelectionnees
                        DropdownMenuItem(
                            text = { Text(categorie) },
                            leadingIcon = {
                                Checkbox(
                                    checked = selectionnee,
                                    onCheckedChange = null // le clic est géré par tout le DropdownMenuItem
                                )
                            },
                            onClick = {
                                categoriesSelectionnees = if (selectionnee) {
                                    categoriesSelectionnees - categorie
                                } else {
                                    categoriesSelectionnees + categorie
                                }
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = prixMaxTexte,
                    onValueChange = { prixMaxTexte = it.filter(Char::isDigit) },
                    label = { Text("Prix max par objet (po) — vide = pas de plafond") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nombreMaxTexte,
                    onValueChange = { nombreMaxTexte = it.filter(Char::isDigit) },
                    label = { Text("Nombre d'articles max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetTexte,
                    onValueChange = { budgetTexte = it.filter(Char::isDigit) },
                    label = { Text("Budget total (po)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Budget suggéré selon le standing : $budgetSuggereEnPo po — modifiable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val filtres = FiltresApprovisionnement(
                        types = categoriesSelectionnees,
                        prixMaxEnPo = prixMaxTexte.toIntOrNull(),
                        nombreArticlesMax = nombreMaxTexte.toIntOrNull() ?: 8
                    )
                    onConfirmer(filtres, budgetTexte.toIntOrNull() ?: budgetSuggereEnPo)
                },
                enabled = nombreMaxTexte.toIntOrNull() != null && budgetTexte.toIntOrNull() != null
            ) { Text("Générer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}