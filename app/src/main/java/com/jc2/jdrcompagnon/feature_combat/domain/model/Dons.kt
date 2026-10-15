package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.possedeDon

/**
 * Règles des dons (Manuel des joueurs 2024, dons du SRD et dons importés) appliquées par
 * l'application en jeu et en combat. Un don est reconnu à son nom en tête d'une entrée des dons
 * de la fiche ([possedeDon]) ; un style de combat (Interception, Protection…) l'est aussi parmi les
 * aptitudes de classe ([Guerrier.aStyle]).
 *
 * Les effets permanents (PV de Robuste, Vitesse de Mobile, maîtrises, résistances, sorts…) sont
 * écrits sur la fiche à l'obtention du don (voir effetsDon / avecEffetsDon). Ici : ce qui dépend
 * de la situation — initiative, CA, dégâts d'armes, réduction des dégâts, jets contre la mort,
 * ressources à utilisations limitées et rappels des bénéfices en combat.
 */
object Dons {

    // Clés de Character.compteursUtilises (ressources des dons).
    const val CHANCE = "don:points-de-chance"
    const val BAROUD = "don:baroud-d-honneur"
    const val REGAIN_VITALITE = "don:regain-de-vitalite"
    const val ESPRIT_PRESERVE = "don:esprit-preserve"
    const val RITUEL_RAPIDE = "don:rituel-rapide"
    const val SORT_FEERIQUE = "don:affinite-feerique"
    const val SORT_OMBREUX = "don:affinite-ombreuse"
    const val DETECTION_PENSEES = "don:telepathe"
    const val INITIE_SORT = "don:initie-a-la-magie"

    fun a(c: Character, nom: String): Boolean = possedeDon(c, nom)

    /** Style de combat (don) choisi par une aptitude de classe ou noté parmi les dons. */
    fun style(c: Character, nom: String): Boolean = Guerrier.aStyle(c, nom)

    private fun mod(score: Int) = Math.floorDiv(score - 10, 2)

    // ── Initiative, CA, déplacement ──

    /** Vigilant — Maîtrise de l'Initiative : + bonus de maîtrise. */
    fun bonusInitiative(c: Character): Int = if (a(c, "Vigilant")) c.proficiencyBonus else 0

    /** Bonus total d'initiative du personnage : Dextérité + bonus saisi sur la fiche + Vigilant. */
    fun initiative(c: Character): Int = mod(c.dexterity) + c.initiativeBonus + bonusInitiative(c)

    /** Maître des armures intermédiaires — Porteur agile : Dextérité jusqu'à +3 (au lieu de +2) si Dex ≥ 16. */
    fun dexMaxArmureIntermediaire(c: Character): Int? =
        if (a(c, "Maître des armures intermédiaires") && c.dexterity >= 16) 3 else null

    /** Vision aveugle (m) : Combat en aveugle, Discret. */
    fun visionAveugle(c: Character): Int = if (style(c, "Combat en aveugle") || a(c, "Discret")) 3 else 0

    // ── Dégâts subis et jets contre la mort ──

    /**
     * Maître des armures lourdes — Réduction des dégâts : en armure lourde, dégâts contondants,
     * perforants et tranchants réduits du bonus de maîtrise.
     */
    fun reductionDegats(c: Character, typeDegats: String?): Int {
        if (!a(c, "Maître des armures lourdes") || !ArmorRules.porteArmureLourde(c)) return 0
        val t = TypesCreature.radical(typeDegats.orEmpty())
        return if (t in setOf("contondant", "perforant", "tranchant")) c.proficiencyBonus else 0
    }

    /** Gaillard — Trompe-la-mort : Avantage aux jets de sauvegarde contre la mort. */
    fun avantageJetMort(c: Character): Boolean = a(c, "Gaillard")

    /** Faveur de récupération — Baroud d'honneur disponible (une fois par repos long). */
    fun baroudDisponible(c: Character): Boolean = ressource(c, BAROUD)?.let { restantes(c, it) > 0 } == true

    /** Baroud d'honneur : 1 PV plus la moitié du maximum de points de vie. */
    fun pvBaroud(c: Character): Int = 1 + c.maxHitPoints / 2

    // ── Armes et frappes à mains nues ──

    /**
     * Dé des frappes à mains nues (sans le modificateur) : Combat à mains nues (1d6, 1d8 sans arme
     * ni bouclier), Bagarreur de tavernes (1d4) — le meilleur ; null sans ces dons.
     */
    fun deMainsNues(c: Character, mainsLibres: Boolean): String? = when {
        style(c, "Combat à mains nues") -> if (mainsLibres) "1d8" else "1d6"
        a(c, "Bagarreur de tavernes") -> "1d4"
        else -> null
    }

    /** Bonus aux dégâts et notes d'une arme selon les dons du personnage. */
    data class EffetArme(val bonusDegats: Int = 0, val notes: List<String> = emptyList())

    fun effetArme(c: Character, nom: String, proprietes: String, aDistance: Boolean, typeDegats: String?): EffetArme {
        var bonus = 0
        val notes = mutableListOf<String>()
        val lancer = proprietes.contains("Lancer", ignoreCase = true)
        if (lancer && style(c, "Armes de lancer")) {
            if (aDistance) {
                bonus += 2
                notes += "Style Armes de lancer : +2 aux dégâts"
            } else notes += "Style Armes de lancer : +2 aux dégâts si elle est lancée"
        }
        if (proprietes.contains("Lourde", ignoreCase = true) && a(c, "Cogneur lourd")) {
            bonus += c.proficiencyBonus
            notes += "Cogneur lourd : +${c.proficiencyBonus} aux dégâts (action Attaque, à votre tour)"
        }
        if (aDistance && a(c, "Tireur d'élite")) notes += "Tireur d'élite : ignore les abris partiel et supérieur, pas de Désavantage au contact ni à longue portée"
        if (nom.contains("arbalète", ignoreCase = true) && a(c, "Maître-arbalétrier")) {
            notes += "Maître-arbalétrier : ignore Chargement, pas de Désavantage au contact"
        }
        val t = TypesCreature.radical(typeDegats.orEmpty())
        if (t == "perforant" && a(c, "Perforateur")) notes += "Perforateur : relancez un dé de dégâts (1/tour) ; critique : +1 dé"
        if (t == "tranchant" && a(c, "Trancheur")) notes += "Trancheur : Vitesse de la cible −3 m (1/tour) ; critique : Désavantage à ses attaques"
        if (t == "contondant" && a(c, "Broyeur")) notes += "Broyeur : déplace la cible de 1,50 m (1/tour) ; critique : Avantage contre elle"
        val hast = proprietes.contains("Allonge", ignoreCase = true) && proprietes.contains("Lourde", ignoreCase = true) ||
            nom.contains("bâton de combat", ignoreCase = true) || nom.contains("lance", ignoreCase = true)
        if (hast && a(c, "Maître d'hast")) notes += "Maître d'hast : action Bonus, 1d4 contondants avec l'autre extrémité ; attaque d'Opportunité quand on entre dans votre allonge"
        if (proprietes.contains("Finesse", ignoreCase = true) && a(c, "Duelliste défensif")) {
            notes += "Duelliste défensif : Réaction, +${c.proficiencyBonus} à la CA contre une attaque de corps à corps"
        }
        return EffetArme(bonus, notes)
    }

    /** Maître d'armes : une botte d'arme en plus (un type d'arme au choix). */
    fun bottesEnPlus(c: Character): Int = if (a(c, "Maître d'armes")) 1 else 0

    // ── Rappels en combat ──

    /** Bénéfices des dons utiles en combat, rappelés avec les capacités d'attaque. */
    fun capacites(c: Character): List<CapaciteAttaque> {
        val pb = c.proficiencyBonus
        val dd = { carac: Int -> 8 + pb + mod(carac) }
        return buildList {
            if (a(c, "Bagarreur de tavernes")) add(CapaciteAttaque("Bagarreur de tavernes", "Mains nues 1d4 + For, relance des 1 ; Bourrade : repoussez la cible de 1,50 m (1/tour) ; maîtrise des armes improvisées"))
            if (style(c, "Combat à mains nues")) add(CapaciteAttaque("Combat à mains nues", "Mains nues 1d6 + For (1d8 sans arme ni bouclier) ; début du tour : 1d4 contondants à une créature agrippée"))
            if (a(c, "Cogneur lourd")) add(CapaciteAttaque("Cogneur lourd", "Arme Lourde : +$pb dégâts ; après un critique ou une créature réduite à 0 PV, attaque en action Bonus"))
            if (a(c, "Combattant à deux armes")) add(CapaciteAttaque("Combattant à deux armes", "Après l'action Attaque avec une arme Légère, attaque en action Bonus avec une autre arme sans Deux mains"))
            if (a(c, "Expert de la charge")) add(CapaciteAttaque("Expert de la charge", "Pointe : +3 m ; après 3 m en ligne droite, +1d8 dégâts ou repousse de 3 m (1/tour)"))
            if (a(c, "Maître des boucliers")) add(CapaciteAttaque("Maître des boucliers", "Coup de bouclier : JS Force DD ${dd(c.strength)} ou repoussée de 1,50 m / À terre (1/tour) ; Réaction : aucun dégât sur un JS Dex réussi"))
            if (a(c, "Sentinelle")) add(CapaciteAttaque("Sentinelle", "Attaque d'Opportunité quand une créature à 1,50 m se désengage ou attaque un autre que vous ; touchée, sa Vitesse tombe à 0"))
            if (a(c, "Combattant monté")) add(CapaciteAttaque("Combattant monté", "Monté : Avantage contre les créatures non montées plus petites que la monture ; Pas de côté, Déviation"))
            if (a(c, "Empoisonneur")) add(CapaciteAttaque("Empoisonneur", "Poison : JS Con DD ${8 + pb + maxOf(mod(c.dexterity), mod(c.intelligence))} ou 2d8 poison et Empoisonné ; ignore la Résistance au poison"))
            if (a(c, "Tueur de mages")) add(CapaciteAttaque("Tueur de mages", "Vos dégâts imposent le Désavantage au JS de Concentration ; Esprit préservé : réussir un JS d'Int/Sag/Cha raté (1/repos)"))
            if (a(c, "Mage de guerre")) add(CapaciteAttaque("Mage de guerre", "Avantage aux JS de Concentration (appliqué) ; Réaction : sort contre une créature qui quitte votre allonge ; composantes somatiques mains pleines"))
            if (a(c, "Incantateur d'élite")) add(CapaciteAttaque("Incantateur d'élite", "Attaques de sort : ignorent les abris partiel et supérieur, pas de Désavantage au contact, portée +18 m"))
            if (a(c, "Télékinésiste")) add(CapaciteAttaque("Télékinésiste", "Action Bonus : JS Force DD ${8 + pb + maxOf(mod(c.intelligence), mod(c.wisdom), mod(c.charisma))} ou déplacée de 1,50 m"))
            if (style(c, "Interception")) add(CapaciteAttaque("Interception", "Réaction : réduit de 1d10 + $pb les dégâts d'un allié à 1,50 m (bouclier ou arme en main)"))
            if (style(c, "Protection")) add(CapaciteAttaque("Protection", "Réaction : Désavantage aux attaques contre un allié à 1,50 m (bouclier)"))
            if (style(c, "Armes de lancer")) add(CapaciteAttaque("Armes de lancer", "+2 aux dégâts d'une arme de lancer lancée (appliqué)"))
            visionAveugle(c).takeIf { it > 0 }?.let { add(CapaciteAttaque("Vision aveugle", "$it m (Combat en aveugle / Discret)")) }
            if (a(c, "Maître des armures lourdes")) add(CapaciteAttaque("Maître des armures lourdes", "En armure lourde : dégâts contondants, perforants, tranchants −$pb (appliqué)"))
            if (a(c, "Gaillard")) add(CapaciteAttaque("Gaillard", "Avantage aux jets contre la mort (appliqué) ; action Bonus : dépensez un dé de vie pour vous soigner"))
            if (a(c, "Faveur de vigueur")) add(CapaciteAttaque("Faveur de vigueur", "+40 PV max (appliqué) ; quand vous regagnez des PV, +${mod(c.constitution)} en plus (1/tour)"))
            if (a(c, "Faveur de vitesse")) add(CapaciteAttaque("Faveur de vitesse", "Vitesse +9 m (appliquée) ; action Bonus : Désengagement qui met fin à Agrippé"))
            if (a(c, "Mobile")) add(CapaciteAttaque("Mobile", "Vitesse +3 m (appliquée) ; Pointe sans surcoût en Terrain difficile ; Désavantage aux attaques d'Opportunité contre vous"))
            if (a(c, "Athlète")) add(CapaciteAttaque("Athlète", "Vitesse d'escalade = Vitesse ; se relever ne coûte que 1,50 m ; saut avec 1,50 m d'élan"))
            if (a(c, "Faveur de récupération")) add(CapaciteAttaque("Faveur de récupération", "Baroud d'honneur appliqué automatiquement à 0 PV (1/repos long)"))
        }
    }

    // ── Ressources à utilisations limitées ──

    fun ressources(c: Character): List<RessourceClasse> = buildList {
        val pb = c.proficiencyBonus
        if (a(c, "Chanceux")) add(RessourceClasse(CHANCE, "Points de chance", pb, Recuperation.REPOS_LONG,
            "Chanceux : 1 point pour l'Avantage à un Test d20, ou le Désavantage à une attaque contre vous."))
        if (a(c, "Faveur de récupération")) {
            add(RessourceClasse(BAROUD, "Baroud d'honneur", 1, Recuperation.REPOS_LONG,
                "Réduit à 0 PV sans mourir : tombez à 1 PV et regagnez la moitié de vos PV max (appliqué en combat)."))
            add(RessourceClasse(REGAIN_VITALITE, "Regain de vitalité", 10, Recuperation.REPOS_LONG,
                "Action Bonus : dépensez des d10 de la réserve et regagnez le total en PV.", de = "d10"))
        }
        if (a(c, "Tueur de mages")) add(RessourceClasse(ESPRIT_PRESERVE, "Esprit préservé", 1, Recuperation.REPOS_COURT,
            "Réussir un JS d'Intelligence, de Sagesse ou de Charisme raté."))
        if (a(c, "Magie rituelle")) add(RessourceClasse(RITUEL_RAPIDE, "Rituel rapide", 1, Recuperation.REPOS_LONG,
            "Lancez un sort rituel préparé avec son temps d'incantation normal, sans emplacement."))
        if (a(c, "Affinité féerique")) add(RessourceClasse(SORT_FEERIQUE, "Sorts féeriques gratuits", 2, Recuperation.REPOS_LONG,
            "Foulée brumeuse et le sort choisi : chacun une fois sans emplacement."))
        if (a(c, "Affinité ombreuse")) add(RessourceClasse(SORT_OMBREUX, "Sorts ombreux gratuits", 2, Recuperation.REPOS_LONG,
            "Invisibilité et le sort choisi : chacun une fois sans emplacement."))
        if (a(c, "Télépathe")) add(RessourceClasse(DETECTION_PENSEES, "Détection des pensées gratuite", 1, Recuperation.REPOS_LONG,
            "Détection des pensées sans emplacement ni composante."))
        if (a(c, "Initié à la magie")) add(RessourceClasse(INITIE_SORT, "Sort du 1er niveau (Initié à la magie)", 1, Recuperation.REPOS_LONG,
            "Le sort du 1er niveau du don, une fois sans emplacement."))
    }

    fun ressource(c: Character, cle: String): RessourceClasse? = ressources(c).firstOrNull { it.cle == cle }

    fun restantes(c: Character, r: RessourceClasse): Int = (r.max - (c.compteursUtilises[r.cle] ?: 0)).coerceIn(0, r.max)
}
