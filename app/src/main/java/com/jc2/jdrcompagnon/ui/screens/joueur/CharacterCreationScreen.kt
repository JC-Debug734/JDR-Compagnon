package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationDraftStore
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationStateHolder
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationWizard
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CreationSnapshot
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.DonParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Espece
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EspeceParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Historique
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.HistoriqueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Langue
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.LangueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.versCharacter
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Écran de création de personnage joueur. Demande d'abord le nom (le wizard
 * lui-même n'a pas ce champ), puis enchaîne sur CharacterCreationWizard —
 * classe / origines / caractéristiques / alignement / récapitulatif, chaque
 * choix demandant confirmation. Les options viennent de la bibliothèque SRD
 * (classes_srd521.md, historiques_srd521.md, especes_srd521.md, langues.md) via
 * SrdRepository, converties en Classe/Historique/Espece par les parsers de
 * SrdCreationParsers.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    currentWorld: WorldState?,
    isMjMode: Boolean = false,
    onCharacterCreated: () -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
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
            nomPersonnage = snapshot.nomPersonnage,
            onReprendre = { reprendreConfirme = true },
            onRecommencer = {
                CharacterCreationDraftStore.effacer(context)
                snapshotExistant = null
                reprendreConfirme = true
            }
        )
        return
    }

    var nomPersonnage by remember { mutableStateOf(snapshot?.nomPersonnage) }
    val nom = nomPersonnage
    if (nom == null) {
        EtapeNomPersonnage(onValider = { nomPersonnage = it }, onBack = onBack, onOpenMenu = onOpenMenu)
        return
    }

    var classes by remember { mutableStateOf<List<Classe>?>(null) }
    var historiques by remember { mutableStateOf<List<Historique>?>(null) }
    var especes by remember { mutableStateOf<List<Espece>?>(null) }
    var langues by remember { mutableStateOf<List<Langue>?>(null) }
    var sorts by remember { mutableStateOf<List<SrdEntry>?>(null) }
    var donsOrigines by remember { mutableStateOf<List<String>?>(null) }

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
        // Dons d'origines seulement : c'est la seule catégorie proposée au choix à la
        // création (trait "Polyvalent" de l'Humain).
        donsOrigines = DonParser.parse(
            SrdRepository.loadDons(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        ).filter { it.categorie == "Origines" }.map { it.nom }
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
        CharacterCreationDraftStore.sauvegarder(context, holder.exporterSnapshot(nom))
    }

    CharacterCreationWizard(
        holder = holder,
        classes = classesChargees,
        historiques = historiquesCharges,
        especes = especesChargees,
        langues = languesChargees,
        sortsDisponibles = sortsCharges,
        donsOriginesNoms = donsOriginesCharges,
        onTermine = { draft ->
            val createdBy = if (isMjMode) "MJ" else "Joueur"
            GameState.addCharacter(draft.versCharacter(nom = nom, worldId = worldId, createdBy = createdBy))
            CharacterCreationDraftStore.effacer(context)
            onCharacterCreated()
        },
        onAnnuler = {
            CharacterCreationDraftStore.effacer(context)
            onBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtapeRepriseCreation(nomPersonnage: String, onReprendre: () -> Unit, onRecommencer: () -> Unit) {
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
            Text("Une création de personnage est en cours pour \"$nomPersonnage\".", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Voulez-vous reprendre où vous en étiez, ou recommencer à zéro ?", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onReprendre, modifier = Modifier.fillMaxWidth()) { Text("Reprendre") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRecommencer, modifier = Modifier.fillMaxWidth()) { Text("Recommencer à zéro") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtapeNomPersonnage(onValider: (String) -> Unit, onBack: () -> Unit, onOpenMenu: () -> Unit = {}) {
    var saisie by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un personnage") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Quel est le nom de votre personnage ?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = saisie,
                onValueChange = { saisie = it },
                label = { Text("Nom du héros") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { saisie = genererNomAleatoire() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🎲 Générer un nom aléatoire") }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onValider(saisie.trim()) },
                enabled = saisie.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Commencer la création") }
        }
    }
}

/**
 * Noms de fantasy génériques (pas liés à une espèce précise, choisie plus tard dans
 * le wizard). Simple combinaison prénom + nom, pas de dépendance externe.
 */
private val PRENOMS_ALEATOIRES = listOf(
    "Aldric", "Branwen", "Corwin", "Elara", "Fenwick", "Gwendolyn", "Hadrian", "Isolde",
    "Joran", "Kaelen", "Lysandra", "Magnus", "Nerys", "Osric", "Perrin", "Quenna",
    "Roderic", "Seraphine", "Thalos", "Ysolde"
)
private val NOMS_ALEATOIRES = listOf(
    "Alderbois", "Brisevent", "Cœurdechêne", "Duval", "Étoile-Noire", "Forgefer",
    "Grisemine", "Hautelame", "Larmedor", "Montcalme", "Noirval", "Ombregarde",
    "Pierrelande", "Rivesombre", "Solvent", "Terraube"
)

private fun genererNomAleatoire(): String = "${PRENOMS_ALEATOIRES.random()} ${NOMS_ALEATOIRES.random()}"