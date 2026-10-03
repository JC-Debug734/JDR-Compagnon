package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.TrancheDefi
import com.jc2.jdrcompagnon.ui.screens.mj.challengeSortKey
import com.jc2.jdrcompagnon.ui.screens.mj.classesDuSort
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdSectionEntry
import com.jc2.jdrcompagnon.ui.screens.mj.monsterChallenge
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltresBibliothequeTest {

    @Test
    fun `tranches de defi`() {
        assertEquals(TrancheDefi.MINEURE, TrancheDefi.de("0"))
        assertEquals(TrancheDefi.MINEURE, TrancheDefi.de("1/4"))
        assertEquals(TrancheDefi.MINEURE, TrancheDefi.de("-"))
        assertEquals(TrancheDefi.PALIER_1, TrancheDefi.de("1"))
        assertEquals(TrancheDefi.PALIER_1, TrancheDefi.de("4"))
        assertEquals(TrancheDefi.PALIER_2, TrancheDefi.de("5"))
        assertEquals(TrancheDefi.PALIER_3, TrancheDefi.de("16"))
        assertEquals(TrancheDefi.PALIER_4, TrancheDefi.de("30"))
    }

    @Test
    fun `chaque monstre du bestiaire a un FP reconnu`() {
        val monstres = MonsterParser.parse(File("src/main/assets/dnd/monster_srd521.md").readText())
        val inconnus = monstres.mapNotNull { m -> monsterChallenge(m)?.first?.takeIf { challengeSortKey(it) >= 999.0 }?.let { m.name to it } }
        assertTrue("FP non reconnus : $inconnus", inconnus.isEmpty())
        // Toutes les tranches sont représentées.
        assertEquals(TrancheDefi.entries.toSet(), monstres.mapNotNull { monsterChallenge(it)?.first?.let(TrancheDefi::de) }.toSet())
    }

    @Test
    fun `classes d'un sort`() {
        assertEquals(listOf("Barde", "Clerc", "Rôdeur"), classesDuSort(SrdSectionEntry("Soins", "Niveau 1", "", classes = "Barde, Clerc , Rôdeur")))
        assertEquals(emptyList<String>(), classesDuSort(SrdSectionEntry("X", "Niveau 1", "")))
    }
}
