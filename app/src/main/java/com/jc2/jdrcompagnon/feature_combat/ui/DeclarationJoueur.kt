package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeEnMain
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.CapaciteAttaque
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.network.CombatJoueurData
import com.jc2.jdrcompagnon.network.CombattantJoueurData
import com.jc2.jdrcompagnon.network.DeclarationJoueurData
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Consommables
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Déplacement annoncé pour le round : finir à [visee] de l'adversaire [cibleId]. */
internal data class DeplacementJoueur(val cibleId: String, val visee: Distance)

/** Action complète prête à partir chez le MJ. */
internal data class DeclarationChoisie(
    val libelle: String,
    val actionId: String?,
    val cible: CombattantJoueurData?,
    val detail: String,
    val attaques: List<String> = emptyList(),
    // Sort de zone : toutes les créatures visées ([cible] est la première).
    val ciblesZone: List<CombattantJoueurData> = emptyList(),
)

/** Précision d'une déclaration « Utiliser » portant sur un objet équipé (« Utiliser : Potion de guérison »). */
internal const val PREFIXE_OBJET = "Utiliser : "

/**
 * Libellé d'une déclaration de sort : « Lancer : Boule de feu (emplacement niv. 3) », ou
 * « Lancer : Projectile magique (sans emplacement) » pour un sort à volonté ou de prédilection
 * (groupe 3 renseigné, groupe 2 vide).
 */
internal val sortRegex = Regex("""^Lancer : (.+?)(?: \((?:emplacement niv\. (\d)|($LIBELLE_SANS_EMPLACEMENT))\))?$""")

/** Niveau d'emplacement « 0 » d'une déclaration : sort lancé sans emplacement (ArsenalPersonnage.SORT_A_VOLONTE…). */
internal const val LIBELLE_SANS_EMPLACEMENT = "sans emplacement"

private val RougeEnnemi = Color(0xFFE57373)
private val BleuAllie = Color(0xFF64B5F6)

// ─── Tactique ────────────────────────────────────────────────────────────────

/** Part de santé approximative d'un monstre (seul son état est transmis au joueur). */
private fun ratioSante(c: CombattantJoueurData): Float = when {
    c.pv != null && c.pvMax != null && c.pvMax > 0 -> c.pv.toFloat() / c.pvMax
    else -> when (c.etat) {
        "Indemne" -> 1f
        "Blessé" -> 0.75f
        "En sang" -> 0.4f
        "Mal en point" -> 0.15f
        else -> 0f
    }
}

/**
 * Vue tactique : ennemis (du plus proche au plus loin) puis alliés, avec la distance et la santé
 * de chacun. Avec [onSelection], sert aussi à choisir la cible d'une action.
 */
@Composable
internal fun TableauTactique(
    combat: CombatJoueurData,
    moiId: String?,
    modifier: Modifier = Modifier,
    selectionId: String? = null,
    onSelection: ((CombattantJoueurData) -> Unit)? = null,
    // Plusieurs créatures sélectionnées (sort de zone).
    selectionIds: Set<String> = setOfNotNull(selectionId),
) {
    val moi = combat.ordre.firstOrNull { it.id == moiId }
    val (ennemis, allies) = combat.ordre.filter { it.id != moiId }.partition { moi != null && it.estMonstre != moi.estMonstre }
    val ordreDistance = { c: CombattantJoueurData -> c.distance?.let { d -> runCatching { Distance.valueOf(d).ordinal }.getOrNull() } ?: 9 }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ennemis", fontWeight = FontWeight.Bold, color = RougeEnnemi)
        if (ennemis.isEmpty()) Text("Aucun ennemi.", style = MaterialTheme.typography.bodySmall, color = Color.White)
        ennemis.sortedWith(compareBy<CombattantJoueurData> { it.horsCombat }.thenBy(ordreDistance)).forEach { c ->
            LigneTactique(c, estMoi = false, selectionnee = c.id in selectionIds, onClick = onSelection?.let { f -> { f(c) } })
        }
        Spacer(Modifier.height(4.dp))
        Text("Alliés", fontWeight = FontWeight.Bold, color = BleuAllie)
        (listOfNotNull(moi) + allies).forEach { c ->
            LigneTactique(c, estMoi = c.id == moiId, selectionnee = c.id in selectionIds, onClick = onSelection?.let { f -> { f(c) } })
        }
    }
}

@Composable
private fun LigneTactique(c: CombattantJoueurData, estMoi: Boolean, selectionnee: Boolean, onClick: (() -> Unit)?) {
    val couleurCamp = if (c.estMonstre) RougeEnnemi else BleuAllie
    val ratio = ratioSante(c)
    val contenu: @Composable () -> Unit = {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 4.dp, height = 36.dp).border(2.dp, couleurCamp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    c.nom + if (estMoi) " (vous)" else "",
                    fontWeight = FontWeight.Bold,
                    color = if (selectionnee) ForcedDarkPalette.AccentGold else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val sante = if (c.pv != null && c.pvMax != null) "PV ${c.pv}/${c.pvMax}" else c.etat
                Text(
                    (listOf(sante) + c.conditions).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp),
                    color = when {
                        ratio > 0.5f -> Color(0xFF66BB6A)
                        ratio > 0.25f -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            // Distance : connue pour chaque adversaire (au contact, courte, longue).
            val distance = c.distance?.let { d -> runCatching { Distance.valueOf(d).court }.getOrNull() }
            Text(
                distance ?: if (c.estMonstre) "" else "Allié",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (distance == Distance.CONTACT.court) ForcedDarkPalette.AccentGold else Color.White,
            )
        }
    }
    val modifier = Modifier
        .fillMaxWidth()
        .then(if (selectionnee) Modifier.border(2.dp, ForcedDarkPalette.AccentGold, MaterialTheme.shapes.medium) else Modifier)
    val fond = MaterialTheme.colorScheme.surface.copy(alpha = if (c.horsCombat) 0.35f else 0.75f)
    if (onClick != null) {
        Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = fond, contentColor = Color.White, modifier = modifier) { contenu() }
    } else {
        Surface(shape = MaterialTheme.shapes.medium, color = fond, contentColor = Color.White, modifier = modifier) { contenu() }
    }
}

// ─── Déplacement (écran des actions) ─────────────────────────────────────────

/** Où le personnage finit son déplacement ce round, par rapport à un adversaire. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CarteDeplacement(
    combat: CombatJoueurData,
    moiId: String?,
    deplacement: DeplacementJoueur?,
    desengage: Boolean,
    onChange: (DeplacementJoueur?) -> Unit,
) {
    val adversaires = combat.ordre.filter { it.id != moiId && it.distance != null && !it.horsCombat }
    var adversaireId by remember(deplacement?.cibleId) { mutableStateOf(deplacement?.cibleId ?: adversaires.firstOrNull()?.id) }
    val adversaire = adversaires.firstOrNull { it.id == adversaireId }
    val actuelle = adversaire?.distance?.let { runCatching { Distance.valueOf(it) }.getOrNull() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Déplacement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = ForcedDarkPalette.AccentGold)
            if (adversaires.isEmpty()) {
                Text("Aucun adversaire placé.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                return@Column
            }
            Text("Par rapport à :", style = MaterialTheme.typography.labelMedium, color = Color.White)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                adversaires.forEach { a ->
                    FilterChip(
                        selected = a.id == adversaireId,
                        onClick = { adversaireId = a.id; onChange(null) },
                        label = { Text(a.nom + " · " + (a.distance?.let { d -> runCatching { Distance.valueOf(d).court }.getOrNull() } ?: "")) },
                    )
                }
            }
            if (adversaire != null && actuelle != null) {
                Text("Finir le tour :", style = MaterialTheme.typography.labelMedium, color = Color.White)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = deplacement == null, onClick = { onChange(null) }, label = { Text("Je ne bouge pas") })
                    Distance.entries.filter { it != actuelle }.forEach { d ->
                        FilterChip(
                            selected = deplacement?.cibleId == adversaire.id && deplacement.visee == d,
                            onClick = { onChange(DeplacementJoueur(adversaire.id, d)) },
                            label = { Text(d.label) },
                        )
                    }
                }
                val visee = deplacement?.takeIf { it.cibleId == adversaire.id }?.visee
                if (actuelle == Distance.CONTACT && visee != null && !desengage) {
                    Text(
                        "Quitter le contact sans vous désengager expose à une attaque d'opportunité.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForcedDarkPalette.AccentGold,
                    )
                }
                if (actuelle == Distance.LONGUE && visee == Distance.CONTACT) {
                    Text(
                        "Depuis la longue distance, atteindre le contact demande en général de Se précipiter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForcedDarkPalette.AccentGold,
                    )
                }
            }
        }
    }
}

// ─── Assistant de déclaration ────────────────────────────────────────────────

/** Actions précisées avant la cible (arme, sort, objet). */
private val actionsAvecDetail = setOf("attaquer", "magie", "utilize")

/** Actions qui visent quelqu'un : la cible se choisit sur l'écran tactique (compétences, sorts…). */
private val actionsAvecCible = setOf("attaquer", "magie", "utilize", "help", "influence", "study", "search")

/**
 * Déclaration d'une action en plein écran : d'abord le détail (armes et attaques, sort, objet),
 * puis la cible sur l'écran tactique (distance et santé de chacun). Le déplacement se règle sur
 * l'écran des actions, avant.
 */
@Composable
internal fun AssistantDeclaration(
    combat: CombatJoueurData,
    moi: CombattantJoueurData,
    action: ActionCombat,
    arsenal: ArsenalJoueur?,
    personnage: Character?,
    consommables: List<EquipmentItem>,
    initiale: DeclarationJoueurData?,
    // Arme touchée sur sa carte (écran des actions) : première attaque déjà choisie.
    armeInitiale: String?,
    onValider: (DeclarationChoisie) -> Unit,
    onFermer: () -> Unit,
    // Simulation : emplacements restants de la simulation, à la place de ceux de la fiche.
    emplacementsRestants: Map<Int, Int>? = null,
) {
    val avecDetail = action.id in actionsAvecDetail
    val avecCible = action.id in actionsAvecCible
    var surCible by remember { mutableStateOf(!avecDetail && avecCible) }
    var cibleId by remember { mutableStateOf(initiale?.cibleId) }
    // Précision libre du joueur (ce qui suit « — » dans une déclaration calculée).
    var precision by remember {
        val calcule = initiale?.libelle?.let { it.startsWith("Attaquer : ") || it.startsWith("Lancer : ") || it == "Utiliser" } == true
        mutableStateOf(if (calcule) initiale?.detail?.substringAfter(" — ", "").orEmpty() else initiale?.detail.orEmpty())
    }
    var attaques by remember {
        mutableStateOf(
            armeInitiale?.let { listOf(it) }
                ?: initiale?.attaques?.takeIf { initiale.actionId == "attaquer" }.orEmpty()
        )
    }
    val sortInitial = initiale?.libelle?.let { sortRegex.find(it) }
    var sortChoisi by remember { mutableStateOf(sortInitial?.groupValues?.get(1)) }
    var niveauEmplacement by remember {
        mutableStateOf(sortInitial?.let { m -> if (m.groupValues[3].isNotEmpty()) 0 else m.groupValues[2].toIntOrNull() })
    }
    var objetChoisi by remember {
        mutableStateOf(initiale?.detail?.takeIf { it.startsWith(PREFIXE_OBJET) }?.removePrefix(PREFIXE_OBJET)?.substringBefore(" — "))
    }
    val sort = arsenal?.sorts?.firstOrNull { it.nom == sortChoisi }
    val objet = consommables.firstOrNull { it.name == objetChoisi }
    // Sort de zone : plusieurs cibles (ordre de sélection), la première sert de cible principale.
    val zone = action.id == "magie" && sort?.zone == true
    var ciblesZone by remember {
        mutableStateOf(initiale?.ciblesZone?.takeIf { it.isNotEmpty() } ?: listOfNotNull(initiale?.cibleId))
    }
    val detailPret = when (action.id) {
        "attaquer" -> attaques.isNotEmpty()
        "magie" -> sort != null && (personnage == null || arsenal?.let { a -> a.lancable(sort, personnage, emplacementsRestants ?: a.restants(personnage)) } != false)
        "utilize" -> objet != null || precision.isNotBlank()
        else -> true
    }

    fun valider() {
        val ciblesChoisies = if (zone) ciblesZone.mapNotNull { id -> combat.ordre.firstOrNull { it.id == id } } else emptyList()
        val cible = if (zone) ciblesChoisies.firstOrNull() else combat.ordre.firstOrNull { it.id == cibleId }
        val libelle = when {
            action.id == "attaquer" -> "Attaquer : " + attaques.joinToString(" + ")
            action.id == "magie" && sort != null ->
                "Lancer : ${sort.nom}" + (niveauEmplacement?.takeIf { sort.niveau > 0 }?.let {
                    if (it == 0) " ($LIBELLE_SANS_EMPLACEMENT)" else " (emplacement niv. $it)"
                } ?: "")
            else -> action.nom
        }
        // Résumé calculé (bonus, dégâts, DD) suivi de la précision du joueur.
        val auto = when (action.id) {
            "attaquer" -> attaques.joinToString(" · ") { resumeAttaque(it, arsenal) }
            "magie" -> sort?.resume?.ifBlank { null }
            "utilize" -> objet?.let { PREFIXE_OBJET + it.name }
            else -> null
        }
        val detail = listOfNotNull(auto, precision.takeIf { it.isNotBlank() && it != auto }).joinToString(" — ")
        onValider(DeclarationChoisie(libelle, action.id, cible, detail, if (action.id == "attaquer") attaques else emptyList(), ciblesChoisies))
    }

    // Affiché dans l'écran (pas en fenêtre plein écran) : la barre du bas de l'appli reste visible,
    // comme pour la lecture d'un scénario. Le retour Android revient à l'étape précédente.
    BackHandler { if (surCible && avecDetail) surCible = false else onFermer() }
    Surface(color = Color.Transparent, contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                IconButton(onClick = { if (surCible && avecDetail) surCible = false else onFermer() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(action.nom, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                    Text(
                        when {
                            surCible -> "Choisissez la cible"
                            action.id == "attaquer" -> "Choisissez vos attaques"
                            action.id == "magie" -> "Choisissez le sort"
                            action.id == "utilize" -> "Choisissez l'objet"
                            else -> "Précisez votre action"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                }
                IconButton(onClick = onFermer) { Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White) }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (surCible) {
                    // Compétence ou sort : rappel de ce qui a été choisi avant la cible.
                    when {
                        action.id == "attaquer" -> Text(attaques.joinToString(" + "), fontWeight = FontWeight.Bold)
                        sort != null -> Text(sort.nom + (sort.resume.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""), fontWeight = FontWeight.Bold)
                        objet != null -> Text(objet.name, fontWeight = FontWeight.Bold)
                        else -> Text(action.description, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    if (zone) {
                        Text(
                            "Sort de zone : touchez chaque créature prise dans la zone (${ciblesZone.size} choisie(s)). Chacune fera son jet de sauvegarde.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForcedDarkPalette.AccentGold,
                        )
                        TableauTactique(
                            combat, moi.id,
                            selectionIds = ciblesZone.toSet(),
                            onSelection = { c -> ciblesZone = if (c.id in ciblesZone) ciblesZone - c.id else ciblesZone + c.id },
                        )
                    } else {
                        FilterChip(selected = cibleId == null, onClick = { cibleId = null }, label = { Text("Sans cible précise") })
                        TableauTactique(combat, moi.id, selectionId = cibleId, onSelection = { c -> cibleId = if (cibleId == c.id) null else c.id })
                    }
                    OutlinedTextField(
                        value = precision,
                        onValueChange = { precision = it.take(200) },
                        label = { Text("Précision (facultatif)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    when (action.id) {
                        "attaquer" -> if (arsenal == null) Text("Chargement de votre fiche…", style = MaterialTheme.typography.bodySmall)
                            else ChoixAttaques(arsenal, attaques) { attaques = it }
                        "magie" -> if (arsenal == null || personnage == null) Text("Chargement de votre fiche…", style = MaterialTheme.typography.bodySmall)
                            else ChoixSort(
                                arsenal = arsenal,
                                character = personnage,
                                choisi = sortChoisi,
                                niveauEmplacement = niveauEmplacement,
                                restantsOverride = emplacementsRestants,
                                onChoisir = { s ->
                                    sortChoisi = s.nom
                                    // Emplacement proposé : le plus bas niveau disponible pour ce sort.
                                    val restants = emplacementsRestants ?: arsenal.restants(personnage)
                                    niveauEmplacement = when {
                                        s.niveau == 0 -> null
                                        // À volonté (Maîtrise des sorts) : sans emplacement par défaut.
                                        s.lancementGratuit == ArsenalPersonnage.SORT_A_VOLONTE -> 0
                                        else -> restants.keys.filter { it >= s.niveau && (restants[it] ?: 0) > 0 }.minOrNull()
                                    }
                                },
                                onEmplacement = { niveauEmplacement = it },
                            )
                        "utilize" -> ChoixObjet(consommables, objetChoisi) { objetChoisi = it }
                        else -> Text(action.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                    if (!avecCible || action.id == "utilize" && consommables.isEmpty()) {
                        OutlinedTextField(
                            value = precision,
                            onValueChange = { precision = it.take(200) },
                            label = { Text("Précision (facultatif)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // Bouton principal bien visible (doré), juste au-dessus de la barre du bas.
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    val couleurs = ButtonDefaults.buttonColors(
                        containerColor = ForcedDarkPalette.AccentGold,
                        contentColor = ForcedDarkPalette.Background,
                        disabledContainerColor = Color.White.copy(alpha = 0.15f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f),
                    )
                    val modifier = Modifier.fillMaxWidth().height(54.dp)
                    if (avecCible && !surCible) {
                        Button(enabled = detailPret, onClick = { surCible = true }, colors = couleurs, modifier = modifier) {
                            Text("Choisir la cible →", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Button(enabled = detailPret, onClick = { valider() }, colors = couleurs, modifier = modifier) {
                            Text("Valider mon action", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Résumé d'une attaque choisie (bonus et dégâts, ou DD de l'empoignade / bousculade). */
private fun resumeAttaque(nom: String, arsenal: ArsenalJoueur?): String = when (nom) {
    ArsenalPersonnage.EMPOIGNADE, ArsenalPersonnage.BOUSCULADE -> "$nom : JS For/Dex DD ${arsenal?.ddMainsNues ?: "?"}"
    else -> (arsenal?.armes?.firstOrNull { it.nom == nom } ?: arsenal?.mainsNues?.takeIf { it.nom == nom })
        ?.let { a -> "$nom ${signeTexte(a.bonusToucher)} · ${a.formuleDegats}" + (a.typeDegats?.let { " $it" } ?: "") }
        ?: nom
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoixObjet(consommables: List<EquipmentItem>, choisi: String?, onChoisir: (String) -> Unit) {
    if (consommables.isEmpty()) {
        Text("Aucun objet utilisable équipé : précisez ce que vous faites.", style = MaterialTheme.typography.bodySmall, color = Color.White)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        consommables.forEach { objet ->
            CarteChoix(selectionnee = objet.name == choisi, onClick = { onChoisir(objet.name) }) {
                Text(objet.name, fontWeight = FontWeight.Bold)
                Text(Consommables.resume(objet), style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
    }
}

/**
 * Attaques de l'action Attaquer : armes équipées, et toujours la frappe à mains nues (coup,
 * empoignade ou bousculade). Avec Attaque supplémentaire, une attaque par emplacement.
 */
@Composable
internal fun ChoixAttaques(arsenal: ArsenalJoueur, choix: List<String>, onChoix: (List<String>) -> Unit) {
    val n = arsenal.nbAttaques.coerceAtLeast(1)
    var mainsNuesOuvert by remember { mutableStateOf(false) }
    var detailCapacite by remember { mutableStateOf<CapaciteAttaque?>(null) }
    fun ajouter(nom: String) = onChoix(
        when {
            n == 1 -> listOf(nom)
            choix.size < n -> choix + nom
            else -> choix.dropLast(1) + nom
        }
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (n > 1) {
            Text(
                "Attaque supplémentaire : $n attaques. Touchez une arme pour chaque attaque (la même plusieurs fois si vous voulez).",
                style = MaterialTheme.typography.bodySmall,
                color = ForcedDarkPalette.AccentGold,
            )
            (0 until n).forEach { i ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Attaque ${i + 1} : ", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Text(choix.getOrNull(i) ?: "—", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (i < choix.size) {
                        IconButton(onClick = { onChoix(choix.filterIndexed { j, _ -> j != i }) }) {
                            Icon(Icons.Default.Close, contentDescription = "Retirer", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        Text("Armes", style = MaterialTheme.typography.labelMedium, color = Color.White)
        arsenal.armes.filter { it.nom != ArsenalPersonnage.MAINS_NUES }.forEach { arme ->
            CarteArmeChoix(arme, fois = choix.count { it == arme.nom }) { if (!arme.sansMunition) ajouter(arme.nom) }
        }
        // Frappe à mains nues : toujours proposée ; la toucher propose coup, empoignade ou bousculade.
        arsenal.mainsNues?.let { mn ->
            val fois = choix.count { it == mn.nom || it == ArsenalPersonnage.EMPOIGNADE || it == ArsenalPersonnage.BOUSCULADE }
            CarteChoix(selectionnee = fois > 0 || mainsNuesOuvert, onClick = { mainsNuesOuvert = !mainsNuesOuvert }) {
                Text("Frappe à mains nues" + if (fois > 1) "  ×$fois" else "", fontWeight = FontWeight.Bold)
                Text("Coup, Empoignade ou Bousculade", style = MaterialTheme.typography.bodySmall, color = Color.White)
                if (mainsNuesOuvert) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(onClick = { ajouter(mn.nom); mainsNuesOuvert = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Coup · ${signeTexte(mn.bonusToucher)} · ${mn.formuleDegats} ${mn.typeDegats.orEmpty()}")
                        }
                        OutlinedButton(onClick = { ajouter(ArsenalPersonnage.EMPOIGNADE); mainsNuesOuvert = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Empoignade · JS DD ${arsenal.ddMainsNues}")
                        }
                        OutlinedButton(onClick = { ajouter(ArsenalPersonnage.BOUSCULADE); mainsNuesOuvert = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("Bousculade · JS DD ${arsenal.ddMainsNues}")
                        }
                        Text(
                            "Empoignade : la cible (au plus une taille de plus que vous) réussit un JS de Force ou Dextérité ou devient Agrippée. " +
                                "Bousculade : même JS, sinon repoussée de 1,50 m ou À terre.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
        if (arsenal.capacites.isNotEmpty()) {
            Text("Capacités (touchez pour le détail)", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(top = 4.dp))
            arsenal.capacites.forEach { cap ->
                TextButton(onClick = { detailCapacite = cap }) { Text("ⓘ ${cap.nom}") }
            }
        }
    }
    detailCapacite?.let { cap ->
        DetailDialog(cap.nom, onDismiss = { detailCapacite = null }) { Text(cap.detail, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun CarteArmeChoix(arme: ArmeEnMain, fois: Int, onClick: () -> Unit) {
    CarteChoix(selectionnee = fois > 0, onClick = onClick) {
        Text("${arme.nom} · ${arme.main}" + if (fois > 1) "  ×$fois" else "", fontWeight = FontWeight.Bold)
        etatMunitions(arme)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (arme.sansMunition) MaterialTheme.colorScheme.error else ForcedDarkPalette.AccentGold)
        }
        Text(
            "${signeTexte(arme.bonusToucher)} au toucher · ${arme.formuleDegats}" + (arme.typeDegats?.let { " $it" } ?: ""),
            style = MaterialTheme.typography.titleSmall,
            color = ForcedDarkPalette.AccentGold,
        )
        arme.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White) }
    }
}
