package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Décompte des entrées reconnues lors du dernier import, affiché à l'utilisateur juste
 * après l'ajout d'un livre pour confirmer ce qui a été intégré à la bibliothèque unifiée.
 */
data class CustomContentSummary(
    val monsters: Int = 0,
    val pnjs: Int = 0,
    val equipment: Int = 0,
    val spells: Int = 0,
    val classes: Int = 0,
    val sousClasses: Int = 0,
    val especes: Int = 0,
    val historiques: Int = 0,
    val dons: Int = 0,
    val rules: Int = 0,
    val unrecognized: List<String> = emptyList(),
) {
    val total: Int get() = monsters + pnjs + equipment + spells + classes + sousClasses + especes + historiques + dons + rules
}

/**
 * Résultat du parsing d'un livre personnalisé : une liste par type d'entité, dans les
 * mêmes types que [SrdRepository] utilise pour le contenu officiel, prêtes à être
 * fusionnées dans ses caches.
 */
data class CustomContentResult(
    val monsters: List<SrdEntry> = emptyList(),
    // Profils au format monstre marqués `<!-- type: pnj -->` : pas ajoutés au bestiaire, mais
    // convertis en fiches PNJ de l'univers à l'import du livre (voir PnjImport).
    val pnjs: List<SrdEntry> = emptyList(),
    val equipment: List<EquipmentItem> = emptyList(),
    val spells: List<SrdEntry> = emptyList(),
    val spellsIndex: List<SrdSectionEntry> = emptyList(),
    val classes: List<SrdSectionEntry> = emptyList(),
    // Sous-classes `<!-- type: sousclasse -->` : rattachées à leur classe (champ "- classe :",
    // repris dans [SrdSectionEntry.category]) au chargement des classes, voir
    // SrdRepository.fusionnerSousClasses.
    val sousClasses: List<SrdSectionEntry> = emptyList(),
    val especes: List<SrdSectionEntry> = emptyList(),
    val historiques: List<SrdSectionEntry> = emptyList(),
    val dons: List<SrdSectionEntry> = emptyList(),
    val rules: List<RuleSection> = emptyList(),
    val unrecognized: List<String> = emptyList(),
) {
    val summary: CustomContentSummary
        get() = CustomContentSummary(
            monsters = monsters.size,
            pnjs = pnjs.size,
            equipment = equipment.size,
            spells = spells.size,
            classes = classes.size,
            sousClasses = sousClasses.size,
            especes = especes.size,
            historiques = historiques.size,
            dons = dons.size,
            rules = rules.size,
            unrecognized = unrecognized,
        )
}

/**
 * Parse un fichier markdown ajouté par l'utilisateur dans la bibliothèque, qui peut mêler
 * librement plusieurs types de contenu (un nouvel objet, un nouveau monstre, une nouvelle
 * règle...) dans un seul fichier. Chaque entrée doit être annotée d'un commentaire
 * `<!-- type: xxx -->` juste après son titre, indiquant à quel parseur existant
 * ([MonsterParser], [EquipmentParser], [SpellParser], [DonsParser], [RulesParser] ou
 * [MarkdownSectionParser]) la déléguer — les entrées non annotées sont ignorées et
 * remontées dans [CustomContentResult.unrecognized] pour que l'utilisateur les corrige.
 *
 * Types reconnus :
 * - Niveau "## " (comme les fichiers classes/espèces/historiques/rules du SRD) :
 *   `classe`, `sousclasse`/`sous_classe`, `espece`/`espèce`, `historique`, `regle`/`règle`.
 *   Une sous-classe désigne sa classe par un champ `- classe : Paladin` ; ses aptitudes
 *   s'écrivent comme celles d'une classe (`#### Niveau N : Nom` + `<!-- id: ...; type: ... -->`)
 *   et ses sorts toujours préparés dans une table `| Niveau de Paladin | Sorts |` ou une liste
 *   `- **Niveau 3** : sort, sort` dans le texte d'une aptitude.
 * - Niveau "### ", imbriqué dans un "## " de regroupement quelconque (comme
 *   monster_srd521.md, equipement_srd521.md, sorts_srd521.md, dons_srd521.md) :
 *   `monstre`/`monster`, `pnj`, `objet`/`equipement`/`équipement`, `sort`, `don`.
 *   Une entrée `pnj` s'écrit comme un monstre (mêmes champs, même `Image:`) mais devient une
 *   fiche PNJ de l'univers à l'import (voir PnjImport) plutôt qu'une entrée du bestiaire.
 *
 * Exemple de fichier mêlant plusieurs types :
 *
 *   ## Nouvelle classe : Artificier
 *   <!-- type: classe -->
 *   ...
 *
 *   ## Nouveaux objets
 *   ### Épée +2
 *   <!-- type: objet -->
 *   **Type** Arme
 *   ...
 *
 *   ## Nouveau monstre
 *   ### Gnome des cavernes
 *   <!-- type: monstre -->
 *   Catégorie: Gnomes
 *   ...
 */
object CustomContentParser {

    private val typeMarkerRegex = Regex("""<!--\s*type:\s*([\p{L}_]+)\s*-->""", RegexOption.IGNORE_CASE)
    // Ligne entière de la balise (fin de ligne comprise), pour la retirer du contenu transmis.
    private val typeMarkerLineRegex = Regex("""(?m)^[ \t]*<!--\s*type:\s*[\p{L}_]+\s*-->[ \t]*\r?\n?""", RegexOption.IGNORE_CASE)
    private val level2HeaderRegex = Regex("""(?m)^##\s+(.+)$""")
    private val level3HeaderRegex = Regex("""(?m)^###\s+(.+)$""")
    private val level1HeaderRegex = Regex("""(?m)^#\s+(.+)$""")

    // Chaque type est normalisé (accents retirés, minuscule) vers une clé canonique unique.
    private val typeAliases = mapOf(
        "classe" to "classe",
        "sousclasse" to "sousclasse",
        "sous_classe" to "sousclasse",
        "espece" to "espece",
        "historique" to "historique",
        "regle" to "regle",
        "monstre" to "monstre",
        "monster" to "monstre",
        "pnj" to "pnj",
        "npc" to "pnj",
        "objet" to "objet",
        "equipement" to "objet",
        "sort" to "sort",
        "don" to "don",
    )

    private val level2Types = setOf("classe", "sousclasse", "espece", "historique", "regle")
    private val level3Types = setOf("monstre", "pnj", "objet", "sort", "don")

    // Sous-titres "## " connus des entrées "### " (voir MonsterParser) : nichés à
    // l'intérieur d'un bloc monstre/objet/sort/don, ils ne délimitent pas la fin de ce
    // bloc — contrairement à tout autre "## " (ex. un groupement ou une règle qui suit).
    private val knownEntrySubsections = setOf(
        "traits", "actions", "actions bonus", "reactions", "actions legendaires",
    )

    fun parse(rawMarkdown: String): CustomContentResult {
        val byType = mutableMapOf<String, MutableList<String>>()
        val unrecognized = mutableListOf<String>()

        // Les entrées de type monstre/objet/sort/don (niveau "### ") sont traitées en
        // premier, sur l'ensemble du document : leur contenu peut légitimement contenir des
        // sous-titres "## " connus (Traits, Actions...), comme dans monster_srd521.md — ce
        // ne sont pas des délimiteurs de regroupement, contrairement à un "## " quelconque
        // qui suivrait (ex. une règle ou un nouveau groupement intercalés).
        val level3Blocks = level3EntryBlocks(rawMarkdown)
        for ((title3, content3, range3) in level3Blocks) {
            val type3 = normalizeType(typeMarkerRegex.find(content3)?.groupValues?.get(1))
            if (type3 == null || type3 !in level3Types) {
                unrecognized.add(title3)
                continue
            }
            // La balise est retirée avant de déléguer au parseur du type : placée juste sous le
            // titre (format documenté ci-dessus), elle interrompait la lecture des champs
            // "Clé: valeur" de MonsterParser (catégorie, CA, PV, FP... perdus à l'import).
            byType.getOrPut(type3) { mutableListOf() }.add(content3.replace(typeMarkerLineRegex, ""))
        }

        // Les entrées de type classe/espece/historique/regle (niveau "## ") ne sont prises
        // en compte que si elles ne contiennent aucun "### " imbriqué — sinon il s'agit d'un
        // titre de regroupement pour des entrées "### " (déjà traitées ci-dessus) ou d'un
        // sous-titre interne à l'une d'elles (déjà exclu, ces "## " tombent dans la plage
        // d'un bloc de [level3Blocks]).
        for ((title2, content2, range2) in splitByHeader(rawMarkdown, level2HeaderRegex)) {
            val insideLevel3Entry = level3Blocks.any { (_, _, r3) -> range2.first in r3 }
            if (insideLevel3Entry) continue
            val containsLevel3Header = level3HeaderRegex.containsMatchIn(content2)
            if (containsLevel3Header) continue

            val type2 = normalizeType(typeMarkerRegex.find(content2)?.groupValues?.get(1))
            if (type2 == null || type2 !in level2Types) {
                unrecognized.add(title2)
                continue
            }
            byType.getOrPut(type2) { mutableListOf() }.add(content2)
        }

        fun joined(type: String) = byType[type]?.joinToString("\n\n").orEmpty()

        val spellsRaw = joined("sort")

        return CustomContentResult(
            monsters = MonsterParser.parse(joined("monstre")),
            pnjs = MonsterParser.parse(joined("pnj")),
            equipment = EquipmentParser.parse(joined("objet")),
            spells = SpellParser.parse(spellsRaw),
            spellsIndex = SpellParser.parseIndex(spellsRaw),
            classes = MarkdownSectionParser.parseFlat(joined("classe"), category = "Classe", requireId = false),
            sousClasses = MarkdownSectionParser.parseFlat(joined("sousclasse"), requireId = false).map {
                it.copy(name = it.name.removePrefix("Sous-classe").trimStart(' ', ':').trim(), category = classeDeSousClasse(it.rawMarkdown))
            },
            especes = MarkdownSectionParser.parseFlat(joined("espece"), category = "Espèce", requireId = false),
            historiques = MarkdownSectionParser.parseFlat(
                joined("historique"), category = "Historique", requireId = false,
            ),
            dons = DonsParser.parse(joined("don")),
            rules = RulesParser.parse(joined("regle")),
            unrecognized = unrecognized,
        )
    }

    private val classeFieldRegex = Regex("""(?mi)^\s*-\s*classe\s*:\s*(.+)$""")

    /** Classe d'une sous-classe, lue dans son champ "- classe : Paladin" (vide si absent). */
    private fun classeDeSousClasse(markdown: String): String =
        classeFieldRegex.find(markdown)?.groupValues?.get(1)?.trim().orEmpty()

    private fun normalizeType(raw: String?): String? {
        if (raw == null) return null
        val stripped = raw.lowercase()
            .replace("è", "e").replace("é", "e").replace("ê", "e")
        return typeAliases[stripped]
    }

    /**
     * Découpe [markdown] en blocs d'entrée "### " (monstre/objet/sort/don), chacun s'étendant
     * jusqu'au premier des éléments suivants : le "### " suivant, un "## " dont le titre n'est
     * pas un sous-titre connu ([knownEntrySubsections]), ou la fin du document. Un "## Traits"
     * ou "## Actions" reste ainsi à l'intérieur du bloc, mais un "## " qui débute une nouvelle
     * règle ou un nouveau groupement met fin au bloc courant.
     */
    private fun level3EntryBlocks(markdown: String): List<Triple<String, String, IntRange>> {
        val level3Matches = level3HeaderRegex.findAll(markdown).toList()
        if (level3Matches.isEmpty()) return emptyList()
        // Un titre "# " (début d'un autre fichier quand plusieurs .md d'une archive sont mis bout
        // à bout, ex. bestiaire puis objets magiques) termine lui aussi le bloc : sans ça, le
        // titre et l'introduction du fichier suivant finissaient dans la fiche du dernier monstre.
        val level2Starts = (
            level2HeaderRegex.findAll(markdown)
                .filter { normalizeLabel(it.groupValues[1]) !in knownEntrySubsections }
                .map { it.range.first } +
                level1HeaderRegex.findAll(markdown).map { it.range.first }
            ).sorted().toList()

        return level3Matches.mapIndexed { index, match ->
            val title = match.groupValues[1].trim()
            val start = match.range.first
            val nextLevel3Start = level3Matches.getOrNull(index + 1)?.range?.first ?: markdown.length
            val nextForeignLevel2Start = level2Starts.firstOrNull { it in (start + 1) until nextLevel3Start }
            val end = nextForeignLevel2Start ?: nextLevel3Start
            Triple(title, markdown.substring(start, end).trim(), start until end)
        }
    }

    private fun normalizeLabel(raw: String): String =
        raw.trim().lowercase()
            .replace("è", "e").replace("é", "e").replace("ê", "e")

    /**
     * Découpe [markdown] en blocs délimités par [headerRegex], chacun avec sa plage
     * (position de début du titre jusqu'au titre suivant exclu, ou fin du document) dans
     * [markdown] d'origine — utilisée par [parse] pour savoir si un titre "## " tombe à
     * l'intérieur d'un bloc "### " déjà traité, plutôt que d'en être un délimiteur.
     */
    private fun splitByHeader(markdown: String, headerRegex: Regex): List<Triple<String, String, IntRange>> {
        val matches = headerRegex.findAll(markdown).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexed { index, match ->
            val title = match.groupValues[1].trim()
            val start = match.range.first
            val end = if (index + 1 < matches.size) matches[index + 1].range.first else markdown.length
            Triple(title, markdown.substring(start, end).trim(), start until end)
        }
    }
}
