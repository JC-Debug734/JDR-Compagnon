package com.jc2.jdrcompagnon.feature_import

import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CampagneArchiveTest {

    private fun dossier(): File = Files.createTempDirectory("campagne").toFile()

    @Test
    fun `les quetes detaillees donnent des quetes de l app`() {
        val racine = dossier()
        File(racine, "campagne.json").writeText(
            """
            { "titre": "Dragon", "objectifs": ["Ancien format ignoré"],
              "quetes": [ { "titre": "Excavation naine", "description": "Prévenir les nains.", "notesMj": "Quête de départ.",
                            "lieu": "Excavation naine", "donneur": "Inconnu", "recompenses": [ { "type": "or", "montant": 50 }, { "type": "BIZARRE" } ] },
                          { "titre": "Le panneau", "statut": "EN_COURS", "visible": true } ] }
            """.trimIndent()
        )
        val dto = CampagneArchive.lire(racine)!!.dto
        val quetes = CampagneArchive.quetes(dto.quetes, "dnd", emptyList())
        assertEquals(listOf("Excavation naine", "Le panneau"), quetes.map { it.title })
        val excavation = quetes[0]
        assertEquals(com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_ATTENTE, excavation.status)
        assertEquals("Excavation naine", excavation.location)
        assertEquals("Quête de départ.", excavation.mjNotes)
        assertNull(excavation.giverCharacterId)
        assertEquals(50, excavation.rewards.single().amount)
        assertEquals(com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_COURS, quetes[1].status)
        assertEquals(true, quetes[1].visibleToPlayers)
        racine.deleteRecursively()
    }

    @Test
    fun `lit campagne json et l image de carte a cote`() {
        val racine = dossier()
        val sous = File(racine, "Campagne").apply { mkdirs() }
        File(sous, "carte.jpg").writeBytes(byteArrayOf(1, 2, 3))
        File(sous, "campagne.json").writeText(
            """
            { "titre": "Dragon", "calendrier": "harptos", "champInconnu": 1,
              "scenarios": ["Phandalin"],
              "carte": { "nom": "Côte", "image": "carte.jpg", "largeurCases": 35, "hauteurCases": 48, "echelleKmParCase": 5 },
              "lieux": [ { "nom": "Phandalin", "type": "VILLE", "x": 20, "y": 37,
                           "lieuxNotables": [ { "nom": "Auberge" } ],
                           "boutiques": [ { "nom": "Barthen", "articles": [ { "nom": "Corde", "prix": 1 } ] } ] } ] }
            """.trimIndent()
        )
        val contenu = CampagneArchive.lire(racine)!!
        assertEquals("Dragon", contenu.dto.titre)
        assertEquals(35, contenu.dto.carte!!.largeurCases)
        val lieu = contenu.dto.lieux.single()
        assertEquals("VILLE", lieu.type)
        assertEquals("Auberge", lieu.lieuxNotables.single().nom)
        assertEquals("Corde", lieu.boutiques.single().articles.single().nom)
        assertArrayEquals(byteArrayOf(1, 2, 3), contenu.imageCarte)
        racine.deleteRecursively()
    }

    @Test
    fun `lit les evenements des lieux et l environnement de campagne`() {
        val racine = dossier()
        File(racine, "campagne.json").writeText(
            """
            { "titre": "Dragon", "environnement": "Région",
              "environnements": [ { "nom": "Région", "evenements": [ { "titre": "Où est le dragon ?", "type": "peril",
                  "monstres": ["Jeune dragon blanc"],
                  "issues": [ { "nature": "ECHEC", "titre": "Il attaque", "combat": true }, { "nature": "INCONNUE" } ] } ] } ],
              "lieux": [ { "nom": "Phandalin", "visible": true,
                           "evenements": [ { "titre": "Rumeur", "type": "RUMEUR_INDICE", "monstres": ["Orc x4"],
                                             "effets": [ { "categorie": "LIEU", "cible": " Tour des tempêtes " }, { "categorie": "???" } ] } ] },
                         { "nom": "Tour des tempêtes" } ] }
            """.trimIndent()
        )
        val dto = CampagneArchive.lire(racine)!!.dto
        assertEquals("Région", dto.environnement)
        assertEquals(true, dto.lieux[0].visible)
        assertEquals(false, dto.lieux[1].visible)

        val rumeur = CampagneArchive.evenement(dto.lieux[0].evenements.single(), "camp", "dnd")
        assertEquals(TypeEvenement.RUMEUR_INDICE, rumeur.type)
        assertEquals("camp", rumeur.campagneId)
        assertEquals(CategorieEffet.LIEU, rumeur.effets[0].categorie)
        assertEquals("Tour des tempêtes", rumeur.effets[0].cible)
        assertEquals(CategorieEffet.AUTRE, rumeur.effets[1].categorie)
        assertEquals("Orc" to 4, rumeur.profils.single().let { it.nom to it.quantite })

        val dragon = CampagneArchive.evenement(dto.environnements.single().evenements.single(), "camp", "dnd")
        assertEquals(TypeEvenement.PERIL, dragon.type)
        assertEquals(listOf(NatureIssue.ECHEC, NatureIssue.REUSSITE), dragon.issues.map { it.nature })
        assertEquals(true, dragon.issues[0].declencheCombat)
        racine.deleteRecursively()
    }

    @Test
    fun `sans campagne json ou json invalide aucune campagne`() {
        val racine = dossier()
        assertNull(CampagneArchive.lire(racine))
        File(racine, "campagne.json").writeText("{ pas du json")
        assertNull(CampagneArchive.lire(racine))
        racine.deleteRecursively()
    }

    @Test
    fun `lit lieux json avec l image et l icone de chaque lieu`() {
        val racine = dossier()
        val sous = File(racine, "Lieux").apply { mkdirs() }
        File(sous, "camp.webp").writeBytes(byteArrayOf(4, 5))
        File(sous, "lieux.json").writeText(
            """
            { "lieux": [ { "nom": "Camp vistani", "type": "VILLE", "icone": "campement", "image": "camp.webp",
                           "boutiques": [ { "nom": "Forge", "services": [ { "nom": "Ferrer", "prix": 1 } ] } ] },
                         { "nom": "" } ] }
            """.trimIndent()
        )
        val contenu = CampagneArchive.lireLieux(racine)!!
        val lieu = contenu.lieux.single()
        assertEquals("campement", lieu.icone)
        assertEquals("Ferrer", lieu.boutiques.single().services.single().nom)
        assertArrayEquals(byteArrayOf(4, 5), contenu.images["Camp vistani"])
        assertNull(CampagneArchive.lireLieux(dossier()))
        racine.deleteRecursively()
    }
}
