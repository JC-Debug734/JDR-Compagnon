# JDR Compagnon - Design Audit & Professional Improvement Specification

**Date:** 2026-08-07  
**Project Type:** Jetpack Compose Android App  
**Scope:** Entry/Onboarding Screens (WorldSelectionScreen, RoleSelectionScreen) + Theme System

---

## 1. CURRENT STATE ANALYSIS

### 1.1 Entry Screens ("Landing Pages")

#### WorldSelectionScreen (`app/src/main/java/.../world/WorldSelectionScreen.kt`)
**Purpose:** First-launch world selection (D&D / Naheulbeuk)

| Aspect | Current Implementation |
|--------|----------------------|
| **Layout** | Scaffold with CenterAlignedTopAppBar + LazyColumn of 2 cards |
| **Animation** | Staggered AnimatedVisibility (fadeIn + slideInVertically) per card |
| **Cards** | ModernWorldCard with radial gradient background, icon surface, selection state |
| **Typography** | MaterialTheme.typography (titleLarge, bodyMedium, labelSmall) |
| **Colors** | World-specific primary/secondary colors (DndRed/DndGold, NaheulGreen/NaheulAmber) |
| **Interaction** | Tap = select world + navigate; Long press haptic |
| **Visual Effects** | Radial gradient accent, tonalElevation, border stroke on selection |

**Strengths:**
- Beautiful world-specific theming
- Smooth staggered entrance animations
- Clear selection feedback (border, elevation, "ACTIF" badge)
- Haptic feedback on interaction

**Weaknesses:**
- No clear visual hierarchy beyond card order
- Missing empty state / onboarding copy (removed per recent commit)
- Title "MULTIVERS" is generic when no world selected
- Limited responsive considerations (single column)

#### RoleSelectionScreen (`app/src/main/java/.../RoleSelectionScreen.kt`)
**Purpose:** Role selection (Maître du Jeu / Joueur) + context switching

| Aspect | Current Implementation |
|--------|----------------------|
| **Layout** | Box with vertical gradient background + centered Column |
| **Animation** | Staggered AnimatedVisibility for title and role cards |
| **Typography** | displayMedium with gradient brush + titleLarge for cards |
| **Colors** | Primary for MJ, Secondary for Joueur, surface for context card |
| **Interaction** | Tap cards for navigation; context card navigates to WorldSelection |
| **Visual Effects** | Horizontal gradient on title, bordered surfaces |

**Strengths:**
- Clean centered layout
- Distinct role colors (primary/secondary)
- Context indicator card shows current world
- Animated entrance

**Weaknesses:**
- Title gradient depends on theme colors (may have contrast issues)
- No description/context for roles (removed per recent commit)
- Context card is visually similar to role cards but different behavior
- Fixed spacers (64dp) not responsive

### 1.2 Theme System Analysis

#### Color.kt
```kotlin
// Three distinct palettes:
1. Default (Mystic): Purple (#8E5AFF) + Cyan (#00E5FF)
2. D&D: Red (#E53935) + Gold (#FFC107)  
3. Naheulbeuk: Green (#4CAF50) + Amber (#FFB300)
```

**Strengths:**
- World-specific theming is unique and well-implemented
- Dark-only color schemes with proper on-colors
- Semantic color roles (primary, secondary, background, surface)

**Weaknesses:**
- No light theme support (forced dark)
- No tertiary color defined for Default scheme
- Missing semantic colors: errorContainer, outlineVariant, surfaceVariant
- Color contrast ratios not verified for accessibility (WCAG AA)

#### Theme.kt
```kotlin
// Dynamic color scheme selection based on GameState.currentWorld.id
val colors = when (currentWorld?.id) {
    "donjon_et_dragon" -> DndDarkColors
    "naheulbeuk" -> NaheulDarkColors
    else -> DefaultDarkColors
}
MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography)
```

**Strengths:**
- Reactive theme switching via StateFlow
- Clean separation of color schemes

**Weaknesses:**
- Uses `MaterialTheme.typography` instead of custom `JdrTypography`
- No shape theming
- No elevation overlays for dark theme depth

#### Type.kt
```kotlin
val JdrTypography = Typography(
    displayLarge = 32sp/Bold, headlineMedium = 24sp/SemiBold,
    titleLarge = 20sp/SemiBold, bodyLarge = 16sp/Normal,
    bodyMedium = 14sp/Normal, labelLarge = 14sp/Medium
)
```

**Strengths:**
- Defined custom typography scale
- Proper line heights and letter spacing

**Weaknesses:**
- **Not actually used** - Theme.kt references `MaterialTheme.typography`
- Uses system default font (no custom font)
- Missing displayMedium, headlineSmall, titleMedium, bodySmall, labelMedium, labelSmall
- No font weight scale definition

---

## 2. PROFESSIONAL DESIGN REQUIREMENTS CHECKLIST

### 2.1 Visual Hierarchy & Layout
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | Clear primary/secondary/tertiary action distinction | ⚠️ Partial (cards only) | HIGH |
| 2 | Consistent spacing scale (4dp base unit) | ✅ Mostly (24dp, 16dp, 8dp) | MEDIUM |
| 3 | Visual grouping with consistent container styles | ⚠️ Inconsistent | HIGH |
| 4 | Proper content max-width for readability | ❌ Missing | HIGH |
| 5 | Responsive breakpoints (mobile/tablet/desktop) | ❌ Mobile only | MEDIUM |

### 2.2 Typography System
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | Complete typography scale (13 styles) | ❌ Only 6 defined | HIGH |
| 2 | Custom brand font (not system default) | ❌ Missing | HIGH |
| 3 | Fluid type scaling (clamp) | ❌ Fixed sp | MEDIUM |
| 4 | Line height ratios (1.2-1.6) | ✅ Good | LOW |
| 5 | Letter spacing for display/headline | ✅ Defined | LOW |

### 2.3 Color & Contrast
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | WCAG AA contrast (4.5:1 text, 3:1 UI) | ❌ Unverified | CRITICAL |
| 2 | Light theme support | ❌ Missing | HIGH |
| 3 | Semantic color roles complete | ⚠️ Partial | HIGH |
| 4 | Color-blind safe palettes | ❌ Unverified | MEDIUM |
| 5 | State colors (hover, pressed, focus, disabled) | ❌ Missing | HIGH |

### 2.4 Responsive & Adaptive
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | Mobile-first breakpoints (320dp, 600dp, 840dp) | ❌ Missing | HIGH |
| 2 | Landscape/portrait adaptations | ❌ Missing | MEDIUM |
| 3 | Foldable/dual-screen support | ❌ Missing | LOW |
| 4 | WindowInsets handling (system bars, IME) | ⚠️ Partial (statusBarsPadding) | MEDIUM |

### 2.5 Accessibility
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | Semantic content descriptions | ✅ Good | LOW |
| 2 | Touch target minimum 48dp | ✅ Cards meet | LOW |
| 3 | Focus order & traversal | ⚠️ Not verified | HIGH |
| 4 | Screen reader labels | ✅ Good | LOW |
| 5 | Reduced motion support | ❌ Missing | MEDIUM |
| 6 | Dynamic font scaling (sp) | ✅ Using sp | LOW |

### 2.6 Motion & Animation
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | Consistent easing curves | ✅ tween() | LOW |
| 2 | Staggered entrance patterns | ✅ Good | LOW |
| 3 | Micro-interactions (press, hover) | ⚠️ Limited | MEDIUM |
| 4 | Loading/skeleton states | ❌ Missing | MEDIUM |
| 5 | Reduced motion preference | ❌ Missing | MEDIUM |

### 2.7 Performance
| # | Requirement | Current Status | Priority |
|---|-------------|----------------|----------|
| 1 | Image/vector optimization | ✅ Vector icons | LOW |
| 2 | Recomposition minimization | ⚠️ Some issues (DiceOverlay) | HIGH |
| 3. | Lazy lists for long content | ✅ LazyColumn | LOW |
| 4 | Baseline profiles | ❌ Missing | MEDIUM |

---

## 3. BEFORE/AFTER WIREFRAME DESCRIPTIONS

### 3.1 WorldSelectionScreen

#### BEFORE (Current)
```
┌─────────────────────────────────────────────────────┐
│                    MULTIVERS                        │  ← CenterAlignedTopAppBar
├─────────────────────────────────────────────────────┤
│                                                     │
│   [Animated Card 1: Donjon & Dragon]                │
│   ┌─────────────────────────────────────────────┐  │
│   │  ●  DONJON ET DRAGON                        │  │
│   │    Médieval-fantastique classique...        │  │
│   │                                    [ACTIF]  │  │
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   [Animated Card 2: Naheulbeuk]                    │
│   ┌─────────────────────────────────────────────┐  │
│   │  ▲  NAHEULBEUK                              │  │
│   │    Humour, chaos et aventures...            │  │
│   └─────────────────────────────────────────────┘  │
│                                                     │
└─────────────────────────────────────────────────────┘
```
- Generic "MULTIVERS" title when no selection
- No descriptive copy/guidance
- Fixed 24dp horizontal padding
- No max-width constraint on cards

#### AFTER (Professional)
```
┌─────────────────────────────────────────────────────┐
│  ←              CHOISISSEZ VOTRE MONDE              │  ← Responsive TopAppBar
├─────────────────────────────────────────────────────┤
│                                                     │
│   "Sélectionnez l'univers qui accueillera          │  ← Hero copy (bodyLarge)
│    vos aventures. Votre choix définit la          │
│    palette de couleurs et les règles disponibles." │
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │ 🛡️  DONJON ET DRAGON        [SÉLECTIONNÉ]   │  │  ← Card with clear
│   │     Médieval-fantastique classique.        │  │     primary action
│   │     Dragons, donjons et épopées légendaires.│  │
│   │     ─────────────────────────────────────   │  │
│   │     Thème : Rouge & Or  |  Règles : D&D 5e  │  │
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │ 🏞️  NAHEULBEUK                              │  │
│   │     Humour, chaos et aventures déjantées    │  │
│   │     dans un donjon pas comme les autres.    │  │
│   │     ─────────────────────────────────────   │  │
│   │     Thème : Vert & Ambre |  Règles : Libre  │  │
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   [Continuer →]                                    │  ← Primary CTA (enabled when selected)
└─────────────────────────────────────────────────────┘
```

**Key Improvements:**
- Hero copy explains the choice
- Cards show metadata (theme, rules system)
- Explicit primary CTA button
- Max-width container (600dp) for readability
- Responsive: 2-column grid on tablet (>600dp)

---

### 3.2 RoleSelectionScreen

#### BEFORE (Current)
```
┌─────────────────────────────────────────────────────┐
│                                                     │
│         JDR COMPAGNON                               │  ← Gradient title
│                                                     │
│                    [64dp spacer]                    │
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │           MAÎTRE DU JEU                     │  │  ← Primary card
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │           JOUEUR                            │  │  ← Secondary card
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │  Univers actuel: Donjon et Dragon           │  │  ← Context card
│   │  Cliquez ici pour sélectionner...           │  │
│   └─────────────────────────────────────────────┘  │
│                                                     │
└─────────────────────────────────────────────────────┘
```
- No role descriptions (removed)
- Context card looks like role card but behaves differently
- Fixed spacers not responsive
- Title gradient may fail contrast

#### AFTER (Professional)
```
┌─────────────────────────────────────────────────────┐
│  ←  JDR COMPAGNON                    [🌐 Monde ▼]   │  ← TopAppBar with world switcher
├─────────────────────────────────────────────────────┤
│                                                     │
│   "Comment souhaitez-vous jouer ?"                 │  ← Section title
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │  🎭  MAÎTRE DU JEU                          │  │
│   │     Créez et gérez des scénarios,           │  │
│   │     menez l'histoire, contrôlez le monde.   │  │
│   │     ─────────────────────────────────────   │  │
│   │     Outils: Scénarios, Groupes, Bestiaire,  │  │
│   │     Notes, Musique d'ambiance               │  │
│   │     [COMMENCER EN TANT QUE MJ]  →           │  │  ← Explicit CTA
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   ┌─────────────────────────────────────────────┐  │
│   │  ⚔️  JOUEUR                                  │  │
│   │     Incarnez un héros, prenez des           │  │
│   │     décisions, vivez l'aventure.            │  │
│   │     ─────────────────────────────────────   │  │
│   │     Fonctions: Fiche perso, Dés, Inventaire,│  │
│   │     Journal, Progression                    │  │
│   │     [REJOINDRE EN TANT QUE JOUEUR] →        │  │
│   └─────────────────────────────────────────────┘  │
│                                                     │
│   ────────────────────────────────────────────────  │
│   Univers actif: Donjon et Dragon (D&D 5e)         │  ← Read-only context
│   [Changer d'univers]                              │  ← Secondary action
└─────────────────────────────────────────────────────┘
```

**Key Improvements:**
- TopAppBar with world switcher dropdown
- Role cards include feature lists + explicit CTAs
- Context separated visually from role selection
- Responsive: side-by-side cards on tablet
- Clear visual distinction between primary actions and context

---

## 4. ACCEPTANCE CRITERIA

### 4.1 Visual Hierarchy & Layout
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| VH-01 | Primary CTA visually distinct (filled button, primary color) | Visual inspection + design token audit |
| VH-02 | Secondary actions use outlined/tonal style | Visual inspection |
| VH-03 | Consistent 4dp spacing scale throughout | Token audit (8, 12, 16, 20, 24, 32, 40, 48, 64) |
| VH-04 | Content max-width 600dp on mobile, centered | Layout inspector |
| VH-05 | Cards use consistent container style (elevation, border, shape) | Component audit |

### 4.2 Typography
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| TY-01 | Custom font family applied (e.g., Inter, Roboto Flex) | Theme audit |
| TY-02 | All 13 Material 3 type styles defined in JdrTypography | Code review (Type.kt) |
| TY-03 | JdrTypography actually used in Theme.kt | Code review (Theme.kt) |
| TY-04 | Display styles use tighter line height (1.1-1.2) | Token audit |
| TY-05 | Body styles use comfortable line height (1.5-1.6) | Token audit |

### 4.3 Color & Contrast
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| CO-01 | All text meets WCAG AA (4.5:1) on background | Automated contrast audit (axe, Accessibility Scanner) |
| CO-02 | Interactive elements meet WCAG AA (3:1) | Automated contrast audit |
| CO-03 | Light theme color schemes defined for all 3 worlds | Code review (Color.kt + Theme.kt) |
| CO-04 | Semantic colors complete (error, outline, surfaceVariant, etc.) | Token audit |
| CO-05 | Focus/pressed/hover/disabled states defined | Component audit |

### 4.4 Responsive
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| RS-01 | Breakpoints at 320dp, 600dp, 840dp implemented | Layout inspector at various widths |
| RS-02 | WorldSelection: 1-col <600dp, 2-col ≥600dp | Layout inspector |
| RS-03 | RoleSelection: stacked <600dp, side-by-side ≥600dp | Layout inspector |
| RS-04 | Proper WindowInsets handling (ime, system bars) | Device testing |
| RS-05 | Landscape layouts functional | Device rotation test |

### 4.5 Accessibility
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| AC-01 | All interactive elements have contentDescription | Accessibility Scanner |
| AC-02 | Touch targets ≥48dp | Layout inspector |
| AC-03 | Focus order logical (top-to-bottom, left-to-right) | TalkBack navigation test |
| AC-04 | Reduced motion respected (animations disabled) | System setting test |
| AC-05 | Dynamic font scaling works (up to 200%) | Font size test |

### 4.6 Motion & Animation
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| MO-01 | Consistent easing: standard (0.2s), emphasized (0.3s) | Animation inspector |
| MO-02 | Stagger delay 50-100ms per item | Code review |
| MO-03 | Press ripple on all interactive surfaces | Interaction test |
| MO-04 | Skeleton/placeholder for async content | Code review |
| MO-05 | Reduced motion disables non-essential animation | System setting test |

### 4.7 Performance
| ID | Criterion | Verification Method |
|----|-----------|---------------------|
| PE-01 | No unnecessary recompositions (key, stable keys) | Recomposition counts (Layout Inspector) |
| PE-02 | Lazy lists for all scrolling content | Code review |
| PE-03 | Vector drawables for icons (no raster) | Asset audit |
| PE-04 | Baseline profile generated | Build verification |

---

## 5. IMPLEMENTATION ROADMAP

### Phase 1: Foundation (Week 1-2)
- [ ] Define complete `JdrTypography` with 13 styles + custom font
- [ ] Apply `JdrTypography` in `Theme.kt`
- [ ] Add light theme color schemes for all 3 worlds
- [ ] Complete semantic color roles
- [ ] Verify WCAG AA contrast for all combinations

### Phase 2: Responsive Layout System (Week 2-3)
- [ ] Create `Breakpoints` object (320, 600, 840, 1200)
- [ ] Build `ResponsiveColumn` / `ResponsiveRow` composables
- [ ] Implement window size class detection
- [ ] Refactor WorldSelectionScreen responsive grid
- [ ] Refactor RoleSelectionScreen responsive layout

### Phase 3: Component Library (Week 3-4)
- [ ] Design system components: `PrimaryButton`, `SecondaryButton`, `TertiaryButton`
- [ ] `SelectionCard` with consistent states (default, hover, pressed, selected, disabled)
- [ ] `HeroSection` composable for onboarding copy
- [ ] `SectionHeader` with consistent styling
- [ ] Loading/skeleton variants

### Phase 4: Screen Refactoring (Week 4-5)
- [ ] WorldSelectionScreen: hero copy, metadata cards, explicit CTA
- [ ] RoleSelectionScreen: TopAppBar switcher, feature lists, explicit CTAs
- [ ] Add reduced motion support
- [ ] Add focus management

### Phase 5: Polish & Validation (Week 5-6)
- [ ] Accessibility audit (TalkBack, Switch Access, font scaling)
- [ ] Performance profiling (recomposition, startup)
- [ ] Device matrix testing (phone, tablet, foldable)
- [ ] Baseline profile generation
- [ ] Design token documentation

---

## 6. FILES TO MODIFY

### Core Theme Files
| File | Changes |
|------|---------|
| `Color.kt` | Add light schemes, complete semantic roles |
| `Theme.kt` | Use JdrTypography, add shapes, elevation overlays |
| `Type.kt` | Complete 13 styles, add custom font family |

### Screen Files
| File | Changes |
|------|---------|
| `WorldSelectionScreen.kt` | Hero copy, metadata, responsive grid, explicit CTA |
| `RoleSelectionScreen.kt` | TopAppBar switcher, feature lists, explicit CTAs |

### New Files (Design System)
| File | Purpose |
|------|---------|
| `ui/theme/Breakpoints.kt` | Responsive breakpoint definitions |
| `ui/theme/Shapes.kt` | Corner radius tokens |
| `ui/theme/Motion.kt` | Easing, duration tokens |
| `ui/components/Buttons.kt` | Button component variants |
| `ui/components/Cards.kt` | SelectionCard, HeroCard, etc. |
| `ui/components/Typography.kt` | Text composables with semantic styles |
| `ui/components/Loading.kt` | Skeleton, shimmer components |

---

## 7. METRICS & SUCCESS CRITERIA

| Metric | Target | Measurement |
|--------|--------|-------------|
| WCAG AA Compliance | 100% | axe-core / Accessibility Scanner |
| Recomposition Count (static screens) | < 5 per frame | Layout Inspector |
| Startup Time (cold) | < 800ms | Macrobenchmark |
| Touch Target Compliance | 100% | Layout Inspector |
| Font Scale Support | 200% without truncation | Manual test |
| Reduced Motion | All non-essential anim disabled | System setting test |

---

## 8. RISKS & MITIGATION

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Custom font increases APK size | Medium | High | Use variable font (Roboto Flex) or downloadable fonts |
| Light theme requires extensive color testing | High | High | Automated contrast testing in CI |
| Responsive layouts break existing flows | Medium | Medium | Feature flag rollout, device lab testing |
| Dynamic theme switching causes flicker | Low | Medium | Ensure Theme.kt reads state synchronously |
| Reduced motion breaks animation-dependent UX | Medium | Low | Provide alternative feedback (haptic, color) |

---

**Document Status:** Draft for Review  
**Next Step:** Stakeholder review → Phase 1 kickoff