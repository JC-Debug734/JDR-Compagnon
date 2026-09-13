package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

/**
 * Un sort connu/préparé par un personnage.
 *
 * TODO : ceci est un modèle de données PLACEHOLDER en mémoire locale.
 * Il faudra le remplacer par les vrais sorts du personnage une fois le
 * modèle GameState/Character connu (probablement un champ `spells` sur
 * Character, persisté comme le reste de la fiche).
 */
data class SpellEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val level: Int, // 0 = sort mineur
    val prepared: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellManagementScreen(
    characterId: String,
    onBack: () -> Unit,
) {
    // TODO : remplacer par la lecture/écriture réelle depuis GameState une
    // fois le champ des sorts du personnage disponible.
    var spells by remember { mutableStateOf(listOf<SpellEntry>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion des sorts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (spells.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aucun sort enregistré", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(spells, key = { it.id }) { spell ->
                    SpellRow(
                        spell = spell,
                        onTogglePrepared = {
                            spells = spells.map { if (it.id == spell.id) it.copy(prepared = !it.prepared) else it }
                        },
                        onDelete = { spells = spells.filterNot { it.id == spell.id } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellRow(spell: SpellEntry, onTogglePrepared: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spell.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (spell.level == 0) "Sort mineur" else "Niveau ${spell.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onTogglePrepared) {
                Icon(
                    if (spell.prepared) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (spell.prepared) "Préparé" else "Non préparé",
                    tint = if (spell.prepared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}