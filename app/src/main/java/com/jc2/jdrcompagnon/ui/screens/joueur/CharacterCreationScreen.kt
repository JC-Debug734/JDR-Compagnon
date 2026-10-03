package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.CharacterExport
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationDraftStore
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationStateHolder
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.construireContenuPaquetages
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationWizard
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CreationSnapshot
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Don
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ContexteChoix
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.DonParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Espece
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EspeceParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Historique
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.HistoriqueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Langue
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.LangueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.sortChoisissable
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.versCharacter
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Écran de création de personnage joueur : enchaîne directement sur
 * CharacterCreationWizard — classe / origines / caractéristiques / alignement /
 * équipement / sorts / nom / récapitulatif, chaque choix demandant confirmation.
 * Le nom est la toute dernière étape du wizard (CreationStep.NOM), une fois
 * l'espèce connue, pour permettre une génération de nom aléatoire cohérente.
 * Les options viennent de la bibliothèque SRD (classes_srd521.md,
 * historiques_srd521.md, especes_srd521.md, langues.md) via SrdRepository,
 * converties en Classe/Historique/Espece par les parsers de SrdCreationParsers.kt.
 */
@Composable
fun CharacterCreationScreen(
    currentWorld: WorldState?,
    isMjMode: Boolean = false,
    onCharacterCreated: () -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    // Tous les textes de la création en blanc : on surcharge les couleurs "on*" du thème
    // (toujours sombre, cf. JdrCompagnonTheme) plutôt que chaque Text un par un, pour couvrir
    // aussi cartes, champs, listes et dialogues du wizard.
    val base = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = base.copy(
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White,
            onPrimaryContainer = Color.White,
            onSecondaryContainer = Color.White,
            onTertiaryContainer = Color.White,
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            CharacterCreationScreenContent(currentWorld, isMjMode, onCharacterCreated, onBack, onOpenMenu)
        }
    }
}

@Composable
private fun CharacterCreationScreenContent(
    currentWorld: WorldState?,
    isMjMode: Boolean,
    onCharacterCreated: () -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val context = LocalContext.current
    val worldId = currentWorld?.id ?: "donjon_et_dragon"

    // Vérifie s'il existe un brouillon sauvegardé (création interrompue précédemment)
    // avant de demander quoi que ce soit à l'utilisateur.
    var snapshotVerifie by remember { mutableStateOf(false) }
    var snapshotExistant by remember { mutableStateOf<CreationSnapshot?>(null) }
    var reprendreConfirme by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotExistant = CharacterCreationDraftStore.charger(context)
        snapshotVerifie = true
    }
    if (!snapshotVerifie) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val snapshot = snapshotExistant
    if (snapshot != null && !reprendreConfirme) {
        EtapeRepriseCreation(
            snapshot = snapshot,
            onReprendre = { reprendreConfirme = true },
            onRecommencer = {
                CharacterCreationDraftStore.effacer(context)
                snapshotExistant = null
                reprendreConfirme = true
            }
        )
        return
    }

    // Choix de départ (nouveau personnage vs import depuis un fichier), uniquement quand il
    // n'y a pas de brouillon à reprendre — une reprise saute directement dans le wizard.
    // Sauté aussi en mode MJ : "Pas à pas" a déjà été choisi explicitement dans le dialogue
    // de CharacterSelectionScreen (qui propose aussi l'import), redemander ici serait redondant.
    var modeChoisi by remember { mutableStateOf(snapshot != null || isMjMode) }
    if (!modeChoisi) {
        EtapeDebutCreation(
            onNouveauPersonnage = { modeChoisi = true },
            onPersonnageImporte = { importe ->
                val createdBy = if (isMjMode) "MJ" else "Joueur"
                val nouveauPersonnage = importe.character.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    worldId = worldId,
                    createdBy = createdBy
                )
                GameState.addCharacter(nouveauPersonnage, historiqueImporte = importe.historique)
                onCharacterCreated()
            }
        )
        return
    }

    var classes by remember { mutableStateOf<List<Classe>?>(null) }
    var historiques by remember { mutableStateOf<List<Historique>?>(null) }
    var especes by remember { mutableStateOf<List<Espece>?>(null) }
    var langues by remember { mutableStateOf<List<Langue>?>(null) }
    var sorts by remember { mutableStateOf<List<SrdEntry>?>(null) }
    var donsOrigines by remember { mutableStateOf<List<Don>?>(null) }
    // Contenu des paquetages (ex. "Explorateur" -> Sac à dos, Corde, 10 Torche...), pour
    // détailler l'équipement de départ objet par objet plutôt que sous le seul nom du
    // paquetage (cf. construireContenuPaquetages). Non bloquant : reste vide le temps du
    // chargement plutôt que de retarder l'affichage du wizard.
    var contenuPaquetages by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var tousLesDons by remember { mutableStateOf<List<Don>>(emptyList()) }
    var contexteChoix by remember { mutableStateOf(ContexteChoix()) }

    LaunchedEffect(worldId) {
        classes = ClasseParser.parse(
            SrdRepository.loadClasses(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        )
        historiques = HistoriqueParser.parse(
            SrdRepository.loadHistoriques(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        )
        especes = EspeceParser.parse(
            SrdRepository.loadEspeces(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        )
        langues = LangueParser.parse(SrdRepository.loadLangues(context, worldId))
        // Sorts mineurs + niveau 1 seulement : rien d'autre n'est castable au niveau 1.
        // loadSpells (pas loadSpellsIndex) : seules les entrées détaillées contiennent
        // le champ "**Classes :**" — l'index ne porte que l'école de magie, sur laquelle
        // un filtre par classe ne peut jamais matcher (c'était le bug remonté).
        sorts = SrdRepository.loadSpells(context, worldId).filter { entry ->
            val premiereLigne = entry.rawMarkdown.lineSequence().firstOrNull().orEmpty()
            premiereLigne.contains("mineur", ignoreCase = true) || premiereLigne.contains("1er niveau", ignoreCase = true)
        }
        // Tous les dons (don d'historique et ses choix) ; ceux balisés "categorie: Origines"
        // sont proposés au trait "Polyvalent" de l'Humain.
        val dons = DonParser.depuisEntrees(SrdRepository.loadDons(context, worldId).map { it.name to it.rawMarkdown })
        tousLesDons = dons
        val equipements = SrdRepository.loadEquipmentList(context, worldId)
        contenuPaquetages = construireContenuPaquetages(equipements)
        // Options des choix balisés "equipement: ..." (instruments, boîtes de jeux, outils) et "langues".
        contexteChoix = ContexteChoix(
            equipements = equipements.map { it.name to it.category },
            langues = langues.orEmpty().map { it.nom }.filterNot { it.equals("Commun", ignoreCase = true) },
            // Sorts mineurs / 1er niveau (ex. ceux du don Initié à la magie, filtrés sur la liste choisie).
            sorts = sorts.orEmpty().mapNotNull { sortChoisissable(it.name, it.rawMarkdown) },
        )
        // En dernier : son chargement débloque l'affichage du wizard (tout le reste est prêt).
        donsOrigines = dons.filter { it.categorie.equals("Origines", ignoreCase = true) }
    }

    val classesChargees = classes
    val historiquesCharges = historiques
    val especesChargees = especes
    val languesChargees = langues
    val sortsCharges = sorts
    val donsOriginesCharges = donsOrigines

    if (classesChargees == null || historiquesCharges == null || especesChargees == null ||
        languesChargees == null || sortsCharges == null || donsOriginesCharges == null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val holder = remember { CharacterCreationStateHolder() }

    // Restaure le brouillon (une seule fois, une fois les données SRD chargées).
    var restaurationFaite by remember { mutableStateOf(false) }
    LaunchedEffect(classesChargees) {
        if (snapshot != null && !restaurationFaite) {
            holder.restaurerDepuisSnapshot(snapshot, classesChargees, historiquesCharges, especesChargees, languesChargees)
            restaurationFaite = true
        }
    }

    // Sauvegarde automatique à chaque étape confirmée : quitter l'écran (retour,
    // fermeture de l'app...) et y revenir plus tard reprend exactement ici.
    val state by holder.uiState.collectAsState()
    LaunchedEffect(state.step, state.draft) {
        CharacterCreationDraftStore.sauvegarder(context, holder.exporterSnapshot())
    }

    CharacterCreationWizard(
        holder = holder,
        classes = classesChargees,
        historiques = historiquesCharges,
        especes = especesChargees,
        langues = languesChargees,
        sortsDisponibles = sortsCharges,
        donsOriginesNoms = donsOriginesCharges,
        tousLesDons = tousLesDons,
        contexteChoix = contexteChoix,
        onTermine = { draft ->
            val createdBy = if (isMjMode) "MJ" else "Joueur"
            val nouveauPersonnage = draft.versCharacter(
                worldId = worldId,
                createdBy = createdBy,
                contenuPaquetages = contenuPaquetages,
                donsOrigines = donsOriginesCharges,
                choixAutres = holder.choixComplementaires(draft).map { it to holder.valeursDe(it, draft.choixComplementaires) },
                donsRecus = holder.donsRecus(draft),
            )
            GameState.addCharacter(nouveauPersonnage)
            // « 2 dagues » → deux « Dague » (nom de la bibliothèque) : arme reconnue en combat.
            GameState.normaliserNomsObjets(nouveauPersonnage.id, contexteChoix.equipements.map { it.first })
            // Équipement de base garanti (sac à dos + sacoche), cf. GameState.equipStarterGear.
            GameState.equipStarterGear(nouveauPersonnage.id)
            CharacterCreationDraftStore.effacer(context)
            onCharacterCreated()
        },
        onAnnuler = {
            CharacterCreationDraftStore.effacer(context)
            onBack()
        },
        onOpenMenu = onOpenMenu
    )
}

/** Résumé lisible d'un brouillon, pour l'écran de reprise (le nom n'est choisi qu'en toute
 * dernière étape, donc souvent pas encore renseigné à ce stade). */
private fun descriptionBrouillon(snapshot: CreationSnapshot): String =
    snapshot.nomPersonnage?.takeIf { it.isNotBlank() }
        ?: listOfNotNull(snapshot.classeNom, snapshot.especeNom).joinToString(" ").ifBlank { null }
        ?: "un personnage"

/**
 * Premier écran de la création : soit démarrer un nouveau personnage (wizard classique),
 * soit importer un fichier JSON de personnage déjà exporté (voir GameState.writeCharacterFile,
 * qui écrit automatiquement ce fichier dans Téléchargements/JDRCompagnon/Personnages/ à chaque
 * création) pour en repartir comme copie de base.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtapeDebutCreation(onNouveauPersonnage: () -> Unit, onPersonnageImporte: (CharacterExport) -> Unit) {
    val context = LocalContext.current
    var erreurImport by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val texte = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
        val personnage = texte?.let { GameState.characterFromJson(it) }
        if (personnage != null) {
            erreurImport = null
            onPersonnageImporte(personnage)
        } else {
            erreurImport = "Fichier invalide : ce n'est pas un personnage JDRCompagnon valide."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Création de personnage") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Comment voulez-vous commencer ?", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Créez un personnage de zéro, ou importez le fichier d'un personnage déjà exporté (Téléchargements/JDRCompagnon/Personnages) pour en repartir comme copie.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onNouveauPersonnage, modifier = Modifier.fillMaxWidth()) { Text("Nouveau personnage") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Importer depuis un fichier") }
            if (erreurImport != null) {
                Spacer(Modifier.height(8.dp))
                Text(erreurImport.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtapeRepriseCreation(snapshot: CreationSnapshot, onReprendre: () -> Unit, onRecommencer: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Création de personnage") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Une création de personnage est en cours pour \"${descriptionBrouillon(snapshot)}\".",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Voulez-vous reprendre où vous en étiez, ou recommencer à zéro ?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onReprendre, modifier = Modifier.fillMaxWidth()) { Text("Reprendre") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRecommencer, modifier = Modifier.fillMaxWidth()) { Text("Recommencer à zéro") }
        }
    }
}