package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import com.jc2.jdrcompagnon.ui.GameState

/**
 * Chapitre d'un scénario ([GameState.MjScenario.chapitreNumero], [GameState.MjScenario.chapitreTitre])
 * et numéro du scénario dans ce chapitre ([GameState.MjScenario.numero]) : ils rangent les
 * scénarios d'une campagne dans l'ordre de jeu (« 3.2 — Vogler : la Fête de l'Alcyon »).
 *
 * Dans le fichier .md, une ligne d'en-tête `{mchapitre: numero=3; titre=Quand la maison brûle; scenario=2}`
 * (lue à l'import et à la synchronisation, réécrite par GameState.scenarioToMarkdown). Tous les
 * champs sont facultatifs : un scénario peut n'avoir qu'un chapitre, ou qu'un numéro.
 */
object ChapitresScenarios {

    const val BALISE = "mchapitre"

    data class Chapitre(val numero: Int?, val titre: String, val scenario: Int?) {
        val estVide: Boolean get() = numero == null && titre.isBlank() && scenario == null
    }

    /** Ligne {mchapitre:} du markdown, ou null s'il n'y en a pas (ou qu'elle est vide). */
    fun lire(markdown: String): Chapitre? = markdown.lineSequence()
        .firstNotNullOfOrNull { GameState.metaLineFields(it, BALISE) }
        ?.let { f ->
            Chapitre(
                numero = f["numero"]?.trim()?.toIntOrNull(),
                titre = f["titre"].orEmpty().trim(),
                scenario = f["scenario"]?.trim()?.toIntOrNull(),
            )
        }
        ?.takeUnless { it.estVide }

    fun de(scenario: GameState.MjScenario) = Chapitre(scenario.chapitreNumero, scenario.chapitreTitre, scenario.numero)

    /** Ligne {mchapitre:} à écrire pour ce scénario, ou null s'il n'a ni chapitre ni numéro. */
    fun ligne(scenario: GameState.MjScenario): String? {
        val c = de(scenario).takeUnless { it.estVide } ?: return null
        val titre = c.titre.replace("\\", "\\\\").replace(";", "\\;").replace("}", "\\}").replace("\n", " ")
        return "{$BALISE: numero=${c.numero ?: ""}; titre=$titre; scenario=${c.scenario ?: ""}}"
    }

    /** Numéro affiché devant le titre : « 3.2 », « 3 » (chapitre seul) ou « 2 » (numéro seul) ; null sinon. */
    fun numeroAffiche(scenario: GameState.MjScenario): String? = when {
        scenario.chapitreNumero != null && scenario.numero != null -> "${scenario.chapitreNumero}.${scenario.numero}"
        scenario.chapitreNumero != null -> "${scenario.chapitreNumero}"
        scenario.numero != null -> "${scenario.numero}"
        else -> null
    }

    /** Titre précédé de son numéro (« 3.2 · Trahison à Grandcolline »), sauf si le titre le porte déjà. */
    fun titreNumerote(scenario: GameState.MjScenario): String {
        val numero = numeroAffiche(scenario) ?: return scenario.title
        return if (scenario.title.trimStart().startsWith(numero)) scenario.title else "$numero · ${scenario.title}"
    }

    /** Libellé d'en-tête d'un chapitre : « Chapitre 3 — Quand la maison brûle » ; null sans chapitre. */
    fun libelleChapitre(numero: Int?, titre: String): String? = when {
        numero != null && titre.isNotBlank() -> "Chapitre $numero — $titre"
        numero != null -> "Chapitre $numero"
        titre.isNotBlank() -> titre
        else -> null
    }

    /**
     * Scénarios rangés par chapitre (numéro croissant, puis titre), et dans chaque chapitre par
     * numéro de scénario puis par titre ; les scénarios sans chapitre viennent en dernier, dans
     * leur ordre d'origine si aucun n'a de numéro. Chaque groupe porte son libellé d'en-tête (null
     * pour le groupe sans chapitre).
     */
    fun grouper(scenarios: List<GameState.MjScenario>): List<Pair<String?, List<GameState.MjScenario>>> {
        val parChapitre = scenarios.groupBy { it.chapitreNumero to it.chapitreTitre.trim().lowercase() }
        return parChapitre.entries
            .sortedWith(
                compareBy<Map.Entry<Pair<Int?, String>, List<GameState.MjScenario>>> { (cle, _) ->
                    if (cle.first == null && cle.second.isBlank()) 1 else 0
                }
                    .thenBy { (cle, _) -> cle.first ?: Int.MAX_VALUE }
                    .thenBy { (cle, _) -> cle.second }
            )
            .map { (_, liste) ->
                val premier = liste.first()
                libelleChapitre(premier.chapitreNumero, premier.chapitreTitre) to trierDansChapitre(liste)
            }
    }

    /** Ordre de jeu dans un chapitre : par numéro (les scénarios sans numéro gardent leur place relative, après). */
    fun trierDansChapitre(scenarios: List<GameState.MjScenario>): List<GameState.MjScenario> =
        scenarios.withIndex()
            .sortedWith(compareBy<IndexedValue<GameState.MjScenario>> { it.value.numero ?: Int.MAX_VALUE }.thenBy { it.index })
            .map { it.value }

    /** Liste à plat dans l'ordre des chapitres (voir [grouper]). */
    fun trier(scenarios: List<GameState.MjScenario>): List<GameState.MjScenario> = grouper(scenarios).flatMap { it.second }

    /** Vrai si au moins un scénario porte un chapitre : les listes affichent alors des en-têtes. */
    fun aDesChapitres(scenarios: List<GameState.MjScenario>): Boolean =
        scenarios.any { it.chapitreNumero != null || it.chapitreTitre.isNotBlank() }
}
