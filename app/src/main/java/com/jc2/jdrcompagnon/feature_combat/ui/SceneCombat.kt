package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.feature_combat.presentation.TypeRecit
import com.jc2.jdrcompagnon.network.CombatJoueurData
import com.jc2.jdrcompagnon.network.RecitData
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Ce que voit un joueur quand ce n'est pas son tour : qui agit, et la dernière chose qui vient de
 * se passer, mise en scène (icône qui frappe ou tremble, bulle de dégâts ou de soins qui monte),
 * au lieu du récit écrit. Chaque nouvel événement remplace le précédent.
 */
@Composable
fun SceneCombat(combat: CombatJoueurData, modifier: Modifier = Modifier) {
    val actif = combat.ordre.firstOrNull { it.id == combat.actifId }
    val prochain = combat.prochainId != null && combat.prochainId == combat.monCombattantId
    // Dernier fait du round en cours (les annonces de tour, déjà dites par le bandeau, sont sautées).
    val dernier = combat.recit.lastOrNull { it.round == combat.round && it.texte.isNotBlank() && typeDe(it) !in setOf(TypeRecit.TOUR, TypeRecit.ROUND) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
        border = if (prochain) BorderStroke(2.dp, ForcedDarkPalette.AccentGold) else null,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Au tour de", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            AnimatedContent(
                targetState = actif?.nom ?: "…",
                transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
                label = "actif",
            ) { nom ->
                Text(
                    nom,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if (actif?.estMonstre == true) Color(0xFFE57373) else ForcedDarkPalette.AccentGold,
                    textAlign = TextAlign.Center,
                )
            }
            if (prochain) {
                Text("⏳ Vous êtes le prochain : préparez votre action !", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold, textAlign = TextAlign.Center)
            }
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = dernier,
                    contentKey = { it?.numero },
                    transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn(tween(250))) togetherWith fadeOut(tween(200)) },
                    label = "evenement",
                ) { r ->
                    if (r == null) {
                        Text("⚔", fontSize = 48.sp, color = Color.White.copy(alpha = 0.35f))
                    } else {
                        EvenementAnime(r)
                    }
                }
            }
        }
    }
}

@Composable
private fun EvenementAnime(r: RecitData) {
    val type = typeDe(r)
    val couleur = couleurRecit(type)
    val secousse = remember(r.numero) { Animatable(0f) }
    val echelle = remember(r.numero) { Animatable(0.5f) }
    val bulle = remember(r.numero) { Animatable(0f) }
    LaunchedEffect(r.numero) {
        echelle.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(r.numero) {
        // Coup porté : l'icône tremble (plus fort sur un critique ou une chute).
        val force = when (type) {
            TypeRecit.CRITIQUE, TypeRecit.MORT -> 18f
            TypeRecit.TOUCHE, TypeRecit.DEGATS, TypeRecit.KO -> 10f
            else -> 0f
        }
        if (force > 0f) listOf(force, -force, force * 0.6f, -force * 0.4f, 0f).forEach { secousse.animateTo(it, tween(70)) }
    }
    LaunchedEffect(r.numero) { if (r.montant != null) bulle.animateTo(1f, tween(1600)) }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                type.icone,
                fontSize = 56.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = secousse.value
                    translationX = secousse.value * 1.5f
                    scaleX = echelle.value
                    scaleY = echelle.value
                },
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = couleur.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, couleur.copy(alpha = 0.6f)),
            ) {
                Text(
                    r.texte,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (type in setOf(TypeRecit.CRITIQUE, TypeRecit.KO, TypeRecit.MORT, TypeRecit.VICTOIRE)) FontWeight.Black else FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        // Bulle de dégâts (rouge) ou de soins (vert) qui s'élève et s'efface.
        r.montant?.takeIf { it > 0 }?.let { n ->
            val soin = type == TypeRecit.SOIN
            Text(
                (if (soin) "+" else "−") + n,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = (if (soin) Color(0xFF81C784) else Color(0xFFFF5252)).copy(alpha = (1f - bulle.value).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 24.dp)
                    .offset(y = (-50 * bulle.value).dp),
            )
        }
    }
}
