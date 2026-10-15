package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot

/** Effet de Frappe malicieuse : dés d'Attaque sournoise sacrifiés ([cout]) pour un effet. */
data class OptionFrappeMalicieuse(val nom: String, val cout: Int, val effet: String)

/**
 * Règles du Roublard (SRD 5.2.1) et de ses sous-classes (Voleur du SRD, sous-classes 2024
 * importées : Âme acérée, Arnaqueur arcanique, Assassin, Voleur) appliquées par l'application :
 * - Attaque sournoise (dés selon le niveau) et Frappe malicieuse / Coups vicieux (dés sacrifiés,
 *   DD 8 + Dex + maîtrise), un effet (deux au niveau 11) ;
 * - Esquive totale (Roublard / Moine niv. 7 : JS Dex à demi-dégâts, appliqué par CombatSession) ;
 * - Esprit fuyant (niv. 15 : maîtrise des JS de Sagesse et Charisme), Insaisissable (niv. 18 :
 *   pas d'Avantage contre vous), Coup de chance (niv. 20, 1/repos court) ;
 * - Âme acérée : Lames psychiques (armes), dés d'Énergie psionique et pouvoirs ;
 * - Arnaqueur arcanique : incantation (lanceur « tiers », voir IncantationDeSousClasse), Voleur de sorts ;
 * - Assassin : Frappe éclair (dégâts = niveau), Armes envenimées, Coup meurtrier ;
 * - Voleur : Attaque furtive (Frappe malicieuse), quatre objets harmonisés (niv. 13).
 */
object Roublard {

    const val CLASSE = "Roublard"

    // Clés de Character.compteursUtilises.
    const val COUP_DE_CHANCE = "coup-de-chance"
    const val ENERGIE_PSIONIQUE = "energie-psionique-ame-aceree"
    const val MURMURES_PSYCHIQUES = "murmures-psychiques"
    const val VOILE_PSYCHIQUE = "voile-psychique"
    const val RUPTURE_SPIRITUELLE = "rupture-spirituelle"
    const val VOLEUR_DE_SORTS = "voleur-de-sorts"

    const val LAME_PSYCHIQUE = "Lame psychique"
    const val SECONDE_LAME = "Lame psychique (seconde)"

    /** Niveau de Roublard (classe « Voleur » des anciennes fiches comprise). */
    fun niveau(c: Character): Int =
        ArsenalPersonnage.niveauxParClasse(c).entries
            .filter { it.key.equals(CLASSE, ignoreCase = true) || it.key.equals("Voleur", ignoreCase = true) }
            .maxOfOrNull { it.value } ?: 0

    /** Sous-classe de Roublard (classe principale, ou « Sous-classe (Roublard) : X » en multiclassage). */
    fun sousClasse(c: Character): String =
        if (c.characterClass.equals(CLASSE, ignoreCase = true) || c.characterClass.equals("Voleur", ignoreCase = true)) c.subclass.trim()
        else Regex("""Sous-classe \(Roublard\) : (.+)""").find(c.classFeatures)?.groupValues?.get(1)?.trim().orEmpty()

    private fun niveauSousClasse(c: Character, nom: String): Int =
        niveau(c).takeIf { it >= 3 && sousClasse(c).equals(nom, ignoreCase = true) } ?: 0

    fun niveauVoleur(c: Character) = niveauSousClasse(c, "Voleur")
    fun niveauAssassin(c: Character) = niveauSousClasse(c, "Assassin")
    fun niveauAmeAceree(c: Character) = niveauSousClasse(c, "Âme acérée")
    fun niveauArnaqueur(c: Character) = niveauSousClasse(c, "Arnaqueur arcanique")

    private fun mod(score: Int) = ArsenalPersonnage.mod(score)

    /** Dés d'Attaque sournoise (d6) : 1 au niveau 1, puis un de plus tous les deux niveaux. */
    fun desAttaqueSournoise(c: Character): Int = niveau(c).let { if (it == 0) 0 else (it + 1) / 2 }

    /** DD de Frappe malicieuse, Coup meurtrier, Rupture spirituelle : 8 + Dextérité + maîtrise. */
    fun dd(c: Character): Int = 8 + mod(c.dexterity) + c.proficiencyBonus

    /** Effets de Frappe malicieuse combinables sur une même Attaque sournoise : 1, 2 au niveau 11. */
    fun effetsMax(c: Character): Int = if (niveau(c) >= 11) 2 else if (niveau(c) >= 5) 1 else 0

    /** Effets de Frappe malicieuse disponibles (Coups vicieux au niveau 14, Attaque furtive du Voleur). */
    fun optionsFrappeMalicieuse(c: Character): List<OptionFrappeMalicieuse> {
        val n = niveau(c)
        if (n < 5) return emptyList()
        val dd = dd(c)
        return buildList {
            add(
                OptionFrappeMalicieuse(
                    "Poison", 1,
                    "JS de Constitution DD $dd ou Empoisonnée 1 minute (nouveau JS à la fin de chacun de ses tours ; matériel d'empoisonneur requis)" +
                        if (niveauAssassin(c) >= 13) " ; Armes envenimées : +2d6 dégâts de poison si elle rate (ignore la Résistance au poison)." else "."
                )
            )
            add(OptionFrappeMalicieuse("Croc-en-jambe", 1, "cible de taille G ou moins : JS de Dextérité DD $dd ou À terre."))
            add(OptionFrappeMalicieuse("Repli", 1, "juste après l'attaque, déplacement de la moitié de votre Vitesse sans attaque d'Opportunité."))
            if (niveauVoleur(c) >= 9) add(OptionFrappeMalicieuse("Attaque furtive", 1, "Invisible par Furtivité : l'attaque ne vous rend pas visible si vous finissez le tour derrière un Abri supérieur ou total."))
            if (n >= 14) {
                add(OptionFrappeMalicieuse("Hébéter", 2, "JS de Constitution DD $dd ou, à son prochain tour, une seule action, action Bonus ou déplacement."))
                add(OptionFrappeMalicieuse("Aveugler", 3, "JS de Dextérité DD $dd ou Aveuglée jusqu'à la fin de son prochain tour."))
                add(OptionFrappeMalicieuse("Assommer", 6, "JS de Constitution DD $dd ou Inconsciente 1 minute (ou jusqu'à des dégâts ; nouveau JS à la fin de chacun de ses tours)."))
            }
        }
    }

    /** Esquive totale (Roublard ou Moine niv. 7) : JS de Dextérité à demi-dégâts → 0 réussi, moitié raté. */
    fun esquiveTotale(c: Character): Boolean =
        niveau(c) >= 7 || (ArsenalPersonnage.niveauxParClasse(c).entries.firstOrNull { it.key.equals("Moine", ignoreCase = true) }?.value ?: 0) >= 7

    /** Esprit fuyant (niv. 15) : maîtrise des JS de Sagesse et de Charisme. */
    fun jsMaitrisesEnPlus(c: Character): Set<String> = if (niveau(c) >= 15) setOf("Sagesse", "Charisme") else emptySet()

    /** Insaisissable (niv. 18) : aucune attaque n'a l'Avantage contre vous tant que vous n'êtes pas Neutralisé. */
    fun insaisissable(c: Character): Boolean = niveau(c) >= 18

    // ── Âme acérée : Lames psychiques ──

    /**
     * Lames psychiques (Âme acérée) : arme courante Finesse, Lancer (12/36 m), 1d6 psychiques +
     * mod. (Force ou Dextérité), botte Ouverture ; la seconde lame, en action Bonus, fait 1d4.
     */
    fun lamesPsychiques(c: Character): List<ArmeEnMain> {
        if (niveauAmeAceree(c) == 0) return emptyList()
        val modCarac = maxOf(mod(c.strength), mod(c.dexterity))
        val carac = if (mod(c.dexterity) >= mod(c.strength)) "Dex" else "For"
        fun lame(nom: String, main: String, de: String) = ArmeEnMain(
            nom = nom, main = main, bonusToucher = modCarac + c.proficiencyBonus,
            formuleDegats = de + when {
                modCarac > 0 -> " + $modCarac"
                modCarac < 0 -> " - ${-modCarac}"
                else -> ""
            },
            typeDegats = "psychiques", proprietes = "Finesse, Lancer (12/36)", botte = "Ouverture", maitrisee = true, aDistance = false,
            notes = listOf("$carac ${if (modCarac >= 0) "+" else ""}$modCarac, maîtrise +${c.proficiencyBonus}", "Main libre, disparaît après l'attaque ; lancer à 12/36 m"),
            seuilCritique = Guerrier.seuilCritique(c),
        )
        val mainSecondaireLibre = c.equippedSlots[EquipmentSlot.OFF_HAND] == null
        return listOfNotNull(
            lame(LAME_PSYCHIQUE, "Main libre", "1d6"),
            if (mainSecondaireLibre) lame(SECONDE_LAME, "Main secondaire", "1d4") else null,
        )
    }

    // ── Ressources ──

    fun ressources(c: Character): List<RessourceClasse> {
        val n = niveau(c)
        if (n == 0) return emptyList()
        return buildList {
            niveauAmeAceree(c).takeIf { it > 0 }?.let { a ->
                val (nombre, de) = RessourcesClasse.desEnergiePsionique(a)
                add(
                    RessourceClasse(ENERGIE_PSIONIQUE, "Énergie psionique", nombre, Recuperation.UNE_AU_REPOS_COURT,
                        "Savoir-faire psionique : dé ajouté à un test de compétence raté (dépensé seulement si le test réussit)." +
                            if (a >= 9) " Lames chercheuses : dé ajouté à une attaque ratée avec une Lame psychique (dépensé si elle touche). Téléportation psychique : action Bonus, téléportation à 3 m × le dé." else "",
                        de = de)
                )
                add(RessourceClasse(MURMURES_PSYCHIQUES, "Murmures psychiques (gratuit)", 1, Recuperation.REPOS_LONG,
                    "Action Magie : télépathie avec jusqu'à ${c.proficiencyBonus} créature(s) pendant 1$de heures ; la première fois après un repos long ne dépense pas de dé."))
                if (a >= 13) add(RessourceClasse(VOILE_PSYCHIQUE, "Voile psychique", 1, Recuperation.REPOS_LONG,
                    "Action Magie : Invisible 1 heure (fin si vous infligez des dégâts ou imposez un JS). Récupérable avec un dé."))
                if (a >= 17) add(RessourceClasse(RUPTURE_SPIRITUELLE, "Rupture spirituelle", 1, Recuperation.REPOS_LONG,
                    "Attaque sournoise avec une Lame psychique : JS de Sagesse DD ${dd(c)} ou Étourdie 1 minute. Récupérable avec trois dés."))
            }
            if (niveauArnaqueur(c) >= 17) add(RessourceClasse(VOLEUR_DE_SORTS, "Voleur de sorts", 1, Recuperation.REPOS_LONG,
                "Réaction après un sort qui vous cible : JS d'Intelligence contre votre DD des sorts ; raté, l'effet est annulé et vous volez le sort 8 heures."))
            if (n >= 20) add(RessourceClasse(COUP_DE_CHANCE, "Coup de chance", 1, Recuperation.REPOS_COURT,
                "Un test d20 raté devient un 20."))
        }
    }
}
