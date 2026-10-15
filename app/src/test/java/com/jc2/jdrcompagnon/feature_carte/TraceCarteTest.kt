package com.jc2.jdrcompagnon.feature_carte

import com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession
import com.jc2.jdrcompagnon.feature_carte.ui.CarteTable
import com.jc2.jdrcompagnon.network.EtapeVoyageData
import com.jc2.jdrcompagnon.network.TraceCarteData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceCarteTest {

    private fun trace(termine: Boolean, vararg points: Pair<Float, Float>, carteId: String = "c1") =
        TraceCarteData("camp", carteId, points.map { (x, y) -> EtapeVoyageData(null, x, y) }, termine, "MJ")

    @After
    fun reinitialiser() {
        TraceCarteSession.diffuseur = {}
        TraceCarteSession.recevoir(null)
    }

    @Test
    fun `un trace publie n est diffuse qu une fois tant qu il ne change pas`() {
        val envois = mutableListOf<TraceCarteData?>()
        TraceCarteSession.diffuseur = { envois += it }
        TraceCarteSession.publier(trace(false, 0.1f to 0.1f))
        TraceCarteSession.publier(trace(false, 0.1f to 0.1f).copy(auteur = "Autre"))
        TraceCarteSession.publier(trace(true, 0.1f to 0.1f))
        assertEquals(2, envois.size)
        assertTrue(envois.last()!!.termine)
    }

    @Test
    fun `un trace recu n est pas renvoye`() {
        val envois = mutableListOf<TraceCarteData?>()
        TraceCarteSession.diffuseur = { envois += it }
        TraceCarteSession.recevoir(trace(true, 0.2f to 0.2f, 0.4f to 0.4f))
        TraceCarteSession.publier(trace(true, 0.2f to 0.2f, 0.4f to 0.4f))
        assertTrue(envois.isEmpty())
    }

    @Test
    fun `la table ne zoome qu une fois le trace termine et cadre le trajet`() {
        val (etapes, enCours) = CarteTable.traceEtCadre(trace(false, 0.2f to 0.2f, 0.6f to 0.4f), "c1")
        assertEquals(2, etapes.size)
        assertNull(enCours)

        val cadre = CarteTable.traceEtCadre(trace(true, 0.2f to 0.2f, 0.6f to 0.4f), "c1").second!!
        assertEquals(0.6f, cadre.largeur, 1e-4f)
        assertEquals(0.3f, cadre.hauteur, 1e-4f)
        // Le trajet est au centre du cadre.
        assertEquals(0.4f, cadre.x + cadre.largeur / 2f, 1e-4f)
        assertEquals(0.3f, cadre.y + cadre.hauteur / 2f, 1e-4f)
    }

    @Test
    fun `trajet tres court ou au bord et trace d une autre carte`() {
        val court = CarteTable.traceEtCadre(trace(true, 0.95f to 0.95f, 0.96f to 0.96f), "c1").second!!
        assertEquals(0.25f, court.largeur, 1e-4f)
        assertEquals(0.75f, court.x, 1e-4f) // reste dans l'image
        val autre = CarteTable.traceEtCadre(trace(true, 0.1f to 0.1f, 0.5f to 0.5f, carteId = "c2"), "c1")
        assertTrue(autre.first.isEmpty())
        assertNull(autre.second)
    }
}
