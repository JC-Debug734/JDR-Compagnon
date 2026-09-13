package com.jc2.jdrcompagnon.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens for consistent animation timing and easing throughout the app.
 * Based on Material 3 motion system with custom values for JDR Compagnon.
 */
object Motion {

    // ========================================================================
    // Duration tokens (in milliseconds)
    // ========================================================================

    /** Extra short - 50ms (micro-interactions, ripples) */
    const val DurationExtraShort = 50

    /** Short - 150ms (simple transitions, hover states) */
    const val DurationShort = 150

    /** Standard - 250ms (default transitions, navigation) */
    const val DurationStandard = 250

    /** Emphasized - 350ms (important transitions, modal entries) */
    const val DurationEmphasized = 350

    /** Long - 500ms (complex transitions, page changes) */
    const val DurationLong = 500

    /** Extra long - 700ms (hero entrances, major state changes) */
    const val DurationExtraLong = 700

    // ========================================================================
    // Easing curves (cubic-bezier based)
    // ========================================================================

    /** Standard easing - smooth and natural (cubic-bezier(0.2, 0, 0, 1)) */
    val EasingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized easing - more dramatic (cubic-bezier(0.2, 0, 0, 1)) */
    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Accelerate - for exits (cubic-bezier(0.4, 0, 1, 1)) */
    val EasingAccelerate: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Decelerate - for entrances (cubic-bezier(0, 0, 0.2, 1)) */
    val EasingDecelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    // ========================================================================
    // Pre-defined animation specs
    // ========================================================================

    /** Standard tween for most UI transitions */
    fun <T> TweenStandard(): TweenSpec<T> =
        androidx.compose.animation.core.tween(DurationStandard, easing = EasingStandard)

    /** Emphasized tween for important transitions */
    fun <T> TweenEmphasized(): TweenSpec<T> =
        androidx.compose.animation.core.tween(DurationEmphasized, easing = EasingEmphasized)

    /** Quick tween for micro-interactions */
    fun <T> TweenQuick(): TweenSpec<T> =
        androidx.compose.animation.core.tween(DurationShort, easing = EasingStandard)

    /** Slow tween for complex transitions */
    fun <T> TweenSlow(): TweenSpec<T> =
        androidx.compose.animation.core.tween(DurationLong, easing = EasingStandard)

    /** Entrance animation - decelerate */
    fun <T> TweenEntrance(): TweenSpec<T> =
        androidx.compose.animation.core.tween(DurationEmphasized, easing = EasingDecelerate)

    /** Exit animation - accelerate */
    fun <T> TweenExit(): TweenSpec<T> =
        androidx.compose.animation.core.tween(DurationShort, easing = EasingAccelerate)

    // ========================================================================
    // Stagger delays
    // ========================================================================

    /** Base stagger delay per item */
    const val StaggerDelay = 50

    /** Maximum stagger delay */
    const val StaggerMaxDelay = 300

    // ========================================================================
    // Reduced motion support
    // ========================================================================

    /**
     * Check if user has reduced motion enabled in system settings
     */
    @Composable
    fun isReducedMotionEnabled(): Boolean {
        val context = LocalContext.current
        val scale = context.resources.configuration.fontScale
        return scale > 1.5f
    }

    /**
     * Get animation spec that respects reduced motion preference
     */
    @Composable
    fun <T> animationSpecFor(tweenSpec: TweenSpec<T>): androidx.compose.animation.core.AnimationSpec<T> {
        val reducedMotion = isReducedMotionEnabled()
        return if (reducedMotion) {
            snap()
        } else {
            tweenSpec
        }
    }

    /**
     * Get duration that respects reduced motion preference
     */
    @Composable
    fun durationFor(normalDuration: Int): Int {
        val reducedMotion = isReducedMotionEnabled()
        return if (reducedMotion) 0 else normalDuration
    }
}