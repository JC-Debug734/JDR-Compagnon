package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.R

/**
 * Complete JDR Compagnon typography scale with all 13 Material 3 type styles.
 *
 * Polices Google Fonts (downloadable, via [GoogleFont.Provider]) :
 * - Titres (display/headline) : Playfair Display — identité "fantasy" affirmée.
 * - Corps de texte (title/body/label) : Inter, avec repli sur Roboto (police
 *   système par défaut d'Android) si le téléchargement échoue.
 */
object Type {

    // Fournisseur Google Fonts (Google Play Services Fonts).
    // Nécessite la dépendance Gradle "androidx.compose.ui:ui-text-google-fonts"
    // et le tableau de certificats res/values/font_certs.xml — voir note en bas de fichier.
    private val fontProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    private val playfairDisplayGoogleFont = GoogleFont("Playfair Display")
    private val interGoogleFont = GoogleFont("Inter")

    /** Police des titres (display, headline). Repli : serif système. */
    private val titleFontFamily = FontFamily(
        Font(googleFont = playfairDisplayGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = playfairDisplayGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = playfairDisplayGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold),
        Font(googleFont = playfairDisplayGoogleFont, fontProvider = fontProvider, weight = FontWeight.Black)
    )

    /** Police du corps de texte (title/body/label). Repli : Roboto système. */
    private val bodyFontFamily = FontFamily(
        Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = interGoogleFont, fontProvider = fontProvider, weight = FontWeight.Bold)
    )


    /**
     * Complete typography with all 13 Material 3 styles
     */
    val JdrTypography = Typography(
        // Display styles - for hero/headline content
        displayLarge = TextStyle(
            fontFamily = titleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = -0.25.sp,
            textAlign = TextAlign.Start
        ),
        displayMedium = TextStyle(
            fontFamily = titleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        displaySmall = TextStyle(
            fontFamily = titleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),

        // Headline styles - for section headers
        headlineLarge = TextStyle(
            fontFamily = titleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        headlineMedium = TextStyle(
            fontFamily = titleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        headlineSmall = TextStyle(
            fontFamily = titleFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),

        // Title styles - for cards, components
        titleLarge = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Start
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
            textAlign = TextAlign.Start
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            textAlign = TextAlign.Start
        ),

        // Body styles - for main content
        bodyLarge = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Start
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            textAlign = TextAlign.Start
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Start
        ),

        // Label styles - for buttons, labels, captions
        labelLarge = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            textAlign = TextAlign.Start
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Start
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFontFamily,
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