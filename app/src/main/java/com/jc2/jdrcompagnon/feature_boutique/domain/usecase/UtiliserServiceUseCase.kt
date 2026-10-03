package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.di.BoutiqueDependencies
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique

/**
 * Un personnage a utilisé un service de boutique (depuis le menu joueur, cf. ServicesLieuDialog) :
 * une place est décomptée si le service est limité, et il est désactivé quand il n'en reste plus
 * (ex. chambres toutes prises). Le paiement, lui, est fait sur la fiche du personnage.
 */
object UtiliserServiceUseCase {

    /** Renvoie la boutique mise à jour, ou null si la boutique ou le service n'existe plus. */
    suspend fun decompter(boutiqueId: String, serviceNom: String): Boutique? {
        val repo = BoutiqueDependencies.repository
        val boutique = repo.getBoutique(boutiqueId) ?: return null
        if (boutique.services.none { it.nom == serviceNom }) return null
        val misAJour = boutique.copy(
            services = boutique.services.map { s ->
                val restant = s.quantiteDisponible
                if (s.nom != serviceNom || restant == null) s
                else (restant - 1).coerceAtLeast(0).let { q -> s.copy(quantiteDisponible = q, actif = s.actif && q > 0) }
            }
        )
        repo.sauvegarderBoutique(misAJour)
        return misAJour
    }
}
