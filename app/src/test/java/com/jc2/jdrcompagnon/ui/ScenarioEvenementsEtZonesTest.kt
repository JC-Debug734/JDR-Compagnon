package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.feature_butin.domain.Bourse
import com.jc2.jdrcompagnon.feature_butin.domain.ButinTexte
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import com.jc2.jdrcompagnon.feature_exploration.ZoneExploration
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.feature_import.CampagneArchive
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioEvenementsEtZonesTest {

    @Test
    fun `un evenement de campagne json arrive dans l outil evenements avec son image`() {
        val json = """
            { "titre": "Pic de givre", "evenements": [ { "titre": "L'ogre congelé", "type": "DECOUVERTE",
              "description": "Ligne 1\nLigne 2", "monstres": ["Ogre x2"], "image": "evenements/ogre.webp",
              "effets": [ { "categorie": "OBJET", "quantite": 3, "cible": "Potion de guérison" } ] } ] }
        """.trimIndent()
        val dto = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(CampagneArchive.Dto.serializer(), json)
        val evtDto = dto.evenements.single()
        assertEquals("evenements/ogre.webp", evtDto.image)

        val evenement = CampagneArchive.evenement(evtDto, "camp1", "dnd")
        assertEquals("L'ogre congelé", evenement.titre)
        assertEquals("camp1", evenement.campagneId)
        assertEquals(TypeEvenement.DECOUVERTE, evenement.type)
        assertEquals("Ligne 1\nLigne 2", evenement.description)
        assertEquals(TypeProfil.MONSTRE, evenement.profils.single().type)
        assertEquals(2, evenement.profils.single().quantite)
        val objet = evenement.effets.single()
        assertEquals(CategorieEffet.OBJET, objet.categorie)
        assertEquals(3, objet.quantite)
    }

    @Test
    fun `une case partagee par deux zones va a la plus petite`() {
        val piste = ZoneExploration(nom = "H1", cases = setOf(1, 2, 3, 4, 5))
        val salle = ZoneExploration(nom = "H2", cases = setOf(4, 5))
        val (pisteSeule, salleIntacte) = ScenarioImport.casesExclusives(listOf(piste, salle))
        assertEquals(setOf(1, 2, 3), pisteSeule.cases)
        assertEquals(setOf(4, 5), salleIntacte.cases)
    }

    @Test
    fun `un butin de scenario separe pieces et objets`() {
        val butin = ButinTexte.lire("450 pc; 182 pa; Longue-vue fêlée (100 po); 6x Pied en corail rouge (25 po); 2 500 po")
        assertEquals(Bourse(pc = 450, pa = 182, po = 2500), butin.bourse)
        assertEquals(listOf("Longue-vue fêlée", "Pied en corail rouge"), butin.objets.map { it.nom })
        assertEquals(6, butin.objets[1].quantite)
        assertEquals("25 po", butin.objets[1].note)
    }

    @Test
    fun `le titre du fichier suivant ne finit pas dans le dernier monstre`() {
        val livres = """
            # Bestiaire

            ### Jeune dragon blanc
            <!-- type: monstre -->
            Catégorie: Dragons
            CA: 17

            ## Actions
            Morsure. Corps à corps : +7.


            # Objets magiques

            Objets magiques de l'aventure.

            ## Objets
            ### Arc long +1
            <!-- type: objet -->
            **Type** Arme
        """.trimIndent()
        val dragon = CustomContentParser.parse(livres).monsters.single()
        assertTrue(dragon.rawMarkdown.contains("Morsure"))
        assertFalse(dragon.rawMarkdown.contains("Objets magiques"))
    }
}

class CharmesEtComparsesTest {

    @Test
    fun `un charme se reconnait a son type et garde ses utilisations`() {
        val charme = com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser.parse(
            """
            ### Charme de tempête
            **Type** Charmes magiques
            **Charges** 3
            **Pouvoir** Éclair : action, 1 utilisation.
            ---
            """.trimIndent()
        ).single()
        assertTrue(com.jc2.jdrcompagnon.ui.screens.joueur.character.ObjetsACharges.estCharme(charme))
        assertEquals(3, charme.charges)
    }

    @Test
    fun `la section personnalite d'un comparse remplit les traits de la fiche`() {
        val corps = "## Personnalité\nPersonnalité. Joyeux drille.\nIdéal. Aucun défi n'est trop grand.\nObligation. L'amitié avant tout.\nFaille. Cogne les grands."
        val import = com.jc2.jdrcompagnon.ui.screens.mj.library.srd.PnjImport
        assertEquals("Joyeux drille.", import.traitDePersonnalite(corps, "Personnalité"))
        assertEquals("L'amitié avant tout.", import.traitDePersonnalite(corps, "Lien", "Obligation"))
        assertEquals("Cogne les grands.", import.traitDePersonnalite(corps, "Défaut", "Faille"))
    }
}

class EtatImagesTest {
    @Test
    fun `le nom du fichier designe son etat, accents et tirets facultatifs`() {
        val e = com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EtatImages
        val cartes = listOf("etourdi", "empoisonne", "aveugle", "a-terre", "charme", "neutralise", "agrippe",
            "entrave", "assourdi", "petrifie", "invisible", "inconscient", "paralyse", "effraye")
        cartes.forEach { assertTrue("non reconnu : $it", e.cleEtat(it) != null) }
        assertEquals(e.cleEtat("À terre"), e.cleEtat("a-terre"))
        assertEquals(e.cleEtat("Neutralisé"), e.cleEtat("neutralise"))
        assertEquals(e.cleEtat("Épuisement"), e.cleEtat("epuisement"))
        assertEquals(14, cartes.map { e.cleEtat(it) }.toSet().size)
        assertEquals(null, e.cleEtat("carte-du-fort"))
    }
}
