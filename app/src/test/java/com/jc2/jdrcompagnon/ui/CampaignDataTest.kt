package com.jc2.jdrcompagnon.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires pour CampaignData et la fiche de suivi (checklist).
 * Ces tests ne nécessitent pas de contexte Android.
 */
class CampaignDataTest {

    @Test
    fun `CampaignData stores title worldId and checklist`() {
        val checklist = listOf(
            GameState.CampaignChecklistItem(label = "Récupérer l'amulette", checked = true),
            GameState.CampaignChecklistItem(label = "Battre le boss final")
        )
        val campaign = GameState.CampaignData(
            title = "La mine oubliée",
            worldId = "donjon_et_dragon",
            checklistItems = checklist
        )
        assertEquals("La mine oubliée", campaign.title)
        assertEquals("donjon_et_dragon", campaign.worldId)
        assertEquals(2, campaign.checklistItems.size)
        assertTrue(campaign.checklistItems[0].checked)
        assertFalse(campaign.checklistItems[1].checked)
    }

    @Test
    fun `CampaignChecklistItem has unique id by default`() {
        val item1 = GameState.CampaignChecklistItem(label = "A")
        val item2 = GameState.CampaignChecklistItem(label = "A")
        assertNotNull(item1.id)
        assertNotNull(item2.id)
        assertTrue(item1.id != item2.id)
    }

    @Test
    fun `CampaignData toMjCampaign preserves fields`() {
        val campaign = GameState.CampaignData(
            title = "Campagne test",
            worldId = "naheulbeuk",
            scenarioIds = listOf("scenario-1", "scenario-2"),
            checklistItems = listOf(
                GameState.CampaignChecklistItem(label = "Objectif 1", checked = true)
            )
        )
        val mjCampaign = campaign.toMjCampaign()
        assertEquals(campaign.id, mjCampaign.id)
        assertEquals(campaign.title, mjCampaign.title)
        assertEquals(campaign.worldId, mjCampaign.worldId)
        assertEquals(campaign.scenarioIds, mjCampaign.scenarioIds)
        assertEquals(campaign.checklistItems, mjCampaign.checklistItems)
    }

    @Test
    fun `MjCampaign toCampaignData preserves fields`() {
        val mjCampaign = GameState.MjCampaign(
            title = "Campagne MJ",
            worldId = "donjon_et_dragon",
            checklistItems = listOf(
                GameState.CampaignChecklistItem(label = "Explorer", checked = false)
            )
        )
        val data = mjCampaign.toCampaignData()
        assertEquals(mjCampaign.id, data.id)
        assertEquals(mjCampaign.title, data.title)
        assertEquals(mjCampaign.worldId, data.worldId)
        assertEquals(mjCampaign.checklistItems, data.checklistItems)
    }

    @Test
    fun `checklist progress counts correctly`() {
        val items = listOf(
            GameState.CampaignChecklistItem(label = "A", checked = true),
            GameState.CampaignChecklistItem(label = "B", checked = true),
            GameState.CampaignChecklistItem(label = "C", checked = false)
        )
        val checkedCount = items.count { it.checked }
        assertEquals(2, checkedCount)
        assertEquals(3, items.size)
    }
}
