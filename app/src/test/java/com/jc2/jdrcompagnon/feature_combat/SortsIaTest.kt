package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.JetSort
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortPret
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortsIa
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import org.junit.Assert.assertEquals
import org.junit.Test

class SortsIaTest {

    private fun sort(nom: String, niveau: Int, jet: JetSort, formule: String?, temps: String = "1 action", portee: String = "36 m") = SortPret(
        nom = nom, niveau = niveau, tempsIncantation = temps, portee = portee, jet = jet,
        bonusAttaque = 5, dd = 13, sauvegarde = if (jet == JetSort.SAUVEGARDE) "Dextérité" else null,
        formuleDegats = formule, typeDegats = "feu", soin = null, resume = "",
    )

    @Test
    fun `sorts offensifs convertis en actions d'IA, limites aux emplacements restants`() {
        val attaques = SortsIa.attaques(
            listOf(
                sort("Trait de feu", 0, JetSort.ATTAQUE, "1d10"),
                sort("Mains brûlantes", 1, JetSort.SAUVEGARDE, "3d6", portee = "Personnelle"),
                sort("Boule de feu", 3, JetSort.SAUVEGARDE, "8d6", portee = "45 m"),
                sort("Soins", 1, JetSort.AUCUN, null),
                sort("Bouclier", 1, JetSort.AUCUN, null, temps = "1 réaction"),
            ),
            emplacementsRestants = mapOf(1 to 2, 2 to 0, 3 to 0),
            pacteRestant = 0,
        )
        assertEquals(listOf("Trait de feu", "Mains brûlantes"), attaques.map { it.nom })
        val trait = attaques.first()
        assertEquals(TypeAttaqueMonstre.DISTANCE, trait.type)
        assertEquals(5, trait.bonusToucher)
        assertEquals(null, trait.usagesParJour)
        val mains = attaques[1]
        assertEquals(TypeAttaqueMonstre.SAUVEGARDE, mains.type)
        assertEquals(13, mains.dd)
        assertEquals(2, mains.usagesParJour)
        assertEquals(10, mains.degatsMoyens)
    }
}
