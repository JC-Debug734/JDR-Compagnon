package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel

// Classes de D&D
val dndClasses = listOf(
    "Guerrier", "Magicien", "Rôdeur", "Clerc", "Voleur", "Paladin",
    "Barde", "Druide", "Ensorceleur", "Moine", "Barbare", "Sorcier",
)

// Races de D&D
val dndRaces = listOf(
    "Humain", "Elfe", "Nain", "Halfelin", "Gnome", "Demi-elfe",
    "Demi-orc", "Tieffelin", "Dragonborn"
)

// Historiques de D&D
val dndBackgrounds = listOf(
    "Acolyte", "Charlatan", "Criminel", "Entertainer", "Herboriste",
    "Marin", "Militaire", "Noble", "Paysan", "Savant", "Voyageur"
)

// Alignements D&D
val dndAlignments = listOf(
    "Loyal Bon", "Neutre Bon", "Chaotique Bon",
    "Loyal Neutre", "Neutre", "Chaotique Neutre",
    "Loyal Mauvais", "Neutre Mauvais", "Chaotique Mauvais"
)

// Compétences de D&D
val skillList = listOf(
    "Acrobaties", "Arcanes", "Athlétisme", "Discrétion", "Dressage",
    "Escamotage", "Histoire", "Intimidation", "Investigation", "Médecine",
    "Nature", "Perception", "Persuasion", "Religion", "Représentation",
    "Survie", "Tromperie"
)

// Capacités de sauvegarde
val savingThrows = listOf(
    "Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme"
)

// Bonus de maîtrise selon le niveau (D&D 5e)
fun calculateProficiencyBonus(level: Int): Int {
    return when {
        level >= 17 -> 6
        level >= 13 -> 5
        level >= 9 -> 4
        level >= 5 -> 3
        else -> 2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    currentWorld: WorldState?,
    onCharacterCreated: () -> Unit,
    onBack: () -> Unit
) {
    // État du personnage
    var characterName by remember { mutableStateOf("") }
    var characterClass by remember { mutableStateOf("") }
    var characterRace by remember { mutableStateOf("") }
    var characterBackground by remember { mutableStateOf("") }
    var characterLevel by remember { mutableIntStateOf(1) }
    var characterAlignment by remember { mutableStateOf("") }

    // Stats principales
    var strength by remember { mutableIntStateOf(10) }
    var dexterity by remember { mutableIntStateOf(10) }
    var constitution by remember { mutableIntStateOf(10) }
    var intelligence by remember { mutableIntStateOf(10) }
    var wisdom by remember { mutableIntStateOf(10) }
    var charisma by remember { mutableIntStateOf(10) }

    // Points de vie
    var maxHp by remember { mutableIntStateOf(10) }
    var currentHp by remember { mutableIntStateOf(10) }
    var tempHp by remember { mutableIntStateOf(0) }

    // Armor Class & Speed
    var armorClass by remember { mutableIntStateOf(10) }
    var speed by remember { mutableIntStateOf(30) }
    var initiative by remember { mutableIntStateOf(10) }

    // Jets de sauvegarde - map skill -> level
    var selectedSavingThrows by remember { mutableStateOf(mapOf<String, ProficiencyLevel>()) }

    // Compétences - map skill -> level (NONE, PROFICIENT, EXPERTISE)
    var selectedSkills by remember { mutableStateOf(mapOf<String, ProficiencyLevel>()) }

    // Équipement
    var equipment by remember { mutableStateOf("") }

    // Traits et capacités
    var traits by remember { mutableStateOf("") }

    // États des dropdowns
    var classExpanded by remember { mutableStateOf(value = false) }
    var raceExpanded by remember { mutableStateOf(value = false) }
    var backgroundExpanded by remember { mutableStateOf(value = false) }
    var alignmentExpanded by remember { mutableStateOf(value = false) }

    // Mettre à jour le bonus de maîtrise quand le niveau change
    val currentProficiencyBonus = calculateProficiencyBonus(characterLevel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un personnage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === SECTION: Identité ===
            SectionTitle("Identité du personnage")

            OutlinedTextField(
                value = characterName,
                onValueChange = { characterName = it },
                label = { Text("Nom du personnage") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Classe
                ExposedDropdownMenuBox(
                    expanded = classExpanded,
                    onExpandedChange = { classExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = characterClass,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Classe") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = classExpanded,
                        onDismissRequest = { classExpanded = false }
                    ) {
                        dndClasses.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    characterClass = cls
                                    classExpanded = false
                                }
                            )
                        }
                    }
                }

                // Race
                ExposedDropdownMenuBox(
                    expanded = raceExpanded,
                    onExpandedChange = { raceExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = characterRace,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Race") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = raceExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = raceExpanded,
                        onDismissRequest = { raceExpanded = false }
                    ) {
                        dndRaces.forEach { race ->
                            DropdownMenuItem(
                                text = { Text(race) },
                                onClick = {
                                    characterRace = race
                                    raceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Niveau
                OutlinedTextField(
                    value = characterLevel.toString(),
                    onValueChange = { characterLevel = it.toIntOrNull()?.coerceIn(1, 20) ?: 1 },
                    label = { Text("Niveau") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Historique
                ExposedDropdownMenuBox(
                    expanded = backgroundExpanded,
                    onExpandedChange = { backgroundExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = characterBackground,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Historique") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = backgroundExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = backgroundExpanded,
                        onDismissRequest = { backgroundExpanded = false }
                    ) {
                        dndBackgrounds.forEach { bg ->
                            DropdownMenuItem(
                                text = { Text(bg) },
                                onClick = {
                                    characterBackground = bg
                                    backgroundExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Alignement (dropdown)
            ExposedDropdownMenuBox(
                expanded = alignmentExpanded,
                onExpandedChange = { alignmentExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = characterAlignment,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Alignement") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = alignmentExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = alignmentExpanded,
                    onDismissRequest = { alignmentExpanded = false }
                ) {
                    dndAlignments.forEach { alignment ->
                        DropdownMenuItem(
                            text = { Text(alignment) },
                            onClick = {
                                characterAlignment = alignment
                                alignmentExpanded = false
                            }
                        )
                    }
                }
            }

            // === SECTION: Statistiques ===
            SectionTitle("Statistiques principales")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatField("FOR", strength, { strength = it }, Modifier.weight(1f))
                StatField("DEX", dexterity, { dexterity = it }, Modifier.weight(1f))
                StatField("CON", constitution, { constitution = it }, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatField("INT", intelligence, { intelligence = it }, Modifier.weight(1f))
                StatField("SAG", wisdom, { wisdom = it }, Modifier.weight(1f))
                StatField("CHA", charisma, { charisma = it }, Modifier.weight(1f))
            }

            // === SECTION: Combat ===
            SectionTitle("Combat et survie")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatField("PV Max", maxHp, { maxHp = it }, Modifier.weight(1f))
                StatField("PV Actuels", currentHp, { currentHp = it }, Modifier.weight(1f))
                StatField("PV Temp", tempHp, { tempHp = it }, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatField("CA", armorClass, { armorClass = it }, Modifier.weight(1f))
                StatField("Vitesse", speed, { speed = it }, Modifier.weight(1f))
                StatField("Init", initiative, { initiative = it }, Modifier.weight(1f))
            }

            // Bonus de maîtrise (affichage seulement, non modifiable)
            OutlinedTextField(
                value = "+$currentProficiencyBonus",
                onValueChange = {},
                label = { Text("Bonus de maîtrise (auto)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true
            )

            // === SECTION: Maîtrises ===
            SectionTitle("Maîtrises")

            Text(
                text = "Jets de sauvegarde",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ProficiencyChips(
                options = savingThrows,
                selected = selectedSavingThrows,
                proficiencyBonus = currentProficiencyBonus,
            ) { selectedSavingThrows = it }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Compétences (tap: — → Maîtrise → Expertise → —)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ProficiencyChips(
                options = skillList,
                selected = selectedSkills,
                proficiencyBonus = currentProficiencyBonus,
            ) { selectedSkills = it }

            // === SECTION: Équipement ===
            SectionTitle("Équipement")

            OutlinedTextField(
                value = equipment,
                onValueChange = { equipment = it },
                label = { Text("Équipement (armes, armures, objets...)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // === SECTION: Traits et capacités ===
            SectionTitle("Traits et capacités")

            OutlinedTextField(
                value = traits,
                onValueChange = { traits = it },
                label = { Text("Traits de race, capacités de classe, talents...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 7
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton de création
            Button(
                onClick = {
                    val newCharacter = Character(
                        name = characterName,
                        type = "PJ",
                        worldId = currentWorld?.id ?: "",
                        characterClass = characterClass,
                        race = characterRace,
                        level = characterLevel,
                        alignment = characterAlignment,
                        background = characterBackground,
                        strength = strength,
                        dexterity = dexterity,
                        constitution = constitution,
                        intelligence = intelligence,
                        wisdom = wisdom,
                        charisma = charisma,
                        maxHp = maxHp,
                        currentHp = currentHp,
                        tempHp = tempHp,
                        armorClass = armorClass,
                        speed = speed,
                        initiative = initiative,
                        savingThrows = selectedSavingThrows,
                        skills = selectedSkills,
                        equipment = equipment,
                        traits = traits,
                        createdBy = "Joueur"
                    )
                    GameState.addCharacter(newCharacter)
                    onCharacterCreated()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Créer le personnage",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onValueChange(it.toIntOrNull() ?: 10) },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun ProficiencyChips(
    options: List<String>,
    selected: Map<String, ProficiencyLevel>,
    proficiencyBonus: Int,
    onToggle: (Map<String, ProficiencyLevel>) -> Unit
) {
    val rows = options.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { option ->
                    val currentLevel = selected[option] ?: ProficiencyLevel.NONE
                    val nextLevel = when (currentLevel) {
                        ProficiencyLevel.NONE -> ProficiencyLevel.PROFICIENT
                        ProficiencyLevel.PROFICIENT -> ProficiencyLevel.EXPERTISE
                        ProficiencyLevel.EXPERTISE -> ProficiencyLevel.NONE
                    }

                    val bonus = when (currentLevel) {
                        ProficiencyLevel.NONE -> 0
                        ProficiencyLevel.PROFICIENT -> proficiencyBonus
                        ProficiencyLevel.EXPERTISE -> proficiencyBonus * 2
                    }

                    Card(
                        onClick = {
                            val newMap = mutableMapOf<String, ProficiencyLevel>()
                            newMap.putAll(selected)
                            newMap[option] = nextLevel
                            if (nextLevel == ProficiencyLevel.NONE) {
                                newMap.remove(option)
                            }
                            onToggle(newMap)
                        },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = when (currentLevel) {
                                ProficiencyLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
                                ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primaryContainer
                                ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (currentLevel) {
                                    ProficiencyLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                                    ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.onPrimaryContainer
                                    ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            Text(
                                text = when (currentLevel) {
                                    ProficiencyLevel.NONE -> "—"
                                    ProficiencyLevel.PROFICIENT -> "+$bonus"
                                    ProficiencyLevel.EXPERTISE -> "+$bonus (E)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (currentLevel) {
                                    ProficiencyLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.onPrimaryContainer
                                    ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}