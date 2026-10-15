package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character
import kotlin.random.Random

/** Résultat d'un jet de sauvegarde lancé par l'application. */
data class ResultatSauvegarde(
    val caracteristique: String,
    val dd: Int,
    // null : échec automatique (Paralysé, Étourdi… aux JS de Force et de Dextérité).
    val d20: Int?,
    val bonus: Int,
    val reussi: Boolean,
    val mode: ModeJet = ModeJet.NORMAL,
    val d20Ecarte: Int? = null,
    val raisonEchecAuto: String? = null,
) {
    val total: Int? get() = d20?.let { it + bonus }

    /** « JS Dextérité 14 (d20 11 +3) contre DD 13 : réussi », « JS Force contre DD 12 : échec automatique (Paralysé) ». */
    val texte: String
        get() {
            if (d20 == null) return "JS $caracteristique contre DD $dd : échec automatique" + (raisonEchecAuto?.let { " ($it)" } ?: "")
            val des = if (d20Ecarte != null) "d20 $d20/$d20Ecarte, ${mode.label}" else "d20 $d20"
            val signe = if (bonus >= 0) "+$bonus" else "$bonus"
            return "JS $caracteristique $total ($des $signe) contre DD $dd : ${if (reussi) "réussi" else "raté"}"
        }
}

/**
 * Jets de sauvegarde lancés par l'application pendant le combat : bonus de chaque combattant
 * (fiche du personnage, ou ligne « Caractéristiques » du bestiaire), états qui font échouer
 * (Paralysé…) ou donnent le désavantage (Entravé aux JS de Dextérité).
 */
object Sauvegardes {

    val CARACTERISTIQUES = listOf("Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme")

    /** « Dex », « dextérité », « DEXTERITE » → « Dextérité » ; null si non reconnue. */
    fun normaliser(nom: String?): String? {
        val n = nom?.trim()?.lowercase()?.replace('é', 'e')?.replace('è', 'e')?.takeIf { it.length >= 3 } ?: return null
        return CARACTERISTIQUES.firstOrNull { it.lowercase().replace('é', 'e').startsWith(n.take(3)) }
    }

    // « For 18 +4 +4 Dex 10 +0 +6 … » : valeur, modificateur, puis bonus de sauvegarde.
    private val ligneBestiaire = Regex("""\b(For|Dex|Con|Int|Sag|Cha)\s+\d+\s+([+\-−–]?\d+)\s+([+\-−–]?\d+)""")

    /** Bonus de sauvegarde d'un monstre d'après sa fiche (ligne « Caractéristiques »). */
    fun depuisFiche(rawMarkdown: String): Map<String, Int> =
        ligneBestiaire.findAll(rawMarkdown).mapNotNull { m ->
            val carac = normaliser(m.groupValues[1]) ?: return@mapNotNull null
            val bonus = m.groupValues[3].replace('−', '-').replace('–', '-').toIntOrNull() ?: return@mapNotNull null
            carac to bonus
        }.toMap()

    /**
     * Bonus de sauvegarde d'un personnage : modificateur, plus la maîtrise pour ses sauvegardes
     * maîtrisées, plus l'Aura de protection du Paladin (niv. 6) pour lui-même.
     */
    fun depuisPersonnage(c: Character): Map<String, Int> {
        val valeurs = listOf(c.strength, c.dexterity, c.constitution, c.intelligence, c.wisdom, c.charisma)
        val aura = Paladin.bonusAuraProtection(c)
        return CARACTERISTIQUES.zip(valeurs).associate { (carac, valeur) ->
            val maitrisee = maitrisee(c, carac)
            carac to Math.floorDiv(valeur - 10, 2) + (if (maitrisee) c.proficiencyBonus else 0) + aura
        }
    }

    /** JS de [carac] maîtrisé : sur la fiche, ou par une aptitude (Esprit fuyant du Roublard : Sagesse, Charisme). */
    fun maitrisee(c: Character, carac: String): Boolean =
        c.savingThrowProficiencies.any { normaliser(it) == normaliser(carac) } || normaliser(carac) in Roublard.jsMaitrisesEnPlus(c)

    /** Bonus de [c] à un JS de [caracteristique] (0 si inconnu). */
    fun bonus(c: Combattant, caracteristique: String?): Int =
        normaliser(caracteristique)?.let { c.sauvegardes[it] } ?: 0

    /**
     * Lance le JS de [c] contre [dd]. Sans caractéristique précise (« JS Force ou Dextérité »),
     * [choix] donne les candidates : le combattant prend la meilleure.
     */
    fun lancer(
        c: Combattant,
        caracteristique: String?,
        dd: Int,
        aleatoire: Random = Random,
        choix: List<String> = emptyList(),
        // Avantage accordé par une aptitude (Esprit occulte, Mage de guerre… pour la concentration).
        avantage: Boolean = false,
        // Désavantage imposé par une aptitude (don Tueur de mages sur le JS de Concentration).
        desavantageImpose: Boolean = false,
    ): ResultatSauvegarde {
        val carac = (choix.mapNotNull(::normaliser).maxByOrNull { bonus(c, it) } ?: normaliser(caracteristique))
            ?: caracteristique?.trim()?.takeIf { it.isNotEmpty() } ?: "?"
        if (Etats.echecAutomatiqueJs(c.conditions, carac)) {
            val raison = Etats.developper(c.conditions).firstOrNull { Etats.echecAutomatiqueJs(listOf(it), carac) }?.label
            return ResultatSauvegarde(carac, dd, null, bonus(c, carac), reussi = false, raisonEchecAuto = raison)
        }
        val desavantage = desavantageImpose || (carac == "Dextérité" && EffetEtat.DESAVANTAGE_JS_DEX in Etats.effets(c.conditions))
        // Avantage et désavantage s'annulent.
        val mode = when {
            desavantage && !avantage -> ModeJet.DESAVANTAGE
            avantage && !desavantage -> ModeJet.AVANTAGE
            else -> ModeJet.NORMAL
        }
        val premier = aleatoire.nextInt(1, 21)
        val second = if (mode == ModeJet.NORMAL) null else aleatoire.nextInt(1, 21)
        val d20 = when {
            second == null -> premier
            mode == ModeJet.AVANTAGE -> maxOf(premier, second)
            else -> minOf(premier, second)
        }
        val b = bonus(c, carac)
        return ResultatSauvegarde(carac, dd, d20, b, reussi = d20 + b >= dd, mode = mode, d20Ecarte = second?.let { if (d20 == premier) it else premier })
    }

    private val jsRegex = Regex("""JS\s+(\p{L}+)?\s*DD\s*(\d+)""")

    /**
     * JS d'un sort de joueur, lu dans le résumé de sa déclaration (« JS Dextérité DD 15 (moitié si
     * réussi) · 8d6 feu ») : caractéristique, DD et dégâts sur une réussite (moitié ou aucun).
     */
    fun duResume(texte: String?): JsSort? {
        val t = texte ?: return null
        val m = jsRegex.find(t) ?: return null
        val dd = m.groupValues[2].toIntOrNull() ?: return null
        return JsSort(normaliser(m.groupValues[1]), dd, moitieSiReussi = t.contains(MOITIE_SI_REUSSI))
    }

    /** Mention ajoutée au résumé d'un sort à JS dont la réussite divise les dégâts par deux. */
    const val MOITIE_SI_REUSSI = "moitié si réussi"
}

/** JS imposé par un sort : caractéristique (null si inconnue), DD, et dégâts sur une réussite. */
data class JsSort(val caracteristique: String?, val dd: Int, val moitieSiReussi: Boolean)
