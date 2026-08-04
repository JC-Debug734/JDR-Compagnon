# Bug Fix: DiceOverlay Race Condition & Race Condition

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Fix the DiceOverlay race condition bug that causes app crashes on rapid clicks by implementing robust state management and gesture handling.

**Architecture:** Refactor DiceOverlay.kt to use single DiceUiState, simple clickable gestures, separate long press handling via pointerInput, proper haptics, and a unique coroutine sequence with isRolling guard.

**Tech Stack:** Jetpack Compose, Android, Kotlin, MVVM pattern with GameState singleton.

---

### Task 1: Create New Implementation Plan for DiceOverlay

**Objective:** Document the complete fix approach for DiceOverlay.kt with specific changes to resolve race conditions.

**Files:**
- Create: `.hermes/plans/2025-07-29_164237-diceoverlay-race-condition-fix.md`

**Step 1: Write implementation details**
```markdown
## New DiceOverlay Implementation Plan

### State Management
- Use single DiceUiState data class with isRolling and rotationDegrees
- Move uiState out of composable function into remember { mutableStateOf() }
- Implement proper coroutine scope for sequence operations

### Gesture Handling
- Use simple .clickable() for tap gestures
- Use pointerInput { detectTapGestures(onLongPress = { ... }) } for long press
- Remove combinedClickable entirely

### Animation & Haptics
- Replace animateFloatAsState with AnimatedScale/AnimatedRotation
- Use HapticFeedbackType.Light for taps, Medium for long press
- Implement isRolling guard to prevent rapid successive clicks

### Sequence Logic
- Create rollDiceSequence() that checks isRolling guard
- Implement coroutine with: animate rotation -> roll dice -> update state -> delay -> reset
- Use LaunchedEffect only for result auto-hide, not state reset

### Files to Modify
- app/src/main/java/com/jc2/jdrcompagnon/ui/components/DiceOverlay.kt (full refactor)
- Test with existing GameState.kt to ensure compatibility
```

**Expected Result:** Complete plan documenting all changes needed.
```

**Step 2: Verify plan saved**

**Step 3: Execute using subagent-driven-development**

**Step 4: Commit changes**

### Task 2: Extract Updated Configuration

**Objective:** Read the current plan and extract specific implementation details for subagent.

**Files:**
- Read: `.hermes/plans/2025-07-29_164237-diceoverlay-race-condition-fix.md`
- Create: `executor-context.md` with key implementation details

**Step 1: Extract complete code examples from plan**

**Step 2: Create simplified implementation guide**

**Expected Result:** Implementation guide ready for execution.
```