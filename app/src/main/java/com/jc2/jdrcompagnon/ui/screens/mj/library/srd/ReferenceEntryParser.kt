package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Découpe un document de référence rédigé en texte continu (rules.md, glossary.md) en entrées
 * individuelles, pour les afficher comme une liste d'objets (une règle = une entrée cliquable
 * ouvrant sa fiche) plutôt qu'en un seul long document.
 *
 * - Chaque titre de niveau 1 à 3 (`#`, `##`, `###`) ouvre une entrée ; les titres plus profonds
 *   (`####`...) restent dans le contenu de l'entrée qui les englobe.
 * - Catégorie d'une entrée : la section `##` qui la contient (elle-même pour un `##`), ou le
 *   chapitre `#` pour le texte d'introduction d'un chapitre.
 * - Une entrée sans texte propre (titre immédiatement suivi d'un sous-titre) est ignorée : ses
 *   sous-entrées la représentent.
 * - Un nom en double (ex. "Force" dans deux chapitres) est complété par sa catégorie, pour que
 *   chaque entrée reste retrouvable par son nom (navigation vers sa fiche).
 */
object ReferenceEntryParser {

    private val headingRegex = Regex("""^(#{1,6})\s+(.+?)\s*$""")

    fun parse(markdown: String): List<SrdSectionEntry> {
        val lines = stripFrontMatter(markdown).lines()
        data class Brouillon(val name: String, val category: String, val content: StringBuilder = StringBuilder())

        val brouillons = mutableListOf<Brouillon>()
        var chapitre = ""
        var section = ""
        var courant: Brouillon? = null

        for (line in lines) {
            val match = headingRegex.find(line)
            val level = match?.groupValues?.get(1)?.length ?: 0
            if (match != null && level <= 3) {
                val titre = match.groupValues[2].trim()
                when (level) {
                    1 -> { chapitre = titre; section = "" }
                    2 -> section = titre
                }
                val categorie = when (level) {
                    1 -> titre
                    else -> section.ifBlank { chapitre }
                }
                courant = Brouillon(titre, categorie).also { brouillons += it }
            } else {
                courant?.content?.appendLine(line)
            }
        }

        val entrees = brouillons
            .map { SrdSectionEntry(name = it.name, category = it.category, rawMarkdown = it.content.toString().trim()) }
            .filter { it.rawMarkdown.isNotBlank() }
        val doublons = entrees.groupingBy { it.name.lowercase() }.eachCount().filterValues { it > 1 }.keys
        return entrees.map { entree ->
            if (entree.name.lowercase() in doublons && !entree.name.equals(entree.category, ignoreCase = true)) {
                entree.copy(name = "${entree.name} (${entree.category})")
            } else {
                entree
            }
        }
    }

    /** Retire l'en-tête YAML (`---` ... `---`) d'un fichier exporté par pandoc. */
    private fun stripFrontMatter(markdown: String): String {
        val trimmed = markdown.trimStart()
        if (!trimmed.startsWith("---")) return markdown
        val end = trimmed.indexOf("\n---", startIndex = 3)
        return if (end < 0) markdown else trimmed.substring(end + 4)
    }
}
