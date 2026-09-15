package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Étapes du wizard. Regroupées sous les 5 étapes officielles du SRD (majorStep),
 * mais découpées en sous-étapes pour que chaque CHOIX individuel ait sa propre
 * confirmation : on ne peut pas avancer sans valider explicitement le choix affiché.
 *
 * SORTS est sautée automatiquement pour les classes non lanceuses de sorts au
 * niveau 1 (cf. [CharacterCreationStateHolder.etapeSuivanteReelle]).
 */
enum class CreationStep(val majorStep: Int, val label: String) {
    CLASSE(1, "Classe"),
    COMPETENCES_CLASSE(1, "Compétences de classe"),
    HISTORIQUE(2, "Historique"),
    ESPECE(2, "Espèce"),
    LANGUES(2, "Langues"),
    METHODE_CARACTERISTIQUES(3, "Méthode de génération"),
    VALEURS_CARACTERISTIQUES(3, "Génération des valeurs"),
    REPARTITION_CARACTERISTIQUES(3, "Répartition des valeurs"),
    AJUSTEMENT_HISTORIQUE(3, "Ajustement lié à l'historique"),
    ALIGNEMENT(4, "Alignement"),
    EQUIPEMENT_CLASSE(5, "Équipement de classe"),
    EQUIPEMENT_HISTORIQUE(5, "Équipement d'historique"),
    SORTS(5, "Sorts connus"),
    PERSONNALITE(5, "Personnalité"),
    RECAPITULATIF(5, "Récapitulatif & finalisation");

    companion object {
        val ordered = entries
    }
}

/**
 * Brouillon de personnage en cours de création. Chaque champ n'est renseigné
 * qu'après confirmation explicite de l'étape correspondante — jamais en avance.
 *
 * classe / historique / espece / langues viennent directement de ClasseParser /
 * HistoriqueParser / EspeceParser / LangueParser (fichiers classes_srd521.md /
 * historiques_srd521.md / especes_srd521.md / langues.md, cf. SrdCreationParsers.kt).
 * Le wizard n'a donc plus aucune liste figée : un nouveau bloc dans ces fichiers,
 * ou un fichier remplacé avec plus de choix, apparaît sans changement de code.
 */
data class OptionChoisie(val id: String, val label: String)

data class CharacterDraft(
    val classe: Classe? = null,
    val competencesClasse: List<String> = emptyList(),

    val historique: Historique? = null,
    val espece: Espece? = null,
    val langues: List<Langue> = emptyList(), // Commun + 2 langues

    val methodeCaracteristiques: MethodeGenerationCaracteristiques? = null,
    val valeursGenerees: List<Int> = emptyList(), // 6 valeurs brutes, pas encore réparties
    val repartition: Map<Caracteristique, Int> = emptyMap(), // valeurs réparties, avant ajustement
    val ajustementHistorique: Map<Caracteristique, Int> = emptyMap(), // +2/+1 ou +1/+1/+1

    val alignement: Alignement? = null,

    val equipementClasseTexte: String? = null,
    val equipementHistoriqueTexte: String? = null,
    val sortsChoisis: List<String> = emptyList(),

    val personnalite: String = "",
    val ideaux: String = "",
    val liens: String = "",
    val defauts: String = ""
) {
    /** Valeurs finales de caractéristique = répartition + ajustement d'historique. */
    val valeursFinales: Map<Caracteristique, Int>
        get() = Caracteristique.entries.associateWith { c ->
            (repartition[c] ?: 0) + (ajustementHistorique[c] ?: 0)
        }

    val modificateurs: Map<Caracteristique, Int>
        get() = valeursFinales.mapValues { (_, v) -> TablesCaracteristiques.modificateur(v) }

    val pointsDeVieNiveau1: Int?
        get() = classe?.pvNiveau1(modificateurs[Caracteristique.CONSTITUTION] ?: 0)

    val orDepart: Int
        get() = extraireOr(equipementClasseTexte.orEmpty()) + extraireOr(equipementHistoriqueTexte.orEmpty())

    val estComplet: Boolean
        get() = classe != null && historique != null && espece != null && langues.size >= 3 &&
                valeursFinales.values.all { it > 0 }
    // L'alignement n'est volontairement pas requis pour estComplet : le SRD le laisse
    // ouvert et certaines tables jouent sans (cf. "Créatures non alignées").
}

/**
 * État exposé à l'UI : brouillon courant + étape courante + historique de navigation.
 * Suit le même schéma StateFlow que GameState (source unique de vérité).
 */
data class CharacterCreationUiState(
    val step: CreationStep = CreationStep.CLASSE,
    val draft: CharacterDraft = CharacterDraft(),
    val stepsConfirmees: Set<CreationStep> = emptySet(),
    /** Choix en cours de sélection sur l'étape courante, PAS ENCORE confirmé. */
    val selectionEnAttente: Any? = null
) {
    val progression: Float get() = stepsConfirmees.size / CreationStep.ordered.size.toFloat()
    val peutReculer: Boolean get() = step != CreationStep.ordered.first()
}

/**
 * Détenteur d'état du wizard. À instancier depuis un ViewModel (ou directement
 * dans GameState si vous préférez centraliser, comme pour playerName/appRole).
 *
 * Règle d'or : aucune fonction "select*" n'écrit dans `draft`. Seule `confirmerEtapeCourante()`
 * fait passer `selectionEnAttente` dans `draft`, ce qui garantit qu'un choix affiché
 * mais non confirmé ne "fuit" jamais dans le personnage final.
 */
class CharacterCreationStateHolder {
    private val _uiState = MutableStateFlow(CharacterCreationUiState())
    val uiState: StateFlow<CharacterCreationUiState> = _uiState.asStateFlow()

    /** Met à jour la sélection en cours, sans rien confirmer. Appelé à chaque tap. */
    fun mettreAJourSelection(selection: Any?) {
        _uiState.update { it.copy(selectionEnAttente = selection) }
    }

    /**
     * Confirme le choix affiché sur l'étape courante et avance à la suivante
     * (en sautant SORTS si la classe choisie n'est pas lanceuse de sorts).
     * Retourne false (et ne fait rien) si aucune sélection valide n'est en attente.
     */
    fun confirmerEtapeCourante(): Boolean {
        val state = _uiState.value
        val selection = state.selectionEnAttente ?: return false
        val nouveauDraft = appliquerSelection(state.step, state.draft, selection) ?: return false
        val etapeSuivante = etapeSuivanteReelle(state.step, nouveauDraft)

        _uiState.update {
            it.copy(
                draft = nouveauDraft,
                stepsConfirmees = it.stepsConfirmees + state.step,
                step = etapeSuivante,
                selectionEnAttente = null
            )
        }
        return true
    }

    /** Revient à l'étape précédente (en sautant SORTS si non pertinente) ; sa confirmation est annulée. */
    fun revenirEtapePrecedente() {
        val state = _uiState.value
        val etapePrecedente = etapePrecedenteReelle(state.step, state.draft)

        _uiState.update {
            it.copy(
                step = etapePrecedente,
                stepsConfirmees = it.stepsConfirmees - etapePrecedente,
                selectionEnAttente = null
            )
        }
    }

    /** Permet de revenir modifier une étape déjà confirmée (depuis le récapitulatif). */
    fun modifierEtape(step: CreationStep) {
        _uiState.update { it.copy(step = step, selectionEnAttente = null) }
    }

    private fun etapeSuivanteReelle(depuis: CreationStep, draft: CharacterDraft): CreationStep {
        val etapesOrdonnees = CreationStep.ordered
        var index = etapesOrdonnees.indexOf(depuis) + 1
        while (index < etapesOrdonnees.size) {
            val candidate = etapesOrdonnees[index]
            if (candidate == CreationStep.SORTS && draft.classe?.estLanceurDeSorts != true) {
                index++
                continue
            }
            return candidate
        }
        return depuis
    }

    private fun etapePrecedenteReelle(depuis: CreationStep, draft: CharacterDraft): CreationStep {
        val etapesOrdonnees = CreationStep.ordered
        var index = etapesOrdonnees.indexOf(depuis) - 1
        while (index >= 0) {
            val candidate = etapesOrdonnees[index]
            if (candidate == CreationStep.SORTS && draft.classe?.estLanceurDeSorts != true) {
                index--
                continue
            }
            return candidate
        }
        return depuis
    }

    private fun appliquerSelection(step: CreationStep, draft: CharacterDraft, selection: Any): CharacterDraft? =
        when (step) {
            CreationStep.CLASSE ->
                (selection as? Classe)?.let { draft.copy(classe = it) }

            CreationStep.COMPETENCES_CLASSE ->
                @Suppress("UNCHECKED_CAST")
                (selection as? List<String>)?.let { draft.copy(competencesClasse = it) }

            CreationStep.HISTORIQUE ->
                (selection as? Historique)?.let { draft.copy(historique = it) }

            CreationStep.ESPECE ->
                (selection as? Espece)?.let { draft.copy(espece = it) }

            CreationStep.LANGUES ->
                @Suppress("UNCHECKED_CAST")
                (selection as? List<Langue>)?.takeIf { it.size >= 3 }
                    ?.let { draft.copy(langues = it) }

            CreationStep.METHODE_CARACTERISTIQUES ->
                (selection as? MethodeGenerationCaracteristiques)?.let { draft.copy(methodeCaracteristiques = it) }

            CreationStep.VALEURS_CARACTERISTIQUES ->
                @Suppress("UNCHECKED_CAST")
                (selection as? List<Int>)?.takeIf { it.size == 6 }
                    ?.let { draft.copy(valeursGenerees = it) }

            CreationStep.REPARTITION_CARACTERISTIQUES ->
                @Suppress("UNCHECKED_CAST")
                (selection as? Map<Caracteristique, Int>)?.takeIf { it.size == 6 }
                    ?.let { draft.copy(repartition = it) }

            CreationStep.AJUSTEMENT_HISTORIQUE ->
                @Suppress("UNCHECKED_CAST")
                (selection as? Map<Caracteristique, Int>)?.let { draft.copy(ajustementHistorique = it) }

            CreationStep.ALIGNEMENT ->
                (selection as? Alignement)?.let { draft.copy(alignement = it) }

            CreationStep.EQUIPEMENT_CLASSE ->
                (selection as? String)?.let { draft.copy(equipementClasseTexte = it) }

            CreationStep.EQUIPEMENT_HISTORIQUE ->
                (selection as? String)?.let { draft.copy(equipementHistoriqueTexte = it) }

            CreationStep.SORTS ->
                @Suppress("UNCHECKED_CAST")
                (selection as? List<String>)?.let { draft.copy(sortsChoisis = it) }

            CreationStep.PERSONNALITE ->
                @Suppress("UNCHECKED_CAST")
                (selection as? Map<String, String>)?.let { m ->
                    draft.copy(
                        personnalite = m["personnalite"] ?: draft.personnalite,
                        ideaux = m["ideaux"] ?: draft.ideaux,
                        liens = m["liens"] ?: draft.liens,
                        defauts = m["defauts"] ?: draft.defauts
                    )
                }

            CreationStep.RECAPITULATIF -> draft // rien à appliquer, c'est l'étape de revue finale
        }
}