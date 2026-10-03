package com.jc2.jdrcompagnon.feature_boutique.domain.model

import kotlinx.serialization.Serializable

/**
 * Service rendu par une boutique (nuitée, soin, recrutement, réparation, etc.). Les boutiques
 * non MARCHAND en reçoivent à la génération ; le MJ peut en ajouter à toute boutique.
 */
@Serializable
data class Service(
    val nom: String,
    val description: String,
    val prixEnPo: Int,
    val quantiteDisponible: Int? = null, // null = illimité (ex: repas), sinon places limitées (ex: chambres)
    val actif: Boolean = true, // le MJ peut désactiver un service sans le supprimer (ex: chambres toutes prises)
    // Ajouté ou modifié à la main par le MJ : conservé lors d'une nouvelle visite ou d'un
    // changement de type/standing (seuls les services générés sont renouvelés).
    val personnalise: Boolean = false
)
