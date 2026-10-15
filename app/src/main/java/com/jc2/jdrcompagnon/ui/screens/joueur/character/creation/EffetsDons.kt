package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.spellcastingAbilityByClass

/**
 * Ce qu'apporte un don à son obtention (création ou montée de niveau), d'après ses balises
 * (dons_srd521.md ou livre importé) :
 * - effets automatiques ([Don.effets]) : `pv-par-niveau: 2` (Robuste : 2 × niveau tout de suite,
 *   puis 2 par niveau, cf. [pvParNiveauDons]), `pv-max: 40`, `vitesse: 3` (m), `formation: Armes de
 *   guerre`, `outils: …`, `sorts: …`, `competences: toutes` (seulement celles non maîtrisées),
 *   `js: augmentee` (maîtrise du JS de la caractéristique augmentée par le don, Résilient) ;
 * - choix faits ([Don.balises], valeurs retenues par id de choix) : compétences (une compétence
 *   déjà maîtrisée passe en Expertise), expertise, outils, sorts, résistances.
 */
data class EffetsDon(
    val pv: Int = 0,
    val vitesse: Int = 0,
    val competences: List<String> = emptyList(),
    // Compétences gagnées sans passage en Expertise si déjà maîtrisées (« competences: toutes »).
    val competencesSiAbsentes: List<String> = emptyList(),
    val expertises: List<String> = emptyList(),
    val outils: List<String> = emptyList(),
    val formations: List<String> = emptyList(),
    val sorts: List<String> = emptyList(),
    val resistances: List<String> = emptyList(),
    val jetsSauvegarde: List<String> = emptyList(),
) {
    val vide: Boolean get() = this == EffetsDon()

    /** Résumé lisible (« +8 PV · Vitesse +3 m · Formation : Armes de guerre »), pour la fiche. */
    fun resume(): String = listOfNotNull(
        pv.takeIf { it != 0 }?.let { "+$it PV max" },
        vitesse.takeIf { it != 0 }?.let { "Vitesse +$it m" },
        (competences + competencesSiAbsentes).takeIf { it.isNotEmpty() }?.let { "Compétences : ${it.joinToString(", ")}" },
        expertises.takeIf { it.isNotEmpty() }?.let { "Expertise : ${it.joinToString(", ")}" },
        outils.takeIf { it.isNotEmpty() }?.let { "Outils : ${it.joinToString(", ")}" },
        formations.takeIf { it.isNotEmpty() }?.let { "Formation : ${it.joinToString(", ")}" },
        sorts.takeIf { it.isNotEmpty() }?.let { "Sorts : ${it.joinToString(", ")}" },
        resistances.takeIf { it.isNotEmpty() }?.let { "Résistances : ${it.joinToString(", ")}" },
        jetsSauvegarde.takeIf { it.isNotEmpty() }?.let { "Maîtrise des JS : ${it.joinToString(", ")}" },
    ).joinToString(" · ")
}

private fun liste(valeur: String?): List<String> =
    valeur?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()

/**
 * Effets du [don] obtenu au niveau de personnage [niveau]. [choix] : valeurs retenues pour ses
 * choix balisés (clé = id de la balise, ex. « expert-maitrise »). [caracAugmentee] : caractéristique
 * augmentée par le don (nom complet, ex. « Constitution »), pour `js: augmentee`.
 */
fun effetsDon(don: Don, choix: Map<String, List<String>>, niveau: Int, caracAugmentee: String? = null): EffetsDon {
    var e = EffetsDon()
    don.effets.forEach { b ->
        b["pv-par-niveau"]?.trim()?.toIntOrNull()?.let { e = e.copy(pv = e.pv + it * niveau) }
        b["pv-max"]?.trim()?.toIntOrNull()?.let { e = e.copy(pv = e.pv + it) }
        b[EffetBalise.VITESSE]?.trim()?.replace(",", ".")?.toDoubleOrNull()?.let { e = e.copy(vitesse = e.vitesse + it.toInt()) }
        b["formation"]?.let { e = e.copy(formations = e.formations + liste(it)) }
        b[EffetBalise.OUTILS]?.let { e = e.copy(outils = e.outils + liste(it)) }
        b[EffetBalise.SORTS]?.let { e = e.copy(sorts = e.sorts + liste(it)) }
        b[EffetBalise.RESISTANCES]?.let { e = e.copy(resistances = e.resistances + liste(it)) }
        b[EffetBalise.COMPETENCES]?.let { v ->
            e = e.copy(competencesSiAbsentes = e.competencesSiAbsentes + if (v.trim().equals("toutes", true)) TOUTES_COMPETENCES else liste(v))
        }
        if (b["js"]?.trim().equals("augmentee", true) && caracAugmentee != null) e = e.copy(jetsSauvegarde = e.jetsSauvegarde + caracAugmentee)
    }
    don.balises.forEach { b ->
        val valeurs = choix[b["id"]].orEmpty()
        if (valeurs.isEmpty()) return@forEach
        e = when (b["effet"]) {
            EffetBalise.COMPETENCES -> e.copy(competences = e.competences + valeurs)
            EffetBalise.COMPETENCES_OU_OUTILS -> e.copy(
                competences = e.competences + valeurs.filter { it in TOUTES_COMPETENCES },
                outils = e.outils + valeurs.filterNot { it in TOUTES_COMPETENCES },
            )
            EffetBalise.EXPERTISE -> e.copy(expertises = e.expertises + valeurs)
            EffetBalise.OUTILS -> e.copy(outils = e.outils + valeurs)
            EffetBalise.SORTS -> e.copy(sorts = e.sorts + valeurs)
            EffetBalise.RESISTANCES -> e.copy(resistances = e.resistances + valeurs)
            else -> e
        }
    }
    return e
}

/** PV gagnés à chaque niveau grâce aux dons déjà possédés (Robuste : 2), parmi [dons] du personnage. */
fun pvParNiveauDons(dons: List<Don>): Int =
    dons.sumOf { d -> d.effets.sumOf { it["pv-par-niveau"]?.trim()?.toIntOrNull() ?: 0 } }

/**
 * Le personnage [c] a-t-il le don [nom] ? Les dons de la fiche (Character.feats) commencent
 * chacun une entrée par leur nom : « Robuste (don d'historique Fermier) : … », « Mobile (Général,
 * niveau 4) — Dextérité +1 : … », ou le repli « Don d'historique : Robuste ».
 */
fun possedeDon(c: Character, nom: String): Boolean {
    val n = Regex.escape(nom.trim())
    return Regex("""(?m)^\s*(Don d'historique\s*:\s*)?$n\s*(\(|:|—|$)""", RegexOption.IGNORE_CASE).containsMatchIn(c.feats)
}

/** Dons de [catalogue] que possède [c]. */
fun donsDuPersonnage(c: Character, catalogue: List<Don>): List<Don> = catalogue.filter { possedeDon(c, it.nom) }

private val REGEX_PREREQUIS = Regex("""\*\*Prérequis\*\*\s*:?\s*(.+)""")
private val REGEX_REPETABLE = Regex("""\*\*Répétable\*\*\s*:?\s*Oui""", RegexOption.IGNORE_CASE)

/** Le don peut-il être pris plusieurs fois (« **Répétable** : Oui ») ? */
fun donRepetable(don: Don): Boolean = REGEX_REPETABLE.containsMatchIn(don.description)

private val CARACS = listOf("Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme")

private fun valeurCarac(c: Character, nom: String): Int = when (nom) {
    "Force" -> c.strength
    "Dextérité" -> c.dexterity
    "Constitution" -> c.constitution
    "Intelligence" -> c.intelligence
    "Sagesse" -> c.wisdom
    else -> c.charisma
}

/** Le personnage a-t-il l'aptitude Sorts ou Magie de pacte (classe ou sous-classe incantatrice) ? */
private fun lanceurDeSorts(c: Character): Boolean {
    val classes = listOf(c.characterClass) + c.classesSecondaires.map { it.classe }
    return classes.any { cl -> spellcastingAbilityByClass.keys.any { it.equals(cl, ignoreCase = true) } } ||
        listOf("Chevalier occulte", "Escroc arcanique").any { c.subclass.contains(it, ignoreCase = true) || c.classFeatures.contains("Sous-classe (Guerrier) : $it") || c.classFeatures.contains("Sous-classe (Roublard) : $it") }
}

/**
 * Prérequis du [don] non remplis par [c] au niveau [niveau] (liste vide = don accessible) :
 * niveau minimal, caractéristique(s) à 13 ou plus, aptitude Sorts ou Magie de pacte, formation
 * aux armures (légères, intermédiaires, lourdes) ou aux boucliers, aptitude Style de combat.
 */
fun prerequisManquants(don: Don, c: Character, niveau: Int): List<String> {
    val texte = REGEX_PREREQUIS.find(don.description)?.groupValues?.get(1)?.trim()?.takeIf { it != "-" } ?: return emptyList()
    val manquants = mutableListOf<String>()
    Regex("""Niveau\s+(\d+)""", RegexOption.IGNORE_CASE).find(texte)?.groupValues?.get(1)?.toIntOrNull()?.let { min ->
        if (niveau < min) manquants += "niveau $min"
    }
    // « Force ou Dextérité 13 ou plus », « Intelligence, Sagesse ou Charisme 13 ou plus ».
    Regex("""((?:\p{L}+(?:,\s*|\s+ou\s+))*\p{L}+)\s+(\d+)\s+ou plus""").findAll(texte).forEach { m ->
        val noms = CARACS.filter { m.groupValues[1].contains(it, ignoreCase = true) }
        val seuil = m.groupValues[2].toInt()
        if (noms.isNotEmpty() && noms.none { valeurCarac(c, it) >= seuil }) manquants += "${noms.joinToString(" ou ")} $seuil"
    }
    if (texte.contains("Sorts ou Magie de pacte", ignoreCase = true) && !lanceurDeSorts(c)) manquants += "aptitude Sorts ou Magie de pacte"
    val formation = c.weaponArmorTraining.lowercase()
    Regex("""formations? aux ([\p{L} ]+?)(?:,|$)""", RegexOption.IGNORE_CASE).findAll(texte).forEach { m ->
        val cible = m.groupValues[1].trim().lowercase()
        val ok = when {
            cible.contains("bouclier") -> formation.contains("bouclier")
            cible.contains("lourde") -> formation.contains("lourde")
            cible.contains("intermédiaire") -> formation.contains("intermédiaire") || formation.contains("lourde")
            cible.contains("légère") -> formation.contains("légère") || formation.contains("intermédiaire") || formation.contains("lourde")
            else -> true
        }
        if (!ok) manquants += "formation aux ${m.groupValues[1].trim()}"
    }
    return manquants
}

/** Expertises possibles : compétences maîtrisées, pas encore en Expertise. */
fun competencesPourExpertise(c: Character, gagnees: List<String> = emptyList()): List<String> =
    (c.skillProficiencies + gagnees).distinct().filter { s -> c.skillExpertise.none { it.equals(s, ignoreCase = true) } }

/**
 * Fiche [this] avec les effets [e] d'un don ([source] : son nom) : vitesse, compétences (une
 * compétence choisie déjà maîtrisée passe en Expertise), expertises, outils, formations aux armes
 * et armures, sorts (toujours préparés), résistances, maîtrises de JS. Les PV ne sont ajoutés que
 * si [avecPv] (à la montée de niveau, ils sont déjà comptés dans les PV gagnés).
 */
fun Character.avecEffetsDon(e: EffetsDon, source: String, avecPv: Boolean = false): Character {
    if (e.vide) return this
    val absentes = e.competencesSiAbsentes.filter { s -> skillProficiencies.none { it.equals(s, ignoreCase = true) } }
    val (maitrises, expertisesCumul) = com.jc2.jdrcompagnon.ui.cumulerMaitrisesCompetences(
        skillProficiencies + absentes, skillExpertise, e.competences,
    )
    val expertises = (expertisesCumul + e.expertises.filter { s -> maitrises.any { it.equals(s, ignoreCase = true) } }).distinct()
    val nouveauxSorts = e.sorts.filter { s -> spells.none { it.equals(s, ignoreCase = true) } }
    val pvEnPlus = if (avecPv) e.pv else 0
    return copy(
        maxHitPoints = maxHitPoints + pvEnPlus,
        currentHitPoints = currentHitPoints + pvEnPlus,
        speed = speed + e.vitesse,
        skillProficiencies = maitrises,
        skillExpertise = expertises,
        proficiencies = (listOf(proficiencies).filter { it.isNotBlank() } + e.outils.filter { !proficiencies.contains(it, ignoreCase = true) })
            .joinToString(", "),
        weaponArmorTraining = if (e.formations.isEmpty()) weaponArmorTraining
        else listOf(weaponArmorTraining, "$source : ${e.formations.joinToString(", ")}").filter { it.isNotBlank() }.joinToString("\n"),
        spells = spells + nouveauxSorts,
        spellSources = spellSources + nouveauxSorts.associateWith { "Don : $source (toujours préparé)" },
        preparedSpells = preparedSpells?.let { (it + nouveauxSorts).distinct() },
        damageResistances = (damageResistances + e.resistances).distinct(),
        savingThrowProficiencies = (savingThrowProficiencies + e.jetsSauvegarde).distinct(),
    )
}
