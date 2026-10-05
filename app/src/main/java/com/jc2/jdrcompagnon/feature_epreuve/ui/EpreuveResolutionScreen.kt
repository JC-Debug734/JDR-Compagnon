package com.jc2.jdrcompagnon.feature_epreuve.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.DifficulteEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ReglesEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveActive
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession
import com.jc2.jdrcompagnon.feature_epreuve.presentation.TentativeEpreuve
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val VertReussite = Color(0xFF43A047)
private val RougeEchec = Color(0xFFC62828)

/** Dernière épreuve dont la musique a été lancée (voir EpreuveResolutionScreen). */
private var musiqueLanceePour: String? = null

/**
 * Résolution MJ de l'épreuve active (EpreuveOutilSession) : chaque joueur annonce son action,
 * le MJ note une réussite (le compteur avance) ou un échec (une complication est tirée). L'état
 * est affiché en direct sur la page table (MjWebServer, /api/epreuve).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EpreuveResolutionScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val etat by EpreuveOutilSession.etat.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    // Musique de l'épreuve : lancée une seule fois par épreuve démarrée (revenir sur l'écran ne la
    // relance pas si le MJ l'a coupée entre-temps).
    androidx.compose.runtime.LaunchedEffect(etat?.lancement) {
        val epreuve = etat?.epreuve ?: return@LaunchedEffect
        val lancement = "${epreuve.id}:${etat?.lancement}"
        if (musiqueLanceePour == lancement) return@LaunchedEffect
        musiqueLanceePour = lancement
        val piste = com.jc2.jdrcompagnon.ui.availableLoopTracks.firstOrNull { it.id.equals(epreuve.musicTrackId, ignoreCase = true) }
            ?: return@LaunchedEffect
        com.jc2.jdrcompagnon.ui.MusicManager.play(context, piste)
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("ÉPREUVE", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                )
                Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    etat?.let { courant ->
                        if (!courant.terminee) {
                            TextButton(onClick = { EpreuveOutilSession.terminer() }) { Text("Interrompre") }
                        }
                        TextButton(onClick = { EpreuveOutilSession.fermer(); onBack() }) { Text("Fermer") }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        val courant = etat
        if (courant == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Aucune épreuve en cours. Lancez-en une depuis l'outil Épreuves.", color = Color.White)
            }
        } else {
            ContenuResolution(courant, Modifier.fillMaxSize().padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenuResolution(etat: EpreuveActive, modifier: Modifier) {
    val epreuve = etat.epreuve
    var joueur by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf("") }
    val participants = participantsDuGroupe()

    fun noter(reussite: Boolean) {
        if (reussite) EpreuveOutilSession.reussite(joueur, action) else EpreuveOutilSession.echec(joueur, action)
        joueur = null
        action = ""
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EnTete(etat)

        if (epreuve.description.isNotBlank()) {
            Text(epreuve.description, color = Color.White, fontStyle = FontStyle.Italic)
        }

        Compteur(etat)

        if (!etat.terminee) ReglageGroupe(etat)

        when {
            etat.terminee -> Issue(etat)
            else -> {
                if (participants.isNotEmpty()) {
                    Text("Qui agit ?", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        participants.forEach { nom ->
                            FilterChip(
                                selected = joueur == nom,
                                onClick = { joueur = if (joueur == nom) null else nom },
                                label = { Text(nom) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("Action annoncée (facultatif)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { noter(true) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VertReussite, contentColor = Color.White),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Réussite", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { noter(false) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RougeEchec, contentColor = Color.White),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Échec", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val derniere = etat.tentatives.lastOrNull()
        if (derniere != null && !derniere.reussite) {
            ComplicationCarte(derniere, peutRelancer = !etat.terminee && epreuve.complications.size > 1)
        }

        if (etat.tentatives.isNotEmpty()) {
            OutlinedButton(onClick = { EpreuveOutilSession.annulerDerniere() }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Annuler la dernière tentative", color = Color.White)
            }
            Journal(etat.tentatives)
        }
    }
}

/**
 * Groupe pour lequel l'épreuve est calibrée (nombre de joueurs, niveau) : le MJ le corrige si le
 * groupe détecté n'est pas le bon, réussites et DD suivent (EpreuveOutilSession.ajusterGroupe).
 */
@Composable
private fun ReglageGroupe(etat: EpreuveActive) {
    val groupe = etat.groupe
    val difficulte = etat.source.difficulte ?: DifficulteEpreuve.MOYENNE
    Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = Color.White)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                (etat.source.difficulte?.let { "Difficulté ${it.libelle.lowercase()}" } ?: "Réussites fixes") +
                    " · DD ${ReglesEpreuve.ddPour(difficulte, groupe.niveau)} · dégâts ${ReglesEpreuve.degatsPour(difficulte.gravite, groupe.niveau)}" +
                    if (groupe.parDefaut) "\nAucun groupe détecté : ajustez joueurs et niveau." else "",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Reglage("Joueurs", groupe.joueurs) { EpreuveOutilSession.ajusterGroupe(it, groupe.niveau) }
                Spacer(Modifier.weight(1f))
                Reglage("Niveau", groupe.niveau) { EpreuveOutilSession.ajusterGroupe(groupe.joueurs, it) }
            }
        }
    }
}

@Composable
private fun Reglage(libelle: String, valeur: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { onChange(valeur - 1) }) { Icon(Icons.Default.Remove, contentDescription = "$libelle moins", tint = Color.White) }
        Text("$valeur", fontWeight = FontWeight.Bold)
        IconButton(onClick = { onChange(valeur + 1) }) { Icon(Icons.Default.Add, contentDescription = "$libelle plus", tint = Color.White) }
    }
}

@Composable
private fun EnTete(etat: EpreuveActive) {
    val image = rememberImageEpreuve(etat.epreuve.imageFileName, echantillonnage = 2)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (image != null) 170.dp else 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ForcedDarkPalette.Indicator),
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
        )
        Text(
            etat.epreuve.nom,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}

/** Une pastille par réussite nécessaire : dorée quand elle est obtenue. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Compteur(etat: EpreuveActive) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Réussites : ${etat.reussites} / ${etat.epreuve.reussitesRequises}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(etat.epreuve.reussitesRequises) { index ->
                    val obtenue = index < etat.reussites
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (obtenue) ForcedDarkPalette.AccentGold else Color.Transparent)
                            .border(2.dp, ForcedDarkPalette.AccentGold, CircleShape),
                    )
                }
            }
            Text("Échecs : ${etat.echecs}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

@Composable
private fun Issue(etat: EpreuveActive) {
    val couleur = if (etat.reussie) VertReussite else ForcedDarkPalette.Content
    Card(
        colors = CardDefaults.cardColors(containerColor = couleur.copy(alpha = 0.25f)),
        border = BorderStroke(2.dp, couleur),
    ) {
        Text(
            if (etat.reussie) "Épreuve surmontée !" else "Épreuve interrompue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun ComplicationCarte(tentative: TentativeEpreuve, peutRelancer: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RougeEchec.copy(alpha = 0.25f)),
        border = BorderStroke(2.dp, RougeEchec),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("COMPLICATION", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White)
            }
            val complication = tentative.complication
            if (complication == null) {
                Text("Aucune complication définie pour cette épreuve : à vous d'improviser.", color = Color.White)
            } else {
                Text(complication.titre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                if (complication.description.isNotBlank()) Text(complication.description, color = Color.White)
                if (peutRelancer) {
                    TextButton(onClick = { EpreuveOutilSession.relancerComplication() }) {
                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tirer une autre complication")
                    }
                }
            }
        }
    }
}

@Composable
private fun Journal(tentatives: List<TentativeEpreuve>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Journal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = Color.White)
            tentatives.withIndex().reversed().forEach { (index, t) ->
                val qui = listOfNotNull(t.joueur, t.action?.let { "« $it »" }).joinToString(" — ").ifEmpty { "Tentative" }
                val resultat = if (t.reussite) "réussite" else "échec" + (t.complication?.let { " → ${it.titre}" } ?: "")
                Text(
                    "${index + 1}. $qui : $resultat",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (t.reussite) Color.White else Color(0xFFFFCDD2),
                )
            }
        }
    }
}

/** Personnages du groupe sélectionné (fiches puis joueurs sans fiche), pour noter qui agit. */
@Composable
private fun participantsDuGroupe(): List<String> {
    val groupeId by GameState.currentGroupId.collectAsState()
    val groupes by GameState.mjGroups.collectAsState()
    val personnages by GameState.characters.collectAsState()
    val groupe = groupes.firstOrNull { it.id == groupeId } ?: return emptyList()
    return (groupe.memberIds.mapNotNull { id -> personnages.firstOrNull { it.id == id }?.name } + groupe.tablePlayers.map { it.name })
        .filter { it.isNotBlank() }
        .distinct()
}
