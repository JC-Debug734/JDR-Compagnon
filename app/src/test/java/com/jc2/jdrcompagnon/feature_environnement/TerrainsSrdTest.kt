package com.jc2.jdrcompagnon.feature_environnement

import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.domain.model.devinerTerrains
import com.jc2.jdrcompagnon.feature_environnement.domain.model.rencontrableDans
import com.jc2.jdrcompagnon.feature_environnement.domain.model.terrainsEffectifs
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerrainsSrdTest {

    @Test
    fun `les environnements d'exemple ont un terrain deviné`() {
        assertEquals(listOf("Forêt"), devinerTerrains("Forêt profonde"))
        assertEquals(listOf("Montagne"), devinerTerrains("Montagnes escarpées"))
        assertEquals(listOf("Marais"), devinerTerrains("Marais fétide"))
        assertEquals(listOf("Donjon/Ruines"), devinerTerrains("Donjon en ruines"))
        assertEquals(listOf("Urbain"), devinerTerrains("Ville animée"))
    }

    @Test
    fun `un mot-cle court ne reconnait pas un mot plus long`() {
        assertTrue(devinerTerrains("Repaire des mercenaires").isEmpty())
    }

    @Test
    fun `les terrains choisis priment sur ceux devines`() {
        val env = Environnement(nom = "Forêt profonde", terrains = listOf("Marais"))
        assertEquals(listOf("Marais"), env.terrainsEffectifs)
    }

    @Test
    fun `un monstre est filtre par terrain, y compris sur une valeur partielle`() {
        val gobelin = SrdEntry(name = "Gobelin", rawMarkdown = "", environments = listOf("Forêt", "Plaines"))
        val kraken = SrdEntry(name = "Kraken", rawMarkdown = "", environments = listOf("Aquatique", "Souterrain"))
        assertTrue(gobelin.rencontrableDans(listOf("Forêt")))
        assertFalse(gobelin.rencontrableDans(listOf("Marais")))
        assertTrue(kraken.rencontrableDans(listOf("Grotte/Souterrain")))
        assertFalse(gobelin.rencontrableDans(emptyList()))
    }
}
