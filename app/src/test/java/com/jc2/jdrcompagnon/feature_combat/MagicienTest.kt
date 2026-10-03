package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.EffetCombatClasse
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSrd
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.NiveauClasse
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Aptitudes du Magicien et de l'Évocateur (balises de classes_srd521.md, effets en combat). */
class MagicienTest {

    private val classes = ClasseParser.parse(
        Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))
    )

    private fun magicien(niveau: Int, sousClasse: String = "", vararg sorts: String) = Character(
        name = "Elminster", type = "PJ", characterClass = "Magicien", subclass = sousClasse, level = niveau,
        intelligence = 18, proficiencyBonus = 2 + (niveau - 1) / 4, spells = sorts.toList(),
    )

    private val trait = SortSrd(
        "Trait de feu", 0,
        "**Temps d'incantation :** Action\n\nFaites une attaque de sort à distance. Touché : 1d10 dégâts de feu.",
        ecole = "Évocation",
    )
    private val bouleDeFeu = SortSrd(
        "Boule de feu", 3,
        "**Temps d'incantation :** Action\n\nSphère de 6 mètres de rayon. Jet de sauvegarde de Dextérité : 8d6 dégâts de feu.",
        ecole = "Évocation",
    )

    @Test
    fun `restauration magique - budget moitie du niveau de Magicien arrondie au superieur`() {
        assertEquals(1, ArsenalPersonnage.budgetRestaurationMagique(magicien(1)))
        assertEquals(3, ArsenalPersonnage.budgetRestaurationMagique(magicien(5)))
        assertEquals(10, ArsenalPersonnage.budgetRestaurationMagique(magicien(20)))
        val guerrierMage = Character(
            name = "G", type = "PJ", characterClass = "Guerrier", level = 6, classesSecondaires = listOf(NiveauClasse("Magicien", 3)),
        )
        assertEquals(2, ArsenalPersonnage.budgetRestaurationMagique(guerrierMage))
        assertEquals(0, ArsenalPersonnage.budgetRestaurationMagique(guerrierMage.copy(classesSecondaires = emptyList())))
    }

    @Test
    fun `rituel - duree et autorisation`() {
        assertTrue(ArsenalPersonnage.estRituel("1 minute ou rituel"))
        assertFalse(ArsenalPersonnage.estRituel("Action"))
        assertEquals(11, ArsenalPersonnage.dureeRituelMinutes("1 minute ou rituel"))
        assertEquals(70, ArsenalPersonnage.dureeRituelMinutes("1 heure ou rituel"))
        assertEquals(10, ArsenalPersonnage.dureeRituelMinutes("Action ou rituel"))
        // Savoir rituel : tout le grimoire, même non préparé.
        val m = magicien(3, "", "Détection de la magie").copy(preparedSpells = emptyList())
        assertTrue(ArsenalPersonnage.rituelAutorise(m, "Détection de la magie"))
        // Un Clerc ne lance en rituel que ses sorts préparés.
        val clerc = m.copy(characterClass = "Clerc")
        assertFalse(ArsenalPersonnage.rituelAutorise(clerc, "Détection de la magie"))
        assertTrue(ArsenalPersonnage.rituelAutorise(clerc.copy(preparedSpells = listOf("Détection de la magie")), "Détection de la magie"))
    }

    @Test
    fun `evocateur - effets de combat lus dans les balises selon le niveau`() {
        assertTrue(ArsenalPersonnage.effetsCombatClasse(magicien(10), classes).isEmpty())
        val niv3 = ArsenalPersonnage.effetsCombatClasse(magicien(3, "Évocateur"), classes).map { it.type }
        assertEquals(listOf(EffetCombatClasse.DEMI_DEGATS_MINEUR), niv3)
        val niv10 = ArsenalPersonnage.effetsCombatClasse(magicien(10, "Évocateur"), classes).map { it.type }.toSet()
        assertEquals(
            setOf(EffetCombatClasse.DEMI_DEGATS_MINEUR, EffetCombatClasse.PROTECTION_ALLIES, EffetCombatClasse.BONUS_DEGATS_CARAC),
            niv10,
        )
    }

    @Test
    fun `evocateur - sort mineur appuye, faconneur de sorts et evocation amelioree`() {
        val c = magicien(10, "Évocateur", "Trait de feu", "Boule de feu")
        val effets = ArsenalPersonnage.effetsCombatClasse(c, classes)
        val (mineur, boule) = ArsenalPersonnage.sortsPrets(c, listOf(trait, bouleDeFeu), effets)
        assertTrue(mineur.demiDegatsSiEchec)
        assertEquals("2d10 + 4", mineur.formuleDegats) // palier niv. 5 + Int +4 (Évocation améliorée)
        assertFalse(boule.demiDegatsSiEchec)
        assertEquals("8d6 + 4", boule.formuleDegats)
        assertTrue(boule.notesClasse.any { it.startsWith("Façonneur de sorts") && it.contains("4 allié") })
        // Sans sous-classe : rien de tout cela.
        val simple = ArsenalPersonnage.sortsPrets(c.copy(subclass = ""), listOf(trait, bouleDeFeu), ArsenalPersonnage.effetsCombatClasse(c.copy(subclass = ""), classes))
        assertEquals("2d10", simple[0].formuleDegats)
        assertTrue(simple.all { it.notesClasse.isEmpty() && !it.demiDegatsSiEchec })
    }

    @Test
    fun `sorts speciaux - toujours prepares et lancement gratuit`() {
        val c = magicien(20, "", "Boule de feu").copy(
            preparedSpells = emptyList(),
            sortsSpeciaux = mapOf("Boule de feu" to ArsenalPersonnage.SORT_PREDILECTION),
        )
        val sort = ArsenalPersonnage.sortsPrets(c, listOf(bouleDeFeu)).single()
        assertEquals(ArsenalPersonnage.SORT_PREDILECTION, sort.lancementGratuit)
        assertTrue(sort.gratuitDisponible)
        val utilise = ArsenalPersonnage.sortsPrets(c.copy(sortsPredilectionUtilises = listOf("Boule de feu")), listOf(bouleDeFeu)).single()
        assertFalse(utilise.gratuitDisponible)
        // Non préparé et sans aptitude : absent des sorts prêts.
        assertTrue(ArsenalPersonnage.sortsPrets(c.copy(sortsSpeciaux = emptyMap()), listOf(bouleDeFeu)).isEmpty())
    }

    @Test
    fun `balises du Magicien - grimoire, sorts speciaux et faveur epique`() {
        val magicien = classes.first { it.nom == "Magicien" }
        assertEquals(6, magicien.grimoireDepart)
        assertNull(classes.first { it.nom == "Clerc" }.grimoireDepart)
        val balises = magicien.aptitudes.flatMap { it.balises }
        assertEquals("1, 2", balises.first { it["sorts-speciaux"] == "a-volonte" }["niveaux"])
        assertEquals("3, 3", balises.first { it["sorts-speciaux"] == "predilection" }["niveaux"])
        assertTrue(classes.all { c -> c.aptitudes.any { a -> a.balises.any { it["don-categorie"] == "Faveur épique" } } })
    }
}
