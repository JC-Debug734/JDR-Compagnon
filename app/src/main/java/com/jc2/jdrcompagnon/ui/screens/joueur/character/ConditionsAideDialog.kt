package com.jc2.jdrcompagnon.ui.screens.joueur.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats

/** Libellé de l'épuisement tel qu'il apparaît dans la case CONDITION : « Épuisement 2 ». */
fun libelleEpuisement(niveau: Int): String? = niveau.takeIf { it > 0 }?.let { "${Etats.NOM_EPUISEMENT} $it" }

/** Texte de la case CONDITION : états du champ condition, plus l'épuisement s'il y en a. */
fun texteConditions(condition: String, niveauEpuisement: Int): String =
    (condition.split(',').map { it.trim() }.filter { it.isNotEmpty() } + listOfNotNull(libelleEpuisement(niveauEpuisement)))
        .joinToString(", ")

/** Effet chiffré du niveau d'épuisement actuel (−2 par niveau aux d20, −1,50 m de vitesse). */
fun effetEpuisement(niveau: Int): String = when {
    niveau <= 0 -> "Aucun épuisement."
    niveau >= 6 -> "Niveau 6 : la créature meurt."
    else -> "Niveau $niveau / 6 : −${niveau * 2} aux jets de d20, vitesse −${"%.2f".format(niveau * 1.5).replace('.', ',')} m. Un repos long retire un niveau."
}

/**
 * Aide sur les conditions (double appui sur la case CONDITION de la fiche) : toutes les
 * conditions et leurs effets, celles subies par le personnage en tête et mises en avant.
 * L'épuisement y figure comme une condition, avec son niveau actuel. Avec [personnage], la
 * faim est détaillée en tête : jours depuis le dernier repas, tolérance et ce qui l'attend.
 */
@Composable
fun ConditionsAideDialog(
    condition: String,
    niveauEpuisement: Int,
    onDismiss: () -> Unit,
    personnage: com.jc2.jdrcompagnon.ui.Character? = null,
) {
    val subies = Etats.developper(Etats.lire(condition))
    // « Affamé » est détaillé par la carte Faim, pas répété dans les mentions libres.
    val autres = Etats.autres(condition).filterNot { personnage != null && it.equals(com.jc2.jdrcompagnon.ui.GameState.CONDITION_FAIM, ignoreCase = true) }
    // Fiches dépliées (celles subies le sont d'office).
    var ouvertes by remember { mutableStateOf(subies.map { it.name }.toSet() + if (niveauEpuisement > 0) setOf(Etats.NOM_EPUISEMENT) else emptySet()) }
    fun basculer(cle: String) { ouvertes = if (cle in ouvertes) ouvertes - cle else ouvertes + cle }

    val fiches = Etats.fiches.filter { it.officiel }.sortedWith(compareBy({ it.condition !in subies }, { it.condition.label }))
    val epuisementActif = niveauEpuisement > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conditions", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    val actuelles = texteConditions(condition, niveauEpuisement)
                    Text(
                        if (actuelles.isBlank()) "Aucune condition en cours. Touchez une condition pour voir ses effets."
                        else "En cours : $actuelles",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (autres.isNotEmpty()) {
                        Text(
                            "Mentions libres (${autres.joinToString(", ")}) : voir avec le MJ.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (personnage != null) item { CarteFaim(personnage) }
                // L'épuisement en tête quand il est subi, sinon après les conditions subies.
                if (epuisementActif) item {
                    CarteCondition(
                        titre = libelleEpuisement(niveauEpuisement)!!,
                        resume = effetEpuisement(niveauEpuisement),
                        regles = Etats.reglesEpuisement,
                        active = true,
                        ouverte = Etats.NOM_EPUISEMENT in ouvertes,
                        onClick = { basculer(Etats.NOM_EPUISEMENT) },
                    )
                }
                items(fiches, key = { it.condition.name }) { fiche ->
                    CarteCondition(
                        titre = fiche.condition.label,
                        resume = fiche.resume,
                        regles = fiche.regles,
                        active = fiche.condition in subies,
                        ouverte = fiche.condition.name in ouvertes,
                        onClick = { basculer(fiche.condition.name) },
                    )
                }
                if (!epuisementActif) item {
                    CarteCondition(
                        titre = Etats.NOM_EPUISEMENT,
                        resume = "État à niveaux (1 à 6) : −2 par niveau aux jets de d20, −1,50 m de vitesse par niveau.",
                        regles = Etats.reglesEpuisement,
                        active = false,
                        ouverte = Etats.NOM_EPUISEMENT in ouvertes,
                        onClick = { basculer(Etats.NOM_EPUISEMENT) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun CarteCondition(
    titre: String,
    resume: String,
    regles: List<String>,
    active: Boolean,
    ouverte: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (active) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titre + if (active) " (en cours)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(if (ouverte) "▲" else "▼", style = MaterialTheme.typography.labelSmall)
            }
            Text(resume, style = MaterialTheme.typography.bodySmall)
            if (ouverte) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                regles.forEach { regle ->
                    Text("• $regle", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

/** Nombre de conditions présentes sur la fiche (mentions du champ condition + épuisement). */
fun nombreConditions(condition: String, niveauEpuisement: Int): Int =
    condition.split(',').count { it.isNotBlank() } + if (niveauEpuisement > 0) 1 else 0

/**
 * Niveau de faim d'un personnage : jours sans manger depuis son dernier repas comparés à sa
 * tolérance (3 + modificateur de Constitution), jauge, et conséquence au prochain repos.
 */
@Composable
private fun CarteFaim(personnage: com.jc2.jdrcompagnon.ui.Character) {
    val horloge by com.jc2.jdrcompagnon.ui.ScenarioClockState.state.collectAsState()
    val jour = com.jc2.jdrcompagnon.ui.ScenarioClockState.dayIndex(horloge.scenarioMinutes)
    val jours = (jour - personnage.lastMealDay).coerceAtLeast(0).toInt()
    val tolerance = com.jc2.jdrcompagnon.ui.GameState.foodToleranceDays(personnage)
    val affame = jours > tolerance || com.jc2.jdrcompagnon.ui.GameState.estAffame(personnage)
    val modCon = com.jc2.jdrcompagnon.ui.GameState.abilityModifier(personnage.constitution)
    val niveau = when {
        jours == 0 -> "Rassasié"
        jours < tolerance -> "Faim légère"
        jours == tolerance -> "Faim forte (limite atteinte)"
        else -> "Affamé"
    }
    // Repliée comme les autres conditions : le détail (tolérance, conséquence) au dépli.
    var ouverte by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (affame) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (affame) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { ouverte = !ouverte },
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🍖 Faim : $niveau" + if (affame) " (en cours)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(if (ouverte) "▲" else "▼", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                if (jours == 0) "A mangé aujourd'hui." else "$jours jour(s) sans manger sur $tolerance de tolérance.",
                style = MaterialTheme.typography.bodySmall,
            )
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (jours.toFloat() / (tolerance + 1)).coerceIn(0f, 1f) },
                color = if (affame) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            if (ouverte) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(
                "Tolérance = 3 ${if (modCon >= 0) "+" else "−"} ${kotlin.math.abs(modCon)} (Constitution) = $tolerance jour(s).",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                when {
                    affame -> "Au-delà de la tolérance : un niveau d'épuisement par jour de dépassement, compté à chaque repos. Un repas remet le compteur à zéro."
                    jours == tolerance -> "Sans repas d'ici demain, le personnage sera affamé et prendra un niveau d'épuisement au prochain repos."
                    else -> "Encore ${tolerance - jours} jour(s) avant d'être affamé."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            }
        }
    }
}
