package com.jc2.jdrcompagnon.feature_combat.domain.model

import kotlin.random.Random

/** Nature d'une action offensive de monstre, lue dans la section « ## Actions » de sa fiche. */
enum class TypeAttaqueMonstre { CORPS_A_CORPS, DISTANCE, POLYVALENTE, SAUVEGARDE, AUTRE }

/**
 * Une action de la fiche d'un monstre (`Cimeterre. Corps à corps : +4, … Touché : 5 (1d6 + 2)
 * dégâts tranchants`, `Souffle (Recharge 5–6). JS Dextérité : DD 13, … Échec : 21 (6d6)…`).
 * [recharge] est la valeur minimale du d6 pour la récupérer (5 pour « Recharge 5–6 »),
 * [usagesParJour] le nombre d'utilisations d'une capacité « (2/jour) ».
 */
data class AttaqueMonstre(
    val nom: String,
    val type: TypeAttaqueMonstre,
    val bonusToucher: Int? = null,
    val formuleDegats: String? = null,
    val degatsMoyens: Int = 0,
    val typeDegats: String? = null,
    val sauvegarde: String? = null,
    val dd: Int? = null,
    val recharge: Int? = null,
    val usagesParJour: Int? = null,
    val zone: Boolean = false,
    // Portée longue en mètres d'une attaque à distance (`portée 24/96 m` → 96), null si absente.
    val porteeLongue: Double? = null,
    // États infligés (« subit l’état Agrippé ») : d'office si l'attaque touche, ou sur un JS raté.
    val conditions: List<ConditionInfligee> = emptyList(),
    // Allonge au corps à corps en mètres (« allonge 3 m ») ; 1,50 m par défaut.
    val allonge: Double = Distance.CONTACT.metres,
    // Forme et taille de la zone (« cône de 9 m ») pour proposer les créatures touchées.
    val zoneEffet: ZoneEffet? = null,
    // Portée d'une capacité à jet de sauvegarde sans zone (« portée 18 m »), sinon 18 m supposés.
    val porteeSauvegarde: Double? = null,
) {
    val corpsACorps: Boolean get() = type == TypeAttaqueMonstre.CORPS_A_CORPS || type == TypeAttaqueMonstre.POLYVALENTE

    /**
     * L'action atteint-elle une cible à [distance] sans se déplacer ? Corps à corps : dans son
     * allonge (1,50 m, parfois 3 m). À distance : jusqu'à sa portée longue (30 m si inconnue),
     * une arme polyvalente frappant aussi au contact. Jet de sauvegarde : la longueur de sa zone
     * (cône, ligne…) ou sa portée.
     */
    fun atteint(distance: Distance): Boolean = when (type) {
        TypeAttaqueMonstre.CORPS_A_CORPS -> distance.metres <= allonge + 0.01
        TypeAttaqueMonstre.DISTANCE -> distance.metres <= (porteeLongue ?: Distance.LONGUE.metres) + 0.01
        TypeAttaqueMonstre.POLYVALENTE ->
            distance.metres <= allonge + 0.01 || distance.metres <= (porteeLongue ?: Distance.LONGUE.metres) + 0.01
        TypeAttaqueMonstre.SAUVEGARDE -> distance.metres <= (zoneEffet?.takeIf { it.forme.depuisLanceur }?.metres
            ?: porteeSauvegarde ?: Distance.M18.metres) + 0.01
        TypeAttaqueMonstre.AUTRE -> false
    }

    /** Capacité à usage limité (recharge ou x/jour), jouée en priorité par certains profils. */
    val estSpeciale: Boolean get() = recharge != null || usagesParJour != null
    val aDistance: Boolean get() = type == TypeAttaqueMonstre.DISTANCE || type == TypeAttaqueMonstre.POLYVALENTE

    /** Résumé d'une ligne : `Cimeterre +4 · 1d6 + 2 tranchants`, `Souffle · JS Dex DD 13 · 6d6`. */
    val resume: String
        get() = buildString {
            append(nom)
            bonusToucher?.let { append(" ").append(if (it >= 0) "+$it" else "$it") }
            if (sauvegarde != null && dd != null) append(" · JS $sauvegarde DD $dd")
            formuleDegats?.let { append(" · ").append(it) }
            typeDegats?.let { append(" ").append(it) }
            recharge?.let { append(if (it >= 6) " (recharge 6)" else " (recharge $it–6)") }
            usagesParJour?.let { append(" ($it/jour)") }
            if (conditions.isNotEmpty()) append(" · ").append(conditions.joinToString { it.libelle })
        }
}

/** Actions offensives d'un monstre et nombre d'attaques de son « Attaques multiples » (1 sinon). */
data class ActionsMonstre(val attaques: List<AttaqueMonstre>, val nbAttaquesMultiples: Int = 1)

object ActionsMonstreParser {

    private val sectionRegex = Regex("""(?ms)^## Actions[ \t]*$(.*?)(?=^## |\z)""")
    private val ligneRegex = Regex("""^(.+?)\.\s+(.*)$""")
    private val rechargeRegex = Regex("""\(\s*[Rr]echarge\s*(\d)""")
    private val usagesRegex = Regex("""\((\d+)\s*/\s*jour""")
    private val parenthesesRegex = Regex("""\s*\([^)]*\)""")
    private val bonusRegex = Regex("""(?:Corps à corps|distance)\s*:\s*([+\-−–])\s*(\d+)""")
    private val degatsRegex = Regex("""(\d+)\s*\((\d+d\d+(?:\s*[+\-−–]\s*\d+)?)\)\s*dégâts?\s+(?:d[’']\s*|de\s+)?(\p{L}+)""")
    private val porteeRegex = Regex("""portée\s*([\d,]+)\s*/\s*([\d,]+)\s*m""")
    private val allongeRegex = Regex("""allonge\s*(\d+(?:[,.]\d+)?)\s*m""")
    private val porteeSimpleRegex = Regex("""portée\s*(?:de\s*)?(\d+(?:[,.]\d+)?)\s*m\b""")
    private val sauvegardeRegex =Regex("""(?:JS|[Jj]et de sauvegarde de)\s+(\p{L}+)\s*:\s*DD\s*(\d+)""")
    private val nombreAttaquesRegex = Regex("""effectue\s+(deux|trois|quatre|cinq|six|\d+)\s+attaques""")
    private val nombres = mapOf("deux" to 2, "trois" to 3, "quatre" to 4, "cinq" to 5, "six" to 6)

    /** Lit la section « ## Actions » d'un [rawMarkdown] de bestiaire (cf. MonsterParser). */
    fun parser(rawMarkdown: String): ActionsMonstre {
        val section = sectionRegex.find(rawMarkdown)?.groupValues?.get(1) ?: return ActionsMonstre(emptyList())
        var multiples = 1
        val attaques = mutableListOf<AttaqueMonstre>()
        section.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { ligne ->
            val match = ligneRegex.find(ligne) ?: return@forEach
            val nomBrut = match.groupValues[1].trim()
            val corps = match.groupValues[2]
            if (nomBrut.startsWith("Attaques multiples", ignoreCase = true)) {
                multiples = nombreAttaques(corps)
                return@forEach
            }
            attaques += lireAttaque(nomBrut, corps)
        }
        return ActionsMonstre(attaques, multiples)
    }

    private fun nombreAttaques(texte: String): Int {
        nombreAttaquesRegex.find(texte)?.let { m ->
            val v = m.groupValues[1]
            return (v.toIntOrNull() ?: nombres[v] ?: 1).coerceIn(1, 8)
        }
        // « une attaque de Fouet embrasé et une de Lame de foudre » → 2.
        val unes = Regex("""\bune (?:attaque )?de\b""").findAll(texte).count()
        return unes.coerceIn(1, 8)
    }

    private fun lireAttaque(nomBrut: String, corps: String): AttaqueMonstre {
        val nom = nomBrut.replace(parenthesesRegex, "").trim()
        val sauvegarde = sauvegardeRegex.find(corps)
        val type = when {
            corps.contains("Corps à corps ou à distance :") -> TypeAttaqueMonstre.POLYVALENTE
            corps.contains("Corps à corps :") -> TypeAttaqueMonstre.CORPS_A_CORPS
            corps.contains("À distance :") -> TypeAttaqueMonstre.DISTANCE
            sauvegarde != null -> TypeAttaqueMonstre.SAUVEGARDE
            else -> TypeAttaqueMonstre.AUTRE
        }
        val bonus = bonusRegex.find(corps)?.let { m ->
            val v = m.groupValues[2].toInt()
            if (m.groupValues[1] == "+") v else -v
        }
        val degats = degatsRegex.find(corps)
        return AttaqueMonstre(
            nom = nom,
            type = type,
            bonusToucher = bonus,
            formuleDegats = degats?.groupValues?.get(2)?.replace('−', '-')?.replace('–', '-'),
            degatsMoyens = degats?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            typeDegats = degats?.groupValues?.get(3),
            sauvegarde = sauvegarde?.groupValues?.get(1),
            dd = sauvegarde?.groupValues?.get(2)?.toIntOrNull(),
            recharge = rechargeRegex.find(nomBrut)?.groupValues?.get(1)?.toIntOrNull(),
            usagesParJour = usagesRegex.find(nomBrut)?.groupValues?.get(1)?.toIntOrNull(),
            zone = corps.contains("chaque créature"),
            porteeLongue = porteeRegex.find(corps)?.groupValues?.get(2)?.replace(',', '.')?.toDoubleOrNull(),
            conditions = Etats.infligesPar(corps),
            allonge = allongeRegex.find(corps)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: Distance.CONTACT.metres,
            zoneEffet = ZoneEffet.depuisTexte(corps),
            porteeSauvegarde = porteeSimpleRegex.find(corps)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull(),
        )
    }
}

/** Qui un combattant piloté par l'IA prend pour cible. */
enum class StrategieCible(val label: String) {
    HASARD("au hasard"),
    PLUS_PROCHE("le plus proche"),
    PLUS_BLESSE("le plus blessé"),
    CA_BASSE("la CA la plus basse"),
    PLUS_FAIBLE("le plus fragile"),
    PLUS_COSTAUD("le plus robuste"),
    MENACE_ALLIE("celui qui menace un allié blessé"),
}

/** Manière de se battre et de se déplacer d'un profil. */
enum class StyleCombat(val label: String) {
    MELEE("Corps à corps : fonce au contact et y reste"),
    DISTANCE("À distance : recule s'il est au contact, puis tire"),
    ARTILLERIE("Artillerie : capacités de zone, se désengage du contact"),
    STATIQUE("Statique : ne quitte pas son poste"),
    HARCELEMENT("Harcèlement : frappe puis recule"),
    POLYVALENT("Polyvalent : meilleure attaque à portée"),
}

/**
 * Profil de comportement d'un combattant piloté par l'IA (monstre ou PNJ). Ses règles de
 * priorité sont évaluées dans l'ordre par [IaMonstre.decider]. [seuilFuite] : fraction de PV sous
 * laquelle il se replie (null = jamais). Jamais transmis aux joueurs.
 */
enum class ProfilIA(
    val label: String,
    val description: String,
    val seuilFuite: Float?,
    val cible: StrategieCible,
    val style: StyleCombat,
    val speciauxEnPremier: Boolean,
) {
    BRUTE("Brute", "Ne lâche pas l'adversaire qu'il a au contact, sinon fonce au corps à corps sur une cible au hasard. Ne fuit jamais.", null, StrategieCible.HASARD, StyleCombat.MELEE, false),
    BERSERKER("Berserker", "Enragé : se jette sur l'adversaire le plus proche, uniquement au corps à corps, même à l'agonie.", null, StrategieCible.PLUS_PROCHE, StyleCombat.MELEE, false),
    PREDATEUR("Prédateur", "Achève les blessés : va chercher le personnage le plus mal en point. Se replie sous 20 % de ses PV.", 0.2f, StrategieCible.PLUS_BLESSE, StyleCombat.POLYVALENT, false),
    TACTICIEN("Tacticien", "Capacités spéciales dès qu'elles touchent quelqu'un, vise la CA la plus basse. Se replie sous 25 %.", 0.25f, StrategieCible.CA_BASSE, StyleCombat.POLYVALENT, true),
    TIREUR("Tireur", "Tire à distance ; au contact, recule d'abord (au risque d'une attaque d'opportunité). Se replie sous 25 %.", 0.25f, StrategieCible.CA_BASSE, StyleCombat.DISTANCE, false),
    ARTILLEUR("Artilleur", "Lanceur de sorts, souffle : capacités de zone en priorité, se désengage du contact pour garder ses distances. Se replie sous 30 %.", 0.3f, StrategieCible.CA_BASSE, StyleCombat.ARTILLERIE, true),
    GARDIEN("Gardien", "Défend un poste : ne se déplace pas, frappe qui l'approche, sinon tire ou se tient prêt. Ne fuit jamais.", null, StrategieCible.PLUS_PROCHE, StyleCombat.STATIQUE, false),
    PROTECTEUR("Protecteur", "Garde du corps : s'en prend à l'adversaire qui menace l'allié le plus blessé. Se replie sous 20 %.", 0.2f, StrategieCible.MENACE_ALLIE, StyleCombat.MELEE, false),
    ESCARMOUCHEUR("Escarmoucheur", "Harcèle la cible la plus fragile : frappe puis recule à courte distance. Se replie sous 30 %.", 0.3f, StrategieCible.PLUS_FAIBLE, StyleCombat.HARCELEMENT, false),
    DUELLISTE("Duelliste", "Défie l'adversaire le plus robuste et reste au contact avec lui. Se replie sous 20 %.", 0.2f, StrategieCible.PLUS_COSTAUD, StyleCombat.MELEE, false),
    LACHE("Lâche", "Harcèle les plus fragiles et s'enfuit (désengagement) dès qu'il a perdu la moitié de ses PV.", 0.5f, StrategieCible.PLUS_FAIBLE, StyleCombat.POLYVALENT, false),
    IMPREVISIBLE("Imprévisible", "Chaotique : adopte chaque round un comportement tiré au hasard parmi les autres profils.", null, StrategieCible.HASARD, StyleCombat.POLYVALENT, false);

    companion object {
        /** Profil par défaut d'après les actions seules (cf. [analyser] pour la fiche complète). */
        fun deduire(actions: ActionsMonstre): ProfilIA = analyser(actions, "").profil

        private val intRegex = Regex("""Int\s+(\d+)""")
        private val typeRegex = Regex("""^\*([^,*]+)""", RegexOption.MULTILINE)
        private val tailleRegex = Regex("""taille\s+([A-Z]+)""")
        private val vitesseRegex = Regex("""\*\*Vitesse :\*\*\s*(.+)""")

        /**
         * Profil par défaut d'un monstre d'après toute sa fiche (cf. MonsterParser) : capacités de
         * zone, sorts, traits de comportement (Tactique de meute, Fuite agile, Téméraire…), type,
         * intelligence, vol et taille. Renvoie aussi la raison, affichée au MJ.
         */
        fun analyser(actions: ActionsMonstre, fiche: String): ProfilDeduit {
            val offensives = actions.attaques.filter { it.degatsMoyens > 0 }
            val meilleureCac = offensives.filter { it.corpsACorps }.maxOfOrNull { it.degatsMoyens } ?: 0
            val meilleureDistance = offensives.filter { it.aDistance }.maxOfOrNull { it.degatsMoyens } ?: 0
            val texte = fiche.lowercase()
            val intelligence = intRegex.find(fiche)?.groupValues?.get(1)?.toIntOrNull()
            val type = typeRegex.find(fiche)?.groupValues?.get(1)?.trim()?.lowercase().orEmpty()
            val taille = tailleRegex.find(fiche)?.groupValues?.get(1).orEmpty()
            val vole = vitesseRegex.find(fiche)?.groupValues?.get(1)?.contains("vol") == true
            fun trait(vararg mots: String) = mots.any { texte.contains(it) }
            return when {
                offensives.any { it.estSpeciale && it.zone } ->
                    ProfilDeduit(ARTILLEUR, "Capacité de zone (souffle, onde…) : la déclenche dès que possible et garde ses distances")
                trait("\nincantation.") && (intelligence ?: 10) >= 8 ->
                    ProfilDeduit(ARTILLEUR, "Lanceur de sorts : reste à distance et utilise sa magie")
                trait("tactique de meute") ->
                    ProfilDeduit(PREDATEUR, "Chasse en meute (Tactique de meute) : s'acharne sur les proies affaiblies")
                trait("fuite agile", "repli aérien", "embuscade", "attaque sournoise", "insaisissable") ->
                    ProfilDeduit(ESCARMOUCHEUR, "Frappe puis se replie (Fuite agile, repli, embuscade…)")
                trait("téméraire", "rage", "implacable", "sanguinaire", "frénésie") ->
                    ProfilDeduit(BERSERKER, "Tempérament enragé (Téméraire, Rage, Implacable…) : ne recule jamais")
                type.startsWith("créature artificielle") && (intelligence ?: 3) <= 6 ->
                    ProfilDeduit(GARDIEN, "Créature artificielle sans volonté propre : défend l'endroit qu'elle garde")
                type.startsWith("mort-vivant") && (intelligence ?: 3) <= 6 ->
                    ProfilDeduit(BRUTE, "Mort-vivant sans esprit : avance sans peur sur la cible la plus proche")
                meilleureDistance > meilleureCac ->
                    ProfilDeduit(TIREUR, "Sa meilleure attaque est à distance")
                offensives.any { it.estSpeciale } ->
                    ProfilDeduit(TACTICIEN, "Capacité spéciale à usage limité : la place au bon moment")
                intelligence != null && intelligence <= 3 && vole && taille in setOf("TP", "P") ->
                    ProfilDeduit(PREDATEUR, "Petite créature volante à l'instinct animal (Int $intelligence) : fond sur les proies affaiblies et fuit si elle est blessée")
                intelligence != null && intelligence <= 3 ->
                    ProfilDeduit(PREDATEUR, "Instinct animal (Int $intelligence) : attaque les proies affaiblies et fuit si elle est gravement blessée")
                intelligence != null && intelligence >= 12 ->
                    ProfilDeduit(TACTICIEN, "Créature intelligente (Int $intelligence) : vise les points faibles")
                else -> ProfilDeduit(BRUTE, "Combattant au corps à corps sans tactique particulière")
            }
        }

        /** Profil par défaut d'un personnage (PNJ) d'après sa classe et ses armes. */
        fun analyserPersonnage(classe: String, attaques: List<AttaqueMonstre>): ProfilDeduit {
            val c = classe.lowercase()
            return when {
                c in setOf("magicien", "ensorceleur", "occultiste", "sorcier", "druide") ->
                    ProfilDeduit(ARTILLEUR, "Lanceur de sorts ($classe) : reste à distance et utilise sa magie")
                c == "clerc" || c == "paladin" -> ProfilDeduit(PROTECTEUR, "$classe : protège ses alliés blessés")
                c == "roublard" || c == "voleur" || c == "moine" -> ProfilDeduit(ESCARMOUCHEUR, "$classe : frappe puis se replie")
                c == "barbare" -> ProfilDeduit(BERSERKER, "Barbare : fonce au contact et ne recule jamais")
                c == "guerrier" -> ProfilDeduit(DUELLISTE, "Guerrier : défie l'adversaire le plus robuste")
                c == "rôdeur" || attaques.any { it.type == TypeAttaqueMonstre.DISTANCE } && attaques.none { it.corpsACorps } ->
                    ProfilDeduit(TIREUR, "Combat surtout à distance")
                else -> ProfilDeduit(BRUTE, "Combattant sans tactique particulière")
            }
        }
    }
}

/** Profil proposé pour un combattant, avec la raison de ce choix (affichée au MJ uniquement). */
data class ProfilDeduit(val profil: ProfilIA, val raison: String)

/** Situation qui fait basculer un combattant complexe vers un autre profil. */
enum class DeclencheurIA(val label: String, val valeurParDefaut: Int?, val unite: String = "") {
    PV_SOUS("Ses PV passent sous", 50, "%"),
    ENCERCLE("Au contact d'au moins", 2, "adversaire(s)"),
    AUCUN_CONTACT("Aucun adversaire au contact", null),
    ADVERSAIRE_AFFAIBLI("Un adversaire est sous", 25, "% de ses PV"),
    ALLIES_TOMBES("Ses alliés tombés atteignent", 50, "%"),
    SEUL("Dernier debout de son camp", null),
    DES_LE_ROUND("À partir du round", 3),
}

/**
 * « Si [declencheur] ([valeur]) alors adopter [profil] » : un combattant peut en avoir plusieurs,
 * évaluées dans l'ordre à chaque décision ; la première qui s'applique l'emporte, sinon son profil
 * de base s'applique.
 */
data class RegleProfilIA(
    val declencheur: DeclencheurIA,
    val profil: ProfilIA,
    val valeur: Int = declencheur.valeurParDefaut ?: 0,
) {
    val libelle: String
        get() = declencheur.label + (declencheur.valeurParDefaut?.let { " $valeur ${declencheur.unite}".trimEnd() } ?: "")

    fun sApplique(s: SituationIA): Boolean = when (declencheur) {
        DeclencheurIA.PV_SOUS -> s.ratioPv * 100 < valeur
        DeclencheurIA.ENCERCLE -> s.nbContacts >= valeur
        DeclencheurIA.AUCUN_CONTACT -> s.nbContacts == 0
        DeclencheurIA.ADVERSAIRE_AFFAIBLI -> s.ratioAdversaireMin * 100 < valeur
        DeclencheurIA.ALLIES_TOMBES -> s.alliesTotal > 0 && s.alliesTombes * 100 >= valeur * s.alliesTotal
        DeclencheurIA.SEUL -> s.alliesTotal > 0 && s.alliesTombes == s.alliesTotal
        DeclencheurIA.DES_LE_ROUND -> s.round >= valeur
    }
}

/** Ce que l'IA sait de la situation d'un combattant au moment de décider. */
data class SituationIA(
    val ratioPv: Float,
    val nbContacts: Int,
    val alliesTotal: Int,
    val alliesTombes: Int,
    val round: Int,
    val ratioAdversaireMin: Float,
)

/**
 * Action décidée pour un combattant pendant la phase de déclaration (modifiable par le MJ).
 * [deplacements] : distance de ce combattant à chaque adversaire concerné APRÈS son déplacement
 * (appliquée quand le MJ passe au combattant suivant). [desengage] : il quitte le contact sans
 * provoquer d'attaque d'opportunité. [profil] : profil réellement appliqué ce round.
 */
data class DecisionMonstre(
    val libelle: String,
    val attaque: AttaqueMonstre? = null,
    val nbAttaques: Int = 1,
    val cibleId: String? = null,
    val cibleNom: String? = null,
    val raison: String,
    val deplacements: Map<String, Distance> = emptyMap(),
    val desengage: Boolean = false,
    val profil: ProfilIA? = null,
)

object IaMonstre {

    /** Une capacité à usage limité est-elle encore utilisable ce round ? */
    fun disponible(monstre: Combattant, attaque: AttaqueMonstre): Boolean {
        val utilisations = monstre.capacitesUtilisees[attaque.nom] ?: 0
        return when {
            attaque.recharge != null -> utilisations == 0
            attaque.usagesParJour != null -> utilisations < attaque.usagesParJour
            else -> true
        }
    }

    /**
     * Décide l'action de [monstre] (monstre ou PNJ) contre ses [adversaires]. [distances] donne sa
     * distance à chaque adversaire (courte par défaut), [distanceEntre] celle entre deux
     * combattants quelconques (pour protéger un allié). Le profil appliqué est celui de la première
     * règle situationnelle vérifiée, sinon son profil de base. Pure : le tirage passe par [aleatoire].
     */
    fun decider(
        monstre: Combattant,
        adversaires: List<Combattant>,
        distances: Map<String, Distance> = emptyMap(),
        aleatoire: Random = Random,
        allies: List<Combattant> = emptyList(),
        distanceEntre: (String, String) -> Distance = { a, b ->
            when (monstre.id) {
                a -> distances[b] ?: Distance.COURTE
                b -> distances[a] ?: Distance.COURTE
                else -> Distance.COURTE
            }
        },
        round: Int = 1,
    ): DecisionMonstre {
        val cibles = adversaires.filter { !it.horsCombat }
        fun dist(c: Combattant) = distances[c.id] ?: Distance.COURTE
        val situation = SituationIA(
            ratioPv = if (monstre.pvMax > 0) monstre.pv.toFloat() / monstre.pvMax else 1f,
            nbContacts = cibles.count { dist(it) == Distance.CONTACT },
            alliesTotal = allies.size,
            alliesTombes = allies.count { it.horsCombat },
            round = round,
            ratioAdversaireMin = cibles.minOfOrNull { if (it.pvMax > 0) it.pv.toFloat() / it.pvMax else 1f } ?: 1f,
        )
        val regle = monstre.reglesIA.firstOrNull { it.sApplique(situation) }
        var profil = regle?.profil ?: monstre.profilIA ?: ProfilIA.BRUTE
        val notes = mutableListOf<String>()
        regle?.let { notes += "Situation « ${it.libelle} » → ${it.profil.label}" }
        if (profil == ProfilIA.IMPREVISIBLE) {
            profil = ProfilIA.entries.filter { it != ProfilIA.IMPREVISIBLE }.let { it[aleatoire.nextInt(it.size)] }
            notes += "Imprévisible : agit en ${profil.label} ce round"
        }
        // Vitesse 0 (Agrippé, Entravé…) : seules les cibles déjà à portée comptent, et il ne bouge pas.
        val immobilise = Etats.immobilisant(monstre.conditions)?.takeIf { Etats.peutAgir(monstre.conditions) }
        val decision = if (immobilise == null) {
            selonProfil(monstre, profil, cibles, allies, ::dist, distanceEntre, aleatoire)
        } else {
            val aPortee = cibles.filter { c ->
                monstre.attaques.any { a -> a.type != TypeAttaqueMonstre.AUTRE && disponible(monstre, a) && a.atteint(dist(c)) }
            }
            if (aPortee.isEmpty()) {
                DecisionMonstre("Se tenir prêt", raison = "${immobilise.label} : vitesse 0, aucun adversaire à portée")
            } else {
                val d = selonProfil(monstre, profil, aPortee, allies, ::dist, distanceEntre, aleatoire)
                d.copy(deplacements = emptyMap(), desengage = false, raison = "${immobilise.label} : ne bouge pas — ${d.raison}")
            }
        }
        val raison = (notes + decision.raison).joinToString(" — ")
        return decision.copy(raison = raison, profil = profil)
    }

    private fun selonProfil(
        monstre: Combattant,
        profil: ProfilIA,
        cibles: List<Combattant>,
        allies: List<Combattant>,
        dist: (Combattant) -> Distance,
        distanceEntre: (String, String) -> Distance,
        aleatoire: Random,
    ): DecisionMonstre {
        Etats.bloquant(monstre.conditions)?.let {
            return DecisionMonstre("Aucune action", raison = "${it.label} : ne peut pas agir")
        }
        if (cibles.isEmpty()) return DecisionMonstre("Aucune action", raison = "Plus aucun adversaire debout")
        val auContact = cibles.filter { dist(it) == Distance.CONTACT }
        val style = profil.style

        profil.seuilFuite?.let { seuil ->
            if (monstre.pvMax > 0 && monstre.pv.toFloat() / monstre.pvMax <= seuil) {
                return fuir(monstre, cibles, dist, "${profil.label} à ${monstre.pv}/${monstre.pvMax} PV (≤ ${(seuil * 100).toInt()} %)")
            }
        }

        val offensives = monstre.attaques.filter {
            it.type != TypeAttaqueMonstre.AUTRE && (it.degatsMoyens > 0 || it.bonusToucher != null) && disponible(monstre, it)
        }
        val normales = offensives.filter { !it.estSpeciale }
        val tirs = normales.filter { it.aDistance }
        val melees = normales.filter { it.corpsACorps }
        fun choisir(candidats: List<Combattant>) = choisirCible(profil.cible, candidats, dist, allies, distanceEntre, aleatoire)

        if (ConditionCombat.EFFRAYE in monstre.conditions) {
            // Effrayé : ne s'approche pas ; tire s'il le peut, sinon s'écarte.
            if (auContact.isNotEmpty()) {
                return DecisionMonstre(
                    "Se désengager et reculer",
                    raison = "Effrayé au contact : s'écarte",
                    deplacements = auContact.associate { it.id to Distance.CONTACT.eloigne(monstre.vitesse) },
                    desengage = true,
                )
            }
            val aPortee = cibles.filter { c -> tirs.any { it.atteint(dist(c)) } }
            if (aPortee.isNotEmpty()) {
                val cible = choisir(aPortee)
                val tir = tirs.filter { it.atteint(dist(cible)) }.maxBy { it.degatsMoyens }
                return attaquer(monstre, tir, cible, "Effrayé : garde ses distances et tire")
            }
            return DecisionMonstre("Esquiver en gardant ses distances", raison = "Effrayé sans attaque à distance")
        }

        // Capacité spéciale à portée, si le profil la privilégie ou si elle rapporte plus.
        // Adversaires à portée de chaque capacité (longueur du cône, de la ligne, portée…).
        fun aPortee(s: AttaqueMonstre) = cibles.count { s.atteint(dist(it)) }
        val speciales = offensives.filter { it.estSpeciale && aPortee(it) > 0 }
        val speciale = speciales.maxByOrNull { valeur(it, aPortee(it)) }
        val enZone = speciale?.let(::aPortee) ?: 0
        val meilleure = normales.maxByOrNull { it.degatsMoyens }
        val valeurNormale = (meilleure?.degatsMoyens ?: 0) * if (meilleure?.bonusToucher != null) monstre.nbAttaquesMultiples else 1
        if (speciale != null && (profil.speciauxEnPremier || valeur(speciale, enZone) > valeurNormale)) {
            val raison = if (profil.speciauxEnPremier) "${profil.label} : capacité spéciale disponible" else "Capacité plus rentable que ses attaques"
            val cible = if (speciale.zone) null else choisir(cibles.filter { speciale.atteint(dist(it)) })
            return attaquer(monstre, speciale, cible, raison + if (speciale.zone) " ($enZone cible(s) à portée)" else "", multiples = false)
        }

        // Styles qui s'écartent du contact avant tout.
        if (auContact.isNotEmpty() && style == StyleCombat.DISTANCE && tirs.isNotEmpty()) {
            val cible = choisir(cibles)
            val base = attaquer(monstre, tirs.maxBy { it.degatsMoyens }, cible, "")
            return base.copy(
                libelle = "Recule puis tire : ${base.libelle}",
                raison = "${profil.label} au contact de ${auContact.joinToString { it.nom }} : recule à courte distance " +
                    "(attaque d'opportunité possible) puis tire sur ${cible.nom}",
                deplacements = auContact.associate { it.id to Distance.CONTACT.eloigne(monstre.vitesse) },
            )
        }
        // Sans attaque à distance (dragon dont le souffle se recharge), il reste se battre au contact.
        if (auContact.isNotEmpty() && style == StyleCombat.ARTILLERIE && tirs.isNotEmpty()) {
            return DecisionMonstre(
                "Se désengager et prendre de la distance",
                raison = "${profil.label} au contact de ${auContact.joinToString { it.nom }} : se dégage pour garder ses distances",
                deplacements = auContact.associate { it.id to Distance.CONTACT.eloigne(monstre.vitesse) },
                desengage = true,
            )
        }

        // Armes utilisées : le style corps à corps ignore ses attaques à distance s'il en a de mêlée.
        val armes = if (style == StyleCombat.MELEE && melees.isNotEmpty()) melees else normales
        val seDeplace = style != StyleCombat.STATIQUE
        fun directe(c: Combattant) = armes.any { it.atteint(dist(c)) }
        fun approchable(c: Combattant) = seDeplace && armes.any { it.corpsACorps } &&
            dist(c) != Distance.CONTACT && dist(c).metresJusquAuContact <= monstre.vitesse + 0.01
        val garderContact = style == StyleCombat.STATIQUE ||
            (style == StyleCombat.MELEE && profil.cible in setOf(StrategieCible.HASARD, StrategieCible.PLUS_PROCHE))
        val pool = when {
            garderContact && auContact.isNotEmpty() -> auContact
            else -> cibles.filter { directe(it) || approchable(it) }
        }

        if (pool.isNotEmpty() && armes.isNotEmpty()) {
            val cible = choisir(pool)
            val d = dist(cible)
            val directes = armes.filter { it.atteint(d) }
            val candidates = when (style) {
                StyleCombat.DISTANCE, StyleCombat.ARTILLERIE ->
                    directes.filter { it.aDistance && d != Distance.CONTACT }.ifEmpty { directes }.ifEmpty { armes.filter { it.corpsACorps } }
                StyleCombat.MELEE -> armes.filter { it.corpsACorps }.ifEmpty { directes }
                StyleCombat.STATIQUE -> directes
                StyleCombat.POLYVALENT, StyleCombat.HARCELEMENT -> when {
                    d == Distance.CONTACT && directes.any { it.corpsACorps } -> directes.filter { it.corpsACorps }
                    directes.isNotEmpty() -> directes
                    else -> armes.filter { it.corpsACorps }
                }
            }
            val choix = candidates.maxByOrNull { it.degatsMoyens }
            if (choix != null) {
                val approche = !choix.atteint(d)
                val base = attaquer(monstre, choix, cible, "${profil.label}, cible ${profil.cible.label}")
                return when {
                    style == StyleCombat.HARCELEMENT && choix.corpsACorps -> base.copy(
                        libelle = "Frappe puis recule : ${base.libelle}",
                        raison = base.raison + " — frappe ${cible.nom} puis repart à courte distance (attaque d'opportunité possible)",
                        deplacements = mapOf(cible.id to Distance.COURTE),
                    )
                    approche -> base.copy(
                        libelle = "Se rapproche de ${cible.nom} et attaque : ${base.libelle}",
                        raison = base.raison + " — à ${d.texteMetres}, fonce au contact",
                        deplacements = mapOf(cible.id to Distance.CONTACT),
                    )
                    else -> base.copy(raison = base.raison + " — ${d.label.lowercase()}")
                }
            }
        }

        if (!seDeplace) {
            return DecisionMonstre(
                "Se tient prêt à son poste (Préparer une action)",
                raison = "${profil.label} : aucun adversaire à sa portée, ne quitte pas sa position",
            )
        }
        // Personne à portée : s'élance vers une cible (longue → courte).
        val cible = choisir(cibles)
        return DecisionMonstre(
            "Se précipiter vers ${cible.nom}",
            cibleId = cible.id,
            cibleNom = cible.nom,
            raison = if (normales.isEmpty()) "Aucune attaque lisible sur la fiche" else "Aucun adversaire à portée : se rapproche",
            deplacements = mapOf(cible.id to dist(cible).rapproche(monstre.vitesse * 2)),
        )
    }

    /** Fuite : se désengage du contact, sinon s'élance plus loin, sinon quitte le combat. */
    private fun fuir(monstre: Combattant, cibles: List<Combattant>, dist: (Combattant) -> Distance, pourquoi: String): DecisionMonstre {
        return when {
            // Désengagement (action) : il ne lui reste que son déplacement normal.
            cibles.any { dist(it) == Distance.CONTACT } -> DecisionMonstre(
                "Fuir : se désengager et s'éloigner", raison = "$pourquoi : se replie",
                deplacements = cibles.associate { it.id to dist(it).eloigne(monstre.vitesse) }, desengage = true
            )
            // Pas encore hors de portée (≤ 30 m) : se précipite, double déplacement.
            cibles.any { dist(it).metres <= Distance.LONGUE.metres } -> DecisionMonstre(
                "Fuir : se précipiter loin du combat", raison = "$pourquoi : s'enfuit",
                deplacements = cibles.associate { it.id to dist(it).eloigne(monstre.vitesse * 2) }
            )
            else -> DecisionMonstre("S'enfuit hors de vue", raison = "$pourquoi : déjà loin, quitte le combat")
        }
    }

    private fun valeur(attaque: AttaqueMonstre, nbCibles: Int): Int =
        attaque.degatsMoyens * if (attaque.zone) minOf(nbCibles, 3) else 1

    private fun attaquer(monstre: Combattant, attaque: AttaqueMonstre, cible: Combattant?, raison: String, multiples: Boolean = true): DecisionMonstre {
        val nb = if (multiples && attaque.bonusToucher != null) monstre.nbAttaquesMultiples else 1
        val libelle = attaque.nom + (if (nb > 1) " ×$nb" else "") + (if (attaque.zone) " (zone)" else "")
        return DecisionMonstre(libelle, attaque, nb, cible?.id, cible?.nom, raison)
    }

    private fun choisirCible(
        strategie: StrategieCible,
        cibles: List<Combattant>,
        dist: (Combattant) -> Distance,
        allies: List<Combattant>,
        distanceEntre: (String, String) -> Distance,
        aleatoire: Random,
    ): Combattant {
        fun auHasard(liste: List<Combattant>) = liste[aleatoire.nextInt(liste.size)]
        fun plusProches(liste: List<Combattant>): Combattant {
            val min = liste.minOf { dist(it).ordinal }
            return auHasard(liste.filter { dist(it).ordinal == min })
        }
        return when (strategie) {
            StrategieCible.HASARD -> auHasard(cibles)
            StrategieCible.PLUS_PROCHE -> plusProches(cibles)
            StrategieCible.PLUS_BLESSE -> cibles.minBy { if (it.pvMax > 0) it.pv.toFloat() / it.pvMax else 1f }
            StrategieCible.CA_BASSE -> cibles.minWith(compareBy<Combattant> { it.ca }.thenBy { it.pv })
            StrategieCible.PLUS_FAIBLE -> cibles.minWith(compareBy<Combattant> { it.pvMax }.thenBy { it.pv })
            StrategieCible.PLUS_COSTAUD -> cibles.maxWith(compareBy<Combattant> { it.pvMax }.thenBy { it.pv })
            StrategieCible.MENACE_ALLIE -> {
                // Allié debout le plus blessé, puis l'adversaire qui est à son contact.
                val protege = allies.filter { !it.horsCombat && it.pv < it.pvMax }.minByOrNull { it.pv.toFloat() / it.pvMax.coerceAtLeast(1) }
                val menaces = protege?.let { p -> cibles.filter { distanceEntre(p.id, it.id) == Distance.CONTACT } }.orEmpty()
                if (menaces.isNotEmpty()) plusProches(menaces) else plusProches(cibles)
            }
        }
    }
}

/** Résultat d'une attaque de monstre lancée par l'application pour le MJ. */
data class ResultatAttaque(
    val d20: Int?,
    val totalToucher: Int?,
    val touche: Boolean,
    val critique: Boolean,
    // Dégâts appliqués (0 si raté) ; [degatsLances] = dés de dégâts lancés quoi qu'il arrive, pour l'affichage.
    val degats: Int,
    val degatsLances: Int = degats,
    // Avantage/désavantage dû aux états (cf. Etats.modeAttaque) : [d20Ecarte] est le dé non retenu.
    val mode: ModeJet = ModeJet.NORMAL,
    val d20Ecarte: Int? = null,
)

/**
 * Lance [nb] attaques de [attaque] contre une CA (null = inconnue : touché si ≥ 10 n'est pas
 * présumé, le MJ tranche ; on considère alors « touché » pour préparer les dégâts). Une action à
 * jet de sauvegarde ne lance que les dégâts (plein tarif, le MJ divise en cas de réussite).
 * [mode] : avantage ou désavantage dû aux états ; [critiqueSiTouche] : toute attaque qui touche
 * est critique (cible Paralysée ou Inconsciente frappée au contact).
 */
fun lancerAttaqueMonstre(
    attaque: AttaqueMonstre,
    nb: Int,
    caCible: Int?,
    aleatoire: Random = Random,
    mode: ModeJet = ModeJet.NORMAL,
    critiqueSiTouche: Boolean = false,
): List<ResultatAttaque> =
    (1..nb.coerceAtLeast(1)).map {
        if (attaque.bonusToucher == null) {
            ResultatAttaque(null, null, true, false, attaque.formuleDegats?.let { f -> lancerFormuleDes(f, aleatoire) } ?: attaque.degatsMoyens)
        } else {
            val premier = aleatoire.nextInt(1, 21)
            val second = if (mode == ModeJet.NORMAL) null else aleatoire.nextInt(1, 21)
            val d20 = when {
                second == null -> premier
                mode == ModeJet.AVANTAGE -> maxOf(premier, second)
                else -> minOf(premier, second)
            }
            val ecarte = second?.let { if (d20 == premier) second else premier }
            val total = d20 + attaque.bonusToucher
            val touche = d20 == 20 || (d20 != 1 && (caCible == null || total >= caCible))
            val critique = d20 == 20 || (touche && critiqueSiTouche)
            // Dégâts toujours lancés (affichés au MJ), appliqués seulement si l'attaque touche.
            val lances = attaque.formuleDegats?.let { f -> lancerFormuleDes(f, aleatoire, critique) } ?: attaque.degatsMoyens
            ResultatAttaque(d20, total, touche, critique, if (touche) lances else 0, lances, mode, ecarte)
        }
    }
