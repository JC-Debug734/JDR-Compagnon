package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser dédié à `dons_srd521.md`, décrit par `guide_format_dons.md` : chaque don est un
 * bloc "### Nom" avec ses champs en gras ("**Champ** valeur", sans deux-points), suivi
 * d'un ou plusieurs sous-blocs "#### Nom de compétence" (ses bénéfices), chacun avec ses
 * propres champs. La catégorie (Origines, Général, Style de combat, Faveur épique) vient de
 * la balise `<!-- id: …; categorie: … -->` du don.
 *
 * Produit une liste de [SrdSectionEntry], une par don, avec [SrdSectionEntry.rawMarkdown]
 * reconstruit en markdown à structure unique — lu tel quel par l'écran de détail de la
 * bibliothèque ([com.mikepenz.markdown.m3.Markdown]) et par la section « Dons » de la fiche
 * (CapaciteDescription) :
 *
 * ```
 * *Don — Origines*
 *
 * - **Prérequis** : …            (champs du don, « - » omis)
 *
 * ### Nom de la compétence       (« Utilisation » si c'est le nom du don)
 * - **Type** : Actif
 * - **Coût** : …                 (champs de la compétence, « - » omis)
 *
 * Effet de la compétence.
 * ```
 */
object DonsParser {

    /** Catégorie d'un don sans balise `categorie:` (ex. don personnalisé mal balisé). */
    private const val CATEGORIE_PAR_DEFAUT = "Général"

    private val boldFieldRegex = Regex("""^\*\*(.+?)\*\*\s?(.*)$""")
    private val categorieRegex = Regex("""categorie:\s*([^;>]+?)\s*(?:;|-->)""")

    private val donSplitRegex = Regex("""(?m)^### """)
    private val skillSplitRegex = Regex("""(?m)^#### """)

    private val donFieldOrder = listOf("Prérequis", "Répétable", "Augmentation de caractéristique", "Compétences")
    private val skillFieldOrder = listOf("Coût", "Condition", "Limitation", "Récupération")

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
            val entete = skillSplit.first()
            val donFields = parseBoldFields(entete.lines().drop(1))
            val skillBlocks = skillSplit.drop(1)
            val balises = entete.lines().map { it.trim() }.filter { it.startsWith("<!--") && it.endsWith("-->") }
            val categorie = balises.firstNotNullOfOrNull { categorieRegex.find(it)?.groupValues?.get(1) } ?: CATEGORIE_PAR_DEFAUT

            val rawContent = buildString {
                // Balises du don (catégorie, choix à la création), conservées telles quelles :
                // lues par DonParser.depuisEntrees, invisibles à l'affichage.
                balises.forEach { appendLine(it) }
                appendLine("*Don — $categorie*")
                appendLine()
                donFieldOrder.forEach { label ->
                    valeur(donFields[label])?.let { appendLine("- **$label** : $it") }
                }
                appendLine()

                skillBlocks.forEach { skillBlock ->
                    val skillLines = skillBlock.lines()
                    val skillName = skillLines.firstOrNull()?.trim().orEmpty()
                    if (skillName.isBlank()) return@forEach
                    val skillFields = parseBoldFields(skillLines.drop(1))

                    appendLine("### " + if (skillName.equals(donName, ignoreCase = true)) "Utilisation" else skillName)
                    valeur(skillFields["Catégorie"])?.let { appendLine("- **Type** : $it") }
                    skillFieldOrder.forEach { label ->
                        valeur(skillFields[label])?.let { appendLine("- **$label** : $it") }
                    }
                    skillFields["Effet"]?.takeIf { it.isNotBlank() }?.let {
                        appendLine()
                        appendLine(it)
                    }
                    appendLine()
                }
            }.trim()

            entries.add(SrdSectionEntry(name = donName, category = categorie, rawMarkdown = rawContent))
        }
        return entries
    }

    /** Valeur affichable d'un champ : null si absent, vide ou « - ». */
    private fun valeur(brut: String?): String? = brut?.trim()?.takeIf { it.isNotBlank() && it != "-" }

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
