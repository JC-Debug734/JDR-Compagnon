package com.jc2.jdrcompagnon

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertTrue
import org.junit.Test

class NaheulbeukEquipmentTest {
    @Test
    fun `naheulbeuk equipment parsed fields`() {
        val raw = """
## Armures
- Cotte de mailles : 45 gp, CA 16, Force 13, discrétion désavantage, 55 lb
- Cuir : 10 po, CA 11 + mod Dex, —, —, 10 lb
""".trimIndent()
        val list = EquipmentParser.parse(raw)
        assertTrue("Equipment list should not be empty", list.isNotEmpty())
        val item = list.find { it.name.equals("Cotte de mailles", ignoreCase = true) }
            ?: throw AssertionError("Cotte de mailles not found")
        assertTrue("Cost should not be blank", item.cost.isNotBlank())
        assertTrue("Armor should have AC, strength, or stealth", item.ac.isNotBlank() || item.strength.isNotBlank() || item.stealth.isNotBlank())
    }
}
