package com.jc2.jdrcompagnon.feature_boutique.domain.model

/**
 * Service rendu par une boutique de type != MARCHAND (nuitée, soin, recrutement, etc.).
 */
data class Service(
    val nom: String,
    val description: String,
    val prixEnPo: Int,
    val quantiteDisponible: Int? = null, // null = illimité (ex: repas), sinon places limitées (ex: chambres)
    val actif: Boolean = true // le MJ peut désactiver un service sans le supprimer (ex: chambres toutes prises)
)
