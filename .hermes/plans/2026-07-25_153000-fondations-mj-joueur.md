# JDRCompagnon V1 — Fondations MJ/Joueur

> **For Hermes:** Implémenter ce plan étape par étape avec vérifications TDD-lite (build après chaque étape).

**Goal :** Poser les fondations d'une app Android Jetpack Compose à deux rôles (Maître du Jeu / Joueur) avec navigation, thème Material 3, et écrans placeholder structurés pour accueillir les futures features.

**Architecture :** Compose pur, navigation simple (NavHost), structure par packages (`ui/theme`, `ui/navigation`, `ui/screens/mj`, `ui/screens/joueur`). Aucun réseau, aucune persistance pour cette V1 — tout en mémoire.

**Tech Stack :** Kotlin, Jetpack Compose (BOM 2024.10+), Material 3, Navigation Compose, AGP 9.2.1, compileSdk 36, minSdk 24.

---

## Contexte / Assumptions

- Projet vide (scaffolding Gradle seulement, pas de `MainActivity`).
- Émulateur Android Studio disponible côté utilisateur.
- V1 = squelette navigable + écrans placeholder **structurés** (pas juste "Hello world").
- Écran Joueur V1 = sélection d'un personnage parmi 3 fiches d'exemple (liste en dur dans le code).
- Écran MJ V1 = grille d'accès aux outils (Lancer de dés, Bestiaire, Notes) avec des boutons qui pour l'instant affichent juste un Snackbar "À venir".
- Mode sombre/clair automatique (Material 3 par défaut).

---

## Fichiers à créer / modifier

### Créer
- `app/src/main/java/com/jc2/jdrcompagnon/ui/theme/Color.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/theme/Type.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/theme/Theme.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/navigation/Routes.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/screens/RoleSelectionScreen.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/screens/joueur/JoueurHomeScreen.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/screens/joueur/CharacterSelectionScreen.kt`
- `app/src/main/java/com/jc2/jdrcompagnon/ui/screens/mj/MjHomeScreen.kt`

### Modifier
- `gradle/libs.versions.toml` — ajouter versions + libs Compose / Navigation / Activity
- `app/build.gradle.kts` — brancher les nouvelles dépendances + plugin Kotlin Compose
- `app/src/main/java/com/jc2/jdrcompagnon/MainActivity.kt` — assembler le tout

---

## Étapes

### Étape 1 — Catalogue de versions
**Fichier :** `gradle/libs.versions.toml`
Ajouter :
- `kotlin = "2.0.21"` (compatible AGP 9.2.1)
- `composeBom = "2024.10.01"`
- `activityCompose = "1.9.3"`
- `navigationCompose = "2.8.4"`
- `lifecycle = "2.8.7"`

Et les `[libraries]` correspondantes : `androidx-compose-bom`, `androidx-compose-ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`, `androidx-activity-compose`, `androidx-navigation-compose`, `androidx-lifecycle-viewmodel-compose`, `androidx-lifecycle-runtime-compose`.

**Vérif :** ouvrir le fichier, confirmer que la table reste cohérente.

### Étape 2 — Plugin + dépendances app
**Fichier :** `app/build.gradle.kts`
- Ajouter plugin `org.jetbrains.kotlin.plugin.compose` (version Kotlin)
- Ajouter toutes les dépendances Compose/Navigation/Activity/Lifecycle
- Garder les libs actuelles (appcompat, material, core-ktx) — elles ne gênent pas

**Vérif :** `./gradlew help` doit passer (juste la résolution de config).

### Étape 3 — Thème Material 3
- `Color.kt` : palette sobre (parchemin `#F4E4C1`, encre `#2B2B2B`, bordeaux `#722F37`, doré `#C9A227`) + variantes dark.
- `Type.kt` : `Typography` Material 3 par défaut (on peaufinera plus tard).
- `Theme.kt` : `JdrCompagnonTheme` qui suit le mode système.

### Étape 4 — Routes + Navigation
- `Routes.kt` : sealed class avec 4 routes : `RoleSelection`, `MjHome`, `JoueurHome`, `CharacterSelection`.
- `NavGraph.kt` : `NavHost` qui orchestre tout.

### Étape 5 — Écran sélection de rôle
- `RoleSelectionScreen.kt` : titre "JDR Compagnon", sous-titre "Choisissez votre rôle", 2 `Card` géantes (MJ avec icône couronne, Joueur avec icône épée). Clic → navigation.

### Étape 6 — Écran Joueur + sélection de perso
- `JoueurHomeScreen.kt` : titre "Espace Joueur", sous-titre explicatif, bouton "Choisir mon personnage" → navigation, bouton retour.
- `CharacterSelectionScreen.kt` : titre "Choisissez votre personnage", 3 cartes de personnages (données en dur : un Guerrier, un Mage, un Voleur — nom + description courte). Sélection → retour à l'écran Joueur avec Snackbar de confirmation (placeholder).

### Étape 7 — Écran MJ + outils
- `MjHomeScreen.kt` : titre "Espace Maître du Jeu", `LazyVerticalGrid` 2 colonnes avec 3 cartes d'outils :
  - 🎲 Lancer de dés
  - 📖 Bestiaire
  - 📝 Notes
- Clic sur une carte → Snackbar "À venir" (placeholder). Bouton retour.

### Étape 8 — MainActivity
- `MainActivity.kt` : `enableEdgeToEdge` + `Scaffold` + `JdrCompagnonTheme` + `NavGraph`.

### Étape 9 — Vérification finale
- `./gradlew assembleDebug` → doit passer sans erreur ni warning bloquant.
- (Manuel, côté utilisateur) : Run sur émulateur, tester la navigation MJ ↔ Joueur ↔ sélection de perso ↔ retour.

---

## Critères de validation

- ✅ Build Gradle assembleDebug : 0 erreur
- ✅ App se lance sur émulateur sans crash
- ✅ Écran d'accueil → MJ → outils (clics → snackbar) → retour
- ✅ Écran d'accueil → Joueur → sélection perso (3 cartes) → sélection → retour avec confirmation
- ✅ Mode sombre suit le système

---

## Risques / Tradeoffs

- **Versions** : AGP 9.2.1 est très récent (preview). Si la BOM Compose ne s'aligne pas, je downgrade à AGP 8.7 stable + Compose BOM 2024.09. — **Mitigation** : tester `assembleDebug` dès l'étape 2.
- **Plugin `kotlin-compose`** : requis depuis Kotlin 2.0. Si absent, build cassé.
- **Pas de tests unitaires** pour cette V1 (UI sans logique métier). On ajoutera quand on aura la persistance.
- **Pas de ViewModel** pour cette V1 : l'état est local (rappel/souris dans le nav). On en ajoutera quand on aura des données.

---

## Open Questions

Aucune bloquante. Choix utilisateur déjà faits :
- Joueur = sélection de perso (V1)
- MJ = accès outils (V1, sans fonctionnel derrière)
