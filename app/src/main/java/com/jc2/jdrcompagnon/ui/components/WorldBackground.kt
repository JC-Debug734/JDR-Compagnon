package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.isSystemInDarkTheme
import com.jc2.jdrcompagnon.ui.GameState
import kotlin.math.sin
import kotlin.random.Random

/**
 * Composable réutilisable qui dessine un fond d'écran selon le monde sélectionné.
 *
 * - D&D (donjon_et_dragon) : texture parchemin — gradient vertical #F5E6C8 → #D4BC7E
 *   + taches de vieillissement (drawBehind avec seed fixe) + vignette
 * - Naheulbeuk (naheulbeuk) : texture bois — gradient vertical #3E2723 → #4E342E
 *   + veinures verticales + nœuds
 * - Default : transparent (utiliser le thème)
 *
 * Toutes les textures sont dessinées procéduralement avec Modifier.drawBehind (pas de bitmap).
 *
 * @param content Le contenu à afficher par-dessus le fond
 */
@Composable
fun WorldBackground(content: @Composable () -> Unit) {
    val currentWorld by GameState.currentWorld.collectAsState()
    val isDark = isSystemInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentWorld?.id) {
            "donjon_et_dragon" -> ParchmentBackground(isDark = isDark)
            "naheulbeuk" -> WoodBackground(isDark = isDark)
            else -> {}
        }
        content()
    }
}

// =====================================================
// PARCHemin (D&D)
// =====================================================

@Composable
private fun ParchmentBackground(isDark: Boolean) {
    val modifier = Modifier.fillMaxSize().drawBehind {
        drawParchmentTexture(this, isDark)
    }
    Box(modifier = modifier)
}

private fun drawParchmentTexture(scope: DrawScope, isDark: Boolean) {
    val width = scope.size.width
    val height = scope.size.height

    // Gradient vertical de base
    val topColor = if (isDark) Color(0xFF6B5A3E) else Color(0xFFF5E6C8)
    val bottomColor = if (isDark) Color(0xFF4A3D2A) else Color(0xFFD4BC7E)

    scope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = 0f,
            endY = height,
        )
    )

    // Taches de vieillissement avec seed fixe (reproductible)
    val random = Random(seed = 42)
    val stainCount = 25
    val stainColor = if (isDark) Color(0xFF3D2E1A) else Color(0xFFB89968)

    repeat(stainCount) {
        val cx = random.nextFloat() * width
        val cy = random.nextFloat() * height
        val radius = random.nextFloat() * 40f + 15f
        val alpha = random.nextFloat() * 0.15f + 0.05f

        scope.drawCircle(
            color = stainColor.copy(alpha = alpha),
            radius = radius,
            center = Offset(cx, cy),
        )
    }

    // Petites taches plus concentrées (encre / eau)
    val smallStainRandom = Random(seed = 137)
    repeat(15) {
        val cx = smallStainRandom.nextFloat() * width
        val cy = smallStainRandom.nextFloat() * height
        val radius = smallStainRandom.nextFloat() * 8f + 3f
        val alpha = smallStainRandom.nextFloat() * 0.2f + 0.1f

        scope.drawCircle(
            color = stainColor.copy(alpha = alpha),
            radius = radius,
            center = Offset(cx, cy),
        )
    }

    // Vignette sur les bords
    drawVignette(scope, width, height, isDark)
}

// =====================================================
// BOIS (Naheulbeuk)
// =====================================================

@Composable
private fun WoodBackground(isDark: Boolean) {
    val modifier = Modifier.fillMaxSize().drawBehind {
        drawWoodTexture(this, isDark)
    }
    Box(modifier = modifier)
}

private fun drawWoodTexture(scope: DrawScope, isDark: Boolean) {
    val width = scope.size.width
    val height = scope.size.height

    // Gradient vertical de base
    val topColor = if (isDark) Color(0xFF2E1F1A) else Color(0xFF3E2723)
    val bottomColor = if (isDark) Color(0xFF3D2A22) else Color(0xFF4E342E)

    scope.drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = 0f,
            endY = height,
        )
    )

    // Veinures verticales (lignes ondulées)
    val grainRandom = Random(seed = 99)
    val grainColor = if (isDark) Color(0xFF1A0F0B) else Color(0xFF2E1A12)
    val grainCount = 12

    repeat(grainCount) { i ->
        val baseX = (i + 1) * (width / (grainCount + 1))
        val alpha = grainRandom.nextFloat() * 0.3f + 0.1f
        val amplitude = grainRandom.nextFloat() * 15f + 5f
        val wavelength = grainRandom.nextFloat() * 200f + 100f

        // Dessine une veine ondulée comme une série de petits cercles
        var y = 0f
        while (y < height) {
            val offsetX = amplitude * sin(y / wavelength * 2 * Math.PI).toFloat()
            val cx = baseX + offsetX
            scope.drawCircle(
                color = grainColor.copy(alpha = alpha),
                radius = 1.5f,
                center = Offset(cx, y),
            )
            y += 4f
        }
    }

    // Nœuds du bois
    val knotRandom = Random(seed = 77)
    val knotColor = if (isDark) Color(0xFF1A0F0B) else Color(0xFF2E1A12)
    val knotCount = 5

    repeat(knotCount) {
        val cx = knotRandom.nextFloat() * width
        val cy = knotRandom.nextFloat() * height
        val outerRadius = knotRandom.nextFloat() * 20f + 10f
        val innerRadius = outerRadius * 0.5f

        // Anneau extérieur du nœud
        scope.drawCircle(
            color = knotColor.copy(alpha = 0.4f),
            radius = outerRadius,
            center = Offset(cx, cy),
        )

        // Centre du nœud
        scope.drawCircle(
            color = knotColor.copy(alpha = 0.6f),
            radius = innerRadius,
            center = Offset(cx, cy),
        )
    }

    // Vignette sur les bords
    drawVignette(scope, width, height, isDark)
}

// =====================================================
// Vignette commune
// =====================================================

private fun drawVignette(scope: DrawScope, width: Float, height: Float, isDark: Boolean) {
    val vignetteColor = if (isDark) Color(0xFF000000) else Color(0xFF5A4020)
    val vignetteAlpha = if (isDark) 0.5f else 0.2f

    // Vignette radiale : sombre sur les bords, transparent au centre
    scope.drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                vignetteColor.copy(alpha = vignetteAlpha),
            ),
            center = Offset(width / 2f, height / 2f),
            radius = maxOf(width, height) * 0.7f,
        )
    )
}