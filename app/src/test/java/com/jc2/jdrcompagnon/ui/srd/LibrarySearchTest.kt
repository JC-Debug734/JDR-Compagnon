package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.LibrarySearch
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LibrarySearchTest {

    private fun trouve(query: String, name: String, content: String? = null) =
        assertNotNull("« $query » devrait trouver « $name »", LibrarySearch.score(query, name, content))

    @Test
    fun `accents, majuscules et ponctuation ignores`() {
        trouve("elementaire de l eau", "Élémentaire de l’eau")
        trouve("ARAIGNEE", "Araignée")
        trouve("araignee-loup", "Araignée-loup géante")
    }

    @Test
    fun `mots dans le desordre et pluriel`() {
        trouve("rouge dragon", "Dragon rouge adulte")
        trouve("gobelins", "Gobelin")
        trouve("geantes araignees", "Araignée géante")
    }

    @Test
    fun `fautes de frappe legeres, mais pas n'importe quoi`() {
        trouve("dargon", "Dragon rouge")
        trouve("squelete", "Squelette")
        assertNull(LibrarySearch.score("chat", "Dragon rouge"))
        assertNull(LibrarySearch.score("xyz", "Aboleth"))
    }

    @Test
    fun `abreviations et recherche dans le texte`() {
        trouve("pv", "Points de vie")
        trouve("points de vie", "Vitalité", "Les pv du personnage")
        assertNull(LibrarySearch.matchesName("souffle", "Dragon rouge").takeIf { it })
        trouve("souffle feu", "Dragon rouge", "Souffle de feu (recharge 5–6).")
    }

    @Test
    fun `classement - nom exact puis debut de nom puis texte`() {
        val exact = LibrarySearch.score("dragon rouge", "Dragon rouge")!!
        val debut = LibrarySearch.score("dragon rouge", "Dragon rouge adulte")!!
        val texte = LibrarySearch.score("dragon rouge", "Kobold", "Serviteur d'un dragon rouge.")!!
        assertTrue(exact > debut && debut > texte)
    }

    @Test
    fun `bestiaire reel - le premier resultat est le bon`() {
        val monstres = MonsterParser.parse(File("src/main/assets/dnd/monster_srd521.md").readText())
        fun meilleur(q: String) = monstres
            .mapNotNull { m -> LibrarySearch.score(q, m.name, m.rawMarkdown)?.let { m.name to it } }
            .maxByOrNull { it.second }?.first
        assertEquals("Aboleth", meilleur("abolet"))
        assertEquals("Araignée géante", meilleur("geante araignee"))
    }
}
