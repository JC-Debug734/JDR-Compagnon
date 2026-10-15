package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeActionBoss
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.network.BossJoueurData
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val RougeBoss = Color(0xFFE53935)
private val BleuGivre = Color(0xFF81D4FA)
private val OrangeAlerte = Color(0xFFFFA726)

/** « ●●○ » : [n] points sur [max]. */
private fun jetons(n: Int, max: Int): String = "●".repeat(n.coerceIn(0, max)) + "○".repeat((max - n).coerceAtLeast(0))

/**
 * Bandeau d'un boss vu par la table (joueurs et MJ) : phase, barre de vie graduée aux seuils de
 * phase (sans PV exacts), actions et résistances légendaires, attaque annoncée, jauge de
 * déséquilibre, points faibles et dernières annonces. À sa chute : coup final et dégâts infligés.
 */
@Composable
internal fun BandeauBoss(boss: BossJoueurData, modifier: Modifier = Modifier, nbAnnonces: Int = 3) {
    val bordure = when {
        boss.vaincu -> ForcedDarkPalette.AccentGold
        boss.prepare -> OrangeAlerte
        else -> RougeBoss
    }
    Card(
        modifier = modifier.fillMaxWidth().border(2.dp, bordure, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IllustrationBoss(boss.image)
            Text("👑 ${boss.nom}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text(boss.titre, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Color.White)
            when {
                boss.vaincu -> Text("🏆 Vaincu !", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                boss.enFuite -> Text("🏃 S'est enfui", fontWeight = FontWeight.Bold, color = Color.White)
                else -> {
                    boss.phaseNom?.let {
                        Text(
                            "Phase ${boss.phase + 1}/${boss.seuils.size + 1} — $it",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ForcedDarkPalette.AccentGold,
                        )
                    }
                    BarreVieBoss(boss.ratioPct, boss.seuils)
                    Text(
                        buildList {
                            if (boss.pointsLegendairesMax > 0) add("⚡ Actions légendaires ${jetons(boss.pointsLegendaires, boss.pointsLegendairesMax)}")
                            if (boss.resistancesMax > 0) add("🛡 Résistances ${jetons(boss.resistances, boss.resistancesMax)}")
                        }.joinToString("   "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                    if (boss.pointsFaiblesRestants > 0) {
                        Text("🎯 ${boss.pointsFaiblesRestants} point(s) faible(s) à détruire", style = MaterialTheme.typography.bodySmall, color = BleuGivre)
                    }
                    if (boss.prepare) {
                        Text(
                            "⚠ Attaque en préparation : frappez-le avant son tour pour l'interrompre, ou dispersez-vous !",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAlerte,
                        )
                    }
                    if (boss.desequilibre) {
                        Text("💥 Déséquilibré jusqu'à son prochain tour", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Déséquilibre", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(84.dp))
                            LinearProgressIndicator(
                                progress = { boss.jaugePct / 100f },
                                modifier = Modifier.weight(1f).height(5.dp),
                                color = if (boss.prepare) OrangeAlerte else BleuGivre,
                            )
                        }
                    }
                }
            }
            if (boss.vaincu && boss.degatsParAuteur.isNotEmpty()) {
                HorizontalDivider()
                boss.coupFinal?.let { Text("🗡 Coup final : $it", fontWeight = FontWeight.Bold, color = Color.White) }
                boss.degatsParAuteur.entries.sortedByDescending { it.value }.forEachIndexed { i, (nom, degats) ->
                    Text((if (i == 0) "🥇 " else "• ") + "$nom : $degats dégâts", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
            boss.annonces.take(nbAnnonces).forEachIndexed { i, annonce ->
                Text(
                    annonce,
                    style = if (i == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (i == 0) Color.White else Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** Barre de vie en segments, un par phase (les seuils de phase servent de graduations). */
@Composable
private fun BarreVieBoss(ratioPct: Int, seuils: List<Int>) {
    val bornes = listOf(100) + seuils.sortedDescending() + 0
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        bornes.zipWithNext().forEach { (haut, bas) ->
            val largeur = (haut - bas).coerceAtLeast(1)
            val rempli = ((ratioPct - bas).toFloat() / largeur).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { rempli },
                modifier = Modifier.weight(largeur.toFloat()).height(10.dp),
                color = RougeBoss,
                trackColor = Color.White.copy(alpha = 0.15f),
            )
        }
    }
}

/**
 * Panneau MJ d'un boss : bandeau de la table, PV exacts, et commandes — actions légendaires
 * (automatiques ou à la main), repaire, phase suivante, fuite.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PanneauBossMj(etat: CombatEnCours, bossId: String, vueTable: BossJoueurData?) {
    val b = etat.boss[bossId] ?: return
    val boss = etat.combattants.firstOrNull { it.id == bossId }
    var legendaire by remember { mutableStateOf(false) }
    var menuRepaire by remember { mutableStateOf(false) }
    var confirmerFuite by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Les textes du boss sont dans l'encadré « À lire » en tête d'écran, pas répétés ici.
        vueTable?.let { BandeauBoss(it, nbAnnonces = 0) }
        if (boss == null || boss.horsCombat) return@Column
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "MJ : ${boss.pv}/${boss.pvMax} PV · CA ${boss.ca} · jauge ${b.jauge}/${b.seuilDesequilibre(boss.pvMax)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Actions légendaires automatiques (IA)", style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                    Switch(checked = b.legendairesAuto, onCheckedChange = { CombatSession.basculerLegendairesAuto(bossId) })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { legendaire = true },
                        enabled = etat.phase == PhaseCombat.EN_COURS && b.points > 0 && !b.desequilibre,
                    ) { Text("⚡ Légendaire") }
                    if (b.definition.actionsRepaire.isNotEmpty()) {
                        Box {
                            OutlinedButton(onClick = { menuRepaire = true }, enabled = etat.demarre) { Text("🏔 Repaire") }
                            DropdownMenu(expanded = menuRepaire, onDismissRequest = { menuRepaire = false }) {
                                DropdownMenuItem(text = { Text("Au hasard") }, onClick = { menuRepaire = false; CombatSession.actionRepaire(bossId) })
                                b.definition.actionsRepaire.forEach { a ->
                                    DropdownMenuItem(text = { Text(a.resume) }, onClick = { menuRepaire = false; CombatSession.actionRepaire(bossId, a.nom) })
                                }
                            }
                        }
                    }
                    if (b.phase + 1 < b.definition.phases.size) {
                        OutlinedButton(onClick = { CombatSession.phaseSuivante(bossId) }) { Text("🔥 Phase suivante") }
                    }
                    OutlinedButton(onClick = { confirmerFuite = true }) { Text("🏃 Fuite") }
                }
                if (!b.repaireActif && b.definition.actionsRepaire.isNotEmpty()) {
                    Text("Le repaire s'éveillera à une phase suivante (ou déclenchez-le à la main).", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }

    if (legendaire && boss != null) {
        ActionLegendaireDialog(etat, bossId, b, onDismiss = { legendaire = false })
    }
    if (confirmerFuite && boss != null) {
        AlertDialog(
            onDismissRequest = { confirmerFuite = false },
            title = { Text("${boss.nom} prend la fuite ?") },
            text = { Text("Il quitte le combat (annoncé à la table) et ne laisse pas de butin.") },
            confirmButton = { TextButton(onClick = { confirmerFuite = false; CombatSession.bossFuit(bossId) }) { Text("Fuir") } },
            dismissButton = { TextButton(onClick = { confirmerFuite = false }) { Text("Annuler") } },
        )
    }
}

/** Choix d'une action légendaire, puis de sa cible pour une action qui n'en vise qu'une. */
@Composable
private fun ActionLegendaireDialog(etat: CombatEnCours, bossId: String, b: EtatBoss, onDismiss: () -> Unit) {
    var choisie by remember { mutableStateOf<ActionBoss?>(null) }
    val boss = etat.combattants.firstOrNull { it.id == bossId } ?: return
    val adversaires = etat.combattants.filter { it.estMonstre != boss.estMonstre && !it.horsCombat && !it.inerte }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(choisie?.let { "${it.nom} : cible" } ?: "Action légendaire (${b.points} point(s))") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val action = choisie
                if (action == null) {
                    b.definition.actionsLegendaires.forEach { a ->
                        val possible = a.cout <= b.points
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = possible) {
                                val uneCible = a.forme == FormeActionBoss.ATTAQUE || (a.forme == FormeActionBoss.SAUVEGARDE_CIBLES && a.nbCibles == 1)
                                if (uneCible) choisie = a else {
                                    CombatSession.actionLegendaire(bossId, a.nom)
                                    onDismiss()
                                }
                            }.padding(vertical = 6.dp)
                        ) {
                            Text(a.resume, fontWeight = FontWeight.Bold, color = if (possible) Color.White else Color.White.copy(alpha = 0.4f))
                            Text(a.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = if (possible) 0.85f else 0.4f))
                        }
                    }
                } else {
                    adversaires.sortedBy { etat.distance(bossId, it.id).metres }.forEach { c ->
                        val distance = etat.distance(bossId, c.id)
                        val aPortee = distance.metres <= action.portee + 0.01
                        TextButton(onClick = {
                            CombatSession.actionLegendaire(bossId, action.nom, c.id)
                            onDismiss()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("${c.nom} — ${distance.label.lowercase()}" + if (aPortee) "" else " (hors de portée)", color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { choisie = null }) { Text("← Autre action") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/**
 * Illustration d'un boss ([chemin] dans les assets, cf. BossDefinition.image), en bandeau aux coins
 * arrondis ; rien si le boss n'en a pas ou si le fichier manque.
 */
@Composable
internal fun IllustrationBoss(chemin: String?, modifier: Modifier = Modifier, hauteur: androidx.compose.ui.unit.Dp = 160.dp) {
    if (chemin.isNullOrBlank()) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val image by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, chemin) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                // Image réduite de moitié : largement assez pour un bandeau, deux fois moins de mémoire.
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                // Chemin absolu : image choisie par le MJ (BossCatalogue.importerImage) ; sinon asset.
                val flux = if (chemin.startsWith("/")) java.io.File(chemin).takeIf { it.isFile }?.inputStream() else context.assets.open(chemin)
                flux?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
            }.getOrNull()
        }
    }
    image?.let {
        androidx.compose.foundation.Image(
            bitmap = it,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(hauteur)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}
