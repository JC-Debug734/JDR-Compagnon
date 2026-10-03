package com.jc2.jdrcompagnon.di

import com.jc2.jdrcompagnon.feature_historique.data.HistoriqueRepository
import com.jc2.jdrcompagnon.feature_historique.data.HistoriqueRepositoryImpl

/**
 * Même convention que [BoutiqueDependencies] (pas de Hilt/Koin dans le projet). Réutilise la
 * base Room partagée initialisée par BoutiqueDependencies.init (voir GameState.init).
 */
object HistoriqueDependencies {
    val repository: HistoriqueRepository by lazy {
        HistoriqueRepositoryImpl(BoutiqueDependencies.requireDatabase().historiqueDao())
    }
}
