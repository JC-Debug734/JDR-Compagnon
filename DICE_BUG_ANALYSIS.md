# Analyse du Bug du Dé - JDRCompagnon

## Symptôme
Le clic sur le FAB du dé (🎲) fait planter l'application.

## Historique des corrections tentées
1. **v1** : `combinedClickable` simple → crash
2. **v2** : `.clickable() + .combinedClickable()` → erreur compilation
3. **v3** : `combinedClickable` avec try/catch haptic → compile mais crash persistant selon user

## Analyse du code actuel (`DiceOverlay.kt` lignes 132-178)

### Problèmes identifiés

| # | Problème | Localisation | Risque |
|---|----------|--------------|--------|
| 1 | `combinedClickable` sans `interactionSource` | L136 | API Compose moderne le requiert, comportement indéfini sans |
| 2 | `isRotating` = `mutableStateOf(false)` modifié dans callback click + `LaunchedEffect` qui le reset | L79, L144, L181-186 | **Race condition** : clic rapide → état incohérent |
| 3 | Animation `animateFloatAsState` pilotée par `isRotating` booléen | L80-84 | Changement brutal 0→720→0 sans garde-fou |
| 4 | `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` | L140, L154 | Type `LongPress` sur un clic court = incohérent, peut throw sur certains devices |
| 5 | `Surface` + `combinedClickable` + animation rotation simultanée | L132-178 | Conflit gesture/animation, Surface récalc layout pendant animation |
| 6 | `LaunchedEffect(isRotating)` reset après 600ms dur | L181-186 | Si user reclique avant 600ms → double reset, état corrompu |

## Cause racine probable
**Race condition sur `isRotating`** :
1. User clic → `isRotating = true` + lance animation 720°
2. `LaunchedEffect` démarre timer 600ms
3. User clic **avant** 600ms (double-clic rapide ou lag) → `isRotating` déjà true
4. `animateFloatAsState` reçoit targetValue 720 (déjà 720) → pas d'animation
5. Timer 600ms expire → `isRotating = false` → animation retour 0°
6. Mais le 2ème clic a déjà appelé `GameState.rollDice()` → état désync

## Solution robuste à implémenter

### Architecture recommandée
```kotlin
// 1. État unique source de vérité
data class DiceUiState(
    val isRolling: Boolean = false,
    val rotationDegrees: Float = 0f
)

// 2. Coroutine unique pour séquence roll
// 3. clickable simple (pas combinedClickable)
// 4. Long press séparé via pointerInput + detectTapGestures
// 5. Haptic: Light / Medium selon action, PAS LongPress
```

### Points clés correction
- ✅ Un seul `MutableState<DiceUiState>` 
- ✅ `clickable` simple pour le tap
- ✅ `pointerInput { detectTapGestures(onLongPress = { ... }) }` pour long press
- ✅ Haptic `HapticFeedbackType.Light` (tap) / `Medium` (long press)
- ✅ Coroutine `launch` pour séquence : rotate → roll → show result → reset
- ✅ `isRolling` guard pour ignorer clics pendant animation

## Plan d'action
1. Refactor complet `DiceOverlay.kt` avec architecture ci-dessus
2. Test build + install
3. Si crash persiste → ajouter logs crash (try/catch global + logcat)
