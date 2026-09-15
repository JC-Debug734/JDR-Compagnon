package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour le fichier unique `sorts_srd521.md` (SRD 5.2.1 FR), format structuré
 * "base de données" (comme [EquipmentParser]).
 *
 * Chaque sort est un bloc de la forme :
 *
 *   ### Nom du sort
 *   École: Transmutation
 *   Niveau: 2e niveau                            (ou "sort mineur" pour un tour de magie)
 *   Classes: Barde, Druide, Ensorceleur, Magicien
 *   Temps d'incantation: action
 *   Portée: 9 m
 *   Composantes: V, S, M (une pincée de poudre de fer)
 *   Durée: Concentration, jusqu'à 1 minute
 *
 *   Description en texte libre sur une ou plusieurs lignes...
 *
 * Ce fichier est la source unique pour les sorts : [parse] produit les entrées
 * détaillées utilisées par l'écran de détail d'un sort ([SrdEntry], catégorie = école),
 * et [parseIndex] produit la liste filtrable par niveau de l'onglet Sorts de la
 * bibliothèque ([SrdSectionEntry], catégorie = "Sorts mineurs" / "Sorts de niveau N").
 * Les deux passent par [parseBlocks], qui ne lit et ne découpe le fichier qu'une seule
 * fois : il n'y a plus qu'un seul fichier et qu'une seule extraction des champs.
 *
 * Le fichier contient 337 sorts.
 */
object SpellParser {

    private val fieldLineRegex = Regex("""^([\p{L}][\p{L} '’-]*):\s*(.*)$""")

    private data class ParsedSpell(
        val name: String,
        val ecole: String,
        val niveau: String,
        val classes: String,
        val temps: String,
        val portee: String,
        val composantes: String,
        val duree: String,
        val description: String,
    )

    /**
     * Parse le contenu markdown en entrées détaillées, triées par nom de sort.
     * [SrdEntry.category] contient l'école de magie (affichée par [SpellDetailScreen]
     * sous le titre : "École de magie : ...").
     */
    fun parse(rawMarkdown: String): List<SrdEntry> =
        parseBlocks(rawMarkdown)
            .map { p ->
                SrdEntry(
                    name = p.name,
                    category = p.ecole,
                    rawMarkdown = buildDetailMarkdown(p),
                )
            }
            .sortedBy { it.name.lowercase() }

    /**
     * Parse le contenu markdown en entrées d'index pour le filtre de la bibliothèque,
     * groupées par niveau ("Sorts mineurs", "Sorts de niveau 1", ...) et triées par
     * niveau croissant puis par nom. [SrdSectionEntry.rawMarkdown] contient l'école de
     * magie, affichée en sous-titre sous chaque sort dans la liste (le niveau étant déjà
     * porté par le regroupement en catégories).
     */
    fun parseIndex(rawMarkdown: String): List<SrdSectionEntry> =
        parseBlocks(rawMarkdown)
            .map { p -> p to levelCategory(p.niveau) }
            .sortedWith(compareBy({ levelSortKey(it.second) }, { it.first.name.lowercase() }))
            .map { (p, category) ->
                SrdSectionEntry(name = p.name, category = category, rawMarkdown = p.ecole)
            }

    /**
     * Découpe le fichier en blocs "### Nom" et en extrait les champs. Effectué une seule
     * fois : [parse] et [parseIndex] partagent ce résultat plutôt que de relire ou
     * reparser le fichier chacun de leur côté.
     */
    private fun parseBlocks(rawMarkdown: String): List<ParsedSpell> {
        val blocks = rawMarkdown.split(Regex("""(?m)^### """)).drop(1)
        val result = mutableListOf<ParsedSpell>()

        for (block in blocks) {
            val lines = block.lines()
            val name = lines.firstOrNull()?.trim().orEmpty()
            if (name.isBlank()) continue

            val fields = mutableMapOf<String, String>()
            var i = 1
            while (i < lines.size) {
                val match = fieldLineRegex.find(lines[i])
                if (match != null) {
                    fields[match.groupValues[1].trim()] = match.groupValues[2].trim()
                    i++
                } else {
                    break
                }
            }
            while (i < lines.size && lines[i].isBlank()) i++
            val description = lines.drop(i).joinToString("\n").trim()

            result.add(
                ParsedSpell(
                    name = name,
                    ecole = fields["École"].orEmpty(),
                    niveau = fields["Niveau"].orEmpty(),
                    classes = fields["Classes"].orEmpty(),
                    temps = fields["Temps d'incantation"].orEmpty(),
                    portee = fields["Portée"].orEmpty(),
                    composantes = fields["Composantes"].orEmpty(),
                    duree = fields["Durée"].orEmpty(),
                    description = description,
                )
            )
        }
        return result
    }

    /**
     * Construit le markdown affiché dans le détail d'un sort : ligne d'école/niveau en
     * italique, bloc de champs (classes, temps d'incantation, portée, composantes,
     * durée) en gras, puis la description.
     */
    private fun buildDetailMarkdown(p: ParsedSpell): String = buildString {
        val ecoleNiveau = listOfNotNull(p.ecole.ifBlank { null }, p.niveau.ifBlank { null })
            .joinToString(" — ")
        if (ecoleNiveau.isNotBlank()) {
            appendLine("*$ecoleNiveau*")
            appendLine()
        }
        if (p.classes.isNotBlank()) appendLine("**Classes :** ${p.classes}")
        if (p.temps.isNotBlank()) appendLine("**Temps d'incantation :** ${p.temps}")
        if (p.portee.isNotBlank()) appendLine("**Portée :** ${p.portee}")
        if (p.composantes.isNotBlank()) appendLine("**Composantes :** ${p.composantes}")
        if (p.duree.isNotBlank()) appendLine("**Durée :** ${p.duree}")
        if (isNotEmpty()) appendLine()
        append(p.description)
    }.trim()

    /**
     * Normalise le champ "Niveau" ("2e niveau", "sort mineur") en catégorie d'affichage
     * ("Sorts de niveau 2", "Sorts mineurs").
     */
    private fun levelCategory(niveau: String): String {
        if (niveau.contains("mineur", ignoreCase = true)) return "Sorts mineurs"
        val n = Regex("""\d+""").find(niveau)?.value
        return if (n != null) "Sorts de niveau $n" else niveau.ifBlank { "Sorts" }
    }

    /** Clé de tri pour ordonner les catégories de niveau : mineur d'abord, puis 1 à 9. */
    private fun levelSortKey(category: String): Int {
        if (category == "Sorts mineurs") return 0
        return Regex("""\d+""").find(category)?.value?.toIntOrNull() ?: 99
    }
}