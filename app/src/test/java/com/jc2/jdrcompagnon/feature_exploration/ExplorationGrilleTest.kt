package com.jc2.jdrcompagnon.feature_exploration

import org.junit.Assert.assertEquals
import org.junit.Test

class ExplorationGrilleTest {

    @Test
    fun `une zone est devoilee d'un coup puis remasquee`() {
        val zone = ZoneExploration(nom = "Cuisine", cases = setOf(1, 2, 3))
        val apresReveal = ExplorationGrille.basculerZone(setOf(3, 10), zone)
        assertEquals(setOf(1, 2, 3, 10), apresReveal)
        // Entièrement visible : un nouveau clic la remasque, sans toucher aux autres cases.
        assertEquals(setOf(10), ExplorationGrille.basculerZone(apresReveal, zone))
    }

    @Test
    fun `toucher une case donne sa zone, la plus precise si elles se chevauchent`() {
        val salle = ZoneExploration(nom = "B3 Salle à manger", cases = setOf(1, 2, 3, 4, 5))
        val placard = ZoneExploration(nom = "B4 Placard", cases = setOf(5))
        val zones = listOf(salle, placard)
        assertEquals(salle, ExplorationGrille.zonePourCase(zones, 2))
        assertEquals(placard, ExplorationGrille.zonePourCase(zones, 5))
        assertEquals(null, ExplorationGrille.zonePourCase(zones, 9))
    }

    @Test
    fun `une zone decrite en pixels couvre les cases dont le centre y tombe`() {
        // Image 100x100 en grille 4x4 (cases de 25 px, centres à 12.5, 37.5...) :
        // le rectangle 0,0,50,50 couvre le quart haut-gauche.
        val cases = ExplorationGrille.casesPourRectangles(listOf(intArrayOf(0, 0, 50, 50)), 100, 100, 4, 4)
        assertEquals(setOf(0, 1, 4, 5), cases)
        assertEquals(5, ExplorationGrille.lignes(4, 100, 125))
    }

    @Test
    fun `changer la taille des cases conserve la meme surface`() {
        // Grille 2x2 : case en haut à gauche (0). En 4x4, elle couvre les 4 cases du quart haut-gauche.
        assertEquals(setOf(0, 1, 4, 5), ExplorationGrille.convertir(setOf(0), 2, 2, 4, 4))
        // Et inversement, en revenant à 2x2.
        assertEquals(setOf(0), ExplorationGrille.convertir(setOf(0, 1, 4, 5), 4, 4, 2, 2))
    }
}
