package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

/**
 * Parsers pour les fichiers markdown structurés du SRD 5.2.1 tels que produits
 * dans le projet (classes_srd521.md, historiques_srd521.md, especes_srd521.md,
 * dons_srd521.md, langues.md) : titres "## Nom" avec commentaire "<!-- id: slug -->",
 * sous-sections "### " / "####", listes "- Clé : valeur".
 *
 * Principe : le wizard de création ne connaît AUCUNE liste de classes, d'historiques
 * ou d'espèces en dur. Tout bloc "## Nouveau nom" ajouté dans un de ces fichiers, ou
 * tout fichier remplacé par une version avec plus de choix, remonte automatiquement
 * sans changement de code — seul le contenu texte est lu.
 */

// ---------------------------------------------------------------------------
// Aide générique : découpage markdown par niveau de titre
// ---------------------------------------------------------------------------

data class SectionMd(val titre: String, val corps: String)

/** Découpe un document markdown en sections au niveau de titre demandé (2 pour "## ", 3 pour "### "...). */
fun decouperSections(markdown: String, niveau: Int): List<SectionMd> {
    val prefixe = "#".repeat(niveau) + " "
    val sections = mutableListOf<SectionMd>()
    var titreCourant: String? = null
    val corpsCourant = StringBuilder()

    markdown.lineSequence().forEach { ligne ->
        if (ligne.startsWith(prefixe)) {
            titreCourant?.let { sections += SectionMd(it, corpsCourant.toString().trim()) }
            titreCourant = ligne.removePrefix(prefixe).trim()
            corpsCourant.clear()
        } else {
            corpsCourant.appendLine(ligne)
        }
    }
    titreCourant?.let { sections += SectionMd(it, corpsCourant.toString().trim()) }
    return sections
}

private val REGEX_ID = Regex("""<!--\s*id:\s*([\w-]+)\s*-->""")
private fun extraireId(corps: String): String? = REGEX_ID.find(corps)?.groupValues?.get(1)

/** Extrait les lignes "- Clé : valeur" (ou "- Clé: valeur") d'un bloc en map ordonnée. */
private fun extraireChamps(corps: String): Map<String, String> =
    corps.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("- ") && it.contains(":") }
        .mapNotNull { ligne ->
            val contenu = ligne.removePrefix("- ")
            val parties = contenu.split(":", limit = 2)
            if (parties.size == 2) parties[0].trim() to parties[1].trim() else null
        }
        .toMap(linkedMapOf())

/** Découpe "A, B et C" / "A et B" / "A, B, C" en liste de tokens propres. */
private fun decouperListe(valeur: String): List<String> =
    valeur.split(Regex(",| et | ou ")).map { it.trim() }.filter { it.isNotBlank() }

// ---------------------------------------------------------------------------
// Compétences "au choix" (classes) et équipement de départ (classes + historiques)
// ---------------------------------------------------------------------------

/** Les 18 compétences du SRD 5.2.1, pour les classes dont le texte ne donne pas de liste fermée (ex. Barde : "3 au choix"). */
val TOUTES_COMPETENCES = listOf(
    "Acrobaties", "Arcanes", "Athlétisme", "Discrétion", "Dressage", "Escamotage",
    "Histoire", "Intimidation", "Intuition", "Investigation", "Médecine", "Nature",
    "Perception", "Persuasion", "Religion", "Représentation", "Survie", "Tromperie"
)

data class ChoixCompetencesClasse(val nombre: Int, val optionsFixes: List<String>?)

/**
 * Parse un champ "Maîtrises de compétence" de classe, ex. "2 au choix parmi
 * Athlétisme, Dressage, Intimidation" ou "3 au choix" (sans liste fermée : Barde
 * peut choisir parmi toutes les compétences, cf. [TOUTES_COMPETENCES]).
 */
fun parseCompetencesClasse(texte: String): ChoixCompetencesClasse {
    val nombre = Regex("""^(\d+)""").find(texte.trim())?.value?.toIntOrNull() ?: 2
    val optionsFixes = texte.substringAfter("parmi", missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
    return ChoixCompetencesClasse(nombre, optionsFixes)
}

data class OptionEquipement(val lettre: String, val texte: String)

/** Découpe un texte "(A) ... ; (B) ... ; ou (C) ..." en options distinctes. */
fun decouperOptionsEquipement(texte: String): List<OptionEquipement> =
    Regex("""\(([A-Z])\)\s*([^;]+)""").findAll(texte).map { m ->
        OptionEquipement(m.groupValues[1], m.groupValues[2].trim())
    }.toList()

/** Somme toutes les mentions "N po" trouvées dans un texte d'équipement. */
fun extraireOr(texte: String): Int =
    Regex("""(\d+)\s*po\b""").findAll(texte).sumOf { it.groupValues[1].toIntOrNull() ?: 0 }

/** Classes lanceuses de sorts dès le niveau 1 (Paladin/Rôdeur : sorts à partir du niveau 2, exclus ici). */
val CLASSES_LANCEUSES_NIVEAU_1 = setOf("Barde", "Clerc", "Druide", "Ensorceleur", "Magicien", "Occultiste")

// ---------------------------------------------------------------------------
// Classes — classes_srd521.md
// ---------------------------------------------------------------------------

data class AptitudeClasse(val nom: String, val description: String)

data class Classe(
    val id: String?,
    val nom: String,
    val caracteristiquePrincipale: List<Caracteristique>,
    val deDeVie: String, // ex. "d12"
    val maitriseJetsSauvegarde: List<Caracteristique>,
    val maitrisesCompetence: String,
    val maitrisesArme: String,
    val maitrisesOutils: String?,
    val formationArmures: String,
    val equipementDepart: String,
    val aptitudesNiveau1: List<AptitudeClasse>
) {
    fun versOption() = OptionChoisie(id = id ?: nom.lowercase(), label = nom)

    /** Points de vie max au niveau 1 = valeur maximale du dé de vie + modificateur de Constitution. */
    fun pvNiveau1(modificateurConstitution: Int): Int {
        val valeurDe = deDeVie.removePrefix("d").toIntOrNull() ?: 0
        return valeurDe + modificateurConstitution
    }

    val estLanceurDeSorts: Boolean get() = nom in CLASSES_LANCEUSES_NIVEAU_1
}

private fun nomVersCaracteristique(nom: String): Caracteristique? =
    Caracteristique.entries.firstOrNull { it.label.equals(nom.trim(), ignoreCase = true) }

object ClasseParser {
    fun parse(contenuMd: String): List<Classe> =
        decouperSections(contenuMd, 2)
            .filterNot { it.titre.startsWith("Tableau récapitulatif") } // section finale non structurée
            .mapNotNull { section ->
                val corpsTraitsDeBase = decouperSections(section.corps, 3)
                    .firstOrNull { it.titre == "Traits de base" }?.corps ?: return@mapNotNull null
                val champs = extraireChamps(corpsTraitsDeBase)

                val aptitudesNiveau1 = decouperSections(section.corps, 3)
                    .firstOrNull { it.titre == "Aptitudes de classe" }
                    ?.let { decouperSections(it.corps, 4) }
                    ?.filter { it.titre.startsWith("Niveau 1") }
                    ?.map { AptitudeClasse(it.titre.substringAfter(":").trim(), it.corps.trim()) }
                    ?: emptyList()

                Classe(
                    id = extraireId(section.corps),
                    nom = section.titre,
                    caracteristiquePrincipale = champs["Caractéristique principale"]
                        ?.let { decouperListe(it).mapNotNull(::nomVersCaracteristique) } ?: emptyList(),
                    deDeVie = Regex("d\\d+").find(champs["Dé de vie"].orEmpty())?.value ?: "",
                    maitriseJetsSauvegarde = champs["Maîtrise des jets de sauvegarde"]
                        ?.let { decouperListe(it).mapNotNull(::nomVersCaracteristique) } ?: emptyList(),
                    maitrisesCompetence = champs["Maîtrises de compétence"].orEmpty(),
                    maitrisesArme = champs["Maîtrises d'arme"].orEmpty(),
                    maitrisesOutils = champs["Maîtrises d'outils"],
                    formationArmures = champs["Formation aux armures"].orEmpty(),
                    equipementDepart = champs["Équipement de départ"].orEmpty(),
                    aptitudesNiveau1 = aptitudesNiveau1
                )
            }
}

// ---------------------------------------------------------------------------
// Historiques — historiques_srd521.md
// ---------------------------------------------------------------------------

data class Historique(
    val id: String?,
    val nom: String,
    val caracteristiques: List<Caracteristique>,
    val don: String,
    val maitrisesCompetence: List<String>,
    val maitriseOutils: String,
    val equipementA: String,
    val equipementB: String
) {
    fun versOption() = OptionChoisie(id = id ?: nom.lowercase(), label = nom)
}

object HistoriqueParser {
    private val REGEX_EQUIP_A = Regex("""-\s*\*\*A\.\*\*\s*(.+)""")
    private val REGEX_EQUIP_B = Regex("""-\s*\*\*B\.\*\*\s*(.+)""")

    fun parse(contenuMd: String): List<Historique> =
        decouperSections(contenuMd, 2).map { section ->
            val champs = extraireChamps(section.corps)
            Historique(
                id = extraireId(section.corps),
                nom = section.titre,
                caracteristiques = champs["Valeurs de caractéristique"]
                    ?.let { decouperListe(it).mapNotNull(::nomVersCaracteristique) } ?: emptyList(),
                don = champs["Don"].orEmpty(),
                maitrisesCompetence = decouperListe(champs["Maîtrises de compétence"].orEmpty()),
                maitriseOutils = champs["Maîtrise d'outils"].orEmpty(),
                equipementA = REGEX_EQUIP_A.find(section.corps)?.groupValues?.get(1)?.trim().orEmpty(),
                equipementB = REGEX_EQUIP_B.find(section.corps)?.groupValues?.get(1)?.trim().orEmpty()
            )
        }
}

// ---------------------------------------------------------------------------
// Espèces — especes_srd521.md
// ---------------------------------------------------------------------------

data class Trait(val nom: String, val description: String)

data class Espece(
    val id: String?,
    val nom: String,
    val typeCreature: String,
    val taille: String,
    val vitesse: String,
    val traits: List<Trait>
) {
    fun versOption() = OptionChoisie(id = id ?: nom.lowercase(), label = nom)
}

object EspeceParser {
    fun parse(contenuMd: String): List<Espece> =
        decouperSections(contenuMd, 2).map { section ->
            val champs = extraireChamps(section.corps)
            val traits = decouperSections(section.corps, 4).map { t -> Trait(t.titre, t.corps.trim()) }
            Espece(
                id = extraireId(section.corps),
                nom = section.titre,
                typeCreature = champs["Type de créature"].orEmpty(),
                taille = champs["Catégorie de taille"].orEmpty(),
                vitesse = champs["Vitesse"].orEmpty(),
                traits = traits
            )
        }
}

// ---------------------------------------------------------------------------
// Dons — dons_srd521.md (utile pour afficher le don d'un historique, et pour
// le trait "Polyvalent" de l'Humain qui laisse choisir n'importe quel don d'origines)
// ---------------------------------------------------------------------------

data class Don(val id: String?, val nom: String, val categorie: String, val description: String)

object DonParser {
    private val REGEX_ID_INLINE = Regex("""<!--.*?-->""")

    fun parse(contenuMd: String): List<Don> =
        decouperSections(contenuMd, 2).flatMap { groupe ->
            val categorie = groupe.titre
                .removePrefix("Dons ")
                .removePrefix("de ")
                .removePrefix("d'")
                .replaceFirstChar { it.uppercase() }
            decouperSections(groupe.corps, 3).map { section ->
                val corpsNettoye = section.corps
                    .replace(REGEX_ID_INLINE, "")
                    .lineSequence()
                    .filterNot { it.trim().let { l -> l.startsWith("- Catégorie") || l.startsWith("- Prérequis") } }
                    .joinToString("\n")
                    .trim()
                Don(
                    id = extraireId(section.corps),
                    nom = section.titre,
                    categorie = categorie,
                    description = corpsNettoye
                )
            }
        }
}

// ---------------------------------------------------------------------------
// Langues — langues.md (format "### Groupe" + "- Nom")
// ---------------------------------------------------------------------------

data class Langue(val nom: String, val groupe: String) {
    fun versOption() = OptionChoisie(id = nom.lowercase(), label = nom)
}

object LangueParser {
    fun parse(contenuMd: String): List<Langue> =
        decouperSections(contenuMd, 3).flatMap { groupe ->
            groupe.corps.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("- ") }
                .map { Langue(it.removePrefix("- ").trim(), groupe.titre) }
        }
}