package com.jc2.jdrcompagnon.feature_carte.data

import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.components.CatalogueVignettes
import com.jc2.jdrcompagnon.ui.components.VignettePredefinie

/**
 * Emblème d'une ville (PointInteret.embleme, miniature des listes de villes) : emblème fourni
 * avec l'app (res/drawable-nodpi/embleme_ville_*.webp), codé "embleme:<clé>", ou image de
 * l'appareil. Distinct de l'illustration (VilleImageStore), photo plein cadre partagée aux joueurs.
 */
object VilleEmblemeStore : CatalogueVignettes(
    prefixe = "embleme:",
    nomDossier = "ville_emblemes",
    predefinies = listOf(
        VignettePredefinie("royaume", "Cité royale", R.drawable.embleme_ville_royaume),
        VignettePredefinie("village", "Village", R.drawable.embleme_ville_village),
        VignettePredefinie("port", "Port", R.drawable.embleme_ville_port),
        VignettePredefinie("cite_naine", "Cité naine", R.drawable.embleme_ville_cite_naine),
        VignettePredefinie("cite_elfique", "Cité elfique", R.drawable.embleme_ville_cite_elfique),
        VignettePredefinie("cite_desert", "Cité du désert", R.drawable.embleme_ville_cite_desert),
        VignettePredefinie("citadelle_glace", "Citadelle des glaces", R.drawable.embleme_ville_citadelle_glace),
        VignettePredefinie("forteresse_orque", "Forteresse orque", R.drawable.embleme_ville_forteresse_orque),
        VignettePredefinie("cite_maudite", "Cité maudite", R.drawable.embleme_ville_cite_maudite),
        VignettePredefinie("hameau", "Hameau", R.drawable.embleme_ville_hameau),
        VignettePredefinie("bourg_fortifie", "Bourg fortifié", R.drawable.embleme_ville_bourg_fortifie),
        VignettePredefinie("cite_portuaire", "Cité portuaire", R.drawable.embleme_ville_cite_portuaire),
        VignettePredefinie("ville_marchande", "Ville marchande", R.drawable.embleme_ville_ville_marchande),
        VignettePredefinie("capitale_doree", "Capitale dorée", R.drawable.embleme_ville_capitale_doree),
        VignettePredefinie("cite_forteresse", "Cité-forteresse", R.drawable.embleme_ville_cite_forteresse),
        VignettePredefinie("port_insulaire", "Port insulaire", R.drawable.embleme_ville_port_insulaire),
        VignettePredefinie("grande_cite", "Grande cité", R.drawable.embleme_ville_grande_cite),
        VignettePredefinie("cite_blanche", "Cité blanche", R.drawable.embleme_ville_cite_blanche),
    )
)
