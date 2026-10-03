package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Parser pour les fichiers d'équipement des deux mondes gérés par [SrdRepository] :
 * - `dnd/equipement_srd521.md` (D&D, SRD 5.2.1 FR) : des blocs "### Nom" contenant des
 *   champs en gras sans deux-points ("**Champ** valeur"), terminés par une ligne "---"
 *   (voir [parseBoldFields]). Le champ **Type** identifie la sorte d'objet (Arme, Armure,
 *   Outil, Paquetage, Matériel, Munition, Propriété, Devise) et sa présence sert de
 *   détection du format.
 * - `naheulbeuk/equipment.md` (Naheulbeuk V4) : sections "## " + items "- Nom : description"
 *   (voir [parseNaheulbeuk]), utilisé dès que le format ci-dessus n'est pas détecté.
 */
object EquipmentParser {

    /**
     * Détecte le format "### Nom" + champs en gras "**Champ** valeur" (sans deux-points) :
     * la présence d'une ligne "**Type**" suffit à l'identifier sans ambiguïté avec le
     * format Naheulbeuk (qui n'a pas de champs en gras).
     */
    private val boldFieldTypeRegex = Regex("""(?m)^\*\*Type\*\*\s""")

    /**
     * Parse le contenu markdown de l'équipement en une liste d'items typés.
     */
    fun parse(rawMarkdown: String): List<EquipmentItem> =
        if (boldFieldTypeRegex.containsMatchIn(rawMarkdown)) parseBoldFields(rawMarkdown)
        else parseNaheulbeuk(rawMarkdown)

    private val boldFieldRegex = Regex("""^\*\*(.+?)\*\*\s?(.*)$""")

    /**
     * Parse le format "### Nom" + champs "**Champ** valeur" de `guide_format_equipement.md`.
     * Chaque bloc se termine par une ligne "---" ; le champ **Contenu** (Paquetage) peut
     * être une liste à puces "- Quantité Nom" sur plusieurs lignes plutôt qu'une valeur en
     * ligne. Les champs sans équivalent direct dans [EquipmentItem] (Sous-catégorie,
     * Caractéristique, Utilisation type, Quantité, Rangement, Botte, Temps enfiler/retirer,
     * Conversion, Contenu, Consommable) sont conservés en texte libre à la suite de la
     * description, dans `rawMarkdown`, pour ne rien perdre.
     *
     * Objets à charges (livres personnalisés) :
     *   **Charges** 10                      (nombre maximum)
     *   **Recharge** 1d6 + 4                (récupérées à chaque repos long)
     *   **Pouvoir** Chant d'oiseau : ...    (utilisable depuis l'onglet Combat, objet équipé)
     *   **Destruction** 1                   (dernière charge : d20, détruit sur ce résultat ou moins)
     *
     * Objets à harmonisation (voir Harmonisation) :
     *   **Harmonisation** Oui               (harmonisation requise)
     *   **Harmonisation** Oui (magicien)    (avec prérequis entre parenthèses)
     * À défaut du champ, la mention « harmonisation requise » dans la description suffit.
     */
    private fun parseBoldFields(rawMarkdown: String): List<EquipmentItem> {
        val entries = mutableListOf<EquipmentItem>()
        // Le premier fragment (avant le tout premier "### ") est l'en-tête du fichier
        // (titre + Univers), ignoré.
        val blocks = rawMarkdown.split(Regex("""(?m)^### """)).drop(1)

        for (block in blocks) {
            val lines = block.lines()
            val name = lines.firstOrNull()?.trim().orEmpty()
            if (name.isBlank()) continue

            val fields = mutableMapOf<String, String>()
            var i = 1
            while (i < lines.size) {
                val trimmed = lines[i].trim()
                if (trimmed == "---") break
                val match = boldFieldRegex.find(trimmed)
                if (match != null) {
                    val fieldName = match.groupValues[1].trim()
                    var fieldValue = match.groupValues[2].trim()
                    if (fieldValue.isEmpty()) {
                        // Champ multi-lignes en liste à puces (ex. Contenu d'un Paquetage).
                        val subLines = mutableListOf<String>()
                        var j = i + 1
                        while (j < lines.size && lines[j].trim().startsWith("- ")) {
                            subLines.add(lines[j].trim().removePrefix("- ").trim())
                            j++
                        }
                        if (subLines.isNotEmpty()) {
                            fields[fieldName] = subLines.joinToString(", ")
                            i = j
                            continue
                        }
                    }
                    fields[fieldName] = fieldValue
                }
                i++
            }

            fun field(key: String): String = fields[key].orEmpty().let { if (it == "-") "" else it }

            val type = fields["Type"].orEmpty()
            val category = buildBoldFieldCategoryLabel(type, fields)

            val extraLines = buildList {
                field("Sous-catégorie").takeIf { it.isNotBlank() }?.let { add("Sous-catégorie : $it") }
                field("Caractéristique").takeIf { it.isNotBlank() }?.let { add("Caractéristique : $it") }
                field("Utilisation type").takeIf { it.isNotBlank() }?.let { add("Utilisation type : $it") }
                field("Quantité").takeIf { it.isNotBlank() }?.let { add("Quantité : $it") }
                field("Rangement").takeIf { it.isNotBlank() }?.let { add("Rangement : $it") }
                field("Botte").takeIf { it.isNotBlank() }?.let { add("Botte : $it") }
                field("Temps enfiler").takeIf { it.isNotBlank() }?.let { add("Temps pour enfiler : $it") }
                field("Temps retirer").takeIf { it.isNotBlank() }?.let { add("Temps pour retirer : $it") }
                field("Conversion").takeIf { it.isNotBlank() }?.let { add("Conversion : $it") }
                field("Contenu").takeIf { it.isNotBlank() }?.let { add("Contenu : $it") }
                field("Consommable").takeIf { it.isNotBlank() }?.let { add("Consommable : $it") }
            }

            val description = field("Description")
            val rawContent = buildString {
                append(description)
                if (extraLines.isNotEmpty()) {
                    if (isNotEmpty()) append("\n\n")
                    append(extraLines.joinToString("\n"))
                }
            }.trim()

            val cost = fields["Coût"]?.let { if (it == "-") "" else it } ?: field("Conversion")
            val (harmonisation, prerequis) = parseHarmonisation(field("Harmonisation"), description)

            entries.add(
                EquipmentItem(
                    name = name,
                    category = category,
                    cost = cost,
                    weight = field("Poids"),
                    damage = field("Dégâts"),
                    ac = field("CA"),
                    strength = field("Force requise"),
                    stealth = field("Discrétion"),
                    properties = field("Propriétés"),
                    rawMarkdown = rawContent,
                    charges = field("Charges").let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                    recharge = field("Recharge"),
                    pouvoir = field("Pouvoir"),
                    destruction = field("Destruction").let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                    consommable = field("Consommable").equals("Oui", ignoreCase = true),
                    harmonisation = harmonisation,
                    harmonisationPrerequis = prerequis,
                )
            )
        }
        return entries
    }

    private val harmonisationDescriptionRegex =
        Regex("""harmonisation requise(?:\s+(?:avec|par)\s+(?:un|une|des)?\s*([^).]+))?""", RegexOption.IGNORE_CASE)

    /**
     * Champ **Harmonisation** ("Oui", "Oui (magicien)", "Non") → (requise, prérequis). Sans champ,
     * repli sur la mention « harmonisation requise (avec un magicien) » de la description.
     */
    internal fun parseHarmonisation(champ: String, description: String): Pair<Boolean, String> {
        if (champ.isNotBlank()) {
            val requise = !champ.trim().startsWith("non", ignoreCase = true)
            val prerequis = Regex("""\(([^)]*)\)""").find(champ)?.groupValues?.get(1)?.trim().orEmpty()
            return requise to (if (requise) prerequis else "")
        }
        val m = harmonisationDescriptionRegex.find(description) ?: return false to ""
        return true to m.groupValues[1].trim()
    }

    /**
     * Construit un libellé de catégorie lisible à partir de **Type** (+ **Catégorie** ou
     * **Sous-catégorie** selon le type) — sert à la fois de titre de section (sticky
     * header) et de filtre dans l'onglet Équipement. Contient toujours "arme"/"armure"
     * en minuscule pour les types concernés, condition dont dépendent l'affichage des
     * dégâts/propriétés (armes) et CA/Force/Discrétion (armures) dans l'UI.
     */
    private fun buildBoldFieldCategoryLabel(type: String, fields: Map<String, String>): String {
        fun sub(key: String): String? = fields[key]?.takeIf { it.isNotBlank() && it != "-" }
        return when (type) {
            "Arme" -> "Armes" + (sub("Catégorie")?.let { " $it" } ?: "")
            "Armure" -> "Armures" + (sub("Catégorie")?.let { " $it" } ?: "")
            "Outil" -> "Outils" + (sub("Sous-catégorie")?.let { " $it" } ?: "")
            "Paquetage" -> "Paquetages"
            "Vêtements" -> "Vêtements"
            "Matériel" -> "Matériel d'aventurier"
            "Munition" -> "Munitions"
            "Propriété" -> "Propriétés" + (sub("Catégorie")?.let { " ($it)" } ?: "")
            "Devise" -> "Monnaie"
            else -> type.ifBlank { "Équipement" }
        }
    }

    /**
     * Parse le format Naheulbeuk V4 : sections "## " + items "- Nom : description".
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