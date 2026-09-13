package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.ui.graphics.Color

// ========================================================================
// Base Color Tokens (raw values)
// ========================================================================

// Neutral
val DeepBlack = Color(0xFF0B0B0D)
val SurfaceDark = Color(0xFF16161C)
val PureWhite = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFF8F9FA)
val LightGray = Color(0xFFE8EAED)
val MediumGray = Color(0xFFBDC1C6)
val DarkGray = Color(0xFF5F6368)
val DeepGray = Color(0xFF3C4043)

// D&D Palette (Rouge & Or)
val DndRed = Color(0xFFE53935)
val DndRedDark = Color(0xFF7B1113)
val DndRedLight = Color(0xFFFFCDD2)
val DndRedContainer = Color(0xFFFFEBEE)
val DndGold = Color(0xFFFFC107)
val DndGoldDark = Color(0xFFF57F17)
val DndGoldLight = Color(0xFFFFF8E1)
val DndGoldContainer = Color(0xFFFFF3E0)
val DndOnPrimary = Color(0xFFFFFFFF)
val DndOnPrimaryContainer = Color(0xFF7B1113)
val DndOnSecondaryContainer = Color(0xFFF57F17)

// Naheulbeuk Palette (Vert & Ambre/Brun)
val NaheulGreen = Color(0xFF4CAF50)
val NaheulGreenDark = Color(0xFF1B5E20)
val NaheulGreenLight = Color(0xFFC8E6C9)
val NaheulGreenContainer = Color(0xFFE8F5E9)
val NaheulAmber = Color(0xFFFFB300)
val NaheulAmberDark = Color(0xFFE65100)
val NaheulAmberLight = Color(0xFFFFF8E1)
val NaheulAmberContainer = Color(0xFFFFF3E0)
val NaheulBrown = Color(0xFF4E342E)
val NaheulBrownDark = Color(0xFF3E2723)
val NaheulBrownLight = Color(0xFFD7CCC8)
val NaheulBrownContainer = Color(0xFFEFEBE9)
val NaheulOnPrimary = Color(0xFFFFFFFF)
val NaheulOnPrimaryContainer = Color(0xFF1B5E20)
val NaheulOnSecondaryContainer = Color(0xFFE65100)
val NaheulOnTertiaryContainer = Color(0xFF3E2723)

// Default/Mystic Palette (Violet & Or) — thème par défaut de l'application
val MysticPurple = Color(0xFF6C63FF)
val MysticPurpleDark = Color(0xFF6A1B9A)
val MysticPurpleLight = Color(0xFFE1BEE7)
val MysticPurpleContainer = Color(0xFFF3E5F5)
val RadiantCyan = Color(0xFF00E5FF)
val RadiantCyanDark = Color(0xFF0097A7)
val RadiantCyanLight = Color(0xFFB2EBF2)
val RadiantCyanContainer = Color(0xFFE0F7FA)
val MysticOnPrimary = Color(0xFFFFFFFF)
val MysticOnPrimaryContainer = Color(0xFF6A1B9A)
val MysticOnSecondaryContainer = Color(0xFF006064)

// Default palette additions (image de référence "MJ Table")
val MysticGold = Color(0xFFF5B642)
val MysticBackground = Color(0xFF0E1117)
val MysticSurface = Color(0xFF1A1F2B)
val MysticTextPrimary = Color(0xFFEDEDED)
val MysticTextSecondary = Color(0xFFA0A7BB)
val MysticSuccess = Color(0xFF22C55E)
val MysticAlert = Color(0xFFEF4444)

// ========================================================================
// Semantic Color Roles for Accessibility
// ========================================================================

// Error
val ErrorDark = Color(0xFFCF6679)
val ErrorLight = Color(0xFFB3261E)
val ErrorContainerDark = Color(0xFF93000A)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorDark = Color(0xFFFFFFFF)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorContainerDark = Color(0xFFFFFFFF)
val OnErrorContainerLight = Color(0xFF410002)

// Outline
val OutlineDark = Color(0xFF8E9096)
val OutlineLight = Color(0xFF73777F)
val OutlineVariantDark = Color(0xFF8E9096)
val OutlineVariantLight = Color(0xFFCAC4D0)

// Surface variants
val SurfaceVariantDark = Color(0xFF2A2B32)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OnSurfaceVariantLight = Color(0xFF49454F)

// Scrim
val ScrimDark = Color(0xFF000000)
val ScrimLight = Color(0xFF000000)

// Shadow
val ShadowDark = Color(0xFF000000)
val ShadowLight = Color(0xFF000000)

// ========================================================================
// Complete Dark Color Schemes (3 Worlds + Default)
// ========================================================================

/** Default/Mystic Dark Scheme */
val DefaultDarkColors = androidx.compose.material3.darkColorScheme(
    primary = MysticPurple,
    onPrimary = MysticOnPrimary,
    primaryContainer = MysticPurpleContainer,
    onPrimaryContainer = MysticOnPrimaryContainer,
    secondary = MysticGold,
    onSecondary = Color.Black,
    secondaryContainer = NaheulAmberContainer,
    onSecondaryContainer = NaheulOnSecondaryContainer,
    tertiary = MysticSuccess,
    onTertiary = PureWhite,
    tertiaryContainer = NaheulGreenContainer,
    onTertiaryContainer = NaheulOnPrimaryContainer,
    error = MysticAlert,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = MysticBackground,
    onBackground = MysticTextPrimary,
    surface = MysticSurface,
    onSurface = MysticTextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MysticTextSecondary,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    inverseSurface = MysticTextPrimary,
    inverseOnSurface = MysticBackground,
    inversePrimary = MysticPurpleDark
)

/** D&D Dark Scheme */
val DndDarkColors = androidx.compose.material3.darkColorScheme(
    primary = DndRed,
    onPrimary = DndOnPrimary,
    primaryContainer = DndRedDark,
    onPrimaryContainer = DndOnPrimaryContainer,
    secondary = DndGold,
    onSecondary = Color.Black,
    secondaryContainer = DndGoldDark,
    onSecondaryContainer = DndOnSecondaryContainer,
    tertiary = DndRed,
    onTertiary = DndOnPrimary,
    tertiaryContainer = DndRedDark,
    onTertiaryContainer = DndOnPrimaryContainer,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF12141A),
    onBackground = PureWhite,
    surface = Color(0xFF1B1E27),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF262A36),
    onSurfaceVariant = Color(0xFFC2C6D2),
    outline = Color(0xFF3C4150),
    outlineVariant = Color(0xFF3C4150),
    scrim = ScrimDark,
    inverseSurface = Color(0xFFF5E6E6),
    inverseOnSurface = DeepBlack,
    inversePrimary = DndRedDark
)

/** Naheulbeuk Dark Scheme */
val NaheulDarkColors = androidx.compose.material3.darkColorScheme(
    primary = NaheulGreen,
    onPrimary = NaheulOnPrimary,
    primaryContainer = NaheulGreenDark,
    onPrimaryContainer = NaheulOnPrimaryContainer,
    secondary = NaheulAmber,
    onSecondary = Color.Black,
    secondaryContainer = NaheulAmberDark,
    onSecondaryContainer = NaheulOnSecondaryContainer,
    tertiary = NaheulBrown,
    onTertiary = NaheulOnPrimary,
    tertiaryContainer = NaheulBrownDark,
    onTertiaryContainer = NaheulOnTertiaryContainer,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = DeepBlack,
    onBackground = Color(0xFFE6F5E6),
    surface = Color(0xFF121A12),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF1E2A1E),
    onSurfaceVariant = Color(0xFFD1DAD1),
    outline = Color(0xFF8E9096),
    outlineVariant = Color(0xFF8E9096),
    scrim = ScrimDark,
    inverseSurface = Color(0xFFE6F5E6),
    inverseOnSurface = DeepBlack,
    inversePrimary = NaheulGreenDark
)

// ========================================================================
// Complete Light Color Schemes (3 Worlds + Default)
// ========================================================================

/** Default/Mystic Light Scheme */
val DefaultLightColors = androidx.compose.material3.lightColorScheme(
    primary = MysticPurpleDark,
    onPrimary = PureWhite,
    primaryContainer = MysticPurpleLight,
    onPrimaryContainer = MysticOnPrimaryContainer,
    secondary = RadiantCyanDark,
    onSecondary = PureWhite,
    secondaryContainer = RadiantCyanLight,
    onSecondaryContainer = MysticOnSecondaryContainer,
    tertiary = MysticPurpleDark,
    onTertiary = PureWhite,
    tertiaryContainer = MysticPurpleLight,
    onTertiaryContainer = MysticOnPrimaryContainer,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = OffWhite,
    onBackground = DeepBlack,
    surface = PureWhite,
    onSurface = DeepBlack,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = DeepBlack,
    inverseOnSurface = OffWhite,
    inversePrimary = MysticPurple
)

/** D&D Light Scheme */
val DndLightColors = androidx.compose.material3.lightColorScheme(
    primary = DndRedDark,
    onPrimary = PureWhite,
    primaryContainer = DndRedLight,
    onPrimaryContainer = DndOnPrimaryContainer,
    secondary = DndGoldDark,
    onSecondary = PureWhite,
    secondaryContainer = DndGoldLight,
    onSecondaryContainer = DndOnSecondaryContainer,
    tertiary = DndRedDark,
    onTertiary = PureWhite,
    tertiaryContainer = DndRedLight,
    onTertiaryContainer = DndOnPrimaryContainer,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = OffWhite,
    onBackground = DeepBlack,
    surface = PureWhite,
    onSurface = DeepBlack,
    surfaceVariant = Color(0xFFFCE4EC),
    onSurfaceVariant = Color(0xFF4A3A3A),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = DeepBlack,
    inverseOnSurface = OffWhite,
    inversePrimary = DndRed
)

/** Naheulbeuk Light Scheme */
val NaheulLightColors = androidx.compose.material3.lightColorScheme(
    primary = NaheulGreenDark,
    onPrimary = PureWhite,
    primaryContainer = NaheulGreenLight,
    onPrimaryContainer = NaheulOnPrimaryContainer,
    secondary = NaheulAmberDark,
    onSecondary = PureWhite,
    secondaryContainer = NaheulAmberLight,
    onSecondaryContainer = NaheulOnSecondaryContainer,
    tertiary = NaheulBrownDark,
    onTertiary = PureWhite,
    tertiaryContainer = NaheulBrownLight,
    onTertiaryContainer = NaheulOnTertiaryContainer,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = OffWhite,
    onBackground = DeepBlack,
    surface = PureWhite,
    onSurface = DeepBlack,
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF2E4A2E),
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = DeepBlack,
    inverseOnSurface = OffWhite,
    inversePrimary = NaheulGreen
)