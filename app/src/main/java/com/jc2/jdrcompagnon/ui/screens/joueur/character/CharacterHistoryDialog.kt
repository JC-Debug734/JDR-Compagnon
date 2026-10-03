package com.jc2.jdrcompagnon.ui.screens.joueur.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.HistoriqueDependencies
import com.jc2.jdrcompagnon.feature_historique.data.local.HistoriqueEntreeEntity
import com.jc2.jdrcompagnon.feature_historique.domain.TypeEvenementHistorique
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun iconePour(type: String): ImageVector = when (type) {
    TypeEvenementHistorique.CREATION.name -> Icons.Default.Star
    TypeEvenementHistorique.NIVEAU.name -> Icons.Default.TrendingUp
    TypeEvenementHistorique.CARACTERISTIQUE.name -> Icons.Default.AutoAwesome
    TypeEvenementHistorique.EQUIPEMENT.name -> Icons.Default.Backpack
    TypeEvenementHistorique.SORT.name -> Icons.AutoMirrored.Filled.MenuBook
    TypeEvenementHistorique.OR.name -> Icons.Default.AttachMoney
    TypeEvenementHistorique.POINTS_DE_VIE.name -> Icons.Default.Favorite
    TypeEvenementHistorique.ETAT.name -> Icons.Default.HealthAndSafety
    else -> Icons.Default.History
}

private val formatteurDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

/**
 * Journal du personnage : liste (plus récent en premier) de chaque modification notable
 * enregistrée depuis la création (niveau, caractéristiques, équipement, sorts, or, état...).
 */
@Composable
fun CharacterHistoryDialog(characterName: String, characterId: String, onDismiss: () -> Unit) {
    val entrees by remember(characterId) {
        HistoriqueDependencies.repository.observerHistorique(characterId)
    }.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historique — $characterName") },
        text = {
            if (entrees.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun événement enregistré pour le moment.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entrees, key = { it.id }) { entree -> HistoriqueLigne(entree) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun HistoriqueLigne(entree: HistoriqueEntreeEntity) {
    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = iconePour(entree.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(entree.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatteurDate.format(Date(entree.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
