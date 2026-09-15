package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.CharacterProgression
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.components.CharacterEquipment
import com.jc2.jdrcompagnon.ui.components.EquipmentManagementContent
import com.jc2.jdrcompagnon.ui.components.JoueurDrawer
import com.jc2.jdrcompagnon.ui.components.SheetBorder
import com.jc2.jdrcompagnon.ui.components.SheetCard
import com.jc2.jdrcompagnon.ui.components.SheetSurface
import com.jc2.jdrcompagnon.ui.components.SheetSurfaceLight
import com.jc2.jdrcompagnon.ui.components.SheetTextPrimary
import com.jc2.jdrcompagnon.ui.components.SheetTextSecondary
import com.jc2.jdrcompagnon.ui.components.sheetTextFieldColors
import com.jc2.jdrcompagnon.ui.screens.joueur.savingThrows
import com.jc2.jdrcompagnon.ui.screens.joueur.skillList
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character,
    isMjMode: Boolean = false,
    onBack: () -> Unit,
    onEdit: ((Character) -> Unit)? = null,
    onLevelUp: ((Character) -> Unit)? = null,
    onManageSpells: ((Character) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Aperçu", "Combat", "Équipement", "Notes")
    var showDeleteDialog by remember { mutableStateOf(false) }
    val allCharacters by GameState.characters.collectAsState()
    val currentCharacter = allCharacters.find { it.id == character.id } ?: character
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            JoueurDrawer(
                onChooseCharacter = onBack,
                onClose = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentCharacter.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = SheetTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${currentCharacter.race} ${currentCharacter.characterClass}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SheetTextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SheetTextPrimary)
                        }
                    },
                    actions = {
                        if (onManageSpells != null) {
                            IconButton(onClick = { onManageSpells(currentCharacter) }) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Gérer les sorts", tint = SheetTextPrimary)
                            }
                        }
                        if (isMjMode && onEdit != null) {
                            IconButton(onClick = { onEdit(currentCharacter) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = SheetTextPrimary)
                            }
                        }
                        if (isMjMode) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = SheetTextPrimary,
                        navigationIconContentColor = SheetTextPrimary,
                        actionIconContentColor = SheetTextPrimary
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Carte d'en-tête commune, présente sur les 4 onglets
                CharacterStatsHeader(currentCharacter, isMjMode)

                // Tab selector, sous la carte d'en-tête
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        tabTitles.forEachIndexed { index, title ->
                            val selected = selectedTab == index
                            Text(
                                text = title,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> OverviewTab(currentCharacter, isMjMode)
                    1 -> CombatTab(currentCharacter)
                    2 -> EquipmentTab(currentCharacter, isMjMode)
                    3 -> NotesTab(currentCharacter, isMjMode)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer ${currentCharacter.name} ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GameState.removeCharacter(character.id)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}

/**
 * Barre de niveau affichée sous le nom du personnage (remplace
 * l'ancien menu "Monter de niveau").
 */
@Composable
private fun LevelBar(level: Int, experience: Int) {
    val progress = CharacterProgression.progressToNextLevel(level, experience)
    val nextThreshold = CharacterProgression.xpForLevel((level + 1).coerceAtMost(20))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            trackColor = SheetSurfaceLight
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NIV. $level",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = SheetTextPrimary
            )
            Text(
                text = if (level >= 20) "$experience XP (max)" else "$experience / $nextThreshold XP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SheetTextPrimary
            )
        }
    }
}

@Composable
private fun OverviewTab(character: Character, isMjMode: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SheetCard {
            Text(
                "Caractéristiques",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            AbilitiesGrid(character, isMjMode)
        }

        SavingThrowsOverviewCard(character)

        SheetCard {
            Text("Compétences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            skillList.forEach { skill ->
                val level = when {
                    character.skillProficiencies.contains(skill) -> ProficiencyLevel.PROFICIENT
                    else -> ProficiencyLevel.NONE
                }
                ProficiencyRow(
                    label = "$skill (${skillAbilityAbbreviation(skill)})",
                    level = level,
                    modifier = GameState.abilityModifierForSkill(skill, character),
                    proficiencyBonus = character.proficiencyBonus,
                    onClick = if (isMjMode) {
                        { GameState.toggleSkillProficiency(character.id, skill) }
                    } else null
                )
            }
        }
    }
}

/**
 * Abréviation de la caractéristique associée à une compétence D&D 5e,
 * affichée entre parenthèses à côté de son nom.
 */
private fun skillAbilityAbbreviation(skill: String): String = when (skill) {
    "Acrobaties", "Furtivité", "Escamotage" -> "DEX"
    "Arcanes", "Histoire", "Investigation", "Nature", "Religion" -> "INT"
    "Athlétisme" -> "FOR"
    "Dressage", "Médecine", "Perception", "Survie" -> "SAG"
    "Intimidation", "Persuasion", "Représentation", "Tromperie" -> "CHA"
    else -> "?"
}

/**
 * Bandeau d'en-tête façon fiche de personnage : armure, initiative,
 * portrait, points de vie avec barre de progression.
 */
@Composable
private fun CharacterStatsHeader(character: Character, isMjMode: Boolean = false) {
    val init = GameState.abilityModifier(character.dexterity)
    val passivePerception = 10 + GameState.abilityModifierForSkill("Perception", character)
    val hitDieFaces = hitDieForClass(character.characterClass)
    val hitDiceRemaining = (character.level - character.hitDiceUsed).coerceIn(0, character.level)
    val hpFraction = if (character.maxHitPoints > 0) {
        character.currentHitPoints.toFloat() / character.maxHitPoints.toFloat()
    } else 0f
    var showPortraitPicker by remember { mutableStateOf(false) }
    var showHpDialog by remember { mutableStateOf(false) }
    var showHitDiceDialog by remember { mutableStateOf(false) }
    var showXpDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }
    // TODO: `character.heroicInspiration` doit être ajouté au data class Character
    // (Boolean, défaut false) et un GameState.setHeroicInspiration(id, value)
    // doit être créé sur le même modèle que GameState.setCharacterPortrait.
    val hasInspiration = character.heroicInspiration

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SheetSurface,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colonne gauche : Maîtrise, Inspiration
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBadge(value = "+${character.proficiencyBonus}", label = "MAÎTRISE")
                    InspirationBadge(
                        active = hasInspiration,
                        onToggle = { GameState.setHeroicInspiration(character.id, !hasInspiration) }
                    )
                }

                // Portrait agrandi, centré
                Box(
                    modifier = Modifier.width(120.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(156.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, PortraitFrameGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SheetBorder)
                            .border(3.dp, PortraitFrameGold, RoundedCornerShape(8.dp))
                            .clickable { showPortraitPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val portraitRes = characterPortraitOptions.find { it.id == character.portrait }?.resId
                        if (portraitRes != null) {
                            Image(
                                painter = painterResource(id = portraitRes),
                                contentDescription = "Portrait de ${character.name}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = SheetTextPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Colonne droite : PV puis Dés de vie
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .let { if (isMjMode) it.clickable { showHpDialog = true } else it }
                            .padding(4.dp)
                    ) {
                        Text(
                            "PV",
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${character.currentHitPoints}/${character.maxHitPoints}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { hpFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = SheetBorder
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .let { if (isMjMode) it.clickable { showHitDiceDialog = true } else it }
                            .padding(4.dp)
                    ) {
                        Text(
                            "DÉ DE VIE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "D$hitDieFaces $hitDiceRemaining/${character.level}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (isMjMode) it.clickable { showXpDialog = true } else it },
                contentAlignment = Alignment.Center
            ) {
                LevelBar(level = character.level, experience = character.experience)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 cases sous la barre d'XP : Initiative, Vitesse, Taille, Perception passive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(
                    value = if (init >= 0) "+$init" else init.toString(),
                    label = "INITIATIVE",
                )
                StatBadge(
                    value = "${character.speed}m",
                    label = "VITESSE",
                    onClick = if (isMjMode) { { showSpeedDialog = true } } else null
                )
                StatBadge(
                    value = character.size.ifBlank { sizeForRace(character.race) },
                    label = "TAILLE",
                    onClick = if (isMjMode) { { showSizeDialog = true } } else null
                )
                StatBadge(
                    value = passivePerception.toString(),
                    label = "PERC. PASSIVE",
                )
            }
        }
    }

    if (showPortraitPicker) {
        PortraitPickerDialog(
            currentPortrait = character.portrait,
            onSelect = { portraitId ->
                GameState.setCharacterPortrait(character.id, portraitId)
                showPortraitPicker = false
            },
            onDismiss = { showPortraitPicker = false }
        )
    }

    if (showHpDialog) {
        HpEditDialog(
            currentHp = character.currentHitPoints,
            maxHp = character.maxHitPoints,
            onConfirm = { current, max ->
                GameState.setCharacterHp(character.id, current, max)
                showHpDialog = false
            },
            onDismiss = { showHpDialog = false }
        )
    }

    if (showHitDiceDialog) {
        HitDiceEditDialog(
            hitDieFaces = hitDieFaces,
            level = character.level,
            hitDiceRemaining = hitDiceRemaining,
            onConfirm = { remaining ->
                GameState.setHitDiceUsed(character.id, character.level - remaining)
                showHitDiceDialog = false
            },
            onDismiss = { showHitDiceDialog = false }
        )
    }

    if (showXpDialog) {
        XpEditDialog(
            currentXp = character.experience,
            onConfirm = { newXp ->
                GameState.setExperience(character.id, newXp)
                showXpDialog = false
            },
            onDismiss = { showXpDialog = false }
        )
    }

    if (showSpeedDialog) {
        SpeedEditDialog(
            currentSpeed = character.speed,
            onConfirm = { newSpeed ->
                GameState.setCharacterSpeed(character.id, newSpeed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showSizeDialog) {
        SizeEditDialog(
            currentSize = character.size.ifBlank { sizeForRace(character.race) },
            onConfirm = { newSize ->
                GameState.setCharacterSize(character.id, newSize)
                showSizeDialog = false
            },
            onDismiss = { showSizeDialog = false }
        )
    }
}

@Composable
private fun HpEditDialog(
    currentHp: Int,
    maxHp: Int,
    onConfirm: (current: Int, max: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentText by remember { mutableStateOf(currentHp.toString()) }
    var maxText by remember { mutableStateOf(maxHp.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les points de vie") },
        text = {
            Column {
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) maxText = input },
                    label = { Text("PV maximum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) currentText = input },
                    label = { Text("PV actuels") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newMax = maxText.toIntOrNull() ?: maxHp
                val newCurrent = currentText.toIntOrNull() ?: currentHp
                onConfirm(newCurrent, newMax)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun HitDiceEditDialog(
    hitDieFaces: Int,
    level: Int,
    hitDiceRemaining: Int,
    onConfirm: (remaining: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var remainingText by remember { mutableStateOf(hitDiceRemaining.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les dés de vie (D$hitDieFaces)") },
        text = {
            OutlinedTextField(
                value = remainingText,
                onValueChange = { input -> if (input.all { it.isDigit() }) remainingText = input },
                label = { Text("Dés restants (sur $level)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val newRemaining = (remainingText.toIntOrNull() ?: hitDiceRemaining).coerceIn(0, level)
                onConfirm(newRemaining)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun XpEditDialog(
    currentXp: Int,
    onConfirm: (newXp: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var xpText by remember { mutableStateOf(currentXp.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l'expérience") },
        text = {
            OutlinedTextField(
                value = xpText,
                onValueChange = { input -> if (input.all { it.isDigit() }) xpText = input },
                label = { Text("XP total") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(xpText.toIntOrNull() ?: currentXp)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun SpeedEditDialog(
    currentSpeed: Int,
    onConfirm: (newSpeed: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var speedText by remember { mutableStateOf(currentSpeed.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la vitesse") },
        text = {
            OutlinedTextField(
                value = speedText,
                onValueChange = { input -> if (input.all { it.isDigit() }) speedText = input },
                label = { Text("Vitesse (mètres)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(speedText.toIntOrNull() ?: currentSpeed)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

private val characterSizeOptions = listOf("Très Petite", "Petite", "Moyenne", "Grande", "Très Grande", "Gigantesque")

@Composable
private fun SizeEditDialog(
    currentSize: String,
    onConfirm: (newSize: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la taille") },
        text = {
            Column {
                characterSizeOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selected == option) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(1.dp, SheetTextSecondary, CircleShape)
                        )
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/** Teinte dorée du cadre de portrait — cohérente avec la couleur de l'or (Bourse). */
private val PortraitFrameGold = Color(0xFFD4AF37)

/**
 * Portrait disponible localement (drawable ajouté au projet).
 */
private data class PortraitOption(val id: String, val label: String, val resId: Int)

private val characterPortraitOptions = listOf(
    PortraitOption("drakeide_f", "Drakéide (F)", R.drawable.av_drakeide_f),
    PortraitOption("drakeide_m", "Drakéide (H)", R.drawable.av_drakeide_m),
    PortraitOption("elfe_f", "Elfe (F)", R.drawable.av_elfe_f),
    PortraitOption("elfe_m", "Elfe (H)", R.drawable.av_elfe_m),
    PortraitOption("gnome_f", "Gnome (F)", R.drawable.av_gnome_f),
    PortraitOption("gnome_m", "Gnome (H)", R.drawable.av_gnome_m),
    PortraitOption("goliath_f", "Goliath (F)", R.drawable.av_goliath_f),
    PortraitOption("goliath_m", "Goliath (H)", R.drawable.av_goliath_m),
    PortraitOption("halfling_f", "Halfelin (F)", R.drawable.av_halfling_f),
    PortraitOption("halfling_m", "Halfelin (H)", R.drawable.av_halfling_m),
    PortraitOption("nain_f", "Nain (F)", R.drawable.av_nain_f),
    PortraitOption("nain_h", "Nain (H)", R.drawable.av_nain_h),
    PortraitOption("nain2_h", "Nain (H) 2", R.drawable.av_nain2_h),
    PortraitOption("orc_f", "Orc (F)", R.drawable.av_orc_f),
    PortraitOption("orc_m", "Orc (H)", R.drawable.av_orc_m),
    PortraitOption("tieffelin_f", "Tieffelin (F)", R.drawable.av_tieffelin_f),
    PortraitOption("tieffelin_h", "Tieffelin (H)", R.drawable.av_tieffelin_h),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortraitPickerDialog(
    currentPortrait: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir un portrait") },
        text = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                characterPortraitOptions.forEach { option ->
                    val isSelected = option.id == currentPortrait
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(option.id) }
                            .padding(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = option.resId),
                            contentDescription = option.label,
                            modifier = Modifier
                                .width(56.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else SheetBorder,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(option.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

/**
 * Badge cliquable pour l'Inspiration Héroïque — s'allume en doré quand
 * elle est disponible, à toggle au tap (dépensée après usage en jeu).
 */
@Composable
private fun InspirationBadge(active: Boolean, onToggle: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) PortraitFrameGold.copy(alpha = 0.25f) else SheetSurfaceLight)
                .border(
                    1.dp,
                    if (active) PortraitFrameGold else SheetTextSecondary.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Inspiration héroïque",
                tint = if (active) PortraitFrameGold else SheetTextSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("INSPIRATION", style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary, letterSpacing = 1.sp)
    }
}

/**
 * Type de dé de vie associé à une classe D&D 5e (noms français du SRD).
 */
private fun hitDieForClass(characterClass: String): Int = when (characterClass.trim().lowercase(Locale.FRANCE)) {
    "barbare" -> 12
    "guerrier", "paladin", "rôdeur", "rodeur" -> 10
    "barde", "clerc", "druide", "moine", "roublard", "occultiste" -> 8
    "ensorceleur", "magicien" -> 6
    else -> 8
}

/**
 * Catégorie de taille D&D 5e associée à une race (noms français du SRD).
 */
private fun sizeForRace(race: String): String = when (race.trim().lowercase(Locale.FRANCE)) {
    "nain", "halfelin", "gnome" -> "Petite"
    else -> "Moyenne"
}

@Composable
private fun StatBadge(value: String, label: String, onClick: (() -> Unit)? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 52.dp, minHeight = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SheetSurfaceLight)
                .border(1.dp, SheetTextSecondary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .let { if (onClick != null) it.clickable { onClick() } else it }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = SheetTextPrimary,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary, letterSpacing = 1.sp)
    }
}

/**
 * Jets de sauvegarde en aperçu (lecture seule) — la version modifiable
 * complète reste dans l'onglet "Compétences".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavingThrowsOverviewCard(character: Character) {
    SheetCard {
        Text(
            "Jets de sauvegarde",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SheetTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            savingThrows.forEach { save ->
                val proficient = character.savingThrowProficiencies.contains(save)
                val total = GameState.abilityModifierForSave(save, character) +
                        if (proficient) character.proficiencyBonus else 0
                Row(
                    modifier = Modifier.width(150.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (proficient) MaterialTheme.colorScheme.error else Color.Transparent)
                                .border(1.dp, SheetTextSecondary, CircleShape)
                        )
                        Text(
                            save.uppercase(Locale.FRANCE),
                            style = MaterialTheme.typography.labelMedium,
                            color = SheetTextPrimary
                        )
                    }
                    Text(
                        text = if (total >= 0) "+$total" else total.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SheetTextPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilitiesGrid(character: Character, isMjMode: Boolean = false) {
    val abilities = listOf(
        "FOR" to character.strength,
        "DEX" to character.dexterity,
        "CON" to character.constitution,
        "INT" to character.intelligence,
        "SAG" to character.wisdom,
        "CHA" to character.charisma
    )
    var editingAbility by remember { mutableStateOf<String?>(null) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        abilities.forEach { (label, value) ->
            val proficient = character.savingThrowProficiencies.contains(abilitySaveName(label))
            AbilityCard(
                label,
                value,
                proficient,
                onClick = if (isMjMode) { { editingAbility = label } } else null
            )
        }
    }

    val abilityBeingEdited = editingAbility
    if (abilityBeingEdited != null) {
        val currentValue = abilities.first { it.first == abilityBeingEdited }.second
        val saveName = abilitySaveName(abilityBeingEdited)
        AbilityEditDialog(
            label = abilityBeingEdited,
            currentValue = currentValue,
            proficient = character.savingThrowProficiencies.contains(saveName),
            onConfirm = { newValue, newProficient ->
                GameState.setAbilityScore(character.id, abilityBeingEdited, newValue)
                if (newProficient != character.savingThrowProficiencies.contains(saveName)) {
                    GameState.toggleSavingThrowProficiency(character.id, saveName)
                }
                editingAbility = null
            },
            onDismiss = { editingAbility = null }
        )
    }
}

@Composable
private fun AbilityEditDialog(
    label: String,
    currentValue: Int,
    proficient: Boolean,
    onConfirm: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var valueText by remember { mutableStateOf(currentValue.toString()) }
    var proficientChecked by remember { mutableStateOf(proficient) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier $label") },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) valueText = input },
                    label = { Text("Valeur") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { proficientChecked = !proficientChecked }
                ) {
                    Checkbox(checked = proficientChecked, onCheckedChange = { proficientChecked = it })
                    Text("Maîtrise le jet de sauvegarde")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newValue = valueText.toIntOrNull() ?: currentValue
                onConfirm(newValue, proficientChecked)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Nom complet du jet de sauvegarde associé à l'abréviation d'une
 * caractéristique (FOR/DEX/CON/INT/SAG/CHA), pour vérifier la maîtrise.
 */
private fun abilitySaveName(label: String): String = when (label) {
    "FOR" -> "Force"
    "DEX" -> "Dextérité"
    "CON" -> "Constitution"
    "INT" -> "Intelligence"
    "SAG" -> "Sagesse"
    "CHA" -> "Charisme"
    else -> ""
}

@Composable
private fun AbilityCard(label: String, value: Int, proficient: Boolean = false, onClick: (() -> Unit)? = null) {
    val modifierValue = GameState.abilityModifier(value)
    Box(modifier = Modifier.width(100.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable { onClick() } else it },
            shape = RoundedCornerShape(16.dp),
            color = SheetSurfaceLight,
            border = BorderStroke(1.dp, SheetBorder)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SheetTextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (modifierValue >= 0) "+$modifierValue" else modifierValue.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = SheetTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SheetBorder,
                    border = BorderStroke(1.dp, SheetTextSecondary.copy(alpha = 0.4f))
                ) {
                    Text(
                        value.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SheetTextPrimary
                    )
                }
            }
        }

        if (proficient) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, SheetTextPrimary.copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "M",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
    }
}

@Composable
private fun CombatTab(character: Character) {
    val acBreakdown = GameState.armorClassBreakdown(character)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SheetCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Shield, null, tint = SheetTextSecondary)
                Text("Classe d'armure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = acBreakdown.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = SheetTextSecondary
            )
            if (acBreakdown.hasArmor || acBreakdown.hasShield) {
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (acBreakdown.hasArmor) {
                        AssistChip(
                            onClick = {},
                            label = { Text(acBreakdown.armorName ?: "Armure") },
                            leadingIcon = { Icon(Icons.Default.Shield, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SheetSurfaceLight,
                                labelColor = SheetTextPrimary,
                                leadingIconContentColor = SheetTextSecondary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = SheetBorder
                            )
                        )
                    }
                    if (acBreakdown.hasShield) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Bouclier +2") },
                            leadingIcon = { Icon(Icons.Default.Shield, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SheetSurfaceLight,
                                labelColor = SheetTextPrimary,
                                leadingIconContentColor = SheetTextSecondary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = SheetBorder
                            )
                        )
                    }
                }
            }
        }

        // HP / death saves / hit dice
        SheetCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.error)
                Text("Points de vie", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))

            val hpFraction = if (character.maxHitPoints > 0) {
                character.currentHitPoints.toFloat() / character.maxHitPoints.toFloat()
            } else 0f
            Text(
                "${character.currentHitPoints} / ${character.maxHitPoints}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = SheetTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { hpFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.error,
                trackColor = SheetBorder
            )
            if ((character.temporaryHitPoints ?: 0) > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("PV temporaires : +${character.temporaryHitPoints}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
        }

        SheetCard {
            Text("Initiative", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Mod DEX : ${GameState.abilityModifier(character.dexterity)}",
                style = MaterialTheme.typography.bodyLarge,
                color = SheetTextSecondary
            )
        }
    }
}

@Composable
private fun ProficiencyRow(
    label: String,
    level: ProficiencyLevel,
    modifier: Int,
    proficiencyBonus: Int,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, labelText) = when (level) {
        ProficiencyLevel.NONE -> SheetSurfaceLight to "—"
        ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) to "M"
        ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f) to "E"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SheetBorder,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(labelText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                }
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
        }
        val total = modifier + when (level) {
            ProficiencyLevel.NONE -> 0
            ProficiencyLevel.PROFICIENT -> proficiencyBonus
            ProficiencyLevel.EXPERTISE -> proficiencyBonus * 2
        }
        Text(
            text = if (total >= 0) "+$total" else "$total",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = SheetTextPrimary
        )
    }
}

@Composable
private fun EquipmentTab(character: Character, isMjMode: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CharacterEquipment(character, isMjMode)
        EquipmentManagementContent(character, isMjMode)
    }
}

@Composable
private fun NotesTab(character: Character, isMjMode: Boolean) {
    var expandedPersonality by remember { mutableStateOf(true) }
    var expandedIdeals by remember { mutableStateOf(false) }
    var expandedBonds by remember { mutableStateOf(false) }
    var expandedFlaws by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SheetCard {
            Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = character.notes,
                onValueChange = { GameState.updateCharacterNotes(character.id, it) },
                label = { Text("Notes du personnage") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = sheetTextFieldColors()
            )
        }

        ExpandableSection("Personnalité", character.personalityTraits, expandedPersonality) { expandedPersonality = !expandedPersonality }
        ExpandableSection("Idéaux", character.ideals, expandedIdeals) { expandedIdeals = !expandedIdeals }
        ExpandableSection("Liens", character.bonds, expandedBonds) { expandedBonds = !expandedBonds }
        ExpandableSection("Défauts", character.flaws, expandedFlaws) { expandedFlaws = !expandedFlaws }

        SheetCard {
            Text("Histoire et personnalité", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "Alignement", value = character.alignment.ifBlank { "—" })
            DetailRow(label = "Historique", value = character.background.ifBlank { "—" })
        }

        if (character.traits.isNotBlank()) {
            SheetCard {
                Text("Traits & capacités", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(character.traits, style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
            }
        }
    }
}

@Composable
private fun ExpandableSection(title: String, content: String, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SheetSurface,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = content.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (content.isBlank()) SheetTextSecondary else SheetTextPrimary
                    )
                }
            }
        }
    }
}