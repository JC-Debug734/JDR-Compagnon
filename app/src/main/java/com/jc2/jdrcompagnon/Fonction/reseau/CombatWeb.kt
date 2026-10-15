package com.jc2.jdrcompagnon.network

import android.content.Context
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.Terrain
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerFormuleDes
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.JetCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.feature_combat.ui.attaquesDuPersonnage
import com.jc2.jdrcompagnon.feature_combat.ui.iconeEtat
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Combattant tel que la table le voit (PV exacts des personnages seulement). */
@Serializable
data class CombattantTableWeb(
    val id: String,
    val nom: String,
    val monstre: Boolean,
    val initiative: Int?,
    val portrait: String?,
    val pv: String?,
    val ratio: Int,
    val sante: String,
    val actif: Boolean,
    val conditions: List<String>,
    val icones: String,
    val position: String?,
    val horsCombat: Boolean,
    val dejaJoue: Boolean,
    val boss: Boolean,
)

@Serializable
data class BossTableWeb(
    val nom: String,
    val titre: String,
    val phase: String,
    val ratio: Int,
    val points: Int,
    val pointsMax: Int,
    val prepare: Boolean,
    val vaincu: Boolean,
    val annonce: String?,
)

/** Le combat en cours pour la page de la table (TV, PC, téléphone d'un joueur sans l'appli). */
@Serializable
data class CombatTableWeb(
    val id: String,
    val titre: String,
    val round: Int,
    val enCours: Boolean,
    val termine: Boolean,
    val actifId: String?,
    val actifNom: String?,
    val actifMonstre: Boolean,
    val prochainNom: String?,
    val boss: List<BossTableWeb>,
    val combattants: List<CombattantTableWeb>,
    val recit: List<RecitData>,
)

/** Personnage qu'un joueur sans l'appli peut incarner depuis la page /joueur. */
@Serializable
data class PersonnageWeb(val id: String, val nom: String)

@Serializable
data class ArmeWeb(val nom: String, val bonus: Int, val formule: String?, val distance: Boolean)

@Serializable
data class CibleWeb(val id: String, val nom: String, val sante: String, val distance: String, val allie: Boolean)

@Serializable
data class ActionWeb(val id: String, val nom: String, val description: String)

@Serializable
data class MoiWeb(val id: String, val nom: String, val pv: Int, val pvMax: Int, val ca: Int, val conditions: List<String>, val horsCombat: Boolean)

/** Vue d'un joueur sans l'appli sur son téléphone (page /joueur). */
@Serializable
data class JoueurWeb(
    val titre: String,
    val round: Int,
    val enCours: Boolean,
    val actifNom: String?,
    val monTour: Boolean,
    val prochain: Boolean,
    val peutAgir: Boolean,
    val moi: MoiWeb,
    // Action choisie ce round (« Attaquer : Épée → Gobelin »), et si son jet d'attaque est parti.
    val action: String?,
    val jetEnvoye: Boolean,
    val armes: List<ArmeWeb>,
    val cibles: List<CibleWeb>,
    val actions: List<ActionWeb>,
    val recit: List<RecitData>,
    // Terrain (voir Terrain) : zones avec leurs combattants, zone du joueur et zones parcourues en marchant.
    val terrain: List<ZoneWeb> = emptyList(),
    val maZone: Int = 1,
    val pas: Int = 1,
)

@Serializable
data class PionWeb(val nom: String, val monstre: Boolean, val moi: Boolean, val actif: Boolean, val horsCombat: Boolean)

@Serializable
data class ZoneWeb(val index: Int, val nom: String, val pions: List<PionWeb>)

@Serializable
data class ReponseWeb(val ok: Boolean, val message: String)

/**
 * Le combat sur le réseau local, sans l'appli : état pour la page de la table, et page /joueur
 * où un joueur choisit son personnage puis joue son tour depuis le navigateur de son téléphone
 * (son attaque, avec ses vrais dés ou ceux de l'appli ; ses actions courantes). Le MJ garde la
 * main : il applique les dégâts comme pour un joueur connecté.
 */
object CombatWeb {

    private val json = Json { encodeDefaults = true }

    private fun combat(): CombatEnCours? = CombatSession.etat.value

    fun table(context: Context): String {
        val c = combat() ?: return "null"
        val actif = c.actif
        val vue = c.versJoueur(null)
        val donnees = CombatTableWeb(
            id = c.id,
            titre = c.nomCombat,
            round = c.round,
            enCours = c.phase == PhaseCombat.EN_COURS,
            termine = c.termine,
            actifId = actif?.id,
            actifNom = actif?.nom,
            actifMonstre = actif?.estMonstre == true,
            prochainNom = c.prochain?.nom,
            boss = vue.boss.map { b ->
                BossTableWeb(b.nom, b.titre, b.phaseNom.orEmpty(), b.ratioPct, b.pointsLegendaires, b.pointsLegendairesMax, b.prepare, b.vaincu, b.annonces.firstOrNull())
            },
            combattants = c.combattants.mapIndexed { i, x ->
                val sante = santeVisible(x)
                CombattantTableWeb(
                    id = x.id,
                    nom = x.nom,
                    monstre = x.estMonstre,
                    initiative = x.initiative,
                    portrait = PortraitsCombat.version(context, x)?.let { v -> "/portrait?id=${android.net.Uri.encode(x.id)}&v=$v" },
                    pv = sante.pv?.let { "$it/${sante.pvMax}" },
                    ratio = sante.ratio,
                    sante = sante.etat,
                    actif = x.id == actif?.id,
                    conditions = x.conditions.sortedBy { it.ordinal }.map { it.label },
                    icones = x.conditions.sortedBy { it.ordinal }.joinToString("") { iconeEtat(it) },
                    position = if (x.horsCombat) null else c.libelleContact(x.id),
                    horsCombat = x.horsCombat,
                    dejaJoue = c.phase == PhaseCombat.EN_COURS && i < c.tourIndex,
                    boss = x.id in c.boss,
                )
            },
            recit = c.recit.pourLaTable(),
        )
        return json.encodeToString(donnees)
    }

    /**
     * Personnages jouables depuis la page : ceux du camp des joueurs que ne pilotent ni l'IA ni un
     * joueur connecté avec l'appli.
     */
    private fun jouables(c: CombatEnCours): List<Combattant> {
        val connectes = NetworkSessionManager.connectedCharacters().map { it.id }.toSet()
        return c.combattants.filter { !it.estMonstre && !it.inerte && !it.piloteParIa && (it.characterId == null || it.characterId !in connectes) }
    }

    fun personnages(): String {
        val c = combat() ?: return "[]"
        return json.encodeToString(jouables(c).map { PersonnageWeb(it.id, it.nom) })
    }

    /** Armes d'un personnage : chargées depuis sa fiche si le combat ne les connaît pas encore. */
    private fun armes(context: Context, moi: Combattant): List<ArmeWeb> {
        var attaques = moi.attaques
        if (attaques.isEmpty() && moi.characterId != null) {
            val perso = GameState.characters.value.firstOrNull { it.id == moi.characterId }
            if (perso != null) {
                attaques = runCatching { runBlocking { attaquesDuPersonnage(context, perso) } }.getOrDefault(emptyList())
                if (attaques.isNotEmpty()) CombatSession.definirAttaques(moi.id, attaques)
            }
        }
        return attaques.filter { it.type != TypeAttaqueMonstre.AUTRE && it.bonusToucher != null && it.nom !in moi.actionsDesactivees }
            .map { ArmeWeb(it.nom, it.bonusToucher ?: 0, it.formuleDegats, it.type == TypeAttaqueMonstre.DISTANCE) }
    }

    fun joueur(context: Context, combattantId: String?): String {
        val c = combat() ?: return "null"
        val moi = jouables(c).firstOrNull { it.id == combattantId } ?: return "null"
        val d = c.declarations[moi.id]
        val donnees = JoueurWeb(
            titre = c.nomCombat,
            round = c.round,
            enCours = c.phase == PhaseCombat.EN_COURS,
            actifNom = c.actif?.nom,
            monTour = c.actif?.id == moi.id,
            prochain = c.prochain?.id == moi.id,
            peutAgir = Etats.peutAgir(moi.conditions) && !moi.horsCombat && d?.degatsAppliques != true,
            moi = MoiWeb(moi.id, moi.nom, moi.pv, moi.pvMax, moi.ca, moi.conditions.map { it.label }, moi.horsCombat),
            action = d?.let { it.libelle + (it.cibleNom?.let { n -> " → $n" } ?: "") },
            jetEnvoye = c.jetsDuRound.any { it.combattantId == moi.id && it.libelle == "Attaque" },
            armes = armes(context, moi),
            cibles = c.combattants.filter { it.id != moi.id && !it.horsCombat }
                .sortedBy { it.estMonstre == moi.estMonstre }
                .map { x -> CibleWeb(x.id, x.nom, santeVisible(x, moi.id).etat,c.distance(moi.id, x.id).court, x.estMonstre == moi.estMonstre) },
            actions = ActionsCombat.toutes.filter { it.id !in setOf("attaquer", "magie") }.map { ActionWeb(it.id, it.nom, it.description) },
            recit = c.recit.pourLaTable().takeLast(15),
            terrain = (Terrain.derniere downTo 0).map { z ->
                ZoneWeb(z, Terrain.nom(z), c.combattants.filter { !it.inerte && Terrain.zoneDe(it) == z }
                    .map { x -> PionWeb(x.nom, x.estMonstre, x.id == moi.id, x.id == c.actif?.id, x.horsCombat) })
            },
            maZone = Terrain.zoneDe(moi),
            pas = Terrain.pasMax(moi.vitesse),
        )
        return json.encodeToString(donnees)
    }

    /**
     * Action envoyée depuis la page /joueur pendant le tour du personnage : une attaque (d20 et
     * dégâts du joueur, ou lancés par l'appli quand il les laisse vides), ou une action courante
     * (Esquiver, Se précipiter…, avec une précision libre).
     */
    fun agir(params: Map<String, String>): String {
        fun reponse(ok: Boolean, message: String) = json.encodeToString(ReponseWeb(ok, message))
        val c = combat() ?: return reponse(false, "Aucun combat en cours.")
        val moi = jouables(c).firstOrNull { it.id == params["id"] } ?: return reponse(false, "Personnage introuvable.")
        if (c.actif?.id != moi.id) return reponse(false, "Ce n'est pas votre tour.")
        if (!Etats.peutAgir(moi.conditions) || moi.horsCombat) return reponse(false, "Vous ne pouvez pas agir.")
        if (c.declarations[moi.id]?.degatsAppliques == true) return reponse(false, "Votre action est déjà résolue.")
        val action = params["action"].orEmpty()
        val cible = params["cible"]?.let { id -> c.combattants.firstOrNull { it.id == id } }
        // Zone du terrain où finir le tour (null = ne bouge pas).
        val zoneVisee = params["zone"]?.toIntOrNull()?.coerceIn(0, Terrain.derniere)?.takeIf { it != Terrain.zoneDe(moi) }
        if (action != "attaquer") {
            val a = ActionsCombat.toutes.firstOrNull { it.id == action } ?: return reponse(false, "Action inconnue.")
            val precision = params["detail"]?.trim()?.take(200)?.takeIf { it.isNotEmpty() }
            CombatSession.declarer(DeclarationAction(moi.id, a.nom, actionId = a.id, cibleId = cible?.id, cibleNom = cible?.nom, detail = precision, zoneVisee = zoneVisee))
            return reponse(true, "${a.nom} : annoncé au MJ.")
        }
        val arme = moi.attaques.firstOrNull { it.nom == params["arme"] && it.bonusToucher != null } ?: return reponse(false, "Choisissez une arme.")
        if (cible == null) return reponse(false, "Choisissez une cible.")
        if (c.jetsDuRound.any { it.combattantId == moi.id && it.libelle == "Attaque" } && (c.relances[moi.id] ?: 0) <= 0) {
            return reponse(false, "Votre attaque est déjà partie : le MJ peut vous accorder un autre jet.")
        }
        val bonus = arme.bonusToucher ?: 0
        val saisi = params["d20"]?.toIntOrNull()?.takeIf { it in 1..20 }
        val d20 = saisi ?: CombatSession.aleatoire.nextInt(1, 21)
        val degats = params["degats"]?.toIntOrNull()?.takeIf { it >= 0 }
            ?: arme.formuleDegats?.let { lancerFormuleDes(it, CombatSession.aleatoire, critique = d20 == 20) }
        CombatSession.declarer(
            DeclarationAction(moi.id, "Attaquer : ${arme.nom}", actionId = "attaquer", cibleId = cible.id, cibleNom = cible.nom, attaquesJoueur = listOf(arme.nom), zoneVisee = zoneVisee)
        )
        CombatSession.enregistrerJet(
            JetCombat(
                combattantId = moi.id,
                round = c.round,
                libelle = "Attaque",
                formule = "1d20${if (bonus >= 0) "+$bonus" else "$bonus"}",
                des = listOf(d20),
                bonus = bonus,
                total = d20 + bonus,
                manuel = saisi != null,
                formuleDegats = degats?.let { arme.formuleDegats ?: "saisis" },
                totalDegats = degats,
                cibleId = cible.id,
                arme = arme.nom,
                typeDegats = arme.typeDegats,
            )
        )
        return reponse(true, "Attaque envoyée : d20 $d20 + $bonus = ${d20 + bonus}" + (degats?.let { ", $it dégâts si elle touche" } ?: "") + ".")
    }
}
