package com.jc2.jdrcompagnon.feature_faction.data

import android.content.Context
import android.content.SharedPreferences
import com.jc2.jdrcompagnon.feature_faction.domain.model.Faction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Factions de tous les mondes (outil FACTIONS), enregistrées à chaque modification dans leurs
 * propres SharedPreferences. Chaque faction porte son monde (Faction.worldId).
 */
object FactionsStore {

    private const val PREFS = "factions"
    private const val KEY = "factions"
    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null

    private val _factions = MutableStateFlow<List<Faction>>(emptyList())
    val factions: StateFlow<List<Faction>> = _factions.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs?.getString(KEY, null)?.let { texte ->
            try {
                _factions.value = json.decodeFromString<List<Faction>>(texte)
            } catch (e: Exception) {
                android.util.Log.e("FactionsStore", "Lecture des factions impossible", e)
            }
        }
    }

    private fun enregistrer() {
        prefs?.edit()?.putString(KEY, json.encodeToString(_factions.value))?.apply()
    }

    fun duMonde(worldId: String): List<Faction> = _factions.value.filter { it.worldId == worldId }

    fun parId(id: String): Faction? = _factions.value.firstOrNull { it.id == id }

    fun ajouter(faction: Faction) {
        _factions.value = _factions.value + faction
        enregistrer()
    }

    /** Modifie la faction [id] (sans effet si elle n'existe plus). */
    fun maj(id: String, transformation: (Faction) -> Faction) {
        val avant = parId(id) ?: return
        val apres = transformation(avant)
        if (apres == avant) return
        _factions.value = _factions.value.map { if (it.id == id) apres else it }
        enregistrer()
    }

    /** Supprime la faction et les relations que les autres avaient avec elle. */
    fun supprimer(id: String) {
        _factions.value = _factions.value
            .filterNot { it.id == id }
            .map { f -> if (f.relations.any { it.factionId == id }) f.copy(relations = f.relations.filterNot { it.factionId == id }) else f }
        enregistrer()
    }
}
