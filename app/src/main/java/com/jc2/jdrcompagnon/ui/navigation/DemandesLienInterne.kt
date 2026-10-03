package com.jc2.jdrcompagnon.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ouverture d'un lien interne (type, nom) — fiche PNJ, fiche de monstre, écran de combat... —
 * depuis un composant qui n'a pas accès au NavController (dialogue d'événement réutilisé dans
 * plusieurs écrans). JdrNavGraph observe la demande et la traite comme un lien de scénario
 * (voir navigateToInternalLink).
 */
object DemandesLienInterne {
    private val _demande = MutableStateFlow<Pair<String, String>?>(null)
    val demande: StateFlow<Pair<String, String>?> = _demande

    fun ouvrir(type: String, nom: String) {
        _demande.value = type to nom
    }

    fun consommer() {
        _demande.value = null
    }
}
