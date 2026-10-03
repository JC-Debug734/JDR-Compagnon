package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Vérifie que classes_srd521.md (codes "desclas"/"caracprinc"/"equipdepX"/"equipdepN",
 * tags <!-- id: ...; type: ... --> sur les aptitudes) reste entièrement compris par
 * ClasseParser : les 12 classes, leur équipement de départ et leurs aptitudes doivent
 * remonter sans qu'aucun code ne connaisse leurs noms à l'avance.
 */
class ClasseParserFullTest {
    private val rawMarkdown = Files.readString(Paths.get("C:/Projet/JDRCompagnon/app/src/main/assets/dnd/classes_srd521.md"))
    private val classes = ClasseParser.parse(rawMarkdown)

    @Test
    fun `les 12 classes du SRD sont reconnues avec leurs traits de base`() {
        assertEquals(12, classes.size)
        classes.forEach { classe ->
            assertTrue("id manquant pour ${classe.nom}", !classe.id.isNullOrBlank())
            assertTrue("description manquante pour ${classe.nom}", classe.description.isNotBlank())
            assertTrue("caractéristique principale manquante pour ${classe.nom}", classe.caracteristiquePrincipale.isNotEmpty())
            assertTrue("dé de vie manquant pour ${classe.nom}", classe.deDeVie.matches(Regex("d\\d+")))
            assertTrue("jets de sauvegarde manquants pour ${classe.nom}", classe.maitriseJetsSauvegarde.isNotEmpty())
            assertTrue("maîtrises de compétence manquantes pour ${classe.nom}", classe.maitrisesCompetence.isNotBlank())
        }
    }

    @Test
    fun `l equipement de depart distingue choix et cumul pour chaque classe`() {
        classes.forEach { classe ->
            // Les 12 classes du SRD proposent toutes un choix (A) / (B) (Guerrier : A/B/C).
            assertTrue("aucune option d'équipement pour ${classe.nom}", classe.equipement.options.size >= 2)
            classe.equipement.options.forEach { option ->
                assertTrue("option ${option.lettre} vide pour ${classe.nom}", option.texte.isNotBlank())
            }
        }
        val guerrier = classes.first { it.nom == "Guerrier" }
        assertEquals(listOf("A", "B", "C"), guerrier.equipement.options.map { it.lettre })
    }

    @Test
    fun `toutes les aptitudes portent un id et un type reconnus`() {
        val typesValides = setOf("automatique", "choix-generique", "choix-sousclasse", "choix-effet")
        classes.forEach { classe ->
            assertTrue("aucune aptitude pour ${classe.nom}", classe.aptitudes.isNotEmpty())
            classe.aptitudes.forEach { apt ->
                assertTrue("aptitude sans niveau chez ${classe.nom} : ${apt.nom}", apt.niveaux.isNotEmpty())
                assertTrue("id manquant chez ${classe.nom} : ${apt.nom}", !apt.id.isNullOrBlank())
                // "choix-expertise-N" (N = nombre de compétences à passer en Expertise, ex.
                // Expertise du Rôdeur/Barde/Roublard, Fin explorateur du Rôdeur) : famille de
                // types à suffixe variable, non couverte par le set fixe ci-dessus.
                val typeReconnu = apt.type in typesValides ||
                    Regex("""^choix-expertise-\d+$""").matches(apt.type)
                assertTrue("type inconnu chez ${classe.nom} : ${apt.nom} (${apt.type})", typeReconnu)
            }
        }
    }

    @Test
    fun `chaque classe propose un choix d amelioration de caracteristique et de sous-classe`() {
        classes.forEach { classe ->
            assertTrue(
                "pas d'Amélioration de caractéristique pour ${classe.nom}",
                classe.aptitudes.any { it.type == "choix-generique" }
            )
            assertTrue(
                "pas de choix de sous-classe pour ${classe.nom}",
                classe.aptitudes.any { it.type == "choix-sousclasse" && 3 in it.niveaux }
            )
        }
    }

    @Test
    fun `chaque classe expose le nom de sa sous-classe`() {
        classes.forEach { classe ->
            assertTrue("nom de sous-classe manquant pour ${classe.nom}", classe.sousClasses.firstOrNull()?.nom.isNullOrBlank().not())
        }
        assertEquals("Voie du Berserker", classes.first { it.nom == "Barbare" }.sousClasses.first().nom)
    }

    @Test
    fun `aptitudesNiveau1 ne garde que les aptitudes de niveau 1`() {
        classes.forEach { classe ->
            assertTrue("pas d'aptitude de niveau 1 pour ${classe.nom}", classe.aptitudesNiveau1.isNotEmpty())
            classe.aptitudesNiveau1.forEach { apt -> assertTrue(1 in apt.niveaux) }
        }
    }

    @Test
    fun `chaque classe expose ses gains de multiclassage, distincts des traits de base`() {
        classes.forEach { classe ->
            assertTrue("maitrisesCompetence (maitcompA) manquante pour ${classe.nom}", classe.maitrisesCompetence.isNotBlank())
        }
        val barde = classes.first { it.nom == "Barde" }
        assertEquals("Armures légères", barde.multiclassage.armures)
        assertEquals("1 compétence au choix", barde.multiclassage.competence)

        val ensorceleur = classes.first { it.nom == "Ensorceleur" }
        assertEquals(null, ensorceleur.multiclassage.armures) // "(aucune)" -> null
        assertEquals(null, ensorceleur.multiclassage.competence)

        val roublard = classes.first { it.nom == "Roublard" }
        assertEquals("Outils de voleur", roublard.multiclassage.outils)
        assertTrue(roublard.multiclassage.competence!!.contains("Roublard"))

        // Table de maîtrises du multiclassage (progression.md) : Occultiste ("Invocateur"
        // dans le texte SRD historique) gagne aussi les armes courantes, pas seulement
        // l'armure légère.
        val occultiste = classes.first { it.nom == "Occultiste" }
        assertEquals("Armures légères", occultiste.multiclassage.armures)
        assertEquals("Armes courantes", occultiste.multiclassage.armes)
    }

    @Test
    fun `une classe sans formation aux armures ne gagne pas d armures en multiclassant`() {
        // Règle SRD (progression.md, section "Maîtrises") : les gains de multiclassage sont
        // une PARTIE des maîtrises de départ, jamais plus — une classe qui ne forme à aucune
        // armure au niveau 1 (Ensorceleur, Magicien...) n'en gagne donc pas non plus en
        // multiclassant.
        classes.filter { it.formationArmures.equals("Aucune", ignoreCase = true) }.forEach { classe ->
            assertEquals("${classe.nom} ne forme à aucune armure, ne devrait pas en gagner via multiclassage", null, classe.multiclassage.armures)
        }
    }

    @Test
    fun `le prerequis de multiclassage distingue le ou du et`() {
        val guerrier = classes.first { it.nom == "Guerrier" } // "Force ou Dextérité" : un seul suffit
        assertTrue(guerrier.remplitPrerequisMulticlasse { car -> if (car == Caracteristique.FORCE) 13 else 8 })
        assertTrue(guerrier.remplitPrerequisMulticlasse { car -> if (car == Caracteristique.DEXTERITE) 13 else 8 })
        assertTrue("aucune des deux à 13+ ne devrait pas remplir le prérequis", !guerrier.remplitPrerequisMulticlasse { 8 })

        val paladin = classes.first { it.nom == "Paladin" } // "Force et Charisme" : les deux requises
        assertTrue(paladin.remplitPrerequisMulticlasse { car -> if (car == Caracteristique.FORCE || car == Caracteristique.CHARISME) 13 else 8 })
        assertTrue(
            "Force seule à 13 ne devrait pas suffire pour le Paladin",
            !paladin.remplitPrerequisMulticlasse { car -> if (car == Caracteristique.FORCE) 13 else 8 }
        )

        val barbare = classes.first { it.nom == "Barbare" } // une seule caractéristique principale
        assertTrue(barbare.remplitPrerequisMulticlasse { car -> if (car == Caracteristique.FORCE) 13 else 8 })
        assertTrue(!barbare.remplitPrerequisMulticlasse { 8 })
    }

    @Test
    fun `les aptitudes choix-effet exposent leurs options`() {
        val clerc = classes.first { it.nom == "Clerc" }
        val ordreDivin = clerc.aptitudes.first { it.id == "ordre-divin" }
        assertEquals("choix-effet", ordreDivin.type)
        assertEquals(listOf("Protecteur", "Thaumaturge"), ordreDivin.options.map { it.nom })
        assertEquals(listOf("protecteur", "thaumaturge"), ordreDivin.options.map { it.id })
        assertTrue(ordreDivin.options.all { it.description.isNotBlank() })
        // La description de l'aptitude garde l'intro mais plus les lignes d'options.
        assertTrue(ordreDivin.description.startsWith("Choix entre"))
        assertTrue(!ordreDivin.description.contains("**Protecteur**"))

        val druide = classes.first { it.nom == "Druide" }
        val ordrePrimitif = druide.aptitudes.first { it.id == "ordre-primitif" }
        assertEquals("choix-effet", ordrePrimitif.type)
        assertEquals(listOf("Mage", "Gardien"), ordrePrimitif.options.map { it.nom })

        // Une aptitude "automatique" ordinaire n'a aucune option.
        val rage = classes.first { it.nom == "Barbare" }.aptitudes.first { it.id == "rage" }
        assertTrue(rage.options.isEmpty())
    }

    @Test
    fun `chaque classe porte le bon type d incantation`() {
        val complets = setOf("Barde", "Clerc", "Druide", "Ensorceleur", "Magicien")
        val demis = setOf("Paladin", "Rôdeur")
        classes.forEach { classe ->
            val attendu = when (classe.nom) {
                in complets -> "complet"
                in demis -> "demi"
                "Occultiste" -> "pacte"
                else -> "aucun"
            }
            assertEquals("typeIncantation incorrect pour ${classe.nom}", attendu, classe.typeIncantation)
        }
    }

    @Test
    fun `la table partagee des emplacements de sort correspond a la table SRD`() {
        val table = EmplacementsDeSortParser.parse(rawMarkdown)
        assertTrue("table des emplacements introuvable", table != null)
        // Magicien niveau 5 (lanceur complet) : 4 emplacements de 1er niveau, 3 de 2e, 2 de 3e.
        assertEquals("4", table!!.valeurNiveau("5", "1er"))
        assertEquals("3", table.valeurNiveau("5", "2e"))
        assertEquals("2", table.valeurNiveau("5", "3e"))
        assertEquals("0", table.valeurNiveau("5", "4e"))
        // Niveau 20 : le maximum de la table (4/3/3/3/3/2/2/1/1).
        assertEquals("2", table.valeurNiveau("20", "7e"))
        assertEquals("1", table.valeurNiveau("20", "9e"))
    }

    @Test
    fun `l Occultiste garde sa propre table Magie de pacte dans sa table de progression`() {
        val occultiste = classes.first { it.nom == "Occultiste" }
        val table = occultiste.tableProgression
        assertTrue("table de progression manquante pour l'Occultiste", table != null)
        // Niveau 5 : 2 emplacements de 3e niveau (table Magie de pacte du SRD).
        assertEquals("2", table!!.valeurNiveau("5", "Emplacements de sort"))
        assertEquals("3", table.valeurNiveau("5", "Niveau des emplacements"))
    }

    @Test
    fun `chaque sous-classe du SRD a un resume de style de jeu et des aptitudes structurees`() {
        val sousClasses = classes.flatMap { c -> c.sousClasses.map { c.nom to it } }
        assertEquals(12, sousClasses.size)
        sousClasses.forEach { (classe, sc) ->
            assertTrue("résumé manquant pour ${sc.nom} ($classe)", sc.description.isNotBlank() && "**" !in sc.description && "|" !in sc.description)
            assertTrue("aptitudes non structurées pour ${sc.nom} ($classe)", sc.aptitudes.size >= 3 && sc.aptitudes.all { it.niveaux.isNotEmpty() && it.id != null })
        }
        val parNom = sousClasses.associate { it.second.nom to it.second }
        assertEquals(listOf("Aide", "Bénédiction", "Soins", "Restauration partielle"), parNom.getValue("Domaine de la Vie").sortsParNiveau[3])
        assertEquals(2, parNom.getValue("Serment de Dévotion").sortsJusquAu(3).size)
        assertTrue(parNom.getValue("Protecteur Fiélon").sortsJusquAu(9).contains("Fléau d’insectes"))
        assertEquals(2, parNom.getValue("Chasseur").aptitudes.first { it.id == "proie-du-chasseur" }.options.size)
        assertTrue(parNom.getValue("Collège du Savoir").aptitudes.first { it.id == "maitrises-supplementaires" }.balises.any { it["choix"] == "3" })
    }
}
