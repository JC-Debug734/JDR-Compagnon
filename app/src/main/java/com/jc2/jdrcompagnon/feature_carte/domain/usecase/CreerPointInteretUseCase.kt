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
        description: String = ""
    ): PointInteret {
        val point = PointInteret(
            id = UUID.randomUUID().toString(),
            campagneId = campagneId,
            nom = nom,
            type = type,
            x = x,
            y = y,
            description = description
        )
        repository.sauvegarderPoint(point)
        return point
    }
}
