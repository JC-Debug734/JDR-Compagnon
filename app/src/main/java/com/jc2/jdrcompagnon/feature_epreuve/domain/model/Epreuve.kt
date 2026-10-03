package com.jc2.jdrcompagnon.feature_epreuve.domain.model

import java.util.UUID
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Épreuve environnementale de l'outil ÉPREUVES (distincte des épreuves Progrès/Menace rattachées
 * aux environnements, voir feature_environnement) : le groupe doit cumuler [reussitesRequises]
 * réussites. Chaque joueur annonce au MJ ce qu'il fait ; une réussite fait avancer le compteur,
 * un échec déclenche une [ComplicationEpreuve] tirée au hasard dans la liste de l'épreuve.
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
)

/** Conséquence d'un échec : toujours défavorable au groupe (dégâts, perte de temps, matériel...). */
@Serializable
data class ComplicationEpreuve(
    val titre: String,
    val description: String = "",
)

object ReglesEpreuve {
    const val REUSSITES_MIN = 1
    const val REUSSITES_MAX = 20

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
}
