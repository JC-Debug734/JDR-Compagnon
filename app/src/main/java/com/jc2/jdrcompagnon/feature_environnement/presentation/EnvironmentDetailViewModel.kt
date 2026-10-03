package com.jc2.jdrcompagnon.feature_environnement.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironnementRepository
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** ViewModel de l'écran détail (EnvironmentDetailScreen) : charge, puis republie à chaque modification. */
class EnvironmentDetailViewModel(
    private val environnementId: String,
    private val repository: EnvironnementRepository
) : ViewModel() {

    private val _environnement = MutableStateFlow<Environnement?>(null)
    val environnement: StateFlow<Environnement?> = _environnement

    init {
        viewModelScope.launch {
            _environnement.value = repository.getEnvironnementParId(environnementId)
        }
    }

    fun mettreAJour(updated: Environnement) {
        _environnement.value = updated
        viewModelScope.launch { repository.sauvegarder(updated) }
    }
}
