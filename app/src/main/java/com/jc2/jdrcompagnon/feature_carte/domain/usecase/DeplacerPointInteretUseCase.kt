package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret

class DeplacerPointInteretUseCase(private val repository: CarteRepository) {
    suspend operator fun invoke(point: PointInteret, nouveauX: Int, nouveauY: Int) {
        repository.sauvegarderPoint(point.copy(x = nouveauX, y = nouveauY))
    }
}
