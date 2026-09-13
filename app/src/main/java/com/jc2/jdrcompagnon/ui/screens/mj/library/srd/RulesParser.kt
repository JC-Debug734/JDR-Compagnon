package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Représente une section de règles.
 *
 * @property title Titre de la section (texte après `##`)
 * @property content Contenu markdown de la section, y compris les sous-sections `###`
 */
data class RuleSection(
    val title: String,
    val content: String
)

/**
 * Parser pour le fichier rules.md du SRD 5.1.
 *
 * Structure du fichier :
 * - Un titre principal `# Rules` (ignoré)
 * - Les sections principales sont marquées par `## `
 * - Les sous-sections `###` restent dans le contenu de leur section parente
 */
object RulesParser {

    /**
     * Parse le contenu markdown des règles en une liste de sections.
     *
     * @param rawMarkdown Le contenu brut du fichier markdown
     * @return Liste des sections dans l'ordre du fichier
     */
    fun parse(rawMarkdown: String): List<RuleSection> {
        val allLines = rawMarkdown.lines()
        val sections = mutableListOf<RuleSection>()
        var currentTitle = ""
        var currentContent = StringBuilder()

        fun flushSection() {
            val content = currentContent.toString().trim()
            if (currentTitle.isNotBlank() && content.isNotBlank()) {
                sections.add(
                    RuleSection(
                        title = currentTitle,
                        content = content
                    )
                )
            }
            currentTitle = ""
            currentContent = StringBuilder()
        }

        for (line in allLines) {
            val trimmed = line.trim()

            // Titre principal ignoré
            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                continue
            }

            // Nouvelle section ##
            if (trimmed.startsWith("## ") && !trimmed.startsWith("### ")) {
                flushSection()
                currentTitle = trimmed.removePrefix("## ").trim()
                currentContent.appendLine(line)
                continue
            }

            currentContent.appendLine(line)
        }

        flushSection()
        if (sections.isEmpty() && rawMarkdown.isNotBlank()) {
            sections.add(RuleSection(title = "Règles", content = rawMarkdown.trim()))
        }
        return sections
    }
}
