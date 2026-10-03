package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.ui.GameState.AttitudePnj
import com.jc2.jdrcompagnon.ui.GameState.PnjDiscussion

private val TexteSecondaire = Color.White.copy(alpha = 0.8f)

/**
 * Attitude suggérée à partir de la réputation du PNJ envers le groupe (onglet PNJ de sa fiche) :
 * l'historique des PJ auprès de lui. Null si le PNJ n'a pas d'avis sur le groupe.
 */
fun attitudeDepuisReputation(score: Int?): AttitudePnj? = when {
    score == null -> null
    score <= -11 -> AttitudePnj.HOSTILE
    score <= 10 -> AttitudePnj.INDIFFERENT
    else -> AttitudePnj.AMICAL
}

/**
 * Fiche d'une discussion de scène, remplie par le MJ dans l'éditeur de scénario : ce que le PNJ
 * attend de l'échange. Chaque champ est facultatif ; laissé vide, il reste libre en jeu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscussionFicheDialog(
    pnjName: String,
    initial: PnjDiscussion?,
    onDismiss: () -> Unit,
    onSave: (PnjDiscussion) -> Unit,
) {
    var attitude by remember { mutableStateOf(initial?.attitude) }
    var desirs by remember { mutableStateOf(initial?.desirs.orEmpty()) }
    var peurs by remember { mutableStateOf(initial?.peurs.orEmpty()) }
    var ligneRouge by remember { mutableStateOf(initial?.ligneRouge.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discussion avec : $pnjName") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Tout est facultatif : un champ laissé vide reste libre, à décider en jeu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TexteSecondaire
                )
                Text("Attitude initiale", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = attitude == null, onClick = { attitude = null }, label = { Text("Libre") })
                    AttitudePnj.entries.forEach { a ->
                        FilterChip(
                            selected = attitude == a,
                            onClick = { attitude = a },
                            label = { Text("${a.label} (ND ${a.nd})") }
                        )
                    }
                }
                Text(
                    "Selon l'historique des PJ, le contexte (armes, lieu, danger) et la nature du PNJ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TexteSecondaire
                )
                OutlinedTextField(
                    value = desirs,
                    onValueChange = { desirs = it },
                    label = { Text("Désirs et motivations") },
                    supportingText = { Text("Utilisés dans l'argumentaire → Avantage") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = peurs,
                    onValueChange = { peurs = it },
                    label = { Text("Peurs et aversions") },
                    supportingText = { Text("Abordées ou déclenchées → Désavantage") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ligneRouge,
                    onValueChange = { ligneRouge = it },
                    label = { Text("Ligne rouge") },
                    supportingText = { Text("Demande inacceptable → échec automatique") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    PnjDiscussion(
                        pnjName = pnjName,
                        attitude = attitude,
                        desirs = desirs.trim(),
                        peurs = peurs.trim(),
                        ligneRouge = ligneRouge.trim(),
                    )
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * Aide de jeu affichée au MJ quand il ouvre une discussion (briefing du PNJ) : attitude et ND,
 * désirs (Avantage), peurs (Désavantage), ligne rouge (échec automatique). Attitude non fixée
 * par le scénario : le MJ la choisit sur place, suggérée par la réputation du PNJ envers le
 * groupe ([reputationGroupe]) quand elle existe.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscussionAideDeJeu(
    discussion: PnjDiscussion?,
    reputationGroupe: Int?,
) {
    val suggestion = attitudeDepuisReputation(reputationGroupe)
    var attitudeEnJeu by remember(discussion) { mutableStateOf(discussion?.attitude ?: suggestion) }
    // ND en vigueur, utilisé pour juger les jets sociaux envoyés par les joueurs.
    val ndEnVigueur = (discussion?.attitude ?: attitudeEnJeu)?.nd
    LaunchedEffect(ndEnVigueur) { com.jc2.jdrcompagnon.network.NetworkSessionManager.setDiscussionNd(ndEnVigueur) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💬 Ce que le PNJ attend de la discussion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)

            Text("Attitude initiale", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (discussion?.attitude != null) {
                Text(
                    "${discussion.attitude.label} — ND ${discussion.attitude.nd} (fixée par le scénario)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    buildString {
                        append("Libre : à choisir selon l'historique, le contexte et la nature du PNJ.")
                        if (reputationGroupe != null) {
                            append(" Réputation envers le groupe : ${ReputationScale.labelFor(reputationGroupe)} ($reputationGroupe).")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TexteSecondaire
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttitudePnj.entries.forEach { a ->
                        FilterChip(
                            selected = attitudeEnJeu == a,
                            onClick = { attitudeEnJeu = a },
                            label = { Text(a.label + if (a == suggestion) " ★" else "") }
                        )
                    }
                }
                attitudeEnJeu?.let {
                    Text("ND du test : ${it.nd}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            RegleLigne(
                titre = "Désirs et motivations",
                contenu = discussion?.desirs,
                effet = "Découverts (Intuition) et utilisés dans l'argumentaire → Avantage au jet de Charisme (Persuasion, Tromperie, Intimidation)."
            )
            RegleLigne(
                titre = "Peurs et aversions",
                contenu = discussion?.peurs,
                effet = "Sujet sensible abordé ou peur déclenchée → Désavantage au jet."
            )
            RegleLigne(
                titre = "Ligne rouge",
                contenu = discussion?.ligneRouge,
                effet = "Demande inacceptable (sa vie ou ses proches en danger mortel) → échec automatique, quel que soit le dé."
            )
        }
    }
}

@Composable
private fun RegleLigne(titre: String, contenu: String?, effet: String) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text(titre, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            contenu?.takeIf { it.isNotBlank() } ?: "Libre (non défini par le scénario).",
            style = MaterialTheme.typography.bodyMedium,
            color = if (contenu.isNullOrBlank()) TexteSecondaire else Color.White
        )
        Text(effet, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
    }
}
