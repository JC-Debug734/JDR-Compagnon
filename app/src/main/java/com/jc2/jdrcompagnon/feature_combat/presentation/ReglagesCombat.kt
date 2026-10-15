package com.jc2.jdrcompagnon.feature_combat.presentation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Ce que les joueurs voient de la santé des monstres (appli, page joueur, écran de la table). */
@Serializable
enum class VisibiliteSante(val label: String, val detail: String) {
    CACHEE("Cachée", "Debout ou vaincu, rien de plus"),
    DESCRIPTIVE("Descriptive", "Indemne, blessé, en sang, mal en point"),
    EXACTE("Exacte", "PV affichés comme pour les personnages"),
}

/** Bannières plein écran du récit (nouveau round, critique, mort, victoire, « À toi »). */
@Serializable
enum class NiveauBannieres(val label: String) {
    TOUTES("Toutes"),
    ESSENTIELLES("Essentielles"),
    AUCUNE("Aucune"),
}

/**
 * Réglages du combat, mémorisés sur l'appareil (outil « Combat » du tiroir). L'audio et les effets
 * s'appliquent à l'appareil qui les règle (MJ comme joueur) ; la visibilité et l'automatisation ne
 * comptent que sur l'appareil du MJ, qui calcule ce que la table reçoit.
 */
@Serializable
data class ReglagesCombat(
    // ── Audio ──
    val sons: Boolean = true,
    val volume: Float = 0.8f,
    val volumeImpacts: Float = 1f,
    val volumeMagie: Float = 0.8f,
    val volumeAnnonces: Float = 0.9f,
    // La musique baisse le temps d'un bruitage fort (critique, mort, boss…).
    val attenuerMusique: Boolean = true,
    // Pistes (id de LoopTrack) lancées au début du combat / à l'arrivée d'un boss ; null = la musique ne change pas.
    val musiqueCombat: String? = "combat_poursuite",
    val musiqueBoss: String? = "boss_epique",
    // Fin du combat : la piste jouée avant le combat reprend.
    val reprendreMusique: Boolean = true,
    // ── Effets ──
    val bannieres: NiveauBannieres = NiveauBannieres.TOUTES,
    val vibrations: Boolean = true,
    // ── Ce que voit la table (MJ) ──
    val santeMonstres: VisibiliteSante = VisibiliteSante.DESCRIPTIVE,
    // PV exacts des autres personnages ; sinon leur état (blessé, en sang…) comme un monstre.
    val pvAllies: Boolean = true,
    // ── Automatisation (MJ) ──
    // Les attaques des monstres pilotés par l'IA s'appliquent d'elles-mêmes à leur tour.
    val attaquesMonstresAuto: Boolean = false,
    // Valeur de départ des actions légendaires automatiques d'un boss (réglable ensuite boss par boss).
    val legendairesAuto: Boolean = true,
)

object ReglagesCombatStore {
    private const val PREFS = "combat"
    private const val CLE = "reglages"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var prefs: android.content.SharedPreferences? = null

    private val _reglages = MutableStateFlow(ReglagesCombat())
    val reglages: StateFlow<ReglagesCombat> = _reglages.asStateFlow()
    val courant: ReglagesCombat get() = _reglages.value

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { p ->
            val lu = p.getString(CLE, null)?.let { runCatching { json.decodeFromString<ReglagesCombat>(it) }.getOrNull() }
            // Avant ces réglages, seul l'interrupteur des sons était mémorisé.
            _reglages.value = lu ?: ReglagesCombat(sons = p.getBoolean("sons_actifs", true))
        }
    }

    fun modifier(transformation: (ReglagesCombat) -> ReglagesCombat) {
        _reglages.update(transformation)
        prefs?.edit()?.putString(CLE, json.encodeToString(ReglagesCombat.serializer(), _reglages.value))?.apply()
    }

    fun reinitialiser() = modifier { ReglagesCombat() }
}
