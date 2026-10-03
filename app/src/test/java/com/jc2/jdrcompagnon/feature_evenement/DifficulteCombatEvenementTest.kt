package com.jc2.jdrcompagnon.feature_evenement

import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.ui.difficulteDuCombat
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class DifficulteCombatEvenementTest {

    @Test
    fun `un combat d'evenement est de difficulte moyenne par defaut`() {
        assertEquals(EncounterDifficulty.MOYENNE, difficulteDuCombat(null))
        assertEquals(EncounterDifficulty.MOYENNE, difficulteDuCombat(IssueEvenement(nature = NatureIssue.REUSSITE, declencheCombat = true)))
        assertEquals(EncounterDifficulty.MOYENNE, difficulteDuCombat(IssueEvenement(nature = NatureIssue.AUTRE, declencheCombat = true)))
    }

    @Test
    fun `un test rate monte la difficulte d'un cran`() {
        assertEquals(EncounterDifficulty.DIFFICILE, difficulteDuCombat(IssueEvenement(nature = NatureIssue.ECHEC, declencheCombat = true)))
    }
}
