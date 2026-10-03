package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import org.junit.Assert.assertEquals
import org.junit.Test

class ParseListeEquipementTest {

    @Test
    fun `chaque objet apparait autant de fois que sa quantite`() {
        val texte = "Armure de cuir, 2 dagues, instrument de musique au choix, paquetage d'artiste et 19 po"
        val objets = parseListeEquipement(texte)

        assertEquals(
            listOf("Armure de cuir", "Dagues", "Dagues", "Instrument de musique au choix", "Artiste"),
            objets
        )
    }

    @Test
    fun `l or seul n est jamais un objet`() {
        assertEquals(emptyList<String>(), parseListeEquipement("90 po"))
    }

    @Test
    fun `paquetage de suit le meme prefixe que paquetage d`() {
        assertEquals(listOf("Cambrioleur"), parseListeEquipement("paquetage de cambrioleur"))
    }

    @Test
    fun `option avec beaucoup d objets garde le bon compte total`() {
        val texte = "Cotte de mailles, épée à deux mains, fléau d'armes, 8 javelines, paquetage d'exploration souterraine et 4 po"
        val objets = parseListeEquipement(texte)

        assertEquals(12, objets.size) // Cotte, épée, fléau, 8 javelines, paquetage = 1+1+1+8+1
        assertEquals(8, objets.count { it == "Javelines" })
        assertEquals(listOf("Exploration souterraine"), objets.filter { it == "Exploration souterraine" })
    }

    @Test
    fun `texte vide ne produit aucun objet`() {
        assertEquals(emptyList<String>(), parseListeEquipement(""))
    }
}
