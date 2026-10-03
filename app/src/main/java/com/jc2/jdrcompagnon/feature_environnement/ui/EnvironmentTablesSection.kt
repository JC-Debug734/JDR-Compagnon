package com.jc2.jdrcompagnon.feature_environnement.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.TableAleatoireDependencies
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_evenement.ui.EventResultDialog
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_table_aleatoire.ui.LootResultDialog
import com.jc2.jdrcompagnon.ui.LectureScenarioState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Tables aléatoires (feature_table_aleatoire) liées à un environnement : remplace la saisie des
 * rencontres une par une. Le MJ lie des tables existantes du monde (événements ou loot), les
 * tire directement depuis le lieu — même tirage/acquittement que TableAleatoireDetailScreen et
 * ScenarioReaderContent via TableAleatoireDependencies.tirage — et peut convertir les anciennes
 * rencontres texte de l'environnement en une vraie table.
 */
@Composable
internal fun TablesAleatoiresSection(
    environnement: Environnement,
    worldId: String?,
    onEnvironnementChanged: (Environnement) -> Unit,
    onOpenTable: (String) -> Unit,
) {
    val monde = worldId ?: environnement.worldId
    val repository = TableAleatoireDependencies.repository
    val tablesFlow = remember(monde) { if (monde.isBlank()) flowOf(emptyList()) else repository.observerTables(monde) }
    val tablesDuMonde by tablesFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Une table supprimée depuis l'outil dédié disparaît simplement de la liste (id orphelin ignoré).
    val tablesLiees = environnement.tablesAleatoiresIds.mapNotNull { id -> tablesDuMonde.firstOrNull { it.id == id } }
    val tablesDisponibles = tablesDuMonde.filter { it.id !in environnement.tablesAleatoiresIds }

    var afficherLiaison by remember { mutableStateOf(false) }
    var resultatEvenement by remember { mutableStateOf<Pair<Boolean, Evenement?>>(false to null) }
    var resultatLoot by remember { mutableStateOf<Pair<String, List<ResultatLoot>>?>(null) }

    fun tirerEvenement(table: TableAleatoire, typeFiltre: TypeEvenement?) {
        val (tableMiseAJour, resultat) = TableAleatoireDependencies.tirage.tirerEvenement(
            table, typeFiltre, LectureScenarioState.minutesEcouleesMaintenant()
        )
        if (resultat != null) scope.launch { repository.sauvegarder(tableMiseAJour) }
        resultatEvenement = true to resultat
    }

    fun tirerLoot(table: TableAleatoire) {
        val (tableMiseAJour, resultats) = TableAleatoireDependencies.tirage.tirerLoot(
            table, 3, LectureScenarioState.minutesEcouleesMaintenant()
        )
        if (resultats.isNotEmpty()) scope.launch { repository.sauvegarder(tableMiseAJour) }
        resultatLoot = "Loot — ${table.nom}" to resultats
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tablesLiees.isEmpty()) {
            Text(
                "Aucune table liée. Liez une table d'événements ou de loot du monde pour la tirer depuis ce lieu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        tablesLiees.forEach { table ->
            TableLieeRow(
                table = table,
                onOuvrir = { onOpenTable(table.id) },
                onTirerEvenement = { typeFiltre -> tirerEvenement(table, typeFiltre) },
                onTirerLoot = { tirerLoot(table) },
                onDelier = {
                    onEnvironnementChanged(
                        environnement.copy(tablesAleatoiresIds = environnement.tablesAleatoiresIds - table.id)
                    )
                }
            )
        }
        OutlinedButton(onClick = { afficherLiaison = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Lier une table")
        }
    }

    if (afficherLiaison) {
        AlertDialog(
            onDismissRequest = { afficherLiaison = false },
            title = { Text("Lier une table aléatoire") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (tablesDisponibles.isEmpty()) {
                        Text(
                            "Aucune autre table dans ce monde. Créez-en depuis l'outil Tables aléatoires.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TypeTable.entries.forEach { type ->
                        val tablesDuType = tablesDisponibles.filter { it.type == type }
                        if (tablesDuType.isNotEmpty()) {
                            Text(
                                type.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            tablesDuType.forEach { table ->
                                Text(
                                    "${table.nom} (${table.nombreEntrees} entrées)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onEnvironnementChanged(
                                                environnement.copy(tablesAleatoiresIds = environnement.tablesAleatoiresIds + table.id)
                                            )
                                            afficherLiaison = false
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { afficherLiaison = false }) { Text("Fermer") } }
        )
    }

    if (resultatEvenement.first) {
        EventResultDialog(
            evenement = resultatEvenement.second,
            worldId = monde,
            onDismiss = { resultatEvenement = false to null }
        )
    }

    resultatLoot?.let { (titre, resultats) ->
        LootResultDialog(titre = titre, resultats = resultats, worldId = monde, onDismiss = { resultatLoot = null })
    }
}

@Composable
private fun TableLieeRow(
    table: TableAleatoire,
    onOuvrir: () -> Unit,
    onTirerEvenement: (TypeEvenement?) -> Unit,
    onTirerLoot: () -> Unit,
    onDelier: () -> Unit,
) {
    var menuTirage by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOuvrir)) {
            Text(table.nom, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                "${table.type.label} · ${table.nombreEntrees} entrées" + if (!table.active) " · inactive" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = {
                when (table.type) {
                    TypeTable.LOOT -> onTirerLoot()
                    TypeTable.EVENEMENTS -> menuTirage = true
                }
            }) {
                Icon(Icons.Default.Casino, contentDescription = "Tirer")
            }
            // Tirage d'événement : toute la table, ou une seule famille (rencontre, découverte, péril...).
            DropdownMenu(expanded = menuTirage, onDismissRequest = { menuTirage = false }) {
                DropdownMenuItem(
                    text = { Text("Tirer (toute la table)") },
                    onClick = { menuTirage = false; onTirerEvenement(null) }
                )
                TypeEvenement.entries
                    .filter { type -> table.entreesEvenements.any { it.evenement?.type == type } }
                    .forEach { type ->
                        DropdownMenuItem(
                            text = { Text("Tirer : ${type.label}") },
                            onClick = { menuTirage = false; onTirerEvenement(type) }
                        )
                    }
            }
        }
        IconButton(onClick = onDelier) {
            Icon(Icons.Default.LinkOff, contentDescription = "Délier")
        }
    }
}
