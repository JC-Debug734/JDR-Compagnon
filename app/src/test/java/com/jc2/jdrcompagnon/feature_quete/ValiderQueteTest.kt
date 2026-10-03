package com.jc2.jdrcompagnon.feature_quete

import com.jc2.jdrcompagnon.feature_group.domain.model.Mount
import com.jc2.jdrcompagnon.feature_group.domain.model.Transport
import com.jc2.jdrcompagnon.feature_quete.domain.ValiderQueteUseCase
import com.jc2.jdrcompagnon.feature_quete.domain.model.Quest
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestReward
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestRewardType
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValiderQueteTest {

    private val pj1 = Character(id = "a", name = "Aria", type = "PJ")
    private val pj2 = Character(id = "b", name = "Bram", type = "PJ")
    private val pnj = Character(id = "c", name = "Sildar", type = "PNJ")

    private val quest = Quest(
        title = "Sauver Sildar",
        rewards = listOf(
            QuestReward(type = QuestRewardType.XP, amount = 500),
            QuestReward(type = QuestRewardType.OR, amount = 100),
            QuestReward(type = QuestRewardType.OR, amount = 1),
            QuestReward(type = QuestRewardType.REPUTATION, label = "Lords' Alliance", amount = 10),
        ),
    )

    @Test
    fun `xp et or partages entre PJ et joueurs sans fiche, reste de l'or au tresor`() {
        val group = GameState.MjGroup(
            name = "Les Lames",
            memberIds = listOf("a", "b", "c"),
            tablePlayers = listOf(GameState.TablePlayer(name = "Invité")),
        )
        val rep = ValiderQueteUseCase.repartition(quest, group, listOf(pj1, pj2, pnj))
        // Le PNJ compagnon ne prend pas de part : 2 PJ + 1 joueur sans fiche.
        assertEquals(3, rep.parts)
        assertEquals(166, rep.xpParJoueur)
        assertEquals(33, rep.orParJoueur)
        assertEquals(2, rep.orAuTresor)
    }

    @Test
    fun `groupe sans joueur ne recoit aucune part`() {
        val rep = ValiderQueteUseCase.repartition(quest, GameState.MjGroup(name = "Vide"), emptyList())
        assertEquals(0, rep.parts)
        assertEquals(0, rep.xpParJoueur)
        assertEquals(101, rep.orAuTresor)
    }

    @Test
    fun `un chariot sans monture attelee est immobile, sinon va a la vitesse de la plus lente`() {
        val chariot = Transport(id = "t", name = "Chariot")
        assertNull(chariot.vitesse(emptyList()))
        val mounts = listOf(
            Mount(name = "Trait", speed = 12, transportId = "t"),
            Mount(name = "Selle", speed = 18, transportId = "t"),
            Mount(name = "Libre", speed = 24),
        )
        assertEquals(12, chariot.vitesse(mounts))
        assertEquals(9, Transport(name = "Barque", needsMount = false, ownSpeed = 9).vitesse(emptyList()))
    }

    @Test
    fun `resume des recompenses`() {
        assertEquals("Réputation -5 — Zhentarim", QuestReward(type = QuestRewardType.REPUTATION, label = "Zhentarim", amount = -5).resume)
        assertEquals("Corde ×2", QuestReward(type = QuestRewardType.EQUIPEMENT, label = "Corde", amount = 2).resume)
    }
}
