package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.ui.adversairesDuScenario
import org.junit.Assert.assertEquals
import org.junit.Test

class AdversairesDuScenarioTest {

    @Test
    fun `seuls les monstres, PNJ et combattants du scenario sont proposes`() {
        val contenu = """
            La mine est gardée par #monster:Gobelin et un #monster:[Loup sanguinaire].
            Le contremaître #pnj:[Aldric le Borgne] trahit le groupe.
            Un #equipment:[Pied-de-biche] traîne au sol, voir #rule:Lumière.
            #combat:[Gobelin x3, Ogre]
            Plus loin, un autre #monster:gobelin.
        """.trimIndent()

        assertEquals(
            listOf("Aldric le Borgne", "Gobelin", "Loup sanguinaire", "Ogre"),
            adversairesDuScenario(contenu)
        )
    }

    @Test
    fun `scenario sans creature - liste vide`() {
        assertEquals(emptyList<String>(), adversairesDuScenario("Une scène calme, sans lien."))
    }
}
