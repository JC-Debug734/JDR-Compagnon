package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.VitesseDeplacement
import kotlin.math.hypot

data class ResultatTrajet(val distanceEnKm: Double, val joursDeTrajet: Double, val heuresDeTrajet: Double)

/**
 * Distance euclidienne en cases × échelle de la carte, convertie en temps de trajet selon la
 * vitesse choisie. [VitesseDeplacement.kmParJour] suit la règle D&D 5e d'un rythme normal sur
 * 8h de marche effective par jour (voyage), d'où la conversion jours -> heures ci-dessous.
 */
class CalculerTempsTrajetUseCase {
    private companion object {
        const val HEURES_DE_TRAJET_PAR_JOUR = 8.0
    }

    operator fun invoke(
        pointA: PointInteret,
        pointB: PointInteret,
        carte: CarteCampagne,
        vitesse: VitesseDeplacement
    ): ResultatTrajet = invoke(pointA.x to pointA.y, pointB.x to pointB.y, carte, vitesse)

    /**
     * [caseA] / [caseB] : (colonne, ligne) de la case sous le centre de chaque icône — les lieux
     * sont placés librement, une icône à cheval sur plusieurs cases compte pour celle de son centre.
     */
    operator fun invoke(
        caseA: Pair<Int, Int>,
        caseB: Pair<Int, Int>,
        carte: CarteCampagne,
        vitesse: VitesseDeplacement
    ): ResultatTrajet = invoke(listOf(caseA, caseB), carte, vitesse)

    /** Tracé à plusieurs étapes : somme des distances entre cases successives. */
    operator fun invoke(
        etapes: List<Pair<Int, Int>>,
        carte: CarteCampagne,
        vitesse: VitesseDeplacement
    ): ResultatTrajet {
        val distanceEnCases = etapes.zipWithNext { a, b ->
            hypot((b.first - a.first).toDouble(), (b.second - a.second).toDouble())
        }.sum()
        val distanceEnKm = distanceEnCases * carte.echelleKmParCase
        val joursDeTrajet = distanceEnKm / vitesse.kmParJour
        return ResultatTrajet(
            distanceEnKm = distanceEnKm,
            joursDeTrajet = joursDeTrajet,
            heuresDeTrajet = joursDeTrajet * HEURES_DE_TRAJET_PAR_JOUR
        )
    }
}
