package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Paladin
import com.jc2.jdrcompagnon.feature_combat.domain.model.Sauvegardes
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypesCreature
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EffetActif
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaladinTest {

    private val epee = ArmeSrd("Épée longue", "1d8 tranchants", "Polyvalente (1d10)", false, "Guerre", "Sape")

    private fun paladin(niveau: Int, sousClasse: String = "Serment de Dévotion", cha: Int = 16) = Character(
        name = "Test", type = "PJ", characterClass = "Paladin", subclass = sousClasse, level = niveau,
        strength = 16, dexterity = 10, intelligence = 10, wisdom = 12, charisma = cha,
        proficiencyBonus = 3, equippedSlots = mapOf(EquipmentSlot.MAIN_HAND to "Épée longue"),
        weaponArmorTraining = "Armes courantes, armes de guerre",
        savingThrowProficiencies = listOf("Sagesse", "Charisme"),
    )

    @Test
    fun `conduit divin, 2 utilisations au niveau 3 et 3 au niveau 11`() {
        assertEquals(0, Paladin.conduitDivinMax(paladin(2)))
        assertEquals(2, Paladin.conduitDivinMax(paladin(3)))
        assertEquals(3, Paladin.conduitDivinMax(paladin(11)))
        assertEquals(1, Paladin.conduitDivinRestants(paladin(3).copy(conduitDivinUtilises = 1)))
    }

    @Test
    fun `aura de protection ajoutee a tous les JS du paladin`() {
        val sans = Sauvegardes.depuisPersonnage(paladin(5))
        val avec = Sauvegardes.depuisPersonnage(paladin(6))
        assertEquals(sans["Force"]!! + 3, avec["Force"]) // Cha 16 → +3
        assertEquals(1, Paladin.bonusAuraProtection(paladin(6, cha = 8))) // minimum +1
        assertEquals(9, Paladin.rayonAura(paladin(18)))
    }

    @Test
    fun `auras de courage et de devotion immunisent`() {
        assertTrue(TypesCreature.defensesPersonnage(paladin(6)).etats.isEmpty())
        assertEquals(setOf(ConditionCombat.CHARME), TypesCreature.defensesPersonnage(paladin(7)).etats)
        assertEquals(setOf(ConditionCombat.CHARME, ConditionCombat.EFFRAYE), TypesCreature.defensesPersonnage(paladin(10)).etats)
        assertEquals(setOf(ConditionCombat.EFFRAYE), TypesCreature.defensesPersonnage(paladin(10, "Serment des Anciens")).etats)
    }

    @Test
    fun `arme sacree ajoute le charisme au toucher de l'arme en main`() {
        val base = ArsenalPersonnage.armesEnMain(paladin(3), listOf(epee)).single().bonusToucher
        val active = paladin(3).copy(effetsActifs = listOf(EffetActif(nom = Paladin.ARME_SACREE, source = "Paladin", debutSecondes = 0, finSecondes = 600)))
        assertEquals(base + 3, ArsenalPersonnage.armesEnMain(active, listOf(epee)).single().bonusToucher)
    }

    @Test
    fun `frappes radiantes et toucher reparateur selon le niveau`() {
        assertFalse(Paladin.aFrappesRadiantes(paladin(10)))
        assertTrue(ArsenalPersonnage.capacitesAttaque(paladin(11)).any { it.nom == "Frappes radiantes" })
        assertEquals(listOf(ConditionCombat.EMPOISONNE), Paladin.etatsRetirables(paladin(13)))
        assertEquals(7, Paladin.etatsRetirables(paladin(14)).size)
    }

    @Test
    fun `fidele destrier toujours prepare et gratuit une fois par repos long`() {
        val sorts = listOf(SortSrd(Paladin.SORT_FIDELE_DESTRIER, 2, "Classes: Paladin"))
        val p = paladin(5).copy(preparedSpells = emptyList())
        val pret = ArsenalPersonnage.sortsPrets(p, sorts).single()
        assertEquals(ArsenalPersonnage.SORT_ARCANUM, pret.lancementGratuit)
        assertTrue(pret.gratuitDisponible)
        val utilise = p.copy(sortsArcanumUtilises = listOf(Paladin.SORT_FIDELE_DESTRIER))
        assertFalse(ArsenalPersonnage.sortsPrets(utilise, sorts).single().gratuitDisponible)
        assertTrue(ArsenalPersonnage.sortsPrets(paladin(4).copy(preparedSpells = emptyList()), sorts).isEmpty())
    }

    @Test
    fun `aptitude de niveau 20 selon le serment`() {
        assertEquals(Paladin.NIMBE_SACRE, Paladin.aptitude20(paladin(20))?.first)
        assertEquals(Paladin.CHAMPION_DES_ANCIENS to 60L, Paladin.aptitude20(paladin(20, "Serment des Anciens")))
        assertEquals(Paladin.LEGENDE_VIVANTE, Paladin.aptitude20(paladin(20, "Serment de Gloire"))?.first)
        assertEquals(Paladin.ANGE_VENGEUR, Paladin.aptitude20(paladin(20, "Serment de Vengeance"))?.first)
        assertEquals(null, Paladin.aptitude20(paladin(19)))
        assertFalse(Paladin.aptitude20Disponible(paladin(20).copy(aptitude20Utilisee = true)))
    }

    @Test
    fun `serment des anciens, aura de garde et sentinelle imperissable`() {
        val anciens = paladin(15, "Serment des Anciens")
        assertTrue(TypesCreature.defensesPersonnage(paladin(6, "Serment des Anciens")).resistances.isEmpty())
        assertEquals(3, TypesCreature.defensesPersonnage(anciens).resistances.size)
        assertTrue(Paladin.sentinelleDisponible(anciens))
        assertFalse(Paladin.sentinelleDisponible(anciens.copy(sentinelleUtilisee = true)))
        assertEquals(45, Paladin.pvSentinelle(anciens))
    }

    @Test
    fun `serment de gloire, aura d'entrain et defense glorieuse`() {
        assertEquals(0, Paladin.bonusVitesse(paladin(6, "Serment de Gloire")))
        assertEquals(3, Paladin.bonusVitesse(paladin(7, "Serment de Gloire")))
        val gloire = paladin(15, "Serment de Gloire")
        assertEquals(3, Paladin.defenseGlorieuseMax(gloire))
        assertEquals(1, Paladin.defenseGlorieuseRestantes(gloire.copy(defenseGlorieuseUtilisees = 2)))
        assertEquals(0, Paladin.defenseGlorieuseMax(paladin(15)))
    }

    @Test
    fun `sort de serment importe avec apostrophe droite retrouve dans le SRD`() {
        val sorts = listOf(SortSrd("Lueur d’espoir", 3, "Classes: Clerc, Paladin"))
        val p = paladin(9).copy(preparedSpells = emptyList())
        assertEquals("Lueur d’espoir", ArsenalPersonnage.sortsPrets(p, sorts, sortsDeSousClasse = listOf("Lueur d'espoir")).single().nom)
    }

    @Test
    fun `sorts de serment toujours prets en combat malgre la preparation`() {
        val sorts = listOf(SortSrd("Bouclier de la foi", 1, "Classes: Clerc, Paladin"))
        val p = paladin(3).copy(spells = listOf("Bouclier de la foi"), preparedSpells = emptyList())
        assertTrue(ArsenalPersonnage.sortsPrets(p, sorts).isEmpty())
        assertEquals(1, ArsenalPersonnage.sortsPrets(p, sorts, sortsDeSousClasse = listOf("Bouclier de la foi")).size)
    }
}
