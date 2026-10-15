package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import com.jc2.jdrcompagnon.ui.components.skillBonus
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.Monnaie
import com.jc2.jdrcompagnon.feature_carte.domain.model.FonctionLieu
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlin.random.Random

/**
 * Ce que le joueur peut faire dans un lieu de ville selon sa fonction (voir FonctionLieu) :
 * prier au temple, lire le panneau de quêtes, dormir à l'auberge... Les effets passent par
 * GameState (fiche du personnage, relayée au MJ en partie réseau). Une action payante n'est
 * faite que si la bourse suffit ; prière, rumeur et pari se font une fois par visite.
 */
@Composable
fun FonctionLieuJoueur(lieu: LieuNotable, personnage: Character?, campagneId: String?) {
    if (lieu.fonction == FonctionLieu.AUCUNE) return
    var resultat by remember(lieu.id) { mutableStateOf<String?>(null) }
    var dejaFait by remember(lieu.id) { mutableStateOf(false) }
    val contexte = androidx.compose.ui.platform.LocalContext.current
    val portee = androidx.compose.runtime.rememberCoroutineScope()

    /** Paie le prix du lieu ; faux (et message) si la bourse ne suffit pas. */
    fun payer(perso: Character): Boolean {
        if (lieu.prixPc <= 0) return true
        if (GameState.payer(perso.id, lieu.prixPc)) return true
        resultat = "✖ Bourse insuffisante (${Monnaie.libelle(lieu.prixPc)})."
        return false
    }
    val prix = if (lieu.prixPc > 0) " (${Monnaie.libelle(lieu.prixPc)})" else ""

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${lieu.fonction.emoji} ${lieu.fonction.label}", style = MaterialTheme.typography.titleSmall, color = ForcedDarkPalette.AccentGold, fontWeight = FontWeight.Bold)
            when (lieu.fonction) {
                FonctionLieu.PANNEAU_QUETES -> PanneauQuetes(lieu, campagneId, personnage)
                FonctionLieu.ECURIE -> Ecurie(lieu, personnage, prix) { monture ->
                    val perso = personnage ?: return@Ecurie
                    if (payer(perso)) {
                        GameState.addItemToBackpack(perso.id, "Monture louée : $monture")
                        resultat = "🐎 $monture vous attend devant l'écurie (ajoutée à vos possessions)."
                    }
                }
                FonctionLieu.BANQUE -> Banque(lieu, personnage) { resultat = it }
                else -> {
                    val (bouton, explication) = when (lieu.fonction) {
                        FonctionLieu.TEMPLE -> "🙏 Prier$prix" to "Chance d'obtenir une bénédiction : ${lieu.chancePourcent} %."
                        FonctionLieu.AUBERGE -> "🛏️ Passer la nuit$prix" to "Une nuit en chambre : repos long."
                        FonctionLieu.GUERISSEUR -> "⚕️ Se faire soigner$prix" to "Points de vie au maximum, poison guéri."
                        FonctionLieu.TAVERNE -> "🍺 Écouter les rumeurs$prix" to "Payez une tournée et tendez l'oreille."
                        FonctionLieu.TRIPOT -> "🎲 Parier$prix" to "${lieu.chancePourcent} % de chances de doubler la mise."
                        FonctionLieu.BIBLIOTHEQUE -> "📚 Faire des recherches$prix" to "Jet d'Investigation (${signe(personnage?.let { skillBonus(it, "Investigation") } ?: 0)}) contre DD ${lieu.dd}."
                        FonctionLieu.ARENE -> "⚔️ Combattre$prix" to "Jet d'Athlétisme (${signe(personnage?.let { skillBonus(it, "Athlétisme") } ?: 0)}) contre DD ${lieu.dd} ; prix du vainqueur : ${lieu.valeur} po."
                        FonctionLieu.ORACLE -> "🔮 Consulter l'oracle$prix" to "L'avenir se dévoile à qui sait écouter."
                        FonctionLieu.THERMES -> "♨️ Prendre un bain$prix" to "Retire un niveau d'épuisement (niveau actuel : ${personnage?.exhaustionLevel ?: 0})."
                        FonctionLieu.ENTRAINEMENT -> "🏋️ Prendre une leçon$prix" to "Gain : ${lieu.valeur} PX."
                        else -> return@Column
                    }
                    Text(explication, style = MaterialTheme.typography.bodySmall)
                    val unique = lieu.fonction in setOf(
                        FonctionLieu.TEMPLE, FonctionLieu.TAVERNE, FonctionLieu.TRIPOT, FonctionLieu.BIBLIOTHEQUE,
                        FonctionLieu.ARENE, FonctionLieu.ORACLE, FonctionLieu.ENTRAINEMENT,
                    )
                    Button(
                        enabled = personnage != null && !(unique && dejaFait),
                        onClick = {
                            val perso = personnage ?: return@Button
                            if (lieu.fonction == FonctionLieu.AUBERGE) {
                                com.jc2.jdrcompagnon.ui.screens.joueur.ReglesRepos.blocageReposLong(
                                    perso, com.jc2.jdrcompagnon.ui.ScenarioClockState.state.value.scenarioMinutes
                                )?.let { resultat = "Repos long impossible : $it"; return@Button }
                            }
                            if (!payer(perso)) return@Button
                            dejaFait = true
                            resultat = when (lieu.fonction) {
                                FonctionLieu.TEMPLE ->
                                    if (Random.nextInt(1, 101) <= lieu.chancePourcent) {
                                        GameState.setHeroicInspiration(perso.id, true)
                                        "✨ Bénédiction ! " + lieu.texte.ifBlank { "Les dieux vous sourient." } + " (Inspiration héroïque obtenue)"
                                    } else "Les dieux restent silencieux."
                                FonctionLieu.AUBERGE -> {
                                    // Une nuit en chambre déclenche un repos long (règles de l'écran Repos).
                                    var resume: String? = null
                                    com.jc2.jdrcompagnon.ui.screens.joueur.effectuerReposLong(contexte, portee, perso) {
                                        if (resume == null) resume = it else resultat = it
                                    }
                                    resume ?: "Bonne nuit de sommeil."
                                }
                                FonctionLieu.GUERISSEUR -> {
                                    GameState.soignerOuGuerirPoison(perso.id, perso.maxHitPoints, retirerPoison = false)
                                    GameState.soignerOuGuerirPoison(perso.id, 0, retirerPoison = true)
                                    "Vous êtes soigné : PV au maximum, poison guéri."
                                }
                                FonctionLieu.TAVERNE ->
                                    lieu.texte.lines().map { it.trim() }.filter { it.isNotEmpty() }.randomOrNull()
                                        ?.let { "On murmure que… $it" } ?: "Rien d'intéressant ce soir."
                                FonctionLieu.TRIPOT ->
                                    if (Random.nextInt(1, 101) <= lieu.chancePourcent) {
                                        GameState.recevoirPc(perso.id, lieu.prixPc * 2)
                                        "🎉 Gagné ! Vous empochez ${Monnaie.libelle(lieu.prixPc * 2)}."
                                    } else "Perdu… la mise est pour la maison."
                                FonctionLieu.BIBLIOTHEQUE -> {
                                    val de = Random.nextInt(1, 21)
                                    val total = de + skillBonus(perso, "Investigation")
                                    if (total >= lieu.dd) {
                                        "📖 Réussite ($de → $total) : " + (lieu.texte.lines().map { it.trim() }.filter { it.isNotEmpty() }.randomOrNull() ?: "vous trouvez un ouvrage utile ; demandez au MJ ce que vous apprenez.")
                                    } else "Échec ($de → $total) : des heures de lecture, sans rien trouver."
                                }
                                FonctionLieu.ARENE -> {
                                    val de = Random.nextInt(1, 21)
                                    val total = de + skillBonus(perso, "Athlétisme")
                                    if (total >= lieu.dd) {
                                        GameState.recevoirPc(perso.id, lieu.valeur * 100)
                                        "🏆 Victoire ($de → $total) ! La foule vous acclame : +${lieu.valeur} po."
                                    } else "Défaite ($de → $total) : vous mordez la poussière."
                                }
                                FonctionLieu.ORACLE ->
                                    lieu.texte.lines().map { it.trim() }.filter { it.isNotEmpty() }.randomOrNull()
                                        ?.let { "🔮 « $it »" } ?: "Les brumes restent opaques…"
                                FonctionLieu.THERMES ->
                                    if (perso.exhaustionLevel > 0) {
                                        GameState.setExhaustionLevel(perso.id, perso.exhaustionLevel - 1)
                                        "Détendu : épuisement ${perso.exhaustionLevel} → ${perso.exhaustionLevel - 1}."
                                    } else "Un bain agréable, mais vous n'étiez pas épuisé."
                                FonctionLieu.ENTRAINEMENT -> {
                                    GameState.addExperience(perso.id, lieu.valeur)
                                    "Leçon terminée : +${lieu.valeur} PX."
                                }
                                else -> null
                            }
                        },
                    ) { Text(bouton) }
                    if (personnage == null) Text("Choisissez un personnage.", style = MaterialTheme.typography.bodySmall)
                }
            }
            resultat?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private fun signe(v: Int) = if (v >= 0) "+$v" else "$v"

/** Montures proposées si le MJ n'en a listé aucune. */
private val MonturesParDefaut = listOf("Cheval de selle", "Poney", "Mule", "Cheval de trait")

/** Écurie : une ligne par monture à louer, au prix de la location du lieu. */
@Composable
private fun Ecurie(lieu: LieuNotable, personnage: Character?, prix: String, onLouer: (String) -> Unit) {
    val montures = lieu.texte.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { MonturesParDefaut }
    Text("Montures disponibles$prix :", style = MaterialTheme.typography.bodySmall)
    montures.forEach { monture ->
        OutlinedButton(onClick = { onLouer(monture) }, enabled = personnage != null, modifier = Modifier.fillMaxWidth()) {
            Text("🐎 Louer : $monture")
        }
    }
    if (personnage == null) Text("Choisissez un personnage.", style = MaterialTheme.typography.bodySmall)
}

/** Banque : coffre du personnage (Character.coffreBanquePc), dépôt (avec frais) et retrait en po. */
@Composable
private fun Banque(lieu: LieuNotable, personnage: Character?, onResultat: (String) -> Unit) {
    if (personnage == null) {
        Text("Choisissez un personnage.", style = MaterialTheme.typography.bodySmall)
        return
    }
    var montant by remember(lieu.id) { mutableStateOf("") }
    val pc = ((montant.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toInt()
    Text("Bourse : ${Monnaie.libelleBourse(personnage)}", style = MaterialTheme.typography.bodySmall)
    Text("Coffre : ${Monnaie.libelle(personnage.coffreBanquePc, zero = "vide")}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    if (lieu.valeur > 0) Text("Frais de dépôt : ${lieu.valeur} %", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = montant,
        onValueChange = { montant = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
        label = { Text("Montant (po)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = pc > 0,
            modifier = Modifier.weight(1f),
            onClick = {
                onResultat(
                    if (GameState.deposerBanque(personnage.id, pc, lieu.valeur)) "🏦 Dépôt de ${Monnaie.libelle(pc)} effectué."
                    else "✖ Bourse insuffisante."
                )
                montant = ""
            },
        ) { Text("Déposer") }
        OutlinedButton(
            enabled = pc > 0,
            modifier = Modifier.weight(1f),
            onClick = {
                onResultat(
                    if (GameState.retirerBanque(personnage.id, pc)) "🏦 Retrait de ${Monnaie.libelle(pc)} effectué."
                    else "✖ Le coffre ne contient pas autant."
                )
                montant = ""
            },
        ) { Text("Retirer") }
    }
}

/** Contrat affiché sur un panneau de quêtes. */
private data class Contrat(
    val id: String,
    val titre: String,
    val endroit: String,
    val description: String,
    val recompenses: String,
    val accepteePar: List<String>,
)

/**
 * Contrats du panneau : quêtes choisies par le MJ et rendues visibles des joueurs. Un appui sur un
 * contrat demande « Voulez-vous accepter la quête ? » ; accepté, il est inscrit pour le personnage
 * (envoyé au MJ en partie réseau, qui en est averti).
 */
@Composable
private fun PanneauQuetes(lieu: LieuNotable, campagneId: String?, personnage: Character?) {
    val quetesReseau by NetworkSessionManager.networkQuests.collectAsState()
    val campagnes by GameState.mjCampaigns.collectAsState()
    var aAccepter by remember { mutableStateOf<Contrat?>(null) }
    val contrats: List<Contrat> = quetesReseau?.filter { it.id in lieu.queteIds && it.status == "EN_COURS" }
        ?.map { Contrat(it.id, it.title, it.location, it.description, it.rewards.joinToString(", "), it.accepteePar) }
        ?: campagnes.firstOrNull { it.id == campagneId }?.quests.orEmpty()
            .filter { it.id in lieu.queteIds && it.visibleToPlayers }
            .map { q ->
                Contrat(
                    q.id, q.title, q.location, q.description,
                    if (q.rewardsVisibleToPlayers) q.rewards.joinToString(", ") { it.resume } else "",
                    q.accepteePar,
                )
            }
    if (contrats.isEmpty()) {
        Text("Aucun contrat affiché pour le moment.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Text("Touchez un contrat pour l'accepter.", style = MaterialTheme.typography.bodySmall)
    contrats.forEach { c ->
        val dejaAcceptee = personnage != null && c.accepteePar.any { it.equals(personnage.name, ignoreCase = true) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = if (dejaAcceptee) 0.12f else 0.06f),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth().clickable(enabled = personnage != null && !dejaAcceptee) { aAccepter = c },
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("📜 ${c.titre}", fontWeight = FontWeight.Bold)
                if (c.endroit.isNotBlank()) Text("📍 ${c.endroit}", style = MaterialTheme.typography.bodySmall)
                if (c.description.isNotBlank()) Text(c.description, style = MaterialTheme.typography.bodySmall)
                if (c.recompenses.isNotBlank()) Text("Récompense : ${c.recompenses}", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
                if (c.accepteePar.isNotEmpty()) {
                    Text(
                        "✔ Acceptée par ${c.accepteePar.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    if (personnage == null) Text("Choisissez un personnage pour accepter un contrat.", style = MaterialTheme.typography.bodySmall)

    aAccepter?.let { c ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { aAccepter = null },
            title = { Text("Accepter la quête ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Voulez-vous accepter la quête « ${c.titre} » ?", fontWeight = FontWeight.Bold)
                    if (c.description.isNotBlank()) Text(c.description, style = MaterialTheme.typography.bodySmall)
                    if (c.recompenses.isNotBlank()) Text("Récompense : ${c.recompenses}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    personnage?.let { NetworkSessionManager.accepterQuete(c.id, it.id, it.name) }
                    aAccepter = null
                }) { Text("Accepter") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { aAccepter = null }) { Text("Refuser") } },
        )
    }
}
