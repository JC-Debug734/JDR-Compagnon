package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.Guerrier
import com.jc2.jdrcompagnon.feature_combat.domain.model.RessourceClasse
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EffetActif
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Ressources du Guerrier et de sa sous-classe, avec leurs compteurs et leurs actions : Second
 * souffle (soins, Sens tactique), Fougue, Inflexible, dés de Supériorité et manœuvres, Énergie
 * psionique (Frappe psionique, Champ protecteur) et pouvoirs psi, Puissance des géants, Bouclier
 * runique et runes. [onAnnonce] (libellé, texte), [onDegats] (libellé, dés, formule) et [onSoin]
 * (PV, détail) appliquent l'effet — directement hors réseau, ou par envoi au MJ en combat — et
 * renvoient le message à afficher, ou null si l'envoi a échoué (rien n'est alors dépensé).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CarteAptitudesGuerrier(
    personnage: Character,
    onAnnonce: (libelle: String, texte: String) -> String?,
    onDegats: (libelle: String, des: List<Int>, formule: String) -> String?,
    onSoin: (pv: Int, detail: String) -> String?,
    modifier: Modifier = Modifier,
) {
    val ressources = Guerrier.ressources(personnage)
    if (ressources.isEmpty()) return
    var message by remember { mutableStateOf<String?>(null) }
    val niveau = Guerrier.niveau(personnage)
    val modInt = ArsenalPersonnage.mod(personnage.intelligence)
    val superiorite = ressources.firstOrNull { it.cle == Guerrier.SUPERIORITE }
    val energie = ressources.firstOrNull { it.cle == Guerrier.ENERGIE_PSIONIQUE }

    fun lancer(de: String, nombre: Int = 1): List<Int> = List(nombre) { (1..(de.removePrefix("d").toIntOrNull() ?: 6)).random() }

    /** Dépense une utilisation de [r] si [action] réussit (renvoie un message non null). */
    fun depenser(r: RessourceClasse, nombre: Int = 1, action: () -> String?) {
        val resultat = action()
        if (resultat != null) GameState.utiliserCompteur(personnage.id, r.cle, r.max, nombre)
        message = resultat ?: ECHEC_ENVOI
    }

    /** Pouvoir à usage limité récupérable en dépensant un dé de [source] (Supériorité, Énergie psionique). */
    fun recupererParDe(r: RessourceClasse, source: RessourceClasse?) {
        source ?: return
        GameState.utiliserCompteur(personnage.id, source.cle, source.max)
        GameState.recupererCompteur(personnage.id, r.cle)
        message = "${r.nom} récupéré (un dé « ${source.nom} » dépensé)."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aptitudes du Guerrier", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)

            ressources.forEach { r ->
                val reste = Guerrier.restantes(personnage, r)
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Text("${r.nom} : $reste/${r.max}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${r.detail} Récupération : ${r.recuperation.libelle}.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        r.cle == Guerrier.SECOND_SOUFFLE -> {
                            TextButton(enabled = reste > 0, onClick = {
                                val d10 = lancer("d10").single()
                                depenser(r) { onSoin(d10 + niveau, "Second souffle : 1d10 ($d10) + $niveau") }
                            }) { Text("Se soigner (1d10 + $niveau)") }
                            if (niveau >= 2) TextButton(enabled = reste > 0, onClick = {
                                val d10 = lancer("d10").single()
                                depenser(r) { onAnnonce("Sens tactique", "+$d10 (1d10) au test de caractéristique raté") }
                            }) { Text("Sens tactique (+1d10)") }
                        }
                        r.cle == Guerrier.INFLEXIBLE -> TextButton(enabled = reste > 0, onClick = {
                            val d20 = lancer("d20").single()
                            depenser(r) { onAnnonce("Inflexible", "JS relancé : ${d20 + niveau} (d20 $d20 + $niveau, plus vos bonus de sauvegarde)") }
                        }) { Text("Relancer un JS (+$niveau)") }
                        r.cle == Guerrier.SUPERIORITE -> {
                            val manoeuvres = Guerrier.manoeuvresConnues(personnage)
                            var choisie by remember(manoeuvres) { mutableStateOf(manoeuvres.firstOrNull()) }
                            Column {
                                if (manoeuvres.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    manoeuvres.forEach { m -> FilterChip(selected = m == choisie, onClick = { choisie = m }, label = { Text(m) }) }
                                } else Text("Aucune manœuvre notée sur la fiche (à choisir en montant de niveau).", style = MaterialTheme.typography.bodySmall)
                                TextButton(enabled = reste > 0, onClick = {
                                    val de = r.de ?: "d8"
                                    val valeur = lancer(de).single()
                                    val nom = choisie ?: "Manœuvre"
                                    depenser(r) { onAnnonce(nom, "dé de Supériorité 1$de = $valeur (DD ${Guerrier.ddManoeuvres(personnage)})") }
                                }) { Text("Dépenser un dé (1${r.de})") }
                            }
                        }
                        r.cle == Guerrier.ENERGIE_PSIONIQUE -> {
                            val de = r.de ?: "d6"
                            TextButton(enabled = reste > 0, onClick = {
                                val valeur = lancer(de).single()
                                depenser(r) { onDegats("Dégâts (Frappe psionique)", listOf(valeur + modInt), "1$de + $modInt force (Frappe psionique : $valeur + $modInt)") }
                            }) { Text("Frappe psionique (1$de + $modInt)") }
                            TextButton(enabled = reste > 0, onClick = {
                                val valeur = lancer(de).single()
                                val reduction = (valeur + modInt).coerceAtLeast(1)
                                depenser(r) { onAnnonce("Champ protecteur", "dégâts réduits de $reduction (1$de = $valeur + $modInt)") }
                            }) { Text("Champ protecteur") }
                        }
                        r.cle == Guerrier.PUISSANCE_GEANTS -> {
                            val actif = personnage.effetsActifs.any { it.nom == Guerrier.EFFET_PUISSANCE_GEANTS }
                            if (actif) {
                                TextButton(onClick = {
                                    val valeur = lancer(r.de ?: "d6").single()
                                    message = onDegats("Dégâts (Puissance des géants)", listOf(valeur), "1${r.de} (Puissance des géants, une fois par tour)") ?: ECHEC_ENVOI
                                }) { Text("Coup géant (1${r.de})") }
                                TextButton(onClick = {
                                    personnage.effetsActifs.filter { it.nom == Guerrier.EFFET_PUISSANCE_GEANTS }.forEach { GameState.retirerEffet(personnage.id, it.id) }
                                    message = "Puissance des géants terminée."
                                }) { Text("Terminer") }
                            } else TextButton(enabled = reste > 0, onClick = {
                                depenser(r) {
                                    onAnnonce("Puissance des géants", "taille G, Avantage aux tests et JS de Force, +1${r.de} une fois par tour pendant 1 minute")?.also {
                                        val maintenant = ScenarioClockState.totalSeconds()
                                        GameState.ajouterEffet(
                                            personnage.id,
                                            EffetActif(nom = Guerrier.EFFET_PUISSANCE_GEANTS, source = Guerrier.CLASSE, description = "+1${r.de} dégâts une fois par tour",
                                                debutSecondes = maintenant, finSecondes = maintenant + 60),
                                        )
                                    }
                                }
                            }) { Text("Activer (action Bonus)") }
                        }
                        r.cle.startsWith(Guerrier.PREFIXE_RUNE) -> TextButton(enabled = reste > 0, onClick = {
                            depenser(r) {
                                if (r.nom.contains("feu", ignoreCase = true)) {
                                    val des = lancer("d6", 2)
                                    onDegats("Dégâts (${r.nom})", des, "2d6 feu (${r.nom} : JS de Force DD ${Guerrier.ddRunes(personnage)} ou Entravée 1 minute)")
                                } else onAnnonce(r.nom, "invoquée (DD ${Guerrier.ddRunes(personnage)})")
                            }
                        }) { Text("Invoquer") }
                        else -> {
                            TextButton(enabled = reste > 0, onClick = { depenser(r) { onAnnonce(r.nom, "utilisé") } }) { Text("Utiliser") }
                            // Pouvoirs récupérables contre un dé (Observation de l'ennemi, pouvoirs psi).
                            val source = when (r.cle) {
                                Guerrier.OBSERVATION -> superiorite
                                Guerrier.DEPLACEMENT_TELEKINETIQUE, Guerrier.BOND_PSI, Guerrier.REMPART_DE_FORCE, Guerrier.MAITRE_TELEKINETIQUE -> energie
                                else -> null
                            }
                            if (source != null && reste == 0 && Guerrier.restantes(personnage, source) > 0) {
                                TextButton(onClick = { recupererParDe(r, source) }) { Text("Récupérer (1 dé)") }
                            }
                        }
                    }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
        }
    }
}
