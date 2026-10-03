package com.jc2.jdrcompagnon.feature_epreuve.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveRepository
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** ViewModel de l'outil ÉPREUVES (EpreuveListScreen) : liste, création/modification, suppression. */
class EpreuveListViewModel(
    private val repository: EpreuveRepository,
) : ViewModel() {

    fun observerEpreuves(worldId: String): Flow<List<Epreuve>> = repository.observerEpreuves(worldId)

    fun sauvegarder(epreuve: Epreuve) {
        viewModelScope.launch { repository.sauvegarder(epreuve) }
        EpreuveOutilSession.epreuveModifiee(epreuve)
    }

    /** [onSupprimee] : nettoyage de l'image (fichier local, voir EpreuveImageStore). */
    fun supprimer(epreuve: Epreuve, onSupprimee: () -> Unit = {}) {
        viewModelScope.launch {
            repository.supprimer(epreuve.id)
            onSupprimee()
        }
    }
}
