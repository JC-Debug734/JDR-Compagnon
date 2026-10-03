package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.JetSort
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSrd
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArsenalPersonnageTest {

    private val armes = listOf(
        ArmeSrd("Épée longue", "1d8 tranchants", "Polyvalente (1d10)", false, "Guerre", "Sape"),
        ArmeSrd("Rapière", "1d8 perforants", "Finesse", false, "Guerre", "Vexation"),
        ArmeSrd("Dague", "1d4 perforants", "Finesse, Lancer (6/18), Légère", false, "Courante", "Coup double"),
        ArmeSrd("Arc long", "1d8 perforants", "Deux mains, Lourde, Munitions (45/180 ; flèches)", true, "Guerre", "Ralentissement"),
    )

    private fun perso(
        classe: String = "Guerrier",
        niveau: Int = 5,
        mains: Map<EquipmentSlot, String> = emptyMap(),
        aptitudes: String = "",
    ) = Character(
        name = "Test", type = "PJ", characterClass = classe, level = niveau,
        strength = 16, dexterity = 14, intelligence = 10, wisdom = 16, charisma = 8,
        proficiencyBonus = 3, equippedSlots = mains, classFeatures = aptitudes,
        weaponArmorTraining = "Armes courantes, armes de guerre",
    )

    @Test
    fun `arme en main principale avec bonus calcules`() {
        val a = ArsenalPersonnage.armesEnMain(perso(mains = mapOf(EquipmentSlot.MAIN_HAND to "Épée longue")), armes).single()
        assertEquals(6, a.bonusToucher) // For +3, maîtrise +3
        assertEquals("1d8 + 3", a.formuleDegats)
        assertEquals("Sape", a.botte)
    }

    @Test
    fun `deux armes, main secondaire sans modificateur aux degats`() {
        val c = perso(classe = "Roublard", mains = mapOf(EquipmentSlot.MAIN_HAND to "Rapière", EquipmentSlot.OFF_HAND to "Dague"))
        val (rapiere, dague) = ArsenalPersonnage.armesEnMain(c, armes)
        assertEquals(6, rapiere.bonusToucher) // Finesse : For +3 > Dex +2
        assertEquals("1d4", dague.formuleDegats)
        assertTrue(dague.notes.any { it.contains("action bonus") })
        val style = ArsenalPersonnage.armesEnMain(c.copy(classFeatures = "Style de combat : Combat à deux armes"), armes)
        assertEquals("1d4 + 3", style[1].formuleDegats)
    }

    @Test
    fun `arme a munitions compte ses fleches et arme de dos listee`() {
        val c = perso(mains = mapOf(EquipmentSlot.MAIN_HAND to "Arc long", EquipmentSlot.BACK_WEAPON_1 to "Dague"))
            .copy(quiverContents = List(3) { "Flèches" }, backpackItems = listOf("Flèches", "Fléchette", "Étui pour carreaux"))
        val (arc, dague) = ArsenalPersonnage.armesEnMain(c, armes)
        assertEquals("flèches", arc.munition)
        assertEquals(4, arc.munitionsRestantes)
        assertEquals("Dans le dos", dague.main)
        assertTrue(dague.lancer)
        assertEquals(EquipmentSlot.BACK_WEAPON_1, dague.slot)
        val vide = ArsenalPersonnage.armesEnMain(c.copy(quiverContents = emptyList(), backpackItems = emptyList()), armes).first()
        assertTrue(vide.sansMunition)
    }

    @Test
    fun `parchemin et focaliseur arcanique ne sont pas des arcs`() {
        val regles = com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
        assertNull(regles.slotForItem("Parchemin (feuille)"))
        assertTrue(!regles.twoHanded("Focaliseur arcanique (baguette)"))
        assertEquals(EquipmentSlot.MAIN_HAND, regles.slotForItem("Arc long"))
        assertTrue(regles.twoHanded("Arc court"))
        assertEquals(EquipmentSlot.MAIN_HAND, regles.slotForItem("Lance"))
    }

    @Test
    fun `armes limitees aux mains et au dos, tenues aux vetements`() {
        val regles = com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
        assertTrue(regles.itemAllowedInSlot("Dague", EquipmentSlot.MAIN_HAND))
        assertTrue(regles.itemAllowedInSlot("Dague", EquipmentSlot.BACK_WEAPON_2))
        assertTrue(!regles.itemAllowedInSlot("Dague", EquipmentSlot.BELT_POUCH_1))
        assertTrue(!regles.itemAllowedInSlot("Dague", EquipmentSlot.ACCESSORY))
        assertTrue(!regles.itemAllowedInSlot("Potion de guérison", EquipmentSlot.MAIN_HAND))
        assertTrue(regles.itemAllowedInSlot("Bouclier", EquipmentSlot.OFF_HAND))
        assertTrue(!regles.itemAllowedInSlot("Robe", EquipmentSlot.TORSO))
        assertTrue(regles.itemAllowedInSlot("Robe", EquipmentSlot.CLOTHING))
        assertTrue(regles.itemAllowedInSlot("Potion de guérison", EquipmentSlot.BELT_POUCH_1))
    }

    @Test
    fun `nom au pluriel ramene au nom de la bibliotheque`() {
        val regles = com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
        val noms = listOf("Dague", "Javeline", "Flèches", "Huile (flasque)")
        assertEquals("Dague", regles.nomCanonique("Dagues", noms))
        assertEquals("Javeline", regles.nomCanonique("javelines", noms))
        assertEquals("Flèches", regles.nomCanonique("Flèches", noms))
        assertEquals("Objet inconnu", regles.nomCanonique("Objet inconnu", noms))
    }

    @Test
    fun `focaliseur arcanique est une propriete, le grimoire l'a pour un magicien`() {
        val f = com.jc2.jdrcompagnon.ui.screens.joueur.character.Focaliseurs
        assertEquals("arcanique" to "bâton de combat", f.separer("Focaliseur arcanique (bâton de combat)"))
        val magicien = perso(classe = "Magicien").copy(focaliseurs = mapOf("Bâton de combat" to "arcanique"))
        assertEquals("arcanique", f.type(magicien, "Bâton de combat"))
        assertEquals("arcanique", f.type(magicien, "Grimoire"))
        assertNull(f.type(perso(classe = "Guerrier"), "Grimoire"))
        assertNull(f.type(perso(classe = "Guerrier"), "Bâton de combat"))
        assertEquals(EquipmentSlot.MAIN_HAND, com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules.slotForItem("Bâton de combat"))
    }

    @Test
    fun `noms de sorts compares sans tenir compte des apostrophes`() {
        assertTrue(ArsenalPersonnage.memeSort("Sphère d’eau ", "sphère d'eau"))
        assertTrue(ArsenalPersonnage.sortConnu(perso().copy(spells = listOf("Main du mage")), "main du  mage"))
    }

    @Test
    fun `style archerie et mains nues`() {
        val arc = ArsenalPersonnage.armesEnMain(perso(mains = mapOf(EquipmentSlot.MAIN_HAND to "Arc long"), aptitudes = "Archerie"), armes).single()
        assertEquals(7, arc.bonusToucher) // Dex +2, maîtrise +3, Archerie +2
        assertEquals("1d8 + 2", arc.formuleDegats)
        assertEquals("Mains nues", ArsenalPersonnage.armesEnMain(perso(), armes).single().nom)
    }

    @Test
    fun `capacites d'attaque selon la classe`() {
        assertTrue(ArsenalPersonnage.capacitesAttaque(perso()).any { it.nom == "Attaque supplémentaire" && it.detail.startsWith("2") })
        val roublard = ArsenalPersonnage.capacitesAttaque(perso(classe = "Roublard", niveau = 5))
        assertTrue(roublard.any { it.nom == "Attaque sournoise" && it.detail.startsWith("+3d6") })
    }

    private val sorts = listOf(
        SortSrd("Flamme sacrée", 0, "**Portée :** 18 m\n\nUne flamme descend sur une créature. Elle doit réussir un jet de sauvegarde de Dextérité, sinon elle subit 1d8 dégâts radiants."),
        SortSrd("Soins", 1, "**Temps d'incantation :** action\n\nLa créature récupère un nombre de points de vie égal à 2d8 + votre modificateur de caractéristique d’incantation."),
        SortSrd("Arme spirituelle", 2, "**Temps d'incantation :** action bonus\n\nEffectuez une attaque de sort au corps à corps. La cible subit 1d8 + votre modificateur de caractéristique d’incantation dégâts de force."),
    )

    @Test
    fun `sorts prets, prepares et calculs`() {
        val clerc = perso(classe = "Clerc", niveau = 5).copy(spells = listOf("Flamme sacrée", "Soins", "Arme spirituelle"))
        // Jamais préparé : tout est prêt.
        assertEquals(3, ArsenalPersonnage.sortsPrets(clerc, sorts).size)
        val prepare = ArsenalPersonnage.sortsPrets(clerc.copy(preparedSpells = listOf("Soins")), sorts)
        assertEquals(listOf("Flamme sacrée", "Soins"), prepare.map { it.nom })
        val flamme = prepare.first()
        assertEquals(JetSort.SAUVEGARDE, flamme.jet)
        assertEquals(14, flamme.dd) // 8 + Sag +3 + maîtrise +3
        assertEquals("2d8", flamme.formuleDegats) // sort mineur au niveau 5
        assertEquals("2d8 + 3", prepare[1].soin)
        val arme = ArsenalPersonnage.sortsPrets(clerc, sorts).first { it.nom == "Arme spirituelle" }
        assertEquals(JetSort.ATTAQUE, arme.jet)
        assertEquals(6, arme.bonusAttaque)
        assertEquals("1d8 + 3", arme.formuleDegats)
        assertEquals("force", arme.typeDegats)
    }

    @Test
    fun `une classe qui ne prepare pas garde tous ses sorts connus`() {
        val barde = perso(classe = "Barde").copy(spells = listOf("Soins"), preparedSpells = emptyList())
        assertEquals(listOf("Soins"), ArsenalPersonnage.sortsPrets(barde, sorts).map { it.nom })
        assertNull(ArsenalPersonnage.sortsPrets(barde, sorts).single().formuleDegats)
    }

    @Test
    fun `paladin, bottes par defaut epee longue et javeline`() {
        val paladin = perso(classe = "Paladin", niveau = 1, mains = mapOf(EquipmentSlot.MAIN_HAND to "Épée longue", EquipmentSlot.OFF_HAND to "Dague"))
        assertEquals(listOf("Épée longue", "Javeline"), ArsenalPersonnage.bottesActives(paladin))
        assertEquals(2, ArsenalPersonnage.nombreBottes(paladin))
        val (epee, dague) = ArsenalPersonnage.armesEnMain(paladin, armes)
        assertEquals("Sape", epee.botte)
        assertNull(dague.botte) // botte non choisie
        val choix = paladin.copy(weaponMasteries = listOf("Dague"))
        assertEquals("Coup double", ArsenalPersonnage.armesEnMain(choix, armes)[1].botte)
    }

    @Test
    fun `paladin, puissance curative et chatiment divin toujours prepare`() {
        val sorts = listOf(SortSrd("Châtiment divin", 1, "Classes: Paladin\n\nL'attaque inflige à la cible 2d8 dégâts radiants supplémentaires."))
        val niveau1 = perso(classe = "Paladin", niveau = 1).copy(preparedSpells = emptyList())
        assertEquals(5, ArsenalPersonnage.reservePuissanceCurative(niveau1))
        assertTrue(ArsenalPersonnage.sortsPrets(niveau1, sorts).isEmpty())
        val niveau2 = niveau1.copy(level = 2)
        assertEquals(10, ArsenalPersonnage.reservePuissanceCurative(niveau2))
        assertEquals("2d8", ArsenalPersonnage.sortsPrets(niveau2, sorts).single().formuleDegats)
    }
}
