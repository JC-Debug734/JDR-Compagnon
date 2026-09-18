package com.jc2.jdrcompagnon.feature_carte.domain.model

/** Choisie manuellement par le MJ au moment du calcul de trajet, pas dérivée des montures du groupe. */
enum class VitesseDeplacement(val label: String, val kmParJour: Int) {
    A_PIED(label = "À pied", kmParJour = 30),
    CHARIOT(label = "Chariot", kmParJour = 40),
    MONTURE(label = "Monture", kmParJour = 60)
}
