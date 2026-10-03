package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Balises de especes_srd521.md : choix détectés, effets appliqués, texte de fiche. */
class EspeceBalisesTest {

    private val especes: List<Espece> = EspeceParser.parse(
        Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/especes_srd521.md"))
    )
    private val dons = listOf(Don("doue", "Doué", "Origines", "Trois compétences."))

    private fun espece(nom: String) = especes.first { it.nom == nom }

    @Test
    fun `chaque espece a ses choix de creation`() {
        val attendus = mapOf(
            "Drakéide" to listOf("ascendance-draconique"),
            "Elfe" to listOf("lignage-elfique", "lignage-elfique-incantation", "sens-aiguises"),
            "Gnome" to listOf("lignage-gnome", "lignage-gnome-incantation"),
            "Goliath" to listOf("ascendance-gigante"),
            "Halfelin" to emptyList(),
            "Humain" to listOf("taille", "competent", "polyvalent"),
            "Nain" to emptyList(),
            "Orc" to emptyList(),
            "Tieffelin" to listOf("taille", "heritage-fielon", "heritage-fielon-incantation"),
        )
        assertEquals(9, especes.size)
        attendus.forEach { (nom, ids) ->
            assertEquals(nom, ids, choixEspece(espece(nom), dons).map { it.id })
        }
    }

    @Test
    fun `options et descriptions resolues depuis le texte`() {
        val drakeide = choixEspece(espece("Drakéide"), dons).single()
        assertEquals(10, drakeide.options.size)
        assertTrue(drakeide.descriptions.getValue("Cuivre").contains("Acide"))
        val goliath = choixEspece(espece("Goliath"), dons).single()
        assertEquals(6, goliath.options.size)
        assertTrue(goliath.descriptions.getValue("Saut des nuées").contains("téléporter"))
        val competent = choixEspece(espece("Humain"), dons).first { it.id == "competent" }
        assertEquals(18, competent.options.size)
        assertEquals("Doué", choixEspece(espece("Humain"), dons).first { it.id == "polyvalent" }.recommande)
    }

    @Test
    fun `effets automatiques et choisis`() {
        val nain = effetsEspece(espece("Nain"), emptyMap())
        assertEquals(listOf("Poison"), nain.resistances)
        assertEquals(1, nain.pvParNiveau)
        assertEquals("Moyenne", nain.taille)

        val choix = mapOf("heritage-fielon" to listOf("Infernal"), "taille" to listOf("Petite"))
        val tieffelinN1 = effetsEspece(espece("Tieffelin"), choix, niveau = 1)
        assertEquals(listOf("Feu"), tieffelinN1.resistances)
        assertEquals(listOf("Trait de feu", "Thaumaturgie"), tieffelinN1.sorts)
        assertEquals("Petite", tieffelinN1.taille)
        assertTrue("Représailles infernales" in effetsEspece(espece("Tieffelin"), choix, niveau = 3).sorts)
        assertFalse("Ténèbres" in effetsEspece(espece("Tieffelin"), choix, niveau = 4).sorts)

        val humain = effetsEspece(espece("Humain"), mapOf("competent" to listOf("Perception"), "polyvalent" to listOf("Doué")))
        assertEquals(listOf("Perception"), humain.competences)
        assertEquals(listOf("Doué"), humain.dons)
        assertEquals(10.5, effetsEspece(espece("Elfe"), mapOf("lignage-elfique" to listOf("Elfe sylvestre"))).vitesse)
    }

    @Test
    fun `fiche - le choix et sa raison sont notes sous le trait`() {
        val texte = traitsEspecePourFiche(espece("Humain"), mapOf("competent" to listOf("Perception")), dons)
        assertTrue(texte.contains("Compétent (Humain) — Perception : Maîtrise choisie : Perception"))
        assertTrue(texte.contains("Pourquoi ce choix : trait « Compétent » de l'espèce Humain."))
        assertFalse("aucune balise ne fuit sur la fiche", texte.contains("<!--"))
        assertFalse(traitsEspecePourFiche(espece("Orc"), emptyMap()).contains("---"))
    }
}
