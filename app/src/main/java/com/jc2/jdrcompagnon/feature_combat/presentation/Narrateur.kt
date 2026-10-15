package com.jc2.jdrcompagnon.feature_combat.presentation

import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import kotlin.random.Random

/** Nature d'une ligne du récit : fixe son icône, sa couleur, son son et son animation. */
enum class TypeRecit(val icone: String) {
    ROUND("⚔"),
    TOUR("▶"),
    ACTION("✦"),
    DEPLACEMENT("🏃"),
    TOUCHE("🗡"),
    CRITIQUE("💥"),
    RATE("💨"),
    DEGATS("🩸"),
    SOIN("💚"),
    KO("☠"),
    MORT("💀"),
    ETAT("🌀"),
    SAUVEGARDE("🛡"),
    BOSS("👑"),
    VICTOIRE("🏆"),
    INFO("•"),
}

/**
 * Une ligne du récit de combat. [texte] est la version racontée, pour le MJ ; [table] la version
 * factuelle montrée aux joueurs (appli et page de la table) : « Nécronte attaque Grèche : réussie,
 * 10 dégâts ». Null = ligne réservée au MJ. Aucune des deux ne donne les PV exacts d'un monstre
 * ni les raisons de l'IA ; le détail des dés reste dans le journal technique.
 */
data class Recit(
    val numero: Int,
    val round: Int,
    val type: TypeRecit,
    val texte: String,
    val acteurId: String? = null,
    val cibleId: String? = null,
    // Dégâts subis ou PV rendus : bulle animée au-dessus de la cible sur la table.
    val montant: Int? = null,
    val table: String? = null,
)

/** Ajoute une ligne au récit (les 200 dernières sont gardées). */
internal fun CombatEnCours.raconter(
    type: TypeRecit,
    texte: String,
    acteurId: String? = null,
    cibleId: String? = null,
    montant: Int? = null,
    table: String? = null,
): CombatEnCours = copy(
    recit = (recit + Recit((recit.lastOrNull()?.numero ?: 0) + 1, round, type, texte, acteurId, cibleId, montant, table)).takeLast(200)
)

/**
 * Formulations du récit. Plusieurs tournures par événement, tirées au hasard, pour que le combat
 * ne se lise pas comme un relevé de dés.
 */
object Narrateur {

    private fun <T> List<T>.au(aleatoire: Random): T = this[aleatoire.nextInt(size)]

    /** « 6 dégâts tranchants », « 6 dégâts d'acide », « 6 dégâts de feu ». */
    fun degats(n: Int, type: String?): String {
        val t = type?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return "$n dégât${if (n > 1) "s" else ""}"
        val nature = when {
            t.endsWith("s") -> " $t"
            t.first() in "aeiouyéèh" -> " d'$t"
            else -> " de $t"
        }
        return "$n dégât${if (n > 1) "s" else ""}$nature"
    }

    /** État de santé d'un monstre tel que la table le perçoit (jamais ses PV). */
    fun sante(c: Combattant): String? = when {
        c.pv <= 0 -> null
        c.pvMax <= 0 || c.pv >= c.pvMax -> null
        c.pv * 2 > c.pvMax -> "blessé"
        c.pv * 4 > c.pvMax -> "en sang"
        else -> "à l'agonie"
    }

    fun round(n: Int): String = if (n == 1) "Round 1 — que le combat commence !" else "Round $n"

    fun tour(c: Combattant): String = if (c.estMonstre) "Au tour de ${c.nom}." else "À toi, ${c.nom} !"

    // ── Attaques ──

    private val toucheCac = listOf<(String, String, String) -> String>(
        { a, c, x -> "$a frappe $c : $x fait mouche !" },
        { a, c, x -> "$a trouve une faille dans la garde de $c ($x)." },
        { a, c, x -> "$x : $a atteint $c !" },
        { a, c, x -> "$a s'abat sur $c — $x porte !" },
    )
    private val toucheDistance = listOf<(String, String, String) -> String>(
        { a, c, x -> "$a vise $c : $x fait mouche !" },
        { a, c, x -> "Le tir de $a ($x) atteint $c !" },
        { a, c, x -> "$x : $a touche $c à distance." },
    )
    private val rateCac = listOf<(String, String, String) -> String>(
        { a, c, x -> "$c esquive l'attaque de $a ($x)." },
        { a, c, _ -> "$a frappe dans le vide, $c s'est écarté à temps." },
        { a, c, x -> "La défense de $c arrête le coup de $a ($x)." },
        { a, c, _ -> "$c pare l'assaut de $a." },
    )
    private val rateDistance = listOf<(String, String, String) -> String>(
        { a, c, x -> "Le tir de $a ($x) manque $c." },
        { a, c, _ -> "$c évite de justesse le projectile de $a." },
        { a, c, x -> "$x : $a manque sa cible, $c est indemne." },
    )
    private val critiques = listOf<(String, String, String) -> String>(
        { a, c, x -> "COUP CRITIQUE ! $a frappe $c avec une violence inouïe ($x) !" },
        { a, c, x -> "Coup terrible ! $x : $a transperce la défense de $c !" },
        { a, c, x -> "CRITIQUE ! $a touche $c en plein point faible ($x) !" },
    )
    private val maladresses = listOf<(String, String, String) -> String>(
        { a, c, _ -> "$a trébuche et manque lamentablement $c." },
        { a, _, x -> "Maladresse ! $x de $a part complètement à côté." },
    )

    /** Jet d'attaque : touché, critique, raté ou maladresse (1 naturel). */
    fun attaque(
        attaquant: String,
        cible: String,
        arme: String?,
        touche: Boolean,
        critique: Boolean,
        maladresse: Boolean,
        aDistance: Boolean,
        aleatoire: Random,
    ): Pair<TypeRecit, String> {
        // Arme inconnue (résultat annoncé à la table) : tournures sans nom d'arme.
        val x = arme?.takeIf { it.isNotBlank() } ?: return when {
            critique -> TypeRecit.CRITIQUE to "COUP CRITIQUE ! $attaquant frappe $cible de plein fouet !"
            touche -> TypeRecit.TOUCHE to listOf("$attaquant touche $cible !", "$attaquant frappe $cible.", "$attaquant atteint $cible !").au(aleatoire)
            else -> TypeRecit.RATE to listOf("$cible esquive l'attaque de $attaquant.", "$attaquant manque $cible.", "$cible pare le coup de $attaquant.").au(aleatoire)
        }
        return when {
            critique -> TypeRecit.CRITIQUE to critiques.au(aleatoire)(attaquant, cible, x)
            touche -> TypeRecit.TOUCHE to (if (aDistance) toucheDistance else toucheCac).au(aleatoire)(attaquant, cible, x)
            maladresse -> TypeRecit.RATE to maladresses.au(aleatoire)(attaquant, cible, x)
            else -> TypeRecit.RATE to (if (aDistance) rateDistance else rateCac).au(aleatoire)(attaquant, cible, x)
        }
    }

    fun aDistance(type: TypeAttaqueMonstre?): Boolean = type == TypeAttaqueMonstre.DISTANCE

    // ── Conséquences ──

    /** « Gobelin encaisse 6 dégâts tranchants — en sang. », « Aldric encaisse 6 dégâts (12/18 PV). » */
    fun encaisse(apres: Combattant, n: Int, type: String?, mention: String? = null): String {
        val suite = when {
            apres.pv <= 0 -> ""
            apres.estMonstre -> sante(apres)?.let { " — $it" }.orEmpty()
            else -> " (${apres.pv}/${apres.pvMax} PV)"
        }
        return "${apres.nom} encaisse ${degats(n, type)}${mention?.let { " $it" }.orEmpty()}$suite."
    }

    private val chutesMonstre = listOf<(String) -> String>(
        { c -> "$c s'effondre. Vaincu !" },
        { c -> "$c tombe et ne se relève pas." },
        { c -> "Le coup terrasse $c !" },
        { c -> "$c mord la poussière !" },
    )

    fun chute(c: Combattant, aleatoire: Random): String =
        if (c.estMonstre) chutesMonstre.au(aleatoire)(c.nom) else "${c.nom} s'écroule, inconscient !"

    fun mort(nom: String): String = "$nom rend son dernier souffle…"

    fun soin(apres: Combattant, n: Int, releve: Boolean): String = when {
        releve ->"${apres.nom} rouvre les yeux et se relève ! (+$n PV)"
        apres.estMonstre -> "${apres.nom} reprend des forces."
        else -> "${apres.nom} récupère $n PV (${apres.pv}/${apres.pvMax})."
    }

    /** Jet de sauvegarde : « Aldric esquive de justesse Souffle de feu ! » */
    fun sauvegarde(cible: String, source: String, reussi: Boolean, dexterite: Boolean, sansEffet: Boolean): String {
        val effet = source.substringAfterLast(" — ").substringBefore(" (").ifBlank { source }
        return when {
            reussi && dexterite -> "$cible esquive de justesse $effet" + if (sansEffet) " !" else ", mais n'y échappe pas tout à fait."
            reussi -> "$cible résiste à $effet" + if (sansEffet) " !" else ", mais en ressent les effets."
            else -> "$cible n'échappe pas à $effet !"
        }
    }

    fun etat(nom: String, condition: ConditionCombat): String = when (condition) {
        ConditionCombat.A_TERRE -> "$nom est jeté à terre !"
        ConditionCombat.AGRIPPE -> "$nom est agrippé et ne peut plus bouger."
        ConditionCombat.AVEUGLE -> "$nom est aveuglé !"
        ConditionCombat.CHARME -> "$nom tombe sous le charme…"
        ConditionCombat.EFFRAYE -> "$nom est saisi d'effroi !"
        ConditionCombat.EMPOISONNE -> "Le poison se répand dans les veines de $nom."
        ConditionCombat.ENTRAVE -> "$nom est entravé !"
        ConditionCombat.ETOURDI -> "$nom est étourdi !"
        ConditionCombat.INCAPABLE_D_AGIR -> "$nom est neutralisé !"
        ConditionCombat.INCONSCIENT -> "$nom sombre dans l'inconscience."
        ConditionCombat.INVISIBLE -> "$nom disparaît aux regards !"
        ConditionCombat.PARALYSE -> "$nom est paralysé !"
        ConditionCombat.PETRIFIE -> "$nom se change en pierre !"
        ConditionCombat.SOURD -> "$nom n'entend plus rien."
        ConditionCombat.CONCENTRATION -> "$nom se concentre sur son sort."
    }

    /** Action annoncée (joueur, MJ ou IA hors attaque) : « Brune se met en garde. » */
    fun action(acteur: String, d: DeclarationAction): String {
        val c = d.cibleNom
        val sur = c?.let { " sur $it" }.orEmpty()
        val sort = Regex("""^Lancer\s*:\s*([^(—]+)""").find(d.libelle)?.groupValues?.get(1)?.trim()
        return when {
            sort != null -> "$acteur lance $sort$sur !"
            d.actionId == "attaquer" || d.libelle.startsWith("Attaquer") -> "$acteur passe à l'attaque" + (c?.let { " contre $it" }.orEmpty()) + " !"
            d.actionId == "magie" -> "$acteur fait appel à la magie$sur."
            d.actionId == "dash" -> "$acteur se précipite !"
            d.actionId == "disengage" -> "$acteur se désengage prudemment."
            d.actionId == "dodge" -> "$acteur se met en garde, prêt à esquiver."
            d.actionId == "hide" -> "$acteur tente de se cacher."
            d.actionId == "help" -> "$acteur vient en aide" + (c?.let { " à $it" }.orEmpty()) + "."
            d.actionId == "ready" -> "$acteur se tient prêt à réagir."
            d.actionId == "search" -> "$acteur scrute les environs."
            d.actionId == "study" -> "$acteur étudie" + (c?.let { " $it" } ?: " la situation") + "."
            d.actionId == "influence" -> "$acteur tente d'influencer" + (c?.let { " $it" } ?: " ses adversaires") + "."
            d.actionId == "utilize" -> "$acteur utilise un objet" + (d.detail?.substringBefore(" — ")?.removePrefix("Objet : ")?.takeIf { it.isNotBlank() && it.length < 40 }?.let { " ($it)" }.orEmpty()) + "."
            d.actionId == "improvise" -> "$acteur improvise" + (d.detail?.takeIf { it.isNotBlank() && it.length < 80 }?.let { " : $it" } ?: " quelque chose") + "."
            d.libelle.startsWith("Fuir") -> "$acteur prend la fuite !"
            d.libelle.startsWith("Se précipiter") -> "$acteur se rue en avant" + (c?.let { " vers $it" }.orEmpty()) + " !"
            else -> "$acteur : ${d.libelle.replaceFirstChar { it.lowercase() }}$sur."
        }
    }

    /** Fin d'un déplacement : « Orque arrive au contact d'Aldric. », « Brune s'éloigne à 12 m du Loup. » */
    fun deplacement(acteur: String, autre: String, auContact: Boolean, seRapproche: Boolean, metres: String): String = when {
        auContact -> "$acteur arrive au contact de $autre."
        seRapproche -> "$acteur s'approche à $metres de $autre."
        else -> "$acteur s'éloigne à $metres de $autre."
    }

    /** Déplacement d'une zone du terrain à une autre ; [contact] : adversaires rejoints là-bas. */
    fun deplacementZone(acteur: String, zone: String, contact: List<String>): String =
        if (contact.isNotEmpty()) "$acteur se porte vers $zone et engage ${contact.joinToString()}."
        else "$acteur se déplace vers $zone."

    fun opportunite(acteur: String, autre: String): String =
        "⚠ $acteur quitte le contact de $autre : attaque d'opportunité possible !"

    fun victoire(): String = "Victoire ! Le dernier ennemi est tombé."

    // ── Version table : les faits, sans phrase ──

    /** « Nécronte attaque Grèche : réussie, 10 dégâts », « … : ratée », « … : coup critique, 18 dégâts ». */
    fun tableAttaque(attaquant: String, cible: String, touche: Boolean, critique: Boolean, degats: Int?): String {
        val suite = degats?.takeIf { touche }?.let { ", $it dégât${if (it > 1) "s" else ""}" }.orEmpty()
        return "$attaquant attaque $cible : " + when {
            critique -> "coup critique$suite"
            touche -> "réussie$suite"
            else -> "ratée"
        }
    }

    fun tableDegats(cible: String, n: Int): String = "$cible : $n dégât${if (n > 1) "s" else ""}"

    fun tableSoin(cible: String, n: Int): String = "$cible : +$n PV"

    fun tableChute(c: Combattant): String = if (c.estMonstre) "${c.nom} est vaincu" else "${c.nom} est à terre"

    fun tableSauvegarde(cible: String, reussi: Boolean): String = "$cible : jet de sauvegarde ${if (reussi) "réussi" else "raté"}"

    fun tableEtat(nom: String, condition: ConditionCombat): String = "$nom : ${condition.label.lowercase()}"
}
