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
    val nomPersonnage: String? = null, // choisi en toute dernière étape (CreationStep.NOM)
    val portrait: String? = null, // id dans characterPortraitOptions, choisi juste après le nom (CreationStep.PORTRAIT)
    val classeNom: String? = null,
    val competencesClasse: List<String> = emptyList(),
    // Choix intégrés aux aptitudes de niveau 1 (ex. Protecteur/Thaumaturge de l'Ordre divin
    // du Clerc) : clé = id de l'aptitude (AptitudeClasse.id), valeur = nom de l'option choisie.
    val classeChoixNiveau1: Map<String, String> = emptyMap(),
    val especeNom: String? = null,
    // Ancien format (un seul sous-choix d'espèce) : ignoré, gardé pour relire les brouillons existants.
    val especeChoixSupplementaire: String? = null,
    // Choix balisés de l'espèce : id du choix -> valeurs retenues (cf. choixEspece).
    val especeChoix: Map<String, List<String>> = emptyMap(),
    // Choix balisés de la classe, de l'historique et des dons (cf. CharacterDraft.choixComplementaires).
    val choixComplementaires: Map<String, List<String>> = emptyMap(),
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
    val sortsMineursChoisis: List<String> = emptyList(),
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