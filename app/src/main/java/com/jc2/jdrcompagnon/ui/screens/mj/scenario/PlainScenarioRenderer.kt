package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import com.jc2.jdrcompagnon.feature_exploration.ExplorationSession
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Style d'un nom (#type:nom) pendant la lecture : mis en avant, dans la couleur de son type
 * (voir couleurLien) — un lien cliquable est en plus souligné, une simple mention (#lieu,
 * #perso, #faction) ne l'est pas.
 */
private fun styleLien(type: String) = SpanStyle(
    color = couleurLien(type),
    fontWeight = FontWeight.SemiBold,
    textDecoration = if (type in TYPES_MENTION) null else TextDecoration.Underline,
)

/** Zones "à lire aux joueurs" ({lire}…{/lire}) : fond vert, même teinte que dans l'éditeur. */
internal val ReadAloudGreen = Color(0xFF2E7D32)

private const val LINK_TAG = "internal"
private val InternalLinkRegex = Regex(INTERNAL_LINK_PATTERN)
private val ColorTagRegex = Regex("""\{color:[^}]+\}|\{/color\}""")
private val NumberedItemRegex = Regex("""^\d+\.\s+""")
private const val READ_OPEN = "{lire}"
private const val READ_CLOSE = "{/lire}"

/**
 * Rendu des scénarios markdown pendant la lecture (le Markdown de mikepenz ne conserve pas les
 * balises couleur). Supporte : titres (# ## ###), gras (**…**), italique (*…*), code (`…`),
 * listes (- et 1.), balises couleur {color:…}…{/color} — imbriquables dans les titres, le gras,
 * les listes et sur plusieurs lignes —, liens internes cliquables et mentions (#lieu, #perso,
 * #faction) colorés selon leur type, et zones à lire aux joueurs
 * {lire}…{/lire} (fond vert + bouton de synthèse vocale).
 *
 * Chaque titre peut être replié (flèche) : tout ce qui suit jusqu'au titre suivant de même
 * niveau ou supérieur est masqué. L'état replié est mémorisé par [collapseKey] (une scène).
 */
@Composable
fun PlainScenarioRenderer(
    content: String,
    onLinkClick: (type: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    collapseKey: String? = null,
) {
    val context = LocalContext.current
    val segments = remember(content) { parseSegments(content) }
    val prefs = remember { context.getSharedPreferences("scenario_reader", Context.MODE_PRIVATE) }
    val prefKey = collapseKey?.let { "collapsed_$it" }
    var collapsed by remember(prefKey) {
        mutableStateOf(
            prefKey?.let { key -> prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet() }
                ?: emptySet()
        )
    }
    fun toggleCollapsed(index: Int) {
        collapsed = if (index in collapsed) collapsed - index else collapsed + index
        prefKey?.let { prefs.edit().putStringSet(it, collapsed.map(Int::toString).toSet()).apply() }
    }
    val tts = rememberScenarioTts()

    Column(modifier = modifier) {
        // Niveau du titre replié en cours : tout ce qui suit est masqué jusqu'à un titre
        // de niveau égal ou supérieur.
        var hiddenUnderLevel: Int? = null
        segments.forEachIndexed { index, segment ->
            if (segment is Segment.Header) {
                val hiddenLevel = hiddenUnderLevel
                if (hiddenLevel != null && segment.level <= hiddenLevel) hiddenUnderLevel = null
                if (hiddenUnderLevel == null) {
                    val isCollapsed = index in collapsed
                    CollapsibleHeader(segment, isCollapsed, onToggle = { toggleCollapsed(index) }, onLinkClick = onLinkClick)
                    if (isCollapsed) hiddenUnderLevel = segment.level
                }
            } else if (hiddenUnderLevel == null) {
                RenderSegment(segment, onLinkClick, tts)
            }
        }
    }
}

@Composable
private fun RenderSegment(segment: Segment, onLinkClick: (String, String) -> Unit, tts: ScenarioTts) {
    val bodyStyle = MaterialTheme.typography.bodyLarge
    when (segment) {
        is Segment.Paragraph -> LinkedText(segment.text, bodyStyle, onLinkClick)
        is Segment.Header -> LinkedText(
            segment.text,
            headerStyle(segment.level).copy(fontWeight = FontWeight.Bold),
            onLinkClick,
            Modifier.padding(top = 12.dp, bottom = 6.dp)
        )
        is Segment.BulletList -> Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            segment.items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", style = bodyStyle, color = Color.White)
                    LinkedText(item, bodyStyle, onLinkClick)
                }
            }
        }
        is Segment.NumberedList -> Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            segment.items.forEachIndexed { index, item ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("${index + 1}. ", style = bodyStyle, color = Color.White)
                    LinkedText(item, bodyStyle, onLinkClick)
                }
            }
        }
        is Segment.ReadAloud -> ReadAloudBlock(segment, onLinkClick, tts)
        is Segment.Image -> ScenarioImageBlock(segment.fileName)
    }
}

/**
 * Image d'une scène (carte, plan...) en pleine largeur, proportions conservées. Un appui ouvre
 * directement son exploration (brouillard de la page table, cf. feature_exploration), qui sert
 * aussi à la voir en grand : zoom au pincement.
 */
@Composable
private fun ScenarioImageBlock(fileName: String) {
    val context = LocalContext.current
    val bitmap = remember(fileName) {
        runCatching {
            val file = ScenarioImageStore.fichier(context, fileName)
            if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }.getOrNull()
    }
    if (bitmap == null) {
        Text("Image introuvable : $fileName", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        return
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Carte de la scène (appuyer pour l'exploration)",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                ExplorationSession.ouvrirEcran(
                    context, "scenario-image:$fileName", "Carte du scénario", ScenarioImageStore.fichier(context, fileName)
                )
            }
    )
}

@Composable
private fun ReadAloudBlock(segment: Segment.ReadAloud, onLinkClick: (String, String) -> Unit, tts: ScenarioTts) {
    val context = LocalContext.current
    val speaking = tts.speakingText == segment.speechText
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ReadAloudGreen.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, ReadAloudGreen),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                segment.children.forEach { RenderSegment(it, onLinkClick, tts) }
            }
            IconButton(onClick = {
                if (!tts.toggle(segment.speechText)) {
                    Toast.makeText(context, "Synthèse vocale indisponible sur cet appareil", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(
                    if (speaking) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                    contentDescription = if (speaking) "Arrêter la lecture" else "Lire à voix haute",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(
    segment: Segment.Header,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    onLinkClick: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LinkedText(segment.text, headerStyle(segment.level).copy(fontWeight = FontWeight.Bold), onLinkClick)
        }
        Icon(
            if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.ExpandMore,
            contentDescription = if (isCollapsed) "Déplier" else "Replier",
            tint = Color.White
        )
    }
}

@Composable
private fun headerStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineMedium
    2 -> MaterialTheme.typography.headlineSmall
    3 -> MaterialTheme.typography.titleLarge
    else -> MaterialTheme.typography.titleMedium
}

/** Texte blanc ; cliquable sur ses liens internes s'il en contient. */
@Composable
private fun LinkedText(
    text: AnnotatedString,
    style: TextStyle,
    onLinkClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (text.getStringAnnotations(LINK_TAG, 0, text.length).isEmpty()) {
        Text(text = text, style = style, color = Color.White, modifier = modifier)
    } else {
        ClickableText(
            text = text,
            style = style.copy(color = Color.White),
            modifier = modifier,
            onClick = { offset ->
                text.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()?.let { annotation ->
                    val (type, name) = annotation.item.split(":", limit = 2)
                    onLinkClick(type, name)
                }
            }
        )
    }
}

private sealed class Segment {
    data class Paragraph(val text: AnnotatedString) : Segment()
    data class Header(val level: Int, val text: AnnotatedString) : Segment()
    data class BulletList(val items: List<AnnotatedString>) : Segment()
    data class NumberedList(val items: List<AnnotatedString>) : Segment()
    data class ReadAloud(val children: List<Segment>, val speechText: String) : Segment()
    /** Ligne `{image:fichier}` : image du scénario (ScenarioImageStore), ex. une carte. */
    data class Image(val fileName: String) : Segment()
}

private val ImageLineRegex = Regex("""^\{image:\s*([^}]+?)\s*\}$""")

private val namedColorMap = mapOf(
    "red" to Color(0xFFF44336),
    "green" to Color(0xFF4CAF50),
    "blue" to Color(0xFF2196F3),
    "yellow" to Color(0xFFFFEB3B),
    "purple" to Color(0xFF9C27B0),
    "orange" to Color(0xFFFF9800)
)

private fun parseColorSpec(spec: String): Color? {
    val lower = spec.lowercase()
    namedColorMap[lower]?.let { return it }
    val hex = when {
        lower.startsWith("custom") -> lower.removePrefix("custom")
        lower.startsWith("#") -> lower.removePrefix("#")
        else -> lower
    }
    if (hex.matches(Regex("^[0-9a-f]{6}\$"))) {
        return Color(android.graphics.Color.parseColor("#$hex"))
    }
    return null
}

private fun parseSegments(content: String): List<Segment> {
    // Balises de lecture sur leur propre ligne, pour pouvoir les écrire en ligne ou en bloc.
    val prepared = content.replace(READ_OPEN, "\n$READ_OPEN\n").replace(READ_CLOSE, "\n$READ_CLOSE\n")
    return segmentLines(normalizeMultilineColors(prepared).lines())
}

/**
 * Une couleur ouverte sur une ligne et fermée plus loin est refermée en fin de ligne et rouverte
 * au début de la suivante (après l'éventuel marqueur de titre/liste), pour que chaque ligne se
 * rende seule.
 */
private fun normalizeMultilineColors(content: String): String {
    val open = kotlin.collections.ArrayDeque<String>()
    val markerRegex = Regex("""^\s*(#{1,6}\s+|-\s+|\d+\.\s+)?""")
    return content.lines().joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed == READ_OPEN || trimmed == READ_CLOSE) return@joinToString line
        val reopen = open.joinToString("")
        ColorTagRegex.findAll(line).forEach { match ->
            if (match.value == "{/color}") open.removeLastOrNull() else open.addLast(match.value)
        }
        val marker = markerRegex.find(line)?.value.orEmpty()
        marker + reopen + line.substring(marker.length) + "{/color}".repeat(open.size)
    }
}

private fun segmentLines(lines: List<String>): List<Segment> {
    val result = mutableListOf<Segment>()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        when {
            trimmed == READ_OPEN -> {
                var end = i + 1
                while (end < lines.size && lines[end].trim() != READ_CLOSE) end++
                val inner = lines.subList(i + 1, end)
                result += Segment.ReadAloud(segmentLines(inner), speechText(inner.joinToString("\n")))
                i = end + 1
            }
            trimmed == READ_CLOSE || trimmed.isBlank() -> i++
            ImageLineRegex.matches(trimmed) -> {
                result += Segment.Image(ImageLineRegex.find(trimmed)!!.groupValues[1])
                i++
            }
            trimmed.startsWith("# ") -> { result += Segment.Header(1, inline(trimmed.removePrefix("# ").trim())); i++ }
            trimmed.startsWith("## ") -> { result += Segment.Header(2, inline(trimmed.removePrefix("## ").trim())); i++ }
            trimmed.startsWith("### ") -> { result += Segment.Header(3, inline(trimmed.removePrefix("### ").trim())); i++ }
            trimmed.startsWith("- ") -> {
                val items = mutableListOf<AnnotatedString>()
                while (i < lines.size && lines[i].trim().startsWith("- ")) {
                    items += inline(lines[i].trim().removePrefix("- "))
                    i++
                }
                result += Segment.BulletList(items)
            }
            NumberedItemRegex.containsMatchIn(trimmed) -> {
                val items = mutableListOf<AnnotatedString>()
                while (i < lines.size && NumberedItemRegex.containsMatchIn(lines[i].trim())) {
                    items += inline(lines[i].trim().replaceFirst(NumberedItemRegex, ""))
                    i++
                }
                result += Segment.NumberedList(items)
            }
            else -> { result += Segment.Paragraph(inline(trimmed)); i++ }
        }
    }
    return result
}

private fun inline(text: String): AnnotatedString = buildAnnotatedString { appendInline(text) }

/**
 * Analyse récursive de la mise en forme en ligne : chaque balise (couleur, gras, italique) peut
 * contenir les autres, et les liens internes sont reconnus à tous les niveaux.
 */
private fun AnnotatedString.Builder.appendInline(text: String) {
    val plain = StringBuilder()
    fun flush() {
        if (plain.isNotEmpty()) {
            append(plain.toString())
            plain.clear()
        }
    }
    var i = 0
    while (i < text.length) {
        if (text.startsWith("{color:", i)) {
            val specEnd = text.indexOf('}', i)
            val close = if (specEnd > 0) findColorClose(text, specEnd + 1) else -1
            if (close >= 0) {
                flush()
                val color = parseColorSpec(text.substring(i + "{color:".length, specEnd).trim())
                val inner = text.substring(specEnd + 1, close)
                if (color != null) withStyle(SpanStyle(color = color)) { appendInline(inner) } else appendInline(inner)
                i = close + "{/color}".length
                continue
            }
        }
        if (text.startsWith("{/color}", i)) {
            i += "{/color}".length
            continue
        }
        if (text.startsWith("**", i)) {
            val close = text.indexOf("**", i + 2)
            if (close > i + 2) {
                flush()
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(text.substring(i + 2, close)) }
                i = close + 2
                continue
            }
        }
        if (text[i] == '*') {
            val close = text.indexOf('*', i + 1)
            if (close > i + 1) {
                flush()
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(text.substring(i + 1, close)) }
                i = close + 1
                continue
            }
        }
        if (text[i] == '`') {
            val close = text.indexOf('`', i + 1)
            if (close > i + 1) {
                flush()
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x33FFFFFF))) {
                    append(text.substring(i + 1, close))
                }
                i = close + 1
                continue
            }
        }
        if (text[i] == '#') {
            val match = InternalLinkRegex.find(text, i)
            if (match != null && match.range.first == i) {
                flush()
                val type = match.groupValues[1]
                val name = match.groupValues[2].ifEmpty { match.groupValues[3] }
                if (type in TYPES_MENTION) {
                    withStyle(styleLien(type)) { append(name) }
                } else {
                    pushStringAnnotation(tag = LINK_TAG, annotation = "$type:$name")
                    withStyle(styleLien(type)) { append(internalLinkLabel(type, name)) }
                    pop()
                }
                i = match.range.last + 1
                continue
            }
        }
        plain.append(text[i])
        i++
    }
    flush()
}

/** Position du {/color} qui ferme la balise ouverte juste avant [from], en tenant compte des imbrications. */
private fun findColorClose(text: String, from: Int): Int {
    var depth = 1
    var j = from
    while (true) {
        val close = text.indexOf("{/color}", j)
        if (close < 0) return -1
        val open = text.indexOf("{color:", j)
        if (open in 0 until close) {
            depth++
            j = open + "{color:".length
        } else {
            depth--
            if (depth == 0) return close
            j = close + "{/color}".length
        }
    }
}

/** Texte brut d'une zone à lire, sans balises, pour la synthèse vocale. */
private fun speechText(text: String): String =
    text.replace(Regex("""\{/?color[^}]*\}"""), "")
        .replace(InternalLinkRegex) { it.groupValues[2].ifEmpty { it.groupValues[3] } }
        .replace(Regex("""^\s*(#{1,6}\s+|-\s+|\d+\.\s+)""", RegexOption.MULTILINE), "")
        .replace("**", "")
        .replace("*", "")
        .replace("`", "")
        .trim()
