package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour les fichiers equipment.md.
 *
 * Format SRD 5.2.1 FR réel (celui utilisé par `equipement_srd521.md`) : des tableaux
 * Markdown ("| Colonne | ... |") sous des titres "## " (catégorie principale : Armes,
 * Armures, Outils, Paquetages, Matériel d'aventurier) et parfois "### " (sous-catégorie,
 * ex. "Armes courantes de corps à corps", "Outils d'artisan"). Chaque ligne de données
 * d'une table devient un [EquipmentItem], la première colonne étant toujours le nom.
 * Seules les tables comportant une colonne "Prix" ou "Poids" sont retenues : ça exclut
 * les tables de référence (ex. "Propriétés des armes", "Bottes d'arme") qui ne décrivent
 * pas des objets achetables. Le tableau des armures utilise en plus des lignes de
 * séparation en gras ("| **Légères** *(...)* | | | | | |") pour sous-catégoriser sans
 * titre "### " dédié ; elles sont détectées (première cellule commençant par "**", le
 * reste vide ou "—") et non transformées en objet.
 *
 * Anciens formats conservés en repli (pour compatibilité avec d'éventuels autres
 * fichiers) :
 * - Format structuré "### Nom" + champs "Clé: valeur" + description (voir [parseStructured]).
 * - Format Naheulbeuk V4 : sections "## " + items "- Nom : description".
 */
object EquipmentParser {

    private val structuredItemRegex = Regex("""(?m)^### .+$""")

    /**
     * Parse le contenu markdown de l'équipement en une liste d'items typés.
     */
    fun parse(rawMarkdown: String): List<EquipmentItem> {
        return when {
            containsItemTable(rawMarkdown) -> parseTableFormat(rawMarkdown)
            structuredItemRegex.containsMatchIn(rawMarkdown) && rawMarkdown.contains("Catégorie:") ->
                parseStructured(rawMarkdown)
            else -> parseNaheulbeuk(rawMarkdown)
        }
    }

    /**
     * Indique si le document contient au moins une table Markdown avec une colonne
     * "Prix" ou "Poids" dans son en-tête (heuristique pour distinguer une table
     * d'objets d'une simple table de référence).
     */
    private fun containsItemTable(rawMarkdown: String): Boolean =
        rawMarkdown.lineSequence().any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("|") && trimmed.endsWith("|") &&
                    splitRow(trimmed).any { it.equals("Prix", ignoreCase = true) || it.equals("Poids", ignoreCase = true) }
        }

    /**
     * Découpe une ligne de table Markdown "| a | b | c |" en cellules ["a", "b", "c"].
     */
    private fun splitRow(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

    /**
     * Parse le format réel à base de tableaux Markdown groupés par titres "## "/"### ".
     */
    private fun parseTableFormat(rawMarkdown: String): List<EquipmentItem> {
        val entries = mutableListOf<EquipmentItem>()
        val lines = rawMarkdown.lines()
        var currentCategory = ""
        var currentSubcategory = ""
        var i = 0

        while (i < lines.size) {
            val trimmed = lines[i].trim()
            when {
                trimmed.startsWith("### ") -> {
                    currentSubcategory = trimmed.removePrefix("### ").trim()
                    i++
                }
                trimmed.startsWith("## ") -> {
                    currentCategory = trimmed.removePrefix("## ").trim()
                    currentSubcategory = ""
                    i++
                }
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().let { it.startsWith("|") && it.endsWith("|") }) {
                        tableLines.add(lines[i].trim())
                        i++
                    }
                    entries += parseItemTable(tableLines, currentCategory, currentSubcategory)
                }
                else -> i++
            }
        }
        return entries
    }

    /**
     * Parse une table Markdown en items, si sa colonne d'en-tête contient bien "Prix"
     * ou "Poids" (sinon c'est une table de référence, pas une liste d'objets).
     */
    private fun parseItemTable(
        tableLines: List<String>,
        category: String,
        subcategory: String,
    ): List<EquipmentItem> {
        // Header + séparateur + au moins une ligne de données
        if (tableLines.size < 3) return emptyList()
        val headerCells = splitRow(tableLines[0])
        if (headerCells.none { it.equals("Prix", ignoreCase = true) || it.equals("Poids", ignoreCase = true) }) {
            return emptyList()
        }

        fun colIndex(label: String): Int = headerCells.indexOfFirst { it.equals(label, ignoreCase = true) }

        val idxDegats = colIndex("Dégâts")
        val idxCa = headerCells.indexOfFirst { it.contains("CA") || it.contains("Classe d'armure") }
        val idxForce = colIndex("Force")
        val idxDiscretion = colIndex("Discrétion")
        val idxPoids = colIndex("Poids")
        val idxPrix = colIndex("Prix")
        val idxProprietes = colIndex("Propriétés")
        val idxBotte = colIndex("Botte")
        val idxCaracteristique = colIndex("Caractéristique")
        val idxUtilisation = headerCells.indexOfFirst { it.startsWith("Utilisation") }
        val idxContenu = colIndex("Contenu")
        val idxQuantite = colIndex("Quantité")
        val idxRangement = colIndex("Rangement")

        var subLabel = subcategory
        val results = mutableListOf<EquipmentItem>()

        // Les deux premières lignes sont l'en-tête et le séparateur "|---|---|"
        for (rowLine in tableLines.drop(2)) {
            val cells = splitRow(rowLine)
            if (cells.isEmpty()) continue
            val firstCell = cells[0]
            if (firstCell.isBlank()) continue

            fun cell(index: Int): String {
                val value = cells.getOrNull(index)?.trim().orEmpty()
                return if (value == "—" || value == "-") "" else value
            }

            // Ligne de séparation en gras (ex. "**Légères** *(enfiler/retirer : 1 min)*")
            // qui sous-catégorise les lignes suivantes sans être un objet elle-même.
            val isDividerRow = firstCell.startsWith("**") && cells.drop(1).all { c ->
                c.isBlank() || c.trim() == "—" || c.trim() == "-"
            }
            if (isDividerRow) {
                subLabel = firstCell.replace("*", "").substringBefore("(").trim()
                continue
            }

            val name = firstCell.replace("*", "").trim()
            if (name.isBlank()) continue

            val extraLines = buildList {
                if (idxCaracteristique >= 0) cell(idxCaracteristique).takeIf { it.isNotBlank() }
                    ?.let { add("Caractéristique : $it") }
                if (idxUtilisation >= 0) cell(idxUtilisation).takeIf { it.isNotBlank() }
                    ?.let { add("Utilisation type (DD) : $it") }
                if (idxContenu >= 0) cell(idxContenu).takeIf { it.isNotBlank() }
                    ?.let { add("Contenu : $it") }
                if (idxQuantite >= 0) cell(idxQuantite).takeIf { it.isNotBlank() }
                    ?.let { add("Quantité : $it") }
                if (idxRangement >= 0) cell(idxRangement).takeIf { it.isNotBlank() }
                    ?.let { add("Rangement : $it") }
                if (idxBotte >= 0) cell(idxBotte).takeIf { it.isNotBlank() }
                    ?.let { add("Botte : $it") }
            }

            val fullCategory = if (subLabel.isNotBlank() && subLabel != category) {
                "$category ($subLabel)"
            } else {
                category
            }

            results.add(
                EquipmentItem(
                    name = name,
                    category = fullCategory,
                    cost = if (idxPrix >= 0) cell(idxPrix) else "",
                    weight = if (idxPoids >= 0) cell(idxPoids) else "",
                    damage = if (idxDegats >= 0) cell(idxDegats) else "",
                    ac = if (idxCa >= 0) cell(idxCa) else "",
                    strength = if (idxForce >= 0) cell(idxForce) else "",
                    stealth = if (idxDiscretion >= 0) cell(idxDiscretion) else "",
                    properties = if (idxProprietes >= 0) cell(idxProprietes) else "",
                    rawMarkdown = extraLines.joinToString("\n"),
                )
            )
        }
        return results
    }

    /**
     * Parse le format structuré "### Nom" + champs "Clé: valeur" + description.
     */
    private fun parseStructured(rawMarkdown: String): List<EquipmentItem> {
        val entries = mutableListOf<EquipmentItem>()
        // Découpe sur chaque "### " en début de ligne ; le premier fragment (avant le tout
        // premier item) est l'introduction du fichier et est ignoré.
        val blocks = rawMarkdown.split(Regex("""(?m)^### """)).drop(1)

        val fieldLineRegex = Regex("""^([\p{L}][\p{L} '-]*):\s*(.*)$""")

        for (block in blocks) {
            val lines = block.lines()
            val name = lines.firstOrNull()?.trim().orEmpty()
            if (name.isBlank()) continue

            val fields = mutableMapOf<String, String>()
            var i = 1
            // Tolère une (ou plusieurs) ligne(s) vide(s) entre le titre et les champs.
            while (i < lines.size && lines[i].isBlank()) i++
            while (i < lines.size) {
                val line = lines[i]
                val match = fieldLineRegex.find(line)
                if (match != null) {
                    fields[match.groupValues[1].trim()] = match.groupValues[2].trim()
                    i++
                } else {
                    break
                }
            }
            while (i < lines.size && lines[i].isBlank()) i++
            val description = lines.drop(i).joinToString("\n").trim()

            val subcategory = fields["Sous-catégorie"]
            val rawContent = buildString {
                if (!subcategory.isNullOrBlank()) {
                    appendLine("*$subcategory*")
                    appendLine()
                }
                append(description)
            }.trim()

            entries.add(
                EquipmentItem(
                    name = name,
                    category = fields["Catégorie"].orEmpty(),
                    cost = fields["Coût"].orEmpty(),
                    weight = fields["Poids"].orEmpty(),
                    damage = fields["Dégâts"].orEmpty(),
                    ac = fields["CA"].orEmpty(),
                    strength = fields["Force"].orEmpty(),
                    stealth = fields["Discrétion"].orEmpty(),
                    properties = fields["Propriétés"].orEmpty(),
                    rawMarkdown = rawContent
                )
            )
        }
        return entries
    }

    /**
     * Parse l'ancien format Naheulbeuk : sections "## " + items "- Nom : description".
     */
    private fun parseNaheulbeuk(rawMarkdown: String): List<EquipmentItem> {
        val entries = mutableListOf<EquipmentItem>()
        var currentCategory = ""
        val itemRegex = Regex("""^-\s+([^:]+):\s*(.*)$""")

        for (line in rawMarkdown.lines()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("## ")) {
                currentCategory = trimmed.removePrefix("## ").trim()
                continue
            }
            val match = itemRegex.find(trimmed)
            if (match != null) {
                val name = match.groupValues[1].trim().removeSuffix(".").trim()
                val description = match.groupValues[2].trim()
                entries.add(
                    EquipmentItem(
                        name = name,
                        category = currentCategory,
                        rawMarkdown = "**$name.** $description"
                    )
                )
            }
        }
        return entries
    }
}