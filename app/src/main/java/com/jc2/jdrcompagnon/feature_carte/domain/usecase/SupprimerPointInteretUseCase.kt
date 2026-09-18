package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository

class SupprimerPointInteretUseCase(private val repository: CarteRepository) {
    suspend operator fun invoke(id: String) {
        repository.supprimerPoint(id)
    }
}
