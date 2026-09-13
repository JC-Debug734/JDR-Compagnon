package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour le fichier monsters.md du SRD 5.1 en français.
 *
 * Structure du fichier :
 * - La section `## Descriptions des monstres` marque le début des monstres individuels
 * - Les monstres individuels commencent à `#### ` (heading niveau 4)
 * - Le contenu d'un monstre s'étend jusqu'au prochain `####`, `##` ou fin de fichier
 */
object MonsterParser {

    /** Marqueur de début de la section des monstres individuels */
    private const val MONSTER_DESCRIPTIONS_HEADER = "## Descriptions des monstres"

    /**
     * Parse le contenu markdown du fichier monsters.md en une liste d'entrées SRD.
     *
     * @param rawMarkdown Le contenu brut du fichier markdown
     * @return Liste triée par nom de monstre
     */
    fun parse(rawMarkdown: String): List<SrdEntry> {
        val allLines = rawMarkdown.lines()

        // Trouve la ligne "## Descriptions des monstres" — tout ce qui précède est ignoré
        val startIndex = allLines.indexOfFirst { line ->
            line.trim().equals(MONSTER_DESCRIPTIONS_HEADER, ignoreCase = true)
        }

        val linesToParse = if (startIndex == -1) allLines else allLines.drop(startIndex + 1)
        return parseMonsters(linesToParse)
    }

    private fun parseMonsters(lines: List<String>): List<SrdEntry> {
        val entries = mutableListOf<SrdEntry>()
        var currentName: String? = null
        var currentContent = StringBuilder()
        var currentCategory = ""

        fun flushEntry() {
            val name = currentName ?: return
            val content = currentContent.toString().trim()
            if (content.isNotBlank()) {
                entries.add(
                    SrdEntry(
                        name = name,
                        rawMarkdown = content,
                        category = currentCategory,
                    )
                )
            }
            currentName = null
            currentContent = StringBuilder()
        }

        for (line in lines) {
            val trimmed = line.trim()

            // Détecte une catégorie de monstres (### Heading) — sous-sections logiques
            if (trimmed.startsWith("### ") && !trimmed.startsWith("#### ")) {
                flushEntry()
                currentCategory = trimmed.removePrefix("### ").trim()
                continue
            }

            // Détecte un monstre (#### Heading)
            if (trimmed.startsWith("#### ")) {
                flushEntry()
                currentName = trimmed.removePrefix("#### ").trim()
                currentContent = StringBuilder()
                currentContent.appendLine(line)
                continue
            }

            // Si on est dans un monstre, accumule le contenu
            if (currentName != null) {
                currentContent.appendLine(line)
            }
        }

        // Flush le dernier monstre
        flushEntry()

        return entries.sortedBy { it.name.lowercase() }
    }
}