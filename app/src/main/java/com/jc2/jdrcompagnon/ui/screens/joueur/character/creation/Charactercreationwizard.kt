package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    // Noms des dons d'origines (dons_srd521.md, catégorie "Origines"), pour le choix
    // de don du trait "Polyvalent" de l'Humain à l'étape ESPECE_CHOIX.
    donsOriginesNoms: List<String> = emptyList(),
    onTermine: (CharacterDraft) -> Unit,
    onAnnuler: () -> Unit
) {
    val state by holder.uiState.collectAsState()
    LaunchedEffect(donsOriginesNoms) { holder.donsOriginesNoms = donsOriginesNoms }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Création de personnage — ${state.step.label}") },
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
            Box(modifier = Modifier.weight(1f)) {
                when (state.step) {
                    CreationStep.CLASSE -> EtapeClasse(state, holder, classes)
                    CreationStep.COMPETENCES_CLASSE -> EtapeCompetencesClasse(state, holder)
                    CreationStep.ESPECE -> EtapeEspece(state, holder, especes)
                    CreationStep.ESPECE_CHOIX -> EtapeEspeceChoix(state, holder)
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
                    sousTitre = listOfNotNull(
                        classe.description.ifBlank { null },
                        "${classe.caracteristiquePrincipale.joinToString(" ou ") { it.label }} · Dé de vie ${classe.deDeVie}"
                    ).joinToString("\n"),
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
    val selectionActuelle = state.selectionEnAttente as? SelectionHistorique
    var historiqueChoisi by remember(state.draft.historique) { mutableStateOf(state.draft.historique) }
    var texte by remember(state.draft.histoirePersonnalite) { mutableStateOf(state.draft.histoirePersonnalite) }

    fun publier() {
        val h = historiqueChoisi
        holder.mettreAJourSelection(if (h != null) SelectionHistorique(h, texte) else null)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("L'historique représente la place et l'occupation les plus formatrices du parcours du personnage.")
        Spacer(Modifier.height(12.dp))
        historiques.forEach { h ->
            ChoixCard(
                titre = h.nom,
                sousTitre = "Don : ${h.don} · Caractéristiques : ${h.caracteristiques.joinToString(", ") { it.label }}",
                selectionne = (selectionActuelle?.historique ?: historiqueChoisi) == h,
                onClick = { historiqueChoisi = h; publier() }
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Imaginez son passé et son présent", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Laissez-vous guider par l'historique et l'espèce du personnage pour imaginer son passé :",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        QUESTIONS_PASSE_PERSONNAGE.forEach { question ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("• ", style = MaterialTheme.typography.bodySmall)
                Text(question, style = MaterialTheme.typography.bodySmall)
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
private fun EtapeEspeceChoix(state: CharacterCreationUiState, holder: CharacterCreationStateHolder) {
    val espece = state.draft.espece
    val choix = espece?.let { detecterChoixEspece(it, holder.donsOriginesNoms) }
    if (espece == null || choix == null) {
        // Ne devrait pas s'afficher (étape sautée automatiquement si non pertinente),
        // filet de sécurité au cas où l'espèce serait modifiée entre-temps.
        Text("Retournez à l'étape précédente pour choisir une espèce.")
        return
    }
    val selection = state.selectionEnAttente as? String

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("${espece.nom} — trait « ${choix.nomTrait} » : choisissez une option.")
        Spacer(Modifier.height(12.dp))
        choix.options.forEach { option ->
            ChoixCard(option, null, selection == option) { holder.mettreAJourSelection(option) }
            Spacer(Modifier.height(8.dp))
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
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
    when (state.draft.methodeCaracteristiques) {
        MethodeGenerationCaracteristiques.VALEURS_STANDARD -> EtapeValeursStandard(holder)
        MethodeGenerationCaracteristiques.GENERATION_ALEATOIRE -> EtapeValeursAleatoires(holder)
        MethodeGenerationCaracteristiques.ACQUISITION_PAR_POINTS -> EtapeValeursParPoints(holder)
        null -> Text("Retournez à l'étape précédente pour choisir une méthode.")
    }
}

@Composable
private fun EtapeValeursStandard(holder: CharacterCreationStateHolder) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Valeurs standard : ${TablesCaracteristiques.valeursStandard.joinToString(", ")}")
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
        Text("4d6, on garde les 3 meilleurs, six fois de suite.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = { lancer() }) { Text(if (valeurs == null) "Lancer les dés" else "Relancer") }
        valeurs?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it.joinToString("   ·   "),
                style = MaterialTheme.typography.headlineSmall
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
        Text("Acquisition par points — $restant point(s) restant(s) sur ${TablesCaracteristiques.BUDGET_POINTS}.")
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
                Text("Valeur ${index + 1}", style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = UiAlignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { publier(valeurs.toMutableList().also { it[index] = valeur - 1 }) },
                        enabled = peutDiminuer
                    ) { Text("−") }
                    Text(valeur.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedButton(
                        onClick = { publier(valeurs.toMutableList().also { it[index] = valeur + 1 }) },
                        enabled = peutAugmenter
                    ) { Text("+") }
                }
            }
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
                                label = { Text(valeur.toString()) },
                                colors = FilterChipDefaults.filterChipColors(
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
        Text("Retournez à l'étape précédente pour choisir un historique.")
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
        Text("Votre historique cite : ${caracteristiquesHistorique.joinToString(", ") { it.label }}.")
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

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
    @Suppress("UNCHECKED_CAST")
    val selection = (state.selectionEnAttente as? List<String>) ?: emptyList()

    // Étape toujours confirmable, même à 0 sort choisi (le sort est facultatif à la
    // création, et surtout aucun sort disponible ne doit jamais bloquer la suite) :
    // on publie la sélection dès l'entrée sur l'étape, comme pour Personnalité.
    LaunchedEffect(classe) { holder.mettreAJourSelection(state.draft.sortsChoisis) }

    Column {
        Text("Sorts connus/préparés au niveau 1 pour ${classe?.nom.orEmpty()} (${selection.size} choisi(s)).")
        Spacer(Modifier.height(12.dp))
        if (sortsDeLaClasse.isEmpty()) {
            Text("Aucun sort trouvé pour cette classe dans la bibliothèque pour l'instant — vous pourrez les ajouter plus tard depuis la fiche. Vous pouvez continuer sans en choisir.")
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
private fun EtapeRecapitulatif(
    state: CharacterCreationUiState,
    holder: CharacterCreationStateHolder,
    onTermine: (CharacterDraft) -> Unit
) {
    val draft = state.draft
    var confirmationFinale by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Vérifiez vos choix avant de créer la fiche.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        RecapLigne("Classe", draft.classe?.nom, CreationStep.CLASSE, holder)
        RecapLigne("Compétences de classe", draft.competencesClasse.joinToString(", ").ifBlank { null }, CreationStep.COMPETENCES_CLASSE, holder)
        RecapLigne("Historique", draft.historique?.nom, CreationStep.HISTORIQUE, holder)
        RecapLigne("Espèce", draft.espece?.nom, CreationStep.ESPECE, holder)
        draft.especeChoixSupplementaire?.let {
            RecapLigne("Particularité d'espèce", it, CreationStep.ESPECE_CHOIX, holder)
        }
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
// Couleur de sélection fixée en dur : les rôles de thème onPrimaryContainer/
// onSecondaryContainer produisent un texte illisible (même couleur que le fond)
// dans le thème actuel de l'app. Un blanc explicite garantit le contraste quel
// que soit l'état du thème.
private val COULEUR_SELECTION = Color(0xFFB71C1C)
private val COULEUR_TEXTE_SELECTION = Color.White

@Composable
private fun ChoixCard(titre: String, sousTitre: String?, selectionne: Boolean, onClick: () -> Unit) {
    val couleurFond = if (selectionne) COULEUR_SELECTION else MaterialTheme.colorScheme.surface
    val couleurTexte = if (selectionne) COULEUR_TEXTE_SELECTION else MaterialTheme.colorScheme.onSurface
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = couleurFond, contentColor = couleurTexte),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(titre, style = MaterialTheme.typography.bodyLarge, color = couleurTexte)
            sousTitre?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = couleurTexte) }
        }
    }
}