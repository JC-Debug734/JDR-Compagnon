package com.jc2.jdrcompagnon.ui.screens.mj

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import java.io.File
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.RuleSection
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdSectionEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdDocSection
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch

/**
 * Les 7 illustrations de couverture disponibles pour personnaliser l'apparence de
 * chaque livre sur l'étagère (voir [LibraryBookSettingsScreen]). Index utilisé comme
 * valeur stockée dans [LibraryBookSettingsStore] pour la couverture choisie.
 */
private val bookSkinDrawables = listOf(
    R.drawable.livre_1,
    R.drawable.livre_2,
    R.drawable.livre_3,
    R.drawable.livre_4,
    R.drawable.livre_5,
    R.drawable.livre_6,
    R.drawable.livre_7,
)

/**
 * Persistance (SharedPreferences, donc conservée à la fermeture de l'application) des
 * réglages par livre définis depuis l'écran de gestion (ouvert en cliquant la bibliothécaire) :
 * la couverture choisie et si le livre est visible pour les joueurs. Clé par monde + par
 * livre, pour pouvoir avoir des réglages différents selon le monde actif.
 */
private object LibraryBookSettingsStore {
    private const val PREFS_NAME = "library_book_settings"
    private const val NO_OVERRIDE = -1

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun skinKey(worldId: String?, tabKey: String) = "${worldId ?: "default"}:$tabKey:skin"
    private fun visibleKey(worldId: String?, tabKey: String) = "${worldId ?: "default"}:$tabKey:visible_to_players"

    /** Index de couverture choisi pour ce livre, ou null si aucun choix (cycle par défaut). */
    fun getSkinIndex(context: Context, worldId: String?, tabKey: String): Int? {
        val value = prefs(context).getInt(skinKey(worldId, tabKey), NO_OVERRIDE)
        return value.takeIf { it in bookSkinDrawables.indices }
    }

    fun setSkinIndex(context: Context, worldId: String?, tabKey: String, index: Int) {
        prefs(context).edit().putInt(skinKey(worldId, tabKey), index).apply()
    }

    /** Vrai par défaut : un livre est visible aux joueurs tant qu'on ne l'a pas masqué. */
    fun isVisibleToPlayers(context: Context, worldId: String?, tabKey: String): Boolean =
        prefs(context).getBoolean(visibleKey(worldId, tabKey), true)

    fun setVisibleToPlayers(context: Context, worldId: String?, tabKey: String, visible: Boolean) {
        prefs(context).edit().putBoolean(visibleKey(worldId, tabKey), visible).apply()
    }
}

/**
 * Un livre ajouté manuellement par l'utilisateur depuis l'écran de gestion des livres
 * (n'importe quel fichier, .md ou non, sélectionné via le sélecteur de fichiers système).
 * [fileName] est le nom du fichier copié dans le stockage interne de l'app (voir
 * [copyPickedFileToInternalStorage]) — la donnée reste disponible même si le fichier
 * d'origine est supprimé ou son URI révoquée.
 */
private data class CustomBook(
    val id: String,
    val name: String,
    val fileName: String,
)

/**
 * Persistance (SharedPreferences) de la liste des livres personnalisés ajoutés par
 * l'utilisateur, par monde. La couverture et la disponibilité aux joueurs de chaque livre
 * personnalisé réutilisent [LibraryBookSettingsStore] avec sa clé "custom_<id>", comme
 * n'importe quel autre livre — seuls le nom et le fichier associé sont propres à ce store.
 */
private object CustomBooksStore {
    private const val PREFS_NAME = "library_custom_books"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun idsKey(worldId: String?) = "${worldId ?: "default"}:ids"
    private fun nameKey(worldId: String?, id: String) = "${worldId ?: "default"}:$id:name"
    private fun fileKey(worldId: String?, id: String) = "${worldId ?: "default"}:$id:file"

    fun list(context: Context, worldId: String?): List<CustomBook> {
        val ids = prefs(context).getStringSet(idsKey(worldId), emptySet()).orEmpty()
        return ids.mapNotNull { id ->
            val name = prefs(context).getString(nameKey(worldId, id), null) ?: return@mapNotNull null
            val fileName = prefs(context).getString(fileKey(worldId, id), null) ?: return@mapNotNull null
            CustomBook(id = id, name = name, fileName = fileName)
        }.sortedBy { it.name.lowercase() }
    }

    /** Enregistre un nouveau livre personnalisé sous l'identifiant [id] (sans préfixe). */
    fun add(context: Context, worldId: String?, id: String, name: String, fileName: String) {
        val ids = prefs(context).getStringSet(idsKey(worldId), emptySet()).orEmpty().toMutableSet()
        ids.add(id)
        prefs(context).edit()
            .putStringSet(idsKey(worldId), ids)
            .putString(nameKey(worldId, id), name)
            .putString(fileKey(worldId, id), fileName)
            .apply()
    }

    fun remove(context: Context, worldId: String?, id: String) {
        val ids = prefs(context).getStringSet(idsKey(worldId), emptySet()).orEmpty().toMutableSet()
        ids.remove(id)
        prefs(context).edit()
            .putStringSet(idsKey(worldId), ids)
            .remove(nameKey(worldId, id))
            .remove(fileKey(worldId, id))
            .apply()
    }
}

/** Dossier interne où sont copiés les fichiers des livres personnalisés. */
private fun customBooksDir(context: Context): File =
    File(context.filesDir, "custom_books").apply { mkdirs() }

/**
 * Copie le contenu d'un fichier choisi via le sélecteur système dans le stockage interne
 * de l'app, sous un nom unique dérivé de [id]. On copie le contenu plutôt que de garder
 * l'URI d'origine : l'autorisation d'accès à une URI "content://" choisie ponctuellement
 * n'est pas garantie de survivre au redémarrage de l'app. Retourne le nom du fichier copié
 * (à passer à [CustomBooksStore.add]), ou null en cas d'échec de lecture.
 */
private fun copyPickedFileToInternalStorage(context: Context, uri: Uri, id: String, originalName: String?): String? {
    val extension = originalName?.substringAfterLast('.', missingDelimiterValue = "md") ?: "md"
    val fileName = "$id.$extension"
    val target = File(customBooksDir(context), fileName)
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        fileName
    } catch (e: Exception) {
        null
    }
}

/** Lit le contenu texte d'un livre personnalisé depuis le stockage interne. */
private fun readCustomBookContent(context: Context, fileName: String): String? =
    try {
        File(customBooksDir(context), fileName).readText()
    } catch (e: Exception) {
        null
    }

/** Nom d'affichage d'un fichier choisi via le sélecteur système (colonne DISPLAY_NAME). */
private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }

/**
 * Extrait le facteur de puissance (FP, ex-"Défi") et les PX d'un monstre depuis la ligne
 * "**FP :** X (Y PX...)" présente dans son bloc de statistiques (SRD 5.2.1). Tolère les
 * deux ordres "Y PX" et "PX Y" rencontrés selon les profils. Retourne null si non trouvé.
 */
private val monsterChallengeRegex =
    Regex("""\*\*FP\s*:?\*\*\s+([^(]+?)\s*\(\s*(?:PX\s*)?([\d][\d\s]*?)\s*(?:PX\s*)?[,;)]""")

fun monsterChallenge(entry: SrdEntry): Pair<String, String>? {
    val match = monsterChallengeRegex.find(entry.rawMarkdown) ?: return null
    return match.groupValues[1].trim() to match.groupValues[2].trim()
}

/**
 * Libellé affiché à droite dans la liste des monstres (ex: "FP 1/4 · 50 PX").
 */
fun monsterChallengeLabel(entry: SrdEntry): String? {
    val (cr, xp) = monsterChallenge(entry) ?: return null
    return "FP $cr · $xp PX"
}

/**
 * Clé de tri numérique pour un niveau de défi ("-" et fractions inclus), afin d'ordonner
 * les options du filtre de façon croissante plutôt qu'alphabétique.
 */
fun challengeSortKey(cr: String): Double = when (cr) {
    "-" -> -1.0
    "1/8" -> 0.125
    "1/4" -> 0.25
    "1/2" -> 0.5
    else -> cr.toDoubleOrNull() ?: 999.0
}

/**
 * Extrait le niveau d'un sort depuis la ligne italique qui suit son titre
 * (ex: "*Evocation de 2ème niveau*" -> "Niveau 2", "*Tour de magie de conjuration*" -> "Mineur").
 * Retourne null si aucun niveau n'est reconnu.
 */
fun spellLevelLabel(entry: SrdEntry): String? {
    val firstItalicLine = entry.rawMarkdown.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("*") && it.endsWith("*") && !it.startsWith("**") }
        ?.trim('*')
        ?.trim()
        ?.lowercase()
        ?: return null
    if ("tour de magie" in firstItalicLine || "cantrip" in firstItalicLine) return "Mineur"
    if ("niveau" !in firstItalicLine && "level" !in firstItalicLine) return null
    val level = Regex("""\d+""").find(firstItalicLine)?.value?.toIntOrNull() ?: return null
    return "Niveau $level"
}

/**
 * Rangée "Filtrer (N) / Réinitialiser" réutilisable au-dessus des listes filtrables.
 */
@Composable
private fun FilterButtonRow(
    activeFilterCount: Int,
    onOpen: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (activeFilterCount == 0) "Filtrer" else "Filtrer ($activeFilterCount)")
        }
        if (activeFilterCount > 0) {
            TextButton(onClick = onReset) {
                Text("Réinitialiser")
            }
        }
    }
}

/**
 * Ligne à cocher réutilisable pour les écrans de filtre (catégorie d'équipement,
 * école de magie...).
 */
@Composable
private fun CheckableFilterRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

/**
 * Écran de bibliothèque SRD avec onglets (Monstres, Sorts, Règles, Équipement, Glossaire).
 *
 * @param currentWorld Le monde actuellement sélectionné
 * @param onBack Action de retour
 * @param initialTab Onglet pré-sélectionné ("monsters" ou "spells"), par défaut "monsters"
 * @param onMonsterClick Callback naviguant vers le détail d'un monstre
 * @param onSpellClick Callback naviguant vers le détail d'un sort
 * @param onEquipmentClick Callback naviguant vers le détail d'un équipement
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    currentWorld: WorldState?,
    onBack: () -> Unit,
    initialTab: String = "monsters",
    initialSectionTitle: String? = null,
    onMonsterClick: (String) -> Unit = {},
    onSpellClick: (String) -> Unit = {},
    onEquipmentClick: (EquipmentItem) -> Unit = {},
    onClasseClick: (String) -> Unit = {},
    onEspeceClick: (String) -> Unit = {},
    onHistoriqueClick: (String) -> Unit = {},
    onDonClick: (String) -> Unit = {},
    onArmeArmureMagiqueClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val tabs = listOf(
        "Monstres", "Sorts", "Règles", "Équipement", "Glossaire",
        "Classes", "Espèces", "Historiques", "Dons", "Armes magiques", "Montures",
    )
    val tabIcons = listOf(
        Icons.Filled.Pets,
        Icons.Filled.AutoAwesome,
        Icons.Filled.Gavel,
        Icons.Filled.Shield,
        Icons.AutoMirrored.Filled.MenuBook,
        Icons.Filled.School,
        Icons.Filled.Groups,
        Icons.Filled.History,
        Icons.Filled.Star,
        Icons.Filled.Bolt,
        Icons.Filled.DirectionsCar,
    )
    val tabKeys = listOf(
        "monsters", "spells", "rules", "equipment", "glossary",
        "classes", "especes", "historiques", "dons", "armes_magiques", "montures",
    )
    // Livres personnalisés ajoutés par l'utilisateur (fichier .md ou autre, couverture et
    // nom choisis depuis l'écran de gestion des livres) ; rechargés à chaque changement de
    // monde ou de réglages (voir plus bas). Déclaré ici, avant `libraryBooks`, qui en a
    // besoin : Kotlin exige qu'une variable locale soit déclarée avant son utilisation.
    var customBooks by remember { mutableStateOf<List<CustomBook>>(emptyList()) }
    val libraryBooks = remember(tabs, customBooks) {
        tabs.mapIndexed { index, label ->
            LibraryBook(label = label, icon = tabIcons[index], tabIndex = index, tabKey = tabKeys[index])
        } + customBooks.mapIndexed { index, book ->
            LibraryBook(
                label = book.name,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tabIndex = tabs.size + index,
                tabKey = "custom_${book.id}",
            )
        }
    }
    val initialIndex = tabKeys.indexOf(initialTab).coerceAtLeast(0)
    var selectedTab by rememberSaveable { mutableStateOf(initialIndex) }
    // Vue étagère (accueil) par défaut ; si l'appelant demande explicitement un onglet
    // autre que celui par défaut ("monsters"), on considère que c'est un lien direct
    // et on saute l'étagère pour aller droit au contenu.
    var showShelf by rememberSaveable { mutableStateOf(initialTab == "monsters") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // Filtre par catégorie pour l'onglet Équipement : vide = toutes les catégories affichées
    var selectedEquipmentCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par école de magie pour l'onglet Sorts : vide = toutes les écoles affichées
    var selectedSpellLevels by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par niveau de défi pour l'onglet Monstres : vide = tous les défis affichés
    var selectedChallenges by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par catégorie pour l'onglet Dons : vide = toutes les catégories affichées
    var selectedDonCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par catégorie pour l'onglet Armes magiques : vide = toutes les catégories affichées
    var selectedArmeMagiqueCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()

    // Recherche globale (icône bibliothécaire) : cherche un mot-clé dans le nom ET le
    // contenu de tous les livres à la fois, contrairement à `searchQuery` qui ne filtre
    // que par nom dans le livre actuellement ouvert. Toujours visible sur l'étagère (plus
    // besoin de toucher la bibliothécaire pour la faire apparaître) : jamais remise à
    // false ailleurs dans ce fichier, seul son texte se vide.
    var showGlobalSearch by rememberSaveable { mutableStateOf(true) }
    var globalSearchQuery by rememberSaveable { mutableStateOf("") }
    // Cible de défilement pour les livres à page unique (Règles, Montures) : initialisée
    // par le lien profond éventuel, puis mise à jour quand on clique un résultat de
    // recherche globale pointant vers une section de l'un de ces deux livres.
    var activeRuleSectionTarget by remember { mutableStateOf(initialSectionTitle) }
    var activeMontureSectionTarget by remember { mutableStateOf<String?>(null) }

    // Écran de gestion des livres, désormais ouvert en cliquant la bibliothécaire :
    // couverture personnalisée + disponibilité aux joueurs pour chaque livre, réglages
    // persistés dans LibraryBookSettingsStore, plus l'ajout de livres personnalisés
    // (CustomBooksStore). `bookSettingsVersion` est incrémenté à chaque modification pour
    // forcer l'étagère à relire les réglages (une SharedPreferences modifiée ne recompose
    // rien toute seule).
    var showBookSettings by rememberSaveable { mutableStateOf(false) }
    var bookSettingsVersion by remember { mutableStateOf(0) }

    // Livre personnalisé actuellement affiché, le cas échéant (`customBooks` lui-même est
    // déclaré plus haut, avant `libraryBooks` qui en a besoin) : pilote l'affichage de son
    // contenu, en parallèle du mécanisme `selectedTab`/`tabKeys` réservé aux livres intégrés.
    var selectedCustomBookId by rememberSaveable { mutableStateOf<String?>(null) }

    // États de chargement
    var monsters by remember { mutableStateOf<List<SrdEntry>?>(null) }
    var spells by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var ruleSections by remember { mutableStateOf<List<RuleSection>?>(null) }
    var equipment by remember { mutableStateOf<List<EquipmentItem>?>(null) }
    var glossaryMarkdown by remember { mutableStateOf<String?>(null) }
    var classes by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var especes by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var historiques by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var dons by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var armesMagiques by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var montures by remember { mutableStateOf<List<SrdDocSection>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // S'assurer que les données sont rechargées quand le monde change
    val worldIdKey = currentWorld?.id

    // Recharge la liste des livres personnalisés à chaque changement de monde et à chaque
    // ajout/suppression (bookSettingsVersion, incrémenté par onSettingChanged côté écran
    // de gestion des livres).
    LaunchedEffect(worldIdKey, bookSettingsVersion) {
        customBooks = CustomBooksStore.list(context, worldIdKey)
    }

    // Chargement des données selon l'onglet sélectionné ET du monde
    LaunchedEffect(selectedTab, worldIdKey) {
        val worldId = currentWorld?.id
        // Réinitialiser les données monde-dépendantes quand le monde change
        if (worldIdKey == null || worldId != worldIdKey) {
            monsters = null
            spells = null
            ruleSections = null
            equipment = null
            glossaryMarkdown = null
            classes = null
            especes = null
            historiques = null
            dons = null
            armesMagiques = null
            montures = null
            selectedEquipmentCategories = emptySet()
            selectedSpellLevels = emptySet()
            selectedChallenges = emptySet()
            selectedDonCategories = emptySet()
            selectedArmeMagiqueCategories = emptySet()
            activeRuleSectionTarget = null
            activeMontureSectionTarget = null
        }
        when (tabKeys[selectedTab]) {
            "monsters" -> {
                if (monsters == null) {
                    try {
                        monsters = SrdRepository.loadMonsters(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "spells" -> {
                if (spells == null) {
                    try {
                        spells = SrdRepository.loadSpellsIndex(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "rules" -> {
                if (ruleSections == null) {
                    try {
                        ruleSections = SrdRepository.loadRuleSections(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "equipment" -> {
                if (equipment == null) {
                    try {
                        equipment = SrdRepository.loadEquipmentList(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "glossary" -> {
                if (glossaryMarkdown == null) {
                    try {
                        glossaryMarkdown = SrdRepository.loadGlossary(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "classes" -> {
                if (classes == null) {
                    try {
                        classes = SrdRepository.loadClasses(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "especes" -> {
                if (especes == null) {
                    try {
                        especes = SrdRepository.loadEspeces(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "historiques" -> {
                if (historiques == null) {
                    try {
                        historiques = SrdRepository.loadHistoriques(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "dons" -> {
                if (dons == null) {
                    try {
                        dons = SrdRepository.loadDons(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "armes_magiques" -> {
                if (armesMagiques == null) {
                    try {
                        armesMagiques = SrdRepository.loadArmesArmuresMagiques(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "montures" -> {
                if (montures == null) {
                    try {
                        montures = SrdRepository.loadMonturesVehicules(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
        }
    }

    // Chargement de TOUTES les catégories dès l'ouverture de la recherche globale (et
    // non plus seulement celle de l'onglet actif), pour pouvoir chercher un mot-clé
    // dans tous les livres à la fois. Chaque catégorie déjà chargée (ex. via l'onglet
    // actif) n'est pas rechargée.
    LaunchedEffect(showGlobalSearch, worldIdKey) {
        if (!showGlobalSearch) return@LaunchedEffect
        val worldId = currentWorld?.id
        try {
            if (monsters == null) monsters = SrdRepository.loadMonsters(context, worldId)
            if (spells == null) spells = SrdRepository.loadSpellsIndex(context, worldId)
            if (ruleSections == null) ruleSections = SrdRepository.loadRuleSections(context, worldId)
            if (equipment == null) equipment = SrdRepository.loadEquipmentList(context, worldId)
            if (glossaryMarkdown == null) glossaryMarkdown = SrdRepository.loadGlossary(context, worldId)
            if (classes == null) classes = SrdRepository.loadClasses(context, worldId)
            if (especes == null) especes = SrdRepository.loadEspeces(context, worldId)
            if (historiques == null) historiques = SrdRepository.loadHistoriques(context, worldId)
            if (dons == null) dons = SrdRepository.loadDons(context, worldId)
            if (armesMagiques == null) armesMagiques = SrdRepository.loadArmesArmuresMagiques(context, worldId)
            if (montures == null) montures = SrdRepository.loadMonturesVehicules(context, worldId)
        } catch (e: Exception) {
            loadError = "Erreur de chargement : ${e.message}"
        }
    }

    // Résultats de la recherche globale : cherche `globalSearchQuery` dans le nom ET le
    // contenu de chaque entrée, toutes catégories confondues. `onSelect` sait exactement
    // où naviguer pour chaque type de livre.
    val globalSearchResults = remember(
        globalSearchQuery, monsters, spells, ruleSections, equipment, glossaryMarkdown,
        classes, especes, historiques, dons, armesMagiques, montures,
    ) {
        val query = globalSearchQuery.trim()
        if (query.length < 2) return@remember emptyList<GlobalSearchResult>()

        buildList {
            monsters?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Monstres") {
                        selectedTab = tabKeys.indexOf("monsters")
                        showShelf = false
                        globalSearchQuery = ""
                        onMonsterClick(entry.name)
                    })
                }
            }
            spells?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Sorts") {
                        selectedTab = tabKeys.indexOf("spells")
                        showShelf = false
                        globalSearchQuery = ""
                        onSpellClick(entry.name)
                    })
                }
            }
            ruleSections?.forEach { section ->
                if (section.title.contains(query, ignoreCase = true) || section.content.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(section.title, "Règles") {
                        selectedTab = tabKeys.indexOf("rules")
                        showShelf = false
                        globalSearchQuery = ""
                        activeRuleSectionTarget = section.title
                    })
                }
            }
            equipment?.forEach { item ->
                if (item.name.contains(query, ignoreCase = true) || item.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(item.name, "Équipement") {
                        selectedTab = tabKeys.indexOf("equipment")
                        showShelf = false
                        globalSearchQuery = ""
                        onEquipmentClick(item)
                    })
                }
            }
            glossaryMarkdown?.let { md ->
                if (md.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult("Glossaire", "Glossaire") {
                        selectedTab = tabKeys.indexOf("glossary")
                        showShelf = false
                        globalSearchQuery = ""
                    })
                }
            }
            classes?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Classes") {
                        selectedTab = tabKeys.indexOf("classes")
                        showShelf = false
                        globalSearchQuery = ""
                        onClasseClick(entry.name)
                    })
                }
            }
            especes?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Espèces") {
                        selectedTab = tabKeys.indexOf("especes")
                        showShelf = false
                        globalSearchQuery = ""
                        onEspeceClick(entry.name)
                    })
                }
            }
            historiques?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Historiques") {
                        selectedTab = tabKeys.indexOf("historiques")
                        showShelf = false
                        globalSearchQuery = ""
                        onHistoriqueClick(entry.name)
                    })
                }
            }
            dons?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Dons") {
                        selectedTab = tabKeys.indexOf("dons")
                        showShelf = false
                        globalSearchQuery = ""
                        onDonClick(entry.name)
                    })
                }
            }
            armesMagiques?.forEach { entry ->
                if (entry.name.contains(query, ignoreCase = true) || entry.rawMarkdown.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(entry.name, "Armes magiques") {
                        selectedTab = tabKeys.indexOf("armes_magiques")
                        showShelf = false
                        globalSearchQuery = ""
                        onArmeArmureMagiqueClick(entry.name)
                    })
                }
            }
            montures?.forEach { section ->
                if (section.title.contains(query, ignoreCase = true) || section.content.contains(query, ignoreCase = true)) {
                    add(GlobalSearchResult(section.title, "Montures") {
                        selectedTab = tabKeys.indexOf("montures")
                        showShelf = false
                        globalSearchQuery = ""
                        activeMontureSectionTarget = section.title
                    })
                }
            }
        }
    }

    // Fond sombre uniforme (ForcedDarkPalette), aligné sur le reste de l'app
    // (MainActivity, AppBottomBar) plutôt que le fond texturé par monde de
    // WorldBackground, qui tranchait avec la barre de navigation du bas.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when {
                                    showBookSettings -> "Réglages des livres"
                                    showShelf -> "Bibliothèque"
                                    selectedCustomBookId != null ->
                                        customBooks.find { it.id == selectedCustomBookId }?.name.orEmpty()
                                    else -> tabs.getOrNull(selectedTab).orEmpty()
                                },
                                fontWeight = FontWeight.Bold,
                            )
                            currentWorld?.name?.let { worldName ->
                                Text(
                                    text = worldName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                showBookSettings -> showBookSettings = false
                                showShelf && globalSearchQuery.isNotBlank() -> globalSearchQuery = ""
                                showShelf -> onBack()
                                else -> {
                                    selectedCustomBookId = null
                                    showShelf = true
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            containerColor = ForcedDarkPalette.Background,
        ) { innerPadding ->
            if (!SrdRepository.isLibraryAvailable(currentWorld?.id)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune bibliothèque disponible pour ${currentWorld?.name ?: "cet univers"}.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                return@Scaffold
            }

            if (showShelf) {
                val trimmedGlobalQuery = globalSearchQuery.trim()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showBookSettings) {
                        LibraryBookSettingsScreen(
                            books = libraryBooks,
                            worldId = currentWorld?.id,
                            onSettingChanged = { bookSettingsVersion++ },
                        )
                    } else {
                        if (showGlobalSearch) {
                            // Réserve de l'espace à droite (padding end) pour que le champ ne
                            // passe jamais sous la bibliothécaire, superposée en absolu par-dessus
                            // tout l'écran (voir plus bas). La roue crantée qui ouvrait autrefois
                            // les réglages des livres est partie : c'est la bibliothécaire elle-même
                            // qui les ouvre désormais.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 132.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = globalSearchQuery,
                                    onValueChange = { globalSearchQuery = it },
                                    placeholder = { Text("Comment puis-je vous aider ?") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                                    trailingIcon = {
                                        if (globalSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { globalSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Effacer la recherche")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                )
                            }
                        }

                        if (showGlobalSearch && trimmedGlobalQuery.length >= 2) {
                            if (globalSearchResults.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Aucun résultat pour « $trimmedGlobalQuery ».",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp),
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(
                                        globalSearchResults,
                                        key = { it.categoryLabel + "_" + it.entryName },
                                    ) { result ->
                                        Surface(
                                            onClick = result.onSelect,
                                            color = Color.Transparent,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            ) {
                                                Text(
                                                    text = result.entryName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    text = result.categoryLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        } else {
                            LibraryBookshelf(
                                books = libraryBooks,
                                modifier = Modifier.fillMaxSize(),
                                worldId = currentWorld?.id,
                                settingsVersion = bookSettingsVersion,
                                onBookClick = { index ->
                                    if (index < tabs.size) {
                                        selectedTab = index
                                        selectedCustomBookId = null
                                    } else {
                                        selectedCustomBookId = customBooks.getOrNull(index - tabs.size)?.id
                                    }
                                    showShelf = false
                                },
                            )
                        }
                    }
                }
                return@Scaffold
            }

            if (selectedCustomBookId != null) {
                val book = customBooks.find { it.id == selectedCustomBookId }
                val content = remember(book?.fileName) {
                    book?.fileName?.let { readCustomBookContent(context, it) }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when {
                        book == null -> Text(
                            text = "Ce livre n'existe plus.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        content == null -> Text(
                            text = "Impossible de lire le contenu de ce fichier.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> Markdown(content = content)
                    }
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Barre de recherche (marge de droite réservée pour la bibliothécaire, qui
                // flotte par-dessus tous les écrans de la bibliothèque, pas seulement l'étagère)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 132.dp, top = 8.dp, bottom = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )

                // Le bandeau d'onglets a été abandonné : la navigation entre catégories
                // se fait uniquement via l'étagère (retour avec la flèche du haut).

                // Bouton de filtre, uniquement pour l'onglet Équipement
                if (tabKeys[selectedTab] == "equipment") {
                    val availableCategories = equipment?.map { it.category }?.distinct().orEmpty()
                    if (availableCategories.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedEquipmentCategories.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedEquipmentCategories = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Monstres
                if (tabKeys[selectedTab] == "monsters") {
                    val availableChallenges = monsters?.mapNotNull { monsterChallenge(it)?.first }?.distinct().orEmpty()
                    if (availableChallenges.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedChallenges.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedChallenges = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Sorts
                if (tabKeys[selectedTab] == "spells") {
                    val availableSpellLevels = spells?.map { it.category }?.distinct().orEmpty()
                    if (availableSpellLevels.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedSpellLevels.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedSpellLevels = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Dons
                if (tabKeys[selectedTab] == "dons") {
                    val availableDonCategories = dons?.map { it.category }?.distinct().orEmpty()
                    if (availableDonCategories.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedDonCategories.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedDonCategories = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Armes magiques
                if (tabKeys[selectedTab] == "armes_magiques") {
                    val availableArmeCategories = armesMagiques?.map { it.category }?.distinct().orEmpty()
                    if (availableArmeCategories.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedArmeMagiqueCategories.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedArmeMagiqueCategories = emptySet() },
                        )
                    }
                }

                // Contenu selon l'onglet
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tabKeys[selectedTab]) {
                        "monsters" -> {
                            val monsterList = monsters
                            if (monsterList == null) {
                                LoadingBox()
                            } else {
                                val filtered = if (selectedChallenges.isEmpty()) {
                                    monsterList
                                } else {
                                    monsterList.filter { monsterChallenge(it)?.first in selectedChallenges }
                                }
                                val bySearch = if (searchQuery.isBlank()) {
                                    filtered
                                } else {
                                    filtered.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                AlphabeticalEntryList(
                                    entries = bySearch,
                                    onItemClick = { onMonsterClick(it.name) },
                                    trailingLabel = { monsterChallengeLabel(it) },
                                )
                            }
                        }
                        "spells" -> {
                            val spellList = spells
                            if (spellList == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    spellList
                                } else {
                                    spellList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                val filtered = if (selectedSpellLevels.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedSpellLevels }
                                }
                                SectionEntryByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onSpellClick(it.name) },
                                    subtitle = { it.rawMarkdown },
                                )
                            }
                        }
                        "rules" -> {
                            val sections = ruleSections
                            if (sections == null) {
                                LoadingBox()
                            } else {
                                RulesTocScreen(
                                    sections = sections,
                                    initialSectionTitle = activeRuleSectionTarget
                                )
                            }
                        }
                        "equipment" -> {
                            val equipmentList = equipment
                            if (equipmentList == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    equipmentList
                                } else {
                                    equipmentList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                val filtered = if (selectedEquipmentCategories.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedEquipmentCategories }
                                }
                                EquipmentByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onEquipmentClick(it) }
                                )
                            }
                        }
                        "glossary" -> {
                            val md = glossaryMarkdown
                            if (md == null) {
                                LoadingBox()
                            } else {
                                MarkdownScrollColumn(md)
                            }
                        }
                        "classes" -> {
                            val list = classes
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                SectionEntryAlphabeticalList(
                                    entries = bySearch,
                                    onItemClick = { onClasseClick(it.name) },
                                )
                            }
                        }
                        "especes" -> {
                            val list = especes
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                SectionEntryAlphabeticalList(
                                    entries = bySearch,
                                    onItemClick = { onEspeceClick(it.name) },
                                )
                            }
                        }
                        "historiques" -> {
                            val list = historiques
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                SectionEntryAlphabeticalList(
                                    entries = bySearch,
                                    onItemClick = { onHistoriqueClick(it.name) },
                                )
                            }
                        }
                        "dons" -> {
                            val list = dons
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                val filtered = if (selectedDonCategories.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedDonCategories }
                                }
                                SectionEntryByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onDonClick(it.name) },
                                )
                            }
                        }
                        "armes_magiques" -> {
                            val list = armesMagiques
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                val filtered = if (selectedArmeMagiqueCategories.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedArmeMagiqueCategories }
                                }
                                SectionEntryByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onArmeArmureMagiqueClick(it.name) },
                                )
                            }
                        }
                        "montures" -> {
                            val sections = montures
                            if (sections == null) {
                                LoadingBox()
                            } else {
                                DocSectionTocScreen(
                                    sections = sections,
                                    initialSectionTitle = activeMontureSectionTarget,
                                )
                            }
                        }
                    }

                    loadError?.let { error ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (showFilterSheet) {
                val activeTabKey = tabKeys[selectedTab]
                val availableCategories = equipment?.map { it.category }?.distinct().orEmpty()
                val availableSpellLevels = spells?.map { it.category }?.distinct().orEmpty()
                val availableChallenges = monsters
                    ?.mapNotNull { monsterChallenge(it)?.first }
                    ?.distinct()
                    ?.sortedBy { challengeSortKey(it) }
                    .orEmpty()
                val availableDonCategories = dons?.map { it.category }?.distinct().orEmpty()
                val availableArmeCategories = armesMagiques?.map { it.category }?.distinct().orEmpty()
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = filterSheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = when (activeTabKey) {
                                "spells" -> "Filtrer les sorts"
                                "monsters" -> "Filtrer les monstres"
                                "dons" -> "Filtrer les dons"
                                "armes_magiques" -> "Filtrer les armes magiques"
                                else -> "Filtrer l'équipement"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        when (activeTabKey) {
                            "spells" -> {
                                Text(
                                    text = "Niveau",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableSpellLevels.forEach { level ->
                                    CheckableFilterRow(
                                        label = level,
                                        checked = level in selectedSpellLevels,
                                        onToggle = { checked ->
                                            selectedSpellLevels = if (checked) {
                                                selectedSpellLevels + level
                                            } else {
                                                selectedSpellLevels - level
                                            }
                                        },
                                    )
                                }
                            }
                            "monsters" -> {
                                Text(
                                    text = "Niveau de défi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableChallenges.forEach { challenge ->
                                    CheckableFilterRow(
                                        label = challenge,
                                        checked = challenge in selectedChallenges,
                                        onToggle = { checked ->
                                            selectedChallenges = if (checked) {
                                                selectedChallenges + challenge
                                            } else {
                                                selectedChallenges - challenge
                                            }
                                        },
                                    )
                                }
                            }
                            "dons" -> {
                                Text(
                                    text = "Catégorie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableDonCategories.forEach { category ->
                                    CheckableFilterRow(
                                        label = category,
                                        checked = category in selectedDonCategories,
                                        onToggle = { checked ->
                                            selectedDonCategories = if (checked) {
                                                selectedDonCategories + category
                                            } else {
                                                selectedDonCategories - category
                                            }
                                        },
                                    )
                                }
                            }
                            "armes_magiques" -> {
                                Text(
                                    text = "Catégorie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableArmeCategories.forEach { category ->
                                    CheckableFilterRow(
                                        label = category,
                                        checked = category in selectedArmeMagiqueCategories,
                                        onToggle = { checked ->
                                            selectedArmeMagiqueCategories = if (checked) {
                                                selectedArmeMagiqueCategories + category
                                            } else {
                                                selectedArmeMagiqueCategories - category
                                            }
                                        },
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Catégorie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableCategories.forEach { category ->
                                    CheckableFilterRow(
                                        label = category,
                                        checked = category in selectedEquipmentCategories,
                                        onToggle = { checked ->
                                            selectedEquipmentCategories = if (checked) {
                                                selectedEquipmentCategories + category
                                            } else {
                                                selectedEquipmentCategories - category
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = {
                                when (activeTabKey) {
                                    "spells" -> selectedSpellLevels = emptySet()
                                    "monsters" -> selectedChallenges = emptySet()
                                    "dons" -> selectedDonCategories = emptySet()
                                    "armes_magiques" -> selectedArmeMagiqueCategories = emptySet()
                                    else -> selectedEquipmentCategories = emptySet()
                                }
                            }) {
                                Text("Réinitialiser")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { showFilterSheet = false }) {
                                Text("Appliquer")
                            }
                        }
                    }
                }
            }
        }

        // La bibliothécaire : superposée par-dessus tout l'écran (et non dans le TopAppBar,
        // dont la hauteur fixe rognait toute icône plus grande que la barre elle-même).
        // Toujours accessible, ouvre l'écran de gestion des livres (couverture, disponibilité
        // aux joueurs, ajout d'un livre personnalisé) et ramène à l'étagère. La recherche
        // globale, elle, est désormais toujours visible en haut de l'étagère (plus besoin de
        // la bibliothécaire pour l'ouvrir) — padding du haut encore augmenté pour que la tête
        // ne soit plus du tout rognée en haut de l'écran (au-dessus de la zone sûre, sous la
        // barre de statut), taille remontée à 121dp (+10% par rapport aux 110dp précédents).
        Image(
            painter = painterResource(R.drawable.av_bliblio),
            contentDescription = "Réglages des livres",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 8.dp)
                .size(121.dp)
                .clickable {
                    showShelf = true
                    showBookSettings = true
                },
        )
    }
}

/**
 * En-tête "Table des matières" pliable, réutilisé par tous les écrans de bibliothèque
 * qui affichent une table des matières (Règles, Montures...) : reste toujours visible
 * en haut de l'écran (il n'est pas dans la zone défilante), et un clic dessus replie ou
 * déplie la liste des entrées en dessous. La liste, quand dépliée, a une hauteur maximale
 * fixe et défile en interne (`heightIn(max=...)` + son propre `verticalScroll`) : c'est
 * ce qui manquait avant et qui laissait la table des matières prendre tout l'écran quand
 * elle contenait beaucoup d'entrées, ne laissant plus de place (ni de défilement possible)
 * au contenu réel en dessous.
 */
@Composable
private fun CollapsibleTableOfContents(
    titles: List<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEntryClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Table des matières",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Replier la table des matières" else "Déplier la table des matières",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                titles.forEachIndexed { index, title ->
                    Surface(
                        onClick = { onEntryClick(index) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• $title",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Écran des règles avec table des matières cliquable et contenu par section.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RulesTocScreen(
    sections: List<RuleSection>,
    initialSectionTitle: String? = null
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var tocExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(sections, initialSectionTitle) {
        initialSectionTitle?.let { target ->
            val index = sections.indexOfFirst { it.title.equals(target, ignoreCase = true) }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table des matières : toujours visible en haut, pliable/dépliable au clic.
        CollapsibleTableOfContents(
            titles = sections.map { it.title },
            expanded = tocExpanded,
            onToggleExpanded = { tocExpanded = !tocExpanded },
            onEntryClick = { index ->
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            },
        )

        HorizontalDivider()

        // Sections : weight(1f) indispensable ici pour que cette liste dispose toujours
        // de tout l'espace restant (et donc défile correctement), quelle que soit la
        // taille de la table des matières au-dessus.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(sections, key = { it.title }) { section ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Markdown(content = section.content)
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * Colonne scrollable affichant du contenu markdown, avec un avertissement
 * indiquant que le contenu SRD est en anglais (traduction en cours).
 */
@Composable
private fun MarkdownScrollColumn(markdownContent: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "⚠️ Contenu en anglais (SRD D&D 5.1). Traduction en cours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
        Markdown(content = markdownContent)
    }
}

/**
 * Box centrée avec CircularProgressIndicator.
 */
@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Liste alphabétique avec sticky headers et cellules cliquables.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlphabeticalEntryList(
    entries: List<SrdEntry>,
    onItemClick: (SrdEntry) -> Unit,
    trailingLabel: ((SrdEntry) -> String?)? = null,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun résultat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Grouper par première lettre
    val grouped = entries.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (letter, items) ->
            stickyHeader(key = "header_$letter") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = letter.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(items = items, key = { index, entry -> "entry_${entry.name}_${index}" }) { _, entry ->
                SrdEntryRow(
                    entry = entry,
                    onClick = { onItemClick(entry) },
                    trailingLabel = trailingLabel?.invoke(entry),
                )
            }
        }
    }
}

/**
 * Liste groupée par catégorie avec sticky headers de catégorie.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EquipmentByCategoryList(
    entries: List<EquipmentItem>,
    onItemClick: (EquipmentItem) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun équipement disponible",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Conserver l'ordre d'apparition des catégories dans le fichier
    val categoryOrder = entries.map { it.category }.distinct()
    val grouped = entries
        .filter { it.category.isNotBlank() && it.category != "Catégorie" }
        .groupBy { it.category }
        .toSortedMap(compareBy { categoryOrder.indexOf(it) })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (category, items) ->
            stickyHeader(key = "cat_header_$category") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(
                items = items.sortedBy { it.name.lowercase() },
                key = { index, entry -> "entry_${entry.category}_${entry.name}_${index}" }
            ) { _, entry ->
                EquipmentItemRow(entry = entry, onClick = { onItemClick(entry) })
            }
        }
    }
}

/**
 * Liste alphabétique avec sticky headers pour les entrées génériques SRD 5.2.1
 * (classes, espèces, historiques) qui n'ont pas de catégorie significative.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionEntryAlphabeticalList(
    entries: List<SrdSectionEntry>,
    onItemClick: (SrdSectionEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun résultat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val grouped = entries.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (letter, items) ->
            stickyHeader(key = "header_$letter") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = letter.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(items = items, key = { index, entry -> "entry_${entry.name}_${index}" }) { _, entry ->
                SectionEntryRow(name = entry.name, category = "", onClick = { onItemClick(entry) })
            }
        }
    }
}

/**
 * Liste groupée par catégorie avec sticky headers de catégorie, pour les entrées
 * génériques SRD 5.2.1 à deux niveaux (dons, armes/armures magiques).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionEntryByCategoryList(
    entries: List<SrdSectionEntry>,
    onItemClick: (SrdSectionEntry) -> Unit,
    subtitle: ((SrdSectionEntry) -> String)? = null,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun résultat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Conserver l'ordre d'apparition des catégories dans le fichier
    val categoryOrder = entries.map { it.category }.distinct()
    val grouped = entries
        .filter { it.category.isNotBlank() }
        .groupBy { it.category }
        .toSortedMap(compareBy { categoryOrder.indexOf(it) })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (category, items) ->
            stickyHeader(key = "cat_header_$category") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(
                items = items.sortedBy { it.name.lowercase() },
                key = { index, entry -> "entry_${entry.category}_${entry.name}_${index}" }
            ) { _, entry ->
                SectionEntryRow(
                    name = entry.name,
                    category = subtitle?.invoke(entry).orEmpty(),
                    onClick = { onItemClick(entry) },
                )
            }
        }
    }
}

/**
 * Écran de sections libres (titre + contenu Markdown) avec table des matières
 * cliquable, pour les documents SRD 5.2.1 sans entrées nommées individuelles
 * (ex. montures et véhicules). Reprend la structure de [RulesTocScreen].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocSectionTocScreen(
    sections: List<SrdDocSection>,
    initialSectionTitle: String? = null,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var tocExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(sections, initialSectionTitle) {
        initialSectionTitle?.let { target ->
            val index = sections.indexOfFirst { it.title.equals(target, ignoreCase = true) }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table des matières : toujours visible en haut, pliable/dépliable au clic.
        CollapsibleTableOfContents(
            titles = sections.map { it.title },
            expanded = tocExpanded,
            onToggleExpanded = { tocExpanded = !tocExpanded },
            onEntryClick = { index ->
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            },
        )

        HorizontalDivider()

        // weight(1f) indispensable : sans ça, la table des matières (Column sans
        // hauteur bornée) pouvait s'étendre sur presque tout l'écran quand elle
        // contenait beaucoup d'entrées, ne laissant plus de place — ni de défilement
        // possible — à cette LazyColumn, d'où le contenu qui ne défilait pas.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(sections, key = { it.title }) { section ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Markdown(content = section.content)
                    }
                }
            }
        }
    }
}

/**
 * Ligne réutilisable pour une entrée générique SRD 5.2.1 (nom + catégorie optionnelle),
 * visuellement identique à [SrdEntryRow] mais indépendante du type [SrdEntry].
 */
@Composable
private fun SectionEntryRow(
    name: String,
    category: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (category.isNotBlank()) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Représente un onglet de la bibliothèque sous la forme d'un livre sur l'étagère
 * d'accueil. [tabIndex] correspond à l'index dans `tabKeys`/`tabs` de [LibraryScreen].
 * [tabKey] est la clé stable ("monsters", "rules"...) utilisée pour retrouver les
 * réglages persistés de ce livre dans [LibraryBookSettingsStore].
 */
private data class LibraryBook(
    val label: String,
    val icon: ImageVector,
    val tabIndex: Int,
    val tabKey: String,
)

/**
 * Un résultat de la recherche globale (icône "bibliothécaire") : un mot-clé trouvé dans
 * le nom OU le contenu d'une entrée, dans n'importe quel livre. [onSelect] encapsule la
 * navigation exacte (changer d'onglet puis ouvrir le détail, ou faire défiler jusqu'à la
 * bonne section pour les livres à page unique comme Règles/Montures).
 */
private data class GlobalSearchResult(
    val entryName: String,
    val categoryLabel: String,
    val onSelect: () -> Unit,
)

/**
 * Écran de gestion des livres (ouvert en cliquant la bibliothécaire) : pour chaque livre,
 * choisir sa couverture parmi les 7 illustrations disponibles et basculer sa disponibilité
 * aux joueurs. Chaque changement est persisté immédiatement (SharedPreferences via
 * [LibraryBookSettingsStore]), donc conservé à la fermeture de l'application, et
 * [onSettingChanged] est appelé pour que l'étagère se mette à jour au retour.
 *
 * Permet aussi d'ajouter un livre personnalisé (n'importe quel fichier, .md ou non,
 * choisi via le sélecteur système) — voir [CustomBooksStore] — et de retirer un livre
 * personnalisé déjà ajouté.
 */
@Composable
private fun LibraryBookSettingsScreen(
    books: List<LibraryBook>,
    worldId: String?,
    onSettingChanged: () -> Unit,
) {
    val context = LocalContext.current

    // Fichier en attente de confirmation (nom + couverture) après sélection dans le
    // sélecteur système, avant d'être copié et enregistré comme nouveau livre.
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var pendingSkin by remember { mutableStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val displayName = queryDisplayName(context, uri)
            pendingName = displayName?.substringBeforeLast('.').orEmpty().ifBlank { displayName.orEmpty() }
            pendingSkin = 0
            pendingUri = uri
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "add_book") {
            Surface(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajouter un livre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        items(books, key = { it.tabKey }) { book ->
            val isCustom = book.tabKey.startsWith("custom_")
            var selectedSkin by remember(book.tabKey, worldId) {
                mutableStateOf(LibraryBookSettingsStore.getSkinIndex(context, worldId, book.tabKey))
            }
            var visibleToPlayers by remember(book.tabKey, worldId) {
                mutableStateOf(LibraryBookSettingsStore.isVisibleToPlayers(context, worldId, book.tabKey))
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = book.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = book.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (isCustom) {
                            IconButton(onClick = {
                                CustomBooksStore.remove(context, worldId, book.tabKey.removePrefix("custom_"))
                                onSettingChanged()
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Retirer ce livre",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Couverture",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookSkinDrawables.forEachIndexed { index, drawableRes ->
                            val isSelected = selectedSkin == index
                            // Bordure épaisse + pastille à coche en surimpression : le voile
                            // semi-transparent précédent était trop discret pour distinguer
                            // la couverture sélectionnée des autres au premier coup d'œil.
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clickable {
                                        selectedSkin = index
                                        LibraryBookSettingsStore.setSkinIndex(context, worldId, book.tabKey, index)
                                        onSettingChanged()
                                    },
                            ) {
                                Image(
                                    painter = painterResource(drawableRes),
                                    contentDescription = "Couverture ${index + 1}",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionnée",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(10.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Disponible pour les joueurs",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = visibleToPlayers,
                            onCheckedChange = { checked ->
                                visibleToPlayers = checked
                                LibraryBookSettingsStore.setVisibleToPlayers(context, worldId, book.tabKey, checked)
                                onSettingChanged()
                            },
                        )
                    }
                }
            }
        }
    }

    // Dialogue de confirmation après sélection d'un fichier : nom du livre et couverture,
    // avant copie du fichier dans le stockage interne et enregistrement définitif.
    val uri = pendingUri
    if (uri != null) {
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text("Ajouter un livre") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pendingName,
                        onValueChange = { pendingName = it },
                        label = { Text("Nom du livre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Couverture",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookSkinDrawables.forEachIndexed { index, drawableRes ->
                            val isSelected = pendingSkin == index
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clickable { pendingSkin = index },
                            ) {
                                Image(
                                    painter = painterResource(drawableRes),
                                    contentDescription = "Couverture ${index + 1}",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionnée",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(10.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pendingName.isNotBlank(),
                    onClick = {
                        val id = "${System.currentTimeMillis()}"
                        val fileName = copyPickedFileToInternalStorage(
                            context, uri, id, queryDisplayName(context, uri),
                        )
                        if (fileName != null) {
                            CustomBooksStore.add(context, worldId, id, pendingName.trim(), fileName)
                            LibraryBookSettingsStore.setSkinIndex(context, worldId, "custom_$id", pendingSkin)
                            onSettingChanged()
                        }
                        pendingUri = null
                    },
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUri = null }) {
                    Text("Annuler")
                }
            },
        )
    }
}

/**
 * Étagère d'accueil de la bibliothèque : les onglets sont présentés comme de fines
 * tranches de livre, rangées par 4, dans l'esprit d'une vraie bibliothèque. Les tranches
 * utilisent un cycle de 3 illustrations (`livre_1/2/3`) issues de `res/drawable`, et la
 * planche d'étagère est l'illustration `bli_etagere` — plus de couleurs Material codées
 * en dur ici, tout vient des assets fournis.
 */
@Composable
private fun LibraryBookshelf(
    books: List<LibraryBook>,
    onBookClick: (Int) -> Unit,
    worldId: String?,
    settingsVersion: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bookSpacing = 10.dp
    val contentPaddingH = 20.dp
    // Largeur de livre fixe et cible (augmentée d'environ 20% par rapport à la largeur
    // trop fine précédente). Le nombre de livres par étagère n'est plus fixé en dur :
    // il est recalculé selon la largeur d'écran disponible, pour que cette largeur de
    // livre reste constante sur tous les écrans (plus de livres sur un écran large,
    // moins sur un écran étroit) plutôt que d'étirer ou de comprimer les livres.
    val bookWidth = 41.dp

    BoxWithConstraints(modifier = modifier) {
        val scope = this
        val availableWidth = scope.maxWidth - contentPaddingH * 2
        val booksPerShelf = ((availableWidth + bookSpacing) / (bookWidth + bookSpacing))
            .toInt()
            .coerceAtLeast(1)
        val shelves = books.chunked(booksPerShelf)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = contentPaddingH),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            itemsIndexed(shelves, key = { index, _ -> "shelf_$index" }) { shelfIndex, shelfBooks ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Superposition explicite dans un Box : la planche est dessinée en
                    // premier, la rangée de livres en second, donc les livres restent
                    // au-dessus de l'étagère (et non l'inverse) sur leur zone de recouvrement.
                    // La planche n'a pas de padding horizontal (elle va bord à bord), alors
                    // que la rangée de livres garde son retrait habituel via son propre padding.
                    val rowHeight = 260.dp
                    val shelfHeight = 32.dp
                    val overlap = 10.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight + shelfHeight - overlap),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.bli_etagere),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(shelfHeight)
                                .align(Alignment.BottomCenter),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = contentPaddingH)
                                .height(rowHeight)
                                .align(Alignment.TopStart)
                                .offset(y = (-5).dp),
                            horizontalArrangement = Arrangement.spacedBy(bookSpacing, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            shelfBooks.forEachIndexed { i, book ->
                                // Couverture choisie dans l'écran de réglages (roue crantée)
                                // si elle existe, sinon cycle par défaut des 7 illustrations.
                                val overrideIndex = remember(book.tabKey, worldId, settingsVersion) {
                                    LibraryBookSettingsStore.getSkinIndex(context, worldId, book.tabKey)
                                }
                                val defaultSkin = bookSkinDrawables[(shelfIndex * booksPerShelf + i) % bookSkinDrawables.size]
                                val bookSkin = overrideIndex?.let { bookSkinDrawables.getOrNull(it) } ?: defaultSkin
                                BookSpine(
                                    book = book,
                                    bookSkinRes = bookSkin,
                                    onClick = { onBookClick(book.tabIndex) },
                                    modifier = Modifier
                                        .width(bookWidth)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fine tranche de livre cliquable : l'illustration [bookSkinRes] sert de fond (couverture
 * du livre), avec l'icône et le titre (pivoté à 90°, comme sur une vraie tranche)
 * superposés par-dessus. Un léger voile sombre en haut/bas garantit la lisibilité du
 * texte quelle que soit la luminosité de l'illustration de couverture.
 */
@Composable
private fun BookSpine(
    book: LibraryBook,
    bookSkinRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Couverture affichée en entier (FillBounds = image complète, sans rognage,
            // même si ça étire légèrement — préférable à un Crop qui coupait le haut/bas
            // des illustrations comme les épées).
            Image(
                painter = painterResource(bookSkinRes),
                contentDescription = book.label,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            // Léger voile en bas uniquement, pour garder le titre lisible sans assombrir
            // le reste de la couverture.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Titre écrit à la verticale, comme sur une tranche de livre : la Text est
                // dimensionnée avant rotation (largeur = hauteur réellement disponible dans
                // cet espace, mesurée via BoxWithConstraints), hauteur = une ligne, puis
                // pivotée de 90°. `requiredWidth` (et non `width`) est indispensable ici :
                // un livre étroit donne un BoxWithConstraints étroit, et un simple `width`
                // aurait été contraint/rogné à cette largeur avant même la rotation, coupant
                // le texte. `requiredWidth` impose la largeur voulue quelles que soient les
                // contraintes du parent — la rotation ramène ensuite tout dans l'emprise du
                // livre (clippée par le Surface englobant), sans troncature.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val scope = this
                    val availableHeight = scope.maxHeight
                    Text(
                        text = book.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                blurRadius = 6f,
                            ),
                        ),
                        modifier = Modifier
                            .requiredWidth(availableHeight)
                            .rotate(-90f),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.dp)
                        .background(ForcedDarkPalette.AccentGold.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

@Composable
private fun SrdEntryRow(
    entry: SrdEntry,
    onClick: () -> Unit,
    trailingLabel: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entry.category.isNotBlank() && entry.category != "Catégorie") {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!trailingLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EquipmentItemRow(
    entry: EquipmentItem,
    onClick: () -> Unit,
) {
    val isWeapon = entry.category.lowercase().contains("arme")
    val isArmor = entry.category.lowercase().contains("armure")
    val subtitle = buildList {
        if (isWeapon && entry.damage.isNotBlank()) add(entry.damage)
        if (isArmor && entry.ac.isNotBlank()) add("CA ${entry.ac}")
        if (entry.weight.isNotBlank() && entry.weight != "-") add(entry.weight)
        if (entry.cost.isNotBlank()) add(entry.cost)
        if (entry.properties.isNotBlank()) add(entry.properties)
    }.joinToString(" • ").takeIf { it.isNotBlank() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}