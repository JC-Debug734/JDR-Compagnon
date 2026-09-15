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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.SrdLibraryPickerDialog
import com.jc2.jdrcompagnon.ui.components.SrdPickerEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Écran de gestion des sorts d'un personnage : affiche les sorts connus
 * (`Character.spells`, une liste de noms), permet au MJ d'en ajouter en
 * cherchant dans la bibliothèque SRD ([SrdRepository.searchSpells]) et de
 * les retirer. Seul le nom est stocké sur le personnage — pas les détails
 * (école, niveau, description), qui restent dans le SRD.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellManagementScreen(
    characterId: String,
    isMjMode: Boolean = false,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val allCharacters by GameState.characters.collectAsState()
    val character = allCharacters.find { it.id == characterId }
    var showPicker by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            if (isMjMode && character != null) {
                FloatingActionButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un sort")
                }
            }
        }
    ) { innerPadding ->
        val spellNames = character?.spells.orEmpty()

        if (character == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Personnage introuvable", style = MaterialTheme.typography.titleMedium)
            }
        } else if (spellNames.isEmpty()) {
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
                    if (isMjMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Utilisez le bouton + pour piocher dans la bibliothèque",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                items(spellNames, key = { it }) { spellName ->
                    SpellRow(
                        spellName = spellName,
                        isMjMode = isMjMode,
                        onDelete = { GameState.removeSpellFromCharacter(character.id, spellName) }
                    )
                }
            }
        }

        if (showPicker && character != null) {
            SrdLibraryPickerDialog(
                title = "Choisir un sort",
                onDismiss = { showPicker = false },
                onSelect = { spellName -> GameState.addSpellToCharacter(character.id, spellName) },
                search = { query ->
                    SrdRepository.searchSpells(context, query, character.worldId.ifBlank { "donjon_et_dragon" })
                        .map { SrdPickerEntry(name = it.name) }
                }
            )
        }
    }
}

/**
 * Ligne d'un sort connu, identifié uniquement par son nom (clé vers la
 * bibliothèque SRD). L'affichage d'un détail (niveau/école) pourra être
 * ajouté ici une fois les champs exacts de `SrdEntry`/`SpellParser`
 * disponibles — pour l'instant seul le nom est affiché, pour ne pas deviner
 * une structure de données non vérifiée.
 */
@Composable
private fun SpellRow(
    spellName: String,
    isMjMode: Boolean,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spellName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isMjMode) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}