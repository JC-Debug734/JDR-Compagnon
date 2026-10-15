package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextDecoration
import com.jc2.jdrcompagnon.ui.AdvantageState
import com.jc2.jdrcompagnon.ui.DicePoolEntry
import com.jc2.jdrcompagnon.ui.DiceRollResult
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.delay

import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiceOverlay(
    currentWorld: WorldState? = null,
) {
    val diceState by GameState.diceState.collectAsState()
    val isDndWorld = currentWorld?.id == "donjon_et_dragon"

    val skinColors = CouleursDes

    // Le résultat reste affiché jusqu'à un appui n'importe où sur l'écran (plus de disparition
    // automatique au bout de quelques secondes).
    val resultatAffiche = (diceState.showResult && diceState.lastResult != null && diceState.dicePool.isEmpty()) ||
        (diceState.showPoolResult && diceState.lastPoolResults.isNotEmpty())

    Box(modifier = Modifier.fillMaxSize()) {
        // Couche qui capte l'appui de fermeture tant qu'un résultat est affiché.
        if (resultatAffiche) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            GameState.hideDiceResult()
                            GameState.hidePoolResult()
                        }
                    }
            )
        }
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
                isDndWorld = isDndWorld,
                onDismiss = { GameState.setDiceOverlayVisible(visible = false) },
                onAddDice = { sides: Int -> GameState.addDiceToPool(sides) },
                onRemoveDice = { sides: Int -> GameState.removeDiceFromPool(sides) },
                onIncrementModifier = { GameState.incrementModifier() },
                onDecrementModifier = { GameState.decrementModifier() },
                onSetAdvantageState = { state: AdvantageState -> GameState.setAdvantageState(state) },
                onToggleSound = { GameState.toggleSound() },
            ) { GameState.resetDicePool() }
        }
    }
}

@Composable
fun DiceResultDisplay(
    result: Int,
    sides: Int,
    advantageState: AdvantageState = AdvantageState.NORMAL,
    skinColors: DiceSkinColors = CouleursDes
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
            verticalAlignment = Alignment.Top,
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

/** Formule du jet : « 2d6 + 1d20 + 3 » (dés regroupés par type, modificateur à la fin). */
fun formuleDuJet(results: List<DiceRollResult>, modifier: Int): String {
    val des = results.groupBy { it.sides }.toSortedMap().map { (faces, lances) -> "${lances.size}d$faces" }
    val mod = when {
        modifier > 0 -> listOf("+ $modifier")
        modifier < 0 -> listOf("− ${-modifier}")
        else -> emptyList()
    }
    return (listOf(des.joinToString(" + ")) + mod).joinToString(" ")
}

/** Détail du calcul : « 4 + 6 + 15 + 3 = 28 » (dés retenus puis modificateur) ; null pour un seul dé sans modificateur. */
fun detailDuJet(results: List<DiceRollResult>, modifier: Int, total: Int): String? {
    val termes = results.filterNot { it.isDiscarded }.map { it.value.toString() } +
        listOfNotNull(modifier.takeIf { it != 0 }?.let { if (it > 0) "+ $it" else "− ${-it}" })
    val somme = termes.mapIndexed { i, t -> if (i == 0 || t.startsWith("+") || t.startsWith("−")) t else "+ $t" }.joinToString(" ")
    return if (termes.size <= 1) null else "$somme = $total"
}

/**
 * Résultat d'un jet de plusieurs dés (ou d'un dé avec modificateur) : la fenêtre s'adapte à son
 * contenu, et indique les dés lancés (« 2d6 + 1d20 + 3 »), l'avantage éventuel, chaque dé (le d20
 * écarté par l'avantage barré) et le détail du calcul jusqu'au total.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DicePoolResultDisplay(
    results: List<DiceRollResult>,
    modifier: Int,
    total: Int,
    advantageState: AdvantageState = AdvantageState.NORMAL,
    skinColors: DiceSkinColors = CouleursDes
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore),
        tonalElevation = 12.dp,
        modifier = Modifier
            .padding(24.dp)
            .widthIn(min = 200.dp, max = 340.dp)
    ) {
        Column(
            // Pas de largeur imposée : la fenêtre prend la taille de son contenu.
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RÉSULTAT",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = skinColors.primary
            )
            // Ce qui a été lancé.
            Text(
                text = formuleDuJet(results, modifier),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (advantageState != AdvantageState.NORMAL && results.any { it.isDiscarded }) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AdvantageBadge(state = advantageState)
                    Text(
                        if (advantageState == AdvantageState.ADVANTAGE) "meilleur d20 gardé" else "pire d20 gardé",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 5
            ) {
                results.forEach { roll ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(if (roll.isDiscarded) 0.4f else 1f)
                    ) {
                        Surface(
                            color = if (roll.isDiscarded)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else
                                skinColors.primary.copy(alpha = 0.25f),
                            shape = CircleShape,
                            border = if (roll.isCritical && !roll.isDiscarded) BorderStroke(1.dp, skinColors.primary) else null,
                        ) {
                            Text(
                                text = roll.value.toString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textDecoration = if (roll.isDiscarded) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                        Text(
                            text = "d${roll.sides}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(modifier = Modifier.width(140.dp), thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)
            Spacer(modifier = Modifier.height(6.dp))
            // D'où vient le total : dés retenus et modificateur.
            detailDuJet(results, modifier, total)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceSettingsDialog(
    dicePool: List<DicePoolEntry>,
    modifier: Int,
    advantageState: AdvantageState,
    soundEnabled: Boolean,
    isDndWorld: Boolean,
    onDismiss: () -> Unit,
    onAddDice: (Int) -> Unit,
    onRemoveDice: (Int) -> Unit,
    onIncrementModifier: () -> Unit,
    onDecrementModifier: () -> Unit,
    onSetAdvantageState: (AdvantageState) -> Unit,
    onToggleSound: () -> Unit,
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
            color = ForcedDarkPalette.Surface,
            contentColor = ForcedDarkPalette.Content,
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
                            containerColor = ForcedDarkPalette.Indicator
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Réinitialiser (garde 1d20 par défaut)",
                            tint = ForcedDarkPalette.Content
                        )
                    }
                }

                if (isDndWorld) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Avantage / Désavantage",
                        style = MaterialTheme.typography.labelLarge,
                        color = ForcedDarkPalette.AccentGold
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
                                    color = if (advSelected) Color.White else ForcedDarkPalette.Content
                                )
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFF4CAF50).copy(alpha = 0.85f),
                                activeContentColor = Color.White,
                                inactiveContainerColor = ForcedDarkPalette.Background,
                                inactiveContentColor = ForcedDarkPalette.Content
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
                                    color = if (disSelected) Color.White else ForcedDarkPalette.Content
                                )
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFFF44336).copy(alpha = 0.85f),
                                activeContentColor = Color.White,
                                inactiveContainerColor = ForcedDarkPalette.Background,
                                inactiveContentColor = ForcedDarkPalette.Content
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Sons",
                    style = MaterialTheme.typography.labelLarge,
                    color = ForcedDarkPalette.AccentGold
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
                            color = ForcedDarkPalette.Content.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ForcedDarkPalette.AccentGold,
                            checkedTrackColor = ForcedDarkPalette.AccentGold.copy(alpha = 0.5f),
                            checkedBorderColor = ForcedDarkPalette.AccentGold,
                            uncheckedThumbColor = ForcedDarkPalette.Content,
                            uncheckedTrackColor = ForcedDarkPalette.Indicator,
                            uncheckedBorderColor = ForcedDarkPalette.Indicator
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(thickness = 0.5.dp, color = ForcedDarkPalette.Content.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Ajouter des dés",
                    style = MaterialTheme.typography.labelLarge,
                    color = ForcedDarkPalette.AccentGold
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
                    color = ForcedDarkPalette.AccentGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDecrementModifier,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = ForcedDarkPalette.Indicator,
                            contentColor = ForcedDarkPalette.Content
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Moins")
                    }
                    Text(
                        text = if (modifier >= 0) "+$modifier" else modifier.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    FilledTonalIconButton(
                        onClick = onIncrementModifier,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = ForcedDarkPalette.Indicator,
                            contentColor = ForcedDarkPalette.Content
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Plus")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForcedDarkPalette.AccentGold,
                        contentColor = ForcedDarkPalette.Background
                    )
                ) {
                    Text("Enregistrer", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Couleurs du dé (apparence « feu », la seule proposée). */
val CouleursDes = DiceSkinColors(
    primary = Color(0xFFFF7043),
    onPrimary = Color.White,
    secondary = Color(0xFFE64A19)
)

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
        color = if (count > 0) ForcedDarkPalette.AccentGold.copy(alpha = 0.2f) else ForcedDarkPalette.Indicator.copy(alpha = 0.5f),
        border = if (count > 0) BorderStroke(1.dp, ForcedDarkPalette.AccentGold) else null,
        contentColor = if (count > 0) ForcedDarkPalette.AccentGold else ForcedDarkPalette.Content,
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
                    color = ForcedDarkPalette.AccentGold,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = count.toString(),
                            color = ForcedDarkPalette.Background,
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
