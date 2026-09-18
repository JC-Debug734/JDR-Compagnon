package com.jc2.jdrcompagnon.feature_boutique.domain.model

/**
 * Rôle purement descriptif (aucun impact mécanique/combat).
 * Sert uniquement à l'affichage et à la saveur narrative de la boutique.
 */
enum class RoleEmploye(val label: String) {
    VENDEUR("Vendeur"),
    APPRENTI("Apprenti"),
    COMPTABLE("Comptable"),
    GARDE("Garde"),
    ARTISAN("Artisan")
}