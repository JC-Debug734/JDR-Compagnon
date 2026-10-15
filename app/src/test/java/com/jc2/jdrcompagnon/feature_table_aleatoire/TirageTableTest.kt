package com.jc2.jdrcompagnon.feature_table_aleatoire

import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatNom
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.TirageTableUseCase
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TirageTableTest {

    private val tirage = TirageTableUseCase()

    private fun entree(type: TypeEvenement, titre: String): EntreeEvenement {
        val evenement = Evenement(worldId = "monde", type = type, titre = titre)
        return EntreeEvenement(evenementId = evenement.id, evenement = evenement)
    }

    @Test
    fun `le filtre par type ne tire que des evenements de ce type`() {
        val table = TableAleatoire(
            nom = "Route",
            entreesEvenements = listOf(
                entree(TypeEvenement.RENCONTRE, "Loups"),
                entree(TypeEvenement.PERIL, "Gué en crue"),
                entree(TypeEvenement.REPIT, "Source claire"),
            )
        )
        repeat(20) { graine ->
            val (_, resultat) = tirage.tirerEvenement(table, TypeEvenement.PERIL, minutesLecture = null, random = Random(graine))
            assertEquals("Gué en crue", resultat?.titre)
        }
    }

    @Test
    fun `une entree dont l'evenement a ete supprime n'est jamais tiree`() {
        val orpheline = EntreeEvenement(evenementId = "supprime", evenement = null)
        val table = TableAleatoire(nom = "Route", entreesEvenements = listOf(orpheline, entree(TypeEvenement.RENCONTRE, "Loups")))
        repeat(20) { graine ->
            val (_, resultat) = tirage.tirerEvenement(table, null, minutesLecture = 120, random = Random(graine))
            assertEquals("Loups", resultat?.titre)
        }
    }

    @Test
    fun `sans evenement disponible le declenchement n'est pas acquitte`() {
        val table = TableAleatoire(nom = "Vide", entreesEvenements = listOf(EntreeEvenement(evenementId = "supprime")))
        val (tableApres, resultat) = tirage.tirerEvenement(table, null, minutesLecture = 120)
        assertNull(resultat)
        assertNull(tableApres.derniereDeclenchementMinutes)
    }

    @Test
    fun `une table de noms donne un seul resultat dont les champs peuvent etre vides`() {
        val table = TableAleatoire(
            nom = "Noms", type = TypeTable.NOMS,
            prenoms = listOf("Thorin"), noms = listOf("Forgefer"), pseudos = listOf("le Têtu"),
            chanceVideNom = 50, chanceVidePseudo = 50
        )
        val resultats = (0 until 200).mapNotNull { tirage.tirerNom(table, Random(it)) }
        assertEquals(200, resultats.size)
        assertTrue(resultats.all { it.prenom == "Thorin" })
        assertTrue(resultats.any { it.nom.isEmpty() } && resultats.any { it.nom == "Forgefer" })
        assertTrue(resultats.any { it.pseudo.isEmpty() } && resultats.any { it.pseudo == "le Têtu" })
        assertEquals("Thorin Forgefer « le Têtu »", ResultatNom("Thorin", "Forgefer", "le Têtu").texte)
        assertEquals("« le Têtu »", ResultatNom("", "", "le Têtu").texte)
    }

    @Test
    fun `un tirage de noms n'est jamais entierement vide`() {
        val table = TableAleatoire(
            nom = "Noms", type = TypeTable.NOMS, prenoms = listOf("Aelar"), pseudos = listOf("l'Archer"),
            chanceVidePrenom = 100, chanceVideNom = 100, chanceVidePseudo = 100
        )
        repeat(20) { assertFalse(tirage.tirerNom(table, Random(it))!!.estVide) }
        assertNull(tirage.tirerNom(TableAleatoire(nom = "Vide", type = TypeTable.NOMS)))
    }
}
