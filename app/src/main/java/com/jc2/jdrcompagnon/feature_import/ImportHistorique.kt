package com.jc2.jdrcompagnon.feature_import

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Genre de contenu reconnu par [ImportCentral], avec son libellé affiché dans l'historique. */
enum class ImportGenre(val libelle: String) {
    LIVRE("Livre"),
    UNIVERS("Univers"),
    SCENARIO("Scénario"),
    CAMPAGNE("Campagne"),
    DOSSIER("Dossier de scénarios"),
    PERSONNAGE("Fiche de personnage"),
    MUSIQUE("Musique"),
    EPREUVE("Épreuve"),
    INCONNU("Import"),
}

/**
 * Historique des imports (outil IMPORT), conservé dans les SharedPreferences : date, fichier,
 * genre reconnu, succès et résumé de ce qui a été intégré (ou raison de l'échec). Limité aux
 * [MAX_ENTREES] derniers imports, le plus récent en premier.
 */
object ImportHistorique {

    data class Entree(
        val date: Long,
        val fichier: String,
        val genre: ImportGenre,
        val succes: Boolean,
        val message: String,
        // Ce que l'import a créé dans l'app, pour pouvoir tout retirer ([ImportCentral.supprimerImport]).
        val elements: List<ElementImporte> = emptyList(),
        // L'import a été supprimé (entrée gardée dans l'historique pour mémoire).
        val supprime: Boolean = false,
    )

    /**
     * Élément créé par un import : [type] parmi [TYPES] (livre, univers, scénario, campagne,
     * fiche, musique), [id] son identifiant dans l'app, [worldId] l'univers d'un livre.
     */
    data class ElementImporte(val type: String, val id: String, val libelle: String, val worldId: String? = null)

    val TYPES = listOf("scenario", "campagne", "personnage", "livre", "univers", "musique", "epreuve")

    private const val PREFS_NAME = "import_historique"
    private const val KEY_ENTREES = "entrees_json"
    private const val MAX_ENTREES = 100

    private val _entrees = MutableStateFlow<List<Entree>?>(null)

    /** Chargé à la première lecture ([charger]) ; null tant que ce n'est pas fait. */
    val entrees: StateFlow<List<Entree>?> = _entrees.asStateFlow()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun charger(context: Context): List<Entree> = _entrees.value ?: lire(context).also { _entrees.value = it }

    fun ajouter(
        context: Context,
        fichier: String,
        genre: ImportGenre,
        resultat: Result<String>,
        elements: List<ElementImporte> = emptyList(),
    ): Entree {
        val entree = Entree(
            date = System.currentTimeMillis(),
            fichier = fichier,
            genre = genre,
            succes = resultat.isSuccess,
            message = resultat.fold(
                onSuccess = { it },
                onFailure = { "Échec : ${it.message ?: "erreur inconnue"}" },
            ),
            elements = elements,
        )
        enregistrer(context, (listOf(entree) + charger(context)).take(MAX_ENTREES))
        return entree
    }

    /** L'import [entree] a été supprimé : il disparaît de la liste des imports présents. */
    fun retirer(context: Context, entree: Entree) {
        enregistrer(context, charger(context).filterNot { it.date == entree.date && it.fichier == entree.fichier })
    }

    fun vider(context: Context) {
        _entrees.value = emptyList()
        prefs(context).edit().remove(KEY_ENTREES).apply()
    }

    private fun enregistrer(context: Context, liste: List<Entree>) {
        _entrees.value = liste
        ecrire(context, liste)
    }

    private fun lire(context: Context): List<Entree> = runCatching {
        val tableau = JSONArray(prefs(context).getString(KEY_ENTREES, null) ?: return emptyList())
        (0 until tableau.length()).map { i ->
            val o = tableau.getJSONObject(i)
            val elements = o.optJSONArray("elements")?.let { a ->
                (0 until a.length()).map { k ->
                    val e = a.getJSONObject(k)
                    ElementImporte(
                        type = e.getString("type"),
                        id = e.getString("id"),
                        libelle = e.optString("libelle"),
                        worldId = e.optString("worldId").ifBlank { null },
                    )
                }
            }.orEmpty()
            Entree(
                date = o.getLong("date"),
                fichier = o.getString("fichier"),
                genre = runCatching { ImportGenre.valueOf(o.getString("genre")) }.getOrDefault(ImportGenre.INCONNU),
                succes = o.getBoolean("succes"),
                message = o.getString("message"),
                elements = elements,
                supprime = o.optBoolean("supprime", false),
            )
        }
    }.getOrDefault(emptyList())

    private fun ecrire(context: Context, liste: List<Entree>) {
        val tableau = JSONArray()
        liste.forEach { e ->
            val elements = JSONArray()
            e.elements.forEach { el ->
                elements.put(
                    JSONObject().put("type", el.type).put("id", el.id).put("libelle", el.libelle).put("worldId", el.worldId ?: "")
                )
            }
            tableau.put(
                JSONObject()
                    .put("date", e.date)
                    .put("fichier", e.fichier)
                    .put("genre", e.genre.name)
                    .put("succes", e.succes)
                    .put("message", e.message)
                    .put("elements", elements)
                    .put("supprime", e.supprime)
            )
        }
        prefs(context).edit().putString(KEY_ENTREES, tableau.toString()).apply()
    }
}
