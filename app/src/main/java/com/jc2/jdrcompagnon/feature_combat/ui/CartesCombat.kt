package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeEnMain
import com.jc2.jdrcompagnon.feature_combat.domain.model.CapaciteAttaque
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerFormuleDes
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.joueur.itemIcon
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val RougeInterdit = Color(0xFFE57373)

/** Ligne d'état des munitions / exemplaires d'une arme (« 12 flèches », « ×3 »), null sinon. */
internal fun etatMunitions(arme: ArmeEnMain): String? = when {
    arme.munition != null && arme.sansMunition -> "Plus de ${arme.munition} : attaque impossible"
    arme.munition != null -> "${arme.munitionsRestantes} ${arme.munition}"
    arme.lancer && arme.quantite > 1 -> "×${arme.quantite} — une de moins à chaque lancer"
    arme.lancer -> "Arme de lancer — perdue une fois lancée"
    else -> null
}

/**
 * Capacités de combat du personnage, une carte chacune : chaque arme équipée (mains et dos) avec
 * son toucher et ses dégâts, puis chaque capacité d'attaque de classe (Attaque supplémentaire,
 * Rage…). Toucher une arme la choisit ([onArme]) ; toucher une capacité ouvre son détail.
 */
@Composable
internal fun CartesCapacitesCombat(arsenal: ArsenalJoueur, onArme: (ArmeEnMain) -> Unit) {
    var detailCapacite by remember { mutableStateOf<CapaciteAttaque?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grille(arsenal.armes) { arme, modifier ->
            CarteArme(arme, modifier) { onArme(arme) }
        }
        grille(arsenal.capacites) { cap, modifier ->
            CarteCapacite(cap, modifier) { detailCapacite = cap }
        }
    }
    detailCapacite?.let { cap ->
        DetailDialog(cap.nom, onDismiss = { detailCapacite = null }) {
            Text(cap.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Grille de cartes sur deux colonnes, de même hauteur par rangée. */
@Composable
private fun <T> grille(elements: List<T>, carte: @Composable (T, Modifier) -> Unit) {
    elements.chunked(2).forEach { rangee ->
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rangee.forEach { carte(it, Modifier.weight(1f).fillMaxHeight()) }
            if (rangee.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CarteArme(arme: ArmeEnMain, modifier: Modifier, onClick: () -> Unit) {
    val interdite = arme.sansMunition
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = if (interdite) 0.03f else 0.08f),
        contentColor = Color.White,
        modifier = modifier.border(1.dp, if (interdite) RougeInterdit else ForcedDarkPalette.AccentGold.copy(alpha = 0.6f), MaterialTheme.shapes.medium),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(itemIcon(arme.nom), contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(arme.nom, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(arme.main, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Text(
                "${signeTexte(arme.bonusToucher)} · ${arme.formuleDegats}" + (arme.typeDegats?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.titleSmall,
                color = if (interdite) Color.White.copy(alpha = 0.5f) else ForcedDarkPalette.AccentGold,
            )
            etatMunitions(arme)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = if (interdite) RougeInterdit else Color.White)
            }
        }
    }
}

@Composable
private fun CarteCapacite(cap: CapaciteAttaque, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.06f),
        contentColor = Color.White,
        modifier = modifier.border(1.dp, Color.White.copy(alpha = 0.25f), MaterialTheme.shapes.medium),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(cap.nom, fontWeight = FontWeight.Bold)
            }
            Text(cap.detail, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Utilise l'arme hors combat en réseau (écran Actions de Combat) : dépense la munition, ou retire
 * l'exemplaire lancé. Retourne le message à afficher.
 */
internal fun utiliserArme(personnage: Character, arme: ArmeEnMain, lancer: Boolean): String {
    arme.munition?.let { type ->
        return if (GameState.consommerMunition(personnage.id, type)) "${arme.nom} : attaque avec 1 munition (${arme.munitionsRestantes - 1} ${type} restantes)."
        else "Plus de $type : impossible d'attaquer avec ${arme.nom}."
    }
    if (lancer && arme.slot != null) {
        return if (GameState.lancerArme(personnage.id, arme.nom, arme.slot)) "${arme.nom} lancée" +
            (if (arme.quantite > 1) " (${arme.quantite - 1} restante(s))." else " : il n'en reste plus d'équipée.")
        else "${arme.nom} n'est plus équipée."
    }
    return "Attaque avec ${arme.nom}."
}

/** Jet d'attaque d'une arme : d20 + toucher, puis dégâts (dés doublés sur un 20 naturel). */
internal fun jetArme(arme: ArmeEnMain): String {
    val d20 = kotlin.random.Random.nextInt(1, 21)
    val critique = d20 == 20
    val toucher = "Toucher ${d20 + arme.bonusToucher} (d20 $d20 ${signeTexte(arme.bonusToucher)})" + when {
        critique -> " CRITIQUE"
        d20 == 1 -> " échec automatique"
        else -> ""
    }
    val degats = lancerFormuleDes(arme.formuleDegats, critique = critique)
        ?: arme.formuleDegats.trim().toIntOrNull()
    return toucher + " · Dégâts " + (degats?.let { "$it (${arme.formuleDegats}${if (critique) ", dés doublés" else ""})" } ?: arme.formuleDegats) +
        (arme.typeDegats?.let { " $it" } ?: "")
}

/** Détail d'une arme touchée hors combat en réseau, avec l'attaque qui dépense munition ou arme lancée. */
@Composable
internal fun DetailArmeDialog(arsenal: ArsenalJoueur, arme: ArmeEnMain, personnage: Character?, onDismiss: () -> Unit) {
    var message by remember(arme.nom) { mutableStateOf<String?>(null) }
    DetailDialog(arme.nom, onDismiss = onDismiss) {
        Text("${arme.main} · ${signeTexte(arme.bonusToucher)} au toucher · ${arme.formuleDegats}" + (arme.typeDegats?.let { " $it" } ?: ""),
            color = ForcedDarkPalette.AccentGold, fontWeight = FontWeight.Bold)
        arme.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        etatMunitions(arme)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (arme.sansMunition) RougeInterdit else Color.White) }
        val proprietes = arme.proprietes.split(',').map { it.trim() }.filter { it.isNotBlank() }
        (proprietes.map { "Propriété" to it } + listOfNotNull(arme.botte?.let { "Botte" to it })).forEach { (genre, nom) ->
            Text("$genre : $nom", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Text(arsenal.definitions[nom.substringBefore('(').trim()] ?: "Pas de description dans la bibliothèque.", style = MaterialTheme.typography.bodyMedium)
        }
        if (personnage != null) {
            Spacer(Modifier.height(8.dp))
            val auCorpsACorps = arme.lancer && !arme.aDistance
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Le jet n'a lieu que si l'attaque est possible (munition restante, arme encore équipée).
                fun attaquer(lancer: Boolean) {
                    val texte = utiliserArme(personnage, arme, lancer)
                    message = if (texte.startsWith("Plus de") || texte.endsWith("n'est plus équipée.")) texte else texte + "\n🎲 " + jetArme(arme)
                }
                if (arme.munition != null) {
                    Button(enabled = !arme.sansMunition, onClick = { attaquer(lancer = false) }) { Text("Tirer") }
                } else {
                    if (!arme.lancer || auCorpsACorps) Button(onClick = { attaquer(lancer = false) }) { Text("Frapper") }
                    if (arme.lancer) Button(onClick = { attaquer(lancer = true) }) { Text("Lancer") }
                }
            }
            message?.let { Box(Modifier.padding(top = 4.dp)) { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) } }
        }
    }
}
