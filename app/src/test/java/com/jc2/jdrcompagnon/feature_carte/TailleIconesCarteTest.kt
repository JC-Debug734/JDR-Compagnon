package com.jc2.jdrcompagnon.feature_carte

import com.jc2.jdrcompagnon.feature_carte.ui.tailleIconesSansChevauchement
import org.junit.Assert.assertEquals
import org.junit.Test

class TailleIconesCarteTest {
    @Test
    fun `les icones retrecissent quand deux lieux sont proches, sans descendre sous 16 dp`() {
        // Lieux espacés : taille normale.
        assertEquals(36f, tailleIconesSansChevauchement(listOf(0f to 0f, 100f to 0f), 36f), 0.01f)
        // Conyberry et le sanctuaire à 22 dp : 90 % de l'écart.
        assertEquals(19.8f, tailleIconesSansChevauchement(listOf(0f to 0f, 22f to 0f, 200f to 200f), 36f), 0.01f)
        // Très proches : plancher de 16 dp.
        assertEquals(16f, tailleIconesSansChevauchement(listOf(0f to 0f, 5f to 0f), 36f), 0.01f)
        // Un seul lieu, ou deux empilés au même endroit : taille normale.
        assertEquals(36f, tailleIconesSansChevauchement(listOf(10f to 10f), 36f), 0.01f)
        assertEquals(36f, tailleIconesSansChevauchement(listOf(10f to 10f, 10f to 10f), 36f), 0.01f)
    }
}
