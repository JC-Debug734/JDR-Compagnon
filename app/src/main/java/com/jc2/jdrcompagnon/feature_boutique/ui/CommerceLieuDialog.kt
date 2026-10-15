package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueImageStore
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CommerceBoutiqueUseCase
import com.jc2.jdrcompagnon.network.ArticleJoueurData
import com.jc2.jdrcompagnon.network.BoutiqueJoueurData
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.PositionVilleData
import com.jc2.jdrcompagnon.network.ServiceJoueurData
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.Monnaie
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val TexteSecondaire = Color.White.copy(alpha = 0.8f)

private enum class OngletCommerce(val label: String) { ACHETER("Acheter"), VENDRE("Vendre"), SERVICES("Services") }

/** Transaction en attente de confirmation : achat ou vente d'un objet, ou service. */
private sealed interface AConfirmer {
    data class Achat(val article: ArticleJoueurData) : AConfirmer
    data class Vente(val objet: String, val prix: Int) : AConfirmer
    data class Utilisation(val service: ServiceJoueurData) : AConfirmer
}

/**
 * Écran d'un lieu de la ville où se trouve le groupe (menu joueur) : sa particularité
 * ([description]), les autres joueurs présents ([presents]) et, si c'est une [boutique], le
 * commerce — acheter dans la limite du stock, vendre dans la limite de la caisse du marchand
 * (moitié du coût de base SRD, cf. CommerceBoutiqueUseCase), utiliser un service.
 *
 * Les achats et ventes passent par NetworkSessionManager.commercer (validés par le MJ en partie
 * réseau) ; l'or, le sac et le stock affichés se mettent à jour à la réception des diffusions.
 */
@Composable
fun CommerceLieuDialog(
    lieu: String,
    description: String,
    boutique: BoutiqueJoueurData?,
    personnage: Character?,
    presents: List<PositionVilleData>,
    onUtiliserService: (BoutiqueJoueurData, ServiceJoueurData) -> Unit,
    onDismiss: () -> Unit,
    // Fonction du lieu (prier, panneau de quêtes...), voir FonctionLieuJoueur ; null = aucune.
    fonction: (@Composable () -> Unit)? = null,
) {
    val resultat by NetworkSessionManager.resultatCommerce.collectAsState()
    var enCours by remember { mutableStateOf(false) }
    var aConfirmer by remember { mutableStateOf<AConfirmer?>(null) }
    var serviceUtilise by remember { mutableStateOf<String?>(null) }
    // Bonne aventure en cours (service à effet TAROKKA) : les trois cartes tirées.
    var tirageTarokka by remember { mutableStateOf<List<TarokkaDeck.Carte>?>(null) }
    val contexte = androidx.compose.ui.platform.LocalContext.current
    val portee = androidx.compose.runtime.rememberCoroutineScope()
    // Objet dont on consulte la fiche (appui sur sa ligne).
    var objetDetail by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { NetworkSessionManager.effacerResultatCommerce() }
    LaunchedEffect(resultat) { if (resultat != null) enCours = false }

    val onglets = buildList {
        if (boutique != null && boutique.articles.isNotEmpty()) add(OngletCommerce.ACHETER)
        if (boutique != null) add(OngletCommerce.VENDRE)
        if (boutique != null && boutique.services.isNotEmpty()) add(OngletCommerce.SERVICES)
    }
    var ongletIndex by remember(boutique?.id) { mutableIntStateOf(0) }
    val onglet = onglets.getOrNull(ongletIndex.coerceAtMost((onglets.size - 1).coerceAtLeast(0)))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ForcedDarkPalette.Background,
            contentColor = Color.White,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // En-tête : lieu, boutique et marchand, bourse du personnage.
                Row(verticalAlignment = Alignment.Top) {
                    if (boutique != null) {
                        EnseigneBoutique(boutique.image, Modifier.size(56.dp), tint = ForcedDarkPalette.AccentGold)
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lieu, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        boutique?.let { b ->
                            if (b.nom != lieu) Text(b.nom, style = MaterialTheme.typography.titleSmall, color = ForcedDarkPalette.AccentGold)
                            Text(
                                listOf(b.type, b.marchand, b.traitMarchand).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TexteSecondaire,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
                }
                if (description.isNotBlank()) {
                    CarteTranslucide { Text(description, style = MaterialTheme.typography.bodyMedium) }
                }
                if (presents.isNotEmpty()) {
                    CarteTranslucide {
                        Text("Ici aussi", style = MaterialTheme.typography.labelMedium, color = ForcedDarkPalette.AccentGold)
                        presents.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = TexteSecondaire)
                                Spacer(Modifier.width(6.dp))
                                Text(nomPresent(p), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Fonction du lieu : défile si elle est longue, sans écraser le commerce dessous.
                if (fonction != null) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    ) { fonction() }
                }

                if (boutique == null) {
                    if (fonction == null) Text("Rien à acheter ni à vendre ici.", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                    return@Column
                }

                Text(
                    personnage?.let { "${it.name} : ${Monnaie.libelleBourse(it)}" } ?: "Choisissez un personnage pour commercer.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    enCours -> Text("Transaction en cours…", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                    resultat != null -> resultat?.let {
                        Text(
                            (if (it.accepte) "✔ " else "✖ ") + it.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.accepte) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                    serviceUtilise != null -> Text("✔ $serviceUtilise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                if (onglets.size > 1 && onglet != null) {
                    TabRow(
                        selectedTabIndex = onglets.indexOf(onglet),
                        containerColor = Color.Transparent,
                        contentColor = ForcedDarkPalette.AccentGold,
                    ) {
                        onglets.forEachIndexed { i, o ->
                            Tab(selected = o == onglet, onClick = { ongletIndex = i }, text = { Text(o.label, color = Color.White) })
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    when (onglet) {
                        OngletCommerce.ACHETER -> ListeAchats(boutique, personnage, enCours, onDetail = { objetDetail = it }) { aConfirmer = AConfirmer.Achat(it) }
                        OngletCommerce.VENDRE -> ListeVentes(personnage, enCours, onDetail = { objetDetail = it }) { objet, prix -> aConfirmer = AConfirmer.Vente(objet, prix) }
                        OngletCommerce.SERVICES -> ListeServices(boutique, personnage) { aConfirmer = AConfirmer.Utilisation(it) }
                        null -> Unit
                    }
                }
            }
        }
    }

    tirageTarokka?.let { TirageTarokkaDialog(it) { tirageTarokka = null } }

    objetDetail?.let { nom ->
        com.jc2.jdrcompagnon.ui.screens.mj.scenario.InternalLinkDetailDialog(
            type = "equipment",
            name = nom,
            worldId = personnage?.worldId?.ifBlank { null },
            onDismiss = { objetDetail = null },
        )
    }

    aConfirmer?.let { action ->
        val (titre, texte) = when (action) {
            is AConfirmer.Achat -> "Acheter ?" to "« ${action.article.nom} » pour ${Monnaie.libelle(action.article.prixPc)}, payés par ${personnage?.name.orEmpty()}."
            is AConfirmer.Vente -> "Vendre ?" to "« ${action.objet} » à ${boutique?.marchand.orEmpty()} pour ${Monnaie.libelle(action.prix)}."
            is AConfirmer.Utilisation -> "Utiliser ce service ?" to "${action.service.nom} pour ${Monnaie.libelle(action.service.prixEnPc)}, payés par ${personnage?.name.orEmpty()}."
        }
        AlertDialog(
            onDismissRequest = { aConfirmer = null },
            title = { Text(titre) },
            text = { Text(texte) },
            confirmButton = {
                TextButton(onClick = {
                    aConfirmer = null
                    val perso = personnage ?: return@TextButton
                    val b = boutique ?: return@TextButton
                    NetworkSessionManager.effacerResultatCommerce()
                    serviceUtilise = null
                    when (action) {
                        is AConfirmer.Achat -> { enCours = true; NetworkSessionManager.commercer(perso.id, b.id, action.article.nom, achat = true) }
                        is AConfirmer.Vente -> { enCours = true; NetworkSessionManager.commercer(perso.id, b.id, action.objet, achat = false) }
                        is AConfirmer.Utilisation -> {
                            onUtiliserService(b, action.service)
                            val paye = "${action.service.nom} : ${Monnaie.libelle(action.service.prixEnPc)}" + if (action.service.prixEnPc > 0) " payés" else ""
                            // Action du service sur la fiche (repas, nuit = repos long, soins...).
                            val effet = com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.depuis(action.service.effet)
                            val compteRendu = EffetsServices.appliquer(contexte, portee, perso, action.service.nom, effet) { maj -> serviceUtilise = "$paye. $maj" }
                            serviceUtilise = paye + (compteRendu?.let { ". $it" } ?: "")
                            val effetReel = if (effet == com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.AUTO)
                                com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.deduire(action.service.nom) else effet
                            if (effetReel == com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.TAROKKA) {
                                val tirage = TarokkaDeck.tirer(contexte)
                                tirageTarokka = tirage
                                // La lecture est gardée dans les notes du personnage (relayées au MJ).
                                if (tirage.size == 3) {
                                    val notes = perso.notes.trimEnd()
                                    com.jc2.jdrcompagnon.ui.GameState.updateCharacterNotes(
                                        perso.id, (if (notes.isBlank()) "" else "$notes\n\n") + TarokkaDeck.texte(tirage),
                                    )
                                }
                            }
                        }
                    }
                }) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { aConfirmer = null }) { Text("Annuler") } },
        )
    }
}

/** « Pseudo (Personnage) », ou l'un des deux s'il manque. */
fun nomPresent(p: PositionVilleData): String = when {
    p.personnage.isBlank() -> p.joueur
    p.joueur.isBlank() || p.joueur == p.personnage -> p.personnage
    else -> "${p.personnage} (${p.joueur})"
}

@Composable
private fun CarteTranslucide(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun LigneCommerce(
    titre: String,
    detail: String,
    bouton: String,
    actif: Boolean,
    onDetail: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    CarteTranslucide {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (onDetail != null) Modifier.clickable(onClick = onDetail) else Modifier,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
            }
            TextButton(onClick = onClick, enabled = actif) { Text(bouton) }
        }
    }
}

@Composable
private fun ListeAchats(
    boutique: BoutiqueJoueurData,
    personnage: Character?,
    enCours: Boolean,
    onDetail: (String) -> Unit,
    onAcheter: (ArticleJoueurData) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(boutique.articles.sortedBy { it.nom }) { article ->
            val epuise = article.stock <= 0
            LigneCommerce(
                titre = "${article.nom} — ${Monnaie.libelle(article.prixPc)}",
                detail = listOf(article.type, if (epuise) "épuisé" else "${article.stock} en stock").filter { it.isNotBlank() }.joinToString(" · "),
                bouton = "Acheter",
                actif = !enCours && !epuise && personnage != null && Monnaie.totalPc(personnage) >= article.prixPc,
                onDetail = { onDetail(article.nom) },
            ) { onAcheter(article) }
        }
    }
}

@Composable
private fun ListeVentes(personnage: Character?, enCours: Boolean, onDetail: (String) -> Unit, onVendre: (String, Int) -> Unit) {
    if (personnage == null) return
    val objets = personnage.backpackItems.groupingBy { it }.eachCount().toList().sortedBy { it.first }
    // Prix de rachat de chaque objet du sac (null = le marchand ne sait pas l'estimer).
    val prix by produceState<Map<String, Int?>>(emptyMap(), objets.map { it.first }, personnage.worldId) {
        value = objets.associate { (nom, _) -> nom to runCatching { CommerceBoutiqueUseCase.prixRachat(nom, personnage.worldId) }.getOrNull() }
    }
    if (objets.isEmpty()) {
        Text("Le sac de ${personnage.name} est vide.", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(objets, key = { it.first }) { (nom, nombre) ->
            val p = prix[nom]
            LigneCommerce(
                titre = if (nombre > 1) "$nom ×$nombre" else nom,
                detail = when {
                    nom !in prix -> "Estimation…"
                    p == null -> "Le marchand n'en veut pas (aucun prix de référence)"
                    else -> "Rachat : ${Monnaie.libelle(p)}"
                },
                bouton = "Vendre",
                actif = !enCours && p != null,
                onDetail = { onDetail(nom) },
            ) { p?.let { onVendre(nom, it) } }
        }
    }
}

@Composable
private fun ListeServices(boutique: BoutiqueJoueurData, personnage: Character?, onUtiliser: (ServiceJoueurData) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(boutique.services) { service ->
            val epuise = service.quantiteDisponible == 0
            // Nuit en chambre = repos long : pas proposée tant qu'il est impossible (24 h, 0 PV).
            val blocageNuit = personnage?.takeIf { service.effet == com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.NUIT.name }
                ?.let { com.jc2.jdrcompagnon.ui.screens.joueur.ReglesRepos.blocageReposLong(it, com.jc2.jdrcompagnon.ui.ScenarioClockState.state.value.scenarioMinutes) }
            LigneCommerce(
                titre = "${service.nom} — ${Monnaie.libelle(service.prixEnPc)}",
                detail = listOfNotNull(
                    service.description.ifBlank { null },
                    service.quantiteDisponible?.let { if (it == 0) "complet" else "$it places" },
                    blocageNuit,
                    com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.depuis(service.effet)
                        .takeIf { it != com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.AUCUN && it != com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService.AUTO }
                        ?.let { "Effet : ${it.label}" },
                ).joinToString(" · "),
                bouton = "Utiliser",
                actif = !epuise && blocageNuit == null && personnage != null && Monnaie.totalPc(personnage) >= service.prixEnPc,
            ) { onUtiliser(service) }
        }
    }
}

/**
 * Enseigne d'une boutique (BoutiqueJoueurData.image) quand elle est disponible sur cet appareil
 * (enseigne fournie avec l'app, ou image locale), sinon l'icône de commerce par défaut.
 */
@Composable
fun EnseigneBoutique(image: String?, modifier: Modifier = Modifier, tint: Color = Color.White) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val disponible = image != null && (BoutiqueImageStore.predefinie(image) != null ||
        BoutiqueImageStore.fichier(context, image)?.exists() == true)
    if (disponible) {
        com.jc2.jdrcompagnon.ui.components.VignetteImage(BoutiqueImageStore, image!!, modifier)
    } else {
        androidx.compose.foundation.layout.Box(modifier, contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Default.Storefront, contentDescription = null, tint = tint)
        }
    }
}
