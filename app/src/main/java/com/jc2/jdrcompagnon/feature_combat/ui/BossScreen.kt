package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueAnnoncee
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossDefinition
import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.DefensesDegats
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.PhaseBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.PointFaibleBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypesCreature
import com.jc2.jdrcompagnon.feature_combat.domain.model.decomposerFormule
import com.jc2.jdrcompagnon.feature_combat.domain.model.formatMetres
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import java.util.UUID

/**
 * Outil BOSS : les adversaires de fin de scénario, qui se jouent différemment d'un monstre (phases,
 * actions légendaires et de repaire, attaque annoncée, points faibles…, voir Boss.kt). On y
 * consulte, crée et modifie les boss, et on lance directement un combat contre l'un d'eux. Dans un
 * scénario, le lien `#combat:[Nom du boss]` fait de même.
 */
@Composable
fun BossScreen(onOpenMenu: () -> Unit = {}, onOuvrirCombat: () -> Unit = {}) =
    TexteBlancCombat { BossScreenInterne(onOpenMenu, onOuvrirCombat) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BossScreenInterne(onOpenMenu: () -> Unit, onOuvrirCombat: () -> Unit) {
    val tous by BossCatalogue.tous.collectAsState()
    var ficheId by remember { mutableStateOf<String?>(null) }
    // null = éditeur fermé ; id absent de la liste = création.
    var enEdition by remember { mutableStateOf<BossDefinition?>(null) }
    var aLancer by remember { mutableStateOf<BossDefinition?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Boss", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
                actions = {
                    IconButton(onClick = { enEdition = nouveauBoss() }) {
                        Icon(Icons.Default.Add, contentDescription = "Nouveau boss", tint = ForcedDarkPalette.AccentGold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                // Explication repliée par défaut (flèche), état mémorisé.
                CollapsibleSectionCard(ownerKey = "outil-boss", title = "Combats de boss", defaultExpanded = false) {
                    Text(
                        "Un boss tient tête à tout le groupe : actions légendaires après le tour des joueurs, phases selon ses PV, " +
                            "repaire qui agit à la fin de chaque round et punit les attaques ratées, résistances légendaires, attaque annoncée que les joueurs peuvent interrompre, " +
                            "points faibles à détruire. Ses PV et ses actions légendaires s'ajustent au nombre de personnages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                    Text(
                        "Lancer : bouton « Combattre » de sa fiche, lien #combat:[Nom] dans un scénario, ou « Ajouter au combat › Boss » pendant un combat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
            items(tous, key = { it.id }) { def -> CarteBoss(def, onClick = { ficheId = def.id }) }
        }
    }

    ficheId?.let { id ->
        val def = tous.firstOrNull { it.id == id }
        if (def == null) ficheId = null else FicheBossDialog(
            def = def,
            onDismiss = { ficheId = null },
            onModifier = { enEdition = def },
            onDupliquer = { enEdition = def.copy(id = UUID.randomUUID().toString(), nom = "${def.nom} (copie)") },
            onCombattre = { aLancer = def },
        )
    }
    enEdition?.let { def ->
        EditeurBoss(
            initial = def,
            autresNoms = tous.filter { it.id != def.id }.map { it.nom.lowercase() }.toSet(),
            onEnregistrer = { BossCatalogue.enregistrer(it); ficheId = it.id; enEdition = null },
            onDismiss = { enEdition = null },
        )
    }
    aLancer?.let { def ->
        LancerCombatDialog(
            composition = def.nom,
            worldId = GameState.currentWorldId(),
            onDismiss = { aLancer = null },
            onLance = { aLancer = null; ficheId = null; onOuvrirCombat() },
        )
    }
}

private fun nouveauBoss() = BossDefinition(
    id = UUID.randomUUID().toString(),
    nom = "",
    titre = "",
    monstreBase = "",
    presentation = "",
    defaite = "",
    phases = listOf(PhaseBoss("Phase 1", 100, "")),
)

@Composable
private fun CarteBossTranslucide(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

/** « 3 phases · 3 actions légendaires · repaire · 2 points faibles ». */
private fun resumeBoss(def: BossDefinition): String = buildList {
    add("${def.phases.size} phase(s)")
    if (def.pointsLegendaires > 0 && def.actionsLegendaires.isNotEmpty()) add("${def.pointsLegendaires} actions légendaires")
    if (def.resistancesLegendaires > 0) add("${def.resistancesLegendaires} résistances")
    if (def.actionsRepaire.isNotEmpty()) add("repaire")
    if (def.attaqueAnnoncee != null) add("attaque annoncée")
    if (def.pointsFaibles.isNotEmpty()) add("${def.pointsFaibles.size} point(s) faible(s)")
}.joinToString(" · ")

private fun origine(def: BossDefinition): String = when {
    BossCatalogue.estModifie(def.id) -> "Intégré, modifié"
    BossCatalogue.estIntegre(def.id) -> "Intégré"
    else -> "Créé par le MJ"
}

@Composable
private fun CarteBoss(def: BossDefinition, onClick: () -> Unit) {
    CarteBossTranslucide(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("👑 ${def.nom}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(origine(def), style = MaterialTheme.typography.labelSmall, color = ForcedDarkPalette.AccentGold)
        }
        if (def.titre.isNotBlank()) Text(def.titre, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        Text("Base : ${def.monstreBase.ifBlank { "?" }}", style = MaterialTheme.typography.bodySmall)
        Text(resumeBoss(def), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
    }
}

// ── Fiche détaillée ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FicheBossDialog(
    def: BossDefinition,
    onDismiss: () -> Unit,
    onModifier: () -> Unit,
    onDupliquer: () -> Unit,
    onCombattre: () -> Unit,
) {
    val presse = LocalClipboardManager.current
    var confirmerSuppression by remember { mutableStateOf(false) }
    var lienCopie by remember { mutableStateOf(false) }
    val integre = BossCatalogue.estIntegre(def.id)
    PleinEcran(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
            Text("👑 ${def.nom}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            IconButton(onClick = onModifier) { Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = ForcedDarkPalette.AccentGold) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IllustrationBoss(BossCatalogue.imageDe(def), hauteur = 200.dp)
            if (def.titre.isNotBlank()) Text(def.titre, fontStyle = FontStyle.Italic)
            Text("${origine(def)} · base : ${def.monstreBase}", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCombattre) { Text("⚔ Combattre") }
                OutlinedButton(onClick = { presse.setText(AnnotatedString("#combat:[${def.nom}]")); lienCopie = true }) {
                    Text(if (lienCopie) "Lien copié ✓" else "Copier le lien #combat")
                }
            }
            ResumeBaseBoss(def)

            SectionFiche("Mise en scène") {
                if (def.presentation.isNotBlank()) Text("Entrée : ${def.presentation}", style = MaterialTheme.typography.bodySmall)
                if (def.defaite.isNotBlank()) Text("Chute : ${def.defaite}", style = MaterialTheme.typography.bodySmall)
            }
            SectionFiche("Puissance (pour 4 personnages)") {
                Text(
                    "PV ${def.pvPour4?.toString() ?: "de la fiche"} · ${def.pointsLegendaires} points d'action légendaire · " +
                        "${def.resistancesLegendaires} résistances légendaires · déséquilibré à ${def.seuilDesequilibrePct} % de ses PV entre deux tours",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SectionFiche("Phases") {
                def.phases.forEachIndexed { i, p -> Text("${i + 1}. ${resumePhase(p)}", style = MaterialTheme.typography.bodySmall) }
            }
            def.attaqueAnnoncee?.let { a ->
                SectionFiche("Attaque annoncée : ${a.attaqueNom}") {
                    Text("Annonce : ${a.annonce}", style = MaterialTheme.typography.bodySmall)
                    Text("Interrompue : ${a.interruption}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (def.actionsLegendaires.isNotEmpty()) SectionFiche("Actions légendaires") {
                def.actionsLegendaires.forEach { LigneAction(it, legendaire = true) }
            }
            if (def.actionsRepaire.isNotEmpty()) SectionFiche("Actions de repaire") {
                def.actionsRepaire.forEach { LigneAction(it, legendaire = false) }
            }
            if (def.pointsFaibles.isNotEmpty()) SectionFiche("Points faibles") {
                def.pointsFaibles.forEach { p ->
                    Text("🎯 ${p.nom} — CA ${p.ca}, ${p.pv} PV", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
                Text(
                    buildList {
                        if (def.regeneration > 0) add("régénère ${def.regeneration} PV par tour tant qu'il en reste un")
                        if (def.malusCaSansPointsFaibles > 0) add("CA −${def.malusCaSansPointsFaibles} une fois tous détruits")
                    }.joinToString(" ; "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (def.butin.isNotEmpty()) SectionFiche("Butin") {
                Text(def.butin.joinToString(), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDupliquer) { Text("Dupliquer") }
                if (!integre || BossCatalogue.estModifie(def.id)) {
                    OutlinedButton(onClick = { confirmerSuppression = true }) { Text(if (integre) "Réinitialiser" else "Supprimer") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmerSuppression) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text(if (integre) "Réinitialiser ${def.nom} ?" else "Supprimer ${def.nom} ?") },
            text = { Text(if (integre) "Vos modifications sont perdues : le boss revient à sa version d'origine." else "Le boss est supprimé définitivement.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmerSuppression = false
                    BossCatalogue.supprimerOuReinitialiser(def.id)
                    if (!integre) onDismiss()
                }) { Text(if (integre) "Réinitialiser" else "Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmerSuppression = false }) { Text("Annuler") } },
        )
    }
}

/** CA, PV et actions de la fiche de base du bestiaire (ou un avertissement si elle est introuvable). */
@Composable
private fun ResumeBaseBoss(def: BossDefinition) {
    val context = LocalContext.current
    var profil by remember(def.monstreBase) { mutableStateOf<ProfilCombatMonstre?>(null) }
    var charge by remember(def.monstreBase) { mutableStateOf(false) }
    LaunchedEffect(def.monstreBase) {
        profil = def.monstreBase.takeIf { it.isNotBlank() }?.let { nom ->
            SrdRepository.getMonsterByName(context, nom, GameState.currentWorldId())?.let { ProfilCombatMonstre.depuisFiche(it.rawMarkdown) }
        }
        charge = true
    }
    val p = profil
    when {
        !charge -> Unit
        p == null -> Text("⚠ Fiche « ${def.monstreBase} » introuvable dans le bestiaire de ce monde : valeurs par défaut en combat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        else -> SectionFiche("Fiche de base") {
            Text(
                "CA ${p.ca} · PV ${def.pvPour4 ?: p.pvMoyens} pour 4 · vitesse ${formatMetres(p.vitesse)}" +
                    (p.typeCreature?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
            p.actions.attaques.forEach { Text("• ${it.resume}", style = MaterialTheme.typography.bodySmall) }
            if (p.actions.nbAttaquesMultiples > 1) Text("Attaques multiples : ×${p.actions.nbAttaquesMultiples}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionFiche(titre: String, content: @Composable () -> Unit) {
    CarteBossTranslucide {
        Text(titre, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
        content()
    }
}

private fun resumePhase(p: PhaseBoss): String = buildString {
    append(p.nom)
    append(if (p.seuilPv >= 100) " (début)" else " (sous ${p.seuilPv} % PV)")
    p.profil?.let { append(" · ${it.label}") }
    if (p.modificateurCa != 0) append(" · CA ${if (p.modificateurCa > 0) "+" else ""}${p.modificateurCa}")
    if (p.bonusPointsLegendaires != 0) append(" · +${p.bonusPointsLegendaires} pt légendaire")
    if (p.rechargeImmediate) append(" · capacités rechargées")
    if (p.repaireActif) append(" · repaire actif")
    if (p.renforts.isNotEmpty()) append(" · renforts : ${CompositionCombat.formater(p.renforts)}")
    if (p.annonce.isNotBlank()) append(" — « ${p.annonce} »")
}

@Composable
private fun LigneAction(a: ActionBoss, legendaire: Boolean) {
    Column {
        Text(
            (if (legendaire) "⚡ " else "🏔 ") + (if (legendaire) a.resume else a.resume.replace(Regex(""" \(\d+ points\)"""), "")),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            listOfNotNull(
                a.forme.label,
                if (a.forme == FormeActionBoss.SAUVEGARDE_CIBLES) "${a.nbCibles} cible(s)" else null,
                if (a.forme != FormeActionBoss.SAUVEGARDE_TOUS && (legendaire || a.forme == FormeActionBoss.SAUVEGARDE_CONTACT)) "portée ${formatMetres(a.portee)}" else null,
                if (a.repli) "puis s'écarte" else null,
            ).joinToString(" · ") + if (a.description.isNotBlank()) " — ${a.description}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

// ── Éditeur ──

@Composable
private fun PleinEcran(onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TexteBlancCombat {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f), contentColor = Color.White) {
                // Hors des barres système et du clavier : la fenêtre plein écran passait dessous, et
                // le bas de la fiche ou de l'éditeur restait inaccessible.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                        .padding(vertical = 8.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun ChampTexte(label: String, valeur: String, onChange: (String) -> Unit, multiligne: Boolean = false, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = !multiligne,
        minLines = if (multiligne) 2 else 1,
        modifier = modifier,
    )
}

/** Champ numérique ; vide = null. Le texte est gardé tel quel pendant la saisie (« - »…). */
@Composable
private fun ChampEntier(label: String, valeur: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var texte by remember { mutableStateOf(valeur?.toString().orEmpty()) }
    OutlinedTextField(
        value = texte,
        onValueChange = { t ->
            texte = t.filter { it.isDigit() || it == '-' }.take(5)
            onChange(texte.toIntOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun LigneInterrupteur(texte: String, valeur: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(texte, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = valeur, onCheckedChange = onChange)
    }
}

/** Liste d'éléments (phases, actions…) : résumé, modifier, retirer, ajouter. */
@Composable
private fun <T> ListeEditable(
    titre: String,
    elements: List<T>,
    resume: (T) -> String,
    onModifier: (Int) -> Unit,
    onRetirer: (Int) -> Unit,
    onAjouter: () -> Unit,
    aide: String? = null,
) {
    SectionFiche(titre) {
        aide?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)) }
        elements.forEachIndexed { i, e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(resume(e), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).clickable { onModifier(i) }.padding(vertical = 4.dp))
                IconButton(onClick = { onModifier(i) }) { Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { onRetirer(i) }) { Icon(Icons.Default.Delete, contentDescription = "Retirer", modifier = Modifier.size(18.dp)) }
            }
        }
        TextButton(onClick = onAjouter) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Ajouter")
        }
    }
}

private fun <T> List<T>.remplace(i: Int, e: T): List<T> = if (i in indices) toMutableList().apply { this[i] = e } else this + e

/** Élément d'une liste en cours d'édition : index (-1 = ajout) et valeur. */
private data class EnCours<T>(val index: Int, val valeur: T)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditeurBoss(
    initial: BossDefinition,
    autresNoms: Set<String>,
    onEnregistrer: (BossDefinition) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var b by remember { mutableStateOf(initial) }
    var phaseEnCours by remember { mutableStateOf<EnCours<PhaseBoss>?>(null) }
    // Action légendaire (true) ou de repaire (false).
    var actionEnCours by remember { mutableStateOf<Pair<Boolean, EnCours<ActionBoss>>?>(null) }
    var pointFaibleEnCours by remember { mutableStateOf<EnCours<PointFaibleBoss>?>(null) }
    var butinTexte by remember { mutableStateOf(initial.butin.joinToString(", ")) }

    // Bestiaire : suggestions pour la fiche de base, et actions de cette fiche (attaque annoncée).
    var nomsMonstres by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) { nomsMonstres = SrdRepository.loadMonsters(context, GameState.currentWorldId()).map { it.name } }
    var profilBase by remember { mutableStateOf<ProfilCombatMonstre?>(null) }
    LaunchedEffect(b.monstreBase) {
        profilBase = b.monstreBase.takeIf { it.isNotBlank() }?.let { nom ->
            SrdRepository.getMonsterByName(context, nom, GameState.currentWorldId())?.let { ProfilCombatMonstre.depuisFiche(it.rawMarkdown) }
        }
    }

    val nomPris = b.nom.trim().lowercase() in autresNoms
    val erreur = when {
        b.nom.isBlank() -> "Donnez un nom au boss."
        nomPris -> "Un autre boss porte déjà ce nom."
        b.monstreBase.isBlank() -> "Choisissez sa fiche de base dans le bestiaire."
        else -> null
    }

    PleinEcran(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Annuler") }
            Text(if (initial.nom.isBlank()) "Nouveau boss" else "Modifier ${initial.nom}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(enabled = erreur == null, onClick = {
                val phases = b.phases.ifEmpty { listOf(PhaseBoss("Phase 1", 100, "")) }
                    .sortedByDescending { it.seuilPv }
                    .mapIndexed { i, p -> if (i == 0) p.copy(seuilPv = 100) else p }
                onEnregistrer(
                    b.copy(
                        nom = b.nom.trim(),
                        monstreBase = b.monstreBase.trim(),
                        phases = phases,
                        butin = butinTexte.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() },
                    )
                )
            }) { Text("Enregistrer") }
        }
        erreur?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp)) }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionFiche("Identité") {
                ChampTexte("Nom (lien #combat:[Nom])", b.nom, { b = b.copy(nom = it) })
                ChampTexte("Titre", b.titre, { b = b.copy(titre = it) })
                ChampTexte("Fiche de base (bestiaire)", b.monstreBase, { b = b.copy(monstreBase = it) })
                val suggestions = if (b.monstreBase.length >= 2 && nomsMonstres.none { it.equals(b.monstreBase, ignoreCase = true) }) {
                    nomsMonstres.filter { it.contains(b.monstreBase.trim(), ignoreCase = true) }.take(6)
                } else emptyList()
                suggestions.forEach { nom ->
                    Text("→ $nom", color = ForcedDarkPalette.AccentGold, modifier = Modifier.fillMaxWidth().clickable { b = b.copy(monstreBase = nom) }.padding(vertical = 4.dp))
                }
                val p = profilBase
                Text(
                    when {
                        b.monstreBase.isBlank() -> "CA, PV, attaques et sauvegardes viennent de cette fiche."
                        p == null -> "⚠ Fiche introuvable dans le bestiaire de ce monde."
                        else -> "✓ CA ${p.ca}, ${p.pvMoyens} PV · " + p.actions.attaques.joinToString { it.nom }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (b.monstreBase.isNotBlank() && p == null) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.8f),
                )
                ChampTexte("Entrée en scène (lue à la table)", b.presentation, { b = b.copy(presentation = it) }, multiligne = true)
                ChampTexte("Chute (lue à la table)", b.defaite, { b = b.copy(defaite = it) }, multiligne = true)
            }

            SectionFiche("Illustration") {
                val image = BossCatalogue.imageDe(b)
                val choisirImage = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        BossCatalogue.importerImage(context, b.id, uri)?.let { b = b.copy(image = it) }
                            ?: android.widget.Toast.makeText(context, "Impossible de lire cette image", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                if (image != null) IllustrationBoss(image, hauteur = 180.dp)
                else Text("Aucune image.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { choisirImage.launch("image/*") }) {
                        Text(if (image != null) "Changer l'image" else "Choisir une image")
                    }
                    if (image != null) {
                        TextButton(onClick = { b = b.copy(image = BossCatalogue.SANS_IMAGE) }) { Text("Retirer") }
                    }
                    // Boss intégré dont l'image d'origine a été remplacée ou retirée.
                    val origine = BossCatalogue.integres.firstOrNull { it.id == b.id }?.image
                    if (origine != null && b.image != null && b.image != origine) {
                        TextButton(onClick = { b = b.copy(image = null) }) { Text("Image d'origine") }
                    }
                }
                Text(
                    "Affichée sur la fiche, dans le bandeau du boss en combat et sur la page table. Une image choisie sur ce téléphone n'est visible que chez vous et sur la page table.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            SectionFiche("Puissance (prévue pour 4 personnages)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChampEntier("PV (vide = fiche)", b.pvPour4, { b = b.copy(pvPour4 = it?.coerceAtLeast(1)) }, Modifier.weight(1f))
                    ChampEntier("Seuil déséquilibre %", b.seuilDesequilibrePct, { b = b.copy(seuilDesequilibrePct = (it ?: 20).coerceIn(1, 100)) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChampEntier("Points légendaires", b.pointsLegendaires, { b = b.copy(pointsLegendaires = (it ?: 0).coerceIn(0, 6)) }, Modifier.weight(1f))
                    ChampEntier("Résistances légendaires", b.resistancesLegendaires, { b = b.copy(resistancesLegendaires = (it ?: 0).coerceIn(0, 6)) }, Modifier.weight(1f))
                }
                Text(
                    "Le boss est déséquilibré s'il perd ce pourcentage de ses PV entre deux de ses tours (et son attaque annoncée avorte).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            ListeEditable(
                titre = "Phases",
                elements = b.phases,
                resume = ::resumePhase,
                onModifier = { i -> phaseEnCours = EnCours(i, b.phases[i]) },
                onRetirer = { i -> b = b.copy(phases = b.phases.filterIndexed { j, _ -> j != i }) },
                onAjouter = {
                    val seuil = ((b.phases.minOfOrNull { it.seuilPv } ?: 100) / 2).coerceAtLeast(1)
                    phaseEnCours = EnCours(-1, PhaseBoss("Phase ${b.phases.size + 1}", seuil, ""))
                },
                aide = "La première commence avec le combat ; chacune des suivantes quand ses PV passent sous son seuil.",
            )

            SectionFiche("Attaque annoncée") {
                val a = b.attaqueAnnoncee
                LigneInterrupteur("Annoncer une capacité un round à l'avance", a != null) { actif ->
                    b = b.copy(
                        attaqueAnnoncee = if (!actif) null else AttaqueAnnoncee(
                            profilBase?.actions?.attaques?.firstOrNull { it.estSpeciale }?.nom ?: "",
                            "${b.nom.ifBlank { "Le boss" }} se prépare à frapper…",
                            "Sous vos coups, ${b.nom.ifBlank { "le boss" }} vacille : son attaque avorte !",
                        )
                    )
                }
                if (a != null) {
                    val actions = profilBase?.actions?.attaques.orEmpty()
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menu = true }, enabled = actions.isNotEmpty()) {
                            Text(a.attaqueNom.ifBlank { "Choisir l'action de la fiche" })
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            actions.forEach { act ->
                                DropdownMenuItem(text = { Text(act.resume) }, onClick = { menu = false; b = b.copy(attaqueAnnoncee = a.copy(attaqueNom = act.nom)) })
                            }
                        }
                    }
                    if (actions.isEmpty()) ChampTexte("Nom de l'action sur la fiche", a.attaqueNom, { b = b.copy(attaqueAnnoncee = a.copy(attaqueNom = it)) })
                    ChampTexte("Annonce (pendant la déclaration)", a.annonce, { b = b.copy(attaqueAnnoncee = a.copy(annonce = it)) }, multiligne = true)
                    ChampTexte("Si elle est interrompue", a.interruption, { b = b.copy(attaqueAnnoncee = a.copy(interruption = it)) }, multiligne = true)
                }
            }

            ListeEditable(
                titre = "Actions légendaires",
                elements = b.actionsLegendaires,
                resume = { it.resume },
                onModifier = { i -> actionEnCours = true to EnCours(i, b.actionsLegendaires[i]) },
                onRetirer = { i -> b = b.copy(actionsLegendaires = b.actionsLegendaires.filterIndexed { j, _ -> j != i }) },
                onAjouter = { actionEnCours = true to EnCours(-1, ActionBoss("", "", FormeActionBoss.ATTAQUE, bonusToucher = 5, degats = "1d8+3")) },
                aide = "Jouées après le tour d'un joueur, au prix de leurs points (rechargés à son tour).",
            )
            ListeEditable(
                titre = "Actions de repaire",
                elements = b.actionsRepaire,
                resume = { it.resume.replace(Regex(""" \(\d+ points\)"""), "") },
                onModifier = { i -> actionEnCours = false to EnCours(i, b.actionsRepaire[i]) },
                onRetirer = { i -> b = b.copy(actionsRepaire = b.actionsRepaire.filterIndexed { j, _ -> j != i }) },
                onAjouter = {
                    actionEnCours = false to EnCours(-1, ActionBoss("", "", FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Dextérité", dd = 13, degats = "2d6", portee = 120.0))
                },
                aide = "Une au hasard à la fin de chaque round, et quand un personnage rate une attaque sans avoir été blessé du round (une fois par round), dans les phases où le repaire est actif.",
            )
            ListeEditable(
                titre = "Points faibles",
                elements = b.pointsFaibles,
                resume = { "🎯 ${it.nom} — CA ${it.ca}, ${it.pv} PV" },
                onModifier = { i -> pointFaibleEnCours = EnCours(i, b.pointsFaibles[i]) },
                onRetirer = { i -> b = b.copy(pointsFaibles = b.pointsFaibles.filterIndexed { j, _ -> j != i }) },
                onAjouter = {
                    pointFaibleEnCours = EnCours(-1, PointFaibleBoss("Point faible ${b.pointsFaibles.size + 1}", 13, 25, "", DefensesDegats(etats = ConditionCombat.entries.toSet())))
                },
                aide = "Éléments du décor à détruire : ils ne jouent pas et n'empêchent pas la victoire.",
            )
            if (b.pointsFaibles.isNotEmpty()) SectionFiche("Effet des points faibles") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChampEntier("Régénération / tour", b.regeneration, { b = b.copy(regeneration = (it ?: 0).coerceAtLeast(0)) }, Modifier.weight(1f))
                    ChampEntier("Malus de CA détruits", b.malusCaSansPointsFaibles, { b = b.copy(malusCaSansPointsFaibles = (it ?: 0).coerceAtLeast(0)) }, Modifier.weight(1f))
                }
                ChampTexte("Annonce quand tous sont détruits", b.annoncePointsFaiblesDetruits.orEmpty(), { b = b.copy(annoncePointsFaiblesDetruits = it.ifBlank { null }) }, multiligne = true)
            }
            SectionFiche("Butin propre au boss") {
                ChampTexte("Objets, séparés par des virgules", butinTexte, { butinTexte = it })
                Text("Ajouté au butin de sa fiche de base quand il est vaincu.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    phaseEnCours?.let { (i, p) ->
        PhaseDialog(
            initiale = p,
            premiere = i == 0 || (i == -1 && b.phases.isEmpty()),
            onValider = { b = b.copy(phases = b.phases.remplace(i, it)); phaseEnCours = null },
            onDismiss = { phaseEnCours = null },
        )
    }
    actionEnCours?.let { (legendaire, enCours) ->
        ActionBossDialog(
            initiale = enCours.valeur,
            legendaire = legendaire,
            onValider = { a ->
                b = if (legendaire) b.copy(actionsLegendaires = b.actionsLegendaires.remplace(enCours.index, a))
                else b.copy(actionsRepaire = b.actionsRepaire.remplace(enCours.index, a))
                actionEnCours = null
            },
            onDismiss = { actionEnCours = null },
        )
    }
    pointFaibleEnCours?.let { (i, p) ->
        PointFaibleDialog(
            initial = p,
            onValider = { b = b.copy(pointsFaibles = b.pointsFaibles.remplace(i, it)); pointFaibleEnCours = null },
            onDismiss = { pointFaibleEnCours = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhaseDialog(initiale: PhaseBoss, premiere: Boolean, onValider: (PhaseBoss) -> Unit, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf(initiale) }
    var renforts by remember { mutableStateOf(CompositionCombat.formater(initiale.renforts)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (premiere) "Phase de départ" else "Phase") },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampTexte("Nom", p.nom, { p = p.copy(nom = it) })
                if (!premiere) ChampEntier("Commence sous (% de PV)", p.seuilPv, { p = p.copy(seuilPv = (it ?: 50).coerceIn(1, 99)) }, Modifier.fillMaxWidth())
                ChampTexte("Annonce lue à la table", p.annonce, { p = p.copy(annonce = it) }, multiligne = true)
                Text("Comportement", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = p.profil == null, onClick = { p = p.copy(profil = null) }, label = { Text("Inchangé") })
                    ProfilIA.entries.forEach { profil ->
                        FilterChip(selected = p.profil == profil, onClick = { p = p.copy(profil = profil) }, label = { Text(profil.label) })
                    }
                }
                p.profil?.let { Text(it.description, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChampEntier("CA (±)", p.modificateurCa, { p = p.copy(modificateurCa = it ?: 0) }, Modifier.weight(1f))
                    ChampEntier("Points légendaires (+)", p.bonusPointsLegendaires, { p = p.copy(bonusPointsLegendaires = (it ?: 0).coerceIn(0, 4)) }, Modifier.weight(1f))
                }
                LigneInterrupteur("Récupère ses capacités à recharge", p.rechargeImmediate) { p = p.copy(rechargeImmediate = it) }
                LigneInterrupteur("Le repaire est actif", p.repaireActif) { p = p.copy(repaireActif = it) }
                ChampTexte("Renforts (ex. Loup arctique x2)", renforts, { renforts = it })
            }
        },
        confirmButton = {
            TextButton(enabled = p.nom.isNotBlank(), onClick = {
                onValider(p.copy(seuilPv = if (premiere) 100 else p.seuilPv, renforts = CompositionCombat.parser(renforts)))
            }) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private val caracteristiques = listOf("Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionBossDialog(initiale: ActionBoss, legendaire: Boolean, onValider: (ActionBoss) -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf(initiale) }
    var degats by remember { mutableStateOf(initiale.degats.orEmpty()) }
    var porteeTexte by remember { mutableStateOf(initiale.portee.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }) }
    val formuleValide = degats.isBlank() || decomposerFormule(degats) != null
    val attaque = a.forme == FormeActionBoss.ATTAQUE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (legendaire) "Action légendaire" else "Action de repaire") },
        text = {
            Column(modifier = Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampTexte("Nom", a.nom, { a = a.copy(nom = it) })
                ChampTexte("Description (lue à la table)", a.description, { a = a.copy(description = it) }, multiligne = true)
                Text("Effet", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormeActionBoss.entries.forEach { f ->
                        FilterChip(selected = a.forme == f, onClick = { a = a.copy(forme = f) }, label = { Text(f.label) })
                    }
                }
                Text(
                    when (a.forme) {
                        FormeActionBoss.ATTAQUE -> "Jet d'attaque contre une cible à portée."
                        FormeActionBoss.SAUVEGARDE_CIBLES -> if (legendaire) "JS imposé à des adversaires à portée." else "JS imposé à des adversaires pris au hasard."
                        FormeActionBoss.SAUVEGARDE_CONTACT -> "JS imposé à chaque adversaire à portée de lui (l'IA la joue quand il est encerclé)."
                        FormeActionBoss.SAUVEGARDE_TOUS -> "JS imposé à tous les adversaires debout."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (legendaire) ChampEntier("Coût (points)", a.cout, { a = a.copy(cout = (it ?: 1).coerceIn(1, 4)) }, Modifier.fillMaxWidth())
                if (attaque) {
                    ChampEntier("Bonus au toucher", a.bonusToucher, { a = a.copy(bonusToucher = it) }, Modifier.fillMaxWidth())
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        caracteristiques.forEach { c ->
                            FilterChip(selected = a.sauvegarde == c, onClick = { a = a.copy(sauvegarde = c) }, label = { Text(c) })
                        }
                    }
                    ChampEntier("DD", a.dd, { a = a.copy(dd = it) }, Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = degats,
                        onValueChange = { degats = it },
                        label = { Text("Dégâts (ex. 2d6+4)") },
                        singleLine = true,
                        isError = !formuleValide,
                        modifier = Modifier.weight(1f),
                    )
                    ChampTexte("Type", a.typeDegats.orEmpty(), { a = a.copy(typeDegats = it.ifBlank { null }) }, modifier = Modifier.weight(1f))
                }
                if (!attaque) LigneInterrupteur("Moitié des dégâts sur un JS réussi", a.moitieSiReussi) { a = a.copy(moitieSiReussi = it) }
                Text(if (attaque) "États infligés s'il touche" else "États infligés sur un JS raté", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConditionCombat.entries.filter { it != ConditionCombat.CONCENTRATION }.forEach { c ->
                        FilterChip(
                            selected = c in a.etats,
                            onClick = { a = a.copy(etats = if (c in a.etats) a.etats - c else a.etats + c) },
                            label = { Text(c.label) },
                        )
                    }
                }
                val avecPortee = a.forme == FormeActionBoss.ATTAQUE || a.forme == FormeActionBoss.SAUVEGARDE_CONTACT ||
                    (legendaire && a.forme == FormeActionBoss.SAUVEGARDE_CIBLES)
                if (avecPortee) {
                    OutlinedTextField(
                        value = porteeTexte,
                        onValueChange = { porteeTexte = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                        label = { Text("Portée (m)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (a.forme == FormeActionBoss.SAUVEGARDE_CIBLES) {
                    ChampEntier("Nombre de cibles", a.nbCibles, { a = a.copy(nbCibles = (it ?: 1).coerceIn(1, 10)) }, Modifier.fillMaxWidth())
                }
                if (legendaire) LigneInterrupteur("Puis s'écarte (sans attaque d'opportunité)", a.repli) { a = a.copy(repli = it) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = a.nom.isNotBlank() && formuleValide && (attaque || (a.sauvegarde != null && a.dd != null)),
                onClick = {
                    val portee = porteeTexte.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(1.5) ?: a.portee
                    onValider(a.copy(degats = degats.trim().ifBlank { null }, portee = portee, cout = if (legendaire) a.cout else 1))
                },
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** « froid, poison » → types de dégâts reconnus (radicaux, comme les fiches du bestiaire). */
private fun lireTypes(texte: String): Set<String> =
    texte.split(',', ';').map { TypesCreature.radical(it.trim().lowercase()) }.filter { it.isNotEmpty() }.toSet()

@Composable
private fun PointFaibleDialog(initial: PointFaibleBoss, onValider: (PointFaibleBoss) -> Unit, onDismiss: () -> Unit) {
    var p by remember { mutableStateOf(initial) }
    var immunites by remember { mutableStateOf(initial.defenses.immunites.joinToString(", ")) }
    var resistances by remember { mutableStateOf(initial.defenses.resistances.joinToString(", ")) }
    var vulnerabilites by remember { mutableStateOf(initial.defenses.vulnerabilites.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Point faible") },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChampTexte("Nom", p.nom, { p = p.copy(nom = it) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChampEntier("CA", p.ca, { p = p.copy(ca = (it ?: 10).coerceIn(1, 30)) }, Modifier.weight(1f))
                    ChampEntier("PV", p.pv, { p = p.copy(pv = (it ?: 1).coerceAtLeast(1)) }, Modifier.weight(1f))
                }
                ChampTexte("Description", p.description, { p = p.copy(description = it) }, multiligne = true)
                ChampTexte("Immunités (ex. froid, poison)", immunites, { immunites = it })
                ChampTexte("Résistances", resistances, { resistances = it })
                ChampTexte("Vulnérabilités (ex. feu)", vulnerabilites, { vulnerabilites = it })
                Text("Un objet : immunisé contre tous les états.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = p.nom.isNotBlank(), onClick = {
                onValider(
                    p.copy(
                        defenses = DefensesDegats(
                            resistances = lireTypes(resistances),
                            immunites = lireTypes(immunites),
                            vulnerabilites = lireTypes(vulnerabilites),
                            etats = ConditionCombat.entries.toSet(),
                        )
                    )
                )
            }) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
