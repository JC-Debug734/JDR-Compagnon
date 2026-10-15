package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Terrain
import com.jc2.jdrcompagnon.feature_combat.domain.model.formatMetres
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Un combattant posé sur le terrain. */
data class PionTerrain(
    val id: String,
    val nom: String,
    val estMonstre: Boolean,
    val zone: Int,
    val horsCombat: Boolean = false,
    val actif: Boolean = false,
    val moi: Boolean = false,
)

private val CouleurGroupe = Color(0xFF64B5F6)
private val CouleurEnnemi = Color(0xFFE57373)

/**
 * Le terrain du combat : les 4 zones de Terrain en bandes, les ennemis en haut, le groupe en bas.
 * Même zone qu'un adversaire = au contact ; 9 m par bande d'écart.
 *
 * - [repereZone] : zone d'où se lisent les distances (« ici », « 9 m »…) ; [pasNormal] / [pasMax]
 *   colorent les bandes atteignables en marchant / en se précipitant.
 * - [zoneChoisie] : bande mise en évidence (destination choisie) ; [pionChoisi] : pion sélectionné.
 * - [onZone] / [onPion] : null = terrain en lecture seule.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerrainCombat(
    pions: List<PionTerrain>,
    modifier: Modifier = Modifier,
    repereZone: Int? = null,
    pasNormal: Int? = null,
    pasMax: Int? = null,
    zoneChoisie: Int? = null,
    pionChoisi: String? = null,
    onZone: ((Int) -> Unit)? = null,
    onPion: ((PionTerrain) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (Terrain.derniere downTo 0).forEach { zone ->
            val ecart = repereZone?.let { kotlin.math.abs(it - zone) }
            val atteinte = when {
                ecart == null || pasNormal == null -> null
                ecart == 0 -> "ici"
                ecart <= pasNormal -> formatMetres(ecart * Terrain.PAS_METRES)
                pasMax != null && ecart <= pasMax -> formatMetres(ecart * Terrain.PAS_METRES) + " · se précipiter"
                else -> formatMetres(ecart * Terrain.PAS_METRES) + " · trop loin"
            }
            val accessible = ecart == null || pasMax == null || ecart <= pasMax
            val fond by animateColorAsState(
                when {
                    zone == zoneChoisie -> ForcedDarkPalette.AccentGold.copy(alpha = 0.30f)
                    zone >= 2 -> CouleurEnnemi.copy(alpha = 0.10f)
                    else -> CouleurGroupe.copy(alpha = 0.10f)
                },
                label = "fondZone",
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = fond,
                contentColor = Color.White,
                border = BorderStroke(
                    if (zone == zoneChoisie) 2.dp else 1.dp,
                    if (zone == zoneChoisie) ForcedDarkPalette.AccentGold else Color.White.copy(alpha = 0.15f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .then(if (onZone != null && accessible) Modifier.clickable { onZone(zone) } else Modifier),
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.width(92.dp)) {
                        Text(Terrain.zonesCourtes[zone], style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        atteinte?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (accessible) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.4f),
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        pions.filter { it.zone == zone }.sortedBy { it.horsCombat }.forEach { p -> Pion(p, p.id == pionChoisi, onPion) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Pion(p: PionTerrain, choisi: Boolean, onPion: ((PionTerrain) -> Unit)?) {
    val camp = if (p.estMonstre) CouleurEnnemi else CouleurGroupe
    Surface(
        shape = RoundedCornerShape(50),
        color = camp.copy(alpha = if (p.horsCombat) 0.12f else if (choisi) 0.65f else 0.30f),
        contentColor = Color.White,
        border = when {
            choisi -> BorderStroke(2.dp, Color.White)
            p.actif -> BorderStroke(2.dp, ForcedDarkPalette.AccentGold)
            p.moi -> BorderStroke(1.dp, Color.White)
            else -> null
        },
        modifier = if (onPion != null) Modifier.clickable { onPion(p) } else Modifier,
    ) {
        Text(
            (if (p.horsCombat) "✝ " else if (p.actif) "▶ " else "") + p.nom + if (p.moi) " (vous)" else "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (p.actif || p.moi) FontWeight.Bold else FontWeight.Normal,
            color = Color.White.copy(alpha = if (p.horsCombat) 0.5f else 1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
