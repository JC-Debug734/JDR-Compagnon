package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendrierTest {

    @Test
    fun `retirer un mois decale les jours feries et le climat`() {
        val config = CalendarConfig(
            monthNames = listOf("A", "B", "C"),
            daysPerMonth = listOf(30, 20, 10),
            joursFeries = listOf(JourFerie("Fête B", 1, 5), JourFerie("Fête C", 2, 3)),
            climatParMois = listOf(ClimatMois.hiver, ClimatMois.printemps, ClimatMois.ete),
        )
        val maj = config.avecMoisRetire(1)
        assertEquals(listOf("A", "C"), maj.monthNames)
        assertEquals(listOf(30, 10), maj.daysPerMonth)
        assertEquals(listOf(JourFerie("Fête C", 1, 3)), maj.joursFeries)
        assertEquals(listOf(ClimatMois.hiver, ClimatMois.ete), maj.climatParMois)
    }

    @Test
    fun `le dernier mois ne peut pas etre retire`() {
        val config = CalendarConfig(monthNames = listOf("Unique"), daysPerMonth = listOf(365))
        assertEquals(config, config.avecMoisRetire(0))
    }

    @Test
    fun `climat d un mois sans entree complete la liste`() {
        val config = CalendarConfig().avecClimat(3, ClimatMois.automne)
        assertEquals(4, config.climatParMois.size)
        assertEquals(ClimatMois.automne, config.climat(3))
        assertEquals(ClimatMois(), config.climat(0))
        assertNull(config.climat(11))
    }

    @Test
    fun `le calendrier des harpistes a un climat par mois`() {
        val harptos = CalendarConfig.calendrierHarptos()
        assertEquals(harptos.monthNames.size, harptos.climatParMois.size)
        assertTrue(ScenarioWeather.FORTE_NEIGE in harptos.climat(0)!!.meteos)
        // Flamerule (7e mois) : plein été.
        assertTrue(ScenarioTemperature.FORTE_CHALEUR in harptos.climat(6)!!.temperatures)
    }

    @Test
    fun `classique et harpistes ont 12 mois et 365 jours, jours feries valides`() {
        listOf(CalendarConfig.calendrierClassique(), CalendarConfig.calendrierHarptos()).forEach { c ->
            assertEquals(12, c.monthNames.size)
            assertEquals(12, c.climatParMois.size)
            assertEquals(365, c.daysPerMonth.sum())
            c.joursFeries.forEach { f -> assertTrue("${c.nom} ${f.nom}", f.jour in 1..c.daysPerMonth[f.mois]) }
        }
    }

    @Test
    fun `saisons de l hemisphere nord`() {
        val classique = CalendarConfig.calendrierClassique()
        assertEquals("Hiver", classique.climat(0)!!.saison)
        assertEquals("Printemps", classique.climat(2)!!.saison)
        assertEquals("Été", classique.climat(6)!!.saison)
        assertEquals("Automne", classique.climat(9)!!.saison)
        assertEquals("Hiver", classique.climat(11)!!.saison)
    }
}
