package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.theme.MysticPurple
import com.jc2.jdrcompagnon.ui.theme.RadiantCyan
import com.jc2.jdrcompagnon.ui.theme.DndRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character,
    isMjMode: Boolean = false,
    onBack: () -> Unit
) {
    val typeColor = getTypeColor(character.type)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // local state to allow immediate UI updates before persistence
    var currentCharacter by remember { mutableStateOf(character) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentCharacter.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header Info (toujours visible)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(32.dp),
                color = typeColor.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.2f)),
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = typeColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = typeColor, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = currentCharacter.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "${currentCharacter.race} • ${currentCharacter.characterClass} Niv.${currentCharacter.level}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = typeColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Onglets (Tabs) pour la navigation entre sections
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary, divider = {}) {
                Tab(
                    selected = (selectedTabIndex == 0),
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Stats", style = MaterialTheme.typography.labelLarge) },
                )
                Tab(
                    selected = (selectedTabIndex == 1),
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Combat", style = MaterialTheme.typography.labelLarge) },
                )
                Tab(
                    selected = (selectedTabIndex == 2),
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Objets", style = MaterialTheme.typography.labelLarge) },
                )
                Tab(
                    selected = (selectedTabIndex == 3),
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Lore", style = MaterialTheme.typography.labelLarge) },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contenu dynamique selon l'onglet sélectionné
            when (selectedTabIndex) {
                0 -> SkillsTab(currentCharacter)
                1 -> CombatTab(currentCharacter)
                2 -> InventoryTab(currentCharacter, isMjMode) { updated -> 
                    currentCharacter = updated
                    GameState.updateCharacter(updated)
                }
                3 -> HistoryTab(currentCharacter)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SkillsTab(character: Character) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "COMPÉTENCES",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
        )

        // Statistiques de base
        Text(
            text = "STATISTIQUES DE BASE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernStatItem(Modifier.weight(1f), "FOR", character.strength, MaterialTheme.colorScheme.primary)
            ModernStatItem(Modifier.weight(1f), "DEX", character.dexterity, MaterialTheme.colorScheme.secondary)
            ModernStatItem(Modifier.weight(1f), "CON", character.constitution, MaterialTheme.colorScheme.tertiary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernStatItem(Modifier.weight(1f), "INT", character.intelligence, MaterialTheme.colorScheme.primary)
            ModernStatItem(Modifier.weight(1f), "SAG", character.wisdom, MaterialTheme.colorScheme.secondary)
            ModernStatItem(Modifier.weight(1f), "CHA", character.charisma, MaterialTheme.colorScheme.tertiary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Compétences
        if (character.skills.isNotEmpty()) {
            Text(
                text = "COMPÉTENCES MAÎTRISÉES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            character.skills.forEach { (skill, level) ->
                SkillItem(skill, level, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Jets de sauvegarde
        if (character.savingThrows.isNotEmpty()) {
            Text(
                text = "JETS DE SAUVEGARDE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            character.savingThrows.forEach { (save, level) ->
                SaveThrowItem(save, level, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun CombatTab(character: Character) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "COMBAT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
        )

        // Bloc PV/CA/Init/Vitesse
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CombatStatCircle("PV", "${character.currentHp}/${character.maxHp}", Icons.Default.Favorite, MaterialTheme.colorScheme.primary)
                CombatStatCircle("CA", character.armorClass.toString(), Icons.Default.Shield, MaterialTheme.colorScheme.secondary)
                CombatStatCircle("INIT", character.initiative.toString(), Icons.Default.Speed, MaterialTheme.colorScheme.tertiary)
                CombatStatCircle("VIT", character.speed.toString(), Icons.AutoMirrored.Filled.DirectionsRun, MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Capacités/Spells/Traits
        if (character.traits.isNotBlank()) {
            Text(
                text = "CAPACITÉS & TRAITS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            ModernContentBox(character.traits)

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Notes DM (si disponible)
        if (character.dmNotes.isNotBlank()) {
            Text(
                text = "NOTES DU MJ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            ModernContentBox(character.dmNotes)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InventoryTab(
    character: Character, 
    isMjMode: Boolean, 
    onUpdate: (Character) -> Unit
) {
    var newItemName by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "ÉQUIPEMENT & SAC",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        // MJ Only: Add item
        if (isMjMode) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Donner un objet") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newItemName.isNotBlank()) {
                                onUpdate(character.copy(backpackItems = character.backpackItems + newItemName))
                                newItemName = ""
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InventorySection(
                title = "SAC À DOS",
                items = character.backpackItems,
                icon = Icons.Default.Backpack,
                modifier = Modifier.weight(1f),
            ) { item -> 
                onUpdate(character.copy(
                    backpackItems = character.backpackItems - item,
                    equippedItems = character.equippedItems + item
                ))
            }

            // PORTÉ
            InventorySection(
                title = "ÉQUIPÉ",
                items = character.equippedItems,
                icon = Icons.Default.Inventory,
                modifier = Modifier.weight(1f),
            ) { item -> 
                onUpdate(character.copy(
                    equippedItems = character.equippedItems - item,
                    backpackItems = character.backpackItems + item
                ))
            }
        }
    }
}

@Composable
private fun InventorySection(
    title: String, 
    items: List<String>, 
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onMove: (String) -> Unit
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
        }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Vide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Surface(
                            onClick = { onMove(item) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = item,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(character: Character) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "HISTOIRE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
        )

        // Background
        Text(
            text = "Background",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = character.background.ifEmpty { "Non spécifié" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
        )

        // Informations supplémentaires
        Text(
            text = "Informations",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        ModernContentBox("Classe: ${character.characterClass}\nRace: ${character.race}\nNiveau: ${character.level}\nType: ${character.type}")
    }
}

@Composable
private fun ModernStatItem(modifier: Modifier, label: String, value: Int, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.7f))
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            val modifierValue = (value - 10) / 2
            Text(
                text = if (modifierValue >= 0) "+$modifierValue" else modifierValue.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CombatStatCircle(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModernContentBox(content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SkillItem(skill: String, level: ProficiencyLevel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(skill, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        
        val label = when (level) {
            ProficiencyLevel.NONE -> "—"
            ProficiencyLevel.PROFICIENT -> "M"
            ProficiencyLevel.EXPERTISE -> "EX"
            else -> "ERR"
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (level) {
                ProficiencyLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
                ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (level) {
                    ProficiencyLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                    ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primary
                    ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SaveThrowItem(save: String, level: ProficiencyLevel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(save, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        
        val label = when (level) {
            ProficiencyLevel.NONE -> "—"
            ProficiencyLevel.PROFICIENT -> "M"
            ProficiencyLevel.EXPERTISE -> "EX"
            else -> "ERR"
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (level) {
                ProficiencyLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
                ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (level) {
                    ProficiencyLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                    ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primary
                    ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun getTypeColor(type: String): Color {
    return when (type) {
        "PJ" -> MysticPurple
        "PNJ" -> RadiantCyan
        "Monstre" -> DndRed
        "Boss" -> Color.Red
        else -> MysticPurple
    }
}
