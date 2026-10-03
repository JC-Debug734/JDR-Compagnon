package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret

/** Place librement le centre de l'icône en ([fx], [fy]), fractions de la carte (0..1). */
class DeplacerPointInteretUseCase(private val repository: CarteRepository) {
    suspend operator fun invoke(point: PointInteret, fx: Float, fy: Float) {
        repository.deplacerPoint(point.id, fx.coerceIn(0f, 1f), fy.coerceIn(0f, 1f))
    }
}
