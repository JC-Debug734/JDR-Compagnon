package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import android.content.Context
import com.jc2.jdrcompagnon.ui.PublicFilesStore
import com.jc2.jdrcompagnon.ui.screens.mj.CustomBooksStore
import com.jc2.jdrcompagnon.ui.screens.mj.customBooksDir
import com.jc2.jdrcompagnon.ui.screens.mj.readCustomBookContent
import com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Un fichier markdown de contenu personnalisé, et le dossier de ses images éventuelles. */
private class CustomContentSource(val content: String, val file: File, val imagesRoot: File?)

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
    // Renseigné uniquement pour les sorts (cf. SpellParser.parseIndex) : liste des classes
    // ayant accès au sort, affichée sous forme de ligne secondaire dans l'aperçu rapide de
    // la bibliothèque (cf. SectionEntryRow dans LibraryScreen.kt).
    val classes: String = "",
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
internal object MarkdownSectionParser {

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
 * Singleton responsable du chargement, parsing et cache des fichiers SRD markdown.
 *
 * Les fichiers SRD sont stockés dans `assets/dnd/` (SRD 5.2.1 FR) et `assets/naheulbeuk/` :
 * - `dnd/monster_srd521.md` — Bestiaire (images dans `dnd/monstres/`, voir [MonsterImages])
 * - `dnd/sorts_srd521.md` — Liste des sorts
 * - `dnd/rules.md` — Règles générales
 * - `dnd/glossary.md` — Glossaire
 * - `dnd/equipement_srd521.md` — Équipement
 * - `dnd/classes_srd521.md`, `dnd/historiques_srd521.md`, `dnd/especes_srd521.md`,
 *   `dnd/dons_srd521.md`, `dnd/langues.md` — création de personnage (cf. Srdcreationparsers.kt)
 * - `naheulbeuk/rules.md` — Règles Naheulbeuk V4
 * - `naheulbeuk/equipment.md` — Équipement Naheulbeuk V4
 *
 * Tous les chargements utilisent `Dispatchers.IO`.
 * Le cache est maintenu en mémoire, indexé par identifiant de monde.
 */
object SrdRepository {

    // Porte les lignes "Environnement:" et "Image:" exploitées par le bestiaire (voir
    // MonsterImages).
    private const val FILE_MONSTERS = "dnd/monster_srd521.md"
    // Fichier unique pour les sorts (SRD 5.2.1) : un bloc "### Nom" par sort, avec ses
    // champs (École, Niveau, Classes, Temps d'incantation, Portée, Composantes, Durée)
    // et sa description — voir [SpellParser]. Sert à la fois au détail d'un sort et à
    // l'index filtrable par niveau de l'onglet Sorts.
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
    // Pas de commentaires <!-- id --> dans ce fichier (juste "### Groupe" + "- Nom"),
    // d'où un chargement brut ici plutôt qu'un passage par MarkdownSectionParser —
    // le découpage revient à LangueParser (cf. SrdCreationParsers.kt).
    private const val FILE_LANGUES = "dnd/langues.md"

    private const val NAHEULBEUK_RULES = "naheulbeuk/rules.md"
    private const val NAHEULBEUK_EQUIPMENT = "naheulbeuk/equipment.md"

    // Un univers importé (worldId ni "donjon_et_dragon" ni "naheulbeuk") n'a pas de fichier
    // officiel : retourne null plutôt que de retomber sur celui de D&D par défaut.
    private fun rulesFileForWorld(worldId: String?): String? = when (worldId) {
        "naheulbeuk" -> NAHEULBEUK_RULES
        "donjon_et_dragon" -> FILE_RULES
        else -> null
    }

    private fun equipmentFileForWorld(worldId: String?): String? = when (worldId) {
        "naheulbeuk" -> NAHEULBEUK_EQUIPMENT
        "donjon_et_dragon" -> FILE_EQUIPMENT
        else -> null
    }

    // Cache en mémoire, indexé par identifiant de monde
    private val monstersCache = mutableMapOf<String, List<SrdEntry>>()
    private val spellsCache = mutableMapOf<String, List<SrdEntry>>()
    private val spellsRawCache = mutableMapOf<String, String>()
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
    private val languesCache = mutableMapOf<String, String>()

    // Contenu extrait des livres personnalisés (cf. [CustomContentParser]) ajoutés par
    // l'utilisateur depuis l'écran de gestion des livres, fusionné dans les listes
    // ci-dessus au premier chargement de chaque monde (voir [ensureCustomContentLoaded]) —
    // c'est ce qui permet à un nouvel objet/monstre/règle importé d'apparaître partout où
    // le contenu officiel est consulté (création de personnage, boutique, environnement...)
    // sans que chaque écran consommateur ait à connaître l'existence des livres personnalisés.
    private val customMonstersCache = mutableMapOf<String, List<SrdEntry>>()
    private val customEquipmentCache = mutableMapOf<String, List<EquipmentItem>>()
    private val customSpellsCache = mutableMapOf<String, List<SrdEntry>>()
    private val customSpellsIndexCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val customClassesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val customSousClassesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val customEspecesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val customHistoriquesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val customDonsCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val customRulesCache = mutableMapOf<String, List<RuleSection>>()
    private val customContentLoadedWorlds = mutableSetOf<String>()

    /**
     * Lit et parse (voir [CustomContentParser]) tous les livres personnalisés enregistrés
     * pour [worldId] ainsi que, pour un univers importé (voir [CustomWorldsRepository]), les
     * fichiers markdown de son propre dossier — une seule fois par monde et par session (voir
     * [customContentLoadedWorlds]) — et fusionne leurs entrées dans les caches `custom*`
     * ci-dessus. Appelé au début de chaque `load*` de ce fichier, avant de lire son propre
     * cache, pour que le contenu importé soit toujours pris en compte.
     */
    private suspend fun ensureCustomContentLoaded(context: Context, worldId: String?) = withContext(Dispatchers.IO) {
        // Verrou : un second chargement simultané (ex. bestiaire et création de personnage)
        // attend la fin du premier. Sans lui, il trouvait le monde déjà marqué « chargé » alors
        // que les caches custom* étaient encore vides, et mettait en cache une liste (espèces,
        // classes...) privée du contenu importé jusqu'à la prochaine invalidation.
        customContentMutex.withLock { chargerContenuPersonnalise(context, worldId) }
    }

    private val customContentMutex = kotlinx.coroutines.sync.Mutex()

    private fun chargerContenuPersonnalise(context: Context, worldId: String?) {
        val key = worldId ?: ""
        if (key in customContentLoadedWorlds) return
        customContentLoadedWorlds.add(key)

        // Chaque source garde son fichier et le dossier où chercher les images de ses monstres
        // (voir CustomBookImages) : le dossier d'extraction d'un livre .zip, celui d'un
        // univers importé ; aucun pour un livre .md seul, rangé à plat avec les autres livres.
        val booksDir = customBooksDir(context)
        val bookContents = CustomBooksStore.list(context, worldId)
            .filter { it.fileName.endsWith(".md", ignoreCase = true) }
            // Livre propre à une campagne non sélectionnée : son contenu reste masqué (PorteeCampagne).
            .filter { com.jc2.jdrcompagnon.ui.PorteeCampagne.livreVisible(it.id, worldId) }
            .mapNotNull { book ->
                val content = readCustomBookContent(context, book.fileName) ?: return@mapNotNull null
                val imagesRoot = book.fileName.substringBefore('/', "").ifBlank { null }?.let { File(booksDir, it) }
                CustomContentSource(content, File(booksDir, book.fileName), imagesRoot)
            }
        val worldContents = CustomWorldsRepository.contentFiles(context, worldId)
            .mapNotNull { file ->
                runCatching { file.readText() }.getOrNull()?.let { CustomContentSource(it, file, file.parentFile) }
            }
        val allContents = bookContents + worldContents
        if (allContents.isEmpty()) return

        val monsters = mutableListOf<SrdEntry>()
        val equipment = mutableListOf<EquipmentItem>()
        val spells = mutableListOf<SrdEntry>()
        val spellsIndex = mutableListOf<SrdSectionEntry>()
        val classes = mutableListOf<SrdSectionEntry>()
        val sousClasses = mutableListOf<SrdSectionEntry>()
        val especes = mutableListOf<SrdSectionEntry>()
        val historiques = mutableListOf<SrdSectionEntry>()
        val dons = mutableListOf<SrdSectionEntry>()
        val rules = mutableListOf<RuleSection>()

        for (source in allContents) {
            val result = CustomContentParser.parse(source.content)
            monsters += source.imagesRoot
                ?.let { CustomBookImages.resolve(result.monsters, source.file, it) }
                ?: result.monsters
            equipment += result.equipment
            spells += result.spells
            spellsIndex += result.spellsIndex
            classes += result.classes
            sousClasses += result.sousClasses
            especes += result.especes
            historiques += result.historiques
            dons += result.dons
            rules += result.rules
        }

        customMonstersCache[key] = monsters
        customEquipmentCache[key] = equipment
        customSpellsCache[key] = spells
        customSpellsIndexCache[key] = spellsIndex
        customClassesCache[key] = classes
        customSousClassesCache[key] = sousClasses
        customEspecesCache[key] = especes
        customHistoriquesCache[key] = historiques
        customDonsCache[key] = dons
        customRulesCache[key] = rules
    }

    /**
     * Indique si le monde dispose d'une bibliothèque consultable.
     */
    // N'importe quel monde non nul dispose d'une bibliothèque consultable — y compris un
    // univers importé (voir [CustomWorldsRepository]), dont tout le contenu vient des
    // fichiers markdown de son dossier plutôt que des assets officiels D&D/Naheulbeuk.
    fun isLibraryAvailable(worldId: String?): Boolean = worldId != null

    /**
     * Charge la liste des monstres pour le monde donné. Seul D&D fournit un bestiaire
     * officiel ; les autres mondes (Naheulbeuk, univers importés) n'ont que les monstres
     * de leurs éventuels livres/contenus personnalisés (voir [ensureCustomContentLoaded]).
     */
    suspend fun loadMonsters(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            monstersCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") {
                MonsterParser.parse(HtmlTableConverter.convertAll(readAsset(context, FILE_MONSTERS)))
            } else {
                emptyList()
            }
            val monsters = official + customMonstersCache[key].orEmpty()
            monstersCache[key] = monsters
            monsters
        }

    /**
     * Lit et convertit le fichier `sorts_srd521.md` pour le monde donné, en cache — lu
     * une seule fois quel que soit le nombre d'appels à [loadSpells]/[loadSpellsIndex].
     */
    private fun loadSpellsRaw(context: Context, worldId: String?): String =
        spellsRawCache.getOrPut(worldId ?: "") {
            HtmlTableConverter.convertAll(readAsset(context, FILE_SPELLS))
        }

    /**
     * Charge la liste détaillée des sorts pour le monde donné, depuis `sorts_srd521.md`
     * (voir [SpellParser.parse]).
     */
    suspend fun loadSpells(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            spellsCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") SpellParser.parse(loadSpellsRaw(context, worldId)) else emptyList()
            val spells = official + customSpellsCache[key].orEmpty()
            spellsCache[key] = spells
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
            ensureCustomContentLoaded(context, worldId)
            val official = rulesFileForWorld(worldId)?.let { RulesParser.parse(readAsset(context, it)) }.orEmpty()
            val sections = official + customRulesCache[key].orEmpty()
            rulesCache[key] = sections
            sections
        }

    private val ruleEntriesCache = mutableMapOf<String, List<SrdSectionEntry>>()
    private val glossaryEntriesCache = mutableMapOf<String, List<SrdSectionEntry>>()

    /**
     * Règles découpées en entrées individuelles (une règle = une fiche, voir
     * [ReferenceEntryParser]), groupées par section, dans l'ordre du fichier. Les règles des
     * livres personnalisés suivent, sous la catégorie "Livres personnalisés".
     */
    suspend fun loadRuleEntries(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            ruleEntriesCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = rulesFileForWorld(worldId)?.let { ReferenceEntryParser.parse(readAsset(context, it)) }.orEmpty()
            val custom = customRulesCache[key].orEmpty().map {
                SrdSectionEntry(name = it.title, category = "Livres personnalisés", rawMarkdown = it.content)
            }
            (official + custom).also { ruleEntriesCache[key] = it }
        }

    /**
     * Une règle par son nom. Repli sur les grandes sections de [loadRuleSections] (titres "##"),
     * que peuvent viser les liens #rule: des scénarios écrits avant le découpage en entrées.
     */
    suspend fun getRuleEntryByName(context: Context, name: String, worldId: String? = "donjon_et_dragon"): SrdSectionEntry? =
        loadRuleEntries(context, worldId).find { it.name.equals(name, ignoreCase = true) }
            ?: loadRuleSections(context, worldId).find { it.title.equals(name, ignoreCase = true) }
                ?.let { SrdSectionEntry(name = it.title, category = "", rawMarkdown = it.content) }

    /** Glossaire découpé en entrées individuelles, comme les règles. D&D uniquement. */
    suspend fun loadGlossaryEntries(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext emptyList()
            glossaryEntriesCache[worldId]?.let { return@withContext it }
            ReferenceEntryParser.parse(loadGlossary(context, worldId)).also { glossaryEntriesCache[worldId] = it }
        }

    suspend fun getGlossaryEntryByName(context: Context, name: String, worldId: String? = "donjon_et_dragon"): SrdSectionEntry? =
        loadGlossaryEntries(context, worldId).find { it.name.equals(name, ignoreCase = true) }

    private val etatsCache = mutableMapOf<String, List<SrdSectionEntry>>()

    /** Livre « États » : généré depuis le catalogue des états (feature_combat, voir [LivreEtats]). */
    suspend fun loadEtats(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            etatsCache[key]?.let { return@withContext it }
            LivreEtats.construire().also { etatsCache[key] = it }
        }

    /** Fiche d'un état par son nom (« Paralysé ») ; « paralysée » ou « Incapable d'agir » sont reconnus aussi. */
    suspend fun getEtatByName(context: Context, name: String, worldId: String? = "donjon_et_dragon"): SrdSectionEntry? {
        val etats = loadEtats(context, worldId)
        return etats.find { it.name.equals(name, ignoreCase = true) }
            ?: com.jc2.jdrcompagnon.feature_combat.domain.model.Etats.reconnaitre(name)?.let { c -> etats.find { it.name == c.label } }
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
    // (Le glossaire reste D&D uniquement : c'est un texte de référence générique, pas une
    // liste d'entrées où fusionner du contenu personnalisé par monde aurait du sens.)

    /**
     * Charge la liste d'équipement pour le monde donné (D&D ou Naheulbeuk).
     */
    suspend fun loadEquipmentList(context: Context, worldId: String? = "donjon_et_dragon"): List<EquipmentItem> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            equipmentListCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = equipmentFileForWorld(worldId)?.let { path ->
                EquipmentParser.parse(HtmlTableConverter.convertAll(readAsset(context, path)))
            }.orEmpty()
            // Un objet d'un livre personnalisé remplace l'objet officiel du même nom (ex. une
            // "Potion de guérison" détaillée) : les écrans cherchent un objet par son nom et
            // tombaient sinon toujours sur la version officielle, placée en premier.
            val custom = customEquipmentCache[key].orEmpty()
            val customNames = custom.map { it.name.lowercase() }.toSet()
            val equipment = official.filterNot { it.name.lowercase() in customNames } + custom
            equipmentListCache[key] = equipment
            equipment
        }

    /**
     * Charge l'index des sorts pour le monde donné (D&D uniquement, SRD 5.2.1), groupé
     * par niveau ("Sorts mineurs", "Sorts de niveau 1", ...) — utilisé pour la liste
     * filtrable de l'onglet Sorts. Lit le même fichier `sorts_srd521.md` que [loadSpells]
     * (voir [loadSpellsRaw]) ; pour le détail complet d'un sort, voir
     * [loadSpells]/[getSpellByName].
     */
    suspend fun loadSpellsIndex(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            spellsIndexCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") {
                SpellParser.parseIndex(loadSpellsRaw(context, worldId))
            } else {
                emptyList()
            }
            val entries = official + customSpellsIndexCache[key].orEmpty()
            spellsIndexCache[key] = entries
            entries
        }

    /**
     * Charge la liste des classes pour le monde donné (D&D uniquement, SRD 5.2.1).
     * Chaque entrée représente une classe complète (traits de base, progression,
     * aptitudes, sous-classe), car ces sous-sections ne sont pas des entrées distinctes.
     */
    suspend fun loadClasses(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            classesCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") {
                MarkdownSectionParser.parseFlat(readAsset(context, FILE_CLASSES), category = "Classe")
            } else {
                emptyList()
            }
            val entries = fusionnerSousClasses(official + customClassesCache[key].orEmpty(), customSousClassesCache[key].orEmpty())
            classesCache[key] = entries
            entries
        }

    private val sousClasseTitreRegex = Regex("""(?m)^###\s+Sous-classe\s*:\s*(.+)$""")

    /**
     * Rattache chaque sous-classe importée (`<!-- type: sousclasse -->`, voir [CustomContentParser])
     * à sa classe (nom ou id donné par son champ "- classe :") : son contenu est ajouté au
     * markdown de la classe sous un titre "### Sous-classe : Nom", exactement comme la
     * sous-classe du SRD — ClasseParser la lit alors sans rien savoir de l'import. Une
     * sous-classe importée remplace celle de même nom déjà présente (ex. le résumé du Serment
     * de Dévotion du SRD). Une sous-classe dont la classe est introuvable est ignorée.
     */
    internal fun fusionnerSousClasses(classes: List<SrdSectionEntry>, sousClasses: List<SrdSectionEntry>): List<SrdSectionEntry> {
        if (sousClasses.isEmpty()) return classes
        return classes.map { classe ->
            val ajouts = sousClasses.filter { sc ->
                sc.category.isNotBlank() && (sc.category.equals(classe.name, ignoreCase = true) ||
                    classe.rawMarkdown.contains(Regex("""<!--\s*id:\s*${Regex.escape(sc.category.lowercase())}\s*-->""")))
            }
            if (ajouts.isEmpty()) return@map classe
            val nomsAjoutes = ajouts.map { it.name.lowercase() }.toSet()
            // Sections "### Sous-classe : X" existantes : retirées si remplacées par un import.
            val titres = sousClasseTitreRegex.findAll(classe.rawMarkdown).toList()
            var markdown = classe.rawMarkdown
            titres.asReversed().forEach { m ->
                if (m.groupValues[1].trim().lowercase() in nomsAjoutes) {
                    val fin = Regex("""(?m)^(###\s|---\s*$)""").find(markdown, m.range.last + 1)?.range?.first ?: markdown.length
                    markdown = markdown.removeRange(m.range.first, fin)
                }
            }
            val blocs = ajouts.joinToString("\n\n") { sc ->
                val corps = sc.rawMarkdown.substringAfter('\n', "").trim().removeSuffix("---").trim()
                "### Sous-classe : ${sc.name}\n\n$corps"
            }
            classe.copy(rawMarkdown = markdown.trimEnd().removeSuffix("---").trimEnd() + "\n\n" + blocs + "\n\n---")
        }
    }

    /**
     * Charge la liste des espèces jouables pour le monde donné (D&D uniquement, SRD 5.2.1).
     */
    suspend fun loadEspeces(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            especesCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") {
                MarkdownSectionParser.parseFlat(readAsset(context, FILE_ESPECES), category = "Espèce")
            } else {
                emptyList()
            }
            val entries = official + customEspecesCache[key].orEmpty()
            especesCache[key] = entries
            entries
        }

    /**
     * Charge la liste des historiques (backgrounds) pour le monde donné
     * (D&D uniquement, SRD 5.2.1).
     */
    suspend fun loadHistoriques(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            historiquesCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") {
                MarkdownSectionParser.parseFlat(readAsset(context, FILE_HISTORIQUES), category = "Historique")
            } else {
                emptyList()
            }
            val entries = official + customHistoriquesCache[key].orEmpty()
            historiquesCache[key] = entries
            entries
        }

    /**
     * Charge le markdown brut du fichier des langues pour le monde donné
     * (D&D uniquement, SRD 5.2.1). Pas de parsing ici : voir [LangueParser]
     * (SrdCreationParsers.kt), qui gère le format "### Groupe" + "- Nom".
     */
    suspend fun loadLangues(context: Context, worldId: String? = "donjon_et_dragon"): String =
        withContext(Dispatchers.IO) {
            if (worldId != "donjon_et_dragon") return@withContext ""
            languesCache[worldId]?.let { return@withContext it }
            val raw = readAsset(context, FILE_LANGUES)
            languesCache[worldId] = raw
            raw
        }

    /**
     * Charge la liste des dons pour le monde donné (D&D uniquement, SRD 5.2.1).
     * Format dédié "### Don" + "#### Compétence" (voir [DonsParser]) ;
     * [SrdSectionEntry.category] est la catégorie du don (Origines, Général, Style de combat,
     * Faveur épique), lue dans sa balise `categorie:`.
     */
    suspend fun loadDons(context: Context, worldId: String? = "donjon_et_dragon"): List<SrdSectionEntry> =
        withContext(Dispatchers.IO) {
            if (!isLibraryAvailable(worldId)) return@withContext emptyList()
            val key = worldId ?: ""
            donsCache[key]?.let { return@withContext it }
            ensureCustomContentLoaded(context, worldId)
            val official = if (worldId == "donjon_et_dragon") DonsParser.parse(readAsset(context, FILE_DONS)) else emptyList()
            val entries = official + customDonsCache[key].orEmpty()
            donsCache[key] = entries
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
            val raw = rulesFileForWorld(worldId)?.let { readAsset(context, it) } ?: ""
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
            monsters.filter { LibrarySearch.matchesName(query, it.name) }
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
            spells.filter { LibrarySearch.matchesName(query, it.name) }
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
        if (query.isBlank()) classes else classes.filter { LibrarySearch.matchesName(query, it.name) }
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
        if (query.isBlank()) especes else especes.filter { LibrarySearch.matchesName(query, it.name) }
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
        if (query.isBlank()) historiques else historiques.filter { LibrarySearch.matchesName(query, it.name) }
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
        if (query.isBlank()) dons else dons.filter { LibrarySearch.matchesName(query, it.name) }
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
        if (query.isBlank()) items else items.filter { LibrarySearch.matchesName(query, it.name) }
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
     * Force une recopie complète des fichiers SRD depuis les assets vers le stockage
     * public, en écrasant la copie existante quelle qu'elle soit (même si la version
     * stockée correspond déjà à [SRD_DATA_VERSION]), puis vide les caches en mémoire pour
     * que les prochains chargements relisent ces nouvelles copies. Déclenché manuellement
     * depuis l'écran de gestion des livres ("Forcer la mise à jour"), en complément du
     * contrôle automatique par version — utile par exemple juste après avoir remplacé un
     * fichier sans avoir encore incrémenté la version, ou pour réparer une copie publique
     * corrompue sans attendre la prochaine mise à jour de l'app.
     */
    fun forceRefreshFromAssets(context: Context) {
        forcedRefreshNeeded = true
        PublicFilesStore.writeText(context, SRD_VERSION_FILENAME, SRD_DATA_VERSION.toString(), SRD_VERSION_SUBFOLDER)
        clearCache()
    }

    /**
     * Invalide tous les caches en mémoire.
     */
    fun clearCache() {
        monstersCache.clear()
        spellsCache.clear()
        spellsRawCache.clear()
        rulesCache.clear()
        ruleEntriesCache.clear()
        rulesRawCache.clear()
        glossaryCache.clear()
        glossaryEntriesCache.clear()
        etatsCache.clear()
        equipmentListCache.clear()
        equipmentRawCache = null
        classesCache.clear()
        especesCache.clear()
        historiquesCache.clear()
        donsCache.clear()
        armesArmuresMagiquesCache.clear()
        monturesVehiculesCache.clear()
        spellsIndexCache.clear()
        languesCache.clear()
        customMonstersCache.clear()
        customEquipmentCache.clear()
        customSpellsCache.clear()
        customSpellsIndexCache.clear()
        customClassesCache.clear()
        customSousClassesCache.clear()
        customEspecesCache.clear()
        customHistoriquesCache.clear()
        customDonsCache.clear()
        customRulesCache.clear()
        customContentLoadedWorlds.clear()
    }

    /**
     * Invalide les caches (officiels et personnalisés) d'un seul monde, sans toucher aux
     * autres — appelé après l'ajout ou le retrait d'un livre personnalisé pour que le
     * prochain chargement refasse la fusion avec [ensureCustomContentLoaded], sans avoir à
     * tout recharger depuis les assets comme le ferait [clearCache].
     */
    fun invalidateWorld(worldId: String?) {
        val key = worldId ?: ""
        monstersCache.remove(key)
        etatsCache.remove(key)
        spellsCache.remove(key)
        spellsRawCache.remove(key)
        rulesCache.remove(key)
        ruleEntriesCache.remove(key)
        equipmentListCache.remove(key)
        classesCache.remove(key)
        especesCache.remove(key)
        historiquesCache.remove(key)
        donsCache.remove(key)
        spellsIndexCache.remove(key)
        customMonstersCache.remove(key)
        customEquipmentCache.remove(key)
        customSpellsCache.remove(key)
        customSpellsIndexCache.remove(key)
        customClassesCache.remove(key)
        customSousClassesCache.remove(key)
        customEspecesCache.remove(key)
        customHistoriquesCache.remove(key)
        customDonsCache.remove(key)
        customRulesCache.remove(key)
        customContentLoadedWorlds.remove(key)
    }

    /**
     * Lit un fichier SRD depuis le stockage public s'il est à jour (voir
     * [SRD_DATA_VERSION]), sinon depuis la version intégrée à l'app (assets/), et
     * (re)copie systématiquement cette dernière dans Téléchargements/JDRCompagnon/
     * quand la version stockée diffère. Ces fichiers ne sont pas destinés à être
     * modifiés par l'utilisateur ; la copie publique n'est qu'un cache, jamais
     * considérée comme faisant autorité une fois la version dépassée.
     */
    private fun readAsset(context: Context, path: String): String {
        val (subfolder, displayName) = splitPublicPath(path)
        if (!needsForcedRefresh(context)) {
            PublicFilesStore.readText(context, displayName, subfolder)?.let { return it }
        }

        val raw = context.assets.open(path).bufferedReader().use { it.readText() }
        PublicFilesStore.writeText(context, displayName, raw, subfolder)
        return raw
    }

    // Version des données SRD embarquées dans les assets : À INCRÉMENTER à chaque
    // modification d'un fichier .md source (ajout, correction, restructuration...).
    // Toute incrémentation force une recopie complète vers le stockage public au
    // prochain lancement, écrasant la copie précédente quelle qu'elle soit — c'est
    // le seul moyen fiable de propager un changement de contenu, puisque la copie
    // publique ne porte par elle-même aucune information de date ni de provenance.
    private const val SRD_DATA_VERSION = 3
    private const val SRD_VERSION_SUBFOLDER = "SRD"
    private const val SRD_VERSION_FILENAME = ".srd_version"

    // Résultat du contrôle de version, calculé une seule fois par session (évite de
    // relire le marqueur de version à chaque fichier SRD chargé).
    private var forcedRefreshNeeded: Boolean? = null

    /**
     * Compare la version stockée sur l'appareil à [SRD_DATA_VERSION]. Si elles
     * diffèrent (première installation, mise à jour de l'app avec des .md modifiés,
     * ou marqueur absent), met à jour le marqueur et retourne `true` : chaque appel à
     * [readAsset] de cette session ignorera alors la copie publique existante et la
     * régénérera depuis les assets.
     */
    private fun needsForcedRefresh(context: Context): Boolean {
        forcedRefreshNeeded?.let { return it }
        val storedVersion = PublicFilesStore.readText(context, SRD_VERSION_FILENAME, SRD_VERSION_SUBFOLDER)?.trim()
        val outdated = storedVersion != SRD_DATA_VERSION.toString()
        if (outdated) {
            PublicFilesStore.writeText(context, SRD_VERSION_FILENAME, SRD_DATA_VERSION.toString(), SRD_VERSION_SUBFOLDER)
        }
        forcedRefreshNeeded = outdated
        return outdated
    }

    /**
     * Convertit un chemin d'asset ("dnd/monster_srd521.md") en sous-dossier public +
     * nom de fichier ("SRD/dnd" + "monster_srd521.md").
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