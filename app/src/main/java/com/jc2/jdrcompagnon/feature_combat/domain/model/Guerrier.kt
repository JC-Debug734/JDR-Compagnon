package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character

/** Comment une ressource de classe se récupère. */
enum class Recuperation(val libelle: String) {
    /** Toutes les utilisations au repos court comme au repos long. */
    REPOS_COURT("repos court ou long"),
    /** Une utilisation au repos court, toutes au repos long. */
    UNE_AU_REPOS_COURT("1 au repos court, toutes au repos long"),
    /** Seulement au repos long. */
    REPOS_LONG("repos long"),
}

/**
 * Ressource de classe à utilisations limitées, suivie dans Character.compteursUtilises sous
 * [cle] : [max] utilisations, récupérées selon [recuperation]. [de] : dé lancé à l'utilisation
 * (« d8 » pour un dé de Supériorité), null si l'aptitude ne lance rien.
 */
data class RessourceClasse(
    val cle: String,
    val nom: String,
    val max: Int,
    val recuperation: Recuperation,
    val detail: String,
    val de: String? = null,
)

/**
 * Règles du Guerrier (SRD 5.2.1) et de ses sous-classes (SRD, sous-classes 2024 et Chevalier
 * runique importés) appliquées par l'application :
 * - Second souffle (2 → 4 utilisations), Fougue (1 → 2), Inflexible (1 → 3), Sens tactique ;
 * - styles de combat choisis (Défense : +1 CA en armure, cf. ArmorRules) ;
 * - Champion : zone de critique (19-20, 18-20), Héros du champ d'honneur, Survivant (CombatSession) ;
 * - Maître de guerre : dés de Supériorité, manœuvres choisies, Observation de l'ennemi ;
 * - Soldat psi : dés d'Énergie psionique et pouvoirs à usage unique ;
 * - Chevalier runique : Puissance des géants, runes choisies (invocations), Bouclier runique ;
 * - Chevalier occulte : incantation (lanceur « tiers », voir IncantationDeSousClasse).
 */
object Guerrier {

    const val CLASSE = "Guerrier"

    // Clés de Character.compteursUtilises.
    const val SECOND_SOUFFLE = "second-souffle"
    const val FOUGUE = "fougue"
    const val INFLEXIBLE = "inflexible"
    const val SUPERIORITE = "des-superiorite"
    const val OBSERVATION = "observation-ennemi"
    const val ENERGIE_PSIONIQUE = "energie-psionique"
    const val DEPLACEMENT_TELEKINETIQUE = "deplacement-telekinetique"
    const val BOND_PSI = "bond-psi"
    const val REMPART_DE_FORCE = "rempart-de-force"
    const val MAITRE_TELEKINETIQUE = "maitre-telekinetique"
    const val PUISSANCE_GEANTS = "puissance-des-geants"
    const val BOUCLIER_RUNIQUE = "bouclier-runique"
    const val PREFIXE_RUNE = "rune:"

    /** Effet actif (fiche) de Puissance des géants : 1 minute. */
    const val EFFET_PUISSANCE_GEANTS = "Puissance des géants"

    /** Styles de combat (dons « Style de combat ») dont l'effet est calculé par l'application. */
    val STYLES_GERES = listOf(
        "Archerie", "Armes à deux mains", "Combat à deux armes", "Défense", "Duel",
        // Dons de Style de combat du Manuel des joueurs (voir Dons) : Armes de lancer et Combat à mains nues
        // changent les dégâts, les autres sont rappelés en combat.
        "Armes de lancer", "Combat à mains nues", "Combat en aveugle", "Interception", "Protection",
    )

    fun niveau(c: Character): Int =
        ArsenalPersonnage.niveauxParClasse(c).entries.firstOrNull { it.key.equals(CLASSE, ignoreCase = true) }?.value ?: 0

    /** Sous-classe de Guerrier (classe principale, ou « Sous-classe (Guerrier) : X » en multiclassage). */
    fun sousClasse(c: Character): String =
        if (c.characterClass.equals(CLASSE, ignoreCase = true)) c.subclass.trim()
        else Regex("""Sous-classe \(Guerrier\) : (.+)""").find(c.classFeatures)?.groupValues?.get(1)?.trim().orEmpty()

    private fun niveauSousClasse(c: Character, nom: String): Int =
        niveau(c).takeIf { it >= 3 && sousClasse(c).equals(nom, ignoreCase = true) } ?: 0

    fun niveauChampion(c: Character) = niveauSousClasse(c, "Champion")
    fun niveauMaitreDeGuerre(c: Character) = niveauSousClasse(c, "Maître de guerre")
    fun niveauSoldatPsi(c: Character) = niveauSousClasse(c, "Soldat psi")
    fun niveauChevalierRunique(c: Character) = niveauSousClasse(c, "Chevalier runique")
    fun niveauChevalierOcculte(c: Character) = niveauSousClasse(c, "Chevalier occulte")

    private fun mod(score: Int) = ArsenalPersonnage.mod(score)

    // ── Styles de combat ──

    /**
     * Le personnage a-t-il le style de combat [nom] : choisi par une aptitude (« Style de combat
     * (Niveau 1) — Défense : … »), ou noté seul sur une ligne de ses aptitudes ou de ses dons
     * (« Défense (…) », « Archerie ») ?
     */
    fun aStyle(c: Character, nom: String): Boolean {
        val n = Regex.escape(nom)
        val ligne = Regex("""(?m)^\s*$n\s*(\(|:|$)""", RegexOption.IGNORE_CASE)
        return Regex("""Style de combat[^\n—:]*[—:]\s*$n\s*(:|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(c.classFeatures) ||
            ligne.containsMatchIn(c.classFeatures) || ligne.containsMatchIn(c.feats)
    }

    // ── Champion ──

    /** Plus petit d20 naturel qui donne un Coup critique : 20, 19 (Champion), 18 (Champion niv. 15). */
    fun seuilCritique(c: Character): Int = when {
        niveauChampion(c) >= 15 -> 18
        niveauChampion(c) >= 3 -> 19
        else -> 20
    }

    /** Héros du champ d'honneur (Champion niv. 10) : Inspiration héroïque au début du tour si absente. */
    fun herosDuChampDHonneur(c: Character): Boolean = niveauChampion(c) >= 10

    /** Survivant (Champion niv. 18) : PV regagnés au début du tour En péril (≤ moitié des PV, au moins 1). */
    fun pvResilienceHeroique(c: Character): Int = if (niveauChampion(c) >= 18) 5 + mod(c.constitution) else 0

    /** Trompe-la-mort (Champion niv. 18) : Avantage aux JS contre la mort, 18-19 comptent comme 20. */
    fun trompeLaMort(c: Character): Boolean = niveauChampion(c) >= 18

    // ── Choix notés sur la fiche (runes, manœuvres) ──

    /** Options retenues pour les aptitudes [aptitudes] (« Nom (Niveau N) — Option : … »). */
    private fun optionsChoisies(c: Character, vararg aptitudes: String): List<String> =
        aptitudes.flatMap { apt ->
            Regex("""${Regex.escape(apt)}[^\n—]*—\s*([^:\n]+?)\s*:""").findAll(c.classFeatures).map { it.groupValues[1].trim() }.toList()
        }.distinct()

    /** Runes connues du Chevalier runique (Sculpteur de runes, Rune supplémentaire). */
    fun runesConnues(c: Character): List<String> =
        if (niveauChevalierRunique(c) == 0) emptyList() else optionsChoisies(c, "Sculpteur de runes", "Rune supplémentaire")

    /** Manœuvres connues du Maître de guerre. */
    fun manoeuvresConnues(c: Character): List<String> =
        if (niveauMaitreDeGuerre(c) == 0) emptyList() else optionsChoisies(c, "Manœuvre")

    /** DD des manœuvres : 8 + maîtrise + Force ou Dextérité (la meilleure). */
    fun ddManoeuvres(c: Character): Int = 8 + c.proficiencyBonus + maxOf(mod(c.strength), mod(c.dexterity))

    /** DD des runes : 8 + maîtrise + Constitution. */
    fun ddRunes(c: Character): Int = 8 + c.proficiencyBonus + mod(c.constitution)

    /** DD du Soldat psi (Poussée télékinétique) : 8 + maîtrise + Intelligence. */
    fun ddPsi(c: Character): Int = 8 + c.proficiencyBonus + mod(c.intelligence)

    /** Dé de Puissance des géants : d6, d8 (Grande carrure, niv. 10), d10 (Mastodonte runique, niv. 18). */
    fun dePuissanceGeants(c: Character): String = when {
        niveauChevalierRunique(c) >= 18 -> "d10"
        niveauChevalierRunique(c) >= 10 -> "d8"
        else -> "d6"
    }

    // ── Ressources ──

    /** Ressources à utilisations limitées du personnage pour le Guerrier et sa sous-classe. */
    fun ressources(c: Character): List<RessourceClasse> {
        val n = niveau(c)
        if (n == 0) return emptyList()
        val pb = c.proficiencyBonus
        return buildList {
            add(
                RessourceClasse(
                    SECOND_SOUFFLE, "Second souffle", if (n >= 10) 4 else if (n >= 4) 3 else 2, Recuperation.UNE_AU_REPOS_COURT,
                    "Action Bonus : regagnez 1d10 + $n PV." +
                        (if (n >= 2) " Sens tactique : dépensez-en une pour ajouter 1d10 à un test de caractéristique raté." else "") +
                        if (n >= 5) " Décalage tactique : déplacement de la moitié de votre Vitesse sans attaque d'Opportunité." else "",
                    de = "d10",
                )
            )
            if (n >= 2) add(
                RessourceClasse(
                    FOUGUE, "Fougue", if (n >= 17) 2 else 1, Recuperation.REPOS_COURT,
                    "Une action supplémentaire à votre tour (hors action Magie)." +
                        if (niveauChevalierOcculte(c) >= 15) " Charge arcanique : téléportation à 9 m avant ou après." else "",
                )
            )
            if (n >= 9) add(
                RessourceClasse(INFLEXIBLE, "Inflexible", if (n >= 17) 3 else if (n >= 13) 2 else 1, Recuperation.REPOS_LONG,
                    "Relancez un JS raté avec un bonus de +$n ; le nouveau résultat s'applique.", de = "d20")
            )
            niveauMaitreDeGuerre(c).takeIf { it > 0 }?.let { m ->
                val de = if (m >= 18) "d12" else if (m >= 10) "d10" else "d8"
                add(
                    RessourceClasse(SUPERIORITE, "Dés de Supériorité", if (m >= 15) 6 else if (m >= 7) 5 else 4, Recuperation.REPOS_COURT,
                        "Un dé par manœuvre (une par attaque). DD des manœuvres : ${ddManoeuvres(c)}." +
                            if (m >= 15) " Implacable : une fois par tour, lancez 1d8 au lieu de dépenser un dé." else "",
                        de = de)
                )
                if (m >= 7) add(
                    RessourceClasse(OBSERVATION, "Observation de l'ennemi", 1, Recuperation.REPOS_LONG,
                        "Action Bonus : Immunités, Résistances et Vulnérabilités d'une créature visible à 9 m (ou un dé de Supériorité).")
                )
            }
            niveauSoldatPsi(c).takeIf { it > 0 }?.let { p ->
                val (nombre, de) = RessourcesClasse.desEnergiePsionique(p)
                val bonusInt = mod(c.intelligence)
                add(
                    RessourceClasse(ENERGIE_PSIONIQUE, "Énergie psionique", nombre, Recuperation.UNE_AU_REPOS_COURT,
                        "Frappe psionique : dé + $bonusInt dégâts de force (une fois par tour, cible à 9 m). Champ protecteur : réduit des dégâts du dé + $bonusInt." +
                            if (p >= 7) " Poussée télékinétique : JS de Force DD ${ddPsi(c)} ou À terre / déplacée de 3 m." else "",
                        de = de)
                )
                add(RessourceClasse(DEPLACEMENT_TELEKINETIQUE, "Déplacement télékinétique", 1, Recuperation.REPOS_COURT,
                    "Action Magie : téléportez un objet de taille G ou moins, ou une créature consentante, à 9 m (ou un dé d'Énergie)."))
                if (p >= 7) add(RessourceClasse(BOND_PSI, "Bond psi", 1, Recuperation.REPOS_COURT,
                    "Action Bonus : Vitesse de vol égale au double de votre Vitesse jusqu'à la fin du tour (ou un dé d'Énergie)."))
                if (p >= 15) add(RessourceClasse(REMPART_DE_FORCE, "Rempart de force", 1, Recuperation.REPOS_LONG,
                    "Action Bonus : jusqu'à ${mod(c.intelligence).coerceAtLeast(1)} créature(s) à 9 m ont un Abri partiel 1 minute (ou un dé d'Énergie)."))
                if (p >= 18) add(RessourceClasse(MAITRE_TELEKINETIQUE, "Maître télékinétique", 1, Recuperation.REPOS_LONG,
                    "Télékinésie sans emplacement (Intelligence) ; attaque d'arme en action Bonus à chaque tour de Concentration (ou un dé d'Énergie)."))
            }
            niveauChevalierRunique(c).takeIf { it > 0 }?.let { r ->
                add(
                    RessourceClasse(PUISSANCE_GEANTS, "Puissance des géants", pb, Recuperation.REPOS_LONG,
                        "Action Bonus, 1 minute : taille " + (if (r >= 18) "G ou TG (allonge +1,50 m)" else "G") +
                            ", Avantage aux tests et JS de Force, +1${dePuissanceGeants(c)} dégâts une fois par tour.",
                        de = dePuissanceGeants(c))
                )
                if (r >= 7) add(RessourceClasse(BOUCLIER_RUNIQUE, "Bouclier runique", pb, Recuperation.REPOS_LONG,
                    "Réaction : une créature visible à 18 m (autre que vous) touchée par une attaque — l'assaillant relance son d20."))
                val parRune = if (r >= 15) 2 else 1
                runesConnues(c).forEach { rune ->
                    add(RessourceClasse(PREFIXE_RUNE + rune, rune, parRune, Recuperation.REPOS_COURT, "Invocation de la rune. DD ${ddRunes(c)}."))
                }
            }
        }
    }

    fun restantes(c: Character, r: RessourceClasse): Int = (r.max - (c.compteursUtilises[r.cle] ?: 0)).coerceIn(0, r.max)

    fun ressource(c: Character, cle: String): RessourceClasse? = ressources(c).firstOrNull { it.cle == cle }

    /** Utilisations restantes de la ressource [cle] (0 si le personnage ne l'a pas). */
    fun restantes(c: Character, cle: String): Int = ressource(c, cle)?.let { restantes(c, it) } ?: 0

    /** Compteurs après un repos court (toutes classes, voir RessourcesClasse). */
    fun apresReposCourt(c: Character): Map<String, Int> = RessourcesClasse.apresReposCourt(c)
}
