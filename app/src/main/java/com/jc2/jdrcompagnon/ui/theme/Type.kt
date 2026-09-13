package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Complete JDR Compagnon typography scale with all 13 Material 3 type styles.
 * Uses Inter font family (via Google Fonts downloadable fonts) for brand consistency.
 * Falls back to system default if custom font unavailable.
 */
object Type {

    // Font family - using Inter as brand font (clean, readable, professional)
    // For production: add google-fonts dependency and use FontFamily(Font(GoogleFont("Inter")))
    // For now: use system default with proper weights
    private val fontFamily = FontFamily.Default

    /**
     * Complete typography with all 13 Material 3 styles
     */
    val JdrTypography = Typography(
        // Display styles - for hero/headline content
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = -0.25.sp,
            textAlign = TextAlign.Start
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),

        // Headline styles - for section headers
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),

        // Title styles - for cards, components
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
            textAlign = TextAlign.Start
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            textAlign = TextAlign.Start
        ),

        // Body styles - for main content
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Start
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            textAlign = TextAlign.Start
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Start
        ),

        // Label styles - for buttons, labels, captions
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            textAlign = TextAlign.Start
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Start
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Start
        )
    )

    // ========================================================================
    // Semantic text style helpers for common use cases
    // ========================================================================

    /** Hero title on landing screens */
    val HeroTitle = JdrTypography.displayMedium.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 4.sp
    )

    /** Section headers */
    val SectionHeader = JdrTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold
    )

    /** Card titles */
    val CardTitle = JdrTypography.titleLarge.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
    )

    /** Card descriptions */
    val CardDescription = JdrTypography.bodyMedium.copy(
        lineHeight = 20.sp
    )

    /** Button text */
    val ButtonText = JdrTypography.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )

    /** Caption/small metadata */
    val Caption = JdrTypography.bodySmall

    /** Overline/category labels */
    val Overline = JdrTypography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )

    /** Metadata lines (theme, rules system) */
    val Metadata = JdrTypography.labelMedium
}