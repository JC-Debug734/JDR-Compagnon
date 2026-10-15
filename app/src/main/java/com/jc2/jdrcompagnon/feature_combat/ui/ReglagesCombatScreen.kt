package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.NiveauBannieres
import com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombatStore
import com.jc2.jdrcompagnon.feature_combat.presentation.VisibiliteSante
import com.jc2.jdrcompagnon.ui.AppRole
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private const val CLE_SECTIONS = "reglages_combat"

/**
 * Outil « Combat » : réglages du combat et accès aux outils qui l'entourent (référence des
 * actions, comportements de l'IA, boss, combat en cours). Le MJ règle aussi ce que la table voit
 * et ce que l'application fait seule ; un joueur ne règle que le son et les effets de son appareil.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReglagesCombatScreen(
    onOpenMenu: () -> Unit,
    onOpenReference: () -> Unit,
    onOpenComportements: () -> Unit,
    onOpenBoss: () -> Unit,
    onOpenCombatEnCours: () -> Unit,
) {
    val context = LocalContext.current
    remember { PreferencesCombat.init(context) }
    val r by ReglagesCombatStore.reglages.collectAsState()
    val estMj = GameState.appRole.collectAsState().value == AppRole.MJ
    val combat by CombatSession.etat.collectAsState()
    var confirmerReinit by remember { mutableStateOf(false) }
    val maj: ((ReglagesCombat) -> ReglagesCombat) -> Unit = ReglagesCombatStore::modifier

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("COMBAT", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "outils") {
                CollapsibleSectionCard(CLE_SECTIONS, "Outils") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (estMj && combat != null) Raccourci("⚔ Combat en cours", onOpenCombatEnCours)
                        Raccourci("📖 Référence des actions", onOpenReference)
                        if (estMj) Raccourci("🧠 Comportements IA", onOpenComportements)
                        if (estMj) Raccourci("👑 Boss", onOpenBoss)
                    }
                }
            }

            item(key = "son") {
                CollapsibleSectionCard(CLE_SECTIONS, "🔊 Sons") {
                    Interrupteur("Bruitages du combat", "Coups, critiques, soins, rounds… sur cet appareil", r.sons) { v -> maj { it.copy(sons = v) } }
                    if (r.sons) {
                        Curseur("Volume général", r.volume) { v -> maj { it.copy(volume = v) } }
                        Curseur("Impacts (coups, ratés, chutes)", r.volumeImpacts) { v -> maj { it.copy(volumeImpacts = v) } }
                        Curseur("Magie (soins, états)", r.volumeMagie) { v -> maj { it.copy(volumeMagie = v) } }
                        Curseur("Annonces (rounds, boss, victoire)", r.volumeAnnonces) { v -> maj { it.copy(volumeAnnonces = v) } }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Text("Écouter", style = MaterialTheme.typography.labelLarge, color = ForcedDarkPalette.AccentGold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SonsCombat.Son.entries.forEach { son ->
                                AssistChip(
                                    onClick = { SonsCombat.essayer(son) },
                                    label = { Text(son.libelle) },
                                    colors = AssistChipDefaults.assistChipColors(labelColor = Color.White),
                                )
                            }
                        }
                    }
                }
            }

            if (estMj) item(key = "musique") {
                CollapsibleSectionCard(CLE_SECTIONS, "🎵 Musique") {
                    ChoixPiste("Au début du combat", r.musiqueCombat) { id -> maj { it.copy(musiqueCombat = id) } }
                    ChoixPiste("À l'arrivée d'un boss", r.musiqueBoss) { id -> maj { it.copy(musiqueBoss = id) } }
                    Interrupteur("Reprendre la musique d'avant", "À la fin du combat, la piste jouée avant reprend", r.reprendreMusique) { v -> maj { it.copy(reprendreMusique = v) } }
                    Interrupteur("Baisser la musique pendant les sons forts", "Critique, chute, boss, victoire", r.attenuerMusique) { v -> maj { it.copy(attenuerMusique = v) } }
                }
            }

            item(key = "effets") {
                CollapsibleSectionCard(CLE_SECTIONS, "✨ Effets à l'écran") {
                    Text("Bannières plein écran", color = Color.White)
                    Choix(NiveauBannieres.entries, r.bannieres, { it.label }) { v -> maj { it.copy(bannieres = v) } }
                    Text(
                        when (r.bannieres) {
                            NiveauBannieres.TOUTES -> "Nouveau round, coup critique, ennemi abattu, victoire, « À toi »"
                            NiveauBannieres.ESSENTIELLES -> "Ennemi abattu, victoire et « À toi » seulement"
                            NiveauBannieres.AUCUNE -> "Aucune bannière : le récit suffit"
                        },
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f),
                    )
                    Interrupteur("Vibration à mon tour", "Le téléphone vibre quand c'est à vous de jouer", r.vibrations) { v -> maj { it.copy(vibrations = v) } }
                }
            }

            if (estMj) item(key = "table") {
                CollapsibleSectionCard(CLE_SECTIONS, "👁 Ce que voit la table") {
                    Text("Santé des monstres", color = Color.White)
                    Choix(VisibiliteSante.entries, r.santeMonstres, { it.label }) { v -> maj { it.copy(santeMonstres = v) } }
                    Text(r.santeMonstres.detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    Interrupteur("PV des autres personnages", "Sinon chacun ne voit que ses PV, et l'état des autres", r.pvAllies) { v -> maj { it.copy(pvAllies = v) } }
                    Text(
                        "S'applique aux appareils des joueurs, à la page joueur et à l'écran de la table.",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            if (estMj) item(key = "auto") {
                CollapsibleSectionCard(CLE_SECTIONS, "⚙ Automatisation") {
                    Interrupteur(
                        "Attaques des monstres automatiques",
                        "L'attaque décidée par l'IA s'applique dès son tour (toucher, dégâts, états). Zones et sauvegardes restent à valider.",
                        r.attaquesMonstresAuto,
                    ) { v -> maj { it.copy(attaquesMonstresAuto = v) } }
                    Interrupteur(
                        "Actions légendaires automatiques",
                        "Valeur de départ pour chaque nouveau boss (modifiable ensuite boss par boss)",
                        r.legendairesAuto,
                    ) { v -> maj { it.copy(legendairesAuto = v) } }
                }
            }

            if (estMj) item(key = "affichage") {
                val vueDetaillee by PreferencesCombat.vueDetaillee.collectAsState()
                val recitComplet by PreferencesCombat.recitComplet.collectAsState()
                val journal by PreferencesCombat.journalTechnique.collectAsState()
                CollapsibleSectionCard(CLE_SECTIONS, "🖥 Écran du MJ") {
                    Interrupteur("Vue détaillée", "Boss, récit et ordre du round sous le tour en cours", vueDetaillee) { PreferencesCombat.basculerVueDetaillee() }
                    Interrupteur("Récit complet", "Tout le récit plutôt que les dernières lignes", recitComplet) { PreferencesCombat.basculerRecitComplet() }
                    Interrupteur("Journal technique", "Dés, bonus et DD sous le récit", journal) { PreferencesCombat.basculerJournalTechnique() }
                }
            }

            item(key = "reinit") {
                TextButton(onClick = { confirmerReinit = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rétablir les réglages par défaut", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }

    if (confirmerReinit) {
        AlertDialog(
            onDismissRequest = { confirmerReinit = false },
            title = { Text("Réglages par défaut ?") },
            text = { Text("Sons, musique, effets, visibilité et automatisation reviennent à leurs valeurs d'origine.") },
            confirmButton = { TextButton(onClick = { ReglagesCombatStore.reinitialiser(); confirmerReinit = false }) { Text("Rétablir") } },
            dismissButton = { TextButton(onClick = { confirmerReinit = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Raccourci(texte: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(texte, color = Color.White) }
}

@Composable
private fun Interrupteur(titre: String, detail: String?, valeur: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(titre, color = Color.White)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f)) }
        }
        Switch(checked = valeur, onCheckedChange = onChange)
    }
}

@Composable
private fun Curseur(titre: String, valeur: Float, onChange: (Float) -> Unit) {
    Column {
        Row {
            Text(titre, color = Color.White, modifier = Modifier.weight(1f))
            Text("${(valeur * 100).toInt()} %", color = ForcedDarkPalette.AccentGold)
        }
        Slider(value = valeur, onValueChange = onChange, valueRange = 0f..1f)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Choix(options: List<T>, choisi: T, libelle: (T) -> String, onChange: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o == choisi,
                onClick = { onChange(o) },
                label = { Text(libelle(o)) },
                colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.White),
            )
        }
    }
}

/** Piste de la bibliothèque musicale (intégrées + importées), ou « ne pas changer » (null). */
@Composable
private fun ChoixPiste(titre: String, pisteId: String?, onChange: (String?) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    val pistes = availableLoopTracks
    val nom = pistes.firstOrNull { it.id == pisteId }?.displayName ?: "Ne pas changer"
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(titre, color = Color.White, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { ouvert = true }) {
                Text(nom, color = Color.White)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.width(20.dp))
            }
            DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                DropdownMenuItem(text = { Text("Ne pas changer") }, onClick = { onChange(null); ouvert = false })
                pistes.forEach { p ->
                    DropdownMenuItem(text = { Text(p.displayName) }, onClick = { onChange(p.id); ouvert = false })
                }
            }
        }
    }
}
