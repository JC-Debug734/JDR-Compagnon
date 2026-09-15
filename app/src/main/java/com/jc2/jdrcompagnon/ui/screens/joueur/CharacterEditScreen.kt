package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.components.SrdLibraryPickerDialog
import com.jc2.jdrcompagnon.ui.components.SrdPickerEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.MysticPurple
import com.jc2.jdrcompagnon.ui.theme.RadiantCyan
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    character: Character,
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
) {
    var editedCharacter by remember { mutableStateOf(character) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ÉDITER : ${character.name.uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Annuler")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(editedCharacter) }) {
                        Icon(Icons.Default.Save, contentDescription = "Sauvegarder", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                val tabs = listOf("Stats", "Combat", "Inventaire", "Histoire")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTabIndex) {
                    0 -> CompetenceTab(editedCharacter) { editedCharacter = it }
                    1 -> CombatTab(editedCharacter) { editedCharacter = it }
                    2 -> InventoryTab(editedCharacter) { editedCharacter = it }
                    3 -> HistoryTab(editedCharacter) { editedCharacter = it }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CompetenceTab(character: Character, onUpdate: (Character) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        EditSectionTitle("CARACTÉRISTIQUES")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityScoreField("FOR", character.strength, Modifier.weight(1f)) { onUpdate(character.copy(strength = it)) }
            AbilityScoreField("DEX", character.dexterity, Modifier.weight(1f)) { onUpdate(character.copy(dexterity = it)) }
            AbilityScoreField("CON", character.constitution, Modifier.weight(1f)) { onUpdate(character.copy(constitution = it)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityScoreField("INT", character.intelligence, Modifier.weight(1f)) { onUpdate(character.copy(intelligence = it)) }
            AbilityScoreField("SAG", character.wisdom, Modifier.weight(1f)) { onUpdate(character.copy(wisdom = it)) }
            AbilityScoreField("CHA", character.charisma, Modifier.weight(1f)) { onUpdate(character.copy(charisma = it)) }
        }

        EditSectionTitle("MAÎTRISES DES SAUVEGARDES")
        val saves = listOf("FOR", "DEX", "CON", "INT", "SAG", "CHA")
        saves.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { save ->
                    ProficiencyToggle(
                        label = save,
                        level = character.savingThrows[save] ?: ProficiencyLevel.NONE,
                        modifier = Modifier.weight(1f)
                    ) { newLevel ->
                        val newSaves = character.savingThrows.toMutableMap()
                        if (newLevel == ProficiencyLevel.NONE) newSaves.remove(save) else newSaves[save] = newLevel
                        onUpdate(character.copy(savingThrows = newSaves))
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatTab(character: Character, onUpdate: (Character) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditSectionTitle("STATISTIQUES DE SURVIE")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatEditField("PV MAX", character.maxHitPoints.toString(), Modifier.weight(1f)) { onUpdate(character.copy(maxHitPoints = it.toIntOrNull() ?: character.maxHitPoints)) }
            StatEditField("PV ACTUELS", character.currentHitPoints.toString(), Modifier.weight(1f)) { onUpdate(character.copy(currentHitPoints = it.toIntOrNull() ?: character.currentHitPoints)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val acBreakdown = GameState.armorClassBreakdown(character)
            ComputedStatField(
                label = "CA",
                value = acBreakdown.total.toString(),
                detail = acBreakdown.armorName?.let { name ->
                    if (acBreakdown.hasShield) "$name + bouclier" else name
                },
                modifier = Modifier.weight(1f)
            )
            StatEditField("INIT", character.initiative.toString(), Modifier.weight(1f)) { onUpdate(character.copy(initiative = it.toIntOrNull() ?: character.initiative)) }
            StatEditField("VIT", character.speed.toString(), Modifier.weight(1f)) { onUpdate(character.copy(speed = it.toIntOrNull() ?: character.speed)) }
        }

        EditSectionTitle("CAPACITÉS & TRAITS")
        OutlinedTextField(
            value = character.traits,
            onValueChange = { onUpdate(character.copy(traits = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            shape = RoundedCornerShape(20.dp)
        )

        EditSectionTitle("NOTES DU MJ (CONFIDENTIEL)")
        OutlinedTextField(
            value = character.dmNotes,
            onValueChange = { onUpdate(character.copy(dmNotes = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.tertiary)
        )
    }
}

@Composable
private fun InventoryTab(character: Character, onUpdate: (Character) -> Unit) {
    var showLibraryPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Drag & Drop State
    var draggedItem by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Nom + isFromBackpack
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var backpackBounds by remember { mutableStateOf(Rect.Zero) }
    var equippedBounds by remember { mutableStateOf(Rect.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            EditSectionTitle("GESTION DES OBJETS")

            // Ajout d'objet depuis la bibliothèque SRD (source unique de l'équipement existant)
            OutlinedButton(
                onClick = { showLibraryPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un objet depuis la bibliothèque")
            }

            if (showLibraryPicker) {
                SrdLibraryPickerDialog(
                    title = "Choisir un objet",
                    onDismiss = { showLibraryPicker = false },
                    onSelect = { itemName ->
                        onUpdate(character.copy(backpackItems = character.backpackItems + itemName))
                    },
                    search = { query ->
                        val items = SrdRepository.loadEquipmentList(context, character.worldId.ifBlank { "donjon_et_dragon" })
                        val filtered = if (query.isBlank()) items else items.filter { it.name.contains(query, ignoreCase = true) }
                        filtered.map { SrdPickerEntry(name = it.name) }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(450.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SAC À DOS
                InventoryZone(
                    title = "SAC À DOS",
                    icon = Icons.Default.Backpack,
                    items = character.backpackItems,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { backpackBounds = it.boundsInWindow() },
                    onDeleteItem = { item ->
                        onUpdate(character.copy(backpackItems = character.backpackItems - item))
                    },
                    onDragStart = { item -> draggedItem = item to true }
                )

                // PORTÉ
                InventoryZone(
                    title = "ÉQUIPÉ",
                    icon = Icons.Default.Inventory,
                    items = character.equippedItems,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { equippedBounds = it.boundsInWindow() },
                    onDeleteItem = { item ->
                        onUpdate(character.copy(equippedItems = character.equippedItems - item))
                    },
                    onDragStart = { item -> draggedItem = item to false }
                )
            }

            Text(
                "Appuyez longuement sur un objet pour le faire glisser vers l'autre zone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Overlay de l'objet pendant le drag
        draggedItem?.let { (itemName, isFromBackpack) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset -> dragOffset = offset },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val finalPos = dragOffset
                                if (isFromBackpack && equippedBounds.contains(finalPos)) {
                                    onUpdate(character.copy(
                                        backpackItems = character.backpackItems - itemName,
                                        equippedItems = character.equippedItems + itemName
                                    ))
                                } else if (!isFromBackpack && backpackBounds.contains(finalPos)) {
                                    onUpdate(character.copy(
                                        equippedItems = character.equippedItems - itemName,
                                        backpackItems = character.backpackItems + itemName
                                    ))
                                }
                                draggedItem = null
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                draggedItem = null
                                dragOffset = Offset.Zero
                            }
                        )
                    }
                    .zIndex(100f)
            ) {
                // L'objet qui suit le doigt
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dragOffset.x.roundToInt() - 100, dragOffset.y.roundToInt() - 50) }
                        .width(150.dp)
                ) {
                    ItemCard(name = itemName, onDelete = {}, isDragging = true)
                }
            }
        }
    }
}

@Composable
private fun InventoryZone(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    modifier: Modifier = Modifier,
    onDeleteItem: (String) -> Unit,
    onDragStart: (String) -> Unit
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            if (items.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Vide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        Box(modifier = Modifier.pointerInput(item) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart(item) },
                                onDrag = { _, _ -> },
                                onDragEnd = {},
                                onDragCancel = {}
                            )
                        }) {
                            ItemCard(
                                name = item,
                                onDelete = { onDeleteItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    name: String,
    onDelete: () -> Unit,
    isDragging: Boolean = false
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val slot = remember(name) { equipmentSlotFor(name) }
    val weightLabel = remember(name) { equipmentWeightLabel(name) }
    val hands = remember(name) { equipmentHandsRequired(name) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = (if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).copy(alpha = if (isDragging) 0.8f else 1f),
        tonalElevation = if (isDragging) 12.dp else 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pastille = couleur de l'emplacement (arme, armure, sac...)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(slot.slotColor(), CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (weightLabel != null) "${slot.slotLabel()} • $weightLabel" else slot.slotLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Icônes de mains requises pour les armes (1 ou 2)
            if (hands != null) {
                HandsIcons(hands, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (!isDragging) {
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Jeter cet objet ?") },
            text = { Text("« $name » sera définitivement retiré de l'inventaire.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Jeter", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            }
        )
    }
}


@Composable
private fun HistoryTab(character: Character, onUpdate: (Character) -> Unit) {
    val context = LocalContext.current
    var showEspecePicker by remember { mutableStateOf(false) }
    var showClassePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditSectionTitle("IDENTITÉ")
        OutlinedTextField(
            value = character.name,
            onValueChange = { onUpdate(character.copy(name = it)) },
            label = { Text("NOM DU HÉROS") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        // RACE et CLASSE sont désormais choisies dans la bibliothèque SRD (especes_srd521.md /
        // classes_srd521.md) plutôt que saisies en texte libre, sur le même principe que le
        // sélecteur d'objets de InventoryTab : source unique de vérité = SrdRepository.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SrdPickerField(
                label = "RACE",
                value = character.race,
                modifier = Modifier.weight(1f),
                onClick = { showEspecePicker = true }
            )
            SrdPickerField(
                label = "CLASSE",
                value = character.characterClass,
                modifier = Modifier.weight(1f),
                onClick = { showClassePicker = true }
            )
        }

        if (showEspecePicker) {
            SrdLibraryPickerDialog(
                title = "Choisir une espèce",
                onDismiss = { showEspecePicker = false },
                onSelect = { name -> onUpdate(character.copy(race = name)) },
                search = { query ->
                    SrdRepository.searchEspeces(context, query, character.worldId.ifBlank { "donjon_et_dragon" })
                        .map { SrdPickerEntry(name = it.name) }
                }
            )
        }

        if (showClassePicker) {
            SrdLibraryPickerDialog(
                title = "Choisir une classe",
                onDismiss = { showClassePicker = false },
                onSelect = { name -> onUpdate(character.copy(characterClass = name)) },
                search = { query ->
                    SrdRepository.searchClasses(context, query, character.worldId.ifBlank { "donjon_et_dragon" })
                        .map { SrdPickerEntry(name = it.name) }
                }
            )
        }

        EditSectionTitle("HISTOIRE & ORIGINES")
        OutlinedTextField(
            value = character.background,
            onValueChange = { onUpdate(character.copy(background = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("Racontez la légende de votre personnage...") }
        )
    }
}

/**
 * Champ non éditable au clavier : affiche la valeur choisie et ouvre le sélecteur
 * de bibliothèque SRD au tap, comme le bouton "Ajouter un objet" de InventoryTab
 * mais dans un champ compact adapté à RACE/CLASSE.
 */
@Composable
private fun SrdPickerField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = { Text("Choisir…") },
        modifier = modifier.clickableNoRipple(onClick),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

/**
 * Un OutlinedTextField readOnly n'intercepte pas les taps par défaut ; ce modifier
 * ouvre le sélecteur au clic sans donner l'apparence d'un champ éditable au clavier.
 */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) })

@Composable
private fun AbilityScoreField(label: String, value: Int, modifier: Modifier = Modifier, onValueChange: (Int) -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { val newVal = it.toIntOrNull() ?: value; onValueChange(newVal.coerceIn(1, 30)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Black),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(60.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun StatEditField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

/**
 * Champ en lecture seule pour les stats dérivées de l'équipement (ex. CA),
 * calculées via [GameState.armorClassBreakdown] (source unique de vérité :
 * [com.jc2.jdrcompagnon.ui.ArmorRules]). Non éditable pour éviter toute
 * désynchronisation avec la fiche de personnage.
 */
@Composable
private fun ComputedStatField(label: String, value: String, detail: String? = null, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        supportingText = detail?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        singleLine = detail == null
    )
}

@Composable
private fun ProficiencyToggle(label: String, level: ProficiencyLevel, modifier: Modifier = Modifier, onToggle: (ProficiencyLevel) -> Unit) {
    val color = when(level) {
        ProficiencyLevel.NONE -> MaterialTheme.colorScheme.outline
        ProficiencyLevel.PROFICIENT -> MysticPurple
        ProficiencyLevel.EXPERTISE -> RadiantCyan
    }

    Surface(
        onClick = {
            val next = when(level) {
                ProficiencyLevel.NONE -> ProficiencyLevel.PROFICIENT
                ProficiencyLevel.PROFICIENT -> ProficiencyLevel.EXPERTISE
                ProficiencyLevel.EXPERTISE -> ProficiencyLevel.NONE
            }
            onToggle(next)
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                text = when(level) {
                    ProficiencyLevel.NONE -> "—"
                    ProficiencyLevel.PROFICIENT -> "M"
                    ProficiencyLevel.EXPERTISE -> "EX"
                },
                color = color,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun EditSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}