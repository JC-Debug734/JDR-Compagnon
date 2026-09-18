# Feature Boutique — Référence technique

Document de référence pour reprendre le travail sur cette fonctionnalité dans une nouvelle conversation. À joindre en pièce jointe si tu me redemandes d'y toucher.

## Contexte

Projet **JDR Compagnon** (`com.jc2.jdrcompagnon`), app Android Kotlin/Compose, compagnon de JDR multi-mondes (Donjon et Dragon / Naheulbeuk). La feature Boutique permet au MJ de créer des boutiques avec un standing, un inventaire et des employés générés automatiquement, puis de les gérer manuellement.

Cette feature est la **première du projet à suivre l'architecture Clean/MVVM stricte** (package by feature, 4 couches ui/presentation/domain/data, Use Cases, Sealed UiState) définie dans le document de règles d'architecture du projet. Le reste de l'app est plus ancien et utilise des singletons `object` (GameState, SrdRepository) + SharedPreferences/fichiers `.md`, sans cette séparation.

C'est aussi la **première utilisation de Room** dans le projet (jusqu'ici uniquement SharedPreferences + fichiers `.md`).

## Arborescence des fichiers

```
com/jc2/jdrcompagnon/
├── core/database/
│   └── AppDatabase.kt
├── di/
│   └── BoutiqueDependencies.kt      # provider manuel (pas de Hilt/Koin dans le projet)
├── feature_boutique/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Boutique.kt          # + Marchand ; + type/services/argentDisponibleEnPo/villeId (feature_carte)
│   │   │   ├── ArticleEnVente.kt    # + EquipementReference ; + toujoursDisponible, estConsommable()
│   │   │   ├── Employe.kt
│   │   │   ├── RoleEmploye.kt
│   │   │   ├── StandingBoutique.kt  # MODESTE/CORRECT/PROSPERE/LUXUEUX
│   │   │   ├── TypeBoutique.kt      # MARCHAND/AUBERGE/TEMPLE/GUILDE_RECRUTEMENT/ECURIE/BANQUE/GUILDE_MAGES
│   │   │   └── Service.kt           # service rendu par une boutique de type != MARCHAND
│   │   └── usecase/
│   │       ├── CreerBoutiqueUseCase.kt
│   │       ├── ModifierBoutiqueUseCase.kt
│   │       ├── EffectuerVisiteBoutiqueUseCase.kt  # "nouvelle visite" : rotation stock/services + reset argent
│   │       ├── GenererStandingAleatoireUseCase.kt
│   │       ├── GenererInventaireUseCase.kt        # tirage pondéré (consommables favorisés)
│   │       ├── GenererServicesUseCase.kt          # catalogue de services par type/standing
│   │       ├── GenererEmployesUseCase.kt
│   │       ├── CalculerPrixArticleUseCase.kt
│   │       ├── GenererNomBoutiqueUseCase.kt
│   │       ├── GenererNomMarchandUseCase.kt
│   │       ├── NomsFantastiques.kt          # pool de prénoms partagé (employés + marchand)
│   │       ├── ListerEquipementsDisponiblesUseCase.kt
│   │       ├── DeterminerBudgetAchatUseCase.kt    # aussi utilisé pour argentDisponibleEnPo
│   │       ├── EchantillonnagePondereUtil.kt      # échantillonnage pondéré sans remise (Efraimidis-Spirakis)
│   │       ├── ApprovisionnerBoutiqueUseCase.kt  # + data class FiltresApprovisionnement
│   │       └── EquipementSourcePort.kt      # port implémenté par SrdEquipementAdapter (data/)
│   ├── data/
│   │   ├── BoutiqueRepository.kt    # interface + BoutiqueRepositoryImpl
│   │   ├── SrdEquipementAdapter.kt  # adapte SrdRepository.loadEquipmentList au port domaine
│   │   ├── local/
│   │   │   ├── BoutiqueEntities.kt  # BoutiqueEntity, ArticleEnVenteEntity, EmployeEntity, ServiceEntity
│   │   │   └── BoutiqueDao.kt       # + BoutiqueAvecDetails (relation Room), remplacerBoutiqueComplete
│   │   └── mapper/
│   │       └── BoutiqueMapper.kt    # Entity <-> Domain
│   ├── presentation/
│   │   ├── BoutiqueViewModel.kt         # écran liste
│   │   └── BoutiqueDetailViewModel.kt   # écran détail (une boutique)
│   └── ui/
│       ├── BoutiqueListScreen.kt
│       ├── BoutiqueFormDialog.kt        # création ET édition (même dialog) ; + sélecteur type
│       ├── BoutiqueDetailScreen.kt      # + carte argent/visite, section services, toggle "permanent"
│       ├── AjouterEmployeDialog.kt
│       ├── AjouterArticleDialog.kt          # ajout d'un objet précis, recherche par nom, + case "toujours disponible"
│       └── ApprovisionnerBoutiqueDialog.kt  # ajout en masse filtré (catégories multiples, prix max, budget)
```

**Fichiers du projet existant modifiés** (pas dans `feature_boutique/`) :
- `ui/screens/mj/MjHomeScreen.kt` — entrée "Boutiques" dans le drawer MJ (`onOpenBoutiques`), + `containerColor = Color.Transparent` sur le Scaffold (bug fond d'écran découvert et corrigé au passage — voir plus bas)
- `ui/navigation/NavGraph.kt` — routes `Boutiques` et `BoutiqueDetail/{boutiqueId}` câblées
- `ui/navigation/Routes.kt` — `data object Boutiques`, `data object BoutiqueDetail`
- `ui/GameState.kt` — appelle `BoutiqueDependencies.init(context)` et `.initEquipementSource(context)` dans `GameState.init()`
- `app/build.gradle.kts` — plugin KSP + dépendances Room (détails dans la section Gradle ci-dessous)
- `gradle.properties` — `android.builtInKotlin=false` + `android.newDsl=false`

## Décisions d'architecture à connaître

- **Pas de Hilt/Koin dans le projet.** `BoutiqueDependencies` (dans `di/`) est un singleton manuel avec `init(context)`, sur le même modèle que `GameState`/`SrdRepository`. Si le projet adopte un jour un vrai framework de DI, seul ce fichier change — les Use Cases et le Repository restent identiques.
- **`EquipementReference`** (domain) est un type allégé, découplé de la vraie classe `EquipmentItem` du module SRD (`EquipmentParser.kt`). `SrdEquipementAdapter` fait le pont : `EquipmentItem.cost` est un texte libre français ("5 po", "10 pa"...), parsé et converti en pièces d'or (1 pp = 10 po, 1 pa = 0,1 po, 1 pc = 0,01 po). Les objets sans coût exploitable (vide, "—", objets de quête) sont exclus du catalogue de la boutique.
- **Édition intelligente du standing** : `ModifierBoutiqueUseCase` ne régénère l'inventaire/les employés QUE si le standing change réellement — renommer une boutique ne fait jamais disparaître le stock ou le personnel existants.
- **Employés et articles n'ont pas d'identifiant propre** (`Employe`, `ArticleEnVente` sont de simples data class sans id) — leur suppression individuelle se fait par **index dans la liste affichée**, pas par égalité d'objet. Attention si un jour on ajoute une fonctionnalité de tri/filtre côté UI : l'index doit rester celui de la liste réellement stockée dans `Boutique`, pas celui d'une liste triée localement.
- **Budget de réapprovisionnement** comparé au **coût de base SRD** (avant marge de standing), pas au prix de vente final — plus lisible et prévisible pour le MJ.
- **Employés purement descriptifs** : aucune stat de combat, décision prise dès le départ.
- **Standing** : choisi manuellement par le MJ OU généré aléatoirement (option "Aléatoire" dans les dialogs), pondération dans `StandingBoutique.poidsTirageAleatoire`.

## Fonctionnalités actuelles

- Créer une boutique (nom, marchand, standing manuel ou aléatoire, **type**) → génère inventaire + employés automatiquement selon le standing (type MARCHAND), ou un catalogue de **services** pour les autres types.
- **Types de boutique** (`TypeBoutique`) : MARCHAND (vend de l'inventaire SRD, comportement historique), AUBERGE, TEMPLE, GUILDE_RECRUTEMENT, ECURIE, BANQUE, GUILDE_MAGES — ces 6 derniers proposent un catalogue de services généré (voir `GenererServicesUseCase`), pas d'inventaire d'objets.
- **Argent disponible** (`argentDisponibleEnPo`) : affiché en évidence sur l'écran de détail, initialisé à la création via `DeterminerBudgetAchatUseCase` (même plage que le budget de réapprovisionnement).
- **Nouvelle visite** (bouton sur l'écran de détail, avec confirmation) : simule le passage du groupe — réinitialise l'argent disponible, renouvelle le stock (MARCHAND) ou régénère les services (autres types). Les articles marqués **"toujours disponibles"** sont conservés lors de la rotation.
- **Génération pondérée** : les consommables (potions, rations, munitions...) apparaissent ~3x plus souvent que le reste dans l'inventaire généré et le réapprovisionnement (heuristique par mots-clés + type "Munition", voir `ArticleEnVente.estConsommable()`).
- Boutons "nom aléatoire" (dé 🎲) sur les champs nom boutique/marchand, à la création et à l'édition.
- Modifier une boutique existante (mêmes champs, + type — changer le standing OU le type régénère inventaire/services/employés).
- Supprimer une boutique (avec confirmation).
- Écran de détail par boutique : liste des employés, et selon le type : inventaire (MARCHAND) ou services (lecture seule, autres types).
- Ajouter un employé manuellement (nom + rôle, avec nom aléatoire).
- Retirer un employé.
- Ajouter un article précis manuellement (recherche par nom dans le catalogue SRD du monde actif, prix suggéré selon le standing mais modifiable, quantité, case "toujours disponible").
- Retirer un article, ou marquer/démarquer "toujours disponible" (icône épingle).
- **Réapprovisionnement en masse** (MARCHAND uniquement) : sélection de plusieurs catégories d'objets (cases à cocher), prix max par objet, nombre d'articles max, budget total (suggéré selon le standing, modifiable) — pioche dans le catalogue SRD en respectant ces contraintes.

## Pas encore fait

- Pas d'édition inline du prix/quantité d'un article déjà en stock (il faut le supprimer et le rajouter).
- Pas d'ajout/suppression manuel de service par le MJ — les services sont entièrement générés (création + chaque visite) à partir du catalogue codé en dur de `GenererServicesUseCase`.
- Style visuel des cartes (`BoutiqueCard` dans `BoutiqueListScreen.kt`) pas harmonisé avec `SheetSurface`/`ForcedDarkPalette` du reste de l'app — TODO explicite dans le code.
- Le seuil de "coût élevé" (500 po, dans `GenererInventaireUseCase`, pour limiter l'accès aux objets chers selon le standing) est arbitraire, à ajuster selon l'équilibrage voulu.
- Les plages de budget par standing (`DeterminerBudgetAchatUseCase`, 20-50 po à 500-2000 po) sont des valeurs de départ, jamais testées en jeu — elles servent maintenant aussi à l'argent disponible en caisse.
- La liste de mots-clés "consommable" (`ArticleEnVente.kt`) est une heuristique à ajuster selon le catalogue SRD réellement utilisé (aucune catégorie "consommable" native dans les données SRD).
- **Migration Room destructive** (`fallbackToDestructiveMigration`, décision explicite) : toute boutique déjà créée sur un appareil est perdue à la prochaine ouverture après cette mise à jour (passage de la base en version 2 pour la table `services_boutique` et les nouvelles colonnes).
- Bug latent corrigé au passage : `BoutiqueDao`/`BoutiqueRepositoryImpl` ne supprimaient jamais les anciennes lignes articles/employés avant réinsertion (PK autoGenerate → `REPLACE` n'avait jamais rien à remplacer, les lignes retirées restaient orphelines en base). Remplacé par `BoutiqueDao.remplacerBoutiqueComplete` (méthode par défaut `@Transaction`) qui supprime puis réinsère articles/employés/services à chaque sauvegarde.

## Historique des galères de build (AGP 9 / KSP) — pour ne pas y retomber

Le projet est sur **AGP 9.2.1** avec Kotlin 2.0.21 déclaré dans `libs.versions.toml`, mais AGP 9 embarque en interne **KGP 2.2.10** (runtime dependency, indépendant de ce qui est déclaré). Ça a causé une cascade de problèmes résolus dans cet ordre :

1. **KSP absent du projet** → ajouté (`id("com.google.devtools.ksp")`).
2. **KSP incompatible avec le "built-in Kotlin" d'AGP 9** (activé par défaut) → `android.builtInKotlin=false` dans `gradle.properties` + `apply(plugin = "org.jetbrains.kotlin.android")` dans `build.gradle.kts` (pas `alias(libs.plugins.kotlin.android)`, qui provoque un conflit de version car AGP a déjà kotlin-android sur le classpath sans version déclarée).
3. **`ClassCastException` sur l'extension `android {}`** (nouveau type `AgpDecorated` incompatible avec le plugin kotlin-android classique) → ajout de `android.newDsl=false` en plus de `builtInKotlin=false`.
4. **Version KSP mal appariée** (`2.0.21-1.0.28` visait Kotlin 2.0.21, mais le KGP réel chargé par AGP est 2.2.10) → `id("com.google.devtools.ksp") version "2.2.10-2.0.2"`.
5. **`IllegalStateException: unexpected jvm signature V`** — bug connu de Room 2.6.1 avec KSP2 sur les fonctions `suspend` retournant `Unit` (exactement le cas de `BoutiqueDao`) → Room monté à 2.8.5.
6. **`Inconsistent JVM-target compatibility`** (Java 11 vs Kotlin 21 par défaut) → `tasks.withType<KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_11) }` dans `build.gradle.kts`, car l'accesseur `kotlin { }` généré par le `plugins{}` block n'est pas disponible (le plugin est appliqué via `apply(plugin=...)`, pas via `alias(...)`).

**Point de vigilance pour l'avenir** : Google a annoncé que l'option `android.builtInKotlin=false` ne sera plus utilisable en AGP 10 (prévu mi-2026). Si le projet monte vers AGP 10, il faudra soit que KSP supporte nativement le built-in Kotlin d'ici là, soit repasser par `kapt` pour Room.

## Bug non lié à Gradle, résolu au passage

Le fond d'écran global de l'app (`MainActivity.kt`, `Image(R.drawable.fond_ecran)` dessinée derrière tout le contenu) n'apparaissait pas sur l'écran MJ : le `Scaffold` de `MjHomeScreen.kt` avait `containerColor = MaterialTheme.colorScheme.background` (opaque), qui recouvrait l'image. Corrigé en `Color.Transparent`. **Si un futur écran ajouté à l'app a le même souci (fond qui ne s'affiche pas), vérifier en premier le `containerColor` de son `Scaffold`.**
