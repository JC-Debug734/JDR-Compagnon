package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.PlayerConnectionState
import com.jc2.jdrcompagnon.network.SessionRole
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Barre de menu globale affichée en bas de tous les écrans
 * (câblée une seule fois dans JdrNavGraph, comme DiceOverlay).
 *
 * - Connexion : outils LAN (héberger si MJ, rejoindre si Joueur)
 * - Bibliothèque : Route.Library
 * - Home : tap = retour à l'écran d'accueil du rôle courant (MJ/Joueur) ;
 *   appui long de 3 secondes = retour à l'écran de choix des rôles — c'est
 *   le SEUL moyen d'y retourner (le retour arrière système est bloqué
 *   côté NavGraph pour ce trajet).
 * - Dé : lance directement les dés au tap, appui long = réglages avancés.
 */
// Palette de la barre du bas — source unique de vérité : ForcedDarkPalette
// dans Theme.kt, partagée avec MainActivity/RoleSelectionScreen, pour que
// toute l'app garde un même thème sombre bleu-nuit, quel que soit le thème
// Material sélectionné.

@Composable
fun AppBottomBar(
    onNavigateConnection: () -> Unit,
    onNavigateLibrary: () -> Unit,
    onHomeTap: () -> Unit,
    onHomeLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val diceState by GameState.diceState.collectAsState()
    val sessionRole by NetworkSessionManager.role.collectAsState()
    val playerState by NetworkSessionManager.playerState.collectAsState()
    val isConnectionActive = sessionRole == SessionRole.HOST || playerState == PlayerConnectionState.CONNECTED
    // Vert cohérent avec le point "Serveur actif" de HostScreen (0xFF2E7D32)
    val connectionColor = if (isConnectionActive) Color(0xFF2E7D32) else Color(0xFFC62828)
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isRolling by remember { mutableStateOf(false) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(durationMillis = 600),
        label = "dice_rotation_bottom_bar"
    )

    fun rollDice() {
        if (isRolling) return

        GameState.hideDiceResult()
        GameState.hidePoolResult()

        isRolling = true
        rotationDegrees += 720f

        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {
            // Ignore les erreurs haptiques
        }

        coroutineScope.launch {
            delay(600)
            if (diceState.dicePool.isEmpty()) {
                GameState.rollDice(diceState.defaultSides)
            } else {
                GameState.rollDicePool()
            }
            isRolling = false
        }
    }

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForcedDarkPalette.AccentGold,
        selectedTextColor = ForcedDarkPalette.AccentGold,
        unselectedIconColor = ForcedDarkPalette.Content,
        unselectedTextColor = ForcedDarkPalette.Content,
        indicatorColor = ForcedDarkPalette.Indicator,
    )

    NavigationBar(
        modifier = modifier,
        containerColor = ForcedDarkPalette.Surface,
        contentColor = ForcedDarkPalette.Content,
    ) {
        NavigationBarItem(
            selected = false,
            onClick = {}, // Le tap/appui long est géré manuellement ci-dessous
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Accueil (appui long : choix du rôle)",
                    modifier = Modifier
                        // Tap = accueil du rôle courant ; appui maintenu 1,5s = choix des rôles.
                        // Détection manuelle (pas de long-press système par défaut,
                        // qui ne dure qu'environ 500ms) via une coroutine minutée
                        // qu'on annule si le doigt est relâché avant la fin.
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    var longPressTriggered = false
                                    val longPressJob = coroutineScope.launch {
                                        delay(1500)
                                        longPressTriggered = true
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {
                                            // Ignore les erreurs haptiques
                                        }
                                        onHomeLongPress()
                                    }
                                    val released = tryAwaitRelease()
                                    longPressJob.cancel()
                                    if (released && !longPressTriggered) {
                                        onHomeTap()
                                    }
                                }
                            )
                        }
                )
            },
            label = { Text("Accueil") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateConnection,
            icon = {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = if (isConnectionActive) "Connexion active" else "Connexion inactive",
                    tint = connectionColor
                )
            },
            label = { Text("Connexion") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateLibrary,
            icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Bibliothèque") },
            label = { Text("Bibliothèque") },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = false,
            onClick = { rollDice() },
            icon = {
                // La zone de détection de l'appui long est volontairement plus
                // grande que l'icône elle-même (56dp au lieu de 32dp) : avant,
                // le geste n'était posé que sur l'icône, ce qui rendait la
                // zone d'appui beaucoup plus petite que celle des 3 autres
                // items (qui bénéficient de toute la surface de l'item).
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    if (!isRolling) {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {
                                            // Ignore les erreurs haptiques
                                        }
                                        GameState.setDiceOverlayVisible(visible = true)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRolling) Icons.Default.Refresh else Icons.Default.Casino,
                        contentDescription = "Lancer les dés (appui long : réglages)",
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(animatedRotation)
                    )
                }
            },
            label = { Text("Dé") },
            colors = itemColors,
        )
    }
}