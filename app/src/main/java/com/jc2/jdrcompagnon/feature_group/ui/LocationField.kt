package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.flow.flowOf

/**
 * Lieux proposés à la saisie d'une localisation : lieux (villes comprises) de la campagne
 * [campaignId] — par défaut la campagne sélectionnée — et lieux déjà utilisés par les groupes et
 * les personnages, pour garder la même orthographe partout (« Phandalin »).
 */
@Composable
fun rememberLieuxConnus(campaignId: String? = GameState.currentCampaignId.collectAsState().value): List<String> {
    val points by remember(campaignId) {
        if (campaignId != null) CarteDependencies.repository.observerPoints(campaignId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val groups by GameState.mjGroups.collectAsState()
    val characters by GameState.characters.collectAsState()
    return remember(points, groups, characters) {
        (points.map { it.nom } +
            groups.flatMap { g -> listOf(g.location) + g.mounts.map { it.location } + g.transports.map { it.location } + g.inventory.map { it.location } + g.assets.map { it.location } } +
            characters.map { it.location })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
}

/** Champ de localisation : saisie libre, avec la liste des lieux connus en suggestion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Lieu",
    lieux: List<String> = rememberLieuxConnus(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = lieux.filter { value.isBlank() || it.contains(value.trim(), ignoreCase = true) }
        .filterNot { it.equals(value.trim(), ignoreCase = true) }
    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(label) },
            placeholder = { Text("ex. Phandalin") },
            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
            trailingIcon = { if (lieux.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            colors = colors,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.take(8).forEach { lieu ->
                DropdownMenuItem(
                    text = { Text(lieu) },
                    onClick = { onValueChange(lieu); expanded = false },
                )
            }
        }
    }
}
