package com.jc2.jdrcompagnon.feature_carte.data

import android.content.Context

/** Côté où le nom d'un point est affiché par rapport à son icône. */
enum class PositionTexte(val label: String) { BAS("Bas"), HAUT("Haut"), GAUCHE("Gauche"), DROITE("Droite") }

/**
 * Réglages d'affichage propres à un point de la carte : taille de l'icône (multiple de la taille
 * d'une case), verrouillage de sa position, présence et côté de son nom.
 */
data class ReglagePoint(
    val taille: Float = 1f,
    val verrouille: Boolean = false,
    val afficherTexte: Boolean = true,
    val positionTexte: PositionTexte = PositionTexte.BAS,
)

/**
 * Purement visuel, comme [CarteGrillePrefs] : stocké à part de PointInteretEntity (Room) pour
 * éviter une migration de base pour de simples réglages d'affichage.
 */
object CartePointPrefs {

    private const val PREFS_NAME = "carte_point_prefs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun lire(context: Context, pointId: String): ReglagePoint {
        val p = prefs(context)
        return ReglagePoint(
            taille = p.getFloat("taille_$pointId", 1f),
            verrouille = p.getBoolean("verrouille_$pointId", false),
            afficherTexte = p.getBoolean("texte_$pointId", true),
            positionTexte = p.getString("position_texte_$pointId", null)
                ?.let { nom -> PositionTexte.entries.firstOrNull { it.name == nom } }
                ?: PositionTexte.BAS,
        )
    }

    fun ecrire(context: Context, pointId: String, reglage: ReglagePoint) {
        prefs(context).edit()
            .putFloat("taille_$pointId", reglage.taille)
            .putBoolean("verrouille_$pointId", reglage.verrouille)
            .putBoolean("texte_$pointId", reglage.afficherTexte)
            .putString("position_texte_$pointId", reglage.positionTexte.name)
            .apply()
    }
}
