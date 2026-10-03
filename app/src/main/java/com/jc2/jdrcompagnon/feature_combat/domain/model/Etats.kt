package com.jc2.jdrcompagnon.feature_combat.domain.model

import java.text.Normalizer

/**
 * Effets mécaniques élémentaires des états (règles 2024). Un état est un ensemble de ces effets ;
 * l'application en automatise une partie (voir [FicheEtat.automatise]), le reste est rappelé au MJ.
 */
enum class EffetEtat(val libelle: String) {
    AUCUNE_ACTION("Ne peut entreprendre ni action, ni action bonus, ni réaction"),
    PERD_CONCENTRATION("Perd sa concentration"),
    IMMOBILE("Vitesse 0 : ne peut pas se déplacer"),
    MUET("Ne peut pas parler (aucun sort à composante verbale)"),
    RAMPE("Ne peut que ramper ; se relever coûte la moitié de sa vitesse"),
    NE_VOIT_PAS("Ne voit pas : échoue aux tests qui demandent la vue"),
    N_ENTEND_PAS("N'entend pas : échoue aux tests qui demandent l'ouïe"),
    INCONSCIENT_DU_MONDE("N'a pas conscience de son environnement"),
    LACHE_TOUT("Lâche ce qu'il tient"),
    DESAVANTAGE_ATTAQUES("Désavantage à ses jets d'attaque"),
    AVANTAGE_ATTAQUES("Avantage à ses jets d'attaque"),
    AVANTAGE_CONTRE("Les attaques contre lui ont l'avantage"),
    DESAVANTAGE_CONTRE("Les attaques contre lui ont le désavantage"),
    A_TERRE_CONTRE("Attaques contre lui : avantage à 1,50 m ou moins, désavantage au-delà"),
    CRITIQUE_AU_CONTACT("Toute attaque qui le touche à 1,50 m ou moins est un coup critique"),
    ECHEC_JS_FOR_DEX("Rate automatiquement ses JS de Force et de Dextérité"),
    DESAVANTAGE_JS_DEX("Désavantage à ses JS de Dextérité"),
    DESAVANTAGE_TESTS("Désavantage à ses tests de caractéristique"),
    RESISTANCE_DEGATS("Résistance à tous les dégâts"),
    NE_PEUT_APPROCHER("Ne peut pas s'approcher volontairement de la source de sa peur"),
    NE_PEUT_ATTAQUER_CHARMEUR("Ne peut pas attaquer le charmeur ni le cibler avec un effet nuisible"),
}

/** Jet de d20 avec avantage (meilleur de deux), désavantage (pire de deux) ou normal. */
enum class ModeJet(val label: String) { NORMAL("normal"), AVANTAGE("avantage"), DESAVANTAGE("désavantage") }

/**
 * Fiche d'un état : règles (texte du livre États), effets mécaniques, états qu'il inclut
 * (Paralysé inclut Neutralisé) et ce que l'application en fait automatiquement.
 */
data class FicheEtat(
    val condition: ConditionCombat,
    val resume: String,
    val regles: List<String>,
    val effets: Set<EffetEtat>,
    val inclut: Set<ConditionCombat> = emptySet(),
    val automatise: List<String> = emptyList(),
    // Non officiel : Concentration n'est pas un état des règles, mais se suit pareil en combat.
    val officiel: Boolean = true,
    // Autres orthographes acceptées dans le champ condition d'une fiche.
    val alias: List<String> = emptyList(),
)

/**
 * Catalogue des états de D&D 2024 (SRD 5.2.1) : source unique pour le livre « États » de la
 * bibliothèque, les effets appliqués en combat (CombatSession, IaMonstre), le menu latéral MJ et
 * le champ condition des fiches de personnage.
 */
object Etats {

    private const val NE_PEUT_AGIR = "Combat : ne peut plus déclarer d'action (« Aucune action » imposée, joueur bloqué) ; sa concentration est rompue."
    private const val NE_BOUGE_PAS = "Combat : aucun déplacement possible (le déplacement déclaré est annulé, l'IA ne le fait pas bouger)."
    private const val AVANTAGE_CONTRE = "Combat : les attaques des monstres contre lui sont lancées avec l'avantage."
    private const val ECHEC_JS = "Combat : JS de Force et de Dextérité signalés comme échecs automatiques au MJ."

    val fiches: List<FicheEtat> = listOf(
        FicheEtat(
            ConditionCombat.AVEUGLE,
            "Ne voit plus rien.",
            listOf(
                "Ne voit pas : la créature échoue automatiquement à tout test de caractéristique qui nécessite la vue.",
                "Les jets d'attaque contre elle ont l'avantage, et ses propres jets d'attaque subissent le désavantage.",
            ),
            setOf(EffetEtat.NE_VOIT_PAS, EffetEtat.AVANTAGE_CONTRE, EffetEtat.DESAVANTAGE_ATTAQUES),
            automatise = listOf(AVANTAGE_CONTRE, "Combat : ses attaques (monstre) sont lancées avec le désavantage."),
            alias = listOf("cécité", "aveuglée"),
        ),
        FicheEtat(
            ConditionCombat.CHARME,
            "Sous l'emprise d'un charmeur.",
            listOf(
                "Ne peut pas attaquer le charmeur, ni le cibler avec une capacité ou un effet magique nuisible.",
                "Le charmeur a l'avantage à tout test de caractéristique pour interagir socialement avec la créature.",
            ),
            setOf(EffetEtat.NE_PEUT_ATTAQUER_CHARMEUR),
            automatise = listOf("Rappel au MJ uniquement : le charmeur n'est pas mémorisé par l'application."),
        ),
        FicheEtat(
            ConditionCombat.SOURD,
            "N'entend plus rien.",
            listOf("Ne peut pas entendre et échoue automatiquement à tout test de caractéristique qui nécessite l'ouïe."),
            setOf(EffetEtat.N_ENTEND_PAS),
            automatise = listOf("Rappel au MJ uniquement."),
            alias = listOf("sourd", "assourdie"),
        ),
        FicheEtat(
            ConditionCombat.EFFRAYE,
            "Terrifié par une source de peur.",
            listOf(
                "Désavantage aux tests de caractéristique et aux jets d'attaque tant que la source de sa peur est en vue.",
                "Ne peut pas se rapprocher volontairement de la source de sa peur.",
            ),
            setOf(EffetEtat.DESAVANTAGE_TESTS, EffetEtat.DESAVANTAGE_ATTAQUES, EffetEtat.NE_PEUT_APPROCHER),
            automatise = listOf(
                "Combat : ses attaques (monstre) sont lancées avec le désavantage (source de la peur supposée en vue).",
                "Combat : un monstre effrayé ne s'approche pas, s'écarte du contact et tire s'il le peut.",
            ),
        ),
        FicheEtat(
            ConditionCombat.AGRIPPE,
            "Tenu par une autre créature.",
            listOf(
                "Vitesse 0, et ne peut bénéficier d'aucun bonus à sa vitesse.",
                "Désavantage aux jets d'attaque contre toute cible autre que celle qui l'agrippe.",
                "Celui qui agrippe peut le déplacer avec lui (sa vitesse est alors divisée par deux, sauf si la créature agrippée est TP ou deux tailles plus petite).",
                "L'état prend fin si l'agrippeur est Neutralisé, ou si un effet les éloigne l'un de l'autre. Évasion : action, test de Force (Athlétisme) ou de Dextérité (Acrobaties) contre le DD indiqué.",
            ),
            setOf(EffetEtat.IMMOBILE),
            automatise = listOf(NE_BOUGE_PAS, "Combat : appliqué automatiquement quand une attaque de monstre qui agrippe touche (DD d'évasion noté au journal)."),
            alias = listOf("agrippée", "empoigné", "empoignée"),
        ),
        FicheEtat(
            ConditionCombat.INCAPABLE_D_AGIR,
            "Hors d'état d'agir.",
            listOf(
                "Ne peut entreprendre aucune action, action bonus ni réaction.",
                "Sa concentration est rompue.",
                "Ne peut pas parler.",
                "S'il est Neutralisé au moment de lancer l'initiative, il a le désavantage à ce jet.",
            ),
            setOf(EffetEtat.AUCUNE_ACTION, EffetEtat.PERD_CONCENTRATION, EffetEtat.MUET),
            automatise = listOf(NE_PEUT_AGIR),
            alias = listOf("incapable d'agir", "incapacité", "invalidité", "neutralisée"),
        ),
        FicheEtat(
            ConditionCombat.INVISIBLE,
            "Impossible à voir.",
            listOf(
                "Surprise : avantage au jet d'initiative.",
                "Dissimulé : n'est pas affecté par les effets qui exigent que la cible soit vue, sauf si leur auteur peut la voir. L'équipement porté est dissimulé aussi.",
                "Les jets d'attaque contre elle ont le désavantage, et ses jets d'attaque ont l'avantage (sauf si l'autre créature peut la voir).",
            ),
            setOf(EffetEtat.AVANTAGE_ATTAQUES, EffetEtat.DESAVANTAGE_CONTRE),
            automatise = listOf("Combat : ses attaques (monstre) ont l'avantage, celles des monstres contre elle le désavantage."),
            alias = listOf("invisibilité"),
        ),
        FicheEtat(
            ConditionCombat.PARALYSE,
            "Totalement figé : ne bouge plus, ne parle plus, n'agit plus.",
            listOf(
                "Neutralisé (aucune action, action bonus ni réaction ; ne peut pas parler).",
                "Vitesse 0, et ne peut bénéficier d'aucun bonus à sa vitesse.",
                "Rate automatiquement ses jets de sauvegarde de Force et de Dextérité.",
                "Les jets d'attaque contre lui ont l'avantage.",
                "Toute attaque qui le touche est un coup critique si l'attaquant se trouve à 1,50 m ou moins.",
            ),
            setOf(EffetEtat.IMMOBILE, EffetEtat.ECHEC_JS_FOR_DEX, EffetEtat.AVANTAGE_CONTRE, EffetEtat.CRITIQUE_AU_CONTACT),
            inclut = setOf(ConditionCombat.INCAPABLE_D_AGIR),
            automatise = listOf(NE_PEUT_AGIR, NE_BOUGE_PAS, AVANTAGE_CONTRE, "Combat : une attaque de monstre au contact qui le touche est un coup critique (dés doublés).", ECHEC_JS),
            alias = listOf("paralysée", "paralysie"),
        ),
        FicheEtat(
            ConditionCombat.PETRIFIE,
            "Changé en pierre.",
            listOf(
                "Transformé, avec tout objet non magique porté, en une substance inanimée (généralement de la pierre) : son poids est multiplié par dix et il cesse de vieillir.",
                "Neutralisé (aucune action, action bonus ni réaction ; ne peut pas parler).",
                "Vitesse 0, et ne peut bénéficier d'aucun bonus à sa vitesse.",
                "Les jets d'attaque contre lui ont l'avantage.",
                "Rate automatiquement ses jets de sauvegarde de Force et de Dextérité.",
                "Résistance à tous les dégâts.",
                "Immunité à l'état Empoisonné (un poison déjà présent est suspendu, pas neutralisé).",
            ),
            setOf(EffetEtat.IMMOBILE, EffetEtat.AVANTAGE_CONTRE, EffetEtat.ECHEC_JS_FOR_DEX, EffetEtat.RESISTANCE_DEGATS, EffetEtat.INCONSCIENT_DU_MONDE),
            inclut = setOf(ConditionCombat.INCAPABLE_D_AGIR),
            automatise = listOf(NE_PEUT_AGIR, NE_BOUGE_PAS, AVANTAGE_CONTRE, ECHEC_JS, "Combat : les dégâts qu'il subit sont divisés par deux (résistance)."),
            alias = listOf("pétrifiée", "pétrification"),
        ),
        FicheEtat(
            ConditionCombat.EMPOISONNE,
            "Affaibli par un poison.",
            listOf("Désavantage aux jets d'attaque et aux tests de caractéristique."),
            setOf(EffetEtat.DESAVANTAGE_ATTAQUES, EffetEtat.DESAVANTAGE_TESTS),
            automatise = listOf("Combat : ses attaques (monstre) sont lancées avec le désavantage.", "Retiré par l'Imposition des mains du paladin."),
            alias = listOf("empoisonnée", "poison"),
        ),
        FicheEtat(
            ConditionCombat.A_TERRE,
            "Allongé au sol.",
            listOf(
                "Seule option de déplacement : ramper, à moins de se relever (ce qui coûte la moitié de sa vitesse) et de mettre ainsi fin à l'état.",
                "Désavantage à ses jets d'attaque.",
                "Un jet d'attaque contre lui a l'avantage si l'attaquant est à 1,50 m ou moins ; sinon il subit le désavantage.",
            ),
            setOf(EffetEtat.RAMPE, EffetEtat.DESAVANTAGE_ATTAQUES, EffetEtat.A_TERRE_CONTRE),
            automatise = listOf(
                "Combat : attaques des monstres contre lui avec l'avantage au contact, le désavantage à distance ; ses attaques (monstre) avec le désavantage.",
                "Combat : appliqué automatiquement quand une attaque de monstre qui renverse touche.",
            ),
            alias = listOf("allongé", "allongée", "couché", "couchée", "renversé", "renversée"),
        ),
        FicheEtat(
            ConditionCombat.ENTRAVE,
            "Ligoté, englué ou pris dans un piège.",
            listOf(
                "Vitesse 0, et ne peut bénéficier d'aucun bonus à sa vitesse.",
                "Les jets d'attaque contre lui ont l'avantage, et ses propres jets d'attaque subissent le désavantage.",
                "Désavantage à ses jets de sauvegarde de Dextérité.",
            ),
            setOf(EffetEtat.IMMOBILE, EffetEtat.AVANTAGE_CONTRE, EffetEtat.DESAVANTAGE_ATTAQUES, EffetEtat.DESAVANTAGE_JS_DEX),
            automatise = listOf(NE_BOUGE_PAS, AVANTAGE_CONTRE, "Combat : ses attaques (monstre) sont lancées avec le désavantage."),
            alias = listOf("entravée", "immobilisé", "immobilisée"),
        ),
        FicheEtat(
            ConditionCombat.ETOURDI,
            "Sonné, incapable de réagir.",
            listOf(
                "Neutralisé (aucune action, action bonus ni réaction ; ne peut pas parler).",
                "Rate automatiquement ses jets de sauvegarde de Force et de Dextérité.",
                "Les jets d'attaque contre lui ont l'avantage.",
            ),
            setOf(EffetEtat.ECHEC_JS_FOR_DEX, EffetEtat.AVANTAGE_CONTRE),
            inclut = setOf(ConditionCombat.INCAPABLE_D_AGIR),
            automatise = listOf(NE_PEUT_AGIR, AVANTAGE_CONTRE, ECHEC_JS),
            alias = listOf("étourdie", "sonné", "sonnée"),
        ),
        FicheEtat(
            ConditionCombat.INCONSCIENT,
            "Évanoui ou plongé dans un sommeil magique.",
            listOf(
                "Neutralisé (aucune action, action bonus ni réaction ; ne peut pas parler) et À terre ; lâche ce qu'il tient. Quand l'état prend fin, la créature reste À terre.",
                "Vitesse 0, et ne peut bénéficier d'aucun bonus à sa vitesse.",
                "Les jets d'attaque contre lui ont l'avantage.",
                "Rate automatiquement ses jets de sauvegarde de Force et de Dextérité.",
                "Toute attaque qui le touche est un coup critique si l'attaquant se trouve à 1,50 m ou moins.",
                "N'a pas conscience de son environnement.",
            ),
            setOf(EffetEtat.IMMOBILE, EffetEtat.AVANTAGE_CONTRE, EffetEtat.ECHEC_JS_FOR_DEX, EffetEtat.CRITIQUE_AU_CONTACT, EffetEtat.INCONSCIENT_DU_MONDE, EffetEtat.LACHE_TOUT),
            inclut = setOf(ConditionCombat.INCAPABLE_D_AGIR, ConditionCombat.A_TERRE),
            automatise = listOf(NE_PEUT_AGIR, NE_BOUGE_PAS, AVANTAGE_CONTRE, "Combat : une attaque de monstre au contact qui le touche est un coup critique (dés doublés).", ECHEC_JS),
            alias = listOf("inconsciente", "évanoui", "évanouie", "endormi", "endormie"),
        ),
        FicheEtat(
            ConditionCombat.CONCENTRATION,
            "Maintient un sort de concentration (pas un état des règles).",
            listOf(
                "Le lanceur maintient un sort à concentration : il ne peut en maintenir qu'un seul à la fois.",
                "Chaque fois qu'il subit des dégâts, il fait un JS de Constitution (DD 10 ou la moitié des dégâts, le plus élevé, DD 30 au plus) pour garder sa concentration.",
                "Elle est rompue s'il est Neutralisé ou meurt.",
            ),
            emptySet(),
            automatise = listOf("Combat : retirée automatiquement quand le combattant devient Neutralisé (ou un état qui l'inclut)."),
            officiel = false,
        ),
    )

    /**
     * Épuisement : état à niveaux, suivi par le compteur de fatigue des fiches (Character.exhaustionLevel)
     * et non par une case à cocher — présent dans le livre seulement.
     */
    const val NOM_EPUISEMENT = "Épuisement"
    val reglesEpuisement: List<String> = listOf(
        "État cumulatif, de 1 à 6 niveaux. Chaque effet qui l'inflige ajoute un niveau ; à 6 niveaux, la créature meurt.",
        "Tests de d20 : chaque jet de d20 (attaque, sauvegarde, test de caractéristique) est réduit de 2 × le niveau d'épuisement.",
        "Vitesse : réduite de 1,50 m × le niveau d'épuisement.",
        "Un repos long retire un niveau d'épuisement ; l'état prend fin quand le niveau tombe à 0.",
    )
    val automatiseEpuisement: List<String> = listOf(
        "Fiche : compteur « Fatigue » (0 à 6) réglable depuis la fiche et le menu latéral MJ.",
        "La faim (jours sans ration) ajoute automatiquement des niveaux d'épuisement.",
    )

    private val parCondition = fiches.associateBy { it.condition }

    fun fiche(condition: ConditionCombat): FicheEtat? = parCondition[condition]

    /** États proposés au choix (menu MJ, combat) : tous ceux du catalogue. */
    val choisissables: List<ConditionCombat> get() = fiches.map { it.condition }

    /** États effectivement subis, inclusions comprises (Paralysé → Neutralisé aussi). */
    fun developper(conditions: Collection<ConditionCombat>): Set<ConditionCombat> {
        val resultat = mutableSetOf<ConditionCombat>()
        val pile = ArrayDeque(conditions)
        while (pile.isNotEmpty()) {
            val c = pile.removeFirst()
            if (resultat.add(c)) parCondition[c]?.inclut?.let { pile.addAll(it) }
        }
        return resultat
    }

    fun effets(conditions: Collection<ConditionCombat>): Set<EffetEtat> =
        developper(conditions).flatMap { parCondition[it]?.effets.orEmpty() }.toSet()

    /** Premier état (parmi ceux choisis) qui empêche d'agir, pour l'expliquer : « Paralysé ». */
    fun bloquant(conditions: Collection<ConditionCombat>): ConditionCombat? =
        conditions.sortedBy { it.ordinal }.firstOrNull { EffetEtat.AUCUNE_ACTION in effets(listOf(it)) }

    fun peutAgir(conditions: Collection<ConditionCombat>): Boolean = EffetEtat.AUCUNE_ACTION !in effets(conditions)

    /** Premier état qui fixe la vitesse à 0, ou null si le combattant peut bouger. */
    fun immobilisant(conditions: Collection<ConditionCombat>): ConditionCombat? =
        conditions.sortedBy { it.ordinal }.firstOrNull { EffetEtat.IMMOBILE in effets(listOf(it)) }

    /** Réussite impossible au JS de [caracteristique] (« Dextérité », « Dex », « For »…). */
    fun echecAutomatiqueJs(conditions: Collection<ConditionCombat>, caracteristique: String?): Boolean {
        val c = caracteristique?.trim()?.lowercase() ?: return false
        return EffetEtat.ECHEC_JS_FOR_DEX in effets(conditions) && (c.startsWith("for") || c.startsWith("dex") || c.startsWith("dé"))
    }

    fun resistanceTousDegats(conditions: Collection<ConditionCombat>): Boolean = EffetEtat.RESISTANCE_DEGATS in effets(conditions)

    /**
     * Mode du jet d'attaque de [attaquant] contre [cible] d'après leurs états, et pourquoi
     * (« cible Paralysée », « attaquant Empoisonné »…). Avantage et désavantage s'annulent.
     */
    fun modeAttaque(
        attaquant: Collection<ConditionCombat>,
        cible: Collection<ConditionCombat>,
        auContact: Boolean,
    ): Pair<ModeJet, List<String>> {
        val avantages = mutableListOf<String>()
        val desavantages = mutableListOf<String>()
        developper(attaquant).forEach { c ->
            val e = parCondition[c]?.effets.orEmpty()
            if (EffetEtat.AVANTAGE_ATTAQUES in e) avantages += "attaquant ${c.label}"
            if (EffetEtat.DESAVANTAGE_ATTAQUES in e) desavantages += "attaquant ${c.label}"
        }
        developper(cible).forEach { c ->
            val e = parCondition[c]?.effets.orEmpty()
            if (EffetEtat.AVANTAGE_CONTRE in e) avantages += "cible ${c.label}"
            if (EffetEtat.DESAVANTAGE_CONTRE in e) desavantages += "cible ${c.label}"
            if (EffetEtat.A_TERRE_CONTRE in e) {
                if (auContact) avantages += "cible ${c.label} au contact" else desavantages += "cible ${c.label} à distance"
            }
        }
        val mode = when {
            avantages.isNotEmpty() && desavantages.isEmpty() -> ModeJet.AVANTAGE
            desavantages.isNotEmpty() && avantages.isEmpty() -> ModeJet.DESAVANTAGE
            else -> ModeJet.NORMAL
        }
        val raisons = when (mode) {
            ModeJet.AVANTAGE -> avantages
            ModeJet.DESAVANTAGE -> desavantages
            ModeJet.NORMAL -> if (avantages.isNotEmpty()) listOf("avantage et désavantage s'annulent") else emptyList()
        }
        return mode to raisons.distinct()
    }

    /** Une attaque qui touche [cible] à 1,50 m ou moins devient un critique (Paralysé, Inconscient). */
    fun critiqueAuContact(cible: Collection<ConditionCombat>, auContact: Boolean): Boolean =
        auContact && EffetEtat.CRITIQUE_AU_CONTACT in effets(cible)

    /** Effets lisibles d'une liste d'états, sans doublon : pour le menu MJ et l'écran du joueur. */
    fun resumeEffets(conditions: Collection<ConditionCombat>): List<String> =
        effets(conditions).sortedBy { it.ordinal }.map { it.libelle }

    // ── Champ condition des fiches (texte libre séparé par des virgules) ──

    /** Clé de comparaison : sans accents ni casse, accords féminin/pluriel retirés. */
    internal fun cle(texte: String): String {
        val sansAccents = Normalizer.normalize(texte.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        // « Empoisonnées », « Empoisonnée », « Empoisonné » → « empoisonn ».
        return sansAccents.replace('’', '\'').replace(Regex("\\s+"), " ").replace(Regex("[es]+$"), "")
    }

    private val parCle: Map<String, ConditionCombat> = buildMap {
        fiches.forEach { f ->
            (listOf(f.condition.label, f.condition.name.replace('_', ' ')) + f.alias).forEach { put(cle(it), f.condition) }
        }
    }

    /** État reconnu dans un libellé (« Paralysée », « à terre », « Incapable d'agir »), ou null. */
    fun reconnaitre(libelle: String): ConditionCombat? = parCle[cle(libelle)]

    private fun morceaux(texte: String): List<String> = texte.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }

    /** États reconnus dans le champ condition d'une fiche. */
    fun lire(texte: String): Set<ConditionCombat> = morceaux(texte).mapNotNull(::reconnaitre).toSet()

    /** Mentions libres du champ condition qui ne sont pas des états connus (« Maudit », « Ivre »…). */
    fun autres(texte: String): List<String> = morceaux(texte).filter { reconnaitre(it) == null }

    /**
     * Réécrit le champ condition avec [conditions] : les mentions libres sont conservées, les états
     * gardent leur libellé officiel, dans l'ordre du catalogue.
     */
    fun ecrire(texte: String, conditions: Collection<ConditionCombat>): String =
        (conditions.sortedBy { it.ordinal }.map { it.label } + autres(texte)).distinct().joinToString(", ")

    // ── États infligés par les monstres (bestiaire) ──

    private val libellesBestiaire: List<Pair<String, ConditionCombat>> =
        fiches.filter { it.officiel }.map { it.condition.label to it.condition }.sortedByDescending { it.first.length }
    private val alternance = libellesBestiaire.joinToString("|") { Regex.escape(it.first) }
    // « subit l’état Agrippé (évasion DD 13) », « se retrouve avec l’état À terre », « subit les états Aveuglé et Entravé ».
    private val mentionRegex = Regex("""(?:subit|subissent|avec|reçoit|soumise? à)\s+l(?:[’']état|es états)\s+($alternance)(?:\s*,\s*($alternance))?(?:\s+et\s+($alternance))?""")
    private val evasionRegex = Regex("""^\s*\(évasion DD\s*(\d+)""")
    private val sauvegardeRegex = Regex("""(?:JS|[Jj]et de sauvegarde de)\s+(\p{L}+)\s*(?::\s*DD|DD)\s*(\d+)""")

    /**
     * États qu'inflige le texte d'une action de monstre. Un état mentionné après un jet de
     * sauvegarde (« JS Constitution : DD 10. Échec : … l’état Paralysé ») n'est subi qu'en cas
     * d'échec : la sauvegarde est retenue pour que le MJ tranche.
     */
    fun infligesPar(texte: String): List<ConditionInfligee> {
        val resultat = mutableListOf<ConditionInfligee>()
        mentionRegex.findAll(texte).forEach { m ->
            val avant = texte.substring(0, m.range.first)
            val js = sauvegardeRegex.findAll(avant).lastOrNull()
            val evasion = evasionRegex.find(texte.substring(m.range.last + 1))?.groupValues?.get(1)?.toIntOrNull()
            listOf(m.groups[1], m.groups[2], m.groups[3]).mapNotNull { it?.value }.forEach { libelle ->
                val condition = libellesBestiaire.first { it.first == libelle }.second
                if (resultat.none { it.condition == condition }) {
                    resultat += ConditionInfligee(
                        condition = condition,
                        sauvegarde = js?.groupValues?.get(1),
                        dd = js?.groupValues?.get(2)?.toIntOrNull(),
                        evasionDd = if (condition == ConditionCombat.AGRIPPE) evasion else null,
                    )
                }
            }
        }
        return resultat
    }
}

/** État infligé par une action de monstre : d'office si elle touche, ou sur un JS raté. */
data class ConditionInfligee(
    val condition: ConditionCombat,
    val sauvegarde: String? = null,
    val dd: Int? = null,
    val evasionDd: Int? = null,
) {
    val surEchecJs: Boolean get() = dd != null

    /** « Paralysé (JS Constitution DD 10) », « Agrippé (évasion DD 13) ». */
    val libelle: String
        get() = condition.label + when {
            dd != null -> " (JS ${sauvegarde ?: ""} DD $dd)".replace("  ", " ")
            evasionDd != null -> " (évasion DD $evasionDd)"
            else -> ""
        }
}
