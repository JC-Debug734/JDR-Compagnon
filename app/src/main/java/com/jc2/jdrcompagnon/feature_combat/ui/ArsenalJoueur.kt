package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeEnMain
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.CapaciteAttaque
import com.jc2.jdrcompagnon.feature_combat.domain.model.JetSort
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortPret
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSrd
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.EmplacementsDeSortResultat
import com.jc2.jdrcompagnon.ui.screens.joueur.calculerEmplacementsDeSort
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EmplacementsDeSortParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Ce que le personnage du joueur peut utiliser en combat, calculé depuis sa fiche. */
internal data class ArsenalJoueur(
    val armes: List<ArmeEnMain>,
    val capacites: List<CapaciteAttaque>,
    val sorts: List<SortPret>,
    val emplacements: EmplacementsDeSortResultat,
    // Description des propriétés et bottes d'armes (Finesse, Coup double…).
    val definitions: Map<String, String> = emptyMap(),
    // Frappe à mains nues, toujours possible (coup, empoignade ou bousculade), et son DD.
    val mainsNues: ArmeEnMain? = null,
    val ddMainsNues: Int = 10,
    // Attaques par action Attaquer (Attaque supplémentaire) ; Fougue (Guerrier).
    val nbAttaques: Int = 1,
    val fougue: Boolean = false,
) {
    /** Emplacements restants par niveau de sort (1-9). */
    fun restants(character: Character): Map<Int, Int> =
        emplacements.classiques.mapValues { (niveau, max) -> (max - (character.spellSlotsUsed[niveau] ?: 0)).coerceAtLeast(0) }

    /** Emplacements de Magie de pacte restants (Occultiste), 0 sans pacte. */
    fun pacteRestant(character: Character): Int =
        emplacements.pacte?.let { (nombre, _) -> (nombre - character.pactSlotsUsed).coerceAtLeast(0) } ?: 0

    /**
     * Le sort peut-il encore être lancé : sort mineur, lancement gratuit disponible, ou un
     * emplacement restant de son niveau ou plus (classique ou de pacte). [restants] remplace les
     * emplacements de la fiche (simulation).
     */
    fun lancable(sort: SortPret, character: Character, restants: Map<Int, Int> = restants(character)): Boolean =
        sort.niveau == 0 || sort.gratuitDisponible ||
            restants.any { (niveau, reste) -> niveau >= sort.niveau && reste > 0 } ||
            (emplacements.pacte?.second ?: 0) >= sort.niveau && pacteRestant(character) > 0
}

/** Données SRD brutes, chargées une fois par monde ; l'arsenal est recalculé à chaque changement de fiche. */
private class SrdCombat(
    val armes: List<ArmeSrd>,
    val sorts: List<SortSrd>,
    val definitions: Map<String, String>,
    val emplacements: (Character) -> EmplacementsDeSortResultat,
    val classes: List<Classe>,
)

@Composable
internal fun rememberArsenal(character: Character?): ArsenalJoueur? {
    val context = LocalContext.current
    val monde = character?.worldId?.ifBlank { null } ?: "donjon_et_dragon"
    var srd by remember(monde) { mutableStateOf<SrdCombat?>(null) }
    LaunchedEffect(monde) {
        val equipements = SrdRepository.loadEquipmentList(context, monde)
        // Propriétés et bottes d'armes (« **Type** Propriété ») : nom → description, pour le détail.
        val definitions = equipements.filter { it.rawMarkdown.contains("**Type** Propriété") }
            .mapNotNull { e -> Regex("""\*\*Description\*\*\s*(.+)""").find(e.rawMarkdown)?.groupValues?.get(1)?.trim()?.let { e.name to it } }
            .toMap()
        val armes = equipements
            .filter { it.damage.isNotBlank() }
            .map { ArmeSrd.depuisFiche(it.name, it.damage, it.properties, it.rawMarkdown) }
        val sorts = SrdRepository.loadSpells(context, monde)
            .map { SortSrd(it.name, ArsenalPersonnage.niveauDepuisLibelle(it.niveauSort), it.rawMarkdown, ecole = it.category) }
        val classesMd = SrdRepository.loadClasses(context, monde).joinToString("\n\n") { it.rawMarkdown }
        val classes = ClasseParser.parse(classesMd)
        val table = EmplacementsDeSortParser.parse(classesMd)
        srd = SrdCombat(armes, sorts, definitions, { c -> calculerEmplacementsDeSort(c, classes, table) }, classes)
    }
    val donnees = srd ?: return null
    character ?: return null
    return remember(character, donnees) {
        ArsenalJoueur(
            armes = ArsenalPersonnage.armesEnMain(character, donnees.armes),
            capacites = ArsenalPersonnage.capacitesAttaque(character),
            sorts = ArsenalPersonnage.sortsPrets(character, donnees.sorts, ArsenalPersonnage.effetsCombatClasse(character, donnees.classes)),
            emplacements = donnees.emplacements(character),
            definitions = donnees.definitions,
            mainsNues = ArsenalPersonnage.frappeMainsNues(character),
            ddMainsNues = ArsenalPersonnage.ddFrappeMainsNues(character),
            nbAttaques = ArsenalPersonnage.nbAttaques(character),
            fougue = ArsenalPersonnage.aFougue(character),
        )
    }
}

internal fun signeTexte(v: Int) = if (v >= 0) "+$v" else "$v"

/** Carte sélectionnable (arme ou sort) dans le choix d'action. */
@Composable
internal fun CarteChoix(
    selectionnee: Boolean,
    onClick: () -> Unit,
    // Choix impossible (plus d'emplacement pour ce sort…) : carte rouge, non sélectionnable.
    indisponible: Boolean = false,
    contenu: @Composable () -> Unit,
) {
    val rouge = MaterialTheme.colorScheme.error
    Surface(
        onClick = onClick,
        enabled = !indisponible,
        shape = MaterialTheme.shapes.medium,
        color = when {
            indisponible -> rouge.copy(alpha = 0.18f)
            selectionnee -> ForcedDarkPalette.AccentGold.copy(alpha = 0.25f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
        },
        contentColor = if (indisponible) Color.White.copy(alpha = 0.7f) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    indisponible -> Modifier.border(1.dp, rouge, MaterialTheme.shapes.medium)
                    selectionnee -> Modifier.border(1.dp, ForcedDarkPalette.AccentGold, MaterialTheme.shapes.medium)
                    else -> Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                }
            ),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { contenu() }
    }
}

/** Armes en main (bonus calculés) et capacités d'attaque, pour l'action Attaquer. */
@Composable
internal fun ChoixArme(arsenal: ArsenalJoueur, choisie: String?, onChoisir: (ArmeEnMain) -> Unit) {
    var detailArme by remember { mutableStateOf<ArmeEnMain?>(null) }
    var detailCapacite by remember { mutableStateOf<CapaciteAttaque?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Armes équipées", style = MaterialTheme.typography.labelMedium, color = Color.White)
        arsenal.armes.forEach { arme ->
            CarteChoix(selectionnee = arme.nom == choisie, onClick = { if (!arme.sansMunition) onChoisir(arme) }) {
                Text("${arme.nom} · ${arme.main}", fontWeight = FontWeight.Bold)
                etatMunitions(arme)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (arme.sansMunition) MaterialTheme.colorScheme.error else ForcedDarkPalette.AccentGold)
                }
                Text(
                    "${signeTexte(arme.bonusToucher)} au toucher · ${arme.formuleDegats}" + (arme.typeDegats?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.titleSmall,
                    color = ForcedDarkPalette.AccentGold,
                )
                arme.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White) }
                if (arme.proprietes.isNotBlank()) Text("Propriétés : ${arme.proprietes}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                arme.botte?.let { Text("Botte : $it", style = MaterialTheme.typography.bodySmall, color = Color.White) }
                if (arme.proprietes.isNotBlank() || arme.botte != null) {
                    TextButton(onClick = { detailArme = arme }) { Text("ⓘ Détail des propriétés et de la botte") }
                }
            }
        }
        if (arsenal.capacites.isNotEmpty()) {
            Text("Capacités d'attaque (touchez pour le détail)", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(top = 4.dp))
            arsenal.capacites.forEach { cap ->
                Text(
                    "ⓘ ${cap.nom}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().clickable { detailCapacite = cap }.padding(vertical = 4.dp),
                )
            }
        }
    }
    detailArme?.let { arme ->
        DetailDialog(arme.nom, onDismiss = { detailArme = null }) {
            arme.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            // « Lancer (6/18) » → « Lancer » pour retrouver la définition.
            val proprietes = arme.proprietes.split(',').map { it.trim() }.filter { it.isNotBlank() }
            (proprietes.map { "Propriété" to it } + listOfNotNull(arme.botte?.let { "Botte" to it })).forEach { (genre, nom) ->
                val cle = nom.substringBefore('(').trim()
                Text("$genre : $nom", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Text(arsenal.definitions[cle] ?: "Pas de description dans la bibliothèque.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    detailCapacite?.let { cap ->
        DetailDialog(cap.nom, onDismiss = { detailCapacite = null }) {
            Text(cap.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Fenêtre de détail (sort, capacité, propriété d'arme) consultable par le joueur. */
@Composable
internal fun DetailDialog(titre: String, onDismiss: () -> Unit, contenu: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) { contenu() }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        containerColor = ForcedDarkPalette.Surface,
        titleContentColor = Color.White,
        textContentColor = Color.White,
    )
}

/**
 * Sorts prêts (mineurs + préparés) pour l'action Magie, groupés par niveau avec les emplacements
 * restants ; un sort de niveau 1+ demande le niveau d'emplacement utilisé.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoixSort(
    arsenal: ArsenalJoueur,
    character: Character,
    choisi: String?,
    niveauEmplacement: Int?,
    onChoisir: (SortPret) -> Unit,
    onEmplacement: (Int) -> Unit,
    // Simulation : emplacements restants de la simulation, à la place de ceux de la fiche.
    restantsOverride: Map<Int, Int>? = null,
) {
    val restants = restantsOverride ?: arsenal.restants(character)
    var detailSort by remember { mutableStateOf<SortPret?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (arsenal.sorts.isEmpty()) {
            Text(
                "Aucun sort prêt. Les sorts se préparent dans l'écran Repos.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
        // Favoris (étoile sur la fiche) en tête, puis les autres sorts par niveau.
        val (favoris, autres) = arsenal.sorts.partition { s -> character.favoriteSpells.any { ArsenalPersonnage.memeSort(it, s.nom) } }
        val groupes = (if (favoris.isNotEmpty()) listOf("★ Favoris" to favoris) else emptyList()) +
            autres.groupBy { it.niveau }.map { (niveau, sorts) ->
                (if (niveau == 0) "Sorts mineurs" else "Niveau $niveau" +
                    (arsenal.emplacements.classiques[niveau]?.let { max -> " — emplacements ${restants[niveau] ?: 0}/$max" } ?: "")) to sorts
            }
        groupes.forEach { (entete, sorts) ->
            Text(
                entete,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (entete.startsWith("★")) ForcedDarkPalette.AccentGold else Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
            sorts.forEach { sort ->
                val lancable = arsenal.lancable(sort, character, restants)
                CarteChoix(selectionnee = sort.nom == choisi && lancable, indisponible = !lancable, onClick = { onChoisir(sort) }) {
                    Text(
                        (if (sort.niveau > 0) "${sort.nom} (niv. ${sort.niveau})" else sort.nom) + if (sort.zone) " · zone" else "",
                        fontWeight = FontWeight.Bold,
                    )
                    if (!lancable) {
                        Text("Plus d'emplacement disponible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    if (sort.resume.isNotBlank()) Text(sort.resume, style = MaterialTheme.typography.titleSmall, color = ForcedDarkPalette.AccentGold)
                    Text(
                        listOf(sort.tempsIncantation, sort.portee).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                    sort.notesClasse.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
                    ArsenalPersonnage.libelleSortSpecial(sort.lancementGratuit)?.let { libelle ->
                        Text(
                            "$libelle : sans emplacement" + when {
                                sort.lancementGratuit == ArsenalPersonnage.SORT_A_VOLONTE -> ", à volonté"
                                sort.gratuitDisponible -> ", disponible"
                                else -> ", déjà utilisé (repos court ou long)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ForcedDarkPalette.AccentGold,
                        )
                    }
                    TextButton(onClick = { detailSort = sort }) { Text("ⓘ Détail du sort") }
                }
            }
        }
        arsenal.sorts.firstOrNull { it.nom == choisi && it.niveau > 0 }?.let { sort ->
            val possibles = arsenal.emplacements.classiques.keys.filter { it >= sort.niveau }.sorted()
            Text("Emplacement utilisé", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(top = 4.dp))
            if (possibles.isEmpty() && sort.lancementGratuit == null) {
                Text("Pas d'emplacement de ce niveau sur la fiche.", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Maîtrise des sorts / Sorts de prédilection : lancé à son niveau sans emplacement (niveau « 0 »).
                ArsenalPersonnage.libelleSortSpecial(sort.lancementGratuit)?.let { libelle ->
                    FilterChip(
                        selected = niveauEmplacement == 0,
                        enabled = sort.gratuitDisponible,
                        onClick = { onEmplacement(0) },
                        label = {
                            Text(
                                "$libelle — sans emplacement" +
                                    if (sort.lancementGratuit == ArsenalPersonnage.SORT_PREDILECTION && !sort.gratuitDisponible) " (déjà utilisé)" else ""
                            )
                        },
                    )
                }
                possibles.forEach { n ->
                    val reste = restants[n] ?: 0
                    FilterChip(
                        selected = niveauEmplacement == n,
                        enabled = reste > 0,
                        onClick = { onEmplacement(n) },
                        label = { Text("Niv. $n ($reste)") },
                    )
                }
            }
        }
    }
    detailSort?.let { s ->
        DetailDialog(s.nom, onDismiss = { detailSort = null }) {
            Text(if (s.niveau == 0) "Sort mineur" else "Sort de niveau ${s.niveau}", color = ForcedDarkPalette.AccentGold)
            if (s.resume.isNotBlank()) Text(s.resume, fontWeight = FontWeight.Bold)
            s.notesClasse.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
            Markdown(content = s.fiche.ifBlank { "Pas de description dans la bibliothèque." })
        }
    }
}


/**
 * Nature d'un jet du joueur : une ATTAQUE lance le d20 pour toucher ET les dégâts en même temps ;
 * DEGATS / SOINS lancent seulement les dés de la formule (sort à jet de sauvegarde, soin) ; D20
 * est un simple d20 + bonus (sauvegarde, compétence…) ; ANNONCE n'a pas de dé côté joueur
 * (empoignade, bousculade : c'est la cible qui fait un jet de sauvegarde).
 */
internal enum class GenreJet { ATTAQUE, DEGATS, SOINS, D20, ANNONCE }

/**
 * Jet proposé au joueur, tiré de sa fiche. [libelle] est le type envoyé au MJ (Attaque, Dégâts,
 * Soins, Sauvegarde…), [formule] celle des dégâts ou des soins, [annonce] le texte transmis pour
 * un jet [GenreJet.ANNONCE].
 */
internal data class OptionJet(
    val nom: String,
    val libelle: String,
    val genre: GenreJet,
    val bonus: Int = 0,
    val formule: String? = null,
    val typeDegats: String? = null,
    // Don Sauvagerie martiale : attaque avec une arme, dés de dégâts lancés deux fois (meilleur gardé).
    val sauvagerie: Boolean = false,
    // Arme dont l'attaque dépense une munition, ou qui est lancée (un exemplaire en moins).
    val arme: ArmeEnMain? = null,
    val lancer: Boolean = false,
    val annonce: String? = null,
) {
    /** Attaque impossible : arme à munitions sans munition. */
    val interdit: Boolean get() = arme?.sansMunition == true
}

/**
 * Attaques possibles avec [a] : une arme de lancer se lance ou frappe au corps à corps (javelines
 * et fléchettes se lancent d'abord ; une arme à distance ne sert pas au corps à corps).
 */
private fun optionsArme(a: ArmeEnMain, sauvagerie: Boolean, libelle: String = "Attaque", prefixe: String = ""): List<OptionJet> {
    fun option(lancer: Boolean) = OptionJet(
        prefixe + "${a.nom} (" + (if (lancer) "lancer · " else "") + "${signeTexte(a.bonusToucher)} · ${a.formuleDegats})",
        libelle, GenreJet.ATTAQUE, a.bonusToucher, a.formuleDegats, a.typeDegats,
        sauvagerie = sauvagerie && a.nom != ArsenalPersonnage.MAINS_NUES, arme = a, lancer = lancer,
    )
    return when {
        a.lancer && a.slot != null && a.aDistance -> listOf(option(lancer = true))
        a.lancer && a.slot != null && ArmorRules.isStackableWeapon(a.nom) -> listOf(option(lancer = true), option(lancer = false))
        a.lancer && a.slot != null -> listOf(option(lancer = false), option(lancer = true))
        else -> listOf(option(lancer = false))
    }
}

/** Empoignade / Bousculade : pas de jet d'attaque, la cible fait un JS de Force ou de Dextérité. */
internal fun optionFrappeSpeciale(nom: String, dd: Int): OptionJet {
    val effet = if (nom == ArsenalPersonnage.EMPOIGNADE) "échec : Agrippée" else "échec : repoussée de 1,50 m ou À terre"
    return OptionJet(nom, "Attaque", GenreJet.ANNONCE, annonce = "$nom (mains nues) : JS de Force ou Dextérité DD $dd — $effet")
}

/** Jets d'un sort : attaque de sort (toucher + dégâts), ou dégâts seuls, et soins. */
private fun optionsSort(s: SortPret, suffixe: String = ""): List<OptionJet> = listOfNotNull(
    when {
        s.jet == JetSort.ATTAQUE -> OptionJet(
            "${s.nom} (${signeTexte(s.bonusAttaque)}" + (s.formuleDegats?.let { " · $it" } ?: "") + ")",
            "Attaque$suffixe", GenreJet.ATTAQUE, s.bonusAttaque, s.formuleDegats, s.typeDegats,
        )
        s.formuleDegats != null -> OptionJet("Dégâts ${s.nom} (${s.formuleDegats})", "Dégâts$suffixe", GenreJet.DEGATS, formule = s.formuleDegats, typeDegats = s.typeDegats)
        else -> null
    },
    s.soin?.let { f -> OptionJet("Soins ${s.nom} ($f)", "Soins$suffixe", GenreJet.SOINS, formule = f) },
)

/**
 * Jets de l'action déclarée, dans l'ordre : une attaque par attaque choisie (Attaque
 * supplémentaire : plusieurs), ou les jets du sort annoncé. Vide pour une action sans jet.
 */
internal fun etapesAction(arsenal: ArsenalJoueur?, attaques: List<String>, sortAnnonce: SortPret?, sauvagerie: Boolean = false): List<OptionJet> {
    if (sortAnnonce != null) return optionsSort(sortAnnonce)
    arsenal ?: return emptyList()
    return attaques.mapNotNull { nom ->
        when (nom) {
            ArsenalPersonnage.EMPOIGNADE, ArsenalPersonnage.BOUSCULADE -> optionFrappeSpeciale(nom, arsenal.ddMainsNues)
            else -> (arsenal.armes.firstOrNull { it.nom == nom } ?: arsenal.mainsNues?.takeIf { it.nom == nom })
                ?.let { optionsArme(it, sauvagerie).first() }
        }
    }
}

/**
 * Ce que le joueur peut encore faire une fois son action résolue : actions bonus (arme de la main
 * secondaire, frappe à mains nues du moine, sorts à incanter en action bonus), Fougue (une action
 * de plus), puis les d20 génériques (sauvegarde, compétence).
 */
internal fun optionsSuivantes(arsenal: ArsenalJoueur?, sauvagerie: Boolean = false): List<OptionJet> {
    val bonus = " (action bonus)"
    val actionsBonus = arsenal?.let { a ->
        a.armes.filter { it.main == "Main secondaire" }.flatMap { optionsArme(it, sauvagerie, "Attaque$bonus", "Action bonus — ") } +
            (if (a.capacites.any { it.nom == "Arts martiaux" }) listOfNotNull(a.mainsNues).flatMap { optionsArme(it, false, "Attaque$bonus", "Action bonus — ") } else emptyList()) +
            a.sorts.filter { it.tempsIncantation.contains("bonus", ignoreCase = true) }.flatMap { s ->
                optionsSort(s, bonus).map { it.copy(nom = "Action bonus — " + it.nom) }
            }
    }.orEmpty()
    val fougue = arsenal?.takeIf { it.fougue }?.let { a ->
        (a.armes.filter { !it.sansMunition }.ifEmpty { listOfNotNull(a.mainsNues) })
            .flatMap { optionsArme(it, sauvagerie, "Attaque (Fougue)", "Fougue — ") }
    }.orEmpty()
    return actionsBonus + fougue + listOf(
        OptionJet("Jet de sauvegarde", "Sauvegarde", GenreJet.D20),
        OptionJet("Test de compétence", "Compétence", GenreJet.D20),
        OptionJet("Autre jet (d20)", "Autre", GenreJet.D20),
    )
}
