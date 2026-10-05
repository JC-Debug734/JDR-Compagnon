package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.content.SharedPreferences
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Focaliseurs
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ObjetsACharges
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.separerObjetsCombines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import com.jc2.jdrcompagnon.di.HistoriqueDependencies
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupAsset
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupItem
import com.jc2.jdrcompagnon.feature_group.domain.model.Mount
import com.jc2.jdrcompagnon.feature_quete.domain.model.Quest
import com.jc2.jdrcompagnon.feature_group.domain.model.Reputation
import com.jc2.jdrcompagnon.feature_group.domain.model.Transport
import com.jc2.jdrcompagnon.feature_historique.data.HistoriqueEntreeExport
import com.jc2.jdrcompagnon.feature_historique.domain.TypeEvenementHistorique

typealias ScenarioFileEntry = PublicFilesStore.FileEntry

/**
 * Store global pour l'état du jeu (monde courant).
 * Utilise un singleton simple pour partager l'état entre les écrans.
 */
object GameState {

    // SharedPreferences pour la persistance
    private const val PREFS_NAME = "jdr_compagnon_dice"
    private const val KEY_DICE_STATE = "dice_state_json"
    private const val KEY_DICE_STATE_PREFIX = "dice_state_json_"
    private const val KEY_CHARACTERS = "characters_json"
    private const val KEY_NAHEULBEUK_CHARACTERS = "naheulbeuk_characters_json"
    private const val KEY_CURRENT_WORLD = "current_world_json"
    private const val KEY_MUSIC_SETTINGS = "music_settings_json"
    private const val KEY_WEATHER_SOUND_SETTINGS = "weather_sound_settings_json"
    private const val KEY_MJ_GROUPS = "mj_groups_json"
    private const val KEY_MJ_SCENARIOS = "mj_scenarios_json"
    private const val KEY_MJ_CAMPAIGNS = "mj_campaigns_json"
    private const val KEY_MJ_LAST_SCENARIO = "mj_last_scenario_id"
    private const val KEY_MJ_LAST_SCENARIO_PREFIX = "mj_last_scenario_id_"
    private const val KEY_APP_ROLE = "app_role"
    private const val KEY_SELECTED_CHARACTER_ID = "selected_character_id"
    private const val KEY_PLAYER_NAME = "player_name"
    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Context applicatif (pas d'Activity, donc pas de fuite mémoire) retenu pour pouvoir
     * écrire automatiquement un fichier par personnage à la création (voir [addCharacter]/
     * [writeCharacterFile]) sans devoir faire passer un Context à chaque call-site.
     */
    private var appContext: Context? = null

    private fun diceStateKey(worldId: String?): String =
        if (worldId.isNullOrBlank()) KEY_DICE_STATE else "$KEY_DICE_STATE_PREFIX$worldId"

    @Serializable
    data class MjGroup(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val memberIds: List<String> = emptyList(),
        val worldId: String = "",
        // Montures et moyens de transport : purement descriptifs (aucun impact mécanique/combat),
        // sur le même principe que les employés de feature_boutique. Réputation : suivi par
        // faction, propre au groupe (et non à un personnage individuel).
        val mounts: List<Mount> = emptyList(),
        val transports: List<Transport> = emptyList(),
        val reputations: List<Reputation> = emptyList(),
        // Joueurs sans fiche : pour animer une partie avec des personnes qui n'ont pas
        // l'application (ou pas de fiche dans celle-ci). Comptent comme des PJ pour le
        // calculateur de rencontre et peuvent entrer en combat (voir LancerCombatDialog).
        val tablePlayers: List<TablePlayer> = emptyList(),
        // Lieu où se trouve le groupe (ex. « Phandalin ») : lieu par défaut de ce qui n'en a pas.
        val location: String = "",
        // Trésor commun (po) : reste de l'or des quêtes non divisible entre les joueurs.
        val gold: Int = 0,
        val inventory: List<GroupItem> = emptyList(),
        val assets: List<GroupAsset> = emptyList(),
        // Position du groupe sur une carte de campagne (icône « compagnie »), en fractions de la
        // carte comme PointInteret.fx/fy. null = pas placé (repli : le lieu [location] s'il est sur la carte).
        val carteId: String? = null,
        val carteFx: Float? = null,
        val carteFy: Float? = null,
        // Icône du groupe sur la carte (clé de IconesPointInteret.PALETTE_GROUPE) et sa couleur
        // (ARGB) : choisies par le MJ comme pour un lieu ; null = icône « compagnie » dorée.
        val iconKey: String? = null,
        val couleurArgb: Int? = null,
    ) {
        /** Nombre total de membres : fiches liées + joueurs sans fiche. */
        val memberCount: Int get() = memberIds.size + tablePlayers.size
    }

    /** Joueur d'un groupe sans fiche associée (voir [MjGroup.tablePlayers]). */
    @Serializable
    data class TablePlayer(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val level: Int = 1,
        val armorClass: Int = 12,
        val maxHitPoints: Int = estimatedHitPoints(1),
    ) {
        companion object {
            /** PV moyens estimés d'un aventurier de ce niveau (d8 + Con +1, moyenne par niveau). */
            fun estimatedHitPoints(level: Int): Int = 9 + (level.coerceIn(1, 20) - 1) * 6
        }
    }

    @Serializable
    data class MjScene(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val markdownContent: String = "",
        val musicTrackId: String? = null,
        // Environnement (feature_environnement) rattaché à cette scène : affiché en bandeau par
        // ScenarioReaderScreen, et utilisé comme repli de musique quand musicTrackId est vide.
        val environmentId: String? = null,
        // Table aléatoire de type EVENEMENTS (feature_table_aleatoire) rattachée à cette scène :
        // le MJ peut y tirer une rencontre/découverte/rumeur pendant qu'il joue la scène. Si
        // vide, MjScenario.tableEvenementsId (table pour tout le scénario) sert de repli.
        val tableAleatoireId: String? = null,
        // Table aléatoire de type LOOT (feature_table_aleatoire) rattachée à cette scène : le
        // butin est propre à chaque scène (contrairement à la table d'événements, mutualisable
        // pour tout le scénario), d'où l'absence de repli au niveau scénario pour celle-ci.
        val tableLootId: String? = null,
        // Boutique (feature_boutique) présente dans la scène : icône à côté du loot en lecture,
        // qui affiche ses services et tarifs.
        val boutiqueId: String? = null,
        // Événements de la bibliothèque (feature_evenement) rattachés à cette scène : listés et
        // tirables en lecture (voir SceneToolsBar), en plus de la table d'événements.
        val evenementIds: List<String> = emptyList(),
        val order: Int = 0,
        // Suivi côté MJ : une scène jouée peut être marquée "validée" pour garder trace
        // de la progression dans le scénario d'une session à l'autre.
        val validated: Boolean = false,
        // Fiches des discussions (#event:[PNJ]) de la scène : ce que le PNJ attend de l'échange.
        val discussions: List<PnjDiscussion> = emptyList(),
    )

    /** Attitude initiale d'un PNJ au début d'une discussion : fixe le ND de base du test. */
    @Serializable
    enum class AttitudePnj(val label: String, val nd: Int) {
        AMICAL("Amical", 10),
        INDIFFERENT("Indifférent", 15),
        HOSTILE("Hostile", 20),
    }

    /**
     * Ce que le PNJ attend d'une discussion de scène (lien #event:[Nom]). Tout champ vide (ou
     * attitude null) est « libre » : le MJ le décide en jeu.
     * - [attitude] : ND de base du test de Charisme (10 / 15 / 20).
     * - [desirs] : motivations ; les utiliser dans l'argumentaire donne l'Avantage.
     * - [peurs] : sujets sensibles ; les aborder impose le Désavantage.
     * - [ligneRouge] : demande inacceptable, échec automatique quel que soit le dé.
     */
    @Serializable
    data class PnjDiscussion(
        val pnjName: String,
        val attitude: AttitudePnj? = null,
        val desirs: String = "",
        val peurs: String = "",
        val ligneRouge: String = "",
    ) {
        val estVide: Boolean get() = attitude == null && desirs.isBlank() && peurs.isBlank() && ligneRouge.isBlank()
    }

    // --- Ligne {mdiscussion: pnj=...; attitude=...; desirs=...; peurs=...; limite=...} ---
    // Valeurs échappées (\; \} \n \\) pour rester sur une ligne lisible dans le fichier .md.

    private fun escapeDiscussionValue(value: String) = value
        .replace("\\", "\\\\").replace(";", "\\;").replace("}", "\\}").replace("\n", "\\n")

    internal fun discussionToMetaLine(d: PnjDiscussion): String =
        "{mdiscussion: pnj=${escapeDiscussionValue(d.pnjName)}; attitude=${d.attitude?.name?.lowercase() ?: ""}; " +
            "desirs=${escapeDiscussionValue(d.desirs)}; peurs=${escapeDiscussionValue(d.peurs)}; " +
            "limite=${escapeDiscussionValue(d.ligneRouge)}}"

    internal fun discussionFromMetaLine(line: String): PnjDiscussion? {
        val fields = metaLineFields(line, "mdiscussion") ?: return null
        val nom = fields["pnj"]?.takeIf { it.isNotBlank() } ?: return null
        return PnjDiscussion(
            pnjName = nom,
            attitude = AttitudePnj.entries.firstOrNull { it.name.equals(fields["attitude"], ignoreCase = true) },
            desirs = fields["desirs"].orEmpty(),
            peurs = fields["peurs"].orEmpty(),
            ligneRouge = fields["limite"].orEmpty(),
        )
    }

    /** Balises de lignes d'en-tête traitées à l'import d'un scénario (voir ScenarioImport). */
    internal val ScenarioImportBalises = listOf("mimage", "mlieu", "mpnj", "mzone", "mchapitre")

    /**
     * Champs `clé=valeur` d'une ligne de métadonnées `{<balise>: clé=valeur; ...}` (valeurs
     * échappées \; \} \n \\), ou null si la ligne n'est pas de cette balise.
     */
    internal fun metaLineFields(line: String, balise: String): Map<String, String>? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{$balise:") || !trimmed.endsWith("}")) return null
        val body = trimmed.removePrefix("{$balise:").dropLast(1)
        val fields = mutableMapOf<String, String>()
        val current = StringBuilder()
        fun flush() {
            val part = current.toString()
            val eq = part.indexOf('=')
            if (eq > 0) fields[part.substring(0, eq).trim()] = part.substring(eq + 1).trim()
            current.clear()
        }
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                c == '\\' && i + 1 < body.length -> {
                    when (val next = body[i + 1]) {
                        'n' -> current.append('\n')
                        else -> current.append(next)
                    }
                    i++
                }
                c == ';' -> flush()
                else -> current.append(c)
            }
            i++
        }
        flush()
        return fields
    }

    @Serializable
    data class MjScenario(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val description: String = "",
        val markdownContent: String = "",
        val createdBy: String = "MJ",
        val worldId: String = "",
        val scenes: List<MjScene> = emptyList(),
        // Lieu associé au scénario, affiché sur la première page du lecteur (nom + image,
        // image copiée en stockage interne via ScenarioImageStore, même principe que
        // CarteImageStore pour les fonds de carte).
        val lieuNom: String = "",
        val lieuImageFileName: String? = null,
        // Table aléatoire de type EVENEMENTS (feature_table_aleatoire) utilisée pour tout le
        // scénario quand le MJ ne veut pas en configurer une par scène : repli utilisé par une
        // scène dont MjScene.tableAleatoireId est vide. Champ propre à l'app (comme lieuNom) :
        // non écrasé par une synchronisation depuis un fichier .md externe (voir syncScenariosFromDisk).
        val tableEvenementsId: String? = null,
        // Chapitre (numéro et titre) et numéro du scénario dans ce chapitre : ordre de jeu d'une
        // campagne, ligne {mchapitre:} du .md (voir ChapitresScenarios).
        val chapitreNumero: Int? = null,
        val chapitreTitre: String = "",
        val numero: Int? = null,
    )

    @Serializable
    data class CampaignChecklistItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val label: String,
        val checked: Boolean = false
    )

    /** Alias public pour l'extérieur du singleton. */
    data class CampaignData(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val worldId: String = "",
        val scenarioIds: List<String> = emptyList(),
        val checklistItems: List<CampaignChecklistItem> = emptyList(),
        val monsterIds: List<String> = emptyList(),
        // Calendrier (ScenarioClockState.CalendarConfig) choisi pour cette campagne, parmi ceux
        // créés dans l'outil Horloge. Null = calendrier actif par défaut.
        val calendarId: String? = null
    ) {
        fun toMjCampaign(): MjCampaign = MjCampaign(
            id = id,
            title = title,
            worldId = worldId,
            scenarioIds = scenarioIds,
            checklistItems = checklistItems,
            monsterIds = monsterIds,
            calendarId = calendarId
        )
    }

    @Serializable
    data class MjCampaign(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val description: String = "",
        val worldId: String = "",
        val scenarioIds: List<String> = emptyList(),
        val checklistItems: List<CampaignChecklistItem> = emptyList(),
        // Monstres du bestiaire SRD attachés à la campagne (identifiés par nom, comme Mount.species).
        val monsterIds: List<String> = emptyList(),
        // Calendrier (ScenarioClockState.CalendarConfig) choisi pour cette campagne, parmi ceux
        // créés dans l'outil Horloge. Null = calendrier actif par défaut.
        val calendarId: String? = null,
        // Fiches PNJ/créatures et livres personnalisés propres à la campagne : comme ses
        // scénarios, ils sont masqués tant que la campagne n'est pas sélectionnée (voir
        // PorteeCampagne), sauf s'ils sont aussi utilisés par ce qui est sélectionné.
        val pnjIds: List<String> = emptyList(),
        val livreIds: List<String> = emptyList(),
        // Image de la campagne affichée sur l'accueil MJ (carte "Campagne en cours") et en tête
        // de sa page de présentation ; copiée en stockage interne par CampagneImageStore.
        val imageFileName: String? = null,
        // Quêtes de la campagne (remplacent la fiche de suivi [checklistItems], migrée au chargement).
        val quests: List<Quest> = emptyList(),
        // Environnement (feature_environnement) par défaut de la campagne : sert aux événements
        // proposés au MJ pendant les trajets (événements liés à cet environnement).
        val environnementId: String? = null,
    ) {
        fun toCampaignData(): CampaignData = CampaignData(
            id = id,
            title = title,
            worldId = worldId,
            scenarioIds = scenarioIds,
            checklistItems = checklistItems,
            monsterIds = monsterIds,
            calendarId = calendarId
        )
    }

    // Ancien monde par défaut pour la migration des données sans worldId
    private const val LEGACY_WORLD_ID = "donjon_et_dragon"

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyData()
        // loadCurrentWorld() doit s'exécuter avant tout chargement dont la clé dépend du monde
        // (dé, campagne, groupe, dernier scénario) : sinon currentWorldId() valait encore null au
        // moment de la lecture, qui retombait sur la clé "sans monde" (jamais écrite par
        // setCurrentCampaignId/setCurrentGroupId, qui suffixent toujours par le monde courant une
        // fois celui-ci chargé) — la dernière campagne/le dernier scénario sélectionnés semblaient
        // alors "oubliés" à chaque redémarrage alors qu'ils étaient bien enregistrés.
        loadCurrentWorld()
        loadDiceState()
        loadCharacters()
        loadNaheulbeukCharacters()
        loadMjGroups()
        // Garde-fou : une liste vide peut venir d'un échec de lecture, on ne vide pas les groupes.
        if (_characters.value.isNotEmpty()) pruneDeletedGroupMembers()
        loadMjScenarios()
        loadMjCampaigns()
        loadLastScenarioId()
        loadCurrentCampaignId()
        loadCurrentGroupId()
        loadAppRole()
        loadSelectedCharacterId()
        loadPlayerName()
        importDefaultScenarioIfNeeded(context)
        syncScenariosFromDisk(context)
        com.jc2.jdrcompagnon.di.BoutiqueDependencies.init(context)
        com.jc2.jdrcompagnon.di.BoutiqueDependencies.initEquipementSource(context)
        com.jc2.jdrcompagnon.di.CarteDependencies.convertirAnciensEvenements()
        // Exemples de la bibliothèque d'événements du monde courant : ajout et mise à jour
        // (attendu, issues, profils) dès le lancement, sans attendre l'ouverture d'un outil.
        currentWorldId()?.let { com.jc2.jdrcompagnon.di.EvenementDependencies.seedExamplesIfNeeded(context, it) }
        loadWeatherSoundSettings()
        WeatherSoundManager.setVolume(_weatherSoundSettings.value.volume)
        ScenarioClockState.init(context)
        ImportedMusicStore.init(context)
    }

    /**
     * Importe le scénario exemple depuis les assets si aucun scénario n'existe pour le monde donjon_et_dragon.
     * Cela garantit que la balise couleur et la musique Taverne sont immédiatement testables.
     */
    private fun importDefaultScenarioIfNeeded(context: Context) {
        if (_mjScenarios.value.any { it.worldId == "donjon_et_dragon" }) return
        try {
            val markdown = context.assets.open("scenarios/la-mine-oubliee.md").bufferedReader().use { it.readText() }
            val parsed = parseMarkdownScenario(markdown)
            val scenario = MjScenario(
                title = parsed.first,
                description = "Scénario d'introduction pour Donjons et Dragons.",
                worldId = "donjon_et_dragon",
                scenes = parsed.second,
                createdBy = "MJ"
            )
            _mjScenarios.value = _mjScenarios.value + scenario
            saveMjScenarios(_mjScenarios.value)
            writeScenarioFile(context, scenario)
            android.util.Log.i("GameState", "Scénario exemple importé : ${scenario.title}")
        } catch (e: Exception) {
            android.util.Log.e("GameState", "Impossible d'importer le scénario exemple", e)
        }
    }

    /**
     * Parse un markdown de scénario structuré en scènes et métadonnées.
     * Format attendu :
     * - Titre principal (# Titre)
     * - Sections # SCENE — Titre
     * - Métadonnées {mscenemeta: music=nom}
     * - Contenu markdown jusqu'à la section suivante.
     */
    /** Extrait l'id caché (commentaire HTML) écrit par [scenarioToMarkdown], ou null si absent. */
    fun hiddenScenarioId(markdown: String): String? = hiddenIdRegex.find(markdown)?.groupValues?.get(1)

    /** Wrapper public de [parseMarkdownScenario], utilisé par CampagneFileStore pour importer des .md externes. */
    fun parseScenarioMarkdown(markdown: String): Pair<String, List<MjScene>> = parseMarkdownScenario(markdown)

    private val sceneHeaderRegex = Regex("""^#\s+SC[EÈ]NE\s*[—-]\s*(.*)$""", RegexOption.IGNORE_CASE)

    /**
     * Introduction destinée au MJ, écrite avant la première « # SCÈNE » (aperçu du lieu, niveau
     * recommandé, objectifs...) : gardée dans [MjScenario.description] et affichée en tête de la
     * première page du lecteur, sans devenir une scène à part. Sans le titre, ni les lignes
     * techniques ({mimage:}, {mpnj:}, {mzone:}...), ni les commentaires. Vide si le fichier n'a
     * pas de scène (tout son texte forme alors l'unique scène).
     */
    fun preambuleScenario(markdown: String): String {
        val lignes = markdown.lines()
        val avant = lignes.takeWhile { sceneHeaderRegex.find(it) == null }
        if (avant.size == lignes.size) return ""
        var titreVu = false
        return avant.filterNot { ligne ->
            val t = ligne.trimStart()
            val estTitre = !titreVu && t.startsWith("# ")
            if (estTitre) titreVu = true
            estTitre || t.startsWith("<!--") || t.startsWith("{mscenemeta") || t.startsWith("{mdiscussion") ||
                ScenarioImportBalises.any { t.startsWith("{$it:") }
        }.joinToString("\n").trim()
    }

    private fun parseMarkdownScenario(markdown: String): Pair<String, List<MjScene>> {
        val lines = markdown.lines()
        val title = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim() ?: "Scénario sans titre"
        val scenes = mutableListOf<MjScene>()
        var currentTitle = "Introduction"
        var currentMusic: String? = null
        var currentEnvironment: String? = null
        var currentTable: String? = null
        var currentLoot: String? = null
        var currentBoutique: String? = null
        var currentEvenements: String? = null
        val currentContent = StringBuilder()
        val currentDiscussions = mutableListOf<PnjDiscussion>()
        var inScene = false

        val metaRegex = Regex(
            """\{mscenemeta:\s*music=([^;}]*)(?:;\s*environment=([^;}]*))?(?:;\s*table=([^;}]*))?(?:;\s*loot=([^;}]*))?(?:;\s*boutique=([^;}]*))?(?:;\s*evenements=([^}]*))?\}"""
        )

        fun flushScene() {
            // Le texte avant la première "# SCÈNE" (résumé, niveau recommandé, accroche...)
            // ne doit pas devenir une scène jouable à part entière : ça produisait une
            // première page "Introduction" sans intérêt en lecture. Ce texte reste dans le
            // fichier source mais n'est plus transformé en MjScene.
            if (inScene) {
                scenes += MjScene(
                    title = currentTitle,
                    markdownContent = currentContent.toString().trim(),
                    musicTrackId = currentMusic?.takeIf { it.isNotBlank() },
                    environmentId = currentEnvironment?.takeIf { it.isNotBlank() },
                    tableAleatoireId = currentTable?.takeIf { it.isNotBlank() },
                    tableLootId = currentLoot?.takeIf { it.isNotBlank() },
                    boutiqueId = currentBoutique?.takeIf { it.isNotBlank() },
                    evenementIds = currentEvenements?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty(),
                    order = scenes.size,
                    discussions = currentDiscussions.toList(),
                )
            }
            currentContent.clear()
            currentDiscussions.clear()
            currentMusic = null
            currentEnvironment = null
            currentTable = null
            currentLoot = null
            currentBoutique = null
            currentEvenements = null
        }

        for (line in lines) {
            val sceneMatch = Regex("""^#\s+SC[EÈ]NE\s*[—-]\s*(.*)$""", RegexOption.IGNORE_CASE).find(line)
            if (sceneMatch != null) {
                flushScene()
                currentTitle = sceneMatch.groupValues[1].trim()
                inScene = true
                continue
            }
            val metaMatch = metaRegex.find(line)
            if (metaMatch != null) {
                currentMusic = metaMatch.groupValues[1].trim()
                currentEnvironment = metaMatch.groupValues[2].trim()
                currentTable = metaMatch.groupValues[3].trim()
                currentLoot = metaMatch.groupValues[4].trim()
                currentBoutique = metaMatch.groupValues[5].trim()
                currentEvenements = metaMatch.groupValues[6].trim()
                continue
            }
            val discussion = discussionFromMetaLine(line)
            if (discussion != null) {
                currentDiscussions += discussion
                continue
            }
            // Images embarquées, lieu et fiches PNJ : lus à l'import (ScenarioImport), jamais
            // affichés dans le texte d'une scène.
            if (ScenarioImportBalises.any { line.trimStart().startsWith("{$it:") }) continue
            currentContent.appendLine(line)
        }
        flushScene()

        if (scenes.isEmpty()) {
            scenes += MjScene(title = title, markdownContent = markdown.trim(), order = 0)
        }
        return title to scenes
    }

    /**
     * Emplacement public des fichiers .md des scénarios : Téléchargements/JDRCompagnon/Scenarios/.
     * La logique MediaStore elle-même est centralisée dans PublicFilesStore (partagée
     * avec SrdRepository pour le bestiaire/sorts/équipement/règles).
     */
    private const val SCENARIOS_SUBFOLDER = "Scenarios"

    /** Chemin (pour affichage à l'utilisateur) du dossier où sont stockés les .md. */
    fun scenariosDirectoryPath(context: Context): String = PublicFilesStore.directoryLabel(SCENARIOS_SUBFOLDER)

    /** Liste les fichiers .md présents dans Téléchargements/JDRCompagnon/Scenarios, triés par nom. */
    fun listScenarioFiles(context: Context): List<ScenarioFileEntry> =
        PublicFilesStore.list(context, SCENARIOS_SUBFOLDER, "md")

    fun slugify(text: String): String {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return normalized.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "scenario" }
    }

    fun scenarioFileName(scenario: MjScenario): String {
        val shortId = scenario.id.take(8)
        return "${slugify(scenario.title)}-$shortId.md"
    }

    /**
     * Sérialise un scénario au même format markdown que parseMarkdownScenario sait lire,
     * avec un identifiant caché (commentaire HTML) pour ré-associer le fichier au bon
     * scénario lors d'une synchronisation ultérieure, même si le titre a changé depuis.
     */
    fun scenarioToMarkdown(scenario: MjScenario): String {
        val builder = StringBuilder()
        builder.appendLine("<!-- id: ${scenario.id} -->")
        builder.appendLine("# ${scenario.title}")
        com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios.ligne(scenario)?.let { builder.appendLine(it) }
        builder.appendLine()
        // Introduction MJ (aperçu du lieu...) avant la première scène, relue par preambuleScenario.
        if (scenario.description.isNotBlank() && scenario.scenes.isNotEmpty()) {
            builder.appendLine(scenario.description.trim())
            builder.appendLine()
        }
        val scenesToWrite = scenario.scenes.ifEmpty {
            listOf(MjScene(title = scenario.title, markdownContent = scenario.markdownContent))
        }
        scenesToWrite.forEach { scene ->
            builder.appendLine("# SCÈNE — ${scene.title}")
            builder.appendLine()
            builder.appendLine("{mscenemeta: music=${scene.musicTrackId ?: ""}; environment=${scene.environmentId ?: ""}; table=${scene.tableAleatoireId ?: ""}; loot=${scene.tableLootId ?: ""}; boutique=${scene.boutiqueId ?: ""}; evenements=${scene.evenementIds.joinToString(",")}}")
            scene.discussions.filterNot { it.estVide }.forEach { builder.appendLine(discussionToMetaLine(it)) }
            builder.appendLine()
            builder.appendLine(scene.markdownContent.trim())
            builder.appendLine()
        }
        return builder.toString().trim() + "\n"
    }

    /**
     * Écrit (ou réécrit) le fichier .md correspondant à ce scénario dans
     * Téléchargements/JDRCompagnon/Scenarios/. Appelé après chaque création/modification
     * depuis l'éditeur, pour que le fichier reste synchronisé avec ce qui est enregistré
     * dans l'app.
     */
    fun writeScenarioFile(context: Context, scenario: MjScenario) {
        PublicFilesStore.writeText(
            context = context,
            displayName = scenarioFileName(scenario),
            content = scenarioToMarkdown(scenario),
            subfolder = SCENARIOS_SUBFOLDER
        )
    }

    private val hiddenIdRegex = Regex("""<!--\s*id:\s*([a-zA-Z0-9-]+)\s*-->""")

    /**
     * Scanne Téléchargements/JDRCompagnon/ et synchronise avec l'app :
     * - un fichier dont l'id caché correspond à un scénario existant met à jour son
     *   titre/ses scènes (le fichier a été édité depuis l'extérieur de l'app) ;
     * - un fichier sans id caché reconnu (ajouté manuellement, copié depuis un autre
     *   appareil...) est importé comme nouveau scénario, rattaché au monde courant,
     *   et le fichier est réécrit avec son nouvel id pour les prochaines synchros.
     * Appelée à l'ouverture de l'app et à chaque ouverture de l'écran/menu Scénarios,
     * pour qu'un fichier ajouté manuellement apparaisse immédiatement.
     */
    fun syncScenariosFromDisk(context: Context) {
        val files = listScenarioFiles(context)

        var current = _mjScenarios.value
        var changed = false

        // ids retrouvés sur le disque au fil du scan ci-dessous, pour pouvoir ensuite
        // retirer de l'app les scénarios dont le fichier .md a été supprimé manuellement
        // (auparavant, un scénario supprimé côté disque restait indéfiniment visible et
        // accessible dans l'app, car cette fonction ne faisait qu'ajouter/mettre à jour,
        // jamais supprimer).
        val idsFoundOnDisk = mutableSetOf<String>()

        for (entry in files) {
            // Fichier déjà importé qu'Android n'a pas laissé supprimer : ne pas le réimporter.
            if (fichierScenarioIgnore(entry)) continue
            val markdown = try {
                context.contentResolver.openInputStream(entry.uri)?.bufferedReader()?.use { it.readText() }
                    ?: continue
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Impossible de lire ${entry.name}", e)
                continue
            }
            val hiddenId = hiddenIdRegex.find(markdown)?.groupValues?.get(1)
            val parsed = parseMarkdownScenario(markdown)
            val existing = hiddenId?.let { id -> current.firstOrNull { it.id == id } }

            if (existing != null) {
                // Réutilise les ids de scènes existants par position : parseMarkdownScenario
                // génère toujours de nouveaux ids aléatoires pour les MjScene reconstruites
                // depuis le markdown, ce qui rendait la comparaison "changed" systématiquement
                // vraie (et déclenchait une réécriture à chaque lancement de l'app) même sans
                // modification réelle du contenu.
                val mergedScenes = parsed.second.mapIndexed { index, scene ->
                    existing.scenes.getOrNull(index)?.let { scene.copy(id = it.id) } ?: scene
                }
                // Chapitre : celui du fichier s'il en porte un, sinon celui déjà enregistré.
                val chapitre = com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios.lire(markdown)
                val updated = existing.copy(
                    title = parsed.first,
                    scenes = mergedScenes,
                    description = preambuleScenario(markdown).ifBlank { existing.description },
                    chapitreNumero = if (chapitre != null) chapitre.numero else existing.chapitreNumero,
                    chapitreTitre = chapitre?.titre ?: existing.chapitreTitre,
                    numero = if (chapitre != null) chapitre.scenario else existing.numero,
                )
                idsFoundOnDisk += existing.id
                if (updated != existing) {
                    current = current.map { if (it.id == existing.id) updated else it }
                    changed = true
                }
            } else {
                // Même traitement qu'un import manuel : images embarquées, lieu et fiches PNJ.
                // L'id éventuellement porté par le fichier est conservé (fichier d'un scénario
                // supprimé de la liste, ou venu d'un autre appareil).
                val (newScenario, nouveauxPnj) = com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImport
                    .preparer(context, markdown, currentWorldId() ?: "", idImpose = hiddenId)
                nouveauxPnj.forEach { addCharacter(it) }
                current = current + newScenario
                idsFoundOnDisk += newScenario.id
                changed = true
                // Réécrit le scénario dans son fichier attitré (nom + id), puis retire le fichier
                // d'origine s'il en est un autre : laissé en place, il était réimporté comme un
                // nouveau scénario à chaque retour sur la liste (duplication).
                writeScenarioFile(context, newScenario)
                if (entry.name != scenarioFileName(newScenario)) supprimerOuIgnorerFichier(context, entry)
                android.util.Log.i("GameState", "Nouveau scénario importé depuis le disque : ${newScenario.title}")
            }
        }

        // Nettoie les copies identiques déjà créées par l'ancienne duplication : même monde,
        // même titre, mêmes scènes. La première est gardée, les autres sont supprimées avec
        // leur fichier et leurs images.
        val (gardes, copies, versGarde) = scenariosSansCopies(current)
        if (copies.isNotEmpty()) {
            // Les campagnes et la sélection qui pointaient vers une copie pointent désormais
            // vers le scénario gardé (versGarde).
            copies.forEach { copie ->
                deleteScenarioFile(context, copie)
                android.util.Log.i("GameState", "Copie de scénario supprimée : ${copie.title}")
            }
            _mjCampaigns.value.filter { c -> c.scenarioIds.any { it in versGarde } }.forEach { c ->
                updateMjCampaign(c.copy(scenarioIds = c.scenarioIds.map { versGarde[it] ?: it }.distinct()))
            }
            _lastScenarioId.value?.let { versGarde[it] }?.let { setLastScenarioId(it) }
            current = gardes
            changed = true
        }

        // Retire les scénarios dont le fichier .md n'existe plus sur le disque (supprimé
        // manuellement en dehors de l'app, ou depuis un autre appareil).
        val orphaned = current.filter { it.id !in idsFoundOnDisk }
        if (orphaned.isNotEmpty()) {
            current = current - orphaned.toSet()
            changed = true
            orphaned.forEach {
                android.util.Log.i("GameState", "Scénario retiré (fichier supprimé) : ${it.title}")
            }
        }

        if (changed) {
            _mjScenarios.value = current
            saveMjScenarios(current)
        }
    }

    /**
     * Réinitialise le dé au changement de monde (charge l'état du monde sélectionné).
     */
    private fun reloadDiceStateForWorld(worldId: String?) {
        val jsonString = prefs?.getString(diceStateKey(worldId), "") ?: ""
        _diceState.value = if (jsonString.isNotBlank()) {
            try {
                json.decodeFromString<DiceState>(jsonString).copy(worldId = worldId ?: "")
            } catch (_: Exception) {
                DiceState(worldId = worldId ?: "")
            }
        } else {
            DiceState(worldId = worldId ?: "")
        }
    }

    /**
     * Migre les données existantes (MJ groups, scenarios, dice state) sans worldId
     * en les assignant au monde legacy D&D.
     */
    private fun migrateLegacyData() {
        prefs?.let { p ->
            // Migration dice state legacy vers D&D
            val legacyDice = p.getString(KEY_DICE_STATE, null)
            if (legacyDice != null) {
                p.edit().putString(diceStateKey(LEGACY_WORLD_ID), legacyDice).remove(KEY_DICE_STATE).apply()
            }
            val groupsJson = p.getString(KEY_MJ_GROUPS, "") ?: ""
            if (groupsJson.isNotBlank()) {
                try {
                    val groups = json.decodeFromString<List<MjGroup>>(groupsJson)
                    if (groups.any { it.worldId.isBlank() }) {
                        val migrated = groups.map { if (it.worldId.isBlank()) it.copy(worldId = LEGACY_WORLD_ID) else it }
                        saveMjGroups(migrated)
                    }
                } catch (_: Exception) { }
            }
            val scenariosJson = p.getString(KEY_MJ_SCENARIOS, "") ?: ""
            if (scenariosJson.isNotBlank()) {
                try {
                    val scenarios = json.decodeFromString<List<MjScenario>>(scenariosJson)
                    val migrated = scenarios.map { scenario ->
                        var s = if (scenario.worldId.isBlank()) scenario.copy(worldId = LEGACY_WORLD_ID) else scenario
                        // Migration markdown unique -> scene Scène 1
                        if (s.scenes.isEmpty() && s.markdownContent.isNotBlank()) {
                            s = s.copy(
                                scenes = listOf(MjScene(title = "Scène 1", markdownContent = s.markdownContent, order = 0)),
                                markdownContent = ""
                            )
                        }
                        s
                    }
                    saveMjScenarios(migrated)
                } catch (_: Exception) { }
            }
            // Migration du dernier scénario : si une clé legacy existe, la copier vers la clé legacy world
            val legacyLast = p.getString(KEY_MJ_LAST_SCENARIO, null)
            if (legacyLast != null) {
                p.edit().putString(lastScenarioKey(LEGACY_WORLD_ID), legacyLast).remove(KEY_MJ_LAST_SCENARIO).apply()
            }
        }
    }

    private val _musicSettings = MutableStateFlow(MusicSettings())
    val musicSettings: StateFlow<MusicSettings> = _musicSettings.asStateFlow()

    fun loadMusicSettings() {
        prefs?.let { p ->
            val jsonString = p.getString(KEY_MUSIC_SETTINGS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    _musicSettings.value = json.decodeFromString<MusicSettings>(jsonString)
                } catch (_: Exception) { }
            }
        }
    }

    fun saveMusicSettings(settings: MusicSettings) {
        _musicSettings.value = settings
        prefs?.edit()?.apply {
            putString(KEY_MUSIC_SETTINGS, json.encodeToString(settings))
            apply()
        }
    }

    private val _weatherSoundSettings = MutableStateFlow(WeatherSoundSettings())
    val weatherSoundSettings: StateFlow<WeatherSoundSettings> = _weatherSoundSettings.asStateFlow()

    fun loadWeatherSoundSettings() {
        prefs?.let { p ->
            val jsonString = p.getString(KEY_WEATHER_SOUND_SETTINGS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    _weatherSoundSettings.value = json.decodeFromString<WeatherSoundSettings>(jsonString)
                } catch (_: Exception) { }
            }
        }
    }

    fun saveWeatherSoundSettings(settings: WeatherSoundSettings) {
        _weatherSoundSettings.value = settings
        prefs?.edit()?.apply {
            putString(KEY_WEATHER_SOUND_SETTINGS, json.encodeToString(settings))
            apply()
        }
    }

    private fun loadDiceState() {
        val worldId = currentWorldId()
        prefs?.let { prefs ->
            val jsonString = prefs.getString(diceStateKey(worldId), "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val state = json.decodeFromString<DiceState>(jsonString)
                    _diceState.value = state.copy(worldId = worldId ?: "")
                } catch (_: Exception) {
                    // Ignore parsing errors, keep default state
                }
            }
        }
    }

    private fun saveDiceState(state: DiceState) {
        val worldId = currentWorldId()
        prefs?.edit()?.apply {
            putString(diceStateKey(worldId), json.encodeToString(state.copy(worldId = worldId ?: "")))
            apply()
        }
    }

    /** Modificateur de caractéristique D&D. */
    fun abilityModifier(score: Int): Int = ArmorRules.abilityModifierPublic(score)

    /**
     * Calcule la CA détaillée d'un personnage depuis equippedSlots.
     * Délégué à ArmorRules (source unique de vérité V7).
     */
    fun armorClassBreakdown(character: Character): ArmorClassBreakdown {
        val breakdown = ArmorRules.computeAc(character)
        return ArmorClassBreakdown(
            total = breakdown.total,
            detail = breakdown.detail,
            hasArmor = breakdown.hasArmor,
            hasShield = breakdown.hasShield,
            armorName = breakdown.armorName
        )
    }

    fun effectiveArmorClass(character: Character): Int = armorClassBreakdown(character).total

    /**
     * Charge max (Force × 7,5).
     */
    fun maxCarryWeight(character: Character): Double {
        return character.strength * 7.5
    }

    /**
     * Poids total de l'équipement porté + sac, retourné en KILOGRAMMES.
     * La source SRD fournit les poids en livres (lb) ; on convertit ici.
     */
    fun totalEquipmentWeight(character: Character): Double {
        val items = character.backpackItems + character.equippedItems + character.backpackExteriorSlots.values +
            character.quiverContents + character.grimoireContents
        val totalLbs = items.sumOf { itemName ->
            ArmorRules.weightInPounds(itemName) ?: 0.0
        }
        return totalLbs / 2.20462
    }

    /** Capacité du sac à dos, SRD : 30 livres (≈ 13,6 kg), convertie en kg. */
    const val BACKPACK_CAPACITY_KG = 30.0 / 2.20462

    /** Poids (kg) des seuls objets stockés dans le sac à dos (hors objets équipés). */
    fun backpackWeight(character: Character): Double {
        val totalLbs = character.backpackItems.sumOf { itemName -> ArmorRules.weightInPounds(itemName) ?: 0.0 }
        return totalLbs / 2.20462
    }

    /** Capacité d'une bourse à la ceinture, SRD : 6 livres (≈ 2,7 kg), convertie en kg. */
    const val PURSE_CAPACITY_KG = 6.0 / 2.20462

    /** Poids (kg) des pièces d'or de la bourse, SRD : 50 pièces par livre. */
    fun purseWeight(character: Character): Double = (character.gold / 50.0) / 2.20462

    private fun saveCharacters(list: List<Character>) {
        prefs?.edit()?.apply {
            try {
                putString(KEY_CHARACTERS, json.encodeToString(list))
                apply()
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Error saving characters", e)
            }
        }
    }

    private fun saveMjGroups(list: List<MjGroup>) {
        prefs?.edit()?.apply {
            try {
                putString(KEY_MJ_GROUPS, json.encodeToString(list))
                apply()
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Error saving MJ groups", e)
            }
        }
    }

    private fun saveMjScenarios(list: List<MjScenario>) {
        prefs?.edit()?.apply {
            try {
                putString(KEY_MJ_SCENARIOS, json.encodeToString(list))
                apply()
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Error saving MJ scenarios", e)
            }
        }
    }

    private fun saveNaheulbeukCharacters(list: List<NaheulbeukCharacter>) {
        prefs?.edit()?.apply {
            try {
                putString(KEY_NAHEULBEUK_CHARACTERS, json.encodeToString(list))
                apply()
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Error saving Naheulbeuk characters", e)
            }
        }
    }

    private fun loadCharacters() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_CHARACTERS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val list = json.decodeFromString<List<Character>>(jsonString)
                    val migrated = list.map { migrateEquippedItemsToSlots(splitCombinedItems(it)) }
                    _characters.value = migrated
                    if (migrated.any { it.equippedSlots.isNotEmpty() } || migrated != list) {
                        saveCharacters(migrated)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading characters", e)
                }
            }
        }
    }

    /**
     * Sépare les objets combinés hérités d'un ancien équipement de départ (ex. "Arc court +
     * 20 flèches" -> "Arc court" + 20 "Flèches") restés dans le sac. Un objet combiné déjà
     * équipé est ramené dans le sac avant d'être séparé.
     */
    private fun splitCombinedItems(character: Character): Character {
        fun isCombined(name: String) = separerObjetsCombines(name).size > 1
        val all = character.backpackItems + character.equippedItems + character.equippedSlots.values
        if (all.none(::isCombined)) return character
        val equippedCombined = character.equippedSlots.filterValues(::isCombined)
        val backpack = (character.backpackItems + equippedCombined.values.distinct())
            .flatMap(::separerObjetsCombines)
        return character.copy(
            backpackItems = backpack,
            equippedItems = character.equippedItems.filterNot(::isCombined),
            equippedSlots = character.equippedSlots - equippedCombined.keys
        )
    }

    private fun migrateEquippedItemsToSlots(character: Character): Character {
        if (character.equippedSlots.isNotEmpty()) return character
        if (character.equippedItems.isEmpty()) return character
        val slots = mutableMapOf<EquipmentSlot, String>()
        for (item in character.equippedItems) {
            val slot = ArmorRules.slotForItem(item) ?: continue
            if (!slots.containsKey(slot)) {
                slots[slot] = item
            }
        }
        return character.copy(equippedSlots = slots)
    }

    private fun loadMjGroups() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_MJ_GROUPS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val list = json.decodeFromString<List<MjGroup>>(jsonString)
                    _mjGroups.value = list
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading MJ groups", e)
                }
            }
        }
    }

    private fun loadMjScenarios() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_MJ_SCENARIOS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val list = json.decodeFromString<List<MjScenario>>(jsonString)
                    _mjScenarios.value = list
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading MJ scenarios", e)
                }
            }
        }
    }

    private fun loadNaheulbeukCharacters() {
        prefs?.let { p ->
            val jsonString = p.getString(KEY_NAHEULBEUK_CHARACTERS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val list = json.decodeFromString<List<NaheulbeukCharacter>>(jsonString)
                    _naheulbeukCharacters.value = list
                } catch (_: Exception) { }
            }
        }
    }

    private fun loadCurrentWorld() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_CURRENT_WORLD, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val world = json.decodeFromString<WorldState>(jsonString)
                    _currentWorld.value = world
                } catch (ignored: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun saveCurrentWorld(world: WorldState?) {
        prefs?.edit()?.apply {
            if (world != null) {
                putString(KEY_CURRENT_WORLD, json.encodeToString(world))
            } else {
                remove(KEY_CURRENT_WORLD)
            }
            apply()
        }
    }

    private val _currentWorld = MutableStateFlow<WorldState?>(null)
    val currentWorld: StateFlow<WorldState?> = _currentWorld.asStateFlow()

    private val _mjGroups = MutableStateFlow<List<MjGroup>>(emptyList())
    val mjGroups: StateFlow<List<MjGroup>> = _mjGroups.asStateFlow()

    private val _mjScenarios = MutableStateFlow<List<MjScenario>>(emptyList())
    val mjScenarios: StateFlow<List<MjScenario>> = _mjScenarios.asStateFlow()

    private val _mjCampaigns = MutableStateFlow<List<MjCampaign>>(emptyList())
    val mjCampaigns: StateFlow<List<MjCampaign>> = _mjCampaigns.asStateFlow()

    private val _lastScenarioId = MutableStateFlow<String?>(null)
    val lastScenarioId: StateFlow<String?> = _lastScenarioId.asStateFlow()

    // Scène en cours de lecture, par scénario : partagée entre le lecteur (ScenarioReaderContent)
    // et le menu latéral MJ (profils présents dans la scène), et persistée pour la retrouver
    // d'une session à l'autre.
    private const val KEY_CURRENT_SCENE_INDEX_PREFIX = "current_scene_index_"
    private val _currentSceneIndexByScenario = MutableStateFlow<Map<String, Int>>(emptyMap())
    val currentSceneIndexByScenario: StateFlow<Map<String, Int>> = _currentSceneIndexByScenario.asStateFlow()

    fun currentSceneIndex(scenarioId: String): Int =
        _currentSceneIndexByScenario.value[scenarioId]
            ?: prefs?.getInt(KEY_CURRENT_SCENE_INDEX_PREFIX + scenarioId, 0)
            ?: 0

    fun setCurrentSceneIndex(scenarioId: String, index: Int) {
        _currentSceneIndexByScenario.value = _currentSceneIndexByScenario.value + (scenarioId to index)
        prefs?.edit()?.putInt(KEY_CURRENT_SCENE_INDEX_PREFIX + scenarioId, index)?.apply()
    }

    private val _currentCampaignId = MutableStateFlow<String?>(null)
    val currentCampaignId: StateFlow<String?> = _currentCampaignId.asStateFlow()

    private val _currentGroupId = MutableStateFlow<String?>(null)
    val currentGroupId: StateFlow<String?> = _currentGroupId.asStateFlow()

    fun selectWorld(world: WorldState) {
        val previousWorld = _currentWorld.value
        _currentWorld.value = world
        saveCurrentWorld(world)
        if (previousWorld?.id != world.id) {
            reloadDiceStateForWorld(world.id)
            // Campagne et groupe courants sont mémorisés par monde : les recharger pour ne pas
            // garder la sélection (et donc les scénarios) d'un autre univers.
            loadCurrentCampaignId()
            loadCurrentGroupId()
        }
        ensureLastScenarioLoadedForWorld(world.id)
    }

    @Suppress("unused")
    fun clearWorld() {
        _currentWorld.value = null
        saveCurrentWorld(null)
        _lastScenarioId.value = null
    }

    fun isWorldSelected(): Boolean = _currentWorld.value != null

    // === RÔLE COURANT (MJ / JOUEUR) ===
    // Distinct de la session réseau (NetworkSessionManager.role) : sert à
    // savoir sous quel rôle l'utilisateur navigue dans l'app (pour rediriger
    // l'icône Connexion de la barre du bas vers "héberger" ou "rejoindre").

    private val _appRole = MutableStateFlow<AppRole?>(null)
    val appRole: StateFlow<AppRole?> = _appRole.asStateFlow()

    private fun loadAppRole() {
        val stored = prefs?.getString(KEY_APP_ROLE, null)
        _appRole.value = when (stored) {
            AppRole.MJ.name -> AppRole.MJ
            AppRole.JOUEUR.name -> AppRole.JOUEUR
            else -> null
        }
    }

    private fun saveAppRole(role: AppRole?) {
        prefs?.edit()?.apply {
            if (role != null) {
                putString(KEY_APP_ROLE, role.name)
            } else {
                remove(KEY_APP_ROLE)
            }
            apply()
        }
    }

    fun setAppRole(role: AppRole) {
        _appRole.value = role
        saveAppRole(role)
    }

    // Demande de retour au choix de rôle, déclenchée depuis le menu latéral
    // (AppDrawer, commun MJ/Joueur). Même principe que l'overlay du dé
    // (setDiceOverlayVisible) : un flag global que NavGraph observe, pour
    // éviter de faire remonter un callback à travers tous les écrans qui
    // affichent le drawer.
    private val _roleChangeRequested = MutableStateFlow(false)
    val roleChangeRequested: StateFlow<Boolean> = _roleChangeRequested.asStateFlow()

    fun requestRoleChange() {
        _roleChangeRequested.value = true
    }

    fun consumeRoleChangeRequest() {
        _roleChangeRequested.value = false
    }

    // Demande de retour au tableau de bord MJ, déclenchée par le bouton "Accueil" de la
    // barre du bas (AppBottomBar). Quand on est déjà sur la route MjHome (ex : en mode
    // lecture de scénario), navController.navigate(homeRoute) est un no-op car c'est déjà
    // la destination courante — ce flag permet à MjHomeScreen de quitter le mode lecture
    // dans ce cas, sur le même principe que roleChangeRequested.
    private val _homeResetRequested = MutableStateFlow(false)
    val homeResetRequested: StateFlow<Boolean> = _homeResetRequested.asStateFlow()

    fun requestHomeReset() {
        _homeResetRequested.value = true
    }

    fun consumeHomeResetRequest() {
        _homeResetRequested.value = false
    }

    // === PERSONNAGE SÉLECTIONNÉ ===
    // Dernier personnage consulté/joué — persiste à la fermeture de l'app,
    // pour le retrouver directement au prochain lancement.

    private val _selectedCharacterId = MutableStateFlow<String?>(null)
    val selectedCharacterId: StateFlow<String?> = _selectedCharacterId.asStateFlow()

    private fun loadSelectedCharacterId() {
        _selectedCharacterId.value = prefs?.getString(KEY_SELECTED_CHARACTER_ID, null)
    }

    private fun saveSelectedCharacterId(characterId: String?) {
        prefs?.edit()?.apply {
            if (characterId != null) {
                putString(KEY_SELECTED_CHARACTER_ID, characterId)
            } else {
                remove(KEY_SELECTED_CHARACTER_ID)
            }
            apply()
        }
    }

    fun selectCharacter(characterId: String?) {
        _selectedCharacterId.value = characterId
        saveSelectedCharacterId(characterId)
    }

    /** Le personnage sélectionné, s'il existe encore dans la liste actuelle. */
    fun selectedCharacter(): Character? =
        _selectedCharacterId.value?.let { id -> _characters.value.find { it.id == id } }

    // === NOM DU JOUEUR ===
    // Demandé au premier lancement (écran de choix du rôle) et persisté.
    // Si l'utilisateur ne choisit pas de nom, un pseudo aléatoire à
    // consonance JDR est généré et enregistré à sa place.

    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName.asStateFlow()

    private fun loadPlayerName() {
        _playerName.value = prefs?.getString(KEY_PLAYER_NAME, null)
    }

    private fun savePlayerName(name: String?) {
        prefs?.edit()?.apply {
            if (name != null) {
                putString(KEY_PLAYER_NAME, name)
            } else {
                remove(KEY_PLAYER_NAME)
            }
            apply()
        }
    }

    /** Vrai tant qu'aucun nom (choisi ou généré) n'a encore été enregistré. */
    fun isPlayerNameSet(): Boolean = _playerName.value != null

    /**
     * Définit le nom du joueur (saisi manuellement ou généré) et le persiste.
     * Un nom vide/blanc est ignoré.
     */
    fun setPlayerName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _playerName.value = trimmed
        savePlayerName(trimmed)
    }

    private val randomPlayerNamePool = listOf(
        "Grokk le Hardi", "Elandril Vif-Argent", "Bromir Barbe-de-Fer",
        "Syldra Nuit-d'Ombre", "Kael Tranche-Vent", "Ombeline la Rôdeuse",
        "Thorgan Poing-de-Roc", "Lyanae des Brumes", "Fendard le Naheulien",
        "Wilfried Sans-Peur", "Aranthir Feu-Follet", "Morgane Piètre-Chance",
        "Ragnok Casse-Bouclier", "Tildwen l'Egarée", "Bogrom le Barde Ivre",
        "Elowyn Lame-Claire", "Grunt le Malchanceux", "Séraphine des Cimes",
        "Durnan Cœur-de-Pierre", "Fizban le Distrait",
    )

    /** Génère un pseudo aléatoire à consonance fantasy/JDR. */
    fun generateRandomPlayerName(): String = randomPlayerNamePool.random()

    /**
     * Génère un pseudo aléatoire, l'enregistre comme nom du joueur et le
     * retourne — utilisé quand l'utilisateur ne veut pas choisir de nom.
     */
    fun assignRandomPlayerName(): String {
        val generated = generateRandomPlayerName()
        setPlayerName(generated)
        return generated
    }

    // === PERSONNAGES CRÉÉS ===

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    private val _naheulbeukCharacters = MutableStateFlow<List<NaheulbeukCharacter>>(emptyList())
    val naheulbeukCharacters: StateFlow<List<NaheulbeukCharacter>> = _naheulbeukCharacters.asStateFlow()

    private val historiqueScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class EntreeAJournaliser(
        val type: TypeEvenementHistorique,
        val description: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Insère les entrées d'historique puis réécrit le fichier JSON du personnage (qui embarque
     * tout l'historique, voir [writeCharacterFile]) — dans cet ordre et dans une seule coroutine,
     * pour que le fichier exporté soit toujours à jour par rapport à ce qui vient d'être inséré.
     */
    private fun logEtExporter(character: Character, entrees: List<EntreeAJournaliser>) {
        if (entrees.isEmpty()) return
        historiqueScope.launch {
            entrees.forEach { entree ->
                HistoriqueDependencies.repository.ajouterEntree(character.id, entree.type, entree.description, entree.timestamp)
            }
            appContext?.let { writeCharacterFile(it, character) }
        }
    }

    /**
     * Point de passage unique pour toute mutation d'un Character existant : persiste la
     * nouvelle valeur ET journalise automatiquement les changements notables (voir [diffEntries]),
     * afin que le journal de personnage n'ait pas besoin d'un call-site dédié à chaque endroit
     * de l'app qui modifie un personnage.
     */
    private fun applyCharacterUpdate(characterId: String, transform: (Character) -> Character) {
        val current = _characters.value.find { it.id == characterId } ?: return
        val updated = releaseLostFocus(releaseLostAttunements(releaseGrimoireContents(releaseQuiverIfUnequipped(transform(current)))))
        val newList = _characters.value.map { if (it.id == characterId) updated else it }
        _characters.value = newList
        saveCharacters(newList)
        logEtExporter(updated, diffEntries(current, updated))
    }

    /** Compare deux versions d'un personnage et retourne les entrées d'historique à journaliser. */
    private fun diffEntries(old: Character, new: Character): List<EntreeAJournaliser> {
        if (old == new) return emptyList()
        val entrees = mutableListOf<EntreeAJournaliser>()

        val caracteristiques = listOf(
            "Force" to (old.strength to new.strength),
            "Dextérité" to (old.dexterity to new.dexterity),
            "Constitution" to (old.constitution to new.constitution),
            "Intelligence" to (old.intelligence to new.intelligence),
            "Sagesse" to (old.wisdom to new.wisdom),
            "Charisme" to (old.charisma to new.charisma)
        )
        caracteristiques.forEach { (nom, valeurs) ->
            val (avant, apres) = valeurs
            if (avant != apres) entrees += EntreeAJournaliser(TypeEvenementHistorique.CARACTERISTIQUE, "$nom : $avant → $apres")
        }

        if (old.level != new.level) {
            entrees += EntreeAJournaliser(TypeEvenementHistorique.NIVEAU, "Niveau ${old.level} → ${new.level}")
        }

        if (old.maxHitPoints != new.maxHitPoints) {
            entrees += EntreeAJournaliser(TypeEvenementHistorique.POINTS_DE_VIE, "Points de vie maximum : ${old.maxHitPoints} → ${new.maxHitPoints}")
        }

        val ancienEquipement = old.backpackItems + old.equippedItems
        val nouvelEquipement = new.backpackItems + new.equippedItems
        val equipementGagne = nouvelEquipement - ancienEquipement
        val equipementPerdu = ancienEquipement - nouvelEquipement
        equipementGagne.forEach { entrees += EntreeAJournaliser(TypeEvenementHistorique.EQUIPEMENT, "Nouvel équipement : $it") }
        equipementPerdu.forEach { entrees += EntreeAJournaliser(TypeEvenementHistorique.EQUIPEMENT, "Équipement retiré : $it") }

        val sortsGagnes = new.spells - old.spells
        val sortsPerdus = old.spells - new.spells
        sortsGagnes.forEach { entrees += EntreeAJournaliser(TypeEvenementHistorique.SORT, "Nouveau sort appris : $it") }
        sortsPerdus.forEach { entrees += EntreeAJournaliser(TypeEvenementHistorique.SORT, "Sort oublié : $it") }

        if (old.gold != new.gold) {
            val diff = new.gold - old.gold
            val signe = if (diff > 0) "+" else ""
            entrees += EntreeAJournaliser(TypeEvenementHistorique.OR, "Or : $signe$diff po (total ${new.gold} po)")
        }

        if (old.condition != new.condition && new.condition.isNotBlank()) {
            entrees += EntreeAJournaliser(TypeEvenementHistorique.ETAT, "État : ${new.condition}")
        }

        if (old.exhaustionLevel != new.exhaustionLevel) {
            entrees += EntreeAJournaliser(TypeEvenementHistorique.ETAT, "Épuisement : ${old.exhaustionLevel} → ${new.exhaustionLevel}")
        }

        if (old.name != new.name) {
            entrees += EntreeAJournaliser(TypeEvenementHistorique.AUTRE, "Renommé : ${old.name} → ${new.name}")
        }

        return entrees
    }

    /**
     * Crée un personnage. [historiqueImporte] permet de reporter l'historique d'un fichier
     * importé (voir CharacterCreationScreen "Importer depuis un fichier") sur la copie créée.
     */
    fun addCharacter(character: Character, historiqueImporte: List<HistoriqueEntreeExport> = emptyList()) {
        val newList = _characters.value + character
        _characters.value = newList
        saveCharacters(newList)
        val entrees = historiqueImporte.map { entry ->
            val type = try { TypeEvenementHistorique.valueOf(entry.type) } catch (e: Exception) { TypeEvenementHistorique.AUTRE }
            EntreeAJournaliser(type, entry.description, entry.timestamp)
        } + EntreeAJournaliser(
            TypeEvenementHistorique.CREATION,
            if (historiqueImporte.isNotEmpty()) "Personnage créé (copié depuis un fichier)" else "Personnage créé"
        )
        logEtExporter(character, entrees)
    }

    private const val PERSONNAGES_SUBFOLDER = "Personnages"

    /** Chemin (pour affichage à l'utilisateur) du dossier où sont stockés les fichiers de personnages. */
    fun personnagesDirectoryPath(): String = PublicFilesStore.directoryLabel(PERSONNAGES_SUBFOLDER)

    fun characterFileName(character: Character): String {
        val shortId = character.id.take(8)
        return "${slugify(character.name)}-$shortId.json"
    }

    /**
     * Écrit (ou réécrit) le fichier JSON du personnage dans Téléchargements/JDRCompagnon/Personnages/,
     * avec tout son historique embarqué (voir [CharacterExport]), pour qu'il soit copiable/
     * partageable et réimportable via [characterFromJson] lors de la création d'un autre
     * personnage (bouton "Importer depuis un fichier") sans perdre le journal du personnage.
     */
    private suspend fun writeCharacterFile(context: Context, character: Character) {
        val historique = HistoriqueDependencies.repository.observerHistorique(character.id).first()
            .map { HistoriqueEntreeExport(it.timestamp, it.type, it.description) }
        val export = CharacterExport(character, historique)
        PublicFilesStore.writeText(
            context = context,
            displayName = characterFileName(character),
            content = json.encodeToString(export),
            subfolder = PERSONNAGES_SUBFOLDER,
            mimeType = "application/json"
        )
    }

    /**
     * Décode un personnage (et son historique) depuis un JSON exporté (fichier choisi via le
     * sélecteur), ou null si invalide. Reste compatible avec un ancien fichier ne contenant
     * qu'un Character brut (sans historique), écrit avant l'ajout de cette fonctionnalité.
     */
    fun characterFromJson(text: String): CharacterExport? = try {
        json.decodeFromString<CharacterExport>(text)
    } catch (e: Exception) {
        try {
            CharacterExport(json.decodeFromString<Character>(text), emptyList())
        } catch (e2: Exception) {
            android.util.Log.e("GameState", "JSON de personnage invalide", e2)
            null
        }
    }

    fun updateCharacter(updatedCharacter: Character) {
        applyCharacterUpdate(updatedCharacter.id) { updatedCharacter }
    }

    /**
     * Intègre un personnage reçu par le réseau (soit un push initial du MJ
     * vers le joueur, soit une réponse de synchronisation du joueur vers le
     * MJ) : remplace le personnage existant s'il a déjà cet id, sinon
     * l'ajoute à la liste locale.
     */
    fun upsertCharacterFromNetwork(character: Character) {
        val exists = _characters.value.any { it.id == character.id }
        val newList = if (exists) {
            _characters.value.map { if (it.id == character.id) character else it }
        } else {
            _characters.value + character
        }
        _characters.value = newList
        saveCharacters(newList)
    }

    fun removeCharacter(characterId: String) {
        val newList = _characters.value.filter { it.id != characterId }
        _characters.value = newList
        saveCharacters(newList)
        pruneDeletedGroupMembers()
        historiqueScope.launch { HistoriqueDependencies.repository.supprimerHistoriqueDePersonnage(characterId) }
    }

    /**
     * Retire des groupes les ids de fiches qui n'existent plus : sans ça, un personnage supprimé
     * restait compté dans [MjGroup.memberCount] (liste des groupes) alors qu'il n'apparaissait
     * plus dans le détail du groupe.
     */
    private fun pruneDeletedGroupMembers() {
        val existingIds = _characters.value.mapTo(HashSet()) { it.id }
        var changed = false
        val newGroups = _mjGroups.value.map { group ->
            val kept = group.memberIds.filter { it in existingIds }
            if (kept.size != group.memberIds.size) {
                changed = true
                group.copy(memberIds = kept)
            } else group
        }
        if (changed) {
            _mjGroups.value = newGroups
            saveMjGroups(newGroups)
        }
    }

    /** Recherche de personnages optionnellement filtrée par monde. */
    fun searchCharacters(query: String, worldId: String? = null, typeFilter: String? = null): List<Character> {
        return _characters.value.filter { character ->
            val matchesWorld = worldId == null || character.worldId == worldId
            val matchesQuery = query.isBlank() ||
                    character.name.contains(query, ignoreCase = true) ||
                    character.characterClass.contains(query, ignoreCase = true) ||
                    character.race.contains(query, ignoreCase = true)
            val matchesType = typeFilter == null || character.type == typeFilter
            matchesWorld && matchesQuery && matchesType
        }
    }

    /** Récupère un personnage par nom dans le monde courant (ou un monde donné). */
    fun getCharacterByName(name: String, worldId: String? = currentWorldId()): Character? {
        return _characters.value.find {
            (worldId == null || it.worldId == worldId) && it.name.equals(name, ignoreCase = true)
        }
    }

    /** Récupère un personnage Naheulbeuk par nom dans le monde courant. */
    fun getNaheulbeukCharacterByName(name: String): NaheulbeukCharacter? {
        return _naheulbeukCharacters.value.find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Ajoute un objet nommé au sac du personnage (bouton "Donner un objet").
     */
    fun addNamedItemToBackpack(characterId: String, itemName: String) {
        if (itemName.isBlank()) return
        val trimmed = itemName.trim()
        applyCharacterUpdate(characterId) { it.copy(backpackItems = it.backpackItems + trimmed) }
    }

    /** Ajoute un item au sac d'un personnage D&D/PNJ. */
    fun addItemToBackpack(characterId: String, itemName: String) {
        applyCharacterUpdate(characterId) { it.copy(backpackItems = it.backpackItems + itemName) }
    }

    /**
     * Déplace un item du sac vers l'équipement porté (legacy, sans slot).
     * Préférer equipInSlot pour le nouvel équipement slot-based.
     */
    fun equipItem(characterId: String, itemName: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val slot = ArmorRules.slotForItem(itemName) ?: run {
            // Aucun slot reconnu : ajoute simplement à equippedItems (mélange)
            val updated = target.copy(
                backpackItems = target.backpackItems - itemName,
                equippedItems = target.equippedItems + itemName
            )
            updateCharacter(updated)
            return
        }
        equipInSlot(characterId, itemName, slot)
    }

    /**
     * Déplace un item de l'équipement porté vers le sac (legacy, sans slot).
     * Préférer une déséquipement par slot pour le nouvel équipement slot-based.
     */
    fun unequipItem(characterId: String, itemName: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        if (itemName in target.equippedSlots.values) return unequipFromSlot(characterId, itemName)
        val updated = target.copy(
            backpackItems = target.backpackItems + itemName,
            equippedItems = target.equippedItems - itemName,
            equippedSlots = target.equippedSlots.filterValues { it != itemName }
        )
        updateCharacter(updated)
    }

    /**
     * Équipe un item dans un slot. Tout objet déjà présent dans ce(s) slot(s) en est
     * délogé et retourne au sac (une arme à 2 mains occupant les deux mains est
     * délogée des deux en une fois). Retourne true si accepté.
     */
    fun equipInSlot(characterId: String, itemName: String, slot: EquipmentSlot): Boolean {
        val target = _characters.value.find { it.id == characterId } ?: return false
        if (!target.backpackItems.contains(itemName)) return false
        // Emplacements typés (vêtements, sac à dos, bourses, accessoires/armes de dos) :
        // refuse un objet qui n'est pas du bon type, contrairement aux 6 emplacements
        // historiques restés permissifs (cf. ArmorRules.itemAllowedInSlot).
        if (!ArmorRules.itemAllowedInSlot(itemName, slot)) return false

        // Arme de jet consommable (javeline, fléchette) sur une arme de dos qui en porte
        // déjà : on empile au lieu de remplacer.
        if (slot in BACK_WEAPON_SLOTS && target.equippedSlots[slot] == itemName && ArmorRules.isStackableWeapon(itemName)) {
            updateCharacter(
                target.copy(
                    backpackItems = target.backpackItems - itemName,
                    equippedItems = target.equippedItems + itemName,
                    slotStackCounts = target.slotStackCounts + (slot to target.stackCount(slot) + 1)
                )
            )
            return true
        }

        // Arme à 2 mains tenue en main : occupe les deux mains. Portée dans le dos (arme de
        // dos), elle ne prend que l'emplacement visé.
        val hands = setOf(EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND)
        val targetSlots = if (slot in hands && ArmorRules.twoHanded(itemName)) {
            listOf(EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND)
        } else {
            listOf(slot)
        }

        // Objet(s) actuellement dans le(s) emplacement(s) visé(s) : ils retournent au sac.
        // distinct() ici ne dédoublonne qu'une arme à 2 mains qui occupe main + main
        // secondaire avec la MÊME entrée de map (un seul objet physique) — jamais le
        // reste du sac, qui peut légitimement contenir plusieurs objets de même nom
        // (pas d'ID unique par objet : deux "Dague" sont deux entrées de liste).
        val displaced = targetSlots.mapNotNull { target.equippedSlots[it] }.distinct()
        // Slots libérés : ceux visés, plus l'autre main quand on déloge une arme à 2 mains
        // (une seule entrée physique répartie sur les deux mains).
        val twoHandedDisplaced = targetSlots.any { it in hands } &&
            target.equippedSlots[EquipmentSlot.MAIN_HAND]?.let { name ->
                name == target.equippedSlots[EquipmentSlot.OFF_HAND] && ArmorRules.twoHanded(name)
            } == true
        val freedSlots = (targetSlots.filter { target.equippedSlots[it] != null } +
            (if (twoHandedDisplaced) hands else emptySet())).toSet()
        // Nombre d'exemplaires délogés par nom (une pile de javelines rend toutes ses unités).
        val displacedCopies = displaced.associateWith { name ->
            val slots = freedSlots.filter { target.equippedSlots[it] == name }
            if (twoHandedDisplaced && slots.toSet() == hands) 1 else slots.sumOf { target.stackCount(it) }
        }

        val newSlots = (target.equippedSlots - freedSlots.toSet()).toMutableMap()
        targetSlots.forEach { newSlots[it] = itemName }

        // Retire une seule occurrence de itemName et rajoute chaque objet déplacé un par
        // un (jamais `.distinct()` sur la liste entière : ça écraserait les piles
        // d'objets identiques déjà présentes dans le sac/l'équipement).
        var newBackpack = target.backpackItems - itemName
        var newEquipped = target.equippedItems + itemName
        displacedCopies.forEach { (name, copies) ->
            repeat(copies) {
                newBackpack = newBackpack + name
                newEquipped = newEquipped - name
            }
        }

        var updated = target.copy(
            backpackItems = newBackpack,
            equippedItems = newEquipped,
            equippedSlots = newSlots,
            slotStackCounts = target.slotStackCounts - freedSlots.toSet() - targetSlots.toSet()
        )
        // Le sac à dos délogé (remplacé par un autre) libère ses emplacements extérieurs :
        // leur contenu retourne dans le sac plutôt que de rester orphelin.
        if (EquipmentSlot.BACKPACK in targetSlots && displaced.isNotEmpty()) {
            updated = clearBackpackExterior(updated)
        }
        updateCharacter(updated)
        return true
    }

    /**
     * Vide les emplacements extérieurs du sac à dos (leur contenu retourne dans le sac
     * général) : appelé chaque fois que le sac à dos quitte EquipmentSlot.BACKPACK, pour ne
     * jamais laisser un objet sur un emplacement extérieur sans sac à dos équipé.
     */
    private fun clearBackpackExterior(character: Character): Character {
        if (character.backpackExteriorSlots.isEmpty()) return character
        return character.copy(
            backpackItems = character.backpackItems + character.backpackExteriorSlots.values,
            backpackExteriorSlots = emptyMap()
        )
    }

    /**
     * Plus de carquois sur l'emplacement BACK : ses flèches retournent dans le sac général
     * (appliqué à chaque mise à jour, pour ne jamais laisser de flèches orphelines).
     */
    private fun releaseQuiverIfUnequipped(character: Character): Character {
        if (character.quiverContents.isEmpty()) return character
        val back = character.equippedSlots[EquipmentSlot.BACK]
        if (back != null && ArmorRules.isQuiverItem(back)) return character
        return character.copy(
            backpackItems = character.backpackItems + character.quiverContents,
            quiverContents = emptyList()
        )
    }

    /** Un objet qui quitte l'inventaire perd sa propriété de focaliseur (cf. Character.focaliseurs). */
    private fun releaseLostFocus(character: Character): Character {
        if (character.focaliseurs.isEmpty()) return character
        val portes = ObjetsACharges.objetsPortes(character).map { it.lowercase() }.toSet()
        val gardes = character.focaliseurs.filterKeys { it.lowercase() in portes }
        return if (gardes.size == character.focaliseurs.size) character else character.copy(focaliseurs = gardes)
    }

    /**
     * Le grimoire ne contient plus de matériel : ce qui y avait été rangé (ancienne version)
     * retourne dans le sac général à la prochaine mise à jour de la fiche.
     */
    private fun releaseGrimoireContents(character: Character): Character {
        if (character.grimoireContents.isEmpty()) return character
        return character.copy(
            backpackItems = character.backpackItems + character.grimoireContents,
            grimoireContents = emptyList()
        )
    }

    /**
     * Un objet harmonisé qui n'est plus dans l'inventaire (jeté, consommé, détruit, donné à un
     * autre personnage) perd son harmonisation (appliqué à chaque mise à jour, cf. Harmonisation).
     */
    private fun releaseLostAttunements(character: Character): Character {
        if (character.attunedItems.isEmpty()) return character
        val portes = ObjetsACharges.objetsPortes(character).toSet()
        val gardes = character.attunedItems.filter { it in portes }
        return if (gardes.size == character.attunedItems.size) character else character.copy(attunedItems = gardes)
    }

    /** Harmonise le personnage avec [itemName] (règles vérifiées par Harmonisation.verifier). */
    fun harmoniser(characterId: String, itemName: String) {
        applyCharacterUpdate(characterId) {
            if (itemName in it.attunedItems) it else it.copy(attunedItems = it.attunedItems + itemName)
        }
    }

    /** Met fin à l'harmonisation avec [itemName] (repos court volontaire, mort, décision du MJ...). */
    fun rompreHarmonisation(characterId: String, itemName: String) {
        applyCharacterUpdate(characterId) { it.copy(attunedItems = it.attunedItems - itemName) }
    }

    /**
     * Range jusqu'à [count] exemplaires de [itemName] (flèches) du sac général dans le
     * carquois équipé, dans la limite de ArmorRules.QUIVER_CAPACITY. Retourne le nombre rangé.
     */
    fun putArrowsInQuiver(characterId: String, itemName: String, count: Int = Int.MAX_VALUE): Int {
        val target = _characters.value.find { it.id == characterId } ?: return 0
        val back = target.equippedSlots[EquipmentSlot.BACK]
        if (back == null || !ArmorRules.isQuiverItem(back) || !ArmorRules.isArrowItem(itemName)) return 0
        val place = ArmorRules.QUIVER_CAPACITY - target.quiverContents.size
        val n = minOf(count, place, target.backpackItems.count { it == itemName })
        if (n <= 0) return 0
        var newBackpack = target.backpackItems
        repeat(n) { newBackpack = newBackpack - itemName }
        updateCharacter(target.copy(backpackItems = newBackpack, quiverContents = target.quiverContents + List(n) { itemName }))
        return n
    }

    /** Sort [count] exemplaires de [itemName] du carquois vers le sac général. */
    fun takeArrowsFromQuiver(characterId: String, itemName: String, count: Int = Int.MAX_VALUE) {
        applyCharacterUpdate(characterId) { c ->
            val n = minOf(count, c.quiverContents.count { it == itemName })
            var contenu = c.quiverContents
            repeat(n) { contenu = contenu - itemName }
            c.copy(quiverContents = contenu, backpackItems = c.backpackItems + List(n) { itemName })
        }
    }

    /**
     * Range un objet (sac de couchage, corde...) sur un emplacement extérieur (1 à 3) du sac
     * à dos équipé. Refuse si aucun sac à dos n'est équipé, si l'objet n'est pas de ceux
     * autorisés en extérieur (cf. ArmorRules.isBackExteriorItem), ou s'il n'est pas dans le
     * sac général. Retourne true si accepté.
     */
    fun equipBackpackExteriorSlot(characterId: String, itemName: String, index: Int): Boolean {
        if (index !in 1..3) return false
        val target = _characters.value.find { it.id == characterId } ?: return false
        if (target.equippedSlots[EquipmentSlot.BACKPACK] == null) return false
        if (!ArmorRules.isBackExteriorItem(itemName)) return false
        if (!target.backpackItems.contains(itemName)) return false

        val displaced = target.backpackExteriorSlots[index]
        var newBackpack = target.backpackItems - itemName
        if (displaced != null) newBackpack = newBackpack + displaced
        val newExterior = target.backpackExteriorSlots + (index to itemName)

        updateCharacter(target.copy(backpackItems = newBackpack, backpackExteriorSlots = newExterior))
        return true
    }

    /**
     * Équipe automatiquement un sac à dos et une sacoche de ceinture à un PJ tout juste créé
     * (appelée une seule fois, à la fin de CharacterCreationScreen/MjCharacterCreationScreen),
     * pour qu'un personnage n'arrive jamais sans les deux contenants de base de la gestion
     * d'équipement. Si l'un de ces deux objets se trouve déjà dans le sac général (ex. fourni
     * par l'équipement de départ de la classe/l'historique), c'est CET exemplaire qui est
     * équipé plutôt que d'en créer un doublon.
     */
    fun equipStarterGear(characterId: String) {
        applyCharacterUpdate(characterId) { character ->
            var updated = character
            if (updated.equippedSlots[EquipmentSlot.BACKPACK] == null) {
                updated = grantStarterContainer(updated, EquipmentSlot.BACKPACK, "Sac à dos")
            }
            if (updated.equippedSlots[EquipmentSlot.BELT_POUCH_1] == null) {
                updated = grantStarterContainer(updated, EquipmentSlot.BELT_POUCH_1, "Sacoche")
            }
            updated
        }
    }

    private fun grantStarterContainer(character: Character, slot: EquipmentSlot, itemName: String): Character {
        val newBackpack = if (character.backpackItems.contains(itemName)) {
            character.backpackItems - itemName
        } else {
            character.backpackItems
        }
        return character.copy(
            backpackItems = newBackpack,
            equippedSlots = character.equippedSlots + (slot to itemName)
        )
    }

    /** Retire l'objet d'un emplacement extérieur (1 à 3) du sac à dos, qui retourne au sac général. */
    fun unequipBackpackExteriorSlot(characterId: String, index: Int) {
        applyCharacterUpdate(characterId) { character ->
            val item = character.backpackExteriorSlots[index] ?: return@applyCharacterUpdate character
            character.copy(
                backpackItems = character.backpackItems + item,
                backpackExteriorSlots = character.backpackExteriorSlots - index
            )
        }
    }

    /**
     * Ajoute un sort (choisi dans la bibliothèque SRD) à la liste des sorts connus d'un
     * personnage. Ne stocke que le nom : école/niveau/description restent dans le SRD,
     * relus via SrdRepository.getSpellByName au moment de l'affichage.
     *
     * [classe] retient la classe par laquelle le sort est appris (défaut : la classe
     * principale du personnage) — indispensable pour un personnage multiclassé, dont les
     * sorts connus/préparés sont déterminés par classe individuellement plutôt que dans
     * une liste combinée (règle SRD, § "Incantation" de progression.md ; cf. PisteIncantation
     * et le sélecteur de classe dans SpellsTab, CharacterSheetScreen.kt).
     */
    fun addSpellToCharacter(characterId: String, spellName: String, classe: String? = null, source: String? = null) {
        applyCharacterUpdate(characterId) {
            if (it.spells.contains(spellName)) return@applyCharacterUpdate it
            it.copy(
                spells = it.spells + spellName,
                spellClasses = it.spellClasses + (spellName to (classe ?: it.characterClass)),
                spellSources = if (source != null) it.spellSources + (spellName to source) else it.spellSources
            )
        }
    }

    /** Retire un sort de la liste des sorts connus d'un personnage. */
    fun removeSpellFromCharacter(characterId: String, spellName: String) {
        applyCharacterUpdate(characterId) {
            it.copy(spells = it.spells - spellName, spellClasses = it.spellClasses - spellName, spellSources = it.spellSources - spellName)
        }
    }

    /**
     * Supprime définitivement un item, qu'il soit dans le sac ou équipé (corbeille). Avec
     * [slot] sur une pile (javelines...), seul un exemplaire est jeté.
     */
    fun removeItemCompletely(characterId: String, itemName: String, slot: EquipmentSlot? = null) {
        val target = _characters.value.find { it.id == characterId } ?: return
        if (slot != null && target.equippedSlots[slot] == itemName && target.stackCount(slot) > 1) {
            updateCharacter(
                target.copy(
                    equippedItems = target.equippedItems - itemName,
                    slotStackCounts = target.slotStackCounts + (slot to target.stackCount(slot) - 1)
                )
            )
            return
        }
        var updated = target.copy(
            slotStackCounts = target.slotStackCounts.filterKeys { target.equippedSlots[it] != itemName },
            backpackItems = target.backpackItems - itemName,
            equippedItems = target.equippedItems - itemName,
            equippedSlots = target.equippedSlots.filterValues { it != itemName },
            itemCharges = target.itemCharges - itemName,
        )
        if (target.equippedSlots[EquipmentSlot.BACKPACK] == itemName) {
            updated = clearBackpackExterior(updated)
        }
        updateCharacter(updated)
    }

    /**
     * Déséquipe un item : de [slot] seulement si précisé (plus l'autre main pour une arme à
     * 2 mains), sinon de tous les slots où il est présent. Une pile (javelines...) retourne
     * entière dans le sac.
     */
    fun unequipFromSlot(characterId: String, itemName: String, slot: EquipmentSlot? = null) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val hands = setOf(EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND)
        val removedSlots = if (slot != null && target.equippedSlots[slot] == itemName) {
            if (slot in hands && ArmorRules.twoHanded(itemName)) hands.filter { target.equippedSlots[it] == itemName }
            else listOf(slot)
        } else {
            target.equippedSlots.filterValues { it == itemName }.keys.toList()
        }
        // Une arme à 2 mains sur les deux mains = un seul objet.
        val copies = if (removedSlots.toSet() == hands && ArmorRules.twoHanded(itemName)) 1
            else removedSlots.sumOf { target.stackCount(it) }.coerceAtLeast(1)
        val newSlots = target.equippedSlots - removedSlots.toSet()
        // Toujours réajouter au sac, même si un objet de même nom s'y trouve déjà : deux
        // objets identiques sont deux entrées distinctes de la liste (pas d'ID unique),
        // donc le "déjà présent -> ne rien ajouter" d'avant faisait disparaître l'objet
        // déséquipé dès qu'un autre du même nom traînait déjà dans le sac.
        var newEquipped = target.equippedItems
        var newBackpack = target.backpackItems
        repeat(copies) {
            newEquipped = newEquipped - itemName
            newBackpack = newBackpack + itemName
        }
        var updated = target.copy(
            backpackItems = newBackpack,
            equippedItems = newEquipped,
            equippedSlots = newSlots,
            slotStackCounts = target.slotStackCounts - removedSlots.toSet()
        )
        if (target.equippedSlots[EquipmentSlot.BACKPACK] == itemName) {
            updated = clearBackpackExterior(updated)
        }
        updateCharacter(updated)
    }

    /** Jette [count] exemplaires de [itemName] du sac général (sélection groupée). */
    fun removeFromBackpack(characterId: String, itemName: String, count: Int) {
        applyCharacterUpdate(characterId) { c ->
            var sac = c.backpackItems
            repeat(count) { sac = sac - itemName }
            c.copy(backpackItems = sac)
        }
    }

    fun getCharactersByType(type: String, worldId: String? = null): List<Character> {
        return _characters.value.filter {
            it.type == type && (worldId == null || it.worldId == worldId)
        }
    }

    /** Met à jour les PV actuels d'un personnage D&D. */
    fun updateCharacterHp(characterId: String, currentHitPoints: Int) {
        applyCharacterUpdate(characterId) {
            it.copy(currentHitPoints = currentHitPoints.coerceIn(0, it.maxHitPoints + it.temporaryHitPoints))
        }
    }

    /** Renomme un personnage D&D. */
    fun renameCharacter(characterId: String, newName: String) {
        if (newName.isBlank()) return
        applyCharacterUpdate(characterId) { it.copy(name = newName.trim()) }
    }

    /** Met à jour les notes d'un personnage D&D. */
    fun updateCharacterNotes(characterId: String, notes: String) {
        applyCharacterUpdate(characterId) { it.copy(notes = notes) }
    }

    /**
     * Confirme la montée de niveau (+1) d'un personnage D&D, depuis le dialogue "Monter
     * de niveau" de la fiche (cf. LevelUpDialog dans CharacterSheetScreen.kt) : bonus de
     * maîtrise recalculé, points de vie maximum (et actuels, homérègle 5e habituelle)
     * augmentés de [pvGagnes], aptitudes gagnées à ce niveau ajoutées à `classFeatures`
     * (texte libre, cf. Classe.aptitudes dans Srdcreationparsers.kt), sous-classe fixée
     * si elle ne l'est pas déjà (`type: choix-sousclasse`, un seul choix possible dans le
     * SRD actuel), et augmentation de caractéristiques le cas échéant (`type:
     * choix-generique`, ex. {"FOR": 2} ou {"FOR": 1, "DEX": 1}), plafonnée à 20.
     *
     * Ne touche jamais `experience` : contrairement à l'ancien comportement, le niveau
     * ne suit plus l'XP automatiquement, pour laisser le joueur confirmer explicitement
     * (et faire ses choix) via ce dialogue dès que l'XP accumulée le permet.
     *
     * [classeMulticlasseCible] : nom de la classe où ce niveau est pris quand ce n'est pas
     * la classe principale (`characterClass`) — incrémente/ajoute l'entrée correspondante
     * dans `classesSecondaires` au lieu du niveau de la classe principale. `null` (par
     * défaut) = niveau pris dans la classe principale, comportement inchangé. Dans ce cas
     * seulement, `sousClasse` peut mettre à jour le champ dédié `subclass` (un personnage
     * multiclassé garde la sous-classe de chaque classe secondaire dans `aptitudesTexte`,
     * `subclass` restant à un seul emplacement). [nouvelleCompetence] ajoute une compétence
     * maîtrisée si elle n'y est pas déjà (choix "au choix" propre au multiclassage, cf.
     * Classe.multiclassage.competence — Barde/Rôdeur/Roublard dans le SRD actuel).
     * [nouvellesExpertises] ajoute chaque compétence à `skillExpertise` (choix "Expertise"/
     * "Fin explorateur" — `type: choix-expertise-N`, cf. AptitudeClasse et LevelUpDialog).
     */
    fun appliquerMonteeDeNiveau(
        characterId: String,
        pvGagnes: Int,
        aptitudesTexte: String = "",
        sousClasse: String? = null,
        ameliorationCaracteristiques: Map<String, Int> = emptyMap(),
        classeMulticlasseCible: String? = null,
        nouvelleCompetence: String? = null,
        // Compétences choisies via une aptitude (ex. Maîtrises supplémentaires du Collège du Savoir).
        nouvellesCompetences: List<String> = emptyList(),
        nouvellesExpertises: List<String> = emptyList(),
        // Sorts appris grâce à un nouvel emplacement de sort préparé, et la classe qui les enseigne.
        nouveauxSorts: List<String> = emptyList(),
        classeNouveauxSorts: String? = null,
        // Origine de chaque nouveau sort (cf. Character.spellSources) ; à défaut, la classe et le niveau.
        sourcesNouveauxSorts: Map<String, String> = emptyMap(),
        // Nouveaux sorts aussitôt préparés (null = tous) : un Magicien ajoute 2 sorts à son grimoire
        // par niveau mais ne prépare que les emplacements de préparation gagnés.
        sortsAPreparer: List<String>? = null,
        // Sorts lançables sans emplacement choisis à ce niveau (cf. Character.sortsSpeciaux).
        nouveauxSortsSpeciaux: Map<String, String> = emptyMap(),
        // Don choisi à ce niveau (Faveur épique), ajouté aux dons de la fiche.
        donTexte: String = "",
        // Plafond des caractéristiques augmentées à ce niveau (30 pour une Faveur épique).
        plafondCaracteristiques: Int = 20,
    ) {
        applyCharacterUpdate(characterId) { perso ->
            val newLevel = (perso.level + 1).coerceAtMost(20)
            val newMaxHp = (perso.maxHitPoints + pvGagnes).coerceAtLeast(1)
            val estMulticlasse = classeMulticlasseCible != null &&
                !classeMulticlasseCible.equals(perso.characterClass, ignoreCase = true)
            val nouvellesClassesSecondaires = if (estMulticlasse) {
                val dejaPresente = perso.classesSecondaires.any { it.classe.equals(classeMulticlasseCible, ignoreCase = true) }
                if (dejaPresente) {
                    perso.classesSecondaires.map {
                        if (it.classe.equals(classeMulticlasseCible, ignoreCase = true)) it.copy(niveau = it.niveau + 1) else it
                    }
                } else {
                    perso.classesSecondaires + NiveauClasse(classeMulticlasseCible!!, 1)
                }
            } else perso.classesSecondaires
            // Compétence déjà maîtrisée et de nouveau accordée : elle passe en Expertise.
            val (maitrises, expertises) = cumulerMaitrisesCompetences(
                perso.skillProficiencies,
                (perso.skillExpertise + nouvellesExpertises).distinct(),
                listOfNotNull(nouvelleCompetence) + nouvellesCompetences,
            )
            var mis = perso.copy(
                level = newLevel,
                proficiencyBonus = calculateProficiencyBonus(newLevel),
                maxHitPoints = newMaxHp,
                currentHitPoints = (perso.currentHitPoints + pvGagnes).coerceIn(0, newMaxHp + perso.temporaryHitPoints),
                classFeatures = listOf(perso.classFeatures, aptitudesTexte)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n"),
                subclass = if (estMulticlasse) perso.subclass else perso.subclass.ifBlank { sousClasse.orEmpty() },
                classesSecondaires = nouvellesClassesSecondaires,
                skillProficiencies = maitrises,
                skillExpertise = expertises,
                spells = (perso.spells + nouveauxSorts).distinct(),
                spellClasses = perso.spellClasses + nouveauxSorts.associateWith { classeNouveauxSorts ?: perso.characterClass },
                spellSources = perso.spellSources + nouveauxSorts.associateWith { s ->
                    sourcesNouveauxSorts[s] ?: "${classeNouveauxSorts ?: perso.characterClass} (niveau $newLevel)"
                },
                // Un sort appris pour un emplacement de préparation est aussitôt préparé.
                preparedSpells = perso.preparedSpells?.let { (it + (sortsAPreparer ?: nouveauxSorts)).distinct() },
                sortsSpeciaux = perso.sortsSpeciaux + nouveauxSortsSpeciaux,
                feats = listOf(perso.feats, donTexte).filter { it.isNotBlank() }.joinToString("\n\n"),
            )
            ameliorationCaracteristiques.forEach { (abreviation, gain) ->
                val plafond = plafondCaracteristiques
                mis = when (abreviation) {
                    "FOR" -> mis.copy(strength = (mis.strength + gain).coerceAtMost(plafond))
                    "DEX" -> mis.copy(dexterity = (mis.dexterity + gain).coerceAtMost(plafond))
                    "CON" -> mis.copy(constitution = (mis.constitution + gain).coerceAtMost(plafond))
                    "INT" -> mis.copy(intelligence = (mis.intelligence + gain).coerceAtMost(plafond))
                    "SAG" -> mis.copy(wisdom = (mis.wisdom + gain).coerceAtMost(plafond))
                    "CHA" -> mis.copy(charisma = (mis.charisma + gain).coerceAtMost(plafond))
                    else -> mis
                }
            }
            mis
        }
    }

    /**
     * Gains d'espèce à la montée de niveau (balises de especes_srd521.md : sorts de lignage
     * niveaux 3/5, choix d'un trait de niveau supérieur...), appliqués en plus de
     * [appliquerMonteeDeNiveau]. [traitsTexte] est ajouté aux « Traits d'espèce » de la fiche.
     */
    fun appliquerGainsEspece(
        characterId: String,
        sorts: List<String>,
        resistances: List<String>,
        competences: List<String>,
        choix: Map<String, List<String>>,
        traitsTexte: String,
    ) {
        applyCharacterUpdate(characterId) { perso ->
            val (maitrises, expertises) = cumulerMaitrisesCompetences(perso.skillProficiencies, perso.skillExpertise, competences)
            perso.copy(
                spells = (perso.spells + sorts).distinct(),
                spellSources = perso.spellSources + sorts.filter { it !in perso.spells }.associateWith { "Espèce : ${perso.race}" },
                damageResistances = (perso.damageResistances + resistances).distinct(),
                skillProficiencies = maitrises,
                skillExpertise = expertises,
                speciesChoices = perso.speciesChoices + choix,
                traits = listOf(perso.traits, traitsTexte).filter { it.isNotBlank() }.joinToString("\n\n"),
            )
        }
    }

    /** Ajoute de l'XP à un personnage (sans changer son niveau : cf. [appliquerMonteeDeNiveau]). */
    fun addExperience(characterId: String, amount: Int) {
        applyCharacterUpdate(characterId) { it.copy(experience = (it.experience + amount).coerceAtLeast(0)) }
    }

    /** Ajoute (ou retire, si négatif) de l'or à la bourse d'un personnage. */
    fun addGold(characterId: String, amount: Int) {
        applyCharacterUpdate(characterId) { it.copy(gold = (it.gold + amount).coerceAtLeast(0)) }
    }

    /** Définit directement le montant d'or d'un personnage. */
    fun setGold(characterId: String, amount: Int) {
        applyCharacterUpdate(characterId) { it.copy(gold = amount.coerceAtLeast(0)) }
    }

    /** Définit le portrait (identifiant d'image locale) d'un personnage. */
    fun setCharacterPortrait(characterId: String, portrait: String) {
        applyCharacterUpdate(characterId) { it.copy(portrait = portrait) }
    }

    /** Active/désactive l'Inspiration Héroïque d'un personnage. */
    fun setHeroicInspiration(characterId: String, value: Boolean) {
        applyCharacterUpdate(characterId) { it.copy(heroicInspiration = value) }
    }

    /** Définit la condition/état courant d'un personnage (ex : "Empoisonné", "À terre"). */
    fun setCondition(characterId: String, value: String) {
        applyCharacterUpdate(characterId) { it.copy(condition = value) }
    }

    /**
     * Dépense un dé de vie (ex : lors d'un repos court). Le nombre de dés
     * disponibles est égal au niveau du personnage moins ceux déjà dépensés.
     */
    fun spendHitDie(characterId: String) {
        applyCharacterUpdate(characterId) { it.copy(hitDiceUsed = (it.hitDiceUsed + 1).coerceIn(0, it.level)) }
    }

    /**
     * Récupère des dés de vie (ex : lors d'un repos long). Par défaut en
     * récupère un seul ; passer un nombre plus élevé pour un repos long
     * (généralement la moitié du total, arrondi au supérieur).
     */
    fun recoverHitDice(characterId: String, amount: Int = 1) {
        applyCharacterUpdate(characterId) { it.copy(hitDiceUsed = (it.hitDiceUsed - amount).coerceIn(0, it.level)) }
    }

    /** Définit directement le nombre de dés de vie déjà dépensés (édition MJ). */
    fun setHitDiceUsed(characterId: String, used: Int) {
        applyCharacterUpdate(characterId) { it.copy(hitDiceUsed = used.coerceIn(0, it.level)) }
    }

    /**
     * Définit le nombre d'emplacements de sort dépensés pour un niveau de sort donné
     * (1er à 9e) — le maximum se recalcule depuis la classe/le niveau du personnage
     * (cf. EmplacementsDeSort dans CharacterSheetScreen.kt), seul le nombre utilisé est
     * persisté ici.
     */
    fun setSpellSlotUsed(characterId: String, niveauSort: Int, utilises: Int) {
        applyCharacterUpdate(characterId) {
            it.copy(spellSlotsUsed = it.spellSlotsUsed + (niveauSort to utilises.coerceAtLeast(0)))
        }
    }

    /** Ajoute ou retire [spellName] des sorts favoris (listés en premier au choix d'un sort). */
    fun toggleFavoriteSpell(characterId: String, spellName: String) {
        applyCharacterUpdate(characterId) {
            it.copy(favoriteSpells = if (spellName in it.favoriteSpells) it.favoriteSpells - spellName else it.favoriteSpells + spellName)
        }
    }

    /** Définit le nombre d'emplacements de Magie de pacte dépensés (Occultiste, pool séparé). */
    fun setPactSlotsUsed(characterId: String, utilises: Int) {
        applyCharacterUpdate(characterId) { it.copy(pactSlotsUsed = utilises.coerceAtLeast(0)) }
    }

    /**
     * Repos long : emplacements de sort (classiques ET Magie de pacte), Puissance curative
     * (Imposition des mains), lancement gratuit de Châtiment divin du Paladin et Restauration
     * magique du Magicien récupérés.
     */
    fun resetSpellSlots(characterId: String) {
        applyCharacterUpdate(characterId) {
            it.copy(
                spellSlotsUsed = emptyMap(), pactSlotsUsed = 0, layOnHandsUsed = 0, divineSmiteFreeUsed = false,
                arcaneRecoveryUsed = false, sortsPredilectionUtilises = emptyList(),
            )
        }
    }

    /** Sort de prédilection lancé gratuitement : plus disponible ainsi avant un repos court ou long. */
    fun utiliserSortPredilection(characterId: String, sort: String) {
        applyCharacterUpdate(characterId) { it.copy(sortsPredilectionUtilises = (it.sortsPredilectionUtilises + sort).distinct()) }
    }

    /** Repos court : les sorts de prédilection redeviennent lançables gratuitement. */
    fun recupererSortsPredilection(characterId: String) {
        applyCharacterUpdate(characterId) { it.copy(sortsPredilectionUtilises = emptyList()) }
    }

    /**
     * Magicien — Restauration magique (fin d'un repos court, 1/repos long) : rend [recuperes]
     * (niveau de sort → nombre d'emplacements) et marque l'aptitude comme utilisée. Le budget
     * (ArsenalPersonnage.budgetRestaurationMagique) est vérifié par l'appelant.
     */
    fun restaurationMagique(characterId: String, recuperes: Map<Int, Int>) {
        applyCharacterUpdate(characterId) { c ->
            c.copy(
                spellSlotsUsed = c.spellSlotsUsed + recuperes.mapValues { (niveau, n) -> ((c.spellSlotsUsed[niveau] ?: 0) - n).coerceAtLeast(0) },
                arcaneRecoveryUsed = true,
            )
        }
    }

    /** Points de Puissance curative (Imposition des mains) dépensés, bornés à la réserve [max]. */
    fun setLayOnHandsUsed(characterId: String, utilises: Int, max: Int) {
        applyCharacterUpdate(characterId) { it.copy(layOnHandsUsed = utilises.coerceIn(0, max)) }
    }

    /** Marque le lancement gratuit de Châtiment divin (Châtiment de paladin) comme utilisé ou non. */
    fun setDivineSmiteFreeUsed(characterId: String, utilise: Boolean) {
        applyCharacterUpdate(characterId) { it.copy(divineSmiteFreeUsed = utilise) }
    }

    /** Bottes d'arme choisies (écran Repos, à la fin d'un repos long). */
    fun setWeaponMasteries(characterId: String, armes: List<String>) {
        applyCharacterUpdate(characterId) { it.copy(weaponMasteries = armes.distinct()) }
    }

    /** Soigne [characterId] de [pv] (sans dépasser le maximum), ou retire l'état Empoisonné si [retirerPoison]. */
    fun soignerOuGuerirPoison(characterId: String, pv: Int, retirerPoison: Boolean) {
        applyCharacterUpdate(characterId) { c ->
            if (retirerPoison) {
                c.copy(condition = c.condition.split(",").map { it.trim() }
                    .filterNot { it.startsWith("Empoisonn", ignoreCase = true) || it.isBlank() }
                    .joinToString(", "))
            } else {
                c.copy(currentHitPoints = (c.currentHitPoints + pv).coerceAtMost(c.maxHitPoints).coerceAtLeast(c.currentHitPoints))
            }
        }
    }

    /** Profil d'IA de combat d'un PNJ (nom d'un ProfilIA), null = automatique. */
    fun setProfilIA(characterId: String, profil: String?) {
        applyCharacterUpdate(characterId) { it.copy(profilIA = profil) }
    }

    /** Sorts préparés (hors sorts mineurs), choisis dans l'écran de repos. */
    fun setPreparedSpells(characterId: String, sorts: List<String>) {
        applyCharacterUpdate(characterId) { it.copy(preparedSpells = sorts.distinct()) }
    }

    fun ajouterEffet(characterId: String, effet: EffetActif) {
        applyCharacterUpdate(characterId) { it.copy(effetsActifs = it.effetsActifs + effet) }
    }

    fun retirerEffet(characterId: String, effetId: String) {
        applyCharacterUpdate(characterId) { it.copy(effetsActifs = it.effetsActifs.filterNot { e -> e.id == effetId }) }
    }

    /** Retire les effets terminés à l'instant de fiction [maintenant] (secondes), sur toutes les fiches. */
    fun purgerEffetsExpires(maintenant: Long) {
        _characters.value
            .filter { c -> c.effetsActifs.any { it.finSecondes <= maintenant } }
            .forEach { c ->
                applyCharacterUpdate(c.id) { it.copy(effetsActifs = it.effetsActifs.filter { e -> e.finSecondes > maintenant }) }
            }
    }

    /**
     * Consomme UN exemplaire de [itemName] : de préférence celui équipé (emplacement de ceinture,
     * accessoire de dos...), sinon un de ceux portés, sinon du sac. Les autres exemplaires restent.
     */
    fun consommerObjet(characterId: String, itemName: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val slot = target.equippedSlots.entries.firstOrNull { it.value == itemName }?.key
        val exterieur = target.backpackExteriorSlots.entries.firstOrNull { it.value == itemName }?.key
        val updated = when {
            // Pile (ex. javelines dans le dos) : on n'en retire qu'une.
            slot != null && target.stackCount(slot) > 1 -> target.copy(
                equippedItems = target.equippedItems - itemName,
                slotStackCounts = target.slotStackCounts + (slot to target.stackCount(slot) - 1)
            )
            slot != null -> target.copy(
                equippedSlots = target.equippedSlots - slot,
                equippedItems = target.equippedItems - itemName,
                slotStackCounts = target.slotStackCounts - slot
            )
            itemName in target.equippedItems -> target.copy(equippedItems = target.equippedItems - itemName)
            exterieur != null -> target.copy(backpackExteriorSlots = target.backpackExteriorSlots - exterieur)
            itemName in target.quiverContents -> target.copy(quiverContents = target.quiverContents - itemName)
            itemName in target.backpackItems -> target.copy(backpackItems = target.backpackItems - itemName)
            else -> return
        }
        updateCharacter(updated)
    }

    /**
     * Tire une munition (flèche, carreau, bille...) de type [typeMunition] (cf.
     * ArmorRules.estMunitionDe) : d'abord dans le carquois, puis dans les emplacements
     * utilitaires (sacoche, étui...), enfin dans le sac. Retourne false s'il n'y en a plus :
     * l'attaque avec l'arme à munitions est alors impossible.
     */
    fun consommerMunition(characterId: String, typeMunition: String): Boolean {
        val target = _characters.value.find { it.id == characterId } ?: return false
        val estMunition = { nom: String -> ArmorRules.estMunitionDe(nom, typeMunition) }
        val dansCarquois = target.quiverContents.firstOrNull(estMunition)
        val slot = target.equippedSlots.entries.firstOrNull { (s, nom) -> s !in BACK_WEAPON_SLOTS && estMunition(nom) }?.key
        val dansSac = target.backpackItems.firstOrNull(estMunition)
        val updated = when {
            dansCarquois != null -> target.copy(quiverContents = target.quiverContents - dansCarquois)
            slot != null -> {
                val nom = target.equippedSlots.getValue(slot)
                if (target.stackCount(slot) > 1) target.copy(
                    equippedItems = target.equippedItems - nom,
                    slotStackCounts = target.slotStackCounts + (slot to target.stackCount(slot) - 1)
                ) else target.copy(
                    equippedSlots = target.equippedSlots - slot,
                    equippedItems = target.equippedItems - nom,
                    slotStackCounts = target.slotStackCounts - slot
                )
            }
            dansSac != null -> target.copy(backpackItems = target.backpackItems - dansSac)
            else -> return false
        }
        updateCharacter(updated)
        return true
    }

    /**
     * Arme de lancer (javeline, dague lancée...) : l'exemplaire lancé quitte l'emplacement
     * [slot] où il est porté (une unité de la pile pour des javelines dans le dos). Il n'est pas
     * rendu au sac : c'est au joueur de le ramasser après le combat (le MJ le rajoute au besoin).
     */
    fun lancerArme(characterId: String, itemName: String, slot: EquipmentSlot): Boolean {
        val target = _characters.value.find { it.id == characterId } ?: return false
        if (target.equippedSlots[slot] != itemName) return false
        val n = target.stackCount(slot)
        val hands = setOf(EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND)
        val liberes = when {
            n > 1 -> emptySet()
            slot in hands -> hands.filter { target.equippedSlots[it] == itemName }.toSet()
            else -> setOf(slot)
        }
        updateCharacter(
            target.copy(
                equippedItems = target.equippedItems - itemName,
                equippedSlots = target.equippedSlots - liberes,
                slotStackCounts = if (n > 1) target.slotStackCounts + (slot to n - 1) else target.slotStackCounts - slot
            )
        )
        return true
    }

    /**
     * Renomme les objets de l'inventaire d'après la bibliothèque (« Dagues » → « Dague », cf.
     * ArmorRules.nomCanonique) : un nom au pluriel issu de l'équipement de départ n'était pas
     * reconnu comme arme (pas d'attaque en combat). Sans effet si tout est déjà reconnu.
     */
    fun normaliserNomsObjets(characterId: String, nomsConnus: Collection<String>) {
        if (nomsConnus.isEmpty()) return
        val target = _characters.value.find { it.id == characterId } ?: return
        // « Focaliseur arcanique (bâton de combat) » : l'objet est le bâton (s'il existe dans la
        // bibliothèque), « focaliseur arcanique » devient sa propriété.
        val nouveauxFocaliseurs = mutableMapOf<String, String>()
        val canon = { nom: String ->
            val focaliseur = Focaliseurs.separer(nom)
            val objet = focaliseur?.let { (_, objet) -> ArmorRules.nomCanonique(objet, nomsConnus) }
                ?.takeIf { o -> nomsConnus.any { it.equals(o, ignoreCase = true) } }
            if (focaliseur != null && objet != null) {
                nouveauxFocaliseurs[objet] = focaliseur.first
                objet
            } else ArmorRules.nomCanonique(nom, nomsConnus)
        }
        val renomme = target.copy(
            backpackItems = target.backpackItems.map(canon),
            equippedItems = target.equippedItems.map(canon),
            equippedSlots = target.equippedSlots.mapValues { canon(it.value) },
            backpackExteriorSlots = target.backpackExteriorSlots.mapValues { canon(it.value) },
            quiverContents = target.quiverContents.map(canon),
            attunedItems = target.attunedItems.map(canon),
            itemCharges = target.itemCharges.mapKeys { canon(it.key) },
        )
        val updated = renomme.copy(focaliseurs = renomme.focaliseurs + nouveauxFocaliseurs)
        if (updated != target) updateCharacter(updated)
    }

    /** Rend au sac le matériel rangé dans le grimoire par l'ancienne version (cf. releaseGrimoireContents). */
    fun rendreContenuGrimoire(characterId: String) {
        applyCharacterUpdate(characterId) { it }
    }

    /**
     * Magicien : recopie [spellName] (niveau [niveau] ≥ 1) dans son grimoire — règle SRD : 2 h et
     * 50 po par niveau de sort, pour les encres et les essais. Le matériel est choisi par le
     * joueur dans son inventaire : [calligraphie] et [encre] servent sans être consommés,
     * [parchemins] (un exemplaire par niveau du sort, noms éventuellement répétés) sont consommés.
     * Retourne un message d'erreur, ou null si le sort a été ajouté.
     */
    fun recopierSortDansGrimoire(
        characterId: String,
        spellName: String,
        niveau: Int,
        calligraphie: String?,
        encre: String?,
        parchemins: List<String>,
    ): String? {
        val target = _characters.value.find { it.id == characterId } ?: return "Personnage introuvable."
        val inventaire = ObjetsACharges.objetsPortes(target)
        val cout = 50 * niveau.coerceAtLeast(1)
        val pages = niveau.coerceAtLeast(1)
        when {
            target.spells.any { it.equals(spellName, ignoreCase = true) } -> return "$spellName est déjà dans le grimoire."
            calligraphie == null || calligraphie !in inventaire -> return "Choisissez votre matériel de calligraphe."
            encre == null || encre !in inventaire -> return "Choisissez votre encre."
            parchemins.size < pages -> return "Choisissez $pages parchemin(s) (${parchemins.size} choisi(s))."
            target.gold < cout -> return "Il faut $cout po pour les encres rares (${target.gold} po disponibles)."
        }
        applyCharacterUpdate(characterId) {
            it.copy(
                gold = it.gold - cout,
                spells = it.spells + spellName,
                spellClasses = it.spellClasses + (spellName to "Magicien"),
                spellSources = it.spellSources + (spellName to "Recopié dans le grimoire")
            )
        }
        parchemins.take(pages).forEach { consommerObjet(characterId, it) }
        return null
    }

    /** Fixe les charges restantes d'un objet à charges (voir Character.itemCharges). */
    fun setItemCharges(characterId: String, itemName: String, charges: Int) {
        applyCharacterUpdate(characterId) {
            it.copy(itemCharges = it.itemCharges + (itemName to charges.coerceAtLeast(0)))
        }
    }

    /** Récupère uniquement la Magie de pacte (Occultiste) — repos court ou long. */
    fun resetPactSlots(characterId: String) {
        applyCharacterUpdate(characterId) { it.copy(pactSlotsUsed = 0) }
    }

    /** Nombre de jours qu'un personnage peut tenir sans nourriture : 3 + mod. Constitution (min 1). */
    fun foodToleranceDays(character: Character): Int =
        (3 + abilityModifier(character.constitution)).coerceAtLeast(1)

    /** Un objet de l'inventaire est-il consommable comme ration (nom contenant "ration") ? */
    fun hasRation(character: Character): Boolean =
        character.backpackItems.any { it.contains("ration", ignoreCase = true) }

    /**
     * Consomme une ration de l'inventaire (sac à dos) du personnage et marque qu'il a mangé
     * aujourd'hui (jour de fiction courant) : remet à zéro son compteur de jours sans
     * nourriture. Ne fait rien et retourne false si aucune ration n'est disponible.
     */
    fun eatRation(characterId: String): Boolean {
        val currentDay = ScenarioClockState.dayIndex(ScenarioClockState.state.value.scenarioMinutes)
        var consumed = false
        applyCharacterUpdate(characterId) { character ->
            val rationItem = character.backpackItems.firstOrNull { it.contains("ration", ignoreCase = true) }
            if (rationItem != null) {
                consumed = true
                character.copy(
                    backpackItems = character.backpackItems - rationItem,
                    lastMealDay = currentDay,
                    hungerCheckedDay = currentDay,
                )
            } else character
        }
        return consumed
    }

    /**
     * Rattrape l'épuisement dû à la faim en fonction du jour de fiction courant : chaque jour
     * au-delà de la tolérance ([foodToleranceDays]) et pas encore comptabilisé ajoute un niveau
     * d'épuisement (règle "Nourriture et eau" du SRD). À appeler avant d'afficher/utiliser
     * l'état de faim d'un personnage (ex. à l'ouverture de l'écran Repos).
     */
    fun syncHunger(characterId: String) {
        val currentDay = ScenarioClockState.dayIndex(ScenarioClockState.state.value.scenarioMinutes)
        applyCharacterUpdate(characterId) { character ->
            val tolerance = foodToleranceDays(character)
            val daysOverLimit = (currentDay - character.lastMealDay - tolerance).coerceAtLeast(0)
            val alreadyApplied = (character.hungerCheckedDay - character.lastMealDay - tolerance).coerceAtLeast(0)
            val newDays = (daysOverLimit - alreadyApplied).coerceAtLeast(0).toInt()
            if (newDays > 0) {
                character.copy(
                    exhaustionLevel = (character.exhaustionLevel + newDays).coerceIn(0, 6),
                    hungerCheckedDay = currentDay,
                )
            } else if (character.hungerCheckedDay != currentDay) {
                character.copy(hungerCheckedDay = currentDay)
            } else character
        }
    }

    /** Définit directement le niveau d'épuisement (0-6) d'un personnage (édition/ajustement MJ). */
    fun setExhaustionLevel(characterId: String, level: Int) {
        applyCharacterUpdate(characterId) { it.copy(exhaustionLevel = level.coerceIn(0, 6)) }
    }

    /** Définit directement les PV maximum et actuels d'un personnage (édition MJ). */
    fun setCharacterHp(characterId: String, currentHitPoints: Int, maxHitPoints: Int) {
        applyCharacterUpdate(characterId) {
            val newMax = maxHitPoints.coerceAtLeast(0)
            it.copy(maxHitPoints = newMax, currentHitPoints = currentHitPoints.coerceIn(0, newMax + it.temporaryHitPoints))
        }
    }

    /** Définit directement la vitesse de déplacement d'un personnage (édition MJ). */
    fun setCharacterSpeed(characterId: String, speed: Int) {
        applyCharacterUpdate(characterId) { it.copy(speed = speed.coerceAtLeast(0)) }
    }

    /** Définit directement la catégorie de taille d'un personnage (édition MJ). Vide = déduite de la race. */
    fun setCharacterSize(characterId: String, size: String) {
        applyCharacterUpdate(characterId) { it.copy(size = size.trim()) }
    }

    /**
     * Définit directement le montant d'XP d'un personnage (édition MJ). Ne change pas
     * son niveau : cf. [appliquerMonteeDeNiveau], déclenché depuis la fiche dès que
     * l'XP dépasse le seuil du niveau suivant.
     */
    fun setExperience(characterId: String, amount: Int) {
        applyCharacterUpdate(characterId) { it.copy(experience = amount.coerceAtLeast(0)) }
    }

    /** Définit directement la valeur d'une caractéristique (FOR/DEX/CON/INT/SAG/CHA) (édition MJ). */
    fun setAbilityScore(characterId: String, abilityLabel: String, value: Int) {
        val clamped = value.coerceIn(1, 30)
        applyCharacterUpdate(characterId) { character ->
            when (abilityLabel) {
                "FOR" -> character.copy(strength = clamped)
                "DEX" -> character.copy(dexterity = clamped)
                "CON" -> character.copy(constitution = clamped)
                "INT" -> character.copy(intelligence = clamped)
                "SAG" -> character.copy(wisdom = clamped)
                "CHA" -> character.copy(charisma = clamped)
                else -> character
            }
        }
    }

    /** Active/désactive la maîtrise d'un jet de sauvegarde (ex: "Force", "Dextérité") (édition MJ). */
    fun toggleSavingThrowProficiency(characterId: String, save: String) {
        applyCharacterUpdate(characterId) { character ->
            val newList = if (character.savingThrowProficiencies.contains(save)) {
                character.savingThrowProficiencies - save
            } else {
                character.savingThrowProficiencies + save
            }
            character.copy(savingThrowProficiencies = newList)
        }
    }

    /** Niveau de maîtrise actuel d'une compétence, déduit de skillProficiencies/skillExpertise. */
    fun skillProficiencyLevel(character: Character, skill: String): ProficiencyLevel = when {
        character.skillExpertise.contains(skill) -> ProficiencyLevel.EXPERTISE
        character.skillProficiencies.contains(skill) -> ProficiencyLevel.PROFICIENT
        else -> ProficiencyLevel.NONE
    }

    /**
     * Fait progresser (cycle) la maîtrise d'une compétence d'un personnage :
     * non maîtrisé -> maîtrisé -> expertise (bonus doublé) -> non maîtrisé.
     */
    fun cycleSkillProficiency(characterId: String, skill: String) {
        applyCharacterUpdate(characterId) { character ->
            when (skillProficiencyLevel(character, skill)) {
                ProficiencyLevel.NONE -> character.copy(
                    skillProficiencies = character.skillProficiencies + skill
                )
                ProficiencyLevel.PROFICIENT -> character.copy(
                    skillExpertise = character.skillExpertise + skill
                )
                ProficiencyLevel.EXPERTISE -> character.copy(
                    skillProficiencies = character.skillProficiencies - skill,
                    skillExpertise = character.skillExpertise - skill
                )
            }
        }
    }

    /**
     * Fait progresser (cycle) la maîtrise d'un jet de sauvegarde d'un personnage.
     */
    fun cycleSavingThrowProficiency(characterId: String, save: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val current = target.savingThrowProficiencies.toMutableList()
        val newProficiencies = if (current.contains(save)) {
            current - save
        } else {
            current + save
        }
        val updated = target.copy(savingThrowProficiencies = newProficiencies)
        updateCharacter(updated)
    }

    /**
     * Retourne le modificateur de caractéristique applicable à une compétence.
     */
    fun abilityModifierForSkill(skill: String, character: Character): Int {
        return when (com.jc2.jdrcompagnon.ui.screens.joueur.skillAbility[skill]) {
            "Force" -> abilityModifier(character.strength)
            "Dextérité" -> abilityModifier(character.dexterity)
            "Intelligence" -> abilityModifier(character.intelligence)
            "Sagesse" -> abilityModifier(character.wisdom)
            "Charisme" -> abilityModifier(character.charisma)
            else -> 0
        }
    }

    /**
     * Retourne le modificateur de caractéristique applicable à un jet de sauvegarde.
     */
    fun abilityModifierForSave(save: String, character: Character): Int {
        return when (save) {
            "Force" -> abilityModifier(character.strength)
            "Dextérité" -> abilityModifier(character.dexterity)
            "Constitution" -> abilityModifier(character.constitution)
            "Intelligence" -> abilityModifier(character.intelligence)
            "Sagesse" -> abilityModifier(character.wisdom)
            "Charisme" -> abilityModifier(character.charisma)
            else -> 0
        }
    }

    /**
     * Caractéristique d'incantation du personnage, déduite de sa classe
     * ([com.jc2.jdrcompagnon.ui.screens.joueur.spellcastingAbilityByClass]) — null
     * si sa classe n'est pas une classe de lanceur de sorts connue.
     */
    fun spellcastingAbility(character: Character): String? =
        com.jc2.jdrcompagnon.ui.screens.joueur.spellcastingAbilityByClass[character.characterClass]

    /** Modificateur de la caractéristique d'incantation du personnage. */
    fun spellcastingModifier(character: Character): Int? {
        val score = when (spellcastingAbility(character)) {
            "Force" -> character.strength
            "Dextérité" -> character.dexterity
            "Intelligence" -> character.intelligence
            "Sagesse" -> character.wisdom
            "Charisme" -> character.charisma
            else -> return null
        }
        return abilityModifier(score)
    }

    /** DD de sauvegarde contre les sorts du personnage : 8 + maîtrise + modificateur d'incantation. */
    fun spellSaveDC(character: Character): Int? =
        spellcastingModifier(character)?.let { 8 + character.proficiencyBonus + it }

    /** Bonus d'attaque avec un sort du personnage : maîtrise + modificateur d'incantation. */
    fun spellAttackBonus(character: Character): Int? =
        spellcastingModifier(character)?.let { character.proficiencyBonus + it }

    fun addNaheulbeukCharacter(character: NaheulbeukCharacter) {
        _naheulbeukCharacters.value = _naheulbeukCharacters.value + character
        saveNaheulbeukCharacters(_naheulbeukCharacters.value)
    }

    fun updateNaheulbeukCharacter(updated: NaheulbeukCharacter) {
        _naheulbeukCharacters.value = _naheulbeukCharacters.value.map { if (it.id == updated.id) updated else it }
        saveNaheulbeukCharacters(_naheulbeukCharacters.value)
    }

    fun removeNaheulbeukCharacter(id: String) {
        _naheulbeukCharacters.value = _naheulbeukCharacters.value.filter { it.id != id }
        saveNaheulbeukCharacters(_naheulbeukCharacters.value)
    }

    /** Ajoute un item au sac d'un personnage Naheulbeuk. */
    fun addItemToNaheulbeukBackpack(characterId: String, itemName: String) {
        val updated = _naheulbeukCharacters.value.map { character ->
            if (character.id == characterId) {
                character.copy(backpackItems = character.backpackItems + itemName)
            } else character
        }
        _naheulbeukCharacters.value = updated
        saveNaheulbeukCharacters(updated)
    }

    /** Déplace un item du sac vers l'équipement porté (Naheulbeuk). */
    fun equipNaheulbeukItem(characterId: String, itemName: String) {
        val updated = _naheulbeukCharacters.value.map { character ->
            if (character.id == characterId) {
                val newBackpack = character.backpackItems.filterNot { it == itemName }
                val newEquipped = character.equippedItems + itemName
                character.copy(backpackItems = newBackpack, equippedItems = newEquipped)
            } else character
        }
        _naheulbeukCharacters.value = updated
        saveNaheulbeukCharacters(updated)
    }

    /** Déplace un item de l'équipement porté vers le sac (Naheulbeuk). */
    fun unequipNaheulbeukItem(characterId: String, itemName: String) {
        val updated = _naheulbeukCharacters.value.map { character ->
            if (character.id == characterId) {
                val newEquipped = character.equippedItems.filterNot { it == itemName }
                val newBackpack = character.backpackItems + itemName
                character.copy(backpackItems = newBackpack, equippedItems = newEquipped)
            } else character
        }
        _naheulbeukCharacters.value = updated
        saveNaheulbeukCharacters(updated)
    }

    // === AJOUTS POUR LA SUPPRESSION ===

    fun removeMjGroup(groupId: String) {
        // Les cavaliers des montures du groupe retrouvent leur vitesse à pied.
        _mjGroups.value.firstOrNull { it.id == groupId }?.mounts?.forEach { mount ->
            val cavalier = mount.riderCharacterId ?: return@forEach
            mount.riderBaseSpeed?.let { vitesse -> applyCharacterUpdate(cavalier) { it.copy(speed = vitesse) } }
        }
        val newList = _mjGroups.value.filter { it.id != groupId }
        _mjGroups.value = newList
        saveMjGroups(newList)
    }

    fun removeMjScenario(scenarioId: String, context: Context? = null) {
        val scenario = _mjScenarios.value.firstOrNull { it.id == scenarioId }
        val newList = _mjScenarios.value.filter { it.id != scenarioId }
        _mjScenarios.value = newList
        saveMjScenarios(newList)
        // Supprime aussi le fichier .md : sinon il reste orphelin sur le disque et
        // syncScenariosFromDisk le réimporterait comme un nouveau scénario au prochain scan.
        if (context != null && scenario != null) {
            deleteScenarioFile(context, scenario)
        }
    }

    /** Supprime tous les fichiers .md portant l'id de ce scénario, et ses images importées. */
    private fun deleteScenarioFile(context: Context, scenario: MjScenario) {
        listScenarioFiles(context)
            .filter { entry ->
                val content = runCatching {
                    context.contentResolver.openInputStream(entry.uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                content?.let { hiddenIdRegex.find(it)?.groupValues?.get(1) } == scenario.id
            }
            .forEach { entry -> supprimerOuIgnorerFichier(context, entry) }
        com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImageStore.supprimerImagesDe(context, scenario.id)
    }

    /**
     * Sépare les scénarios en (à garder, copies identiques) : même monde, même titre et mêmes
     * scènes (titres et textes, les noms de fichiers d'images importées — préfixés par l'id du
     * scénario — étant ramenés à leur nom d'origine). L'ordre de la liste est conservé.
     */
    internal data class TriCopies(
        val gardes: List<MjScenario>,
        val copies: List<MjScenario>,
        val versGarde: Map<String, String>, // id d'une copie -> id du scénario gardé
    )

    internal fun scenariosSansCopies(scenarios: List<MjScenario>): TriCopies {
        val imagePrefixee = Regex("""\{image:[0-9a-fA-F-]{36}_""")
        val gardeParSignature = mutableMapOf<Any, String>()
        val gardes = mutableListOf<MjScenario>()
        val copies = mutableListOf<MjScenario>()
        val versGarde = mutableMapOf<String, String>()
        scenarios.forEach { s ->
            val signature = Triple(
                s.worldId,
                s.title.trim(),
                s.scenes.map { it.title.trim() to it.markdownContent.replace(imagePrefixee, "{image:").trim() }
            )
            val garde = gardeParSignature[signature]
            if (garde == null) {
                gardeParSignature[signature] = s.id
                gardes += s
            } else {
                copies += s
                versGarde[s.id] = garde
            }
        }
        return TriCopies(gardes, copies, versGarde)
    }

    private const val KEY_FICHIERS_SCENARIO_IGNORES = "scenario_files_ignored"

    /**
     * Supprime un fichier .md du dossier Scénarios ; s'il ne peut pas l'être (fichier déposé par
     * un autre moyen que l'app, qu'Android ne la laisse pas effacer), il est mémorisé pour que
     * [syncScenariosFromDisk] ne le réimporte plus — c'est ce qui dupliquait un scénario à chaque
     * retour sur la liste.
     */
    private fun supprimerOuIgnorerFichier(context: Context, entry: ScenarioFileEntry) {
        val supprime = runCatching { context.contentResolver.delete(entry.uri, null, null) > 0 }.getOrDefault(false)
        if (!supprime) {
            val ignores = prefs?.getStringSet(KEY_FICHIERS_SCENARIO_IGNORES, emptySet()).orEmpty()
            prefs?.edit()?.putStringSet(KEY_FICHIERS_SCENARIO_IGNORES, ignores + entry.uri.toString())?.apply()
        }
    }

    private fun fichierScenarioIgnore(entry: ScenarioFileEntry): Boolean =
        entry.uri.toString() in prefs?.getStringSet(KEY_FICHIERS_SCENARIO_IGNORES, emptySet()).orEmpty()

    fun addMjGroup(group: MjGroup) {
        _mjGroups.value = _mjGroups.value + group
        saveMjGroups(_mjGroups.value)
    }

    fun updateMjGroup(updatedGroup: MjGroup) {
        _mjGroups.value = _mjGroups.value.map { if (it.id == updatedGroup.id) updatedGroup else it }
        saveMjGroups(_mjGroups.value)
    }

    /**
     * Range [objet] du sac du personnage [characterId] dans les sacoches de la monture [mountId]
     * du groupe [groupId]. Sans effet si le personnage n'a pas l'objet dans son sac.
     */
    fun rangerSurMonture(groupId: String, mountId: String, characterId: String, objet: String) {
        val groupe = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        if (groupe.mounts.none { it.id == mountId }) return
        val perso = _characters.value.firstOrNull { it.id == characterId } ?: return
        if (objet !in perso.backpackItems) return
        removeFromBackpack(characterId, objet, 1)
        updateMjGroup(groupe.copy(mounts = groupe.mounts.map { if (it.id == mountId) it.copy(bagages = it.bagages + objet) else it }))
    }

    /** Reprend [objet] des sacoches de la monture [mountId] et le remet dans le sac du personnage [characterId]. */
    fun reprendreDeMonture(groupId: String, mountId: String, characterId: String, objet: String) {
        val groupe = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        val monture = groupe.mounts.firstOrNull { it.id == mountId } ?: return
        if (objet !in monture.bagages || _characters.value.none { it.id == characterId }) return
        updateMjGroup(groupe.copy(mounts = groupe.mounts.map { if (it.id == mountId) it.copy(bagages = it.bagages - objet) else it }))
        addItemToBackpack(characterId, objet)
    }

    fun addMjScenario(scenario: MjScenario, context: Context? = null) {
        _mjScenarios.value = _mjScenarios.value + scenario
        saveMjScenarios(_mjScenarios.value)
        context?.let { writeScenarioFile(it, scenario) }
    }

    fun updateMjScenario(updatedScenario: MjScenario, context: Context? = null) {
        _mjScenarios.value = _mjScenarios.value.map { if (it.id == updatedScenario.id) updatedScenario else it }
        saveMjScenarios(_mjScenarios.value)
        context?.let { writeScenarioFile(it, updatedScenario) }
    }

    /**
     * Marque une scène comme validée/non-validée (suivi de progression MJ en cours de partie),
     * appelée directement depuis le lecteur de scénario (ScenarioReaderContent).
     */
    fun setSceneValidated(scenarioId: String, sceneId: String, validated: Boolean, context: Context? = null) {
        val scenario = _mjScenarios.value.firstOrNull { it.id == scenarioId } ?: return
        val updatedScenes = scenario.scenes.map { if (it.id == sceneId) it.copy(validated = validated) else it }
        if (updatedScenes == scenario.scenes) return
        updateMjScenario(scenario.copy(scenes = updatedScenes), context)
    }

    fun addMjCampaign(campaign: MjCampaign) {
        _mjCampaigns.value = _mjCampaigns.value + campaign
        saveMjCampaigns(_mjCampaigns.value)
    }

    fun updateMjCampaign(updated: MjCampaign) {
        _mjCampaigns.value = _mjCampaigns.value.map { if (it.id == updated.id) updated else it }
        saveMjCampaigns(_mjCampaigns.value)
    }

    /**
     * Remet la progression d'une campagne à zéro pour la rejouer : quêtes de nouveau en cours (et
     * récompensables), scènes de ses scénarios non validées. Le contenu préparé par le MJ
     * (quêtes, récompenses, scénarios, PNJ, cartes...) est conservé.
     */
    fun resetMjCampaignProgress(campaignId: String, context: Context? = null) {
        val campaign = _mjCampaigns.value.firstOrNull { it.id == campaignId } ?: return
        updateMjCampaign(
            campaign.copy(
                quests = campaign.quests.map {
                    it.copy(
                        status = com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_COURS,
                        rewardedGroupId = null,
                        rewardSummary = "",
                    )
                }
            )
        )
        _mjScenarios.value.filter { it.id in campaign.scenarioIds && it.scenes.any { s -> s.validated } }
            .forEach { scenario ->
                updateMjScenario(scenario.copy(scenes = scenario.scenes.map { it.copy(validated = false) }), context)
            }
    }

    fun removeMjCampaign(id: String) {
        _mjCampaigns.value = _mjCampaigns.value.filter { it.id != id }
        saveMjCampaigns(_mjCampaigns.value)
    }

    /**
     * Ancienne fiche de suivi (objectifs cochables) convertie en quêtes : un objectif coché devient
     * une quête terminée (sans récompense à distribuer), les autres des quêtes en cours.
     */
    private fun migrerFicheDeSuivi(campaign: MjCampaign): MjCampaign {
        if (campaign.checklistItems.isEmpty()) return campaign
        val quetes = campaign.checklistItems.filter { it.label.isNotBlank() }.map { item ->
            Quest(
                id = item.id,
                title = item.label,
                status = if (item.checked) com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.TERMINEE
                else com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_COURS,
            )
        }
        return campaign.copy(quests = campaign.quests + quetes, checklistItems = emptyList())
    }

    /** Ajoute ou remplace une quête d'une campagne (enregistrement immédiat). */
    fun upsertQuest(campaignId: String, quest: Quest) {
        val campaign = _mjCampaigns.value.firstOrNull { it.id == campaignId } ?: return
        val quests = if (campaign.quests.any { it.id == quest.id }) {
            campaign.quests.map { if (it.id == quest.id) quest else it }
        } else {
            campaign.quests + quest
        }
        updateMjCampaign(campaign.copy(quests = quests))
    }

    fun removeQuest(campaignId: String, questId: String) {
        val campaign = _mjCampaigns.value.firstOrNull { it.id == campaignId } ?: return
        updateMjCampaign(campaign.copy(quests = campaign.quests.filterNot { it.id == questId }))
    }

    /** PNJ/créature rattaché à une campagne (ou retiré) : voir MjCampaign.pnjIds et PorteeCampagne. */
    fun setCampaignPnj(campaignId: String, characterId: String, attached: Boolean) {
        val campaign = _mjCampaigns.value.firstOrNull { it.id == campaignId } ?: return
        val pnjIds = if (attached) (campaign.pnjIds + characterId).distinct() else campaign.pnjIds - characterId
        updateMjCampaign(campaign.copy(pnjIds = pnjIds))
    }

    /** Lieu où se trouve un personnage (PJ, PNJ, créature). */
    fun setCharacterLocation(characterId: String, location: String) {
        applyCharacterUpdate(characterId) { it.copy(location = location.trim()) }
    }

    /**
     * Met en selle [riderId] sur la monture [mountId] du groupe (null = pied à terre). Le cavalier
     * prend la vitesse de la monture ; l'ancien cavalier retrouve sa vitesse d'origine. Un
     * personnage ne monte qu'une monture à la fois, et une monture attelée est dételée.
     */
    fun assignMountRider(groupId: String, mountId: String, riderId: String?) {
        val group = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        val mount = group.mounts.firstOrNull { it.id == mountId } ?: return
        if (mount.riderCharacterId == riderId) return
        // Un animal a un maître mais ne se monte pas : aucun effet sur la vitesse.
        val monture = mount.kind == com.jc2.jdrcompagnon.feature_group.domain.model.MountKind.MONTURE
        var mounts = group.mounts
        // Le nouveau cavalier descend d'abord de son éventuelle autre monture.
        if (riderId != null && monture) {
            mounts.filter { it.riderCharacterId == riderId && it.id != mountId && it.kind == mount.kind }.forEach { autre ->
                autre.riderBaseSpeed?.let { vitesse -> applyCharacterUpdate(riderId) { it.copy(speed = vitesse) } }
                mounts = mounts.map { if (it.id == autre.id) it.copy(riderCharacterId = null, riderBaseSpeed = null) else it }
            }
        }
        // L'ancien cavalier retrouve sa vitesse.
        mount.riderCharacterId?.let { ancien ->
            mount.riderBaseSpeed?.let { vitesse -> applyCharacterUpdate(ancien) { it.copy(speed = vitesse) } }
        }
        val baseSpeed = if (monture) riderId?.let { id -> _characters.value.firstOrNull { it.id == id }?.speed } else null
        if (riderId != null && baseSpeed != null) {
            applyCharacterUpdate(riderId) { it.copy(speed = mount.speed) }
        }
        mounts = mounts.map {
            if (it.id == mountId) it.copy(
                riderCharacterId = riderId,
                riderBaseSpeed = baseSpeed,
                transportId = if (riderId != null) null else it.transportId,
            )
            else it
        }
        updateMjGroup(group.copy(mounts = mounts))
    }

    /** Attelle la monture [mountId] au véhicule [transportId] (null = dételer) ; son cavalier descend. */
    fun harnessMount(groupId: String, mountId: String, transportId: String?) {
        if (transportId != null) assignMountRider(groupId, mountId, null)
        val group = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        updateMjGroup(group.copy(mounts = group.mounts.map { if (it.id == mountId) it.copy(transportId = transportId) else it }))
    }

    /** Retire une monture du groupe (son cavalier retrouve sa vitesse). */
    fun removeMount(groupId: String, mountId: String) {
        assignMountRider(groupId, mountId, null)
        val group = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        updateMjGroup(group.copy(mounts = group.mounts.filterNot { it.id == mountId }))
    }

    /** Modifie une monture du groupe ; si sa vitesse change, celle de son cavalier suit. */
    fun updateMount(groupId: String, mount: com.jc2.jdrcompagnon.feature_group.domain.model.Mount) {
        val avant = _mjGroups.value.firstOrNull { it.id == groupId }?.mounts?.firstOrNull { it.id == mount.id } ?: return
        // Changement monture <-> animal : on repart d'une monture sans cavalier.
        if (avant.kind != mount.kind && avant.riderCharacterId != null) assignMountRider(groupId, mount.id, null)
        val group = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        val ancienne = group.mounts.firstOrNull { it.id == mount.id } ?: return
        var corrigee = mount.copy(riderCharacterId = ancienne.riderCharacterId, riderBaseSpeed = ancienne.riderBaseSpeed)
        if (corrigee.kind == com.jc2.jdrcompagnon.feature_group.domain.model.MountKind.ANIMAL) corrigee = corrigee.copy(transportId = null)
        if (ancienne.riderCharacterId != null && ancienne.riderBaseSpeed != null && ancienne.speed != corrigee.speed) {
            applyCharacterUpdate(ancienne.riderCharacterId) { it.copy(speed = corrigee.speed) }
        }
        updateMjGroup(group.copy(mounts = group.mounts.map { if (it.id == mount.id) corrigee else it }))
    }

    /** Retire un véhicule du groupe (ses montures sont dételées). */
    fun removeTransport(groupId: String, transportId: String) {
        val group = _mjGroups.value.firstOrNull { it.id == groupId } ?: return
        updateMjGroup(
            group.copy(
                transports = group.transports.filterNot { it.id == transportId },
                mounts = group.mounts.map { if (it.transportId == transportId) it.copy(transportId = null) else it },
            )
        )
    }

    fun getCampaignById(id: String): MjCampaign? {
        return _mjCampaigns.value.find { it.id == id }
    }

    private fun saveMjCampaigns(list: List<MjCampaign>) {
        prefs?.edit()?.apply {
            try {
                putString(KEY_MJ_CAMPAIGNS, json.encodeToString(list))
                apply()
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Error saving MJ campaigns", e)
            }
        }
    }

    private fun loadMjCampaigns() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_MJ_CAMPAIGNS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val list = json.decodeFromString<List<MjCampaign>>(jsonString)
                    val migrees = list.map { migrerFicheDeSuivi(it) }
                    _mjCampaigns.value = migrees
                    if (migrees != list) saveMjCampaigns(migrees)
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading MJ campaigns", e)
                }
            }
        }
    }

    private fun lastScenarioKey(worldId: String): String = "$KEY_MJ_LAST_SCENARIO_PREFIX$worldId"

    private var lastLoadedWorldId: String? = null

    private fun loadLastScenarioId() {
        // Charge immédiatement pour le monde courant (déjà chargé par loadCurrentWorld() à ce
        // stade de init()) ; ensureLastScenarioLoadedForWorld reste appelée en plus à chaque
        // changement de monde en cours de session (voir setCurrentWorld).
        ensureLastScenarioLoadedForWorld(currentWorldId() ?: "")
    }

    private fun ensureLastScenarioLoadedForWorld(worldId: String) {
        if (lastLoadedWorldId == worldId) return
        lastLoadedWorldId = worldId
        _lastScenarioId.value = prefs?.getString(lastScenarioKey(worldId), null)
    }

    private fun saveLastScenarioIdForWorld(worldId: String, id: String?) {
        prefs?.edit()?.apply {
            if (id == null) {
                remove(lastScenarioKey(worldId))
            } else {
                putString(lastScenarioKey(worldId), id)
            }
            apply()
        }
    }
    private const val KEY_CURRENT_CAMPAIGN_ID_PREFIX = "current_campaign_id_"
    private fun currentCampaignKey(worldId: String?): String =
        if (worldId.isNullOrBlank()) "current_campaign_id" else "$KEY_CURRENT_CAMPAIGN_ID_PREFIX$worldId"

    fun setCurrentCampaignId(campaignId: String?) {
        _currentCampaignId.value = campaignId
        prefs?.edit()?.apply {
            if (campaignId == null) remove(currentCampaignKey(currentWorldId()))
            else putString(currentCampaignKey(currentWorldId()), campaignId)
            apply()
        }
    }

    private fun loadCurrentCampaignId() {
        _currentCampaignId.value = prefs?.getString(currentCampaignKey(currentWorldId()), null)
    }

    private const val KEY_CURRENT_GROUP_ID_PREFIX = "current_group_id_"
    private fun currentGroupKey(worldId: String?): String =
        if (worldId.isNullOrBlank()) "current_group_id" else "$KEY_CURRENT_GROUP_ID_PREFIX$worldId"

    fun setCurrentGroupId(groupId: String?) {
        _currentGroupId.value = groupId
        prefs?.edit()?.apply {
            if (groupId == null) remove(currentGroupKey(currentWorldId()))
            else putString(currentGroupKey(currentWorldId()), groupId)
            apply()
        }
    }

    private fun loadCurrentGroupId() {
        _currentGroupId.value = prefs?.getString(currentGroupKey(currentWorldId()), null)
    }

    /** Groupe (du monde courant) auquel appartient le personnage donné, s'il y en a un. */
    fun groupForCharacter(characterId: String?, worldId: String?): MjGroup? {
        if (characterId == null) return null
        return groupsForWorld(worldId).firstOrNull { characterId in it.memberIds }
    }

    /**
     * Attitude d'un PNJ envers un personnage, déduite de sa réputation envers le(s) groupe(s)
     * de ce personnage (Character.groupReputations) : un score réglé pour un groupe vaut
     * automatiquement pour tous ses membres. Null si le PNJ n'a d'avis sur aucun de ses groupes.
     */
    fun pnjReputationToward(pnj: Character, characterId: String): Int? =
        groupsForWorld(pnj.worldId)
            .filter { characterId in it.memberIds }
            .mapNotNull { pnj.groupReputations[it.id] }
            .takeIf { it.isNotEmpty() }
            ?.let { scores -> scores.sum() / scores.size }

    /**
     * Règle le dernier scénario pour le monde courant.
     */
    fun setLastScenarioId(id: String?) {
        val worldId = _currentWorld.value?.id ?: return
        _lastScenarioId.value = id
        saveLastScenarioIdForWorld(worldId, id)
    }

    // Accès aux scénarios/groupes filtrés par monde courant
    fun scenariosForWorld(worldId: String?): List<MjScenario> {
        return if (worldId == null) emptyList() else _mjScenarios.value.filter { it.worldId == worldId }
    }

    fun groupsForWorld(worldId: String?): List<MjGroup> {
        return if (worldId == null) emptyList() else _mjGroups.value.filter { it.worldId == worldId }
    }

    fun currentWorldId(): String? = _currentWorld.value?.id

    /**
     * Récupère le scénario actuellement sélectionné pour un monde, avec fallback.
     */
    fun currentScenarioForWorld(worldId: String?): MjScenario? {
        ensureLastScenarioLoadedForWorld(worldId ?: "")
        val scenarios = scenariosForWorld(worldId)
        if (scenarios.isEmpty()) return null
        val lastId = _lastScenarioId.value
        return scenarios.find { it.id == lastId } ?: scenarios.firstOrNull()
    }

    /** Définit le monde courant et recharge le dernier scénario associé. */
    fun setCurrentWorld(world: WorldState?) {
        _currentWorld.value = world
        saveCurrentWorld(world)
        loadCurrentCampaignId()
        loadCurrentGroupId()
        ensureLastScenarioLoadedForWorld(world?.id ?: "")
    }

    // === ÉTAT DU DÉ ===

    private val _diceState = MutableStateFlow(DiceState())
    val diceState: StateFlow<DiceState> = _diceState.asStateFlow()

    fun rollDice(sides: Int = 20): Int {
        val state = _diceState.value
        val (result, actualSides) = when {
            sides == 20 && state.advantageState != AdvantageState.NORMAL -> {
                val roll1 = (1..sides).random()
                val roll2 = (1..sides).random()
                val chosen = if (state.advantageState == AdvantageState.ADVANTAGE) maxOf(roll1, roll2) else minOf(roll1, roll2)
                chosen to sides
            }
            else -> (1..sides).random() to sides
        }
        val newState = state.copy(
            lastResult = result,
            lastSides = actualSides,
            showResult = true,
            isRolling = false,
        )
        _diceState.value = newState
        saveDiceState(newState)
        return result
    }

    // Lance le pool de dés complet
    fun rollDicePool(): List<DiceRollResult> {
        val state = _diceState.value
        val results = mutableListOf<DiceRollResult>()

        // Avantage/Désavantage : les 2 premiers d20 du pool servent à la
        // mécanique 2d20 (on garde le meilleur ou le pire) ; d20
        // supplémentaires au-delà de ces 2, s'il y en a, lancés normalement.
        val isD20Adv = state.advantageState != AdvantageState.NORMAL &&
                (state.dicePool.find { it.sides == 20 }?.count ?: 0) >= 2

        state.dicePool.forEach { poolEntry ->
            if (isD20Adv && poolEntry.sides == 20) {
                // On lance bien les 2 d20 et on les garde tous les deux dans les
                // résultats affichés (isDiscarded marque celui qui n'est pas retenu),
                // pour que le nombre d'icônes affichées corresponde au nombre de dés
                // réellement lancés. Seule la valeur retenue compte dans le total.
                val advRolls = List(2) { (1..poolEntry.sides).random() }
                val keepIndex = if (state.advantageState == AdvantageState.ADVANTAGE) {
                    if (advRolls[0] >= advRolls[1]) 0 else 1
                } else {
                    if (advRolls[0] <= advRolls[1]) 0 else 1
                }
                advRolls.forEachIndexed { index, value ->
                    results.add(
                        DiceRollResult(
                            sides = poolEntry.sides,
                            value = value,
                            isCritical = value == 20,
                            advantageState = state.advantageState,
                            isDiscarded = index != keepIndex
                        )
                    )
                }
                val extra = poolEntry.count - 2
                if (extra > 0) {
                    repeat(extra) {
                        val roll = (1..poolEntry.sides).random()
                        results.add(DiceRollResult(sides = poolEntry.sides, value = roll, isCritical = roll == 20))
                    }
                }
            } else {
                repeat(poolEntry.count) {
                    val roll = (1..poolEntry.sides).random()
                    results.add(DiceRollResult(sides = poolEntry.sides, value = roll, isCritical = roll == 20 && poolEntry.sides == 20))
                }
            }
        }

        // Appliquer le modificateur au total (le d20 écarté ne compte pas)
        val baseTotal = results.filterNot { it.isDiscarded }.sumOf { it.value }
        val total = baseTotal + state.modifier

        val newState = state.copy(
            lastPoolResults = results,
            lastPoolTotal = total,
            showPoolResult = true,
            isRolling = false
        )
        _diceState.value = newState
        saveDiceState(newState)
        return results
    }

    // Ajoute un dé au pool (tap sur un type de dé)
    fun addDiceToPool(sides: Int) {
        val state = _diceState.value
        val currentEntry = state.dicePool.find { it.sides == sides }
        val newPool = if (currentEntry != null) {
            state.dicePool.map {
                if (it.sides == sides) it.copy(count = it.count + 1) else it
            }
        } else {
            state.dicePool + DicePoolEntry(sides = sides, count = 1)
        }
        val newState = state.copy(dicePool = newPool)
        _diceState.value = newState
        saveDiceState(newState)
    }

    // Retire un dé du pool
    fun removeDiceFromPool(sides: Int) {
        val state = _diceState.value
        val totalDiceCount = state.dicePool.sumOf { it.count }
        // On ne peut jamais vider complètement le pool.
        if (totalDiceCount <= 1) return

        val currentD20Count = state.dicePool.find { it.sides == 20 }?.count ?: 0
        // Avantage/Désavantage actif : toujours garder au moins 2d20.
        if (sides == 20 && state.advantageState != AdvantageState.NORMAL && currentD20Count <= 2) return

        val newPool = state.dicePool.mapNotNull { entry ->
            if (entry.sides == sides) {
                if (entry.count > 1) entry.copy(count = entry.count - 1) else null
            } else entry
        }
        val newState = state.copy(dicePool = newPool)
        _diceState.value = newState
        saveDiceState(newState)
    }

    // Modifie le modificateur (+1/-1)
    @Suppress("unused")
    fun setModifier(modifier: Int) {
        val newState = _diceState.value.copy(modifier = modifier)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun incrementModifier() {
        val newState = _diceState.value.copy(modifier = _diceState.value.modifier + 1)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun decrementModifier() {
        val newState = _diceState.value.copy(modifier = _diceState.value.modifier - 1)
        _diceState.value = newState
        saveDiceState(newState)
    }

    // Reset complet du pool — laisse toujours un d20 par défaut (jamais de pool vide)
    fun resetDicePool() {
        val newState = _diceState.value.copy(
            dicePool = listOf(DicePoolEntry(sides = 20, count = 1)),
            modifier = 0,
            lastPoolResults = emptyList(),
            lastPoolTotal = 0,
            showPoolResult = false
        )
        _diceState.value = newState
        saveDiceState(newState)
    }

    @Suppress("unused")
    fun setDiceType(sides: Int) {
        val newState = _diceState.value.copy(defaultSides = sides)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun hideDiceResult() {
        val newState = _diceState.value.copy(showResult = false)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun hidePoolResult() {
        val newState = _diceState.value.copy(showPoolResult = false)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun setDiceOverlayVisible(visible: Boolean) {
        val newState = _diceState.value.copy(overlayVisible = visible)
        _diceState.value = newState
        saveDiceState(newState)
    }

    // === AVANTAGE/DÉSAVANTAGE (D&D 5e) ===
    // Active l'avantage/désavantage force la présence d'au moins 2d20 dans le
    // pool, puisque la mécanique consiste à lancer 2d20 et garder le
    // meilleur (avantage) ou le pire (désavantage) des deux.
    fun setAdvantageState(state: AdvantageState) {
        val current = _diceState.value
        var newPool = current.dicePool
        if (state != AdvantageState.NORMAL) {
            val d20Entry = newPool.find { it.sides == 20 }
            newPool = when {
                d20Entry == null -> newPool + DicePoolEntry(sides = 20, count = 2)
                d20Entry.count < 2 -> newPool.map { if (it.sides == 20) it.copy(count = 2) else it }
                else -> newPool
            }
        }
        val newState = current.copy(advantageState = state, dicePool = newPool)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun toggleSound() {
        val newState = _diceState.value.copy(soundEnabled = !_diceState.value.soundEnabled)
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun setDiceSkin(skin: DiceSkin) {
        val newState = _diceState.value.copy(diceSkin = skin)
        _diceState.value = newState
        saveDiceState(newState)
    }
}

/**
 * Bonus de maîtrise selon le niveau (D&D 5e)
 */
fun calculateProficiencyBonus(level: Int): Int {
    return when {
        level >= 17 -> 6
        level >= 13 -> 5
        level >= 9 -> 4
        level >= 5 -> 3
        else -> 2
    }
}

/**
 * Table d'avancement du personnage (progression.md) : XP requis pour
 * atteindre chaque niveau, du niveau 1 au niveau 20.
 */
object CharacterProgression {
    private val xpThresholds = mapOf(
        1 to 0, 2 to 300, 3 to 900, 4 to 2700, 5 to 6500,
        6 to 14000, 7 to 23000, 8 to 34000, 9 to 48000, 10 to 64000,
        11 to 85000, 12 to 100000, 13 to 120000, 14 to 140000, 15 to 165000,
        16 to 195000, 17 to 225000, 18 to 265000, 19 to 305000, 20 to 355000
    )

    /** XP nécessaire pour atteindre ce niveau (0 si niveau 1 ou invalide). */
    fun xpForLevel(level: Int): Int = xpThresholds[level.coerceIn(1, 20)] ?: 0

    /** Niveau correspondant à un total d'XP donné. */
    fun levelForXp(xp: Int): Int = xpThresholds.entries.lastOrNull { xp >= it.value }?.key ?: 1

    /** Progression (0f à 1f) vers le niveau suivant, à partir de l'XP actuelle. */
    fun progressToNextLevel(level: Int, xp: Int): Float {
        if (level >= 20) return 1f
        val currentThreshold = xpForLevel(level)
        val nextThreshold = xpForLevel(level + 1)
        val span = (nextThreshold - currentThreshold).coerceAtLeast(1)
        return ((xp - currentThreshold).toFloat() / span).coerceIn(0f, 1f)
    }
}

/**
 * Un niveau pris dans une classe autre que la classe principale du personnage
 * (`Character.characterClass`), pour le multiclassage (cf. LevelUpDialog dans
 * CharacterSheetScreen.kt et GameState.appliquerMonteeDeNiveau). Le niveau total du
 * personnage (`Character.level`) reste la somme du niveau dans la classe principale
 * (= level - somme des niveaux ici) et de ces niveaux secondaires.
 */
@Serializable
data class NiveauClasse(val classe: String, val niveau: Int)

/**
 * Niveaux de maîtrise
 */
@Serializable
enum class ProficiencyLevel(@Suppress("unused") val label: String, @Suppress("unused") val multiplier: Int) {
    NONE("—", 0),
    PROFICIENT("Maîtrise", 1),
    EXPERTISE("Expertise", 2)
}

/**
 * Ajoute des maîtrises de compétence : une compétence déjà maîtrisée (ou reçue deux fois
 * dans [nouvelles]) qu'une autre source permet de maîtriser passe en Expertise.
 * Renvoie les listes (maîtrises, expertises) mises à jour.
 */
fun cumulerMaitrisesCompetences(
    maitrises: List<String>,
    expertises: List<String>,
    nouvelles: List<String>,
): Pair<List<String>, List<String>> {
    val toutes = maitrises.toMutableList()
    val expertisesFinales = expertises.toMutableList()
    nouvelles.forEach { competence ->
        val existante = toutes.firstOrNull { it.equals(competence, ignoreCase = true) }
        if (existante != null) {
            // Même libellé que la maîtrise existante : skillExpertise est comparée telle quelle.
            if (expertisesFinales.none { it.equals(existante, ignoreCase = true) }) expertisesFinales += existante
        } else {
            toutes += competence
        }
    }
    return toutes to expertisesFinales
}

/**
 * Format du fichier JSON écrit par GameState.writeCharacterFile/lu par characterFromJson :
 * le personnage ET son historique complet, pour qu'une copie/un partage du fichier n'en perde
 * jamais le journal (voir GameState.addCharacter historiqueImporte).
 */
@Serializable
data class CharacterExport(
    val character: Character,
    val historique: List<HistoriqueEntreeExport> = emptyList()
)

/**
 * Personnage créé (PJ ou PNJ) - Version D&D 5e complète
 */
@Serializable
data class Character(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "PJ", "PNJ", "Monstre", "Boss"
    val worldId: String = "", // Monde auquel le personnage appartient
    val characterClass: String = "",
    // Sous-classe (ex: "Voie du Berserker" pour un Barbare), choisie généralement au
    // niveau 1-3 selon la classe. Vide tant qu'elle n'a pas été choisie.
    val subclass: String = "",
    val race: String = "",
    val level: Int = 1,
    // Niveaux pris dans d'autres classes que `characterClass` (multiclassage, cf.
    // NiveauClasse). Le niveau dans la classe principale = level - somme de ces niveaux.
    val classesSecondaires: List<NiveauClasse> = emptyList(),
    val experience: Int = 0,
    val gold: Int = 0,
    val portrait: String = "",
    val alignment: String = "",
    val background: String = "",
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val maxHitPoints: Int = 10,
    val currentHitPoints: Int = 10,
    val temporaryHitPoints: Int = 0,
    val armorClass: Int = 10,
    val speed: Int = 30,
    val size: String = "", // Catégorie de taille (ex: "Petite", "Moyenne") ; vide = déduite de la race via sizeForRace
    val initiative: Int = 10,
    val initiativeBonus: Int = 0,
    val proficiencyBonus: Int = 2,
    val heroicInspiration: Boolean = false,
    val hitDiceUsed: Int = 0,
    // Emplacements de sort dépensés par niveau de sort (1er à 9e) — le maximum se recalcule
    // depuis la classe/le niveau (cf. EmplacementsDeSort, CharacterSheetScreen.kt). Récupérés
    // à un repos long (GameState.resetSpellSlots).
    val spellSlotsUsed: Map<Int, Int> = emptyMap(),
    // Emplacements de Magie de pacte dépensés (Occultiste) : pool séparé des emplacements
    // classiques ci-dessus, récupéré à un repos court OU long (GameState.resetPactSlots).
    val pactSlotsUsed: Int = 0,
    // Charges restantes des objets à charges portés (clé = nom de l'objet, cf.
    // EquipmentItem.charges) ; un objet absent de la table a toutes ses charges. Récupérées
    // au repos long selon la formule de l'objet (cf. ObjetsACharges).
    val itemCharges: Map<String, Int> = emptyMap(),
    // Objets magiques avec lesquels le personnage est harmonisé (noms d'objets, sans doublon,
    // au plus Harmonisation.maximum) : un repos court par objet, voir Harmonisation. Un objet
    // qui quitte l'inventaire en sort automatiquement (GameState.releaseLostAttunements).
    val attunedItems: List<String> = emptyList(),
    // Effets en cours à durée limitée (potion bue, sort lancé...), expirés d'après l'horloge de
    // scénario (voir EffetActif, GameState.purgerEffetsExpires).
    val effetsActifs: List<EffetActif> = emptyList(),
    // Épuisement (0-6, règles SRD) : peut être causé par la faim, mais aussi par d'autres
    // effets (maladie, chaleur/froid extrême...) ajustables manuellement par le MJ.
    val exhaustionLevel: Int = 0,
    // Dernier jour de fiction (ScenarioClockState.dayIndex) où le personnage a mangé une
    // ration complète. Sert à calculer le nombre de jours sans nourriture.
    val lastMealDay: Long = 1L,
    // Dernier jour de fiction jusqu'auquel l'épuisement dû à la faim a déjà été appliqué,
    // pour ne pas recompter les mêmes jours à chaque synchronisation.
    val hungerCheckedDay: Long = 1L,
    val condition: String = "",
    val savingThrows: Map<String, ProficiencyLevel> = emptyMap(),
    val savingThrowProficiencies: List<String> = emptyList(),
    val skills: Map<String, ProficiencyLevel> = emptyMap(),
    val skillProficiencies: List<String> = emptyList(),
    // Sous-ensemble de skillProficiencies passé au double de bonus de maîtrise (règle
    // "Expertise", ex. Roublard/Barde) — cf. ProficiencyLevel.EXPERTISE et
    // GameState.cycleSkillProficiency.
    val skillExpertise: List<String> = emptyList(),
    val equipment: String = "",
    val backpackItems: List<String> = emptyList(),
    val equippedItems: List<String> = emptyList(),
    val equippedSlots: Map<EquipmentSlot, String> = emptyMap(),
    // Nombre d'exemplaires empilés sur un emplacement (absent = 1). Seules les armes de
    // jet consommables (javelines, fléchettes) s'empilent, sur les emplacements d'arme de
    // dos — cf. ArmorRules.isStackableWeapon et GameState.equipInSlot. Chaque exemplaire
    // reste une entrée de equippedItems (poids compté à l'unité).
    val slotStackCounts: Map<EquipmentSlot, Int> = emptyMap(),
    // Flèches rangées dans le carquois équipé (EquipmentSlot.BACK), une entrée par flèche,
    // au plus ArmorRules.QUIVER_CAPACITY. Vidé dans backpackItems dès que le carquois
    // quitte l'emplacement BACK, cf. GameState.releaseQuiverIfUnequipped.
    val quiverContents: List<String> = emptyList(),
    // Emplacements EXTÉRIEURS du sac à dos équipé (clés 1..3, ex. sac de couchage, corde) —
    // un contenant distinct de equippedSlots/backpackItems, cf. ArmorRules.isBackExteriorItem
    // et GameState.equipBackpackExteriorSlot. Vidé (retour dans backpackItems) dès que le sac
    // à dos quitte EquipmentSlot.BACKPACK, cf. GameState.clearBackpackExterior.
    val backpackExteriorSlots: Map<Int, String> = emptyMap(),
    // Obsolète : matériel d'écriture autrefois rangé dans le grimoire. Plus rien n'y entre (le
    // matériel est choisi dans l'inventaire au moment de recopier un sort) ; conservé pour rendre
    // au sac le contenu des fiches existantes (GameState.releaseGrimoireContents).
    val grimoireContents: List<String> = emptyList(),
    // Objets qui servent aussi de focaliseur (nom de l'objet → « arcanique » / « druidique ») :
    // propriété d'un objet réel, ex. le bâton de combat du Magicien (cf. Focaliseurs). Le
    // grimoire d'un Magicien est focaliseur arcanique sans y figurer.
    val focaliseurs: Map<String, String> = emptyMap(),
    // Sorts connus/préparés, référencés par leur nom exact dans la bibliothèque SRD
    // (SrdRepository.getSpellByName) — pas de duplication des détails (école, niveau,
    // description) sur le personnage, ils sont relus depuis le SRD à l'affichage.
    val spells: List<String> = emptyList(),
    // Classe par laquelle chaque sort de `spells` est appris (clé = nom du sort). Absent
    // d'une entrée = classe principale (`characterClass`), pour rester compatible avec les
    // personnages sauvegardés avant l'ajout du multiclassage. Sert à compartimenter les
    // sorts connus/préparés par classe pour un personnage multiclassé (cf. PisteIncantation,
    // CharacterSheetScreen.kt).
    val spellClasses: Map<String, String> = emptyMap(),
    // Origine de chaque sort de `spells` (clé = nom du sort), affichée sur sa carte dans
    // l'onglet Sorts : « Magicien niv. 1 », « Espèce : Elfe », « Don : Initié à la magie »,
    // « Recopié dans le grimoire »... Absente pour les personnages d'avant ce suivi.
    val spellSources: Map<String, String> = emptyMap(),
    // Sorts (hors sorts mineurs) préparés par les classes qui changent leur préparation au repos
    // long (Clerc, Druide, Magicien, Paladin), choisis dans l'écran de repos. null = jamais
    // préparés : tous les sorts connus comptent comme prêts (personnages d'avant cette option).
    val preparedSpells: List<String>? = null,
    // Bottes d'arme (Maîtrise des armes) : noms des armes dont la botte est utilisable, changés
    // à chaque repos long (écran Repos). Vide = défaut de la classe (cf. ArsenalPersonnage.bottesActives).
    val weaponMasteries: List<String> = emptyList(),
    // Paladin — Imposition des mains : points de « Puissance curative » dépensés (réserve de
    // 5 × niveau de Paladin), récupérés au repos long (GameState.resetSpellSlots).
    val layOnHandsUsed: Int = 0,
    // Paladin niv. 2+ — Châtiment de paladin : lancement gratuit de Châtiment divin déjà utilisé
    // depuis le dernier repos long.
    val divineSmiteFreeUsed: Boolean = false,
    // Magicien — Restauration magique : déjà utilisée depuis le dernier repos long (récupération
    // d'emplacements à la fin d'un repos court, cf. RestScreen.ShortRestCard).
    val arcaneRecoveryUsed: Boolean = false,
    // Sorts lançables sans emplacement accordés par une aptitude (balise « sorts-speciaux: » de
    // classes_srd521.md) : nom du sort → mode, « a-volonte » (Maîtrise des sorts du Magicien) ou
    // « predilection » (Sorts de prédilection, une fois par repos court ou long). Toujours préparés.
    val sortsSpeciaux: Map<String, String> = emptyMap(),
    // Sorts de prédilection déjà lancés gratuitement depuis le dernier repos (court ou long).
    val sortsPredilectionUtilises: List<String> = emptyList(),
    // Sorts marqués d'une étoile sur la fiche : listés en premier quand il faut choisir un sort.
    val favoriteSpells: List<String> = emptyList(),
    // PNJ : profil de l'IA de combat (nom d'un ProfilIA), réglé par le MJ sur la fiche. null =
    // déduit de la classe et des armes à l'entrée en combat. Jamais montré aux joueurs.
    val profilIA: String? = null,
    val weapons: List<String> = emptyList(),
    val armor: List<String> = emptyList(),
    // Traits raciaux/d'espèce (ex: "Vision dans le noir", "Résistance naine"),
    // affichés dans l'onglet Notes de la fiche sous "Traits d'espèce".
    val traits: String = "",
    // Capacités octroyées par la classe (ex: "Attaque supplémentaire", "Rage"),
    // affichées dans l'onglet Notes de la fiche sous "Capacités de classe".
    val classFeatures: String = "",
    // Dons choisis au fil des niveaux, affichés dans l'onglet Notes sous "Dons".
    val feats: String = "",
    // Apparence physique du personnage, affichée dans l'onglet Notes sous "Apparence".
    val appearance: String = "",
    val notes: String = "",
    // Langues connues (Commun + langues choisies à la création), affichées dans
    // l'onglet Notes de la fiche.
    val languages: List<String> = emptyList(),
    // Maîtrises d'outils octroyées par l'historique (ex. "matériel de calligraphe"),
    // affichées dans l'onglet Notes de la fiche sous "Maîtrises".
    val proficiencies: String = "",
    // Maîtrise des armes/armures/outils octroyée par la classe (ex. "Armes courantes
    // et armes de guerre"), affichée dans l'onglet Combat de la fiche.
    val weaponArmorTraining: String = "",
    // Types de dégâts (voir damageTypes) auxquels le personnage résiste (dégâts divisés par
    // deux) ou est vulnérable (dégâts doublés), affichés dans l'onglet Combat de la fiche.
    val damageResistances: List<String> = emptyList(),
    val damageVulnerabilities: List<String> = emptyList(),
    // Choix d'espèce faits à la création (balises "choix:" de especes_srd521.md) : id du
    // choix -> valeurs retenues. Relu à la montée de niveau (sorts de lignage niveaux 3/5...).
    val speciesChoices: Map<String, List<String>> = emptyMap(),
    val personalityTraits: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val secrets: String = "",
    val dmNotes: String = "",
    // Réservés aux PNJ (type == "PNJ") : briefing MJ utilisé par l'événement de scène
    // "#event:[Nom]" (PnjBriefingOverlay), jamais visible des joueurs.
    val comportement: String = "",
    val intentions: String = "",
    val objectif: String = "",
    // Réservés aux PNJ : attitude du PNJ envers des factions, et envers des groupes
    // d'aventuriers (clé = MjGroup.id, score -100..100, cf. ReputationScale). Le score d'un
    // groupe s'applique automatiquement à tous ses membres (cf. GameState.pnjReputationToward).
    val factionReputations: List<Reputation> = emptyList(),
    val groupReputations: Map<String, Int> = emptyMap(),
    // Lieu où se trouve le personnage (ex. « Phandalin »), vide = non précisé / avec son groupe.
    val location: String = "",
    val createdBy: String = "MJ" // "MJ" ou "Joueur"
)

/**
 * Effet à durée limitée sur un personnage (ex. « Respiration aquatique » pendant 1 heure après
 * avoir bu la potion). [finSecondes] est l'instant de fin en secondes de fiction
 * (ScenarioClockState.totalSeconds) : l'effet s'écoule avec l'horloge de scénario, y compris
 * round par round pendant un combat (6 s par round).
 */
@Serializable
data class EffetActif(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String,
    // Objet ou sort à l'origine de l'effet.
    val source: String,
    val description: String = "",
    val debutSecondes: Long,
    val finSecondes: Long,
    val concentration: Boolean = false,
)

/**
 * Personnage Naheulbeuk V4
 */
@Serializable
data class NaheulbeukCharacter(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val origine: String = "",
    val metier: String = "",
    val worldId: String = "",
    val courage: Int = 10,
    val intelligence: Int = 10,
    val charisme: Int = 10,
    val adresse: Int = 10,
    val force: Int = 10,
    val evMax: Int = 10,
    val ev: Int = 10,
    val eaMax: Int = 10,
    val ea: Int = 10,
    val at: Int = 8,
    val prd: Int = 10,
    val effectiveAttaque: Int = 8,
    val effectiveParade: Int = 10,
    val eaCurrent: Int = 10,
    val pointsDestin: Int = 0,
    val destin: Int = 0,
    val fortune: Int = 70,
    val or: Int = 70,
    val backpackItems: List<String> = emptyList(),
    val equippedItems: List<String> = emptyList(),
    val stuff: String = ""
)

/**
 * Entrée dans le pool de dés (ex: 3d6, 1d20)
 */
@Serializable
data class DicePoolEntry(
    val sides: Int,
    val count: Int = 1
)

/**
 * Résultat d'un lancer de dé individuel
 */
@Serializable
data class DiceRollResult(
    val sides: Int,
    val value: Int,
    val isCritical: Boolean = false,
    val advantageState: AdvantageState = AdvantageState.NORMAL,
    // true pour le d20 non retenu d'un jet avantage/désavantage (affiché mais exclu du total)
    val isDiscarded: Boolean = false
)

/**
 * État global du dé
 */
@Serializable
data class DiceState(
    val defaultSides: Int = 20,
    val lastResult: Int? = null,
    val lastSides: Int = 20,
    val showResult: Boolean = false,
    val isRolling: Boolean = false,
    val overlayVisible: Boolean = false,

    // Pool de dés avancé
    val dicePool: List<DicePoolEntry> = emptyList(),
    val modifier: Int = 0,
    val lastPoolResults: List<DiceRollResult> = emptyList(),
    val lastPoolTotal: Int = 0,
    val showPoolResult: Boolean = false,

    // Avantage/Désavantage (D&D 5e)
    val advantageState: AdvantageState = AdvantageState.NORMAL,

    // Son
    val soundEnabled: Boolean = true,

    // Skin de dé
    val diceSkin: DiceSkin = DiceSkin.CLASSIC,

    // Monde associé
    val worldId: String = ""
)

/**
 * État simplifié du monde pour la couche UI.
 *
 * Les champs après [description] sont facultatifs et ne sont renseignés que pour un
 * univers importé par l'utilisateur (voir [com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository]) :
 * un monde intégré (D&D, Naheulbeuk) les laisse à `null`/valeur par défaut et garde son
 * apparence codée en dur (voir Theme.kt, WorldSelectionScreen.kt).
 */
@Serializable
data class WorldState(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean = false,
    // Nom d'icône Material reconnu (voir WorldIcons.kt), ou null pour l'icône par défaut.
    val iconName: String? = null,
    val primaryColorHex: String? = null,
    val secondaryColorHex: String? = null,
    val backgroundColorHex: String? = null,
    // Chemin absolu vers l'image de fond extraite de l'archive importée, ou null.
    val backgroundImagePath: String? = null,
    // Clés d'onglets de bibliothèque à afficher pour ce monde (voir LibraryScreen.kt) ;
    // null = toutes les catégories qui ont du contenu, comme pour les mondes intégrés.
    val enabledCategories: List<String>? = null,
)

/**
 * Emplacements d'équipement pour la silhouette.
 *
 * - CLOTHING : vêtements (un seul emplacement, cf. ArmorRules.isClothing).
 * - BACKPACK : le sac à dos lui-même (un seul emplacement) — distinct de BACK, qui reste
 *   pour cape/manteau/carquois. Le sac à dos équipé peut avoir jusqu'à 3 objets sur ses
 *   emplacements extérieurs (cf. Character.backpackExteriorSlots), un contenant à part de
 *   la silhouette.
 * - BELT_POUCH_1/2 : bourses portées à la ceinture.
 * - BACK_ACCESSORY_1..4 : accessoires portés dans le dos (bourses, potions...).
 * - BACK_WEAPON_1..3 : armes portées dans le dos, en plus des mains.
 */
@Serializable
enum class EquipmentSlot {
    HEAD, TORSO, MAIN_HAND, OFF_HAND, BACK, ACCESSORY,
    CLOTHING, BACKPACK,
    BELT_POUCH_1, BELT_POUCH_2,
    BACK_ACCESSORY_1, BACK_ACCESSORY_2, BACK_ACCESSORY_3, BACK_ACCESSORY_4,
    BACK_WEAPON_1, BACK_WEAPON_2, BACK_WEAPON_3
}

/** Emplacements d'arme portée dans le dos (les seuls où une pile d'armes de jet est admise). */
val BACK_WEAPON_SLOTS = setOf(EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3)

/** Nombre d'exemplaires sur [slot] : 0 si vide, 1 par défaut, plus pour une pile. */
fun Character.stackCount(slot: EquipmentSlot): Int =
    if (equippedSlots[slot] == null) 0 else (slotStackCounts[slot] ?: 1).coerceAtLeast(1)

/**
 * Résumé du calcul de CA.
 */
@Serializable
data class ArmorClassBreakdown(
    val total: Int,
    val detail: String,
    val hasArmor: Boolean,
    val hasShield: Boolean = false,
    val armorName: String? = null
)

/**
 * États d'avantage/désavantage.
 */
@Serializable
enum class AdvantageState {
    NORMAL,
    ADVANTAGE,
    DISADVANTAGE
}

/**
 * Rôle courant côté app : MJ ou Joueur (distinct de la session réseau).
 */
enum class AppRole {
    MJ,
    JOUEUR
}

/**
 * Skins visuels de dé.
 */
@Serializable
enum class DiceSkin {
    CLASSIC,
    SHADOW,
    ICE,
    FIRE
}

/**
 * Liste des mondes disponibles
 */
@Suppress("unused")
val availableWorlds = listOf(
    WorldState(
        id = "donjon_et_dragon",
        name = "Donjon et Dragon",
        description = "Un monde médiéval-fantastique classique avec des dragons, des donjons et des héros."
    ),
    WorldState(
        id = "naheulbeuk",
        name = "Naheulbeuk",
        description = "L'univers déjanté du Donjon de Naheulbeuk, plein d'humour et de chaos."
    )
)

/**
 * Mondes intégrés ([availableWorlds]) suivis des univers importés par l'utilisateur (voir
 * [com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository]) — utilisé par
 * [WorldSelectionScreen] pour afficher une carte par univers importé en plus des deux
 * mondes intégrés. Retourne uniquement [availableWorlds] si l'app n'est pas encore
 * initialisée ([GameState.init]).
 */
fun allWorlds(context: android.content.Context): List<WorldState> =
    availableWorlds + com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository.listCustomWorlds(context)