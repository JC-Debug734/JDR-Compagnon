package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Responsive breakpoint definitions following Material 3 guidelines.
 * Mobile-first approach with breakpoints at 600dp, 840dp, 1200dp.
 */
object Breakpoints {

    /** Extra small screens (phones in portrait) - < 600dp */
    const val XS = 600

    /** Small screens (tablets in portrait, large phones) - 600dp to 840dp */
    const val SM = 840

    /** Medium screens (tablets in landscape, small desktop) - 840dp to 1200dp */
    const val MD = 1200

    /** Large screens (desktop) - >= 1200dp */
    const val LG = 1600

    /** Maximum content width for readability */
    const val MAX_CONTENT_WIDTH = 600

    /** Breakpoint values for window size class detection */
    val windowSizeClassBreakpoints = intArrayOf(XS, SM, MD, LG)

    /**
     * Get current window width in dp from configuration
     */
    @Composable
    private fun currentWindowWidthDp(): Int {
        val config = LocalConfiguration.current
        return config.screenWidthDp
    }

    /**
     * Check if current window is compact width (< 600dp)
     */
    @Composable
    fun isCompactWidth(): Boolean {
        return currentWindowWidthDp() < XS
    }

    /**
     * Check if current window is medium width (600dp - 839dp)
     */
    @Composable
    fun isMediumWidth(): Boolean {
        val width = currentWindowWidthDp()
        return width >= XS && width < SM
    }

    /**
     * Check if current window is expanded width (>= 840dp)
     */
    @Composable
    fun isExpandedWidth(): Boolean {
        return currentWindowWidthDp() >= SM
    }
}