package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Coquille commune des menus latéraux MJ et Joueur : même en-tête "MENU",
 * même palette (ForcedDarkPalette, celle de la barre du bas), même
 * comportement de défilement, même item "Changer de rôle" en bas. Chaque
 * rôle fournit uniquement son propre contenu via [content] — c'est la
 * seule chose qui diffère entre les deux.
 *
 * "Changer de rôle" ne navigue pas directement (ce composant n'a pas accès
 * au NavController) : il se contente de positionner
 * GameState.requestRoleChange(), sur le même principe que l'overlay du dé
 * (GameState.setDiceOverlayVisible). C'est JdrNavGraph qui observe ce flag
 * et effectue la navigation vers l'écran de choix de rôle.
 */
@Composable
fun AppDrawer(
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = ForcedDarkPalette.Surface,
        drawerContentColor = ForcedDarkPalette.Content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "MENU",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ForcedDarkPalette.AccentGold,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                )
            }
            content()

            HorizontalDivider(color = ForcedDarkPalette.Indicator)
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.SwapHoriz, null, tint = ForcedDarkPalette.Content) },
                label = { Text("Changer de rôle", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = { GameState.requestRoleChange() },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content,
                ),
            )
        }
    }
}