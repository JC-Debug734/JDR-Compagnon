package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.characterPortraitOptions
import com.jc2.jdrcompagnon.ui.components.FilePortraits
import com.jc2.jdrcompagnon.ui.components.PnjPortraits
import com.jc2.jdrcompagnon.ui.components.rememberCharacterPortraitPainter

/**
 * Briefing plein écran d'un PNJ, ouvert par le MJ en cliquant un lien `#event:[Nom]` dans une
 * scène. Visible uniquement du MJ tant qu'il n'a pas cliqué "Envoyer aux joueurs" (comportement/
 * intentions/objectif sont des champs confidentiels de Character, jamais transmis eux-mêmes —
 * seuls le nom et le portrait sont diffusés).
 */
@Composable
fun PnjBriefingOverlay(
    character: Character,
    onDismiss: () -> Unit,
    discussion: com.jc2.jdrcompagnon.ui.GameState.PnjDiscussion? = null,
    reputationGroupe: Int? = null,
) {
    val context = LocalContext.current
    val portraitResId = remember(character.portrait) {
        characterPortraitOptions.find { it.id == character.portrait }?.resId
    }
    val portraitPainter = rememberCharacterPortraitPainter(character.portrait)
    // Tant que ce briefing est ouvert, les jets des joueurs s'y traitent (voir HostSocialRollOverlay).
    DisposableEffect(Unit) {
        NetworkSessionManager.setDiscussionOpen(true)
        onDispose {
            NetworkSessionManager.setDiscussionOpen(false)
            // Portrait envoyé sur la page d'affichage table : retiré en quittant la discussion
            // (la table revient à l'image de la scène en cours).
            NetworkSessionManager.clearWebMedia()
        }
    }

    // Résultat du dernier envoi : null = pas encore envoyé, -1 = aucune partie hébergée,
    // sinon nombre de joueurs connectés qui ont reçu la discussion.
    var envoi by remember(character.id) { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Hors des barres système (état, navigation) : la fenêtre plein écran passait dessous.
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "💬 Discussion",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Quitter la discussion", tint = Color.White)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (portraitPainter != null) {
                        Image(
                            painter = portraitPainter,
                            contentDescription = null,
                            // Portrait entier, comme côté joueur (pas de rognage en rond).
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        // PNJ créé avant l'assignation automatique d'un portrait (ou personnage
                        // sans portrait valide) : silhouette de repli plutôt que rien du tout.
                        PortraitPlaceholder(size = 110.dp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(character.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    // Démarrage de la discussion : en haut, bien visible, avec un retour clair.
                    Button(
                        onClick = {
                            envoi = NetworkSessionManager.sendPnjBriefingToAll(character)
                            if (PnjPortraits.isPnjPortrait(character.portrait)) {
                                val path = PnjPortraits.assetPath(character.portrait)
                                NetworkSessionManager.sendAssetToWeb(path, "${character.name}.${path.substringAfterLast('.')}")
                            } else if (FilePortraits.isFilePortrait(character.portrait)) {
                                val fichier = FilePortraits.file(character.portrait)
                                if (fichier.isFile) {
                                    NetworkSessionManager.sendMediaToWeb(Uri.fromFile(fichier), "${character.name}.${fichier.extension}")
                                }
                            } else if (portraitResId != null) {
                                val portraitUri = Uri.parse("android.resource://${context.packageName}/$portraitResId")
                                // La page d'affichage table détecte une image via l'extension du
                                // nom de fichier (voir MjWebServer.isImage) : sans elle, character.name
                                // seul (ex. "Gobelin") serait servi comme document générique, pas
                                // affiché comme photo.
                                NetworkSessionManager.sendMediaToWeb(portraitUri, "${character.name}.png")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (envoi != null && envoi!! > 0) "Renvoyer aux joueurs" else "Envoyer aux joueurs")
                    }
                    envoi?.let { n ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            when {
                                n < 0 -> "Aucune partie hébergée : lancez la partie (Connexion) pour que les joueurs reçoivent la discussion."
                                n == 0 -> "Aucun joueur connecté pour l'instant."
                                else -> "✔ Discussion ouverte chez $n joueur(s) : leurs jets arrivent ci-dessous."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (n > 0) Color(0xFF7BC67B) else MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Jets de compétence sociale demandés par les joueurs (après « Envoyer aux joueurs »).
                val requests by NetworkSessionManager.socialRequests.collectAsState()
                val notices by NetworkSessionManager.discussionNotices.collectAsState()
                com.jc2.jdrcompagnon.ui.components.DiscussionNoticesPanel(notices, Modifier.padding(vertical = 6.dp))
                com.jc2.jdrcompagnon.ui.components.SocialRequestsPanel(requests, Modifier.padding(vertical = 6.dp))

                // Comportement, personnalité, secrets (ou personnalité improvisée), MJ uniquement.
                InfosComportementalesPnj(character)

                // Aide de jeu de la discussion (confidentielle, jamais envoyée aux joueurs).
                DiscussionAideDeJeu(discussion = discussion, reputationGroupe = reputationGroupe)

                Spacer(modifier = Modifier.height(12.dp))
                // Quitter : le MJ sort de l'écran, la discussion continue chez les joueurs (leurs
                // jets arrivent via HostSocialRollOverlay). Fermer : elle se termine pour tous.
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("🚪 Quitter la discussion", color = Color.White)
                }
                // Visible dès qu'une partie est hébergée : la discussion a pu être envoyée lors d'une
                // ouverture précédente de ce briefing.
                val role by NetworkSessionManager.role.collectAsState()
                if (role == com.jc2.jdrcompagnon.network.SessionRole.HOST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            NetworkSessionManager.closeDiscussionForAll()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("✖ Fermer la discussion pour tous", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** Silhouette générique affichée quand un PNJ n'a pas (encore) de portrait assigné. */
@Composable
internal fun PortraitPlaceholder(size: androidx.compose.ui.unit.Dp = 140.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
