package com.jc2.jdrcompagnon.feature_boutique.data

import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.components.CatalogueVignettes
import com.jc2.jdrcompagnon.ui.components.VignettePredefinie

/**
 * Image d'une boutique (Boutique.image, miniature de la liste) : enseigne fournie avec l'app
 * (res/drawable-nodpi/enseigne_*.webp), codée "enseigne:<clé>", ou image de l'appareil.
 */
object BoutiqueImageStore : CatalogueVignettes(
    prefixe = "enseigne:",
    nomDossier = "boutique_images",
    predefinies = listOf(
        VignettePredefinie("forge", "Forge", R.drawable.enseigne_forge),
        VignettePredefinie("forge_2", "Forge (2)", R.drawable.enseigne_forge_2),
        VignettePredefinie("forge_3", "Forge (3)", R.drawable.enseigne_forge_3),
        VignettePredefinie("armurerie", "Armurerie", R.drawable.enseigne_armurerie),
        VignettePredefinie("armure", "Armures", R.drawable.enseigne_armure),
        VignettePredefinie("taverne", "Taverne", R.drawable.enseigne_taverne),
        VignettePredefinie("taverne_2", "Taverne (2)", R.drawable.enseigne_taverne_2),
        VignettePredefinie("auberge", "Auberge", R.drawable.enseigne_auberge),
        VignettePredefinie("boulangerie", "Boulangerie", R.drawable.enseigne_boulangerie),
        VignettePredefinie("boulangerie_2", "Boulangerie (2)", R.drawable.enseigne_boulangerie_2),
        VignettePredefinie("herboriste", "Herboriste", R.drawable.enseigne_herboriste),
        VignettePredefinie("alchimiste", "Alchimiste", R.drawable.enseigne_alchimiste),
        VignettePredefinie("alchimiste_2", "Alchimiste (2)", R.drawable.enseigne_alchimiste_2),
        VignettePredefinie("mage", "Mage", R.drawable.enseigne_mage),
        VignettePredefinie("mage_2", "Mage (2)", R.drawable.enseigne_mage_2),
        VignettePredefinie("grimoire", "Grimoires", R.drawable.enseigne_grimoire),
        VignettePredefinie("scribe", "Scribe", R.drawable.enseigne_scribe),
        VignettePredefinie("cartographe", "Cartographe", R.drawable.enseigne_cartographe),
        VignettePredefinie("explorateur", "Explorateur", R.drawable.enseigne_explorateur),
        VignettePredefinie("maroquinier", "Maroquinier", R.drawable.enseigne_maroquinier),
        VignettePredefinie("bazar", "Bazar", R.drawable.enseigne_bazar),
        VignettePredefinie("bazar_2", "Bazar (2)", R.drawable.enseigne_bazar_2),
        VignettePredefinie("joaillier", "Joaillier", R.drawable.enseigne_joaillier),
        VignettePredefinie("tailleur", "Tailleur", R.drawable.enseigne_tailleur),
        VignettePredefinie("ecurie", "Écurie", R.drawable.enseigne_ecurie),
        VignettePredefinie("ecurie_2", "Écurie (2)", R.drawable.enseigne_ecurie_2),
        VignettePredefinie("pecheur", "Pêcheur", R.drawable.enseigne_pecheur),
    )
)
