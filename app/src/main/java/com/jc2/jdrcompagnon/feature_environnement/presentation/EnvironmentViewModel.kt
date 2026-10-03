package com.jc2.jdrcompagnon.feature_environnement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironnementRepository
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** ViewModel de l'écran liste (EnvironmentListScreen) : mêmes responsabilités que BoutiqueViewModel. */
class EnvironmentViewModel(
    private val repository: EnvironnementRepository
) : ViewModel() {

    fun observerEnvironnements(worldId: String): Flow<List<Environnement>> =
        repository.observerEnvironnements(worldId)

    fun creer(nom: String, worldId: String) {
        viewModelScope.launch {
            repository.sauvegarder(Environnement(nom = nom, worldId = worldId))
        }
    }

    fun supprimer(id: String) {
        viewModelScope.launch { repository.supprimer(id) }
    }
}
