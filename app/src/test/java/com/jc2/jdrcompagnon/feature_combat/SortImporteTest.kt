package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeZone
import com.jc2.jdrcompagnon.feature_combat.domain.model.JetSort
import com.jc2.jdrcompagnon.feature_combat.domain.model.Sauvegardes
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSrd
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.DureeEffet
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sort importé (sorts_complement_2024.md) : reconnu à l'import, puis lu en combat — jet de
 * sauvegarde, dégâts, moitié si réussi, zone et concentration.
 */
class SortImporteTest {

    private val fichier = """
        # Sorts complémentaires 2024

        ## Sorts

        ### Présence royale de Yolande
        <!-- type: sort -->
        École: Enchantement
        Niveau: 5e niveau
        Classes: Barde, Magicien
        Temps d'incantation: action
        Portée: personnelle
        Composantes: V, S, M (un diadème miniature)
        Durée: Concentration, jusqu’à 1 minute

        Une aura de majesté surnaturelle vous entoure : une Émanation de 3 m centrée sur vous, pour toute la durée du sort. Quand l'Émanation arrive dans l'espace d'une créature que vous voyez, ou quand une créature que vous voyez entre dans l'Émanation ou y termine son tour, vous pouvez lui imposer un jet de sauvegarde de Sagesse. En cas d'échec, elle subit 4d6 dégâts psychiques, tombe À terre et vous pouvez la repousser jusqu'à 3 m de vous. En cas de réussite, elle subit seulement la moitié de ces dégâts. Une même créature ne fait pas ce jet de sauvegarde plus d'une fois par tour.
    """.trimIndent()

    private val importe = CustomContentParser.parse(fichier)
    private val entree = importe.spells.single()
    private val sort = SortSrd(entree.name, ArsenalPersonnage.niveauDepuisLibelle(entree.niveauSort), entree.rawMarkdown, ecole = entree.category)

    private fun perso(classe: String, sousClasse: String = "") = Character(
        name = "Test", type = "PJ", characterClass = classe, subclass = sousClasse, level = 17,
        strength = 10, dexterity = 10, intelligence = 10, wisdom = 10, charisma = 18,
        proficiencyBonus = 6, spells = listOf(sort.nom),
    )

    @Test
    fun `le sort est reconnu a l'import`() {
        assertEquals(1, importe.summary.spells)
        assertTrue(importe.unrecognized.isEmpty())
        assertEquals("Présence royale de Yolande", entree.name)
        assertEquals(5, sort.niveau)
        assertEquals("Enchantement", sort.ecole)
    }

    @Test
    fun `en combat, JS de Sagesse, 4d6 psychiques, moitie si reussi, en zone`() {
        val pret = ArsenalPersonnage.sortsPrets(perso("Barde"), listOf(sort)).single()
        assertEquals(JetSort.SAUVEGARDE, pret.jet)
        assertEquals("Sagesse", pret.sauvegarde)
        assertEquals(8 + 6 + 4, pret.dd) // Barde : Charisme 18 (+4), maîtrise +6
        assertEquals("4d6", pret.formuleDegats)
        assertEquals("psychiques", pret.typeDegats)
        assertTrue(pret.zone)
        assertEquals(FormeZone.EMANATION, pret.zoneEffet?.forme)
        assertEquals(3.0, pret.zoneEffet!!.metres, 0.01)
        // Le résumé envoyé au MJ déclenche le JS automatique avec moitié des dégâts en cas de réussite.
        val js = Sauvegardes.duResume(pret.resume)
        assertNotNull(js)
        assertEquals("Sagesse", js!!.caracteristique)
        assertEquals(18, js.dd)
        assertTrue(js.moitieSiReussi)
    }

    @Test
    fun `sort de concentration d'une minute`() {
        val duree = DureeEffet.depuisSort(sort.fiche())
        assertNotNull(duree)
        assertEquals(60L, duree!!.secondes)
        assertTrue(duree.concentration)
    }

    @Test
    fun `toujours prepare pour un paladin du serment de gloire`() {
        val paladin = perso("Paladin", "Serment de Gloire").copy(spells = emptyList(), preparedSpells = emptyList())
        val pret = ArsenalPersonnage.sortsPrets(paladin, listOf(sort), sortsDeSousClasse = listOf("Présence royale de Yolande")).single()
        assertEquals(8 + 6 + 4, pret.dd) // Paladin : Charisme
    }

    private fun SortSrd.fiche() = rawMarkdown
}
