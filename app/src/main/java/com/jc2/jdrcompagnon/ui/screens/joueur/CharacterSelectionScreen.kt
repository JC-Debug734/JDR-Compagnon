package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.CharacterProgression
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.JoueurDrawer
import com.jc2.jdrcompagnon.ui.NaheulbeukCharacter
import com.jc2.jdrcompagnon.ui.WorldState
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// Sélection de personnage : routeur (CharacterSelectionScreen)
// + liste avec recherche/filtres (CharacterListWithSearchScreen),
// fusionnés dans un seul fichier.
// Câblage inchangé côté NavGraph.kt : les deux fonctions publiques
// gardent leur nom.
// ─────────────────────────────────────────────────────────────

@Composable
fun CharacterSelectionScreen(
    currentWorld: WorldState?,
    onViewCharacter: (Character) -> Unit,
    onViewNaheulbeukCharacter: (NaheulbeukCharacter) -> Unit = {},
    onCreateQuick: () -> Unit = {},
    onCreateWizard: () -> Unit = {},
    onBack: () -> Unit,
    isMjMode: Boolean = false,
) {
    val allCharacters by GameState.characters.collectAsState()
    val characters = if (currentWorld != null) {
        allCharacters.filter { it.worldId == currentWorld.id }
    } else {
        allCharacters
    }

    if (currentWorld?.id == "naheulbeuk") {
        // TODO : écran de sélection dédié à Naheulbeuk — à implémenter plus tard
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sélection de personnage Naheulbeuk à venir")
        }
    } else {
        // Utilise le nouvel écran avec recherche et filtres
        CharacterListWithSearchScreen(
            characters = characters,
            onCharacterSelected = onViewCharacter,
            onCreateQuick = if (isMjMode) onCreateQuick else { {} },
            onCreateWizard = if (isMjMode) onCreateWizard else { {} },
            onBack = onBack,
            showCreateButton = isMjMode
        )
    }
}

/**
 * Types de personnages disponibles pour le filtrage
 */
val characterTypes = listOf("Tous", "PJ", "PNJ", "Monstre", "Boss", "Créature", "Familier")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CharacterListWithSearchScreen(
    characters: List<Character>,
    onCharacterSelected: (Character) -> Unit,
    onCreateQuick: () -> Unit,
    onCreateWizard: () -> Unit,
    onBack: () -> Unit,
    showCreateButton: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("Tous") }
    var showFilters by remember { mutableStateOf(false) }
    var showChoixCreation by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val filteredCharacters = remember(searchQuery, selectedTypeFilter, characters) {
        characters.filter { character ->
            val matchesSearch = searchQuery.isBlank() ||
                    character.name.contains(searchQuery, ignoreCase = true) ||
                    character.characterClass.contains(searchQuery, ignoreCase = true) ||
                    character.race.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedTypeFilter == "Tous" || character.type == selectedTypeFilter
            matchesSearch && matchesType
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            JoueurDrawer(
                onChooseCharacter = {},
                onClose = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Personnages",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtres",
                                tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (showCreateButton) {
                    ExtendedFloatingActionButton(
                        onClick = { showChoixCreation = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Nouveau") }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Barre de recherche
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filtres de type
                AnimatedVisibility(
                    visible = showFilters,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = fadeOut()
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        characterTypes.forEach { type ->
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = { selectedTypeFilter = type },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Compteur de résultats
                Text(
                    text = "${filteredCharacters.size} personnage${if (filteredCharacters.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Liste des personnages
                if (filteredCharacters.isEmpty()) {
                    EmptyCharacterList(
                        hasSearch = searchQuery.isNotBlank() || selectedTypeFilter != "Tous",
                        onClearFilters = {
                            searchQuery = ""
                            selectedTypeFilter = "Tous"
                        }
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredCharacters, key = { it.id }) { character ->
                            EnhancedCharacterListItem(
                                character = character,
                                onClick = { onCharacterSelected(character) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChoixCreation) {
        AlertDialog(
            onDismissRequest = { showChoixCreation = false },
            title = { Text("Créer un personnage") },
            text = { Text("Comment voulez-vous créer ce personnage ?") },
            confirmButton = {
                TextButton(onClick = { showChoixCreation = false; onCreateWizard() }) {
                    Text("Pas à pas (recommandé)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChoixCreation = false; onCreateQuick() }) {
                    Text("Rapide")
                }
            }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Rechercher par nom, classe, race...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Effacer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { /* dismiss keyboard */ })
    )
}

@Composable
fun EnhancedCharacterListItem(
    character: Character,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColors = mapOf(
        "PJ" to MaterialTheme.colorScheme.primary,
        "PNJ" to MaterialTheme.colorScheme.secondary,
        "Monstre" to MaterialTheme.colorScheme.error,
        "Boss" to Color(0xFF9C27B0),
        "Créature" to MaterialTheme.colorScheme.tertiary,
        "Familier" to Color(0xFF00BCD4)
    )

    val typeColor = typeColors[character.type] ?: MaterialTheme.colorScheme.primary
    val hpPercent = character.currentHitPoints.toFloat() / character.maxHitPoints.coerceAtLeast(1)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar avec initiales
                Surface(
                    shape = CircleShape,
                    color = typeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = character.characterClass.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = typeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${character.characterClass} ${character.level} • ${character.race}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge de type
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = character.type,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barre de PV
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PV",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                LinearProgressIndicator(
                    progress = { hpPercent },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = when {
                        hpPercent > 0.5f -> MaterialTheme.colorScheme.primary
                        hpPercent > 0.25f -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${character.currentHitPoints}/${character.maxHitPoints}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barre d'XP
            val xpProgress = CharacterProgression.progressToNextLevel(character.level, character.experience)
            val nextXpThreshold = CharacterProgression.xpForLevel((character.level + 1).coerceAtMost(20))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (character.level >= 20) "MAX" else "${character.experience}/$nextXpThreshold",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Stats rapides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat("CA", GameState.armorClassBreakdown(character).total.toString())
                QuickStat("INIT", "${GameState.abilityModifier(character.dexterity)}")
                QuickStat("VIT", "${character.speed}m")
                if (character.proficiencyBonus > 0) {
                    QuickStat("BM", "+${character.proficiencyBonus}")
                }
            }
        }
    }
}

@Composable
fun QuickStat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyCharacterList(
    hasSearch: Boolean = false,
    onClearFilters: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearch) "Aucun résultat" else "Aucun personnage",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasSearch) "Essayez d'autres critères de recherche" else "Créez votre premier héros !",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        if (hasSearch) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClearFilters) {
                Text("Effacer les filtres")
            }
        }
    }
}