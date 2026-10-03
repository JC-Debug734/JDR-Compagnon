package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.bornesFormuleDes
import com.jc2.jdrcompagnon.feature_combat.domain.model.pvSelonTranche
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_group.domain.usecase.EvaluateEncounterDifficultyUseCase
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.monsterChallenge
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Un groupe de monstres identiques, avec son profil (null si introuvable dans le bestiaire),
 * sa quantité prévue par le scénario ([quantiteScenario], base de l'ajustement à la difficulté)
 * et l'XP d'un exemplaire (0 si inconnue : ce groupe n'est alors pas ajusté).
 */
private data class GroupeMonstres(
    val nom: String,
    val quantite: Int,
    val profil: ProfilCombatMonstre?,
    val quantiteScenario: Int = quantite,
    val xp: Int = 0,
)

/** Un participant du côté des personnages : fiche (PJ/PNJ) ou joueur sans fiche d'un groupe. */
private data class Participant(
    val cle: String,
    val nom: String,
    val niveau: Int,
    val detail: String,
    val versCombattant: () -> Combattant,
)

private val evaluerRencontre = EvaluateEncounterDifficultyUseCase()

/**
 * Quantités de monstres visant la [difficulte] pour ce groupe de niveaux. Toutes les
 * combinaisons de quantités sont essayées, un type de monstre pouvant tomber à 0 s'il est trop
 * fort (ex. une succube face à des niveaux 3 en "Facile" : on la retire et on ajoute des
 * squelettes). Critères, dans l'ordre :
 * 1. l'XP ajustée tombe dans la tranche de la difficulté (entre son seuil et le suivant) ;
 * 2. au plus près du milieu de cette tranche ;
 * 3. à égalité, au plus près des quantités du scénario.
 * Les groupes sans XP connue gardent leur quantité ; il reste toujours au moins un monstre.
 */
private fun ajusterALaDifficulte(
    groupes: List<GroupeMonstres>,
    niveaux: List<Int>,
    difficulte: EncounterDifficulty,
): List<GroupeMonstres> {
    val ajustables = groupes.indices.filter { groupes[it].xp > 0 }
    if (niveaux.isEmpty() || ajustables.isEmpty()) return groupes
    val budgets = evaluerRencontre(niveaux, emptyList()).budgets
    val seuil = budgets.getValue(difficulte)
    val suivant = EncounterDifficulty.entries.getOrNull(difficulte.ordinal + 1)?.let { budgets.getValue(it) }
    val plafond = suivant ?: (seuil * 1.5).roundToInt()
    val cible = (seuil + plafond) / 2.0

    // Quantité max par type : de quoi atteindre le plafond à lui seul (multiplicateur de
    // nombre compris), bornée pour garder la recherche rapide (≤ ~20 000 combinaisons).
    val maxParType = when (ajustables.size) {
        1 -> 50; 2 -> 30; 3 -> 20; 4 -> 11; else -> 6
    }
    val bornes = ajustables.map { i ->
        (plafond / groupes[i].xp + 1).coerceIn(1, maxParType)
    }
    val fixes = groupes.indices.filter { it !in ajustables }
    val xpFixe = fixes.flatMap { i -> List(groupes[i].quantite) { groupes[i].xp } }
    val monstresFixes = fixes.sumOf { groupes[it].quantite }

    var meilleure: IntArray? = null
    var meilleurScore = Double.MAX_VALUE
    val courante = IntArray(ajustables.size)
    fun explorer(k: Int) {
        if (k == ajustables.size) {
            if (courante.sum() + monstresFixes == 0) return
            val xp = xpFixe + ajustables.indices.flatMap { j -> List(courante[j]) { groupes[ajustables[j]].xp } }
            val ajustee = evaluerRencontre(niveaux, xp).adjustedMonsterXp
            val horsTranche = if (ajustee < seuil || ajustee >= plafond) 1_000_000.0 else 0.0
            val ecartScenario = ajustables.indices.sumOf { j -> abs(courante[j] - groupes[ajustables[j]].quantiteScenario) }
            val score = horsTranche + abs(ajustee - cible) / cible * 1000 + ecartScenario
            if (score < meilleurScore) {
                meilleurScore = score
                meilleure = courante.copyOf()
            }
            return
        }
        for (n in 0..bornes[k]) {
            courante[k] = n
            explorer(k + 1)
        }
    }
    explorer(0)

    val resultat = meilleure ?: return groupes
    return groupes.mapIndexed { i, g ->
        val j = ajustables.indexOf(i)
        if (j >= 0) g.copy(quantite = resultat[j]) else g
    }
}

/**
 * Lien de scénario #combat:[Gobelin x3, Loup x2] : prépare le combat (difficulté, quantités
 * ajustables, participants, PV des monstres) puis le démarre dans [CombatSession]. Si un combat
 * est déjà en cours, propose d'abord de le reprendre.
 *
 * Participants : uniquement le groupe sélectionné (outil GROUPES), tous ses membres cochés par
 * défaut — fiches et joueurs sans fiche. Sans groupe sélectionné : les PJ du monde, les
 * personnages connectés en réseau étant cochés d'office.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LancerCombatDialog(
    composition: String,
    worldId: String?,
    onDismiss: () -> Unit,
    onLance: () -> Unit,
    // Difficulté présélectionnée (quantités ajustées au niveau des participants dès l'ouverture) ;
    // null = quantités prévues telles quelles ("Scénario").
    difficulteInitiale: EncounterDifficulty? = null,
) {
    val context = LocalContext.current
    val combatActif by CombatSession.etat.collectAsState()
    var ignorerCombatActif by remember { mutableStateOf(false) }

    val actif = combatActif
    if (actif != null && !ignorerCombatActif) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Combat en cours", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "« ${actif.titre} » est déjà en cours" +
                        (if (actif.demarre) " (round ${actif.round})." else " (en préparation).") +
                        " Le reprendre, ou le remplacer par ce nouveau combat ?"
                )
            },
            confirmButton = { TextButton(onClick = onLance) { Text("Reprendre") } },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    TextButton(onClick = { ignorerCombatActif = true }) { Text("Nouveau combat") }
                }
            }
        )
        return
    }

    var chargement by remember(composition) { mutableStateOf(true) }
    var groupes by remember(composition) { mutableStateOf(listOf<GroupeMonstres>()) }
    LaunchedEffect(composition, worldId) {
        chargement = true
        groupes = CompositionCombat.parser(composition).map { ligne ->
            val fiche = SrdRepository.getMonsterByName(context, ligne.monstreNom, worldId)
            // Pas au bestiaire : PNJ / créature du scénario, pris depuis sa fiche personnage.
            val perso = if (fiche == null) {
                GameState.characters.value.firstOrNull { it.type != "PJ" && it.name.equals(ligne.monstreNom.trim(), ignoreCase = true) }
            } else null
            val profil = fiche?.let { ProfilCombatMonstre.depuisFiche(it.rawMarkdown) }
                ?: perso?.let { ProfilCombatMonstre(it.armorClass, it.maxHitPoints.coerceAtLeast(1), null, Math.floorDiv(it.dexterity - 10, 2)) }
            val xp = fiche?.let { monsterChallenge(it)?.second?.filter(Char::isDigit)?.toIntOrNull() } ?: 0
            GroupeMonstres(fiche?.name ?: perso?.name ?: ligne.monstreNom, ligne.quantite, profil, xp = xp)
        }
        chargement = false
    }

    val personnages by GameState.characters.collectAsState()
    val groupesMj by GameState.mjGroups.collectAsState()
    val groupeId by GameState.currentGroupId.collectAsState()
    val mondeCourant = worldId ?: GameState.currentWorldId()
    val groupe = groupesMj.firstOrNull { it.id == groupeId }
    val connectes = remember { NetworkSessionManager.connectedCharacters() }

    val participants: List<Participant> = remember(personnages, groupe, connectes) {
        val fiches = if (groupe != null) {
            personnages.filter { it.id in groupe.memberIds && (it.type == "PJ" || it.type == "PNJ") }
        } else {
            (connectes + personnages.filter { it.type == "PJ" && (mondeCourant == null || it.worldId.isBlank() || it.worldId == mondeCourant) })
                .distinctBy { it.id }
        }
        fiches.map { perso ->
            Participant(
                cle = perso.id,
                nom = perso.name,
                niveau = perso.level,
                detail = "niv. ${perso.level}, CA ${perso.armorClass}, PV ${perso.currentHitPoints}/${perso.maxHitPoints}" +
                    if (connectes.any { it.id == perso.id }) " (connecté)" else "",
                versCombattant = { CombatSession.combattantDepuisPersonnage(perso) },
            )
        } + groupe?.tablePlayers.orEmpty().map { joueur ->
            Participant(
                cle = "table:${joueur.id}",
                nom = joueur.name,
                niveau = joueur.level,
                detail = "niv. ${joueur.level}, CA ${joueur.armorClass}, PV ${joueur.maxHitPoints} (sans fiche)",
                // Sans fiche : PV suivis dans le combat seulement (aucune fiche à mettre à jour).
                versCombattant = {
                    Combattant(
                        id = UUID.randomUUID().toString(),
                        nom = joueur.name,
                        estMonstre = false,
                        ca = joueur.armorClass,
                        pvMax = joueur.maxHitPoints,
                        pv = joueur.maxHitPoints,
                        bonusInitiative = 0
                    )
                },
            )
        }
    }
    // Tout le groupe coché par défaut ; sans groupe, seuls les personnages connectés.
    val selection = remember(participants) {
        mutableStateMapOf<String, Boolean>().apply {
            participants.forEach { p -> put(p.cle, groupe != null || connectes.any { it.id == p.cle }) }
        }
    }
    val niveauxSelectionnes = participants.filter { selection[it.cle] == true }.map { it.niveau }

    // null = quantités du scénario telles quelles.
    var difficulte by remember { mutableStateOf(difficulteInitiale) }
    // Sur "Scénario", cocher/décocher un personnage ne touche pas aux quantités (éventuellement
    // retouchées à la main) : seule une difficulté choisie se recalcule avec les participants.
    LaunchedEffect(difficulte, niveauxSelectionnes.takeIf { difficulte != null }, chargement) {
        if (chargement) return@LaunchedEffect
        val cible = difficulte
        val base = groupes
        groupes = if (cible == null) base.map { it.copy(quantite = it.quantiteScenario) }
        else withContext(Dispatchers.Default) { ajusterALaDifficulte(base, niveauxSelectionnes, cible) }
    }
    val evaluation = remember(groupes, niveauxSelectionnes) {
        if (niveauxSelectionnes.isEmpty() || groupes.none { it.xp > 0 }) null
        else evaluerRencontre(niveauxSelectionnes, groupes.flatMap { g -> List(g.quantite) { g.xp } })
    }

    // PV d'un monstre : fixés par la difficulté choisie (Facile = minimum de la formule, Mortelle
    // = maximum, les tranches intermédiaires entre les deux) ; "Scénario" = PV moyens du bestiaire.
    fun pvPour(profil: ProfilCombatMonstre): Int {
        val d = difficulte ?: return profil.pvMoyens
        return profil.formulePv?.let { pvSelonTranche(it, d.ordinal, EncounterDifficulty.entries.size) } ?: profil.pvMoyens
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚔ Lancer le combat", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Difficulté", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = difficulte == null,
                        onClick = { difficulte = null },
                        label = { Text("Scénario") }
                    )
                    EncounterDifficulty.entries.forEach { d ->
                        FilterChip(
                            selected = difficulte == d,
                            onClick = { difficulte = d },
                            label = { Text(d.label) },
                            enabled = niveauxSelectionnes.isNotEmpty() && groupes.any { it.xp > 0 }
                        )
                    }
                }
                Text(
                    when {
                        niveauxSelectionnes.isEmpty() -> "Cochez des personnages pour ajuster la difficulté."
                        groupes.none { it.xp > 0 } -> "XP des adversaires inconnue : quantités du scénario."
                        evaluation == null -> ""
                        else -> "XP ajustée ${evaluation.adjustedMonsterXp} → rencontre " +
                            (evaluation.resultingDifficulty?.label?.lowercase() ?: "très facile") +
                            " pour ${niveauxSelectionnes.size} personnage(s)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()
                Text("Adversaires", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (chargement) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (groupes.isEmpty()) {
                    Text("Aucun monstre dans ce combat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                groupes.forEachIndexed { index, g ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(g.nom, fontWeight = FontWeight.SemiBold)
                            val profil = g.profil
                            if (profil != null) {
                                Text(
                                    "CA ${profil.ca} · PV ${pvPour(profil)}" +
                                        (profil.formulePv?.let { f -> bornesFormuleDes(f)?.let { " ($f : ${it.first}–${it.last})" } ?: " ($f)" } ?: "") +
                                        " · Init ${signe(profil.bonusInitiative)}" +
                                        if (g.xp > 0) " · ${g.xp} XP" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 4.dp))
                                    Text("Introuvable (bestiaire et fiches) : valeurs par défaut", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        IconButton(onClick = {
                            groupes = groupes.mapIndexed { i, x -> if (i == index) x.copy(quantite = (x.quantite - 1).coerceAtLeast(0)) else x }
                        }) { Icon(Icons.Default.Remove, contentDescription = "Un de moins") }
                        Text("${g.quantite}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            groupes = groupes.mapIndexed { i, x -> if (i == index) x.copy(quantite = (x.quantite + 1).coerceAtMost(50)) else x }
                        }) { Icon(Icons.Default.Add, contentDescription = "Un de plus") }
                    }
                }
                Text(
                    "PV des adversaires selon la difficulté : Facile = minimum, Mortelle = maximum" +
                        " (Scénario = moyenne du bestiaire).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()
                Text(
                    groupe?.let { "Groupe : ${it.name}" } ?: "Personnages",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (groupe == null) {
                    Text(
                        "Aucun groupe sélectionné (outil GROUPES) : PJ du monde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (participants.isEmpty()) {
                    Text("Aucun personnage : vous pourrez ajouter des combattants plus tard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                participants.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selection[p.cle] = selection[p.cle] != true }
                    ) {
                        Checkbox(checked = selection[p.cle] == true, onCheckedChange = { selection[p.cle] = it })
                        Text("${p.nom} — ${p.detail}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !chargement && (groupes.any { it.quantite > 0 } || selection.values.any { it }),
                onClick = {
                    val monstres = groupes.filter { it.quantite > 0 }.flatMap { g ->
                        val profil = g.profil ?: ProfilCombatMonstre(10, 10, null, 0)
                        (1..g.quantite).map { numero ->
                            val pv = pvPour(profil)
                            Combattant(
                                id = UUID.randomUUID().toString(),
                                nom = if (g.quantite > 1) "${g.nom} $numero" else g.nom,
                                estMonstre = true,
                                monstreNom = g.nom,
                                ca = profil.ca,
                                pvMax = pv,
                                pv = pv,
                                bonusInitiative = profil.bonusInitiative,
                                attaques = profil.actions.attaques,
                                nbAttaquesMultiples = profil.actions.nbAttaquesMultiples,
                                profilIA = profil.comportement.profil,
                                raisonProfil = profil.comportement.raison
                            )
                        }
                    }
                    val personnagesCombat = participants.filter { selection[it.cle] == true }.map { it.versCombattant() }
                    CombatSession.demarrer(composition, personnagesCombat + monstres)
                    onLance()
                }
            ) { Text("Préparer le combat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

internal fun signe(valeur: Int): String = if (valeur >= 0) "+$valeur" else "$valeur"
