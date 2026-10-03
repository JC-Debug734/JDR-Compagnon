package com.jc2.jdrcompagnon.feature_group.domain.model

import kotlinx.serialization.Serializable

/**
 * Véhicule du groupe (chariot, charrette, calèche, barque...). Un véhicule [needsMount] ne se
 * déplace que si au moins une monture lui est attelée (Mount.transportId) : il avance alors à la
 * vitesse de la plus lente d'entre elles (voir [vitesse]).
 */
@Serializable
data class Transport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String = "",
    val capacity: Int = 0,
    val location: String = "",
    // false pour une barque, un navire... qui ne se tirent pas.
    val needsMount: Boolean = true,
    // Vitesse propre (mètres) d'un véhicule qui n'a pas besoin de monture.
    val ownSpeed: Int = 0,
) {
    /** Vitesse du véhicule selon les montures attelées, null = immobile. */
    fun vitesse(mounts: List<Mount>): Int? {
        if (!needsMount) return ownSpeed.takeIf { it > 0 }
        return mounts.filter { it.transportId == id }.minOfOrNull { it.speed }
    }
}
