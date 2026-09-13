@file:OptIn(ExperimentalFoundationApi::class)

package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jc2.jdrcompagnon.ui.ArmorRules
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlin.math.roundToInt

// Enlever les imports inutilisés signalés par le compilo.
private typealias EquipmentItemSRD = com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem

/**
 * Layout d'équipement en silhouette avec slots.
 *
 * - Sac à dos en haut avec items draggables.
 * - Silhouette humaine stylisée avec emplacements.
 * - Glisser-déposer un item d'un slot vers le sac ou inversement.
 * - Double-clic sur un item équipé → détails.
 * - Règles : 1 armure max (TORSO), bouclier = OFF_HAND, 2 mains = MAIN+OFF.
 */
/**
 * Gestion complète de l'équipement d'un personnage, embarquée directement
 * dans l'onglet "Équipement" de la fiche (plus d'écran séparé) :
 *
 * - Sac à dos en haut avec items draggables.
 * - Silhouette humaine stylisée avec emplacements.
 * - Glisser-déposer un item d'un slot vers le sac ou inversement.
 * - Double-clic sur un item équipé → détails.
 * - Règles : 1 armure max (TORSO), bouclier = OFF_HAND, 2 mains = MAIN+OFF.
 */
@Composable
fun EquipmentManagementContent(character: Character, isMjMode: Boolean = false, modifier: Modifier = Modifier) {
    val allCharacters by GameState.characters.collectAsState()
    val liveCharacter = allCharacters.find { it.id == character.id } ?: character
    var currentCharacter by remember { mutableStateOf(liveCharacter) }

    LaunchedEffect(liveCharacter) {
        currentCharacter = liveCharacter
    }

    var draggedItem by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var selectedItemDetail by remember { mutableStateOf<String?>(null) }
    var backpackBounds by remember { mutableStateOf(Rect.Zero) }
    var trashBounds by remember { mutableStateOf(Rect.Zero) }
    var slotBounds by remember { mutableStateOf<Map<EquipmentSlot, Rect>>(emptyMap()) }

    // Emplacement actuellement survolé — sert uniquement au surlignage visuel
    // pendant le glisser. L'action réelle (équiper/déséquiper) ne se déclenche
    // qu'au relâchement du doigt, dans resolveDrop().
    val hoveredSlot = if (draggedItem != null) {
        slotBounds.entries.firstOrNull { it.value.contains(dragOffset) }?.key
    } else null
    val isHoveringTrash = draggedItem != null && trashBounds.contains(dragOffset)

    // Résout la dépose au relâchement du doigt : supprime si on relâche sur la
    // corbeille, équipe si on relâche sur un emplacement (quel qu'il soit —
    // permet de passer directement d'un emplacement à un autre sans repasser
    // par le sac), déséquipe si on relâche sur le sac, ne fait rien sinon.
    fun resolveDrop() {
        val item = draggedItem ?: return
        val targetSlot = slotBounds.entries.firstOrNull { it.value.contains(dragOffset) }?.key
        when {
            trashBounds.contains(dragOffset) -> {
                GameState.removeItemCompletely(currentCharacter.id, item)
                currentCharacter = GameState.characters.value.find { it.id == currentCharacter.id } ?: currentCharacter
            }
            targetSlot != null -> {
                val accepted = GameState.equipInSlot(currentCharacter.id, item, targetSlot)
                if (accepted) {
                    currentCharacter = GameState.characters.value.find { it.id == currentCharacter.id } ?: currentCharacter
                }
            }
            backpackBounds.contains(dragOffset) && currentCharacter.equippedSlots.containsValue(item) -> {
                GameState.unequipFromSlot(currentCharacter.id, item)
                currentCharacter = GameState.characters.value.find { it.id == currentCharacter.id } ?: currentCharacter
            }
        }
        draggedItem = null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Infos charge / poids
        WeightSummaryCard(currentCharacter, isMjMode)

        // Sac à dos draggable — surligné quand un objet équipé est glissé au-dessus
        BackpackSection(
            items = currentCharacter.backpackItems,
            isDropTarget = draggedItem != null && currentCharacter.equippedSlots.containsValue(draggedItem),
            onBoundsChanged = { backpackBounds = it },
            onDragStart = { item, offset ->
                draggedItem = item
                dragStart = offset
                dragOffset = offset
            },
            onDragMove = { delta -> dragOffset += delta },
            onDragEnd = { resolveDrop() },
            onItemDoubleClick = { selectedItemDetail = it }
        )

        // Silhouette avec slots
        SilhouetteSlots(
            character = currentCharacter,
            hoveredSlot = hoveredSlot,
            isDraggingFromBackpack = draggedItem != null && currentCharacter.backpackItems.contains(draggedItem),
            isHoveringTrash = isHoveringTrash,
            onSlotBoundsChanged = { slot, bounds -> slotBounds = slotBounds + (slot to bounds) },
            onTrashBoundsChanged = { trashBounds = it },
            onSlotDragStart = { item, offset ->
                draggedItem = item
                dragStart = offset
                dragOffset = offset
            },
            onDragMove = { delta -> dragOffset += delta },
            onDragEnd = { resolveDrop() },
            onItemDoubleClick = { selectedItemDetail = it }
        )
    }

    if (draggedItem != null) {
        DraggedItemOverlay(
            itemName = draggedItem!!,
            offset = dragOffset
        )
    }

    selectedItemDetail?.let { item ->
        EquipmentItemDetailDialog(
            itemName = item,
            character = currentCharacter,
            onDismiss = { selectedItemDetail = null }
        )
    }
}

@Composable
private fun WeightSummaryCard(character: Character, isMjMode: Boolean) {
    val totalWeight = GameState.totalEquipmentWeight(character)
    val maxCarry = GameState.maxCarryWeight(character)
    val isOverloaded = totalWeight > maxCarry
    var showDetail by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isOverloaded) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else SheetSurface,
        border = BorderStroke(1.dp, if (isOverloaded) MaterialTheme.colorScheme.error else SheetBorder),
        onClick = { showDetail = !showDetail }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Charge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Text(
                    text = "${String.format(java.util.Locale.FRANCE, "%.1f", totalWeight)} / ${String.format(java.util.Locale.FRANCE, "%.1f", maxCarry)} kg",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverloaded) MaterialTheme.colorScheme.error else SheetTextPrimary
                )
            }
            LinearProgressIndicator(
                progress = { (totalWeight / maxCarry.coerceAtLeast(0.1)).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = SheetBorder
            )
            if (isOverloaded) {
                Text("Surcharge ! Force × 7,5 dépassé.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            AnimatedVisibility(visible = showDetail) {
                Text(
                    text = "Charge max = ${character.strength} (FOR) × 7,5 = ${String.format(java.util.Locale.FRANCE, "%.1f", maxCarry)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Bourse :",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SheetTextPrimary
                    )
                }
                if (isMjMode) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { GameState.addGold(character.id, -10) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Retirer 10 po", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${character.gold} po",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                        IconButton(onClick = { GameState.addGold(character.id, 10) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter 10 po", modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Text(
                        text = "${character.gold} po",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackpackSection(
    items: List<String>,
    isDropTarget: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onItemDoubleClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size
                onBoundsChanged(Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height))
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isDropTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else SheetSurface,
        border = BorderStroke(if (isDropTarget) 2.dp else 1.dp, if (isDropTarget) MaterialTheme.colorScheme.primary else SheetBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Backpack, null, tint = SheetTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sac à dos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            }
            if (items.isEmpty()) {
                Text(
                    "Glissez un objet équipé ici pour le déséquiper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary
                )
            } else {
                // FlowRow nécessite un opt-in expérimental ; on utilise des lignes fixes.
                val rowCapacity = 3
                val rows = items.chunked(rowCapacity)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                DraggableItemChip(
                                    item = item,
                                    isEquipped = false,
                                    onDragStart = onDragStart,
                                    onDragMove = onDragMove,
                                    onDragEnd = onDragEnd,
                                    onDoubleClick = { onItemDoubleClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SilhouetteSlots(
    character: Character,
    hoveredSlot: EquipmentSlot?,
    isDraggingFromBackpack: Boolean,
    isHoveringTrash: Boolean,
    onSlotBoundsChanged: (EquipmentSlot, Rect) -> Unit,
    onTrashBoundsChanged: (Rect) -> Unit,
    onSlotDragStart: (String, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onItemDoubleClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SheetSurface)
            .border(1.dp, SheetBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Corbeille — glisser un objet ici (du sac ou d'un emplacement) le supprime définitivement
            TrashDropZone(
                isHovering = isHoveringTrash,
                onBoundsChanged = onTrashBoundsChanged
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TORSO (large)
            SlotBox(
                slot = EquipmentSlot.TORSO,
                item = character.equippedSlots[EquipmentSlot.TORSO],
                icon = Icons.Default.Shield,
                label = "Torse",
                modifier = Modifier.width(180.dp),
                isHighlighted = isDraggingFromBackpack,
                isHovered = hoveredSlot == EquipmentSlot.TORSO,
                onBoundsChanged = { onSlotBoundsChanged(EquipmentSlot.TORSO, it) },
                onDragStart = onSlotDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                onDoubleClick = onItemDoubleClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // MAIN_HAND + OFF_HAND
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SlotBox(
                    slot = EquipmentSlot.MAIN_HAND,
                    item = character.equippedSlots[EquipmentSlot.MAIN_HAND],
                    icon = Icons.Default.Build,
                    label = "Main princ.",
                    isHighlighted = isDraggingFromBackpack,
                    isHovered = hoveredSlot == EquipmentSlot.MAIN_HAND,
                    onBoundsChanged = { onSlotBoundsChanged(EquipmentSlot.MAIN_HAND, it) },
                    onDragStart = onSlotDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    onDoubleClick = onItemDoubleClick
                )
                SlotBox(
                    slot = EquipmentSlot.OFF_HAND,
                    item = character.equippedSlots[EquipmentSlot.OFF_HAND],
                    icon = Icons.Default.Shield,
                    label = "Main sec.",
                    isHighlighted = isDraggingFromBackpack,
                    isHovered = hoveredSlot == EquipmentSlot.OFF_HAND,
                    onBoundsChanged = { onSlotBoundsChanged(EquipmentSlot.OFF_HAND, it) },
                    onDragStart = onSlotDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    onDoubleClick = onItemDoubleClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BACK + ACCESSORY
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SlotBox(
                    slot = EquipmentSlot.BACK,
                    item = character.equippedSlots[EquipmentSlot.BACK],
                    icon = Icons.Default.Backpack,
                    label = "Dos",
                    isHighlighted = isDraggingFromBackpack,
                    isHovered = hoveredSlot == EquipmentSlot.BACK,
                    onBoundsChanged = { onSlotBoundsChanged(EquipmentSlot.BACK, it) },
                    onDragStart = onSlotDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    onDoubleClick = onItemDoubleClick
                )
                SlotBox(
                    slot = EquipmentSlot.ACCESSORY,
                    item = character.equippedSlots[EquipmentSlot.ACCESSORY],
                    icon = Icons.Default.Star,
                    label = "Accessoire",
                    isHighlighted = isDraggingFromBackpack,
                    isHovered = hoveredSlot == EquipmentSlot.ACCESSORY,
                    onBoundsChanged = { onSlotBoundsChanged(EquipmentSlot.ACCESSORY, it) },
                    onDragStart = onSlotDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd,
                    onDoubleClick = onItemDoubleClick
                )
            }
        }
    }
}

@Composable
private fun SlotBox(
    slot: EquipmentSlot,
    item: String?,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean,
    isHovered: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onDragStart: (String, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDoubleClick: (String) -> Unit
) {
    val hasItem = item != null
    var slotPosition by remember { mutableStateOf(Offset.Zero) }

    val borderColor = when {
        isHovered -> MaterialTheme.colorScheme.primary
        hasItem -> SheetTextSecondary
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> SheetBorder
    }
    val containerColor = when {
        isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        hasItem -> SheetSurfaceLight
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> SheetSurface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isHovered) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .onGloballyPositioned { coords ->
                slotPosition = coords.positionInRoot()
                val pos = coords.positionInRoot()
                val size = coords.size
                onBoundsChanged(Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height))
            }
            .then(
                if (item != null) {
                    Modifier.pointerInput(item) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset -> onDragStart(item, slotPosition + offset) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragMove(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = { },
                onDoubleClick = item?.let { { onDoubleClick(it) } }
            )
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 120.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (hasItem) MaterialTheme.colorScheme.primary else SheetTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item ?: label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (hasItem) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (hasItem) SheetTextPrimary else SheetTextSecondary,
                modifier = Modifier.widthIn(min = 80.dp, max = 140.dp)
            )
            if (hasItem) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Glisser pour retirer",
                    style = MaterialTheme.typography.labelSmall,
                    color = SheetTextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrashDropZone(
    isHovering: Boolean,
    onBoundsChanged: (Rect) -> Unit
) {
    val borderColor = if (isHovering) MaterialTheme.colorScheme.error else SheetBorder
    val containerColor = if (isHovering) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else SheetSurfaceLight

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isHovering) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size
                onBoundsChanged(Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height))
            }
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 120.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Corbeille",
                tint = if (isHovering) MaterialTheme.colorScheme.error else SheetTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Jeter",
                style = MaterialTheme.typography.bodySmall,
                color = if (isHovering) MaterialTheme.colorScheme.error else SheetTextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Glisser un objet ici pour le supprimer",
                style = MaterialTheme.typography.labelSmall,
                color = SheetTextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DraggableItemChip(
    item: String,
    isEquipped: Boolean,
    onDragStart: (String, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDoubleClick: () -> Unit
) {
    var chipPosition by remember { mutableStateOf(Offset.Zero) }

    val containerColor = if (isEquipped) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else SheetSurfaceLight
    val contentColor = SheetTextPrimary

    Surface(
        modifier = Modifier
            .pointerInput(item) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        onDragStart(item, chipPosition + offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragMove(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .onGloballyPositioned { chipPosition = it.positionInRoot() }
            .combinedClickable(
                onClick = { },
                onDoubleClick = onDoubleClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, if (isEquipped) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else SheetBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isEquipped) {
                Icon(Icons.Default.Check, null, tint = contentColor, modifier = Modifier.size(16.dp))
            }
            Text(
                text = item,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DraggedItemOverlay(
    itemName: String,
    offset: Offset
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Surface(
            modifier = Modifier.offset {
                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 12.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, SheetTextPrimary.copy(alpha = 0.4f))
        ) {
            Text(
                text = itemName,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentItemDetailDialog(
    itemName: String,
    character: Character,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var itemDetails by remember { mutableStateOf<EquipmentItemSRD?>(null) }

    LaunchedEffect(itemName) {
        itemDetails = SrdRepository.getEquipmentByName(context, itemName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(itemName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (itemDetails != null) {
                    val item = itemDetails!!
                    if (item.category.isNotBlank()) Text("Catégorie : ${item.category}")
                    if (item.cost.isNotBlank()) Text("Coût : ${item.cost}")
                    if (item.weight.isNotBlank()) Text("Poids : ${item.weight}")
                    if (item.damage.isNotBlank()) Text("Dégâts : ${item.damage}")
                    if (item.properties.isNotBlank()) Text("Propriétés : ${item.properties}")
                    if (item.ac.isNotBlank()) Text("CA : ${item.ac}")
                } else {
                    Text("Aucune fiche SRD trouvée pour cet objet.")
                }
                Spacer(modifier = Modifier.height(8.dp))
                val slot = ArmorRules.slotForItem(itemName)
                Text(
                    text = "Slot : ${slot?.let { slotLabel(it) } ?: "Sac / divers"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

private fun slotLabel(slot: EquipmentSlot): String = when (slot) {
    EquipmentSlot.HEAD -> "Tête"
    EquipmentSlot.TORSO -> "Torse"
    EquipmentSlot.MAIN_HAND -> "Main principale"
    EquipmentSlot.OFF_HAND -> "Main secondaire"
    EquipmentSlot.BACK -> "Dos"
    EquipmentSlot.ACCESSORY -> "Accessoire"
}

private fun tryEquip(characterId: String, itemName: String, slot: EquipmentSlot): Boolean {
    return GameState.equipInSlot(characterId, itemName, slot)
}