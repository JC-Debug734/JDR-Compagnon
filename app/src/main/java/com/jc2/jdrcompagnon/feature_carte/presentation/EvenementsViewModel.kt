package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [propres] : événements de la bibliothèque marqués pour cette campagne ; [communs] : ceux du monde. */
data class EvenementsUiState(
    val propres: List<Evenement> = emptyList(),
    val communs: List<Evenement> = emptyList(),
)

/**
 * Événements de campagne : vue de la bibliothèque (feature_evenement) limitée à la campagne et
 * aux événements communs de son monde, avec tirage parmi eux.
 */
class EvenementsViewModel(
    private val campagneId: String,
    val worldId: String,
    private val repository: EvenementRepository,
) : ViewModel() {

    val uiState: StateFlow<EvenementsUiState> = repository.observerEvenements(worldId)
        .map { evenements ->
            EvenementsUiState(
                propres = evenements.filter { it.campagneId == campagneId },
                communs = evenements.filter { it.campagneId == null }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EvenementsUiState())

    private val _dernierTirage = MutableStateFlow<Evenement?>(null)
    val dernierTirage: StateFlow<Evenement?> = _dernierTirage

    fun onSauvegarder(evenement: Evenement) {
        viewModelScope.launch { repository.sauvegarder(evenement) }
    }

    fun onSupprimer(id: String) {
        viewModelScope.launch { repository.supprimer(id) }
    }

    /** Tire parmi les événements de la campagne et les communs, filtrés par [type] si fourni. */
    fun onTirerEvenement(type: TypeEvenement?) {
        val etat = uiState.value
        _dernierTirage.value = (etat.propres + etat.communs).filter { type == null || it.type == type }.randomOrNull()
    }

    fun clearDernierTirage() {
        _dernierTirage.value = null
    }
}
