package com.jc2.jdrcompagnon.ui.screens.joueur.character

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import java.text.Normalizer

/**
 * Harmonisation des objets magiques (champ **Harmonisation** de l'objet, voir EquipmentParser),
 * suivie par personnage dans [Character.attunedItems].
 *
 * - Un objet à harmonisation peut être porté/équipé sans harmonisation, mais ses propriétés
 *   magiques (pouvoir, charges) restent inactives tant que le porteur n'est pas harmonisé ([estActif]).
 * - Au plus 3 objets à la fois ([maximum], davantage pour l'Artificier), jamais deux exemplaires
 *   du même objet.
 * - S'harmoniser demande un repos court entier consacré à l'objet, un seul objet par repos court,
 *   l'objet dans l'inventaire et le prérequis éventuel rempli (classe, lanceur de sorts, espèce) :
 *   proposé dans la carte Repos court de l'écran Repos ([verifier]).
 * - Fin : volontaire (repos court), objet qui quitte l'inventaire (jeté, donné, consommé, détruit —
 *   automatique, cf. GameState.releaseLostAttunements), prérequis perdu ([prerequisRempli]), ou
 *   décision du MJ (mort, éloignement de plus de 30 m pendant 24 h, harmonisation par un tiers).
 */
object Harmonisation {

    const val MAXIMUM_STANDARD = 3

    private val classes = listOf(
        "barbare", "barde", "clerc", "druide", "ensorceleur", "guerrier", "magicien", "moine",
        "occultiste", "paladin", "rodeur", "roublard", "artificier",
    )
    private val classesLanceurs = setOf(
        "barde", "clerc", "druide", "ensorceleur", "magicien", "occultiste", "paladin", "rodeur", "artificier",
    )
    private val especes = listOf(
        "nain", "elfe", "halfelin", "gnome", "humain", "orc", "drakeide", "tieffelin", "tiefelin", "goliath", "aasimar",
    )

    /** Nombre maximum d'objets harmonisés : 3, ou 4/5/6 pour un Artificier de niveau 10/14/18. */
    fun maximum(character: Character): Int {
        val artificier = niveauDansClasse(character, "artificier")
        return when {
            artificier >= 18 -> 6
            artificier >= 14 -> 5
            artificier >= 10 -> 4
            else -> MAXIMUM_STANDARD
        }
    }

    fun estHarmonise(character: Character, nom: String): Boolean = nom in character.attunedItems

    /** Les propriétés magiques de [item] fonctionnent-elles pour ce personnage ? */
    fun estActif(character: Character, item: EquipmentItem): Boolean =
        !item.harmonisation || (estHarmonise(character, item.name) && prerequisRempli(character, item) != false)

    /** Libellé de l'exigence : « Harmonisation requise (magicien) ». Vide si l'objet n'en demande pas. */
    fun libelle(item: EquipmentItem): String = when {
        !item.harmonisation -> ""
        item.harmonisationPrerequis.isBlank() -> "Harmonisation requise"
        else -> "Harmonisation requise (${item.harmonisationPrerequis})"
    }

    /**
     * Le personnage remplit-il le prérequis de [item] ? true/false quand il a pu être vérifié
     * (classe, lanceur de sorts, espèce), null quand l'application ne sait pas le vérifier
     * (alignement, autre condition) — au MJ d'arbitrer. Alternatives séparées par « ou ».
     */
    fun prerequisRempli(character: Character, item: EquipmentItem): Boolean? {
        val prerequis = normaliser(item.harmonisationPrerequis)
        if (prerequis.isBlank()) return true
        val resultats = prerequis.split(Regex("""\s+ou\s+|,""")).map { verifierAlternative(character, it) }
        return when {
            resultats.any { it == true } -> true
            resultats.all { it == false } -> false
            else -> null
        }
    }

    private fun verifierAlternative(character: Character, alternative: String): Boolean? {
        val mots = Regex("""[a-z]+""").findAll(alternative).map { it.value }.toList()
        classes.firstOrNull { c -> mots.any { it == c || it == c + "s" } }?.let { classe ->
            return niveauDansClasse(character, classe) > 0
        }
        if ("lanceur" in mots || "lanceuse" in mots || "incantateur" in mots) {
            return toutesClasses(character).any { it in classesLanceurs } || character.spells.isNotEmpty()
        }
        especes.firstOrNull { e -> mots.any { it == e || it == e + "s" } }?.let { espece ->
            return normaliser(character.race).contains(espece)
        }
        return null
    }

    /**
     * Raison pour laquelle [character] ne peut pas s'harmoniser avec [item] pendant ce repos
     * court, null s'il le peut.
     */
    fun verifier(character: Character, item: EquipmentItem): String? = when {
        !item.harmonisation -> "${item.name} ne nécessite pas d'harmonisation."
        estHarmonise(character, item.name) ->
            "${character.name} est déjà harmonisé avec un exemplaire de ${item.name} (pas de doublon possible)."
        item.name !in ObjetsACharges.objetsPortes(character) ->
            "${item.name} doit être dans l'inventaire (à portée de main pendant tout le repos)."
        character.attunedItems.size >= maximum(character) ->
            "Limite atteinte : ${maximum(character)} objets harmonisés au maximum. Rompez d'abord une harmonisation."
        prerequisRempli(character, item) == false ->
            "Prérequis non rempli : ${item.name} demande une harmonisation avec ${item.harmonisationPrerequis}."
        else -> null
    }

    private fun toutesClasses(character: Character): List<String> =
        (listOf(character.characterClass) + character.classesSecondaires.map { it.classe }).map(::normaliser)

    private fun niveauDansClasse(character: Character, classe: String): Int {
        val secondaires = character.classesSecondaires
        val principale = if (normaliser(character.characterClass) == classe)
            (character.level - secondaires.sumOf { it.niveau }).coerceAtLeast(1) else 0
        return principale + secondaires.filter { normaliser(it.classe) == classe }.sumOf { it.niveau }
    }

    private fun normaliser(texte: String): String =
        Normalizer.normalize(texte.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "").trim()
}
