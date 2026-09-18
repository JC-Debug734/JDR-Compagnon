package com.jc2.jdrcompagnon.feature_boutique.domain.model

/** Employé purement descriptif : pas de stats, pas de combat. */
data class Employe(
    val nom: String,
    val role: RoleEmploye,
    val trait: String = "" // courte description de caractère générée (ex: "gentil et peu enclin à la négociation")
)