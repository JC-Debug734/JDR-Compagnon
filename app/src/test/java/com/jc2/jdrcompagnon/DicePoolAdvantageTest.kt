package com.jc2.jdrcompagnon

import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.AdvantageState
import org.junit.Assert.assertEquals
import org.junit.Test

class DicePoolAdvantageTest {
    @Test
    fun testPool2d20AdvantageShowsTwoDice() {
        // Prepare state with 2d20 in pool and advantage
        GameState.addDiceToPool(20) // adds one
        GameState.addDiceToPool(20) // should increment count to 2
        GameState.setAdvantageState(AdvantageState.ADVANTAGE)
        val results = GameState.rollDicePool()
        // Expect two dice rolled (even if one is kept for advantage, UI should show both)
        assertEquals("Pool should contain two dice", 2, results.size)
    }
}
