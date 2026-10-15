package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Occultiste
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Le personnage a-t-il des aptitudes de protecteur à utilisations limitées ? */
internal fun aAptitudesOccultiste(c: Character): Boolean =
    Occultiste.fouleesFeesMax(c) > 0 || Occultiste.reserveLumiere(c) > 0 || Occultiste.niveauGrandAncien(c) >= 6 ||
        Occultiste.niveauFielon(c) >= 6 || Occultiste.niveauArchifee(c) >= 10 || Occultiste.niveauCeleste(c) >= 14

/**
 * Aptitudes de protecteur de l'Occultiste à utilisations limitées, avec leur compteur :
 * Foulée des fées et Charmantes défenses (Archifée), Lumière guérisseuse et Vengeance aveuglante
 * (Céleste), Combattant clairvoyant (Grand Ancien), Chance du ténébreux et Traversée des enfers
 * (Fiélon). Les effets automatiques (Bénédiction, Chance, Vengeance… en combat) sont appliqués
 * par CombatSession ; ici on dépense et on récupère. [onSoin] applique les soins de Lumière
 * guérisseuse (directement, ou par envoi au MJ en combat) et renvoie le message, null en cas d'échec.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CarteAptitudesOccultiste(
    personnage: Character,
    cibles: List<CibleContact>,
    onSoin: (cible: CibleContact, pv: Int, detail: String) -> String?,
    modifier: Modifier = Modifier,
) {
    if (!aAptitudesOccultiste(personnage)) return
    var message by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aptitudes de protecteur (Occultiste)", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)

            // ── Archifée ──
            Occultiste.fouleesFeesMax(personnage).takeIf { it > 0 }?.let { max ->
                val reste = Occultiste.fouleesFeesRestantes(personnage)
                Ligne(
                    "Foulée des fées : $reste/$max",
                    "Foulée brumeuse sans emplacement, avec un effet : provocatrice (JS Sagesse ou Désavantage contre les autres) ou revigorante (1d10 PV temporaires)" +
                        if (Occultiste.niveauArchifee(personnage) >= 6) ", de disparition (Invisible) ou effroyable (JS Sagesse ou 2d10 psychiques). Aussi en Réaction après avoir subi des dégâts." else ".",
                    bouton = "Utiliser".takeIf { reste > 0 },
                ) {
                    GameState.utiliserFouleeFees(personnage.id)
                    message = "Foulée des fées : foulée brumeuse lancée sans emplacement."
                }
            }
            if (Occultiste.niveauArchifee(personnage) >= 10) {
                UsageUnique(
                    "Charmantes défenses", "Immunité contre l'état Charmé (appliquée en combat). Réaction quand on vous touche : dégâts réduits de moitié, l'assaillant fait un JS de Sagesse ou subit autant de dégâts psychiques.",
                    disponible = Occultiste.charmantesDefensesDisponibles(personnage),
                    onUtiliser = { GameState.aptitudeOccultiste(personnage.id, "charmantes-defenses", true); message = "Charmantes défenses utilisées." },
                    onRecuperer = { GameState.aptitudeOccultiste(personnage.id, "charmantes-defenses", false, parEmplacement = true) },
                )
            }

            // ── Céleste ──
            Occultiste.reserveLumiere(personnage).takeIf { it > 0 }?.let { reserve ->
                val reste = Occultiste.lumiereRestante(personnage)
                val maxDes = minOf(Occultiste.desMaxParSoin(personnage), reste)
                var nbDes by remember { mutableIntStateOf(1) }
                var cibleId by remember { mutableStateOf(cibles.firstOrNull()?.id) }
                val n = nbDes.coerceIn(1, maxDes.coerceAtLeast(1))
                Text("Lumière guérisseuse : $reste/$reserve d6 (action Bonus, 18 m)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cibles.forEach { c -> FilterChip(selected = c.id == cibleId, onClick = { cibleId = c.id }, label = { Text(c.nom) }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { nbDes = (n - 1).coerceAtLeast(1) }, enabled = n > 1) { Text("−") }
                    Text("${n}d6", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { nbDes = (n + 1).coerceAtMost(maxDes) }, enabled = n < maxDes) { Text("+") }
                }
                val cible = cibles.firstOrNull { it.id == cibleId }
                Button(enabled = reste > 0 && cible != null, onClick = {
                    val c = cible ?: return@Button
                    val des = List(n) { (1..6).random() }
                    val resultat = onSoin(c, des.sum(), "Lumière guérisseuse : ${n}d6 (${des.joinToString("+")})")
                    if (resultat != null) GameState.utiliserLumiereGuerisseuse(personnage.id, n)
                    message = resultat ?: "Envoi impossible : connexion au MJ perdue. Rien n'a été dépensé."
                }, modifier = Modifier.fillMaxWidth()) { Text("Soigner " + (cible?.nom ?: "…") + " (${n}d6)") }
            }
            if (Occultiste.niveauCeleste(personnage) >= 14) {
                UsageUnique(
                    "Vengeance aveuglante", "Utilisée automatiquement en combat quand vous ou un allié à 18 m allez faire un jet contre la mort.",
                    disponible = Occultiste.vengeanceDisponible(personnage),
                )
            }

            // ── Grand Ancien ──
            if (Occultiste.niveauGrandAncien(personnage) >= 6) {
                UsageUnique(
                    "Combattant clairvoyant", "En créant un lien d'Esprit éveillé : JS de Sagesse de la créature ; raté, elle a le Désavantage contre vous et vous l'Avantage contre elle pendant le lien.",
                    disponible = Occultiste.clairvoyantDisponible(personnage),
                    onUtiliser = { GameState.aptitudeOccultiste(personnage.id, "combattant-clairvoyant", true); message = "Combattant clairvoyant utilisé." },
                    onRecuperer = { GameState.aptitudeOccultiste(personnage.id, "combattant-clairvoyant", false, parEmplacement = true) },
                )
            }

            // ── Fiélon ──
            if (Occultiste.niveauFielon(personnage) >= 6) {
                val max = Occultiste.chanceRestante(personnage) + personnage.chanceTenebreuxUtilisees
                val reste = Occultiste.chanceRestante(personnage)
                Ligne(
                    "Chance du ténébreux : $reste/$max",
                    "+1d10 à un test ou un JS, après le jet. En combat, utilisée d'office sur un JS raté que le d10 peut sauver.",
                    bouton = "Utiliser (+1d10)".takeIf { reste > 0 },
                ) {
                    val d10 = (1..10).random()
                    GameState.utiliserChanceTenebreux(personnage.id)
                    message = "Chance du ténébreux : +$d10 au jet."
                }
            }
            if (Occultiste.niveauFielon(personnage) >= 14) {
                UsageUnique(
                    "Traversée des enfers", "Option du MJ en combat, après une attaque qui touche.",
                    disponible = Occultiste.traverseeDisponible(personnage),
                    onRecuperer = { GameState.traverseeDesEnfers(personnage.id, utilisee = false, parEmplacement = true) },
                )
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
        }
    }
}


@Composable
private fun Ligne(titre: String, detail: String, bouton: String?, onClick: () -> Unit) {
    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        if (bouton != null) TextButton(onClick = onClick) { Text(bouton) }
    }
}

/** Aptitude utilisable une fois (par repos), avec récupération possible contre un emplacement de pacte. */
@Composable
private fun UsageUnique(
    nom: String,
    detail: String,
    disponible: Boolean,
    onUtiliser: (() -> Unit)? = null,
    onRecuperer: (() -> Unit)? = null,
) {
    Ligne(
        "$nom : ${if (disponible) "disponible" else "utilisée"}",
        detail,
        bouton = when {
            disponible && onUtiliser != null -> "Utiliser"
            !disponible && onRecuperer != null -> "Récupérer (1 empl. de pacte)"
            else -> null
        },
    ) { if (disponible) onUtiliser?.invoke() else onRecuperer?.invoke() }
}
