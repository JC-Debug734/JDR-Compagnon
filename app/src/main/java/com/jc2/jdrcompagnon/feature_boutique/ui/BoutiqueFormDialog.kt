package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique

private const val OPTION_ALEATOIRE = "Aléatoire"

/**
 * Formulaire de boutique, utilisé pour la création (boutiqueExistante = null, standing
 * initial "Aléatoire") et l'édition (boutiqueExistante fourni, champs pré-remplis).
 */
@Composable
fun BoutiqueFormDialog(
    boutiqueExistante: Boutique? = null,
    onGenererNomBoutique: () -> String,
    onGenererNomMarchand: () -> String,
    onDismiss: () -> Unit,
    onConfirmer: (nom: String, nomMarchand: String, standing: StandingBoutique?, type: TypeBoutique) -> Unit
) {
    var nom by remember { mutableStateOf(boutiqueExistante?.nom.orEmpty()) }
    var nomMarchand by remember { mutableStateOf(boutiqueExistante?.marchand?.nom.orEmpty()) }
    // null = "Aléatoire" (uniquement pertinent à la création ; en édition on part toujours
    // du standing actuel, jamais de null, mais l'option reste accessible si le MJ veut relancer).
    var standingSelectionne by remember { mutableStateOf(boutiqueExistante?.standing) }
    var menuOuvert by remember { mutableStateOf(false) }
    var typeSelectionne by remember { mutableStateOf(boutiqueExistante?.type ?: TypeBoutique.MARCHAND) }
    var menuTypeOuvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (boutiqueExistante == null) "Nouvelle boutique" else "Modifier la boutique") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom de la boutique") },
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    )
                    IconButton(onClick = { nom = onGenererNomBoutique() }) {
                        Icon(Icons.Default.Casino, contentDescription = "Nom aléatoire")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nomMarchand,
                        onValueChange = { nomMarchand = it },
                        label = { Text("Nom du marchand") },
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    )
                    IconButton(onClick = { nomMarchand = onGenererNomMarchand() }) {
                        Icon(Icons.Default.Casino, contentDescription = "Nom aléatoire")
                    }
                }
                TextButton(onClick = { menuOuvert = true }) {
                    Text("Standing : ${standingSelectionne?.label ?: OPTION_ALEATOIRE}")
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(
                        text = { Text(OPTION_ALEATOIRE) },
                        onClick = { standingSelectionne = null; menuOuvert = false }
                    )
                    StandingBoutique.entries.forEach { standing ->
                        DropdownMenuItem(
                            text = { Text(standing.label) },
                            onClick = { standingSelectionne = standing; menuOuvert = false }
                        )
                    }
                }
                TextButton(onClick = { menuTypeOuvert = true }) {
                    Text("Type : ${typeSelectionne.label}")
                }
                DropdownMenu(expanded = menuTypeOuvert, onDismissRequest = { menuTypeOuvert = false }) {
                    TypeBoutique.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = { typeSelectionne = type; menuTypeOuvert = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmer(nom, nomMarchand, standingSelectionne, typeSelectionne) },
                enabled = nom.isNotBlank() && nomMarchand.isNotBlank()
            ) { Text(if (boutiqueExistante == null) "Créer" else "Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
