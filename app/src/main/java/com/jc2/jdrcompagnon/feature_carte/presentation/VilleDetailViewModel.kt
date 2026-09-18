package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.DelierBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.LierBoutiqueAVilleUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.TirerEvenementAleatoireUseCase
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
        val evenements: List<EvenementAleatoire>,
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
    private val tirerEvenementAleatoire: TirerEvenementAleatoireUseCase,
    private val mondeActif: () -> String
) : ViewModel() {

    val uiState: StateFlow<VilleDetailUiState> = combine(
        repository.observerPoints(campagneId).map { it.firstOrNull { p -> p.id == villeId } },
        boutiqueRepository.observerToutesLesBoutiques(),
        repository.observerLieuxNotables(villeId),
        repository.observerEvenementsDeVille(villeId),
        GameState.mjScenarios
    ) { ville, boutiques, lieux, evenements, scenarios ->
        if (ville == null) {
            VilleDetailUiState.Introuvable
        } else {
            VilleDetailUiState.Success(
                ville = ville,
                toutesLesBoutiques = boutiques,
                lieuxNotables = lieux,
                evenements = evenements,
                scenariosMonde = scenarios.filter { it.worldId == mondeActif() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VilleDetailUiState.Loading)

    private val _dernierTirage = MutableStateFlow<EvenementAleatoire?>(null)
    val dernierTirage: StateFlow<EvenementAleatoire?> = _dernierTirage

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

    fun onSupprimerLieu(id: String) {
        viewModelScope.launch { repository.supprimerLieuNotable(id) }
    }

    fun onAjouterEvenement(titre: String, description: String, effets: List<EffetEvenement>) {
        viewModelScope.launch {
            repository.sauvegarderEvenement(
                EvenementAleatoire(
                    id = UUID.randomUUID().toString(),
                    campagneId = campagneId,
                    villeId = villeId,
                    titre = titre,
                    description = description,
                    effets = effets
                )
            )
        }
    }

    fun onSupprimerEvenement(id: String) {
        viewModelScope.launch { repository.supprimerEvenement(id) }
    }

    fun onTirerEvenement() {
        val evenementsVille = (uiState.value as? VilleDetailUiState.Success)?.evenements ?: emptyList()
        _dernierTirage.value = tirerEvenementAleatoire(evenementsVille)
    }

    fun clearDernierTirage() {
        _dernierTirage.value = null
    }

    fun onAppliquerReputation(groupeId: String, factionNom: String, delta: Int) {
        appliquerReputationAuGroupe(groupeId, factionNom, delta)
    }
}
