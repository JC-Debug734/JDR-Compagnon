package com.jc2.jdrcompagnon.feature_carte.domain.model

/** Une carte quadrillée par campagne (relation 1-1), créée avec des valeurs par défaut à la première ouverture. */
data class CarteCampagne(
    val campagneId: String,
    val largeurCases: Int = 20,
    val hauteurCases: Int = 15,
    val echelleKmParCase: Int = 10,
    val imageFileName: String? = null // image de fond choisie par le MJ (copiée en stockage interne), null = grille nue
)
