package com.jc2.jdrcompagnon.feature_table_aleatoire

import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.TirageTableUseCase
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
