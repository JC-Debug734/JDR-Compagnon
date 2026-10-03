package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import java.util.UUID

class CreerPointInteretUseCase(private val repository: CarteRepository) {
    suspend operator fun invoke(
        campagneId: String,
        nom: String,
        type: TypePointInteret,
        x: Int,
        y: Int,
        description: String = "",
        carteId: String? = null,
        // Position libre du centre de l'icône (fractions de la carte), voir PointInteret.fx.
        fx: Float? = null,
        fy: Float? = null
    ): PointInteret {
        val point = PointInteret(
            id = UUID.randomUUID().toString(),
            campagneId = campagneId,
            nom = nom,
            type = type,
            x = x,
            y = y,
            description = description,
            carteId = carteId,
            fx = fx,
            fy = fy
        )
        repository.sauvegarderPoint(point)
        return point
    }
}
