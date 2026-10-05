package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.formatMetres
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.ModeJet
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.ResultatAttaque
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.JetCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Partie basse d'une carte de combattant côté MJ : action déclarée (joueur, IA ou MJ), jets reçus
 * ce round, et boutons pour déclarer, changer ou résoudre l'action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BlocDeclaration(
    combattant: Combattant,
    actif: Boolean,
    phase: PhaseCombat,
    declaration: DeclarationAction?,
    jets: List<JetCombat>,
    joueurConnecte: Boolean,
    onDeclarer: () -> Unit,
    onResoudre: () -> Unit,
    onRedecider: () -> Unit,
    deplacement: String? = null,
    onImpossible: () -> Unit = {},
    // Tous les combattants (cible des dégâts/soins d'un joueur) et jets supplémentaires accordés.
    combattants: List<Combattant> = emptyList(),
    relances: Int = 0,
) {
    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        if (declaration == null) {
            Text(
                if (joueurConnecte) "⏳ Le joueur choisit son action…" else "⏳ Action à déclarer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val origine = when {
                declaration.parIa -> "IA · "
                declaration.parMj -> "MJ · "
                else -> ""
            }
            Text(
                origine + declaration.libelle + (declaration.cibleNom?.let { " → $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (actif) FontWeight.Bold else FontWeight.Normal
            )
            // Jets déjà lancés à la déclaration : toucher (d20) et dés de dégâts de chaque attaque.
            declaration.resultats?.forEach { r ->
                Text(
                    "🎲 " + texteResultat(r, declaration.attaque),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD778)
                )
            }
            if (declaration.degatsAppliques) {
                Text("✓ appliqué", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD778))
                // États soumis à un JS (« JS Constitution DD 10 ou Paralysé ») : le MJ tranche.
                val cibleTouchee = combattants.firstOrNull { it.id == declaration.cibleId }
                val aTrancher = declaration.attaque?.conditions.orEmpty()
                    .filter { it.surEchecJs && cibleTouchee != null && it.condition !in cibleTouchee.conditions }
                if (cibleTouchee != null && declaration.resultats?.any { it.touche } == true && aTrancher.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        aTrancher.forEach { inflige ->
                            OutlinedButton(onClick = {
                                CombatSession.appliquerCondition(cibleTouchee.id, inflige.condition, "${combattant.nom} — JS ${inflige.sauvegarde ?: ""} DD ${inflige.dd} raté")
                            }) { Text("JS raté : ${cibleTouchee.nom} ${inflige.condition.label}") }
                        }
                    }
                }
            }
            deplacement?.let {
                Text("🏃 $it", style = MaterialTheme.typography.bodySmall)
            }
            // Décision de l'IA : seul son nom est affiché, pas son raisonnement. Arme ou sort d'un
            // joueur : le résumé calculé (bonus, dégâts) n'est pas répété, ses jets le montrent.
            val calcule = declaration.libelle.startsWith("Attaquer : ") || declaration.libelle.startsWith("Lancer : ")
            declaration.detail
                ?.let { if (calcule) it.substringAfter(" — ", "") else it }
                ?.takeIf { !declaration.parIa && it.isNotBlank() }
                ?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
        }
        val cibleDeclaree = combattants.firstOrNull { it.id == declaration?.cibleId }
        jets.forEach { j ->
            // Jet d'attaque comparé à la CA de la cible annoncée (indicatif : le MJ tranche).
            val verdict = if (j.libelle.startsWith("Attaque") && j.des.isNotEmpty() && cibleDeclaree != null) {
                val d20 = j.des.singleOrNull()
                when {
                    j.formule.startsWith("1d20") && d20 == 20 -> " → CRITIQUE sur ${cibleDeclaree.nom}"
                    j.formule.startsWith("1d20") && d20 == 1 -> " → échec automatique"
                    j.total >= cibleDeclaree.ca -> " → touche ${cibleDeclaree.nom} (CA ${cibleDeclaree.ca})"
                    else -> " → rate ${cibleDeclaree.nom} (CA ${cibleDeclaree.ca})"
                }
            } else ""
            val source = if (j.manuel) "vrais dés" else "appli"
            // Jet d'attaque : le d20 pour toucher, puis les dés de dégâts lancés en même temps.
            val titre = if (j.totalDegats != null || j.libelle.startsWith("Attaque")) "Toucher" else j.libelle
            Text(
                // Sans dés : capacité annoncée (ex. Imposition des mains retirant Empoisonné).
                if (j.des.isEmpty()) "✋ ${j.libelle} : ${j.formule}"
                else "🎲 $titre ${j.total}  (${j.formule} · ${if (titre == "Toucher") "d20" else "dés"} ${j.des.joinToString("+")} · $source)$verdict",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD778)
            )
            j.totalDegats?.let { degats ->
                Text(
                    "🎲 Dégâts $degats  (${j.formuleDegats} · dés ${j.desDegats.joinToString("+").ifEmpty { "–" }})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD778)
                )
            }
            if (j.applique) {
                Text("✓ traité par le MJ", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD778))
            }
            // Dégâts / soins d'un joueur : rien n'est appliqué sans le MJ. « Soins (Imposition des
            // mains) », « Dégâts (Châtiment divin) » : jets de capacité, distincts des jets d'action.
            val soin = j.libelle.startsWith("Soins")
            val montant = j.totalDegats ?: j.total
            if ((j.libelle.startsWith("Dégâts") || soin || j.totalDegats != null) && !j.applique) {
                var choixCible by remember(j.id) { mutableStateOf(false) }
                var choixZone by remember(j.id) { mutableStateOf(false) }
                val cibleParDefaut = (combattants.firstOrNull { it.id == j.cibleId } ?: cibleDeclaree)?.takeIf { soin || !it.horsCombat }
                // Sort de zone déclaré par le joueur : les dégâts vont à chacune des cibles choisies.
                val zone = !soin && j.cibleId == null && (declaration?.ciblesZone?.size ?: 0) > 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (zone) {
                        Button(onClick = { choixZone = true }) { Text("Appliquer aux ${declaration?.ciblesZone?.size} cibles") }
                    } else if (cibleParDefaut != null) {
                        Button(onClick = { CombatSession.appliquerJet(j.id, cibleParDefaut.id, soin) }) {
                            Text(if (soin) "Soigner ${cibleParDefaut.nom} de $montant" else "Appliquer $montant à ${cibleParDefaut.nom}")
                        }
                    }
                    MenuOptions(
                        buildList {
                            add((if (cibleParDefaut != null) "Autre cible" else "Choisir la cible") to { choixCible = true })
                            if (!soin) add("Plusieurs cibles (zone)" to { choixZone = true })
                            add("Ne pas appliquer" to { CombatSession.ignorerJet(j.id) })
                        }
                    )
                }
                if (choixZone) {
                    ZoneJetDialog(
                        jet = j,
                        montant = montant,
                        auteur = combattant,
                        ciblesDeclarees = declaration?.ciblesZone.orEmpty().ifEmpty { listOfNotNull(cibleDeclaree?.id) },
                        combattants = combattants,
                        onDismiss = { choixZone = false },
                    )
                }
                if (choixCible) {
                    AlertDialog(
                        onDismissRequest = { choixCible = false },
                        title = { Text(if (soin) "Soigner qui ? ($montant PV)" else "Appliquer $montant dégâts à…") },
                        text = {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                combattants.filter { soin || !it.horsCombat }.forEach { c ->
                                    OutlinedButton(onClick = {
                                        CombatSession.appliquerJet(j.id, c.id, soin)
                                        choixCible = false
                                    }) { Text(c.nom) }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { choixCible = false }) { Text("Annuler") } }
                    )
                }
            }
        }
        // Action principale en bouton ; toutes les autres options dans un menu déroulant.
        val attaqueAResoudre = phase == PhaseCombat.RESOLUTION && actif && declaration?.attaque != null
        val options = buildList<Pair<String, () -> Unit>> {
            if (attaqueAResoudre && declaration?.attaque?.let { it.zone || it.bonusToucher == null } == false) {
                add("Changer de cible / relancer" to onResoudre)
            }
            // Joueur connecté qui n'a pas encore choisi : le MJ peut exceptionnellement le faire.
            if (declaration == null && joueurConnecte) add("Déclarer à sa place" to onDeclarer)
            if (combattant.piloteParIa && phase != PhaseCombat.PREPARATION) add("Autre décision IA" to onRedecider)
            if (declaration != null && (phase == PhaseCombat.DECLARATION || actif)) {
                add((if (phase == PhaseCombat.RESOLUTION) "Changer l'action" else "Modifier l'action") to onDeclarer)
            }
            // Un seul jet par type et par round côté joueur : le MJ peut en accorder un de plus.
            if (phase == PhaseCombat.RESOLUTION && joueurConnecte) {
                add(("Autoriser un jet de plus" + (relances.takeIf { it > 0 }?.let { " ($it accordé)" } ?: "")) to { CombatSession.autoriserRelance(combattant.id) })
            }
            if (phase == PhaseCombat.RESOLUTION && actif && declaration != null && !declaration.libelle.startsWith("Action impossible")) {
                add("Action impossible" to onImpossible)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (attaqueAResoudre && declaration?.attaque != null) {
                val resultats = declaration.resultats
                val total = resultats?.sumOf { it.degats } ?: 0
                val ciblee = declaration.cibleNom
                if (declaration.attaque.zone || declaration.attaque.bonusToucher == null) {
                    Button(onClick = onResoudre) { Text("Appliquer aux cibles") }
                } else if (resultats != null && ciblee != null && !declaration.degatsAppliques) {
                    Button(onClick = { CombatSession.appliquerJets(combattant.id) }) {
                        Text(if (total > 0) "Appliquer $total dégâts à $ciblee" else "Noter l'échec")
                    }
                }
            }
            if (declaration == null && !joueurConnecte) {
                TextButton(onClick = onDeclarer) { Text("Déclarer") }
            }
            Spacer(Modifier.weight(1f))
            MenuOptions(options)
        }
    }
}

/**
 * Dégâts d'un jet de joueur appliqués à plusieurs créatures (sort de zone) : chacune fait son JS,
 * le MJ applique les dégâts entiers (échec) ou la moitié (réussite). Les cibles annoncées par le
 * joueur sont en tête ; « Terminer » marque le jet comme traité.
 */
@Composable
private fun ZoneJetDialog(
    jet: JetCombat,
    montant: Int,
    auteur: Combattant,
    ciblesDeclarees: List<String>,
    combattants: List<Combattant>,
    onDismiss: () -> Unit,
) {
    val debout = combattants.filter { !it.horsCombat }
    val cibles = ciblesDeclarees.mapNotNull { id -> debout.firstOrNull { it.id == id } } +
        debout.filter { it.id !in ciblesDeclarees }.sortedBy { it.estMonstre == auteur.estMonstre }
    // Cibles déjà traitées pendant cette fenêtre : texte du résultat.
    var traitees by remember(jet.id) { mutableStateOf(mapOf<String, String>()) }
    val moitie = montant / 2
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${auteur.nom} — ${jet.libelle} : $montant dégâts") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Échec au JS = $montant, réussite = $moitie.", style = MaterialTheme.typography.bodySmall)
                cibles.forEach { c ->
                    val visee = c.id in ciblesDeclarees
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.nom, fontWeight = if (visee) FontWeight.Bold else FontWeight.Normal)
                            Text(
                                traitees[c.id] ?: if (visee) "dans la zone" else "hors zone annoncée",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (traitees[c.id] != null) Color(0xFFFFD778) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (c.id !in traitees) {
                            OutlinedButton(onClick = {
                                CombatSession.appliquerPv(c.id, montant)
                                CombatSession.noter("${auteur.nom} — ${jet.libelle} : ${c.nom} rate son JS, $montant dégâts")
                                traitees = traitees + (c.id to "✓ $montant dégâts")
                            }) { Text("Échec $montant") }
                            TextButton(onClick = {
                                CombatSession.appliquerPv(c.id, moitie)
                                CombatSession.noter("${auteur.nom} — ${jet.libelle} : ${c.nom} réussit son JS, $moitie dégâts")
                                traitees = traitees + (c.id to "✓ $moitie dégâts")
                            }) { Text("Réussite $moitie") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                CombatSession.marquerJetTraite(jet.id)
                onDismiss()
            }) { Text("Terminer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

/** Bouton « Options ▾ » ouvrant un menu déroulant ; rien si aucune option. */
@Composable
private fun MenuOptions(options: List<Pair<String, () -> Unit>>) {
    if (options.isEmpty()) return
    var ouvert by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { ouvert = true }) {
            Text("Options")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            options.forEach { (libelle, action) ->
                DropdownMenuItem(
                    text = {
                        Text(libelle, color = if (libelle == "Action impossible") MaterialTheme.colorScheme.error else Color.Unspecified)
                    },
                    onClick = { ouvert = false; action() }
                )
            }
        }
    }
}

/**
 * Le MJ fixe l'action d'un combattant : PNJ ou PJ sans joueur connecté pendant la déclaration, ou
 * n'importe qui pendant la résolution quand la situation a changé (cible tombée, chemin bloqué…).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeclarationMjDialog(combattant: Combattant, etat: CombatEnCours, onDismiss: () -> Unit) {
    val existante = etat.declarations[combattant.id]
    var actionId by remember { mutableStateOf(existante?.actionId) }
    var attaque by remember { mutableStateOf(existante?.attaque) }
    var cibleId by remember { mutableStateOf(existante?.cibleId) }
    var detail by remember { mutableStateOf(existante?.detail?.takeIf { existante.parMj || !existante.parIa }.orEmpty()) }
    val cibles = etat.combattants.filter { it.id != combattant.id && !it.horsCombat }
    val action = ActionsCombat.toutes.firstOrNull { it.id == actionId }
    var distanceVisee by remember { mutableStateOf(existante?.deplacements?.get(existante.cibleId)) }
    val cibleAdverse = cibles.firstOrNull { it.id == cibleId && it.estMonstre != combattant.estMonstre }
    // PJ ou PNJ entré en combat sans attaques : on les tire de ses armes équipées, sinon l'action
    // Attaquer n'aurait aucun jet (ni toucher ni dégâts) à proposer pendant la résolution.
    val context = LocalContext.current
    var chargementArmes by remember { mutableStateOf(combattant.characterId != null && combattant.attaques.isEmpty()) }
    LaunchedEffect(combattant.id) {
        if (combattant.characterId != null && combattant.attaques.isEmpty()) {
            val perso = GameState.characters.value.firstOrNull { it.id == combattant.characterId }
            if (perso != null) {
                val armes = attaquesDuPersonnage(context, perso)
                if (armes.isNotEmpty()) CombatSession.definirAttaques(combattant.id, armes)
            }
        }
        chargementArmes = false
    }
    val attaquesFiche = combattant.attaques.filter { it.type != TypeAttaqueMonstre.AUTRE }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Action — ${combattant.nom}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (attaquesFiche.isNotEmpty()) {
                    Text("Attaques de la fiche (l'appli lance toucher et dégâts)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        attaquesFiche.forEach { a ->
                            FilterChip(
                                selected = attaque?.nom == a.nom,
                                onClick = { attaque = a; actionId = null },
                                label = { Text(a.nom) }
                            )
                        }
                    }
                } else if (chargementArmes) {
                    Text("Chargement des armes…", style = MaterialTheme.typography.bodySmall)
                }
                Text("Actions", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionsCombat.toutes.forEach { a ->
                        FilterChip(
                            selected = a.id == actionId,
                            onClick = {
                                // Attaquer : l'arme principale est choisie d'office, pour que les dés soient lancés.
                                val arme = if (a.id == "attaquer") attaquesFiche.firstOrNull { it.bonusToucher != null } ?: attaquesFiche.firstOrNull() else null
                                if (arme != null) { attaque = arme; actionId = null } else { actionId = a.id; attaque = null }
                            },
                            label = { Text(a.nom) }
                        )
                    }
                }
                if (actionId == "attaquer" && attaque == null && !chargementArmes) {
                    Text(
                        "Aucune arme trouvée sur la fiche : pas de jet automatique. Lancez les dés à la main puis utilisez « PV ± ».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (cibles.isNotEmpty()) {
                    Text("Cible", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        cibles.forEach { c ->
                            FilterChip(
                                selected = c.id == cibleId,
                                onClick = { cibleId = if (cibleId == c.id) null else c.id; distanceVisee = null },
                                label = {
                                    val d = if (c.estMonstre != combattant.estMonstre) " · " + etat.distance(combattant.id, c.id).court else ""
                                    Text(c.nom + d)
                                }
                            )
                        }
                    }
                }
                cibleAdverse?.let { c ->
                    val actuelle = etat.distance(combattant.id, c.id)
                    Text("Déplacement : finir à… (actuellement ${actuelle.court.lowercase()}, vitesse ${formatMetres(combattant.vitesse)}, double en se précipitant)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = distanceVisee == null, onClick = { distanceVisee = null }, label = { Text("Ne bouge pas") })
                        Distance.entries.filter { it != actuelle }.forEach { d ->
                            FilterChip(selected = distanceVisee == d, onClick = { distanceVisee = d }, label = { Text(d.court) })
                        }
                    }
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it.take(200) },
                    label = { Text("Précision / ce qui se passe") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = action != null || attaque != null || detail.isNotBlank(),
                onClick = {
                    val cible = cibles.firstOrNull { it.id == cibleId }
                    val nb = if (attaque?.bonusToucher != null) combattant.nbAttaquesMultiples else 1
                    val desengage = action?.id == "disengage"
                    val deplacements = when {
                        cibleAdverse != null && distanceVisee != null -> mapOf(cibleAdverse.id to distanceVisee!!)
                        desengage -> etat.distancesDe(combattant.id).filterValues { it == Distance.CONTACT }.mapValues { it.value.eloigne(combattant.vitesse) }
                        else -> emptyMap()
                    }
                    val declaration = DeclarationAction(
                        combattantId = combattant.id,
                        libelle = attaque?.let { it.nom + if (nb > 1) " ×$nb" else "" } ?: action?.nom ?: "Action libre",
                        actionId = action?.id,
                        cibleId = cible?.id,
                        cibleNom = cible?.nom,
                        detail = detail.takeIf { it.isNotBlank() },
                        parMj = true,
                        attaque = attaque,
                        nbAttaques = nb,
                        deplacements = deplacements,
                        desengage = desengage,
                    )
                    if (etat.phase == PhaseCombat.DECLARATION) CombatSession.declarer(declaration)
                    else CombatSession.modifierDeclaration(declaration)
                    onDismiss()
                }
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * Résolution d'une attaque de monstre : l'appli lance le d20 contre la CA de la cible et les
 * dégâts, puis le MJ applique (ou change de cible, ou déclare l'action impossible).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResolutionMonstreDialog(monstre: Combattant, declaration: DeclarationAction, etat: CombatEnCours, onDismiss: () -> Unit) {
    val attaque: AttaqueMonstre = declaration.attaque ?: return
    val cibles = etat.combattants.filter { it.estMonstre != monstre.estMonstre && !it.horsCombat }
    var cibleId by remember { mutableStateOf(declaration.cibleId?.takeIf { id -> cibles.any { it.id == id } } ?: cibles.firstOrNull()?.id) }
    // Jets déjà lancés à la déclaration ; changer de cible oblige à relancer.
    var resultats by remember { mutableStateOf(declaration.resultats) }
    val cible = cibles.firstOrNull { it.id == cibleId }
    val jetSauvegarde = attaque.bonusToucher == null && attaque.dd != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${monstre.nom} — ${declaration.libelle}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(attaque.resume, style = MaterialTheme.typography.bodyMedium)
                declaration.detail?.takeIf { !declaration.parIa }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (cibles.isEmpty()) {
                    Text("Plus aucune cible debout.", color = MaterialTheme.colorScheme.error)
                } else if (attaque.zone && attaque.zoneEffet?.forme?.depuisLanceur == false) {
                    // Sphère, cube… lancés sur un point : la créature visée est le centre de la zone.
                    Text("Centre de la zone (${attaque.zoneEffet.libelle})", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        cibles.forEach { c ->
                            FilterChip(selected = c.id == cibleId, onClick = { cibleId = c.id }, label = { Text(c.nom) })
                        }
                    }
                } else if (!attaque.zone) {
                    Text("Cible (changez-la si la situation a évolué)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        cibles.forEach { c ->
                            FilterChip(
                                selected = c.id == cibleId,
                                onClick = { cibleId = c.id; resultats = null },
                                label = { Text("${c.nom} (CA ${c.ca})") }
                            )
                        }
                    }
                    cible?.let { c ->
                        // Distance une fois son déplacement fait (ex. il fonce au contact).
                        val d = declaration.deplacements[c.id] ?: etat.distance(monstre.id, c.id)
                        if (!attaque.atteint(d)) {
                            Text(
                                "⚠ ${c.nom} est à ${d.label.lowercase()} : hors de portée de ${attaque.nom}.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (d == Distance.CONTACT && attaque.type == TypeAttaqueMonstre.DISTANCE) {
                            Text("Tir au contact : désavantage.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                // États de l'attaquant et de la cible : avantage, désavantage, critique au contact.
                val modeEtats = CombatSession.modeAttaque(etat, monstre.id, cible?.id, attaque)
                if (modeEtats.mode != ModeJet.NORMAL || modeEtats.critiqueAuto) {
                    Text(
                        listOfNotNull(
                            modeEtats.mode.takeIf { it != ModeJet.NORMAL }?.let { "Jet avec ${it.label} (${modeEtats.raisons.joinToString()})" },
                            "Touché = coup critique (cible au contact)".takeIf { modeEtats.critiqueAuto },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD778),
                    )
                }
                Button(
                    enabled = cibles.isNotEmpty(),
                    onClick = {
                        resultats = lancerAttaqueMonstre(
                            attaque, declaration.nbAttaques, if (attaque.zone) null else cible?.ca,
                            mode = modeEtats.mode, critiqueSiTouche = modeEtats.critiqueAuto,
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (resultats == null) "Lancer" else "Relancer") }

                resultats?.let { liste ->
                    liste.forEachIndexed { i, r ->
                        Text((if (liste.size > 1) "${i + 1}. " else "") + texteResultat(r, attaque), fontWeight = FontWeight.Bold)
                    }
                    val total = liste.sumOf { it.degats }
                    if (jetSauvegarde || attaque.zone) {
                        Text(
                            "Les cibles font un JS ${attaque.sauvegarde ?: ""} DD ${attaque.dd ?: "?"} : échec = $total, réussite = ${total / 2}.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (attaque.conditions.isNotEmpty()) {
                            Text(
                                "Échec : subit aussi ${attaque.conditions.joinToString { it.condition.label }}.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // Zone connue (« cône de 9 m ») : créatures prises d'après les distances, alliés
                        // du monstre compris pour une sphère ; sinon tous les adversaires debout.
                        val zone = attaque.zoneEffet?.takeIf { attaque.zone }
                        val touchees = when {
                            zone != null -> etat.dansLaZone(zone, monstre.id, cibleId)
                            attaque.zone -> cibles
                            else -> cibles.filter { it.id == cibleId }
                        }
                        zone?.let {
                            Text(
                                "${it.libelle} : ${touchees.size} créature(s) dans la zone d'après les distances.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD778),
                            )
                        }
                        touchees.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.nom)
                                    // Paralysé, Étourdi… : JS de Force/Dextérité ratés d'office.
                                    if (Etats.echecAutomatiqueJs(c.conditions, attaque.sauvegarde)) {
                                        Text(
                                            "Échec automatique (${Etats.developper(c.conditions).first { Etats.echecAutomatiqueJs(listOf(it), attaque.sauvegarde) }.label})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                OutlinedButton(onClick = {
                                    CombatSession.appliquerPv(c.id, total)
                                    CombatSession.noter("${monstre.nom} — ${attaque.nom} : ${c.nom} rate son JS, $total dégâts")
                                    attaque.conditions.forEach { CombatSession.appliquerCondition(c.id, it.condition, "${monstre.nom} — ${attaque.nom}") }
                                }) { Text("Échec $total") }
                                TextButton(onClick = {
                                    CombatSession.appliquerPv(c.id, total / 2)
                                    CombatSession.noter("${monstre.nom} — ${attaque.nom} : ${c.nom} réussit son JS, ${total / 2} dégâts")
                                }) { Text("Réussite ${total / 2}") }
                            }
                        }
                    } else if (cible != null) {
                        Button(
                            onClick = {
                                // Cible et jets retenus remplacent ceux de la déclaration, puis sont appliqués une fois.
                                CombatSession.remplacerJets(monstre.id, cible.id, liste)
                                CombatSession.appliquerJets(monstre.id)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (total > 0) "Appliquer $total dégâts à ${cible.nom}" else "Noter l'échec") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        dismissButton = {
            TextButton(onClick = {
                CombatSession.annulerAction(monstre.id)
                onDismiss()
            }) { Text("Action impossible", color = MaterialTheme.colorScheme.error) }
        }
    )
}

/**
 * Placement d'un monstre par rapport à chaque personnage : au contact, à courte ou à longue
 * distance. Réglé par le MJ au début du combat ; ensuite mis à jour par les déplacements.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PositionsDialog(monstre: Combattant, etat: CombatEnCours, onDismiss: () -> Unit) {
    // Adversaires d'abord, puis alliés (utiles aux zones d'effet : une boule de feu sur un
    // gobelin touche aussi ses voisins).
    val personnages = etat.combattants.filter { it.estMonstre != monstre.estMonstre } +
        etat.combattants.filter { it.estMonstre == monstre.estMonstre && it.id != monstre.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Position — ${monstre.nom}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (personnages.isEmpty()) {
                    Text("Aucun adversaire dans le combat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Tous les adversaires", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Distance.entries.forEach { d ->
                            OutlinedButton(onClick = { CombatSession.definirDistancesMonstre(monstre.id, d) }) { Text(d.court) }
                        }
                    }
                    HorizontalDivider()
                    personnages.forEach { p ->
                        val actuelle = etat.distance(monstre.id, p.id)
                        Text(
                            p.nom + (if (p.estMonstre == monstre.estMonstre) " (allié)" else "") + if (p.horsCombat) " (à terre)" else "",
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Distance.entries.forEach { d ->
                                FilterChip(
                                    selected = d == actuelle,
                                    onClick = { CombatSession.definirDistance(monstre.id, p.id, d) },
                                    label = { Text(d.court) }
                                )
                            }
                        }
                    }
                    Text(
                        "Contact = 1,50 m (allonge d'une attaque au corps à corps, grille de 1,50 m). " +
                            "Vitesse de ${monstre.nom} : ${formatMetres(monstre.vitesse)} par tour, le double en se précipitant. " +
                            "Non placés : 9 m entre adversaires, 3 m entre alliés. Les zones d'effet (sphère, cône…) " +
                            "touchent les créatures selon ces distances. Pendant la déclaration, l'IA revoit sa décision à chaque changement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

/** Résumé des distances d'un combattant au camp adverse : `Contact : Aldric · Longue : Brune`. */
internal fun resumePositions(etat: CombatEnCours, combattant: Combattant): String? {
    val parDistance = etat.distancesDe(combattant.id)
        .mapNotNull { (id, d) -> etat.combattants.firstOrNull { it.id == id && !it.horsCombat }?.let { d to it.nom } }
        .groupBy({ it.first }, { it.second })
    if (parDistance.isEmpty()) return null
    return Distance.entries.mapNotNull { d -> parDistance[d]?.let { "${d.court} : ${it.joinToString()}" } }.joinToString(" · ")
}

/**
 * Jet pour toucher et dés de dégâts d'une attaque : « Toucher 17 (d20 12 +5) touché · Dégâts 6 (1d6 + 2) »,
 * « Toucher 8 (d20 3 +5) raté · Dégâts 5 (1d6 + 2), non appliqués », « Dégâts 14 (6d6) · JS Dex DD 13 ».
 */
internal fun texteResultat(r: ResultatAttaque, attaque: AttaqueMonstre? = null): String {
    val formule = attaque?.formuleDegats?.let { " ($it)" } ?: ""
    if (r.d20 == null) {
        val js = attaque?.dd?.let { " · JS ${attaque.sauvegarde ?: ""} DD $it".replace("  ", " ") } ?: ""
        return "Dégâts ${r.degatsLances}$formule$js"
    }
    val bonus = attaque?.bonusToucher?.let { " ${if (it >= 0) "+$it" else "$it"}" } ?: ""
    val verdict = when {
        r.critique -> "CRITIQUE"
        r.touche -> "touché"
        else -> "raté"
    }
    // Avantage/désavantage dû aux états : « d20 15 (avantage, écarté 8) ».
    val mode = r.d20Ecarte?.let { ", ${r.mode.label}, écarté $it" } ?: ""
    return "Toucher ${r.totalToucher} (d20 ${r.d20}$mode$bonus) $verdict · Dégâts ${r.degatsLances}$formule" +
        if (!r.touche) ", non appliqués" else ""
}
