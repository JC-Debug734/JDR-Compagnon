package com.jc2.jdrcompagnon.feature_carte.data

import com.jc2.jdrcompagnon.feature_carte.data.local.CarteDao
import com.jc2.jdrcompagnon.feature_carte.data.mapper.versBibliotheque
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository

/**
 * Déplace les anciens événements de campagne/ville (table evenements_aleatoires) dans la
 * bibliothèque (feature_evenement), une fois pour toutes : chacun devient un événement propre à
 * sa campagne (même id), rattaché à sa ville le cas échéant, puis l'ancienne ligne est supprimée.
 * Ligne par ligne, dans cet ordre, pour qu'une interruption ne perde ni ne duplique rien : une
 * ligne non supprimée sera simplement réécrite (même id) au lancement suivant.
 */
class ConversionEvenementsCarte(
    private val dao: CarteDao,
    private val evenementRepository: EvenementRepository,
) {
    suspend operator fun invoke(mondeDeCampagne: (String) -> String) {
        dao.anciensEvenements().forEach { ancien ->
            val evenement = ancien.versBibliotheque(mondeDeCampagne(ancien.campagneId))
            evenementRepository.sauvegarder(evenement)
            ancien.villeId?.let { villeId ->
                val ids = dao.getEvenementsDuPoint(villeId)?.split(",")?.filter { it.isNotBlank() }
                if (ids != null && evenement.id !in ids) {
                    dao.definirEvenementsDuPoint(villeId, (ids + evenement.id).joinToString(","))
                }
            }
            dao.supprimerAncienEvenement(ancien.id)
        }
    }
}
