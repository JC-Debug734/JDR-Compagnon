package com.jc2.jdrcompagnon.feature_environnement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jc2.jdrcompagnon.feature_environnement.domain.model.CapaciteEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DifficulteRelative
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EchelleEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.GraviteDegats
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeCapacite
import com.jc2.jdrcompagnon.feature_environnement.presentation.EpreuveEnCours
import com.jc2.jdrcompagnon.feature_environnement.presentation.EpreuveSession
import com.jc2.jdrcompagnon.feature_environnement.presentation.IssueEpreuve
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.SessionRole

/** Écran MJ de résolution de l'épreuve environnementale active (EpreuveSession). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpreuveEnCoursScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val etat by EpreuveSession.etat.collectAsStateWithLifecycle()
    val role by NetworkSessionManager.role.collectAsStateWithLifecycle()
    val clients by NetworkSessionManager.connectedClients.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("ÉPREUVE") },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                )
                Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    Spacer(Modifier.weight(1f))
                    etat?.let { courant ->
                        if (courant.issue == null) {
                            TextButton(onClick = { EpreuveSession.abandonner() }) { Text("Interrompre") }
                        } else {
                            TextButton(onClick = { EpreuveSession.fermer(); onBack() }) { Text("Clore") }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val courant = etat
        if (courant == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Aucune épreuve en cours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Lancez-en une depuis l'outil Environnement ou un lien #epreuve: de scénario.", style = MaterialTheme.typography.bodySmall)
            }
            return@Scaffold
        }
        EpreuveContenu(
            etat = courant,
            partage = if (role == SessionRole.HOST) clients.size else null,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

@Composable
private fun EpreuveContenu(etat: EpreuveEnCours, partage: Int?, modifier: Modifier) {
    val epreuve = etat.epreuve
    val tier = etat.tier
    val enCours = etat.issue == null

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(epreuve.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${epreuve.type.label} · Tier $tier (${EchelleEpreuve.niveauxDuTier(tier)}) · ${etat.nbJoueurs} joueur(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Stepper(
                        label = if (etat.niveauAuto) "Niveau (auto, joueurs connectés)" else "Niveau (choisi par le MJ)",
                        valeur = etat.niveau, min = 1, max = 20
                    ) { EpreuveSession.changerNiveau(it) }
                    if (epreuve.description.isNotBlank()) Text(epreuve.description, style = MaterialTheme.typography.bodyMedium)
                    if (epreuve.pulsions.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Pulsions : ${epreuve.pulsions.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                    }
                    partage?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Progrès partagé avec $it joueur(s) connecté(s) (la Menace reste secrète).", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Jauge("Progrès", etat.progres, etat.progresMax, MaterialTheme.colorScheme.primary, enCours) { EpreuveSession.ajusterProgres(it) }
                    Spacer(Modifier.height(8.dp))
                    Jauge("Menace", etat.menace, etat.menaceMax, MaterialTheme.colorScheme.error, enCours) { EpreuveSession.ajusterMenace(it) }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("DD du tier $tier", style = MaterialTheme.typography.labelLarge)
                    Text(DifficulteRelative.entries.joinToString("  ·  ") { "${it.label} ${EchelleEpreuve.dd(tier, it)}" }, style = MaterialTheme.typography.bodyMedium)
                    Text("Dégâts", style = MaterialTheme.typography.labelLarge)
                    Text(GraviteDegats.entries.joinToString("  ·  ") { "${it.label} ${EchelleEpreuve.degats(tier, it)}" }, style = MaterialTheme.typography.bodyMedium)
                    if (epreuve.competences.isNotEmpty()) {
                        Text("Compétences utiles", style = MaterialTheme.typography.labelLarge)
                        Text(epreuve.competences.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (enCours) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { EpreuveSession.reussite() }, modifier = Modifier.weight(1f)) { Text("Réussite") }
                        Button(
                            onClick = { EpreuveSession.echec() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Échec") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { EpreuveSession.reussite(critique = true) }, modifier = Modifier.weight(1f)) { Text("20 naturel (+2)") }
                        OutlinedButton(onClick = { EpreuveSession.echec(critique = true) }, modifier = Modifier.weight(1f)) { Text("1 naturel (−2)") }
                    }
                }
            }
        }

        if (enCours && etat.echecEnAttente) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Échec : l'environnement peut réagir. Déclenchez une Réaction ou une Action ci-dessous.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { EpreuveSession.ignorerEchec() }) { Text("Ignorer") }
                    }
                }
            }
        }

        etat.issue?.let { issue ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (issue == IssueEpreuve.REUSSITE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(issue.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            when (issue) {
                                IssueEpreuve.REUSSITE -> "Le groupe a surmonté l'épreuve. Les échecs subis en chemin restent acquis (dégâts, états, matériel perdu)."
                                IssueEpreuve.ECHEC -> "La Menace est épuisée : appliquez une conséquence majeure (détour coûteux, perte importante, rencontre avec un adversaire, séparation du groupe…)."
                                IssueEpreuve.ABANDON -> "Épreuve interrompue par le MJ."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        TypeCapacite.entries.forEach { type ->
            val capacites = epreuve.capacites.filter { it.type == type }
            if (capacites.isNotEmpty()) {
                item { Text(type.label + "s", style = MaterialTheme.typography.titleMedium) }
                items(capacites) { capacite ->
                    CapaciteCard(
                        capacite = capacite,
                        tier = tier,
                        declenchable = enCours && etat.echecEnAttente && type != TypeCapacite.PASSIVE
                    )
                }
            }
        }

        if (epreuve.adversaires.isNotEmpty()) {
            item {
                Text("Adversaires possibles", style = MaterialTheme.typography.titleMedium)
                Text(epreuve.adversaires.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            }
        }

        item { Text("Journal", style = MaterialTheme.typography.titleMedium) }
        items(etat.journal.asReversed()) { ligne ->
            Text("• $ligne", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Jauge(label: String, valeur: Int, max: Int, couleur: Color, modifiable: Boolean, onAjuster: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        if (modifiable) {
            IconButton(onClick = { onAjuster(-1) }) { Icon(Icons.Default.Remove, contentDescription = "$label −1") }
        }
        Text("$valeur / $max", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (modifiable) {
            IconButton(onClick = { onAjuster(1) }) { Icon(Icons.Default.Add, contentDescription = "$label +1") }
        }
    }
    LinearProgressIndicator(
        progress = { valeur.toFloat() / max.coerceAtLeast(1) },
        modifier = Modifier.fillMaxWidth(),
        color = couleur
    )
}

@Composable
private fun CapaciteCard(capacite: CapaciteEpreuve, tier: Int, declenchable: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (declenchable) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(capacite.nom, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            EchelleEpreuve.resoudre(capacite, tier)?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (capacite.description.isNotBlank()) Text(capacite.description, style = MaterialTheme.typography.bodySmall)
            capacite.question?.let {
                Text("« $it »", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            if (declenchable) {
                TextButton(onClick = { EpreuveSession.declencher(capacite) }) { Text("Déclencher") }
            }
        }
    }
}
