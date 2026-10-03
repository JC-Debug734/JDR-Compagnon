package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.scenario.TYPES_MENTION
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.couleurLien
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.extractInternalLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MentionsScenarioTest {

    private val texte = "Le #perso:[Grand Al] attend à la #lieu:[Ferme Crâne-beurré] près de #lieu:Phandalin, " +
        "gardé par des #monster:[Orc]. Parlez à #pnj:[Grand Al Kalazorn] et trouvez la #equipment:[Cotte de mailles]."

    @Test
    fun `les mentions de lieux et de personnages ne sont pas des liens`() {
        assertEquals(
            listOf("monster" to "Orc", "pnj" to "Grand Al Kalazorn", "equipment" to "Cotte de mailles"),
            extractInternalLinks(texte),
        )
        assertEquals(setOf("lieu", "perso", "faction"), TYPES_MENTION)
    }

    @Test
    fun `chaque type de nom a sa couleur, personnages cites et PNJ partagent la leur`() {
        assertEquals(couleurLien("pnj"), couleurLien("perso"))
        assertNotEquals(couleurLien("lieu"), couleurLien("perso"))
        assertNotEquals(couleurLien("lieu"), couleurLien("monster"))
        assertNotEquals(couleurLien("monster"), couleurLien("equipment"))
    }
}
