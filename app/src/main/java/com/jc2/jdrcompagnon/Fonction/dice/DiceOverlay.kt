package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import com.jc2.jdrcompagnon.ui.AdvantageState
import com.jc2.jdrcompagnon.ui.DicePoolEntry
import com.jc2.jdrcompagnon.ui.DiceRollResult
import com.jc2.jdrcompagnon.ui.DiceSkin
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import kotlinx.coroutines.delay

import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiceOverlay(
    currentWorld: WorldState? = null,
) {
    val diceState by GameState.diceState.collectAsState()
    val isDndWorld = currentWorld?.id == "donjon_et_dragon"

    val skinColors = rememberDiceSkinColors(diceState.diceSkin)

    // Auto-hide résultat dé simple - only for result auto-hide, not state reset
    LaunchedEffect(diceState.showResult) {
        if (diceState.showResult) {
            delay(3000.milliseconds)
            GameState.hideDiceResult()
        }
    }

    // Auto-hide résultat pool - only for result auto-hide, not state reset
    LaunchedEffect(diceState.showPoolResult) {
        if (diceState.showPoolResult) {
            delay(4000.milliseconds)
            GameState.hidePoolResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Résultats du dé
        if (diceState.showResult && (diceState.lastResult != null) && diceState.dicePool.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DiceResultDisplay(
                    result = diceState.lastResult ?: 1,
                    sides = diceState.lastSides,
                    advantageState = diceState.advantageState,
                    skinColors = skinColors
                )
            }
        }

        if (diceState.showPoolResult && diceState.lastPoolResults.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DicePoolResultDisplay(
                    results = diceState.lastPoolResults,
                    modifier = diceState.modifier,
                    total = diceState.lastPoolTotal,
                    advantageState = diceState.advantageState,
                    skinColors = skinColors
                )
            }
        }

        // 2. Dialogue des paramètres
        if (diceState.overlayVisible) {
            DiceSettingsDialog(
                dicePool = diceState.dicePool,
                modifier = diceState.modifier,
                advantageState = diceState.advantageState,
                soundEnabled = diceState.soundEnabled,
                diceSkin = diceState.diceSkin,
                isDndWorld = isDndWorld,
                onDismiss = { GameState.setDiceOverlayVisible(visible = false) },
                onAddDice = { sides: Int -> GameState.addDiceToPool(sides) },
                onRemoveDice = { sides: Int -> GameState.removeDiceFromPool(sides) },
                onIncrementModifier = { GameState.incrementModifier() },
                onDecrementModifier = { GameState.decrementModifier() },
                onSetAdvantageState = { state: AdvantageState -> GameState.setAdvantageState(state) },
                onToggleSound = { GameState.toggleSound() },
                onSetDiceSkin = { skin: DiceSkin -> GameState.setDiceSkin(skin) },
            ) { GameState.resetDicePool() }
        }
    }
}

@Composable
fun DiceResultDisplay(
    result: Int,
    sides: Int,
    advantageState: AdvantageState = AdvantageState.NORMAL,
    skinColors: DiceSkinColors = rememberDiceSkinColors(DiceSkin.CLASSIC)
) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Effet d'éclat en arrière-plan
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            color = skinColors.primary.copy(alpha = 0.1f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(skinColors.primary, Color.Transparent),
                ),
            )
        ) {}

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (advantageState != AdvantageState.NORMAL) {
                AdvantageBadge(state = advantageState)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "d$sides",
                    color = skinColors.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = result.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 100.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(4f, 4f),
                            blurRadius = 8f,
                        ),
                    ),
                    color = skinColors.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DicePoolResultDisplay(
    results: List<DiceRollResult>,
    modifier: Int,
    total: Int,
    advantageState: AdvantageState = AdvantageState.NORMAL,
    skinColors: DiceSkinColors = rememberDiceSkinColors(DiceSkin.CLASSIC)
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 12.dp,
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 340.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RÉSULTAT DU POOL",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = skinColors.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (advantageState != AdvantageState.NORMAL) {
                AdvantageBadge(state = advantageState)
                Spacer(modifier = Modifier.height(8.dp))
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5
            ) {
                results.forEach { roll ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = roll.value.toString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "d${roll.sides}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (modifier != 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Modificateur ${if (modifier > 0) "+$modifier" else modifier}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (advantageState != AdvantageState.NORMAL) {
                    AdvantageBadge(state = advantageState)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.verticalGradient(
                            listOf(skinColors.primary, skinColors.secondary),
                        ),
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceSettingsDialog(
    dicePool: List<DicePoolEntry>,
    modifier: Int,
    advantageState: AdvantageState,
    soundEnabled: Boolean,
    diceSkin: DiceSkin,
    isDndWorld: Boolean,
    onDismiss: () -> Unit,
    onAddDice: (Int) -> Unit,
    onRemoveDice: (Int) -> Unit,
    onIncrementModifier: () -> Unit,
    onDecrementModifier: () -> Unit,
    onSetAdvantageState: (AdvantageState) -> Unit,
    onToggleSound: () -> Unit,
    onSetDiceSkin: (DiceSkin) -> Unit,
    onReset: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 400.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paramètres du dé",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onReset,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Réinitialiser (garde 1d20 par défaut)",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (isDndWorld) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Avantage / Désavantage",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val advSelected = advantageState == AdvantageState.ADVANTAGE
                        val disSelected = advantageState == AdvantageState.DISADVANTAGE
                        SegmentedButton(
                            selected = advSelected,
                            onClick = {
                                onSetAdvantageState(
                                    if (advSelected) AdvantageState.NORMAL else AdvantageState.ADVANTAGE
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {},
                            label = {
                                Text(
                                    "Avantage",
                                    color = if (advSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFF4CAF50).copy(alpha = 0.85f),
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        SegmentedButton(
                            selected = disSelected,
                            onClick = {
                                onSetAdvantageState(
                                    if (disSelected) AdvantageState.NORMAL else AdvantageState.DISADVANTAGE
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {},
                            label = {
                                Text(
                                    "Désavantage",
                                    color = if (disSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFFF44336).copy(alpha = 0.85f),
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Sons",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Sons des dés", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Dé qui roule + résultat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { onToggleSound() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Apparence du dé",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiceSkin.entries.forEach { skin ->
                        val skinColors = rememberDiceSkinColors(skin)
                        val selected = diceSkin == skin
                        Surface(
                            onClick = { onSetDiceSkin(skin) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) skinColors.primary else skinColors.primary.copy(alpha = 0.15f),
                            border = if (selected) BorderStroke(2.dp, skinColors.primary) else null,
                            modifier = Modifier.animateContentSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Casino,
                                    contentDescription = null,
                                    tint = if (selected) skinColors.onPrimary else skinColors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = skin.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (selected) skinColors.onPrimary else skinColors.primary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Ajouter des dés",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (if (isDndWorld) listOf(4, 6, 8, 10, 12, 20, 100) else listOf(6, 20)).forEach { sides ->
                        val count = dicePool.find { it.sides == sides }?.count ?: 0
                        DiceSelectionChip(
                            sides = sides,
                            count = count,
                            onAdd = { onAddDice(sides) },
                            onRemove = { onRemoveDice(sides) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Modificateur",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(onClick = onDecrementModifier) {
                        Icon(Icons.Default.Remove, contentDescription = "Moins")
                    }
                    Text(
                        text = if (modifier >= 0) "+$modifier" else modifier.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    FilledTonalIconButton(onClick = onIncrementModifier) {
                        Icon(Icons.Default.Add, contentDescription = "Plus")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enregistrer", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun rememberDiceSkinColors(skin: DiceSkin): DiceSkinColors {
    return when (skin) {
        DiceSkin.CLASSIC -> DiceSkinColors(
            primary = MaterialTheme.colorScheme.primary,
            onPrimary = MaterialTheme.colorScheme.onPrimary,
            secondary = MaterialTheme.colorScheme.secondary
        )
        DiceSkin.SHADOW -> DiceSkinColors(
            primary = Color(0xFF37474F),
            onPrimary = Color.White,
            secondary = Color(0xFF263238)
        )
        DiceSkin.ICE -> DiceSkinColors(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF01579B),
            secondary = Color(0xFF0288D1)
        )
        DiceSkin.FIRE -> DiceSkinColors(
            primary = Color(0xFFFF7043),
            onPrimary = Color.White,
            secondary = Color(0xFFE64A19)
        )
    }
}

data class DiceSkinColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color
)

@Composable
fun DiceSelectionChip(
    sides: Int,
    count: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (count > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = "d$sides",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onAdd() }
                    .padding(vertical = 4.dp)
            )

            if (count > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = count.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}