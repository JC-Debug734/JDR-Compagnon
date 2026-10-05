package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import androidx.compose.foundation.Image
import com.jc2.jdrcompagnon.ui.components.SortApercuCarte
import com.jc2.jdrcompagnon.ui.components.FiltreSortsBarre
import com.jc2.jdrcompagnon.ui.components.rememberFiltreSorts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.screens.joueur.characterPortraitOptions
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry

/**
 * Écran principal du wizard. À brancher dans NavGraph.kt sur une route dédiée
 * (ex: Routes.CHARACTER_CREATION), avec onTermine appelant la conversion
 * CharacterDraft -> votre modèle Character existant puis la sauvegarde via GameState.
 *
 * Exemple d'appel côté NavGraph/écran parent, chargement via SrdRepository :
 *
 *   val classes = ClasseParser.parse(SrdRepository.loadClasses(context, worldId).joinToString("\n\n") { it.rawMarkdown })
 *   val historiques = HistoriqueParser.parse(SrdRepository.loadHistoriques(context, worldId).joinToString("\n\n") { it.rawMarkdown })
 *   val especes = EspeceParser.parse(SrdRepository.loadEspeces(context, worldId).joinToString("\n\n") { it.rawMarkdown })
 *   val langues = LangueParser.parse(SrdRepository.loadLangues(context, worldId))
 *   val sortsNiveau1 = SrdRepository.loadSpells(context, worldId).filter { val l = it.rawMarkdown.lineSequence().firstOrNull().orEmpty(); l.contains("mineur", true) || l.contains("1er niveau", true) }
 *   CharacterCreationWizard(classes = classes, historiques = historiques, especes = especes, langues = langues, sortsDisponibles = sortsNiveau1, ...)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationWizard(
    holder: CharacterCreationStateHolder = remember { CharacterCreationStateHolder() },
    // Chargées une fois par l'appelant (SrdRepository) via ClasseParser / HistoriqueParser /
    // EspeceParser / LangueParser (cf. SrdCreationParsers.kt). Le wizard ne connaît plus
    // aucune donnée SRD en dur : tout bloc ajouté dans ces fichiers (nouvelle classe,
    // nouvel historique, nouvelle espèce, fichier remplacé avec plus de choix) apparaît
    // automatiquement sans toucher à cet écran.
    classes: List<Classe>,
    historiques: List<Historique>,
    especes: List<Espece>,
    langues: List<Langue>,
    // Sorts mineurs + sorts de niveau 1 (cf. SrdRepository.loadSpellsIndex, filtré en amont).
    // Liste vide acceptable : l'étape SORTS est de toute façon sautée pour les classes
    // non lanceuses, et si la liste est vide pour une classe lanceuse le joueur passera
    // l'étape sans rien choisir plutôt que de planter.
    // Sorts mineurs + sorts de niveau 1 (entrées détaillées de SrdRepository.loadSpells,
    // filtrées en amont sur le niveau ; voir CharacterCreationScreen). Liste vide
    // acceptable : l'étape SORTS est de toute façon sautée pour les classes non
    // lanceuses, et si la liste est vide pour une classe lanceuse le joueur passe
    // l'étape sans rien choisir plutôt que de bloquer.
    sortsDisponibles: List<SrdEntry> = emptyList(),
    // Dons d'origines (dons_srd521.md, catégorie "Origines"), pour le choix de don
    // du trait "Polyvalent" de l'Humain à l'étape ESPECE_CHOIX (nom + description).
    donsOriginesNoms: List<Don> = emptyList(),
    // Tous les dons + objets d'équipement et langues dont les choix balisés tirent leurs options.
    tousLesDons: List<Don> = emptyList(),
    contexteChoix: ContexteChoix = ContexteChoix(),
    onTermine: (CharacterDraft) -> Unit,
    onAnnuler: () -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    val state by holder.uiState.collectAsState()
    LaunchedEffect(donsOriginesNoms, tousLesDons, contexteChoix) {
        holder.donsOriginesNoms = donsOriginesNoms
        holder.tousLesDons = tousLesDons
        holder.contexte = contexteChoix
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Création de personnage — ${state.step.label}") },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                LinearProgressIndicator(
                    progress = { state.progression },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Rappel persistant de la classe choisie, affiché sur toutes les étapes suivantes
            // (une fois CLASSE confirmée) pour garder la caractéristique principale associée
            // sous les yeux pendant le reste de la création — sans avoir à revenir en arrière.
            state.draft.classe?.takeIf { state.step != CreationStep.CLASSE }?.let { classe ->
                ClasseRappelBandeau(classe)
                Spacer(Modifier.height(12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                when (state.step) {
                    CreationStep.CLASSE -> EtapeClasse(state, holder, classes)
                    CreationStep.COMPETENCES_CLASSE -> EtapeCompetencesClasse(state, holder)
                    CreationStep.CLASSE_APTITUDE_CHOIX -> EtapeClasseAptitudeChoix(state, holder)
                    CreationStep.ESPECE -> EtapeEspece(state, holder, especes)
                    CreationStep.ESPECE_CHOIX -> EtapeEspeceChoix(state, holder)
                    CreationStep.CHOIX_COMPLEMENTAIRES -> EtapeChoixComplementaires(state, holder, sortsDisponibles)
                    CreationStep.HISTORIQUE -> EtapeHistorique(state, holder, historiques)
                    CreationStep.LANGUES -> EtapeLangues(state, holder, langues)
                    CreationStep.METHODE_CARACTERISTIQUES -> EtapeMethodeCaracteristiques(state, holder)
                    CreationStep.VALEURS_CARACTERISTIQUES -> EtapeValeursCaracteristiques(state, holder)
                    CreationStep.REPARTITION_CARACTERISTIQUES -> EtapeRepartitionCaracteristiques(state, holder)
                    CreationStep.AJUSTEMENT_HISTORIQUE -> EtapeAjustementHistorique(state, holder)
                    CreationStep.ALIGNEMENT -> EtapeAlignement(state, holder)
                    CreationStep.EQUIPEMENT_CLASSE -> EtapeEquipementClasse(state, holder)
                    CreationStep.EQUIPEMENT_HISTORIQUE -> EtapeEquipementHistorique(state, holder)
                    CreationStep.SORTS -> EtapeSorts(state, holder, sortsDisponibles)
                    CreationStep.NOM -> EtapeNom(state, holder)
                    CreationStep.PORTRAIT -> EtapePortrait(state, holder)
                    CreationStep.RECAPITULATIF -> EtapeRecapitulatif(state, holder, onTermine)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.peutReculer) {
                    OutlinedButton(onClick = { holder.revenirEtapePrecedente() }) { Text("Retour") }
                } else {
                    OutlinedButton(onClick = onAnnuler) { Text("Annuler") }
                }
                if (state.step != CreationStep.RECAPITULATIF) {
                    Button(
                        onClick = { holder.confirmerEtapeCourante() },
                        // Rouge (actif) seulement si l'étape est réellement validable.
                        enabled = holder.etapeCouranteValide(state),
                        modifier = Modifier.weight(1f)
                    ) { Text("Confirmer") }
                }
            }
        }
    }
}

/** Bandeau de rappel de la classe choisie (nom + caractéristique(s) principale(s)), affiché
 * en permanence sur les étapes suivant la confirmation de CreationStep.CLASSE. */
@Composable
private fun ClasseRappelBandeau(classe: Classe) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = UiAlignment.CenterVertically
        ) {
            Text(
                "Classe : ${classe.nom}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (classe.caracteristiquePrincipale.isNotEmpty()) {
                Text(
                    "Caractéristique : ${classe.caracteristiquePrincipale.joinToString(" ou ") { it.label }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

/** Carte translucide (fond commun de l'app) posée derrière un bloc de texte ou de champs. */
@Composable
private fun CarteFond(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

// ---------------------------------------------------------------------------
// Étape 1 — Classe
// ---------------------------------------------------------------------------
@Composable
private fun EtapeClasse(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, classes: List<Classe>) {
    val selection = state.selectionEnAttente as? Classe
    val context = androidx.compose.ui.platform.LocalContext.current
    Column {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(classes) { classe ->
                val icone = remember(classe.nom) { classeIconeBitmap(context, classe.nom) }
                ChoixCard(
                    titre = classe.nom,
                    sousTitre = listOfNotNull(
                        classe.description.ifBlank { null },
                        "${classe.caracteristiquePrincipale.joinToString(" ou ") { it.label }} · Dé de vie ${classe.deDeVie}",
                        classe.difficulte.ifBlank { null }?.let { "Difficulté : $it" }
                    ).joinToString("\n"),
                    selectionne = selection == classe,
                    titreEnGras = true,
                    tailleTitre = 20.sp,
                    icone = icone,
                    onClick = { holder.mettreAJourSelection(classe) }
                )
            }
        }
    }
}

// Icônes de classe fournies en asset (assets/dnd/icone/classe/, pas en drawable — même
// convention que ic_acceuil.png dans AppBottomBar.kt) : les classes sans image retombent
// sans icône (ChoixCard l'affiche simplement en son absence), à compléter si d'autres
// images sont ajoutées sous ce dossier sans changer ce code.
private val CLASSE_ICONE_ASSET = mapOf(
    "Barbare" to "dnd/icone/classe/barbar-removebg.png",
    "Barde" to "dnd/icone/classe/barde-removebg.png",
    "Clerc" to "dnd/icone/classe/clerc-removebg.png",
    "Druide" to "dnd/icone/classe/druide-removebg.png",
    "Ensorceleur" to "dnd/icone/classe/ensorceleur-removebg.png",
    "Guerrier" to "dnd/icone/classe/guerrier-removebg.png",
    "Magicien" to "dnd/icone/classe/magicien-removebg.png",
    "Moine" to "dnd/icone/classe/moine-removebg.png",
    "Occultiste" to "dnd/icone/classe/occultiste-removebg.png",
    "Paladin" to "dnd/icone/classe/paladin-removebg.png",
    "Rôdeur" to "dnd/icone/classe/rodeur-removebg.png",
    "Roublard" to "dnd/icone/classe/roublard-removebg.png",
)

private fun classeIconeBitmap(context: android.content.Context, nomClasse: String): androidx.compose.ui.graphics.ImageBitmap? {
    val chemin = CLASSE_ICONE_ASSET[nomClasse] ?: return null
    return runCatching {
        context.assets.open(chemin).use { android.graphics.BitmapFactory.decodeStream(it) }
            ?.asImageBitmap()
    }.getOrNull()
}

@Composable
private fun EtapeCompetencesClasse(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val classe = state.draft.classe
    if (classe == null) {
        Text("Retournez à l'étape précédente pour choisir une classe.", color = Color.White)
        return
    }
    val choix = parseCompetencesClasse(classe.maitrisesCompetence)
    val options = choix.optionsFixes ?: TOUTES_COMPETENCES
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? List<String>) ?: emptyList()
    // Compétences déjà données par l'historique : les reprendre ici les passe en Expertise.
    val competencesHistorique = state.draft.historique?.maitrisesCompetence.orEmpty()

    Column {
        Text(
            "${classe.nom} : choisissez ${choix.nombre} compétence(s) (${selection.size}/${choix.nombre}).",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { competence ->
                val choisie = competence in selection
                val titre = if (competencesHistorique.any { it.equals(competence, ignoreCase = true) }) {
                    "$competence (déjà maîtrisée → Expertise)"
                } else competence
                ChoixCard(titre, DESCRIPTIONS_COMPETENCES[competence], choisie) {
                    val nouvelle = if (choisie) selection - competence
                    else if (selection.size < choix.nombre) selection + competence else selection
                    holder.mettreAJourSelection(nouvelle)
                }
            }
        }
    }
}

/**
 * Choix intégrés aux aptitudes de niveau 1 de la classe (ex. Ordre divin du Clerc : Protecteur
 * ou Thaumaturge — `AptitudeClasse.type == "choix-effet"`, options lues depuis la liste à puces
 * du SRD, cf. Srdcreationparsers.parserAptitude). Une seule étape gère toutes les aptitudes à
 * choix de la classe (il n'y en a qu'une dans le contenu actuel, mais rien n'empêche d'en
 * ajouter d'autres à une classe sans changer ce code).
 */
@Composable
private fun EtapeClasseAptitudeChoix(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val classe = state.draft.classe
    val aptitudes = classe?.aptitudesNiveau1?.filter { it.type == "choix-effet" } ?: emptyList()
    if (classe == null || aptitudes.isEmpty()) {
        Text("Retournez à l'étape précédente pour choisir une classe.", color = Color.White)
        return
    }
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? Map<String, String>) ?: emptyMap()

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        aptitudes.forEach { apt ->
            val cle = apt.id ?: apt.nom
            Text(apt.nom, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            if (apt.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(apt.description, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            apt.options.forEach { option ->
                ChoixCard(option.nom, option.description, selection[cle] == option.nom) {
                    holder.mettreAJourSelection(selection + (cle to option.nom))
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Étape 2 — Origines : historique, espèce, langues
// ---------------------------------------------------------------------------
// Les données viennent de historiques_srd521.md / especes_srd521.md / langues.md via
// HistoriqueParser / EspeceParser / LangueParser (voir SrdCreationParsers.kt).
// Rien n'est codé en dur ici : ajouter un bloc "## Nom" dans un de ces fichiers,
// ou remplacer le fichier par une version avec davantage de choix, suffit.

@Composable
private fun EtapeHistorique(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, historiques: List<Historique>) {
    val selectionActuelle = state.selectionEnAttente as? SelectionHistorique
    var historiqueChoisi by remember(state.draft.historique) { mutableStateOf(state.draft.historique) }
    var texte by remember(state.draft.histoirePersonnalite) { mutableStateOf(state.draft.histoirePersonnalite) }

    fun publier() {
        val h = historiqueChoisi
        holder.mettreAJourSelection(if (h != null) SelectionHistorique(h, texte) else null)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "L'historique représente la place et l'occupation les plus formatrices du parcours du personnage.",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        historiques.forEach { h ->
            ChoixCard(
                titre = h.nom,
                // Avantages sur trois lignes : don, caractéristiques, compétences.
                sousTitre = listOf(
                    "Don : ${h.don}",
                    "Caractéristiques : ${h.caracteristiques.joinToString(", ") { it.label }}",
                    "Compétences : ${h.maitrisesCompetence.joinToString(", ").ifBlank { "—" }}",
                ).joinToString("\n"),
                selectionne = (selectionActuelle?.historique ?: historiqueChoisi) == h,
                onClick = { historiqueChoisi = h; publier() }
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Imaginez son passé et son présent", style = MaterialTheme.typography.titleSmall, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Laissez-vous guider par l'historique et l'espèce du personnage pour imaginer son passé :",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        QUESTIONS_PASSE_PERSONNAGE.forEach { question ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("• ", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text(question, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = texte,
            onValueChange = { texte = it; publier() },
            label = { Text("Histoire et personnalité") },
            placeholder = { Text("Racontez la légende de votre personnage...") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
        )
    }
}

/** Courte présentation par espèce (aucune source SRD ne fournit ce texte : especes_srd521.md
 * ne contient que des champs structurés et des traits, cf. EspeceParser). */
private val DESCRIPTIONS_ESPECES: Map<String, String> = mapOf(
    "Drakéide" to "Descendants de dragons, fiers et loyaux, dotés d'un souffle destructeur hérité de leur ascendance.",
    "Elfe" to "Peuple gracieux et longévif, à l'ouïe et à la vue perçantes, indifférent au sommeil ordinaire.",
    "Gnome" to "Petit peuple curieux et ingénieux, à l'esprit vif et à l'imagination débordante.",
    "Goliath" to "Colosses des hautes cimes, endurants et compétitifs, façonnés par des terres hostiles.",
    "Halfelin" to "Petit peuple discret et chanceux, brave malgré sa taille et attaché aux plaisirs simples.",
    "Humain" to "Espèce la plus répandue et la plus adaptable, ambitieuse et diverse dans ses cultures.",
    "Nain" to "Peuple robuste des montagnes et des mines, résistant au poison et attaché à la tradition.",
    "Orc" to "Guerriers endurants et déterminés, portés par une farouche volonté de survivre.",
    "Tieffelin" to "Marqués par un héritage infernal, ils portent cornes ou queue et une résistance au feu."
)

@Composable
private fun EtapeEspece(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, especes: List<Espece>) {
    val selection = state.selectionEnAttente as? Espece
    Column {
        Text(
            "L'espèce détermine notamment la catégorie de taille et la Vitesse du personnage.",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(especes) { e ->
                ChoixCard(
                    titre = e.nom,
                    sousTitre = listOfNotNull(
                        // Espèce importée : première phrase de sa présentation.
                        DESCRIPTIONS_ESPECES[e.nom] ?: e.description.takeIf { it.isNotBlank() }
                            ?.let { d -> d.substringBefore(". ").trimEnd('.') + "." },
                        "Taille ${e.taille} · Vitesse ${e.vitesse} · ${e.traits.count { !it.base }} traits" +
                            choixEspece(e, holder.donsOriginesNoms).count { it.niveau <= 1 }
                                .takeIf { it > 0 }?.let { " · $it choix à faire" }.orEmpty()
                    ).joinToString("\n"),
                    selectionne = selection == e,
                    onClick = { holder.mettreAJourSelection(e) }
                )
            }
        }
    }
}

/**
 * Tous les choix balisés de l'espèce (balises "choix:" de especes_srd521.md) sur une même
 * étape, chacun avec sa raison : compétence du trait Compétent, lignage, taille, don...
 * L'étape ne se valide que lorsque chaque choix a son nombre exact de valeurs.
 */
@Composable
private fun EtapeEspeceChoix(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val espece = state.draft.espece
    val tousLesChoix = holder.choixEspeceCreation(state.draft)
    if (espece == null || tousLesChoix.isEmpty()) {
        // Ne devrait pas s'afficher (étape sautée automatiquement si non pertinente),
        // filet de sécurité au cas où l'espèce serait modifiée entre-temps.
        Text("Retournez à l'étape précédente pour choisir une espèce.", color = Color.White)
        return
    }
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? Map<String, List<String>>) ?: state.draft.especeChoix
    // Compétences déjà maîtrisées (classe, historique) : les reprendre les passe en Expertise.
    val competencesAcquises = (state.draft.competencesClasse + state.draft.historique?.maitrisesCompetence.orEmpty()).toSet()

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tousLesChoix.forEach { choix ->
            ChoixBaliseCarte(
                choix = choix,
                selection = selection[choix.id].orEmpty(),
                onSelectionChange = { valeurs -> holder.mettreAJourSelection(selection + (choix.id to valeurs)) },
                versExpertise = if (choix.effet == EffetBalise.COMPETENCES) competencesAcquises else emptySet(),
            )
        }
    }
}

/**
 * Choix balisés de la classe (outils, objet d'équipement), de l'historique (outils) et des dons
 * reçus (ex. Initié à la magie : liste de sorts + caractéristique ; Doué : 3 maîtrises), chacun
 * avec sa raison. Les valeurs imposées par la source sont affichées sans pouvoir être changées.
 */
@Composable
private fun EtapeChoixComplementaires(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, sortsDisponibles: List<SrdEntry>) {
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? Map<String, List<String>>) ?: state.draft.choixComplementaires
    // Recalculé avec la sélection en cours : les sorts proposés suivent la liste choisie.
    val tousLesChoix = holder.choixComplementaires(state.draft, selection)
    // Choix entièrement imposés : la sélection est publiée d'office pour pouvoir valider l'étape.
    LaunchedEffect(tousLesChoix) {
        if (state.selectionEnAttente == null && tousLesChoix.all { it.impose != null }) {
            holder.mettreAJourSelection(state.draft.choixComplementaires)
        }
    }
    val competencesAcquises = (state.draft.competencesClasse + state.draft.historique?.maitrisesCompetence.orEmpty()).toSet()

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tousLesChoix.forEach { choix ->
            ChoixBaliseCarte(
                choix = choix,
                selection = selection[choix.id].orEmpty(),
                onSelectionChange = { valeurs ->
                    // Changer de liste de sorts invalide les sorts déjà cochés qui n'y figurent plus.
                    val nouvelle = selection + (choix.id to valeurs)
                    val nettoyee = holder.choixComplementaires(state.draft, nouvelle)
                        .associate { c -> c.id to nouvelle[c.id].orEmpty().filter { it in c.options } }
                    holder.mettreAJourSelection(nouvelle + nettoyee)
                },
                indisponibles = (state.draft.sortsMineursChoisis + state.draft.sortsChoisis).toSet(),
                versExpertise = if (choix.effet == EffetBalise.COMPETENCES) competencesAcquises else emptySet(),
                sorts = sortsDisponibles,
            )
        }
    }
}

/** Langue signature associée à l'espèce (aucune source SRD ne relie explicitement les deux :
 * pré-sélectionnée par défaut à l'étape langues, cf. EtapeLangues). */
private val LANGUE_PAR_ESPECE: Map<String, String> = mapOf(
    "Drakéide" to "Draconique",
    "Elfe" to "Elfique",
    "Gnome" to "Gnome",
    "Goliath" to "Gigant",
    "Halfelin" to "Halfelin",
    "Nain" to "Nain",
    "Orc" to "Orc",
    "Tieffelin" to "Infernal"
)

@Composable
private fun EtapeLangues(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, languesDisponibles: List<Langue>) {
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? List<Langue>) ?: emptyList()
    val commun = languesDisponibles.firstOrNull { it.nom.equals("Commun", ignoreCase = true) }
    val langueEspece = state.draft.espece?.let { LANGUE_PAR_ESPECE[it.nom] }
        ?.let { nom -> languesDisponibles.firstOrNull { it.nom.equals(nom, ignoreCase = true) } }

    // Langues en plus des 2 de base, données par la classe (balise "effet: langues", ex. Roublard).
    val supplementaires = holder.languesSupplementaires(state.draft)
    val nombreLangues = 2 + supplementaires.sumOf { it.nombre }

    // Langues rares masquées, sauf si une option le permet : langue(s) en plus données par la
    // classe (au choix libre), ou langue signature de l'espèce (ex. Infernal du Tieffelin).
    val raresAutorisees = supplementaires.isNotEmpty()
    val autresLangues = languesDisponibles.filterNot { it == commun }.filter { langue ->
        raresAutorisees || !langue.groupe.contains("rare", ignoreCase = true) ||
            langue == langueEspece || langue in selection
    }

    // Présélectionne Commun + la langue signature de l'espèce (ex. Elfique pour un Elfe),
    // tant qu'aucun choix n'a encore été fait sur cette étape (n'écrase pas une reprise
    // de brouillon ou un retour depuis le récapitulatif, où une sélection existe déjà).
    LaunchedEffect(langueEspece) {
        if (state.selectionEnAttente == null && langueEspece != null) {
            holder.mettreAJourSelection(listOfNotNull(commun, langueEspece).distinct())
        }
    }

    Column {
        Text(
            "Le Commun est automatique. Choisissez exactement $nombreLangues langues supplémentaires (${selection.count { it != commun }}/$nombreLangues).",
            color = Color.White
        )
        supplementaires.forEach { c ->
            Text(
                "⚑ Dont ${c.nombre} au choix grâce à « ${c.nomTrait} ».",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(autresLangues) { langue ->
                val choisie = langue in selection
                val autresChoisies = selection.count { it != commun }
                ChoixCard(langue.nom, langue.groupe, choisie) {
                    val sansCelleCi = selection - langue
                    val nouvelle = if (choisie) sansCelleCi
                    else if (autresChoisies < nombreLangues) selection + langue else selection
                    val avecCommun = (commun?.let { nouvelle + it } ?: nouvelle).distinct()
                    holder.mettreAJourSelection(avecCommun)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Étape 3 — Caractéristiques
// ---------------------------------------------------------------------------
@Composable
private fun EtapeMethodeCaracteristiques(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val selection = state.selectionEnAttente as? MethodeGenerationCaracteristiques
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Choisissez la méthode de génération des 6 valeurs de caractéristique.",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        MethodeGenerationCaracteristiques.entries.forEach { methode ->
            ChoixCard(methode.label, null, selection == methode) { holder.mettreAJourSelection(methode) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EtapeValeursCaracteristiques(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    when (state.draft.methodeCaracteristiques) {
        MethodeGenerationCaracteristiques.VALEURS_STANDARD -> EtapeValeursStandard(holder)
        MethodeGenerationCaracteristiques.GENERATION_ALEATOIRE -> EtapeValeursAleatoires(holder)
        MethodeGenerationCaracteristiques.ACQUISITION_PAR_POINTS -> EtapeValeursParPoints(holder)
        null -> Text("Retournez à l'étape précédente pour choisir une méthode.", color = Color.White)
    }
}

@Composable
private fun EtapeValeursStandard(holder: CharacterCreationStateHolder) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Valeurs standard : ${TablesCaracteristiques.valeursStandard.joinToString(", ")}",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { holder.mettreAJourSelection(TablesCaracteristiques.valeursStandard) }) {
            Text("Utiliser ces valeurs")
        }
    }
}

@Composable
private fun EtapeValeursAleatoires(holder: CharacterCreationStateHolder) {
    var valeurs by remember { mutableStateOf<List<Int>?>(null) }

    fun lancer() {
        val resultat = List(6) {
            List(4) { (1..6).random() }.sortedDescending().take(3).sum()
        }
        valeurs = resultat
        holder.mettreAJourSelection(resultat)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("4d6, on garde les 3 meilleurs, six fois de suite.", color = Color.White)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { lancer() }) { Text(if (valeurs == null) "Lancer les dés" else "Relancer") }
        valeurs?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it.joinToString("   ·   "),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EtapeValeursParPoints(holder: CharacterCreationStateHolder) {
    var valeurs by remember { mutableStateOf(List(6) { 8 }) }
    val cout = TablesCaracteristiques.coutParValeur
    val depense = valeurs.sumOf { cout[it] ?: 0 }
    val restant = TablesCaracteristiques.BUDGET_POINTS - depense

    fun publier(nouvelles: List<Int>) {
        valeurs = nouvelles
        holder.mettreAJourSelection(nouvelles)
    }

    // Valeurs par défaut (8 partout) déjà valides : on publie tout de suite pour ne pas
    // bloquer "Confirmer" tant qu'on n'a touché à rien, comme pour l'étape Personnalité.
    LaunchedEffect(Unit) { holder.mettreAJourSelection(valeurs) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Acquisition par points — $restant point(s) restant(s) sur ${TablesCaracteristiques.BUDGET_POINTS}.",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        valeurs.forEachIndexed { index, valeur ->
            val coutSuivant = cout[valeur + 1]
            val peutAugmenter = valeur < 15 && coutSuivant != null && (depense - (cout[valeur] ?: 0) + coutSuivant) <= TablesCaracteristiques.BUDGET_POINTS
            val peutDiminuer = valeur > 8

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = UiAlignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Valeur ${index + 1}", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Row(verticalAlignment = UiAlignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { publier(valeurs.toMutableList().also { it[index] = valeur - 1 }) },
                        enabled = peutDiminuer
                    ) { Text("−") }
                    Text(
                        valeur.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { publier(valeurs.toMutableList().also { it[index] = valeur + 1 }) },
                        enabled = peutAugmenter
                    ) { Text("+") }
                }
            }
        }
    }
}

/**
 * Ordre de répartition suggéré pour les Valeurs standard (15, 14, 13, 12, 10, 8), sur le
 * principe du "Build rapide" du SRD 5.2.1 (p.22) : la/les caractéristique(s) principale(s)
 * de la classe reçoivent la valeur la plus haute, ses jets de sauvegarde maîtrisés suivent,
 * puis Constitution en priorité pour les points de vie, le reste comblant les valeurs basses.
 */
private fun ordreCaracteristiquesRecommande(classe: Classe): List<Caracteristique> {
    val principales = classe.caracteristiquePrincipale
    val sauvegardes = classe.maitriseJetsSauvegarde.filterNot { it in principales }
    val reste = Caracteristique.entries.filterNot { it in principales || it in sauvegardes }
        .sortedByDescending { it == Caracteristique.CONSTITUTION }
    return (principales + sauvegardes + reste).distinct()
}

@Composable
private fun EtapeRepartitionCaracteristiques(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val valeurs = state.draft.valeursGenerees
    val classe = state.draft.classe
    // assignation[caractéristique] = index dans `valeurs` (indexé plutôt que par valeur,
    // pour gérer sans ambiguïté d'éventuels doublons issus de la génération aléatoire).
    var assignation by remember(valeurs) { mutableStateOf(mapOf<Caracteristique, Int>()) }

    fun publier(nouvelle: Map<Caracteristique, Int>) {
        assignation = nouvelle
        val complet = nouvelle.takeIf { it.size == 6 }?.mapValues { (_, index) -> valeurs[index] }
        holder.mettreAJourSelection(complet)
    }

    // Caractéristique(s) principale(s) déjà visible en permanence dans ClasseRappelBandeau
    // au-dessus de cette étape (cf. affichage du wizard) : pas de répétition ici.
    // Valeur standard (15, 14, 13, 12, 10, 8) recommandée par caractéristique pour cette
    // classe, à titre indicatif quelle que soit la méthode de génération effectivement
    // choisie — affichée entre parenthèses à côté de chaque caractéristique ci-dessous
    // plutôt qu'en phrase séparée, moins lisible (cf. ordreCaracteristiquesRecommande).
    val valeurStandardRecommandee: Map<Caracteristique, Int> = classe
        ?.let { ordreCaracteristiquesRecommande(it).zip(TablesCaracteristiques.valeursStandard).toMap() }
        ?: emptyMap()
    val methodeStandard = state.draft.methodeCaracteristiques == MethodeGenerationCaracteristiques.VALEURS_STANDARD

    Column {
        CarteFond {
            Text(
                "Attribuez chacune de vos 6 valeurs (${valeurs.joinToString(", ")}) à une caractéristique.",
                color = Color.White
            )
            if (classe != null && methodeStandard) {
                val ordre = ordreCaracteristiquesRecommande(classe)
                val valeursTriees = valeurs.withIndex().sortedByDescending { it.value }
                OutlinedButton(onClick = {
                    publier(ordre.zip(valeursTriees.map { it.index }).toMap())
                }) {
                    Text("Utiliser la répartition standard pour ${classe.nom}")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Caracteristique.entries) { c ->
                Column {
                    Text(
                        valeurStandardRecommandee[c]?.let { "${c.label} (${it})" } ?: c.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        valeurs.forEachIndexed { index, valeur ->
                            val prisAilleurs = assignation.any { it.key != c && it.value == index }
                            val selectionne = assignation[c] == index
                            FilterChip(
                                selected = selectionne,
                                enabled = !prisAilleurs || selectionne,
                                onClick = { publier(assignation.filterValues { it != index } + (c to index)) },
                                label = { Text(valeur.toString()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                    selectedContainerColor = COULEUR_SELECTION,
                                    selectedLabelColor = COULEUR_TEXTE_SELECTION
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EtapeAjustementHistorique(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    // Les 3 caractéristiques citées viennent directement de l'historique choisi
    // (champ "Valeurs de caractéristique" de historiques_srd521.md).
    val caracteristiquesHistorique = state.draft.historique?.caracteristiques.orEmpty()
    if (caracteristiquesHistorique.size < 3) {
        Text("Retournez à l'étape précédente pour choisir un historique.", color = Color.White)
        return
    }

    var modeReparti by remember { mutableStateOf<Boolean?>(null) } // null = rien choisi ; true = +1 aux trois ; false = +2/+1
    var plus2 by remember { mutableStateOf<Caracteristique?>(null) }
    var plus1 by remember { mutableStateOf<Caracteristique?>(null) }

    fun publier() {
        val map = when (modeReparti) {
            true -> caracteristiquesHistorique.associateWith { 1 }
            false -> if (plus2 != null && plus1 != null) mapOf(plus2!! to 2, plus1!! to 1) else null
            null -> null
        }
        holder.mettreAJourSelection(map)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Votre historique cite : ${caracteristiquesHistorique.joinToString(", ") { it.label }}.",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = modeReparti == true,
                onClick = { modeReparti = true; publier() },
                label = { Text("+1 aux trois") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = COULEUR_SELECTION,
                    selectedLabelColor = COULEUR_TEXTE_SELECTION
                )
            )
            FilterChip(
                selected = modeReparti == false,
                onClick = { modeReparti = false; plus2 = null; plus1 = null },
                label = { Text("+2 sur une, +1 sur une autre") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = COULEUR_SELECTION,
                    selectedLabelColor = COULEUR_TEXTE_SELECTION
                )
            )
        }

        if (modeReparti == false) {
            Spacer(Modifier.height(16.dp))
            Text("Reçoit +2 :", style = MaterialTheme.typography.labelLarge, color = Color.White)
            caracteristiquesHistorique.forEach { c ->
                ChoixCard(c.label, null, plus2 == c) {
                    plus2 = c
                    if (plus1 == c) plus1 = null
                    publier()
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Reçoit +1 :", style = MaterialTheme.typography.labelLarge, color = Color.White)
            caracteristiquesHistorique.filter { it != plus2 }.forEach { c ->
                ChoixCard(c.label, null, plus1 == c) { plus1 = c; publier() }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Étape 4 — Alignement
// ---------------------------------------------------------------------------
@Composable
private fun EtapeAlignement(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val selection = state.selectionEnAttente as? Alignement
    var confirmationMauvaisRequise by remember { mutableStateOf<Alignement?>(null) }

    Column {
        Text("Choisissez l'alignement de votre personnage.", color = Color.White)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Alignement.entries) { a ->
                ChoixCard("${a.label} (${a.code})", a.description, selection == a) {
                    if (a.estMauvais) confirmationMauvaisRequise = a
                    else holder.mettreAJourSelection(a)
                }
            }
        }
    }

    confirmationMauvaisRequise?.let { alignementMauvais ->
        AlertDialog(
            onDismissRequest = { confirmationMauvaisRequise = null },
            title = { Text("Personnage mauvais") },
            text = { Text("Le jeu part du principe que les PJ ne sont pas d'alignement mauvais. Confirmez avec votre MJ avant de continuer.") },
            confirmButton = {
                TextButton(onClick = {
                    holder.mettreAJourSelection(alignementMauvais)
                    confirmationMauvaisRequise = null
                }) { Text("Confirmer quand même") }
            },
            dismissButton = {
                TextButton(onClick = { confirmationMauvaisRequise = null }) { Text("Annuler") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Étape 5 — Équipement, sorts, personnalité, récapitulatif
// ---------------------------------------------------------------------------

/**
 * Étape équipement de classe : le nombre d'options est lu dans le contenu (champs
 * "equipdepA/B/C..." du bloc Traits de base, cf. [EquipementClasse]), jamais supposé
 * fixe à 2. `equipement.fixe` (champs "equipdep1/2...") est toujours ajouté en plus,
 * sans faire partie du choix — ex. un objet que la classe reçoit systématiquement,
 * quelle que soit l'option retenue.
 */
@Composable
private fun EtapeEquipementClasse(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val classe = state.draft.classe
    if (classe == null) {
        Text("Retournez à l'étape précédente pour choisir une classe.", color = Color.White)
        return
    }
    val equipement = classe.equipement
    val texteFixe = equipement.fixe.joinToString(", ").ifBlank { null }
    fun combiner(choix: String?) = listOfNotNull(choix, texteFixe).joinToString(", ")

    val selection = state.selectionEnAttente as? String
    // Pas de choix réel (0 ou 1 option) : l'équipement est déjà déterminé, on le publie
    // automatiquement pour ne pas bloquer la suite du wizard sur un bouton "Confirmer"
    // qui n'aurait rien à confirmer.
    val texteUnique = if (equipement.options.size <= 1) {
        combiner(equipement.options.firstOrNull()?.texte ?: equipement.sansChoix)
    } else null
    LaunchedEffect(classe) {
        if (texteUnique != null && state.selectionEnAttente == null) {
            holder.mettreAJourSelection(texteUnique)
        }
    }

    Column {
        Text(
            "Équipement de départ de ${classe.nom}" +
                if (equipement.options.size > 1) " — choisissez une option." else ".",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        if (texteUnique != null) {
            Text(texteUnique, color = Color.White)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(equipement.options) { option ->
                    val texteComplet = combiner(option.texte)
                    ChoixCard(
                        titre = "Option ${option.lettre}",
                        sousTitre = texteComplet,
                        selectionne = selection == texteComplet,
                        onClick = { holder.mettreAJourSelection(texteComplet) }
                    )
                }
            }
            texteFixe?.let {
                Spacer(Modifier.height(8.dp))
                Text("Équipement supplémentaire (toujours reçu) : $it", color = Color.White)
            }
        }
    }
}

@Composable
private fun EtapeEquipementHistorique(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val historique = state.draft.historique
    if (historique == null) {
        Text("Retournez à l'étape précédente pour choisir un historique.", color = Color.White)
        return
    }
    val selection = state.selectionEnAttente as? String

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Équipement de départ de l'historique ${historique.nom} — choisissez une option.",
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        ChoixCard("Option A", historique.equipementA, selection == historique.equipementA) {
            holder.mettreAJourSelection(historique.equipementA)
        }
        Spacer(Modifier.height(8.dp))
        ChoixCard("Option B", historique.equipementB, selection == historique.equipementB) {
            holder.mettreAJourSelection(historique.equipementB)
        }
    }
}

@Composable
private fun EtapeSorts(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, sortsDisponibles: List<SrdEntry>) {
    val classe = state.draft.classe
    // Le champ "Classes :" n'existe que dans les entrées détaillées (SrdRepository.loadSpells),
    // pas dans l'index par niveau (dont le rawMarkdown ne contient que l'école de magie) —
    // c'est cette confusion qui causait "aucun sort trouvé" pour toutes les classes.
    val regexClasses = Regex("""\*\*Classes\s*:\*\*\s*(.+)""")
    val sortsDeLaClasse = remember(classe, sortsDisponibles) {
        sortsDisponibles.filter { sort ->
            if (classe == null) return@filter false
            val classesDuSort = regexClasses.find(sort.rawMarkdown)?.groupValues?.get(1).orEmpty()
            classesDuSort.split(",").map { it.trim() }.any { it.equals(classe.nom, ignoreCase = true) }
        }
    }
    // Séparation mineurs / niveau 1, sur le même critère que le filtre initial de
    // CharacterCreationScreen (première ligne de l'entrée SRD).
    val sortsMineursDisponibles = remember(sortsDeLaClasse) {
        sortsDeLaClasse.filter { it.rawMarkdown.lineSequence().firstOrNull().orEmpty().contains("mineur", ignoreCase = true) }
    }
    val sortsNiveau1Disponibles = remember(sortsDeLaClasse) {
        sortsDeLaClasse.filter { it.rawMarkdown.lineSequence().firstOrNull().orEmpty().contains("1er niveau", ignoreCase = true) }
    }
    // Nombre requis par la classe (table de progression, ligne "Niveau 1"), plafonné au
    // nombre de sorts réellement présents dans la bibliothèque : un manque de contenu
    // SRD ne doit jamais empêcher de terminer la création (cf. message ci-dessous).
    val requisMineurs = minOf(classe?.sortsMineursNiveau1 ?: 0, sortsMineursDisponibles.size)
    // Classe à grimoire (Magicien) : on choisit les sorts du grimoire de départ (balise
    // « grimoire-depart »), dont seuls les premiers seront préparés (cf. Characterdraftmapping).
    val requisPrepares = minOf(classe?.let { it.grimoireDepart ?: it.sortsPreparesNiveau1 } ?: 0, sortsNiveau1Disponibles.size)

    var mineursChoisis by remember(classe) { mutableStateOf(state.draft.sortsMineursChoisis) }
    // Sorts recommandés pour la classe (ex. Paladin : Héroïsme, Châtiment de fournaise), pré-cochés
    // tant qu'aucun choix n'a été fait.
    val recommandes = remember(classe, sortsNiveau1Disponibles) {
        SORTS_RECOMMANDES_NIVEAU_1[classe?.nom].orEmpty()
            .filter { nom -> sortsNiveau1Disponibles.any { it.name.equals(nom, ignoreCase = true) } }
    }
    var preparesChoisis by remember(classe) {
        mutableStateOf(state.draft.sortsChoisis.ifEmpty { recommandes.take(requisPrepares) })
    }

    fun publier() {
        val complet = (mineursChoisis.size == requisMineurs && preparesChoisis.size == requisPrepares)
        holder.mettreAJourSelection(if (complet) SelectionSorts(mineursChoisis, preparesChoisis) else null)
    }

    // Republie l'état courant en arrivant sur l'étape (reprise de brouillon, ou "Modifier"
    // depuis le récapitulatif) sans attendre un nouveau tap, comme pour les autres étapes
    // à quota (cf. EtapeRepartitionCaracteristiques).
    LaunchedEffect(classe) { publier() }

    val filtreMineurs = rememberFiltreSorts(classe)
    val filtrePrepares = rememberFiltreSorts(classe)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Sorts de ${classe?.nom.orEmpty()} au niveau 1.", color = Color.White)
        Spacer(Modifier.height(12.dp))

        if (sortsMineursDisponibles.isEmpty() && sortsNiveau1Disponibles.isEmpty()) {
            Text(
                "Aucun sort trouvé pour cette classe dans la bibliothèque pour l'instant — vous pourrez les ajouter plus tard depuis la fiche. Vous pouvez continuer sans en choisir.",
                color = Color.White
            )
        }

        if (requisMineurs > 0) {
            Text(
                "Sorts mineurs — choisissez-en $requisMineurs (${mineursChoisis.size}/$requisMineurs).",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            FiltreSortsBarre(filtreMineurs, sortsMineursDisponibles, couleurTexte = Color.White)
            Spacer(Modifier.height(8.dp))
            // Même carte que l'onglet Sorts de la fiche : aperçu rapide, détail dépliable.
            filtreMineurs.appliquer(sortsMineursDisponibles).forEach { sort ->
                val choisi = sort.name in mineursChoisis
                SortApercuCarte(
                    sort = sort,
                    selectionne = choisi,
                    selectionPossible = mineursChoisis.size < requisMineurs,
                    onSelection = {
                        mineursChoisis = if (choisi) mineursChoisis - sort.name
                        else if (mineursChoisis.size < requisMineurs) mineursChoisis + sort.name else mineursChoisis
                        publier()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        if (requisPrepares > 0) {
            Text(
                (if (classe?.grimoireDepart != null) "Grimoire de départ (sorts de 1er niveau)" else "Sorts de 1er niveau") +
                    " — choisissez-en $requisPrepares (${preparesChoisis.size}/$requisPrepares)." +
                    (if (classe?.grimoireDepart != null) " Les ${classe.sortsPreparesNiveau1} premiers sont préparés ; changez-les dans l'écran Repos." else ""),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            if (recommandes.isNotEmpty()) {
                Text(
                    "Recommandés : ${recommandes.joinToString(" et ")}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            FiltreSortsBarre(filtrePrepares, sortsNiveau1Disponibles, couleurTexte = Color.White)
            Spacer(Modifier.height(8.dp))
            // Sorts recommandés en tête de liste.
            filtrePrepares.appliquer(sortsNiveau1Disponibles).sortedByDescending { s -> recommandes.any { it.equals(s.name, ignoreCase = true) } }.forEach { sort ->
                val choisi = sort.name in preparesChoisis
                SortApercuCarte(
                    sort = sort,
                    selectionne = choisi,
                    selectionPossible = preparesChoisis.size < requisPrepares,
                    mention = if (recommandes.any { it.equals(sort.name, ignoreCase = true) }) "★ recommandé" else null,
                    onSelection = {
                        preparesChoisis = if (choisi) preparesChoisis - sort.name
                        else if (preparesChoisis.size < requisPrepares) preparesChoisis + sort.name else preparesChoisis
                        publier()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Étape 6 — Nom (toute dernière étape, une fois l'espèce connue)
// ---------------------------------------------------------------------------
@Composable
private fun EtapeNom(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val espece = state.draft.espece
    var saisie by remember(state.draft.nomPersonnage) { mutableStateOf(state.draft.nomPersonnage.orEmpty()) }

    fun publier(valeur: String) {
        saisie = valeur
        holder.mettreAJourSelection(valeur.trim().takeIf { it.isNotBlank() })
    }

    // Republie la valeur déjà connue en arrivant sur l'étape (ex. "Modifier" depuis le
    // récapitulatif) : évite de bloquer "Confirmer" tant que le nom n'a pas été retapé.
    LaunchedEffect(state.draft.nomPersonnage) {
        state.draft.nomPersonnage?.takeIf { it.isNotBlank() }?.let { holder.mettreAJourSelection(it) }
    }

    CarteFond {
        Text(
            if (espece != null) "Comment s'appelle votre ${espece.nom.lowercase()} ?"
            else "Quel est le nom de votre personnage ?",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        OutlinedTextField(
            value = saisie,
            onValueChange = { publier(it) },
            label = { Text("Nom du héros") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedButton(
            onClick = { publier(genererNomAleatoire(espece)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("🎲 Générer un nom aléatoire" + (espece?.let { " (${it.nom})" } ?: "")) }
    }
}

// ---------------------------------------------------------------------------
// Étape 7 — Portrait (juste après le nom, dernier choix avant le récapitulatif)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtapePortrait(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val selection = (state.selectionEnAttente as? String) ?: state.draft.portrait

    // Republie le portrait déjà choisi en arrivant sur l'étape (ex. "Modifier" depuis le
    // récapitulatif) : évite de bloquer "Confirmer" tant qu'aucun portrait n'a été retapé.
    LaunchedEffect(state.draft.portrait) {
        state.draft.portrait?.takeIf { it.isNotBlank() }?.let { holder.mettreAJourSelection(it) }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Choisissez le portrait de votre personnage.", color = Color.White)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            characterPortraitOptions.forEach { option ->
                val choisi = option.id == selection
                Column(
                    horizontalAlignment = UiAlignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { holder.mettreAJourSelection(option.id) }
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = option.resId),
                        contentDescription = option.label,
                        modifier = Modifier
                            .width(64.dp)
                            .height(82.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                if (choisi) 3.dp else 1.dp,
                                if (choisi) COULEUR_SELECTION else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(option.label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

/**
 * Génère un nom aléatoire cohérent avec l'espèce du personnage (prénom + nom de
 * famille tirés dans des banques distinctes par espèce, cf. [NOMS_PAR_ESPECE]).
 * Espèce inconnue ou absente de la table -> retombe sur des noms fantasy génériques.
 */
private fun genererNomAleatoire(espece: Espece?): String {
    val (prenoms, noms) = NOMS_PAR_ESPECE[espece?.nom] ?: (PRENOMS_GENERIQUES to NOMS_GENERIQUES)
    return "${prenoms.random()} ${noms.random()}"
}

private val PRENOMS_GENERIQUES = listOf(
    "Aldric", "Branwen", "Corwin", "Elara", "Fenwick", "Gwendolyn", "Hadrian", "Isolde",
    "Joran", "Kaelen", "Lysandra", "Magnus", "Nerys", "Osric", "Perrin", "Quenna",
    "Roderic", "Seraphine", "Thalos", "Ysolde"
)
private val NOMS_GENERIQUES = listOf(
    "Alderbois", "Brisevent", "Cœurdechêne", "Duval", "Étoile-Noire", "Forgefer",
    "Grisemine", "Hautelame", "Larmedor", "Montcalme", "Noirval", "Ombregarde",
    "Pierrelande", "Rivesombre", "Solvent", "Terraube"
)

/**
 * Banques de noms par espèce (clé = Espece.nom tel que défini dans especes_srd521.md :
 * Drakéide, Elfe, Gnome, Goliath, Halfelin, Humain, Nain, Orc, Tieffelin). Noms
 * inventés, au style phonétique distinct par espèce plutôt que repris d'une œuvre.
 */
private val NOMS_PAR_ESPECE: Map<String, Pair<List<String>, List<String>>> = mapOf(
    "Humain" to (PRENOMS_GENERIQUES to NOMS_GENERIQUES),
    "Elfe" to (
        listOf(
            "Aerendyl", "Caelinor", "Elowen", "Faelivrin", "Ithildae", "Larethiel", "Miriel",
            "Naerion", "Silvaeril", "Thalanil", "Vaelith", "Ylaenor"
        ) to listOf(
            "Duskwhisper", "Feuillargent", "Lunombre", "Moonshadow", "Rossignol", "Songe-d'Aube",
            "Sylvenoire", "Vent-des-Cimes"
        )
        ),
    "Nain" to (
        listOf(
            "Balin", "Brenna", "Dorin", "Ekkehard", "Grunhilde", "Harnok", "Isolde Barbe-de-Fer",
            "Korgrim", "Runa", "Thrudi", "Vondal", "Wulfric"
        ) to listOf(
            "Barbe-de-Granit", "Cassepierre", "Forgechaude", "Marteaudur", "Poingdefer",
            "Roc-Ancien", "Tonnebrume", "Veinargent"
        )
        ),
    "Halfelin" to (
        listOf(
            "Bramble", "Cora", "Doran", "Elly", "Finn", "Ivy", "Jorey", "Milo", "Pip",
            "Rosie", "Tobin", "Wren"
        ) to listOf(
            "Boncoeur", "Bouton-d'Or", "Champdoré", "Doucemine", "Pieds-Légers",
            "Sac-de-Voyage", "Terre-Fertile", "Trèfle-Chanceux"
        )
        ),
    "Gnome" to (
        listOf(
            "Bibelo", "Fizwick", "Glimmick", "Nixie", "Pippick", "Quimble", "Sprocket",
            "Tinka", "Wrenna", "Ziggle"
        ) to listOf(
            "Cliquetis", "Étincelle-Vive", "Frimboulon", "Grelot-de-Cuivre", "Machinbroc",
            "Rouage-Fou", "Tourbillon"
        )
        ),
    "Goliath" to (
        listOf(
            "Aukan", "Dorthu", "Eglath", "Gauthak", "Kuvara", "Manneo", "Orilo", "Thalu",
            "Vimak", "Yevha"
        ) to listOf(
            "Bourrasque-de-Pierre", "Cime-Brisée", "Éclat-de-Granit", "Épaule-de-Roc",
            "Poing-d'Orage", "Sommet-Gris"
        )
        ),
    "Orc" to (
        listOf(
            "Grukk", "Hurga", "Krosh", "Mogul", "Nazka", "Orgath", "Skrall", "Thokk",
            "Uzgar", "Yelka"
        ) to listOf(
            "Brise-Crâne", "Dent-Rouge", "Griffe-Noire", "Poing-de-Fer", "Tranche-Gorge",
            "Vent-de-Guerre"
        )
        ),
    "Tieffelin" to (
        listOf(
            "Akmenos", "Damaia", "Ekemon", "Ishvala", "Lerissa", "Nemeia", "Orianus",
            "Rieta", "Skamos", "Zephyrine"
        ) to listOf(
            "Braise-Ancienne", "Cendre-Noire", "Larme-d'Onyx", "Ombre-Écarlate",
            "Soupir-des-Abysses", "Voile-de-Fumée"
        )
        ),
    "Drakéide" to (
        listOf(
            "Arjhan", "Balasar", "Donaar", "Ghesh", "Kriv", "Medrash", "Nadarr", "Pandjed",
            "Rhogar", "Torinn"
        ) to listOf(
            "Écaille-d'Or", "Griffe-d'Ambre", "Queue-de-Braise", "Souffle-de-Cendre",
            "Vol-du-Levant", "Crocs-d'Onyx"
        )
        )
)

@Composable
private fun EtapeRecapitulatif(
    state: CharacterCreationUiState,
    holder: CharacterCreationStateHolder,
    onTermine: (CharacterDraft) -> Unit
) {
    val draft = state.draft
    var confirmationFinale by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            "Résumé de votre personnage",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                RecapLigne("Nom", draft.nomPersonnage)
                RecapLigne("Portrait", characterPortraitOptions.firstOrNull { it.id == draft.portrait }?.label)
                RecapLigne("Classe", draft.classe?.nom)
                RecapLigne("Compétences de classe", draft.competencesClasse.joinToString(", ").ifBlank { null })
                RecapLigne("Historique", draft.historique?.nom)
                RecapLigne("Espèce", draft.espece?.nom)
                draft.espece?.let { espece ->
                    choixEspece(espece, emptyList()).filter { it.niveau <= 1 }.forEach { c ->
                        draft.especeChoix[c.id]?.takeIf { it.isNotEmpty() }?.let { valeurs ->
                            RecapLigne("${c.nomTrait} (${EffetBalise.libelleChoix(c.effet).lowercase()})", valeurs.joinToString(", "))
                        }
                    }
                }
                RecapLigne("Langues", draft.langues.joinToString(", ") { it.nom })
                RecapLigne(
                    "Caractéristiques",
                    draft.valeursFinales.entries.joinToString(", ") { (c, v) -> "${c.label} $v (${draft.modificateurs[c]?.let { if (it >= 0) "+$it" else "$it" }})" }
                )
                RecapLigne("Alignement", draft.alignement?.label)
                RecapLigne("Équipement (classe)", draft.equipementClasseTexte)
                RecapLigne("Équipement (historique)", draft.equipementHistoriqueTexte)
                if (draft.classe?.estLanceurDeSorts == true) {
                    RecapLigne("Sorts mineurs", draft.sortsMineursChoisis.joinToString(", ").ifBlank { "Aucun" })
                    RecapLigne("Sorts de 1er niveau", draft.sortsChoisis.joinToString(", ").ifBlank { "Aucun" })
                }
                RecapLigne("Or de départ", "${draft.orDepart} po")
                draft.pointsDeVieNiveau1?.let {
                    RecapLigne("Points de vie au niveau 1", it.toString())
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { confirmationFinale = true },
            enabled = draft.estComplet,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Créer le personnage") }
    }

    if (confirmationFinale) {
        AlertDialog(
            onDismissRequest = { confirmationFinale = false },
            title = { Text("Confirmer la création") },
            text = { Text("Ces choix seront enregistrés sur la fiche de personnage. Continuer ?") },
            confirmButton = {
                TextButton(onClick = { confirmationFinale = false; onTermine(draft) }) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmationFinale = false }) { Text("Revoir") }
            }
        )
    }
}

@Composable
private fun RecapLigne(label: String, valeur: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(valeur ?: "—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---------------------------------------------------------------------------
// Composant partagé
// ---------------------------------------------------------------------------
// Couleur de sélection fixée en dur : les rôles de thème onPrimaryContainer/
// onSecondaryContainer produisent un texte illisible (même couleur que le fond)
// dans le thème actuel de l'app. Un blanc explicite garantit le contraste quel
// que soit l'état du thème.
private val COULEUR_SELECTION = Color(0xFFB71C1C)
private val COULEUR_TEXTE_SELECTION = Color.White

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ChoixCard(
    titre: String,
    sousTitre: String?,
    selectionne: Boolean,
    titreEnGras: Boolean = false,
    tailleTitre: androidx.compose.ui.unit.TextUnit? = null,
    // Icône optionnelle affichée à gauche du titre (ex. icône de classe, cf. classeIconeAsset
    // dans EtapeClasse) — absente pour les cartes qui n'en ont pas besoin.
    icone: androidx.compose.ui.graphics.ImageBitmap? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val couleurFond = if (selectionne) COULEUR_SELECTION else MaterialTheme.colorScheme.surface
    val couleurTexte = if (selectionne) COULEUR_TEXTE_SELECTION else MaterialTheme.colorScheme.onSurface
    val styleTitre = MaterialTheme.typography.bodyLarge.let { base ->
        base.copy(
            fontWeight = if (titreEnGras) FontWeight.Bold else base.fontWeight,
            fontSize = tailleTitre ?: base.fontSize
        )
    }
    val modifierClic = if (onDoubleClick != null) {
        Modifier.combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = couleurFond, contentColor = couleurTexte),
        modifier = Modifier.fillMaxWidth().then(modifierClic)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = UiAlignment.CenterVertically) {
            if (icone != null) {
                Image(
                    bitmap = icone,
                    contentDescription = null,
                    modifier = Modifier.size(126.dp) // 48dp + 50%, puis +75% supplémentaires
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(titre, style = styleTitre, color = couleurTexte)
                sousTitre?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = couleurTexte) }
            }
        }
    }
}