package com.jc2.jdrcompagnon.ui.screens.mj.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.NaheulbeukCharacter
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.WorldState
import com.mikepenz.markdown.m3.Markdown

/**
 * Écran de détail d'un équipement.
 * Affiche les champs structurés mis en avant (Dégâts/Poids/Propriétés/Prix, etc.)
 * puis le markdown brut complet en dessous, repliable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EquipmentDetailScreen(
    equipmentName: String?,
    isMj: Boolean = false,
    currentWorld: WorldState? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var item by remember { mutableStateOf<EquipmentItem?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }

    LaunchedEffect(equipmentName, currentWorld?.id) {
        if (equipmentName == null) {
            item = null
            return@LaunchedEffect
        }
        item = SrdRepository.loadEquipmentList(context, currentWorld?.id).find {
            it.name.equals(equipmentName, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item?.name ?: "Équipement",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    if (isMj) {
                        TextButton(onClick = { showAssignDialog = true }) {
                            Text("Attribuer")
                        }
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (item == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val current = item!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Catégorie
                    Text(
                        text = current.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Grille de champs structurés
                    EquipmentStatsGrid(item = current, currentWorld = currentWorld)

                    HorizontalDivider()

                    // Description markdown complète (toujours visible, pas de repli)
                    Text(
                        text = "Description complète",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (current.rawMarkdown.isNotBlank()) {
                        Markdown(content = current.rawMarkdown)
                    } else {
                        Text(
                            text = "Aucun détail disponible pour cet équipement.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showAssignDialog && item != null) {
                AssignEquipmentDialog(
                    item = item!!,
                    currentWorld = currentWorld,
                    onDismiss = { showAssignDialog = false },
                    onAssign = { characterId: String, isNaheulbeuk: Boolean ->
                        if (isNaheulbeuk) {
                            GameState.addItemToNaheulbeukBackpack(characterId, item!!.name)
                        } else {
                            GameState.addItemToBackpack(characterId, item!!.name)
                        }
                        showAssignDialog = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignEquipmentDialog(
    item: EquipmentItem,
    currentWorld: WorldState?,
    onDismiss: () -> Unit,
    onAssign: (String, Boolean) -> Unit
) {
    val allCharacters by GameState.characters.collectAsState()
    val allNaheulbeuk by GameState.naheulbeukCharacters.collectAsState()
    val isNaheulbeukWorld = currentWorld?.id == "naheulbeuk"

    val characters: List<Character> = remember(allCharacters, currentWorld) {
        if (currentWorld != null && !isNaheulbeukWorld) {
            allCharacters.filter { it.worldId == currentWorld.id }
        } else allCharacters
    }
    val naheulbeukCharacters: List<NaheulbeukCharacter> = remember(allNaheulbeuk, currentWorld) {
        if (currentWorld != null && isNaheulbeukWorld) {
            allNaheulbeuk.filter { it.worldId == currentWorld.id }
        } else allNaheulbeuk
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attribuer ${item.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isNaheulbeukWorld) {
                    if (naheulbeukCharacters.isEmpty()) {
                        Text("Aucun personnage dans ce monde.")
                    } else {
                        naheulbeukCharacters.forEach { character ->
                            TextButton(
                                onClick = { onAssign(character.id, true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${character.name} (Naheulbeuk)")
                            }
                        }
                    }
                } else {
                    if (characters.isEmpty()) {
                        Text("Aucun personnage dans ce monde.")
                    } else {
                        characters.forEach { character ->
                            TextButton(
                                onClick = { onAssign(character.id, false) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${character.name} (${character.type})")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentStatsGrid(
    item: EquipmentItem,
    currentWorld: WorldState? = null
) {
    val isWeapon = item.category.lowercase().contains("arme")
    val isArmor = item.category.lowercase().contains("armure")
    val isMagicItem = item.category.equals("Objets magiques", ignoreCase = true) ||
            item.category.equals("Artefacts", ignoreCase = true)

    val stats = buildList {
        if (isMagicItem) {
            if (item.properties.isNotBlank()) add("Type" to item.properties)
        }
        if (isWeapon) {
            if (item.damage.isNotBlank()) add("Dégâts" to item.damage)
            if (item.properties.isNotBlank()) add("Propriétés" to item.properties)
        }
        if (isArmor) {
            if (item.ac.isNotBlank()) add("CA" to item.ac)
            if (item.strength.isNotBlank()) add("Force" to item.strength)
            if (item.stealth.isNotBlank()) add("Discrétion" to item.stealth)
        }
        if (item.weight.isNotBlank() && item.weight != "-") {
            val isNaheulbeuk = currentWorld?.id == "naheulbeuk"
            val weightDisplay = if (isNaheulbeuk) {
                // Déjà en kg pour Naheulbeuk
                item.weight.replace(" kg", "").trim()
                    .toDoubleOrNull()?.let {
                        if (it < 1) "${(it * 1000).toInt()} g" else "${"%.1f".format(it)} kg"
                    } ?: item.weight
            } else {
                // Conversion lb → kg pour D&D
                val weightValue = item.weight.replace(" lb", "").toDoubleOrNull()
                if (weightValue != null) {
                    val kg = weightValue * 0.453592
                    if (kg < 1) "${(kg * 1000).toInt()} g" else "${"%.1f".format(kg)} kg"
                } else {
                    item.weight
                }
            }
            add("Poids" to weightDisplay)
        }
        if (item.cost.isNotBlank()) {
            val isNaheulbeuk = currentWorld?.id == "naheulbeuk"
            val costDisplay = if (isNaheulbeuk) {
                val costValue = item.cost.replace(" gp", "").replace(",", ".").toDoubleOrNull()
                if (costValue != null) {
                    val gold = costValue.toInt()
                    "$gold gold"
                } else {
                    item.cost
                }
            } else {
                // D&D: garder gp
                item.cost
            }
            add("Prix" to costDisplay)
        }
    }

    if (stats.isEmpty()) return

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2
    ) {
        stats.forEach { (label, value) ->
            val isPrice = label == "Prix"
            ElevatedCard(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isPrice) Color(0xFFFFD700).copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPrice) Color(0xFFFFD700)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPrice) Color(0xFFFFD700)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (isPrice) TextAlign.End else TextAlign.Start,
                        modifier = if (isPrice) Modifier.fillMaxWidth()
                        else Modifier
                    )
                }
            }
        }
    }
}