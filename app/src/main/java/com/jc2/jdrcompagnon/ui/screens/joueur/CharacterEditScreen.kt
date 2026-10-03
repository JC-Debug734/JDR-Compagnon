package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.components.PnjReputationEditor
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.components.SrdLibraryPickerDialog
import com.jc2.jdrcompagnon.ui.components.SrdPickerEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.MysticPurple
import com.jc2.jdrcompagnon.ui.theme.RadiantCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    character: Character,
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    var editedCharacter by remember { mutableStateOf(character) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ÉDITER : ${character.name.uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Onglet "PNJ" ajouté uniquement pour un PNJ : c'est là (et nulle part ailleurs) que
            // le MJ règle le briefing (comportement/intentions/objectif) utilisé par l'événement
            // de scène "#event:[Nom]" — sinon perdu au milieu de l'onglet Combat, pas assez
            // visible.
            val tabs = remember(editedCharacter.type) {
                if (editedCharacter.type == "PNJ") {
                    listOf("Stats", "Combat", "Inventaire", "Histoire", "PNJ")
                } else {
                    listOf("Stats", "Combat", "Inventaire", "Histoire")
                }
            }
            LaunchedEffect(tabs) {
                if (selectedTabIndex >= tabs.size) selectedTabIndex = 0
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
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
                when (tabs.getOrNull(selectedTabIndex)) {
                    "Stats" -> CompetenceTab(editedCharacter) { editedCharacter = it }
                    "Combat" -> CombatTab(editedCharacter) { editedCharacter = it }
                    "Inventaire" -> InventoryTab(editedCharacter) { editedCharacter = it }
                    "Histoire" -> HistoryTab(editedCharacter) { editedCharacter = it }
                    "PNJ" -> PnjTab(editedCharacter) { editedCharacter = it }
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

        EditSectionTitle("MAÎTRISES DE COMBAT")
        OutlinedTextField(
            value = character.weaponArmorTraining,
            onValueChange = { onUpdate(character.copy(weaponArmorTraining = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(20.dp)
        )

        EditSectionTitle("CAPACITÉS DE CLASSE")
        OutlinedTextField(
            value = character.classFeatures,
            onValueChange = { onUpdate(character.copy(classFeatures = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            shape = RoundedCornerShape(20.dp)
        )

        EditSectionTitle("TRAITS D'ESPÈCE")
        OutlinedTextField(
            value = character.traits,
            onValueChange = { onUpdate(character.copy(traits = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            shape = RoundedCornerShape(20.dp)
        )

        EditSectionTitle("DONS")
        OutlinedTextField(
            value = character.feats,
            onValueChange = { onUpdate(character.copy(feats = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
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

/**
 * Onglet visible uniquement pour un PNJ (Character.type == "PNJ") : briefing confidentiel utilisé
 * par PnjBriefingOverlay quand le MJ clique un lien "#event:[Nom]" dans une scène. Jamais transmis
 * aux joueurs (seuls le nom et le portrait le sont, voir NetworkSessionManager.sendPnjBriefingToAll).
 */
@Composable
private fun PnjTab(character: Character, onUpdate: (Character) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditSectionTitle("BRIEFING PNJ (ÉVÉNEMENT DE SCÈNE, CONFIDENTIEL)")
        OutlinedTextField(
            value = character.comportement,
            onValueChange = { onUpdate(character.copy(comportement = it)) },
            label = { Text("Comportement") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.tertiary)
        )
        OutlinedTextField(
            value = character.intentions,
            onValueChange = { onUpdate(character.copy(intentions = it)) },
            label = { Text("Intentions") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.tertiary)
        )
        OutlinedTextField(
            value = character.objectif,
            onValueChange = { onUpdate(character.copy(objectif = it)) },
            label = { Text("Objectif") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.tertiary)
        )

        PnjReputationEditor(
            character = character,
            onUpdate = onUpdate,
            textColor = Color.White,
            sectionTitle = { EditSectionTitle(it) }
        )
    }
}

@Composable
private fun InventoryTab(character: Character, onUpdate: (Character) -> Unit) {
    var showLibraryPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Placement au clic (remplace le glisser-déposer) : un clic sur un objet le sélectionne,
    // un clic sur l'autre zone l'y déplace. Re-cliquer l'objet annule la sélection.
    var selectedItem by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Nom + isFromBackpack

    fun toggleSelection(item: String, fromBackpack: Boolean) {
        selectedItem = if (selectedItem == item to fromBackpack) null else item to fromBackpack
    }

    fun moveSelectionTo(toBackpack: Boolean) {
        val (itemName, fromBackpack) = selectedItem ?: return
        if (fromBackpack == toBackpack) {
            selectedItem = null
            return
        }
        if (fromBackpack) {
            onUpdate(character.copy(
                backpackItems = character.backpackItems - itemName,
                equippedItems = character.equippedItems + itemName
            ))
        } else {
            onUpdate(character.copy(
                equippedItems = character.equippedItems - itemName,
                backpackItems = character.backpackItems + itemName
            ))
        }
        selectedItem = null
    }

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
                selectedItem = selectedItem?.takeIf { it.second }?.first,
                isTarget = selectedItem?.second == false,
                modifier = Modifier.weight(1f),
                onDeleteItem = { item ->
                    if (selectedItem == item to true) selectedItem = null
                    onUpdate(character.copy(backpackItems = character.backpackItems - item))
                },
                onItemClick = { item -> toggleSelection(item, fromBackpack = true) },
                onZoneClick = { moveSelectionTo(toBackpack = true) }
            )

            // PORTÉ
            InventoryZone(
                title = "ÉQUIPÉ",
                icon = Icons.Default.Inventory,
                items = character.equippedItems,
                selectedItem = selectedItem?.takeIf { !it.second }?.first,
                isTarget = selectedItem?.second == true,
                modifier = Modifier.weight(1f),
                onDeleteItem = { item ->
                    if (selectedItem == item to false) selectedItem = null
                    onUpdate(character.copy(equippedItems = character.equippedItems - item))
                },
                onItemClick = { item -> toggleSelection(item, fromBackpack = false) },
                onZoneClick = { moveSelectionTo(toBackpack = false) }
            )
        }

        val selection = selectedItem
        Text(
            if (selection == null) "Touchez un objet pour le sélectionner, puis touchez l'autre zone pour l'y placer."
            else "« ${selection.first} » sélectionné : touchez la zone ${if (selection.second) "ÉQUIPÉ" else "SAC À DOS"} pour l'y placer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InventoryZone(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    selectedItem: String?,
    isTarget: Boolean,
    modifier: Modifier = Modifier,
    onDeleteItem: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onZoneClick: () -> Unit
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        }

        // Toute la zone est cliquable : c'est la cible quand un objet de l'autre zone est sélectionné.
        Surface(
            onClick = onZoneClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(20.dp),
            color = if (isTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = if (isTarget) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                        ItemCard(
                            name = item,
                            onDelete = { onDeleteItem(item) },
                            isSelected = item == selectedItem,
                            onClick = { onItemClick(item) }
                        )
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
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val slot = remember(name) { equipmentSlotFor(name) }
    val weightLabel = remember(name) { equipmentWeightLabel(name) }
    val hands = remember(name) { equipmentHandsRequired(name) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Contour de couleur autour du nom = emplacement où l'objet peut être équipé
                Box(
                    modifier = Modifier
                        .border(1.dp, slot.slotColor(), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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

            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
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

        // Sous-classe : pas de source SRD dédiée (elle apparaît en sous-section des
        // classes), donc saisie libre plutôt qu'un picker comme RACE/CLASSE.
        OutlinedTextField(
            value = character.subclass,
            onValueChange = { onUpdate(character.copy(subclass = it)) },
            label = { Text("SOUS-CLASSE") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

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

        EditSectionTitle("APPARENCE")
        OutlinedTextField(
            value = character.appearance,
            onValueChange = { onUpdate(character.copy(appearance = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            shape = RoundedCornerShape(20.dp),
            placeholder = { Text("Silhouette, tenue, signes distinctifs...") }
        )

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