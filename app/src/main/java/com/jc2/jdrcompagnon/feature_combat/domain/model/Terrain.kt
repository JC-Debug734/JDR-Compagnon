package com.jc2.jdrcompagnon.feature_combat.domain.model

import kotlin.math.abs
import kotlin.math.floor

/**
 * Le champ de bataille en 4 zones alignées, du côté du groupe vers celui des ennemis. Remplace un
 * placement au mètre près par une règle que toute la table comprend d'un coup d'œil :
 *
 * - deux adversaires dans la même zone sont **au contact** ; deux alliés y sont groupés (3 m) ;
 * - chaque zone d'écart compte **9 m** : une vitesse de 9 m fait passer d'une zone à la voisine,
 *   « Se précipiter » de deux ;
 * - quitter une zone où se tient un adversaire debout, sans se désengager, lui ouvre une attaque
 *   d'opportunité.
 *
 * Les personnages commencent sur la ligne du groupe, les monstres sur la ligne ennemie (9 m).
 */
object Terrain {
    const val PAS_METRES = 9.0

    val zones = listOf("Arrière du groupe", "Ligne du groupe", "Ligne ennemie", "Arrière ennemi")
    val zonesCourtes = listOf("Arrière", "Groupe", "Ennemis", "Arr. ennemi")
    val derniere: Int get() = zones.lastIndex

    fun zoneDepart(estMonstre: Boolean): Int = if (estMonstre) 2 else 1

    fun zoneDe(c: Combattant): Int = (c.zone ?: zoneDepart(c.estMonstre)).coerceIn(0, derniere)

    fun nom(zone: Int): String = zones.getOrElse(zone) { zones.last() }

    /** Dans une phrase : « vers la ligne ennemie », « vers l'arrière du groupe ». */
    fun phrase(zone: Int): String = listOf("l'arrière du groupe", "la ligne du groupe", "la ligne ennemie", "l'arrière ennemi")
        .getOrElse(zone) { "l'arrière ennemi" }

    /** Distance entre deux combattants d'après leurs zones. */
    fun distance(a: Combattant, b: Combattant): Distance = distanceEntre(zoneDe(a), a.estMonstre, zoneDe(b), b.estMonstre)

    fun distanceEntre(zoneA: Int, monstreA: Boolean, zoneB: Int, monstreB: Boolean): Distance {
        val ecart = abs(zoneA - zoneB)
        if (ecart == 0) return if (monstreA != monstreB) Distance.CONTACT else Distance.ALLIES_PAR_DEFAUT
        return Distance.depuisMetres(ecart * PAS_METRES)
    }

    /** Zones parcourues au plus avec [metres] de déplacement (au moins une si on peut bouger). */
    fun pasMax(metres: Double): Int = if (metres <= 0.0) 0 else floor(metres / PAS_METRES + 0.01).toInt().coerceAtLeast(1)

    /**
     * Zone où [acteur] finit au plus près de [visee] par rapport à [autre] (au contact, à 9 m…) ;
     * à écart égal, la plus proche de sa zone actuelle.
     */
    fun zonePour(acteur: Combattant, autre: Combattant, visee: Distance): Int {
        val depart = zoneDe(acteur)
        val zoneAutre = zoneDe(autre)
        return (0..derniere).minWith(
            compareBy<Int> { z -> abs(distanceEntre(z, acteur.estMonstre, zoneAutre, autre.estMonstre).metres - visee.metres) }
                .thenBy { z -> abs(z - depart) }
        )
    }

    /** Zone atteinte en allant vers [visee] depuis [depart] avec au plus [pas] zones. */
    fun versZone(depart: Int, visee: Int, pas: Int): Int = (depart + (visee - depart).coerceIn(-pas, pas)).coerceIn(0, derniere)

    /** Recul d'une zone, du côté de son camp (s'éloigner du camp adverse). */
    fun recul(c: Combattant): Int = (zoneDe(c) + if (c.estMonstre) 1 else -1).coerceIn(0, derniere)
}
