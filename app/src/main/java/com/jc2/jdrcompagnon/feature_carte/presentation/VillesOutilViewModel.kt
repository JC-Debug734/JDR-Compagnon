package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.CreerPointInteretUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.SupprimerPointInteretUseCase
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ville listée par l'outil VILLES ; [nomCampagne] null = ville de la bibliothèque (sans campagne). */
data class VilleOutil(val ville: PointInteret, val nomCampagne: String?)

/**
 * Outil VILLES : bibliothèque des villes, hors de toute campagne, et villes des campagnes du monde
 * courant (seulement celles de la campagne en cours quand une campagne est sélectionnée). Une
 * ville créée ici n'appartient à aucune campagne : une campagne l'ajoute ensuite comme ville
 * existante (voir AjouterVilleACampagneUseCase).
 */
class VillesOutilViewModel(
    private val repository: CarteRepository,
    private val creerPointInteret: CreerPointInteretUseCase,
    private val supprimerPointInteret: SupprimerPointInteretUseCase,
    private val mondeActif: () -> String?,
) : ViewModel() {

    val villes: StateFlow<List<VilleOutil>> = combine(
        repository.observerToutesLesVilles(),
        GameState.mjCampaigns,
        GameState.currentCampaignId,
    ) { villes, campagnes, campagneCourante ->
        val campagnesDuMonde = campagnes.filter { it.worldId == mondeActif().orEmpty() }.associateBy { it.id }
        villes.mapNotNull { ville ->
            when {
                ville.campagneId == PointInteret.SANS_CAMPAGNE -> VilleOutil(ville, null)
                campagneCourante != null && ville.campagneId != campagneCourante -> null
                else -> campagnesDuMonde[ville.campagneId]?.let { VilleOutil(ville, it.title) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onCreerVille(nom: String, description: String) {
        viewModelScope.launch {
            creerPointInteret(PointInteret.SANS_CAMPAGNE, nom.trim(), TypePointInteret.VILLE, x = 0, y = 0, description = description, carteId = null)
        }
    }

    fun onSupprimerVille(id: String) {
        viewModelScope.launch { supprimerPointInteret(id) }
    }
}
