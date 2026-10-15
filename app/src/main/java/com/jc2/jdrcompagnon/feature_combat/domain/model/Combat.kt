package com.jc2.jdrcompagnon.feature_combat.domain.model

/** Conditions (états) courantes de D&D 2024, attribuables à un combattant pendant le combat. */
enum class ConditionCombat(val label: String) {
    A_TERRE("À terre"),
    AGRIPPE("Agrippé"),
    AVEUGLE("Aveuglé"),
    CHARME("Charmé"),
    EFFRAYE("Effrayé"),
    EMPOISONNE("Empoisonné"),
    ENTRAVE("Entravé"),
    ETOURDI("Étourdi"),
    INCAPABLE_D_AGIR("Neutralisé"),
    INCONSCIENT("Inconscient"),
    INVISIBLE("Invisible"),
    PARALYSE("Paralysé"),
    PETRIFIE("Pétrifié"),
    SOURD("Assourdi"),
    CONCENTRATION("Concentration")
}

/**
 * Distance entre deux combattants, en paliers de mètres (grille de 1,50 m des règles). Fixée par
 * le MJ au début du combat puis mise à jour par les déplacements déclarés.
 *
 * Règles (SRD 5.2) : une attaque au corps à corps porte à 1,50 m (« allonge 1,50 m ») — c'est le
 * contact ; certaines armes ou créatures ont une allonge de 3 m. Un déplacement standard est de
 * 9 m (18 m en se précipitant). [COURTE] (9 m) reste la distance par défaut d'un adversaire non
 * placé, [LONGUE] (30 m) la portée longue type d'une arme de jet.
 */
enum class Distance(val metres: Double) {
    CONTACT(1.5),
    M3(3.0),
    M4_5(4.5),
    M6(6.0),
    COURTE(9.0),
    M12(12.0),
    M18(18.0),
    M24(24.0),
    LONGUE(30.0),
    M45(45.0),
    M60(60.0),
    M90(90.0),
    M120(120.0);

    /** « 4,5 m » */
    val texteMetres: String get() = formatMetres(metres)

    /** Libellé court (puces) : « Contact (1,5 m) », « 9 m », « 120 m + ». */
    val court: String get() = when (this) {
        CONTACT -> "Contact (1,5 m)"
        M120 -> "120 m +"
        else -> texteMetres
    }

    /** Libellé dans une phrase : « au contact », « à 9 m ». */
    val label: String get() = if (this == CONTACT) "Au contact" else "À $texteMetres"

    /** Déplacement nécessaire pour arriver au contact. */
    val metresJusquAuContact: Double get() = (metres - CONTACT.metres).coerceAtLeast(0.0)

    /** S'éloigner de [m] mètres (fuite, recul). */
    fun eloigne(m: Double): Distance = depuisMetres(metres + m)

    /** Se rapprocher de [m] mètres au plus (sans dépasser le contact). */
    fun rapproche(m: Double): Distance = depuisMetres((metres - m).coerceAtLeast(CONTACT.metres))

    /**
     * Distance atteinte en visant [visee] avec au plus [metresMax] de déplacement : le palier visé
     * s'il est à portée, sinon aussi près (ou loin) que le déplacement le permet.
     */
    fun versAvecDeplacement(visee: Distance, metresMax: Double): Distance = when {
        visee.metres < metres -> if (metres - visee.metres <= metresMax + 0.01) visee else rapproche(metresMax).let { if (it.metres < visee.metres) visee else it }
        visee.metres > metres -> if (visee.metres - metres <= metresMax + 0.01) visee else entries.lastOrNull { it.metres <= metres + metresMax + 0.01 } ?: this
        else -> this
    }

    companion object {
        /** Déplacement standard d'une créature de taille M (9 m par tour). */
        const val VITESSE_STANDARD = 9.0

        /** Palier correspondant à [m] mètres : le premier palier qui ne la sous-estime pas. */
        fun depuisMetres(m: Double): Distance = entries.firstOrNull { it.metres >= m - 0.01 } ?: M120

        /** Distance par défaut entre deux membres d'un même camp (groupés), faute de placement. */
        val ALLIES_PAR_DEFAUT = M3
    }
}

/** Forme d'une zone d'effet ; [depuisLanceur] : la zone part du lanceur (cône, ligne, émanation). */
enum class FormeZone(val label: String, val depuisLanceur: Boolean) {
    SPHERE("Sphère", false),
    CUBE("Cube", false),
    CYLINDRE("Cylindre", false),
    CONE("Cône", true),
    LIGNE("Ligne", true),
    EMANATION("Émanation", true),
}

/**
 * Zone d'effet d'un sort ou d'une capacité (« sphère de 6 m de rayon », « cône de 9 m ») : sert à
 * proposer d'office les créatures touchées d'après les distances du combat.
 */
data class ZoneEffet(val forme: FormeZone, val metres: Double) {
    /**
     * Distance maximale depuis le centre (cible visée, ou lanceur pour un cône/une ligne/une
     * émanation) pour être pris dans la zone. Cube : la moitié du côté autour du point visé.
     */
    val portee: Double get() = if (forme == FormeZone.CUBE) (metres / 2).coerceAtLeast(Distance.CONTACT.metres) else metres

    val libelle: String get() = "${forme.label} de ${formatMetres(metres)}"

    fun versTexte(): String = "${forme.name}:$metres"

    companion object {
        private val formeRegex = Regex(
            // IGNORE_CASE ne couvre que l'ASCII : « Émanation » avec majuscule écrit explicitement.
            """(sphère|cube|cylindre|cône|ligne|[éÉ]manation)[^.;]{0,40}?(\d+(?:[,.]\d+)?)\s*(?:m\b|mètres?)""",
            RegexOption.IGNORE_CASE
        )
        private val rayonRegex = Regex("""rayon de (\d+(?:[,.]\d+)?)\s*(?:m\b|mètres?)""", RegexOption.IGNORE_CASE)

        /** Lit la première zone décrite dans [texte] (description de sort, action de monstre). */
        fun depuisTexte(texte: String?): ZoneEffet? {
            val t = texte ?: return null
            formeRegex.find(t)?.let { m ->
                val forme = when (m.groupValues[1].lowercase()) {
                    "sphère" -> FormeZone.SPHERE
                    "cube" -> FormeZone.CUBE
                    "cylindre" -> FormeZone.CYLINDRE
                    "cône" -> FormeZone.CONE
                    "ligne" -> FormeZone.LIGNE
                    else -> FormeZone.EMANATION
                }
                val metres = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return@let
                return ZoneEffet(forme, metres)
            }
            return rayonRegex.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()?.let { ZoneEffet(FormeZone.SPHERE, it) }
        }

        fun depuisTexteCode(code: String?): ZoneEffet? {
            val (forme, metres) = code?.split(':')?.takeIf { it.size == 2 } ?: return null
            return ZoneEffet(runCatching { FormeZone.valueOf(forme) }.getOrNull() ?: return null, metres.toDoubleOrNull() ?: return null)
        }
    }
}

/**
 * Un participant au combat : monstre (instancié depuis le bestiaire, [monstreNom] renseigné)
 * ou personnage (PJ/PNJ de GameState, [characterId] renseigné).
 */
data class Combattant(
    val id: String,
    val nom: String,
    val estMonstre: Boolean,
    val monstreNom: String? = null,
    val characterId: String? = null,
    val ca: Int,
    val pvMax: Int,
    val pv: Int,
    val pvTemporaires: Int = 0,
    val bonusInitiative: Int,
    val initiative: Int? = null,
    val conditions: Set<ConditionCombat> = emptySet(),
    // Actions lues sur la fiche (monstre) ou tirées des armes (PNJ), et comportement de l'IA de
    // déclaration (IaMonstre). Un monstre est toujours piloté par l'IA (Brute par défaut), un
    // personnage seulement si le MJ lui donne un profil. Jamais transmis aux joueurs.
    val attaques: List<AttaqueMonstre> = emptyList(),
    val nbAttaquesMultiples: Int = 1,
    val profilIA: ProfilIA? = null,
    // Pourquoi ce profil a été proposé (d'après la fiche), affiché au MJ.
    val raisonProfil: String? = null,
    // Changements de profil selon la situation, évalués dans l'ordre (cf. RegleProfilIA).
    val reglesIA: List<RegleProfilIA> = emptyList(),
    // Capacités à usage limité déjà utilisées (nom → nombre), cf. IaMonstre.disponible.
    val capacitesUtilisees: Map<String, Int> = emptyMap(),
    // Vitesse de déplacement en mètres par tour (fiche ; 9 m par défaut).
    val vitesse: Double = Distance.VITESSE_STANDARD,
    // Actions de la fiche (par nom) que le MJ a retirées : l'IA ne les choisit plus et elles ne
    // sont plus proposées à la déclaration.
    val actionsDesactivees: Set<String> = emptySet(),
    // Comportement créé par le MJ (outil COMPORTEMENT) : prioritaire sur [profilIA] s'il est renseigné.
    val comportementId: String? = null,
    // Personnage incarné par un joueur connecté : il déclare lui-même, l'IA ne décide jamais pour lui
    // (même pendant une courte déconnexion, pour qu'il retrouve sa place en se reconnectant).
    val joueParJoueur: Boolean = false,
    // Bonus aux jets de sauvegarde par caractéristique (« Dextérité » → +3), voir Sauvegardes.
    val sauvegardes: Map<String, Int> = emptyMap(),
    // Personnage à 0 PV : jets de sauvegarde contre la mort lancés à chacun de ses tours.
    val succesMort: Int = 0,
    val echecsMort: Int = 0,
    val stable: Boolean = false,
    val mort: Boolean = false,
    // Type de créature (« Mort-vivant », « Humanoïde »…, voir TypesCreature) et défenses aux types de dégâts.
    val typeCreature: String? = null,
    val defenses: DefensesDegats = DefensesDegats(),
    // Boss (BossCatalogue) : son état de boss vit dans CombatEnCours.boss.
    val bossId: String? = null,
    // Élément du décor à détruire (point faible d'un boss) : ne déclare ni ne joue de tour, et
    // n'empêche pas la victoire.
    val inerte: Boolean = false,
    // Zone du champ de bataille (voir Terrain) ; null = zone de départ de son camp.
    val zone: Int? = null,
) {
    /** Actions de la fiche que le MJ autorise pour ce combattant. */
    val attaquesAutorisees: List<AttaqueMonstre> get() = attaques.filter { it.nom !in actionsDesactivees }

    val horsCombat: Boolean get() = pv <= 0

    /**
     * Déclare seul ses actions : tout monstre, ou un personnage doté d'un comportement (grille par
     * classe, ou choix du MJ) que ne joue aucun joueur.
     */
    val piloteParIa: Boolean get() = !inerte && (estMonstre || (!joueParJoueur && (profilIA != null || comportementId != null)))

    /** Comportement appliqué par l'IA : créé par le MJ, sinon celui du profil (Brute par défaut). */
    val comportement: Comportement
        get() = Comportements.parId(comportementId) ?: Comportements.pour(profilIA ?: ProfilIA.BRUTE)
}

/** Un groupe de monstres identiques d'un lien #combat:[Gobelin x3, Loup x2]. */
@kotlinx.serialization.Serializable
data class LigneCompositionCombat(val monstreNom: String, val quantite: Int)

/**
 * Format texte d'un combat de scénario : `Gobelin x3, Loup x2` (quantité facultative, 1 par
 * défaut ; `×` accepté). C'est le contenu entre crochets du lien `#combat:[...]`.
 */
object CompositionCombat {

    private val ligneRegex = Regex("""^(.*?)\s*[x×]\s*(\d+)$""", RegexOption.IGNORE_CASE)

    fun parser(texte: String): List<LigneCompositionCombat> =
        texte.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { morceau ->
                val match = ligneRegex.find(morceau)
                if (match != null && match.groupValues[1].isNotBlank()) {
                    LigneCompositionCombat(match.groupValues[1].trim(), match.groupValues[2].toInt().coerceIn(1, 50))
                } else {
                    LigneCompositionCombat(morceau, 1)
                }
            }

    fun formater(lignes: List<LigneCompositionCombat>): String =
        lignes.filter { it.quantite > 0 }.joinToString(", ") { ligne ->
            if (ligne.quantite == 1) ligne.monstreNom else "${ligne.monstreNom} x${ligne.quantite}"
        }
}

/** Profil de combat extrait d'une fiche du bestiaire (CA, PV, initiative, actions). */
data class ProfilCombatMonstre(
    val ca: Int,
    val pvMoyens: Int,
    val formulePv: String?,
    val bonusInitiative: Int,
    val actions: ActionsMonstre = ActionsMonstre(emptyList()),
    // Comportement proposé à l'IA d'après toute la fiche (cf. ProfilIA.analyser).
    val comportement: ProfilDeduit = ProfilIA.analyser(actions, ""),
    // Vitesse au sol en mètres (« **Vitesse :** 9 m, vol 18 m » → 9).
    val vitesse: Double = Distance.VITESSE_STANDARD,
    // Bonus aux jets de sauvegarde (ligne « Caractéristiques » de la fiche).
    val sauvegardes: Map<String, Int> = emptyMap(),
    // Type de créature et défenses aux types de dégâts (voir TypesCreature).
    val typeCreature: String? = null,
    val defenses: DefensesDegats = DefensesDegats(),
) {
    companion object {
        private val caRegex = Regex("""\*\*CA :\*\*\s*(\d+)""")
        private val initRegex = Regex("""Initiative\s*([+\-−–]?)\s*(\d+)""")
        private val pvRegex = Regex("""\*\*Pv :\*\*\s*(\d+)(?:\s*\(([^)]+)\))?""")
        private val vitesseRegex = Regex("""\*\*Vitesse :\*\*\s*(\d+(?:[,.]\d+)?)""")

        /**
         * Lit le résumé en gras produit par MonsterParser (`**CA :** 15 Initiative +2 (12)`,
         * `**Pv :** 7 (2d6)`). Valeurs par défaut prudentes si une ligne est absente (monstre
         * personnalisé d'un autre monde au format libre).
         */
        fun depuisFiche(rawMarkdown: String): ProfilCombatMonstre {
            val ca = caRegex.find(rawMarkdown)?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val init = initRegex.find(rawMarkdown)?.let { m ->
                val valeur = m.groupValues[2].toIntOrNull() ?: 0
                if (m.groupValues[1].isNotEmpty() && m.groupValues[1] != "+") -valeur else valeur
            } ?: 0
            val pvMatch = pvRegex.find(rawMarkdown)
            val pv = pvMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val formule = pvMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() }
            val actions = ActionsMonstreParser.parser(rawMarkdown)
            val vitesse = vitesseRegex.find(rawMarkdown)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
                ?: Distance.VITESSE_STANDARD
            return ProfilCombatMonstre(
                ca, pv.coerceAtLeast(1), formule, init, actions, ProfilIA.analyser(actions, rawMarkdown), vitesse,
                Sauvegardes.depuisFiche(rawMarkdown), TypesCreature.depuisFiche(rawMarkdown), TypesCreature.defensesDepuisFiche(rawMarkdown),
            )
        }
    }

    /** Combattant (monstre piloté par l'IA) tiré de ce profil, nommé [nom], avec [pv] PV. */
    fun versCombattant(nom: String, monstreNom: String, pv: Int = pvMoyens): Combattant = Combattant(
        id = java.util.UUID.randomUUID().toString(),
        nom = nom,
        estMonstre = true,
        monstreNom = monstreNom,
        ca = ca,
        pvMax = pv,
        pv = pv,
        bonusInitiative = bonusInitiative,
        attaques = actions.attaques,
        nbAttaquesMultiples = actions.nbAttaquesMultiples,
        profilIA = comportement.profil,
        raisonProfil = comportement.raison,
        vitesse = vitesse,
        sauvegardes = sauvegardes,
        typeCreature = typeCreature,
        defenses = defenses,
    )
}

/**
 * Lance une formule de dés du type `2d6 + 3` / `19d12 + 133` / `4d8 − 4`. Retourne null si la
 * formule n'est pas reconnue. [critique] double le nombre de dés (coup critique).
 */
fun lancerFormuleDes(formule: String, aleatoire: kotlin.random.Random = kotlin.random.Random, critique: Boolean = false): Int? {
    val (nb, faces, bonus) = decomposerFormule(formule) ?: return null
    val des = if (critique) nb * 2 else nb
    return ((1..des).sumOf { aleatoire.nextInt(1, faces + 1) } + bonus).coerceAtLeast(1)
}

/**
 * Comme [lancerFormuleDes] (mêmes tirages), avec le détail des dés pour le journal :
 * `1d8 + 3 : 6 +3 = 9`, ou `1d8 + 3 (dés doublés) : 6+2 +3 = 11` sur un critique.
 */
fun lancerFormuleDesDetail(formule: String, aleatoire: kotlin.random.Random = kotlin.random.Random, critique: Boolean = false): Pair<Int, String>? {
    val (nb, faces, bonus) = decomposerFormule(formule) ?: return null
    val des = (1..(if (critique) nb * 2 else nb)).map { aleatoire.nextInt(1, faces + 1) }
    val total = (des.sum() + bonus).coerceAtLeast(1)
    val bonusTexte = when {
        bonus > 0 -> " +$bonus"
        bonus < 0 -> " $bonus"
        else -> ""
    }
    return total to "$formule${if (critique) " (dés doublés)" else ""} : ${des.joinToString("+")}$bonusTexte = $total"
}

/** (nombre de dés, faces, bonus) d'une formule `2d6 + 3`, ou null si elle n'est pas reconnue. */
internal fun decomposerFormule(formule: String): Triple<Int, Int, Int>? {
    val normalisee = formule.replace('−', '-').replace('–', '-').replace(" ", "")
    val match = Regex("""^(\d+)d(\d+)(?:([+-])(\d+))?$""", RegexOption.IGNORE_CASE).find(normalisee) ?: return null
    val nb = match.groupValues[1].toInt()
    val faces = match.groupValues[2].toInt()
    if (nb <= 0 || faces <= 0) return null
    val bonus = match.groupValues[4].toIntOrNull()?.let { if (match.groupValues[3] == "-") -it else it } ?: 0
    return Triple(nb, faces, bonus)
}

/** Valeurs minimale et maximale d'une formule de dés (ex. `1d6 + 1` → 2..7), ou null. */
fun bornesFormuleDes(formule: String): IntRange? {
    val (nb, faces, bonus) = decomposerFormule(formule) ?: return null
    return (nb + bonus).coerceAtLeast(1)..(nb * faces + bonus).coerceAtLeast(1)
}

/**
 * PV d'un monstre selon la difficulté du combat : l'écart min–max de sa formule est découpé en
 * [nbTranches] - 1 intervalles égaux ; la tranche 0 (la plus facile) prend le minimum, la
 * dernière (la plus difficile) le maximum. Ex. `1d6 + 1` en 4 tranches → 2, 4, 5, 7.
 */
fun pvSelonTranche(formule: String, tranche: Int, nbTranches: Int): Int? {
    val bornes = bornesFormuleDes(formule) ?: return null
    if (nbTranches <= 1) return bornes.last
    val t = tranche.coerceIn(0, nbTranches - 1)
    return bornes.first + Math.round((bornes.last - bornes.first) * t / (nbTranches - 1).toDouble()).toInt()
}

/**
 * Vitesse d'une fiche en mètres : les fiches stockent la vitesse en pieds (30 = 9 m) ; une petite
 * valeur (≤ 20) est déjà en mètres.
 */
fun vitesseEnMetres(speed: Int): Double = when {
    speed <= 0 -> Distance.VITESSE_STANDARD
    speed <= 20 -> speed.toDouble()
    else -> speed * 3 / 10.0
}

/** « 4,5 m », « 9 m ». */
fun formatMetres(m: Double): String =
    (if (m % 1.0 == 0.0) m.toInt().toString() else m.toString().replace('.', ',')) + " m"
