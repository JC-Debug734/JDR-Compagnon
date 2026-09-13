# Brief UX/UI — Évolution JDR Compagnon

> **Auteur :** MUSE (UX/UI Designer)  
> **Date :** 21 août 2026  
> **Projet :** JDR Compagnon — `com.jc2.jdrcompagnon`  
> **Cible :** Android / Kotlin / Jetpack Compose / Material 3

---

## Table des matières

1. [Parcours bestiaire](#1--parcours-bestiaire)
2. [Parcours sorts et règles](#2--parcours-sorts-et-règles)
3. [Éditeur de scénarios markdown](#3--éditeur-de-scénarios-markdown)
4. [Design des fonds d'écran](#4--design-des-fonds-décran)
5. [Layout des nouveaux écrans](#5--layout-des-nouveaux-écrans)
6. [String resources](#6--string-resources)
7. [Amélioration visuelle générale](#7--amélioration-visuelle-générale)
8. [Schémas ASCII](#8--schémas-ascii-par-écran)

---

## 1 — Parcours bestiaire

### Point de départ

L'écran `MjHomeScreen` contient déjà une carte `BESTIAIRE` (id=`"bestiary"`, icône `Icons.AutoMirrored.Filled.MenuBook`).  
Actuellement, cette carte appelle `onViewCharacters()` qui navigue vers `CharacterSelectionScreen?isMj=true` — c'est-à-dire la **liste des personnages créés**, pas un vrai bestiaire.

### Nouveau parcours proposé

```
MjHomeScreen
  └── carte BESTIAIRE (id="bestiary")
      └── onTap → navigate(Route.Bestiary.path)
          └── BestiaryScreen
              ├── Onglets haut : Monstres | Sorts | Règles | Équipement | Glossaire
              ├── Barre de recherche (SearchBar)
              ├── Rail alphabétique (scroll vertical A→Z)
              └── → tap sur un monstre
                  └── MonsterDetailScreen (plein écran, scroll vertical)
                      ├── En-tête : nom + type + alignement + CR
                      ├── Stat block : AC, HP, Speed, Stats (STR/DEX/CON/INT/WIS/CHA)
                      ├── Traits (scroll)
                      ├── Actions (scroll)
                      └── Actions légendaires (si présentes)
```

### Détail des interactions

| Étape | Action utilisateur | Résultat |
|-------|-------------------|----------|
| 1 | Tap carte BESTIAIRE sur MjHomeScreen | Navigation vers `BestiaryScreen` (onglet « Monstres » actif par défaut) |
| 2 | Saisie dans la SearchBar | Filtrage en temps réel sur le nom des monstres |
| 3 | Scroll vertical dans la liste | Parcours alphabétique A→Z, header `A`, `B`, `C` sticky |
| 4 | Tap sur un monstre | Navigation vers `MonsterDetailScreen` avec le contenu markdown rendu |
| 5 | Swipe horizontal / bouton ←→ | Navigation entre monstres (précédent / suivant) sans revenir à la liste |
| 6 | Bouton retour | Retour à la liste, recherche conservée |

### Architecture technique

- **Source :** `assets/srd/monsters-a-z.md` (506 KB, 400+ monstres, format `### Nom` + stat block en HTML/MD mixte)
- **Parsing :** un `BestiaryRepository` lit le fichier au premier accès, parse par regex `^### (.+)$` pour extraire chaque entrée (nom + contenu jusqu'au prochain `###` ou `##`)
- **ViewModel :** `BestiaryViewModel` expose `uiState: StateFlow<BestiaryUiState>` avec `query`, `monsters: List<MonsterEntry>`, `selectedMonsterIndex`
- **Rendu markdown :** librairie `dev.jeziellago:markwon` ou `com.mikepenz:multiplatform-markdown-renderer` pour afficher le stat block HTML+MD

---

## 2 — Parcours sorts et règles

### Approche : onglets dans la même Bibliothèque

Plutôt que des écrans séparés, regrouper Bestiaire, Sorts, Règles, Équipement et Glossaire sous un même écran `LibraryScreen` avec un `TabRow` en haut. Justification :

- Cohérence : tous ces contenus sont des références SRD embarquées
- Navigation fluide : l'utilisateur switch d'onglet sans re-naviguer
- Réutilisation : même SearchBar, même pattern liste→détail

### Parcours

```
MjHomeScreen
  └── carte BIBLIOTHÈQUE (anciennement BESTIAIRE, renommée)
      └── onTap → navigate(Route.Library.path)
          └── LibraryScreen
              ├── TabRow : [Monstres] [Sorts] [Règles] [Équipement] [Glossaire]
              ├── SearchBar (filtrage contextuel à l'onglet actif)
              ├── Liste alphabétique
              └── → tap sur une entrée
                  └── EntryDetailScreen (générique, rendu markdown)
```

### Parsing par source

| Onglet | Fichier | Pattern de parsing | ~Nombre d'entrées |
|--------|---------|--------------------|--------------------|
| Monstres | `monsters-a-z.md` (506 KB) | `^### Nom$` | 400+ |
| Sorts | `spells.md` (319 KB) | `^## Nom du sort$` (niveau 2) | 500+ |
| Règles | `rules.md` (64 KB) | `^## Section$` | ~20 sections |
| Équipement | `equipment.md` (71 KB) | `^## / ^### ` | ~50 |
| Glossaire | `rules-glossary.md` (72 KB) | `^### Terme$` | ~100 |

### Détail onglet Sorts

- Chaque sort est délimité par `## Nom du sort` suivi de métadonnées (niveau, école, temps d'incantation, portée, composantes, durée, description)
- La liste affiche : nom du sort + badge de niveau (cantrip, 1er, 2e…) + école (icone couleur)
- Vue détail : rendu markdown complet avec mise en forme du stat block

### Détail onglet Règles

- Navigation par section (pas alphabétique mais par thèmes : « Rythme de jeu », « Capacités », « Combat »…)
- Affichage en mode « document » : scroll vertical continu, pas besoin de vue détail séparée
- Ajouter une mini-table-des-matières cliquable en haut

---

## 3 — Éditeur de scénarios markdown

### État actuel

- Scénarios stockés dans `GameState.MjScenario` (titre + description en texte brut)
- Édition via `AlertDialog` avec deux `OutlinedTextField` basiques (titre + description)
- Notes de scénario dans un `OutlinedTextField` de 160 dp
- **Aucun support markdown** nulle part

### Nouveau parcours proposé

```
MjSetupScreen
  └── section « 1. Choix du Scénario »
      ├── liste déroulante scénarios existants
      ├── bouton Éditer (icône Edit) → ScenarioEditorScreen
      └── bouton + « Créer un nouveau scénario » → ScenarioEditorScreen

MjHomeScreen (session en cours)
  └── carte SCÉNARIO (nouvelle carte) → ScenarioEditorScreen(scenarioId)

ScenarioEditorScreen
  ├── TopAppBar : titre écran + bouton retour + bouton Enregistrer
  ├── TextField : Titre du scénario
  ├── TabRow : [Éditer] [Aperçu]
  ├── Mode Éditer :
  │   ├── Barre d'outils formatage (horizontal scroll)
  │   │   ├── B (bold)     → insère **texte**
  │   │   ├── I (italic)   → insère *texte*
  │   │   ├── H (heading)  → insère ## 
  │   │   ├── • (list)     → insère - 
  │   │   ├── 1. (ordered) → insère 1. 
  │   │   ├── — (hr)      → insère ---
  │   │   ├── "" (quote)  → insère > 
  │   │   └── {} (code)   → insère `texte`
  │   └── OutlinedTextField multiline (hauteur flexible, min 300dp)
  └── Mode Aperçu :
      └── MarkdownRenderer (rendu temps réel du contenu saisi)
```

### Détail des interactions

| Étape | Action | Résultat |
|-------|--------|----------|
| 1 | Tap « Créer un nouveau scénario » | `ScenarioEditorScreen(mode=CREATE)` |
| 2 | Saisie du titre | TextField standard, singleLine |
| 3 | Saisie markdown dans le corps | OutlinedTextField avec police monospace |
| 4 | Tap bouton B | Insertion de `**` autour de la sélection (ou au curseur) |
| 5 | Tap onglet « Aperçu » | Rendu markdown en temps réel |
| 6 | Tap onglet « Éditer » | Retour à l'éditeur, curseur conservé |
| 7 | Tap « Enregistrer » | Sauvegarde dans `GameState`, retour à l'écran précédent |

### Architecture technique

- **Stockage :** Ajouter un champ `markdownContent: String = ""` à `GameState.MjScenario` (rétrocompatible)
- **Éditeur :** `OutlinedTextField` avec `Modifier.weight(1f).fillMaxHeight()` et `TextStyle(fontFamily = FontFamily.Monospace)`
- **Aperçu :** `Markwon.create(context).setMarkdown(text)` ou composable `MarkdownText(markdown = content)`
- **Barre d'outils :** `LazyRow` d'`IconButton` avec icônes Material : `FormatBold`, `FormatItalic`, `Title`, `FormatListBulleted`, `FormatListNumbered`, `HorizontalRule`, `FormatQuote`, `Code`
- **Insertion au curseur :** utiliser `TextFieldValue` avec `TextFieldValue.selection` pour détecter la position du curseur et insérer les marqueurs markdown

---

## 4 — Design des fonds d'écran

### Principe

Le fond d'écran est un `Box` en arrière-plan de chaque écran, derrière le `Scaffold`. Il s'adapte au monde sélectionné (`currentWorld?.id`).

### 4.1 — Thème D&D : texture parchemin

**Objectif :** évoquer un grimoire ancien, un parchemin vieilli.

**Implémentation Compose :**

```kotlin
// Box de fond, derrière le Scaffold
Box(modifier = Modifier.fillMaxSize()) {
    // Cou1 : gradient vertical (haut plus clair, bas plus sombre)
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF5E6C8),  // haut : parchemin clair
                Color(0xFFE8D5A8),  // milieu
                Color(0xFFD4BC7E),  // bas : parchemin plus sombre
            )
        ))
    )
    // Couche 2 : motifs subtils (fibres du parchemin)
    // → drawable tile PNG en repeat, opacité 0.06
    // → ou vectoriel : lignes fines horizontales aléatoires en couleur #C4A86A
    Box(modifier = Modifier
        .fillMaxSize()
        .paint(parcheminTexturePainter)  // tile mode
        .alpha(0.08f)
    )
    // Couche 3 : vignette (bords assombris)
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF8B6F3A).copy(alpha = 0.15f),
            ),
            radius = 1.4f  // dépasse les bords pour un effet doux
        ))
    )
    // Scaffold par-dessus
    Scaffold(containerColor = Color.Transparent) { ... }
}
```

**Palette précise :**

| Token | Couleur | Usage |
|-------|---------|-------|
| `ParcheminClair` | `#F5E6C8` | Haut du gradient |
| `ParcheminMoyen` | `#E8D5A8` | Centre du gradient |
| `ParcheminSombre` | `#D4BC7E` | Bas du gradient |
| `ParcheminVignette` | `#8B6F3A` alpha 0.15 | Bords assombris |
| `ParcheminFibre` | `#C4A86A` alpha 0.06 | Motif de fibre |
| `EncreDnd` | `#3E2723` | Texte sur parchemin (mode clair) |

**Comportement des cartes sur le fond parchemin :**

- Cartes : `surface` avec `Color(0xFFF8F0DC)` (légèrement plus clair que le fond), bordure `1.dp` couleur `#C4A86A` alpha 0.3, `tonalElevation = 0.dp` (pas d'ombre portée, ou très légère)
- Cards Material : `containerColor = Color.White.copy(alpha = 0.75f)` pour effet « page posée sur le parchemin »
- Texte sur fond parchemin : couleur `#3E2723` (brun foncé) pour le mode clair, `OffWhite` pour le mode sombre

### 4.2 — Thème Naheulbeuk : texture bois

**Objectif :** évoquer une planche de bois usée, une taverne.

**Implémentation Compose :**

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Couche 1 : gradient vertical (haut + sombre, bas + clair, effet planche)
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3E2723),  // haut : bois sombre
                Color(0xFF5D4037),  // centre
                Color(0xFF4E342E),  // bas
            )
        ))
    )
    // Couche 2 : motif de fibres du bois (lignes verticales ondulées)
    // → drawable tile PNG en repeat, opacité 0.10
    Box(modifier = Modifier
        .fillMaxSize()
        .paint(woodTexturePainter)
        .alpha(0.10f)
    )
    // Couche 3 : veines plus claires (noeuds du bois)
    // → spots radiaux subtils en couleur #8D6E63 alpha 0.04
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.radialGradient(
            colors = listOf(
                Color(0xFF8D6E63).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(0.3f, 0.2f),  // décalé pour asymétrie
            radius = 0.8f,
        ))
    )
    // Scaffold par-dessus
    Scaffold(containerColor = Color.Transparent) { ... }
}
```

**Palette précise :**

| Token | Couleur | Usage |
|-------|---------|-------|
| `BoisSombre` | `#3E2723` | Haut du gradient |
| `BoisMoyen` | `#5D4037` | Centre |
| `BoisFonce` | `#4E342E` | Bas |
| `BoisVeine` | `#8D6E63` alpha 0.04 | Noeuds / veines |
| `BoisFibre` | `#6D4C41` alpha 0.10 | Motif fibre |
| `TexteBois` | `#EFEBE9` | Texte sur bois |

**Comportement des cartes sur le fond bois :**

- Cartes : `surface` avec `Color(0xFF5D4037).copy(alpha = 0.85f)` ou `MaterialTheme.colorScheme.surface` (déjà sombre)
- Bordure : `1.dp` couleur `#8D6E63` alpha 0.3
- `tonalElevation = 2.dp` pour un léger relief (la carte « flotte » au-dessus du bois)
- Texte sur fond bois : couleur `#EFEBE9` (beige clair) en mode sombre

### 4.3 — Structure du composant de fond

Créer un composant réutilisable `WorldBackground` :

```kotlin
@Composable
fun WorldBackground(content: @Composable () -> Unit) {
    val currentWorld by GameState.currentWorld.collectAsState()
    val isDark = isSystemInDarkTheme()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (currentWorld?.id) {
            "donjon_et_dragon" -> ParchmentBackground(isDark)
            "naheulbeuk" -> WoodBackground(isDark)
            else -> {}  // fond par défaut (DeepBlack/SurfaceDark)
        }
        content()
    }
}
```

**Usage dans chaque écran :**

```kotlin
WorldBackground {
    Scaffold(containerColor = Color.Transparent) { ... }
}
```

### 4.4 — Adaptation dark/light

- **Mode sombre D&D :** le parchemin devient plus sombre — gradient de `#2A1F14` à `#3E2D1A`, fibres plus visibles (alpha 0.12), vignette plus marquée (alpha 0.25)
- **Mode sombre Naheulbeuk :** le bois devient plus profond — gradient de `#1E1410` à `#2E1A12`, fibres alpha 0.08
- **Mode clair D&D :** parchemin lumineux comme décrit ci-dessus
- **Mode clair Naheulbeuk :** bois plus chaud, gradient de `#6D4C41` à `#8D6E63`

---

## 5 — Layout des nouveaux écrans

### 5.1 — LibraryScreen (Bestiaire + Sorts + Règles)

```
┌─────────────────────────────────────┐
│ TopAppBar                           │
│  ← BIBLIOTHÈQUE          [monde]    │
├─────────────────────────────────────┤
│ TabRow (scrollable)                 │
│ [Monstres] [Sorts] [Règles] [Équip] │
│ [Glossaire]                         │
├─────────────────────────────────────┤
│ SearchBar                           │
│  🔍 Rechercher un monstre...        │
├─────────────────────────────────────┤
│                                     │
│  ┌─ A ─────────────────────────┐   │
│  │ Aboleth          CR 10      │   │
│  │ Aberration · Loyal Mauvais  │   │
│  ├─────────────────────────────┤   │
│  │ Air Elemental     CR 5      │   │
│  │ Élémentaire · Neutre        │   │
│  ├─────────────────────────────┤   │
│  │ Animated Armor    CR 1      │   │
│  │ Construct · ...             │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ B ─────────────────────────┐   │
│  │ Balor             CR 19     │   │
│  │ Démon · Chaotique Mauvais   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ C ─────────────────────────┐   │
│  │ ...                         │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

**Hiérarchie visuelle :**

1. **TopAppBar** — titre `BIBLIOTHÈQUE` en `Type.HeroTitle` (lettres espacées), bouton retour, badge monde
2. **TabRow scrollable** — 5 onglets, icône + libellé, onglet actif souligné par `secondaryContainer`
3. **SearchBar** — `OutlinedTextField` avec `Icons.Default.Search` en leading, placeholder contextuel
4. **LazyColumn** — headers sticky alphabétiques (`A`, `B`, `C`…), cellules monstres
5. **Cellule monstre** — `Surface` cliquable, 2 lignes : nom (titleMedium, Bold) + métadonnées (bodySmall, onSurfaceVariant), badge CR à droite

**Composants :**

- `TabRow` (scrollable, Material 3 `ScrollableTabRow` equivalent : `TabRow(selectedTabIndex)` avec `ScrollableTabRow`)
- `OutlinedTextField` avec `leadingIcon = Icons.Default.Search`
- `LazyColumn` avec `stickyHeader` pour les lettres alphabétiques
- `MonsterListEntry` : composable custom, `Surface` cliquable, `Row` avec `Column` (nom + meta) et `Column` (badge CR)

### 5.2 — EntryDetailScreen (vue détail générique)

```
┌─────────────────────────────────────┐
│ TopAppBar                           │
│  ← Aboleth            [←] [→]       │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ABOLETH                      │   │
│  │ Large Aberration, Lawful Evil│   │
│  │ CR 10 · XP 5,900             │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Stat Block ────────────────┐   │
│  │ AC 17    Initiative +7      │   │
│  │ HP 150   Speed 10, Swim 40  │   │
│  ├─────────────────────────────┤   │
│  │ STR 21 (+5)   DEX 9 (-1)    │   │
│  │ CON 15 (+2)   INT 18 (+4)   │   │
│  │ WIS 15 (+2)   CHA 18 (+4)   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Traits ────────────────────┐   │
│  │ Amphibious                  │   │
│  │ Eldritch Restoration        │   │
│  │ Legendary Resistance        │   │
│  │ Mucus Cloud                 │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Actions ──────────────────┐   │
│  │ Multiattack                 │   │
│  │ Tentacle                    │   │
│  │ Consume Memories            │   │
│  │ Dominate Mind               │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ Legendary Actions ────────┐   │
│  │ Lash                        │   │
│  │ Psychic Drain               │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

**Hiérarchie visuelle :**

1. **TopAppBar** — nom du monstre en `titleMedium` Bold, boutons ←→ pour navigation séquentielle
2. **En-tête** — carte large (`Surface` `Shapes.Large`), nom en `headlineMedium` Bold, type/alignement en `bodyMedium` onSurfaceVariant, badge CR en `Surface` coloré (primary pour CR élevé, secondary pour CR bas)
3. **Stat block** — `Surface` avec bordure, grille 2 colonnes : stats (STR/DEX/CON) à gauche, (INT/WIS/CHA) à droite
4. **Sections** — chacune un `Surface` avec titre de section (`labelLarge` Bold, primary), contenu markdown rendu

**Navigation entre monstres :**

- `IconButton` `Icons.AutoMirrored.Filled.ArrowBack` (gauche) et `Icons.AutoMirrored.Filled.ArrowForward` (droite) dans les `actions` de la TopAppBar
- Maintient l'index dans la liste filtrée ; désactivé aux extrémités
- Alternative : `HorizontalPager` pour swipe entre monstres

### 5.3 — ScenarioEditorScreen

```
┌─────────────────────────────────────┐
│ TopAppBar                           │
│  ← Éditeur scénario      ✓ Enregistrer│
├─────────────────────────────────────┤
│ Titre du scénario                   │
│ ┌─────────────────────────────────┐ │
│ │ La Malédiction du Marais        │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ TabRow                              │
│   [✏ Éditer]     [👁 Aperçu]        │
├─────────────────────────────────────┤
│                                     │
│  ┌─ Barre d'outils ─────────────┐  │
│  │ B  I  H  •  1.  —  "  {}    │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ## Acte I : L'arrivée        │   │
│  │                              │   │
│  │ Les aventuriers arrivent    │   │
│  │ au village de **Brumeval**. │   │
│  │                              │   │
│  │ - Le maire les accueille    │   │
│  │ - Un paysan parle de monstres│   │
│  │ - La vieille mage offre une │   │
│  │   carte ancienne             │   │
│  │                              │   │
│  │ > « Méfiez-vous du marais » │   │
│  │                              │   │
│  │ ---                          │   │
│  │                              │   │
│  │ ## Acte II : Le marais       │   │
│  │ ...                          │   │
│  │ ▼ curseur                    │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

**Mode Aperçu :**

```
┌─────────────────────────────────────┐
│ TopAppBar                           │
│  ← Éditeur scénario      ✓ Enregistrer│
├─────────────────────────────────────┤
│ Titre du scénario                   │
│ ┌─────────────────────────────────┐ │
│ │ La Malédiction du Marais        │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ TabRow                              │
│   [✏ Éditer]     [👁 Aperçu]        │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │                              │   │
│  │  Acte I : L'arrivée          │   │ ← H2 rendu en headlineSmall
│  │                              │   │
│  │  Les aventuriers arrivent    │   │
│  │  au village de Brumeval.     │   │ ← **bold** rendu
│  │                              │   │
│  │  • Le maire les accueille    │   │ ← liste à puces
│  │  • Un paysan parle de monstres│   │
│  │  • La vieille mage offre une │   │
│  │    carte ancienne            │   │
│  │                              │   │
│  │  ┌──────────────────────┐   │   │ ← blockquote
│  │  │ Méfiez-vous du marais │   │   │
│  │  └──────────────────────┘   │   │
│  │                              │   │
│  │  ──────────────────────     │   │ ← hr
│  │                              │   │
│  │  Acte II : Le marais        │   │
│  │  ...                         │   │
│  │                              │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

**Hiérarchie visuelle :**

1. **TopAppBar** — titre `Éditeur scénario` + bouton retour + bouton Enregistrer (icône `Icons.Default.Save`)
2. **TextField titre** — plein largeur, `singleLine = true`, placeholder « Titre du scénario »
3. **TabRow** — 2 onglets avec icône : `Icons.Default.Edit` + `Icons.Default.Visibility`
4. **Barre d'outils** (mode Éditer uniquement) — `LazyRow` d'`IconButton`, fond `surfaceVariant`, séparateurs entre groupes
5. **Zone d'édition** — `OutlinedTextField` avec `fontFamily = FontFamily.Monospace`, `minHeight = 300.dp`, scroll interne
6. **Zone d'aperçu** — `Column` scrollable avec `MarkdownText` rendu, padding 16dp

---

## 6 — String resources

Nouveaux libellés à ajouter dans `res/values/strings.xml` :

```xml
<!-- ===== LibraryScreen (Bibliothèque) ===== -->
<string name="library_title">BIBLIOTHÈQUE</string>
<string name="library_tab_monsters">Monstres</string>
<string name="library_tab_spells">Sorts</string>
<string name="library_tab_rules">Règles</string>
<string name="library_tab_equipment">Équipement</string>
<string name="library_tab_glossary">Glossaire</string>
<string name="library_search_monsters">Rechercher un monstre…</string>
<string name="library_search_spells">Rechercher un sort…</string>
<string name="library_search_rules">Rechercher une règle…</string>
<string name="library_search_equipment">Rechercher un équipement…</string>
<string name="library_search_glossary">Rechercher un terme…</string>
<string name="library_empty">Aucun résultat trouvé.</string>
<string name="library_loading">Chargement de la bibliothèque…</string>
<string name="library_count">%1$d entrées</string>

<!-- ===== Monster Detail ===== -->
<string name="monster_cr_label">CR %1$s</string>
<string name="monster_xp_label">XP %1$s</string>
<string name="monster_section_stats">Statistiques</string>
<string name="monster_section_traits">Traits</string>
<string name="monster_section_actions">Actions</string>
<string name="monster_section_legendary">Actions légendaires</string>
<string name="monster_section_reactions">Réactions</string>
<string name="monster_nav_prev">Monstre précédent</string>
<string name="monster_nav_next">Monstre suivant</string>

<!-- ===== Spell Detail ===== -->
<string name="spell_level_cantrip">Cantrip</string>
<string name="spell_level_n">%1$der niveau</string>
<string name="spell_school_abjuration">Abjuration</string>
<string name="spell_school_conjuration">Conjuration</string>
<string name="spell_school_divination">Divination</string>
<string name="spell_school_enchantment">Enchantement</string>
<string name="spell_school_evocation">Évocation</string>
<string name="spell_school_illusion">Illusion</string>
<string name="spell_school_necromancy">Nécromancie</string>
<string name="spell_school_transmutation">Transmutation</string>
<string name="spell_casting_time">Temps d\'incantation</string>
<string name="spell_range">Portée</string>
<string name="spell_components">Composantes</string>
<string name="spell_duration">Durée</string>

<!-- ===== Rules ===== -->
<string name="rules_toc_title">Sommaire</string>

<!-- ===== Scenario Editor ===== -->
<string name="scenario_editor_title">Éditeur de scénario</string>
<string name="scenario_editor_title_hint">Titre du scénario</string>
<string name="scenario_editor_tab_edit">Éditer</string>
<string name="scenario_editor_tab_preview">Aperçu</string>
<string name="scenario_editor_save">Enregistrer</string>
<string name="scenario_editor_body_hint">Saisissez votre scénario en Markdown…</string>
<string name="scenario_editor_body_empty">Aucun contenu. Commencez à écrire votre scénario.</string>
<string name="scenario_editor_preview_empty">L\'aperçu apparaîtra ici.</string>
<string name="scenario_editor_fmt_bold">Gras</string>
<string name="scenario_editor_fmt_italic">Italique</string>
<string name="scenario_editor_fmt_heading">Titre</string>
<string name="scenario_editor_fmt_list_bullet">Liste à puces</string>
<string name="scenario_editor_fmt_list_ordered">Liste numérotée</string>
<string name="scenario_editor_fmt_hr">Ligne de séparation</string>
<string name="scenario_editor_fmt_quote">Citation</string>
<string name="scenario_editor_fmt_code">Code</string>

<!-- ===== MjHomeScreen (nouvelles cartes) ===== -->
<string name="mj_tool_library">BIBLIOTHÈQUE</string>
<string name="mj_tool_library_desc">Monstres, sorts & règles</string>
<string name="mj_tool_scenario">SCÉNARIO</string>
<string name="mj_tool_scenario_desc">Éditeur markdown</string>

<!-- ===== WorldBackground (accessibility) ===== -->
<string name="a11y_bg_parchment">Texture parchemin</string>
<string name="a11y_bg_wood">Texture bois</string>
```

---

## 7 — Amélioration visuelle générale

### 7.1 — Cartes et surfaces

| Élément actuel | Amélioration proposée |
|----------------|----------------------|
| `Surface` plat avec `tonalElevation = 2.dp` | Ajouter `shadowElevation = 4.dp` + `border = BorderStroke(1.dp, primary.copy(alpha=0.15f))` |
| Pas de transition au tap | `AnimatedContent` pour les changements d'onglet, `Crossfade` pour edit/preview |
| `RoundedCornerShape(28.dp)` uniforme | Utiliser `Shapes.HeroCard` (32dp) pour les cartes hero, `Shapes.Card` (12dp) pour les cellules de liste |
| Divider invisible | `HorizontalDivider` avec `color = outlineVariant.copy(alpha = 0.3f)` |

### 7.2 — Transitions et animations

- **Navigation entre écrans :** `AnimatedContentTransitionSpec` avec slide horizontal (enter from right, exit to left)
- **Changement d'onglet :** `Crossfade` avec `Motion.TweenStandard()` (250ms)
- **Apparition liste :** `AnimatedVisibility` + `slideInVertically` par item avec stagger 50ms (utiliser `Motion.StaggerDelay`)
- **SearchBar :** `animateContentSize` quand la barre apparaît/disparaît
- **Toggle edit/preview :** `Crossfade(animationSpec = Motion.TweenEmphasized())` (350ms)

### 7.3 — Contraste et lisibilité

- **Mode sombre :** s'assurer que `onSurface` a un ratio de contraste ≥ 4.5:1 avec `surface`
  - D&D dark : `surface = #1A1212`, `onSurface = #FFFFFF` → ratio 16:1 ✓
  - Naheulbeuk dark : `surface = #121A12`, `onSurface = #FFFFFF` → ratio 17:1 ✓
- **Mode clair avec parchemin :** texte en `#3E2723` sur `#F5E6C8` → ratio 12:1 ✓
- **Badges CR :** utiliser `primary` (rouge/vert) sur `primaryContainer` pour un contraste suffisant
- **SearchBar placeholder :** `onSurfaceVariant.copy(alpha = 0.5f)` au lieu de `onSurfaceVariant` pur

### 7.4 — Icônes Material recommandées

| Usage | Icône Material |
|-------|---------------|
| Onglet Monstres | `Icons.AutoMirrored.Filled.MenuBook` |
| Onglet Sorts | `Icons.Default.AutoFixHigh` |
| Onglet Règles | `Icons.Default.Gavel` (ou `Icons.Default.Rule`) |
| Onglet Équipement | `Icons.Default.Backpack` (ou `Icons.Default.Shield`) |
| Onglet Glossaire | `Icons.Default.Translate` (ou `Icons.Default.MenuBook`) |
| Recherche | `Icons.Default.Search` |
| Éditer scénario | `Icons.Default.Edit` |
| Aperçu scénario | `Icons.Default.Visibility` |
| Enregistrer | `Icons.Default.Save` |
| Bold | `Icons.Default.FormatBold` |
| Italic | `Icons.Default.FormatItalic` |
| Heading | `Icons.Default.Title` |
| List bullet | `Icons.Default.FormatListBulleted` |
| List ordered | `Icons.Default.FormatListNumbered` |
| HR | `Icons.Default.HorizontalRule` |
| Quote | `Icons.Default.FormatQuote` |
| Code | `Icons.Default.Code` |
| Monstre précédent | `Icons.AutoMirrored.Filled.ArrowBack` |
| Monstre suivant | `Icons.AutoMirrored.Filled.ArrowForward` |
| Scénario carte MJ | `Icons.Default.AutoStories` (ou `Icons.Default.Description`) |

### 7.5 — Amélioration de l'ambiance

- **TopAppBar :** ajouter un léger gradient de fond `verticalGradient(surface, surface.copy(alpha=0.95f))` pour un effet de profondeur
- **Cartes d'outils MjHomeScreen :** ajouter une lueur subtile `Brush.radialGradient` en coin haut-droit (déjà présent sur `WorldSelectionCard`, étendre à `ModernToolCard`)
- **FAB optionnel :** sur `LibraryScreen`, un FAB « retour en haut » (`Icons.Default.ArrowUpward`) qui apparaît après scroll
- **Empty states :** illustration icône + texte centré, ex : `Icons.SearchOff` + « Aucun monstre ne correspond à votre recherche. »
- **Loading states :** `CircularProgressIndicator` centré avec `Motion.TweenEmphasized()` sur l'opacité

---

## 8 — Schémas ASCII par écran

### 8.1 — MjHomeScreen (évolué)

```
┌──────────────────────────────────────────┐
│  ←  MAÎTRE DU JEU              ☰  [monde]│
├──────────────────────────────────────────┤
│                                          │
│  Scénario : La Malédiction du Marais     │
│  Groupe : Les Aventuriers de Brumeval   │
│  ┌──────────────────────────────────┐   │
│  │ 📝 Notes du scénario              │   │
│  │ ┌──────────────────────────────┐ │   │
│  │ │ Ajoutez des notes...          │ │   │
│  │ └──────────────────────────────┘ │   │
│  └──────────────────────────────────┘   │
│                                          │
│  VOS OUTILS DE GESTION                   │
│                                          │
│  ┌────────────┐  ┌────────────┐          │
│  │     ➕      │  │     📖     │          │
│  │   CRÉER     │  │ BIBLIOTHÈQUE│         │
│  │ Héros/PNJ   │  │ Monstres & │          │
│  │             │  │ Sorts &    │          │
│  └────────────┘  │ Règles     │          │
│                  └────────────┘          │
│  ┌────────────┐  ┌────────────┐          │
│  │     📝      │  │     📄     │          │
│  │   NOTES     │  │  SCÉNARIO  │          │
│  │ Intrigues & │  │  Éditeur   │          │
│  │   Idées     │  │  Markdown  │          │
│  └────────────┘  └────────────┘          │
│                                          │
└──────────────────────────────────────────┘
```

### 8.2 — LibraryScreen (onglet Monstres)

```
┌──────────────────────────────────────────┐
│  ←  BIBLIOTHÈQUE            [D&D]  ☰     │
├──────────────────────────────────────────┤
│ [📖 Monstres] [✨ Sorts] [⚖ Règles] [🎒 É] │
├──────────────────────────────────────────┤
│  🔍 Rechercher un monstre…               │
├──────────────────────────────────────────┤
│                                          │
│  ╔═ A ════════════════════════════╗      │
│  ║ Aboleth              CR 10    ║      │
│  ║ Aberration · Loyal Mauvais    ║      │
│  ╠────────────────────────────────╣      │
│  ║ Air Elemental        CR 5     ║      │
│  ║ Élémentaire · Neutre           ║      │
│  ╠────────────────────────────────╣      │
│  ║ Animated Armor        CR 1     ║      │
│  ║ Construct · Neutre              ║      │
│  ╚════════════════════════════════╝      │
│                                          │
│  ╔═ B ════════════════════════════╗      │
│  ║ Balor                 CR 19    ║      │
│  ║ Démon · Chaotique Mauvais      ║      │
│  ╠────────────────────────────────╣      │
│  ║ Basilisk              CR 3     ║      │
│  ║ Monstruosité · Neutre          ║      │
│  ╚════════════════════════════════╝      │
│                                          │
│  ╔═ C ════════════════════════════╗      │
│  ║ ...                           ║      │
│  ╚════════════════════════════════╝      │
│                                          │
└──────────────────────────────────────────┘
```

### 8.3 — LibraryScreen (onglet Sorts)

```
┌──────────────────────────────────────────┐
│  ←  BIBLIOTHÈQUE            [D&D]  ☰     │
├──────────────────────────────────────────┤
│ [📖 Monstres] [✨ Sorts] [⚖ Règles] [🎒 É] │
├──────────────────────────────────────────┤
│  🔍 Rechercher un sort…                  │
├──────────────────────────────────────────┤
│                                          │
│  Cantrips                                │
│  ┌──────────────────────────────────┐    │
│  │ ⚡ Bougie de feu    Évocation     │    │
│  │ Temps: 1 action · Portée: 36 m   │    │
│  ├──────────────────────────────────┤    │
│  │ ❄ Rayon de givre   Évocation     │    │
│  │ Temps: 1 action · Portée: 18 m   │    │
│  └──────────────────────────────────┘    │
│                                          │
│  Niveau 1                                │
│  ┌──────────────────────────────────┐    │
│  │ 🛡 Bouclier         Évocation     │    │
│  │ Temps: 1 réaction · Durée: 1 rd  │    │
│  ├──────────────────────────────────┤    │
│  │ ✨ Projectile magique  Évocation  │    │
│  │ Temps: 1 action · Portée: 36 m   │    │
│  └──────────────────────────────────┘    │
│                                          │
│  Niveau 2                                │
│  ┌──────────────────────────────────┐    │
│  │ ...                              │    │
│  └──────────────────────────────────┘    │
│                                          │
└──────────────────────────────────────────┘
```

### 8.4 — EntryDetailScreen (détail monstre)

```
┌──────────────────────────────────────────┐
│  ← Aboleth                    [←] [→]   │
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────────────┐    │
│  │  ABOLETH                         │    │
│  │  Large Aberration, Lawful Evil   │    │
│  │  ┌──────┐  ┌──────────┐         │    │
│  │  │CR 10 │  │XP 5,900  │         │    │
│  │  └──────┘  └──────────┘         │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ┌── STAT BLOCK ────────────────────┐    │
│  │ AC 17        Initiative +7      │    │
│  │ HP 150        Speed 10, Swim 40  │    │
│  ├──────────────────────────────────┤    │
│  │ STR 21 (+5)  │  INT 18 (+4)     │    │
│  │ DEX  9 (-1)  │  WIS 15 (+2)     │    │
│  │ CON 15 (+2)  │  CHA 18 (+4)     │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ┌── TRAITS ────────────────────────┐    │
│  │ Amphibious                      │    │
│  │ Eldritch Restoration             │    │
│  │ Legendary Resistance (3/Day)     │    │
│  │ Mucus Cloud                      │    │
│  │ Probing Telepathy                │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ┌── ACTIONS ───────────────────────┐   │
│  │ Multiattack                      │    │
│  │ Tentacle                         │    │
│  │ Consume Memories                  │    │
│  │ Dominate Mind (2/Day)            │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ┌── LEGENDARY ACTIONS ─────────────┐   │
│  │ Lash                             │    │
│  │ Psychic Drain                    │    │
│  └──────────────────────────────────┘    │
│                                          │
└──────────────────────────────────────────┘
```

### 8.5 — ScenarioEditorScreen (mode Éditer)

```
┌──────────────────────────────────────────┐
│  ←  Éditeur de scénario        ✓ Enregistrer│
├──────────────────────────────────────────┤
│  Titre du scénario                       │
│  ┌──────────────────────────────────────┐│
│  │ La Malédiction du Marais            ││
│  └──────────────────────────────────────┘│
├──────────────────────────────────────────┤
│  [✏ Éditer]     [👁 Aperçu]             │
├──────────────────────────────────────────┤
│  ┌──────────────────────────────────────┐│
│  │ B  I  H  •  1.  —  "  {}             ││
│  └──────────────────────────────────────┘│
│                                          │
│  ┌──────────────────────────────────────┐│
│  │ ## Acte I : L'arrivée               ││
│  │                                      ││
│  │ Les aventuriers arrivent au village  ││
│  │ de **Brumeval**.                     ││
│  │                                      ││
│  │ - Le maire les accueille             ││
│  │ - Un paysan parle de monstres        ││
│  │ - La vieille mage offre une carte    ││
│  │                                      ││
│  │ > « Méfiez-vous du marais »          ││
│  │                                      ││
│  │ ---                                  ││
│  │                                      ││
│  │ ## Acte II : Le marais               ││
│  │ ...                                  ││
│  │ ▏                                    ││
│  └──────────────────────────────────────┘│
│                                          │
└──────────────────────────────────────────┘
```

### 8.6 — ScenarioEditorScreen (mode Aperçu)

```
┌──────────────────────────────────────────┐
│  ←  Éditeur de scénario        ✓ Enregistrer│
├──────────────────────────────────────────┤
│  Titre du scénario                       │
│  ┌──────────────────────────────────────┐│
│  │ La Malédiction du Marais            ││
│  └──────────────────────────────────────┘│
├──────────────────────────────────────────┤
│  [✏ Éditer]     [👁 Aperçu]             │
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────────────────┐│
│  │                                      ││
│  │  Acte I : L'arrivée                  ││
│  │  ──────────────────────              ││
│  │                                      ││
│  │  Les aventuriers arrivent au village  ││
│  │  de Brumeval.                         ││
│  │                                      ││
│  │  • Le maire les accueille             ││
│  │  • Un paysan parle de monstres        ││
│  │  • La vieille mage offre une carte    ││
│  │                                      ││
│  │  ┌──────────────────────────────┐    ││
│  │  │ Méfiez-vous du marais        │    ││
│  │  └──────────────────────────────┘    ││
│  │                                      ││
│  │  ────────────────────────             ││
│  │                                      ││
│  │  Acte II : Le marais                  ││
│  │  ──────────────────────               ││
│  │  ...                                  ││
│  │                                      ││
│  └──────────────────────────────────────┘│
│                                          │
└──────────────────────────────────────────┘
```

---

## Annexe — Nouvelles routes de navigation

Ajouter dans `Routes.kt` :

```kotlin
data object Library : Route("library")
data object LibraryEntry : Route("library_entry/{type}/{index}")  // type: monster|spell|rule|equipment|glossary
data object ScenarioEditor : Route("scenario_editor?scenarioId={scenarioId}")
```

Ajouter dans `NavGraph.kt` :

```kotlin
composable(Route.Library.path) {
    LibraryScreen(
        currentWorld = currentWorld,
        onBack = { navController.popBackStack() },
        onEntryClick = { type, index ->
            navController.navigate("library_entry/$type/$index")
        }
    )
}

composable(
    route = Route.LibraryEntry.path,
    arguments = listOf(
        navArgument("type") { type = NavType.StringType },
        navArgument("index") { type = NavType.IntType }
    )
) { backStackEntry ->
    val type = backStackEntry.arguments?.getString("type") ?: "monster"
    val index = backStackEntry.arguments?.getInt("index") ?: 0
    EntryDetailScreen(
        entryType = type,
        entryIndex = index,
        onBack = { navController.popBackStack() }
    )
}

composable(
    route = Route.ScenarioEditor.path,
    arguments = listOf(
        navArgument("scenarioId") { type = NavType.StringType; defaultValue = "" }
    )
) { backStackEntry ->
    val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
    ScenarioEditorScreen(
        scenarioId = scenarioId.ifBlank { null },
        onBack = { navController.popBackStack() },
        onSaved = { navController.popBackStack() }
    )
}
```

---

## Annexe — Résumé des changements de MjHomeScreen

La carte `BESTIAIRE` (id=`"bestiary"`) devient `BIBLIOTHÈQUE` (id=`"library"`) et navigue vers `Route.Library` au lieu de `onViewCharacters()`. Ajouter une nouvelle carte `SCÉNARIO` (id=`"scenario"`, icône `Icons.Default.AutoStories`).

```kotlin
val tools = listOf(
    MjTool("create_character", "CRÉER", "Héros ou PNJ",
        Icons.Default.Add, MaterialTheme.colorScheme.primary),
    MjTool("library", "BIBLIOTHÈQUE", "Monstres, sorts & règles",
        Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.secondary),
    MjTool("scenario", "SCÉNARIO", "Éditeur markdown",
        Icons.Default.AutoStories, MaterialTheme.colorScheme.tertiary),
    MjTool("notes", "NOTES", "Intrigues & Idées",
        Icons.Default.Edit, MaterialTheme.colorScheme.tertiary),
)
```

Callback `onViewCharacters` remplacé par `onOpenLibrary: () -> Unit` et `onOpenScenarioEditor: (String?) -> Unit`.

---

*Fin du brief — MUSE, UX/UI Designer*