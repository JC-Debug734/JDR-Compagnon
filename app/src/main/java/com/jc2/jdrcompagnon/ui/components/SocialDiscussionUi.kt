package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.NetworkSessionManager.SocialRollMode
import com.jc2.jdrcompagnon.network.NetworkSessionManager.SocialRollRequest
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.screens.joueur.skillAbility

/** Compétences proposées au joueur pendant une discussion avec un PNJ. */
val socialSkills = listOf(
    "Persuasion", "Tromperie", "Intimidation", "Représentation", // Charisme
    "Intuition",                                                 // Sagesse
    "Histoire", "Religion",                                      // Intelligence
)

/** À quoi sert chaque compétence sociale dans une discussion (aide affichée au joueur). */
val socialSkillDescriptions = mapOf(
    "Persuasion" to "Convaincre avec des arguments honnêtes, négocier, obtenir une faveur ou apaiser.",
    "Tromperie" to "Mentir, bluffer, cacher ses vraies intentions sans se faire démasquer.",
    "Intimidation" to "Faire pression par la menace, la force ou une attitude hostile.",
    "Représentation" to "Captiver par un discours, un récit, un chant : gagner l'attention ou la sympathie.",
    "Intuition" to "Deviner les vraies intentions du PNJ, sentir un mensonge ou son humeur.",
    "Histoire" to "Se rappeler un fait, une lignée, un événement passé utile à la conversation.",
    "Religion" to "Connaître dieux, rites et cultes : parler à un prêtre ou à un fidèle.",
)

/** Bonus total d'une compétence : modificateur de caractéristique + maîtrise (doublée en expertise). */
fun skillBonus(character: Character, skill: String): Int {
    val ability = skillAbility[skill] ?: return 0
    val mod = GameState.abilityModifierForSave(ability, character)
    return mod + when (GameState.skillProficiencyLevel(character, skill)) {
        ProficiencyLevel.NONE -> 0
        ProficiencyLevel.PROFICIENT -> character.proficiencyBonus
        ProficiencyLevel.EXPERTISE -> character.proficiencyBonus * 2
    }
}

private fun signe(v: Int) = if (v >= 0) "+$v" else "$v"
private val TexteSecondaire = Color.White.copy(alpha = 0.8f)
private val VertReussite = Color(0xFF7BC67B)

// ─────────────────────────── Côté joueur ───────────────────────────

/**
 * Fenêtre de discussion côté joueur, ouverte quand le MJ envoie le PNJ : portrait, nom, et les
 * compétences sociales du personnage incarné avec leur bonus. Choisir une compétence envoie la
 * demande au MJ ; une fois sa décision reçue (normal / avantage / désavantage), le joueur lance
 * et voit son dé — jamais s'il a réussi.
 */
@Composable
fun PnjDiscussionPlayerDialog(name: String, portraitId: String?, onLeave: (characterName: String?) -> Unit) {
    val portraitPainter = rememberCharacterPortraitPainter(portraitId)
    val characters by GameState.characters.collectAsState()
    val claimedId by NetworkSessionManager.claimedCharacterId.collectAsState()
    val selectedId by GameState.selectedCharacterId.collectAsState()
    // Personnage incarné : réservé auprès du MJ, sinon celui choisi sur l'appareil (fiche envoyée
    // par le MJ ou sélectionnée par le joueur), sinon l'unique PJ présent.
    val personnage = characters.find { it.id == claimedId }
        ?: characters.find { it.id == selectedId }
        ?: characters.filter { it.type == "PJ" }.singleOrNull()
    val jet by NetworkSessionManager.playerSocialRoll.collectAsState()
    val verrou by NetworkSessionManager.discussionLock.collectAsState()
    val avis by NetworkSessionManager.playerDiscussionNotice.collectAsState()
    val quitter = { onLeave(personnage?.name) }

    Dialog(onDismissRequest = quitter, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = Color.White) {
            Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (portraitPainter != null) {
                    Image(
                        painter = portraitPainter,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text("💬 Discussion avec", style = MaterialTheme.typography.labelLarge, color = TexteSecondaire)
                Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                avis?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

                val enCours = jet
                // Un autre joueur agit : on attend la fin de son interaction avec le MJ.
                val bloquePar = verrou?.takeIf { it.requestId != enCours?.id }
                when {
                    personnage == null -> Text(
                        "Aucun personnage incarné : impossible de faire un jet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TexteSecondaire
                    )
                    enCours == null -> {
                        if (bloquePar != null) {
                            Text(
                                "⏳ ${bloquePar.characterName} est en train d'agir… Patientez jusqu'à la fin de son action.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text("Quelle compétence utilisez-vous ?", style = MaterialTheme.typography.titleMedium)
                        }
                        socialSkills.forEach { skill ->
                            CompetenceSocialeCarte(
                                skill = skill,
                                bonus = skillBonus(personnage, skill),
                                enabled = bloquePar == null,
                                onClick = {
                                    NetworkSessionManager.requestSocialRoll(personnage.name, name, skill, skillBonus(personnage, skill))
                                },
                            )
                        }
                    }
                    else -> JetJoueurCarte(enCours, interactionTerminee = verrou == null)
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = quitter) { Text("🚪 Quitter la discussion", color = Color.White) }
            }
        }
    }
}

/** Une compétence sociale cliquable, avec son bonus et une phrase expliquant à quoi elle sert. */
@Composable
private fun CompetenceSocialeCarte(skill: String, bonus: Int, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(skill, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(signe(bonus), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            skillAbility[skill]?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TexteSecondaire) }
            socialSkillDescriptions[skill]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire) }
        }
    }
}

@Composable
private fun JetJoueurCarte(jet: NetworkSessionManager.PlayerSocialRoll, interactionTerminee: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("${jet.skill} ${signe(jet.bonus)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when {
                jet.mode == null -> Text("En attente du MJ…", style = MaterialTheme.typography.bodyLarge, color = TexteSecondaire)
                jet.dice == null -> {
                    Text(
                        when (jet.mode) {
                            SocialRollMode.AVANTAGE -> "Le MJ vous accorde l'Avantage : 2d20, le meilleur compte."
                            SocialRollMode.DESAVANTAGE -> "Le MJ impose le Désavantage : 2d20, le pire compte."
                            else -> "Jet normal : 1d20."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { NetworkSessionManager.rollSocial() }) { Text("🎲 Lancer le dé") }
                    SaisieVraiDe(nbDes = if (jet.mode == SocialRollMode.NORMAL) 1 else 2)
                }
                else -> {
                    Text("🎲 ${jet.dice.joinToString(" / ")}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Total : ${jet.total}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Le MJ décide de l'issue de la discussion.", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                    OutlinedButton(onClick = { NetworkSessionManager.clearPlayerSocialRoll() }, enabled = interactionTerminee) {
                        Text(if (interactionTerminee) "Nouveau jet" else "En attente du MJ…", color = Color.White)
                    }
                }
            }
        }
    }
}

/** Saisie du résultat d'un vrai dé (1 d20, ou 2 avec avantage/désavantage) à la place du tirage de l'appli. */
@Composable
private fun SaisieVraiDe(nbDes: Int) {
    var valeurs by remember(nbDes) { mutableStateOf(List(nbDes) { "" }) }
    val des = valeurs.map { it.toIntOrNull() }
    val valides = des.all { it != null && it in 1..20 }
    Text("… ou j'ai lancé un vrai dé :", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        valeurs.forEachIndexed { i, v ->
            OutlinedTextField(
                value = v,
                onValueChange = { saisie ->
                    val chiffres = saisie.filter { it.isDigit() }.take(2)
                    valeurs = valeurs.toMutableList().also { it[i] = chiffres }
                },
                label = { Text(if (nbDes == 1) "d20" else "d20 n°${i + 1}") },
                isError = v.isNotEmpty() && v.toIntOrNull()?.let { it in 1..20 } != true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(90.dp),
            )
        }
        OutlinedButton(
            onClick = { NetworkSessionManager.rollSocial(manualDice = des.filterNotNull()) },
            enabled = valides,
        ) { Text("Valider", color = Color.White) }
    }
}

// ─────────────────────────── Côté MJ ───────────────────────────

/**
 * Jets sociaux des joueurs pendant la discussion, côté MJ : pour chaque demande, choisir
 * Normal / Avantage / Désavantage / Ligne rouge, puis voir le résultat face au ND.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialRequestsPanel(requests: List<SocialRollRequest>, modifier: Modifier = Modifier) {
    if (requests.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🎲 Jets des joueurs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            requests.forEach { req -> SocialRequestRow(req) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SocialRequestRow(req: SocialRollRequest) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${req.characterName} → ${req.skill} ${signe(req.bonus)}" + if (req.pnjName.isNotBlank()) " (${req.pnjName})" else "",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        // Retour en arrière : le joueur ne veut plus faire cette action (tant que le dé n'est pas lancé).
        val annuler = @Composable {
            TextButton(onClick = { NetworkSessionManager.cancelSocialRequest(req.id) }) {
                Text("↩ Annuler l'action", color = Color.White)
            }
        }
        when {
            req.mode == null -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SocialRollMode.entries.forEach { mode ->
                        OutlinedButton(onClick = { NetworkSessionManager.decideSocialRoll(req.id, mode) }) {
                            Text(mode.label, color = if (mode == SocialRollMode.LIGNE_ROUGE) MaterialTheme.colorScheme.error else Color.White)
                        }
                    }
                }
                annuler()
            }
            req.total == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${req.mode.label} — en attente du lancer…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TexteSecondaire,
                    modifier = Modifier.weight(1f)
                )
                annuler()
            }
            else -> {
                Text(
                    "${req.mode.label} · dés ${req.dice.orEmpty().joinToString(" / ")} → total ${req.total}" +
                        (req.nd?.let { " contre ND $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
                val verdict = req.reussite
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            req.mode == SocialRollMode.LIGNE_ROUGE -> "✖ Échec (ligne rouge)"
                            verdict == true -> "✔ Réussite"
                            verdict == false -> "✖ Échec"
                            else -> "ND non fixé : à vous de trancher"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = when (verdict) {
                            true -> VertReussite
                            false -> MaterialTheme.colorScheme.error
                            null -> TexteSecondaire
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { NetworkSessionManager.clearSocialRequest(req.id) }) { Text("OK", color = Color.White) }
                }
            }
        }
    }
}

/**
 * Relais côté MJ quand le briefing du PNJ est fermé : une demande de jet ou un résultat arrivé
 * s'affiche quel que soit l'écran (monté avec HostNetworkOverlay dans le NavGraph).
 */
@Composable
fun HostSocialRollOverlay() {
    val requests by NetworkSessionManager.socialRequests.collectAsState()
    val notices by NetworkSessionManager.discussionNotices.collectAsState()
    val discussionOpen by NetworkSessionManager.discussionOpen.collectAsState()
    if (discussionOpen) return
    // Demandes à traiter : sans décision, ou lancées (résultat à lire). Celles « en attente du
    // lancer » ne demandent rien au MJ.
    val aTraiter = requests.filter { it.mode == null || it.total != null }
    if (aTraiter.isEmpty() && notices.isEmpty()) return
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DiscussionNoticesPanel(notices)
            SocialRequestsPanel(aTraiter)
        }
    }
}

/** Avis au MJ : joueurs qui ont quitté la discussion (ou se sont déconnectés pendant). */
@Composable
fun DiscussionNoticesPanel(notices: List<String>, modifier: Modifier = Modifier) {
    if (notices.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            notices.forEach { notice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notice, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { NetworkSessionManager.dismissDiscussionNotice(notice) }) { Text("OK", color = Color.White) }
                }
            }
        }
    }
}
