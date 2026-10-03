package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueHeros
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.DeclarationHeros
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatSimulation
import com.jc2.jdrcompagnon.feature_combat.domain.model.FrappeHeros
import com.jc2.jdrcompagnon.feature_combat.domain.model.HerosSimule
import com.jc2.jdrcompagnon.feature_combat.domain.model.IssueSimulation
import com.jc2.jdrcompagnon.feature_combat.domain.model.JetSort
import com.jc2.jdrcompagnon.feature_combat.domain.model.SimulateurCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSimule
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationCombatTest {

    private val epee = AttaqueHeros("Épée longue", bonusToucher = 5, formuleDegats = "1d8 + 3", aDistance = false)
    private val arc = AttaqueHeros("Arc court", bonusToucher = 5, formuleDegats = "1d6 + 3", aDistance = true)

    private fun heros(pv: Int = 30, attaques: List<AttaqueHeros> = listOf(epee), emplacements: Map<Int, Int> = emptyMap()) =
        HerosSimule(nom = "Aldric", ca = 16, pvMax = pv, attaques = attaques, nbAttaques = 1, emplacements = emplacements, bonusInitiative = 100)

    private fun gobelin(numero: Int, pv: Int = 7) = Combattant(
        id = "gob$numero",
        nom = "Gobelin $numero",
        estMonstre = true,
        ca = 13,
        pvMax = pv,
        pv = pv,
        bonusInitiative = 2,
        attaques = listOf(
            AttaqueMonstre("Cimeterre", TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = 4, formuleDegats = "1d6 + 2", degatsMoyens = 5)
        ),
    )

    /** Démarre au premier tour du personnage (initiative +100 : il agit avant les monstres). */
    private fun auTourDuHeros(simulateur: SimulateurCombat, monstres: List<Combattant>, distance: Distance): EtatSimulation {
        val etat = simulateur.demarrer(monstres, distance)
        check(etat.tourDuHeros)
        return etat
    }

    @Test
    fun `les monstres jouent seuls jusqu'au tour du personnage`() {
        val etat = SimulateurCombat(heros(), Random(1)).demarrer(listOf(gobelin(1), gobelin(2)))
        assertTrue(etat.issue != null || etat.tourDuHeros)
    }

    @Test
    fun `une simulation automatique va jusqu'a une issue`() {
        repeat(20) { graine ->
            val simulateur = SimulateurCombat(heros(), Random(graine))
            val fin = simulateur.simulerJusquALaFin(simulateur.demarrer(listOf(gobelin(1), gobelin(2))))
            assertNotNull(fin.issue)
            when (fin.issue) {
                IssueSimulation.VICTOIRE -> assertTrue(fin.monstresDebout.isEmpty())
                IssueSimulation.DEFAITE -> assertEquals(0, fin.heros.pv)
                else -> Unit
            }
        }
    }

    @Test
    fun `un personnage tres fort gagne contre un gobelin`() {
        val colosse = HerosSimule(nom = "Colosse", ca = 30, pvMax = 200, attaques = listOf(epee.copy(bonusToucher = 30)), nbAttaques = 3)
        val simulateur = SimulateurCombat(colosse, Random(3))
        val fin = simulateur.simulerJusquALaFin(simulateur.demarrer(listOf(gobelin(1))))
        assertEquals(IssueSimulation.VICTOIRE, fin.issue)
    }

    @Test
    fun `depuis la longue distance le contact demande de se precipiter`() {
        val gobelinPassif = gobelin(1).copy(attaques = emptyList())
        val simulateur = SimulateurCombat(heros(pv = 500), Random(4))
        val etat = auTourDuHeros(simulateur, listOf(gobelinPassif), Distance.LONGUE)
        val apres = simulateur.jouerHeros(
            etat,
            DeclarationHeros("Esquiver", "dodge", deplacementCibleId = "gob1", distanceVisee = Distance.CONTACT)
        )
        assertTrue(apres.journal.any { it.texte.contains("le contact demande de se précipiter") })
    }

    @Test
    fun `quitter le contact sans se desengager provoque une attaque d'opportunite`() {
        val simulateur = SimulateurCombat(heros(pv = 500), Random(5))
        val etat = auTourDuHeros(simulateur, listOf(gobelin(1, pv = 500)), Distance.CONTACT)
        val sansDesengager = simulateur.jouerHeros(
            etat, DeclarationHeros("Esquiver", "dodge", deplacementCibleId = "gob1", distanceVisee = Distance.COURTE)
        )
        assertTrue(sansDesengager.journal.any { it.texte.startsWith("Attaque d'opportunité de Gobelin 1") })

        val avecDesengager = simulateur.jouerHeros(
            etat, DeclarationHeros("Se désengager", "disengage", deplacementCibleId = "gob1", distanceVisee = Distance.COURTE)
        )
        assertTrue(avecDesengager.journal.none { it.texte.startsWith("Attaque d'opportunité de Gobelin 1") })
    }

    @Test
    fun `un sort consomme un emplacement de la simulation`() {
        val projectile = SortSimule("Projectile magique", 1, JetSort.AUCUN, 0, 0, null, "3d4 + 3", null)
        val simulateur = SimulateurCombat(heros(pv = 500, emplacements = mapOf(1 to 1)), Random(6))
        val etat = auTourDuHeros(simulateur, listOf(gobelin(1, pv = 500)), Distance.LONGUE)
        val apres = simulateur.jouerHeros(etat, DeclarationHeros("Lancer : Projectile magique (emplacement niv. 1)", "magie", "gob1", sort = projectile, niveauEmplacement = 1))
        assertEquals(0, apres.emplacements[1])
        assertTrue(apres.monstres.first().pv < 500)
    }

    @Test
    fun `un sort de zone touche tous les monstres vises`() {
        val bouleDeFeu = SortSimule("Boule de feu", 3, JetSort.SAUVEGARDE, 0, 15, "Dextérité", "8d6", null)
        val simulateur = SimulateurCombat(heros(pv = 500, emplacements = mapOf(3 to 1)), Random(4))
        val monstres = listOf(gobelin(1, pv = 500), gobelin(2, pv = 500), gobelin(3, pv = 500))
        val etat = auTourDuHeros(simulateur, monstres, Distance.LONGUE)
        val apres = simulateur.jouerHeros(
            etat,
            DeclarationHeros("Lancer : Boule de feu (emplacement niv. 3)", "magie", "gob1", ciblesZone = listOf("gob1", "gob2"), sort = bouleDeFeu, niveauEmplacement = 3)
        )
        assertEquals(0, apres.emplacements[3])
        // 8d6 : au moins 8 dégâts (4 si le JS est réussi) sur chaque cible de la zone, aucun sur la troisième.
        assertTrue(apres.monstres.first { it.id == "gob1" }.pv <= 496)
        assertTrue(apres.monstres.first { it.id == "gob2" }.pv <= 496)
        assertEquals(500, apres.monstres.first { it.id == "gob3" }.pv)
    }

    @Test
    fun `une potion soigne le personnage`() {
        val simulateur = SimulateurCombat(heros(pv = 40).copy(pvDepart = 10), Random(8))
        val etat = auTourDuHeros(simulateur, listOf(gobelin(1).copy(attaques = emptyList())), Distance.LONGUE)
        val apres = simulateur.jouerHeros(etat, DeclarationHeros("Utiliser", "utilize", objetNom = "Potion de soins", soinObjet = "2d4+2"))
        assertTrue(apres.heros.pv > 10)
    }

    @Test
    fun `une bousculade reussie met le monstre a terre`() {
        val simulateur = SimulateurCombat(heros(pv = 500), Random(9))
        val etat = auTourDuHeros(simulateur, listOf(gobelin(1, pv = 500)), Distance.CONTACT)
        // DD 21 : le jet du monstre (d20 sans bonus) échoue toujours.
        val apres = simulateur.jouerHeros(etat, DeclarationHeros("Attaquer : Bousculade", "attaquer", "gob1", frappes = listOf(FrappeHeros.Bousculade(21))))
        assertTrue(apres.journal.any { it.texte.contains("à terre") })
    }
}
