package com.jc2.jdrcompagnon.ui.screens.joueur.character

import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.DonParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EspeceParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.entreeFicheAvecChoix
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.traitsEspecePourFiche
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Une capacité de fiche = une carte, même quand sa description SRD contient des lignes vides. */
class CapacitesFicheTest {

    private fun lire(nom: String) = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/$nom"))

    @Test
    fun `chaque trait d'espece reste une seule capacite`() {
        EspeceParser.parse(lire("especes_srd521.md")).forEach { espece ->
            val texte = traitsEspecePourFiche(espece, emptyMap())
            val attendus = espece.traits.filterNot { it.base }.map { "${it.nom} (${espece.nom})" }
            val obtenus = parserCapacitesFiche(texte).map { it.titre.substringBefore(" [niveau") }
            assertEquals(espece.nom, attendus, obtenus)
        }
    }

    @Test
    fun `chaque don reste une seule capacite`() {
        // Même découpage que SrdRepository.loadDons : une entrée par "### Nom".
        val entrees = lire("dons_srd521.md").split(Regex("""(?m)^### """)).drop(1).map { bloc ->
            bloc.substringBefore("\n").trim() to bloc.substringAfter("\n")
        }
        val dons = DonParser.depuisEntrees(entrees)
        assertTrue(dons.isNotEmpty())
        val texte = dons.joinToString("\n\n") {
            entreeFicheAvecChoix(titre = "${it.nom} (don)", retenus = emptyList(), pourquoi = "", description = it.description)
        }
        assertEquals(dons.map { "${it.nom} (don)" }, parserCapacitesFiche(texte).map { it.titre })
    }

    @Test
    fun `les valeurs choisies sont extraites du titre`() {
        val capacite = parserCapacitesFiche(
            "Initié à la magie (don d'historique Sage) — Magicien, Intelligence : Sorts mineurs : Magicien\nPourquoi ce choix : Sage.\n\n### Deux sorts mineurs\n*Passive*"
        ).single()
        assertEquals(listOf("Magicien", "Intelligence"), capacite.choix)
        assertTrue(capacite.description.contains("### Deux sorts mineurs"))
    }
}
