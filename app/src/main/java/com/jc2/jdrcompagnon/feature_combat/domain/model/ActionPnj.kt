package com.jc2.jdrcompagnon.feature_combat.domain.model

import kotlinx.serialization.Serializable

/** Nature d'une action de PNJ : jet d'attaque au contact, à distance, ou jet de sauvegarde imposé. */
@Serializable
enum class TypeActionPnj(val label: String) {
    CORPS_A_CORPS("Corps à corps"),
    DISTANCE("À distance"),
    SAUVEGARDE("Jet de sauvegarde"),
}

/**
 * Action d'un PNJ façon bloc de stats de monstre : bonus et dégâts fixés une fois pour toutes
 * (« Épée. Corps à corps : +3, allonge 1,50 m. Touché : 5 (1d8 + 1) dégâts tranchants »), sans
 * arme équipée ni calcul depuis la fiche. Convertie en [AttaqueMonstre] via la ligne de bloc de
 * stats et [ActionsMonstreParser], pour que l'IA de combat la traite comme celle d'un monstre.
 */
@Serializable
data class ActionPnj(
    val id: String = java.util.UUID.randomUUID().toString(),
    // Peut porter « (Recharge 5–6) » ou « (2/jour) », comme dans le bestiaire.
    val nom: String,
    val type: TypeActionPnj = TypeActionPnj.CORPS_A_CORPS,
    val bonus: Int = 0,
    // Formule de dégâts : « 1d8+1 », « 2d6 ». Vide = pas de dégâts (effet seul).
    val degats: String = "",
    val typeDegats: String = "",
    // Jet de sauvegarde : caractéristique (« Dextérité ») et DD.
    val sauvegarde: String = "Dextérité",
    val dd: Int = 12,
    // Portée en mètres : « 24/96 » à distance, ou « cône de 9 m » pour une sauvegarde ; vide = contact.
    val portee: String = "",
    // Texte libre ajouté à la fin (état infligé, effet spécial...).
    val effet: String = "",
) {
    private val formule: String
        get() = degats.trim().replace(Regex("""\s*([+\-−–])\s*"""), " $1 ")

    val moyenne: Int get() = if (degats.isBlank()) 0 else SortsIa.moyenne(degats.replace('−', '-').replace('–', '-'))

    private val degatsTexte: String
        // Le type de dégâts est indispensable à la lecture de la ligne (cf. ActionsMonstreParser) : contondants par défaut.
        get() = if (degats.isBlank()) "" else "$moyenne ($formule) dégâts " +
            typeDegats.trim().ifBlank { "contondants" }.let { t -> if (type == TypeActionPnj.SAUVEGARDE && !t.startsWith("de ") && !t.startsWith("d'")) "de $t" else t }

    /** Ligne de bloc de stats, au format du bestiaire (lue par [ActionsMonstreParser]). */
    val ligneBloc: String
        get() {
            val signe = if (bonus >= 0) "+$bonus" else "−${-bonus}"
            val corps = when (type) {
                TypeActionPnj.CORPS_A_CORPS ->
                    "Corps à corps : $signe, allonge ${portee.ifBlank { "1,50" }.removeSuffix(" m")} m." +
                        (if (degatsTexte.isNotBlank()) " Touché : ${degatsTexte.trim()}." else "")
                TypeActionPnj.DISTANCE ->
                    "À distance : $signe, portée ${portee.ifBlank { "24/96" }.removeSuffix(" m")} m." +
                        (if (degatsTexte.isNotBlank()) " Touché : ${degatsTexte.trim()}." else "")
                TypeActionPnj.SAUVEGARDE ->
                    "JS $sauvegarde : DD $dd" + (portee.takeIf { it.isNotBlank() }?.let { ", $it" } ?: "") + "." +
                        (if (degatsTexte.isNotBlank()) " Échec : ${degatsTexte.trim()}. Réussite : moitié des dégâts." else "")
            }
            return "${nom.trim().ifBlank { "Attaque" }}. $corps" + (effet.trim().takeIf { it.isNotBlank() }?.let { " $it" } ?: "")
        }

    fun versAttaque(): AttaqueMonstre? = ActionsMonstreParser.parser("## Actions\n$ligneBloc").attaques.firstOrNull()

    companion object {
        /** Action fixe reprise d'une attaque calculée (armes du PNJ, cf. ArmesPersonnage). */
        fun depuis(attaque: AttaqueMonstre): ActionPnj = ActionPnj(
            nom = attaque.nom,
            type = if (attaque.type == TypeAttaqueMonstre.DISTANCE) TypeActionPnj.DISTANCE else TypeActionPnj.CORPS_A_CORPS,
            bonus = attaque.bonusToucher ?: 0,
            degats = attaque.formuleDegats.orEmpty(),
            typeDegats = attaque.typeDegats.orEmpty(),
            portee = if (attaque.type == TypeAttaqueMonstre.DISTANCE) {
                attaque.porteeLongue?.let { "${(it / 4).toInt()}/${it.toInt()}" }.orEmpty()
            } else "",
        )
    }
}
