package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Parse le fichier d'équipement D&D réellement chargé par SrdRepository
 * (dnd/equipement_srd521.md, format "### Nom" + champs "**Champ** valeur", cf.
 * EquipmentParser.parseBoldFields) et vérifie les comptes attendus. L'ancien chemin
 * "srd/equipment.md" n'existe plus : le fichier a été restructuré en blocs et déplacé
 * sous dnd/, cf. FILE_EQUIPMENT dans SrdRepository.kt.
 */
class EquipmentParserFullTest {
    @Test
    fun `full equipment parse counts`() {
        val mdPath = Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/equipement_srd521.md")
        val markdown = Files.readString(mdPath)
        val entries = EquipmentParser.parse(markdown)
        // Catégorie = "Armes"/"Armures" + sous-catégorie (ex. "Armes Courante"), cf.
        // EquipmentParser.buildBoldFieldCategoryLabel : on filtre par préfixe, pas égalité.
        val weapons = entries.filter { it.category.startsWith("Armes", ignoreCase = true) }
        val armors = entries.filter { it.category.startsWith("Armures", ignoreCase = true) }
        assertEquals("Nombre d'armes", 38, weapons.size)
        assertEquals("Nombre d'armures", 13, armors.size)
        // Ensure no parasite property
        assertTrue("Toutes les armes ont un nom", weapons.all { it.name.isNotBlank() })
        assertTrue("Toutes les armures ont une CA", armors.all { it.ac.isNotBlank() })
    }
}
