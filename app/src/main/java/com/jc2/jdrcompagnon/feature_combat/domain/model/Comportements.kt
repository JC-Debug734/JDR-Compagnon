package com.jc2.jdrcompagnon.feature_combat.domain.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Comportement de combat d'un combattant piloté par l'IA (outil COMPORTEMENT) : qui il vise, comment
 * il se bat et se déplace, quand il se replie. Les comportements intégrés reprennent les profils
 * [ProfilIA] (id = nom du profil) et restent modifiables ; le MJ peut en créer d'autres.
 */
@Serializable
data class Comportement(
    val id: String,
    val nom: String,
    val description: String = "",
    // Fraction de PV sous laquelle il se replie (null = jamais).
    val seuilFuite: Float? = null,
    val cible: StrategieCible = StrategieCible.HASARD,
    val style: StyleCombat = StyleCombat.POLYVALENT,
    // Capacités spéciales (souffle, sorts…) jouées dès qu'elles touchent quelqu'un.
    val speciauxEnPremier: Boolean = false,
    // Change chaque round de comportement, tiré au hasard parmi les intégrés.
    val imprevisible: Boolean = false,
) {
    /** Profil intégré correspondant (null pour un comportement créé par le MJ). */
    val profilIntegre: ProfilIA? get() = ProfilIA.entries.firstOrNull { it.name == id }

    companion object {
        fun depuis(p: ProfilIA) = Comportement(
            id = p.name,
            nom = p.label,
            description = p.description,
            seuilFuite = p.seuilFuite,
            cible = p.cible,
            style = p.style,
            speciauxEnPremier = p.speciauxEnPremier,
            imprevisible = p == ProfilIA.IMPREVISIBLE,
        )
    }
}

/** Ligne de la grille de comportement : classe de personnage → comportement par défaut. */
@Serializable
data class LigneGrille(val classe: String, val comportementId: String)

/**
 * Comportements (intégrés éventuellement modifiés, et créés par le MJ) et grille de comportement des
 * personnages par classe. La grille donne le comportement par défaut d'un PJ ou PNJ que personne ne
 * joue en combat ; une classe absente prend [CLASSE_PAR_DEFAUT]. Persisté en SharedPreferences.
 */
object Comportements {

    /** Ligne de la grille appliquée aux classes qu'elle ne cite pas. */
    const val CLASSE_PAR_DEFAUT = "Autre classe"

    private const val PREFS = "comportements_ia"
    private const val CLE_PERSO = "personnalises_json"
    private const val CLE_GRILLE = "grille_json"
    private val json = Json { ignoreUnknownKeys = true }
    private var appContext: Context? = null

    /** Grille par défaut : reprend la répartition par classe de ProfilIA.analyserPersonnage. */
    val grilleParDefaut: List<LigneGrille> = listOf(
        LigneGrille("Barbare", ProfilIA.BERSERKER.name),
        LigneGrille("Barde", ProfilIA.TACTICIEN.name),
        LigneGrille("Clerc", ProfilIA.PROTECTEUR.name),
        LigneGrille("Druide", ProfilIA.ARTILLEUR.name),
        LigneGrille("Ensorceleur", ProfilIA.ARTILLEUR.name),
        LigneGrille("Guerrier", ProfilIA.DUELLISTE.name),
        LigneGrille("Magicien", ProfilIA.ARTILLEUR.name),
        LigneGrille("Moine", ProfilIA.ESCARMOUCHEUR.name),
        LigneGrille("Occultiste", ProfilIA.ARTILLEUR.name),
        LigneGrille("Paladin", ProfilIA.PROTECTEUR.name),
        LigneGrille("Rôdeur", ProfilIA.TIREUR.name),
        LigneGrille("Roublard", ProfilIA.ESCARMOUCHEUR.name),
        LigneGrille(CLASSE_PAR_DEFAUT, ProfilIA.BRUTE.name),
    )

    // Comportements enregistrés par le MJ : intégrés modifiés (même id) et nouveaux.
    private val _personnalises = MutableStateFlow<List<Comportement>>(emptyList())
    private val _grille = MutableStateFlow(grilleParDefaut)
    val grille: StateFlow<List<LigneGrille>> = _grille.asStateFlow()

    private val _tous = MutableStateFlow(calculerTous())
    /** Tous les comportements : intégrés (modifiés le cas échéant) puis ceux créés par le MJ. */
    val tous: StateFlow<List<Comportement>> = _tous.asStateFlow()

    private fun calculerTous(): List<Comportement> {
        val perso = _personnalises.value
        val integres = ProfilIA.entries.map { p -> perso.firstOrNull { it.id == p.name } ?: Comportement.depuis(p) }
        return integres + perso.filter { c -> ProfilIA.entries.none { it.name == c.id } }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        runCatching { json.decodeFromString<List<Comportement>>(prefs.getString(CLE_PERSO, "[]").orEmpty()) }
            .getOrNull()?.let { _personnalises.value = it }
        runCatching { json.decodeFromString<List<LigneGrille>>(prefs.getString(CLE_GRILLE, "").orEmpty()) }
            .getOrNull()?.takeIf { it.isNotEmpty() }?.let { _grille.value = it }
        _tous.value = calculerTous()
    }

    private fun enregistrer() {
        _tous.value = calculerTous()
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(CLE_PERSO, json.encodeToString(_personnalises.value))
            ?.putString(CLE_GRILLE, json.encodeToString(_grille.value))
            ?.apply()
    }

    fun parId(id: String?): Comportement? = id?.let { i -> _tous.value.firstOrNull { it.id == i } }

    /** Comportement effectif d'un profil intégré (avec les modifications du MJ). */
    fun pour(profil: ProfilIA): Comportement = parId(profil.name) ?: Comportement.depuis(profil)

    /** Crée ou modifie un comportement (un intégré modifié garde son id). */
    fun enregistrer(c: Comportement) {
        _personnalises.value = _personnalises.value.filterNot { it.id == c.id } + c
        enregistrer()
    }

    /** Intégré : retour aux réglages d'origine. Créé par le MJ : supprimé (la grille repasse sur Brute). */
    fun supprimerOuReinitialiser(id: String) {
        _personnalises.value = _personnalises.value.filterNot { it.id == id }
        if (ProfilIA.entries.none { it.name == id }) {
            _grille.value = _grille.value.map { if (it.comportementId == id) it.copy(comportementId = ProfilIA.BRUTE.name) else it }
        }
        enregistrer()
    }

    fun definirGrille(lignes: List<LigneGrille>) {
        _grille.value = lignes
        enregistrer()
    }

    fun reinitialiserGrille() = definirGrille(grilleParDefaut)

    private fun normaliser(s: String) = java.text.Normalizer.normalize(s.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    /** Comportement par défaut d'un personnage de la classe [classe] d'après la grille. */
    fun pourClasse(classe: String): Comportement {
        val c = normaliser(classe)
        val ligne = _grille.value.firstOrNull { it.classe != CLASSE_PAR_DEFAUT && c.isNotEmpty() && normaliser(it.classe) == c }
            ?: _grille.value.firstOrNull { it.classe == CLASSE_PAR_DEFAUT }
        return parId(ligne?.comportementId) ?: pour(ProfilIA.BRUTE)
    }
}
