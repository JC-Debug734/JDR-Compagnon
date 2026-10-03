package com.jc2.jdrcompagnon.feature_evenement

import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.di.TableAleatoireDependencies
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExemplesEvenementsTest {

    private val tous = EvenementDependencies.exemples("monde") + TableAleatoireDependencies.modelesEvenements()

    @Test
    fun `chaque exemple dit ce qui est attendu des joueurs`() {
        val sansObjectif = tous.filter { it.objectif.isBlank() }.map { it.titre }
        assertEquals(emptyList<String>(), sansObjectif)
    }

    @Test
    fun `chaque rencontre d'une table d'exemple a ses profils`() {
        val rencontresSansProfil = TableAleatoireDependencies.modelesEvenements()
            .filter { it.type == TypeEvenement.RENCONTRE && it.profils.isEmpty() }
            .map { it.titre }
        assertEquals(emptyList<String>(), rencontresSansProfil)
    }

    @Test
    fun `les monstres cites existent dans le bestiaire SRD`() {
        val bestiaire = File("src/main/assets/dnd/monster_srd521.md").readLines()
            .filter { it.startsWith("### ") }
            .map { it.removePrefix("### ").trim() }
            .toSet()
        assertTrue(bestiaire.size > 100)
        val inconnus = tous.flatMap { it.profils }
            .filter { it.type == TypeProfil.MONSTRE && it.nom !in bestiaire }
            .map { it.nom }
            .distinct()
        assertEquals(emptyList<String>(), inconnus)
    }

    @Test
    fun `un evenement de table converti sans contenu recoit profils, attendu et choix`() {
        // Cas d'une ancienne table convertie : titre et description seuls.
        val converti = Evenement(
            worldId = "monde", type = TypeEvenement.RENCONTRE,
            titre = "Bandits de grand chemin", description = "Des brigands exigent une bourse en échange du passage."
        )
        val enrichi = EvenementDependencies.enrichirDepuisModele(converti)
        assertEquals(converti.id, enrichi.id)
        assertEquals(listOf("Bandit ×4", "Chef de bande"), enrichi.profils.map { it.libelle })
        assertEquals(
            listOf("Accepter de payer", "Négociation réussie", "Intimidation réussie", "Combat"),
            enrichi.issues.map { it.libelle }
        )
        assertTrue(enrichi.objectif.isNotBlank())
    }

    @Test
    fun `un exemple deja complete recoit l'objectif en liste et l'issue qui mene au combat`() {
        val modele = TableAleatoireDependencies.modelesEvenements().first { it.titre == "Bandits de grand chemin" }
        // Tel qu'enregistré par la version précédente : objectif en phrase, aucun marquage combat.
        val ancien = modele.copy(
            id = "evt-bandits",
            objectif = "Trois choix : payer les 20 po, négocier (Charisme (Persuasion) DD 13) ou intimider " +
                "(Charisme (Intimidation) DD 15). Un refus ou un jet raté mène au combat.",
            issues = modele.issues.map { it.copy(id = "issue-${it.libelle}", declencheCombat = false) }
        )
        val maj = EvenementDependencies.enrichirDepuisModele(ancien)
        assertEquals(4, maj.objectif.lines().size)
        assertEquals(listOf("Combat"), maj.issues.filter { it.declencheCombat }.map { it.libelle })
        // Les ids des issues sont conservés.
        assertEquals(ancien.issues.map { it.id }, maj.issues.map { it.id })
    }

    @Test
    fun `un evenement modifie par le MJ n'est pas touche`() {
        val modifie = Evenement(
            worldId = "monde", type = TypeEvenement.RENCONTRE,
            titre = "Bandits de grand chemin", description = "Ma version.", objectif = "Mon objectif"
        )
        assertEquals(modifie, EvenementDependencies.enrichirDepuisModele(modifie))
    }

    @Test
    fun `l'ancienne patrouille d'orques devient la patrouille de hobgobelins`() {
        val ancienne = Evenement(
            worldId = "monde", type = TypeEvenement.RENCONTRE,
            titre = "Patrouille d'orques", description = "Une patrouille d'orques bien armée surveille son territoire."
        )
        val corrigee = EvenementDependencies.enrichirDepuisModele(ancienne)
        assertEquals("Patrouille de hobgobelins", corrigee.titre)
        assertEquals(listOf("Combattant hobgobelin ×3", "Capitaine hobgobelin"), corrigee.profils.map { it.libelle })
    }

    @Test
    fun `les titres d'exemples sont uniques`() {
        val doublons = tous.groupBy { it.titre }.filterValues { it.size > 1 }.keys
        assertEquals(emptySet<String>(), doublons)
    }
}
