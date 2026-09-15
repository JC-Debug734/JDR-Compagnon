package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdSectionEntry

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
 *   val sortsNiveau1 = SrdRepository.loadSpellsIndex(context, worldId).filter { it.category in setOf("Sorts mineurs", "Sorts de niveau 1") }
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
    sortsDisponibles: List<SrdSectionEntry> = emptyList(),
    onTermine: (CharacterDraft) -> Unit,
    onAnnuler: () -> Unit
) {
    val state by holder.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Création de personnage — ${state.step.label}") })
                LinearProgressIndicator(
                    progress = { state.progression },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (state.step) {
                    CreationStep.CLASSE -> EtapeClasse(state, holder, classes)
                    CreationStep.COMPETENCES_CLASSE -> EtapeCompetencesClasse(state, holder)
                    CreationStep.HISTORIQUE -> EtapeHistorique(state, holder, historiques)
                    CreationStep.ESPECE -> EtapeEspece(state, holder, especes)
                    CreationStep.LANGUES -> EtapeLangues(state, holder, langues)
                    CreationStep.METHODE_CARACTERISTIQUES -> EtapeMethodeCaracteristiques(state, holder)
                    CreationStep.VALEURS_CARACTERISTIQUES -> EtapeValeursCaracteristiques(state, holder)
                    CreationStep.REPARTITION_CARACTERISTIQUES -> EtapeRepartitionCaracteristiques(state, holder)
                    CreationStep.AJUSTEMENT_HISTORIQUE -> EtapeAjustementHistorique(state, holder)
                    CreationStep.ALIGNEMENT -> EtapeAlignement(state, holder)
                    CreationStep.EQUIPEMENT_CLASSE -> EtapeEquipementClasse(state, holder)
                    CreationStep.EQUIPEMENT_HISTORIQUE -> EtapeEquipementHistorique(state, holder)
                    CreationStep.SORTS -> EtapeSorts(state, holder, sortsDisponibles)
                    CreationStep.PERSONNALITE -> EtapePersonnalite(state, holder)
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
                        enabled = state.selectionEnAttente != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Confirmer") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Étape 1 — Classe
// ---------------------------------------------------------------------------
@Composable
private fun EtapeClasse(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, classes: List<Classe>) {
    val selection = state.selectionEnAttente as? Classe
    Column {
        Text(
            "Tout aventurier a une classe : elle englobe sa vocation, ses talents et ses tactiques.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(classes) { classe ->
                ChoixCard(
                    titre = classe.nom,
                    sousTitre = "${classe.caracteristiquePrincipale.joinToString(" ou ") { it.label }} · Dé de vie ${classe.deDeVie}",
                    selectionne = selection == classe,
                    onClick = { holder.mettreAJourSelection(classe) }
                )
            }
        }
    }
}

@Composable
private fun EtapeCompetencesClasse(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val classe = state.draft.classe
    if (classe == null) {
        Text("Retournez à l'étape précédente pour choisir une classe.")
        return
    }
    val choix = parseCompetencesClasse(classe.maitrisesCompetence)
    val options = choix.optionsFixes ?: TOUTES_COMPETENCES
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? List<String>) ?: emptyList()

    Column {
        Text("${classe.nom} : choisissez ${choix.nombre} compétence(s) (${selection.size}/${choix.nombre}).")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { competence ->
                val choisie = competence in selection
                ChoixCard(competence, null, choisie) {
                    val nouvelle = if (choisie) selection - competence
                    else if (selection.size < choix.nombre) selection + competence else selection
                    holder.mettreAJourSelection(nouvelle)
                }
            }
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
    val selection = state.selectionEnAttente as? Historique
    Column {
        Text("L'historique représente la place et l'occupation les plus formatrices du parcours du personnage.")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(historiques) { h ->
                ChoixCard(
                    titre = h.nom,
                    sousTitre = "Don : ${h.don} · Caractéristiques : ${h.caracteristiques.joinToString(", ") { it.label }}",
                    selectionne = selection == h,
                    onClick = { holder.mettreAJourSelection(h) }
                )
            }
        }
    }
}

@Composable
private fun EtapeEspece(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, especes: List<Espece>) {
    val selection = state.selectionEnAttente as? Espece
    Column {
        Text("L'espèce détermine notamment la catégorie de taille et la Vitesse du personnage.")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(especes) { e ->
                ChoixCard(
                    titre = e.nom,
                    sousTitre = "Taille ${e.taille} · Vitesse ${e.vitesse} · ${e.traits.size} traits",
                    selectionne = selection == e,
                    onClick = { holder.mettreAJourSelection(e) }
                )
            }
        }
    }
}

@Composable
private fun EtapeLangues(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, languesDisponibles: List<Langue>) {
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? List<Langue>) ?: emptyList()
    val commun = languesDisponibles.firstOrNull { it.nom.equals("Commun", ignoreCase = true) }
    val autresLangues = languesDisponibles.filterNot { it == commun }

    Column {
        Text("Le Commun est automatique. Choisissez exactement 2 langues supplémentaires (${selection.count { it != commun }}/2).")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(autresLangues) { langue ->
                val choisie = langue in selection
                val autresChoisies = selection.count { it != commun }
                ChoixCard(langue.nom, langue.groupe, choisie) {
                    val sansCelleCi = selection - langue
                    val nouvelle = if (choisie) sansCelleCi
                    else if (autresChoisies < 2) selection + langue else selection
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
    Column {
        Text("Choisissez la méthode de génération des 6 valeurs de caractéristique.")
        Spacer(Modifier.height(12.dp))
        MethodeGenerationCaracteristiques.entries.forEach { methode ->
            ChoixCard(methode.label, null, selection == methode) { holder.mettreAJourSelection(methode) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EtapeValeursCaracteristiques(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    // Pour "valeurs standard" la liste est fixe : on la propose directement.
    // Pour les deux autres méthodes, l'app doit lancer les dés / laisser saisir
    // les points, puis appeler holder.mettreAJourSelection(listeDe6Valeurs).
    val methode = state.draft.methodeCaracteristiques
    Column {
        when (methode) {
            MethodeGenerationCaracteristiques.VALEURS_STANDARD -> {
                Text("Valeurs standard : ${TablesCaracteristiques.valeursStandard.joinToString(", ")}")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { holder.mettreAJourSelection(TablesCaracteristiques.valeursStandard) }) {
                    Text("Utiliser ces valeurs")
                }
            }
            MethodeGenerationCaracteristiques.GENERATION_ALEATOIRE ->
                Text("TODO intégration : lancer 4d6 (garder les 3 meilleurs) × 6, puis appeler mettreAJourSelection(valeurs).")
            MethodeGenerationCaracteristiques.ACQUISITION_PAR_POINTS ->
                Text("TODO intégration : UI de répartition de ${TablesCaracteristiques.BUDGET_POINTS} points selon la table des coûts.")
            null -> Text("Retournez à l'étape précédente pour choisir une méthode.")
        }
    }
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

    Column {
        Text("Attribuez chacune de vos 6 valeurs (${valeurs.joinToString(", ")}) à une caractéristique.")
        classe?.caracteristiquePrincipale?.takeIf { it.isNotEmpty() }?.let { principales ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Caractéristique(s) principale(s) de ${classe.nom} : ${principales.joinToString(" ou ") { it.label }}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Caracteristique.entries) { c ->
                Column {
                    Text(c.label, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        valeurs.forEachIndexed { index, valeur ->
                            val prisAilleurs = assignation.any { it.key != c && it.value == index }
                            val selectionne = assignation[c] == index
                            FilterChip(
                                selected = selectionne,
                                enabled = !prisAilleurs || selectionne,
                                onClick = { publier(assignation.filterValues { it != index } + (c to index)) },
                                label = { Text(valeur.toString()) }
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
        Text("Retournez à l'étape précédente pour choisir un historique.")
        return
    }

    var modeReparti by remember { mutableStateOf(true) } // true = +1 aux trois, false = +2/+1
    var plus2 by remember { mutableStateOf<Caracteristique?>(null) }
    var plus1 by remember { mutableStateOf<Caracteristique?>(null) }

    fun publier() {
        val map = when {
            modeReparti -> caracteristiquesHistorique.associateWith { 1 }
            plus2 != null && plus1 != null -> mapOf(plus2!! to 2, plus1!! to 1)
            else -> null
        }
        holder.mettreAJourSelection(map)
    }

    Column {
        Text("Votre historique cite : ${caracteristiquesHistorique.joinToString(", ") { it.label }}.")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = modeReparti,
                onClick = { modeReparti = true; publier() },
                label = { Text("+1 aux trois") }
            )
            FilterChip(
                selected = !modeReparti,
                onClick = { modeReparti = false; plus2 = null; plus1 = null },
                label = { Text("+2 sur une, +1 sur une autre") }
            )
        }

        if (!modeReparti) {
            Spacer(Modifier.height(16.dp))
            Text("Reçoit +2 :", style = MaterialTheme.typography.labelLarge)
            caracteristiquesHistorique.forEach { c ->
                ChoixCard(c.label, null, plus2 == c) {
                    plus2 = c
                    if (plus1 == c) plus1 = null
                    publier()
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Reçoit +1 :", style = MaterialTheme.typography.labelLarge)
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
        Text("Choisissez l'alignement de votre personnage.")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Alignement.entries) { a ->
                ChoixCard(a.label, a.code, selection == a) {
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

@Composable
private fun EtapeEquipementClasse(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val classe = state.draft.classe
    if (classe == null) {
        Text("Retournez à l'étape précédente pour choisir une classe.")
        return
    }
    val options = decouperOptionsEquipement(classe.equipementDepart)
    val selection = state.selectionEnAttente as? String

    Column {
        Text("Équipement de départ de ${classe.nom} — choisissez une option.")
        Spacer(Modifier.height(12.dp))
        if (options.isEmpty()) {
            Text(classe.equipementDepart)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    ChoixCard(
                        titre = "Option ${option.lettre}",
                        sousTitre = option.texte,
                        selectionne = selection == option.texte,
                        onClick = { holder.mettreAJourSelection(option.texte) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EtapeEquipementHistorique(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val historique = state.draft.historique
    if (historique == null) {
        Text("Retournez à l'étape précédente pour choisir un historique.")
        return
    }
    val selection = state.selectionEnAttente as? String

    Column {
        Text("Équipement de départ de l'historique ${historique.nom} — choisissez une option.")
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
private fun EtapeSorts(state: CharacterCreationUiState, holder: CharacterCreationStateHolder, sortsDisponibles: List<SrdSectionEntry>) {
    val classe = state.draft.classe
    // Filtrage heuristique : le nom de la classe doit apparaître dans le texte brut
    // du sort (champ "Classes :" de sorts_srd521.md). Pas de comptage exact "N sorts
    // mineurs + N sorts de niveau 1" par classe ici — le joueur choisit librement
    // dans sa liste et ajuste ensuite si besoin, cf. limite documentée dans
    // CharacterDraftMapping.kt.
    val sortsDeLaClasse = remember(classe, sortsDisponibles) {
        sortsDisponibles.filter { classe != null && it.rawMarkdown.contains(classe.nom, ignoreCase = true) }
    }
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? List<String>) ?: emptyList()

    Column {
        Text("Sorts connus/préparés au niveau 1 pour ${classe?.nom.orEmpty()} (${selection.size} choisi(s)).")
        Spacer(Modifier.height(12.dp))
        if (sortsDeLaClasse.isEmpty()) {
            Text("Aucun sort trouvé pour cette classe dans la bibliothèque — vous pourrez les ajouter plus tard depuis la fiche.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortsDeLaClasse) { sort ->
                    val choisi = sort.name in selection
                    ChoixCard(sort.name, sort.category, choisi) {
                        val nouvelle = if (choisi) selection - sort.name else selection + sort.name
                        holder.mettreAJourSelection(nouvelle)
                    }
                }
            }
        }
    }
}

@Composable
private fun EtapePersonnalite(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val draft = state.draft
    var personnalite by remember { mutableStateOf(draft.personnalite) }
    var ideaux by remember { mutableStateOf(draft.ideaux) }
    var liens by remember { mutableStateOf(draft.liens) }
    var defauts by remember { mutableStateOf(draft.defauts) }

    // Étape facultative : on publie tout de suite une sélection (même vide) pour que
    // "Confirmer" soit utilisable sans rien remplir.
    LaunchedEffect(Unit) {
        holder.mettreAJourSelection(mapOf("personnalite" to personnalite, "ideaux" to ideaux, "liens" to liens, "defauts" to defauts))
    }

    fun publier() {
        holder.mettreAJourSelection(mapOf("personnalite" to personnalite, "ideaux" to ideaux, "liens" to liens, "defauts" to defauts))
    }

    Column {
        Text("Facultatif — vous pourrez toujours les modifier depuis la fiche.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = personnalite,
            onValueChange = { personnalite = it; publier() },
            label = { Text("Traits de personnalité") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ideaux,
            onValueChange = { ideaux = it; publier() },
            label = { Text("Idéaux") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = liens,
            onValueChange = { liens = it; publier() },
            label = { Text("Liens") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = defauts,
            onValueChange = { defauts = it; publier() },
            label = { Text("Défauts") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EtapeRecapitulatif(
    state: CharacterCreationUiState,
    holder: CharacterCreationStateHolder,
    onTermine: (CharacterDraft) -> Unit
) {
    val draft = state.draft
    var confirmationFinale by remember { mutableStateOf(false) }

    Column {
        Text("Vérifiez vos choix avant de créer la fiche.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        RecapLigne("Classe", draft.classe?.nom, CreationStep.CLASSE, holder)
        RecapLigne("Compétences de classe", draft.competencesClasse.joinToString(", ").ifBlank { null }, CreationStep.COMPETENCES_CLASSE, holder)
        RecapLigne("Historique", draft.historique?.nom, CreationStep.HISTORIQUE, holder)
        RecapLigne("Espèce", draft.espece?.nom, CreationStep.ESPECE, holder)
        RecapLigne("Langues", draft.langues.joinToString(", ") { it.nom }, CreationStep.LANGUES, holder)
        RecapLigne(
            "Caractéristiques",
            draft.valeursFinales.entries.joinToString(", ") { (c, v) -> "${c.label} $v (${draft.modificateurs[c]?.let { if (it >= 0) "+$it" else "$it" }})" },
            CreationStep.REPARTITION_CARACTERISTIQUES, holder
        )
        RecapLigne("Alignement", draft.alignement?.label, CreationStep.ALIGNEMENT, holder)
        RecapLigne("Équipement (classe)", draft.equipementClasseTexte, CreationStep.EQUIPEMENT_CLASSE, holder)
        RecapLigne("Équipement (historique)", draft.equipementHistoriqueTexte, CreationStep.EQUIPEMENT_HISTORIQUE, holder)
        if (draft.classe?.estLanceurDeSorts == true) {
            RecapLigne("Sorts", draft.sortsChoisis.joinToString(", ").ifBlank { "Aucun" }, CreationStep.SORTS, holder)
        }
        RecapLigne("Or de départ", "${draft.orDepart} po", CreationStep.EQUIPEMENT_HISTORIQUE, holder)

        draft.pointsDeVieNiveau1?.let {
            Spacer(Modifier.height(8.dp))
            Text("Points de vie au niveau 1 : $it", style = MaterialTheme.typography.bodyMedium)
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
private fun RecapLigne(label: String, valeur: String?, step: CreationStep, holder: CharacterCreationStateHolder) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = UiAlignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valeur ?: "—", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = { holder.modifierEtape(step) }) { Text("Modifier") }
    }
}

// ---------------------------------------------------------------------------
// Composant partagé
// ---------------------------------------------------------------------------
@Composable
private fun ChoixCard(titre: String, sousTitre: String?, selectionne: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selectionne) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(titre, style = MaterialTheme.typography.bodyLarge)
            sousTitre?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}