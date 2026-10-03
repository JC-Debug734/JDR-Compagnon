package com.jc2.jdrcompagnon.feature_carte.data

import android.content.Context

/**
 * Lieu de la ville où se trouve le joueur (menu latéral joueur), mémorisé sur l'appareil. En
 * arrivant dans une ville (autre que la dernière visitée), le joueur repart toujours de l'entrée
 * de la ville (lieu null) ; quitter la ville oublie la visite.
 */
object VisiteVille {
    private const val PREFS = "visite_ville"
    private const val CLE_VILLE = "villeId"
    private const val CLE_LIEU = "lieuId"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Id du lieu notable actuel dans [villeId], null = entrée de la ville. */
    fun lieuActuel(context: Context, villeId: String): String? =
        prefs(context).takeIf { it.getString(CLE_VILLE, null) == villeId }?.getString(CLE_LIEU, null)

    /** Arrivée dans [villeId] : repart de l'entrée, sauf si la visite de cette ville est en cours. */
    fun arriver(context: Context, villeId: String) {
        val p = prefs(context)
        if (p.getString(CLE_VILLE, null) != villeId) {
            p.edit().putString(CLE_VILLE, villeId).remove(CLE_LIEU).apply()
        }
    }

    fun aller(context: Context, villeId: String, lieuId: String?) {
        prefs(context).edit().putString(CLE_VILLE, villeId).putString(CLE_LIEU, lieuId).apply()
    }

    fun quitter(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
