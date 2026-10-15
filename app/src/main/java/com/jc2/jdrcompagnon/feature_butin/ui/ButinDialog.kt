package com.jc2.jdrcompagnon.feature_butin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_butin.domain.Butin
import com.jc2.jdrcompagnon.feature_butin.domain.GenerateurButin
import com.jc2.jdrcompagnon.feature_butin.domain.ObjetButin
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.SendItemToCharacterDialog
import com.jc2.jdrcompagnon.ui.components.SendItemsToGroupDialog
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Butins déjà tirés pendant la session (clé : « combat:<id du combattant> », « bestiaire:<nom> »…),
 * avec ce qui a déjà été remis aux joueurs : rouvrir la fouille d'un même vaincu montre le même
 * butin, sans permettre de distribuer deux fois la même chose. En mémoire seulement.
 */
object ButinsGeneres {
    data class Entree(val butin: Butin, val objetsRemis: Set<Int> = emptySet(), val bourseRemise: Boolean = false)

    private val _entrees = MutableStateFlow<Map<String, Entree>>(emptyMap())
    val entrees: StateFlow<Map<String, Entree>> = _entrees.asStateFlow()

    fun enregistrer(cle: String, butin: Butin) = _entrees.update { it + (cle to Entree(butin)) }
    fun marquerObjet(cle: String, index: Int) = _entrees.update { m ->
        m[cle]?.let { m + (cle to it.copy(objetsRemis = it.objetsRemis + index)) } ?: m
    }
    fun marquerBourse(cle: String) = _entrees.update { m ->
        m[cle]?.let { m + (cle to it.copy(bourseRemise = true)) } ?: m
    }
}

/** Graine stable tirée de la clé : même vaincu → même butin. */
internal fun graine(cle: String, tirage: Int = 0) = Random((cle.hashCode().toLong() shl 8) + tirage)

/**
 * Butin d'un monstre du bestiaire (fiche lue par son nom), à fouiller et distribuer. [cle] identifie
 * ce monstre précis (un combattant, un lien de scénario...).
 */
@Composable
fun ButinMonstreDialog(nom: String, cle: String, worldId: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entrees by ButinsGeneres.entrees.collectAsState()
    var introuvable by remember(cle) { mutableStateOf(false) }
    var fiche by remember(cle) { mutableStateOf<com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry?>(null) }
    LaunchedEffect(cle) {
        val entry = SrdRepository.getMonsterByName(context, nom, worldId)
        fiche = entry
        if (entry == null) introuvable = true
        if (entrees[cle] == null) {
            ButinsGeneres.enregistrer(cle, genererMonstre(nom, entry, graine(cle)))
        }
    }
    var tirage by remember(cle) { mutableStateOf(0) }
    ButinDialog(
        titre = "Butin — $nom",
        sousTitre = if (introuvable) "Fiche introuvable dans le bestiaire : butin générique." else null,
        cle = cle,
        worldId = worldId,
        onRelancer = {
            tirage++
            ButinsGeneres.enregistrer(cle, genererMonstre(nom, fiche, graine(cle, tirage)))
        },
        onDismiss = onDismiss,
    )
}

internal fun genererMonstre(nom: String, fiche: com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry?, random: Random): Butin =
    GenerateurButin.pourMonstre(
        nom = nom,
        type = fiche?.fields?.get("Type"),
        fp = fiche?.fields?.get("FP"),
        taille = fiche?.fields?.get("Taille"),
        equipement = fiche?.fields?.get("Équipement"),
        random = random,
    )

/**
 * Possessions d'un PNJ (ou personnage ennemi) : ce qu'il porte réellement, plus quelques objets de
 * poche. Donner un objet qu'il possède le retire de sa fiche (échange, vol, dépouille).
 */
@Composable
fun ButinPersonnageDialog(character: Character, cle: String = "pnj:${character.worldId}:${character.name}", onDismiss: () -> Unit) {
    val entrees by ButinsGeneres.entrees.collectAsState()
    LaunchedEffect(cle) {
        if (entrees[cle] == null) ButinsGeneres.enregistrer(cle, GenerateurButin.pourPersonnage(character, graine(cle)))
    }
    ButinDialog(
        titre = "Possessions — ${character.name}",
        sousTitre = "À échanger, voler ou récupérer : un objet qu'il porte quitte sa fiche une fois donné.",
        cle = cle,
        worldId = character.worldId,
        pnjId = character.id,
        onRelancer = {
            // Recalcule depuis la fiche à jour (objets déjà donnés partis), nouvelles trouvailles de poche.
            val maj = GameState.characters.value.find { it.id == character.id } ?: character
            ButinsGeneres.enregistrer(cle, GenerateurButin.pourPersonnage(maj, Random.Default))
        },
        onDismiss = onDismiss,
    )
}

/**
 * Butin ou trésor écrit dans un scénario (lien `#butin:[...]`, voir ButinTexte) : ses pièces et
 * objets se donnent aux joueurs comme un butin tiré. [cle] identifie ce trésor précis (scénario,
 * scène, texte) pour ne pas le distribuer deux fois pendant la session.
 */
@Composable
fun ButinScenarioDialog(contenu: String, cle: String, worldId: String?, onDismiss: () -> Unit) {
    val entrees by ButinsGeneres.entrees.collectAsState()
    LaunchedEffect(cle) {
        if (entrees[cle] == null) ButinsGeneres.enregistrer(cle, com.jc2.jdrcompagnon.feature_butin.domain.ButinTexte.lire(contenu))
    }
    ButinDialog(
        titre = "Trésor",
        sousTitre = "Touchez « Donner » ou les icônes pour remettre chaque élément aux joueurs.",
        cle = cle,
        worldId = worldId,
        onRelancer = null,
        onDismiss = onDismiss,
    )
}

/**
 * Affichage d'un butin enregistré sous [cle] : pièces et objets, chacun envoyable à un
 * personnage ou proposé à un groupe (mêmes dialogues que le loot des tables aléatoires).
 */
@Composable
private fun ButinDialog(
    titre: String,
    sousTitre: String?,
    cle: String,
    worldId: String?,
    pnjId: String? = null,
    onRelancer: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val entrees by ButinsGeneres.entrees.collectAsState()
    val entree = entrees[cle]
    var objetPourPersonnage by remember { mutableStateOf<Pair<Int, ObjetButin>?>(null) }
    var objetsPourGroupe by remember { mutableStateOf<List<Pair<Int, ObjetButin>>?>(null) }
    var donnerPieces by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sousTitre?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)) }
                if (entree == null) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    return@Column
                }
                val butin = entree.butin
                if (butin.vide) {
                    Text("Rien de valeur.", color = Color.White.copy(alpha = 0.8f))
                }
                if (!butin.bourse.vide) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "💰 ${butin.bourse.libelle()}",
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (entree.bourseRemise) TextDecoration.LineThrough else null,
                            )
                            Text(
                                if (entree.bourseRemise) "Remise" else "≈ ${com.jc2.jdrcompagnon.ui.Monnaie.libelle(butin.bourse.valeurPc)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        if (!entree.bourseRemise) {
                            TextButton(onClick = { donnerPieces = true }) { Text("Donner") }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                butin.objets.forEachIndexed { index, objet ->
                    val remis = index in entree.objetsRemis
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                objet.libelle,
                                textDecoration = if (remis) TextDecoration.LineThrough else null,
                                fontWeight = if (objet.possede) FontWeight.Bold else FontWeight.Normal,
                            )
                            val details = listOfNotNull(
                                objet.note.takeIf { it.isNotBlank() },
                                "porté".takeIf { objet.possede },
                                "remis".takeIf { remis },
                            ).joinToString(" · ")
                            if (details.isNotBlank()) {
                                Text(details, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                            }
                        }
                        if (!remis) {
                            IconButton(onClick = { objetPourPersonnage = index to objet }) {
                                Icon(Icons.Default.Person, contentDescription = "Envoyer « ${objet.libelle} » à un personnage")
                            }
                            IconButton(onClick = { objetsPourGroupe = listOf(index to objet) }) {
                                Icon(Icons.Default.Group, contentDescription = "Proposer « ${objet.libelle} » à un groupe")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        dismissButton = {
            Row {
                if (onRelancer != null && entree != null && entree.objetsRemis.isEmpty() && !entree.bourseRemise) {
                    TextButton(onClick = onRelancer) {
                        Icon(Icons.Default.Casino, contentDescription = null)
                        Text(" Relancer")
                    }
                }
                val restants = entree?.butin?.objets?.withIndex()?.filter { it.index !in entree.objetsRemis }.orEmpty()
                if (restants.size > 1) {
                    TextButton(onClick = { objetsPourGroupe = restants.map { it.index to it.value } }) { Text("Tout au groupe") }
                }
            }
        },
    )

    fun retirerDuPnj(objet: ObjetButin) = retirerObjetDuPnj(pnjId, objet)

    objetPourPersonnage?.let { (index, objet) ->
        SendItemToCharacterDialog(
            itemName = objet.libelle,
            worldId = worldId,
            onDismiss = { objetPourPersonnage = null },
            onSent = {
                ButinsGeneres.marquerObjet(cle, index)
                retirerDuPnj(objet)
            },
        )
    }
    objetsPourGroupe?.let { liste ->
        SendItemsToGroupDialog(
            itemNames = liste.map { it.second.libelle },
            worldId = worldId,
            onDismiss = { objetsPourGroupe = null },
            onSent = {
                liste.forEach { (index, objet) ->
                    ButinsGeneres.marquerObjet(cle, index)
                    retirerDuPnj(objet)
                }
            },
        )
    }
    if (donnerPieces && entree != null) {
        DonnerPiecesDialog(
            bourse = entree.butin.bourse,
            worldId = worldId,
            onDismiss = { donnerPieces = false },
            onDonne = {
                ButinsGeneres.marquerBourse(cle)
                if (pnjId != null) GameState.payer(pnjId, entree.butin.bourse.valeurPc)
            },
        )
    }
}

/** Un objet que le PNJ [pnjId] portait réellement quitte sa fiche une fois donné. */
internal fun retirerObjetDuPnj(pnjId: String?, objet: ObjetButin) {
    if (pnjId == null || !objet.possede) return
    repeat(objet.quantite) { GameState.removeItemCompletely(pnjId, objet.nom) }
    // Armes et armures listées à part sur les fiches de PNJ.
    val pnj = GameState.characters.value.find { it.id == pnjId } ?: return
    GameState.updateCharacter(pnj.copy(weapons = pnj.weapons - objet.nom, armor = pnj.armor - objet.nom))
}

/**
 * Pièces données à un personnage (sa bourse les reçoit telles quelles, toutes sortes de pièces) ou
 * versées au trésor commun d'un groupe (compté en po).
 */
@Composable
private fun DonnerPiecesDialog(
    bourse: com.jc2.jdrcompagnon.feature_butin.domain.Bourse,
    worldId: String?,
    onDismiss: () -> Unit,
    onDonne: () -> Unit,
) {
    val characters by GameState.characters.collectAsState()
    val groups by GameState.mjGroups.collectAsState()
    val pj = characters.filter { it.worldId == worldId && it.type == "PJ" }
    val groupes = groups.filter { it.worldId == worldId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Donner ${bourse.libelle()}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (groupes.isNotEmpty() && bourse.valeurPc % 100 != 0) {
                    Text("Le trésor d'un groupe compte en po : versé, il reçoit ${bourse.valeurPo} po (menue monnaie perdue).", style = MaterialTheme.typography.bodySmall)
                }
                if (groupes.isNotEmpty()) Text("Trésor d'un groupe", style = MaterialTheme.typography.labelLarge)
                groupes.forEach { groupe ->
                    ListItem(
                        headlineContent = { Text(groupe.name) },
                        supportingContent = { Text("Trésor : ${groupe.gold} po") },
                        modifier = Modifier.clickable {
                            GameState.updateMjGroup(groupe.copy(gold = groupe.gold + bourse.valeurPo))
                            onDonne()
                            onDismiss()
                        },
                    )
                }
                if (pj.isNotEmpty()) Text("Bourse d'un personnage", style = MaterialTheme.typography.labelLarge)
                pj.forEach { c ->
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(com.jc2.jdrcompagnon.ui.Monnaie.libelleBourse(c)) },
                        modifier = Modifier.clickable {
                            GameState.ajouterPieces(c.id, pp = bourse.pp, po = bourse.po, pe = bourse.pe, pa = bourse.pa, pc = bourse.pc)
                            onDonne()
                            onDismiss()
                        },
                    )
                }
                if (pj.isEmpty() && groupes.isEmpty()) Text("Aucun personnage ni groupe dans ce monde.")
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
