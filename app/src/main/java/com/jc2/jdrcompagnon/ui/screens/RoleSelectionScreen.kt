package com.jc2.jdrcompagnon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.Breakpoints

// ========================================================================
// RoleCard — clickable M3 Card, no CTA button, arrow in bottom-right
// ========================================================================

// Couleur unique pour toutes les cases (rôles + univers + réglages), pour que le bloc
// de boutons empilés forme un ensemble visuellement homogène plutôt que des cases de
// couleurs différentes par entrée.
internal val RoleSelectionColor = Color(0xFFE0B84C)

@Composable
fun RoleSelectionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = RoleSelectionColor,
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .border(2.dp, color, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

// ========================================================================
// HeroSection — title (HeroTitle/primary) + optional subtitle
// ========================================================================

/**
 * Hero section: displays the app title in HeroTitle/primary and an optional
 * subtitle in headlineMedium/onSurfaceVariant. Centered horizontally.
 */
@Composable
fun HeroSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                color = Color.White,
            ),
            textAlign = TextAlign.Center,
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ========================================================================
// NameEntryDialog — demande le nom du joueur au premier lancement
// ========================================================================

/**
 * Dialogue bloquant demandant le nom du joueur. Affiché tant qu'aucun
 * nom (choisi ou généré) n'a été enregistré dans GameState.
 *
 * - "Valider" : enregistre le nom saisi (ignoré si vide/blanc)
 * - "Générer un pseudo" : laisse GameState choisir un pseudo aléatoire
 */
@Composable
fun NameEntryDialog(
    onNameConfirmed: (String) -> Unit,
    onGenerateRandomName: () -> Unit,
) {
    var nameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Saisie obligatoire : pas de fermeture par tap extérieur */ },
        title = { Text("Bienvenue, aventurier !") },
        text = {
            Column {
                Text(
                    text = "Comment souhaites-tu être appelé ?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Ton nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onNameConfirmed(nameInput) },
                enabled = nameInput.isNotBlank(),
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onGenerateRandomName) {
                Text("Générer un pseudo")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    currentWorld: WorldState?,
    playerName: String?,
    onNameConfirmed: (String) -> Unit,
    onGenerateRandomName: () -> Unit,
    onSelectMj: () -> Unit,
    onSelectJoueur: () -> Unit,
    onSelectContext: () -> Unit,
    onSelectSettings: () -> Unit = {},
) {
    if (playerName == null) {
        NameEntryDialog(
            onNameConfirmed = onNameConfirmed,
            onGenerateRandomName = onGenerateRandomName,
        )
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ── Message de bienvenue (nom du joueur persistant) ──
                if (playerName != null) {
                    Text(
                        text = "Bienvenue, $playerName !",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Hero section (delay 100ms, 200ms) ──
                // Any? car le modèle Coil accepte aussi bien un id de drawable (mondes
                // intégrés) qu'un File (fond d'écran d'un univers importé, voir
                // WorldState.backgroundImagePath / CustomWorldsRepository).
                val worldCoverRes: Any? = when (currentWorld?.id) {
                    // TODO: image temporairement retirée (fichier PNG invalide, à corriger puis remettre)
                    // "donjon_et_dragon" -> R.drawable.dnd_cover_image
                    "naheulbeuk" -> R.drawable.naheulbeuk
                    else -> currentWorld?.backgroundImagePath?.let { java.io.File(it) }
                }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 100)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 200, delayMillis = 100),
                                initialOffsetY = { it / 4 },
                            ),
                ) {
                    if (worldCoverRes != null) {
                        // Bandeau visuel lié au monde actif
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(MaterialTheme.shapes.large),
                        ) {
                            AsyncImage(
                                model = worldCoverRes,
                                contentDescription = "Couverture ${currentWorld?.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Text(
                                    text = stringResource(R.string.role_selection_title),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HeroSection(
                                title = stringResource(R.string.role_selection_title),
                                subtitle = "",
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ── Bottom bar with stacked rectangles for roles and settings ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Role selection stacked rectangles (même couleur pour toutes les cases)
                RoleSelectionButton(
                    label = "Maître du Jeu",
                    onClick = onSelectMj,
                    modifier = Modifier.fillMaxWidth(),
                )
                RoleSelectionButton(
                    label = "Joueur",
                    onClick = onSelectJoueur,
                    modifier = Modifier.fillMaxWidth(),
                )
                RoleSelectionButton(
                    label = "Univers",
                    onClick = onSelectContext,
                    modifier = Modifier.fillMaxWidth(),
                )
                RoleSelectionButton(
                    label = "Réglages",
                    onClick = onSelectSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}