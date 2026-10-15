package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeZone
import com.jc2.jdrcompagnon.feature_combat.domain.model.ZoneEffet
import com.jc2.jdrcompagnon.feature_combat.domain.model.vitesseEnMetres
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.cleDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistancesZonesTest {

    @Test
    fun `le contact vaut 1,5 m comme l allonge des regles`() {
        assertEquals(1.5, Distance.CONTACT.metres, 0.0)
        assertEquals(Distance.CONTACT, Distance.depuisMetres(1.0))
        assertEquals(Distance.M6, Distance.depuisMetres(5.0))
    }

    @Test
    fun `deplacement limite a la vitesse`() {
        // À 24 m avec 9 m de vitesse : on s'arrête à 18 m (palier jamais sous-estimé).
        assertEquals(Distance.M18, Distance.M24.versAvecDeplacement(Distance.CONTACT, 9.0))
        // En se précipitant (18 m) depuis 18 m : contact atteint.
        assertEquals(Distance.CONTACT, Distance.M18.versAvecDeplacement(Distance.CONTACT, 18.0))
        // Recul de 9 m depuis le contact : 10,5 m → palier 12 m au plus loin atteignable ≤ 10,5 ⇒ 9 m.
        assertEquals(Distance.COURTE, Distance.CONTACT.versAvecDeplacement(Distance.M60, 9.0))
    }

    @Test
    fun `vitesse des fiches en pieds convertie en metres`() {
        assertEquals(9.0, vitesseEnMetres(30), 0.0001)
        assertEquals(7.5, vitesseEnMetres(25), 0.0001)
        assertEquals(12.0, vitesseEnMetres(12), 0.0001)
    }

    @Test
    fun `lecture des zones d effet`() {
        assertEquals(ZoneEffet(FormeZone.SPHERE, 6.0), ZoneEffet.depuisTexte("chaque créature dans une sphère de 6 mètres de rayon centrée sur ce point"))
        assertEquals(ZoneEffet(FormeZone.CONE, 9.0), ZoneEffet.depuisTexte("JS Dextérité : DD 13, chaque créature dans un cône de 9 m."))
        assertEquals(ZoneEffet(FormeZone.LIGNE, 30.0), ZoneEffet.depuisTexte("une ligne de 30 m de long et 1,50 m de large"))
        assertEquals(ZoneEffet(FormeZone.CUBE, 4.5), ZoneEffet.depuisTexte("un cube de 4,50 m d'arête"))
        assertNull(ZoneEffet.depuisTexte("Touché : 5 (1d6 + 2) dégâts tranchants."))
    }

    private fun c(id: String, monstre: Boolean) =
        Combattant(id = id, nom = id, estMonstre = monstre, ca = 12, pvMax = 10, pv = 10, bonusInitiative = 0)

    @Test
    fun `boule de feu sur un gobelin touche ses voisins et le heros au contact`() {
        val etat = CombatEnCours(
            id = "c", titre = "t",
            combattants = listOf(c("mage", false), c("guerrier", false), c("g1", true), c("g2", true), c("g3", true)),
            distances = mapOf(
                cleDistance("g1", "g2") to Distance.M3,
                cleDistance("g1", "g3") to Distance.M24,
                cleDistance("g1", "guerrier") to Distance.CONTACT,
                cleDistance("g1", "mage") to Distance.LONGUE,
            ),
        )
        val touches = etat.dansLaZone(ZoneEffet(FormeZone.SPHERE, 6.0), "mage", "g1").map { it.id }.toSet()
        assertEquals(setOf("g1", "g2", "guerrier"), touches)
    }

    @Test
    fun `un cone ne touche que le camp adverse a portee`() {
        val etat = CombatEnCours(
            id = "c", titre = "t",
            combattants = listOf(c("dragon", true), c("allie", true), c("a", false), c("b", false)),
            distances = mapOf(cleDistance("dragon", "a") to Distance.M6, cleDistance("dragon", "b") to Distance.M12),
        )
        val touches = etat.dansLaZone(ZoneEffet(FormeZone.CONE, 9.0), "dragon", null).map { it.id }
        assertEquals(listOf("a"), touches)
    }
}
