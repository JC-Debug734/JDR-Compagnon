package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Export de boutique antérieur aux prix en pièces de cuivre : relu et converti (× 100). */
class BoutiqueEnPcTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `ancien export en po converti en pc`() {
        val ancien = """
            {"id":"b1","nom":"Forge","standing":"CORRECT","marchand":{"nom":"Brom"},"employes":[],
             "inventaire":[{"equipement":{"nom":"Torche","coutBaseEnPo":1,"type":"Équipement"},"prixApplique":2,"quantiteStock":3}],
             "services":[{"nom":"Réparation","description":"","prixEnPo":5}],
             "argentDisponibleEnPo":40}
        """.trimIndent()
        val b = json.decodeFromJsonElement(Boutique.serializer(), boutiqueEnPc(json.parseToJsonElement(ancien)))
        assertEquals(4000, b.argentDisponibleEnPc)
        assertEquals(100, b.inventaire.single().equipement.coutBaseEnPc)
        assertEquals(200, b.inventaire.single().prixEnPc)
        assertEquals(500, b.services.single().prixEnPc)
    }
}
