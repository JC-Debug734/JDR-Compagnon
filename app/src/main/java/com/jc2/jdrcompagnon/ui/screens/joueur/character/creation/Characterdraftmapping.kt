package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.cumulerMaitrisesCompetences

/**
 * Convertit le brouillon rempli par le wizard en [Character] réel, prêt pour
 * GameState.addCharacter(...). Le nom est saisi en toute dernière étape du wizard
 * (CreationStep.NOM, cf. Charactercreationwizard.kt) et vit dans
 * `draft.nomPersonnage`.
 *
 * `background` reçoit le nom de l'historique SRD (Acolyte/Criminel/Sage/Soldat) :
 * CharacterSheetScreen l'affiche sous le libellé "Historique" (onglet Notes,
 * DetailRow). Le sous-choix d'espèce (ex. type de dragon) et les traits d'espèce
 * vont dans `traits` (section "Traits d'espèce" de l'onglet Notes), les aptitudes
 * de classe de niveau 1 dans `classFeatures` (section "Capacités de classe"), et
 * le don d'historique dans `feats` (section "Dons"). `languages` (carte "Langues
 * connues") et `proficiencies` (carte "Maîtrises", outils de l'historique) sont
 * dédiés. `weaponArmorTraining` (armes/armures de la classe) va dans l'onglet
 * Combat.
 *
 * `backpackItems` (sac à dos affiché dans l'onglet Équipement de la fiche, cf.
 * EquipmentManagementContent) est reconstruit objet par objet à partir du texte
 * d'équipement choisi (classe + historique), via [parseListeEquipement] : une entrée
 * de liste par unité (ex. "2 dagues" -> deux entrées "Dague"), l'or ("N po") étant
 * exclu de cette liste et déjà compté à part dans `gold`/`orDepart`. Le texte brut
 * reste aussi dans `equipment` pour référence.
 *
 * Champ non couvert par ce mapping, car le wizard ne propose pas encore ce choix :
 * - `weapons` / `armor` (emplacements déjà équipés) : les objets créés atterrissent
 *   tous dans le sac à dos, le joueur les équipe ensuite lui-même depuis la fiche.
 */
fun CharacterDraft.versCharacter(
    worldId: String,
    createdBy: String = "Joueur",
    // cf. construireContenuPaquetages (Srdcreationparsers.kt) : permet de détailler un
    // paquetage choisi comme équipement de départ (ex. "Explorateur") en ses objets
    // réels plutôt que de l'ajouter au sac à dos comme un unique objet nommé "Explorateur".
    contenuPaquetages: Map<String, List<String>> = emptyMap(),
    // Dons d'origines (dons_srd521.md) : description du don choisi via une balise "effet: don".
    donsOrigines: List<Don> = emptyList(),
    // Choix balisés de la classe, de l'historique et des dons (étape CHOIX_COMPLEMENTAIRES)
    // avec leurs valeurs retenues, cf. CharacterCreationStateHolder.choixComplementaires.
    choixAutres: List<Pair<ChoixBalise, List<String>>> = emptyList(),
    // Dons reçus (historique, espèce) et leur origine, cf. CharacterCreationStateHolder.donsRecus.
    donsRecus: List<Pair<Don, String>> = emptyList()
): Character {
    val choixFaits = choixAutres.filter { it.second.isNotEmpty() }
    fun valeursEffet(prefixe: String, vararg effets: String) =
        choixFaits.filter { (c, _) -> c.id.startsWith(prefixe) && c.effet in effets }.flatMap { it.second }

    // "instrument de musique au choix" -> "Luth" : la balise "remplace:" d'un choix substitue
    // l'objet choisi dans le texte de l'équipement de départ (s'il y figure).
    fun remplacerChoix(texte: String?): String? = texte?.let { t ->
        choixFaits.fold(t) { acc, (c, valeurs) ->
            c.remplace?.let { r -> acc.replace(r, valeurs.joinToString(", "), ignoreCase = true) } ?: acc
        }
    }
    val equipementClasse = remplacerChoix(equipementClasseTexte)
    val equipementHistorique = remplacerChoix(equipementHistoriqueTexte)

    // Doué & co : une maîtrise choisie est une compétence si c'en est une, sinon un outil.
    val maitrisesDons = valeursEffet("don:", EffetBalise.COMPETENCES, EffetBalise.COMPETENCES_OU_OUTILS)
    val competencesDons = maitrisesDons.filter { it in TOUTES_COMPETENCES }
    val outilsDons = maitrisesDons.filterNot { it in TOUTES_COMPETENCES } + valeursEffet("don:", EffetBalise.OUTILS)
    val outilsClasse = valeursEffet("classe:", EffetBalise.OUTILS)
    val outilsHistorique = valeursEffet("historique:", EffetBalise.OUTILS)
    // Langues données d'office par la classe (balise "langues:" sans choix, ex. Argot des voleurs).
    val languesClasse = classe?.let { c ->
        (c.balisesTraitsDeBase + c.aptitudesNiveau1.flatMap { it.balises })
            .filter { !it.containsKey("choix") }
            .flatMap { b -> b[EffetBalise.LANGUES]?.split(",")?.map { it.trim() }.orEmpty() }
    }.orEmpty()
    // Effets balisés de l'espèce (automatiques + choix), cf. effetsEspece / especes_srd521.md.
    val effetsEspece = espece?.let { effetsEspece(it, especeChoix, niveau = 1) } ?: EffetsEspece()

    val modConstitution = modificateurs[Caracteristique.CONSTITUTION] ?: 0
    val pv = (classe?.pvNiveau1(modConstitution) ?: 10) + effetsEspece.pvParNiveau

    val vitesse = effetsEspece.vitesse?.toInt()
        ?: espece?.vitesse
            ?.replace(",", ".")
            ?.let { Regex("""[\d.]+""").find(it)?.value?.toDoubleOrNull()?.toInt() }
        ?: 9

    // Une entrée par ligne VIDE ("\n\n"), jamais un simple retour à la ligne : une
    // description sur plusieurs lignes (ex. liste à puces d'une aptitude) doit rester une
    // seule entrée pour l'affichage "une capacité = une ligne + détails" de la fiche
    // (cf. parserCapacites dans CharacterSheetScreen.kt). Un trait à choix porte la valeur
    // choisie dans son titre, puis le choix et sa raison en détail (traitsEspecePourFiche).
    val traitsTexte = espece?.let { traitsEspecePourFiche(it, especeChoix, donsOrigines) }.orEmpty()

    val classFeaturesTexte = classe?.aptitudesNiveau1.orEmpty().joinToString("\n\n") { a ->
        // Aptitude à choix intégré (ex. Ordre divin : Protecteur/Thaumaturge) : la ligne
        // porte directement l'option retenue à la création, pas juste l'intro générique
        // ("Choix entre deux ordres :") — cf. CreationStep.CLASSE_APTITUDE_CHOIX.
        val choix = a.id?.let { classeChoixNiveau1[it] }
        val option = choix?.let { nom -> a.options.firstOrNull { it.nom == nom } }
        val langueEnPlus = a.balises.any { it.containsKey("choix") && it["effet"] == EffetBalise.LANGUES }
            .takeIf { it }?.let { "\nLangue choisie en plus à l'étape Langues : voir Langues connues." }.orEmpty()
        if (choix != null) {
            "${a.nom} (${classe?.nom}) — $choix : ${option?.description.orEmpty()}$langueEnPlus"
        } else {
            "${a.nom} (${classe?.nom}) : ${a.description}$langueEnPlus"
        }
    }

    // Dons reçus : chacun avec ses choix (ex. Initié à la magie — Clerc, Sagesse) et leur raison.
    val nomsDonsDecrits = donsRecus.map { it.first.nom }.toSet()
    val featsTexte = (
        donsRecus.map { (don, origine) ->
            entreeFicheAvecChoix(
                titre = "${don.nom} ($origine)",
                retenus = choixFaits.filter { it.first.id.startsWith("don:${don.nom}:") },
                pourquoi = "$origine",
                description = don.description,
            )
        } +
            // Repli : don introuvable dans dons_srd521.md, seul son nom est connu.
            listOfNotNull(historique?.don?.ifBlank { null }?.takeIf { historique.donNom !in nomsDonsDecrits }?.let { "Don d'historique : $it" }) +
            effetsEspece.dons.filterNot { it in nomsDonsDecrits }.map { nom ->
                val description = donsOrigines.firstOrNull { it.nom == nom }?.description.orEmpty()
                "$nom (don d'espèce ${espece?.nom}) : $description".trimEnd(' ', ':')
            }
        ).joinToString("\n\n")

    val equipementTexte = listOfNotNull(equipementClasse, equipementHistorique)
        .joinToString(", ")

    val objetsSacADos = parseListeEquipement(equipementClasse.orEmpty(), contenuPaquetages) +
        parseListeEquipement(equipementHistorique.orEmpty(), contenuPaquetages)

    // Une compétence accordée par deux sources (historique, classe, espèce, don) passe en Expertise.
    val (competencesFinales, expertisesFinales) = cumulerMaitrisesCompetences(
        emptyList(),
        emptyList(),
        historique?.maitrisesCompetence.orEmpty() + competencesClasse + effetsEspece.competences + competencesDons,
    )

    // Outils maîtrisés : le texte d'origine ("Choisissez un type de boîte de jeux") est remplacé
    // par l'outil choisi, avec la mention du choix.
    // Seulement les outils maîtrisés (le choix fait remplace le texte « Choisissez… »).
    val proficiencesTexte = (
        listOfNotNull(historique?.maitriseOutils?.ifBlank { null }?.let { texte -> if (outilsHistorique.isEmpty()) texte else null }) +
            outilsHistorique + outilsDons
        ).distinct().joinToString(", ")

    val armesArmuresTexte = classe?.let { c ->
        listOfNotNull(
            "Armes : ${c.maitrisesArme}".takeIf { c.maitrisesArme.isNotBlank() },
            "Armures : ${c.formationArmures}".takeIf { c.formationArmures.isNotBlank() },
            c.maitrisesOutils?.ifBlank { null }?.let { texte ->
                if (outilsClasse.isEmpty()) "Outils : $texte"
                else "Outils : ${outilsClasse.joinToString(", ")} (choix : « $texte »)"
            }
        ).joinToString("\n")
    }.orEmpty()

    return Character(
        name = nomPersonnage.orEmpty(),
        type = "PJ",
        worldId = worldId,
        characterClass = classe?.nom.orEmpty(),
        race = espece?.nom.orEmpty(),
        alignment = alignement?.label.orEmpty(),
        background = historique?.nom.orEmpty(),
        strength = valeursFinales[Caracteristique.FORCE] ?: 10,
        dexterity = valeursFinales[Caracteristique.DEXTERITE] ?: 10,
        constitution = valeursFinales[Caracteristique.CONSTITUTION] ?: 10,
        intelligence = valeursFinales[Caracteristique.INTELLIGENCE] ?: 10,
        wisdom = valeursFinales[Caracteristique.SAGESSE] ?: 10,
        charisma = valeursFinales[Caracteristique.CHARISME] ?: 10,
        maxHitPoints = pv,
        currentHitPoints = pv,
        speed = vitesse,
        gold = orDepart,
        equipment = equipementTexte,
        backpackItems = objetsSacADos,
        proficiencyBonus = BONUS_MAITRISE_NIVEAU_1,
        savingThrowProficiencies = classe?.maitriseJetsSauvegarde?.map { it.label } ?: emptyList(),
        skillProficiencies = competencesFinales,
        skillExpertise = expertisesFinales,
        spells = (sortsMineursChoisis + sortsChoisis + effetsEspece.sorts + valeursEffet("don:", EffetBalise.SORTS)).distinct(),
        // D'où vient chaque sort (affiché sur sa carte) : le premier gain l'emporte.
        spellSources = buildMap {
            sortsMineursChoisis.forEach { putIfAbsent(it, "${classe?.nom.orEmpty()} niv. 1 — sort mineur") }
            sortsChoisis.forEach { putIfAbsent(it, "${classe?.nom.orEmpty()} niv. 1") }
            effetsEspece.sorts.forEach { putIfAbsent(it, "Espèce : ${espece?.nom.orEmpty()}") }
            choixFaits.filter { (c, _) -> c.id.startsWith("don:") && c.effet == EffetBalise.SORTS }
                .forEach { (c, valeurs) -> valeurs.forEach { putIfAbsent(it, "Don : ${c.nomTrait}") } }
        },
        // Classe à grimoire (Magicien) : seuls les premiers sorts du grimoire sont préparés.
        preparedSpells = classe?.takeIf { it.grimoireDepart != null }?.let { sortsChoisis.take(it.sortsPreparesNiveau1) },
        damageResistances = effetsEspece.resistances,
        speciesChoices = especeChoix,
        size = effetsEspece.taille.orEmpty(),
        traits = traitsTexte,
        classFeatures = classFeaturesTexte,
        feats = featsTexte,
        personalityTraits = histoirePersonnalite,
        languages = (langues.map { it.nom } + languesClasse).distinct(),
        proficiencies = proficiencesTexte,
        weaponArmorTraining = armesArmuresTexte,
        // Bottes d'arme par défaut de la classe (Paladin : épée longue, javeline), modifiables au repos long.
        weaponMasteries = ArsenalPersonnage.bottesParDefaut[classe?.nom].orEmpty(),
        portrait = portrait.orEmpty(),
        createdBy = createdBy
        // `size` vide (espèce sans balise "taille") : Character le déduit de `race` via
        // sizeForRace ailleurs dans le code.
    )
}