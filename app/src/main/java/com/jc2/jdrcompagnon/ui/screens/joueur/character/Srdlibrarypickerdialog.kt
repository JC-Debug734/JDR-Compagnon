package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Résultat affiché dans [SrdLibraryPickerDialog] : le nom exact tel qu'il existe dans
 * la bibliothèque SRD (utilisé tel quel comme clé de référence, ex. dans
 * `Character.backpackItems` ou `Character.spells`), et un sous-titre optionnel
 * (catégorie, école/niveau, etc.) purement informatif.
 */
data class SrdPickerEntry(
    val name: String,
    val subtitle: String? = null,
)

/**
 * Dialogue générique de recherche dans une bibliothèque SRD (équipement, sorts, ...).
 * Ne connaît rien du contenu réel : [search] est fourni par l'appelant et interroge
 * [com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository], pour rester la
 * source unique de vérité sur le contenu SRD plutôt que de le dupliquer ici.
 *
 * [search] est ré-invoqué à chaque changement de texte (avec un léger anti-rebond),
 * y compris avec une requête vide pour afficher la liste complète.
 */
@Composable
fun SrdLibraryPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    search: suspend (query: String) -> List<SrdPickerEntry>,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SrdPickerEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        isLoading = true
        delay(200) // anti-rebond : évite une recherche à chaque frappe
        results = search(query)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Rechercher...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    when {
                        isLoading -> Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }

                        results.isEmpty() -> Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aucun résultat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(results, key = { it.name }) { entry ->
                                Surface(
                                    onClick = {
                                        onSelect(entry.name)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)) {
                                        Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                                        entry.subtitle?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}