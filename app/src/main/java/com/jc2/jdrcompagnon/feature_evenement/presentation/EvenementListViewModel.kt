package com.jc2.jdrcompagnon.feature_evenement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** ViewModel de la bibliothèque d'événements (EvenementListScreen). */
class EvenementListViewModel(
    private val repository: EvenementRepository,
) : ViewModel() {

    fun observerEvenements(worldId: String): Flow<List<Evenement>> = repository.observerEvenements(worldId)

    fun sauvegarder(evenement: Evenement) {
        viewModelScope.launch { repository.sauvegarder(evenement) }
    }

    fun supprimer(id: String) {
        viewModelScope.launch { repository.supprimer(id) }
    }
}
