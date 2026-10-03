package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement

/** Libellé compact d'un effet, ex. "+50 XP", "-10 réputation (Gardes)", "Gagne 1x Dague". */
fun effetLabel(effet: EffetEvenement): String {
    val signe = if (effet.gain) "+" else "-"
    return when (effet.categorie) {
        CategorieEffet.EXPERIENCE -> "$signe${effet.quantite} XP"
        CategorieEffet.OR -> "$signe${effet.quantite} po"
        CategorieEffet.REPUTATION -> "$signe${effet.quantite} réputation (${effet.cible.ifBlank { "?" }})"
        CategorieEffet.OBJET -> "${if (effet.gain) "Gagne" else "Perd"} ${effet.quantite}x ${effet.cible.ifBlank { "?" }}"
        CategorieEffet.AUTRE -> effet.description.ifBlank { "Autre" }
    }
}

/**
 * Éditeur des effets (gains / pertes) d'un événement : liste avec suppression, puis formulaire
 * d'ajout. Partagé par la bibliothèque d'événements et les tables aléatoires.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffetsEditor(
    effets: List<EffetEvenement>,
    onEffetsChanged: (List<EffetEvenement>) -> Unit,
    modifier: Modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
    titre: String = "Effets (gains / pertes)",
) {
    var categorie by remember { mutableStateOf(CategorieEffet.AUTRE) }
    var gain by remember { mutableStateOf(true) }
    var quantite by remember { mutableStateOf("1") }
    var cible by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categorieExpanded by remember { mutableStateOf(false) }

    val besoinCible = categorie == CategorieEffet.REPUTATION || categorie == CategorieEffet.OBJET
    val peutAjouter = if (categorie == CategorieEffet.AUTRE) {
        description.isNotBlank()
    } else {
        (quantite.toIntOrNull() ?: 0) > 0 && (!besoinCible || cible.isNotBlank())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(titre, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        if (effets.isEmpty()) {
            Text("Aucun effet pour l'instant.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        effets.forEachIndexed { index, effet ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(effetLabel(effet), style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { onEffetsChanged(effets.filterIndexed { i, _ -> i != index }) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer l'effet")
                }
            }
        }

        ExposedDropdownMenuBox(expanded = categorieExpanded, onExpandedChange = { categorieExpanded = !categorieExpanded }) {
            OutlinedTextField(
                value = categorie.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Catégorie") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categorieExpanded) }
            )
            ExposedDropdownMenu(expanded = categorieExpanded, onDismissRequest = { categorieExpanded = false }) {
                CategorieEffet.entries.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.label) }, onClick = { categorie = cat; categorieExpanded = false })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = gain, onClick = { gain = true }, label = { Text("Gain") })
            FilterChip(selected = !gain, onClick = { gain = false }, label = { Text("Perte") })
        }
        if (categorie != CategorieEffet.AUTRE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it.filter(Char::isDigit) },
                    label = { Text("Quantité") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp),
                    singleLine = true
                )
                if (besoinCible) {
                    OutlinedTextField(
                        value = cible,
                        onValueChange = { cible = it },
                        label = { Text(if (categorie == CategorieEffet.REPUTATION) "Faction" else "Nom de l'objet") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(if (categorie == CategorieEffet.AUTRE) "Description" else "Description (optionnel)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = peutAjouter,
            onClick = {
                onEffetsChanged(
                    effets + EffetEvenement(
                        categorie = categorie,
                        gain = gain,
                        quantite = quantite.toIntOrNull() ?: 1,
                        cible = cible,
                        description = description
                    )
                )
                quantite = "1"
                cible = ""
                description = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ajouter l'effet") }
    }
}
