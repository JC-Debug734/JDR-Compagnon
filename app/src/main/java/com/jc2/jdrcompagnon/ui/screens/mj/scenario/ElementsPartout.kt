package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.feature_exploration.ExplorationSession
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Élément « partout » d'un scénario : ce qui peut arriver à plusieurs endroits (le dragon se
 * réveille, une patrouille passe, un PNJ réagit...). Écrit dans le fichier entre
 * `{partout: Titre}` et `{/partout}`, n'importe où (introduction ou scène) ; retiré du texte
 * des pages et affiché dans le menu flottant ([MenuFlottantPartout]).
 */
data class ElementPartout(val titre: String, val contenu: String)

private val partoutRegex = Regex("""\{partout:\s*([^}\n]*)\}[ \t]*\n?(.*?)\{/partout\}[ \t]*\n?""", RegexOption.DOT_MATCHES_ALL)

fun extraireElementsPartout(texte: String): List<ElementPartout> =
    partoutRegex.findAll(texte).map { ElementPartout(it.groupValues[1].trim().ifBlank { "Sans titre" }, it.groupValues[2].trim()) }.toList()

/** Texte d'une page sans ses éléments « partout » (affichés dans le menu flottant). */
fun sansElementsPartout(texte: String): String = if ("{partout:" !in texte) texte else texte.replace(partoutRegex, "")

private const val PREFS = "scenario_reader"

/**
 * Éléments « partout » du scénario en cours de lecture : le lecteur les déclare ici, et la
 * racine de l'app ([MenuFlottantPartoutHote], dans JdrNavGraph) les affiche par-dessus tout
 * l'écran, barres du haut et du bas comprises, comme l'écran d'exploration.
 */
object PartoutSession {
    class Actif(
        val elements: List<ElementPartout>,
        val onLinkClick: (type: String, name: String) -> Unit,
        val zonesCarte: List<ExplorationSession.ZoneDeCarte>,
        val onRevelerZone: (ExplorationSession.ZoneDeCarte) -> Unit,
    )

    private val _actif = MutableStateFlow<Actif?>(null)
    val actif: StateFlow<Actif?> = _actif.asStateFlow()

    fun afficher(actif: Actif) { _actif.value = actif }

    /** Le lecteur se ferme : plus de menu, sauf si un autre lecteur l'a remplacé entre-temps. */
    fun retirer(actif: Actif) { if (_actif.value === actif) _actif.value = null }
}

/** Menu flottant du scénario en cours de lecture, à placer à la racine de l'app. */
@Composable
fun MenuFlottantPartoutHote() {
    val actif by PartoutSession.actif.collectAsState()
    val a = actif ?: return
    MenuFlottantPartout(a.elements, a.onLinkClick, a.zonesCarte, a.onRevelerZone)
}

/**
 * Menu flottant des éléments « partout » : une pastille 📌 déplaçable n'importe où dans la zone
 * qui le contient (position mémorisée). Un appui l'ouvre sur la liste des titres ; un appui sur
 * un titre déplie son détail, liens cliquables comme dans la page. Seule la bulle capte les
 * touchers : le reste de l'écran reste utilisable.
 */
@Composable
fun MenuFlottantPartout(
    elements: List<ElementPartout>,
    onLinkClick: (type: String, name: String) -> Unit,
    zonesCarte: List<ExplorationSession.ZoneDeCarte> = emptyList(),
    onRevelerZone: (ExplorationSession.ZoneDeCarte) -> Unit = {},
) {
    if (elements.isEmpty()) return
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    // Position en fraction de l'écran (0..1) : garde un sens après une rotation.
    var fx by remember { mutableFloatStateOf(prefs.getFloat("partout_x", 0.82f)) }
    var fy by remember { mutableFloatStateOf(prefs.getFloat("partout_y", 0.70f)) }
    var ouvert by remember { mutableStateOf(false) }
    var deplie by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current

    // Zone visible seulement : jamais sous la barre d'état ni la barre de navigation du téléphone.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val largeurPx = with(density) { maxWidth.toPx() }
        val hauteurPx = with(density) { maxHeight.toPx() }
        val largeurBulle = if (ouvert) minOf(maxWidth - 16.dp, 340.dp) else 56.dp
        val bullePx = with(density) { largeurBulle.toPx() }
        // Toujours dans l'écran ; le panneau ouvert se déploie vers le bas depuis sa poignée.
        val x = (fx * largeurPx).coerceIn(0f, (largeurPx - bullePx).coerceAtLeast(0f))
        val y = (fy * hauteurPx).coerceIn(0f, (hauteurPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f))

        val glisser = Modifier.pointerInput(largeurPx, hauteurPx) {
            detectDragGestures(
                onDragEnd = { prefs.edit().putFloat("partout_x", fx).putFloat("partout_y", fy).apply() },
            ) { change, d ->
                change.consume()
                fx = ((fx * largeurPx + d.x) / largeurPx).coerceIn(0f, 1f)
                fy = ((fy * hauteurPx + d.y) / hauteurPx).coerceIn(0f, 1f)
            }
        }

        Box(modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }) {
            if (!ouvert) {
                Surface(
                    shape = CircleShape,
                    color = ForcedDarkPalette.AccentGold,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(56.dp).then(glisser).clickable { ouvert = true },
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("📌", fontSize = 24.sp) }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    contentColor = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(largeurBulle),
                ) {
                    Column {
                        // Barre de titre : poignée pour déplacer le panneau.
                        Row(
                            modifier = Modifier.fillMaxWidth().then(glisser).padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📌 Partout", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { ouvert = false; deplie = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Réduire", tint = Color.White)
                            }
                        }
                        HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .heightIn(max = with(density) { (hauteurPx * 0.6f).toDp() })
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            elements.forEach { e ->
                                val estDeplie = deplie == e.titre
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { deplie = if (estDeplie) null else e.titre }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(e.titre, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Icon(if (estDeplie) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.White)
                                }
                                if (estDeplie) {
                                    PlainScenarioRenderer(
                                        content = e.contenu,
                                        onLinkClick = onLinkClick,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        zonesCarte = zonesCarte,
                                        onRevelerZone = onRevelerZone,
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
