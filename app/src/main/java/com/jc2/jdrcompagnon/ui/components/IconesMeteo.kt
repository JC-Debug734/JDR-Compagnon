package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.DayPeriod
import com.jc2.jdrcompagnon.ui.ScenarioTemperature
import com.jc2.jdrcompagnon.ui.ScenarioWeather
import kotlin.math.cos
import kotlin.math.sin

/*
 * Icônes météo en couleur, dessinées en vectoriel (nettes à toutes les tailles). Une seule icône
 * combine la météo et le moment de la journée : soleil le jour, lune la nuit, soleil couchant à
 * l'aube et au crépuscule (avant, le soleil du moment de la journée s'ajoutait à celui du « ciel
 * dégagé », d'où deux soleils côte à côte).
 */

private val SoleilCentre = Color(0xFFFFE57F)
private val SoleilBord = Color(0xFFFFA726)
private val SoleilCouchant = Color(0xFFFF7043)
private val Lune = Color(0xFFFFF3C4)
private val LuneOmbre = Color(0xFFE6D58F)
private val NuageClair = Color(0xFFFFFFFF)
private val NuageGris = Color(0xFFCFD8DC)
private val NuageSombre = Color(0xFF78909C)
private val NuageOrage = Color(0xFF546E7A)
private val Goutte = Color(0xFF4FC3F7)
private val Eclair = Color(0xFFFFD600)
private val EclairBord = Color(0xFFFF8F00)
private val Flocon = Color(0xFFE1F5FE)
private val Brume = Color(0xFFB0BEC5)
private val Vent = Color(0xFF80DEEA)
private val Chaleur = Color(0xFFFF5722)

/** Icône météo en couleur ([taille] carré), adaptée au moment de la journée [periode]. */
@Composable
fun IconeMeteo(
    meteo: ScenarioWeather,
    periode: DayPeriod = DayPeriod.JOUR,
    taille: Dp = 24.dp,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Canvas(
        modifier = modifier
            .size(taille)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
    ) {
        val s = size.minDimension
        when (meteo) {
            ScenarioWeather.CLAIR -> astre(periode, Offset(s * 0.5f, s * 0.5f), s * 0.42f)
            ScenarioWeather.SEC -> {
                astre(DayPeriod.JOUR, Offset(s * 0.5f, s * 0.42f), s * 0.36f, chaud = true)
                vaguesChaleur(s)
            }
            ScenarioWeather.NUAGEUX -> {
                astre(periode, Offset(s * 0.66f, s * 0.34f), s * 0.28f)
                nuage(s, Offset(s * 0.44f, s * 0.62f), s * 0.80f, NuageClair, NuageGris)
            }
            ScenarioWeather.PLUIE -> {
                nuage(s, Offset(s * 0.5f, s * 0.40f), s * 0.86f, NuageGris, NuageSombre)
                gouttes(s, 3)
            }
            ScenarioWeather.ORAGE -> {
                nuage(s, Offset(s * 0.5f, s * 0.38f), s * 0.86f, NuageSombre, NuageOrage)
                gouttes(s, 2, decalage = true)
                eclair(s)
            }
            ScenarioWeather.NEIGE -> {
                nuage(s, Offset(s * 0.5f, s * 0.40f), s * 0.86f, NuageClair, NuageGris)
                flocon(Offset(s * 0.32f, s * 0.80f), s * 0.10f)
                flocon(Offset(s * 0.66f, s * 0.82f), s * 0.10f)
            }
            ScenarioWeather.FORTE_NEIGE -> {
                nuage(s, Offset(s * 0.5f, s * 0.36f), s * 0.86f, NuageGris, NuageSombre)
                flocon(Offset(s * 0.24f, s * 0.74f), s * 0.09f)
                flocon(Offset(s * 0.50f, s * 0.86f), s * 0.09f)
                flocon(Offset(s * 0.76f, s * 0.74f), s * 0.09f)
            }
            ScenarioWeather.BROUILLARD -> {
                astre(periode, Offset(s * 0.5f, s * 0.40f), s * 0.26f, voile = true)
                brume(s)
            }
            ScenarioWeather.VENT_FORT -> vent(s)
        }
    }
}

/** Thermomètre en couleur, rempli selon la température. */
@Composable
fun IconeTemperature(temperature: ScenarioTemperature, couleur: Color, taille: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(taille)) {
        val s = size.minDimension
        val largeur = s * 0.26f
        val haut = s * 0.06f
        val bulbe = Offset(s * 0.5f, s * 0.76f)
        val rayonBulbe = s * 0.19f
        val contour = Color.White.copy(alpha = 0.85f)
        // Tube puis bulbe, remplis de verre sombre.
        drawRoundRect(
            color = Color(0x55FFFFFF),
            topLeft = Offset(s * 0.5f - largeur / 2, haut),
            size = Size(largeur, bulbe.y - haut),
            cornerRadius = CornerRadius(largeur / 2),
        )
        drawCircle(Color(0x55FFFFFF), rayonBulbe, bulbe)
        // Liquide : du bulbe jusqu'à une hauteur qui monte avec la chaleur.
        val niveau = (temperature.ordinal + 1f) / ScenarioTemperature.entries.size
        val sommet = bulbe.y - (bulbe.y - haut - largeur * 0.3f) * niveau
        val fin = largeur * 0.46f
        drawRoundRect(
            color = couleur,
            topLeft = Offset(s * 0.5f - fin / 2, sommet),
            size = Size(fin, bulbe.y - sommet),
            cornerRadius = CornerRadius(fin / 2),
        )
        drawCircle(couleur, rayonBulbe * 0.72f, bulbe)
        // Contours.
        drawRoundRect(
            color = contour,
            topLeft = Offset(s * 0.5f - largeur / 2, haut),
            size = Size(largeur, bulbe.y - haut - rayonBulbe * 0.6f),
            cornerRadius = CornerRadius(largeur / 2),
            style = Stroke(width = s * 0.05f),
        )
        drawCircle(contour, rayonBulbe, bulbe, style = Stroke(width = s * 0.05f))
    }
}

// ── Éléments de dessin ──

/** Soleil (jour), soleil couchant (aube, crépuscule) ou lune (nuit). */
private fun DrawScope.astre(periode: DayPeriod, centre: Offset, rayon: Float, chaud: Boolean = false, voile: Boolean = false) {
    val alpha = if (voile) 0.55f else 1f
    when (periode) {
        DayPeriod.NUIT -> lune(centre, rayon * 0.92f, alpha)
        DayPeriod.AUBE, DayPeriod.CREPUSCULE -> soleil(centre, rayon, SoleilCentre, SoleilCouchant, alpha)
        DayPeriod.JOUR -> soleil(centre, rayon, SoleilCentre, if (chaud) Chaleur else SoleilBord, alpha)
    }
}

private fun DrawScope.soleil(centre: Offset, rayon: Float, centreCouleur: Color, bord: Color, alpha: Float) {
    val coeur = rayon * 0.56f
    // Rayons : 8 traits arrondis autour du disque.
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val debut = Offset(centre.x + cos(angle).toFloat() * coeur * 1.28f, centre.y + sin(angle).toFloat() * coeur * 1.28f)
        val fin = Offset(centre.x + cos(angle).toFloat() * rayon, centre.y + sin(angle).toFloat() * rayon)
        drawLine(bord.copy(alpha = alpha), debut, fin, strokeWidth = rayon * 0.16f, cap = StrokeCap.Round)
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(centreCouleur, bord), center = centre, radius = coeur),
        radius = coeur,
        center = centre,
        alpha = alpha,
    )
}

private fun DrawScope.lune(centre: Offset, rayon: Float, alpha: Float) {
    val disque = Path().apply { addOval(Rect(centre, rayon)) }
    val ombre = Path().apply { addOval(Rect(Offset(centre.x + rayon * 0.55f, centre.y - rayon * 0.35f), rayon * 0.9f)) }
    val croissant = Path().apply { op(disque, ombre, PathOperation.Difference) }
    drawPath(croissant, Brush.linearGradient(listOf(Lune, LuneOmbre), start = Offset(centre.x - rayon, centre.y - rayon), end = Offset(centre.x + rayon, centre.y + rayon)), alpha = alpha)
}

/** Nuage centré en [centre], de largeur [largeur], dégradé de [haut] (clair) à [bas]. */
private fun DrawScope.nuage(s: Float, centre: Offset, largeur: Float, haut: Color, bas: Color) {
    val h = largeur * 0.52f
    val gauche = centre.x - largeur / 2
    val dessus = centre.y - h / 2
    val forme = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(gauche, dessus + h * 0.42f, gauche + largeur, dessus + h), CornerRadius(h * 0.29f)))
        addOval(Rect(Offset(gauche + largeur * 0.30f, dessus + h * 0.50f), h * 0.32f))
        addOval(Rect(Offset(gauche + largeur * 0.58f, dessus + h * 0.40f), h * 0.42f))
    }
    // Ombre douce sous le nuage, puis le nuage.
    translate(top = s * 0.025f) { drawPath(forme, Color.Black.copy(alpha = 0.18f)) }
    drawPath(forme, Brush.verticalGradient(listOf(haut, bas), startY = dessus, endY = dessus + h))
}

private fun DrawScope.gouttes(s: Float, nombre: Int, decalage: Boolean = false) {
    val xs = if (nombre == 3) listOf(0.30f, 0.50f, 0.70f) else if (decalage) listOf(0.28f, 0.72f) else listOf(0.38f, 0.62f)
    xs.forEachIndexed { i, fx ->
        val y = s * (if (i % 2 == 0) 0.74f else 0.80f)
        drawLine(Goutte, Offset(s * fx + s * 0.03f, y - s * 0.07f), Offset(s * fx - s * 0.03f, y + s * 0.07f), strokeWidth = s * 0.075f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.eclair(s: Float) {
    val p = Path().apply {
        moveTo(s * 0.56f, s * 0.50f)
        lineTo(s * 0.38f, s * 0.74f)
        lineTo(s * 0.50f, s * 0.74f)
        lineTo(s * 0.42f, s * 0.96f)
        lineTo(s * 0.66f, s * 0.66f)
        lineTo(s * 0.53f, s * 0.66f)
        close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Eclair, EclairBord), startY = s * 0.5f, endY = s * 0.96f))
    drawPath(p, EclairBord, style = Stroke(width = s * 0.02f))
}

private fun DrawScope.flocon(centre: Offset, rayon: Float) {
    for (i in 0 until 3) {
        rotate(degrees = i * 60f, pivot = centre) {
            drawLine(Flocon, Offset(centre.x, centre.y - rayon), Offset(centre.x, centre.y + rayon), strokeWidth = rayon * 0.32f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.brume(s: Float) {
    listOf(0.58f to 0.72f, 0.72f to 0.84f, 0.86f to 0.60f).forEachIndexed { i, (y, longueur) ->
        val debut = s * (0.5f - longueur / 2) + if (i == 1) s * 0.06f else 0f
        drawLine(Brume, Offset(debut, s * y), Offset(debut + s * longueur, s * y), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.vent(s: Float) {
    val trait = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
    // Trois souffles terminés par une boucle.
    listOf(Triple(0.30f, 0.62f, 0.12f), Triple(0.50f, 0.80f, 0.14f), Triple(0.70f, 0.52f, 0.10f)).forEach { (y, longueur, boucle) ->
        val p = Path().apply {
            moveTo(s * 0.10f, s * y)
            lineTo(s * (0.10f + longueur), s * y)
            cubicTo(
                s * (0.10f + longueur + boucle), s * y,
                s * (0.10f + longueur + boucle), s * (y - boucle * 1.6f),
                s * (0.10f + longueur), s * (y - boucle * 1.4f),
            )
        }
        drawPath(p, Vent, style = trait)
    }
}

private fun DrawScope.vaguesChaleur(s: Float) {
    val trait = Stroke(width = s * 0.06f, cap = StrokeCap.Round)
    listOf(0.30f, 0.50f, 0.70f).forEach { fx ->
        val p = Path().apply {
            moveTo(s * fx, s * 0.96f)
            cubicTo(s * (fx - 0.06f), s * 0.90f, s * (fx + 0.06f), s * 0.86f, s * fx, s * 0.80f)
        }
        drawPath(p, Chaleur.copy(alpha = 0.85f), style = trait)
    }
}
