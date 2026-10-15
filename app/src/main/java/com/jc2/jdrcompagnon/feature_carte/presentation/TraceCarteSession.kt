package com.jc2.jdrcompagnon.feature_carte.presentation

import com.jc2.jdrcompagnon.network.TraceCarteData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracé en cours sur une carte de campagne, partagé entre toutes les vues : écran du MJ, appareils
 * des joueurs et page table.
 *
 * Celui qui trace (MJ ou joueur) le [publier] ; NetworkSessionManager l'envoie ([diffuseur]) : le
 * MJ le diffuse à tous (TYPE_MAP_TRACE) et l'affiche sur la table, un joueur l'envoie au MJ qui le
 * rediffuse. Un tracé reçu du réseau est seulement reflété ([recevoir]) : l'écran de carte
 * l'affiche, et le cadre (zoom sur le trajet) une fois le tracé terminé.
 */
object TraceCarteSession {

    private val _etat = MutableStateFlow<TraceCarteData?>(null)
    val etat: StateFlow<TraceCarteData?> = _etat.asStateFlow()

    /** Envoi réseau d'un tracé fait sur cet appareil (renseigné par NetworkSessionManager). */
    var diffuseur: (TraceCarteData?) -> Unit = {}

    /** Tracé modifié sur cet appareil : partagé avec les autres vues (sans effet s'il n'a pas changé). */
    fun publier(data: TraceCarteData) {
        if (data.memeTrace(_etat.value)) return
        _etat.value = data
        diffuseur(data)
    }

    /** Tracé reçu du réseau (MJ ou joueur). */
    fun recevoir(data: TraceCarteData?) {
        _etat.value = data
    }
}
