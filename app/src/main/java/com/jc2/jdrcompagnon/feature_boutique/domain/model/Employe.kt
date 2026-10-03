package com.jc2.jdrcompagnon.feature_boutique.domain.model

import kotlinx.serialization.Serializable

/** Employé purement descriptif : pas de stats, pas de combat. */
@Serializable
data class Employe(
    val nom: String,
    val role: RoleEmploye,
    val trait: String = "" // courte description de caractère générée (ex: "gentil et peu enclin à la négociation")
)