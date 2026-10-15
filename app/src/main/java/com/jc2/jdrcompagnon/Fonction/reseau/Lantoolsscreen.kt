package com.jc2.jdrcompagnon.ui.tools

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jc2.jdrcompagnon.network.DiscoveredServer
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.PlayerConnectionState
import com.jc2.jdrcompagnon.network.SessionRole

private enum class LanMode { CHOIX, HOTE, JOUEUR }


/** Carte translucide commune à l'écran réseau : tout texte posé dessus est blanc par défaut. */
@Composable
private fun LanCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
private fun whiteListItemColors() = ListItemDefaults.colors(
    containerColor = Color.Transparent,
    headlineColor = Color.White,
    supportingColor = Color.White,
    trailingIconColor = Color.White,
    leadingIconColor = Color.White
)

/**
 * Écran "Outils > Réseau local". La connexion elle-même (socket, découverte,
 * reconnexion automatique) vit dans NetworkSessionManager, qui survit à la
 * navigation entre écrans et au passage de l'app en arrière-plan grâce à
 * LanConnectionService. Cet écran ne fait qu'observer et piloter cet état.
 */
@Composable
fun LanToolsScreen(
    startInHostMode: Boolean = false,
    startInJoinMode: Boolean = false,
    groupId: String? = null,
    campaignTitle: String? = null,
    onExit: (() -> Unit)? = null,
    // Ouvre le menu latéral : l'écran du serveur affiche alors l'icône menu au lieu de la flèche retour.
    onOpenMenu: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var mode by remember {
        mutableStateOf(
            when {
                startInHostMode -> LanMode.HOTE
                startInJoinMode -> LanMode.JOUEUR
                else -> LanMode.CHOIX
            }
        )
    }

    // Sur Android 13+, NSD et les notifications du service nécessitent une autorisation runtime.
    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasPermissions = results.values.all { it } }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
    when (mode) {
        LanMode.CHOIX -> LanChoiceScreen(
            onHost = { mode = LanMode.HOTE },
            onJoin = { mode = LanMode.JOUEUR }
        )

        LanMode.HOTE -> {
            // Le serveur ne démarre plus automatiquement à l'ouverture de
            // cet écran : seule la permission est demandée ici. Le MJ doit
            // appuyer sur "Démarrer le serveur" (bouton dans HostScreen).
            LaunchedEffect(hasPermissions) {
                if (!hasPermissions) {
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                }
            }
            val role by NetworkSessionManager.role.collectAsState()
            HostScreen(
                hasPermissions = hasPermissions,
                isHosting = role == SessionRole.HOST,
                onStartServer = {
                    val hostDisplayName = com.jc2.jdrcompagnon.ui.GameState.playerName.value
                        ?: com.jc2.jdrcompagnon.ui.GameState.assignRandomPlayerName()
                    NetworkSessionManager.startHosting(
                        context,
                        hostDisplayName = hostDisplayName,
                        // Sans groupe explicite (ouverture depuis le menu), on reprend celui
                        // sélectionné par le MJ dans l'outil GROUPES.
                        groupId = groupId ?: com.jc2.jdrcompagnon.ui.GameState.currentGroupId.value,
                        campaignTitle = campaignTitle
                    )
                },
                onRequestPermissions = { permissionLauncher.launch(requiredPermissions.toTypedArray()) },
                onNavigateBack = { if (onExit != null) onExit() else mode = LanMode.CHOIX },
                onOpenMenu = onOpenMenu,
                onStopServer = {
                    NetworkSessionManager.stopHosting(context)
                    if (onExit != null) onExit() else mode = LanMode.CHOIX
                }
            )
        }

        LanMode.JOUEUR -> {
            LaunchedEffect(hasPermissions) {
                if (!hasPermissions) {
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                } else {
                    NetworkSessionManager.discovery(context).startDiscovery()
                }
            }
            // Seul le SCAN s'arrête en quittant l'écran : une connexion déjà
            // établie (ou en reconnexion) continue de vivre dans le manager.
            DisposableEffect(Unit) {
                onDispose { NetworkSessionManager.discovery(context).stopDiscovery() }
            }
            JoinScreen(
                hasPermissions = hasPermissions,
                onRequestPermissions = { permissionLauncher.launch(requiredPermissions.toTypedArray()) },
                onBack = { if (onExit != null) onExit() else mode = LanMode.CHOIX }
            )
        }
    }
    }
}

@Composable
private fun LanChoiceScreen(onHost: () -> Unit, onJoin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LanCard {
            Text("Partie en réseau local", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onHost, modifier = Modifier.fillMaxWidth()) {
                Text("Héberger une partie (MJ)")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) {
                Text("Rejoindre une partie (Joueur)", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Assurez-vous d'être sur le même réseau Wi-Fi que les autres participants.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PermissionRequiredScreen(onRequestPermissions: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        LanCard {
            Text("Autorisation requise", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "La partie en réseau local nécessite l'autorisation \"Appareils à proximité\" " +
                        "et l'autorisation d'afficher une notification (Android 13 et plus).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Autoriser")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Retour", color = Color.White)
            }
        }
    }
}

/**
 * Imports (livres de la bibliothèque du monde) partagés avec les joueurs : chacun peut les
 * télécharger à sa connexion s'il ne les a pas (voir ImportsPartages). La carte n'affiche que
 * le résumé ; un tap ouvre le choix, filtrable par type de contenu. Choix conservé d'une
 * partie à l'autre.
 */
@Composable
private fun ImportsPartagesSelector() {
    val context = LocalContext.current
    val monde = com.jc2.jdrcompagnon.ui.GameState.currentWorldId() ?: "donjon_et_dragon"
    var selection by remember(monde) { mutableStateOf(com.jc2.jdrcompagnon.network.ImportsPartages.selection(context, monde)) }
    val livres = remember(monde) { com.jc2.jdrcompagnon.network.ImportsPartages.livresPartageables(context, monde) }
    val partages = livres.filter { it.id in selection }
    var choixOuvert by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { choixOuvert = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Imports partagés", style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text(
                    when {
                        livres.isEmpty() -> "Aucun import dans la bibliothèque de ce monde"
                        partages.isEmpty() -> "Aucun — touchez pour en choisir"
                        else -> partages.joinToString(", ") { it.name }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choisir les imports partagés", tint = Color.White)
        }
    }

    if (choixOuvert) {
        ImportsPartagesDialog(
            monde = monde,
            livres = livres,
            selection = selection,
            onBasculer = { livre ->
                com.jc2.jdrcompagnon.network.ImportsPartages.definirPartage(context, monde, livre.id, livre.id !in selection)
                selection = com.jc2.jdrcompagnon.network.ImportsPartages.selection(context, monde)
                NetworkSessionManager.importsPartagesModifies()
            },
            onDismiss = { choixOuvert = false },
        )
    }
}

/** Choix des imports partagés : filtre par type de contenu et par nom, une case par import. */
@Composable
private fun ImportsPartagesDialog(
    monde: String,
    livres: List<com.jc2.jdrcompagnon.ui.screens.mj.CustomBook>,
    selection: Set<String>,
    onBasculer: (com.jc2.jdrcompagnon.ui.screens.mj.CustomBook) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Contenu de chaque livre, lu une fois à l'ouverture (fichiers parfois volumineux).
    var contenus by remember(monde) { mutableStateOf<Map<String, Map<com.jc2.jdrcompagnon.ui.components.CategorieContenu, Int>>?>(null) }
    LaunchedEffect(monde, livres) {
        contenus = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            livres.associate { it.id to com.jc2.jdrcompagnon.ui.components.categoriesDuLivre(context, it) }
        }
    }
    var filtre by remember { mutableStateOf(emptySet<com.jc2.jdrcompagnon.ui.components.CategorieContenu>()) }
    var recherche by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Imports partagés", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Les joueurs qui ne les possèdent pas pourront les télécharger (textes et images). " +
                        "Pour ne partager que le contenu de jeu d'une campagne, séparez-le de ses scénarios dans un import à part.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                val c = contenus
                if (c == null) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    androidx.compose.material3.OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        label = { Text("Rechercher") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    com.jc2.jdrcompagnon.ui.components.FiltreCategoriesContenu(
                        disponibles = c.values.flatMap { it.keys }.toSet(),
                        selection = filtre,
                        onSelectionChange = { filtre = it },
                    )
                    val visibles = livres.filter { livre ->
                        com.jc2.jdrcompagnon.ui.components.correspondAuFiltre(c[livre.id].orEmpty().keys, filtre) &&
                            (recherche.isBlank() || livre.name.contains(recherche.trim(), ignoreCase = true))
                    }
                    Spacer(Modifier.height(8.dp))
                    // La liste prend la place restante (sans dépasser l'écran) : le bouton Fermer
                    // reste visible et la liste défile jusqu'au dernier import.
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        if (visibles.isEmpty()) {
                            item { Text("Aucun import ne correspond.", style = MaterialTheme.typography.bodySmall) }
                        }
                        items(visibles.size) { i ->
                            val livre = visibles[i]
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onBasculer(livre) }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Checkbox(checked = livre.id in selection, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(livre.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        com.jc2.jdrcompagnon.ui.components.resumeCategories(c[livre.id].orEmpty()),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Fermer") }
            }
        }
    }
}

/**
 * Affiche le groupe actuellement associé à la partie (dont les membres sont
 * proposés aux joueurs) et permet d'en changer directement depuis l'écran
 * serveur, sans repasser par le tableau de bord MJ.
 */
@Composable
private fun GroupSelector() {
    val mjGroups by com.jc2.jdrcompagnon.ui.GameState.mjGroups.collectAsState()
    val activeGroupId by NetworkSessionManager.activeGroupId.collectAsState()
    val activeGroup = mjGroups.find { it.id == activeGroupId }
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMenu = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Groupe",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        activeGroup?.name ?: "Aucun groupe — les joueurs ne verront aucun personnage disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Changer de groupe", tint = Color.White)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (mjGroups.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Aucun groupe créé") },
                        onClick = { showMenu = false },
                        enabled = false
                    )
                } else {
                    mjGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                NetworkSessionManager.setActiveGroup(group.id)
                                showMenu = false
                            },
                            leadingIcon = if (group.id == activeGroupId) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostScreen(
    hasPermissions: Boolean,
    isHosting: Boolean,
    onStartServer: () -> Unit,
    onRequestPermissions: () -> Unit,
    onNavigateBack: () -> Unit,
    onStopServer: () -> Unit,
    onOpenMenu: (() -> Unit)? = null,
) {
    // Icône de gauche : menu latéral (petits traits) si disponible, sinon flèche retour.
    val navigationIcon: @Composable () -> Unit = {
        if (onOpenMenu != null) {
            IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
        } else {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
        }
    }
    if (!hasPermissions) {
        PermissionRequiredScreen(onRequestPermissions, onNavigateBack)
        return
    }
    if (!isHosting) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Serveur local") },
                    navigationIcon = navigationIcon,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LanCard {
                    Text(
                        "Le serveur n'est pas encore démarré.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Les joueurs sur le même Wi-Fi pourront rejoindre la partie une fois le serveur lancé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    // Port de la page d'affichage table : gardé d'une session à l'autre.
                    val context = LocalContext.current
                    var port by remember { mutableStateOf(NetworkSessionManager.portTable(context).toString()) }
                    val portValide = port.toIntOrNull()?.let { it in NetworkSessionManager.PLAGE_PORTS } == true
                    OutlinedTextField(
                        value = port,
                        onValueChange = { saisie -> port = saisie.filter(Char::isDigit).take(5) },
                        label = { Text("Port de la page table") },
                        supportingText = {
                            Text(
                                if (portValide) "Toujours le même à chaque démarrage (${NetworkSessionManager.PORT_TABLE_DEFAUT} par défaut)."
                                else "Entre ${NetworkSessionManager.PLAGE_PORTS.first} et ${NetworkSessionManager.PLAGE_PORTS.last}."
                            )
                        },
                        isError = !portValide,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            NetworkSessionManager.setPortTable(context, port.toInt())
                            onStartServer()
                        },
                        enabled = portValide,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Démarrer le serveur")
                    }
                }
            }
        }
        return
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val webServerUrl by NetworkSessionManager.webServerUrl.collectAsState()
    val clients by NetworkSessionManager.connectedClients.collectAsState()
    val pendingProposals by NetworkSessionManager.pendingProposals.collectAsState()
    val claimedCharacters by NetworkSessionManager.claimedCharacters.collectAsState()
    val activeGroupId by NetworkSessionManager.activeGroupId.collectAsState()
    val mjGroups by com.jc2.jdrcompagnon.ui.GameState.mjGroups.collectAsState()
    val myCharactersForGroup by com.jc2.jdrcompagnon.ui.GameState.characters.collectAsState()
    val activeGroup = mjGroups.find { it.id == activeGroupId }
    val groupCharacters = myCharactersForGroup.filter { it.id in (activeGroup?.memberIds ?: emptyList()) && it.type == "PJ" }
    val availableGroupCharacters = groupCharacters.filter { it.id !in claimedCharacters.keys }
    val takenGroupCharacters = groupCharacters.filter { it.id in claimedCharacters.keys }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Serveur local") },
                navigationIcon = navigationIcon,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        },
        bottomBar = {
            // Bouton toujours visible et cliquable, même si le contenu au-dessus
            // (groupes, personnages, propositions, joueurs...) dépasse la hauteur
            // de l'écran : avant, il n'y avait pas de défilement et ce bouton
            // pouvait se retrouver hors-écran, inatteignable.
            // La barre de menu globale (AppBottomBar, ~80dp) est dessinée par-dessus les
            // écrans : marge basse pour que le bouton reste bien au-dessus d'elle.
            Surface(color = Color.Transparent) {
                Button(
                    onClick = onStopServer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 96.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Arrêter le serveur")
                }
            }
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LanCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF2E7D32), shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Serveur actif", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Les joueurs sur le même Wi-Fi peuvent maintenant rejoindre la partie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Page d'affichage table",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    if (webServerUrl != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                webServerUrl!!,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(webServerUrl!!)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copier l'adresse", tint = Color.White)
                            }
                        }
                        Text(
                            "Ouvrez cette adresse dans un navigateur, sur le même Wi-Fi, pour afficher la scène en cours à la table.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        // Joueurs sans l'appli : ils jouent leur tour de combat depuis le navigateur de leur téléphone.
                        val urlJoueur = webServerUrl!!.trimEnd('/') + "/joueur"
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                urlJoueur,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(urlJoueur)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copier l'adresse joueur", tint = Color.White)
                            }
                        }
                        Text(
                            "Page joueur : un joueur sans l'appli y choisit son personnage et joue son tour de combat (attaque, dés, actions) depuis son téléphone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    } else {
                        Text(
                            "Indisponible pour l'instant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            GroupSelector()

            Spacer(Modifier.height(20.dp))
            ImportsPartagesSelector()

            if (activeGroup != null) {
                Spacer(Modifier.height(20.dp))
                LanCard {
                Text("Personnages du groupe", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))

                if (groupCharacters.isEmpty()) {
                    Text(
                        "Ce groupe n'a aucun personnage associé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                } else {
                    availableGroupCharacters.forEach { character ->
                        ListItem(
                            headlineContent = { Text(character.name) },
                            supportingContent = { Text("${character.race} ${character.characterClass} — Niv. ${character.level}") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF2E7D32), shape = androidx.compose.foundation.shape.CircleShape)
                                )
                            },
                            trailingContent = { Text("Disponible", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32)) },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = Color.White,
                                supportingColor = Color.White
                            )
                        )
                    }
                    takenGroupCharacters.forEach { character ->
                        val clientId = claimedCharacters[character.id]
                        val clientName = clients.find { it.id == clientId }?.displayName ?: "Joueur"
                        ListItem(
                            headlineContent = { Text(character.name) },
                            supportingContent = { Text("Réservé par $clientName") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.White.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = Color.White,
                                supportingColor = Color.White
                            )
                        )
                    }
                }
                }
            }

            if (pendingProposals.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                LanCard {
                Text("Propositions de personnage", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                pendingProposals.forEach { (clientId, character) ->
                    val clientName = clients.find { it.id == clientId }?.displayName ?: "Joueur"
                    ListItem(
                        headlineContent = { Text(character.name) },
                        supportingContent = { Text("Proposé par $clientName") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { NetworkSessionManager.acceptProposal(clientId) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Accepter", tint = Color.White)
                                }
                                IconButton(onClick = { NetworkSessionManager.rejectProposal(clientId) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Refuser", tint = Color.White)
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = Color.White,
                            supportingColor = Color.White
                        )
                    )
                }
                }
            }

            Spacer(Modifier.height(20.dp))
            LanCard {
            Text("${clients.size} joueur(s) connecté(s)", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(12.dp))

            if (clients.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.GroupOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "En attente de joueurs…",
                        color = Color.White
                    )
                }
            } else {
                val sentCharacterByClient by NetworkSessionManager.sentCharacterByClient.collectAsState()
                val myCharacters by com.jc2.jdrcompagnon.ui.GameState.characters.collectAsState()
                Column {
                    clients.forEach { client ->
                        ClientRow(
                            client = client,
                            myCharacters = myCharacters,
                            hasSentCharacter = sentCharacterByClient.containsKey(client.id),
                            onSendCharacter = { character ->
                                NetworkSessionManager.pushCharacterToClient(client.id, character)
                            },
                            onRequestSync = { NetworkSessionManager.requestCharacterSync(client.id) }
                        )
                    }
                }
            }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Ligne d'un joueur connecté : nom, bouton pour lui envoyer un personnage
 * (choix parmi les personnages du MJ), et bouton pour redemander l'état
 * actuel du dernier personnage envoyé.
 */
@Composable
private fun ClientRow(
    client: com.jc2.jdrcompagnon.network.ConnectedClient,
    myCharacters: List<com.jc2.jdrcompagnon.ui.Character>,
    hasSentCharacter: Boolean,
    onSendCharacter: (com.jc2.jdrcompagnon.ui.Character) -> Unit,
    onRequestSync: () -> Unit
) {
    var showCharacterMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(client.displayName) },
        supportingContent = if (hasSentCharacter) {
            { Text("Personnage envoyé", style = MaterialTheme.typography.bodySmall) }
        } else null,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = Color.White,
            supportingColor = Color.White
        ),
        trailingContent = {
            Row {
                Box {
                    IconButton(onClick = { showCharacterMenu = true }) {
                        Icon(Icons.Default.Send, contentDescription = "Envoyer un personnage", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showCharacterMenu,
                        onDismissRequest = { showCharacterMenu = false }
                    ) {
                        if (myCharacters.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Aucun personnage") },
                                onClick = { showCharacterMenu = false },
                                enabled = false
                            )
                        } else {
                            myCharacters.forEach { character ->
                                DropdownMenuItem(
                                    text = { Text(character.name) },
                                    onClick = {
                                        onSendCharacter(character)
                                        showCharacterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (hasSentCharacter) {
                    IconButton(onClick = onRequestSync) {
                        Icon(Icons.Default.Sync, contentDescription = "Synchroniser", tint = Color.White)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinScreen(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onBack: () -> Unit,
) {
    if (!hasPermissions) {
        PermissionRequiredScreen(onRequestPermissions, onBack)
        return
    }
    val context = LocalContext.current
    val discovery = remember { NetworkSessionManager.discovery(context) }
    val servers by discovery.servers.collectAsState()
    val isScanning by discovery.isScanning.collectAsState()
    val playerState by NetworkSessionManager.playerState.collectAsState()

    // Même structure que HostScreen (Scaffold + TopAppBar) pour un calage
    // cohérent entre les deux écrans réseau : zone de retour standard,
    // padding système géré automatiquement au lieu d'un simple bouton en bas.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rejoindre une partie") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (playerState == PlayerConnectionState.CONNECTED) {
                CharacterClaimSection(onBack = onBack)
                return@Column
            }

            LanCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recherche de parties…", style = MaterialTheme.typography.titleMedium)
                    if (isScanning) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { discovery.stopDiscovery(); discovery.startDiscovery() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Relancer la recherche", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Assurez-vous d'être sur le même réseau Wi-Fi que le MJ (et coupez les données mobiles si la partie ne se trouve pas).",
                    style = MaterialTheme.typography.bodySmall
                )

                if (playerState == PlayerConnectionState.RECONNECTING) {
                    val lastError by NetworkSessionManager.lastConnectionError.collectAsState()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Connexion perdue, nouvelle tentative en cours…",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    lastError?.let {
                        Text(
                            "Détail : $it",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Le MJ a pu couper sa partie : sans ce bouton, le joueur restait
                // coincé en reconnexion infinie, sans moyen d'arrêter la session.
                if (playerState == PlayerConnectionState.RECONNECTING || playerState == PlayerConnectionState.CONNECTING) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { NetworkSessionManager.disconnect(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Arrêter et se déconnecter", color = Color.White) }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (servers.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LanCard {
                        Text(
                            "Aucune partie trouvée pour l'instant sur ce réseau Wi-Fi.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(servers) { server: DiscoveredServer ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(server.serviceName) },
                                supportingContent = {
                                    Text(
                                        if (playerState == PlayerConnectionState.CONNECTING) "Connexion en cours…"
                                        else "${server.host}:${server.port}"
                                    )
                                },
                                trailingContent = {
                                    if (playerState == PlayerConnectionState.CONNECTING) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                    }
                                },
                                colors = whiteListItemColors(),
                                modifier = Modifier.clickable(enabled = playerState != PlayerConnectionState.CONNECTING) {
                                    val playerName = com.jc2.jdrcompagnon.ui.GameState.playerName.value
                                        ?: com.jc2.jdrcompagnon.ui.GameState.assignRandomPlayerName()
                                    NetworkSessionManager.connectToServer(context, server, playerName = playerName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
/**
 * Affichée une fois connecté au MJ : liste des personnages du groupe encore
 * disponibles à réserver, ou — si aucun n'est disponible — proposition d'un
 * des personnages du joueur lui-même, en attente de validation du MJ.
 */
@Composable
private fun CharacterClaimSection(onBack: () -> Unit) {
    val context = LocalContext.current
    val availableCharacters by NetworkSessionManager.availableCharacters.collectAsState()
    val claimedCharacterId by NetworkSessionManager.claimedCharacterId.collectAsState()
    val proposalPending by NetworkSessionManager.proposalPending.collectAsState()
    val myCharacters by com.jc2.jdrcompagnon.ui.GameState.characters.collectAsState()
    val claimedCharacter = myCharacters.find { it.id == claimedCharacterId }
    var showProposeMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        LanCard {
        when {
            claimedCharacter != null -> {
                Text("Vous incarnez :", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text(claimedCharacter.name) },
                    supportingContent = { Text("${claimedCharacter.race} ${claimedCharacter.characterClass} — Niv. ${claimedCharacter.level}") },
                    colors = whiteListItemColors()
                )
            }
            proposalPending -> {
                Text("En attente de validation du MJ…", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(color = Color.White)
            }
            availableCharacters.isNotEmpty() -> {
                Text("Choisissez un personnage :", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(availableCharacters) { summary ->
                        ListItem(
                            headlineContent = { Text(summary.name) },
                            supportingContent = { Text("${summary.race} ${summary.characterClass} — Niv. ${summary.level}") },
                            colors = whiteListItemColors(),
                            modifier = Modifier.clickable {
                                NetworkSessionManager.requestClaimCharacter(summary.id)
                            }
                        )
                    }
                }
            }
            else -> {
                Text("Aucun personnage disponible pour l'instant.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vous pouvez proposer un de vos personnages au MJ.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    Button(onClick = { showProposeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Proposer un personnage")
                    }
                    DropdownMenu(
                        expanded = showProposeMenu,
                        onDismissRequest = { showProposeMenu = false }
                    ) {
                        if (myCharacters.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Aucun personnage sur cet appareil") },
                                onClick = { showProposeMenu = false },
                                enabled = false
                            )
                        } else {
                            myCharacters.forEach { character ->
                                DropdownMenuItem(
                                    text = { Text(character.name) },
                                    onClick = {
                                        NetworkSessionManager.proposeCharacter(character)
                                        showProposeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { NetworkSessionManager.disconnect(context) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Se déconnecter", color = Color.White) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour (rester connecté)", color = Color.White)
        }
    }
}