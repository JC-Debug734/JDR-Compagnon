package com.jc2.jdrcompagnon.feature_exploration

import android.content.Context
import android.graphics.BitmapFactory
import com.jc2.jdrcompagnon.network.MjWebServer
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

/**
 * Zone d'exploration préparée par le MJ (une pièce, une clairière, un couloir...) : un ensemble
 * de cases que l'on dévoile — ou remasque — d'un seul clic en jeu.
 */
@Serializable
data class ZoneExploration(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String,
    val cases: Set<Int>,
)

/**
 * Lieu (point d'intérêt d'une carte de campagne) posé sur la carte d'exploration : centre de son
 * icône en fractions de l'image ([fx], [fy] entre 0 et 1). [visible] : montré aux joueurs sur la
 * page table, au choix du MJ (même dans une zone encore masquée).
 */
data class LieuExploration(
    val id: String,
    val nom: String,
    val fx: Float,
    val fy: Float,
    val emoji: String,
    val couleurArgb: Int?,
    val visible: Boolean,
)

/**
 * Carte en cours d'exploration : [imageFile] découpée en [colonnes] x [lignes] cases carrées
 * (le nombre de lignes suit les proportions de l'image). Les cases de [revelees] (index =
 * ligne * colonnes + colonne) sont visibles des joueurs ; toutes les autres sont noires sur la
 * page d'affichage table. [zones] : zones préparées, révélables d'un clic.
 */
data class ExplorationEtat(
    val cle: String,
    val titre: String,
    val imageFile: File,
    val colonnes: Int,
    val lignes: Int,
    val revelees: Set<Int>,
    val afficheeSurTable: Boolean,
    val zones: List<ZoneExploration> = emptyList(),
    val lieux: List<LieuExploration> = emptyList(),
) {
    fun index(colonne: Int, ligne: Int): Int = ligne * colonnes + colonne

    /** Zone entièrement révélée ? (une zone vide ne l'est jamais) */
    fun estRevelee(zone: ZoneExploration): Boolean = zone.cases.isNotEmpty() && revelees.containsAll(zone.cases)
}

/** Calculs de grille indépendants d'Android (testables). */
object ExplorationGrille {

    /**
     * Convertit des cases d'une grille [ancColonnes] x [ancLignes] vers une grille
     * [colonnes] x [lignes] couvrant la même image : une nouvelle case est retenue si son centre
     * tombe dans une ancienne case retenue. Sert à garder cases révélées et zones quand le MJ
     * change la taille des cases.
     */
    fun convertir(cases: Set<Int>, ancColonnes: Int, ancLignes: Int, colonnes: Int, lignes: Int): Set<Int> {
        if (cases.isEmpty() || (ancColonnes == colonnes && ancLignes == lignes)) return cases
        val resultat = mutableSetOf<Int>()
        for (l in 0 until lignes) {
            val ancL = (((l + 0.5) / lignes) * ancLignes).toInt().coerceIn(0, ancLignes - 1)
            for (c in 0 until colonnes) {
                val ancC = (((c + 0.5) / colonnes) * ancColonnes).toInt().coerceIn(0, ancColonnes - 1)
                if (ancL * ancColonnes + ancC in cases) resultat += l * colonnes + c
            }
        }
        return resultat
    }

    /** Nombre de lignes pour des cases carrées, d'après les proportions de l'image. */
    fun lignes(colonnes: Int, largeurImage: Int, hauteurImage: Int): Int {
        if (largeurImage <= 0 || hauteurImage <= 0) return colonnes
        return (colonnes * hauteurImage.toFloat() / largeurImage).roundToInt().coerceAtLeast(1)
    }

    /**
     * Cases couvertes par des rectangles exprimés en pixels de l'image (x, y, largeur, hauteur) :
     * une case est retenue si son centre tombe dans l'un d'eux. Sert aux zones préparées dans un
     * fichier de scénario ({mzone:}), indépendamment de la taille de grille choisie.
     */
    fun casesPourRectangles(
        rectangles: List<IntArray>,
        largeurImage: Int,
        hauteurImage: Int,
        colonnes: Int,
        lignes: Int,
    ): Set<Int> {
        val resultat = mutableSetOf<Int>()
        for (l in 0 until lignes) {
            val cy = (l + 0.5) / lignes * hauteurImage
            for (c in 0 until colonnes) {
                val cx = (c + 0.5) / colonnes * largeurImage
                if (rectangles.any { r -> cx >= r[0] && cx < r[0] + r[2] && cy >= r[1] && cy < r[1] + r[3] }) {
                    resultat += l * colonnes + c
                }
            }
        }
        return resultat
    }

    /**
     * Zone à laquelle appartient une case (la plus petite si plusieurs se chevauchent : c'est la
     * plus précise, ex. le placard B4 plutôt que la salle B3), ou null si la case n'est dans aucune.
     */
    fun zonePourCase(zones: List<ZoneExploration>, index: Int): ZoneExploration? =
        zones.filter { index in it.cases }.minByOrNull { it.cases.size }

    /** Révèle la zone si elle n'est pas entièrement visible, sinon la remasque. */
    fun basculerZone(revelees: Set<Int>, zone: ZoneExploration): Set<Int> =
        if (zone.cases.isNotEmpty() && revelees.containsAll(zone.cases)) revelees - zone.cases else revelees + zone.cases
}

/**
 * Système d'exploration (brouillard de guerre) : le MJ ouvre une carte (carte de campagne ou
 * image de scénario), l'envoie sur la page d'affichage table où elle apparaît entièrement noire,
 * puis révèle les lieux où entrent les personnages — case par case, ou d'un clic via une zone
 * préparée à l'avance.
 *
 * Une seule exploration à la fois (comme la page table n'affiche qu'une chose à la fois). Les
 * cases révélées, les zones et la taille de grille sont mémorisées par carte
 * ([ExplorationEtat.cle]) : rouvrir la même carte reprend l'exploration là où elle en était.
 */
object ExplorationSession {

    const val COLONNES_DEFAUT = 20
    val PLAGE_COLONNES = 4..80

    private val json = Json { ignoreUnknownKeys = true }

    private val _etat = MutableStateFlow<ExplorationEtat?>(null)
    val etat: StateFlow<ExplorationEtat?> = _etat.asStateFlow()

    // Écran d'exploration affiché par-dessus l'app (ExplorationOverlay, monté une seule fois
    // dans le NavGraph) ; la carte reste sur la table une fois l'écran fermé.
    private val _ecranOuvert = MutableStateFlow(false)
    val ecranOuvert: StateFlow<Boolean> = _ecranOuvert.asStateFlow()

    // Enregistre le choix « visible des joueurs » d'un lieu (fiche du lieu en base), fourni par
    // la carte de campagne qui a ouvert l'exploration.
    private var persisterVisibiliteLieu: ((id: String, visible: Boolean) -> Unit)? = null

    /**
     * Ouvre l'écran d'exploration du MJ sur cette carte. [lieux] : points d'intérêt de la carte de
     * campagne, que le MJ montre ou cache aux joueurs ([onVisibiliteLieu] l'enregistre).
     */
    fun ouvrirEcran(
        context: Context,
        cle: String,
        titre: String,
        imageFile: File,
        lieux: List<LieuExploration> = emptyList(),
        onVisibiliteLieu: ((id: String, visible: Boolean) -> Unit)? = null,
    ) {
        ouvrir(context, cle, titre, imageFile)
        persisterVisibiliteLieu = onVisibiliteLieu
        majLieux(context, cle, lieux)
        _ecranOuvert.value = true
    }

    /** Lieux de la carte [cle] mis à jour (déplacés, ajoutés, visibilité changée depuis la carte). */
    fun majLieux(context: Context, cle: String, lieux: List<LieuExploration>) {
        val e = _etat.value ?: return
        if (e.cle != cle || e.lieux == lieux) return
        publier(context, e.copy(lieux = lieux))
    }

    /** Montre ou cache un lieu aux joueurs. */
    fun basculerLieu(context: Context, id: String) {
        val e = _etat.value ?: return
        val lieu = e.lieux.firstOrNull { it.id == id } ?: return
        val visible = !lieu.visible
        publier(context, e.copy(lieux = e.lieux.map { if (it.id == id) it.copy(visible = visible) else it }))
        persisterVisibiliteLieu?.invoke(id, visible)
    }

    fun fermerEcran() {
        _ecranOuvert.value = false
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("exploration", Context.MODE_PRIVATE)

    /** Ouvre (ou rouvre) l'exploration de [imageFile] ; [cle] identifie la carte de façon stable. */
    fun ouvrir(context: Context, cle: String, titre: String, imageFile: File) {
        val courant = _etat.value
        if (courant?.cle == cle && courant.imageFile == imageFile) return
        val p = prefs(context)
        val colonnes = p.getInt("colonnes_$cle", COLONNES_DEFAUT).coerceIn(PLAGE_COLONNES)
        val lignes = lignesPour(imageFile, colonnes)
        val total = colonnes * lignes
        val revelees = p.getString("revelees_$cle", "").orEmpty()
            .split(',').mapNotNull { it.toIntOrNull() }.filter { it in 0 until total }.toSet()
        val zones = runCatching {
            json.decodeFromString<List<ZoneExploration>>(p.getString("zones_$cle", "[]").orEmpty())
        }.getOrDefault(emptyList()).map { z -> z.copy(cases = z.cases.filter { it in 0 until total }.toSet()) }
        // Une autre carte est peut-être affichée sur la table : elle y reste jusqu'à ce que le MJ
        // envoie celle-ci (« Envoyer à la table »), avec son propre brouillard.
        publier(context, ExplorationEtat(cle, titre, imageFile, colonnes, lignes, revelees, cleSurTable == cle, zones), majTable = false)
    }

    /** Bascule une case (révélée <-> masquée). */
    fun basculer(context: Context, colonne: Int, ligne: Int) {
        val e = _etat.value ?: return
        if (colonne !in 0 until e.colonnes || ligne !in 0 until e.lignes) return
        val i = e.index(colonne, ligne)
        publier(context, e.copy(revelees = if (i in e.revelees) e.revelees - i else e.revelees + i))
    }

    /** Révèle (ou masque) toutes les cases d'un glissé du doigt. */
    fun definir(context: Context, cases: Collection<Int>, revelee: Boolean) {
        val e = _etat.value ?: return
        val valides = cases.filter { it in 0 until e.colonnes * e.lignes }
        val nouvelles = if (revelee) e.revelees + valides else e.revelees - valides.toSet()
        if (nouvelles != e.revelees) publier(context, e.copy(revelees = nouvelles))
    }

    fun toutReveler(context: Context) {
        val e = _etat.value ?: return
        publier(context, e.copy(revelees = (0 until e.colonnes * e.lignes).toSet()))
    }

    fun toutMasquer(context: Context) {
        val e = _etat.value ?: return
        publier(context, e.copy(revelees = emptySet()))
    }

    /**
     * Remasque toute la carte [cle], ouverte ou non (réinitialisation d'une campagne) : les zones
     * préparées et la taille des cases sont conservées.
     */
    fun reinitialiserBrouillard(context: Context, cle: String) {
        val e = _etat.value
        if (e?.cle == cle) publier(context, e.copy(revelees = emptySet()))
        else prefs(context).edit().remove("revelees_$cle").apply()
    }

    /** Un clic sur une zone : elle est dévoilée d'un coup (ou remasquée si elle l'était déjà). */
    fun basculerZone(context: Context, zoneId: String) {
        val e = _etat.value ?: return
        val zone = e.zones.firstOrNull { it.id == zoneId } ?: return
        publier(context, e.copy(revelees = ExplorationGrille.basculerZone(e.revelees, zone)))
    }

    /** Crée la zone, ou remplace celle de même id. Une zone sans case n'est pas enregistrée. */
    fun enregistrerZone(context: Context, zone: ZoneExploration) {
        val e = _etat.value ?: return
        if (zone.cases.isEmpty() || zone.nom.isBlank()) return
        val zones = if (e.zones.any { it.id == zone.id }) e.zones.map { if (it.id == zone.id) zone else it } else e.zones + zone
        publier(context, e.copy(zones = zones))
    }

    fun supprimerZone(context: Context, zoneId: String) {
        val e = _etat.value ?: return
        publier(context, e.copy(zones = e.zones.filterNot { it.id == zoneId }))
    }

    /** Change la taille des cases : cases révélées et zones sont converties vers la nouvelle grille. */
    fun changerColonnes(context: Context, colonnes: Int) {
        val e = _etat.value ?: return
        val n = colonnes.coerceIn(PLAGE_COLONNES)
        if (n == e.colonnes) return
        val lignes = lignesPour(e.imageFile, n)
        fun convertir(cases: Set<Int>) = ExplorationGrille.convertir(cases, e.colonnes, e.lignes, n, lignes)
        publier(
            context,
            e.copy(
                colonnes = n,
                lignes = lignes,
                revelees = convertir(e.revelees),
                zones = e.zones.map { it.copy(cases = convertir(it.cases)) }
            )
        )
    }

    /** Affiche la carte sur la page table, ou l'en retire. */
    fun afficherSurTable(context: Context, afficher: Boolean) {
        val e = _etat.value ?: return
        publier(context, e.copy(afficheeSurTable = afficher))
    }

    // Titre de la carte d'exploration actuellement sur la table (null = aucune), qui peut ne pas
    // être celle ouverte par le MJ.
    private val _titreSurTable = MutableStateFlow<String?>(null)
    val titreSurTable: StateFlow<String?> = _titreSurTable.asStateFlow()
    private var cleSurTable: String? = null

    /**
     * [majTable] faux : la carte vient d'être ouverte, la table garde ce qu'elle affiche. Sinon
     * la table suit la carte ouverte — si c'est elle qui y est, ou si le MJ l'y envoie.
     */
    private fun publier(context: Context, e: ExplorationEtat, majTable: Boolean = true) {
        _etat.value = e
        prefs(context).edit()
            .putInt("colonnes_${e.cle}", e.colonnes)
            .putString("revelees_${e.cle}", e.revelees.sorted().joinToString(","))
            .putString("zones_${e.cle}", json.encodeToString(e.zones))
            .apply()
        // Carte non envoyée, alors qu'une autre est sur la table : on n'y touche pas.
        if (!majTable || (!e.afficheeSurTable && cleSurTable != null && cleSurTable != e.cle)) return
        cleSurTable = e.cle.takeIf { e.afficheeSurTable }
        _titreSurTable.value = e.titre.takeIf { e.afficheeSurTable }
        NetworkSessionManager.updateTableExploration(
            if (e.afficheeSurTable) {
                MjWebServer.ExplorationInfo(
                    e.imageFile, e.colonnes, e.lignes, e.revelees,
                    // Seuls les lieux que le MJ a rendus visibles partent sur la table.
                    lieux = e.lieux.filter { it.visible }.map { MjWebServer.LieuTable(it.nom, it.fx, it.fy, it.emoji, it.couleurArgb) },
                )
            } else null
        )
    }

    /**
     * Prépare l'exploration d'une carte avant sa première ouverture (zones livrées avec un
     * scénario importé). Ne touche pas à une carte déjà préparée ou explorée par le MJ.
     */
    fun preparerCarte(context: Context, cle: String, colonnes: Int, zones: List<ZoneExploration>) {
        val p = prefs(context)
        if (p.contains("zones_$cle") || p.contains("revelees_$cle")) return
        p.edit()
            .putInt("colonnes_$cle", colonnes.coerceIn(PLAGE_COLONNES))
            .putString("zones_$cle", json.encodeToString(zones.filter { it.cases.isNotEmpty() }))
            .apply()
    }

    /** Nombre de lignes pour des cases carrées, d'après les proportions de l'image. */
    private fun lignesPour(imageFile: File, colonnes: Int): Int {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)
        return ExplorationGrille.lignes(colonnes, options.outWidth, options.outHeight)
    }
}
