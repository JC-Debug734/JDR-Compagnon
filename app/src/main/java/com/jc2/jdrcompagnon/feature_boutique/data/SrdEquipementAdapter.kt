package com.jc2.jdrcompagnon.feature_boutique.data

import android.content.Context
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.EquipementSourcePort
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlin.math.roundToInt

/**
 * Enveloppe SrdRepository.loadEquipmentList pour fournir les équipements à
 * GenererInventaireUseCase.
 *
 * EquipmentItem.cost est un texte libre français ("5 po", "10 pa", "—"...), jamais un
 * nombre brut : [parseCoutEnPc] extrait la valeur et la convertit en pièces de cuivre selon l'unité
 * (1 pp = 1000 pc, 1 po = 100 pc, 1 pa = 10 pc — barème standard D&D), pour garder exactement
 * les prix inférieurs à 1 po (torche 1 pc, ration 5 pa...). Les objets sans coût exploitable
 * (champ vide, "—", objets de quête...) sont exclus de la boutique plutôt que vendus à 0 po.
 */
class SrdEquipementAdapter(
    private val applicationContext: Context
) : EquipementSourcePort {

    override suspend fun getEquipementsDisponibles(monde: String): List<EquipementReference> {
        val equipements = SrdRepository.loadEquipmentList(applicationContext, worldId = monde)
        return equipements.mapNotNull { item ->
            val coutEnPc = parseCoutEnPc(item.cost)
            if (coutEnPc <= 0) return@mapNotNull null
            EquipementReference(
                nom = item.name,
                coutBaseEnPc = coutEnPc,
                type = item.category
            )
        }
    }

    private companion object {
        val COUT_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*(pp|po|pe|pa|pc)?""", RegexOption.IGNORE_CASE)

        fun parseCoutEnPc(cout: String): Int {
            val match = COUT_REGEX.find(cout) ?: return 0
            val valeur = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return 0
            val multiplicateur = when (match.groupValues[2].lowercase()) {
                "pp" -> 1000.0 // 1 pièce de platine = 1000 pc
                "pe" -> 50.0   // 1 pièce d'électrum = 50 pc
                "pa" -> 10.0   // 1 pièce d'argent = 10 pc
                "pc" -> 1.0
                else -> 100.0  // "po" ou unité absente : pièces d'or
            }
            return (valeur * multiplicateur).roundToInt()
        }
    }
}