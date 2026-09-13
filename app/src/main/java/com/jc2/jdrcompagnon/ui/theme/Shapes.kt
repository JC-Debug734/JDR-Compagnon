package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shape tokens for consistent corner radius throughout the app.
 * Based on Material 3 shape scale with custom values for JDR Compagnon.
 */
object Shapes {

    /** Extra small - 4dp (chips, badges, small components) */
    val ExtraSmall = RoundedCornerShape(4.dp)

    /** Small - 8dp (buttons, cards, text fields) */
    val Small = RoundedCornerShape(8.dp)

    /** Medium - 12dp (standard cards, dialogs, menus) */
    val Medium = RoundedCornerShape(12.dp)

    /** Large - 16dp (large cards, sheets, bottom sheets) */
    val Large = RoundedCornerShape(16.dp)

    /** Extra large - 24dp (hero cards, featured content) */
    val ExtraLarge = RoundedCornerShape(24.dp)

    /** Extra extra large - 28dp (role cards, main action cards) */
    val ExtraExtraLarge = RoundedCornerShape(28.dp)

    /** Maximum - 32dp (world selection cards, primary surfaces) */
    val Max = RoundedCornerShape(32.dp)

    /** Full - 9999dp (pills, FABs, circular elements) */
    val Full = RoundedCornerShape(CornerSize(9999.dp))

    // ========================================================================
    // Semantic shape aliases for clear intent
    // ========================================================================

    /** Primary action buttons */
    val Button = Small

    /** Outlined/tonal buttons */
    val ButtonOutlined = Small

    /** Card containers */
    val Card = Medium

    /** Hero/feature cards */
    val HeroCard = Max

    /** Role selection cards */
    val RoleCard = ExtraExtraLarge

    /** World selection cards */
    val WorldCard = Max

    /** Dialogs and bottom sheets */
    val Dialog = Large

    /** Chips and badges */
    val Chip = ExtraSmall

    /** Text fields */
    val TextField = Small

    /** Navigation elements */
    val Navigation = Medium

    /** Icon containers */
    val IconContainer = Medium
}