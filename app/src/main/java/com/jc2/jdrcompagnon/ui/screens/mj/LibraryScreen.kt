package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
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
 * Extrait le niveau de défi et les XP d'un monstre depuis la ligne "**Défi** X (Y XP)"
 * présente dans son bloc de statistiques. Retourne null si non trouvé.
 */
private val monsterChallengeRegex = Regex("""\*\*Défi\*\*\s+([^(]+?)\s*\(([^)]+?)\s*XP\)""")

fun monsterChallenge(entry: SrdEntry): Pair<String, String>? {
    val match = monsterChallengeRegex.find(entry.rawMarkdown) ?: return null
    return match.groupValues[1].trim() to match.groupValues[2].trim()
}

/**
 * Libellé affiché à droite dans la liste des monstres (ex: "Défi 1/4 · 50 XP").
 */
fun monsterChallengeLabel(entry: SrdEntry): String? {
    val (cr, xp) = monsterChallenge(entry) ?: return null
    return "Défi $cr · $xp XP"
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
    val libraryBooks = remember(tabs) {
        tabs.mapIndexed { index, label -> LibraryBook(label = label, icon = tabIcons[index], tabIndex = index) }
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

    // Fond sombre uniforme (ForcedDarkPalette), aligné sur le reste de l'app
    // (MainActivity, AppBottomBar) plutôt que le fond texturé par monde de
    // WorldBackground, qui tranchait avec la barre de navigation du bas.
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (showShelf) "Bibliothèque" else tabs.getOrNull(selectedTab).orEmpty(),
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
                    IconButton(onClick = { if (showShelf) onBack() else showShelf = true }) {
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
            LibraryBookshelf(
                books = libraryBooks,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onBookClick = { index ->
                    selectedTab = index
                    showShelf = false
                },
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                initialSectionTitle = initialSectionTitle
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
                            DocSectionTocScreen(sections = sections)
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

    LaunchedEffect(sections, initialSectionTitle) {
        initialSectionTitle?.let { target ->
            val index = sections.indexOfFirst { it.title.equals(target, ignoreCase = true) }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOC
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Table des matières",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• ${section.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Sections
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
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
                SectionEntryRow(name = entry.name, category = "", onClick = { onItemClick(entry) })
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
private fun DocSectionTocScreen(sections: List<SrdDocSection>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Table des matières",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• ${section.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
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
 */
private data class LibraryBook(
    val label: String,
    val icon: ImageVector,
    val tabIndex: Int,
)

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
    modifier: Modifier = Modifier,
) {
    // Cycle des skins de couverture de livre fournis en drawable.
    val bookSkins = listOf(
        R.drawable.livre_1,
        R.drawable.livre_2,
        R.drawable.livre_3,
        R.drawable.livre_4,
        R.drawable.livre_5,
        R.drawable.livre_6,
        R.drawable.livre_7,
    )
    val booksPerShelf = 6
    val shelves = books.chunked(booksPerShelf)
    val bookSpacing = 10.dp
    val contentPaddingH = 20.dp

    // Largeur de livre fixe, calculée pour 4 livres par rangée : ainsi une rangée
    // incomplète (ex. la dernière) ne s'étire pas pour combler l'espace — les livres
    // gardent la même largeur partout et l'espace restant reste vide à droite.
    BoxWithConstraints(modifier = modifier) {
        val scope = this
        // Largeur "pleine" si 4 livres se partageaient toute la ligne, puis réduite de 40%
        // pour des tranches moins épaisses (les rangées incomplètes ou pleines laissent donc
        // un peu d'espace libre à droite, au lieu de forcer les livres à occuper toute la largeur).
        val fullBookWidth = (scope.maxWidth - contentPaddingH * 2 - bookSpacing * (booksPerShelf - 1)) / booksPerShelf
        val bookWidth = fullBookWidth * 0.6f

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
                            horizontalArrangement = Arrangement.spacedBy(bookSpacing),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            shelfBooks.forEachIndexed { i, book ->
                                val bookSkin = bookSkins[(shelfIndex * booksPerShelf + i) % bookSkins.size]
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