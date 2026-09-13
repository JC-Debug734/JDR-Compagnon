package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.content.SharedPreferences
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import com.jc2.jdrcompagnon.ui.MusicSettings

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

    private fun diceStateKey(worldId: String?): String =
        if (worldId.isNullOrBlank()) KEY_DICE_STATE else "$KEY_DICE_STATE_PREFIX$worldId"

    @Serializable
    data class MjGroup(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val memberIds: List<String> = emptyList(),
        val worldId: String = ""
    )

    @Serializable
    data class MjScene(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val markdownContent: String = "",
        val musicTrackId: String? = null,
        val order: Int = 0
    )

    @Serializable
    data class MjScenario(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val description: String = "",
        val markdownContent: String = "",
        val createdBy: String = "MJ",
        val worldId: String = "",
        val scenes: List<MjScene> = emptyList()
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
        val checklistItems: List<CampaignChecklistItem> = emptyList()
    ) {
        fun toMjCampaign(): MjCampaign = MjCampaign(
            id = id,
            title = title,
            worldId = worldId,
            scenarioIds = scenarioIds,
            checklistItems = checklistItems
        )
    }

    @Serializable
    data class MjCampaign(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val description: String = "",
        val worldId: String = "",
        val scenarioIds: List<String> = emptyList(),
        val checklistItems: List<CampaignChecklistItem> = emptyList()
    ) {
        fun toCampaignData(): CampaignData = CampaignData(
            id = id,
            title = title,
            worldId = worldId,
            scenarioIds = scenarioIds,
            checklistItems = checklistItems
        )
    }

    // Ancien monde par défaut pour la migration des données sans worldId
    private const val LEGACY_WORLD_ID = "donjon_et_dragon"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyData()
        loadDiceState()
        loadCharacters()
        loadNaheulbeukCharacters()
        loadMjGroups()
        loadMjScenarios()
        loadMjCampaigns()
        loadLastScenarioId()
        loadCurrentCampaignId()
        loadCurrentWorld()
        loadAppRole()
        loadSelectedCharacterId()
        loadPlayerName()
        importDefaultScenarioIfNeeded(context)
        syncScenariosFromDisk(context)
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
    private fun parseMarkdownScenario(markdown: String): Pair<String, List<MjScene>> {
        val lines = markdown.lines()
        val title = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim() ?: "Scénario sans titre"
        val scenes = mutableListOf<MjScene>()
        var currentTitle = "Introduction"
        var currentMusic: String? = null
        val currentContent = StringBuilder()
        var inScene = false

        val metaRegex = Regex("""\{mscenemeta:\s*music=([^}]*)\}""")

        fun flushScene() {
            if (inScene || currentContent.isNotBlank()) {
                scenes += MjScene(
                    title = currentTitle,
                    markdownContent = currentContent.toString().trim(),
                    musicTrackId = currentMusic?.takeIf { it.isNotBlank() },
                    order = scenes.size
                )
            }
            currentContent.clear()
            currentMusic = null
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
                continue
            }
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

    private fun slugify(text: String): String {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return normalized.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "scenario" }
    }

    private fun scenarioFileName(scenario: MjScenario): String {
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
        builder.appendLine()
        val scenesToWrite = scenario.scenes.ifEmpty {
            listOf(MjScene(title = scenario.title, markdownContent = scenario.markdownContent))
        }
        scenesToWrite.forEach { scene ->
            builder.appendLine("# SCÈNE — ${scene.title}")
            builder.appendLine()
            builder.appendLine("{mscenemeta: music=${scene.musicTrackId ?: ""}}")
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
        if (files.isEmpty()) return

        var current = _mjScenarios.value
        var changed = false

        for (entry in files) {
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
                val updated = existing.copy(title = parsed.first, scenes = parsed.second)
                if (updated != existing) {
                    current = current.map { if (it.id == existing.id) updated else it }
                    changed = true
                }
            } else {
                val newScenario = MjScenario(
                    title = parsed.first,
                    worldId = currentWorldId() ?: "",
                    scenes = parsed.second,
                    createdBy = "MJ"
                )
                current = current + newScenario
                changed = true
                // Réécrit le fichier avec l'id désormais connu, pour une synchro fiable la prochaine fois.
                writeScenarioFile(context, newScenario)
                android.util.Log.i("GameState", "Nouveau scénario importé depuis le disque : ${newScenario.title}")
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
        val items = character.backpackItems + character.equippedItems
        val totalLbs = items.sumOf { itemName ->
            ArmorRules.weightInPounds(itemName) ?: 0.0
        }
        return totalLbs / 2.20462
    }

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
                    val migrated = list.map { migrateEquippedItemsToSlots(it) }
                    _characters.value = migrated
                    if (migrated.any { it.equippedSlots.isNotEmpty() }) {
                        saveCharacters(migrated)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading characters", e)
                }
            }
        }
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

    private val _currentCampaignId = MutableStateFlow<String?>(null)
    val currentCampaignId: StateFlow<String?> = _currentCampaignId.asStateFlow()

    fun selectWorld(world: WorldState) {
        val previousWorld = _currentWorld.value
        _currentWorld.value = world
        saveCurrentWorld(world)
        if (previousWorld?.id != world.id) {
            reloadDiceStateForWorld(world.id)
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

    fun addCharacter(character: Character) {
        val newList = _characters.value + character
        _characters.value = newList
        saveCharacters(newList)
    }

    fun updateCharacter(updatedCharacter: Character) {
        val newList = _characters.value.map {
            if (it.id == updatedCharacter.id) updatedCharacter else it
        }
        _characters.value = newList
        saveCharacters(newList)
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

    @Suppress("unused")
    fun removeCharacter(characterId: String) {
        val newList = _characters.value.filter { it.id != characterId }
        _characters.value = newList
        saveCharacters(newList)
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
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(backpackItems = character.backpackItems + trimmed)
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Ajoute un item au sac d'un personnage D&D/PNJ. */
    fun addItemToBackpack(characterId: String, itemName: String) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(backpackItems = character.backpackItems + itemName)
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
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
                equippedItems = (target.equippedItems + itemName).distinct()
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
        val updated = target.copy(
            backpackItems = if (target.backpackItems.contains(itemName)) target.backpackItems else target.backpackItems + itemName,
            equippedItems = target.equippedItems - itemName,
            equippedSlots = target.equippedSlots.filterValues { it != itemName }
        )
        updateCharacter(updated)
    }

    /** Équipe un item dans un slot. Retourne true si accepté. */
    fun equipInSlot(characterId: String, itemName: String, slot: EquipmentSlot): Boolean {
        val target = _characters.value.find { it.id == characterId } ?: return false
        if (!target.backpackItems.contains(itemName)) return false
        val currentSlots = target.equippedSlots.toMutableMap()

        // Règle : 1 armure max (slot TORSO)
        if (slot == EquipmentSlot.TORSO && currentSlots[EquipmentSlot.TORSO] != null) return false
        // Règle : OFF_HAND occupé et item n'est pas un bouclier -> refus
        if (slot == EquipmentSlot.OFF_HAND && !itemName.contains("bouclier", ignoreCase = true)) return false
        // Arme à 2 mains : occupe les deux mains
        if (ArmorRules.twoHanded(itemName)) {
            if (currentSlots[EquipmentSlot.MAIN_HAND] != null || currentSlots[EquipmentSlot.OFF_HAND] != null) return false
            currentSlots[EquipmentSlot.MAIN_HAND] = itemName
            currentSlots[EquipmentSlot.OFF_HAND] = itemName
        } else {
            // Si arme 1 main en main principale et item est bouclier -> OK
            currentSlots[slot] = itemName
        }

        val newBackpack = target.backpackItems - itemName
        val newEquipped = (target.equippedItems + itemName).distinct()
        val updated = target.copy(
            backpackItems = newBackpack,
            equippedItems = newEquipped,
            equippedSlots = currentSlots
        )
        updateCharacter(updated)
        return true
    }

    /** Supprime définitivement un item, qu'il soit dans le sac ou équipé (corbeille). */
    fun removeItemCompletely(characterId: String, itemName: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val updated = target.copy(
            backpackItems = target.backpackItems - itemName,
            equippedItems = target.equippedItems - itemName,
            equippedSlots = target.equippedSlots.filterValues { it != itemName }
        )
        updateCharacter(updated)
    }

    /** Déséquipe un item de tous les slots où il est présent. */
    fun unequipFromSlot(characterId: String, itemName: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val newSlots = target.equippedSlots.filterValues { it != itemName }
        val newEquipped = target.equippedItems - itemName
        val newBackpack = if (target.backpackItems.contains(itemName)) target.backpackItems else target.backpackItems + itemName
        val updated = target.copy(
            backpackItems = newBackpack,
            equippedItems = newEquipped,
            equippedSlots = newSlots
        )
        updateCharacter(updated)
    }

    fun getCharactersByType(type: String, worldId: String? = null): List<Character> {
        return _characters.value.filter {
            it.type == type && (worldId == null || it.worldId == worldId)
        }
    }

    /** Met à jour les PV actuels d'un personnage D&D. */
    fun updateCharacterHp(characterId: String, currentHitPoints: Int) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(currentHitPoints = currentHitPoints.coerceIn(0, character.maxHitPoints + character.temporaryHitPoints))
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Renomme un personnage D&D. */
    fun renameCharacter(characterId: String, newName: String) {
        if (newName.isBlank()) return
        val updated = _characters.value.map { character ->
            if (character.id == characterId) character.copy(name = newName.trim()) else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Met à jour les notes d'un personnage D&D. */
    fun updateCharacterNotes(characterId: String, notes: String) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) character.copy(notes = notes) else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Fait progresser le niveau d'un personnage D&D et met à jour le bonus de maîtrise. */
    fun levelUpCharacter(characterId: String) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                val newLevel = (character.level + 1).coerceAtMost(20)
                character.copy(level = newLevel, proficiencyBonus = calculateProficiencyBonus(newLevel))
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /**
     * Ajoute de l'XP à un personnage et met à jour son niveau (et son bonus
     * de maîtrise) en conséquence, selon la table d'avancement du personnage.
     */
    fun addExperience(characterId: String, amount: Int) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                val newXp = (character.experience + amount).coerceAtLeast(0)
                val newLevel = CharacterProgression.levelForXp(newXp)
                character.copy(
                    experience = newXp,
                    level = newLevel,
                    proficiencyBonus = calculateProficiencyBonus(newLevel)
                )
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Ajoute (ou retire, si négatif) de l'or à la bourse d'un personnage. */
    fun addGold(characterId: String, amount: Int) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(gold = (character.gold + amount).coerceAtLeast(0))
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Définit directement le montant d'or d'un personnage. */
    fun setGold(characterId: String, amount: Int) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(gold = amount.coerceAtLeast(0))
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Définit le portrait (identifiant d'image locale) d'un personnage. */
    fun setCharacterPortrait(characterId: String, portrait: String) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(portrait = portrait)
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /** Active/désactive l'Inspiration Héroïque d'un personnage. */
    fun setHeroicInspiration(characterId: String, value: Boolean) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(heroicInspiration = value)
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /**
     * Dépense un dé de vie (ex : lors d'un repos court). Le nombre de dés
     * disponibles est égal au niveau du personnage moins ceux déjà dépensés.
     */
    fun spendHitDie(characterId: String) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(hitDiceUsed = (character.hitDiceUsed + 1).coerceIn(0, character.level))
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /**
     * Récupère des dés de vie (ex : lors d'un repos long). Par défaut en
     * récupère un seul ; passer un nombre plus élevé pour un repos long
     * (généralement la moitié du total, arrondi au supérieur).
     */
    fun recoverHitDice(characterId: String, amount: Int = 1) {
        val updated = _characters.value.map { character ->
            if (character.id == characterId) {
                character.copy(hitDiceUsed = (character.hitDiceUsed - amount).coerceIn(0, character.level))
            } else character
        }
        _characters.value = updated
        saveCharacters(updated)
    }

    /**
     * Fait progresser (cycle) la maîtrise d'une compétence d'un personnage.
     */
    fun cycleSkillProficiency(characterId: String, skill: String) {
        val target = _characters.value.find { it.id == characterId } ?: return
        val current = target.skillProficiencies.toMutableList()
        val newProficiencies = if (current.contains(skill)) {
            // Simple cycle : maîtrisé -> non maîtrisé (le modèle actuel n'a que binaire)
            current - skill
        } else {
            current + skill
        }
        val updated = target.copy(skillProficiencies = newProficiencies)
        updateCharacter(updated)
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
        return when (skill) {
            "Acrobaties", "Escamotage", "Furtivité", "Vol" -> abilityModifier(character.dexterity)
            "Arcanes", "Histoire", "Investigation", "Nature", "Religion" -> abilityModifier(character.intelligence)
            "Dressage", "Médecine", "Perception", "Survie", "Intuition" -> abilityModifier(character.wisdom)
            "Tromperie", "Intimidation", "Représentation", "Persuasion" -> abilityModifier(character.charisma)
            "Athlétisme" -> abilityModifier(character.strength)
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
        val newList = _mjGroups.value.filter { it.id != groupId }
        _mjGroups.value = newList
        saveMjGroups(newList)
    }

    fun removeMjScenario(scenarioId: String) {
        val newList = _mjScenarios.value.filter { it.id != scenarioId }
        _mjScenarios.value = newList
        saveMjScenarios(newList)
    }

    fun addMjGroup(group: MjGroup) {
        _mjGroups.value = _mjGroups.value + group
        saveMjGroups(_mjGroups.value)
    }

    fun updateMjGroup(updatedGroup: MjGroup) {
        _mjGroups.value = _mjGroups.value.map { if (it.id == updatedGroup.id) updatedGroup else it }
        saveMjGroups(_mjGroups.value)
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

    fun addMjCampaign(campaign: MjCampaign) {
        _mjCampaigns.value = _mjCampaigns.value + campaign
        saveMjCampaigns(_mjCampaigns.value)
    }

    fun updateMjCampaign(updated: MjCampaign) {
        _mjCampaigns.value = _mjCampaigns.value.map { if (it.id == updated.id) updated else it }
        saveMjCampaigns(_mjCampaigns.value)
    }

    fun removeMjCampaign(id: String) {
        _mjCampaigns.value = _mjCampaigns.value.filter { it.id != id }
        saveMjCampaigns(_mjCampaigns.value)
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
                    _mjCampaigns.value = list
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading MJ campaigns", e)
                }
            }
        }
    }

    private fun lastScenarioKey(worldId: String): String = "$KEY_MJ_LAST_SCENARIO_PREFIX$worldId"

    private var lastLoadedWorldId: String? = null

    private fun loadLastScenarioId() {
        // chargement différé par monde dans ensureLastScenarioLoadedForWorld
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
                val advRolls = List(2) { (1..poolEntry.sides).random() }
                val chosenValue = if (state.advantageState == AdvantageState.ADVANTAGE) advRolls.max() else advRolls.min()
                results.add(
                    DiceRollResult(
                        sides = poolEntry.sides,
                        value = chosenValue,
                        isCritical = chosenValue == 20,
                        advantageState = state.advantageState
                    )
                )
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

        // Appliquer le modificateur au total
        val baseTotal = results.sumOf { it.value }
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
 * Niveaux de maîtrise
 */
@Serializable
enum class ProficiencyLevel(@Suppress("unused") val label: String, @Suppress("unused") val multiplier: Int) {
    NONE("—", 0),
    PROFICIENT("Maîtrise", 1),
    EXPERTISE("Expertise", 2)
}

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
    val race: String = "",
    val level: Int = 1,
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
    val initiative: Int = 10,
    val initiativeBonus: Int = 0,
    val proficiencyBonus: Int = 2,
    val heroicInspiration: Boolean = false,
    val hitDiceUsed: Int = 0,
    val savingThrows: Map<String, ProficiencyLevel> = emptyMap(),
    val savingThrowProficiencies: List<String> = emptyList(),
    val skills: Map<String, ProficiencyLevel> = emptyMap(),
    val skillProficiencies: List<String> = emptyList(),
    val equipment: String = "",
    val backpackItems: List<String> = emptyList(),
    val equippedItems: List<String> = emptyList(),
    val equippedSlots: Map<EquipmentSlot, String> = emptyMap(),
    val weapons: List<String> = emptyList(),
    val armor: List<String> = emptyList(),
    val traits: String = "",
    val notes: String = "",
    val personalityTraits: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val secrets: String = "",
    val dmNotes: String = "",
    val createdBy: String = "MJ" // "MJ" ou "Joueur"
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
    val advantageState: AdvantageState = AdvantageState.NORMAL
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
 * État simplifié du monde pour la couche UI
 */
@Serializable
data class WorldState(
    val id: String,
    val name: String,
    val description: String
)

/**
 * Emplacements d'équipement pour la silhouette.
 */
@Serializable
enum class EquipmentSlot {
    HEAD, TORSO, MAIN_HAND, OFF_HAND, BACK, ACCESSORY
}

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