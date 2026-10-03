package com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model

/** Résultat d'un tirage de loot : l'entrée tirée et la quantité effectivement obtenue. */
data class ResultatLoot(val entree: EntreeLoot, val quantite: Int)
