package com.jc2.jdrcompagnon.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.calculateProficiencyBonus
import com.jc2.jdrcompagnon.ui.screens.joueur.DndConstants
import com.jc2.jdrcompagnon.ui.screens.joueur.dndBackgrounds
import com.jc2.jdrcompagnon.ui.screens.joueur.skillList

// Standard Array D&D 5e
private val STANDARD_ARRAY = listOf(15, 14, 13, 12, 10, 8)
private val ABILITY_KEYS = listOf("strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma")
private val ABILITY_SHORT_NAMES = listOf("FOR", "DEX", "CON", "INT", "SAG", "CHA")
private val ABILITY_NAMES = listOf("Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme")
private val ALL_DND_SKILLS = skillList
private val DND_BACKGROUNDS = dndBackgrounds
private enum class StatMethod(val label: String) {
    STANDARD_ARRAY("Standard Array"),
    ROLL_4D6("4d6"),
    POINT_BUY("Point Buy")
}

// Point Buy D&D 5e
private val POINT_BUY_COSTS = mapOf(
    8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9
)
private const val POINT_BUY_BUDGET = 27
private const val POINT_BUY_MIN = 8
private const val POINT_BUY_MAX = 15

private fun hitDieMax(hitDice: String?): Int {
    return hitDice?.removePrefix("d")?.toIntOrNull() ?: 8
}

private fun statModifier(value: Int): Int = (value - 10) / 2

private fun roll4d6KeepBest3(): Int {
    val rolls = List(4) { (1..6).random() }
    return rolls.sorted().drop(1).sum()
}

data class DndClassInfo(
    val displayName: String,
    val hitDice: String,
    val savingThrows: List<String>,
    val skillsPool: List<String>,
    val skillsCount: Int
) {
    companion object {
        private val byName = mapOf(
            "Guerrier" to DndClassInfo("Guerrier", "d10", listOf("Force", "Constitution"), listOf("Athlétisme", "Intimidation", "Survie", "Dressage", "Perception", "Histoire"), 2),
            "Magicien" to DndClassInfo("Magicien", "d6", listOf("Intelligence", "Sagesse"), listOf("Arcanes", "Histoire", "Investigation", "Médecine", "Religion", "Perspicacité"), 2),
            "Rôdeur" to DndClassInfo("Rôdeur", "d10", listOf("Force", "Dextérité"), listOf("Athlétisme", "Discrétion", "Dressage", "Investigation", "Nature", "Perception", "Survie"), 3),
            "Clerc" to DndClassInfo("Clerc", "d8", listOf("Sagesse", "Charisme"), listOf("Histoire", "Perspicacité", "Médecine", "Persuasion", "Religion"), 2),
            "Voleur" to DndClassInfo("Voleur", "d8", listOf("Dextérité", "Intelligence"), listOf("Acrobaties", "Athlétisme", "Discrétion", "Escamotage", "Investigation", "Perception", "Persuasion", "Tromperie"), 4),
            "Paladin" to DndClassInfo("Paladin", "d10", listOf("Sagesse", "Charisme"), listOf("Athlétisme", "Intimidation", "Médecine", "Persuasion", "Religion"), 2),
            "Barde" to DndClassInfo("Barde", "d8", listOf("Dextérité", "Charisme"), skillList, 3),
            "Druide" to DndClassInfo("Druide", "d8", listOf("Intelligence", "Sagesse"), listOf("Arcanes", "Dressage", "Médecine", "Nature", "Perception", "Religion", "Survie"), 2),
            "Ensorceleur" to DndClassInfo("Ensorceleur", "d6", listOf("Constitution", "Charisme"), listOf("Arcanes", "Intimidation", "Persuasion", "Religion", "Tromperie"), 2),
            "Moine" to DndClassInfo("Moine", "d8", listOf("Force", "Dextérité"), listOf("Acrobaties", "Athlétisme", "Discrétion", "Histoire", "Investigation", "Nature", "Religion"), 2),
            "Barbare" to DndClassInfo("Barbare", "d12", listOf("Force", "Constitution"), listOf("Athlétisme", "Dressage", "Intimidation", "Nature", "Perception", "Survie"), 2),
            "Sorcier" to DndClassInfo("Sorcier", "d6", listOf("Sagesse", "Charisme"), listOf("Arcanes", "Histoire", "Investigation", "Nature", "Religion", "Tromperie"), 2)
        )
        fun entries(): List<DndClassInfo> = byName.values.toList()
        fun find(name: String): DndClassInfo? = byName[name]
    }
}

data class DndRaceInfo(
    val displayName: String,
    val speed: Int
) {
    companion object {
        private val byName = mapOf(
            "Humain" to DndRaceInfo("Humain", 30),
            "Elfe" to DndRaceInfo("Elfe", 30),
            "Nain" to DndRaceInfo("Nain", 25),
            "Halfelin" to DndRaceInfo("Halfelin", 25),
            "Gnome" to DndRaceInfo("Gnome", 25),
            "Demi-elfe" to DndRaceInfo("Demi-elfe", 30),
            "Demi-orc" to DndRaceInfo("Demi-orc", 30),
            "Tieffelin" to DndRaceInfo("Tieffelin", 30),
            "Dragonborn" to DndRaceInfo("Dragonborn", 30),
            "Gobelin" to DndRaceInfo("Gobelin", 30),
            "Orc" to DndRaceInfo("Orc", 30),
            "Hobgoblin" to DndRaceInfo("Hobgoblin", 30)
        )
        fun entries(): List<DndRaceInfo> = byName.values.toList()
        fun find(name: String): DndRaceInfo? = byName[name]
    }
}

private typealias DndClass = DndClassInfo
private typealias DndRace = DndRaceInfo
private typealias DndAlignment = DndConstants.DndAlignment

private fun DndClassInfo?.hitDieMax(): Int = hitDieMax(this?.hitDice)
private fun DndClassInfo?.savingThrows(): List<String> = this?.savingThrows ?: emptyList()
private fun DndClassInfo?.skillsPool(): List<String> = this?.skillsPool ?: emptyList()
private fun DndClassInfo?.skillsCount(): Int = this?.skillsCount ?: 0
private fun DndRaceInfo?.speed(): Int = this?.speed ?: 30
private fun DndConstants.DndAlignment.displayName(): String = this.displayName

private val DND_CLASSES = DndClassInfo.entries()
private val DND_RACES = DndRaceInfo.entries()
private val DND_ALIGNMENTS = DndConstants.DndAlignment.entriesList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndCharacterCreationForm(
    currentWorld: WorldState?,
    defaultType: String,
    submitLabel: String,
    onSubmit: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Identité
    var characterName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf<DndClass?>(null) }
    var selectedRace by remember { mutableStateOf<DndRace?>(null) }
    var characterLevel by remember { mutableIntStateOf(1) }
    var characterBackground by remember { mutableStateOf("") }
    var characterAlignment by remember { mutableStateOf("") }

    // Standard Array assignments : ability -> value
    var abilityAssignments by remember {
        mutableStateOf<Map<String, Int?>>(ABILITY_KEYS.associateWith { null })
    }

    // Méthode de génération des stats
    var statMethod by remember { mutableStateOf(StatMethod.STANDARD_ARRAY) }

    // Équipement / traits
    var equipment by remember { mutableStateOf("") }
    var traits by remember { mutableStateOf("") }

    // Dropdown states
    var classExpanded by remember { mutableStateOf(false) }
    var raceExpanded by remember { mutableStateOf(false) }
    var backgroundExpanded by remember { mutableStateOf(false) }
    var alignmentExpanded by remember { mutableStateOf(false) }

    val currentProficiencyBonus = calculateProficiencyBonus(characterLevel)

    // Valeurs finales
    val strength = abilityAssignments["strength"] ?: 10
    val dexterity = abilityAssignments["dexterity"] ?: 10
    val constitution = abilityAssignments["constitution"] ?: 10
    val intelligence = abilityAssignments["intelligence"] ?: 10
    val wisdom = abilityAssignments["wisdom"] ?: 10
    val charisma = abilityAssignments["charisma"] ?: 10

    val conMod = statModifier(constitution)
    val dexMod = statModifier(dexterity)
    val maxHp = selectedClass.hitDieMax() + conMod
    val armorClass = 10 + dexMod
    val initiative = dexMod
    val speed = selectedRace?.speed ?: 30

    // Maîtrises pré-remplies par la classe
    val classSavingThrows = selectedClass?.savingThrows ?: emptyList()
    val classSkillPool = selectedClass?.skillsPool ?: emptyList()
    val classSkillCount = selectedClass?.skillsCount ?: 0

    var selectedSavingThrows by remember { mutableStateOf<Map<String, ProficiencyLevel>>(emptyMap()) }
    var selectedSkills by remember { mutableStateOf<Map<String, ProficiencyLevel>>(emptyMap()) }

    // Réinitialiser les maîtrises quand la classe change
    androidx.compose.runtime.LaunchedEffect(selectedClass) {
        val newSaves = mutableMapOf<String, ProficiencyLevel>()
        classSavingThrows.forEach { newSaves[it] = ProficiencyLevel.PROFICIENT }
        selectedSavingThrows = newSaves
        selectedSkills = emptyMap()
    }

    val assignedValues = abilityAssignments.values.filterNotNull()

    val allStatsAssigned = abilityAssignments.values.all { it != null }

    // Budget Point Buy
    val pointBuyCost = abilityAssignments.values.filterNotNull().sumOf { POINT_BUY_COSTS[it] ?: 0 }
    val pointBuyRemaining = POINT_BUY_BUDGET - pointBuyCost
    val pointBuyValid = pointBuyRemaining >= 0

    val canSubmit = characterName.isNotBlank()
            && selectedClass != null
            && selectedRace != null
            && characterBackground.isNotBlank()
            && characterAlignment.isNotBlank()
            && allStatsAssigned
            && pointBuyValid

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Identité du personnage")

        OutlinedTextField(
            value = characterName,
            onValueChange = { characterName = it },
            label = { Text("Nom du personnage") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = classExpanded,
                onExpandedChange = { classExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedClass?.displayName ?: "",
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
                    DND_CLASSES.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls.displayName) },
                            onClick = {
                                selectedClass = cls
                                classExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = raceExpanded,
                onExpandedChange = { raceExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedRace?.displayName ?: "",
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
                    DND_RACES.forEach { race ->
                        DropdownMenuItem(
                            text = { Text(race.displayName) },
                            onClick = {
                                selectedRace = race
                                raceExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = characterLevel.toString(),
                onValueChange = { characterLevel = it.toIntOrNull()?.coerceIn(1, 20) ?: 1 },
                label = { Text("Niveau") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

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
                    DND_BACKGROUNDS.forEach { bg ->
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
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = alignmentExpanded,
                onDismissRequest = { alignmentExpanded = false }
            ) {
                DND_ALIGNMENTS.forEach { alignment ->
                    DropdownMenuItem(
                        text = { Text(alignment.displayName) },
                        onClick = {
                            characterAlignment = alignment.displayName
                            alignmentExpanded = false
                        }
                    )
                }
            }
        }

        SectionTitle("Méthode de génération des statistiques")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMethod.entries.forEach { method ->
                FilterChip(
                    selected = statMethod == method,
                    onClick = {
                        when (method) {
                            StatMethod.STANDARD_ARRAY -> {
                                abilityAssignments = ABILITY_KEYS.associateWith { null }
                            }
                            StatMethod.ROLL_4D6 -> {
                                abilityAssignments = ABILITY_KEYS.associateWith { roll4d6KeepBest3() }
                            }
                            StatMethod.POINT_BUY -> {
                                abilityAssignments = ABILITY_KEYS.associateWith { POINT_BUY_MIN }
                            }
                        }
                        statMethod = method
                    },
                    label = { Text(method.label) },
                    leadingIcon = if (statMethod == method) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionTitle("Caractéristiques")

        if (statMethod == StatMethod.STANDARD_ARRAY) {
            Text(
                text = "Assignez chaque valeur à une caractéristique. Chaque valeur ne peut être utilisée qu'une seule fois.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (statMethod == StatMethod.ROLL_4D6) {
            Button(
                onClick = { abilityAssignments = ABILITY_KEYS.associateWith { roll4d6KeepBest3() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Relancer les statistiques (4d6)")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (pointBuyValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Points restants : $pointBuyRemaining / $POINT_BUY_BUDGET",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (pointBuyValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (!pointBuyValid) {
                        Text(
                            text = "Budget dépassé !",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ABILITY_KEYS.chunked(2).forEach { rowAbilities ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowAbilities.forEach { ability ->
                        val labelIndex = ABILITY_KEYS.indexOf(ability)
                        val currentValue = abilityAssignments[ability]
                        StatAssigner(
                            label = ABILITY_SHORT_NAMES[labelIndex],
                            fullName = ABILITY_NAMES[labelIndex],
                            selectedValue = currentValue,
                            availableValues = when (statMethod) {
                                StatMethod.STANDARD_ARRAY -> STANDARD_ARRAY.filter { it !in assignedValues || it == currentValue }
                                StatMethod.ROLL_4D6,
                                StatMethod.POINT_BUY -> (POINT_BUY_MIN..POINT_BUY_MAX).toList()
                            },
                            onValueSelected = { value ->
                                when (statMethod) {
                                    StatMethod.STANDARD_ARRAY -> {
                                        abilityAssignments = abilityAssignments.toMutableMap().apply {
                                            entries.forEach { (key, existing) ->
                                                if (key != ability && existing == value) {
                                                    this[key] = null
                                                }
                                            }
                                            this[ability] = value
                                        }
                                    }
                                    StatMethod.ROLL_4D6,
                                    StatMethod.POINT_BUY -> {
                                        abilityAssignments = abilityAssignments.toMutableMap().apply {
                                            this[ability] = value
                                        }
                                    }
                                }
                            },
                            onClear = {
                                if (statMethod == StatMethod.STANDARD_ARRAY) {
                                    abilityAssignments = abilityAssignments.toMutableMap().apply {
                                        this[ability] = null
                                    }
                                }
                            },
                            readOnly = statMethod == StatMethod.ROLL_4D6,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAbilities.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        SectionTitle("Calculs automatiques")

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Calculs automatiques", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PV Max", style = MaterialTheme.typography.labelSmall)
                        Text("$maxHp", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("dé ${selectedClass?.hitDice ?: "d8"} + $conMod (CON)", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CA", style = MaterialTheme.typography.labelSmall)
                        Text("$armorClass", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("10 + $dexMod (DEX)", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Initiative", style = MaterialTheme.typography.labelSmall)
                        Text("%+d".format(initiative), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("$dexMod (DEX)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadOnlyStat("Bonus maîtrise", "+$currentProficiencyBonus", Modifier.weight(1f))
            ReadOnlyStat("Vitesse", "$speed ft", Modifier.weight(1f))
            ReadOnlyStat("Dé de vie", selectedClass?.hitDice ?: "—", Modifier.weight(1f))
        }

        SectionTitle("Maîtrises de classe — Jets de sauvegarde")

        Text(
            text = classSavingThrows.joinToString(", ").ifBlank { "Sélectionnez une classe" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        SectionTitle("Compétences de classe (${selectedSkills.size}/$classSkillCount)")

        Text(
            text = if (selectedClass != null) "Choisissez ${classSkillCount} compétence(s) parmi celles de la classe." else "Sélectionnez une classe",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (selectedClass != null) {
            SkillSelector(
                pool = classSkillPool,
                maxCount = classSkillCount,
                selected = selectedSkills,
                onChange = { selectedSkills = it }
            )
        }

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

        if (!canSubmit) {
            Text(
                text = when {
                    !pointBuyValid -> "Budget Point Buy dépassé. Réduisez vos caractéristiques."
                    characterName.isBlank() || selectedClass == null || selectedRace == null ||
                            characterBackground.isBlank() || characterAlignment.isBlank() || !allStatsAssigned ->
                        "Remplissez toutes les informations obligatoires (nom, classe, race, historique, alignement et les 6 stats)."
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val character = Character(
                    name = characterName,
                    type = defaultType,
                    worldId = currentWorld?.id ?: "",
                    characterClass = selectedClass!!.displayName,
                    race = selectedRace!!.displayName,
                    level = characterLevel,
                    alignment = characterAlignment,
                    background = characterBackground,
                    strength = strength,
                    dexterity = dexterity,
                    constitution = constitution,
                    intelligence = intelligence,
                    wisdom = wisdom,
                    charisma = charisma,
                    maxHitPoints = maxHp,
                    currentHitPoints = maxHp,
                    temporaryHitPoints = 0,
                    armorClass = armorClass,
                    speed = speed,
                    initiative = initiative,
                    proficiencyBonus = currentProficiencyBonus,
                    savingThrows = selectedSavingThrows,
                    skills = selectedSkills,
                    savingThrowProficiencies = classSavingThrows,
                    skillProficiencies = selectedSkills.keys.toList(),
                    equipment = equipment,
                    traits = traits,
                    notes = "",
                    personalityTraits = "",
                    ideals = "",
                    bonds = "",
                    flaws = "",
                    createdBy = if (defaultType == "PJ") "Joueur" else "MJ"
                )
                onSubmit(character)
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(submitLabel, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatAssigner(
    label: String,
    fullName: String,
    selectedValue: Int?,
    availableValues: List<Int>,
    onValueSelected: (Int) -> Unit,
    onClear: () -> Unit,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && !readOnly,
        onExpandedChange = { if (!readOnly) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue?.toString() ?: "—",
            onValueChange = {},
            readOnly = true,
            label = { Text("$label — $fullName") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedValue != null && !readOnly) {
                        TextButton(onClick = onClear) { Text("×") }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && !readOnly)
                }
            },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded && !readOnly,
            onDismissRequest = { expanded = false }
        ) {
            availableValues.forEach { value ->
                val mod = statModifier(value)
                DropdownMenuItem(
                    text = { Text("$value (%+d)".format(mod)) },
                    onClick = {
                        onValueSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillSelector(
    pool: List<String>,
    maxCount: Int,
    selected: Map<String, ProficiencyLevel>,
    onChange: (Map<String, ProficiencyLevel>) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pool.forEach { skill ->
            val isSelected = selected.containsKey(skill)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newMap = selected.toMutableMap()
                    if (isSelected) {
                        newMap.remove(skill)
                    } else if (selected.size < maxCount) {
                        newMap[skill] = ProficiencyLevel.PROFICIENT
                    }
                    onChange(newMap)
                },
                label = { Text(skill) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.width(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun ReadOnlyStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
