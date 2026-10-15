package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character

/**
 * Règles du Paladin (SRD 5.2.1) appliquées par l'application, au-delà d'Imposition des mains et
 * de Châtiment de paladin (voir ArsenalPersonnage) :
 * - Conduit divin (niv. 3) : 2 utilisations (3 au niveau 11), une récupérée au repos court, toutes
 *   au repos long — Perception divine, Arme sacrée (Dévotion), Abjuration d'ennemis (niv. 9) ;
 * - Fidèle destrier (niv. 5) : Appel de destrier toujours préparé, une fois sans emplacement par repos long ;
 * - Aura de protection (niv. 6) : + mod. Charisme (min. +1) aux JS, appliqué aux JS du Paladin ;
 * - Aura de courage (niv. 10) et Aura de dévotion (Dévotion niv. 7) : immunités Effrayé / Charmé ;
 * - Frappes radiantes (niv. 11) : +1d8 radiants aux attaques au corps à corps qui touchent ;
 * - Toucher réparateur (niv. 14) : Imposition des mains retire aussi d'autres états (5 points chacun) ;
 * - Serments (SRD, et sous-classes 2024 importées) :
 *   - Dévotion : Arme sacrée (Cha au toucher), Aura de dévotion, Châtiment de protection, Nimbe sacré ;
 *   - Anciens : Courroux de la nature, Aura de garde (résistances), Sentinelle impérissable,
 *     Champion des Anciens (régénération) ;
 *   - Gloire : Athlète d'exception, Châtiment d'exaltation, Aura d'entrain (+3 m), Défense
 *     glorieuse (utilisations), Légende vivante ;
 *   - Vengeance : Vœu d'hostilité, Vengeur implacable, Esprit de la vengeance, Ange vengeur.
 *   L'aptitude de niveau 20 de chaque serment s'utilise 1/repos long, ou se récupère avec un
 *   emplacement du 5e niveau. Les effets d'aura en combat sont appliqués par CombatSession.
 */
object Paladin {

    const val CLASSE = "Paladin"

    /** Effets actifs de la fiche (EffetActif.nom) posés par les aptitudes du Paladin. */
    const val ARME_SACREE = "Arme sacrée"
    const val NIMBE_SACRE = "Nimbe sacré"
    const val CHAMPION_DES_ANCIENS = "Champion des Anciens"
    const val LEGENDE_VIVANTE = "Légende vivante"
    const val ANGE_VENGEUR = "Ange vengeur"
    const val ATHLETE_D_EXCEPTION = "Athlète d'exception"
    const val VOEU_D_HOSTILITE = "Vœu d'hostilité"

    /** Durée d'Arme sacrée et de Nimbe sacré : 10 minutes. */
    const val DUREE_10_MINUTES = 600L

    const val SORT_FIDELE_DESTRIER = "Appel de destrier"

    /** Coût en Puissance curative d'un état retiré par Imposition des mains. */
    const val COUT_ETAT = 5

    fun niveau(c: Character): Int =
        ArsenalPersonnage.niveauxParClasse(c).entries.firstOrNull { it.key.equals(CLASSE, ignoreCase = true) }?.value ?: 0

    /** Sous-classe de Paladin (classe principale, ou « Sous-classe (Paladin) : X » en multiclassage). */
    private fun sousClasse(c: Character): String =
        if (c.characterClass.equals(CLASSE, ignoreCase = true)) c.subclass
        else Regex("""Sous-classe \(Paladin\) : (.+)""").find(c.classFeatures)?.groupValues?.get(1).orEmpty()

    /** Niveau de Paladin si la sous-classe contient l'un des [mots], 0 sinon. */
    private fun niveauSerment(c: Character, vararg mots: String): Int =
        niveau(c).takeIf { mots.any { sousClasse(c).contains(it, ignoreCase = true) } } ?: 0

    /** Niveau de Paladin selon le serment prêté (0 pour un autre serment). */
    fun niveauDevotion(c: Character): Int = niveauSerment(c, "Dévotion", "Devotion")
    fun niveauAnciens(c: Character): Int = niveauSerment(c, "Anciens")
    fun niveauGloire(c: Character): Int = niveauSerment(c, "Gloire")
    fun niveauVengeance(c: Character): Int = niveauSerment(c, "Vengeance")

    fun modCha(c: Character): Int = ArsenalPersonnage.mod(c.charisma)

    /** DD de sauvegarde des sorts (et du Conduit divin) du Paladin : 8 + maîtrise + Charisme. */
    fun ddSorts(c: Character): Int = 8 + c.proficiencyBonus + modCha(c)

    // ── Conduit divin ──

    fun conduitDivinMax(c: Character): Int = when {
        niveau(c) >= 11 -> 3
        niveau(c) >= 3 -> 2
        else -> 0
    }

    fun conduitDivinRestants(c: Character): Int = (conduitDivinMax(c) - c.conduitDivinUtilises).coerceAtLeast(0)

    /** Abjuration d'ennemis (niv. 9) : nombre de créatures visées, mod. Charisme (min. 1). */
    fun ciblesAbjuration(c: Character): Int = modCha(c).coerceAtLeast(1)

    // ── Auras ──

    /** Aura de protection (niv. 6) : bonus aux JS, mod. Charisme (min. +1) ; 0 sans l'aptitude. */
    fun bonusAuraProtection(c: Character): Int = if (niveau(c) >= 6) modCha(c).coerceAtLeast(1) else 0

    /** Rayon de l'Aura de protection : 3 m, 9 m avec Aura étendue (niv. 18). */
    fun rayonAura(c: Character): Int = if (niveau(c) >= 18) 9 else 3

    /** États contre lesquels les auras immunisent le Paladin (et ses alliés dans l'aura). */
    fun immunitesAura(c: Character): Set<ConditionCombat> = buildSet {
        if (niveau(c) >= 10) add(ConditionCombat.EFFRAYE)
        if (niveauDevotion(c) >= 7) add(ConditionCombat.CHARME)
    }

    // ── Autres aptitudes ──

    /** Frappes radiantes (niv. 11) : +1d8 radiants aux attaques au corps à corps et à mains nues. */
    fun aFrappesRadiantes(c: Character): Boolean = niveau(c) >= 11

    /**
     * États que l'Imposition des mains peut retirer (5 points chacun) : Empoisonné, plus Assourdi,
     * Aveuglé, Charmé, Effrayé, Étourdi et Paralysé avec Toucher réparateur (niv. 14).
     */
    fun etatsRetirables(c: Character): List<ConditionCombat> =
        listOf(ConditionCombat.EMPOISONNE) + if (niveau(c) >= 14) listOf(
            ConditionCombat.SOURD, ConditionCombat.AVEUGLE, ConditionCombat.CHARME,
            ConditionCombat.EFFRAYE, ConditionCombat.ETOURDI, ConditionCombat.PARALYSE,
        ) else emptyList()

    /** Fidèle destrier (niv. 5) : Appel de destrier lançable une fois sans emplacement par repos long. */
    fun sortsSpeciaux(c: Character): Map<String, String> =
        if (niveau(c) >= 5) mapOf(SORT_FIDELE_DESTRIER to ArsenalPersonnage.SORT_ARCANUM) else emptyMap()

    /** Sorts accordés par une aptitude de Paladin (classe à leur attribuer en multiclassage). */
    fun estSortDePaladin(nom: String): Boolean =
        ArsenalPersonnage.memeSort(nom, ArsenalPersonnage.SORT_CHATIMENT_DIVIN) || ArsenalPersonnage.memeSort(nom, SORT_FIDELE_DESTRIER)

    fun effetActif(c: Character, nom: String): Boolean = c.effetsActifs.any { it.nom.equals(nom, ignoreCase = true) }

    /** Arme sacrée active : + mod. Charisme (min. +1) au toucher de l'arme de corps à corps tenue. */
    fun bonusArmeSacree(c: Character): Int = if (effetActif(c, ARME_SACREE)) modCha(c).coerceAtLeast(1) else 0

    // ── Aptitude de niveau 20 du serment ──

    /** Aptitude de niveau 20 du serment et sa durée en secondes (null sans serment reconnu ou avant le niveau 20). */
    fun aptitude20(c: Character): Pair<String, Long>? = when {
        niveauDevotion(c) >= 20 -> NIMBE_SACRE to DUREE_10_MINUTES
        niveauAnciens(c) >= 20 -> CHAMPION_DES_ANCIENS to 60L
        niveauGloire(c) >= 20 -> LEGENDE_VIVANTE to DUREE_10_MINUTES
        niveauVengeance(c) >= 20 -> ANGE_VENGEUR to DUREE_10_MINUTES
        else -> null
    }

    fun aptitude20Disponible(c: Character): Boolean = aptitude20(c) != null && !c.aptitude20Utilisee

    /** Nimbe sacré : dégâts radiants infligés aux ennemis dans l'aura au début de leur tour. */
    fun degatsNimbe(c: Character): Int = modCha(c) + c.proficiencyBonus

    /** Champion des Anciens (Régénération) : PV regagnés au début de chacun de vos tours. */
    const val REGENERATION_CHAMPION = 10

    // ── Serment des Anciens ──

    /** Aura de garde (Anciens niv. 7) : Résistance aux dégâts nécrotiques, psychiques et radiants. */
    fun resistancesAura(c: Character): List<String> =
        if (niveauAnciens(c) >= 7) listOf("nécrotiques", "psychiques", "radiants") else emptyList()

    /** Sentinelle impérissable (Anciens niv. 15), 1/repos long : à 0 PV, on reste à 1 PV et regagne 3 × niveau. */
    fun sentinelleDisponible(c: Character): Boolean = niveauAnciens(c) >= 15 && !c.sentinelleUtilisee

    fun pvSentinelle(c: Character): Int = 3 * niveau(c)

    // ── Serment de Gloire ──

    /** Aura d'entrain (Gloire niv. 7) : +3 m de Vitesse (alliés dans l'aura aussi, jusqu'à la fin de leur tour suivant). */
    fun bonusVitesse(c: Character): Int = if (niveauGloire(c) >= 7) 3 else 0

    /** Défense glorieuse (Gloire niv. 15) : utilisations par repos long, mod. Charisme (min. 1). */
    fun defenseGlorieuseMax(c: Character): Int = if (niveauGloire(c) >= 15) modCha(c).coerceAtLeast(1) else 0

    fun defenseGlorieuseRestantes(c: Character): Int = (defenseGlorieuseMax(c) - c.defenseGlorieuseUtilisees).coerceAtLeast(0)

    /** Le personnage a-t-il des aptitudes de Paladin suivies par la carte « Aptitudes du Paladin » ? */
    fun aAptitudes(c: Character): Boolean = niveau(c) >= 3
}
