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
 * L'ordre suit le SRD : Classe → Espèce (dont son éventuel sous-choix, ex. type de
 * dragon) → Historique (avec le champ libre "Histoire et personnalité") → Langues →
 * Caractéristiques → Alignement → Équipement/Sorts → Récapitulatif.
 *
 * SORTS et ESPECE_CHOIX sont sautées automatiquement quand elles ne s'appliquent
 * pas (classe non lanceuse de sorts / espèce sans sous-choix), cf.
 * [CharacterCreationStateHolder.etapeEstPertinente].
 */
enum class CreationStep(val majorStep: Int, val label: String) {
    CLASSE(1, "Classe"),
    COMPETENCES_CLASSE(1, "Compétences de classe"),
    ESPECE(2, "Espèce"),
    ESPECE_CHOIX(2, "Particularité d'espèce"),
    HISTORIQUE(2, "Historique"),
    LANGUES(2, "Langues"),
    METHODE_CARACTERISTIQUES(3, "Méthode de génération"),
    VALEURS_CARACTERISTIQUES(3, "Génération des valeurs"),
    REPARTITION_CARACTERISTIQUES(3, "Répartition des valeurs"),
    AJUSTEMENT_HISTORIQUE(3, "Ajustement lié à l'historique"),
    ALIGNEMENT(4, "Alignement"),
    EQUIPEMENT_CLASSE(5, "Équipement de classe"),
    EQUIPEMENT_HISTORIQUE(5, "Équipement d'historique"),
    SORTS(5, "Sorts connus"),
    RECAPITULATIF(5, "Récapitulatif & finalisation");

    companion object {
        val ordered = entries
    }
}

/** Choix combiné de l'étape Historique : l'historique lui-même + le texte libre "Histoire et personnalité". */
data class SelectionHistorique(val historique: Historique, val texte: String)

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

    val espece: Espece? = null,
    val especeChoixSupplementaire: String? = null, // ex. type de dragon, lignage elfique, don "Polyvalent"...
    val historique: Historique? = null,
    val histoirePersonnalite: String = "", // saisi sur l'étape Historique, sous les questions d'aide
    val langues: List<Langue> = emptyList(), // Commun + 2 langues

    val methodeCaracteristiques: MethodeGenerationCaracteristiques? = null,
    val valeursGenerees: List<Int> = emptyList(), // 6 valeurs brutes, pas encore réparties
    val repartition: Map<Caracteristique, Int> = emptyMap(), // valeurs réparties, avant ajustement
    val ajustementHistorique: Map<Caracteristique, Int> = emptyMap(), // +2/+1 ou +1/+1/+1

    val alignement: Alignement? = null,

    val equipementClasseTexte: String? = null,
    val equipementHistoriqueTexte: String? = null,
    val sortsChoisis: List<String> = emptyList()
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

    /**
     * Noms des dons d'origines (dons_srd521.md, catégorie "Origines"), nécessaires
     * pour savoir si l'étape ESPECE_CHOIX s'applique au trait "Polyvalent" de
     * l'Humain. Fixé par l'appelant une fois les données chargées (cf.
     * CharacterCreationScreen : LaunchedEffect qui assigne cette liste).
     */
    var donsOriginesNoms: List<String> = emptyList()

    /** Met à jour la sélection en cours, sans rien confirmer. Appelé à chaque tap. */
    fun mettreAJourSelection(selection: Any?) {
        _uiState.update { it.copy(selectionEnAttente = selection) }
    }

    /**
     * Confirme le choix affiché sur l'étape courante et avance à la suivante (en
     * sautant les étapes non pertinentes, cf. [etapeEstPertinente]). Retourne false
     * (et ne fait rien) si aucune sélection valide n'est en attente, ou si la
     * sélection ne respecte pas la règle de l'étape (ex. mauvais nombre de choix).
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

    /** Revient à l'étape précédente pertinente ; sa confirmation est annulée. */
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

    /**
     * Instantané sérialisable de l'état courant, pour sauvegarde locale
     * (CharacterCreationDraftStore) — permet de reprendre une création interrompue.
     */
    fun exporterSnapshot(nomPersonnage: String): CreationSnapshot {
        val s = _uiState.value
        val d = s.draft
        return CreationSnapshot(
            step = s.step.name,
            nomPersonnage = nomPersonnage,
            classeNom = d.classe?.nom,
            competencesClasse = d.competencesClasse,
            especeNom = d.espece?.nom,
            especeChoixSupplementaire = d.especeChoixSupplementaire,
            historiqueNom = d.historique?.nom,
            histoirePersonnalite = d.histoirePersonnalite,
            languesNoms = d.langues.map { it.nom },
            methodeCaracteristiques = d.methodeCaracteristiques?.name,
            valeursGenerees = d.valeursGenerees,
            repartition = d.repartition.entries.associate { it.key.name to it.value },
            ajustementHistorique = d.ajustementHistorique.entries.associate { it.key.name to it.value },
            alignementNom = d.alignement?.name,
            equipementClasseTexte = d.equipementClasseTexte,
            equipementHistoriqueTexte = d.equipementHistoriqueTexte,
            sortsChoisis = d.sortsChoisis,
            stepsConfirmees = s.stepsConfirmees.map { it.name }
        )
    }

    /**
     * Reconstruit l'état à partir d'un instantané sauvegardé, en retrouvant les
     * objets Classe/Historique/Espece/Langue par leur nom dans les listes
     * fraîchement rechargées depuis la bibliothèque SRD.
     */
    fun restaurerDepuisSnapshot(
        snapshot: CreationSnapshot,
        classes: List<Classe>,
        historiques: List<Historique>,
        especes: List<Espece>,
        langues: List<Langue>
    ) {
        val draft = CharacterDraft(
            classe = classes.firstOrNull { it.nom == snapshot.classeNom },
            competencesClasse = snapshot.competencesClasse,
            espece = especes.firstOrNull { it.nom == snapshot.especeNom },
            especeChoixSupplementaire = snapshot.especeChoixSupplementaire,
            historique = historiques.firstOrNull { it.nom == snapshot.historiqueNom },
            histoirePersonnalite = snapshot.histoirePersonnalite,
            langues = langues.filter { it.nom in snapshot.languesNoms },
            methodeCaracteristiques = snapshot.methodeCaracteristiques
                ?.let { nom -> MethodeGenerationCaracteristiques.entries.firstOrNull { it.name == nom } },
            valeursGenerees = snapshot.valeursGenerees,
            repartition = snapshot.repartition.mapNotNull { (k, v) ->
                Caracteristique.entries.firstOrNull { it.name == k }?.let { it to v }
            }.toMap(),
            ajustementHistorique = snapshot.ajustementHistorique.mapNotNull { (k, v) ->
                Caracteristique.entries.firstOrNull { it.name == k }?.let { it to v }
            }.toMap(),
            alignement = snapshot.alignementNom?.let { nom -> Alignement.entries.firstOrNull { it.name == nom } },
            equipementClasseTexte = snapshot.equipementClasseTexte,
            equipementHistoriqueTexte = snapshot.equipementHistoriqueTexte,
            sortsChoisis = snapshot.sortsChoisis
        )
        val step = CreationStep.entries.firstOrNull { it.name == snapshot.step } ?: CreationStep.CLASSE
        val stepsConfirmees = snapshot.stepsConfirmees
            .mapNotNull { nom -> CreationStep.entries.firstOrNull { it.name == nom } }
            .toSet()
        _uiState.value = CharacterCreationUiState(
            step = step,
            draft = draft,
            stepsConfirmees = stepsConfirmees,
            selectionEnAttente = null
        )
    }

    /** Une étape peut être sautée automatiquement si elle ne s'applique pas au brouillon courant. */
    private fun etapeEstPertinente(step: CreationStep, draft: CharacterDraft): Boolean = when (step) {
        CreationStep.SORTS -> draft.classe?.estLanceurDeSorts == true
        CreationStep.ESPECE_CHOIX -> draft.espece?.let { detecterChoixEspece(it, donsOriginesNoms) } != null
        else -> true
    }

    private fun etapeSuivanteReelle(depuis: CreationStep, draft: CharacterDraft): CreationStep {
        val etapesOrdonnees = CreationStep.ordered
        var index = etapesOrdonnees.indexOf(depuis) + 1
        while (index < etapesOrdonnees.size) {
            val candidate = etapesOrdonnees[index]
            if (!etapeEstPertinente(candidate, draft)) {
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
            if (!etapeEstPertinente(candidate, draft)) {
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
                (selection as? List<String>)?.let { liste ->
                    val requis = draft.classe?.let { parseCompetencesClasse(it.maitrisesCompetence).nombre } ?: 2
                    // Refuse d'avancer tant que le nombre exact de compétences requis
                    // n'est pas atteint (ni moins, ni plus).
                    liste.takeIf { it.size == requis }?.let { draft.copy(competencesClasse = it) }
                }

            CreationStep.ESPECE ->
                (selection as? Espece)?.let { draft.copy(espece = it, especeChoixSupplementaire = null) }

            CreationStep.ESPECE_CHOIX ->
                (selection as? String)?.let { draft.copy(especeChoixSupplementaire = it) }

            CreationStep.HISTORIQUE ->
                (selection as? SelectionHistorique)?.let {
                    draft.copy(historique = it.historique, histoirePersonnalite = it.texte)
                }

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

            CreationStep.RECAPITULATIF -> draft // rien à appliquer, c'est l'étape de revue finale
        }
}