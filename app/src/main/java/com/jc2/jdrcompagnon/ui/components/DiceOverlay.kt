package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import com.jc2.jdrcompagnon.ui.DicePoolEntry
import com.jc2.jdrcompagnon.ui.DiceRollResult
import com.jc2.jdrcompagnon.ui.DiceState
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiceOverlay(
    currentWorld: WorldState? = null,
) {
    val diceState by GameState.diceState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val isDndWorld = currentWorld?.id == "donjon_et_dragon"

    // État UI unique pour le dé (source of truth) - moved to top-level composable
    data class DiceUiState(
        val isRolling: Boolean = false,
        val rotationDegrees: Float = 0f
    )
    val uiState = remember { mutableStateOf(DiceUiState()) }

    // Coroutine scope pour lancer la coroutine de reset
    val coroutineScope = rememberCoroutineScope()

    // Animation de rotation fluide
    val animatedRotation by animateFloatAsState(
        targetValue = uiState.value.rotationDegrees,
        animationSpec = tween(durationMillis = 600),
        label = "dice_rotation"
    )

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

    // Lancer la séquence de dés - with isRolling guard to prevent rapid clicks
    fun rollDiceSequence() {
        if (uiState.value.isRolling) return
        
        // Reset visibility to allow re-triggering the LaunchedEffect timer
        GameState.hideDiceResult()
        GameState.hidePoolResult()

        // Mark as rolling immediately and start animation
        uiState.value = uiState.value.copy(
            isRolling = true,
            rotationDegrees = uiState.value.rotationDegrees + 720f
        )
        
        // Light haptic for tap
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (ignored: Exception) {
            // Ignore
        }

        // Coroutine sequence: animate rotation -> roll -> reset state
        coroutineScope.launch {
            delay(600.milliseconds) // Attendre la fin de la rotation
            
            // Roll the dice (update GameState)
            if (diceState.dicePool.isEmpty()) {
                GameState.rollDice(diceState.defaultSides)
            } else {
                GameState.rollDicePool()
            }
            
            uiState.value = uiState.value.copy(isRolling = false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Le Bouton du Dé (Positionné absolument en bas à droite)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 100.dp)
                    // Use only pointerInput to handle both tap and long press without conflict
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                rollDiceSequence()
                            },
                            onLongPress = {
                                if (!uiState.value.isRolling) {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {
                                        // Ignore haptic errors
                                    }
                                    GameState.setDiceOverlayVisible(visible = true)
                                }
                            }
                        )
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .rotate(animatedRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.value.isRolling) Icons.Filled.Refresh else Icons.Filled.Casino,
                        contentDescription = "Lancer les dés",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 2. Résultats du dé (S'affichent par DESSUS le bouton si besoin)
        if (diceState.showResult && (diceState.lastResult != null) && diceState.dicePool.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DiceResultDisplay(
                    result = diceState.lastResult ?: 1,
                    sides = diceState.lastSides
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
                    total = diceState.lastPoolTotal
                )
            }
        }

        // 3. Dialogue des paramètres
        if (diceState.overlayVisible) {
            DiceSettingsDialog(
                dicePool = diceState.dicePool,
                modifier = diceState.modifier,
                advantage = diceState.advantage,
                disadvantage = diceState.disadvantage,
                isDndWorld = isDndWorld,
                onDismiss = { GameState.setDiceOverlayVisible(visible = false) },
                onAddDice = { sides: Int -> GameState.addDiceToPool(sides) },
                onRemoveDice = { sides: Int -> GameState.removeDiceFromPool(sides) },
                onIncrementModifier = { GameState.incrementModifier() },
                onDecrementModifier = { GameState.decrementModifier() },
                onSetAdvantage = { enabled: Boolean -> GameState.setAdvantage(enabled) },
                onSetDisadvantage = { enabled: Boolean -> GameState.setDisadvantage(enabled) },
            ) { GameState.resetDicePool() }
        }
    }
}

@Composable
fun DiceResultDisplay(result: Int, sides: Int) {
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
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, Color.Transparent),
                ),
            )
        ) {}

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "d$sides",
                color = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DicePoolResultDisplay(results: List<DiceRollResult>, modifier: Int, total: Int) {
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
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5
            ) {
                results.forEach { roll ->
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
                }
            }
            
            if (modifier != 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Modificateur: ${if (modifier > 0) "+$modifier" else modifier}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    ),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceSettingsDialog(
    dicePool: List<DicePoolEntry>,
    modifier: Int,
    advantage: Boolean,
    disadvantage: Boolean,
    isDndWorld: Boolean,
    onDismiss: () -> Unit,
    onAddDice: (Int) -> Unit,
    onRemoveDice: (Int) -> Unit,
    onIncrementModifier: () -> Unit,
    onDecrementModifier: () -> Unit,
    onSetAdvantage: (Boolean) -> Unit,
    onSetDisadvantage: (Boolean) -> Unit,
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
                        text = "Pool de dés",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onReset,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Réinitialiser",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                    listOf(4, 6, 8, 10, 12, 20, 100).forEach { sides ->
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
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "Modificateur total",
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
                
                if (isDndWorld) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Options D&D 5e",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = advantage,
                            onClick = { onSetAdvantage(!advantage) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text("Avantage") }
                        )
                        SegmentedButton(
                            selected = disadvantage,
                            onClick = { onSetDisadvantage(!disadvantage) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("Désavantage") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lancer les dés", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

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