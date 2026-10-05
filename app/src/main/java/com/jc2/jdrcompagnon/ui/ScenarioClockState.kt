package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Condition météo courante du scénario. Nouvelles valeurs ajoutées en fin d'énumération (le nom
 * est sérialisé, l'ordre n'a pas d'importance pour les données existantes).
 */
@Serializable
enum class ScenarioWeather { CLAIR, NUAGEUX, PLUIE, ORAGE, NEIGE, BROUILLARD, FORTE_NEIGE, VENT_FORT, SEC }

/**
 * Température ressentie, indépendante du ciel (une journée claire peut être glaciale). Les deux
 * extrêmes reprennent les règles de froid/chaleur extrêmes du SRD (voir [regle]).
 */
@Serializable
enum class ScenarioTemperature(val label: String, val regle: String? = null) {
    FROID_INTENSE("Froid intense", "Froid extrême : sans protection, JS de Constitution DD 10 à chaque heure ou 1 niveau d'épuisement."),
    FROID("Froid"),
    FRAIS("Frais"),
    DOUX("Doux"),
    CHAUD("Chaud"),
    FORTE_CHALEUR("Forte chaleur", "Chaleur extrême : sans eau, JS de Constitution à chaque heure (DD 5, +1 par heure) ou 1 niveau d'épuisement."),
}

/** Fête ou jour férié du calendrier : [mois] 0-indexé, [jour] 1-indexé. */
@Serializable
data class JourFerie(val nom: String, val mois: Int, val jour: Int)

/**
 * Climat d'un mois du calendrier : saison affichée et météos/températures possibles pour la météo
 * automatique. Listes vides = aucune contrainte (toutes les météos, température inchangée).
 */
@Serializable
data class ClimatMois(
    val saison: String = "",
    val meteos: List<ScenarioWeather> = emptyList(),
    val temperatures: List<ScenarioTemperature> = emptyList(),
) {
    companion object {
        /** Préréglages de saison proposés dans l'éditeur du calendrier. */
        val hiver = ClimatMois(
            "Hiver",
            listOf(ScenarioWeather.CLAIR, ScenarioWeather.NUAGEUX, ScenarioWeather.NEIGE, ScenarioWeather.FORTE_NEIGE, ScenarioWeather.BROUILLARD, ScenarioWeather.VENT_FORT),
            listOf(ScenarioTemperature.FROID_INTENSE, ScenarioTemperature.FROID),
        )
        val printemps = ClimatMois(
            "Printemps",
            listOf(ScenarioWeather.CLAIR, ScenarioWeather.NUAGEUX, ScenarioWeather.PLUIE, ScenarioWeather.ORAGE, ScenarioWeather.BROUILLARD),
            listOf(ScenarioTemperature.FRAIS, ScenarioTemperature.DOUX),
        )
        val ete = ClimatMois(
            "Été",
            listOf(ScenarioWeather.CLAIR, ScenarioWeather.SEC, ScenarioWeather.NUAGEUX, ScenarioWeather.ORAGE),
            listOf(ScenarioTemperature.DOUX, ScenarioTemperature.CHAUD, ScenarioTemperature.FORTE_CHALEUR),
        )
        val automne = ClimatMois(
            "Automne",
            listOf(ScenarioWeather.NUAGEUX, ScenarioWeather.PLUIE, ScenarioWeather.BROUILLARD, ScenarioWeather.VENT_FORT, ScenarioWeather.CLAIR),
            listOf(ScenarioTemperature.FROID, ScenarioTemperature.FRAIS, ScenarioTemperature.DOUX),
        )
        val saisons: List<ClimatMois> = listOf(hiver, printemps, ete, automne)
    }
}

/** Période du jour déduite de l'heure de scénario, pour choisir l'icône soleil/lune/crépuscule. */
enum class DayPeriod { NUIT, AUBE, JOUR, CREPUSCULE }

@Serializable
data class ScenarioClockData(
    val enabled: Boolean = false,
    val autoAdvanceEnabled: Boolean = true,
    val autoWeatherEnabled: Boolean = true,
    val weather: ScenarioWeather = ScenarioWeather.CLAIR,
    val temperature: ScenarioTemperature = ScenarioTemperature.DOUX,
    // Minutes écoulées depuis un jour fictif 0, 08:00 par défaut. Un simple compteur (plutôt que
    // java.time, absent du reste du projet) suffit pour dérouler heure/jour/nuit et météo.
    val scenarioMinutes: Long = 8 * 60,
    // Secondes (0..59) de la minute en cours : l'heure défile en temps réel (1 s de fiction par
    // seconde réelle), [scenarioMinutes] restant la référence utilisée partout ailleurs.
    val scenarioSeconds: Int = 0,
)

/**
 * Configuration du calendrier personnalisable (noms de mois/jours, jours par mois, année de
 * départ), purement une couche d'affichage par-dessus [ScenarioClockData.scenarioMinutes] — ce
 * dernier reste l'unique source de vérité du temps écoulé. Réglée uniquement côté MJ (pas
 * rediffusée aux joueurs, contrairement à ScenarioClockData).
 */
@Serializable
data class CalendarConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String = "Calendrier",
    val monthNames: List<String> = defaultMonthNames,
    val daysPerMonth: List<Int> = List(12) { 30 },
    val dayNames: List<String> = defaultDayNames,
    val startYear: Int = 1,
    val joursFeries: List<JourFerie> = emptyList(),
    // Un climat par mois (même index que [monthNames]) ; un mois sans entrée n'a pas de contrainte.
    val climatParMois: List<ClimatMois> = emptyList(),
) {
    /** Ajoute un mois en fin d'année (30 jours, sans climat). */
    fun avecMoisAjoute(): CalendarConfig = copy(
        monthNames = monthNames + "Mois ${monthNames.size + 1}",
        daysPerMonth = daysPerMonth + 30,
    )

    /** Retire le mois [index] (au moins un mois reste) et décale les jours fériés suivants. */
    fun avecMoisRetire(index: Int): CalendarConfig {
        if (monthNames.size <= 1 || index !in monthNames.indices) return this
        return copy(
            monthNames = monthNames.filterIndexed { i, _ -> i != index },
            daysPerMonth = daysPerMonth.filterIndexed { i, _ -> i != index },
            climatParMois = climatParMois.filterIndexed { i, _ -> i != index },
            joursFeries = joursFeries.filter { it.mois != index }.map { if (it.mois > index) it.copy(mois = it.mois - 1) else it },
        )
    }

    fun climat(monthIndex: Int): ClimatMois? = climatParMois.getOrNull(monthIndex)

    /** Remplace le climat du mois [index], en complétant la liste si besoin. */
    fun avecClimat(index: Int, climat: ClimatMois): CalendarConfig {
        val liste = climatParMois.toMutableList()
        while (liste.size <= index) liste += ClimatMois()
        liste[index] = climat
        return copy(climatParMois = liste)
    }

    companion object {
        val defaultMonthNames: List<String> = (1..12).map { "Mois $it" }
        val defaultDayNames: List<String> = (1..7).map { "Jour $it" }

        /**
         * Calendrier des Harpistes (Calendar of Harptos) : calendrier standard des Royaumes
         * Oubliés (Forgotten Realms), cadre de campagne par défaut de D&D 5e. 12 mois de 30 jours
         * entrecoupés de 5 jours de fête (Solstice d'hiver, Verdeprêt, Solstice d'été,
         * Grandmoisson, Fête de la Lune) — modélisés ici comme des "mois" d'un seul jour, pour
         * rester compatible avec le modèle mois/jours existant (total 365 jours, comme la
         * véritable année de Faerûn hors Shieldmeet, jour bissextile tous les 4 ans non
         * modélisé ici). Semaines de 10 jours ("décades"), pas de 7 comme le calendrier
         * générique par défaut.
         */
        fun calendrierHarptos(): CalendarConfig = CalendarConfig(
            nom = "Calendrier des Harpistes (Faerûn)",
            monthNames = listOf(
                "Hammer", "Solstice d'hiver", "Alturiak", "Ches", "Tarsakh", "Verdeprêt",
                "Mirtul", "Kythorn", "Flamerule", "Solstice d'été", "Eleasis", "Eleint",
                "Grandmoisson", "Marpenoth", "Uktar", "Fête de la Lune", "Nightal"
            ),
            daysPerMonth = listOf(30, 1, 30, 30, 30, 1, 30, 30, 30, 1, 30, 30, 1, 30, 30, 1, 30),
            dayNames = (1..10).map { "Jour $it" },
            startYear = 1492,
            climatParMois = listOf(
                ClimatMois.hiver, ClimatMois.hiver, ClimatMois.hiver, ClimatMois.printemps, ClimatMois.printemps,
                ClimatMois.printemps, ClimatMois.printemps, ClimatMois.ete, ClimatMois.ete, ClimatMois.ete,
                ClimatMois.ete, ClimatMois.automne, ClimatMois.automne, ClimatMois.automne, ClimatMois.hiver,
                ClimatMois.hiver, ClimatMois.hiver,
            ),
            joursFeries = listOf(
                JourFerie("Solstice d'hiver", 1, 1),
                JourFerie("Verdeprêt", 5, 1),
                JourFerie("Solstice d'été", 9, 1),
                JourFerie("Grandmoisson", 12, 1),
                JourFerie("Fête de la Lune", 15, 1),
            ),
        )
    }
}

/**
 * Horloge de scénario (temps in-fiction + météo), affichée dans le menu latéral MJ/Joueur et
 * pilotée depuis une fenêtre de gestion sur le tableau de bord MJ (MjHomeScreen).
 *
 * Objet singleton, même principe que GameState / NetworkSessionManager : source unique de
 * vérité côté MJ, persistée en SharedPreferences. Il n'existe pas de backend partagé
 * (Firebase/Firestore) dans ce projet : la synchronisation MJ → Joueurs passe par le protocole
 * réseau LAN existant (NetworkMessage.TYPE_TIME_STATE, voir NetworkSessionManager), qui rediffuse
 * l'état complet à chaque changement local côté MJ (y compris chaque tic de l'horloge en cours de
 * défilement), garantissant que l'arrêt du défilement par le MJ stoppe aussi tous les joueurs dès
 * la prochaine diffusion.
 */
object ScenarioClockState {
    private val GENERIC_DAY_NAME = Regex("""Jour \d+""")

    private const val PREFS_NAME = "jdr_compagnon_clock"
    private const val KEY_CLOCK_STATE = "clock_state_json"
    // Ancienne clé (un seul calendrier) : lue une fois pour migration vers KEY_CALENDARS, jamais
    // réécrite depuis le passage à plusieurs calendriers nommés.
    private const val KEY_CALENDAR_CONFIG_LEGACY = "calendar_config_json"
    private const val KEY_CALENDARS = "calendars_json"
    private const val KEY_ACTIVE_CALENDAR_ID = "active_calendar_id"
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Calendrier des Harpistes (Faerûn) par défaut : c'est le calendrier standard de D&D 5e
    // (Forgotten Realms, cadre de campagne officiel par défaut), plus pertinent pour un nouveau
    // MJ que le calendrier générique "Mois 1, Mois 2...".
    private val _calendars = MutableStateFlow(listOf(CalendarConfig.calendrierHarptos()))
    val calendars: StateFlow<List<CalendarConfig>> = _calendars.asStateFlow()

    private val _activeCalendarId = MutableStateFlow(_calendars.value.first().id)
    val activeCalendarId: StateFlow<String> = _activeCalendarId.asStateFlow()

    // Côté joueur : calendrier actif du MJ, reçu avec chaque état d'horloge (voir
    // NetworkSessionManager.TYPE_TIME_STATE). Prioritaire sur les calendriers locaux tant qu'il
    // est renseigné, pour que les joueurs voient le calendrier personnalisé du MJ. Jamais persisté.
    private val _remoteCalendar = MutableStateFlow<CalendarConfig?>(null)
    val remoteCalendar: StateFlow<CalendarConfig?> = _remoteCalendar.asStateFlow()

    /** Calendrier actuellement actif (toujours résolu, retombe sur le premier si l'id est invalide). */
    fun activeCalendar(): CalendarConfig = _remoteCalendar.value
        ?: _calendars.value.find { it.id == _activeCalendarId.value } ?: _calendars.value.first()

    fun calendarById(id: String?): CalendarConfig =
        _calendars.value.find { it.id == id } ?: activeCalendar()

    // Temps réel : 1 seconde de fiction par seconde réelle.
    private const val TICK_MS = 1000L

    private val _state = MutableStateFlow(ScenarioClockData())
    val state: StateFlow<ScenarioClockData> = _state.asStateFlow()

    private var tickJob: Job? = null

    /** Branché par NetworkSessionManager pour rediffuser aux joueurs à chaque changement côté MJ. */
    private var onLocalChange: ((ScenarioClockData) -> Unit)? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        load()
        loadCalendars()
        startTicking()
        syncAmbientSound(_state.value)
    }

    fun setOnLocalChangeListener(listener: (ScenarioClockData) -> Unit) {
        onLocalChange = listener
    }

    /**
     * Applique un état reçu du MJ (côté joueur) : ne persiste pas, ne rediffuse pas. [calendar]
     * est le calendrier actif du MJ (null si le MJ utilise une ancienne version de l'app).
     */
    fun applyRemote(remoteState: ScenarioClockData, calendar: CalendarConfig? = null) {
        _state.value = remoteState
        if (calendar != null) _remoteCalendar.value = calendar
        syncAmbientSound(remoteState)
    }

    /** Côté joueur, à la déconnexion : on revient aux calendriers locaux. */
    fun clearRemoteCalendar() {
        _remoteCalendar.value = null
    }

    /** Rediffuse l'état courant (ex. après un changement de calendrier, qui accompagne l'état). */
    private fun rebroadcast() {
        onLocalChange?.invoke(_state.value)
    }

    /**
     * Démarre/arrête/change l'ambiance sonore météo (WeatherSoundManager) pour rester cohérente
     * avec l'état courant : rien tant que l'horloge n'est pas activée, sinon la boucle
     * correspondant à [state].weather (silencieuse si aucune piste de assets/son/effet n'existe
     * encore pour cette météo). Appelé à chaque changement, côté MJ (source) comme côté joueurs
     * (état reçu du réseau).
     */
    /** Réévalue l'ambiance sonore météo après un changement de réglage (activé/volume). */
    fun refreshAmbientSound() = syncAmbientSound(_state.value)

    private fun syncAmbientSound(state: ScenarioClockData) {
        val context = appContext ?: return
        if (!GameState.weatherSoundSettings.value.enabled || !state.enabled) {
            WeatherSoundManager.stop()
            return
        }
        WeatherSoundManager.play(context, state.weather, GameState.weatherSoundSettings.value.indoor)
    }

    private fun load() {
        val jsonString = prefs?.getString(KEY_CLOCK_STATE, "") ?: ""
        if (jsonString.isNotBlank()) {
            try {
                _state.value = json.decodeFromString<ScenarioClockData>(jsonString)
            } catch (_: Exception) { }
        }
    }

    private fun save() {
        prefs?.edit()?.apply {
            putString(KEY_CLOCK_STATE, json.encodeToString(_state.value))
            apply()
        }
    }

    private fun loadCalendars() {
        val calendarsJson = prefs?.getString(KEY_CALENDARS, "") ?: ""
        if (calendarsJson.isNotBlank()) {
            try {
                val loaded = json.decodeFromString<List<CalendarConfig>>(calendarsJson)
                if (loaded.isNotEmpty()) _calendars.value = loaded
            } catch (_: Exception) { }
        } else {
            // Migration depuis l'ancien calendrier unique (avant la prise en charge de plusieurs
            // calendriers nommés) : devient le premier calendrier de la liste.
            val legacyJson = prefs?.getString(KEY_CALENDAR_CONFIG_LEGACY, "") ?: ""
            if (legacyJson.isNotBlank()) {
                try {
                    _calendars.value = listOf(json.decodeFromString<CalendarConfig>(legacyJson).copy(nom = "Calendrier par défaut"))
                } catch (_: Exception) { }
            }
            saveCalendars()
        }
        val savedActiveId = prefs?.getString(KEY_ACTIVE_CALENDAR_ID, null)
        _activeCalendarId.value = _calendars.value.find { it.id == savedActiveId }?.id ?: _calendars.value.first().id
    }

    private fun saveCalendars() {
        prefs?.edit()?.apply {
            putString(KEY_CALENDARS, json.encodeToString(_calendars.value))
            apply()
        }
    }

    private fun saveActiveCalendarId() {
        prefs?.edit()?.apply {
            putString(KEY_ACTIVE_CALENDAR_ID, _activeCalendarId.value)
            apply()
        }
    }

    /** Crée un nouveau calendrier (copie des valeurs par défaut) et le retourne. */
    fun addCalendar(nom: String): CalendarConfig {
        val newCalendar = CalendarConfig(nom = nom.ifBlank { "Nouveau calendrier" })
        _calendars.value = _calendars.value + newCalendar
        saveCalendars()
        return newCalendar
    }

    /**
     * Ajoute le calendrier des Harpistes (Faerûn/D&D) préréglé, pour un MJ qui a déjà d'autres
     * calendriers et veut l'ajouter sans repartir du générique (celui-ci n'est le calendrier de
     * départ que pour une toute nouvelle installation, voir [_calendars]).
     */
    fun addHarptosCalendar(): CalendarConfig {
        val newCalendar = CalendarConfig.calendrierHarptos().copy(id = java.util.UUID.randomUUID().toString())
        _calendars.value = _calendars.value + newCalendar
        saveCalendars()
        return newCalendar
    }

    /** Met à jour un calendrier existant (par id) — utilisé par l'éditeur (noms, jours par mois...). */
    fun updateCalendar(config: CalendarConfig) {
        _calendars.value = _calendars.value.map { if (it.id == config.id) config else it }
        saveCalendars()
        if (config.id == _activeCalendarId.value) rebroadcast()
    }

    /** Supprime un calendrier — refusé s'il ne reste plus qu'un seul calendrier. */
    fun deleteCalendar(id: String) {
        if (_calendars.value.size <= 1) return
        _calendars.value = _calendars.value.filterNot { it.id == id }
        saveCalendars()
        if (_activeCalendarId.value == id) {
            _activeCalendarId.value = _calendars.value.first().id
            saveActiveCalendarId()
            rebroadcast()
        }
    }

    fun setActiveCalendarId(id: String) {
        if (_calendars.value.none { it.id == id }) return
        _activeCalendarId.value = id
        saveActiveCalendarId()
        rebroadcast()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            // Cadencé sur l'horloge système (et non un simple delay fixe cumulé) pour ne pas
            // dériver par rapport au temps réel au fil des minutes.
            var nextTick = System.currentTimeMillis() + TICK_MS
            while (true) {
                delay((nextTick - System.currentTimeMillis()).coerceAtLeast(0))
                val now = System.currentTimeMillis()
                // Gros retard (appareil en veille...) : on repart de maintenant plutôt que
                // d'enchaîner des centaines de tics (et de diffusions réseau) d'un coup.
                nextTick = if (now - nextTick > TICK_MS) now + TICK_MS else nextTick + TICK_MS
                val current = _state.value
                // Pendant un combat, le temps ne défile plus : chaque round l'avance de
                // [SECONDES_PAR_ROUND] (voir CombatSession).
                if (current.enabled && current.autoAdvanceEnabled && !_pauseCombat.value) {
                    advanceOneSecond()
                }
            }
        }
    }

    private fun advanceOneSecond() = advanceSeconds(1)

    /** Durée d'un round de combat dans les règles de D&D 5e : 6 secondes (10 rounds = 1 minute). */
    const val SECONDES_PAR_ROUND = 6

    // Combat en cours (côté MJ) : l'horloge ne défile plus en temps réel.
    private val _pauseCombat = MutableStateFlow(false)
    val pauseCombat: StateFlow<Boolean> = _pauseCombat.asStateFlow()

    fun setPauseCombat(enPause: Boolean) {
        _pauseCombat.value = enPause
    }

    /** Instant de fiction en secondes (référence des effets à durée, voir EffetActif). */
    fun totalSeconds(data: ScenarioClockData = _state.value): Long = data.scenarioMinutes * 60 + data.scenarioSeconds

    /** Avance l'horloge de [seconds] secondes de fiction (tic, round de combat...). */
    fun advanceSeconds(seconds: Int) {
        if (seconds <= 0) return
        val current = _state.value
        val total = current.scenarioSeconds + seconds
        val newMinutes = current.scenarioMinutes + total / 60
        val moved = current.copy(scenarioMinutes = newMinutes, scenarioSeconds = total % 60)
        applyLocal(if (current.autoWeatherEnabled && newMinutes != current.scenarioMinutes) rollWeatherIfDue(current, moved) else moved)
    }

    /** Météos tirées au hasard quand le mois n'a pas de climat défini (comportement historique). */
    private val meteosParDefaut = listOf(
        ScenarioWeather.CLAIR, ScenarioWeather.NUAGEUX, ScenarioWeather.PLUIE,
        ScenarioWeather.ORAGE, ScenarioWeather.NEIGE, ScenarioWeather.BROUILLARD,
    )

    /** Climat du mois en cours à [minutes] selon [config] (null = aucune contrainte). */
    fun climatCourant(minutes: Long, config: CalendarConfig = activeCalendar()): ClimatMois? {
        val (_, monthIndex, _) = dateForTotalDays(config, dayIndex(minutes) - 1)
        return config.climat(monthIndex)
    }

    /**
     * Tire une nouvelle météo (33% de chance) à chaque tranche de 3h de fiction franchie, parmi
     * celles permises par le climat du mois. Retirée d'office si la météo/température courante
     * n'est plus permise (changement de mois ou de saison).
     */
    private fun rollWeatherIfDue(previous: ScenarioClockData, next: ScenarioClockData): ScenarioClockData {
        val climat = climatCourant(next.scenarioMinutes)
        val meteos = climat?.meteos.orEmpty().ifEmpty { meteosParDefaut }
        val temperatures = climat?.temperatures.orEmpty()
        val horsClimat = next.weather !in meteos || (temperatures.isNotEmpty() && next.temperature !in temperatures)
        val boundaryCrossed = next.scenarioMinutes / 180 != previous.scenarioMinutes / 180
        if (!horsClimat && !(boundaryCrossed && (0 until 100).random() < 33)) return next
        return next.copy(
            weather = meteos.random(),
            temperature = if (temperatures.isNotEmpty()) temperatures.random() else next.temperature,
        )
    }

    /** Après un saut dans le temps (heure, date) : météo remise en accord avec la saison si automatique. */
    private fun withClimate(previous: ScenarioClockData, next: ScenarioClockData): ScenarioClockData =
        if (next.autoWeatherEnabled) rollWeatherIfDue(previous, next) else next

    private fun applyLocal(newState: ScenarioClockData) {
        _state.value = newState
        save()
        onLocalChange?.invoke(newState)
        syncAmbientSound(newState)
    }

    // === Actions MJ (fenêtre de gestion) ===

    fun setEnabled(enabled: Boolean) = applyLocal(_state.value.copy(enabled = enabled))
    fun setAutoAdvanceEnabled(enabled: Boolean) = applyLocal(_state.value.copy(autoAdvanceEnabled = enabled))
    fun setAutoWeatherEnabled(enabled: Boolean) = applyLocal(_state.value.copy(autoWeatherEnabled = enabled))
    fun setWeather(weather: ScenarioWeather) = applyLocal(_state.value.copy(weather = weather))
    fun setTemperature(temperature: ScenarioTemperature) = applyLocal(_state.value.copy(temperature = temperature))

    /** Avance/recule manuellement l'heure (ex. +1h, +10min), utilisé quand le défilement auto est coupé. */
    fun advanceManually(minutes: Long) {
        val current = _state.value
        applyLocal(withClimate(current, current.copy(scenarioMinutes = (current.scenarioMinutes + minutes).coerceAtLeast(0))))
    }

    /** Avance/recule manuellement le jour (ex. +1 jour), heure du jour conservée. */
    fun advanceDays(days: Long) {
        val current = _state.value
        applyLocal(withClimate(current, current.copy(scenarioMinutes = (current.scenarioMinutes + days * 1440).coerceAtLeast(0))))
    }

    /** Fixe l'heure du jour courant (jour de fiction conservé), pour un réglage initial précis. */
    fun setTimeOfDay(hour: Int, minute: Int) {
        val current = _state.value
        val dayStart = (current.scenarioMinutes / 1440) * 1440
        applyLocal(current.copy(scenarioMinutes = dayStart + hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59), scenarioSeconds = 0))
    }

    /**
     * Fixe directement une date calendaire (année, mois 1-indexé, jour du mois 1-indexé) selon
     * [calendarId] (ou le calendrier actif si null/inconnu), en conservant l'heure du jour
     * courante. Le temps écoulé ([ScenarioClockData.scenarioMinutes]) reste unique et partagé :
     * seul le calendrier utilisé pour interpréter la date choisie change.
     */
    fun setCurrentDate(year: Int, month: Int, dayOfMonth: Int, calendarId: String? = null) {
        val config = calendarId?.let { id -> _calendars.value.find { it.id == id } } ?: activeCalendar()
        val current = _state.value
        val timeOfDay = ((current.scenarioMinutes % 1440) + 1440) % 1440
        val totalDays = totalDaysForDate(config, year, month, dayOfMonth)
        applyLocal(withClimate(current, current.copy(scenarioMinutes = totalDays * 1440 + timeOfDay)))
    }

    // === Lecture ===

    fun hourOfDay(minutes: Long): Int = (((minutes % 1440) + 1440) % 1440 / 60).toInt()
    fun minuteOfHour(minutes: Long): Int = (((minutes % 60) + 60) % 60).toInt()

    /** Jour de fiction courant, 1-indexé pour l'affichage (jour 1 = début de la partie). */
    fun dayIndex(minutes: Long): Long = minutes / 1440 + 1

    fun formattedTime(minutes: Long): String = "%02d:%02d".format(hourOfDay(minutes), minuteOfHour(minutes))

    /** Heure avec secondes ("08:34:12"), pour l'affichage en temps réel. */
    fun formattedTime(minutes: Long, seconds: Int): String =
        "%s:%02d".format(formattedTime(minutes), seconds.coerceIn(0, 59))

    /** Jour + heure, largeur fixe ("J001 08:34") pour ne pas faire sauter le reste de la ligne à chaque tic. */
    fun formattedDateTime(minutes: Long): String = "J%03d %s".format(dayIndex(minutes), formattedTime(minutes))

    fun dayPeriod(hour: Int): DayPeriod = when (hour) {
        in 5..6 -> DayPeriod.AUBE
        in 7..17 -> DayPeriod.JOUR
        in 18..19 -> DayPeriod.CREPUSCULE
        else -> DayPeriod.NUIT
    }

    // === Calendrier ===

    /** (année, index de mois 0-indexé, jour du mois 1-indexé) pour le jour de fiction 0-indexé donné. */
    private fun dateForTotalDays(config: CalendarConfig, totalDays: Long): Triple<Int, Int, Int> {
        val yearLength = config.daysPerMonth.sum().toLong().coerceAtLeast(1)
        val year = config.startYear + Math.floorDiv(totalDays, yearLength).toInt()
        var dayInYear = Math.floorMod(totalDays, yearLength)
        var monthIndex = 0
        for ((index, length) in config.daysPerMonth.withIndex()) {
            if (dayInYear < length) {
                monthIndex = index
                break
            }
            dayInYear -= length
            monthIndex = index
        }
        return Triple(year, monthIndex, dayInYear.toInt() + 1)
    }

    /** Jour de fiction 0-indexé (depuis le début de la partie) pour une date calendaire donnée. */
    private fun totalDaysForDate(config: CalendarConfig, year: Int, month: Int, dayOfMonth: Int): Long {
        val yearLength = config.daysPerMonth.sum().toLong().coerceAtLeast(1)
        val monthIndex = (month - 1).coerceIn(0, config.daysPerMonth.size - 1)
        val daysBeforeMonth = config.daysPerMonth.take(monthIndex).sum()
        val dayInYear = daysBeforeMonth + (dayOfMonth - 1).coerceAtLeast(0)
        return (year - config.startYear).toLong() * yearLength + dayInYear
    }

    /** Nom du jour de la semaine pour le jour de fiction 0-indexé donné. */
    private fun weekdayName(config: CalendarConfig, totalDays: Long): String {
        if (config.dayNames.isEmpty()) return ""
        val index = Math.floorMod(totalDays, config.dayNames.size.toLong()).toInt()
        return config.dayNames[index]
    }

    /**
     * Date calendaire lisible ("Jour 3 12 Mois 5, an 1503 — 08:34") pour l'heure de fiction donnée,
     * selon [config] (par défaut le calendrier actif, [activeCalendar]). Remplace
     * [formattedDateTime] partout où un calendrier personnalisé est configuré.
     */
    fun formattedCalendarDate(minutes: Long, config: CalendarConfig = activeCalendar(), seconds: Int? = null): String {
        val time = seconds?.let { formattedTime(minutes, it) } ?: formattedTime(minutes)
        return "${formattedCalendarDay(minutes, config)} — $time"
    }

    /** Date calendaire seule, sans l'heure ("Jour 3 12 Mois 5, an 1503"). */
    fun formattedCalendarDay(minutes: Long, config: CalendarConfig = activeCalendar()): String {
        val totalDays = dayIndex(minutes) - 1
        val (year, monthIndex, dayOfMonth) = dateForTotalDays(config, totalDays)
        val monthName = config.monthNames.getOrElse(monthIndex) { "Mois ${monthIndex + 1}" }
        // Un nom de jour de semaine générique ("Jour 3", valeur par défaut et semaines du
        // calendrier des Harpistes) faisait doublon avec le jour du mois ("Jour 3 12 Mois 5") :
        // il n'est affiché que s'il a été réellement renommé (ex. "Lundi").
        val weekday = weekdayName(config, totalDays).takeUnless { it.isBlank() || GENERIC_DAY_NAME.matches(it.trim()) }
        return listOfNotNull(weekday, "$dayOfMonth $monthName, an $year").joinToString(" ")
    }

    /** Jour férié tombant à [minutes] dans [config], null s'il n'y en a pas. */
    fun jourFerie(minutes: Long, config: CalendarConfig = activeCalendar()): JourFerie? {
        val (_, monthIndex, dayOfMonth) = dateForTotalDays(config, dayIndex(minutes) - 1)
        return config.joursFeries.firstOrNull { it.mois == monthIndex && it.jour == dayOfMonth }
    }

    /** (année, mois 1-indexé, jour du mois 1-indexé) courants, pour préremplir un sélecteur de date. */
    fun currentDateParts(minutes: Long, config: CalendarConfig = activeCalendar()): Triple<Int, Int, Int> {
        val (year, monthIndex, dayOfMonth) = dateForTotalDays(config, dayIndex(minutes) - 1)
        return Triple(year, monthIndex + 1, dayOfMonth)
    }
}
