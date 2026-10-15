package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.Guerrier
import com.jc2.jdrcompagnon.feature_combat.domain.model.Recuperation
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.screens.joueur.calculerEmplacementsDeSort
import com.jc2.jdrcompagnon.ui.screens.joueur.calculerPistesIncantation
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EmplacementsDeSortParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.listeSortsDeClasse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class GuerrierTest {

    private val classesMd = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))
    private val classes = ClasseParser.parse(classesMd)
    private val tableEmplacements = EmplacementsDeSortParser.parse(classesMd)
    private val epee = ArmeSrd("Épée longue", "1d8 tranchants", "Polyvalente (1d10)", false, "Guerre", "Sape")

    private fun guerrier(niveau: Int, sousClasse: String = "", aptitudes: String = "") = Character(
        name = "Test", type = "PJ", characterClass = "Guerrier", subclass = sousClasse, level = niveau,
        strength = 16, dexterity = 14, constitution = 14, intelligence = 16, wisdom = 10, charisma = 8,
        proficiencyBonus = 2 + (niveau - 1) / 4, classFeatures = aptitudes,
        weaponArmorTraining = "Armes courantes, armes de guerre",
        equippedSlots = mapOf(EquipmentSlot.MAIN_HAND to "Épée longue"),
    )

    @Test
    fun `style de combat au choix au niveau 1, et supplementaire du Champion au niveau 7`() {
        val g = classes.first { it.nom == "Guerrier" }
        val style = g.aptitudes.first { it.nom == "Style de combat" }
        assertEquals("choix-effet", style.type)
        assertEquals(
            listOf("Archerie", "Armes à deux mains", "Combat à deux armes", "Défense", "Duel", "Armes de lancer", "Combat à mains nues", "Combat en aveugle", "Interception", "Protection"),
            style.options.map { it.nom }
        )
        assertTrue(g.aptitudesNiveau1.any { it.nom == "Style de combat" && it.type == "choix-effet" })
        val champion = g.sousClasses.first { it.nom == "Champion" }
        assertEquals("choix-effet", champion.aptitudes.first { it.nom == "Style de combat supplémentaire" }.type)
    }

    @Test
    fun `style choisi reconnu sur la fiche, Defense +1 CA en armure`() {
        val defense = guerrier(1, aptitudes = "Style de combat (Guerrier) — Défense : +1 à la CA en portant une armure.")
        assertTrue(Guerrier.aStyle(defense, "Défense"))
        assertFalse(Guerrier.aStyle(defense, "Duel"))
        val enArmure = defense.copy(equippedSlots = mapOf(EquipmentSlot.TORSO to "Cotte de mailles"))
        assertEquals(17, ArmorRules.computeAc(enArmure).total) // 16 + Défense
        assertEquals(12, ArmorRules.computeAc(defense).total) // sans armure : 10 + Dex, pas de Défense
        // Un « Duelliste défensif » n'est pas le style Duel.
        assertFalse(Guerrier.aStyle(guerrier(1, aptitudes = "Duelliste défensif (Don) : …"), "Duel"))
        val duel = guerrier(1, aptitudes = "Style de combat (Guerrier) — Duel : +2 aux dégâts.")
        assertEquals("1d8 + 5", ArsenalPersonnage.armesEnMain(duel, listOf(epee)).single().formuleDegats) // For +3, Duel +2
    }

    @Test
    fun `modificateur de caracteristique arrondi a l'inferieur`() {
        assertEquals(-1, ArmorRules.abilityModifierPublic(9))
        assertEquals(-2, ArmorRules.abilityModifierPublic(7))
        assertEquals(0, ArmorRules.abilityModifierPublic(10))
        assertEquals(2, ArmorRules.abilityModifierPublic(15))
    }

    @Test
    fun `ressources du guerrier selon le niveau et leur recuperation`() {
        fun max(n: Int, cle: String) = Guerrier.ressource(guerrier(n), cle)?.max ?: 0
        assertEquals(listOf(2, 3, 4), listOf(1, 4, 10).map { max(it, Guerrier.SECOND_SOUFFLE) })
        assertEquals(listOf(0, 1, 2), listOf(1, 2, 17).map { max(it, Guerrier.FOUGUE) })
        assertEquals(listOf(0, 1, 2, 3), listOf(8, 9, 13, 17).map { max(it, Guerrier.INFLEXIBLE) })
        // Repos court : Second souffle −1, Fougue remise à zéro, Inflexible inchangé.
        val g = guerrier(17).copy(compteursUtilises = mapOf(Guerrier.SECOND_SOUFFLE to 3, Guerrier.FOUGUE to 2, Guerrier.INFLEXIBLE to 1))
        assertEquals(mapOf(Guerrier.SECOND_SOUFFLE to 2, Guerrier.INFLEXIBLE to 1), Guerrier.apresReposCourt(g))
        assertFalse(ArsenalPersonnage.aFougue(g))
        assertTrue(ArsenalPersonnage.aFougue(guerrier(2)))
    }

    @Test
    fun `champion, zone de critique et survivant, sans faux positif`() {
        assertEquals(20, Guerrier.seuilCritique(guerrier(3)))
        assertEquals(19, Guerrier.seuilCritique(guerrier(3, "Champion")))
        assertEquals(18, Guerrier.seuilCritique(guerrier(15, "Champion")))
        assertEquals(19, ArsenalPersonnage.armesEnMain(guerrier(3, "Champion"), listOf(epee)).single().seuilCritique)
        assertEquals(7, Guerrier.pvResilienceHeroique(guerrier(18, "Champion"))) // 5 + Con +2
        // Le mot « champion » dans les aptitudes (Champion des Anciens d'un Paladin) ne suffit plus.
        assertEquals(20, Guerrier.seuilCritique(guerrier(5, aptitudes = "Champion des Anciens")))
    }

    @Test
    fun `maitre de guerre, des de superiorite et manoeuvres choisies`() {
        val m = guerrier(10, "Maître de guerre",
            "Manœuvre (Niveau 3) — Riposte : Réaction…\n\nManœuvre supplémentaire (Niveau 3) — Parade : Réaction…\n\nManœuvre de départ (Niveau 3) — Feinte : action Bonus…")
        val des = Guerrier.ressource(m, Guerrier.SUPERIORITE)!!
        assertEquals(5, des.max)
        assertEquals("d10", des.de)
        assertEquals(Recuperation.REPOS_COURT, des.recuperation)
        assertEquals(listOf("Riposte", "Parade", "Feinte"), Guerrier.manoeuvresConnues(m))
        assertEquals(8 + 4 + 3, Guerrier.ddManoeuvres(m))
    }

    @Test
    fun `soldat psi et chevalier runique`() {
        val psi = Guerrier.ressource(guerrier(9, "Soldat psi"), Guerrier.ENERGIE_PSIONIQUE)!!
        assertEquals(8, psi.max)
        assertEquals("d8", psi.de)
        assertEquals(Recuperation.UNE_AU_REPOS_COURT, psi.recuperation)
        val runique = guerrier(15, "Chevalier runique",
            "Sculpteur de runes (Niveau 3) — Rune du feu : …\n\nRune supplémentaire (Niveau 3) — Rune de givre : …")
        assertEquals(listOf("Rune du feu", "Rune de givre"), Guerrier.runesConnues(runique))
        val runes = Guerrier.ressources(runique).filter { it.cle.startsWith(Guerrier.PREFIXE_RUNE) }
        assertEquals(listOf(2, 2), runes.map { it.max }) // Maître des runes
        assertEquals(5, Guerrier.ressource(runique, Guerrier.PUISSANCE_GEANTS)!!.max) // bonus de maîtrise
        assertEquals("d8", Guerrier.dePuissanceGeants(runique))
    }

    @Test
    fun `chevalier occulte, incantation de lanceur tiers`() {
        val sansSousClasse = guerrier(3)
        assertTrue(calculerPistesIncantation(sansSousClasse, classes, tableEmplacements).isEmpty())
        val ek3 = guerrier(3, "Chevalier occulte")
        val piste = calculerPistesIncantation(ek3, classes, tableEmplacements).single()
        assertEquals("Intelligence", piste.ability)
        assertEquals(1, piste.maxNiveauSort)
        assertEquals(2, piste.sortsMineursMax)
        assertEquals(3, piste.sortsPreparesMax)
        assertEquals(mapOf(1 to 2), calculerEmplacementsDeSort(ek3, classes, tableEmplacements).classiques)
        assertEquals(mapOf(1 to 4, 2 to 2), calculerEmplacementsDeSort(guerrier(7, "Chevalier occulte"), classes, tableEmplacements).classiques)
        assertEquals(mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 1), calculerEmplacementsDeSort(guerrier(19, "Chevalier occulte"), classes, tableEmplacements).classiques)
        assertEquals("Magicien", listeSortsDeClasse("Guerrier"))
    }
}
