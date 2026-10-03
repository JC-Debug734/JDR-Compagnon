package com.jc2.jdrcompagnon.feature_carte

import com.jc2.jdrcompagnon.feature_carte.domain.usecase.PlanifierTrajet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanifierTrajetTest {

    @Test
    fun `9 m donnent 4,8 km par heure et 18 m le double`() {
        assertEquals(4.8, PlanifierTrajet.kmParHeure(9), 1e-9)
        assertEquals(9.6, PlanifierTrajet.kmParHeure(18), 1e-9)
    }

    @Test
    fun `une halte a chaque journee de marche, placee sur le trace`() {
        // Carte de 10 cases de large, 10 km par case : traversée horizontale = 100 km.
        // 5 km/h × 8 h = 40 km par jour → haltes à 40 km et 80 km.
        val plan = PlanifierTrajet.planifier(
            etapes = listOf(0f to 0.5f, 1f to 0.5f),
            casesEnLargeur = 10f,
            casesEnHauteur = 10f,
            echelleKmParCase = 10,
            kmParHeure = 5.0,
            heuresMaxParJour = 8,
        )
        assertEquals(100.0, plan.distanceKm, 1e-6)
        assertEquals(20.0, plan.heures, 1e-6)
        assertEquals(3, plan.jours)
        assertEquals(2, plan.arrets.size)
        assertEquals(0.4f, plan.arrets[0].fx, 1e-4f)
        assertEquals(0.8f, plan.arrets[1].fx, 1e-4f)
    }

    @Test
    fun `halte dans le bon segment d'un trace a plusieurs etapes`() {
        // Deux segments de 30 km (vertical puis horizontal), 40 km par jour : halte à 10 km dans le second.
        val plan = PlanifierTrajet.planifier(
            etapes = listOf(0f to 0f, 0f to 0.3f, 0.3f to 0.3f),
            casesEnLargeur = 10f,
            casesEnHauteur = 10f,
            echelleKmParCase = 10,
            kmParHeure = 5.0,
            heuresMaxParJour = 8,
        )
        assertEquals(1, plan.arrets.size)
        assertEquals(0.1f, plan.arrets[0].fx, 1e-4f)
        assertEquals(0.3f, plan.arrets[0].fy, 1e-4f)
    }

    @Test
    fun `pas de halte quand l'arrivee tombe dans la journee ou pile a la fin`() {
        val plan = PlanifierTrajet.planifier(listOf(0f to 0f, 0.4f to 0f), 10f, 10f, 10, 5.0, 8)
        assertTrue(plan.arrets.isEmpty())
        assertEquals(1, plan.jours)
    }
}
