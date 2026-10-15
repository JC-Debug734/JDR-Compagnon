package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.Occultiste
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSrd
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Aptitudes de l'Occultiste et du Protecteur Fiélon (classes_srd521.md, effets en jeu). */
class OccultisteTest {

    private val classes = ClasseParser.parse(
        Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))
    )
    private val occultisteClasse = classes.first { it.nom == "Occultiste" }

    private fun occultiste(niveau: Int, sousClasse: String = "", charisme: Int = 18, vararg manifestations: String) = Character(
        name = "Morgane", type = "PJ", characterClass = "Occultiste", subclass = sousClasse, level = niveau,
        charisma = charisme, proficiencyBonus = 2 + (niveau - 1) / 4,
        classFeatures = manifestations.joinToString("\n\n") { "Manifestation occulte (Niveau 2) — $it : texte de l'option." },
    )

    @Test
    fun `les manifestations sont proposees aux bons niveaux, avec toutes leurs options`() {
        val manif = occultisteClasse.aptitudes.first { it.id == "manifestation-occulte" }
        assertEquals("choix-effet", manif.type)
        assertEquals(listOf(1, 2, 5, 7, 9, 12, 15, 18), manif.niveaux)
        assertEquals(28, manif.options.size)
        assertTrue(manif.options.any { it.nom == "Pacte de la lame" })
        val supplementaire = occultisteClasse.aptitudes.first { it.id == "manifestation-occulte-supplementaire" }
        assertEquals(listOf(2, 5), supplementaire.niveaux)
        // 10 manifestations au niveau 18, comme la table de progression.
        assertEquals(10, manif.niveaux.size + supplementaire.niveaux.size)
    }

    @Test
    fun `arcanum mystique et communication avec le protecteur sont balises`() {
        val arcanum = occultisteClasse.aptitudes.first { it.id == "arcanum-mystique" }
        val balise = arcanum.balises.first { it.containsKey("sorts-speciaux") }
        assertEquals(ArsenalPersonnage.SORT_ARCANUM, balise["sorts-speciaux"])
        assertEquals("11, 13, 15, 17", balise["un-par-niveau"])
        assertEquals("liste", balise["source"])
        val communication = occultisteClasse.aptitudes.first { it.id == "communication-avec-le-protecteur" }
        assertEquals("Contact avec les plans", communication.balises.first { it.containsKey("sorts-speciaux") }["sort"])
    }

    @Test
    fun `manifestations lues sur la fiche - sorts sans emplacement et lame`() {
        val c = occultiste(12, "", 18, "Armure d’ombres", "Pacte de la lame", "Lame assoiffée", "Lame dévorante", "Décharge déchirante")
            .copy(armeDePacte = "Épée longue (arme de pacte)", armeDePacteInvoquee = true)
        assertEquals(5, Occultiste.manifestations(c).size)
        val sorts = Occultiste.sortsDesManifestations(c)
        assertEquals(ArsenalPersonnage.SORT_A_VOLONTE, sorts["Armure du mage"])
        assertEquals(3, ArsenalPersonnage.nbAttaques(c))
        assertEquals(2, ArsenalPersonnage.nbAttaques(c.copy(level = 11)))
        assertEquals(4, Occultiste.modArmeDePacte(c, "Épée longue (arme de pacte)"))
        assertEquals(null, Occultiste.modArmeDePacte(c, "Dague"))
        // Sans arme de pacte invoquée ou liée : pas d'attaque supplémentaire.
        assertEquals(1, ArsenalPersonnage.nbAttaques(c.copy(armeDePacte = "")))
        // Décharge déchirante : Charisme ajouté aux dégâts de décharge occulte.
        val decharge = SortSrd("Décharge occulte", 0, "**Temps d'incantation :** Action\n\nFaites une attaque de sort. Touché : 1d10 dégâts de force.")
        val pret = ArsenalPersonnage.sortPret(c, decharge, "Occultiste")
        assertTrue(pret.formuleDegats!!.endsWith("+ 4"))
        // Sans manifestation : rien de tout ça.
        val simple = occultiste(12)
        assertTrue(Occultiste.sortsDesManifestations(simple).isEmpty())
        assertEquals(1, ArsenalPersonnage.nbAttaques(simple))
    }

    @Test
    fun `rouerie magique - moitie du maximum, tout au niveau 20`() {
        val c = occultiste(5).copy(pactSlotsUsed = 2)
        assertTrue(Occultiste.rouerieDisponible(c))
        assertEquals(1, Occultiste.apresRouerie(c, 2))
        assertEquals(0, Occultiste.apresRouerie(c.copy(level = 20, pactSlotsUsed = 4), 4))
        assertEquals(2, Occultiste.apresRouerie(c.copy(level = 17, pactSlotsUsed = 4), 4))
        assertFalse(Occultiste.rouerieDisponible(c.copy(rouerieMagiqueUtilisee = true)))
        assertFalse(Occultiste.rouerieDisponible(occultiste(1)))
    }

    @Test
    fun `protecteur fielon - benediction, chance, resistance, traversee`() {
        val f = occultiste(14, "Protecteur Fiélon", charisme = 16)
        assertEquals(17, Occultiste.pvBenediction(f))
        assertEquals(3, Occultiste.chanceRestante(f))
        assertEquals(2, Occultiste.chanceRestante(f.copy(chanceTenebreuxUtilisees = 1)))
        assertTrue(Occultiste.resiste(f.copy(resistanceFielonne = "feu"), "feu"))
        assertFalse(Occultiste.resiste(f.copy(resistanceFielonne = "feu"), "froid"))
        assertTrue(Occultiste.traverseeDisponible(f))
        assertEquals(16, Occultiste.ddSorts(f))
        // Sans le Protecteur Fiélon, aucune de ces aptitudes.
        assertEquals(0, Occultiste.pvBenediction(occultiste(14)))
        assertEquals(0, Occultiste.chanceRestante(occultiste(5, "Protecteur Fiélon")))
    }
}
