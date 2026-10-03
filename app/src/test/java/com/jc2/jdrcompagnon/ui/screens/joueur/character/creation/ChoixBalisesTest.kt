package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.DonsParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Balises de choix des classes, historiques et dons, de la création jusqu'à la fiche. */
class ChoixBalisesTest {

    private fun lire(fichier: String) = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/$fichier"))

    private val classes = ClasseParser.parse(lire("classes_srd521.md"))
    private val historiques = HistoriqueParser.parse(lire("historiques_srd521.md"))
    private val dons = DonParser.depuisEntrees(DonsParser.parse(lire("dons_srd521.md")).map { it.name to it.rawMarkdown })
    private val ctx = ContexteChoix(
        equipements = EquipmentParser.parse(lire("equipement_srd521.md")).map { it.name to it.category },
        langues = listOf("Elfique", "Nain"),
    )

    private fun holder() = CharacterCreationStateHolder().apply {
        tousLesDons = dons
        donsOriginesNoms = dons.filter { it.categorie == "Origines" }
        contexte = ctx
    }

    @Test
    fun `dons d'origines reconnus par leur balise`() {
        assertEquals(listOf("Doué", "Initié à la magie", "Sauvagerie martiale", "Vigilant"), dons.filter { it.categorie == "Origines" }.map { it.nom })
        assertEquals(listOf("initie-liste", "initie-incantation", "initie-sorts-mineurs", "initie-sort-1"), dons.first { it.nom == "Initié à la magie" }.balises.map { it["id"] })
    }

    @Test
    fun `options tirees du fichier equipement`() {
        val barde = classes.first { it.nom == "Barde" }
        val instruments = construireChoix(barde.balisesTraitsDeBase.first { it["id"] == "barde-instruments" }, "Barde", "", ctx)!!
        assertEquals(3, instruments.nombre)
        assertEquals(10, instruments.options.size)
        assertTrue(instruments.options.all { it.startsWith("Instrument de musique") })
        val moine = classes.first { it.nom == "Moine" }
        val outilMoine = construireChoix(moine.balisesTraitsDeBase.single(), "Moine", "", ctx)!!
        assertTrue(outilMoine.options.containsAll(instruments.options))
        assertTrue(outilMoine.options.size > instruments.options.size)
        assertEquals(1, barde.balisesMulticlasse.size)
    }

    @Test
    fun `initie a la magie - liste imposee par l'historique Acolyte`() {
        val h = holder()
        val draft = CharacterDraft(classe = classes.first { it.nom == "Guerrier" }, historique = historiques.first { it.nom == "Acolyte" })
        val choix = h.choixComplementaires(draft)
        assertEquals(listOf("Clerc"), choix.first { it.id.endsWith("initie-liste") }.impose)
        assertEquals(null, choix.first { it.id.endsWith("initie-incantation") }.impose)
    }

    @Test
    fun `soldat - boite de jeux choisie en maitrise et dans l'equipement`() {
        val h = holder()
        val draft = CharacterDraft(
            classe = classes.first { it.nom == "Guerrier" },
            historique = historiques.first { it.nom == "Soldat" },
            equipementHistoriqueTexte = historiques.first { it.nom == "Soldat" }.equipementA,
        )
        val choix = h.choixComplementaires(draft)
        val jeux = choix.first { it.id == "historique:soldat-jeux" }
        val selection = mapOf(jeux.id to listOf("Boîte de jeux (dés)"))
        val perso = draft.copy(choixComplementaires = selection).versCharacter(
            worldId = "donjon_et_dragon",
            choixAutres = choix.map { it to h.valeursDe(it, selection) },
            donsRecus = h.donsRecus(draft),
        )
        assertTrue(perso.proficiencies, perso.proficiencies == "Boîte de jeux (dés)")
        assertTrue(perso.backpackItems.toString(), "Boîte de jeux (dés)" in perso.backpackItems)
        assertTrue(perso.feats, perso.feats.startsWith("Sauvagerie martiale (don d'historique Soldat)"))
    }

    @Test
    fun `initie a la magie - sorts de la liste choisie`() {
        val h = holder().apply {
            contexte = ctx.copy(
                sorts = listOf(
                    SortChoisissable("Flamme sacrée", 0, listOf("Clerc"), ""),
                    SortChoisissable("Thaumaturgie", 0, listOf("Clerc"), ""),
                    SortChoisissable("Trait de feu", 0, listOf("Ensorceleur", "Magicien"), ""),
                    SortChoisissable("Bénédiction", 1, listOf("Clerc", "Paladin"), ""),
                    SortChoisissable("Projectile magique", 1, listOf("Magicien"), ""),
                )
            )
        }
        // Acolyte : liste Clerc imposée -> sorts du Clerc seulement.
        val acolyte = CharacterDraft(classe = classes.first { it.nom == "Guerrier" }, historique = historiques.first { it.nom == "Acolyte" })
        val choixAcolyte = h.choixComplementaires(acolyte)
        assertEquals(listOf("Flamme sacrée", "Thaumaturgie"), choixAcolyte.first { it.id.endsWith("initie-sorts-mineurs") }.options)
        assertEquals(listOf("Bénédiction"), choixAcolyte.first { it.id.endsWith("initie-sort-1") }.options)

        // Via Polyvalent (sans précision) : en attente tant que la liste n'est pas choisie.
        val humain = CharacterDraft(
            classe = classes.first { it.nom == "Guerrier" },
            historique = historiques.first { it.nom == "Soldat" },
            espece = EspeceParser.parse(lire("especes_srd521.md")).first { it.nom == "Humain" },
            especeChoix = mapOf("polyvalent" to listOf("Initié à la magie")),
        )
        assertTrue(h.choixComplementaires(humain).first { it.id.endsWith("initie-sort-1") }.enAttente != null)
        val selection = mapOf(
            "don:Initié à la magie:initie-liste" to listOf("Magicien"),
            "don:Initié à la magie:initie-incantation" to listOf("Intelligence"),
            "don:Initié à la magie:initie-sorts-mineurs" to listOf("Trait de feu"),
            "don:Initié à la magie:initie-sort-1" to listOf("Projectile magique"),
            "historique:soldat-jeux" to listOf("Boîte de jeux (dés)"),
        )
        val choix = h.choixComplementaires(humain, selection)
        assertEquals(listOf("Projectile magique"), choix.first { it.id.endsWith("initie-sort-1") }.options)
        val perso = humain.copy(choixComplementaires = selection).versCharacter(
            worldId = "donjon_et_dragon",
            choixAutres = choix.map { it to h.valeursDe(it, selection) },
            donsRecus = h.donsRecus(humain),
        )
        assertTrue(perso.spells.containsAll(listOf("Trait de feu", "Projectile magique")))
        assertTrue(perso.feats, perso.feats.contains("Initié à la magie (don d'espèce Humain) — Magicien, Intelligence, Trait de feu, Projectile magique"))
        assertTrue(perso.feats, perso.feats.contains("Sort du 1er niveau choisi : Projectile magique"))
    }

    @Test
    fun `roublard - une langue en plus et l'argot des voleurs`() {
        val h = holder()
        val draft = CharacterDraft(classe = classes.first { it.nom == "Roublard" })
        assertEquals(1, h.languesSupplementaires(draft).sumOf { it.nombre })
        val perso = draft.copy(langues = listOf(Langue("Commun", ""), Langue("Elfique", ""), Langue("Nain", ""), Langue("Orc", "")))
            .versCharacter(worldId = "donjon_et_dragon")
        assertTrue("Argot des voleurs" in perso.languages)
    }
}
