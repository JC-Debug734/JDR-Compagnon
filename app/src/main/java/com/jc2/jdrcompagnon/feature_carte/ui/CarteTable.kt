package com.jc2.jdrcompagnon.feature_carte.ui

import android.content.Context
import com.jc2.jdrcompagnon.feature_carte.data.CarteGrillePrefs
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.data.CartePointPrefs
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.presentation.TraceCarteSession
import com.jc2.jdrcompagnon.network.TraceCarteData
import com.jc2.jdrcompagnon.network.MjWebServer
import com.jc2.jdrcompagnon.network.NetworkSessionManager

/**
 * Carte de campagne affichée sur la page table (MjWebServer) : son image, sans brouillard, avec les
 * lieux « Visibles des joueurs » par-dessus — la même règle que sur l'appareil des joueurs. Passe par
 * l'affichage des cartes d'exploration (une seule case, entièrement révélée) qui sait déjà dessiner
 * les lieux et suivre l'orientation de la table.
 */
object CarteTable {

    /**
     * Envoie [carte] et ses lieux visibles à la table. Faux si la carte n'a pas d'image ou si la page
     * table n'est pas en ligne (partie non hébergée).
     */
    fun afficher(context: Context, carte: CarteCampagne, points: List<PointInteret>): Boolean {
        val image = carte.imageFileName?.let { CarteImageStore.fichier(context, it) }?.takeIf { it.exists() } ?: return false
        if (!NetworkSessionManager.isTableDisplayOnline) return false
        NetworkSessionManager.updateTableExploration(info(context, image, carte, points))
        return true
    }

    /**
     * Lieux de [carte] modifiés (visibilité, position, nom...) : la table suit si c'est bien cette
     * carte qu'elle affiche (une autre carte ou une exploration envoyée depuis n'est pas remplacée).
     */
    fun majSiAffichee(context: Context, carte: CarteCampagne, points: List<PointInteret>) {
        val image = carte.imageFileName?.let { CarteImageStore.fichier(context, it) } ?: return
        val surTable = NetworkSessionManager.explorationSurTable ?: return
        if (surTable.imageFile != image || surTable.colonnes != 1 || surTable.lignes != 1) return
        val info = info(context, image, carte, points)
        if (info != surTable) NetworkSessionManager.updateTableExploration(info)
    }

    private fun info(context: Context, image: java.io.File, carte: CarteCampagne, points: List<PointInteret>): MjWebServer.ExplorationInfo {
        // Taille de la carte sans zoom sur l'écran du MJ (dp) : celle de son image de fond.
        val bornes = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(image.absolutePath, bornes)
        val densite = context.resources.displayMetrics.density
        val largeurDp = (bornes.outWidth / densite).takeIf { it > 0f } ?: (TAILLE_CASE_REFERENCE_DP * carte.largeurCases)
        val hauteurDp = (bornes.outHeight / densite).takeIf { it > 0f } ?: (TAILLE_CASE_REFERENCE_DP * carte.hauteurCases)

        val placesIci = points.filter { it.carteId == carte.id }
        fun position(p: PointInteret): Pair<Float, Float> =
            if (p.fx != null && p.fy != null) p.fx to p.fy
            // Point sans position libre (lieu importé) : centre de sa case de la grille de la carte (comme positionDe).
            else ((p.x + 0.5f) / carte.largeurCases.coerceAtLeast(1)).coerceIn(0f, 1f) to
                ((p.y + 0.5f) / carte.hauteurCases.coerceAtLeast(1)).coerceIn(0f, 1f)
        // Même taille de base que l'écran du MJ : réduite quand des lieux se touchent (calculée sur
        // tous les lieux de la carte, visibles ou non, comme sur son écran).
        val tailleBaseDp = tailleIconesSansChevauchement(
            placesIci.map { p -> position(p).let { (fx, fy) -> fx * largeurDp to fy * hauteurDp } },
            TAILLE_CASE_REFERENCE_DP,
        )
        val nomsAffiches = CarteGrillePrefs.afficherNoms(context, carte.id)
        val (trace, zoom) = traceEtCadre(TraceCarteSession.etat.value, carte.id)
        return MjWebServer.ExplorationInfo(
            imageFile = image,
            colonnes = 1,
            lignes = 1,
            revelees = setOf(0),
            carteId = carte.id,
            trace = trace,
            traceTermine = TraceCarteSession.etat.value?.takeIf { it.carteId == carte.id }?.termine == true,
            zoom = zoom,
            lieux = placesIci.filter { it.visibleJoueurs }.map { p ->
                val (fx, fy) = position(p)
                // Réglages du MJ pour ce lieu : taille de l'icône, nom affiché et son côté.
                val reglage = CartePointPrefs.lire(context, p.id)
                MjWebServer.LieuTable(
                    nom = p.nom,
                    fx = fx,
                    fy = fy,
                    emoji = IconesPointInteret.emojiPour(p.type, p.iconKey),
                    couleurArgb = p.couleurArgb,
                    taille = tailleBaseDp * reglage.taille / largeurDp,
                    afficherNom = nomsAffiches && reglage.afficherTexte,
                    positionNom = reglage.positionTexte.name,
                )
            },
        )
    }

    /**
     * Tracé de la carte modifié (MJ ou joueur, voir TraceCarteSession) : la table le dessine si elle
     * affiche cette carte, et cadre le trajet une fois le tracé terminé (comme les écrans).
     */
    fun majTrace(data: TraceCarteData?) {
        val surTable = NetworkSessionManager.explorationSurTable ?: return
        val carteId = surTable.carteId ?: return
        val (trace, zoom) = traceEtCadre(data, carteId)
        val info = surTable.copy(
            trace = trace,
            traceTermine = data?.takeIf { it.carteId == carteId }?.termine == true,
            zoom = zoom,
        )
        if (info != surTable) NetworkSessionManager.updateTableExploration(info)
    }

    /**
     * Étapes du tracé de [carteId] et cadre de zoom de la table : le trajet terminé et sa marge,
     * sans grossir à l'excès un trajet très court (même règle que l'écran de carte) ; null tant
     * que le tracé n'est pas terminé (carte entière).
     */
    internal fun traceEtCadre(data: TraceCarteData?, carteId: String): Pair<List<Pair<Float, Float>>, MjWebServer.ZoomTable?> {
        val etapes = data?.takeIf { it.carteId == carteId }?.etapes.orEmpty().map { it.fx to it.fy }
        if (data?.termine != true || etapes.size < 2) return etapes to null
        val minX = etapes.minOf { it.first }
        val maxX = etapes.maxOf { it.first }
        val minY = etapes.minOf { it.second }
        val maxY = etapes.maxOf { it.second }
        val largeur = ((maxX - minX) * 1.5f).coerceIn(0.25f, 1f)
        val hauteur = ((maxY - minY) * 1.5f).coerceIn(0.25f, 1f)
        val x = ((minX + maxX) / 2f - largeur / 2f).coerceIn(0f, 1f - largeur)
        val y = ((minY + maxY) / 2f - hauteur / 2f).coerceIn(0f, 1f - hauteur)
        return etapes to MjWebServer.ZoomTable(x, y, largeur, hauteur)
    }

    // Taille (dp) d'une case de référence : même valeur que TAILLE_CASE_REFERENCE de l'écran de carte.
    private const val TAILLE_CASE_REFERENCE_DP = 36f}
