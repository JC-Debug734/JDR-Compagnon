package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Rendu alternatif des scénarios markdown si le Markdown de mikepenz ne conserve pas
 * les attributs style des balises HTML.
 *
 * Découpe le texte en segments colorés et segments normaux, sans utiliser Markdown.
 * Supporte en plus : gras (**texte**), italique (*texte*), code (`texte`),
 * listes (- et 1.), titres (# ## ###), balises couleur et liens internes cliquables.
 */
@Composable
fun PlainScenarioRenderer(
    content: String,
    onLinkClick: (type: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val segments = rememberSegmentedContent(content)
    Column(modifier = modifier) {
        segments.forEach { segment ->
            when (segment) {
                is Segment.Text -> {
                    if (segment.parts.isNotEmpty()) {
                        val annotatedString = buildAnnotatedString {
                            segment.parts.forEach { part ->
                                when (part) {
                                    is TextPart.Normal -> append(part.text)
                                    is TextPart.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
                                    is TextPart.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(part.text) }
                                    is TextPart.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE0E0E0))) { append(part.text) }
                                    is TextPart.Link -> {
                                        withStyle(
                                            SpanStyle(
                                                color = Color(0xFF2196F3),
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(part.name)
                                        }
                                    }
                                    is TextPart.Colored -> {
                                        withStyle(SpanStyle(color = part.color)) {
                                            append(part.text)
                                        }
                                    }
                                }
                            }
                        }
                        val clickableAnnotations = rememberClickableAnnotations(segment.parts)
                        if (clickableAnnotations.isNotEmpty()) {
                            ClickableText(
                                text = annotatedString,
                                onClick = { offset ->
                                    clickableAnnotations.firstOrNull { offset in it.range }?.let {
                                        onLinkClick(it.type, it.name)
                                    }
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is Segment.Header -> {
                    Text(
                        text = segment.text,
                        style = when (segment.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            3 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                is Segment.BulletList -> {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                        segment.items.forEach { item ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", style = MaterialTheme.typography.bodyLarge)
                                RenderAnnotatedLine(item, onLinkClick)
                            }
                        }
                    }
                }
                is Segment.NumberedList -> {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                        segment.items.forEachIndexed { index, item ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("${index + 1}. ", style = MaterialTheme.typography.bodyLarge)
                                RenderAnnotatedLine(item, onLinkClick)
                            }
                        }
                    }
                }
                is Segment.Link -> {
                    Surface(
                        onClick = { onLinkClick(segment.type, segment.name) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2196F3).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF2196F3)),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "#${segment.type}:${segment.name}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderAnnotatedLine(item: Pair<AnnotatedString, List<ClickableAnnotation>>, onLinkClick: (type: String, name: String) -> Unit) {
    val (annotatedString, annotations) = item
    if (annotations.isNotEmpty()) {
        ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotations.firstOrNull { offset in it.range }?.let {
                    onLinkClick(it.type, it.name)
                }
            },
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge)
    }
}

private data class ClickableAnnotation(val range: IntRange, val type: String, val name: String)

@Composable
private fun rememberClickableAnnotations(parts: List<TextPart>): List<ClickableAnnotation> {
    val annotations = mutableListOf<ClickableAnnotation>()
    var currentIndex = 0
    parts.forEach { part ->
        when (part) {
            is TextPart.Normal -> currentIndex += part.text.length
            is TextPart.Bold -> currentIndex += part.text.length
            is TextPart.Italic -> currentIndex += part.text.length
            is TextPart.Code -> currentIndex += part.text.length
            is TextPart.Link -> {
                annotations += ClickableAnnotation(currentIndex until currentIndex + part.name.length, part.type, part.name)
                currentIndex += part.name.length
            }
            is TextPart.Colored -> currentIndex += part.text.length
        }
    }
    return annotations
}

private sealed class Segment {
    data class Text(val parts: List<TextPart>) : Segment()
    data class Header(val level: Int, val text: String) : Segment()
    data class BulletList(val items: List<Pair<AnnotatedString, List<ClickableAnnotation>>>) : Segment()
    data class NumberedList(val items: List<Pair<AnnotatedString, List<ClickableAnnotation>>>) : Segment()
    data class Link(val type: String, val name: String) : Segment()
}

private sealed class TextPart {
    data class Normal(val text: String) : TextPart()
    data class Bold(val text: String) : TextPart()
    data class Italic(val text: String) : TextPart()
    data class Code(val text: String) : TextPart()
    data class Link(val type: String, val name: String) : TextPart()
    data class Colored(val text: String, val color: Color) : TextPart()
}

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

private fun rememberSegmentedContent(content: String): List<Segment> {
    val lines = content.lines()
    val result = mutableListOf<Segment>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimEnd()
        when {
            trimmed.startsWith("# ") -> {
                result += Segment.Header(1, trimmed.removePrefix("# ").trim())
                i++
            }
            trimmed.startsWith("## ") -> {
                result += Segment.Header(2, trimmed.removePrefix("## ").trim())
                i++
            }
            trimmed.startsWith("### ") -> {
                result += Segment.Header(3, trimmed.removePrefix("### ").trim())
                i++
            }
            trimmed.matches(Regex("^#(monster|pnj|npc|equipment|spell|rule):(\\[[^\\]]+\\]|[^\\s]+)\$")) -> {
                val match = Regex("^#(monster|pnj|npc|equipment|spell|rule):(?:\\[([^\\]]+)\\]|([^\\s]+))\$").find(trimmed)!!
                val name = match.groupValues[2].ifEmpty { match.groupValues[3] }
                result += Segment.Link(match.groupValues[1], name)
                i++
            }
            trimmed.startsWith("- ") -> {
                val items = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith("- ")) {
                    items += lines[i].trimStart().removePrefix("- ").trimEnd()
                    i++
                }
                result += Segment.BulletList(items.map { parseInlineAnnotated(it) })
            }
            trimmed.matches(Regex("^\\d+\\.\\s+")) -> {
                val items = mutableListOf<String>()
                val regex = Regex("^\\d+\\.\\s+(.*)")
                while (i < lines.size && lines[i].trimStart().matches(Regex("^\\d+\\.\\s+"))) {
                    items += regex.find(lines[i].trimStart())?.groupValues?.get(1)?.trimEnd() ?: lines[i]
                    i++
                }
                result += Segment.NumberedList(items.map { parseInlineAnnotated(it) })
            }
            trimmed.isBlank() -> i++
            else -> {
                result += segmentLine(trimmed)
                i++
            }
        }
    }
    return result
}

private fun segmentLine(line: String): Segment.Text {
    val parts = mutableListOf<TextPart>()
    var remaining = line

    // 1. Parse couleurs {color:...}...
    val colorRegex = Regex("""\{color:([^}]+)\}(.*?)\{/color\}""", RegexOption.DOT_MATCHES_ALL)
    var last = 0
    colorRegex.findAll(line).forEach { match ->
        if (match.range.first > last) {
            parts.addAll(parseInlineMarkdown(line.substring(last, match.range.first)))
        }
        val color = parseColorSpec(match.groupValues[1])
        if (color != null) {
            parts.add(TextPart.Colored(match.groupValues[2], color))
        } else {
            parts.addAll(parseInlineMarkdown(match.groupValues[2]))
        }
        last = match.range.last + 1
    }
    if (last < line.length) {
        parts.addAll(parseInlineMarkdown(line.substring(last)))
    }

    // 2. Parse liens internes dans chaque partie texte
    val partsWithLinks = mutableListOf<TextPart>()
    parts.forEach { part ->
        when (part) {
            is TextPart.Normal -> partsWithLinks.addAll(parseInlineLinks(part.text) { TextPart.Normal(it) })
            is TextPart.Bold -> partsWithLinks.addAll(parseInlineLinks(part.text) { TextPart.Bold(it) })
            is TextPart.Italic -> partsWithLinks.addAll(parseInlineLinks(part.text) { TextPart.Italic(it) })
            is TextPart.Code -> partsWithLinks.addAll(parseInlineLinks(part.text) { TextPart.Code(it) })
            is TextPart.Colored -> {
                val subParts = parseInlineLinks(part.text) { TextPart.Normal(it) }
                subParts.forEach { sub ->
                    when (sub) {
                        is TextPart.Normal -> partsWithLinks.add(TextPart.Colored(sub.text, part.color))
                        is TextPart.Link -> partsWithLinks.add(sub)
                        else -> partsWithLinks.add(sub)
                    }
                }
            }
            else -> partsWithLinks.add(part)
        }
    }

    return Segment.Text(partsWithLinks)
}

private fun parseInlineMarkdown(text: String): List<TextPart> {
    val parts = mutableListOf<TextPart>()
    var pos = 0
    while (pos < text.length) {
        val remaining = text.substring(pos)
        // Cherche le prochain délimiteur parmi **, *, `
        val nextBold = remaining.indexOf("**")
        val nextItalic = remaining.indexOf("*")
        val nextCode = remaining.indexOf("`")

        val candidates = mutableListOf<Pair<String, Int>>()
        if (nextBold >= 0) candidates += "**" to nextBold
        if (nextItalic >= 0 && nextItalic != nextBold) candidates += "*" to nextItalic
        if (nextCode >= 0) candidates += "`" to nextCode

        if (candidates.isEmpty()) {
            if (remaining.isNotEmpty()) parts.add(TextPart.Normal(remaining))
            break
        }

        // Priorité au plus proche. Pour *, si ** est au même index on ignore * (géré par **).
        val (delimiter, start) = candidates.minByOrNull { it.second }!!
        if (start > 0) parts.add(TextPart.Normal(remaining.substring(0, start)))
        val end = remaining.indexOf(delimiter, start + delimiter.length)
        if (end < 0) {
            // Pas de fermeture : traiter le délimiteur comme texte normal
            parts.add(TextPart.Normal(remaining.substring(start, start + delimiter.length)))
            pos += start + delimiter.length
            continue
        }
        val inner = remaining.substring(start + delimiter.length, end)
        when (delimiter) {
            "**" -> parts.add(TextPart.Bold(inner))
            "*" -> parts.add(TextPart.Italic(inner))
            "`" -> parts.add(TextPart.Code(inner))
        }
        pos += end + delimiter.length
    }
    return parts
}

private fun parseInlineLinks(text: String, wrap: (String) -> TextPart): List<TextPart> {
    val parts = mutableListOf<TextPart>()
    val regex = Regex("""(?<!\\)#(monster|pnj|npc|equipment|spell|rule):(?:\[([^\]\n]+)\]|([^\s\n\]]+))""")
    var last = 0
    regex.findAll(text).forEach { match ->
        if (match.range.first > last) {
            parts.add(wrap(text.substring(last, match.range.first)))
        }
        val name = match.groupValues[2].ifEmpty { match.groupValues[3] }
        parts.add(TextPart.Link(match.groupValues[1], name))
        last = match.range.last + 1
    }
    if (last < text.length) {
        parts.add(wrap(text.substring(last)))
    }
    return parts
}

private fun parseInlineAnnotated(text: String): Pair<AnnotatedString, List<ClickableAnnotation>> {
    val parts = segmentLine(text).parts
    val annotatedString = buildAnnotatedString {
        parts.forEach { part ->
            when (part) {
                is TextPart.Normal -> append(part.text)
                is TextPart.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
                is TextPart.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(part.text) }
                is TextPart.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFE0E0E0))) { append(part.text) }
                is TextPart.Link -> withStyle(SpanStyle(color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(part.name) }
                is TextPart.Colored -> withStyle(SpanStyle(color = part.color)) { append(part.text) }
            }
        }
    }
    val annotations = mutableListOf<ClickableAnnotation>()
    var index = 0
    parts.forEach { part ->
        val length = when (part) {
            is TextPart.Normal -> part.text.length
            is TextPart.Bold -> part.text.length
            is TextPart.Italic -> part.text.length
            is TextPart.Code -> part.text.length
            is TextPart.Link -> {
                annotations += ClickableAnnotation(index until index + part.name.length, part.type, part.name)
                part.name.length
            }
            is TextPart.Colored -> part.text.length
        }
        index += length
    }
    return annotatedString to annotations
}
