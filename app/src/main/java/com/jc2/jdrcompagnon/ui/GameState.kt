package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Store global pour l'état du jeu (monde courant).
 * Utilise un singleton simple pour partager l'état entre les écrans.
 */
object GameState {

    // SharedPreferences pour la persistance
    private const val PREFS_NAME = "jdr_compagnon_dice"
    private const val KEY_DICE_STATE = "dice_state_json"
    private const val KEY_CHARACTERS = "characters_json"
    private const val KEY_CURRENT_WORLD = "current_world_json"
    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadDiceState()
        loadCharacters()
        loadCurrentWorld()
    }

    private fun loadDiceState() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_DICE_STATE, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val state = json.decodeFromString<DiceState>(jsonString)
                    _diceState.value = state
                } catch (_: Exception) {
                    // Ignore parsing errors, keep default state
                }
            }
        }
    }

    private fun saveDiceState(state: DiceState) {
        prefs?.edit()?.apply {
            putString(KEY_DICE_STATE, json.encodeToString(state))
            apply()
        }
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

    private fun loadCharacters() {
        prefs?.let { prefs ->
            val jsonString = prefs.getString(KEY_CHARACTERS, "") ?: ""
            if (jsonString.isNotBlank()) {
                try {
                    val list = json.decodeFromString<List<Character>>(jsonString)
                    _characters.value = list
                } catch (e: Exception) {
                    android.util.Log.e("GameState", "Error loading characters", e)
                }
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

    fun selectWorld(world: WorldState) {
        _currentWorld.value = world
        saveCurrentWorld(world)
    }

    @Suppress("unused")
    fun clearWorld() {
        _currentWorld.value = null
        saveCurrentWorld(null)
    }

    fun isWorldSelected(): Boolean = _currentWorld.value != null

    // === PERSONNAGES CRÉÉS ===

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

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

    @Suppress("unused")
    fun removeCharacter(characterId: String) {
        val newList = _characters.value.filter { it.id != characterId }
        _characters.value = newList
        saveCharacters(newList)
    }

    // === ÉTAT DU DÉ ===

    private val _diceState = MutableStateFlow(DiceState())
    val diceState: StateFlow<DiceState> = _diceState.asStateFlow()

    fun rollDice(sides: Int = 20): Int {
        val result = (1..sides).random()
        val newState = _diceState.value.copy(
            lastResult = result,
            lastSides = sides,
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

        state.dicePool.forEach { poolEntry ->
            repeat(poolEntry.count) {
                val roll = (1..poolEntry.sides).random()
                results.add(DiceRollResult(sides = poolEntry.sides, value = roll))
            }
        }

        // Appliquer le modificateur au total
        val total = results.sumOf { it.value } + state.modifier

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

    // Reset complet du pool
    fun resetDicePool() {
        val newState = _diceState.value.copy(
            dicePool = emptyList(),
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
    fun setAdvantage(enabled: Boolean) {
        val newState = _diceState.value.copy(
            advantage = enabled,
            disadvantage = false
        )
        _diceState.value = newState
        saveDiceState(newState)
    }

    fun setDisadvantage(enabled: Boolean) {
        val newState = _diceState.value.copy(
            disadvantage = enabled,
            advantage = false
        )
        _diceState.value = newState
        saveDiceState(newState)
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
 * Personnage créé (PJ ou PNJ)
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
    val alignment: String = "",
    val background: String = "",
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val maxHp: Int = 10,
    val currentHp: Int = 10,
    val tempHp: Int = 0,
    val armorClass: Int = 10,
    val speed: Int = 30,
    val initiative: Int = 10,
    val savingThrows: Map<String, ProficiencyLevel> = emptyMap(),
    val skills: Map<String, ProficiencyLevel> = emptyMap(),
    val equipment: String = "",
    val backpackItems: List<String> = emptyList(),
    val equippedItems: List<String> = emptyList(),
    val traits: String = "",
    val dmNotes: String = "",
    val createdBy: String = "MJ" // "MJ" ou "Joueur"
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
    val value: Int
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
    val advantage: Boolean = false,
    val disadvantage: Boolean = false
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