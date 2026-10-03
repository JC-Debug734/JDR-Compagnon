package com.jc2.jdrcompagnon.feature_carte.data

import android.content.Context

/**
 * Réglages d'affichage de la grille de la carte (taille de case, couleur des traits), par
 * campagne. Purement visuel : stocké à part de CarteCampagneEntity (Room) pour éviter une
 * migration de base pour un simple réglage d'affichage.
 */
object CarteGrillePrefs {

    private const val PREFS_NAME = "carte_grille_prefs"
    private const val TAILLE_CASE_DEFAUT = 36
    private const val EPAISSEUR_LIGNE_DEFAUT = 1

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _version = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** Incrémenté quand un réglage utile aux joueurs change (taille des cases) : rediffusion réseau. */
    val version: kotlinx.coroutines.flow.StateFlow<Int> = _version

    fun tailleCaseDp(context: Context, campagneId: String): Int =
        prefs(context).getInt("taille_$campagneId", TAILLE_CASE_DEFAUT)

    fun setTailleCaseDp(context: Context, campagneId: String, tailleDp: Int) {
        prefs(context).edit().putInt("taille_$campagneId", tailleDp).apply()
        _version.value++
    }

    /**
     * Nombre de cases sur la largeur de la carte, imposé par le MJ (reçu en réseau) : les joueurs
     * mesurent ainsi exactement les mêmes distances que lui, quelles que soient la densité de leur
     * écran et la résolution de l'image reçue. null = calculé localement (côté MJ).
     */
    fun casesEnLargeur(context: Context, carteId: String): Float? {
        val p = prefs(context)
        return if (p.contains("cases_largeur_$carteId")) p.getFloat("cases_largeur_$carteId", 0f).takeIf { it > 0f } else null
    }

    fun setCasesEnLargeur(context: Context, carteId: String, cases: Float) {
        prefs(context).edit().putFloat("cases_largeur_$carteId", cases).apply()
    }

    /** Heures de déplacement maximum par jour pour le calcul des pauses d'un trajet (8 h par défaut, règle D&D). */
    fun heuresMaxParJour(context: Context): Int = prefs(context).getInt("heures_max_jour", 8)

    fun setHeuresMaxParJour(context: Context, heures: Int) {
        prefs(context).edit().putInt("heures_max_jour", heures).apply()
    }

    /** Épaisseur (dp) des traits du quadrillage. */
    fun epaisseurLigneDp(context: Context, campagneId: String): Int =
        prefs(context).getInt("epaisseur_$campagneId", EPAISSEUR_LIGNE_DEFAUT)

    fun setEpaisseurLigneDp(context: Context, campagneId: String, epaisseurDp: Int) {
        prefs(context).edit().putInt("epaisseur_$campagneId", epaisseurDp).apply()
    }

    /** Couleur ARGB (packée comme un android.graphics.Color) des traits de la grille, ou null si jamais réglée (repli sur la couleur de thème). */
    fun couleurGrille(context: Context, campagneId: String): Int? {
        val prefs = prefs(context)
        val cle = "couleur_$campagneId"
        return if (prefs.contains(cle)) prefs.getInt(cle, 0) else null
    }

    fun setCouleurGrille(context: Context, campagneId: String, couleurArgb: Int) {
        prefs(context).edit().putInt("couleur_$campagneId", couleurArgb).apply()
    }

    /** Affiche ou non le nom de chaque point sous son icône sur la carte. */
    fun afficherNoms(context: Context, campagneId: String): Boolean =
        prefs(context).getBoolean("afficher_noms_$campagneId", true)

    fun setAfficherNoms(context: Context, campagneId: String, afficher: Boolean) {
        prefs(context).edit().putBoolean("afficher_noms_$campagneId", afficher).apply()
    }

    /** Affiche ou non le quadrillage en mode édition (jamais tracé en mode mesure). */
    fun afficherGrille(context: Context, campagneId: String): Boolean =
        prefs(context).getBoolean("afficher_grille_$campagneId", true)

    fun setAfficherGrille(context: Context, campagneId: String, afficher: Boolean) {
        prefs(context).edit().putBoolean("afficher_grille_$campagneId", afficher).apply()
    }

    /** Verrouille l'emplacement de toutes les icônes de la carte (plus de glisser-déposer). */
    fun iconesVerrouillees(context: Context, campagneId: String): Boolean =
        prefs(context).getBoolean("icones_verrouillees_$campagneId", false)

    fun setIconesVerrouillees(context: Context, campagneId: String, verrouillees: Boolean) {
        prefs(context).edit().putBoolean("icones_verrouillees_$campagneId", verrouillees).apply()
    }
}
