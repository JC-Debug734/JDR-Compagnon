package com.jc2.jdrcompagnon.feature_combat.domain.model

import java.text.Normalizer

/** Résistances, immunités et vulnérabilités aux types de dégâts d'un combattant (radicaux : « feu », « tranch »…). */
@kotlinx.serialization.Serializable
data class DefensesDegats(
    val resistances: Set<String> = emptySet(),
    val immunites: Set<String> = emptySet(),
    val vulnerabilites: Set<String> = emptySet(),
    // États auxquels la créature est immunisée (« Immunités: poison ; Charmé, Effrayé »).
    val etats: Set<ConditionCombat> = emptySet(),
) {
    operator fun plus(autre: DefensesDegats) = DefensesDegats(
        resistances + autre.resistances, immunites + autre.immunites, vulnerabilites + autre.vulnerabilites, etats + autre.etats,
    )

    /** Multiplicateur des dégâts de [type] : 0 (immunité), ½ (résistance), 2 (vulnérabilité) ou 1. */
    fun effet(type: String?): EffetDefense {
        val t = TypesCreature.radical(type ?: return EffetDefense.AUCUN)
        if (t.isEmpty()) return EffetDefense.AUCUN
        return when {
            t in immunites -> EffetDefense.IMMUNITE
            t in resistances && t in vulnerabilites -> EffetDefense.AUCUN
            t in resistances -> EffetDefense.RESISTANCE
            t in vulnerabilites -> EffetDefense.VULNERABILITE
            else -> EffetDefense.AUCUN
        }
    }
}

enum class EffetDefense(val libelle: String) { AUCUN(""), IMMUNITE("immunité"), RESISTANCE("résistance"), VULNERABILITE("vulnérabilité") }

/** Bonus d'une arme contre certains types de créatures (Tueuse de dragons : +3d6 contre un Dragon). */
data class BonusContreType(val formule: String, val types: Set<String>, val typeDegats: String? = null, val source: String)

/**
 * Types de créatures du SRD 5.2.1 (Aberration, Bête, Céleste…) : lus dans les fiches du bestiaire
 * (ligne « Type: Mort-vivant ») ou des PNJ importés, pour les armes et capacités qui visent un type
 * (Tueuse de dragons, Masse d'anéantissement contre Fiélons et Morts-vivants, Traversée des enfers…),
 * et défenses aux types de dégâts (lignes « Résistances / Immunités / Vulnérabilités »).
 */
object TypesCreature {

    val TYPES = listOf(
        "Aberration", "Artificiel", "Bête", "Céleste", "Dragon", "Élémentaire", "Fée", "Fiélon",
        "Géant", "Humanoïde", "Monstruosité", "Mort-vivant", "Plante", "Vase",
    )

    private fun sansAccents(t: String) =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /**
     * Type canonique d'un texte (« Mort-vivant », « Fée (archifée) », « Nuée de taille M de Bêtes »,
     * « Fiélons ») ; null s'il n'est pas reconnu. Une nuée a le type de ses membres.
     */
    fun normaliser(texte: String?): String? {
        val t = sansAccents(texte?.trim() ?: return null)
        if (t.isBlank()) return null
        // Nuée : « Nuée de taille M de Bêtes » → Bête.
        val cible = (if (t.startsWith("nuee")) t.substringAfterLast(" de ") else t).filter { it.isLetter() }
        // Préfixe de 4 lettres (3 pour « fée ») : tolère pluriels et tirets (« Morts-vivants », « Fiélons »).
        return TYPES.firstOrNull { type ->
            val k = sansAccents(type).filter { it.isLetter() }
            cible.startsWith(k.take(if (k == "fee") 3 else 4))
        }
    }

    /** Type d'une fiche du bestiaire : « *Mort-vivant, taille TP, Chaotique Mauvais* » ou champ « Type: ». */
    fun depuisFiche(rawMarkdown: String): String? {
        val champ = Regex("""(?m)^Type:\s*(.+)$""").find(rawMarkdown)?.groupValues?.get(1)
        val italique = Regex("""(?m)^\*([^*,]+),""").find(rawMarkdown)?.groupValues?.get(1)
        return normaliser(champ ?: italique)
    }

    // ── Types de dégâts ──

    private val typesDegats = listOf(
        "acide", "contondant", "feu", "force", "foudre", "froid", "necrotique", "perforant", "poison",
        "psychique", "radiant", "tonnerre", "tranchant",
    )

    /** Radical d'un type de dégâts : « Nécrotiques » → « necrotique », « tranchants » → « tranchant ». */
    fun radical(type: String): String = sansAccents(type).trim().let { t -> typesDegats.firstOrNull { t.startsWith(it) } ?: "" }

    private fun listeDegats(texte: String): Set<String> =
        texte.substringBefore(';').split(',', '/').flatMap { it.split(" et ") }
            .map { radical(it) }.filter { it.isNotEmpty() }.toSet()

    /** Défenses d'une fiche : « **Résistances :** feu, froid » (et « Résistances: … » d'un livre importé). */
    fun defensesDepuisFiche(rawMarkdown: String): DefensesDegats {
        fun ligne(nom: String) = Regex("""(?m)^(?:\*\*)?$nom\s*:?(?:\*\*)?\s*:?\s*(.+)$""").find(rawMarkdown)?.groupValues?.get(1).orEmpty()
        val immunites = ligne("Immunités")
        return DefensesDegats(
            resistances = listeDegats(ligne("Résistances")),
            immunites = listeDegats(immunites),
            vulnerabilites = listeDegats(ligne("Vulnérabilités")),
            // États après le « ; » (ou toute la ligne s'il n'y a que des états).
            etats = (if (';' in immunites) immunites.substringAfter(';') else immunites)
                .split(',').mapNotNull { Etats.reconnaitre(it.trim()) }.toSet(),
        )
    }

    /** Défenses d'une fiche de personnage (résistances, immunités, vulnérabilités notées sur la fiche). */
    fun defensesPersonnage(c: com.jc2.jdrcompagnon.ui.Character): DefensesDegats = DefensesDegats(
        // Serment des Anciens niv. 7 — Aura de garde : nécrotiques, psychiques, radiants.
        resistances = (c.damageResistances + Paladin.resistancesAura(c)).map(::radical).filter { it.isNotEmpty() }.toSet() +
            Occultiste.resistancesProtecteur(c),
        immunites = c.damageImmunities.map(::radical).filter { it.isNotEmpty() }.toSet(),
        vulnerabilites = c.damageVulnerabilities.map(::radical).filter { it.isNotEmpty() }.toSet(),
        // Archifée niveau 10 — Charmantes défenses : immunité contre l'état Charmé. Paladin : Aura
        // de courage (Effrayé) et Aura de dévotion (Charmé).
        etats = c.immunitesEtats.mapNotNull { Etats.reconnaitre(it) }.toSet() +
            (if (Occultiste.immuniseCharme(c)) setOf(ConditionCombat.CHARME) else emptySet()) +
            Paladin.immunitesAura(c),
    )

    // ── Armes efficaces contre certains types ──

    /** Armes magiques du SRD 5.2.1 (armes_armures_magiques_srd521.md) qui frappent plus fort certains types. */
    private val armesSrd = listOf(
        BonusContreType("1d8", setOf("Mort-vivant"), "radiants", "Épée radieuse"),
        BonusContreType("2d6", setOf("Fiélon", "Mort-vivant"), "radiants", "Masse d'anéantissement"),
        BonusContreType("3d6", setOf("Dragon"), null, "Tueuse de dragons"),
        BonusContreType("2d6", setOf("Géant"), null, "Tueuse de géants"),
        BonusContreType("2d10", setOf("Fiélon", "Mort-vivant"), "radiants", "Vengeresse sacrée"),
    )

    // « +2d6 dégâts radiants contre Fiélons/Morts-vivants », « +3d6 dégâts … contre un Dragon ».
    private val bonusRegex = Regex("""\+\s*(\d+d\d+)\s*dégâts\s*([\p{L}]*)[^.]*?contre\s+(?:les\s+|un\s+|une\s+)?([\p{L}\-]+(?:\s*/\s*[\p{L}\-]+)*)""")

    /**
     * Bonus de l'arme [nomArme] contre des types de créatures : arme magique du SRD reconnue à son
     * nom (« Épée longue, tueuse de dragons »), sinon lu dans sa [description] (objet d'un livre).
     */
    fun bonusArme(nomArme: String?, description: String? = null): BonusContreType? {
        val nom = sansAccents(nomArme ?: return null)
        armesSrd.firstOrNull { nom.contains(sansAccents(it.source)) }?.let { return it }
        val m = bonusRegex.find(description ?: return null) ?: return null
        val types = m.groupValues[3].split('/').mapNotNull { normaliser(it) }.toSet()
        if (types.isEmpty()) return null
        return BonusContreType(m.groupValues[1], types, m.groupValues[2].takeIf { radical(it).isNotEmpty() }, nomArme)
    }
}
