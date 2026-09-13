package com.jc2.jdrcompagnon.ui.screens.world

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldSelectionScreen(
    onWorldSelected: (String, String, String, ImageVector, Color, Color) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    var selectedWorldId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { visible = true }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isCompact = screenWidthDp < 600
    val gridColumns = if (isCompact) 1 else 2

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = selectedWorldId?.let {
                            when {
                                it == "donjon_et_dragon" -> stringResource(R.string.world_dnd_label)
                                it == "naheulbeuk" -> stringResource(R.string.world_naheul_label)
                                else -> "CHOISIR UN MONDE"
                            }
                        } ?: "CHOISIR UN MONDE",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    )
                },
                navigationIcon = {
                    onBack?.let { backAction ->
                        IconButton(onClick = backAction) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedWorldId == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "CHOISIR UN MONDE",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sélectionnez un monde pour commencer à jouer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isCompact) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    itemsIndexed(items = listOf("dnd", "naheul"), key = { _, id -> id }) { index, worldKey ->
                        val worldId = if (worldKey == "dnd") "donjon_et_dragon" else "naheulbeuk"
                        val isSelected = selectedWorldId == worldId
                        val label = stringResource(
                            when (worldKey) {
                                "dnd" -> R.string.world_dnd_label
                                "naheul" -> R.string.world_naheul_label
                                else -> R.string.world_dnd_label
                            }
                        )
                        val description = stringResource(
                            when (worldKey) {
                                "dnd" -> R.string.world_dnd_description
                                "naheul" -> R.string.world_naheul_description
                                else -> R.string.world_dnd_description
                            }
                        )
                        val metadata = stringResource(
                            when (worldKey) {
                                "dnd" -> R.string.world_dnd_metadata
                                "naheul" -> R.string.world_naheul_metadata
                                else -> R.string.world_dnd_metadata
                            }
                        )
                        val icon = when (worldKey) {
                            "dnd" -> Icons.Filled.Shield
                            "naheul" -> Icons.Filled.Landscape
                            else -> Icons.Filled.Public
                        }
                        val primaryColor = if (worldKey == "dnd") Color(0xFFC62828) else Color(0xFF2E7D32)
                        val secondaryColor = if (worldKey == "dnd") Color(0xFFFFD54F) else Color(0xFFFFB300)
                        val cardData = WorldCardData(
                            id = worldId, label = label,
                            description = description,
                            metadata = metadata,
                            icon = icon,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )

                        WorldSelectionCard(
                            data = cardData,
                            isSelected = isSelected,
                            onClick = {
                                selectedWorldId = worldId
                                onWorldSelected(worldId, label, description, icon, primaryColor, secondaryColor)
                            },
                            animationIndex = index, animationVisible = visible,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    itemsIndexed(items = listOf("dnd", "naheul"), key = { _, id -> id }) { index, worldKey ->
                        val worldId = if (worldKey == "dnd") "donjon_et_dragon" else "naheulbeuk"
                        val isSelected = selectedWorldId == worldId
                        val label = stringResource(
                            when (worldKey) {
                                "dnd" -> R.string.world_dnd_label
                                "naheul" -> R.string.world_naheul_label
                                else -> R.string.world_dnd_label
                            }
                        )
                        val description = stringResource(
                            when (worldKey) {
                                "dnd" -> R.string.world_dnd_description
                                "naheul" -> R.string.world_naheul_description
                                else -> R.string.world_dnd_description
                            }
                        )
                        val metadata = stringResource(
                            when (worldKey) {
                                "dnd" -> R.string.world_dnd_metadata
                                "naheul" -> R.string.world_naheul_metadata
                                else -> R.string.world_dnd_metadata
                            }
                        )
                        val icon = when (worldKey) {
                            "dnd" -> Icons.Filled.Shield
                            "naheul" -> Icons.Filled.Landscape
                            else -> Icons.Filled.Public
                        }
                        val primaryColor = if (worldKey == "dnd") Color(0xFFC62828) else Color(0xFF2E7D32)
                        val secondaryColor = if (worldKey == "dnd") Color(0xFFFFD54F) else Color(0xFFFFB300)
                        val cardData = WorldCardData(
                            id = worldId, label = label,
                            description = description,
                            metadata = metadata,
                            icon = icon,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )

                        WorldSelectionCard(
                            data = cardData,
                            isSelected = isSelected,
                            onClick = {
                                selectedWorldId = worldId
                                onWorldSelected(worldId, label, description, icon, primaryColor, secondaryColor)
                            },
                            animationIndex = index, animationVisible = visible,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class WorldCardData(
    val id: String,
    val label: String,
    val description: String,
    val metadata: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
)

/**
 * World selection card with hero styling, metadata, and selection state
 */
@Composable
fun WorldSelectionCard(
    data: WorldCardData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationIndex: Int = 0,
    animationVisible: Boolean = true,
) {
    val density = LocalDensity.current

    val borderStroke = if (isSelected) {
        BorderStroke(2.dp, data.primaryColor)
    } else {
        BorderStroke(1.dp, data.primaryColor.copy(alpha = 0.2f))
    }

    val content = @Composable {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
            color = if (isSelected) data.primaryColor.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isSelected) 6.dp else 2.dp,
            border = borderStroke,
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Decorative gradient accent
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(120.dp)
                        .offset(x = 40.dp, y = (-40).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    data.primaryColor.copy(alpha = if (isSelected) 0.22f else 0.12f),
                                    Color.Transparent
                                )
                            ),
                        ),
                )

                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon container
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = data.primaryColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(56.dp),
                        border = BorderStroke(1.dp, data.primaryColor.copy(alpha = 0.2f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = data.icon,
                                contentDescription = "Icone ${data.label}",
                                tint = data.primaryColor,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Text content
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.label.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = data.metadata,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }

                    // Selection badge
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = data.secondaryColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, data.secondaryColor.copy(alpha = 0.2f)),
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                text = stringResource(R.string.world_badge_selected),
                                style = MaterialTheme.typography.labelSmall,
                                color = data.secondaryColor,
                            )
                        }
                    }
                }
            }
        }
    }

    if (animationVisible) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = animationIndex * 100)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 600, delayMillis = animationIndex * 100),
                        initialOffsetY = { with(density) { 40.dp.roundToPx() } },
                    ),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(durationMillis = 200)) +
                    androidx.compose.animation.slideOutVertically(
                        animationSpec = tween(durationMillis = 200),
                        targetOffsetY = { with(density) { -40.dp.roundToPx() } },
                    ),
        ) {
            content()
        }
    } else {
        content()
    }
}