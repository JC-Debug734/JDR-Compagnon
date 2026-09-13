package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class EquipmentParserFullTest {
    @Test
    fun `full equipment parse counts`() {
        val mdPath = Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/srd/equipment.md")
        val markdown = Files.readString(mdPath)
        val entries = EquipmentParser.parse(markdown)
        val weapons = entries.filter { it.category.equals("Armes", ignoreCase = true) }
        val armors = entries.filter { it.category.equals("Armure", ignoreCase = true) }
        // Expected counts based on spec: 37 weapons, 11 armors
        assertEquals("Nombre d'armes", 37, weapons.size)
        assertEquals("Nombre d'armures", 11, armors.size)
        // Ensure no parasite property
        val parasite = entries.find { it.rawMarkdown.contains("parasite", ignoreCase = true) }
        assertEquals("Aucune propriété parasite", null, parasite)
    }
}
