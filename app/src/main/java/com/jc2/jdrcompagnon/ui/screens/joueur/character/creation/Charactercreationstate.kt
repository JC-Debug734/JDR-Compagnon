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
 * Caractéristiques → Alignement → Équipement/Sorts → Nom → Récapitulatif.
 *
 * Le Nom est volontairement la toute dernière étape avant le récapitulatif : le
 * joueur connaît déjà l'espèce (et la classe/l'historique) du personnage, ce qui
 * permet une génération de nom aléatoire cohérente avec l'espèce (cf.
 * [genererNomAleatoire] dans Charactercreationwizard.kt).
 *
 * SORTS et ESPECE_CHOIX sont sautées automatiquement quand elles ne s'appliquent
 * pas (classe non lanceuse de sorts / espèce sans sous-choix), cf.
 * [CharacterCreationStateHolder.etapeEstPertinente].
 */
enum class CreationStep(val majorStep: Int, val label: String) {
    CLASSE(1, "Classe"),
    COMPETENCES_CLASSE(1, "Compétences de classe"),
    CLASSE_APTITUDE_CHOIX(1, "Choix de classe"),
    ESPECE(2, "Espèce"),
    ESPECE_CHOIX(2, "Choix d'espèce"),
    HISTORIQUE(2, "Historique"),
    CHOIX_COMPLEMENTAIRES(2, "Outils, dons et autres choix"),
    LANGUES(2, "Langues"),
    METHODE_CARACTERISTIQUES(3, "Méthode de génération"),
    VALEURS_CARACTERISTIQUES(3, "Génération des valeurs"),
    REPARTITION_CARACTERISTIQUES(3, "Répartition des valeurs"),
    AJUSTEMENT_HISTORIQUE(3, "Ajustement lié à l'historique"),
    ALIGNEMENT(4, "Alignement"),
    EQUIPEMENT_CLASSE(5, "Équipement de classe"),
    EQUIPEMENT_HISTORIQUE(5, "Équipement d'historique"),
    SORTS(5, "Sorts connus"),
    NOM(5, "Nom du personnage"),
    PORTRAIT(5, "Portrait"),
    RECAPITULATIF(5, "Récapitulatif & finalisation");

    companion object {
        val ordered = entries
    }
}

/** Choix combiné de l'étape Historique : l'historique lui-même + le texte libre "Histoire et personnalité". */
data class SelectionHistorique(val historique: Historique, val texte: String)

/** Choix combiné de l'étape Sorts : sorts mineurs (cantrips) + sorts de 1er niveau préparés/connus. */
data class SelectionSorts(val mineurs: List<String>, val prepares: List<String>)

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
    // Choix intégrés aux aptitudes de niveau 1 (ex. Protecteur/Thaumaturge de l'Ordre divin
    // du Clerc, cf. AptitudeClasse.type == "choix-effet") : clé = id de l'aptitude, valeur =
    // nom de l'option choisie. Vide pour une classe sans aptitude de ce type au niveau 1.
    val classeChoixNiveau1: Map<String, String> = emptyMap(),

    val espece: Espece? = null,
    // Choix balisés de l'espèce (especes_srd521.md, balises "choix:") : id du choix -> valeurs
    // retenues (ex. "competent" -> ["Perception"]), cf. choixEspece / effetsEspece.
    val especeChoix: Map<String, List<String>> = emptyMap(),
    val historique: Historique? = null,
    val histoirePersonnalite: String = "", // saisi sur l'étape Historique, sous les questions d'aide
    // Choix balisés de la classe, de l'historique et des dons reçus (étape CHOIX_COMPLEMENTAIRES) :
    // id du choix ("classe:barde-instruments", "don:Initié à la magie:initie-liste"...) -> valeurs.
    val choixComplementaires: Map<String, List<String>> = emptyMap(),
    val langues: List<Langue> = emptyList(), // Commun + 2 langues

    val methodeCaracteristiques: MethodeGenerationCaracteristiques? = null,
    val valeursGenerees: List<Int> = emptyList(), // 6 valeurs brutes, pas encore réparties
    val repartition: Map<Caracteristique, Int> = emptyMap(), // valeurs réparties, avant ajustement
    val ajustementHistorique: Map<Caracteristique, Int> = emptyMap(), // +2/+1 ou +1/+1/+1

    val alignement: Alignement? = null,

    val equipementClasseTexte: String? = null,
    val equipementHistoriqueTexte: String? = null,
    val sortsMineursChoisis: List<String> = emptyList(), // cantrips, cf. Classe.sortsMineursNiveau1
    val sortsChoisis: List<String> = emptyList(), // sorts de 1er niveau, cf. Classe.sortsPreparesNiveau1

    val nomPersonnage: String? = null, // dernière étape du wizard, cf. CreationStep.NOM
    val portrait: String? = null // id dans characterPortraitOptions (CharacterSheetScreen.kt), cf. CreationStep.PORTRAIT
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
            ?.plus(espece?.let { effetsEspece(it, especeChoix).pvParNiveau } ?: 0)

    val orDepart: Int
        get() = extraireOr(equipementClasseTexte.orEmpty()) + extraireOr(equipementHistoriqueTexte.orEmpty())

    val estComplet: Boolean
        get() = classe != null && historique != null && espece != null && langues.size >= 3 &&
                valeursFinales.values.all { it > 0 } && !nomPersonnage.isNullOrBlank() && !portrait.isNullOrBlank()
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
     * Dons d'origines (dons_srd521.md, catégorie "Origines"), nécessaires pour savoir
     * si l'étape ESPECE_CHOIX s'applique au trait "Polyvalent" de l'Humain, et pour
     * afficher la description de chaque don au choix. Fixé par l'appelant une fois
     * les données chargées (cf. CharacterCreationScreen : LaunchedEffect qui assigne
     * cette liste).
     */
    var donsOriginesNoms: List<Don> = emptyList()

    /** Objets d'équipement, langues et dons d'origines dont les choix balisés tirent leurs options. */
    var contexte: ContexteChoix = ContexteChoix()

    /** Tous les dons (dons_srd521.md), pour retrouver les choix du don d'historique ou d'espèce. */
    var tousLesDons: List<Don> = emptyList()

    private fun ctx() = contexte.copy(donsOrigines = donsOriginesNoms.ifEmpty { contexte.donsOrigines })

    /** Choix balisés de l'espèce à faire à la création (ceux d'un niveau supérieur viendront à la montée de niveau). */
    fun choixEspeceCreation(draft: CharacterDraft): List<ChoixBalise> =
        draft.espece?.let { choixEspece(it, donsOriginesNoms, ctx()) }.orEmpty().filter { it.niveau <= 1 }

    /**
     * Choix balisés de la classe (outils, objet d'équipement), de l'historique (outils) et des dons
     * reçus (don d'historique, don d'espèce) — étape CHOIX_COMPLEMENTAIRES. Les langues en plus
     * (effet: langues) se choisissent à l'étape Langues, cf. [languesSupplementaires].
     */
    fun choixComplementaires(
        draft: CharacterDraft,
        // Sélection en cours : les choix dépendants (sorts de la liste choisie) s'y ajustent.
        selection: Map<String, List<String>> = draft.choixComplementaires,
    ): List<ChoixBalise> {
        val c = ctx()
        val classe = draft.classe
        val historique = draft.historique
        val deClasse = classe?.balisesTraitsDeBase.orEmpty()
            .filter { it.containsKey("choix") && it["effet"] != EffetBalise.LANGUES }
            .mapNotNull { b ->
                val raison = when (b["effet"]) {
                    EffetBalise.EQUIPEMENT -> "Équipement de départ de la classe ${classe!!.nom} : " +
                        classe.equipement.options.firstOrNull { o -> b["remplace"]?.let { o.texte.contains(it, ignoreCase = true) } == true }?.texte.orEmpty() +
                        " (s'applique si vous prenez ce lot)"
                    else -> "Maîtrise d'outils de la classe ${classe!!.nom} : ${classe.maitrisesOutils.orEmpty()}"
                }
                construireChoix(b, classe.nom, raison, c, prefixeId = "classe:")
            }
        val dHistorique = historique?.balises.orEmpty().filter { it.containsKey("choix") }.mapNotNull { b ->
            construireChoix(b, historique!!.nom, "Maîtrise d'outils de l'historique ${historique.nom} : ${historique.maitriseOutils}", c, prefixeId = "historique:")
        }
        return deClasse + dHistorique + donsRecus(draft).flatMap { (don, origine) ->
            // Dans l'ordre du fichier : un choix peut dépendre d'un précédent (balise "liste:").
            val retenues = mutableMapOf<String, List<String>>()
            don.balises.mapNotNull { b ->
                construireChoix(
                    b, don.nom, "don « ${don.nom} » ($origine) : ${don.description}", c,
                    prefixeId = "don:${don.nom}:", valeursAutres = retenues,
                )?.let { choix ->
                    // "Initié à la magie (Clerc)" : la précision de l'historique impose l'option.
                    val precision = historique?.donPrecision?.takeIf { historique.donNom == don.nom }
                    val impose = precision?.let { p -> choix.options.firstOrNull { it.equals(p, ignoreCase = true) } }
                    (if (impose != null) choix.copy(impose = listOf(impose)) else choix)
                        .also { retenues[it.id] = valeursDe(it, selection) }
                }
            }
        }
    }

    /** Dons reçus à la création (historique + espèce) avec leur origine lisible. */
    fun donsRecus(draft: CharacterDraft): List<Pair<Don, String>> {
        val parHistorique = draft.historique?.let { h ->
            tousLesDons.firstOrNull { it.nom.equals(h.donNom, ignoreCase = true) }?.let { it to "don d'historique ${h.nom}" }
        }
        val parEspece = draft.espece?.let { e ->
            effetsEspece(e, draft.especeChoix).dons.mapNotNull { nom ->
                tousLesDons.firstOrNull { it.nom == nom }?.let { it to "don d'espèce ${e.nom}" }
            }
        }.orEmpty()
        return listOfNotNull(parHistorique) + parEspece
    }

    /** Langues à choisir en plus des 2 de base (balises "effet: langues" de la classe, ex. Roublard). */
    fun languesSupplementaires(draft: CharacterDraft): List<ChoixBalise> =
        draft.classe?.let { classe ->
            (classe.balisesTraitsDeBase.map { classe.nom to it } +
                classe.aptitudesNiveau1.flatMap { a -> a.balises.map { "${a.nom} (${classe.nom})" to it } })
                .filter { (_, b) -> b.containsKey("choix") && b["effet"] == EffetBalise.LANGUES }
                .mapNotNull { (source, b) -> construireChoix(b, source, "", ctx(), prefixeId = "classe:") }
        }.orEmpty()

    /** Valeurs retenues d'un choix : imposées par la source, sinon choisies. */
    fun valeursDe(choix: ChoixBalise, selection: Map<String, List<String>>): List<String> =
        choix.impose ?: selection[choix.id].orEmpty()

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
    /** Vrai si la sélection en attente permet de passer à l'étape suivante (même règle que [confirmerEtapeCourante]). */
    fun etapeCouranteValide(state: CharacterCreationUiState): Boolean {
        val selection = state.selectionEnAttente ?: return false
        return appliquerSelection(state.step, state.draft, selection) != null
    }

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
    fun exporterSnapshot(): CreationSnapshot {
        val s = _uiState.value
        val d = s.draft
        return CreationSnapshot(
            step = s.step.name,
            nomPersonnage = d.nomPersonnage,
            portrait = d.portrait,
            classeNom = d.classe?.nom,
            competencesClasse = d.competencesClasse,
            classeChoixNiveau1 = d.classeChoixNiveau1,
            especeNom = d.espece?.nom,
            especeChoix = d.especeChoix,
            choixComplementaires = d.choixComplementaires,
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
            sortsMineursChoisis = d.sortsMineursChoisis,
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
            nomPersonnage = snapshot.nomPersonnage,
            portrait = snapshot.portrait,
            classe = classes.firstOrNull { it.nom == snapshot.classeNom },
            competencesClasse = snapshot.competencesClasse,
            classeChoixNiveau1 = snapshot.classeChoixNiveau1,
            espece = especes.firstOrNull { it.nom == snapshot.especeNom },
            especeChoix = snapshot.especeChoix,
            choixComplementaires = snapshot.choixComplementaires,
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
            sortsMineursChoisis = snapshot.sortsMineursChoisis,
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
        CreationStep.ESPECE_CHOIX -> choixEspeceCreation(draft).isNotEmpty()
        CreationStep.CHOIX_COMPLEMENTAIRES -> choixComplementaires(draft).isNotEmpty()
        CreationStep.CLASSE_APTITUDE_CHOIX -> draft.classe?.aptitudesNiveau1?.any { it.type == "choix-effet" } == true
        CreationStep.VALEURS_CARACTERISTIQUES -> draft.methodeCaracteristiques != MethodeGenerationCaracteristiques.VALEURS_STANDARD
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

            CreationStep.CLASSE_APTITUDE_CHOIX ->
                @Suppress("UNCHECKED_CAST")
                (selection as? Map<String, String>)?.let { map ->
                    val aptitudesRequises = draft.classe?.aptitudesNiveau1?.filter { it.type == "choix-effet" } ?: emptyList()
                    // Toutes les aptitudes à choix de la classe doivent avoir une option retenue.
                    if (aptitudesRequises.all { apt -> map.containsKey(apt.id ?: apt.nom) }) {
                        draft.copy(classeChoixNiveau1 = map)
                    } else null
                }

            CreationStep.ESPECE ->
                // Changer d'espèce efface ses choix ; reconfirmer la même espèce les conserve.
                (selection as? Espece)?.let { draft.copy(espece = it, especeChoix = if (it == draft.espece) draft.especeChoix else emptyMap()) }

            CreationStep.ESPECE_CHOIX ->
                @Suppress("UNCHECKED_CAST")
                (selection as? Map<String, List<String>>)?.let { map ->
                    // Chaque choix doit avoir exactement le nombre de valeurs demandé par sa balise.
                    val complet = choixEspeceCreation(draft).all { c -> map[c.id].orEmpty().size == c.nombre }
                    if (complet) draft.copy(especeChoix = map) else null
                }

            CreationStep.HISTORIQUE ->
                (selection as? SelectionHistorique)?.let {
                    draft.copy(historique = it.historique, histoirePersonnalite = it.texte)
                }

            CreationStep.CHOIX_COMPLEMENTAIRES ->
                @Suppress("UNCHECKED_CAST")
                (selection as? Map<String, List<String>>)?.let { map ->
                    val complet = choixComplementaires(draft, map).all { c -> valeursDe(c, map).size == c.nombre }
                    if (complet) draft.copy(choixComplementaires = map) else null
                }

            CreationStep.LANGUES ->
                @Suppress("UNCHECKED_CAST")
                (selection as? List<Langue>)?.takeIf { it.size >= 3 + languesSupplementaires(draft).sumOf { c -> c.nombre } }
                    ?.let { draft.copy(langues = it) }

            CreationStep.METHODE_CARACTERISTIQUES ->
                (selection as? MethodeGenerationCaracteristiques)?.let { methode ->
                    // Valeurs standard : rien à générer, les valeurs sont posées tout de suite et
                    // l'étape VALEURS_CARACTERISTIQUES est sautée (cf. etapeEstPertinente).
                    if (methode == MethodeGenerationCaracteristiques.VALEURS_STANDARD) {
                        draft.copy(methodeCaracteristiques = methode, valeursGenerees = TablesCaracteristiques.valeursStandard)
                    } else {
                        draft.copy(methodeCaracteristiques = methode)
                    }
                }

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
                // Le nombre exact requis (Classe.sortsMineursNiveau1 / sortsPreparesNiveau1,
                // plafonné au nombre de sorts réellement disponibles dans la bibliothèque)
                // est déjà vérifié côté UI avant publication : cf. EtapeSorts.
                (selection as? SelectionSorts)?.let {
                    draft.copy(sortsMineursChoisis = it.mineurs, sortsChoisis = it.prepares)
                }

            CreationStep.NOM ->
                (selection as? String)?.takeIf { it.isNotBlank() }?.let { draft.copy(nomPersonnage = it) }

            CreationStep.PORTRAIT ->
                (selection as? String)?.takeIf { it.isNotBlank() }?.let { draft.copy(portrait = it) }

            CreationStep.RECAPITULATIF -> draft // rien à appliquer, c'est l'étape de revue finale
        }
}