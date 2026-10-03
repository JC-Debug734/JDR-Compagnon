package com.jc2.jdrcompagnon.feature_boutique.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Marchand(
    val nom: String,
    val description: String = "",
    val trait: String = "" // courte description de caractère générée (ex: "gentil et peu enclin à la négociation")
)

@Serializable
data class Boutique(
    val id: String,
    val nom: String,
    val standing: StandingBoutique,
    val marchand: Marchand,
    val employes: List<Employe>,
    val inventaire: List<ArticleEnVente>,
    val type: TypeBoutique = TypeBoutique.MARCHAND,
    val services: List<Service> = emptyList(),
    val argentDisponibleEnPo: Int = 0,
    val villeId: String? = null // lien optionnel vers un PointInteret (feature_carte) de type VILLE
)
