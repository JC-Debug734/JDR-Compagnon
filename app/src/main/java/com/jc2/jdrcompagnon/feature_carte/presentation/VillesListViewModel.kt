package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.CreerPointInteretUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Liste des villes d'une campagne, gérable sans passer par la carte (menu dédié). */
class VillesListViewModel(
    private val campagneId: String,
    private val repository: CarteRepository,
    private val creerPointInteret: CreerPointInteretUseCase
) : ViewModel() {

    val villes: StateFlow<List<PointInteret>> = repository.observerPoints(campagneId)
        .map { points -> points.filter { it.type == TypePointInteret.VILLE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Créée sans être placée sur une carte (carteId null) : le MJ la place lui-même ensuite,
     * sur la carte de son choix, depuis l'écran Carte ("Placer un lieu existant"). */
    fun onCreerVille(nom: String, description: String) {
        viewModelScope.launch {
            creerPointInteret(campagneId, nom, TypePointInteret.VILLE, x = 0, y = 0, description = description, carteId = null)
        }
    }
}
