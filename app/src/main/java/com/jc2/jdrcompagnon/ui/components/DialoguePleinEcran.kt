package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Fenêtre plein écran aux mêmes emplacements qu'un AlertDialog (titre, contenu, boutons), pour
 * les fenêtres longues (montée de niveau...) : plus lisible qu'une petite fenêtre centrée. Le fond
 * passe sous les barres système (barre de navigation du téléphone comprise) tandis que le contenu
 * et les boutons restent au-dessus d'elles ; le contenu occupe toute la hauteur restante (il gère
 * lui-même son défilement).
 */
@Composable
fun DialoguePleinEcran(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    // Hauteur des barres système vue depuis l'écran principal : dans la fenêtre du dialogue,
    // certains téléphones (Samsung, navigation à 3 boutons) ne transmettent pas ces marges, et
    // la rangée de boutons finissait cachée sous la barre de navigation.
    val margesEcran = WindowInsets.systemBars.asPaddingValues()
    val basEcran = margesEcran.calculateBottomPadding()
    val hautEcran = margesEcran.calculateTopPadding()
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // Fenêtre propre au dialogue : barres système transparentes, icônes claires, pour que le fond
        // de la fenêtre passe dessous (sinon barre de navigation blanche sur certains téléphones).
        val vue = androidx.compose.ui.platform.LocalView.current
        val fenetre = (vue.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            fenetre?.let { w ->
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) w.isNavigationBarContrastEnforced = false
                androidx.core.view.WindowCompat.getInsetsController(w, vue).apply {
                    isAppearanceLightNavigationBars = false
                    isAppearanceLightStatusBars = false
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .let { m ->
                        // Complément si la fenêtre du dialogue n'a pas reçu les marges système.
                        val marges = WindowInsets.systemBars.asPaddingValues()
                        m.padding(
                            top = (hautEcran - marges.calculateTopPadding()).coerceAtLeast(0.dp),
                            bottom = (basEcran - marges.calculateBottomPadding()).coerceAtLeast(0.dp),
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall) { title() }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp, color = IconesAsset.LisereDore)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = IconesAsset.LisereDore)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
