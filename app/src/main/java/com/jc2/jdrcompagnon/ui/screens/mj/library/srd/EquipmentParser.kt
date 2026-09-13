package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour les fichiers equipment.md.
 *
 * Format SRD 5.1 FR (structuré, "base de données") :
 * Chaque objet est un bloc de la forme :
 *
 *   ### Nom de l'objet
 *   Catégorie: Armes
 *   Sous-catégorie: Armes de mêlée martiales   (optionnel)
 *   Coût: 15 gp                                 (optionnel)
 *   Poids: 3 lb.                                (optionnel)
 *   Dégâts: 1d8 tranchant                       (armes)
 *   CA: 16                                      (armure)
 *   Force: Str 13                               (armure, optionnel)
 *   Discrétion: Désavantage                     (armure, optionnel)
 *   Propriétés: Polyvalent (1d10)                (armes / objets magiques / montures)
 *
 *   Description en texte libre sur une ou plusieurs lignes...
 *
 * Chaque ligne "Champ: valeur" avant la première ligne vide est un champ ; tout ce qui suit
 * la première ligne vide est la description (affichée telle quelle dans le détail de l'objet).
 * La catégorie du fichier ("## ") n'est plus utilisée pour le SRD : chaque item porte sa propre
 * catégorie, ce qui rend le fichier robuste à l'ordre et à la mise en page.
 *
 * Format Naheulbeuk V4 (inchangé) :
 * - Les sections principales sont marquées par `## `.
 * - Les items individuels sont des listes `- Nom : description`.
 */
object EquipmentParser {

    private val structuredItemRegex = Regex("""(?m)^### .+\r?\nCatégorie:""")

    /**
     * Parse le contenu markdown de l'équipement en une liste d'items typés.
     */
    fun parse(rawMarkdown: String): List<EquipmentItem> {
        return if (structuredItemRegex.containsMatchIn(rawMarkdown)) {
            parseStructured(rawMarkdown)
        } else {
            parseNaheulbeuk(rawMarkdown)
        }
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