package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.RessourcesClasse
import com.jc2.jdrcompagnon.feature_combat.domain.model.Roublard
import com.jc2.jdrcompagnon.feature_combat.domain.model.Sauvegardes
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.screens.joueur.calculerEmplacementsDeSort
import com.jc2.jdrcompagnon.ui.screens.joueur.calculerPistesIncantation
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Harmonisation
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EffetBalise
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EmplacementsDeSortParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class RoublardTest {

    private val classesMd = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))
    private val classes = ClasseParser.parse(classesMd)
    private val tableEmplacements = EmplacementsDeSortParser.parse(classesMd)

    private fun roublard(niveau: Int, sousClasse: String = "", classe: String = "Roublard") = Character(
        name = "Test", type = "PJ", characterClass = classe, subclass = sousClasse, level = niveau,
        strength = 10, dexterity = 18, constitution = 12, intelligence = 14, wisdom = 12, charisma = 10,
        proficiencyBonus = 2 + (niveau - 1) / 4, savingThrowProficiencies = listOf("Dextérité", "Intelligence"),
    )

    @Test
    fun `attaque sournoise et frappe malicieuse selon le niveau`() {
        assertEquals(listOf(1, 2, 3, 10), listOf(1, 3, 5, 19).map { Roublard.desAttaqueSournoise(roublard(it)) })
        assertTrue(Roublard.optionsFrappeMalicieuse(roublard(4)).isEmpty())
        assertEquals(listOf("Poison", "Croc-en-jambe", "Repli"), Roublard.optionsFrappeMalicieuse(roublard(5)).map { it.nom })
        assertEquals(listOf(1, 1, 2), listOf(5, 10, 11).map { Roublard.effetsMax(roublard(it)) })
        val vicieux = Roublard.optionsFrappeMalicieuse(roublard(14)).associate { it.nom to it.cout }
        assertEquals(mapOf("Hébéter" to 2, "Aveugler" to 3, "Assommer" to 6), vicieux.filterKeys { it in setOf("Hébéter", "Aveugler", "Assommer") })
        assertEquals(8 + 4 + 3, Roublard.dd(roublard(5)))
        // Voleur niv. 9 : Attaque furtive ; Assassin niv. 13 : Armes envenimées sur Poison.
        assertTrue(Roublard.optionsFrappeMalicieuse(roublard(9, "Voleur")).any { it.nom == "Attaque furtive" })
        assertTrue(Roublard.optionsFrappeMalicieuse(roublard(13, "Assassin")).first { it.nom == "Poison" }.effet.contains("2d6"))
        // Ancienne classe « Voleur » reconnue comme Roublard.
        assertEquals(3, Roublard.desAttaqueSournoise(roublard(5, classe = "Voleur")))
    }

    @Test
    fun `esquive totale, esprit fuyant, insaisissable`() {
        assertFalse(Roublard.esquiveTotale(roublard(6)))
        assertTrue(Roublard.esquiveTotale(roublard(7)))
        assertTrue(Roublard.esquiveTotale(roublard(7, classe = "Moine")))
        val sans = Sauvegardes.depuisPersonnage(roublard(14))
        val avec = Sauvegardes.depuisPersonnage(roublard(15))
        assertEquals(1 + 3, sans["Sagesse"]!! + 3) // Sag +1, pas de maîtrise au niveau 14
        assertEquals(1 + 5, avec["Sagesse"]) // maîtrise +5 au niveau 15
        assertTrue(Sauvegardes.maitrisee(roublard(15), "Charisme"))
        assertTrue(Roublard.insaisissable(roublard(18)))
    }

    @Test
    fun `ame aceree, lames psychiques et energie psionique`() {
        val ame = roublard(9, "Âme acérée")
        val lames = ArsenalPersonnage.armesEnMain(ame, emptyList()).filter { it.nom.startsWith(Roublard.LAME_PSYCHIQUE) }
        assertEquals(listOf("1d6 + 4", "1d4 + 4"), lames.map { it.formuleDegats })
        assertEquals("psychiques", lames.first().typeDegats)
        assertEquals("Ouverture", lames.first().botte)
        assertEquals("Main secondaire", lames[1].main) // seconde lame en action Bonus
        // Main secondaire occupée : pas de seconde lame.
        val avecDague = ame.copy(equippedSlots = mapOf(EquipmentSlot.OFF_HAND to "Bouclier"))
        assertEquals(1, Roublard.lamesPsychiques(avecDague).size)
        val energie = RessourcesClasse.ressource(ame, Roublard.ENERGIE_PSIONIQUE)!!
        assertEquals(8, energie.max)
        assertEquals("d8", energie.de)
        val apres = RessourcesClasse.apresReposCourt(ame.copy(compteursUtilises = mapOf(Roublard.ENERGIE_PSIONIQUE to 3, Roublard.MURMURES_PSYCHIQUES to 1)))
        assertEquals(mapOf(Roublard.ENERGIE_PSIONIQUE to 2, Roublard.MURMURES_PSYCHIQUES to 1), apres)
    }

    @Test
    fun `arnaqueur arcanique, incantation de lanceur tiers`() {
        assertTrue(calculerPistesIncantation(roublard(3), classes, tableEmplacements).isEmpty())
        val piste = calculerPistesIncantation(roublard(3, "Arnaqueur arcanique"), classes, tableEmplacements).single()
        assertEquals("Intelligence", piste.ability)
        assertEquals(3, piste.sortsMineursMax) // main du mage + 2
        assertEquals(4, calculerPistesIncantation(roublard(10, "Arnaqueur arcanique"), classes, tableEmplacements).single().sortsMineursMax)
        assertEquals(mapOf(1 to 4, 2 to 2), calculerEmplacementsDeSort(roublard(7, "Arnaqueur arcanique"), classes, tableEmplacements).classiques)
    }

    @Test
    fun `voleur, quatre objets harmonises au niveau 13, coup de chance au niveau 20`() {
        assertEquals(3, Harmonisation.maximum(roublard(12, "Voleur")))
        assertEquals(4, Harmonisation.maximum(roublard(13, "Voleur")))
        assertEquals(1, RessourcesClasse.ressource(roublard(20), Roublard.COUP_DE_CHANCE)!!.max)
    }

    @Test
    fun `expertise au niveau 1 proposee a la creation`() {
        val r = classes.first { it.nom == "Roublard" }
        val balise = r.balisesTraitsDeBase.single { it["effet"] == EffetBalise.EXPERTISE }
        assertEquals("2", balise["choix"])
    }
}
