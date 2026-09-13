package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown

/**
 * Rendu markdown enrichi pour les scénarios.
 *
 * Prend en charge :
 * - la syntaxe custom `{color:nom}...{/color}` et `<color=#RRGGBB>...<color>`
 * - les liens internes `#type:nom` (monster, pnj, equipment, spell, rule) cliquables inline
 *
 * @param content Le contenu markdown brut
 * @param onLinkClick Callback appelé avec le type et le nom du lien interne
 */
@Composable
fun ScenarioMarkdown(
    content: String,
    onLinkClick: (type: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Utilise le rendu mikepenz Markdown pour le formattage riche ;
        // si les couleurs sont strippées, un fallback existance dans PlainScenarioRenderer.
        ClickableScenarioMarkdown(
            rawContent = content,
            onLinkClick = onLinkClick
        )
    }
}

/**
 * Préprocesse les balises `{color:nom}...{/color}` et `<color=...>...<color>` en HTML inline
 * pour le composant Markdown de mikepenz.
 */
internal fun preProcessColors(content: String): String {
    val namedColorMap = mapOf(
        "red" to "#F44336",
        "green" to "#4CAF50",
        "blue" to "#2196F3",
        "yellow" to "#FFEB3B",
        "purple" to "#9C27B0",
        "orange" to "#FF9800",
        "grey" to "#9E9E9E",
        "gray" to "#9E9E9E",
        "black" to "#000000",
        "white" to "#FFFFFF"
    )
    var result = content
    // Balises nommées {color:red}...{/color}
    namedColorMap.forEach { (name, hex) ->
        result = result.replace(
            Regex("""\{color:$name\}(.*?)\{/color\}""", RegexOption.DOT_MATCHES_ALL),
            "<span style=\"color:$hex\"\u003e\$1\u003c/span\u003e"
        )
    }
    // Balises nommées [color=red]...[/color]
    namedColorMap.forEach { (name, hex) ->
        result = result.replace(
            Regex("""\[color=$name\](.*?)\[/color\]""", RegexOption.DOT_MATCHES_ALL),
            "<span style=\"color:$hex\"\u003e\$1\u003c/span\u003e"
        )
    }
    // Balises hex [color=#RRGGBB]...[/color]
    result = result.replace(
        Regex("""\[color=([#0-9A-Fa-f]+)\](.*?)\[/color\]""", RegexOption.DOT_MATCHES_ALL)
    ) { match ->
        val colorSpec = match.groupValues[1].trim().lowercase()
        val hex = namedColorMap[colorSpec] ?: colorSpec
        "<span style=\"color:$hex\"\u003e${match.groupValues[2]}\u003c/span\u003e"
    }
    // Balises hex <color=#RRGGBB>...<color> ou <color=red>...<color> (legacy)
    result = result.replace(
        Regex("""\u003ccolor=([^\u003e]+)\u003e(.*?)\u003c/color\u003e""", RegexOption.DOT_MATCHES_ALL)
    ) { match ->
        val colorSpec = match.groupValues[1].trim().lowercase()
        val hex = namedColorMap[colorSpec] ?: colorSpec
        "<span style=\"color:$hex\"\u003e${match.groupValues[2]}\u003c/span\u003e"
    }
    return result
}

/**
 * Supprime les balises couleur du texte affiché inline pour ne pas les montrer littéralement.
 */
private fun stripColorTags(text: String): String {
    var result = text
    result = result.replace(Regex("""\{color:[^}]+\}"""), "")
    result = result.replace(Regex("""\{/color\}"""), "")
    result = result.replace(Regex("""\[color=[^\]]+\]"""), "")
    result = result.replace(Regex("""\[/color\]"""), "")
    result = result.replace(Regex("""\u003ccolor=[^\u003e]+\u003e"""), "")
    result = result.replace(Regex("""\u003c/color\u003e"""), "")
    return result
}

private fun stripInternalLinkTags(text: String): String {
    return text.replace(Regex("""#(monster|pnj|npc|equipment|spell|rule):(?:\[([^\]\n]+)\]|([^\s\n\]]+))""")) { match ->
        match.groupValues[2].ifEmpty { match.groupValues[3] }
    }
}

/**
 * Remplace les `\\#` par `#` pour afficher un littéral.
 */
private fun unescapeHashes(text: String): String = text.replace("\\\\#", "#")

/**
 * Regex des liens internes : #type:nom. Ignore les `\\#` (échappé).
 */
private val internalLinkRegex = Regex("""(?<!\\)#(monster|pnj|npc|equipment|spell|rule):(?:\[([^\]\n]+)\]|([^\s\n\]]+))""")

private fun MatchResult.internalLinkName(): String = groupValues[2].ifEmpty { groupValues[3] }

/**
 * Construit un texte enrichi avec les liens internes cliquables.
 * Le texte entre les liens est rendu par Markdown ; les liens eux-mêmes apparaissent inline.
 */
@Composable
private fun ClickableScenarioMarkdown(
    rawContent: String,
    onLinkClick: (type: String, name: String) -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val matches = remember(rawContent) { internalLinkRegex.findAll(rawContent).toList() }

    if (matches.isEmpty()) {
        Markdown(content = preProcessColors(rawContent).replace(internalLinkRegex) { "**${it.internalLinkName()}**" })
        return
    }

    var lastIndex = 0
    Column {
        matches.forEachIndexed { index, match ->
            val start = match.range.first
            val end = match.range.last + 1
            val textBefore = rawContent.substring(lastIndex, start)
            val type = match.groupValues[1]
            val name = match.internalLinkName()

            if (textBefore.isNotBlank()) {
                Markdown(content = preProcessColors(textBefore).replace(internalLinkRegex) { "**${it.internalLinkName()}**" })
            }

            ClickableText(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(name)
                    }
                },
                onClick = { onLinkClick(type, name) },
                modifier = Modifier.padding(vertical = 0.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            lastIndex = end
        }
        val tail = rawContent.substring(lastIndex)
        if (tail.isNotBlank()) {
            Markdown(content = preProcessColors(tail).replace(internalLinkRegex) { "**${it.internalLinkName()}**" })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowInternalLinks(links: List<Pair<String, String>>, onLinkClick: (String, String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { (type, name) ->
            val label = when (type) {
                "monster" -> "🐲 $name"
                "pnj", "npc" -> "👤 $name"
                "equipment" -> "⚔️ $name"
                "spell" -> "✨ $name"
                "rule" -> "📜 $name"
                else -> "$type:$name"
            }
            Surface(
                onClick = { onLinkClick(type, name) },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2196F3).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF2196F3))
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Liste tous les liens internes présents dans le contenu.
 * Format : #type:nom ou #type:[nom en plusieurs mots]
 */
fun extractInternalLinks(content: String): List<Pair<String, String>> {
    val regex = Regex("""(?<!\\)#(monster|pnj|npc|equipment|spell|rule):(?:\[([^\]\n]+)\]|([^\s\n\]]+))""")
    return regex.findAll(content).map { it.groupValues[1] to it.internalLinkName() }.toList()
}

/**
 * Crée un [AnnotatedString] avec les liens internes cliquables.
 * Utilisé par les composants qui ont besoin d'un rendu texte cliquable
 * plutôt qu'un rendu markdown complet.
 */
@Composable
fun buildClickableScenarioText(
    content: String,
    baseStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onLinkClick: (type: String, name: String) -> Unit
): AnnotatedString = buildAnnotatedString {
    withStyle(baseStyle.toSpanStyle()) {
        val regex = Regex("""(?<!\\)#(monster|pnj|npc|equipment|spell|rule):(?:\[([^\]\n]+)\]|([^\s\n\]]+))""")
        var lastIndex = 0
        regex.findAll(content).forEach { match ->
            val name = match.internalLinkName()
            append(content.substring(lastIndex, match.range.first))
            pushStringAnnotation(tag = "internal", annotation = "${match.groupValues[1]}:$name")
            withStyle(SpanStyle(color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                append(name)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        append(content.substring(lastIndex))
    }
}
