package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.PortraitPlaceholder
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.components.GlobalMusicIndicator
import com.jc2.jdrcompagnon.ui.components.rememberCharacterPortraitPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoueurHomeScreen(
    currentWorld: WorldState?,
    onChooseCharacter: () -> Unit,
    onCreateCharacter: () -> Unit,
    onBack: () -> Unit,
    onViewCharacter: (Character) -> Unit = {},
    onOpenMusic: () -> Unit = {},
    onSelectWorld: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
) {
    val allCharacters by GameState.characters.collectAsState()
    val selectedCharacterId by GameState.selectedCharacterId.collectAsState()
    val lastCharacter = allCharacters.find { it.id == selectedCharacterId }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("AVENTURIER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        GlobalMusicIndicator(onOpenMusic = onOpenMusic)
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (lastCharacter != null) {
                    Surface(
                        onClick = { onViewCharacter(lastCharacter) },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "DERNIER PERSONNAGE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastCharacter.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${lastCharacter.race} ${lastCharacter.characterClass} • Niv. ${lastCharacter.level}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Ouvrez le menu pour accéder à votre personnage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }

    // Le briefing PNJ est maintenant affiché par PlayerNetworkOverlay (ui/components), monté une
    // fois au niveau du NavGraph — visible quel que soit l'écran, pas seulement l'accueil.
}

/**
 * Briefing PNJ reçu du MJ (événement de scène), affiché plein écran chez le joueur — seuls le nom
 * et le portrait sont transmis (comportement/intentions/objectif restent confidentiels côté MJ,
 * cf. PnjBriefingOverlay).
 */
@Composable
fun PnjBriefingPlayerDialog(name: String, portraitId: String?, onDismiss: () -> Unit) {
    val portraitPainter = rememberCharacterPortraitPainter(portraitId)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (portraitPainter != null) {
                        // Portrait affiché en entier (pas de rognage en rond) : le joueur doit
                        // voir le PNJ tel que le MJ le présente.
                        Image(
                            painter = portraitPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        PortraitPlaceholder(size = 220.dp)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onDismiss) { Text("Fermer") }
                }
            }
        }
    }
}