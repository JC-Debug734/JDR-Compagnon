package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jc2.jdrcompagnon.di.VillesListViewModelFactory
import com.jc2.jdrcompagnon.feature_carte.presentation.VillesListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillesListScreen(
    campagneId: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    onOpenVille: (String) -> Unit,
    viewModel: VillesListViewModel = viewModel(factory = VillesListViewModelFactory(campagneId)),
) {
    val villes by viewModel.villes.collectAsStateWithLifecycle()
    var afficherCreation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("VILLES") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { afficherCreation = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouvelle ville") }
            )
        }
    ) { padding ->
        if (villes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Text(
                    "Aucune ville pour l'instant. Créez-en une avec le bouton +, ou depuis la carte de la campagne.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(villes, key = { it.id }) { ville ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenVille(ville.id) }
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(ville.nom, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${ville.boutiqueIds.size} boutiques · ${ville.scenarioIds.size} scénarios",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (afficherCreation) {
        var nom by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { afficherCreation = false },
            title = { Text("Nouvelle ville") },
            text = {
                Column {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onCreerVille(nom, description); afficherCreation = false },
                    enabled = nom.isNotBlank()
                ) { Text("Créer") }
            },
            dismissButton = { TextButton(onClick = { afficherCreation = false }) { Text("Annuler") } }
        )
    }
}
