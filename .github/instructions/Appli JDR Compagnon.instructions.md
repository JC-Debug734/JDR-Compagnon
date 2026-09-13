Agis en tant qu'expert développeur Android senior spécialisé en Jetpack Compose et Material 3.

Je développe une application Android d'animation de parties de jeux de rôle (JDR) multi-univers (Heroic Fantasy, Cyberpunk/Sci-Fi, Horreur/Cthulhu, Post-Apocalyptique, Steampunk).

Je souhaite que tu mettes en place l'architecture complète du Design System et du système de thèmes dynamiques (« Caméléon ») pour mon application.

### Spécifications techniques requises :

1. Enums et État de l'Univers :
   - Crée un enum ou sealed class `RPGUniverse` contenant : `HEROIC_FANTASY`, `CYBERPUNK`, `HORROR`, `POST_APO`, `STEAMPUNK`.

2. Couleurs (`Color.kt`) :
   - Socle commun Sombre (Background : `#121218`, Card Surface : `#1F212B`, Border : `#2E3140`).
   - Couleurs d'état globales (Health/Success : `#2ECC71`, Damage/Danger : `#E74C3C`, Mana/Energy : `#3498DB`).
   - Palettes d'accents spécifiques par univers :
     - HEROIC_FANTASY : Primary Gold (`#D4AF37`), Secondary Crimson (`#800020`)
     - CYBERPUNK : Primary Cyan (`#00F0FF`), Secondary Magenta (`#FF0055`)
     - HORROR : Primary Blood Red (`#9A031E`), Secondary Miasma Green (`#2D4A3E`)
     - POST_APO : Primary Amber (`#FB8500`), Secondary Warning Yellow (`#E5A93C`)
     - STEAMPUNK : Primary Bronze (`#B87333`), Secondary Leather (`#4A2E19`)

3. Typographie (`Type.kt`) :
   - Définis les styles Material 3 Typography.
   - Utilise une police Monospace pour les valeurs numériques (points de vie, dés, statistiques) afin d'éviter les saut de layout lors des mises à jour.

4. Thème Compose (`Theme.kt`) :
   - Implémente un wrapper `RPGAppTheme(universe: RPGUniverse, content: @Composable () -> Unit)` qui applique dynamiquement le bon `ColorScheme` Material 3 selon l'univers sélectionné.

5. Composant d'Exemple :
   - Fournis le code d'une carte de fiche de personnage `@Composable CharacterCardPreview` utilisant les couleurs du thème (Jauge de PV, Nom, Classe d'Armure, Bouton d'action) pour valider le rendu du thème.

Livre un code Kotlin propre, modulaire, commenté et prêt à intégrer dans un projet Android Android Studio récents.

