package com.jc2.jdrcompagnon.feature_butin.domain

import com.jc2.jdrcompagnon.ui.Character
import kotlin.random.Random

/** Pièces trouvées sur une créature (taux de change du SRD : 1 pp = 10 po = 20 pe = 100 pa = 1 000 pc). */
data class Bourse(val pc: Int = 0, val pa: Int = 0, val pe: Int = 0, val po: Int = 0, val pp: Int = 0) {
    val vide: Boolean get() = pc + pa + pe + po + pp == 0

    /** Valeur totale en pièces de cuivre. */
    val valeurPc: Int get() = pp * 1000 + po * 100 + pe * 50 + pa * 10 + pc

    /** Valeur totale arrondie à la pièce d'or inférieure (trésor de groupe, qui ne compte qu'en po). */
    val valeurPo: Int get() = valeurPc / 100

    fun libelle(): String = listOf(pp to "pp", po to "po", pe to "pe", pa to "pa", pc to "pc")
        .filter { it.first > 0 }
        .joinToString(", ") { "${it.first} ${it.second}" }

    operator fun plus(autre: Bourse) = Bourse(pc + autre.pc, pa + autre.pa, pe + autre.pe, po + autre.po, pp + autre.pp)
}

/**
 * Un objet du butin. [nom] reprend autant que possible le nom exact d'un objet du SRD (« Potion de
 * guérison », « Rations (1 jour) »…) pour qu'il soit reconnu une fois envoyé dans un inventaire ;
 * [note] précise l'état ou l'origine sans polluer ce nom. [possede] : objet réellement porté par
 * un PNJ (retiré de sa fiche quand il est donné).
 */
data class ObjetButin(
    val nom: String,
    val quantite: Int = 1,
    val note: String = "",
    val possede: Boolean = false,
) {
    val libelle: String get() = if (quantite > 1) "${quantite}x $nom" else nom
}

data class Butin(val bourse: Bourse = Bourse(), val objets: List<ObjetButin> = emptyList()) {
    val vide: Boolean get() = bourse.vide && objets.isEmpty()
    operator fun plus(autre: Butin) = Butin(bourse + autre.bourse, objets + autre.objets)
}

/** Grande famille de créature, déduite du champ « Type » du bestiaire (« Fée (Gobelinoïde) »…). */
enum class FamilleCreature {
    HUMANOIDE, BETE, MORT_VIVANT, DRAGON, FIELON, CELESTE, ELEMENTAIRE, ARTIFICIEL, PLANTE, VASE, FEE, GEANT,
    ABERRATION, MONSTRUOSITE, AUTRE;

    companion object {
        fun depuisType(type: String?): FamilleCreature {
            val t = type.orEmpty().lowercase()
            return when {
                "gobelinoïde" in t -> HUMANOIDE
                "humanoïde" in t -> HUMANOIDE
                "nuée" in t || "bête" in t -> BETE
                "mort-vivant" in t -> MORT_VIVANT
                "dragon" in t -> DRAGON
                "fiélon" in t -> FIELON
                "céleste" in t -> CELESTE
                "élémentaire" in t -> ELEMENTAIRE
                "artificiel" in t -> ARTIFICIEL
                "plante" in t -> PLANTE
                "vase" in t -> VASE
                "fée" in t -> FEE
                "géant" in t -> GEANT
                "aberration" in t -> ABERRATION
                "monstruosité" in t -> MONSTRUOSITE
                else -> AUTRE
            }
        }
    }
}

/**
 * Butin aléatoire mais crédible d'une créature vaincue (ou fouillée, dépouillée, rançonnée...) :
 *  - les pièces suivent la table « Trésor individuel » du Guide du maître selon la puissance (FP),
 *    et seules les créatures qui ont l'usage de l'argent en portent (humanoïdes, fées, géants,
 *    fiélons ; un peu moins souvent les morts-vivants et les monstres, restes de leurs victimes) ;
 *  - l'équipement listé sur la fiche (« Équipement: arc court, bouclier… ») est récupérable,
 *    avec ses munitions ;
 *  - les bêtes et créatures non civilisées donnent des matières premières (peau, crocs, viande,
 *    écailles, glande à venin...) dont la quantité dépend de la taille ;
 *  - un peu de bric-à-brac de poche, puis potions, gemmes et objets d'art plus la créature est
 *    puissante.
 */
object GenerateurButin {

    private val fractionRegex = Regex("""^\s*(\d+)\s*/\s*(\d+)""")
    private val nombreRegex = Regex("""^\s*(\d+)""")

    /** « 1/4 (50 PX ; BM +2) » → 0,25 ; « 5 (1 800 PX...) » → 5,0 ; illisible → 0. */
    fun facteurPuissance(fp: String?): Double {
        val texte = fp.orEmpty()
        fractionRegex.find(texte)?.let { m ->
            val den = m.groupValues[2].toDouble()
            return if (den == 0.0) 0.0 else m.groupValues[1].toDouble() / den
        }
        return nombreRegex.find(texte)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun Random.d(nb: Int, faces: Int): Int = (1..nb).sumOf { nextInt(1, faces + 1) }

    /** Table « Trésor individuel » (Guide du maître) selon la puissance. */
    fun bourseIndividuelle(fp: Double, random: Random): Bourse {
        val jet = random.nextInt(1, 101)
        return when {
            fp <= 4 -> when {
                jet <= 30 -> Bourse(pc = random.d(5, 6))
                jet <= 60 -> Bourse(pa = random.d(4, 6))
                jet <= 70 -> Bourse(pe = random.d(3, 6))
                jet <= 95 -> Bourse(po = random.d(3, 6))
                else -> Bourse(pp = random.d(1, 6))
            }
            fp <= 10 -> when {
                jet <= 30 -> Bourse(pc = random.d(4, 6) * 100, pe = random.d(1, 6) * 10)
                jet <= 60 -> Bourse(pa = random.d(6, 6) * 10, po = random.d(2, 6) * 10)
                jet <= 70 -> Bourse(pe = random.d(3, 6) * 10, po = random.d(2, 6) * 10)
                jet <= 95 -> Bourse(po = random.d(4, 6) * 10)
                else -> Bourse(po = random.d(2, 6) * 10, pp = random.d(3, 6))
            }
            fp <= 16 -> when {
                jet <= 20 -> Bourse(pa = random.d(4, 6) * 100, po = random.d(1, 6) * 100)
                jet <= 35 -> Bourse(pe = random.d(1, 6) * 100, po = random.d(1, 6) * 100)
                jet <= 75 -> Bourse(po = random.d(2, 6) * 100, pp = random.d(1, 6) * 10)
                else -> Bourse(po = random.d(2, 6) * 100, pp = random.d(2, 6) * 10)
            }
            else -> when {
                jet <= 15 -> Bourse(pe = random.d(2, 6) * 1000, po = random.d(8, 6) * 100)
                jet <= 55 -> Bourse(po = random.d(1, 6) * 1000, pp = random.d(1, 6) * 100)
                else -> Bourse(po = random.d(1, 6) * 1000, pp = random.d(2, 6) * 100)
            }
        }
    }

    // Bric-à-brac de poche d'un humanoïde : surtout des objets du SRD (envoyables tels quels).
    private val objetsDePoche = listOf(
        ObjetButin("Boîte de jeux (dés)", note = "dés en os usés"),
        ObjetButin("Boîte de jeux (cartes)", note = "jeu de cartes écorné"),
        ObjetButin("Torche"),
        ObjetButin("Boîte à amadou"),
        ObjetButin("Outre", note = "à moitié pleine"),
        ObjetButin("Bougie"),
        ObjetButin("Ficelle"),
        ObjetButin("Flasque", note = "alcool fort"),
        ObjetButin("Sac"),
        ObjetButin("Rations (1 jour)"),
        ObjetButin("Lettre froissée", note = "à rédiger par le MJ : une piste ?"),
        ObjetButin("Clé en fer", note = "ouvre quelque chose, quelque part"),
        ObjetButin("Pipe et tabac"),
        ObjetButin("Amulette porte-bonheur", note = "sans valeur magique"),
        ObjetButin("Carte griffonnée", note = "un trajet ou une cachette"),
        ObjetButin("Médaillon", note = "une mèche de cheveux à l'intérieur"),
    )

    private val gemmes10 = listOf("Agate", "Hématite", "Quartz bleu", "Turquoise", "Obsidienne")
    private val gemmes100 = listOf("Améthyste", "Grenat", "Jade", "Perle", "Tourmaline")
    private val gemmes1000 = listOf("Saphir", "Émeraude", "Rubis", "Opale noire")
    private val objetsArt = listOf(
        "Statuette en os sculpté" to 25, "Bracelet en argent" to 25, "Calice en argent" to 250,
        "Bague en or sertie" to 250, "Masque cérémoniel en or" to 750,
    )

    /**
     * Butin d'un monstre du bestiaire. [taille] : lettre du champ « Taille » (TP, P, M, G, TG, Gig) ;
     * [equipement] : champ « Équipement » de la fiche ; [random] : graine stable pour qu'un même
     * vaincu donne toujours le même butin tant que le MJ ne le relance pas.
     */
    fun pourMonstre(
        nom: String,
        type: String?,
        fp: String?,
        taille: String? = null,
        equipement: String? = null,
        random: Random = Random.Default,
    ): Butin {
        val famille = FamilleCreature.depuisType(type)
        val puissance = facteurPuissance(fp)
        val nomCourt = nom.lowercase()
        val objets = mutableListOf<ObjetButin>()

        // Pièces : selon l'usage que la créature fait de l'argent.
        val chancePieces = when (famille) {
            FamilleCreature.HUMANOIDE, FamilleCreature.GEANT, FamilleCreature.FIELON -> 100
            FamilleCreature.FEE, FamilleCreature.MORT_VIVANT, FamilleCreature.DRAGON -> 60
            FamilleCreature.ABERRATION, FamilleCreature.MONSTRUOSITE, FamilleCreature.CELESTE -> 30
            FamilleCreature.VASE -> 25
            else -> 0
        }
        var bourse = if (random.nextInt(100) < chancePieces) bourseIndividuelle(puissance, random) else Bourse()
        if (famille == FamilleCreature.VASE && !bourse.vide) {
            objets += ObjetButin("Pièces non digérées", note = "retrouvées dans la vase, acide à rincer")
        }

        // Équipement visible sur la fiche.
        equipementDepuisFiche(equipement, random).forEach { objets += it }

        // Matières premières et trophées.
        objets += composants(nom, nomCourt, famille, taille, puissance, random)

        // Bric-à-brac de poche (créatures qui portent des affaires).
        if (famille in setOf(FamilleCreature.HUMANOIDE, FamilleCreature.GEANT, FamilleCreature.FEE)) {
            val nb = random.nextInt(0, 3)
            objetsDePoche.shuffled(random).take(nb).forEach { objets += it }
            if (famille == FamilleCreature.GEANT && random.nextInt(100) < 50) {
                objets += ObjetButin("Sac de géant", note = listOf(
                    "un fromage entier, une marmite cabossée et 3 grosses pierres",
                    "un tonnelet de bière, une peau d'ours et un os à ronger",
                    "un chaudron, une chèvre morte et une cloche de temple",
                ).random(random))
            }
            if (lanceurDeSorts(nomCourt)) {
                objets += listOf(
                    ObjetButin("Sacoche à composantes"),
                    ObjetButin("Parchemin de sort (sort mineur)"),
                    ObjetButin("Symbole sacré"),
                    ObjetButin("Livre", note = "notes de rituels"),
                ).random(random)
            }
        }
        if (famille == FamilleCreature.MORT_VIVANT && random.nextInt(100) < 40) {
            objets += ObjetButin("Bijou terni", note = "vaut ${listOf(10, 25, 50).random(random)} po une fois nettoyé")
        }

        // Trésors plus rares selon la puissance (créatures intelligentes ou repaires).
        val porteTresor = chancePieces > 0
        if (porteTresor) {
            val chancePotion = when {
                puissance < 1 -> 8
                puissance <= 4 -> 15
                else -> 25
            }
            if (random.nextInt(100) < chancePotion) objets += ObjetButin("Potion de guérison")
            if (puissance >= 3 && random.nextInt(100) < 25) {
                val (liste, valeur) = when {
                    puissance >= 11 -> gemmes1000 to 1000
                    puissance >= 5 -> gemmes100 to 100
                    else -> gemmes10 to 10
                }
                objets += ObjetButin("${liste.random(random)} (gemme)", quantite = random.nextInt(1, 3), note = "$valeur po pièce")
            }
            if (puissance >= 5 && random.nextInt(100) < 20) {
                val (art, valeur) = objetsArt.filter { it.second <= puissance * 75 }.ifEmpty { objetsArt.take(1) }.random(random)
                objets += ObjetButin(art, note = "objet d'art, $valeur po")
            }
        }
        if (famille == FamilleCreature.DRAGON && puissance >= 5) {
            objets += ObjetButin("Indice du repaire", note = "le vrai trésor du dragon est dans son antre")
            bourse = bourse + Bourse(po = random.d(2, 6) * 10)
        }

        return Butin(bourse, objets.fusionnees())
    }

    private fun lanceurDeSorts(nomCourt: String): Boolean =
        listOf("mage", "prêtre", "acolyte", "druide", "sorci", "chaman", "cultiste", "nécromancien", "archimage").any { it in nomCourt }

    /** « arc court, bouclier, chemise de mailles » → objets SRD, munitions comprises. */
    private fun equipementDepuisFiche(equipement: String?, random: Random): List<ObjetButin> {
        val morceaux = equipement.orEmpty()
            .split(',', ';')
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() }
        return morceaux.flatMap { morceau ->
            val nom = morceau.replaceFirstChar { it.uppercase() }
            val bas = morceau.lowercase()
            val armure = listOf("armure", "chemise de mailles", "cotte", "cuirasse", "harnois", "broigne", "clibanion", "demi-plate").any { it in bas }
            buildList {
                add(ObjetButin(nom, note = if (armure && random.nextInt(100) < 50) "abîmée par le combat (à réparer)" else ""))
                when {
                    "arc" in bas -> add(ObjetButin("Flèches", quantite = random.d(2, 6), note = "récupérables"))
                    "arbalète" in bas -> add(ObjetButin("Carreaux", quantite = random.d(2, 6), note = "récupérables"))
                    "fronde" in bas -> add(ObjetButin("Billes de fronde", quantite = random.d(2, 6)))
                    "sarbacane" in bas -> add(ObjetButin("Dards", quantite = random.d(1, 6)))
                }
            }
        }
    }

    /** Nombre de « portions » récupérables (viande, écailles…) selon la taille. */
    private fun portions(taille: String?, random: Random): Int = when (taille?.trim()?.uppercase()) {
        "TP" -> 0
        "P" -> 1
        "M", null, "" -> random.nextInt(2, 5)
        "G" -> random.nextInt(6, 11)
        "TG" -> random.nextInt(15, 26)
        else -> random.nextInt(40, 61) // Gig
    }

    private fun composants(
        nom: String,
        nomCourt: String,
        famille: FamilleCreature,
        taille: String?,
        puissance: Double,
        random: Random,
    ): List<ObjetButin> {
        val p = portions(taille, random)
        val objets = mutableListOf<ObjetButin>()
        when (famille) {
            FamilleCreature.BETE -> {
                val oiseau = listOf("aigle", "faucon", "hibou", "chouette", "corbeau", "vautour", "oiseau", "rapace", "chauve-souris").any { it in nomCourt }
                val venimeux = listOf("araignée", "serpent", "scorpion", "mille-pattes", "guêpe", "crapaud").any { it in nomCourt }
                val fourrure = listOf("loup", "ours", "lion", "tigre", "panthère", "sanglier", "chèvre", "cerf", "élan", "renard", "chien", "mammouth", "félin", "hyène", "blaireau").any { it in nomCourt }
                val insecte = listOf("araignée", "scorpion", "mille-pattes", "guêpe", "nuée", "crabe", "insecte").any { it in nomCourt }
                if (p > 0 && !insecte) objets += ObjetButin("Rations (1 jour)", quantite = p, note = "viande à cuire, se gâte en 1 jour")
                if (fourrure) objets += ObjetButin("Peau de ${nom.lowercase()}", note = "à tanner, se vend 1 à 10 po selon l'état")
                if (oiseau) objets += ObjetButin("Plumes de ${nom.lowercase()}", quantite = random.nextInt(3, 13), note = "pour l'empennage ou les plumes d'écriture")
                if (venimeux) objets += ObjetButin("Glande à venin de ${nom.lowercase()}", note = "Médecine/Nature DD 12 pour la prélever ; base d'un poison")
                if (!oiseau && !insecte && random.nextInt(100) < 50) objets += ObjetButin("Crocs et griffes", quantite = random.nextInt(2, 7), note = "trophée de chasse")
                if (insecte && !venimeux) objets += ObjetButin("Carapace de ${nom.lowercase()}", note = "chitine, utile à un artisan")
            }
            FamilleCreature.DRAGON -> {
                objets += ObjetButin("Écailles de dragon", quantite = maxOf(1, p), note = "très recherchées par les armuriers")
                objets += ObjetButin("Dent de dragon", quantite = random.nextInt(1, 4))
                objets += ObjetButin("Sang de dragon (fiole)", quantite = random.nextInt(1, 3), note = "composante d'alchimie rare")
            }
            FamilleCreature.MORT_VIVANT -> {
                if ("squelette" in nomCourt || "zombi" in nomCourt) objets += ObjetButin("Os", quantite = random.nextInt(2, 7), note = "ossements anciens")
                objets += ObjetButin("Lambeaux de linceul", note = "d'une autre époque : indice sur sa vie passée")
                if (puissance >= 5) objets += ObjetButin("Poussière de non-mort (fiole)", note = "composante nécromantique")
            }
            FamilleCreature.FIELON -> {
                objets += ObjetButin("Ichor infernal (fiole)", note = "brûle la peau, composante de rituel")
                if (random.nextInt(100) < 50) objets += ObjetButin("Corne de ${nom.lowercase()}", note = "trophée inquiétant")
            }
            FamilleCreature.CELESTE -> objets += ObjetButin("Plume céleste", quantite = random.nextInt(1, 4), note = "luit faiblement, composante sacrée")
            FamilleCreature.ELEMENTAIRE -> objets += ObjetButin("Essence élémentaire (${nom.lowercase()})", note = "se dissipe en 1 semaine sans contenant scellé")
            FamilleCreature.ARTIFICIEL -> {
                objets += ObjetButin("Pièces détachées", quantite = random.nextInt(2, 7), note = "engrenages, rivets, plaques")
                if (puissance >= 3) objets += ObjetButin("Noyau d'animation", note = "inerte ; un artificier en donnerait cher")
            }
            FamilleCreature.PLANTE -> objets += ObjetButin("Composantes végétales (${nom.lowercase()})", quantite = random.nextInt(1, 5), note = "écorce, sève ou spores pour un herboriste")
            FamilleCreature.VASE -> objets += ObjetButin("Résidu acide (fiole)", note = "Acide utilisable une fois mis en fiole")
            FamilleCreature.ABERRATION -> objets += ObjetButin("Organe d'aberration", note = "œil ou tentacule, intéresse les mages")
            FamilleCreature.MONSTRUOSITE -> {
                if (p > 0 && random.nextInt(100) < 60) objets += ObjetButin("Rations (1 jour)", quantite = (p / 2).coerceAtLeast(1), note = "viande coriace, se gâte en 1 jour")
                objets += ObjetButin("Trophée de ${nom.lowercase()}", note = "griffe, corne ou crâne")
            }
            FamilleCreature.FEE -> if (random.nextInt(100) < 40) objets += ObjetButin("Poussière féerique (pincée)", note = "scintille au clair de lune")
            FamilleCreature.HUMANOIDE, FamilleCreature.GEANT, FamilleCreature.AUTRE -> Unit
        }
        return objets
    }

    /**
     * Possessions d'un PNJ (ou d'un personnage ennemi) : ce que sa fiche porte réellement (armes,
     * armure, sac, or), plus quelques objets de poche. Sans or sur la fiche, une bourse est tirée
     * selon son niveau (comme un monstre de puissance niveau/2).
     */
    fun pourPersonnage(character: Character, random: Random = Random.Default): Butin {
        val possessions = (character.equippedItems + character.weapons + character.armor + character.backpackItems)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val objets = possessions
            .groupingBy { it }
            .eachCount()
            .map { (nom, nb) -> ObjetButin(nom, quantite = nb, possede = true) }
            // Une arme listée à la fois dans weapons et equippedItems ne compte qu'une fois.
            .map { o -> if (o.nom in character.weapons && o.nom in character.equippedItems) o.copy(quantite = (o.quantite - 1).coerceAtLeast(1)) else o }
            .toMutableList()
        val bourse = if (com.jc2.jdrcompagnon.ui.Monnaie.totalPc(character) > 0)
            Bourse(pc = character.copper, pa = character.silver, pe = character.electrum, po = character.gold, pp = character.platinum)
        else bourseIndividuelle(character.level / 2.0, random)
        objetsDePoche.shuffled(random).take(random.nextInt(0, 3)).forEach { objets += it }
        return Butin(bourse, objets.fusionnees())
    }

    /** Regroupe les objets identiques (même nom, même note, même provenance). */
    private fun List<ObjetButin>.fusionnees(): List<ObjetButin> =
        groupBy { Triple(it.nom, it.note, it.possede) }
            .map { (cle, liste) -> ObjetButin(cle.first, liste.sumOf { it.quantite }, cle.second, cle.third) }
}
