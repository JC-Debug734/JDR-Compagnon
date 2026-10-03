package com.jc2.jdrcompagnon.feature_carte

import com.jc2.jdrcompagnon.feature_carte.data.local.EvenementAleatoireEntity
import com.jc2.jdrcompagnon.feature_carte.data.mapper.versBibliotheque
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ConversionEvenementsCarteTest {

    private fun ancien(effetsJson: String) = EvenementAleatoireEntity(
        id = "evt-1",
        campagneId = "camp-1",
        villeId = "ville-1",
        titre = "Querelle au marché",
        description = "Deux marchands en viennent aux mains.",
        effetsJson = effetsJson
    )

    @Test
    fun `une reputation perdue donne une complication et un effet de perte`() {
        val evenement = ancien("""[{"kind":"REPUTATION","factionNom":"Guilde","delta":-5},{"kind":"INFO","texte":"La garde arrive."}]""")
            .versBibliotheque("monde-1")

        assertEquals("evt-1", evenement.id)
        assertEquals("monde-1", evenement.worldId)
        assertEquals("camp-1", evenement.campagneId)
        assertEquals(TypeEvenement.COMPLICATION, evenement.type)
        val reputation = evenement.effets[0]
        assertEquals(CategorieEffet.REPUTATION, reputation.categorie)
        assertFalse(reputation.gain)
        assertEquals(5, reputation.quantite)
        assertEquals("Guilde", reputation.cible)
        assertEquals(CategorieEffet.AUTRE, evenement.effets[1].categorie)
        assertEquals("La garde arrive.", evenement.effets[1].description)
    }

    @Test
    fun `une reputation gagnee donne une opportunite`() {
        val evenement = ancien("""[{"kind":"REPUTATION","factionNom":"Habitants","delta":8}]""").versBibliotheque("monde-1")
        assertEquals(TypeEvenement.OPPORTUNITE, evenement.type)
    }

    @Test
    fun `sans reputation ni json valide on obtient une rencontre sans effet`() {
        val evenement = ancien("pas du json").versBibliotheque("monde-1")
        assertEquals(TypeEvenement.RENCONTRE, evenement.type)
        assertEquals(0, evenement.effets.size)
    }
}
