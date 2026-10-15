package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.joueur.ReglesRepos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Cadence des repos (rules.md « Reposez-vous ») : 3 repos courts, 1 repos long par 24 h, 1 PV minimum. */
class ReglesReposTest {

    private fun perso(reposCourts: Int = 0, dernierLong: Long? = null, pv: Int = 10) = Character(
        name = "Lia", type = "PJ", currentHitPoints = pv, maxHitPoints = 10,
        reposCourtsPris = reposCourts, dernierReposLongMinutes = dernierLong,
    )

    @Test
    fun `trois repos courts au maximum`() {
        assertEquals(3, ReglesRepos.reposCourtsRestants(perso()))
        assertNull(ReglesRepos.blocageReposCourt(perso(reposCourts = 2)))
        assertEquals(0, ReglesRepos.reposCourtsRestants(perso(reposCourts = 3)))
        assertNotNull(ReglesRepos.blocageReposCourt(perso(reposCourts = 3)))
    }

    @Test
    fun `un seul repos long par periode de 24 heures`() {
        val debut = 1000L
        assertNull(ReglesRepos.blocageReposLong(perso(), debut))
        // Commencé 16 h après le précédent : encore 8 h à attendre.
        assertEquals(8 * 60L, ReglesRepos.attenteReposLong(perso(dernierLong = debut), debut + 16 * 60))
        assertNotNull(ReglesRepos.blocageReposLong(perso(dernierLong = debut), debut + 16 * 60))
        assertNull(ReglesRepos.blocageReposLong(perso(dernierLong = debut), debut + 24 * 60))
        // Horloge reculée par le MJ sous le dernier repos : permis.
        assertNull(ReglesRepos.blocageReposLong(perso(dernierLong = debut), debut - 60))
    }

    @Test
    fun `repos long impossible a 0 PV`() {
        assertNotNull(ReglesRepos.blocageReposLong(perso(pv = 0), 0L))
    }

    @Test
    fun `durees des repos`() {
        assertEquals(60L, ReglesRepos.DUREE_REPOS_COURT_MINUTES)
        assertEquals(8 * 60L, ReglesRepos.DUREE_REPOS_LONG_MINUTES)
        assertEquals("7 h 05", ReglesRepos.formaterDuree(7 * 60 + 5L))
    }
}
