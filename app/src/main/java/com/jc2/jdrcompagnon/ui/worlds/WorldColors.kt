package com.jc2.jdrcompagnon.ui.worlds

import androidx.compose.ui.graphics.Color

/**
 * Parse une couleur hexadécimale ("#RRGGBB" ou "#AARRGGBB", le `#` étant optionnel) telle
 * que déclarée dans le `reference.md` d'un univers importé (voir [ReferenceMdParser]).
 * Retourne `null` si la valeur est absente ou mal formée, auquel cas l'appelant retombe sur
 * une couleur par défaut plutôt que de planter.
 */
fun String?.toWorldColorOrNull(): Color? {
    if (this.isNullOrBlank()) return null
    val hex = removePrefix("#").trim()
    if (hex.length != 6 && hex.length != 8) return null
    return try {
        val argb = if (hex.length == 6) "FF$hex" else hex
        Color(argb.toLong(16).toInt())
    } catch (e: NumberFormatException) {
        null
    }
}
