package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Convertit les tables HTML inline `<table>` en tables Markdown GFM (GitHub Flavored Markdown).
 *
 * Les fichiers SRD 5.1 contiennent du HTML inline dans les stat blocks de monstres,
 * notamment pour les tables de caractéristiques et les blocs de capacités.
 */
object HtmlTableConverter {

    private val tableRegex = Regex(
        """<table[^>]*>(.*?)</table>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    /**
     * Convertit toutes les tables HTML d'un texte markdown en tables GFM.
     * Utilise replaceAll (single pass) pour éviter les boucles O(n²).
     */
    fun convertAll(markdown: String): String {
        return tableRegex.replace(markdown) { match ->
            convertTable(match.value)
        }
    }

    /**
     * Convertit une table HTML individuelle en table GFM.
     *
     * Protections :
     * - Les cellules contenant `|` sont échappées en `\|` pour ne pas casser la table GFM.
     * - Si la table a plus de 8 colonnes, seules les 8 premières sont conservées
     *   (les tables larges provoquent des rendus markdown malformés sur mobile).
     */
    private fun convertTable(html: String): String {
        val rows = extractRows(html)
        if (rows.isEmpty()) return ""

        val parsedRows = rows.map { row -> extractCells(row) }
        if (parsedRows.isEmpty()) return ""

        // Nombre de colonnes déterminé par la première ligne (en-tête)
        val rawColumnCount = parsedRows.first().size
        if (rawColumnCount == 0) return ""

        // Limiter à 8 colonnes maximum pour éviter les tables GFM malformées
        val maxColumns = 8
        val columnCount = minOf(rawColumnCount, maxColumns)

        // Tronque chaque ligne à `columnCount` cellules et échappe les `|`
        val sanitizedRows = parsedRows.map { row ->
            val padded = row + List((columnCount - row.size).coerceAtLeast(0)) { "" }
            padded.take(columnCount).map { escapePipe(it) }
        }

        val headerRow = sanitizedRows.first()
        val bodyRows = sanitizedRows.drop(1)

        val sb = StringBuilder()

        // En-tête
        sb.append("| ")
        sb.append(headerRow.joinToString(" | "))
        sb.append(" |")
        sb.appendLine()

        // Ligne de séparation
        sb.append("|")
        repeat(columnCount) {
            sb.append(" --- |")
        }
        sb.appendLine()

        // Corps
        for (row in bodyRows) {
            sb.append("| ")
            sb.append(row.joinToString(" | "))
            sb.append(" |")
            sb.appendLine()
        }

        return sb.toString()
    }

    /**
     * Échappe les `|` dans le contenu d'une cellule pour ne pas casser la table GFM.
     */
    private fun escapePipe(text: String): String = text.replace("|", "\\|")

    /**
     * Extrait les lignes `<tr>` d'une table HTML.
     */
    private fun extractRows(html: String): List<String> {
        val rowRegex = Regex(
            """<tr[^>]*>(.*?)</tr>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return rowRegex.findAll(html).map { it.value }.toList()
    }

    /**
     * Extrait les cellules (`<th>` ou `<td>`) d'une ligne `<tr>`.
     */
    private fun extractCells(rowHtml: String): List<String> {
        val cellRegex = Regex(
            """<t[hd][^>]*>(.*?)</t[hd]>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return cellRegex.findAll(rowHtml).map { match ->
            stripHtmlTags(match.groupValues[1]).trim()
        }.toList()
    }

    /**
     * Supprime toutes les balises HTML d'un texte, en conservant le contenu textuel.
     */
    private fun stripHtmlTags(html: String): String {
        val withBreaks = html.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " ")
        return withBreaks.replace(Regex("""<[^>]+>"""), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }
}

/**
 * Équipement typé, avec les champs extraits des tableaux SRD ou des descriptions.
 */
data class EquipmentItem(
    val name: String,
    val category: String,
    val cost: String = "",
    val damage: String = "",
    val weight: String = "",
    val properties: String = "",
    val ac: String = "",
    val strength: String = "",
    val stealth: String = "",
    val rawMarkdown: String = ""
)