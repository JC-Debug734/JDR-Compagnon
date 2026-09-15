package com.jc2.jdrcompagnon.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jc2.jdrcompagnon.BuildConfig
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.Breakpoints
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import com.jc2.jdrcompagnon.update.UpdateManager
import com.jc2.jdrcompagnon.update.UpdateUiState
import kotlinx.coroutines.launch

// ========================================================================
// WorldBadge — AssistChip M3 for the TopAppBar
// ========================================================================

/**
 * World badge displayed in the TopAppBar as a clickable AssistChip.
 * Shows the current world name + icon, or "Choisir un monde" if null.
 */
@Composable
fun WorldBadge(
    world: WorldState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = when (world?.id) {
        "donjon_et_dragon" -> Icons.Filled.Shield
        "naheulbeuk" -> Icons.Filled.Landscape
        else -> Icons.Filled.Public
    }
    val label = world?.name ?: stringResource(R.string.world_badge_choose)

    AssistChip(
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = "${label} badge",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier,
    )
}

// ========================================================================
// RoleCard — clickable M3 Card, no CTA button, arrow in bottom-right
// ========================================================================

data class RoleCardData(
    val label: String,
    val description: String,
    val features: String,
    val iconRes: Int,
    val color: Color,
    val onClick: () -> Unit,
)

/**
 * Role selection card: grande icône cliquable, sans carte ni texte.
 */
@Composable
fun RoleCard(
    data: RoleCardData,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clickable(onClick = data.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = data.iconRes),
            // Le nom du rôle reste porté par contentDescription pour
            // l'accessibilité, même s'il n'est plus affiché en texte.
            contentDescription = data.label,
            modifier = Modifier.size(168.dp),
        )
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
                color = MaterialTheme.colorScheme.primary,
            ),
            textAlign = TextAlign.Center,
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
) {
    if (playerName == null) {
        NameEntryDialog(
            onNameConfirmed = onNameConfirmed,
            onGenerateRandomName = onGenerateRandomName,
        )
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val isCompact = Breakpoints.isCompactWidth()

    // ── Mises à jour (version.txt sur GitHub, téléchargement manuel sur Drive) ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateState by UpdateManager.state.collectAsState()
    val lastRemoteVersion by UpdateManager.lastRemoteVersion.collectAsState()

    // Vérification automatique et silencieuse au lancement de l'écran.
    LaunchedEffect(Unit) {
        if (updateState is UpdateUiState.Idle) {
            UpdateManager.checkForUpdate(context)
        }
    }

    val roleCards = listOf(
        RoleCardData(
            label = stringResource(R.string.role_mj_label),
            description = stringResource(R.string.role_mj_description),
            features = stringResource(R.string.role_mj_features),
            iconRes = R.drawable.ic_mj,
            color = MaterialTheme.colorScheme.primary,
            onClick = onSelectMj,
        ),
        RoleCardData(
            label = stringResource(R.string.role_player_label),
            description = stringResource(R.string.role_player_description),
            features = stringResource(R.string.role_player_features),
            iconRes = R.drawable.ic_joueur,
            color = MaterialTheme.colorScheme.secondary,
            onClick = onSelectJoueur,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (lastRemoteVersion != null) "distante v$lastRemoteVersion" else "distante —",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val current = updateState
                                if (current is UpdateUiState.UpdateAvailable) {
                                    UpdateManager.openDownloadPage(context, current.release)
                                } else {
                                    scope.launch {
                                        val found = UpdateManager.checkForUpdate(context)
                                        if (!found) {
                                            Toast.makeText(context, "Aucune mise à jour disponible", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = updateState is UpdateUiState.Idle || updateState is UpdateUiState.UpdateAvailable,
                        ) {
                            when (updateState) {
                                is UpdateUiState.Checking -> {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                                is UpdateUiState.UpdateAvailable -> {
                                    Icon(
                                        imageVector = Icons.Filled.SystemUpdate,
                                        contentDescription = "Télécharger la mise à jour",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Vérifier les mises à jour",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    WorldBadge(
                        world = currentWorld,
                        onClick = onSelectContext,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    // Fond forcé : le thème ne doit pas influencer la couleur
                    // de fond, quelle que soit sa configuration.
                    containerColor = ForcedDarkPalette.Background,
                ),
            )
        },
        containerColor = ForcedDarkPalette.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Hero section (delay 100ms, 200ms) ──
            val worldCoverRes = when (currentWorld?.id) {
                // TODO: image temporairement retirée (fichier PNG invalide, à corriger puis remettre)
                // "donjon_et_dragon" -> R.drawable.dnd_cover_image
                "naheulbeuk" -> R.drawable.naheulbeuk
                else -> null
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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Role cards (delay 300ms / 380ms, 250ms each) ──
            if (isCompact) {
                // Compact: vertical Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    roleCards.forEachIndexed { index, roleData ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    delayMillis = 300 + index * 80,
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    delayMillis = 300 + index * 80,
                                ),
                                initialOffsetY = { it / 4 },
                            ),
                        ) {
                            RoleCard(data = roleData)
                        }
                    }
                }
            } else {
                // Expanded: horizontal Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    roleCards.forEachIndexed { index, roleData ->
                        AnimatedVisibility(
                            visible = visible,
                            modifier = Modifier.weight(1f),
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    delayMillis = 300 + index * 80,
                                )
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    delayMillis = 300 + index * 80,
                                ),
                                initialOffsetY = { it / 4 },
                            ),
                        ) {
                            RoleCard(data = roleData)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

        }
    }
}