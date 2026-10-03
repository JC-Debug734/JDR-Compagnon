package com.jc2.jdrcompagnon.feature_combat.domain.model

/** Caractéristiques d'une arme du SRD utiles au combat (cf. equipement_srd521.md). */
data class ArmeSrd(
    val nom: String,
    // `1d8 perforants`
    val degats: String,
    // `Finesse, Lancer (6/18), Légère`
    val proprietes: String,
    val aDistance: Boolean,
    // `Courante` / `Guerre` (maîtrise), et botte de Maîtrise des armes (`Coup double`…).
    val categorie: String = "",
    val botte: String? = null,
) {
    companion object {
        private val categorieRegex = Regex("""\*\*Catégorie\*\*\s*(.+)""")
        private val botteRegex = Regex("""\*\*Botte\*\*\s*(.+)""")

        /** Depuis une entrée d'equipement_srd521.md (EquipmentItem : nom, dégâts, propriétés, fiche). */
        fun depuisFiche(nom: String, degats: String, proprietes: String, rawMarkdown: String) = ArmeSrd(
            nom = nom,
            degats = degats,
            proprietes = proprietes,
            aDistance = rawMarkdown.contains("**Portée** Distance"),
            categorie = categorieRegex.find(rawMarkdown)?.groupValues?.get(1)?.trim().orEmpty(),
            botte = botteRegex.find(rawMarkdown)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() && it != "-" },
        )
    }
}

/**
 * Attaques d'un personnage (PNJ piloté par l'IA) à partir de ses armes : bonus = modificateur
 * (Force, Dextérité pour une arme à distance, le meilleur des deux avec Finesse) + maîtrise,
 * supposée acquise. Sans arme reconnue : frappe à mains nues.
 */
object ArmesPersonnage {

    private val degatsRegex = Regex("""(\d+d\d+)\s*(\p{L}+)?""")
    private val porteeRegex = Regex("""\((\d+)\s*/\s*(\d+)""")
    // Bonus d'une arme magique : "Hache d'armes +1" (nom) ou "1d8 tranchants (+1)" (dégâts).
    private val bonusNomRegex = Regex("""\+(\d+)\s*$""")
    private val bonusDegatsRegex = Regex("""\(\+(\d+)\)""")

    /** Bonus magique d'une arme (+1, +2...) aux jets d'attaque et de dégâts, 0 sinon. */
    fun bonusMagique(arme: ArmeSrd): Int =
        (bonusNomRegex.find(arme.nom) ?: bonusDegatsRegex.find(arme.degats))?.groupValues?.get(1)?.toIntOrNull() ?: 0

    fun attaques(
        nomsArmes: List<String>,
        armes: List<ArmeSrd>,
        force: Int,
        dexterite: Int,
        maitrise: Int,
    ): List<AttaqueMonstre> {
        val modFor = Math.floorDiv(force - 10, 2)
        val modDex = Math.floorDiv(dexterite - 10, 2)
        val trouvees = nomsArmes.distinct().mapNotNull { nom ->
            armes.firstOrNull { it.nom.equals(nom, ignoreCase = true) }
                ?: armes.filter { nom.contains(it.nom, ignoreCase = true) }.maxByOrNull { it.nom.length }
        }.distinctBy { it.nom }
        val attaques = trouvees.mapNotNull { arme ->
            val m = degatsRegex.find(arme.degats) ?: return@mapNotNull null
            val finesse = arme.proprietes.contains("Finesse", ignoreCase = true)
            val lancer = arme.proprietes.contains("Lancer", ignoreCase = true)
            val magie = bonusMagique(arme)
            val mod = when {
                arme.aDistance -> modDex
                finesse -> maxOf(modFor, modDex)
                else -> modFor
            } + magie
            val formule = m.groupValues[1] + when {
                mod > 0 -> " + $mod"
                mod < 0 -> " - ${-mod}"
                else -> ""
            }
            val (nb, faces) = m.groupValues[1].split('d', 'D').map { it.toInt() }
            AttaqueMonstre(
                nom = arme.nom,
                type = when {
                    arme.aDistance -> TypeAttaqueMonstre.DISTANCE
                    lancer -> TypeAttaqueMonstre.POLYVALENTE
                    else -> TypeAttaqueMonstre.CORPS_A_CORPS
                },
                bonusToucher = mod + maitrise,
                formuleDegats = formule,
                degatsMoyens = (nb * (faces + 1) / 2 + mod).coerceAtLeast(1),
                typeDegats = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() },
                porteeLongue = porteeRegex.find(arme.proprietes)?.groupValues?.get(2)?.toDoubleOrNull(),
            )
        }
        return attaques.ifEmpty {
            listOf(
                AttaqueMonstre(
                    nom = "Mains nues",
                    type = TypeAttaqueMonstre.CORPS_A_CORPS,
                    bonusToucher = modFor + maitrise,
                    degatsMoyens = (1 + modFor).coerceAtLeast(1),
                    typeDegats = "contondants",
                )
            )
        }
    }
}
