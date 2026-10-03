package com.jc2.jdrcompagnon.ui.srd

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Couvre les deux formats réellement chargés par SrdRepository.loadEquipmentList :
 * les blocs "**Champ** valeur" de equipement_srd521.md (D&D) et les listes
 * "- Nom : description" de naheulbeuk/equipment.md. L'ancien format à base de
 * tableaux Markdown ("| Colonne | ... |") n'est plus utilisé par aucun fichier
 * du projet et a été retiré d'EquipmentParser avec ses tests.
 */
class EquipmentParserTest {

    @Test
    fun `chaque bloc en gras devient une entree distincte avec sa categorie`() {
        val snippet = """
            # Équipement — SRD 5.2.1

            ### Dague

            **Type** Arme
            **Catégorie** Courante
            **Portée** Corps à corps
            **Dégâts** 1d4 perforants
            **Propriétés** Finesse, Lancer (6/18), Légère
            **Botte** Coup double
            **Poids** 0,5 kg
            **Coût** 2 po
            **Consommable** Non
            **Description** Arme courante de corps à corps.
            **Contenu** -

            ---

            ### Armure de cuir

            **Type** Armure
            **Catégorie** Légère
            **CA** 11 + mod. Dex
            **Force requise** -
            **Discrétion** -
            **Poids** 5 kg
            **Coût** 10 po
            **Description** Armure légère en cuir souple.
            **Contenu** -

            ---
        """.trimIndent()

        val entries = EquipmentParser.parse(snippet)

        assertEquals(2, entries.size)
        val dague = entries.single { it.name == "Dague" }
        assertEquals("Armes Courante", dague.category)
        assertEquals("1d4 perforants", dague.damage)
        assertEquals("Finesse, Lancer (6/18), Légère", dague.properties)

        val armure = entries.single { it.name == "Armure de cuir" }
        assertEquals("Armures Légère", armure.category)
        assertEquals("11 + mod. Dex", armure.ac)
    }

    @Test
    fun `un champ multi-lignes en liste a puces est rattache au bon champ`() {
        val snippet = """
            ### Paquetage d'explorateur

            **Type** Paquetage
            **Coût** 10 po
            **Poids** 12 kg
            **Description** Kit de base pour l'exploration.
            **Contenu**
            - Sac à dos
            - Corde (15 m)
            - 10 Torche

            ---
        """.trimIndent()

        val paquetage = EquipmentParser.parse(snippet).single()

        assertEquals("Paquetages", paquetage.category)
        assertTrue(
            "Le contenu du paquetage doit lister ses objets",
            paquetage.rawMarkdown.contains("Contenu : Sac à dos, Corde (15 m), 10 Torche")
        )
    }

    @Test
    fun `le format Naheulbeuk sert de repli quand il n y a pas de champ en gras`() {
        val snippet = """
            # Équipement Naheulbeuk

            ## Armes courantes
            - Dague : légère, mêlée ou distance courte.
            - Épée courte : arme de mêlée standard.

            ## Armures
            - Armures légères : matelassée, cuir, cuir clouté.
        """.trimIndent()

        val entries = EquipmentParser.parse(snippet)

        assertEquals(
            listOf("Dague", "Épée courte", "Armures légères"),
            entries.map { it.name }
        )
        assertEquals("Armes courantes", entries[0].category)
        assertEquals("Armures", entries[2].category)
        assertTrue(entries[0].rawMarkdown.contains("légère, mêlée ou distance courte"))
    }

    @Test
    fun `le champ Harmonisation est lu avec son prerequis`() {
        val snippet = """
            ### Chapeau de magicien
            **Type** Objets merveilleux
            **Harmonisation** Oui (magicien)
            **Description** Objet magique, courant.
            ---

            ### Luth à illusions
            **Type** Objets merveilleux
            **Harmonisation** Oui
            **Description** Objet magique, courant.
            ---

            ### Arme ardente
            **Type** Arme
            **Description** Objet magique, rare (harmonisation requise avec un paladin).
            ---

            ### Heaume effrayant
            **Type** Objets merveilleux
            **Description** Objet magique, courant.
            ---
        """.trimIndent()

        val entries = EquipmentParser.parse(snippet)

        assertTrue(entries[0].harmonisation)
        assertEquals("magicien", entries[0].harmonisationPrerequis)
        assertTrue(entries[1].harmonisation)
        assertEquals("", entries[1].harmonisationPrerequis)
        assertTrue(entries[2].harmonisation)
        assertEquals("paladin", entries[2].harmonisationPrerequis)
        assertEquals(false, entries[3].harmonisation)
    }
}
