package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret

/** Palette d'icônes proposées pour personnaliser un point sur la carte, indépendamment de son [TypePointInteret]. */
object IconesPointInteret {

    // Les clés sont enregistrées avec chaque point (PointInteret.iconKey) : ne pas les renommer.
    val PALETTE: List<Pair<String, ImageVector>> = listOf(
        "ville" to Icons.Default.LocationCity,
        "village" to IconesLieuxDnd.village,
        "chateau" to IconesLieuxDnd.chateau,
        "taverne" to IconesLieuxDnd.taverne,
        "temple" to IconesLieuxDnd.temple,
        "tour_mage" to IconesLieuxDnd.tourMage,
        "donjon" to IconesLieuxDnd.donjon,
        "grotte" to IconesLieuxDnd.grotte,
        "dragon" to IconesLieuxDnd.dragon,
        "crane" to IconesLieuxDnd.crane,
        "cimetiere" to IconesLieuxDnd.cimetiere,
        "portail" to IconesLieuxDnd.portail,
        "tresor" to IconesLieuxDnd.tresor,
        "mine" to IconesLieuxDnd.mine,
        "port" to IconesLieuxDnd.port,
        "pont" to IconesLieuxDnd.pont,
        "foret" to IconesLieuxDnd.foret,
        "montagne" to IconesLieuxDnd.montagne,
        "campement" to IconesLieuxDnd.campement,
        "ruine" to IconesLieuxDnd.ruine,
        "lieu" to Icons.Default.Place,
        "maison" to Icons.Default.Home,
        "etoile" to Icons.Default.Star,
        "alerte" to Icons.Default.Warning
    )

    private val paletteParCle = PALETTE.toMap()

    /** Nom lisible de chaque icône (description d'accessibilité du sélecteur). */
    val LIBELLES: Map<String, String> = mapOf(
        "groupe" to "Groupe", "ville" to "Ville", "village" to "Village", "chateau" to "Château", "taverne" to "Taverne",
        "temple" to "Temple", "tour_mage" to "Tour de mage", "donjon" to "Donjon", "grotte" to "Grotte",
        "dragon" to "Repaire de dragon", "crane" to "Repaire de monstres", "cimetiere" to "Cimetière",
        "portail" to "Portail magique", "tresor" to "Trésor", "mine" to "Mine", "port" to "Port",
        "pont" to "Pont", "foret" to "Forêt", "montagne" to "Montagne", "campement" to "Campement",
        "ruine" to "Ruines", "lieu" to "Lieu", "maison" to "Maison", "etoile" to "Étoile", "alerte" to "Danger"
    )

    private val cleParDefaut: Map<TypePointInteret, String> = mapOf(
        TypePointInteret.VILLE to "ville",
        TypePointInteret.DONJON to "donjon",
        TypePointInteret.CAMPEMENT to "campement",
        TypePointInteret.RUINE to "ruine",
        TypePointInteret.AUTRE to "lieu"
    )

    // Équivalent de chaque icône pour la page table (navigateur des joueurs) : pas d'icône Material.
    private val emojiParCle: Map<String, String> = mapOf(
        "ville" to "🏙️", "village" to "🏘️", "chateau" to "🏰", "taverne" to "🍺", "temple" to "🏛️",
        "tour_mage" to "🔮", "donjon" to "🏯", "grotte" to "🕳️", "dragon" to "🐉", "crane" to "💀",
        "cimetiere" to "🪦", "portail" to "🌀", "tresor" to "💰", "mine" to "⛏️", "port" to "⚓",
        "pont" to "🌉", "foret" to "🌲", "montagne" to "⛰️", "campement" to "⛺", "ruine" to "🏚️",
        "lieu" to "📍", "maison" to "🏠", "etoile" to "⭐", "alerte" to "⚠️"
    )

    fun emojiPour(type: TypePointInteret, iconKey: String?): String =
        (iconKey?.takeIf { it in emojiParCle } ?: cleParDefaut[type])?.let { emojiParCle[it] } ?: "📍"

    /** Icônes proposées pour un groupe (MjGroup.iconKey) : la « compagnie » par défaut, puis celles des lieux. */
    val PALETTE_GROUPE: List<Pair<String, ImageVector>> = listOf("groupe" to Icons.Default.Groups) + PALETTE

    /** Couleur par défaut de l'icône d'un groupe (dorée). */
    const val COULEUR_GROUPE_DEFAUT: Int = 0xFFFFB300.toInt()

    fun iconeGroupe(iconKey: String?): ImageVector =
        iconKey?.let { cle -> PALETTE_GROUPE.firstOrNull { it.first == cle }?.second } ?: Icons.Default.Groups

    /** [iconKey] choisi manuellement par le MJ, sinon icône par défaut selon [type]. */
    fun iconePour(type: TypePointInteret, iconKey: String?): ImageVector =
        iconKey?.let { paletteParCle[it] } ?: paletteParCle[cleParDefaut[type]] ?: Icons.Default.Place
}
