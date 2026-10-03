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
import androidx.compose.ui.Modifier
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Employe
import com.jc2.jdrcompagnon.feature_boutique.domain.model.RoleEmploye

@Composable
fun AjouterEmployeDialog(
    onGenererNom: () -> String,
    onGenererTrait: () -> String,
    onDismiss: () -> Unit,
    onConfirmer: (nom: String, role: RoleEmploye, trait: String) -> Unit,
    // Non null : modification d'un employé existant (champs pré-remplis).
    initial: Employe? = null,
) {
    var nom by remember { mutableStateOf(initial?.nom ?: "") }
    var role by remember { mutableStateOf(initial?.role ?: RoleEmploye.VENDEUR) }
    var trait by remember { mutableStateOf(initial?.trait ?: onGenererTrait()) }
    var menuOuvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvel employé" else "Modifier l'employé") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { nom = onGenererNom() }) {
                        Icon(Icons.Default.Casino, contentDescription = "Nom aléatoire")
                    }
                }
                TextButton(onClick = { menuOuvert = true }) { Text("Rôle : ${role.label}") }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    RoleEmploye.entries.forEach { r ->
                        DropdownMenuItem(text = { Text(r.label) }, onClick = { role = r; menuOuvert = false })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = trait,
                        onValueChange = { trait = it },
                        label = { Text("Caractère") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { trait = onGenererTrait() }) {
                        Icon(Icons.Default.Casino, contentDescription = "Caractère aléatoire")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmer(nom, role, trait) }, enabled = nom.isNotBlank()) { Text(if (initial == null) "Ajouter" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
