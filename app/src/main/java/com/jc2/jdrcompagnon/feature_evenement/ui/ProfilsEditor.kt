package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.domain.model.ProfilEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val TypeProfil.icone get() = if (this == TypeProfil.PNJ) Icons.Default.Person else Icons.Default.Pets

/**
 * Profils impliqués dans un événement : monstres du bestiaire ou fiches PNJ du monde, avec une
 * quantité (ex. 4 bandits et leur chef). Ils sont affichés à la résolution, et les monstres
 * permettent de lancer le combat.
 */
@Composable
fun ProfilsEditor(
    profils: List<ProfilEvenement>,
    worldId: String,
    onProfilsChanged: (List<ProfilEvenement>) -> Unit,
) {
    var afficherSelecteur by remember { mutableStateOf(false) }

    fun remplacer(ancien: ProfilEvenement, nouveau: ProfilEvenement) =
        onProfilsChanged(profils.map { if (it == ancien) nouveau else it })

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Profils impliqués", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        if (profils.isEmpty()) {
            Text(
                "Aucun profil (monstre ou PNJ) lié à cet événement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        profils.forEach { profil ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(profil.type.icone, contentDescription = profil.type.label, modifier = Modifier.size(18.dp))
                Text(profil.nom, color = Color.White, modifier = Modifier.weight(1f).padding(start = 8.dp))
                IconButton(
                    onClick = { remplacer(profil, profil.copy(quantite = profil.quantite - 1)) },
                    enabled = profil.quantite > 1,
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Remove, contentDescription = "Un de moins", modifier = Modifier.size(18.dp)) }
                Text("×${profil.quantite}", color = Color.White)
                IconButton(
                    onClick = { remplacer(profil, profil.copy(quantite = (profil.quantite + 1).coerceAtMost(50))) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "Un de plus", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { onProfilsChanged(profils - profil) }) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer ${profil.nom}")
                }
            }
        }
        OutlinedButton(onClick = { afficherSelecteur = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Ajouter un profil", modifier = Modifier.padding(start = 6.dp))
        }
    }

    if (afficherSelecteur) {
        ProfilPickerDialog(
            worldId = worldId,
            onDismiss = { afficherSelecteur = false },
            onChoisir = { profil ->
                // Même profil déjà présent : on augmente sa quantité plutôt que de le doubler.
                val existant = profils.firstOrNull { it.type == profil.type && it.nom.equals(profil.nom, ignoreCase = true) }
                onProfilsChanged(
                    if (existant != null) profils.map { if (it == existant) it.copy(quantite = it.quantite + 1) else it }
                    else profils + profil
                )
                afficherSelecteur = false
            }
        )
    }
}

@Composable
private fun ProfilPickerDialog(
    worldId: String,
    onDismiss: () -> Unit,
    onChoisir: (ProfilEvenement) -> Unit,
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(TypeProfil.MONSTRE) }
    var recherche by remember { mutableStateOf("") }
    var noms by remember { mutableStateOf(listOf<String>()) }
    var chargement by remember { mutableStateOf(false) }

    LaunchedEffect(type, recherche, worldId) {
        chargement = true
        noms = withContext(Dispatchers.IO) {
            val tous = when (type) {
                TypeProfil.MONSTRE -> SrdRepository.loadMonsters(context, worldId).map { it.name }
                TypeProfil.PNJ -> GameState.characters.value
                    .filter { (it.type == "PNJ" || it.type == "Monstre") && (it.worldId == worldId || it.worldId.isBlank()) }
                    .map { it.name }
            }
            val q = recherche.trim()
            tous.filter { q.isBlank() || it.contains(q, ignoreCase = true) }.distinct().take(60)
        }
        chargement = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeProfil.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(if (t == TypeProfil.MONSTRE) "Bestiaire" else "PNJ") },
                            leadingIcon = { Icon(t.icone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (chargement) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (!chargement && noms.isEmpty()) {
                    Text("Aucun résultat.", style = MaterialTheme.typography.bodySmall)
                }
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(noms) { nom ->
                        Text(
                            nom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChoisir(ProfilEvenement(type = type, nom = nom)) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
