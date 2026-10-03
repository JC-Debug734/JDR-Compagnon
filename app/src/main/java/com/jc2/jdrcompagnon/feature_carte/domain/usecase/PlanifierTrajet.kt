package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import kotlin.math.ceil
import kotlin.math.hypot

/** Arrêt de fin de journée sur un trajet : position (fractions de la carte) et distance parcourue. */
data class ArretTrajet(val numero: Int, val fx: Float, val fy: Float, val kmParcourus: Double)

data class PlanTrajet(
    val distanceKm: Double,
    val kmParHeure: Double,
    val heuresMaxParJour: Int,
    val arrets: List<ArretTrajet>,
) {
    val heures: Double get() = if (kmParHeure > 0) distanceKm / kmParHeure else 0.0
    /** Jours de marche entamés (1 jour = [heuresMaxParJour] heures de déplacement au plus). */
    val jours: Int get() = if (heures <= 0.0) 0 else ceil(heures / heuresMaxParJour).toInt()
    val kmParJour: Double get() = kmParHeure * heuresMaxParJour
}

/**
 * Trajet tracé sur une carte : distance réelle, durée à la vitesse du groupe, et arrêts à la fin
 * de chaque journée de marche (toutes les [heuresMaxParJour] heures), placés sur le tracé.
 *
 * Les positions sont en fractions de la carte ; [casesEnLargeur]/[casesEnHauteur] convertissent en
 * cases, [echelleKmParCase] en kilomètres (distance euclidienne, comme CalculerTempsTrajetUseCase).
 */
object PlanifierTrajet {

    /**
     * Vitesse de voyage (km/h) déduite de la vitesse de déplacement en mètres, selon la règle 5e
     * « 30 pieds = 3 miles par heure » : 9 m → 4,8 km/h, 18 m (cheval) → 9,6 km/h.
     */
    fun kmParHeure(vitesseM: Int): Double = vitesseM * 4.8 / 9.0

    fun planifier(
        etapes: List<Pair<Float, Float>>,
        casesEnLargeur: Float,
        casesEnHauteur: Float,
        echelleKmParCase: Int,
        kmParHeure: Double,
        heuresMaxParJour: Int,
    ): PlanTrajet {
        val segments = etapes.zipWithNext { a, b ->
            Triple(a, b, hypot(((b.first - a.first) * casesEnLargeur).toDouble(), ((b.second - a.second) * casesEnHauteur).toDouble()) * echelleKmParCase)
        }
        val total = segments.sumOf { it.third }
        val parJour = kmParHeure * heuresMaxParJour
        val arrets = mutableListOf<ArretTrajet>()
        if (parJour > 0) {
            var cible = parJour
            var numero = 1
            var cumul = 0.0
            // Un arrêt tombant pile à l'arrivée n'en est pas un (tolérance de 10 m).
            while (cible < total - 0.01) {
                // Segment qui contient la distance cible.
                for ((a, b, longueur) in segments) {
                    if (cumul + longueur >= cible && longueur > 0) {
                        val t = ((cible - cumul) / longueur).toFloat()
                        arrets += ArretTrajet(numero, a.first + (b.first - a.first) * t, a.second + (b.second - a.second) * t, cible)
                        break
                    }
                    cumul += longueur
                }
                cumul = 0.0
                cible += parJour
                numero++
            }
        }
        return PlanTrajet(total, kmParHeure, heuresMaxParJour, arrets)
    }
}
