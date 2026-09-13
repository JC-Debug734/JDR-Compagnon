package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour le fichier spells.md du SRD 5.1.
 *
 * Structure du fichier :
 * - La section "## Spell Descriptions" marque le début des sorts individuels
 * - Les sorts individuels commencent à `#### ` (heading niveau 4)
 * - Le contenu d'un sort s'étend jusqu'au prochain `####` ou `##` ou fin de fichier
 *
 * Le fichier contient environ 352 sorts.
 */
object SpellParser {

    /** Marqueur de début de la section des sorts individuels */
    private const val SPELL_DESCRIPTIONS_HEADER = "## Descriptions des sorts"

    /**
     * Sous-titres qui peuvent apparaître sous `#### ` dans le contenu d'un sort
     * (ex: Find Steed) mais qui ne sont PAS des sorts individuels.
     * Si un `#### ` porte l'un de ces noms, on ne crée pas de nouvelle entrée —
     * on continue à accumuler le contenu dans le sort courant.
     */
    private val NON_SPELL_HEADINGS = setOf(
        "Traits", "Actions", "Bonus Actions", "Reactions", "Legendary Actions",
        "Trait", "Action", "Bonus Action", "Reaction", "Legendary Action",
        "Cast at Higher Level", "Lancer un sort à un niveau supérieur",
        "Action bonus", "Réactions", "Action", "Réaction", "Actions",
        "Actions légendaires", "Action légendaire", "Usage limité", "Équipement",
        "Concentration", "Instantané", "Verbal (V)", "Somatique (S)", "Matériel (M)",
        "Un chemin clair vers la cible", "Se cibler", "Cône", "Cube", "Cylindre", "Ligne", "Sphère",
        "Durée", "Portée", "Temps de coulée plus longs",
    )

    /**
     * Parse le contenu markdown du fichier spells.md en une liste d'entrées SRD.
     *
     * @param rawMarkdown Le contenu brut du fichier markdown
     * @return Liste triée par nom de sort
     */
    fun parse(rawMarkdown: String): List<SrdEntry> {
        val allLines = rawMarkdown.lines()

        // Trouve la ligne "## Descriptions des sorts" — tout ce qui précède est ignoré
        val startIndex = allLines.indexOfFirst { line ->
            line.trim().equals(SPELL_DESCRIPTIONS_HEADER, ignoreCase = true)
        }

        if (startIndex == -1) {
            // Si on ne trouve pas le marqueur, on tente de parser depuis le début
            return parseSpells(allLines)
        }

        // On commence après le marqueur
        return parseSpells(allLines.drop(startIndex + 1))
    }

    /**
     * Parse les lignes contenant les sorts individuels (#### headings).
     */
    private fun parseSpells(lines: List<String>): List<SrdEntry> {
        val entries = mutableListOf<SrdEntry>()
        var currentName: String? = null
        var currentContent = StringBuilder()

        fun flushEntry() {
            val name = currentName ?: return
            val content = currentContent.toString().trim()
            if (content.isNotBlank()) {
                entries.add(
                    SrdEntry(
                        name = name,
                        category = extractSchool(content).orEmpty(),
                        rawMarkdown = content,
                    )
                )
            }
            currentName = null
            currentContent = StringBuilder()
        }

        for (line in lines) {
            val trimmed = line.trim()

            // Détecte un sort (#### Heading)
            if (trimmed.startsWith("#### ")) {
                val candidateName = trimmed.removePrefix("#### ").trim()
                // Si ce "#### " est un sous-titre (Traits, Actions, etc.), ce n'est pas un sort :
                // on ne crée pas de nouvelle entrée, on continue d'accumuler dans le sort courant.
                if (candidateName in NON_SPELL_HEADINGS) {
                    currentContent.appendLine(line)
                    continue
                }
                flushEntry()
                currentName = candidateName
                currentContent = StringBuilder()
                currentContent.appendLine(line)
                continue
            }

            // Une section ## arrête le sort courant (nouvelle section majeure)
            if (trimmed.startsWith("## ") && !trimmed.startsWith("### ") && !trimmed.startsWith("#### ")) {
                flushEntry()
                continue
            }

            // Si on est dans un sort, accumule le contenu
            if (currentName != null) {
                currentContent.appendLine(line)
            }
        }

        // Flush le dernier sort
        flushEntry()

        return entries.sortedBy { it.name.lowercase() }
    }

    private val schoolKeywords = listOf(
        "abjuration" to "Abjuration",
        "conjuration" to "Conjuration",
        "divination" to "Divination",
        "enchantement" to "Enchantement",
        "évocation" to "Évocation",
        "evocation" to "Évocation",
        "évation" to "Évocation",
        "evation" to "Évocation",
        "illusion" to "Illusion",
        "nécromancie" to "Nécromancie",
        "necromancie" to "Nécromancie",
        "transmutation" to "Transmutation",
        "trans du" to "Transmutation",
        "juration" to "Abjuration",
    )

    /**
     * Extrait l'école de magie depuis la ligne italique qui suit le titre d'un sort
     * (ex: "*Evocation de 2ème niveau*", "*Tour de magie de conjuration*"). Tolérant aux
     * artefacts OCR du document source (ex: "ab juration" -> Abjuration).
     */
    private fun extractSchool(content: String): String? {
        val firstItalicLine = content.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("*") && it.endsWith("*") && !it.startsWith("**") }
            ?.trim('*')
            ?.trim()
            ?.lowercase()
            ?: return null
        for ((keyword, school) in schoolKeywords) {
            if (firstItalicLine.contains(keyword)) return school
        }
        return null
    }
}