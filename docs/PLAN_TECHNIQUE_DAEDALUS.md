# 🏛️ PLAN TECHNIQUE DAEDALUS — Évolution JDR Compagnon

> **Architecte :** DAEDALUS  
> **Date :** 21 août 2026  
> **Projet :** JDR Compagnon — `com.jc2.jdrcompagnon`  
> **Stack :** Kotlin 2.0.21 · AGP 9.2.1 · Compose BOM 2025.02.00 · min SDK 24 · compile SDK 37

---

## Table des matières

1. [Dépendance markdown](#1-dépendance-markdown)
2. [Lecture des fichiers assets](#2-lecture-des-fichiers-assets)
3. [Nouvelle architecture d'écrans](#3-nouvelle-architecture-décrans)
4. [Navigation](#4-navigation)
5. [Stockage des scénarios en markdown](#5-stockage-des-scénarios-en-markdown)
6. [Fonds d'écran](#6-fonds-décran)
7. [Structure des fichiers](#7-structure-des-fichiers)

---

## 1. Dépendance markdown

### Choix recommandé : `mikepenz/multiplatform-markdown-renderer` (Maven Central)

| Critère | jeziellago/compose-markdown | mikepenz/multiplatform-markdown-renderer |
|---|---|---|
| Repository | JitPack (à configurer) | Maven Central (déjà fonctionnel) |
| Compat Kotlin 2.0.21 | ✅ mais dépend de JitPack | ✅ v0.28.0+ |
| Compat Compose BOM 2025.02.00 | ✅ | ✅ |
| Support tables HTML `<table>` | ❌ (markdown natif uniquement) | ✅ via `Markwon` ou extension HTML |
| Support tables markdown `|` | ✅ | ✅ |
| Taille lib | ~200KB | ~150KB (core) + ~80KB (material3) |
| Maintenance | Active | Active, plus récente |
| Dépendances transitive | Minimal | Minimal (coil pour images) |

**Décision :** `mikepenz/multiplatform-markdown-renderer` — Maven Central (aucune config JitPack), support natif Material3, tables markdown natives, API idiomatique Compose.

> ⚠️ **Note sur les tables HTML `<table>`** : Les fichiers SRD contiennent des `<table>` HTML pur dans les stat blocks de monstres. Aucune librairie markdown Compose ne rend nativement le HTML inline. **Solution :** pré-traiter le markdown au chargement pour convertir les `<table>` en tables markdown GFM (`| col | col |`). Voir §2 pour le parser.

### Code exact à ajouter

#### `gradle/libs.versions.toml`

Ajouter dans la section `[versions]` :

```toml
[versions]
# ... versions existantes ...
markdownRenderer = "0.28.0"
```

Ajouter dans la section `[libraries]` :

```toml
[libraries]
# ... libraries existantes ...

# Markdown Renderer
markdown-renderer = { group = "com.mikepenz", name = "multiplatform-markdown-renderer", version.ref = "markdownRenderer" }
markdown-renderer-m3 = { group = "com.mikepenz", name = "multiplatform-markdown-renderer-m3", version.ref = "markdownRenderer" }
markdown-renderer-coil = { group = "com.mikepenz", name = "multiplatform-markdown-renderer-coil", version.ref = "markdownRenderer" }
```

#### `app/build.gradle.kts`

Ajouter dans le bloc `dependencies` :

```kotlin
dependencies {
    // ... dépendances existantes ...

    // Markdown Renderer (mikepenz — Maven Central)
    implementation(libs.markdown.renderer)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.markdown.renderer.coil)
}
```

> **Aucune modification** du `build.gradle.kts` racine nécessaire (Maven Central est déjà le repository par défaut).

### Utilisation dans un Composable

```kotlin
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColorScheme

@Composable
fun MonsterStatBlock(markdownContent: String) {
    Markdown(
        content = markdownContent,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = markdownColorScheme(
            text = MaterialTheme.colorScheme.onSurface,
            codeText = MaterialTheme.colorScheme.primary,
            dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
    )
}
```

---

## 2. Lecture des fichiers assets

### 2.1 Structure des fichiers SRD (analyse réelle)

| Fichier | Taille | Format | Séparateur d'entrée |
|---|---|---|---|
| `monsters-a-z.md` | 506 KB | `## Category` → `### MonsterName` | `### ` (235 monstres) |
| `spells.md` | 319 KB | `## Section` (règles) → `#### SpellName` | `#### ` (352 sorts) |
| `rules.md` | 64 KB | Markdown continu | N/A (affichage full) |
| `rules-glossary.md` | 72 KB | Markdown continu | N/A (affichage full) |
| `equipment.md` | 71 KB | Markdown continu | N/A (affichage full) |
| `monsters.md` | 19 KB | Règles des stat blocks | N/A (affichage full) |

### 2.2 Modèle de données

```kotlin
// SrdEntry.kt
package com.jc2.jdrcompagnon.ui.srd

/**
 * Représente une entrée parsée depuis un fichier SRD markdown.
 * Utilisé pour les monstres (### ) et les sorts (#### ).
 */
data class SrdEntry(
    val name: String,           // "Aboleth", "Acid Arrow"
    val rawMarkdown: String,     // Le contenu markdown complet de l'entrée
    val category: String = "",   // Pour les monstres : nom de la catégorie ## (ex: "Animated Objects")
    val firstLetter: Char = name.firstOrNull()?.uppercaseChar() ?: 'A', // Pour le regroupement alphabétique
)
```

### 2.3 Service de chargement et mise en cache

```kotlin
// SrdRepository.kt
package com.jc2.jdrcompagnon.ui.srd

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Singleton responsable du chargement, parsing et cache des fichiers SRD markdown.
 * Les fichiers sont chargés une seule fois en mémoire puis conservés.
 */
object SrdRepository {

    private var _monsters: List<SrdEntry>? = null
    private var _spells: List<SrdEntry>? = null
    private var _rulesMarkdown: String? = null
    private var _glossaryMarkdown: String? = null
    private var _equipmentMarkdown: String? = null
    private var _monsterRulesMarkdown: String? = null

    // ── Accès公开 ──

    val monsters: List<SrdEntry>? get() = _monsters
    val spells: List<SrdEntry>? get() = _spells
    val isMonstersLoaded: Boolean get() = _monsters != null
    val isSpellsLoaded: Boolean get() = _spells != null

    // ── Chargement asynchrone ──

    suspend fun loadMonsters(context: Context): List<SrdEntry> = withContext(Dispatchers.IO) {
        _monsters?.let { return@withContext it }
        val raw = context.assets.open("srd/monsters-a-z.md").bufferedReader().use { it.readText() }
        val parsed = MonsterParser.parse(raw)
        _monsters = parsed
        parsed
    }

    suspend fun loadSpells(context: Context): List<SrdEntry> = withContext(Dispatchers.IO) {
        _spells?.let { return@withContext it }
        val raw = context.assets.open("srd/spells.md").bufferedReader().use { it.readText() }
        val parsed = SpellParser.parse(raw)
        _spells = parsed
        parsed
    }

    suspend fun loadRules(context: Context): String = withContext(Dispatchers.IO) {
        _rulesMarkdown?.let { return@withContext it }
        val raw = context.assets.open("srd/rules.md").bufferedReader().use { it.readText() }
        _rulesMarkdown = raw
        raw
    }

    suspend fun loadGlossary(context: Context): String = withContext(Dispatchers.IO) {
        _glossaryMarkdown?.let { return@withContext it }
        val raw = context.assets.open("srd/rules-glossary.md").bufferedReader().use { it.readText() }
        _glossaryMarkdown = raw
        raw
    }

    suspend fun loadEquipment(context: Context): String = withContext(Dispatchers.IO) {
        _equipmentMarkdown?.let { return@withContext it }
        val raw = context.assets.open("srd/equipment.md").bufferedReader().use { it.readText() }
        _equipmentMarkdown = raw
        raw
    }

    suspend fun loadMonsterRules(context: Context): String = withContext(Dispatchers.IO) {
        _monsterRulesMarkdown?.let { return@withContext it }
        val raw = context.assets.open("srd/monsters.md").bufferedReader().use { it.readText() }
        _monsterRulesMarkdown = raw
        raw
    }

    // ── Recherche ──

    fun searchMonsters(query: String): List<SrdEntry> {
        val all = _monsters ?: return emptyList()
        if (query.isBlank()) return all
        return all.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun searchSpells(query: String): List<SrdEntry> {
        val all = _spells ?: return emptyList()
        if (query.isBlank()) return all
        return all.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun getMonsterByName(name: String): SrdEntry? =
        _monsters?.find { it.name.equals(name, ignoreCase = true) }

    fun getSpellByName(name: String): SrdEntry? =
        _spells?.find { it.name.equals(name, ignoreCase = true) }
}
```

### 2.4 Parser des monstres

Le fichier `monsters-a-z.md` a cette structure :

```
# Monsters A–Z

## Aboleth          ← catégorie (##)
### Aboleth         ← monstre individuel (###)
Large Aberration...
#### Traits
#### Actions
## Air Elemental    ← catégorie suivante
### Air Elemental   ← monstre individuel
...
```

**Logique de parsing :**
- Les entrées `## ` sont des **catégories** (30 catégories, ex: "Animated Objects" contient Animated Armor, Animated Flying Sword, Animated Rug)
- Les entrées `### ` sont les **monstres individuels** (235 monstres)
- Chaque monstre `### ` contient tout le texte jusqu'au prochain `### ` ou `## `

```kotlin
// MonsterParser.kt
package com.jc2.jdrcompagnon.ui.srd

/**
 * Parser pour monsters-a-z.md.
 * Structure: ## Category > ### MonsterName > contenu (stat block + traits + actions)
 *
 * 235 monstres répartis sur 30 catégories.
 * Le séparateur de monstre est "### " (heading niveau 3).
 * Le contenu d'un monstre s'étend jusqu'au prochain "### " ou "## " ou fin de fichier.
 */
object MonsterParser {

    private val MONSTER_HEADING = Regex("^### (.+)$", RegexOption.MULTILINE)
    private val CATEGORY_HEADING = Regex("^## (.+)$", RegexOption.MULTILINE)

    fun parse(rawMarkdown: String): List<SrdEntry> {
        val lines = rawMarkdown.lines()
        val entries = mutableListOf<SrdEntry>()
        var currentCategory = ""
        var currentName: String? = null
        var currentContent = StringBuilder()

        for (line in lines) {
            val catMatch = CATEGORY_HEADING.find(line)
            val monMatch = MONSTER_HEADING.find(line)

            when {
                catMatch != null -> {
                    // Finaliser le monstre précédent
                    currentName?.let { name ->
                        entries.add(SrdEntry(
                            name = name,
                            rawMarkdown = currentContent.toString().trim(),
                            category = currentCategory,
                            firstLetter = name.firstOrNull()?.uppercaseChar() ?: 'A',
                        ))
                    }
                    currentCategory = catMatch.groupValues[1].trim()
                    currentName = null
                    currentContent = StringBuilder()
                }
                monMatch != null -> {
                    // Finaliser le monstre précédent
                    currentName?.let { name ->
                        entries.add(SrdEntry(
                            name = name,
                            rawMarkdown = currentContent.toString().trim(),
                            category = currentCategory,
                            firstLetter = name.firstOrNull()?.uppercaseChar() ?: 'A',
                        ))
                    }
                    currentName = monMatch.groupValues[1].trim()
                    currentContent = StringBuilder()
                    // Le contenu du monstre commence APRÈS le heading
                }
                else -> {
                    if (currentName != null) {
                        currentContent.appendLine(line)
                    }
                }
            }
        }

        // Finaliser le dernier monstre
        currentName?.let { name ->
            entries.add(SrdEntry(
                name = name,
                rawMarkdown = currentContent.toString().trim(),
                category = currentCategory,
                firstLetter = name.firstOrNull()?.uppercaseChar() ?: 'A',
            ))
        }

        return entries
    }
}
```

### 2.5 Parser des sorts

Le fichier `spells.md` a cette structure :

```
# Spells

## Gaining Spells          ← section de règles (##)
### Preparing Spells       ← sous-section (###)
## Spell Descriptions      ← section de règles (##)
#### Acid Arrow            ← sort individuel (####)
Level 2 Evocation...
#### Acid Splash           ← sort suivant (####)
...
```

**Logique de parsing :**
- Les sorts individuels commencent à `#### ` après la ligne `## Spell Descriptions` (ligne 260)
- Les `#### ` avant `## Spell Descriptions` sont des sous-sections de règles, PAS des sorts
- Le contenu d'un sort s'étend jusqu'au prochain `#### ` ou `## ` ou fin de fichier

```kotlin
// SpellParser.kt
package com.jc2.jdrcompagnon.ui.srd

/**
 * Parser pour spells.md.
 * 352 sorts individuels délimités par "#### " (heading niveau 4).
 * IMPORTANT: Les "#### " avant la section "## Spell Descriptions" sont des
 * sous-sections de règles, pas des sorts. Le parsing commence après
 * "## Spell Descriptions".
 */
object SpellParser {

    private val SPELL_HEADING = Regex("^#### (.+)$", RegexOption.MULTILINE)
    private const val SPELL_DESCRIPTIONS_HEADER = "## Spell Descriptions"

    fun parse(rawMarkdown: String): List<SrdEntry> {
        // Trouver le point de départ : après "## Spell Descriptions"
        val startIndex = rawMarkdown.indexOf(SPELL_DESCRIPTIONS_HEADER)
        if (startIndex == -1) return emptyList()

        val spellsSection = rawMarkdown.substring(startIndex)
        val lines = spellsSection.lines()
        val entries = mutableListOf<SrdEntry>()
        var currentName: String? = null
        var currentContent = StringBuilder()

        for (line in lines) {
            val spellMatch = SPELL_HEADING.find(line)

            if (spellMatch != null) {
                // Finaliser le sort précédent
                currentName?.let { name ->
                    entries.add(SrdEntry(
                        name = name,
                        rawMarkdown = currentContent.toString().trim(),
                        firstLetter = name.firstOrNull()?.uppercaseChar() ?: 'A',
                    ))
                }
                currentName = spellMatch.groupValues[1].trim()
                currentContent = StringBuilder()
            } else {
                if (currentName != null) {
                    currentContent.appendLine(line)
                }
            }
        }

        // Finaliser le dernier sort
        currentName?.let { name ->
            entries.add(SrdEntry(
                name = name,
                rawMarkdown = currentContent.toString().trim(),
                firstLetter = name.firstOrNull()?.uppercaseChar() ?: 'A',
            ))
        }

        return entries
    }
}
```

### 2.6 Conversion des tables HTML vers markdown GFM

Les fichiers SRD contiennent des `<table>` HTML inline. La librairie markdown ne rend pas le HTML. Il faut un convertisseur :

```kotlin
// HtmlTableConverter.kt
package com.jc2.jdrcompagnon.ui.srd

/**
 * Convertit les tables HTML <table> en tables markdown GFM.
 * Les fichiers SRD (D&D 5.2) utilisent des tables HTML pour les stat blocks.
 */
object HtmlTableConverter {

    private val TABLE_REGEX = Regex("""<table>(.*?)</table>""", RegexOption.DOT_MATCHES_ALL)

    fun convert(markdown: String): String {
        return TABLE_REGEX.replace(markdown) { matchResult ->
            val html = matchResult.value
            val rows = extractRows(html)
            convertRowsToMarkdown(rows)
        }
    }

    private fun extractRows(html: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val rowRegex = Regex("""<tr>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("""<t[hd][^>]*>(.*?)</t[hd]>""", RegexOption.DOT_MATCHES_ALL)

        rowRegex.findAll(html).forEach { rowMatch ->
            val cells = cellRegex.findAll(rowMatch.value).map { cellMatch ->
                cleanHtml(cellMatch.groupValues[1])
            }.toList()
            if (cells.isNotEmpty()) rows.add(cells)
        }
        return rows
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace("<strong>", "**")
            .replace("</strong>", "**")
            .replace("<br>", " ")
            .replace("<br/>", " ")
            .replace(Regex("<[^>]+>"), "") // Strip remaining tags
            .trim()
    }

    private fun convertRowsToMarkdown(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""
        val maxCols = rows.maxOf { it.size }
        val normalized = rows.map { row ->
            row + List(maxCols - row.size) { "" }
        }

        val header = normalized.first()
        val separator = List(maxCols) { "---" }
        val body = normalized.drop(1).ifEmpty { emptyList() }

        return buildString {
            appendLine(header.joinToString(" | ", prefix = "| ", postfix = " |"))
            appendLine(separator.joinToString(" | ", prefix = "| ", postfix = " |"))
            body.forEach { row ->
                appendLine(row.joinToString(" | ", prefix = "| ", postfix = " |"))
            }
        }
    }
}
```

**Intégration dans le chargement :**

```kotlin
// Dans SrdRepository.loadMonsters(), après parsing :
suspend fun loadMonsters(context: Context): List<SrdEntry> = withContext(Dispatchers.IO) {
    _monsters?.let { return@withContext it }
    val raw = context.assets.open("srd/monsters-a-z.md").bufferedReader().use { it.readText() }
    val converted = HtmlTableConverter.convert(raw) // <table> → | table |
    val parsed = MonsterParser.parse(converted)
    _monsters = parsed
    parsed
}
```

---

## 3. Nouvelle architecture d'écrans

### 3.1 Écrans à créer (nouveaux)

| Écran | Fichier | Rôle |
|---|---|---|
| Bestiaire — liste | `BestiaryListScreen.kt` | Recherche + liste alphabétique de monstres |
| Bestiaire — détail | `BestiaryDetailScreen.kt` | Stat block d'un monstre (rendu markdown) |
| Sorts — liste | `SpellListScreen.kt` | Recherche + liste des sorts |
| Sorts — détail | `SpellDetailScreen.kt` | Détail d'un sort (rendu markdown) |
| Règles — lecture | `RulesReaderScreen.kt` | Affichage markdown des règles/glossaire/équipement |
| Éditeur scénario | `ScenarioEditorScreen.kt` | Titre + édition markdown + prévisualisation |
| Composant markdown | `MarkdownRenderer.kt` | Wrapper réutilisable autour de la lib markdown |

### 3.2 Écrans à modifier (existants)

| Écran | Modifications |
|---|---|
| `MjHomeScreen.kt` | Ajouter outils "Sorts" et "Règles" dans la grille. Brancher les callbacks de navigation vers les nouvelles routes. Transformer les notes de scénario en ouvrant l'éditeur markdown. |
| `MjSetupScreen.kt` | Ajouter bouton "Créer/Éditer scénario en markdown" qui ouvre `ScenarioEditorScreen`. |
| `GameState.kt` | Modifier `MjScenario` pour stocker `markdownContent` au lieu de `description` (voir §5). |

### 3.3 Bestiaire — `BestiaryListScreen.kt`

```kotlin
package com.jc2.jdrcompagnon.ui.screens.srd

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.srd.SrdRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryListScreen(
    onMonsterClick: (String) -> Unit,  // passe le nom du monstre
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var monsters by remember { mutableStateOf<List<SrdEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Chargement asynchrone au premier affichage
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            monsters = SrdRepository.loadMonsters(context)
            isLoading = false
        }
    }

    val filteredMonsters = remember(monsters, searchQuery) {
        if (searchQuery.isBlank()) monsters
        else monsters.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    // Regroupement alphabétique
    val groupedMonsters = remember(filteredMonsters) {
        filteredMonsters.groupBy { it.firstLetter }.toSortedMap()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BESTIAIRE", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Barre de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher un monstre...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groupedMonsters.forEach { (letter, entries) ->
                        item(key = "header_$letter") {
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(items = entries, key = { it.name }) { monster ->
                            ListItem(
                                headlineContent = { Text(monster.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text(monster.category, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.clickable { onMonsterClick(monster.name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
```

### 3.4 Bestiaire — détail `BestiaryDetailScreen.kt`

```kotlin
package com.jc2.jdrcompagnon.ui.screens.srd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.srd.SrdRepository
import com.mikepenz.markdown.m3.Markdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryDetailScreen(
    monsterName: String,
    onBack: () -> Unit
) {
    val monster = remember(monsterName) {
        SrdRepository.getMonsterByName(monsterName)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(monster?.name ?: "Monstre", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (monster != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Markdown(
                    content = monster.rawMarkdown,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Monstre introuvable")
            }
        }
    }
}
```

### 3.5 Sorts — `SpellListScreen.kt` et `SpellDetailScreen.kt`

Même pattern que le bestiaire, mais avec `SrdRepository.loadSpells()` et `SrdRepository.getSpellByName()`. Le détail affiche le markdown du sort avec `Markdown()`.

### 3.6 Éditeur de scénarios — `ScenarioEditorScreen.kt`

```kotlin
package com.jc2.jdrcompagnon.ui.screens.srd

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.mikepenz.markdown.m3.Markdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioEditorScreen(
    scenarioId: String?,  // null = nouveau scénario
    onBack: () -> Unit
) {
    val scenarios by GameState.mjScenarios.collectAsState()
    val existingScenario = scenarioId?.let { id -> scenarios.firstOrNull { it.id == id } }

    var title by rememberSaveable { mutableStateOf(existingScenario?.title ?: "") }
    var markdownContent by rememberSaveable { mutableStateOf(existingScenario?.markdownContent ?: "") }
    var isPreviewMode by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ÉDITEUR SCÉNARIO", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Bouton de sauvegarde
                    TextButton(onClick = {
                        val scenario = GameState.MjScenario(
                            id = existingScenario?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title.ifBlank { "Scénario sans titre" },
                            markdownContent = markdownContent,
                        )
                        if (existingScenario != null) {
                            GameState.updateMjScenario(scenario)
                        } else {
                            GameState.addMjScenario(scenario)
                        }
                        onBack()
                    }) {
                        Text("Sauver", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Titre du scénario
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre du scénario") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )

            // Toggle édition/prévisualisation
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isPreviewMode,
                    onClick = { isPreviewMode = false },
                    label = { Text("Éditer") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                FilterChip(
                    selected = isPreviewMode,
                    onClick = { isPreviewMode = true },
                    label = { Text("Prévisualiser") },
                    leadingIcon = { Icon(Icons.Default.Preview, null) }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isPreviewMode) {
                // Prévisualisation markdown
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Markdown(
                        content = markdownContent.ifBlank { "_Rien à prévisualiser_" },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            } else {
                // Édition markdown brut
                OutlinedTextField(
                    value = markdownContent,
                    onValueChange = { markdownContent = it },
                    label = { Text("Contenu markdown") },
                    placeholder = { Text("## Introduction\n\nVotre scénario...") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).weight(1f),
                )
            }
        }
    }
}
```

---

## 4. Navigation

### 4.1 Nouvelles routes dans `Routes.kt`

```kotlin
// Ajouter dans la sealed class Route :
sealed class Route(val path: String) {
    // ... routes existantes ...

    // ── SRD / Outils de référence ──
    data object Bestiary : Route("bestiary")
    data object BestiaryDetail : Route("bestiary_detail")  // ?name={name}
    data object Spells : Route("spells")
    data object SpellDetail : Route("spell_detail")  // ?name={name}
    data object Rules : Route("rules")  // ?file={file} (rules, glossary, equipment, monsters)

    // ── Éditeur de scénarios ──
    data object ScenarioEditor : Route("scenario_editor")  // ?scenarioId={id} (null pour nouveau)
}
```

### 4.2 Nouveaux composables dans `NavGraph.kt`

Ajouter ces blocs `composable(...)` dans le `NavHost` :

```kotlin
// ── BESTIAIRE ──
composable(Route.Bestiary.path) {
    BestiaryListScreen(
        onMonsterClick = { monsterName ->
            val encoded = Uri.encode(monsterName)
            navController.navigate("${Route.BestiaryDetail.path}?name=$encoded")
        },
        onBack = { navController.popBackStack() }
    )
}

composable(
    route = Route.BestiaryDetail.path + "?name={name}",
    arguments = listOf(navArgument("name") { type = NavType.StringType })
) { backStackEntry ->
    val monsterName = backStackEntry.arguments?.getString("name") ?: ""
    BestiaryDetailScreen(
        monsterName = monsterName,
        onBack = { navController.popBackStack() }
    )
}

// ── SORTS ──
composable(Route.Spells.path) {
    SpellListScreen(
        onSpellClick = { spellName ->
            val encoded = Uri.encode(spellName)
            navController.navigate("${Route.SpellDetail.path}?name=$encoded")
        },
        onBack = { navController.popBackStack() }
    )
}

composable(
    route = Route.SpellDetail.path + "?name={name}",
    arguments = listOf(navArgument("name") { type = NavType.StringType })
) { backStackEntry ->
    val spellName = backStackEntry.arguments?.getString("name") ?: ""
    SpellDetailScreen(
        spellName = spellName,
        onBack = { navController.popBackStack() }
    )
}

// ── RÈGLES ──
composable(
    route = Route.Rules.path + "?file={file}",
    arguments = listOf(navArgument("file") { type = NavType.StringType; defaultValue = "rules" })
) { backStackEntry ->
    val fileId = backStackEntry.arguments?.getString("file") ?: "rules"
    RulesReaderScreen(
        fileId = fileId,
        onBack = { navController.popBackStack() }
    )
}

// ── ÉDITEUR DE SCÉNARIOS ──
composable(
    route = Route.ScenarioEditor.path + "?scenarioId={scenarioId}",
    arguments = listOf(navArgument("scenarioId") { type = NavType.StringType; defaultValue = "" })
) { backStackEntry ->
    val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
    ScenarioEditorScreen(
        scenarioId = scenarioId.ifBlank { null },
        onBack = { navController.popBackStack() }
    )
}
```

### 4.3 Modifications de `MjHomeScreen.kt`

Ajouter les nouveaux outils dans la liste `tools` et les callbacks :

```kotlin
// Nouveaux paramètres du composable :
@Composable
fun MjHomeScreen(
    currentWorld: WorldState?,
    selectedScenarioId: String? = null,
    selectedGroupId: String? = null,
    onCreateCharacter: () -> Unit,
    onViewCharacters: () -> Unit,
    onOpenBestiary: () -> Unit,       // NOUVEAU
    onOpenSpells: () -> Unit,         // NOUVEAU
    onOpenRules: () -> Unit,          // NOUVEAU
    onEditScenario: (String?) -> Unit, // NOUVEAU — null = nouveau scénario
    onBack: () -> Unit
) {
    // ...

    val tools = listOf(
        MjTool("create_character", "CRÉER", "Héros ou PNJ", Icons.Default.Add, MaterialTheme.colorScheme.primary),
        MjTool("bestiary", "BESTIAIRE", "Monstres & Fiches", Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.secondary),
        MjTool("spells", "SORTS", "Grimmoire complet", Icons.Default.AutoFixHigh, MaterialTheme.colorScheme.tertiary),    // NOUVEAU
        MjTool("rules", "RÈGLES", "SRD & Glossaire", Icons.Default.Rule, MaterialTheme.colorScheme.secondary),             // NOUVEAU
        MjTool("scenario_editor", "SCÉNARIO", "Éditeur markdown", Icons.Default.EditNote, MaterialTheme.colorScheme.primary), // NOUVEAU
        MjTool("notes", "NOTES", "Intrigues & Idées", Icons.Default.Edit, MaterialTheme.colorScheme.tertiary),
    )

    // Dans le when(tool.id) :
    when (tool.id) {
        "create_character" -> onCreateCharacter()
        "bestiary" -> onOpenBestiary()
        "spells" -> onOpenSpells()
        "rules" -> onOpenRules()
        "scenario_editor" -> onEditScenario(selectedScenarioId)
        "notes" -> { /* garder le comportement actuel des notes */ }
    }
}
```

Et dans `NavGraph.kt`, modifier le composable `MjHome` pour passer les nouveaux callbacks :

```kotlin
composable(
    route = Route.MjHome.path + "?scenarioId={scenarioId}&groupId={groupId}",
    arguments = listOf(...)
) { backStackEntry ->
    val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
    val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
    MjHomeScreen(
        currentWorld = currentWorld,
        selectedScenarioId = scenarioId.ifBlank { null },
        selectedGroupId = groupId.ifBlank { null },
        onCreateCharacter = { navController.navigate(Route.MjCharacterCreation.path) },
        onViewCharacters = { navController.navigate(Route.CharacterSelection.path + "?isMj=true") },
        onOpenBestiary = { navController.navigate(Route.Bestiary.path) },                    // NOUVEAU
        onOpenSpells = { navController.navigate(Route.Spells.path) },                        // NOUVEAU
        onOpenRules = { navController.navigate(Route.Rules.path + "?file=rules") },          // NOUVEAU
        onEditScenario = { id ->
            val encodedId = if (id != null) Uri.encode(id) else ""
            navController.navigate("${Route.ScenarioEditor.path}?scenarioId=$encodedId")
        },
        onBack = { navController.popBackStack() }
    )
}
```

---

## 5. Stockage des scénarios en markdown

### 5.1 Modification de `MjScenario` dans `GameState.kt`

```kotlin
@Serializable
data class MjScenario(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",          // CONSERVÉ pour rétrocompatibilité (anciennes données)
    val markdownContent: String = "",      // NOUVEAU — contenu markdown du scénario
    val createdBy: String = "MJ"
)
```

> **Migration :** Le champ `description` est conservé pour ne pas casser les scénarios existants. À l'affichage, on privilégie `markdownContent` ; si vide, on fallback sur `description`. `ignoreUnknownKeys = true` est déjà activé dans le `Json { }`, donc la désérialisation des anciennes données ne plantera pas.

### 5.2 Logique de migration douce

```kotlin
// Dans GameState, ajouter une fonction de migration appelée au chargement :

private fun loadMjScenarios() {
    prefs?.let { prefs ->
        val jsonString = prefs.getString(KEY_MJ_SCENARIOS, "") ?: ""
        if (jsonString.isNotBlank()) {
            try {
                val list = json.decodeFromString<List<MjScenario>>(jsonString)
                // Migration : si markdownContent est vide mais description non vide,
                // copier description → markdownContent
                val migrated = list.map { scenario ->
                    if (scenario.markdownContent.isBlank() && scenario.description.isNotBlank()) {
                        scenario.copy(markdownContent = scenario.description)
                    } else {
                        scenario
                    }
                }
                _mjScenarios.value = migrated
                // Si migration a eu lieu, re-sauver
                if (migrated != list) saveMjScenarios(migrated)
            } catch (e: Exception) {
                android.util.Log.e("GameState", "Error loading MJ scenarios", e)
            }
        }
    }
}
```

### 5.3 Persistance

La persistance reste identique : `SharedPreferences` + `kotlinx-serialization` JSON. Le champ `markdownContent` est un `String` sérialisable — pas de changement de mécanisme de stockage.

> ⚠️ **Limite de SharedPreferences :** Les scénarios markdown peuvent être longs (plusieurs KB). SharedPreferences stocke tout dans un seul fichier XML. Si les scénarios deviennent volumineux (>100KB total), envisager de migrer vers **DataStore Preferences** (déjà dans les dépendances) ou un fichier JSON dédié. Pour l'instant, SharedPreferences reste suffisant.

### 5.4 Affichage du scénario dans MjHomeScreen

Remplacer le `OutlinedTextField` brut des notes par un affichage markdown + bouton d'édition :

```kotlin
// Dans MjHomeScreen, section "Notes du scénario" :
selectedScenario?.let { scenario ->
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scénario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                TextButton(onClick = { onEditScenario(scenario.id) }) {
                    Text("Éditer en markdown")
                }
            }
            // Afficher le markdown si présent, sinon la description
            val displayContent = scenario.markdownContent.ifBlank { scenario.description }
            if (displayContent.isNotBlank()) {
                Markdown(
                    content = displayContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

---

## 6. Fonds d'écran

### 6.1 Stratégie recommandée : Drawable Vector + Compose Modifier

**Approche :** Créer des drawables vectoriels (XML) qui définissent des textures de fond, puis les appliquer via `Modifier.background()` ou un `Canvas` Compose. Cette approche est légère (pas de gros PNG), scalable, et s'adapte au thème courant.

### 6.2 Solution 1 : Drawable vectoriel répété (parchemin)

Créer `res/drawable/bg_parchment.xml` — un gradient beige avec une texture subtile :

```xml
<!-- res/drawable/bg_parchment.xml -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:startColor="#F5E6C8"
        android:centerColor="#EDE0C0"
        android:endColor="#E0D0A8"
        android:angle="135"
        android:type="linear" />
</shape>
```

Créer `res/drawable/bg_wood.xml` — texture bois pour Naheulbeuk :

```xml
<!-- res/drawable/bg_wood.xml -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:startColor="#6D4C2E"
        android:centerColor="#5A3D22"
        android:endColor="#4A3318"
        android:angle="90"
        android:type="linear" />
</shape>
```

### 6.3 Solution 2 : Compose Brush avec gradient + motif procédural

Pour une texture plus riche (fibres de parchemin, veinures du bois), utiliser un `Brush` Compose combiné à un `Canvas` pour dessiner un motif répété :

```kotlin
// BackgroundTexture.kt
package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Applique une texture de fond selon le monde actif.
 * - D&D (donjon_et_dragon) : texture parchemin (beige doré)
 * - Naheulbeuk : texture bois (brun foncé)
 * - Défaut : fond uni du thème
 */
@Composable
fun Modifier.worldBackground(worldId: String?): Modifier {
    if (worldId == null) return this
    return this.drawBehind {
        when (worldId) {
            "donjon_et_dragon" -> drawParchmentTexture(size.width, size.height)
            "naheulbeuk" -> drawWoodTexture(size.width, size.height)
        }
    }
}

// ── Texture Parchemin ──

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawParchmentTexture(w: Float, h: Float) {
    // Couleur de base : dégradé beige doré
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFF5E6C8),
                Color(0xFFEDE0C0),
                Color(0xFFE0D0A8),
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
    )
    // Taches subtiles de vieillissement
    val random = Random(42) // Seed fixe pour un rendu stable
    repeat(40) {
        val x = random.nextFloat() * w
        val y = random.nextFloat() * h
        val radius = random.nextFloat() * 30f + 10f
        drawCircle(
            color = Color(0xFFD4C49A).copy(alpha = random.nextFloat() * 0.15f),
            radius = radius,
            center = Offset(x, y),
        )
    }
}

// ── Texture Bois ──

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWoodTexture(w: Float, h: Float) {
    // Couleur de base : dégradé brun
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF6D4C2E),
                Color(0xFF5A3D22),
                Color(0xFF4A3318),
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, h),
        )
    )
    // Veinures du bois — lignes horizontales subtiles
    val random = Random(99)
    repeat(25) { i ->
        val y = (i.toFloat() / 25f) * h
        val yOffset = random.nextFloat() * 6f - 3f
        drawLine(
            color = Color(0xFF3A2810).copy(alpha = 0.2f + random.nextFloat() * 0.15f),
            start = Offset(0f, y + yOffset),
            end = Offset(w, y + yOffset),
            strokeWidth = 1f + random.nextFloat() * 2f,
        )
    }
    // Nœuds du bois
    repeat(8) {
        val x = random.nextFloat() * w
        val y = random.nextFloat() * h
        drawCircle(
            color = Color(0xFF2A1A08).copy(alpha = 0.3f),
            radius = random.nextFloat() * 8f + 4f,
            center = Offset(x, y),
        )
    }
}
```

### 6.4 Utilisation dans les écrans

```kotlin
// Dans n'importe quel écran (ex: BestiaryListScreen, MjHomeScreen) :
@Composable
fun MjHomeScreen(...) {
    val currentWorld by GameState.currentWorld.collectAsState()

    Scaffold(
        modifier = Modifier.worldBackground(currentWorld?.id),
        // ...
    ) { ... }
}

// Ou sur un Box/Column :
Box(modifier = Modifier
    .fillMaxSize()
    .worldBackground(currentWorld?.id)
) {
    // Contenu
}
```

### 6.5 Recommandation finale pour les fonds

| Aspect | Solution |
|---|---|
| Texture de base (gradient) | Compose `Brush.linearGradient` dans `drawBehind` |
| Motif (fibres, taches, veinures) | Compose `Canvas` / `DrawScope` (procédural) |
| Performance | `drawBehind` est cheap, pas d'allocation de bitmap |
| Thématisation | Choix de la texture selon `currentWorld?.id` |
| Fichiers à créer | `BackgroundTexture.kt` (un seul fichier, 100% Compose) |
| Fallback | Si `worldId == null`, pas de texture → fond uni du thème |

> **Pas besoin de PNG/bitmaps.** Les textures procédurales sont légères, scalables, et ne nécessitent pas de gérer des drawable-nodpi/ldpi/mdpi/hdpi. Le seed fixe garantit un rendu identique à chaque frame (pas de flickering).

---

## 7. Structure des fichiers

### 7.1 Arborescence complète des nouveaux fichiers

```
app/src/main/java/com/jc2/jdrcompagnon/
├── ui/
│   ├── components/
│   │   └── BackgroundTexture.kt              [NOUVEAU] — Modificateur de texture de fond (parchemin/bois)
│   ├── screens/
│   │   └── srd/                               [NOUVEAU — dossier]
│   │       ├── BestiaryListScreen.kt          [NOUVEAU] — Recherche + liste alphabétique des monstres
│   │       ├── BestiaryDetailScreen.kt        [NOUVEAU] — Stat block d'un monstre (rendu markdown)
│   │       ├── SpellListScreen.kt            [NOUVEAU] — Recherche + liste des sorts
│   │       ├── SpellDetailScreen.kt          [NOUVEAU] — Détail d'un sort (rendu markdown)
│   │       ├── RulesReaderScreen.kt           [NOUVEAU] — Affichage markdown des règles/glossaire/équipement
│   │       └── ScenarioEditorScreen.kt        [NOUVEAU] — Éditeur de scénario markdown (édition + prévisualisation)
│   ├── srd/                                   [NOUVEAU — dossier]
│   │   ├── SrdEntry.kt                        [NOUVEAU] — Modèle de données (entry name + raw markdown)
│   │   ├── SrdRepository.kt                   [NOUVEAU] — Singleton de chargement/cache/recherche des SRD
│   │   ├── MonsterParser.kt                   [NOUVEAU] — Parser ### → SrdEntry (monsters-a-z.md)
│   │   ├── SpellParser.kt                     [NOUVEAU] — Parser #### → SrdEntry (spells.md)
│   │   └── HtmlTableConverter.kt              [NOUVEAU] — Convertit <table> HTML → markdown GFM
│   ├── navigation/
│   │   ├── Routes.kt                          [MODIFIER] — Ajouter routes Bestiary, BestiaryDetail, Spells, SpellDetail, Rules, ScenarioEditor
│   │   └── NavGraph.kt                        [MODIFIER] — Ajouter composable() pour chaque nouvelle route + nouveaux callbacks MjHomeScreen
│   ├── GameState.kt                           [MODIFIER] — MjScenario: ajouter markdownContent + migration
│   └── screens/
│       └── mj/
│           └── MjHomeScreen.kt                 [MODIFIER] — Ajouter outils Sorts/Règles/Scénario + callbacks navigation
│
└── (build files)
    ├── gradle/libs.versions.toml               [MODIFIER] — Ajouter markdownRenderer version + 3 libraries
    └── app/build.gradle.kts                   [MODIFIER] — Ajouter 3 dépendances markdown renderer
```

### 7.2 Résumé des modifications

| Fichier | Action | Détail |
|---|---|---|
| `gradle/libs.versions.toml` | MODIFIER | +1 version, +3 libraries (markdown-renderer, -m3, -coil) |
| `app/build.gradle.kts` | MODIFIER | +3 `implementation(libs...)` |
| `Routes.kt` | MODIFIER | +6 routes (Bestiary, BestiaryDetail, Spells, SpellDetail, Rules, ScenarioEditor) |
| `NavGraph.kt` | MODIFIER | +6 blocs `composable()`, +4 callbacks MjHomeScreen |
| `GameState.kt` | MODIFIER | MjScenario +`markdownContent`, +migration |
| `MjHomeScreen.kt` | MODIFIER | +3 outils, +4 callbacks, affichage markdown scénario |
| `SrdEntry.kt` | CRÉER | Modèle de données |
| `SrdRepository.kt` | CRÉER | Chargement/cache/recherche |
| `MonsterParser.kt` | CRÉER | Parser `### ` → 235 monstres |
| `SpellParser.kt` | CRÉER | Parser `#### ` → 352 sorts |
| `HtmlTableConverter.kt` | CRÉER | `<table>` HTML → markdown GFM |
| `BackgroundTexture.kt` | CRÉER | Modifier de texture de fond |
| `BestiaryListScreen.kt` | CRÉER | Écran liste bestiaire |
| `BestiaryDetailScreen.kt` | CRÉER | Écran détail monstre |
| `SpellListScreen.kt` | CRÉER | Écran liste sorts |
| `SpellDetailScreen.kt` | CRÉER | Écran détail sort |
| `RulesReaderScreen.kt` | CRÉER | Écran lecture règles |
| `ScenarioEditorScreen.kt` | CRÉER | Éditeur scénario markdown |

**Total : 12 fichiers créés + 6 fichiers modifiés = 18 fichiers.**

---

## Synthèse des décisions architecturales

| Point | Décision | Justification |
|---|---|---|
| Lib markdown | mikepenz/multiplatform-markdown-renderer v0.28.0 | Maven Central (pas de JitPack), support Material3 natif |
| Tables HTML | Conversion `<table>` → GFM au chargement | Aucune lib ne rend le HTML inline en Compose |
| Parsing monstres | `### ` comme séparateur, 235 entrées | Structure réelle du fichier |
| Parsing sorts | `#### ` après "## Spell Descriptions", 352 entrées | Les `####` avant sont des sous-sections de règles |
| Cache SRD | Singleton en mémoire, chargement asynchrone IO | 506KB max, chargé une fois |
| Recherche | Filtre `String.contains()` in-memory sur liste cachée | 235/352 entrées — assez rapide sans FTS |
| Scénarios | `markdownContent: String` ajouté à `MjScenario` | Rétrocompatible, migration douce |
| Fonds d'écran | `drawBehind` procédural (gradient + motif) | Léger, pas de bitmap, thématique par monde |
| Navigation | Routes paramétrées `?name=` / `?file=` / `?scenarioId=` | Pattern existant respecté |

---

*DAEDALUS — Architecte technique*