package com.jc2.jdrcompagnon.ui.screens.mj

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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
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
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.screens.joueur.dndBackgrounds
import com.jc2.jdrcompagnon.ui.screens.joueur.dndClasses
import com.jc2.jdrcompagnon.ui.screens.joueur.skillList
import com.jc2.jdrcompagnon.ui.screens.joueur.savingThrows
import com.jc2.jdrcompagnon.ui.screens.joueur.dndAlignments
import com.jc2.jdrcompagnon.ui.screens.joueur.calculateProficiencyBonus

// Types de personnages pour le MJ
val characterTypes = listOf(
    "Personnage Joueur (PJ)",
    "Personnage Non-Joueur (PNJ)",
    "Monstre/Créature",
    "Boss/Antagoniste",
)

// Types de PNJ
val npcTypes = listOf(
    "Marchand", "Gardien", "Serviteur", "Noble", "Prêtre",
    "Artisan", "Voyageur", "Bandit", "Guide", "Aubergiste",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MjCharacterCreationScreen(
    currentWorld: WorldState?,
    onCharacterCreated: () -> Unit,
    onBack: () -> Unit
) {
    // Type de personnage (PJ/PNJ/Monstre)
    var characterType by remember { mutableStateOf("Personnage Joueur (PJ)") }
    var npcType by remember { mutableStateOf("") }
    var isPnj by remember { mutableStateOf(value = false) }
    var isMonster by remember { mutableStateOf(value = false) }
    
    // ... rest of the states ...
    
    // Dans le bouton de création :
    // worldId = currentWorld?.id ?: "",


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

    // Challenge Rating (pour les monstres)
    var challengeRating by remember { mutableStateOf("") }

    // Jets de sauvegarde
    var selectedSavingThrows by remember { mutableStateOf(mapOf<String, ProficiencyLevel>()) }

    // Compétences
    var selectedSkills by remember { mutableStateOf(mapOf<String, ProficiencyLevel>()) }

    // Actions/Attaques
    var actions by remember { mutableStateOf("") }

    // Équipement
    var equipment by remember { mutableStateOf("") }

    // Traits et capacités
    var traits by remember { mutableStateOf("") }

    // Notes du MJ
    var dmNotes by remember { mutableStateOf("") }

    // États des dropdowns
    var typeExpanded by remember { mutableStateOf(value = false) }
    var npcTypeExpanded by remember { mutableStateOf(false) }
    var classExpanded by remember { mutableStateOf(false) }
    var raceExpanded by remember { mutableStateOf(false) }
    var backgroundExpanded by remember { mutableStateOf(false) }
    var alignmentExpanded by remember { mutableStateOf(false) }

    // Bonus de maîtrise calculé
    val currentProficiencyBonus = calculateProficiencyBonus(characterLevel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un personnage (MJ)") },
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
            // === SECTION: Type de personnage ===
            SectionTitle("Type de personnage")

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = characterType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    characterTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                characterType = type
                                isPnj = type.contains("PNJ")
                                isMonster = type.contains("Monstre") || type.contains("Boss")
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Type de PNJ (si PNJ sélectionné)
            if (isPnj) {
                ExposedDropdownMenuBox(
                    expanded = npcTypeExpanded,
                    onExpandedChange = { npcTypeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = npcType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de PNJ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = npcTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = npcTypeExpanded,
                        onDismissRequest = { npcTypeExpanded = false }
                    ) {
                        npcTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    npcType = type
                                    npcTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

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
                // Classe (non affiché pour les monstres)
                if (!isMonster) {
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

                // Challenge Rating (pour les monstres)
                if (isMonster) {
                    OutlinedTextField(
                        value = challengeRating,
                        onValueChange = { challengeRating = it },
                        label = { Text("CR (ex: 5, 1/2)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                } else {
                    // Historique (pour les PJ)
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
            }

            // Alignement (dropdown)
            if (!isMonster) {
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

            // Bonus de maîtrise (affichage seulement)
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

            // === SECTION: Actions (pour monstres) ===
            if (isMonster) {
                SectionTitle("Actions et attaques")

                OutlinedTextField(
                    value = actions,
                    onValueChange = { actions = it },
                    label = { Text("Actions (attaques, sorts...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }

            // === SECTION: Équipement ===
            SectionTitle("Équipement")

            OutlinedTextField(
                value = equipment,
                onValueChange = { equipment = it },
                label = { Text("Équipement (armes, armures, objets...)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            // === SECTION: Traits et capacités ===
            SectionTitle("Traits et capacités")

            OutlinedTextField(
                value = traits,
                onValueChange = { traits = it },
                label = { Text("Traits de race, capacités de classe, compétences spéciales...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // === SECTION: Notes du MJ ===
            SectionTitle("Notes du Maître du Jeu")

            OutlinedTextField(
                value = dmNotes,
                onValueChange = { dmNotes = it },
                label = { Text("Notes privées (background, motivations, secrets...)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton génération aléatoire
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    generateRandomCharacter(
                        isMonster = isMonster,
                        onNameGenerated = { characterName = it },
                        onClassGenerated = { characterClass = it },
                        onRaceGenerated = { characterRace = it },
                        onAlignmentGenerated = { characterAlignment = it },
                        onBackgroundGenerated = { characterBackground = it },
                    ) { str, dex, con, intel, wis, cha ->
                        strength = str; dexterity = dex; constitution = con
                        intelligence = intel; wisdom = wis; charisma = cha
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    Icons.Filled.Casino,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Générer aléatoirement",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton de création
            Button(
                onClick = {
                    // Sauvegarder le personnage
                    val character = Character(
                        name = characterName,
                        type = when {
                            isMonster -> if (characterType.contains("Boss")) "Boss" else "Monstre"
                            isPnj -> "PNJ"
                            else -> "PJ"
                        },
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
                        armorClass = armorClass,
                        speed = speed,
                        initiative = initiative,
                        savingThrows = selectedSavingThrows,
                        skills = selectedSkills,
                        equipment = equipment,
                        traits = traits,
                        dmNotes = dmNotes,
                        createdBy = "MJ"
                    )
                    GameState.addCharacter(character)
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

/**
 * Génère un personnage aléatoire (PJ/PNJ - style D&D)
 */
private fun generateRandomCharacter(
    isMonster: Boolean,
    onNameGenerated: (String) -> Unit,
    onClassGenerated: (String) -> Unit,
    onRaceGenerated: (String) -> Unit,
    onAlignmentGenerated: (String) -> Unit,
    onBackgroundGenerated: (String) -> Unit,
    onStatsGenerated: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    // Noms aléatoires
    val firstNames = listOf("Thorin", "Elara", "Gandalf", "Aragorn", "Legolas", "Gimli", "Frodon", "Sam", "Gollum", "Sarouman")
    val lastNames = listOf("Barbar", "Fleurdelis", "Noirbois", "Lumiere", "Sombre", "Aiguetranchant", "Rodependu", "Sauvage", "Sage", "Furtif")
    
    val name = if (isMonster) "Monstre ${firstNames.random()}" else "${firstNames.random()} ${lastNames.random()}"
    
    // Génération aléatoire des stats (3d6 en ordre ou point buy simplifié)
    val stats = (1..6).map { (1..3).random() + (1..3).random() + (1..3).random() }
    onStatsGenerated(stats[0], stats[1], stats[2], stats[3], stats[4], stats[5])
    
    onNameGenerated(name)
    onClassGenerated(dndClasses.random())
    onRaceGenerated(dndRaces.random())
    onAlignmentGenerated(dndAlignments.random())
    onBackgroundGenerated(dndBackgrounds.random())
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

                    androidx.compose.material3.Card(
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
                        colors = androidx.compose.material3.CardDefaults.cardColors(
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

// Import tambahan pour les races
val dndRaces = listOf(
    "Humain", "Elfe", "Nain", "Halfelin", "Gnome", "Demi-elfe",
    "Demi-orc", "Tieffelin", "Dragonborn", "Gobelin", "Orc", "Hobgoblin"
)