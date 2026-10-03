package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsMonstreParser
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmesPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.DeclencheurIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.RegleProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.IaMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.cleDistance
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.JetCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.network.versJoueur
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RoundCombatTest {

    private val chefGobelin = """
        ### Chef gobelin
        Catégorie: Gobelins
        CA: 17 Initiative +2 (12)
        Pv: 21 (6d6)

        ## Actions
        Attaques multiples. Le gobelin effectue deux attaques, réparties à sa guise entre Cimeterre et Arc court.
        Cimeterre. Corps à corps : +4, allonge 1,50 m. Touché : 5 (1d6 + 2) dégâts tranchants, plus 2 (1d4) dégâts tranchants si le jet d’attaque avait l’Avantage.
        Arc court. À distance : +4, portée 24/96 m. Touché : 5 (1d6 + 2) dégâts perforants.

        ## Actions Bonus
        Fuite agile. Le gobelin entreprend l’action Désengagement ou Furtivité.
    """.trimIndent()

    private val souffle = AttaqueMonstre(
        nom = "Aspersion acide", type = TypeAttaqueMonstre.SAUVEGARDE, formuleDegats = "4d6", degatsMoyens = 14,
        sauvegarde = "Dextérité", dd = 12, recharge = 6, zone = true
    )
    private val morsure = AttaqueMonstre("Morsure", TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = 5, formuleDegats = "1d10 + 3", degatsMoyens = 8)

    private fun pj(nom: String, ca: Int = 14, pv: Int = 20, pvMax: Int = 20) =
        Combattant(id = nom, nom = nom, estMonstre = false, characterId = "c-$nom", ca = ca, pvMax = pvMax, pv = pv, bonusInitiative = 0, initiative = 10)

    private fun monstre(nom: String, profil: ProfilIA, attaques: List<AttaqueMonstre> = listOf(morsure), pv: Int = 20, init: Int = 15) =
        Combattant(id = nom, nom = nom, estMonstre = true, monstreNom = nom, ca = 12, pvMax = 20, pv = pv, bonusInitiative = 0,
            initiative = init, attaques = attaques, profilIA = profil)

    @After
    fun fin() {
        CombatSession.fermer()
        CombatSession.aleatoire = Random
    }

    @Test
    fun `actions du bestiaire lues`() {
        val profil = ProfilCombatMonstre.depuisFiche(MonsterParser.parse(chefGobelin).single().rawMarkdown)
        assertEquals(2, profil.actions.nbAttaquesMultiples)
        assertEquals(listOf("Cimeterre", "Arc court"), profil.actions.attaques.map { it.nom })
        val cimeterre = profil.actions.attaques.first()
        assertEquals(TypeAttaqueMonstre.CORPS_A_CORPS, cimeterre.type)
        assertEquals(4, cimeterre.bonusToucher)
        assertEquals("1d6 + 2", cimeterre.formuleDegats)
        assertEquals("tranchants", cimeterre.typeDegats)
        assertEquals(TypeAttaqueMonstre.DISTANCE, profil.actions.attaques[1].type)
    }

    @Test
    fun `capacite a sauvegarde avec recharge`() {
        val actions = ActionsMonstreParser.parser(
            "## Actions\nAspersion acide (recharge 6). JS Dextérité : DD 12, chaque créature dans une Ligne de 9 m. Échec : 14 (4d6) dégâts d’acide. Réussite : demi-dégâts."
        )
        val a = actions.attaques.single()
        assertEquals("Aspersion acide", a.nom)
        assertEquals(TypeAttaqueMonstre.SAUVEGARDE, a.type)
        assertEquals(6, a.recharge)
        assertEquals(12, a.dd)
        assertEquals("Dextérité", a.sauvegarde)
        assertEquals("acide", a.typeDegats)
        assertTrue(a.zone)
        assertEquals(ProfilIA.ARTILLEUR, ProfilIA.deduire(actions))
    }

    @Test
    fun `le predateur vise le plus blesse et le tacticien la CA la plus basse`() {
        val pjs = listOf(pj("Aldric", ca = 18, pv = 5), pj("Brune", ca = 12, pv = 20))
        assertEquals("Aldric", IaMonstre.decider(monstre("Loup", ProfilIA.PREDATEUR), pjs).cibleNom)
        assertEquals("Brune", IaMonstre.decider(monstre("Loup", ProfilIA.TACTICIEN), pjs).cibleNom)
    }

    @Test
    fun `fuite sous le seuil et monstre neutralise`() {
        val pjs = listOf(pj("Aldric"))
        val lache = monstre("Kobold", ProfilIA.LACHE, pv = 9)
        assertTrue(IaMonstre.decider(lache, pjs).libelle.startsWith("Fuir"))
        val brute = monstre("Ogre", ProfilIA.BRUTE, pv = 1)
        assertEquals("Morsure", IaMonstre.decider(brute, pjs).attaque?.nom)
        val etourdi = brute.copy(conditions = setOf(ConditionCombat.ETOURDI))
        assertEquals("Aucune action", IaMonstre.decider(etourdi, pjs).libelle)
    }

    @Test
    fun `capacite de zone preferee contre plusieurs cibles puis indisponible`() {
        val dragonnet = monstre("Dragonnet", ProfilIA.BRUTE, attaques = listOf(morsure, souffle))
        val pjs = listOf(pj("A"), pj("B"), pj("C"))
        assertEquals("Aspersion acide", IaMonstre.decider(dragonnet, pjs).attaque?.nom)
        val utilise = dragonnet.copy(capacitesUtilisees = mapOf("Aspersion acide" to 1))
        assertEquals("Morsure", IaMonstre.decider(utilise, pjs).attaque?.nom)
    }

    @Test
    fun `attaque contre la CA`() {
        val r = lancerAttaqueMonstre(morsure, 20, caCible = 30, aleatoire = Random(1))
        // Seul un 20 naturel touche une CA de 30.
        assertTrue(r.all { it.touche == (it.d20 == 20) })
        assertTrue(r.filter { !it.touche }.all { it.degats == 0 })
    }

    @Test
    fun `round complet declaration puis resolution puis nouveau round`() {
        CombatSession.aleatoire = Random(3)
        CombatSession.demarrer("Test", listOf(pj("Aldric"), pj("Brune"), monstre("Loup", ProfilIA.BRUTE)))
        CombatSession.commencer()
        var etat = CombatSession.etat.value!!
        assertEquals(PhaseCombat.DECLARATION, etat.phase)
        // Le monstre a déjà décidé, les joueurs non.
        assertNotNull(etat.declarations["Loup"])
        assertEquals(setOf("Aldric", "Brune"), etat.enAttente.map { it.id }.toSet())
        assertNull(etat.actif)

        CombatSession.declarer(DeclarationAction("Aldric", "Attaquer", actionId = "attaquer", cibleId = "Loup", cibleNom = "Loup"))
        assertEquals(PhaseCombat.DECLARATION, CombatSession.etat.value!!.phase)
        CombatSession.declarer(DeclarationAction("Brune", "Esquiver", actionId = "dodge"))
        etat = CombatSession.etat.value!!
        assertEquals(PhaseCombat.RESOLUTION, etat.phase)
        assertEquals(etat.combattants.first().id, etat.actif?.id)

        repeat(etat.combattants.size) { CombatSession.tourSuivant() }
        etat = CombatSession.etat.value!!
        assertEquals(2, etat.round)
        assertEquals(PhaseCombat.DECLARATION, etat.phase)
        assertEquals(setOf("Loup"), etat.declarations.keys)
    }

    @Test
    fun `le joueur ne voit que sa propre declaration`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), pj("Brune"), monstre("Loup", ProfilIA.BRUTE)))
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aldric", "Attaquer", cibleNom = "Loup"))
        val vueBrune = CombatSession.etat.value!!.versJoueur("c-Brune")
        assertNull(vueBrune.maDeclaration)
        assertEquals("Brune", vueBrune.monCombattantId)
        assertEquals(2, vueBrune.nbDeclares)
        val vueAldric = CombatSession.etat.value!!.versJoueur("c-Aldric")
        assertEquals("Attaquer", vueAldric.maDeclaration?.libelle)
        // Les PV exacts des monstres ne sont pas transmis.
        assertNull(vueAldric.ordre.first { it.estMonstre }.pv)
    }

    private val arc = AttaqueMonstre("Arc court", TypeAttaqueMonstre.DISTANCE, bonusToucher = 4, formuleDegats = "1d6 + 2", degatsMoyens = 5, porteeLongue = 96.0)
    private val cimeterre = AttaqueMonstre("Cimeterre", TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = 4, formuleDegats = "1d6 + 2", degatsMoyens = 5)

    @Test
    fun `portee lue et atteinte selon la distance`() {
        val profil = ProfilCombatMonstre.depuisFiche(MonsterParser.parse(chefGobelin).single().rawMarkdown)
        val arcLu = profil.actions.attaques.first { it.nom == "Arc court" }
        assertEquals(96.0, arcLu.porteeLongue!!, 0.01)
        assertTrue(arcLu.atteint(Distance.LONGUE))
        assertTrue(!cimeterre.atteint(Distance.COURTE))
        val dague = AttaqueMonstre("Dague", TypeAttaqueMonstre.POLYVALENTE, bonusToucher = 4, degatsMoyens = 4, porteeLongue = 18.0)
        assertTrue(dague.atteint(Distance.COURTE))
        assertTrue(!dague.atteint(Distance.LONGUE))
    }

    @Test
    fun `la brute fonce au contact d'une cible a courte distance`() {
        val d = IaMonstre.decider(monstre("Orque", ProfilIA.BRUTE, listOf(cimeterre)), listOf(pj("Aldric")), mapOf("Aldric" to Distance.COURTE))
        assertEquals("Cimeterre", d.attaque?.nom)
        assertEquals(mapOf("Aldric" to Distance.CONTACT), d.deplacements)
    }

    @Test
    fun `sans attaque a distance une cible lointaine oblige a s'elancer`() {
        val d = IaMonstre.decider(monstre("Orque", ProfilIA.BRUTE, listOf(cimeterre)), listOf(pj("Aldric")), mapOf("Aldric" to Distance.LONGUE))
        assertNull(d.attaque)
        assertTrue(d.libelle.startsWith("Se précipiter"))
        assertEquals(mapOf("Aldric" to Distance.COURTE), d.deplacements)
    }

    @Test
    fun `la brute garde l'adversaire qu'elle a au contact`() {
        val pjs = listOf(pj("Aldric"), pj("Brune"))
        repeat(10) {
            val d = IaMonstre.decider(monstre("Orque", ProfilIA.BRUTE, listOf(cimeterre)), pjs,
                mapOf("Aldric" to Distance.LONGUE, "Brune" to Distance.CONTACT), Random(it))
            assertEquals("Brune", d.cibleNom)
            assertTrue(d.deplacements.isEmpty())
        }
    }

    @Test
    fun `le tireur au contact recule puis tire`() {
        val d = IaMonstre.decider(monstre("Archer", ProfilIA.TIREUR, listOf(cimeterre, arc)), listOf(pj("Aldric")), mapOf("Aldric" to Distance.CONTACT))
        assertEquals("Arc court", d.attaque?.nom)
        assertEquals(mapOf("Aldric" to Distance.COURTE), d.deplacements)
        assertTrue(!d.desengage)
    }

    @Test
    fun `fuite au contact avec desengagement puis au loin`() {
        val lache = monstre("Kobold", ProfilIA.LACHE, pv = 5)
        val auContact = IaMonstre.decider(lache, listOf(pj("Aldric")), mapOf("Aldric" to Distance.CONTACT))
        assertTrue(auContact.desengage)
        assertEquals(mapOf("Aldric" to Distance.COURTE), auContact.deplacements)
        val aCourte = IaMonstre.decider(lache, listOf(pj("Aldric")), mapOf("Aldric" to Distance.COURTE))
        assertEquals(mapOf("Aldric" to Distance.LONGUE), aCourte.deplacements)
    }

    @Test
    fun `deplacement applique a la resolution avec attaque d'opportunite`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), monstre("Archer", ProfilIA.TIREUR, listOf(cimeterre, arc), init = 30)))
        CombatSession.definirDistance("Archer", "Aldric", Distance.CONTACT)
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aldric", "Attaquer"))
        var etat = CombatSession.etat.value!!
        assertEquals("Archer", etat.actif?.id)
        assertEquals(Distance.CONTACT, etat.distance("Archer", "Aldric"))
        CombatSession.tourSuivant()
        etat = CombatSession.etat.value!!
        assertEquals(Distance.COURTE, etat.distance("Archer", "Aldric"))
        assertTrue(etat.journal.any { it.startsWith("⚠ Attaque d'opportunité : Aldric") })
    }

    @Test
    fun `changer une distance pendant la declaration fait redecider l'IA`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), monstre("Orque", ProfilIA.BRUTE, listOf(cimeterre))))
        CombatSession.commencer()
        assertEquals(mapOf("Aldric" to Distance.CONTACT), CombatSession.etat.value!!.declarations["Orque"]?.deplacements)
        CombatSession.definirDistance("Orque", "Aldric", Distance.CONTACT)
        assertTrue(CombatSession.etat.value!!.declarations["Orque"]!!.deplacements.isEmpty())
    }

    @Test
    fun `action impossible annule le deplacement`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), monstre("Orque", ProfilIA.BRUTE, listOf(cimeterre), init = 30)))
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aldric", "Esquiver"))
        CombatSession.annulerAction("Orque", "coincé derrière le chariot")
        CombatSession.tourSuivant()
        assertEquals(Distance.COURTE, CombatSession.etat.value!!.distance("Orque", "Aldric"))
    }

    @Test
    fun `monstre au contact de plusieurs joueurs`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), pj("Brune"), pj("Cedric"), monstre("Ogre", ProfilIA.BRUTE, listOf(cimeterre))))
        CombatSession.definirDistance("Ogre", "Aldric", Distance.CONTACT)
        CombatSession.definirDistance("Ogre", "Brune", Distance.CONTACT)
        val etat = CombatSession.etat.value!!
        assertEquals(
            mapOf("Aldric" to Distance.CONTACT, "Brune" to Distance.CONTACT, "Cedric" to Distance.COURTE),
            etat.distancesDe("Ogre")
        )
    }

    @Test
    fun `regle situationnelle bascule le profil`() {
        val dragon = monstre("Dragon", ProfilIA.TACTICIEN, listOf(cimeterre, arc), pv = 8).copy(
            reglesIA = listOf(
                RegleProfilIA(DeclencheurIA.ENCERCLE, ProfilIA.ARTILLEUR, 2),
                RegleProfilIA(DeclencheurIA.PV_SOUS, ProfilIA.BERSERKER, 50),
            )
        )
        val pjs = listOf(pj("A"), pj("B"))
        // Encerclé (2 au contact) : la première règle l'emporte → se dégage.
        val encercle = IaMonstre.decider(dragon, pjs, mapOf("A" to Distance.CONTACT, "B" to Distance.CONTACT))
        assertEquals(ProfilIA.ARTILLEUR, encercle.profil)
        assertTrue(encercle.desengage)
        // Plus encerclé mais à 8/20 PV : berserker, ne fuit pas malgré ses PV.
        val blesse = IaMonstre.decider(dragon, pjs, mapOf("A" to Distance.CONTACT))
        assertEquals(ProfilIA.BERSERKER, blesse.profil)
        assertEquals("Cimeterre", blesse.attaque?.nom)
        assertTrue(blesse.raison.startsWith("Situation"))
        // Sans situation particulière : profil de base.
        assertEquals(ProfilIA.TACTICIEN, IaMonstre.decider(dragon.copy(pv = 20), pjs, mapOf("A" to Distance.CONTACT)).profil)
    }

    @Test
    fun `le gardien ne quitte pas son poste`() {
        val garde = monstre("Garde", ProfilIA.GARDIEN, listOf(cimeterre))
        val d = IaMonstre.decider(garde, listOf(pj("A")), mapOf("A" to Distance.COURTE))
        assertNull(d.attaque)
        assertTrue(d.deplacements.isEmpty())
    }

    @Test
    fun `l'escarmoucheur frappe puis recule`() {
        val d = IaMonstre.decider(monstre("Loup", ProfilIA.ESCARMOUCHEUR, listOf(cimeterre)), listOf(pj("A")), mapOf("A" to Distance.CONTACT))
        assertEquals("Cimeterre", d.attaque?.nom)
        assertEquals(mapOf("A" to Distance.COURTE), d.deplacements)
    }

    @Test
    fun `le protecteur vise celui qui menace l'allie blesse`() {
        val pnj = pj("Garde du roi").copy(profilIA = ProfilIA.PROTECTEUR, attaques = listOf(cimeterre))
        val roi = pj("Roi", pv = 3)
        val menace = monstre("Assassin", ProfilIA.BRUTE)
        val autre = monstre("Gobelin", ProfilIA.BRUTE)
        val distances = mapOf(cleDistance("Roi", "Assassin") to Distance.CONTACT)
        val d = IaMonstre.decider(
            pnj, listOf(autre, menace), allies = listOf(roi),
            distanceEntre = { a, b -> distances[cleDistance(a, b)] ?: Distance.COURTE }
        )
        assertEquals("Assassin", d.cibleNom)
    }

    @Test
    fun `un PNJ allie avec un profil declare seul contre les monstres`() {
        val pnj = pj("Garde").copy(profilIA = ProfilIA.BRUTE, attaques = listOf(cimeterre))
        CombatSession.demarrer("Test", listOf(pj("Aldric"), pnj, monstre("Loup", ProfilIA.BRUTE)))
        CombatSession.commencer()
        val etat = CombatSession.etat.value!!
        assertEquals("Loup", etat.declarations["Garde"]?.cibleNom)
        assertTrue(etat.declarations["Garde"]!!.parIa)
        assertEquals(setOf("Aldric"), etat.enAttente.map { it.id }.toSet())
        // Rendu au MJ : sa décision disparaît, il est de nouveau attendu.
        CombatSession.changerProfilIA("Garde", null)
        assertEquals(setOf("Aldric", "Garde"), CombatSession.etat.value!!.enAttente.map { it.id }.toSet())
    }

    @Test
    fun `PNJ ennemi cache aux joueurs`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), pj("Traitre").copy(estMonstre = true, attaques = listOf(cimeterre), profilIA = ProfilIA.DUELLISTE)))
        CombatSession.commencer()
        val vue = CombatSession.etat.value!!.versJoueur("c-Aldric")
        val traitre = vue.ordre.first { it.id == "Traitre" }
        assertNull(traitre.pv)
        assertEquals("Aldric", CombatSession.etat.value!!.declarations["Traitre"]?.cibleNom)
    }

    @Test
    fun `attaques tirees des armes d'un PNJ`() {
        val armes = listOf(
            ArmeSrd("Épée longue", "1d8 tranchants", "Polyvalente (1d10)", false),
            ArmeSrd("Arc long", "1d8 perforants", "Deux mains, Lourde, Munitions (45/180 ; flèches)", true),
            ArmeSrd("Dague", "1d4 perforants", "Finesse, Lancer (6/18), Légère", false),
        )
        val a = ArmesPersonnage.attaques(listOf("Épée longue", "Arc long", "Dague"), armes, force = 16, dexterite = 14, maitrise = 2)
        val epee = a.first { it.nom == "Épée longue" }
        assertEquals(5, epee.bonusToucher)
        assertEquals("1d8 + 3", epee.formuleDegats)
        assertEquals(TypeAttaqueMonstre.CORPS_A_CORPS, epee.type)
        val arcLong = a.first { it.nom == "Arc long" }
        assertEquals(4, arcLong.bonusToucher)
        assertEquals(180.0, arcLong.porteeLongue!!, 0.01)
        val dague = a.first { it.nom == "Dague" }
        assertEquals(TypeAttaqueMonstre.POLYVALENTE, dague.type)
        assertEquals(5, dague.bonusToucher)
        assertEquals("Mains nues", ArmesPersonnage.attaques(emptyList(), armes, 10, 10, 2).single().nom)
    }

    private fun comportement(md: String) =
        ProfilCombatMonstre.depuisFiche(MonsterParser.parse(md.trimIndent()).single().rawMarkdown).comportement

    @Test
    fun `profil deduit de toute la fiche`() {
        val strige = comportement(
            """
            ### Strige
            Catégorie: Squelettes
            Type: Monstruosité
            Taille: TP
            CA: 13 Initiative +3 (13)
            Pv: 5 (2d4)
            Vitesse: 3 m, vol 12 m
            Caractéristiques: For 4 −3 −3 Dex 16 +3 +3 Con 11 +0 +0 / Int 2 −4 −4 Sag 8 −1 −1 Cha 6 −2 −2

            ## Actions
            Trompe. Corps à corps : +5, allonge 1,50 m. Touché : 6 (1d6 + 3) dégâts perforants.
            """
        )
        assertEquals(ProfilIA.PREDATEUR, strige.profil)
        assertTrue(strige.raison.contains("volante"))
        val loup = comportement(
            """
            ### Loup
            Type: Bête
            Taille: M
            CA: 12 Initiative +2 (12)
            Pv: 11 (2d8 + 2)
            Caractéristiques: For 14 +2 +2 Dex 15 +2 +2 Con 12 +1 +1 / Int 3 −4 −4 Sag 12 +1 +1 Cha 6 −2 −2

            ## Traits
            Tactique de meute. Quand au moins un de ses alliés est proche, le loup a l’Avantage.

            ## Actions
            Morsure. Corps à corps : +4, allonge 1,50 m. Touché : 5 (1d6 + 2) dégâts perforants.
            """
        )
        assertEquals(ProfilIA.PREDATEUR, loup.profil)
        assertEquals(ProfilIA.ESCARMOUCHEUR, comportement(chefGobelin.replace("### Chef gobelin", "### Combattant gobelin").replace(
            "Attaques multiples. Le gobelin effectue deux attaques, réparties à sa guise entre Cimeterre et Arc court.\n", ""
        )).profil)
    }

    @Test
    fun `attaque de monstre lancee d'avance et appliquee en un geste`() {
        CombatSession.aleatoire = Random(7)
        CombatSession.demarrer("Test", listOf(pj("Aldric", ca = 1), monstre("Orque", ProfilIA.BRUTE, listOf(cimeterre), init = 30)))
        CombatSession.definirDistance("Orque", "Aldric", Distance.CONTACT)
        CombatSession.commencer()
        val decl = CombatSession.etat.value!!.declarations["Orque"]!!
        val resultats = decl.resultats!!
        assertEquals(1, resultats.size)
        CombatSession.declarer(DeclarationAction("Aldric", "Esquiver"))
        val total = resultats.sumOf { it.degats }
        CombatSession.appliquerJets("Orque")
        CombatSession.appliquerJets("Orque") // une seule fois
        val etat = CombatSession.etat.value!!
        assertEquals(20 - total, etat.combattants.first { it.id == "Aldric" }.pv)
        assertTrue(etat.declarations["Orque"]!!.degatsAppliques)
    }

    @Test
    fun `un seul jet par type et par round sauf jet accorde, degats appliques par le MJ`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), monstre("Loup", ProfilIA.BRUTE)))
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aldric", "Attaquer", cibleId = "Loup", cibleNom = "Loup"))
        fun jet(libelle: String, total: Int) = JetCombat(combattantId = "Aldric", round = 0, libelle = libelle, formule = "1d8", des = listOf(total), bonus = 0, total = total, manuel = true)
        CombatSession.enregistrerJet(jet("Dégâts", 6))
        CombatSession.enregistrerJet(jet("Dégâts", 8)) // refusé
        assertEquals(1, CombatSession.etat.value!!.jetsDuRound.size)
        assertEquals(listOf("Dégâts"), CombatSession.etat.value!!.versJoueur("c-Aldric").typesLances)
        CombatSession.autoriserRelance("Aldric")
        assertEquals(1, CombatSession.etat.value!!.versJoueur("c-Aldric").relances)
        CombatSession.enregistrerJet(jet("Dégâts", 8)) // accepté grâce au jet accordé
        CombatSession.enregistrerJet(jet("Dégâts", 3)) // refusé à nouveau
        val jets = CombatSession.etat.value!!.jetsDuRound
        assertEquals(listOf(6, 8), jets.map { it.total })
        CombatSession.appliquerJet(jets.first().id, "Loup", soin = false)
        CombatSession.appliquerJet(jets.first().id, "Loup", soin = false) // une seule fois
        assertEquals(14, CombatSession.etat.value!!.combattants.first { it.id == "Loup" }.pv)
    }

    @Test
    fun `un monstre vaincu pendant la declaration n'est plus attendu`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric"), monstre("Loup", ProfilIA.BRUTE)))
        CombatSession.commencer()
        CombatSession.appliquerPv("Loup", 50)
        CombatSession.declarer(DeclarationAction("Aldric", "Se précipiter"))
        assertEquals(PhaseCombat.RESOLUTION, CombatSession.etat.value!!.phase)
        assertEquals("Aldric", CombatSession.etat.value!!.actif?.id)
    }
}
