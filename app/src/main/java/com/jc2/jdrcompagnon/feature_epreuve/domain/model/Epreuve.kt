package com.jc2.jdrcompagnon.feature_epreuve.domain.model

import java.util.UUID
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Épreuve environnementale de l'outil ÉPREUVES (seul système d'épreuves de l'app, aussi lancé
 * par les liens de scénario #epreuve:) : le groupe doit cumuler [reussitesRequises]
 * réussites avant d'accumuler [echecsMax] échecs. Chaque joueur annonce au MJ ce qu'il fait
 * (esquive, discussion, manœuvre... jamais un combat) ; une réussite fait avancer le compteur,
 * un échec déclenche une [ComplicationEpreuve] tirée au hasard dans la liste de l'épreuve.
 * L'épreuve se termine sur [issueReussite] ou, trop d'échecs, sur [issueEchec] : seule cette
 * issue peut mener à un combat ([combatEchec], même format que les liens `#combat:[...]`).
 *
 * [imageFileName] : image copiée dans le stockage interne (EpreuveImageStore), affichée sur la
 * page table (MjWebServer) pendant l'épreuve.
 */
data class Epreuve(
    val id: String = UUID.randomUUID().toString(),
    val worldId: String = "",
    val nom: String,
    val description: String = "",
    val reussitesRequises: Int = 3,
    val imageFileName: String? = null,
    val complications: List<ComplicationEpreuve> = emptyList(),
    // Piste d'ambiance (LoopTrack.id, voir availableLoopTracks) jouée au lancement ; null = aucune.
    val musicTrackId: String? = null,
    // Null : [reussitesRequises] fixe. Sinon le nombre de réussites est calculé au lancement
    // d'après le nombre de joueurs (voir ReglesEpreuve.adapter).
    val difficulte: DifficulteEpreuve? = null,
    // Échecs qui font perdre l'épreuve quand [difficulte] est null (sinon calculés, voir ReglesEpreuve.adapter).
    val echecsMax: Int = ReglesEpreuve.ECHECS_DEFAUT,
    // Ce qui arrive quand le groupe réussit l'épreuve, puis quand il la perd (trop d'échecs).
    val issueReussite: String = "",
    val issueEchec: String = "",
    // Combat déclenché par la perte de l'épreuve (`Kraken des Brumes x1, Sahuagin x4`) ; vide = aucun.
    val combatEchec: String = "",
)

/**
 * Difficulté d'une épreuve adaptative : [reussitesParJoueur] fixe le nombre de réussites
 * demandées (arrondi au supérieur) et [echecsParReussite] le nombre d'échecs qui font perdre
 * l'épreuve, en proportion des réussites (plus l'épreuve est dure, moins on tolère d'échecs),
 * [ddBase] le DD au niveau 1, [gravite] la colonne de dégâts de `{degats}` (0 contretemps, 1 dangereux, 2 mortel, table « gravité des dégâts » du DMG).
 */
enum class DifficulteEpreuve(
    val libelle: String,
    val reussitesParJoueur: Double,
    val echecsParReussite: Double,
    val ddBase: Int,
    val gravite: Int,
) {
    FACILE("Facile", 0.75, 1.0, 10, 0),
    MOYENNE("Moyenne", 1.0, 0.8, 12, 0),
    DIFFICILE("Difficile", 1.5, 0.65, 14, 1),
    MORTELLE("Mortelle", 2.0, 0.6, 16, 2),
}

/** Groupe qui affronte l'épreuve. [parDefaut] : aucun groupe ni joueur connecté, valeurs supposées. */
data class GroupeEpreuve(val joueurs: Int, val niveau: Int, val parDefaut: Boolean = false)

/** Conséquence d'un échec : toujours défavorable au groupe (dégâts, perte de temps, matériel...). */
@Serializable
data class ComplicationEpreuve(
    val titre: String,
    val description: String = "",
)

object ReglesEpreuve {
    const val REUSSITES_MIN = 1
    const val REUSSITES_MAX = 20
    const val ECHECS_MIN = 1
    const val ECHECS_MAX = 10
    const val ECHECS_DEFAUT = 3

    /**
     * Complication déclenchée par un échec, au hasard parmi celles de l'épreuve. On évite de
     * retomber sur [precedente] quand il y a le choix, pour que deux échecs d'affilée ne
     * racontent pas deux fois la même chose. Null si l'épreuve n'a aucune complication.
     */
    fun tirerComplication(
        complications: List<ComplicationEpreuve>,
        precedente: ComplicationEpreuve?,
        random: Random = Random.Default,
    ): ComplicationEpreuve? {
        val candidates = complications.filter { it.titre.isNotBlank() }
        if (candidates.isEmpty()) return null
        val sansRepetition = candidates.filter { it != precedente }.ifEmpty { candidates }
        return sansRepetition[random.nextInt(sansRepetition.size)]
    }

    const val JOUEURS_MAX = 10
    const val NIVEAU_MAX = 20

    // Dégâts par tranche de niveau (1-4, 5-10, 11-16, 17-20) et gravité (contretemps, dangereux, mortel).
    private val gravitesDegats = listOf(
        listOf("1d10", "2d10", "4d10"),
        listOf("2d10", "4d10", "10d10"),
        listOf("4d10", "10d10", "18d10"),
        listOf("10d10", "18d10", "24d10"),
    )

    private val Variable = Regex("""\{(DD|attaque|degats_leger|degats_lourd|degats|po)([+-]\d+)?\}""", RegexOption.IGNORE_CASE)

    /** Réussites demandées : [DifficulteEpreuve.reussitesParJoueur] par joueur, au moins 2. */
    fun reussitesPour(difficulte: DifficulteEpreuve, joueurs: Int): Int =
        kotlin.math.ceil(joueurs.coerceIn(1, JOUEURS_MAX) * difficulte.reussitesParJoueur).toInt()
            .coerceIn(2, REUSSITES_MAX)

    /**
     * Échecs qui font perdre l'épreuve : [DifficulteEpreuve.echecsParReussite] par réussite demandée,
     * au moins 2 (4 joueurs : 3 avant 3 en facile, 4 avant 4 en moyenne, 6 avant 4 en difficile, 8 avant 5 en mortelle).
     */
    fun echecsPour(difficulte: DifficulteEpreuve, joueurs: Int): Int =
        kotlin.math.ceil(reussitesPour(difficulte, joueurs) * difficulte.echecsParReussite - 1e-9).toInt()
            .coerceIn(2, ECHECS_MAX)

    /** Écart aléatoire du DD tiré à chaque lancement et à chaque réussite (voir EpreuveOutilSession). */
    val PLAGE_VARIATION_DD = -2..2

    /**
     * DD des jets de l'épreuve : celui de la difficulté au niveau 1, +1 tous les 4 niveaux, plus
     * [variation] (tirée dans [PLAGE_VARIATION_DD]) pour que les joueurs ne s'habituent pas à un DD fixe.
     */
    fun ddPour(difficulte: DifficulteEpreuve, niveau: Int, variation: Int = 0): Int =
        difficulte.ddBase + (niveau.coerceIn(1, NIVEAU_MAX) - 1) / 4 + variation

    fun degatsPour(gravite: Int, niveau: Int): String {
        val tranche = when (niveau.coerceIn(1, NIVEAU_MAX)) {
            in 1..4 -> 0
            in 5..10 -> 1
            in 11..16 -> 2
            else -> 3
        }
        return gravitesDegats[tranche][gravite.coerceIn(0, 2)]
    }

    /**
     * Épreuve prête à jouer pour [groupe] : réussites et échecs calculés si elle a une difficulté,
     * et variables remplacées dans sa description, ses complications et ses issues (épreuve sans difficulté :
     * calculées en difficulté moyenne) :
     * `{DD}` (et `{DD+2}`, `{DD-1}`...), `{attaque}` bonus au toucher, `{degats}` /
     * `{degats_leger}` / `{degats_lourd}` dés de dégâts, `{po}` somme d'or.
     * [variationDd] décale le DD (et le bonus d'attaque qui en découle).
     */
    fun adapter(epreuve: Epreuve, groupe: GroupeEpreuve, variationDd: Int = 0): Epreuve {
        val difficulte = epreuve.difficulte ?: DifficulteEpreuve.MOYENNE
        val dd = ddPour(difficulte, groupe.niveau, variationDd)
        fun remplacer(texte: String): String = texte.replace(Variable) { m ->
            val decalage = m.groupValues[2].toIntOrNull() ?: 0
            when (m.groupValues[1].lowercase()) {
                "dd" -> "${dd + decalage}"
                "attaque" -> "+${dd - 8 + decalage}"
                "degats" -> degatsPour(difficulte.gravite, groupe.niveau)
                "degats_leger" -> degatsPour(0, groupe.niveau)
                "degats_lourd" -> degatsPour(difficulte.gravite + 1, groupe.niveau)
                else -> "${(groupe.niveau.coerceIn(1, NIVEAU_MAX) * 25 * (difficulte.ordinal + 1)) + decalage}"
            }
        }
        return epreuve.copy(
            description = remplacer(epreuve.description),
            reussitesRequises = epreuve.difficulte?.let { reussitesPour(it, groupe.joueurs) }
                ?: epreuve.reussitesRequises.coerceAtLeast(REUSSITES_MIN),
            echecsMax = epreuve.difficulte?.let { echecsPour(it, groupe.joueurs) }
                ?: epreuve.echecsMax.coerceIn(ECHECS_MIN, ECHECS_MAX),
            complications = epreuve.complications.map { it.copy(titre = remplacer(it.titre), description = remplacer(it.description)) },
            issueReussite = remplacer(epreuve.issueReussite),
            issueEchec = remplacer(epreuve.issueEchec),
        )
    }
}
