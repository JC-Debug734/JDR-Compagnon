package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.di.TableAleatoireDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Session de lecture de scénario : [scenarioId] null tant qu'aucun scénario n'a été ouvert en
 * lecture depuis le lancement de l'app. [minutesAccumulees] cumule le temps de scénario écoulé
 * pendant les périodes de lecture passées ; la période en cours (si [enLecture]) court depuis
 * [minuteReference] (valeur de ScenarioClockState au moment où la lecture a repris).
 */
data class LectureScenarioData(
    val scenarioId: String? = null,
    val enLecture: Boolean = false,
    val minutesAccumulees: Long = 0,
    val minuteReference: Long = 0,
) {
    fun minutesEcoulees(minutesHorloge: Long): Long? {
        if (scenarioId == null) return null
        val enCours = if (enLecture) minutesHorloge - minuteReference else 0
        return (minutesAccumulees + enCours).coerceAtLeast(0)
    }
}

/**
 * Compteur de temps utilisé par les tables aléatoires (feature_table_aleatoire) : il ne compte
 * que pendant qu'un scénario est affiché en lecture (ScenarioReaderContent), en temps de
 * scénario (ScenarioClockState). L'ouverture d'un scénario remet le compteur à zéro, ainsi que
 * le dernier déclenchement de chaque table, pour que chaque table reparte de son intervalle
 * complet. En dehors de toute lecture (aucun scénario ouvert), aucune table ne se déclenche.
 */
object LectureScenarioState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(LectureScenarioData())
    val state: StateFlow<LectureScenarioData> = _state.asStateFlow()

    /** Minutes de scénario comptées depuis l'ouverture du scénario ; null si aucun n'est ouvert. */
    val minutesEcoulees: StateFlow<Long?> = combine(_state, ScenarioClockState.state) { lecture, horloge ->
        lecture.minutesEcoulees(horloge.scenarioMinutes)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun minutesEcouleesMaintenant(): Long? =
        _state.value.minutesEcoulees(ScenarioClockState.state.value.scenarioMinutes)

    /** Ouverture d'un scénario : compteur à zéro et tables réarmées. */
    fun ouvrir(scenarioId: String) {
        _state.value = LectureScenarioData(
            scenarioId = scenarioId,
            enLecture = true,
            minuteReference = horlogeMinutes(),
        )
        scope.launch { TableAleatoireDependencies.repository.reinitialiserDeclenchements() }
    }

    /** Retour sur le scénario déjà ouvert (après un passage par un autre écran) : le comptage reprend. */
    fun reprendre() {
        _state.update { if (it.scenarioId == null || it.enLecture) it else it.copy(enLecture = true, minuteReference = horlogeMinutes()) }
    }

    /** Le lecteur n'est plus affiché : le temps écoulé est figé jusqu'à la reprise. */
    fun suspendre() {
        _state.update { lecture ->
            if (!lecture.enLecture) lecture
            else lecture.copy(
                enLecture = false,
                minutesAccumulees = lecture.minutesEcoulees(horlogeMinutes()) ?: 0,
            )
        }
    }

    private fun horlogeMinutes(): Long = ScenarioClockState.state.value.scenarioMinutes
}
