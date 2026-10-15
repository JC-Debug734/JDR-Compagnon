package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import kotlin.random.Random

/**
 * Personnalité tirée au hasard pour improviser un PNJ dont la fiche ne dit rien (ou qui n'a même
 * pas de fiche : simple nom cité dans un scénario). Inspirée des tables de PNJ du Guide du maître.
 */
data class PersonnaliteImprovisee(
    val maniere: String,
    val trait: String,
    val desir: String,
    val peur: String,
    val secret: String,
    val attitude: GameState.AttitudePnj,
) {
    companion object {
        private val manieres = listOf(
            "Parle très vite, saute d'une idée à l'autre", "Chuchote comme si on l'écoutait", "Ponctue chaque phrase d'un proverbe",
            "Voix tonitruante, rit fort", "Bégaie quand il est nerveux", "Vouvoie tout le monde avec emphase",
            "Répond aux questions par des questions", "Ne regarde jamais dans les yeux", "Accent d'une région lointaine",
            "Mâchonne quelque chose en permanence", "Utilise des mots savants, souvent de travers", "Silences longs avant de répondre",
        )
        private val traits = listOf(
            "méfiant", "cupide", "généreux", "curieux", "lâche", "fier", "bavard", "pieux", "cynique",
            "naïf", "colérique", "mélancolique", "honnête jusqu'à la maladresse", "flatteur",
        )
        private val desirs = listOf(
            "rembourser une dette qui l'étrangle", "protéger sa famille", "obtenir la reconnaissance de ses pairs",
            "quitter cette ville au plus vite", "se venger d'un ancien associé", "gagner assez pour ouvrir sa boutique",
            "retrouver un proche disparu", "être laissé tranquille", "découvrir la vérité sur une rumeur",
            "plaire à quelqu'un d'influent",
        )
        private val peurs = listOf(
            "la garde et les autorités", "la magie qu'il ne comprend pas", "perdre son gagne-pain", "les morts et les esprits",
            "être ridiculisé en public", "les étrangers armés", "que son secret soit révélé", "la maladie",
            "les représailles d'une guilde", "l'eau profonde",
        )
        private val secrets = listOf(
            "il informe en douce une bande de voleurs", "il a vu quelque chose la nuit du crime", "il n'est pas celui qu'il prétend être",
            "il cache un objet volé sous son plancher", "il doit de l'argent à la mauvaise personne", "il sait où se trouve l'entrée cachée",
            "il est lié à un culte, à contrecœur", "il a trahi un ami autrefois", "il n'a aucun secret… et s'en vexe",
            "il connaît le vrai nom du méchant",
        )

        fun tirer(random: Random = Random.Default): PersonnaliteImprovisee = PersonnaliteImprovisee(
            maniere = manieres.random(random),
            trait = traits.random(random),
            desir = desirs.random(random),
            peur = peurs.random(random),
            secret = secrets.random(random),
            attitude = GameState.AttitudePnj.entries.random(random),
        )
    }
}

/**
 * Infos comportementales d'un PNJ, réservées au MJ : comportement, intentions, objectif,
 * personnalité (traits, idéaux, liens, défauts), secrets et notes MJ de sa fiche. Quand la fiche
 * n'en dit rien, une personnalité improvisée est proposée (relançable, enregistrable sur la fiche
 * si le PNJ existe).
 */
@Composable
fun InfosComportementalesPnj(character: Character) {
    val lignes = listOf(
        "Comportement" to character.comportement,
        "Intentions" to character.intentions,
        "Objectif" to character.objectif,
        "Traits de personnalité" to character.personalityTraits,
        "Idéaux" to character.ideals,
        "Liens" to character.bonds,
        "Défauts" to character.flaws,
        "Secrets" to character.secrets,
        "Notes du MJ" to character.dmNotes,
    ).filter { it.second.isNotBlank() }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎭 Comportement (MJ uniquement)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            lignes.forEach { (titre, texte) ->
                Text(titre, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(texte, style = MaterialTheme.typography.bodyMedium)
            }
            if (character.alignment.isNotBlank()) {
                Text("Alignement : ${character.alignment}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            if (lignes.size < 3) {
                ImprovisationPnj(character)
            }
        }
    }
}

/** Personnalité improvisée (relançable) pour compléter une fiche trop maigre. */
@Composable
private fun ImprovisationPnj(character: Character) {
    var tirage by remember(character.id) { mutableStateOf(0) }
    val personnalite = remember(character.id, tirage) {
        PersonnaliteImprovisee.tirer(Random(character.name.hashCode().toLong() * 31 + tirage))
    }
    val existe = GameState.characters.value.any { it.id == character.id }
    Text(
        "Improvisation",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp),
    )
    Text("🗣 ${personnalite.maniere}.", style = MaterialTheme.typography.bodyMedium)
    Text("Caractère : ${personnalite.trait}.", style = MaterialTheme.typography.bodyMedium)
    Text("Veut : ${personnalite.desir}.", style = MaterialTheme.typography.bodyMedium)
    Text("Craint : ${personnalite.peur}.", style = MaterialTheme.typography.bodyMedium)
    Text("Secret : ${personnalite.secret}.", style = MaterialTheme.typography.bodyMedium)
    Text(
        "Attitude suggérée : ${personnalite.attitude.label} (ND ${personnalite.attitude.nd})",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.8f),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { tirage++ }) { Text("🎲 Autre personnalité", color = Color.White) }
        if (existe) {
            TextButton(onClick = {
                val maj = GameState.characters.value.find { it.id == character.id } ?: return@TextButton
                GameState.updateCharacter(
                    maj.copy(
                        comportement = maj.comportement.ifBlank { "${personnalite.maniere}. Plutôt ${personnalite.trait}." },
                        intentions = maj.intentions.ifBlank { "Craint ${personnalite.peur}." },
                        objectif = maj.objectif.ifBlank { personnalite.desir.replaceFirstChar { it.uppercase() } + "." },
                        secrets = maj.secrets.ifBlank { personnalite.secret.replaceFirstChar { it.uppercase() } + "." },
                    )
                )
            }) { Text("Garder sur la fiche", color = Color.White) }
        }
    }
}

/**
 * PNJ cité par son nom dans un scénario : sa fiche s'il en a une dans le monde, sinon un PNJ
 * éphémère (non enregistré) pour pouvoir improviser la conversation quand même.
 */
fun pnjPourConversation(nom: String, worldId: String?): Character =
    GameState.characters.value.firstOrNull { it.type != "PJ" && it.name.equals(nom, ignoreCase = true) && (worldId == null || it.worldId == worldId) }
        ?: GameState.characters.value.firstOrNull { it.type != "PJ" && it.name.equals(nom, ignoreCase = true) }
        ?: Character(name = nom, type = "PNJ", worldId = worldId.orEmpty())
