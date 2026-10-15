package com.jc2.jdrcompagnon.feature_combat.domain.model

/**
 * Sorts offensifs d'un personnage (PJ ou PNJ) piloté par l'IA, sous la forme des actions de monstre
 * qu'elle sait choisir (IaMonstre) : attaque de sort (corps à corps ou à distance) ou sort à jet de
 * sauvegarde, de zone ou non. Seuls les sorts qui infligent des dégâts et se lancent en une action
 * sont retenus. Un sort de niveau 1+ est limité, pour le combat, au nombre d'emplacements qui
 * restent au personnage pour son niveau ou plus (Magie de pacte comprise).
 */
object SortsIa {

    private val metresRegex = Regex("""(\d+(?:[,.]\d+)?)\s*m""")
    private val desRegex = Regex("""(\d+)d(\d+)""")
    private val bonusRegex = Regex("""([+-])\s*(\d+)\s*$""")

    fun attaques(sorts: List<SortPret>, emplacementsRestants: Map<Int, Int>, pacteRestant: Int): List<AttaqueMonstre> =
        sorts.mapNotNull { s ->
            val formule = s.formuleDegats ?: return@mapNotNull null
            if (s.jet == JetSort.AUCUN || !seLanceEnUneAction(s.tempsIncantation)) return@mapNotNull null
            val usages = if (s.niveau == 0) null else {
                (emplacementsRestants.filterKeys { it >= s.niveau }.values.sum() + pacteRestant).takeIf { it > 0 }
                    ?: return@mapNotNull null
            }
            val portee = metresRegex.find(s.portee)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            val contact = s.portee.contains("contact", ignoreCase = true)
            AttaqueMonstre(
                nom = s.nom,
                type = when {
                    s.jet == JetSort.SAUVEGARDE -> TypeAttaqueMonstre.SAUVEGARDE
                    contact -> TypeAttaqueMonstre.CORPS_A_CORPS
                    else -> TypeAttaqueMonstre.DISTANCE
                },
                bonusToucher = s.bonusAttaque.takeIf { s.jet == JetSort.ATTAQUE },
                formuleDegats = formule,
                degatsMoyens = moyenne(formule),
                typeDegats = s.typeDegats,
                sauvegarde = s.sauvegarde.takeIf { s.jet == JetSort.SAUVEGARDE },
                dd = s.dd.takeIf { s.jet == JetSort.SAUVEGARDE },
                usagesParJour = usages,
                zone = s.zone,
                porteeLongue = portee.takeIf { s.jet == JetSort.ATTAQUE && !contact },
                zoneEffet = s.zoneEffet,
                porteeSauvegarde = portee.takeIf { s.jet == JetSort.SAUVEGARDE },
            )
        }

    /** « 1 action » oui ; action bonus, réaction, minutes ou heures non (pas de jet d'IA pour eux). */
    private fun seLanceEnUneAction(temps: String): Boolean {
        val t = temps.lowercase()
        return t.isBlank() || (t.contains("action") && !t.contains("bonus") && !t.contains("réaction") && !t.contains("reaction"))
    }

    /** Dégâts moyens d'une formule « 2d6 + 3 » (arrondis à l'inférieur). */
    internal fun moyenne(formule: String): Int {
        val des = desRegex.findAll(formule).sumOf { m -> m.groupValues[1].toInt() * (m.groupValues[2].toInt() + 1) / 2.0 }
        val bonus = bonusRegex.find(formule.trim())?.let { m -> m.groupValues[2].toInt() * if (m.groupValues[1] == "-") -1 else 1 } ?: 0
        return (des + bonus).toInt().coerceAtLeast(0)
    }
}
