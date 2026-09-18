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
 * nombre brut : [parseCoutEnPo] extrait la valeur et convertit selon l'unité (1 pp = 10 po,
 * 1 po = 10 pa, 1 pa = 10 pc — barème standard D&D). Les objets sans coût exploitable
 * (champ vide, "—", objets de quête...) sont exclus de la boutique plutôt que vendus à 0 po.
 */
class SrdEquipementAdapter(
    private val applicationContext: Context
) : EquipementSourcePort {

    override suspend fun getEquipementsDisponibles(monde: String): List<EquipementReference> {
        val equipements = SrdRepository.loadEquipmentList(applicationContext, worldId = monde)
        return equipements.mapNotNull { item ->
            val coutEnPo = parseCoutEnPo(item.cost)
            if (coutEnPo <= 0) return@mapNotNull null
            EquipementReference(
                nom = item.name,
                coutBaseEnPo = coutEnPo,
                type = item.category
            )
        }
    }

    private companion object {
        val COUT_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*(pp|po|pa|pc)?""", RegexOption.IGNORE_CASE)

        fun parseCoutEnPo(cout: String): Int {
            val match = COUT_REGEX.find(cout) ?: return 0
            val valeur = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return 0
            val multiplicateur = when (match.groupValues[2].lowercase()) {
                "pp" -> 10.0   // 1 pièce de platine = 10 po
                "pa" -> 0.1    // 1 pièce d'argent = 0.1 po
                "pc" -> 0.01   // 1 pièce de cuivre = 0.01 po
                else -> 1.0    // "po" ou unité absente : déjà en pièces d'or
            }
            return (valeur * multiplicateur).roundToInt()
        }
    }
}