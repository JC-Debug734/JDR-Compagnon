package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.DelierBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.LierBoutiqueAVilleUseCase
import com.jc2.jdrcompagnon.ui.GameState
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VilleDetailUiState {
    data object Loading : VilleDetailUiState
    data object Introuvable : VilleDetailUiState
    data class Success(
        val ville: PointInteret,
        val toutesLesBoutiques: List<Boutique>,
        val lieuxNotables: List<LieuNotable>,
        val scenariosMonde: List<GameState.MjScenario>
    ) : VilleDetailUiState
}

class VilleDetailViewModel(
    private val campagneId: String,
    private val villeId: String,
    private val repository: CarteRepository,
    private val boutiqueRepository: BoutiqueRepository,
    private val lierBoutiqueAVille: LierBoutiqueAVilleUseCase,
    private val delierBoutique: DelierBoutiqueUseCase,
    private val mondeActif: () -> String
) : ViewModel() {

    val uiState: StateFlow<VilleDetailUiState> = combine(
        repository.observerPoints(campagneId).map { it.firstOrNull { p -> p.id == villeId } },
        boutiqueRepository.observerToutesLesBoutiques(),
        repository.observerLieuxNotables(villeId),
        GameState.mjScenarios
    ) { ville, boutiques, lieux, scenarios ->
        if (ville == null) {
            VilleDetailUiState.Introuvable
        } else {
            VilleDetailUiState.Success(
                ville = ville,
                toutesLesBoutiques = boutiques,
                lieuxNotables = lieux,
                scenariosMonde = scenarios.filter { it.worldId == mondeActif() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VilleDetailUiState.Loading)

    fun onRenommer(ville: PointInteret, nom: String, description: String) {
        viewModelScope.launch { repository.sauvegarderPoint(ville.copy(nom = nom, description = description)) }
    }

    fun onLierBoutique(ville: PointInteret, boutique: Boutique) {
        viewModelScope.launch { lierBoutiqueAVille(ville, boutique) }
    }

    fun onDelierBoutique(ville: PointInteret, boutique: Boutique) {
        viewModelScope.launch { delierBoutique(ville, boutique) }
    }

    fun onAttacherScenario(ville: PointInteret, scenarioId: String) {
        viewModelScope.launch {
            if (scenarioId !in ville.scenarioIds) {
                repository.sauvegarderPoint(ville.copy(scenarioIds = ville.scenarioIds + scenarioId))
            }
        }
    }

    fun onDetacherScenario(ville: PointInteret, scenarioId: String) {
        viewModelScope.launch { repository.sauvegarderPoint(ville.copy(scenarioIds = ville.scenarioIds - scenarioId)) }
    }

    fun onAjouterLieu(nom: String, description: String) {
        viewModelScope.launch {
            repository.sauvegarderLieuNotable(LieuNotable(id = UUID.randomUUID().toString(), villeId = villeId, nom = nom, description = description))
        }
    }

    fun onModifierLieu(lieu: LieuNotable) {
        viewModelScope.launch { repository.sauvegarderLieuNotable(lieu) }
    }

    fun onSupprimerLieu(id: String) {
        viewModelScope.launch { repository.supprimerLieuNotable(id) }
    }

    /** Événements de la bibliothèque rattachés à la ville (voir EvenementsLiesSection). */
    fun onChangerEvenements(evenementIds: List<String>) {
        viewModelScope.launch { repository.definirEvenementsDuPoint(villeId, evenementIds) }
    }
}
