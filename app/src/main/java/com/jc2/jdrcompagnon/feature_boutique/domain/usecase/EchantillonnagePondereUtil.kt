package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import kotlin.math.pow
import kotlin.random.Random

/**
 * Échantillonnage pondéré sans remise (algorithme d'Efraimidis-Spirakis) : chaque élément reçoit
 * une clé aléatoire dépendant de son poids, puis on garde les n meilleures clés. Un poids plus
 * élevé augmente la probabilité d'être sélectionné, sans jamais garantir la sélection ni créer
 * de doublons.
 */
fun <T> List<T>.echantillonnerSansRemise(n: Int, poids: (T) -> Double, random: Random): List<T> {
    if (n >= size) return shuffled(random)
    return asSequence()
        .map { it to random.nextDouble().pow(1.0 / poids(it)) }
        .sortedByDescending { it.second }
        .take(n)
        .map { it.first }
        .toList()
}
