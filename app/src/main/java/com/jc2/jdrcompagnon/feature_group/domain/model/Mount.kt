package com.jc2.jdrcompagnon.feature_group.domain.model

import kotlinx.serialization.Serializable

/** Monture (se chevauche, donne sa vitesse à son cavalier) ou simple animal de compagnie. */
@Serializable
enum class MountKind(val label: String) {
    MONTURE("Monture"),
    ANIMAL("Animal"),
}

/**
 * Monture ou animal du groupe.
 *
 * - [riderCharacterId] : PJ/PNJ qui la monte. Tant qu'il est en selle, sa vitesse
 *   (Character.speed) vaut [speed] ; sa vitesse d'origine est gardée dans [riderBaseSpeed] pour
 *   lui être rendue quand il descend (voir GameState.assignMountRider).
 * - [transportId] : chariot (Transport) auquel elle est attelée. Une monture attelée ne peut pas
 *   être montée en même temps.
 * - [location] : lieu où elle se trouve (ex. « Phandalin »), vide = avec le groupe.
 * - [bagages] : objets rangés dans ses sacoches, déposés depuis le sac d'un personnage (voir
 *   GameState.rangerSurMonture / reprendreDeMonture).
 */
@Serializable
data class Mount(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val species: String = "",
    val riderCharacterId: String? = null,
    val kind: MountKind = MountKind.MONTURE,
    // Vitesse de déplacement en mètres (comme Character.speed), ex. cheval de selle 18 m.
    val speed: Int = 18,
    val riderBaseSpeed: Int? = null,
    val transportId: String? = null,
    val location: String = "",
    val bagages: List<String> = emptyList(),
)
