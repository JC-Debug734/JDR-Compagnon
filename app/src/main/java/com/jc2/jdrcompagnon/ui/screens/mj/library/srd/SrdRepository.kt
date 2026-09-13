package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import android.content.Context
import com.jc2.jdrcompagnon.ui.PublicFilesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Entrée générique d'un document SRD 5.2.1 structuré en sections nommées
 * (classes, espèces, historiques, dons, armes/armures magiques).
 *
 * [category] est vide pour les documents à plat où chaque section "## " est une
 * entrée à part entière (classes, espèces, historiques), et renseigné pour les
 * documents à deux niveaux où "## " est une catégorie et "### " une entrée
 * (dons, armes/armures magiques).
 */
data class SrdSectionEntry(
    val name: String,
    val category: String,
    val rawMarkdown: String,
)

/**
 * Section libre d'un document SRD non structuré en entrées nommées identifiables
 * (ex. montures et véhicules : tables et paragraphes, sans commentaire `<!-- id -->`).
 */
data class SrdDocSection(
    val title: String,
    val content: String,
)

/**
 * Parseur générique pour les fichiers SRD 5.2.1 générés au format Markdown structuré :
 * titres "## " (et "### " pour les documents à deux niveaux), chaque entrée réelle étant
 * précédée d'un commentaire `<!-- id: identifiant -->` qui sert de marqueur pour la
 * distinguer des titres d'introduction, de notes de fin ou de tableaux récapitulatifs.
 */
private object MarkdownSectionParser {

    private val idCommentRegex = Regex("""<!--\s*id:\s*.+?-->""")

    /**
     * Découpe un document à un seul niveau ("## " uniquement). Par défaut, ne conserve
     * que les sections contenant un commentaire `<!-- id: ... -->`, ce qui exclut
     * l'introduction et les sections annexes (ex. "Tableau récapitulatif", "Notes de
     * fiabilité"). Passer [requireId] à false pour les documents sans commentaires id
     * (ex. montures et véhicules), auquel cas [parseSections] est généralement préférable.
     */
    fun parseFlat(markdown: String, category: String = "", requireId: Boolean = true): List<SrdSectionEntry> =
        splitByHeaderLevel(markdown, level = 2)
            .filter { (_, content) -> !requireId || idCommentRegex.containsMatchIn(content) }
            .map { (title, content) -> SrdSectionEntry(name = title, category = category, rawMarkdown = content) }

    /**
     * Découpe un document à deux niveaux : "## " délimite une catégorie, "### " délimite
     * les entrées nommées à l'intérieur. Ne conserve que les entrées comportant un
     * commentaire `<!-- id: ... -->` (exclut par exemple une catégorie "Notes" sans
     * sous-entrées, ou les préambules de catégorie).
     */
    fun parseTwoLevel(markdown: String): List<SrdSectionEntry> =
        splitByHeaderLevel(markdown, level = 2).flatMap { (category, categoryContent) ->
            splitByHeaderLevel(categoryContent, level = 3)
                .filter { (_, content) -> idCommentRegex.containsMatchIn(content) }
                .map { (name, content) -> SrdSectionEntry(name = name, category = category, rawMarkdown = content) }
        }

    /**
     * Découpe un document en sections libres de niveau "## " (titre + contenu complet),
     * sans filtrage sur la présence d'un identifiant. Adapté aux documents de référence
     * qui ne définissent pas d'entrées nommées individuelles (ex. montures et véhicules).
     */
    fun parseSections(markdown: String): List<SrdDocSection> =
        splitByHeaderLevel(markdown, level = 2).map { (title, content) -> SrdDocSection(title, content) }

    /**
     * Découpe [markdown] en blocs délimités par des titres du niveau [level] exact
     * (ex. niveau 2 = lignes "## Titre", qui ne capture pas les lignes "### Titre" car
     * celles-ci ont un caractère '#' immédiatement après le préfixe au lieu d'un espace).
     * Retourne une liste de paires (titre, contenu du bloc jusqu'au titre suivant inclus).
     */
    private fun splitByHeaderLevel(markdown: String, level: Int): List<Pair<String, String>> {
        val prefix = "#".repeat(level)
        val headerRegex = Regex("(?m)^$prefix\\s+(.+)$")
        val matches = headerRegex.findAll(markdown).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexed { index, match ->
            val title = match.groupValues[1].trim()
            val start = match.range.first
            val end = if (index + 1 < matches.size) matches[index + 1].range.first else markdown.length
            title to markdown.substring(start, end).trim()
        }
    }
}

/**
 * Parseur dédié à l'index des sorts SRD 5.2.1 (`sorts_srd521.md`). Ce fichier liste les
 * sorts sous forme de tables Markdown groupées par niveau ("## Sorts de niveau 1" puis
 * un tableau "| Sort | École | Spécial | Classes |"), un format différent de l'ancien
 * `spells.md` (un bloc détaillé par sort) attendu par [SpellParser] — ce qui explique
 * un affichage vide si l'on tente de lire ce nouveau fichier avec l'ancien parseur.
 * Chaque ligne de tableau devient ici une [SrdSectionEntry], la catégorie étant le
 * titre de niveau ("Sorts de niveau 1", etc.).
 */
private object SpellIndexParser {

    fun parse(markdown: String): List<SrdSectionEntry> {
        val levelHeaders = Regex("(?m)^##\\s+(.+)$").findAll(markdown).toList()
        if (levelHeaders.isEmpty()) return emptyList()
        return levelHeaders.flatMapIndexed { index, match ->
            val level = match.groupValues[1].trim()
            val start = match.range.first
            val end = if (index + 1 < levelHeaders.size) levelHeaders[index + 1].range.first else markdown.length
            val block = markdown.substring(start, end)
            parseTableRows(block).mapNotNull { columns ->
                val name = columns.getOrNull(0)?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                val ecole = columns.getOrNull(1)?.trim().orEmpty()
                val special = columns.getOrNull(2)?.trim().orEmpty()
                val classes = columns.getOrNull(3)?.trim().orEmpty()
                val detail = buildString {
                    if (ecole.isNotBlank()) appendLine("- École : $ecole")
                    if (special.isNotBlank() && special != "—") appendLine("- Spécial : $special")
                    if (classes.isNotBlank()) appendLine("- Classes : $classes")
                }
                SrdSectionEntry(name = name, category = level, rawMarkdown = detail.trim())
            }
        }
    }

    /**
     * Extrait les lignes de données d'une table Markdown "| a | b | c |", en ignorant
     * la ligne d'en-tête et la ligne de séparation ("|---|---|---|").
     */
    private fun parseTableRows(block: String): List<List<String>> {
        val lines = block.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("|") && it.endsWith("|") }
            .toList()
        if (lines.size < 2) return emptyList()
        return lines.drop(2).map { line -> line.trim('|').split("|").map { it.trim() } }
    }
}

/**
 * Singleton responsable du chargement, parsing et cache des fichiers SRD markdown.
 *
 * Les fichiers SRD sont stockés dans `assets/srd/` (SRD 5.1 FR) et `assets/naheulbeuk/` :
 * - `srd/monsters.md` — Bestiaire (SRD 5.1 FR)
 * - `srd/spells.md` — Liste des sorts (SRD 5.1 FR)
 * - `srd/rules.md` — Règles générales (SRD 5.1 FR)
 * - `srd/glossary.md` — Glossaire (SRD 5.1 FR)
 * - `srd/equipment.md` — Équipement (SRD 5.1 FR)
 * - `naheulbeuk/rules.md` — Règles Naheulbeuk V4
 * - `naheulbeuk/equipment.md` — Équipement Naheulbeuk V4
 *
 * Tous les chargements utilisent `Dispatchers.IO`.
 * Le cache est maintenu en mémoire, indexé par identifiant de monde.
 */
object SrdRepository {

    private const val FILE_MONSTERS = "dnd/monsters.md"
    private const val FILE_SPELLS = "dnd/sorts_srd521.md"
    private const val FILE_GLOSSARY = "dnd/glossary.md"
    private const val FILE_EQUIPMENT = "dnd/equipement_srd521.md"
    private const val FILE_RULES = "dnd/rules.md"

    // Nouveaux fichiers SRD 5.2.1, sans équivalent "ancien" à remplacer
    private const val FILE_ARMES_ARMURES_MAGIQUES = "dnd/armes_armures_magiques_srd521.md"
    private const val FILE_CLASSES = "dnd/classes_srd521.md"
    private const val FILE_DONS = "dnd/dons_srd521.md"
    private const val FILE_ESPECES = "dnd/especes_srd521.md"
    private const val FILE_HISTORIQUES = "dnd/historiques_srd521.md"
    private const val FILE_MONTURES_VEHICULES = "dnd/montures_vehicules_srd521.md"

    private const val NAHEULBEUK_RULES = "naheulbeuk/rules.md"
    private const val NAHEULBEUK_EQUIPMENT = "naheulbeuk/equipment.md"

    private fun rulesFileForWorld(worldId: String?): String =
        if (worldId == "naheulbeuk") NAHEULBEUK_RULES else FILE_RULES

    private fun equipmentFileForWorld(worldId: String?): String =
        if (worldId == "naheulbeuk") NAHEULBEUK_EQUIPMENT else FILE_EQUIPMENT

    // Cache en mémoire, indexé par identifiant de monde
    private val monstersCache = mutableMapOf<String, List<SrdEntry>>()
    private val spellsCache = mutableMapOf<String, List<SrdEntry>>()
    private val rulesCache = mutableMapOf<String, List<RuleSection>>()
    private val glossaryCache = mutableMapOf<String, String>()
    private val equipmentListCache = mutableMapOf<String, List<EquipmentItem>>()
    private var equipmentRawCache: String? = null

    // Caches pour les nouveaux fichiers SRD 5.2.1
    private val classesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val especesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val historiquesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val donsCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val armesArmuresMagiquesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val monturesVehiculesCache = mutableMapOf<String, List<SrdDocSection>>()
    private val spellsIndexCache = mutableMapOf<String, List<SrdSectionEntry>>()

    /**
     * Indique si le monde dispose d'une bibliothèque consultable.
     */
    fun isLibraryAvailable(worldId: String?): Boolean = worldId == "donjon_et_dragon" || worldId == "naheulbeuk"

    /**
     * Charge la liste des monstres pour le monde donné.
     */
    suspend fun loadMonsters(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            monstersCache[worldId]?.let { return@withContext it }
            val rawMarkdown = readAsset(context, FILE_MONSTERS)
            val converted = HtmlTableConverter.convertAll(rawMarkdown)
            val monsters = MonsterParser.parse(converted)
            monstersCache[worldId] = monsters
            monsters
        }

    /**
     * Charge la liste des sorts pour le monde donné.
     */
    suspend fun loadSpells(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            spellsCache[worldId]?.let { return@withContext it }
            val rawMarkdown = readAsset(context, FILE_SPELLS)
            val converted = HtmlTableConverter.convertAll(rawMarkdown)
            val spells = SpellParser.parse(converted)
            spellsCache[worldId] = spells
            spells
        }

    /**
     * Charge les sections de règles pour le monde donné (D&D ou Naheulbeuk).
     */
    suspend fun loadRuleSections(context: Context, worldId: String? = "donjon_et_dragon"): List<RuleSection> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            rulesCache[key]?.let { return@withContext it }
            val raw = readAsset(context, rulesFileForWorld(worldId))
            val sections = RulesParser.parse(raw)
            rulesCache[key] = sections
            sections
        }

    /**
     * Charge le glossaire pour le monde donné.
     */
    suspend fun loadGlossary(context: Context, worldId: String? = "donjon_et_dragon"): String =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext ""
            glossaryCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_GLOSSARY)
            glossaryCache[worldId] = raw
            raw
        }

    /**
     * Charge la liste d'équipement pour le monde donné (D&D ou Naheulbeuk).
     */
    suspend fun loadEquipmentList(context: Context, worldId: String? = "donjon_et_dragon"): List<EquipmentItem> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            equipmentListCache[key]?.let { return@withContext it }
            val rawMarkdown = readAsset(context, equipmentFileForWorld(worldId))
            val converted = HtmlTableConverter.convertAll(rawMarkdown)
            val equipment = EquipmentParser.parse(converted)
            equipmentListCache[key] = equipment
            equipment
        }

    /**
     * Charge l'index des sorts pour le monde donné (D&D uniquement, SRD 5.2.1), au format
     * table par niveau (voir [SpellIndexParser]). À utiliser à la place de [loadSpells]
     * pour afficher `sorts_srd521.md`, qui n'est pas compatible avec [SpellParser]
     * (conçu pour l'ancien format `spells.md` à un bloc détaillé par sort).
     */
    suspend fun loadSpellsIndex(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            spellsIndexCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_SPELLS)
            val entries = SpellIndexParser.parse(raw)
            spellsIndexCache[worldId] = entries
            entries
        }

    /**
     * Charge la liste des classes pour le monde donné (D&D uniquement, SRD 5.2.1).
     * Chaque entrée représente une classe complète (traits de base, progression,
     * aptitudes, sous-classe), car ces sous-sections ne sont pas des entrées distinctes.
     */
    suspend fun loadClasses(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            classesCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_CLASSES)
            val entries = MarkdownSectionParser.parseFlat(raw, category = "Classe")
            classesCache[worldId] = entries
            entries
        }

    /**
     * Charge la liste des espèces jouables pour le monde donné (D&D uniquement, SRD 5.2.1).
     */
    suspend fun loadEspeces(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            especesCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_ESPECES)
            val entries = MarkdownSectionParser.parseFlat(raw, category = "Espèce")
            especesCache[worldId] = entries
            entries
        }

    /**
     * Charge la liste des historiques (backgrounds) pour le monde donné
     * (D&D uniquement, SRD 5.2.1).
     */
    suspend fun loadHistoriques(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            historiquesCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_HISTORIQUES)
            val entries = MarkdownSectionParser.parseFlat(raw, category = "Historique")
            historiquesCache[worldId] = entries
            entries
        }

    /**
     * Charge la liste des dons pour le monde donné (D&D uniquement, SRD 5.2.1).
     * [SrdSectionEntry.category] contient la catégorie de don (Origines, Général,
     * Style de combat, Faveur épique).
     */
    suspend fun loadDons(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            donsCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_DONS)
            val entries = MarkdownSectionParser.parseTwoLevel(raw)
            donsCache[worldId] = entries
            entries
        }

    /**
     * Charge la liste des armes et armures magiques pour le monde donné
     * (D&D uniquement, SRD 5.2.1). [SrdSectionEntry.category] vaut "Armes magiques"
     * ou "Armures magiques".
     */
    suspend fun loadArmesArmuresMagiques(
        context: Context,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdSectionEntry> = withContext(Dispatchers.IO) {
        if (worldId != "donjon_et_dragon") return@withContext emptyList()
        armesArmuresMagiquesCache[worldId]?.let { return@withContext it }
        val raw = readAsset(context, FILE_ARMES_ARMURES_MAGIQUES)
        val entries = MarkdownSectionParser.parseTwoLevel(raw)
        armesArmuresMagiquesCache[worldId] = entries
        entries
    }

    /**
     * Charge les sections de référence montures et véhicules pour le monde donné
     * (D&D uniquement, SRD 5.2.1). Ce fichier ne définit pas d'entrées nommées
     * individuelles (pas de commentaires `<!-- id -->`), d'où l'usage de
     * [SrdDocSection] plutôt que [SrdSectionEntry] : chaque section correspond à
     * un titre "## " (ex. "Montures et autres animaux", "Véhicules aériens et bateaux").
     */
    suspend fun loadMonturesVehicules(
        context: Context,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdDocSection> = withContext(Dispatchers.IO) {
        if (worldId != "donjon_et_dragon") return@withContext emptyList()
        monturesVehiculesCache[worldId]?.let { return@withContext it }
        val raw = readAsset(context, FILE_MONTURES_VEHICULES)
        val sections = MarkdownSectionParser.parseSections(raw)
        monturesVehiculesCache[worldId] = sections
        sections
    }

    /**
     * Charge le markdown brut des règles générales pour le monde donné.
     */
    suspend fun loadRules(context: Context, worldId: String? = "donjon_et_dragon"): String =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext ""
            val key = worldId ?: ""
            rulesRawCache[key]?.let { return@withContext it }
            val raw = readAsset(context, rulesFileForWorld(worldId))
            rulesRawCache[key] = raw
            raw
        }

    // Cache brute des règles (markdown complet), indexé par monde
    private val rulesRawCache = mutableMapOf<String, String>()

    /**
     * Charge le markdown brut de l'équipement (déprécié, préférer loadEquipmentList).
     */
    suspend fun loadEquipment(context: Context): String = withContext(Dispatchers.IO) {
        equipmentRawCache?.let { return@withContext it }
        val raw = readAsset(context, FILE_EQUIPMENT)
        equipmentRawCache = raw
        raw
    }

    /**
     * Recherche des monstres par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchMonsters(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdEntry> = withContext(Dispatchers.IO) {
        val monsters = loadMonsters(context, worldId)
        if (query.isBlank()) {
            monsters
        } else {
            monsters.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    /**
     * Recherche des sorts par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchSpells(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdEntry> = withContext(Dispatchers.IO) {
        val spells = loadSpells(context, worldId)
        if (query.isBlank()) {
            spells
        } else {
            spells.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    /**
     * Récupère un monstre par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getMonsterByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdEntry? = withContext(Dispatchers.IO) {
        loadMonsters(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Récupère un sort par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getSpellByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdEntry? = withContext(Dispatchers.IO) {
        loadSpells(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Récupère un équipement par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getEquipmentByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): EquipmentItem? = withContext(Dispatchers.IO) {
        loadEquipmentList(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Recherche des classes par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchClasses(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdSectionEntry> = withContext(Dispatchers.IO) {
        val classes = loadClasses(context, worldId)
        if (query.isBlank()) classes else classes.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Récupère une classe par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getClasseByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdSectionEntry? = withContext(Dispatchers.IO) {
        loadClasses(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Recherche des espèces par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchEspeces(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdSectionEntry> = withContext(Dispatchers.IO) {
        val especes = loadEspeces(context, worldId)
        if (query.isBlank()) especes else especes.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Récupère une espèce par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getEspeceByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdSectionEntry? = withContext(Dispatchers.IO) {
        loadEspeces(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Recherche des historiques par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchHistoriques(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdSectionEntry> = withContext(Dispatchers.IO) {
        val historiques = loadHistoriques(context, worldId)
        if (query.isBlank()) historiques else historiques.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Récupère un historique par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getHistoriqueByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdSectionEntry? = withContext(Dispatchers.IO) {
        loadHistoriques(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Recherche des dons par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchDons(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdSectionEntry> = withContext(Dispatchers.IO) {
        val dons = loadDons(context, worldId)
        if (query.isBlank()) dons else dons.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Récupère un don par son nom exact (insensible à la casse) pour le monde donné.
     */
    suspend fun getDonByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdSectionEntry? = withContext(Dispatchers.IO) {
        loadDons(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Recherche des armes/armures magiques par nom (insensible à la casse) pour le monde donné.
     */
    suspend fun searchArmesArmuresMagiques(
        context: Context,
        query: String,
        worldId: String? = "donjon_et_dragon",
    ): List<SrdSectionEntry> = withContext(Dispatchers.IO) {
        val items = loadArmesArmuresMagiques(context, worldId)
        if (query.isBlank()) items else items.filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Récupère une arme/armure magique par son nom exact (insensible à la casse)
     * pour le monde donné.
     */
    suspend fun getArmeArmureMagiqueByName(
        context: Context,
        name: String,
        worldId: String? = "donjon_et_dragon",
    ): SrdSectionEntry? = withContext(Dispatchers.IO) {
        loadArmesArmuresMagiques(context, worldId).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Invalide tous les caches en mémoire.
     */
    fun clearCache() {
        monstersCache.clear()
        spellsCache.clear()
        rulesCache.clear()
        rulesRawCache.clear()
        glossaryCache.clear()
        equipmentListCache.clear()
        equipmentRawCache = null
        classesCache.clear()
        especesCache.clear()
        historiquesCache.clear()
        donsCache.clear()
        armesArmuresMagiquesCache.clear()
        monturesVehiculesCache.clear()
        spellsIndexCache.clear()
    }

    /**
     * Lit un fichier SRD, en donnant la priorité à une version modifiée par
     * l'utilisateur dans Téléchargements/JDRCompagnon/SRD/... si elle existe.
     * Sinon, lit la version intégrée à l'app (assets/) et la copie dans ce
     * dossier public pour qu'elle soit consultable/modifiable en dehors de l'app.
     */
    private fun readAsset(context: Context, path: String): String {
        val (subfolder, displayName) = splitPublicPath(path)
        PublicFilesStore.readText(context, displayName, subfolder)?.let { return it }

        val raw = context.assets.open(path).bufferedReader().use { it.readText() }
        PublicFilesStore.writeText(context, displayName, raw, subfolder)
        return raw
    }

    /**
     * Convertit un chemin d'asset ("dnd/monsters.md") en sous-dossier public +
     * nom de fichier ("SRD/dnd" + "monsters.md").
     */
    private fun splitPublicPath(assetPath: String): Pair<String, String> {
        val parts = assetPath.split("/")
        val displayName = parts.last()
        val subfolder = "SRD/" + parts.dropLast(1).joinToString("/")
        return subfolder to displayName
    }

    /**
     * Cherche un item d'équipement par nom dans le cache D&D (fallback sur le premier monde chargé).
     * Utilisé pour calculer le poids total ; tolérant aux majuscules.
     */
    fun findEquipmentItemCached(name: String): EquipmentItem? {
        val cache = equipmentListCache.values.firstOrNull() ?: return null
        val lower = name.lowercase()
        return cache.firstOrNull { item -> item.name.equals(name, ignoreCase = true) }
            ?: cache.firstOrNull { item ->
                val itemLower = item.name.lowercase()
                itemLower.contains(lower) || lower.contains(itemLower)
            }
    }
}