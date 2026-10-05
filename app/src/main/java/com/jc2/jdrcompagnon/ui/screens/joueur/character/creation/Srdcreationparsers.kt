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

/** Courte description + caractéristique associée pour chacune des 18 compétences, affichée
 * comme détail lors du choix des compétences de classe (aucune source SRD ne fournit ce
 * texte : les classes ne citent que les noms de compétence, cf. TOUTES_COMPETENCES). */
val DESCRIPTIONS_COMPETENCES: Map<String, String> = mapOf(
    "Acrobaties" to "Dextérité — garder l'équilibre, esquiver ou effectuer une figure acrobatique.",
    "Arcanes" to "Intelligence — connaissances sur la magie, les objets magiques et les plans.",
    "Athlétisme" to "Force — grimper, nager, sauter ou lutter contre un adversaire.",
    "Discrétion" to "Dextérité — se déplacer sans être vu ni entendu.",
    "Dressage" to "Sagesse — calmer, monter ou faire obéir un animal.",
    "Escamotage" to "Dextérité — dérober un objet ou manipuler quelque chose discrètement.",
    "Histoire" to "Intelligence — connaissances sur les événements, peuples et civilisations passés.",
    "Intimidation" to "Charisme — influencer autrui par la menace ou l'hostilité affichée.",
    "Intuition" to "Sagesse — deviner les intentions ou détecter le mensonge d'autrui.",
    "Investigation" to "Intelligence — déduire des informations à partir d'indices.",
    "Médecine" to "Sagesse — stabiliser un mourant ou diagnostiquer un mal.",
    "Nature" to "Intelligence — connaissances sur la faune, la flore et les terrains.",
    "Perception" to "Sagesse — remarquer un détail, un bruit ou une présence.",
    "Persuasion" to "Charisme — convaincre autrui avec tact, diplomatie ou charme.",
    "Religion" to "Intelligence — connaissances sur les divinités, cultes et rites.",
    "Représentation" to "Charisme — divertir un public (musique, danse, théâtre...).",
    "Survie" to "Sagesse — suivre une piste, s'orienter ou vivre en milieu sauvage.",
    "Tromperie" to "Charisme — mentir ou dissimuler la vérité de façon convaincante."
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

/**
 * Équipement de départ d'une classe (cf. codes `equipdepX` / `equipdepN` / `equipdep`
 * dans classes_srd521.md, extraits par [extraireEquipementClasse]) :
 * - [options] : choix exclusif entre plusieurs lots (`equipdepA`, `equipdepB`...), triés
 *   par lettre. Peut être vide (aucun choix) ou n'avoir qu'une seule entrée (pas un vrai
 *   choix non plus : un seul lot possible).
 * - [fixe] : objets reçus systématiquement en plus, sans choix (`equipdep1`, `equipdep2`...).
 * - [sansChoix] : lot unique sans aucun choix (`equipdep` seul, sans lettre ni chiffre).
 */
data class EquipementClasse(
    val options: List<OptionEquipement>,
    val fixe: List<String>,
    val sansChoix: String?
)

private val REGEX_EQUIPDEP_CHOIX = Regex("""^equipdep([A-Z])$""")
private val REGEX_EQUIPDEP_FIXE = Regex("""^equipdep(\d+)$""")

/** Reconstitue l'équipement de départ d'une classe à partir des champs "equipdep*" de son
 * bloc Traits de base — aucune liste de classes n'est codée en dur : ajouter/retirer une
 * lettre `equipdepX` dans le fichier suffit à faire apparaître/disparaître un choix. */
fun extraireEquipementClasse(champs: Map<String, String>): EquipementClasse {
    val options = champs.entries
        .mapNotNull { (cle, valeur) -> REGEX_EQUIPDEP_CHOIX.find(cle)?.groupValues?.get(1)?.let { it to valeur } }
        .sortedBy { it.first }
        .map { (lettre, valeur) -> OptionEquipement(lettre, valeur) }
    val fixe = champs.entries
        .filter { (cle, _) -> REGEX_EQUIPDEP_FIXE.matches(cle) }
        .sortedBy { it.key }
        .map { it.value }
    return EquipementClasse(options, fixe, champs["equipdep"])
}

/**
 * Ce qu'un personnage gagne en multiclassant dans une classe (section "### Multiclassage" de
 * classes_srd521.md, distincte des Traits de base qui ne s'appliquent qu'en classe de départ) :
 * formation aux armures/armes/outils réduite (souvent moindre que `formationArmures`/
 * `maitrisesArme`, parfois absente), et [competence] pour la classe qui accorde une compétence
 * au choix propre au multiclassage (ex. Barde, Rôdeur, Roublard — `null` sinon, cf. table SRD).
 * Les aptitudes de niveau 1 elles-mêmes restent les mêmes qu'en classe de départ (le SRD le
 * précise explicitement) : aucun champ dédié n'est nécessaire, `Classe.aptitudes` suffit.
 */
data class InfosMulticlasse(
    val armures: String?,
    val armes: String?,
    val outils: String?,
    val competence: String?
)

private fun extraireInfosMulticlasse(champs: Map<String, String>) = InfosMulticlasse(
    armures = champs["multiarmures"]?.takeUnless { it == "(aucune)" },
    armes = champs["multiarmes"]?.takeUnless { it == "(aucune)" },
    outils = champs["multioutils"],
    competence = champs["maitcompB"]
)

/** Somme toutes les mentions "N po" trouvées dans un texte d'équipement. */
fun extraireOr(texte: String): Int =
    Regex("""(\d+)\s*po\b""").findAll(texte).sumOf { it.groupValues[1].toIntOrNull() ?: 0 }

private val REGEX_QUANTITE_OBJET = Regex("""^(\d+)\s+(.+)$""")
private val REGEX_GOLD_SEUL = Regex("""^\d+\s*po$""", RegexOption.IGNORE_CASE)
// "paquetage d'artiste" / "paquetage de cambrioleur" -> "Artiste" / "Cambrioleur" : les
// paquetages sont référencés par leur nom seul dans equipement_srd521.md (## Artiste,
// ## Cambrioleur...), sans le mot "paquetage".
private val REGEX_PREFIXE_PAQUETAGE = Regex("""^paquetage\s+(?:d['’]|de\s+)""", RegexOption.IGNORE_CASE)

private fun normaliserNomObjet(nom: String): String =
    REGEX_PREFIXE_PAQUETAGE.replace(nom.trim(), "").replaceFirstChar { it.uppercase() }

/**
 * Convertit un texte d'équipement de départ choisi (ex. "Armure de cuir, 2 dagues,
 * instrument de musique au choix, paquetage d'artiste et 19 po") en objets individuels
 * pour le sac à dos — un élément de liste par unité ("2 dagues" -> deux entrées "Dague"),
 * car c'est ainsi que GameState.totalEquipmentWeight comptabilise le poids porté (une
 * entrée = un objet). Les mentions d'or ("N po") sont ignorées : extraireOr calcule
 * l'or de départ séparément.
 *
 * `contenuPaquetages` (cf. [construireContenuPaquetages]) détaille un paquetage (ex.
 * "Explorateur") en ses objets réels (Sac à dos, Corde, 10 Torche...) plutôt que de
 * l'ajouter comme un unique objet portant le nom du paquetage.
 */
fun parseListeEquipement(texte: String, contenuPaquetages: Map<String, List<String>> = emptyMap()): List<String> {
    if (texte.isBlank()) return emptyList()
    return texte
        .replace(Regex("""\s+et\s+"""), ", ") // "... et 19 po" -> "..., 19 po" (dernier séparateur de liste)
        .split(",")
        // "Arc court + 20 flèches" : deux objets distincts, pas un seul.
        .flatMap { it.split(REGEX_SEPARATEUR_PLUS) }
        .map { it.trim() }
        .filterNot { it.isEmpty() || REGEX_GOLD_SEUL.matches(it) }
        .flatMap { token ->
            val correspondance = REGEX_QUANTITE_OBJET.find(token)
            val quantite = correspondance?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val nom = normaliserNomObjet(correspondance?.groupValues?.get(2) ?: token)
            val contenu = contenuPaquetages[nom]
            if (contenu != null) {
                List(quantite) { contenu }.flatten().flatMap(::decouperContenuPaquetage)
            } else {
                List(quantite) { nom }
            }
        }
}

private fun decouperContenuPaquetage(entree: String): List<String> =
    entree.split(REGEX_SEPARATEUR_PLUS).flatMap { morceau ->
        val correspondance = REGEX_QUANTITE_OBJET.find(morceau.trim())
        val quantite = correspondance?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val nom = (correspondance?.groupValues?.get(2) ?: morceau).trim()
        List(quantite) { nom }
    }

private val REGEX_SEPARATEUR_PLUS = Regex("""\s+\+\s+""")

/**
 * Sépare un nom d'objet combiné ("Arc court + 20 flèches") en ses objets individuels
 * ("Arc court", puis 20 × "Flèches"). Un nom simple est renvoyé tel quel.
 */
fun separerObjetsCombines(nom: String): List<String> {
    val morceaux = nom.split(REGEX_SEPARATEUR_PLUS).map { it.trim() }.filter { it.isNotEmpty() }
    if (morceaux.size <= 1) return listOf(nom)
    return morceaux.flatMap { morceau ->
        val correspondance = REGEX_QUANTITE_OBJET.find(morceau)
        val quantite = correspondance?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val nomObjet = normaliserNomObjet(correspondance?.groupValues?.get(2) ?: morceau)
        List(quantite) { nomObjet }
    }
}

private val REGEX_CONTENU_PAQUETAGE = Regex("""Contenu\s*:\s*(.+)""")

/**
 * Table "nom de paquetage normalisé" -> ses objets tels que listés dans
 * equipement_srd521.md (champ **Contenu**, cf. EquipmentParser), pour détailler
 * l'équipement de départ objet par objet plutôt que sous le seul nom du paquetage.
 */
fun construireContenuPaquetages(
    equipements: List<com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem>
): Map<String, List<String>> =
    equipements
        .filter { it.category.contains("aquetage", ignoreCase = true) }
        .mapNotNull { item ->
            val contenu = REGEX_CONTENU_PAQUETAGE.find(item.rawMarkdown)?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            item.name to contenu
        }
        .toMap()

// ---------------------------------------------------------------------------
// Tables markdown génériques ("Table de progression" des classes, entre autres)
// ---------------------------------------------------------------------------

data class TableMd(val entetes: List<String>, val lignes: List<List<String>>)

/** Parse une table markdown "| Col1 | Col2 |" (en-tête + séparateur "|---|---|" + lignes de données). */
fun parseTableMarkdown(texte: String): TableMd? {
    val lignesBrutes = texte.lineSequence().map { it.trim() }.filter { it.startsWith("|") }.toList()
    if (lignesBrutes.size < 2) return null
    fun decouperLigne(ligne: String) = ligne.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
    val entetes = decouperLigne(lignesBrutes[0])
    val lignes = lignesBrutes.drop(2).map(::decouperLigne) // drop(2) saute l'en-tête ET le séparateur "|---|"
    return TableMd(entetes, lignes)
}

/** Valeur d'une colonne pour la ligne dont la colonne "Niveau" vaut [niveau] (ex. "1"). */
fun TableMd.valeurNiveau(niveau: String, colonne: String): String? {
    val idxNiveau = entetes.indexOf("Niveau")
    val idxColonne = entetes.indexOf(colonne)
    if (idxNiveau < 0 || idxColonne < 0) return null
    return lignes.firstOrNull { it.getOrNull(idxNiveau) == niveau }?.getOrNull(idxColonne)
}

/** Classes lanceuses de sorts dès le niveau 1 (règles 2024 : le Paladin prépare 2 sorts dès le niveau 1). */
val CLASSES_LANCEUSES_NIVEAU_1 = setOf("Barde", "Clerc", "Druide", "Ensorceleur", "Magicien", "Occultiste", "Paladin")

/** Sorts de 1er niveau recommandés à la création, pré-cochés à l'étape Sorts. */
val SORTS_RECOMMANDES_NIVEAU_1: Map<String, List<String>> = mapOf("Paladin" to listOf("Héroïsme", "Châtiment de fournaise"))

// ---------------------------------------------------------------------------
// Balises d'espèce (especes_srd521.md) : règle universelle, aucun nom de trait ni
// d'espèce codé en dur. Voir l'en-tête du fichier pour la liste des balises :
//   <!-- id: competent; choix: 1; effet: competences; options: toutes -->   (choix)
//   <!-- id: resistance-naine; resistances: Poison -->                      (automatique)
//   <!-- option: Infernal; resistances: Feu; sorts: Trait de feu; sorts-3: ... -->
// ---------------------------------------------------------------------------

/** Clés d'effet reconnues dans une balise (trait automatique ou option choisie). */
object EffetBalise {
    const val COMPETENCES = "competences"
    const val RESISTANCES = "resistances"
    const val SORTS = "sorts"
    const val DON = "don"
    const val TAILLE = "taille"
    const val VITESSE = "vitesse"
    const val PV_PAR_NIVEAU = "pv-par-niveau"
    const val INCANTATION = "incantation"
    const val OPTION = "option"
    const val OUTILS = "outils"
    const val LANGUES = "langues"
    const val EQUIPEMENT = "equipement"
    const val NOTE = "note"
    // Compétence ou outil selon la valeur choisie (don Doué).
    const val COMPETENCES_OU_OUTILS = "competences-ou-outils"

    /** Libellé affiché sur la fiche devant la valeur choisie (ex. "Maîtrise choisie : Perception"). */
    fun libelleChoix(effet: String): String = when (effet) {
        COMPETENCES -> "Maîtrise choisie"
        DON -> "Don choisi"
        TAILLE -> "Taille choisie"
        INCANTATION -> "Caractéristique d'incantation choisie"
        OPTION -> "Option choisie"
        OUTILS -> "Outil maîtrisé choisi"
        SORTS -> "Sorts choisis"
        LANGUES -> "Langue choisie"
        EQUIPEMENT -> "Objet choisi"
        COMPETENCES_OU_OUTILS -> "Maîtrise choisie"
        else -> "Choix"
    }
}

/** Une option d'un choix "effet: option" (ligne `<!-- option: Nom; ... -->`) et ses effets. */
data class OptionEspece(val nom: String, val description: String, val effets: Map<String, String>)

/**
 * Un choix à faire pour l'espèce, issu d'une balise `choix: N`.
 * [raison] : pourquoi ce choix est demandé (texte du trait), affiché au joueur et noté sur la fiche.
 * [niveau] : niveau de personnage auquel le choix se fait (1 = création).
 */
data class ChoixBalise(
    val id: String,
    val nomTrait: String,
    val raison: String,
    val nombre: Int,
    val effet: String,
    val options: List<String>,
    val descriptions: Map<String, String> = emptyMap(),
    val recommande: String? = null,
    val niveau: Int = 1,
    // Libellé affiché sur la fiche devant la valeur (balise "libelle:"), sinon déduit de l'effet.
    val libelle: String? = null,
    // Texte de l'équipement de départ remplacé par la valeur choisie (balise "remplace:"),
    // ex. "instrument de musique au choix" -> "Luth".
    val remplace: String? = null,
    // Valeur imposée par la source (ex. "Initié à la magie (Clerc)" : liste Clerc) — affichée
    // sans pouvoir être changée.
    val impose: List<String>? = null,
    // Message quand les options dépendent d'un autre choix pas encore fait (ex. liste de sorts).
    val enAttente: String? = null,
) {
    /** Libellé devant la valeur choisie sur la fiche. */
    val libelleFiche: String get() = libelle ?: EffetBalise.libelleChoix(effet)
}

private val REGEX_BALISE = Regex("""<!--\s*(.*?)\s*-->""", RegexOption.DOT_MATCHES_ALL)

/** "k: v; k2: v2" -> map ordonnée (clés en minuscules). */
private fun lireBalise(contenu: String): Map<String, String> =
    contenu.split(";")
        .mapNotNull { morceau ->
            val idx = morceau.indexOf(':')
            if (idx <= 0) null else morceau.substring(0, idx).trim().lowercase() to morceau.substring(idx + 1).trim()
        }
        .toMap(linkedMapOf())

private fun balisesDe(texte: String): List<Map<String, String>> =
    REGEX_BALISE.findAll(texte).map { lireBalise(it.groupValues[1]) }.filter { it.isNotEmpty() }.toList()

private fun sansBalises(texte: String): String =
    texte.replace(REGEX_BALISE, "").replace(Regex("""\n{3,}"""), "\n\n").trim()

/** Clé d'une valeur dans une balise "options-<valeur>" : minuscules, sans accents ni espaces. */
private fun cleOption(valeur: String): String =
    java.text.Normalizer.normalize(valeur, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

private fun listeValeurs(valeur: String?): List<String> =
    valeur?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()

/**
 * Description d'une option trouvée dans le texte du trait : ligne de table dont une cellule vaut
 * [nom] (cellules suivantes, préfixées de leur en-tête, jusqu'à la prochaine colonne de même
 * en-tête que celle du nom), ou puce "- **Nom (...).** description".
 */
private fun descriptionOption(texteTrait: String, nom: String): String? {
    val table = parseTableMarkdown(texteTrait)
    if (table != null) {
        table.lignes.forEach { ligne ->
            val idx = ligne.indexOfFirst { it.equals(nom, ignoreCase = true) }
            if (idx >= 0) {
                val enteteNom = table.entetes.getOrNull(idx)
                val cellules = mutableListOf<String>()
                for (i in idx + 1 until ligne.size) {
                    if (table.entetes.getOrNull(i) == enteteNom) break
                    cellules += listOfNotNull(table.entetes.getOrNull(i)?.takeIf { it.isNotBlank() }, ligne[i]).joinToString(" : ")
                }
                return cellules.joinToString(" · ")
            }
        }
    }
    val puce = Regex("""(?m)^-\s*\*\*${Regex.escape(nom)}[^*]*\*\*\s*(.+)$""", RegexOption.IGNORE_CASE).find(texteTrait)
    return puce?.groupValues?.get(1)?.trim()
}

/** Résumé lisible des effets d'une option (ex. "Résistance : Feu · Sorts : Trait de feu"). */
private fun resumeEffets(effets: Map<String, String>): String = effets.entries.joinToString(" · ") { (cle, valeur) ->
    val libelle = when {
        cle == EffetBalise.RESISTANCES -> "Résistance"
        cle == EffetBalise.SORTS -> "Sorts"
        cle.startsWith("${EffetBalise.SORTS}-") -> "Sort au niveau ${cle.substringAfter('-')}"
        cle == EffetBalise.VITESSE -> "Vitesse"
        else -> cle
    }
    "$libelle : $valeur"
}

/**
 * Données externes dont un choix balisé peut tirer ses options : dons d'origines
 * (`options: dons-origines`), objets du fichier équipement (`options: equipement: Instrument de
 * musique | Artisan` : nom qui commence par le texte, ou catégorie qui le contient) et langues
 * (`options: langues`).
 */
data class ContexteChoix(
    val donsOrigines: List<Don> = emptyList(),
    val equipements: List<Pair<String, String>> = emptyList(), // (nom, catégorie)
    val langues: List<String> = emptyList(),
    val sorts: List<SortChoisissable> = emptyList(),
)

/** Un sort proposable dans un choix `options: sorts-mineurs` / `sorts-niveau-1` (niveau 0 = mineur). */
data class SortChoisissable(val nom: String, val niveau: Int, val classes: List<String>, val resume: String)

private val REGEX_CLASSES_SORT = Regex("""\*\*Classes\s*:\*\*\s*(.+)""")

/** Sort mineur ou du 1er niveau à partir d'une entrée détaillée de sorts_srd521.md (null sinon). */
fun sortChoisissable(nom: String, rawMarkdown: String): SortChoisissable? {
    val premiereLigne = rawMarkdown.lineSequence().firstOrNull().orEmpty()
    val niveau = when {
        premiereLigne.contains("mineur", ignoreCase = true) -> 0
        premiereLigne.contains("1er niveau", ignoreCase = true) -> 1
        else -> return null
    }
    val classes = REGEX_CLASSES_SORT.find(rawMarkdown)?.groupValues?.get(1).orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
    return SortChoisissable(nom, niveau, classes, premiereLigne.trim())
}

/**
 * Construit un choix à partir d'une balise `choix: N` ([b]), quelle que soit sa source (trait
 * d'espèce, historique, don, classe). [source] est le nom affiché (« Compétent », « Soldat »...),
 * [raison] le texte qui explique pourquoi ce choix est demandé.
 */
fun construireChoix(
    b: Map<String, String>,
    source: String,
    raison: String,
    ctx: ContexteChoix,
    niveau: Int = 1,
    optionsTrait: List<OptionEspece> = emptyList(),
    prefixeId: String = "",
    // Valeurs déjà retenues pour les autres choix de la même source (clé = id complet), pour une
    // balise "liste: <id>" dont les options dépendent d'un autre choix (ex. sorts de la liste choisie).
    valeursAutres: Map<String, List<String>> = emptyMap(),
): ChoixBalise? {
    val effet = b["effet"] ?: EffetBalise.OPTION
    // "selon: <id>" : les options dépendent de la valeur d'un autre choix de la même source,
    // listées par "options-<valeur>: ..." (ex. sorts de la lune choisie, « options-nuitari: ... »).
    val selon = b["selon"]
    val valeurSelon = selon?.let { valeursAutres[prefixeId + it]?.firstOrNull() }
    if (selon != null && valeurSelon == null) {
        return ChoixBalise(
            id = prefixeId + (b["id"] ?: source),
            nomTrait = source,
            raison = raison,
            nombre = b["choix"]?.toIntOrNull() ?: 1,
            effet = effet,
            options = emptyList(),
            libelle = b["libelle"],
            enAttente = "Faites d'abord le choix précédent.",
        )
    }
    val spec = (if (valeurSelon != null) b["options-${cleOption(valeurSelon)}"] else b["options"]).orEmpty().trim()
    // "options: sorts-mineurs" / "sorts-niveau-1" + "liste: initie-liste" : sorts de la classe choisie.
    val niveauSorts = when {
        spec.equals("sorts-mineurs", ignoreCase = true) -> 0
        spec.equals("sorts-niveau-1", ignoreCase = true) -> 1
        else -> null
    }
    if (niveauSorts != null) {
        val listeRef = b["liste"]
        // "liste: <id d'un autre choix>" (liste choisie par le joueur) ou "liste: Magicien" (liste
        // imposée, sans choix intermédiaire à une seule option).
        val classeFixe = listeRef?.takeIf { ref -> ctx.sorts.any { s -> s.classes.any { it.equals(ref, ignoreCase = true) } } }
        val classeSorts = listeRef?.let { valeursAutres[prefixeId + it]?.firstOrNull() } ?: classeFixe
        val sorts = ctx.sorts.filter { s ->
            s.niveau == niveauSorts && (listeRef == null || s.classes.any { it.equals(classeSorts, ignoreCase = true) })
        }
        return ChoixBalise(
            id = prefixeId + (b["id"] ?: source),
            nomTrait = source,
            raison = raison,
            nombre = b["choix"]?.toIntOrNull() ?: 1,
            effet = effet,
            options = sorts.map { it.nom },
            descriptions = sorts.associate { it.nom to it.resume },
            libelle = b["libelle"],
            // Tant que la liste de référence n'est pas choisie, il n'y a rien à proposer.
            enAttente = if (listeRef != null && classeSorts == null) "Choisissez d'abord la liste de sorts." else null,
        )
    }
    val (options, descriptions) = when {
        effet == EffetBalise.OPTION ->
            optionsTrait.map { it.nom } to optionsTrait.associate { o ->
                o.nom to listOf(o.description, resumeEffets(o.effets)).filter { it.isNotBlank() }.joinToString("\n")
            }
        spec.equals("dons-origines", ignoreCase = true) ->
            ctx.donsOrigines.map { it.nom } to ctx.donsOrigines.associate { it.nom to it.description }
        spec.equals("langues", ignoreCase = true) -> ctx.langues to emptyMap()
        else -> {
            // Plusieurs sources séparées par "|" : "toutes", "equipement: X", ou une liste "A, B".
            val liste = spec.split("|").map { it.trim() }.filter { it.isNotBlank() }.flatMap { morceau ->
                when {
                    morceau.equals("toutes", ignoreCase = true) -> TOUTES_COMPETENCES
                    morceau.startsWith("equipement:", ignoreCase = true) -> {
                        val filtre = morceau.substringAfter(':').trim()
                        ctx.equipements.filter { (nom, categorie) ->
                            nom.startsWith(filtre, ignoreCase = true) || categorie.contains(filtre, ignoreCase = true)
                        }.map { it.first }
                    }
                    else -> listeValeurs(morceau)
                }
            }.distinct()
            // Description de chaque option : compétence connue, sort (son résumé), ou puce
            // « - **Option** : texte » du texte de la source (ex. les lunes d'un don).
            liste to liste.mapNotNull { option ->
                val texte = DESCRIPTIONS_COMPETENCES[option]
                    ?: descriptionOption(raison, option)?.trimStart('—', '–', ':', ' ')
                    ?: ctx.sorts.firstOrNull { it.nom.equals(option, ignoreCase = true) }?.resume
                texte?.let { option to it }
            }.toMap()
        }
    }
    if (options.isEmpty()) return null
    return ChoixBalise(
        id = prefixeId + (b["id"] ?: source),
        nomTrait = source,
        raison = raison,
        nombre = b["choix"]?.toIntOrNull() ?: 1,
        effet = effet,
        options = options,
        descriptions = descriptions,
        recommande = b["recommande"],
        niveau = niveau,
        libelle = b["libelle"],
        remplace = b["remplace"],
    )
}

/** Tous les choix balisés de l'espèce (création et niveaux suivants). */
fun choixEspece(espece: Espece, donsOrigines: List<Don>, ctx: ContexteChoix = ContexteChoix(donsOrigines)): List<ChoixBalise> =
    espece.traits.flatMap { trait ->
        trait.balises.filter { it.containsKey("choix") }.mapNotNull { b ->
            construireChoix(b, trait.nom, trait.description, ctx, trait.niveau, trait.options)
        }
    }

/** Balises "choix-en-jeu" d'un trait : choix faits pendant la partie, jamais demandés à la création. */
fun choixEnJeu(balises: List<Map<String, String>>): List<String> = balises.mapNotNull { it["choix-en-jeu"] }

/** Ce que l'espèce apporte mécaniquement jusqu'au niveau [niveau], choix compris. */
data class EffetsEspece(
    val competences: List<String> = emptyList(),
    val resistances: List<String> = emptyList(),
    val sorts: List<String> = emptyList(),
    val dons: List<String> = emptyList(),
    val taille: String? = null,
    val vitesse: Double? = null,
    val pvParNiveau: Int = 0,
)

/**
 * Applique les balises automatiques des traits acquis à [niveau] et les options choisies
 * ([choix] : id du choix -> valeurs retenues). Les sorts `sorts-N` ne comptent que si N ≤ [niveau].
 */
fun effetsEspece(espece: Espece, choix: Map<String, List<String>>, niveau: Int = 1): EffetsEspece {
    val competences = mutableListOf<String>()
    val resistances = mutableListOf<String>()
    val sorts = mutableListOf<String>()
    val dons = mutableListOf<String>()
    var taille: String? = null
    var vitesse: Double? = null
    var pvParNiveau = 0

    fun appliquer(effets: Map<String, String>) {
        effets.forEach { (cle, valeur) ->
            when {
                cle == EffetBalise.COMPETENCES -> competences += listeValeurs(valeur)
                cle == EffetBalise.RESISTANCES -> resistances += listeValeurs(valeur)
                cle == EffetBalise.SORTS -> sorts += listeValeurs(valeur)
                cle.startsWith("${EffetBalise.SORTS}-") ->
                    if ((cle.substringAfter('-').toIntOrNull() ?: Int.MAX_VALUE) <= niveau) sorts += listeValeurs(valeur)
                cle == EffetBalise.DON -> dons += listeValeurs(valeur)
                cle == EffetBalise.TAILLE -> taille = valeur
                cle == EffetBalise.VITESSE -> vitesse = valeur.replace(",", ".").toDoubleOrNull() ?: vitesse
                cle == EffetBalise.PV_PAR_NIVEAU -> pvParNiveau += valeur.toIntOrNull() ?: 0
            }
        }
    }

    espece.traits.filter { it.niveau <= niveau }.forEach { trait ->
        trait.balises.forEach { b ->
            if (b.containsKey("choix")) {
                val valeurs = choix[b["id"] ?: trait.nom].orEmpty()
                when (val effet = b["effet"] ?: EffetBalise.OPTION) {
                    EffetBalise.OPTION -> valeurs.forEach { v -> trait.options.firstOrNull { it.nom == v }?.let { appliquer(it.effets) } }
                    else -> valeurs.forEach { v -> appliquer(mapOf(effet to v)) }
                }
            } else {
                appliquer(b - "id" - "niveau")
            }
        }
    }
    return EffetsEspece(
        competences.distinct(), resistances.distinct(), sorts.distinct(), dons.distinct(), taille, vitesse, pvParNiveau
    )
}

/**
 * Texte des traits d'espèce pour la fiche (section "Traits d'espèce") : une entrée par trait ;
 * pour un trait à choix, le titre porte la valeur choisie et le détail note le choix puis sa raison.
 */
fun traitsEspecePourFiche(espece: Espece, choix: Map<String, List<String>>, donsOrigines: List<Don> = emptyList()): String {
    val tousLesChoix = choixEspece(espece, donsOrigines)
    return espece.traits.mapNotNull { trait ->
        val choixDuTrait = tousLesChoix.filter { it.nomTrait == trait.nom }
        val retenus = choixDuTrait.mapNotNull { c -> choix[c.id]?.takeIf { it.isNotEmpty() }?.let { c to it } }
        val suffixeNiveau = if (trait.niveau > 1) " [niveau ${trait.niveau}]" else ""
        if (trait.base && retenus.isEmpty()) {
            null
        } else {
            entreeFicheAvecChoix(
                titre = "${trait.nom} (${espece.nom})$suffixeNiveau",
                retenus = retenus,
                pourquoi = "trait « ${trait.nom} » de l'espèce ${espece.nom}",
                description = trait.description,
                enJeu = choixEnJeu(trait.balises),
            )
        }
    }.joinToString("\n\n")
}

/**
 * Une entrée de fiche (trait, don, capacité) : sans choix, "Titre : description" ; avec choix, le
 * titre porte les valeurs retenues, puis le détail note chaque choix, sa raison, et le texte d'origine.
 * Les choix faits en jeu (balise "choix-en-jeu") sont rappelés sans être demandés.
 */
fun entreeFicheAvecChoix(
    titre: String,
    retenus: List<Pair<ChoixBalise, List<String>>>,
    pourquoi: String,
    description: String,
    enJeu: List<String> = emptyList(),
): String {
    val lignesEnJeu = enJeu.map { "Choix en jeu : $it" }
    if (retenus.isEmpty()) {
        return "$titre : " + (listOf(description) + lignesEnJeu).filter { it.isNotBlank() }.joinToString("\n")
    }
    val lignesChoix = retenus.map { (c, valeurs) ->
        "${c.libelleFiche} : ${valeurs.joinToString(", ")}" +
            valeurs.mapNotNull { v -> c.descriptions[v]?.takeIf { c.effet == EffetBalise.OPTION }?.let { "\n  $it" } }.joinToString("")
    }
    return "$titre — ${retenus.flatMap { it.second }.joinToString(", ")} : " +
        (lignesChoix + "Pourquoi ce choix : $pourquoi." + description + lignesEnJeu).filter { it.isNotBlank() }.joinToString("\n")
}

// ---------------------------------------------------------------------------
// Classes — classes_srd521.md
// ---------------------------------------------------------------------------

/** Une option d'une aptitude "choix-effet" (ex. Protecteur/Thaumaturge de l'Ordre divin du Clerc). */
data class OptionAptitude(val id: String?, val nom: String, val description: String)

/**
 * Une aptitude de classe telle que taguée dans classes_srd521.md :
 * `#### Niveau N[, M...] : Nom` suivi de `<!-- id: slug; type: T -->`.
 * [id]/[type] sont absents (id=null, type="automatique") si le tag est manquant,
 * pour rester tolérant à du contenu ajouté sans respecter le format.
 *
 * [type] vaut "automatique" (rien à choisir), "choix-generique" (Amélioration de
 * caractéristique, identique dans toutes les classes), "choix-sousclasse" (sélection de la
 * sous-classe au niveau indiqué), "choix-effet" (choix intégré à l'aptitude elle-même,
 * ex. Ordre divin du Clerc — voir [options], une liste "- **Nom** <!-- id: ... --> :
 * description." dans le texte de l'aptitude), ou "choix-expertise-N" (Expertise/Fin
 * explorateur : choix de N compétences déjà maîtrisées parmi celles du personnage, cf.
 * LevelUpDialog dans CharacterSheetScreen.kt et GameState.skillExpertise).
 */
data class AptitudeClasse(
    val niveaux: List<Int>,
    val id: String?,
    val type: String,
    val nom: String,
    val description: String,
    val options: List<OptionAptitude> = emptyList(),
    // Balises supplémentaires de l'aptitude (ex. choix de langue du Roublard, cf. construireChoix).
    val balises: List<Map<String, String>> = emptyList()
)

private val REGEX_NIVEAUX_APTITUDE = Regex("""^Niveaux?\s+([\d,\s]+?)\s*:\s*(.+)$""")
private val REGEX_META_APTITUDE = Regex("""<!--\s*id:\s*([\w-]+)\s*;\s*type:\s*([\w-]+)\s*-->""")
private val REGEX_OPTION_APTITUDE = Regex(
    """(?m)^-\s*\*\*(.+?)\*\*\s*(?:<!--\s*id:\s*([\w-]+)\s*-->)?\s*:\s*(.+)$"""
)

/** Parse une section "#### Niveau N[, M...] : Nom" (+ tag id/type optionnel) en [AptitudeClasse]. */
private fun parserAptitude(section: SectionMd): AptitudeClasse {
    val correspondance = REGEX_NIVEAUX_APTITUDE.find(section.titre)
    val niveaux = correspondance?.groupValues?.get(1)
        ?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?: emptyList()
    val nom = correspondance?.groupValues?.get(2)?.trim() ?: section.titre
    val meta = REGEX_META_APTITUDE.find(section.corps)
    // Balises en plus du tag id/type (les id seuls sont ceux des options "- **Nom** <!-- id -->").
    val balises = balisesDe(section.corps).filter { !it.containsKey("type") && it.size > 1 }
    val corpsSansMeta = section.corps.replace(REGEX_META_APTITUDE, "").trim()
    val options = REGEX_OPTION_APTITUDE.findAll(corpsSansMeta).map { m ->
        OptionAptitude(id = m.groupValues[2].ifBlank { null }, nom = m.groupValues[1].trim(), description = m.groupValues[3].trim())
    }.toList()
    // Description sans les lignes d'options (ne garde que l'intro, ex. "Choix entre deux ordres :").
    val description = sansBalises(
        corpsSansMeta.lineSequence()
            .filterNot { REGEX_OPTION_APTITUDE.matches(it) }
            .joinToString("\n")
    )
    return AptitudeClasse(
        niveaux = niveaux,
        id = meta?.groupValues?.get(1),
        type = meta?.groupValues?.get(2) ?: "automatique",
        nom = nom,
        description = description,
        options = options,
        balises = balises
    )
}

data class Classe(
    val id: String?,
    val nom: String,
    val description: String,
    val caracteristiquePrincipale: List<Caracteristique>,
    // Texte brut du champ "caracprinc" (ex. "Force ou Dextérité", "Force et Charisme") :
    // caracteristiquePrincipale seul ne distingue plus le "ou" (une caractéristique au choix,
    // ex. Guerrier) du "et" (toutes requises, ex. Paladin/Moine/Rôdeur), nécessaire pour
    // vérifier les prérequis de multiclassage (cf. remplitPrerequisMulticlasse).
    val caracteristiquePrincipaleTexte: String,
    val deDeVie: String, // ex. "d12"
    // Difficulté de prise en main de la classe (champ "difficulte" du SRD : Facile,
    // Intermédiaire ou Difficile), affichée à titre indicatif lors du choix de classe.
    val difficulte: String,
    val maitriseJetsSauvegarde: List<Caracteristique>,
    // Choix de compétences quand la classe est prise au niveau 1 (champ "maitcompA").
    val maitrisesCompetence: String,
    val maitrisesArme: String,
    val maitrisesOutils: String?,
    val formationArmures: String,
    val equipement: EquipementClasse,
    // Ce qu'un personnage gagne en multiclassant DANS cette classe (section "### Multiclassage"),
    // distinct des Traits de base ci-dessus qui ne s'appliquent qu'en classe de départ — cf.
    // Classe.remplitPrerequisMulticlasse pour le prérequis de caractéristique correspondant.
    val multiclassage: InfosMulticlasse,
    // Toutes les aptitudes de classe, tous niveaux confondus (table de progression comprise
    // via leurs niveaux). aptitudesNiveau1 reste dédié pour l'existant (résumé de fiche à la
    // création, niveau 1 uniquement).
    val aptitudes: List<AptitudeClasse>,
    val aptitudesNiveau1: List<AptitudeClasse>,
    // Sous-classes de la classe (titres "### Sous-classe : Nom"), celle du SRD et celles
    // importées (fusionnées par SrdRepository.loadClasses) : choisies à la montée de niveau
    // où l'aptitude "choix-sousclasse" est gagnée (cf. LevelUpDialog, CharacterSheetScreen.kt).
    val sousClasses: List<SousClasse> = emptyList(),
    // Colonnes "Sorts mineurs" / "Sorts préparés" de la table de progression, ligne
    // "Niveau 1" (cf. ClasseParser). 0 pour les classes non lanceuses de sorts, ou si
    // la table ne comporte pas ces colonnes.
    val sortsMineursNiveau1: Int = 0,
    val sortsPreparesNiveau1: Int = 0,
    // "complet" (Barde/Clerc/Druide/Ensorceleur/Magicien), "demi" (Paladin/Rôdeur), "pacte"
    // (Occultiste, table Magie de pacte séparée — cf. tableProgression), ou "aucun" (pas
    // d'emplacement de sort). Détermine comment calculer les emplacements de sort — voir
    // EmplacementsDeSort dans CharacterSheetScreen.kt.
    val typeIncantation: String = "aucun",
    // Table de progression brute (cf. ClasseParser), conservée pour lire, chez l'Occultiste,
    // les colonnes "Emplacements de sort" / "Niveau des emplacements" (Magie de pacte) niveau
    // par niveau — propre à cette classe, pas dans la table partagée des lanceurs complets.
    val tableProgression: TableMd? = null,
    // Balises des Traits de base (choix d'outils, d'objet d'équipement...) et de la section
    // Multiclassage (choix propres au multiclassage), cf. construireChoix.
    val balisesTraitsDeBase: List<Map<String, String>> = emptyList(),
    val balisesMulticlasse: List<Map<String, String>> = emptyList()
) {
    fun versOption() = OptionChoisie(id = id ?: nom.lowercase(), label = nom)

    /** Points de vie max au niveau 1 = valeur maximale du dé de vie + modificateur de Constitution. */
    fun pvNiveau1(modificateurConstitution: Int): Int {
        val valeurDe = deDeVie.removePrefix("d").toIntOrNull() ?: 0
        return valeurDe + modificateurConstitution
    }

    val estLanceurDeSorts: Boolean get() = nom in CLASSES_LANCEUSES_NIVEAU_1

    /**
     * Taille du grimoire de départ d'une classe à grimoire (Magicien : balise « grimoire-depart: 6 »
     * de son aptitude Sorts), null pour les autres classes.
     */
    val grimoireDepart: Int?
        get() = aptitudes.flatMap { it.balises }.firstNotNullOfOrNull { it["grimoire-depart"]?.trim()?.toIntOrNull() }

    /**
     * Prérequis de multiclassage SRD (13+ dans la/les caractéristique(s) principale(s)) :
     * "ou" dans le texte (ex. Guerrier "Force ou Dextérité") = une seule des deux suffit ;
     * "et" (ex. Paladin "Force et Charisme") = toutes requises. Une classe sans
     * caractéristique principale reconnue (contenu incomplet) ne bloque jamais le choix.
     */
    fun remplitPrerequisMulticlasse(valeurCaracteristique: (Caracteristique) -> Int): Boolean {
        if (caracteristiquePrincipale.isEmpty()) return true
        val estChoix = caracteristiquePrincipaleTexte.contains(" ou ", ignoreCase = true)
        return if (estChoix) caracteristiquePrincipale.any { valeurCaracteristique(it) >= 13 }
        else caracteristiquePrincipale.all { valeurCaracteristique(it) >= 13 }
    }
}

/**
 * Sous-classe d'une classe ("### Sous-classe : Nom") : ses aptitudes ("#### Niveau N : Nom",
 * même format que les aptitudes de classe) et ses sorts toujours préparés, lus dans une table
 * dont la 1re colonne est un niveau de classe et la 2de une liste de sorts séparés par des
 * virgules ("| Niveau de Paladin | Sorts |"), ou à défaut dans une liste
 * ("- **Niveau 3** : sort, sort") placée dans le texte d'une aptitude.
 */
data class SousClasse(
    val id: String?,
    val nom: String,
    val description: String,
    val aptitudes: List<AptitudeClasse>,
    val sortsParNiveau: Map<Int, List<String>> = emptyMap(),
) {
    /** Sorts toujours préparés accessibles à [niveauClasse] dans la classe (nom capitalisé). */
    fun sortsJusquAu(niveauClasse: Int): List<String> =
        sortsParNiveau.filterKeys { it <= niveauClasse }.toSortedMap().values.flatten()
}

private val LIGNE_SORTS_SOUS_CLASSE = Regex("""(?m)^\s*-\s*\*\*\s*Niveau\s+(\d+)\s*\*\*\s*:\s*(.+?)\s*$""")

private fun parserSousClasse(section: SectionMd): SousClasse {
    val aptitudes = decouperSections(section.corps, 4).map(::parserAptitude).filter { it.niveaux.isNotEmpty() }
    // Table des sorts : n'importe quelle table du corps dont la 1re colonne commence par "Niveau".
    val corpsCandidats = decouperSections(section.corps, 4).map { it.corps }.plus(section.corps.substringBefore("\n#### "))
    val lignesSorts: List<Pair<String, String>>? = corpsCandidats
        .firstNotNullOfOrNull { corps ->
            parseTableMarkdown(corps)?.takeIf { t -> t.entetes.firstOrNull()?.startsWith("Niveau", ignoreCase = true) == true }
        }
        ?.lignes?.map { it.getOrNull(0).orEmpty() to it.getOrNull(1).orEmpty() }
        // Sans table : 1re liste "- **Niveau 3** : sort, sort" trouvée dans le corps d'une section.
        ?: corpsCandidats.firstNotNullOfOrNull { corps ->
            LIGNE_SORTS_SOUS_CLASSE.findAll(corps).map { it.groupValues[1] to it.groupValues[2] }.toList().ifEmpty { null }
        }
    val sorts = lignesSorts.orEmpty().mapNotNull { (colNiveau, colSorts) ->
        val niveau = Regex("""\d+""").find(colNiveau)?.value?.toIntOrNull() ?: return@mapNotNull null
        niveau to colSorts.split(",").map { it.trim().trim('*', '_').replaceFirstChar(Char::uppercase) }
            .filter { it.isNotBlank() }
    }.toMap()
    val intro = section.corps.substringBefore("\n#### ").lineSequence()
        .filterNot { it.trim().startsWith("|") || it.trim().matches(Regex("""-\s*classe\s*:.*""", RegexOption.IGNORE_CASE)) }
        // Champs "- devise : ..." / "- description : ..." : seule la valeur est gardée.
        .map { it.replace(Regex("""^\s*-\s*(devise|description)\s*:\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^\s*-\s*pr[ée]ceptes\s*:\s*""", RegexOption.IGNORE_CASE), "Préceptes : ") }
        .joinToString("\n")
    return SousClasse(
        id = extraireId(section.corps),
        nom = section.titre.removePrefix("Sous-classe").trimStart(' ', ':').trim(),
        description = sansBalises(intro).removeSuffix("---").trim(),
        aptitudes = aptitudes,
        sortsParNiveau = sorts,
    )
}

private fun nomVersCaracteristique(nom: String): Caracteristique? =
    Caracteristique.entries.firstOrNull { it.label.equals(nom.trim(), ignoreCase = true) }

/** Sections de classes_srd521.md qui ne décrivent pas une classe (tables/annexes globales). */
private val SECTIONS_HORS_CLASSES = listOf("Tableau récapitulatif", "Emplacements de sort")

object ClasseParser {
    fun parse(contenuMd: String): List<Classe> =
        decouperSections(contenuMd, 2)
            .filterNot { section -> SECTIONS_HORS_CLASSES.any { section.titre.startsWith(it) } }
            .mapNotNull { section ->
                val sousSections = decouperSections(section.corps, 3)
                val corpsTraitsDeBase = sousSections.firstOrNull { it.titre == "Traits de base" }?.corps
                    ?: return@mapNotNull null
                val champs = extraireChamps(corpsTraitsDeBase)
                val champsMulticlasse = sousSections.firstOrNull { it.titre == "Multiclassage" }
                    ?.corps?.let(::extraireChamps) ?: emptyMap()

                val toutesAptitudes = sousSections.firstOrNull { it.titre == "Aptitudes de classe" }
                    ?.let { decouperSections(it.corps, 4) }
                    ?.map(::parserAptitude)
                    ?: emptyList()

                val tableProgression = sousSections.firstOrNull { it.titre == "Table de progression" }
                    ?.corps
                    ?.let(::parseTableMarkdown)

                Classe(
                    id = extraireId(section.corps),
                    nom = section.titre,
                    description = champs["desclas"].orEmpty(),
                    caracteristiquePrincipale = champs["caracprinc"]
                        ?.let { decouperListe(it).mapNotNull(::nomVersCaracteristique) } ?: emptyList(),
                    caracteristiquePrincipaleTexte = champs["caracprinc"].orEmpty(),
                    deDeVie = Regex("d\\d+").find(champs["dv"].orEmpty())?.value ?: "",
                    difficulte = champs["difficulte"].orEmpty(),
                    maitriseJetsSauvegarde = champs["jssauv"]
                        ?.let { decouperListe(it).mapNotNull(::nomVersCaracteristique) } ?: emptyList(),
                    maitrisesCompetence = champs["maitcompA"].orEmpty(),
                    maitrisesArme = champs["maitarme"].orEmpty(),
                    maitrisesOutils = champs["maitoutils"],
                    formationArmures = champs["formarm"].orEmpty(),
                    equipement = extraireEquipementClasse(champs),
                    multiclassage = extraireInfosMulticlasse(champsMulticlasse),
                    aptitudes = toutesAptitudes,
                    aptitudesNiveau1 = toutesAptitudes.filter { 1 in it.niveaux },
                    sousClasses = sousSections.filter { it.titre.startsWith("Sous-classe") }.map(::parserSousClasse),
                    sortsMineursNiveau1 = tableProgression?.valeurNiveau("1", "Sorts mineurs")?.toIntOrNull() ?: 0,
                    sortsPreparesNiveau1 = tableProgression?.valeurNiveau("1", "Sorts préparés")?.toIntOrNull() ?: 0,
                    typeIncantation = champs["typeincantation"] ?: "aucun",
                    tableProgression = tableProgression,
                    balisesTraitsDeBase = balisesDe(corpsTraitsDeBase).filter { it.containsKey("id") },
                    balisesMulticlasse = sousSections.firstOrNull { it.titre == "Multiclassage" }?.corps
                        ?.let(::balisesDe)?.filter { it.containsKey("id") }.orEmpty()
                )
            }
}

/**
 * Table partagée "## Emplacements de sort (lanceurs complets)" de classes_srd521.md :
 * emplacements de sort par niveau de sort (1er à 9e), indexée par niveau de lanceur
 * effectif (somme des niveaux des classes "complet" + moitié arrondie à l'inférieur des
 * classes "demi", cf. Classe.typeIncantation). Table unique pour éviter de la dupliquer
 * dans chacune des 7 classes concernées — voir EmplacementsDeSort dans
 * CharacterSheetScreen.kt pour son utilisation sur la fiche de personnage.
 */
object EmplacementsDeSortParser {
    fun parse(contenuMd: String): TableMd? =
        decouperSections(contenuMd, 2)
            .firstOrNull { it.titre.startsWith("Emplacements de sort") }
            ?.corps
            ?.let(::parseTableMarkdown)
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
    val equipementB: String,
    // Balises de choix (ex. type de boîte de jeux du Soldat), cf. construireChoix.
    val balises: List<Map<String, String>> = emptyList()
) {
    fun versOption() = OptionChoisie(id = id ?: nom.lowercase(), label = nom)

    /** Nom du don sans sa précision : "Initié à la magie (Clerc)" -> "Initié à la magie". */
    val donNom: String get() = don.substringBefore("(").trim()

    /** Précision entre parenthèses du don : "Initié à la magie (Clerc)" -> "Clerc" (null sinon). */
    val donPrecision: String? get() = Regex("""\(([^)]+)\)""").find(don)?.groupValues?.get(1)?.trim()
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
                equipementB = REGEX_EQUIP_B.find(section.corps)?.groupValues?.get(1)?.trim().orEmpty(),
                balises = balisesDe(section.corps).filter { it.size > 1 && it.containsKey("id") }
            )
        }
}

// ---------------------------------------------------------------------------
// Espèces — especes_srd521.md
// ---------------------------------------------------------------------------

/**
 * Trait d'espèce. [description] est le texte sans balises ; [balises] ses balises `<!-- id: ... -->`
 * (choix ou effets automatiques), [options] ses lignes `<!-- option: ... -->`, [niveau] le niveau de
 * personnage à partir duquel il s'applique (balise `niveau:`). [base] : pseudo-trait portant les
 * balises des champs de base de l'espèce (ex. catégorie de taille), pas un trait à part entière.
 */
data class Trait(
    val nom: String,
    val description: String,
    val balises: List<Map<String, String>> = emptyList(),
    val options: List<OptionEspece> = emptyList(),
    val niveau: Int = 1,
    val base: Boolean = false,
)

data class Espece(
    val id: String?,
    val nom: String,
    val typeCreature: String,
    val taille: String,
    val vitesse: String,
    val traits: List<Trait>,
    // Paragraphe de présentation écrit sous les champs de base (espèce importée, ex. le kender) ;
    // vide pour les espèces du SRD, présentées par l'assistant de création.
    val description: String = "",
) {
    fun versOption() = OptionChoisie(id = id ?: nom.lowercase(), label = nom)
}

object EspeceParser {
    private fun parserTrait(nom: String, corps: String, base: Boolean = false): Trait {
        val balises = balisesDe(corps)
        // Le dernier trait d'une espèce se termine par le séparateur "---" avant la suivante.
        val description = sansBalises(corps).removeSuffix("---").trim()
        val options = balises.filter { it.containsKey("option") }.map { b ->
            val nomOption = b.getValue("option")
            OptionEspece(nomOption, descriptionOption(description, nomOption).orEmpty(), b - "option")
        }
        val balisesTrait = balises.filter { it.containsKey("id") && !it.containsKey("option") }
        return Trait(
            nom = nom,
            description = description,
            balises = balisesTrait,
            options = options,
            niveau = balisesTrait.firstNotNullOfOrNull { it["niveau"]?.toIntOrNull() } ?: 1,
            base = base,
        )
    }

    fun parse(contenuMd: String): List<Espece> =
        decouperSections(contenuMd, 2).map { section ->
            val champs = extraireChamps(section.corps)
            // Balises des champs de base (avant "### Traits"), hors l'id de l'espèce elle-même. Une
            // espèce importée n'a pas de "### Traits spéciaux" (un "### " y couperait l'entrée,
            // cf. CustomContentParser) : ses champs s'arrêtent alors au premier trait "#### ",
            // sans quoi les balises de tous ses traits étaient comptées une seconde fois.
            val corpsBase = if ("\n### " in section.corps) section.corps.substringBefore("\n### ")
            else section.corps.substringBefore("\n#### ")
            val traitBase = balisesDe(corpsBase).filter { it.size > 1 && it.containsKey("id") }
                .takeIf { it.isNotEmpty() }
                ?.let { balises ->
                    Trait(
                        nom = "Catégorie de taille",
                        description = champs["Catégorie de taille"].orEmpty(),
                        balises = balises,
                        base = true,
                    )
                }
            val traits = listOfNotNull(traitBase) + decouperSections(section.corps, 4).map { t -> parserTrait(t.titre, t.corps) }
            Espece(
                id = extraireId(section.corps),
                nom = section.titre,
                typeCreature = champs["Type de créature"].orEmpty(),
                taille = champs["Catégorie de taille"].orEmpty(),
                vitesse = champs["Vitesse"].orEmpty(),
                traits = traits,
                description = sansBalises(corpsBase).lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("-") && !it.startsWith("#") && it != "---" }
                    .joinToString(" "),
            )
        }
}

// ---------------------------------------------------------------------------
// Dons — dons_srd521.md (utile pour afficher le don d'un historique, et pour
// le trait "Polyvalent" de l'Humain qui laisse choisir n'importe quel don d'origines)
// ---------------------------------------------------------------------------

/** [balises] : balises de choix du don (ex. liste de sorts et caractéristique d'Initié à la magie). */
data class Don(
    val id: String?,
    val nom: String,
    val categorie: String,
    val description: String,
    val balises: List<Map<String, String>> = emptyList(),
)

object DonParser {
    private val REGEX_ID_INLINE = Regex("""<!--.*?-->""")

    /**
     * Dons tels que chargés par SrdRepository.loadDons (une entrée par "### Nom" de
     * dons_srd521.md) : la catégorie vient de la balise `categorie:` (ex. "Origines"), les
     * choix des autres balises du don.
     */
    fun depuisEntrees(entrees: List<Pair<String, String>>): List<Don> = entrees.map { (nom, contenu) ->
        val balises = balisesDe(contenu)
        Don(
            id = balises.firstNotNullOfOrNull { it["id"] },
            nom = nom,
            categorie = balises.firstNotNullOfOrNull { it["categorie"] }.orEmpty(),
            description = sansBalises(contenu),
            balises = balises.filter { it.containsKey("choix") },
        )
    }

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