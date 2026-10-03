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
 * Distance entre un monstre et un personnage, fixée par le MJ au début du combat puis mise à jour
 * par les déplacements déclarés. Au contact ≈ 1,50 m ; courte ≈ un déplacement (≤ 9 m) ;
 * longue = au-delà, hors de portée d'une charge en un tour.
 */
enum class Distance(val label: String, val court: String) {
    CONTACT("Au contact", "Contact"),
    COURTE("Courte distance", "Courte"),
    LONGUE("Longue distance", "Longue");

    /** Un cran plus loin (fuite, recul). */
    val plusLoin: Distance get() = if (this == CONTACT) COURTE else LONGUE
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
    val capacitesUtilisees: Map<String, Int> = emptyMap()
) {
    val horsCombat: Boolean get() = pv <= 0

    /** Déclare seul ses actions : tout monstre, ou un personnage auquel le MJ a donné un profil. */
    val piloteParIa: Boolean get() = estMonstre || profilIA != null
}

/** Un groupe de monstres identiques d'un lien #combat:[Gobelin x3, Loup x2]. */
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
    val comportement: ProfilDeduit = ProfilIA.analyser(actions, "")
) {
    companion object {
        private val caRegex = Regex("""\*\*CA :\*\*\s*(\d+)""")
        private val initRegex = Regex("""Initiative\s*([+\-−–]?)\s*(\d+)""")
        private val pvRegex = Regex("""\*\*Pv :\*\*\s*(\d+)(?:\s*\(([^)]+)\))?""")

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
            return ProfilCombatMonstre(ca, pv.coerceAtLeast(1), formule, init, actions, ProfilIA.analyser(actions, rawMarkdown))
        }
    }
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
