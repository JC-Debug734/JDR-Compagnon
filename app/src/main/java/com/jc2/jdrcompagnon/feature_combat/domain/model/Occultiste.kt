package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character

/**
 * Règles de l'Occultiste (SRD 5.2.1) appliquées par l'application :
 * - Manifestations occultes choisies (lues dans les aptitudes de la fiche, « Manifestation
 *   occulte (Niveau N) — Armure d’ombres : … ») : sorts lançables sans emplacement, Attaque
 *   supplémentaire de Lame assoiffée / Lame dévorante, Charisme de l'arme du Pacte de la lame,
 *   Charisme ajouté aux dégâts de Décharge occulte (Décharge déchirante) ;
 * - Rouerie magique / Maître de l'occulte (emplacements de pacte récupérés) ;
 * - Protecteur Fiélon : Bénédiction du ténébreux, Chance du ténébreux, Résistance fiélonne,
 *   Traversée des enfers (en combat, voir CombatSession).
 */
object Occultiste {

    const val CLASSE = "Occultiste"

    fun niveau(c: Character): Int =
        ArsenalPersonnage.niveauxParClasse(c).entries.firstOrNull { it.key.equals(CLASSE, ignoreCase = true) }?.value ?: 0

    /** Sous-classe d'Occultiste (classe principale, ou « Sous-classe (Occultiste) : X » en multiclassage). */
    private fun sousClasse(c: Character): String =
        if (c.characterClass.equals(CLASSE, ignoreCase = true)) c.subclass
        else Regex("""Sous-classe \(Occultiste\) : (.+)""").find(c.classFeatures)?.groupValues?.get(1).orEmpty()

    /** Niveau d'Occultiste si le personnage a le Protecteur Fiélon, 0 sinon. */
    fun niveauFielon(c: Character): Int =
        niveau(c).takeIf { sousClasse(c).contains("Fiélon", ignoreCase = true) || sousClasse(c).contains("Fielon", ignoreCase = true) } ?: 0

    private fun modCha(c: Character) = ArsenalPersonnage.mod(c.charisma)

    /** DD de sauvegarde des sorts d'Occultiste : 8 + maîtrise + Charisme. */
    fun ddSorts(c: Character): Int = 8 + c.proficiencyBonus + modCha(c)

    // ── Manifestations occultes ──

    private val manifestationRegex = Regex("""Manifestation occulte[^\n—]*—\s*([^:\n]+?)\s*:""")

    /** Manifestations occultes choisies, d'après les aptitudes notées sur la fiche. */
    fun manifestations(c: Character): List<String> =
        if (niveau(c) == 0) emptyList()
        else manifestationRegex.findAll(c.classFeatures).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()

    fun a(c: Character, manifestation: String): Boolean =
        manifestations(c).any { normaliser(it) == normaliser(manifestation) }

    private fun normaliser(t: String) = t.lowercase().replace('’', '\'').trim()

    /**
     * Sorts que les manifestations permettent de lancer sans emplacement : à volonté
     * (Armure d’ombres → armure du mage…), ou une fois par repos long (Présent des profondeurs).
     */
    private val sortsParManifestation = mapOf(
        "Armure d'ombres" to ("Armure du mage" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Maître des formes" to ("Modification d’apparence" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Maître des ombres" to ("Invisibilité" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Mille visages" to ("Déguisement" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Murmures de la tombe" to ("Communication avec les morts" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Pas aérien" to ("Lévitation" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Royaumes lointains" to ("Œil du mage" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Saut d'outre-monde" to ("Saut" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Vigueur fiélonne" to ("Simulacre de vie" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Visions embrumées" to ("Image silencieuse" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Pacte de la chaîne" to ("Appel de familier" to ArsenalPersonnage.SORT_A_VOLONTE),
        "Présent des profondeurs" to ("Respiration aquatique" to ArsenalPersonnage.SORT_ARCANUM),
    ).mapKeys { normaliser(it.key) }

    /** Sorts spéciaux accordés par les manifestations choisies (nom du sort → mode). */
    fun sortsDesManifestations(c: Character): Map<String, String> =
        manifestations(c).mapNotNull { sortsParManifestation[normaliser(it)] }.toMap()

    /** Attaque supplémentaire de l'arme de pacte : 2 avec Lame assoiffée, 3 avec Lame dévorante (0 sinon). */
    fun attaquesArmeDePacte(c: Character): Int = when {
        c.armeDePacte.isBlank() -> 0
        a(c, "Lame dévorante") && niveau(c) >= 12 -> 3
        a(c, "Lame assoiffée") && niveau(c) >= 5 -> 2
        else -> 0
    }

    /**
     * Avantage aux JS de Constitution pour maintenir la Concentration : manifestation Esprit
     * occulte, ou don Mage de guerre (tout lanceur de sorts).
     */
    fun avantageConcentration(c: Character): Boolean = raisonAvantageConcentration(c) != null

    fun raisonAvantageConcentration(c: Character): String? = when {
        a(c, "Esprit occulte") -> "Esprit occulte"
        c.feats.contains("Mage de guerre", ignoreCase = true) -> "Mage de guerre"
        else -> null
    }

    /** [nomArme] est-elle l'arme de pacte du personnage (invoquée ou liée par le Pacte de la lame) ? */
    fun estArmeDePacte(c: Character, nomArme: String): Boolean =
        a(c, "Pacte de la lame") && c.armeDePacte.isNotBlank() && c.armeDePacte.equals(nomArme, ignoreCase = true)

    /**
     * Pacte de la lame : l'arme de pacte (et elle seule) peut utiliser le Charisme au lieu de la
     * Force ou de la Dextérité ; null pour toute autre arme.
     */
    fun modArmeDePacte(c: Character, nomArme: String): Int? = if (estArmeDePacte(c, nomArme)) modCha(c) else null

    /** Décharge déchirante : Charisme ajouté aux dégâts de Décharge occulte. */
    fun bonusDechargeDechirante(c: Character, sort: String): Int? =
        modCha(c).takeIf { a(c, "Décharge déchirante") && ArsenalPersonnage.memeSort(sort, "Décharge occulte") }

    // ── Rouerie magique ──

    /** Rouerie magique (niveau 2) disponible : pas encore utilisée depuis le dernier repos long. */
    fun rouerieDisponible(c: Character): Boolean = niveau(c) >= 2 && !c.rouerieMagiqueUtilisee

    /**
     * Emplacements de pacte dépensés après Rouerie magique : on en regagne jusqu'à la moitié du
     * maximum (arrondie au supérieur), ou tous au niveau 20 (Maître de l'occulte).
     */
    fun apresRouerie(c: Character, maximumPacte: Int): Int =
        if (niveau(c) >= 20) 0 else (c.pactSlotsUsed - (maximumPacte + 1) / 2).coerceAtLeast(0)

    // ── Protecteur Fiélon ──

    /** Bénédiction du ténébreux (niveau 3) : PV temporaires = Charisme + niveau d'Occultiste (1 au minimum). */
    fun pvBenediction(c: Character): Int = if (niveauFielon(c) >= 3) (modCha(c) + niveau(c)).coerceAtLeast(1) else 0

    /** Chance du ténébreux (niveau 6) : utilisations restantes (Charisme, 1 au minimum, par repos long). */
    fun chanceRestante(c: Character): Int =
        if (niveauFielon(c) >= 6) (modCha(c).coerceAtLeast(1) - c.chanceTenebreuxUtilisees).coerceAtLeast(0) else 0

    /** Résistance fiélonne (niveau 10) : type de dégâts choisi au dernier repos, ou null. */
    fun resistanceFielonne(c: Character): String? = c.resistanceFielonne.takeIf { niveauFielon(c) >= 10 && it.isNotBlank() }

    /** Types de dégâts proposés pour Résistance fiélonne (tous sauf force). */
    val TYPES_RESISTANCE = listOf(
        "acide", "contondants", "feu", "foudre", "froid", "nécrotiques", "perforants", "poison", "psychiques", "radiants", "tonnerre", "tranchants",
    )

    /** Les dégâts de [type] sont-ils réduits de moitié par la Résistance fiélonne ? */
    fun resiste(c: Character, type: String?): Boolean {
        val choisi = resistanceFielonne(c) ?: return false
        val t = type?.lowercase()?.trim() ?: return false
        return t.startsWith(choisi.lowercase().take(5))
    }

    /** Traversée des enfers (niveau 14) disponible : une fois par repos long (ou contre un emplacement de pacte). */
    fun traverseeDisponible(c: Character): Boolean = niveauFielon(c) >= 14 && !c.traverseeEnfersUtilisee

    // ── Autres protecteurs (livre de sous-classes 2024) ──

    private fun niveauProtecteur(c: Character, vararg mots: String): Int =
        niveau(c).takeIf { n -> n > 0 && mots.any { sousClasse(c).contains(it, ignoreCase = true) } } ?: 0

    fun niveauArchifee(c: Character): Int = niveauProtecteur(c, "Archifée", "Archifee")
    fun niveauCeleste(c: Character): Int = niveauProtecteur(c, "Céleste", "Celeste")
    fun niveauGrandAncien(c: Character): Int = niveauProtecteur(c, "Grand Ancien")

    /** Archifée — Foulée des fées (niveau 3) : foulée brumeuse sans emplacement, Charisme fois (1 au minimum) par repos long. */
    fun fouleesFeesMax(c: Character): Int = if (niveauArchifee(c) >= 3) modCha(c).coerceAtLeast(1) else 0
    fun fouleesFeesRestantes(c: Character): Int = (fouleesFeesMax(c) - c.fouleesFeesUtilisees).coerceAtLeast(0)

    /** Archifée — Charmantes défenses (niveau 10) : immunité contre l'état Charmé, et riposte 1/repos long. */
    fun immuniseCharme(c: Character): Boolean = niveauArchifee(c) >= 10
    fun charmantesDefensesDisponibles(c: Character): Boolean = niveauArchifee(c) >= 10 && !c.charmantesDefensesUtilisees

    /** Céleste — Lumière guérisseuse (niveau 3) : réserve de d6 = 1 + niveau d'Occultiste, Charisme dés au plus par soin. */
    fun reserveLumiere(c: Character): Int = if (niveauCeleste(c) >= 3) 1 + niveau(c) else 0
    fun lumiereRestante(c: Character): Int = (reserveLumiere(c) - c.lumiereGuerisseuseUtilises).coerceAtLeast(0)
    fun desMaxParSoin(c: Character): Int = modCha(c).coerceAtLeast(1)

    /** Céleste — Âme radiante (niveau 6) : Charisme ajouté aux dégâts radiants ou de feu de ses sorts (une cible par tour). */
    fun bonusAmeRadiante(c: Character, typeDegats: String?): Int? =
        modCha(c).takeIf { niveauCeleste(c) >= 6 && TypesCreature.radical(typeDegats.orEmpty()) in setOf("radiant", "feu") }

    /** Céleste — Résilience céleste (niveau 10) : PV temporaires à chaque repos et Rouerie magique. */
    fun pvResilienceCeleste(c: Character): Int = if (niveauCeleste(c) >= 10) niveau(c) + modCha(c) else 0
    fun pvResilienceAllies(c: Character): Int = if (niveauCeleste(c) >= 10) (niveau(c) / 2 + modCha(c)).coerceAtLeast(0) else 0

    /** Céleste — Vengeance aveuglante (niveau 14), 1/repos long : remplace un jet contre la mort (soi ou allié à 18 m). */
    fun vengeanceDisponible(c: Character): Boolean = niveauCeleste(c) >= 14 && !c.vengeanceAveuglanteUtilisee

    /** Grand Ancien — Combattant clairvoyant (niveau 6), 1/repos court ou long. */
    fun clairvoyantDisponible(c: Character): Boolean = niveauGrandAncien(c) >= 6 && !c.combattantClairvoyantUtilise

    /** Grand Ancien — Bouclier mental (niveau 10) : Résistance aux dégâts psychiques, renvoyés à l'assaillant. */
    fun bouclierMental(c: Character): Boolean = niveauGrandAncien(c) >= 10

    /** Résistances accordées par le protecteur : radiants (Âme radiante), psychiques (Bouclier mental). */
    fun resistancesProtecteur(c: Character): Set<String> = buildSet {
        if (niveauCeleste(c) >= 6) add("radiant")
        if (bouclierMental(c)) add("psychique")
    }
}
