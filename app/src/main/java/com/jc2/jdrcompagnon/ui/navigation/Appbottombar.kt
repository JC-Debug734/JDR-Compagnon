package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.graphics.BitmapFactory
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
 * - Home : tap = retour à l'écran d'accueil du rôle courant (MJ/Joueur).
 *   Le retour au choix de rôle se fait désormais via "Changer de rôle"
 *   dans le menu latéral (AppDrawer), plus par appui long ici.
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

    // Icône personnalisée "Accueil", fournie en asset (pas en drawable) pour permettre
    // plus tard une icône différente par monde ; repli silencieux sur l'icône Material
    // si le fichier est absent, pour ne jamais casser la barre du bas.
    val context = LocalContext.current
    val homeIconBitmap = remember {
        runCatching {
            context.assets.open("dnd/icone/ic_acceuil.png").use { BitmapFactory.decodeStream(it) }
                ?.asImageBitmap()
        }.getOrNull()
    }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(durationMillis = 600),
        label = "dice_rotation_bottom_bar"
    )

    fun rollDice() {
        if (isRolling) return
        // Un seul lancer à la fois : tant que le résultat précédent est affiché, l'appui le
        // ferme seulement (comme un appui ailleurs sur l'écran) ; il faut un nouvel appui,
        // résultat fermé, pour relancer. Évite une rafale de lancers en tapotant le dé.
        val state = GameState.diceState.value
        val resultatAffiche = (state.showResult && state.lastResult != null && state.dicePool.isEmpty()) ||
            (state.showPoolResult && state.lastPoolResults.isNotEmpty())
        if (resultatAffiche) {
            GameState.hideDiceResult()
            GameState.hidePoolResult()
            return
        }

        isRolling = true
        rotationDegrees += 720f

        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {
            // Ignore les erreurs haptiques
        }

        com.jc2.jdrcompagnon.ui.components.SonsDes.lancer()
        coroutineScope.launch {
            delay(600)
            // Résultat : tintement (après le roulement), plus éclatant sur un 20 naturel.
            val d20 = if (diceState.dicePool.isEmpty()) {
                val r = GameState.rollDice(diceState.defaultSides)
                r.takeIf { diceState.defaultSides == 20 }
            } else {
                GameState.rollDicePool().firstOrNull { it.sides == 20 && !it.isDiscarded }?.value
            }
            com.jc2.jdrcompagnon.ui.components.SonsDes.resultat(critique = d20 == 20, echec = d20 == 1)
            isRolling = false
        }
    }

    // Libellés et pastille de sélection dorés, assortis aux icônes illustrées.
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Unspecified,
        selectedTextColor = IconesAsset.TexteDore,
        unselectedIconColor = Color.Unspecified,
        unselectedTextColor = IconesAsset.TexteDore,
        indicatorColor = ForcedDarkPalette.AccentGold.copy(alpha = 0.18f),
    )
    val iconeAccueil = rememberIconeAsset(IconesAsset.BARRE_ACCUEIL)
    val iconeConnexion = rememberIconeAsset(IconesAsset.BARRE_CONNEXION)
    val iconeBibliotheque = rememberIconeAsset(IconesAsset.BARRE_BIBLIOTHEQUE)
    val iconeDe = rememberIconeAsset(IconesAsset.BARRE_DE)
    // Icônes seules, sans libellé : elles occupent toute la hauteur de la barre.
    val tailleIcone = 56.dp

    Column(modifier = modifier) {
        // Liseré doré en haut de la barre, comme le cadre des illustrations.
        HorizontalDivider(thickness = 1.dp, color = IconesAsset.LisereDore)
        NavigationBar(
            // Fond uni bleu nuit, celui des icônes illustrées : elles s'y fondent sans cadre visible.
            containerColor = IconesAsset.FondIcones,
            contentColor = IconesAsset.TexteDore,
        ) {
            NavigationBarItem(
                selected = false,
                onClick = onHomeTap,
                icon = {
                    val bitmap = iconeAccueil ?: homeIconBitmap
                    if (bitmap != null) {
                        Icon(bitmap = bitmap, contentDescription = "Accueil", tint = Color.Unspecified, modifier = Modifier.size(tailleIcone))
                    } else {
                        Icon(Icons.Default.Home, contentDescription = "Accueil", tint = IconesAsset.TexteDore)
                    }
                },
                colors = itemColors,
            )
            NavigationBarItem(
                selected = false,
                onClick = onNavigateConnection,
                icon = {
                    val description = if (isConnectionActive) "Connexion active" else "Connexion inactive"
                    Box {
                        if (iconeConnexion != null) {
                            Icon(bitmap = iconeConnexion, contentDescription = description, tint = Color.Unspecified, modifier = Modifier.size(tailleIcone))
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = description, tint = connectionColor)
                        }
                        // Pastille d'état de la connexion (vert : partie en cours, rouge : aucune).
                        if (iconeConnexion != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(11.dp)
                                    .background(connectionColor, CircleShape)
                                    .border(1.5.dp, IconesAsset.FondIcones, CircleShape)
                            )
                        }
                    }
                },
                colors = itemColors,
            )
            NavigationBarItem(
                selected = false,
                onClick = onNavigateLibrary,
                icon = {
                    if (iconeBibliotheque != null) {
                        Icon(bitmap = iconeBibliotheque, contentDescription = "Bibliothèque", tint = Color.Unspecified, modifier = Modifier.size(tailleIcone))
                    } else {
                        Icon(Icons.Default.LibraryBooks, contentDescription = "Bibliothèque", tint = IconesAsset.TexteDore)
                    }
                },
                colors = itemColors,
            )
            NavigationBarItem(
                selected = false,
                onClick = { rollDice() },
                icon = {
                    val modifierDe = Modifier
                        .size(tailleIcone)
                        .rotate(animatedRotation)
                        .pointerInput(Unit) {
                            // Ce détecteur consomme aussi les appuis simples sur l'icône (qui
                            // occupe presque toute la case) : sans onTap, ils n'atteignaient
                            // jamais le onClick de la case et le dé semblait « bloqué ».
                            detectTapGestures(
                                onTap = { rollDice() },
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
                        }
                    if (iconeDe != null) {
                        // Le d20 tourne pendant le lancer (pas d'icône « en cours » à part).
                        Icon(bitmap = iconeDe, contentDescription = "Lancer les dés (appui long : réglages)", tint = Color.Unspecified, modifier = modifierDe)
                    } else {
                        Icon(
                            imageVector = if (isRolling) Icons.Default.Refresh else Icons.Default.Casino,
                            contentDescription = "Lancer les dés (appui long : réglages)",
                            tint = IconesAsset.TexteDore,
                            modifier = modifierDe,
                        )
                    }
                },
                colors = itemColors,
            )
        }
    }
}
