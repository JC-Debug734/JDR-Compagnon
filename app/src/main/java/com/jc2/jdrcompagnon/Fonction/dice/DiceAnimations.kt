package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.jc2.jdrcompagnon.ui.AdvantageState

/**
 * Particule d'étincelle pour les animations de dés critiques
 */
@Composable
fun SparkleParticle(
    color: Color = Color(0xFFFFD700),
    size: Float = 6f,
    delayMillis: Int = 0,
    durationMillis: Int = 800
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .background(color, CircleShape)
    )
}

/**
 * Explosion d'étincelles pour un coup critique (20)
 */
@Composable
fun CriticalHitExplosion(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false
) {
    if (!isVisible) return

    val particles = remember { List(12) { index ->
        val angle = (index * 30f) * (Math.PI / 180f)
        val distance = Random.nextFloat() * 60f + 20f
        Triple(
            cos(angle).toFloat() * distance,
            sin(angle).toFloat() * distance,
            Random.nextFloat() * 300f + 200f
        )
    } }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        particles.forEachIndexed { index, (dx, dy, duration) ->
            val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
            val animAlpha = remember { Animatable(1f) }

            LaunchedEffect(isVisible) {
                launch {
                    animOffset.animateTo(
                        targetValue = Offset(dx, dy),
                        animationSpec = tween(duration.toInt(), easing = LinearOutSlowInEasing)
                    )
                }
                launch {
                    animAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(duration.toInt(), easing = LinearEasing)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(animOffset.value.x.dp, animOffset.value.y.dp)
                    .alpha(animAlpha.value)
                    .size((Random.nextFloat() * 4f + 3f).dp)
                    .background(
                        when (index % 4) {
                            0 -> Color(0xFFFFD700) // Or
                            1 -> Color(0xFFFF6D00) // Orange
                            2 -> Color(0xFFFF1744) // Rouge
                            else -> Color(0xFFFFFF00) // Jaune
                        },
                        CircleShape
                    )
            )
        }
    }
}

/**
 * Effet de tremblement d'écran pour les coups critiques
 */
@Composable
fun Modifier.shakeEffect(
    enabled: Boolean = false,
    intensity: Float = 8f
): Modifier = if (enabled) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            repeat(10) {
                shake.animateTo(
                    targetValue = if (it % 2 == 0) intensity else -intensity,
                    animationSpec = tween(50)
                )
            }
            shake.animateTo(0f, tween(100))
        }
    }
    this.graphicsLayer {
        translationX = shake.value
    }
} else {
    this
}

/**
 * Effet de pulsation pour les modificateurs de dés
 */
@Composable
fun Modifier.pulseEffect(
    enabled: Boolean = false,
    scale: Float = 1.2f
): Modifier = if (enabled) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    this.scale(scaleAnim)
} else {
    this
}

/**
 * Aura magique autour du résultat du dé
 */
@Composable
fun DiceResultAura(
    modifier: Modifier = Modifier,
    result: Int,
    sides: Int = 20
) {
    val isCritical = result == 20 && sides == 20
    val isFumble = result == 1 && sides == 20

    val auraColor = when {
        isCritical -> Color(0xFFFFD700)
        isFumble -> Color(0xFFB71C1C)
        result >= (sides * 0.75) -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    Box(
        modifier = modifier
            .size(220.dp)
            .scale(scale)
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    listOf(
                        auraColor.copy(alpha = 0.5f),
                        auraColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )
}

/**
 * Indicateur visuel d'avantage/désavantage
 */
@Composable
fun AdvantageBadge(state: AdvantageState) {
    if (state == AdvantageState.NORMAL) return

    val color = when (state) {
        AdvantageState.ADVANTAGE -> Color(0xFF4CAF50)
        AdvantageState.DISADVANTAGE -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    val label = when (state) {
        AdvantageState.ADVANTAGE -> "AV"
        AdvantageState.DISADVANTAGE -> "DA"
        else -> ""
    }

    val infiniteTransition = rememberInfiniteTransition(label = "adv_indicator")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "adv_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .background(color, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Indicateur textuel complet d'avantage/désavantage (pour les dialogues)
 */
@Composable
fun AdvantageIndicator(
    hasAdvantage: Boolean = false,
    hasDisadvantage: Boolean = false
) {
    if (!hasAdvantage && !hasDisadvantage) return

    val color = when {
        hasAdvantage -> Color(0xFF4CAF50)
        hasDisadvantage -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    val label = when {
        hasAdvantage -> "✦ Avantage"
        hasDisadvantage -> "✦ Désavantage"
        else -> ""
    }

    val infiniteTransition = rememberInfiniteTransition(label = "adv_indicator")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "adv_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .background(color, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
