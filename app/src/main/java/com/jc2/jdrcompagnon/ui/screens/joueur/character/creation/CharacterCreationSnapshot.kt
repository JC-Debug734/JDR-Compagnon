package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Instantané léger et sérialisable de l'état du wizard, suffisant pour reprendre
 * une création interrompue. Ne stocke QUE des types simples : les objets Classe/
 * Historique/Espece/Langue eux-mêmes viennent de fichiers .md rechargés à chaque
 * ouverture d'écran (via SrdRepository), donc on ne les duplique pas ici — on
 * garde juste leur nom pour les retrouver dans la liste rechargée au moment de
 * la reprise (cf. [CharacterCreationStateHolder.restaurerDepuisSnapshot]).
 */
@Serializable
data class CreationSnapshot(
    val step: String,
    val nomPersonnage: String,
    val classeNom: String? = null,
    val competencesClasse: List<String> = emptyList(),
    val especeNom: String? = null,
    val especeChoixSupplementaire: String? = null,
    val historiqueNom: String? = null,
    val histoirePersonnalite: String = "",
    val languesNoms: List<String> = emptyList(),
    val methodeCaracteristiques: String? = null,
    val valeursGenerees: List<Int> = emptyList(),
    val repartition: Map<String, Int> = emptyMap(), // clé = nom de l'entrée Caracteristique
    val ajustementHistorique: Map<String, Int> = emptyMap(),
    val alignementNom: String? = null,
    val equipementClasseTexte: String? = null,
    val equipementHistoriqueTexte: String? = null,
    val sortsChoisis: List<String> = emptyList(),
    val stepsConfirmees: List<String> = emptyList()
)

/**
 * Sauvegarde locale (SharedPreferences) d'une création de personnage en cours,
 * un seul brouillon à la fois. Permet de quitter l'écran de création (retour,
 * fermeture de l'app...) et de reprendre exactement où on en était.
 */
object CharacterCreationDraftStore {
    private const val PREFS = "character_creation_draft"
    private const val CLE_SNAPSHOT = "snapshot"

    fun sauvegarder(context: Context, snapshot: CreationSnapshot) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(CLE_SNAPSHOT, Json.encodeToString(snapshot)).apply()
    }

    fun charger(context: Context): CreationSnapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val brut = prefs.getString(CLE_SNAPSHOT, null) ?: return null
        return try {
            Json.decodeFromString(CreationSnapshot.serializer(), brut)
        } catch (e: Exception) {
            null // brouillon corrompu ou d'un format obsolète : on repart de zéro plutôt que de planter
        }
    }

    fun effacer(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(CLE_SNAPSHOT).apply()
    }
}