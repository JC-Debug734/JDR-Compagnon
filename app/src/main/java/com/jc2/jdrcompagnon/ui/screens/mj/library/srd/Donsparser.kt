package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser dédié à `dons_srd521.md`, décrit par `guide_format_dons.md` : chaque don est un
 * bloc "### Nom" avec ses champs en gras ("**Champ** valeur", sans deux-points), suivi
 * d'un ou plusieurs sous-blocs "#### Nom de compétence" (ses bénéfices), chacun avec ses
 * propres champs. Contrairement à `equipement_srd521.md`, il n'y a pas de champ
 * `Description` au niveau du don, et la catégorie (Origines/Général/Style de combat/
 * Faveur épique) a été volontairement retirée — voir le guide, §2 et §6.3.
 *
 * Produit une liste de [SrdSectionEntry], une par don, avec [SrdSectionEntry.rawMarkdown]
 * reconstruit en markdown lisible (champs du don + une section par compétence) pour
 * l'écran de détail, qui l'affiche tel quel via [com.mikepenz.markdown.m3.Markdown].
 *
 * [SrdSectionEntry.category] vaut toujours [CATEGORY_LABEL] pour tous les dons : ce champ
 * n'existe plus dans la source (voir ci-dessus), mais le laisser vide ferait disparaître
 * les dons de [SectionEntryByCategoryList], qui filtre les entrées à catégorie vide.
 */
object DonsParser {

    private const val CATEGORY_LABEL = "Dons"

    private val boldFieldRegex = Regex("""^\*\*(.+?)\*\*\s?(.*)$""")

    private val donSplitRegex = Regex("""(?m)^### """)
    private val skillSplitRegex = Regex("""(?m)^#### """)

    private val donFieldOrder = listOf(
        "Prérequis" to "Prérequis",
        "Répétable" to "Répétable",
        "Augmentation de caractéristique" to "Augmentation de caractéristique",
        "Compétences" to "Compétences",
    )
    private val skillFieldOrder = listOf(
        "Coût" to "Coût",
        "Condition" to "Condition",
        "Limitation" to "Limitation",
        "Récupération" to "Récupération",
    )

    fun parse(rawMarkdown: String): List<SrdSectionEntry> {
        val entries = mutableListOf<SrdSectionEntry>()
        // Le premier fragment (avant le tout premier "### ") est l'en-tête du fichier
        // (titre + Univers), ignoré.
        val donBlocks = rawMarkdown.split(donSplitRegex).drop(1)

        for (block in donBlocks) {
            val lines = block.lines()
            val donName = lines.firstOrNull()?.trim().orEmpty()
            if (donName.isBlank()) continue

            // "### " a isolé le don (et toutes ses compétences) du don suivant ; on
            // sépare maintenant ses propres champs (avant la première "#### ") de ses
            // sous-blocs de compétence, un niveau plus profond.
            val skillSplit = block.split(skillSplitRegex)
            val donFields = parseBoldFields(skillSplit.first().lines().drop(1))
            val skillBlocks = skillSplit.drop(1)

            val rawContent = buildString {
                donFieldOrder.forEach { (label, key) ->
                    val value = donFields[key]?.takeIf { it.isNotBlank() && it != "-" }
                    if (value != null) {
                        appendLine("**$label :** $value")
                        appendLine()
                    }
                }

                skillBlocks.forEach { skillBlock ->
                    val skillLines = skillBlock.lines()
                    val skillName = skillLines.firstOrNull()?.trim().orEmpty()
                    if (skillName.isBlank()) return@forEach
                    val skillFields = parseBoldFields(skillLines.drop(1))

                    appendLine("### $skillName")
                    val category = skillFields["Catégorie"]?.takeIf { it.isNotBlank() && it != "-" }
                    if (category != null) {
                        appendLine("*$category*")
                    }
                    appendLine()

                    skillFieldOrder.forEach { (label, key) ->
                        val value = skillFields[key]?.takeIf { it.isNotBlank() && it != "-" }
                        if (value != null) {
                            appendLine("- $label : $value")
                        }
                    }

                    val effet = skillFields["Effet"]?.takeIf { it.isNotBlank() }
                    if (effet != null) {
                        appendLine()
                        appendLine(effet)
                    }
                    appendLine()
                }
            }.trim()

            entries.add(
                SrdSectionEntry(
                    name = donName,
                    category = CATEGORY_LABEL,
                    rawMarkdown = rawContent,
                )
            )
        }
        return entries
    }

    /**
     * Parse une suite de lignes "**Champ** valeur" en table champ → valeur, en s'arrêtant
     * à la ligne "---" si elle est atteinte (fin de fichier sur le dernier don).
     */
    private fun parseBoldFields(lines: List<String>): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed == "---") break
            val match = boldFieldRegex.find(trimmed) ?: continue
            fields[match.groupValues[1].trim()] = match.groupValues[2].trim()
        }
        return fields
    }
}