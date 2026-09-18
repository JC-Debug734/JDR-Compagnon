package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.GenererEvenementsGeneriquesUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.TirerEvenementAleatoireUseCase
import com.jc2.jdrcompagnon.ui.GameState
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EvenementsUiState(
    val generiques: List<EvenementAleatoire>,
    val personnalises: List<EvenementAleatoire>,
    val groupes: List<GameState.MjGroup>
)

class EvenementsViewModel(
    private val campagneId: String,
    private val repository: CarteRepository,
    private val genererEvenementsGeneriques: GenererEvenementsGeneriquesUseCase,
    private val tirerEvenementAleatoire: TirerEvenementAleatoireUseCase
) : ViewModel() {

    val uiState: StateFlow<EvenementsUiState> = combine(
        repository.observerEvenementsCustom(campagneId),
        GameState.mjGroups
    ) { personnalises, groupes ->
        EvenementsUiState(generiques = genererEvenementsGeneriques(), personnalises = personnalises, groupes = groupes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EvenementsUiState(genererEvenementsGeneriques(), emptyList(), GameState.mjGroups.value)
    )

    private val _dernierTirage = MutableStateFlow<EvenementAleatoire?>(null)
    val dernierTirage: StateFlow<EvenementAleatoire?> = _dernierTirage

    fun onAjouterEvenement(titre: String, description: String, effets: List<EffetEvenement>) {
        viewModelScope.launch {
            repository.sauvegarderEvenement(
                EvenementAleatoire(id = UUID.randomUUID().toString(), campagneId = campagneId, titre = titre, description = description, effets = effets)
            )
        }
    }

    fun onSupprimerEvenement(id: String) {
        viewModelScope.launch { repository.supprimerEvenement(id) }
    }

    fun onTirerEvenement() {
        val personnalises = uiState.value.personnalises
        _dernierTirage.value = tirerEvenementAleatoire(personnalises)
    }

    fun clearDernierTirage() {
        _dernierTirage.value = null
    }

    fun onAppliquerReputation(groupeId: String, factionNom: String, delta: Int) {
        appliquerReputationAuGroupe(groupeId, factionNom, delta)
    }
}
