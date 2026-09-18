package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.VitesseDeplacement
import kotlin.math.hypot

data class ResultatTrajet(val distanceEnKm: Double, val joursDeTrajet: Double)

/** Distance euclidienne en cases × échelle de la carte, convertie en jours selon la vitesse choisie. */
class CalculerTempsTrajetUseCase {
    operator fun invoke(
        pointA: PointInteret,
        pointB: PointInteret,
        carte: CarteCampagne,
        vitesse: VitesseDeplacement
    ): ResultatTrajet {
        val distanceEnCases = hypot((pointB.x - pointA.x).toDouble(), (pointB.y - pointA.y).toDouble())
        val distanceEnKm = distanceEnCases * carte.echelleKmParCase
        return ResultatTrajet(
            distanceEnKm = distanceEnKm,
            joursDeTrajet = distanceEnKm / vitesse.kmParJour
        )
    }
}
