package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueAnnoncee
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossDefinition
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.EffetDefense
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneCompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.PhaseBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.BossPrepare
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.JetCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.ResultatTable
import com.jc2.jdrcompagnon.feature_combat.presentation.cleDistance
import com.jc2.jdrcompagnon.network.versJoueur
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class BossCombatTest {

    private val souffle = AttaqueMonstre(
        nom = "Souffle", type = TypeAttaqueMonstre.SAUVEGARDE, formuleDegats = "4d6", degatsMoyens = 14,
        sauvegarde = "Constitution", dd = 13, recharge = 5, zone = true,
    )
    private val griffe = AttaqueMonstre("Griffe", TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = 5, formuleDegats = "1d6 + 2", degatsMoyens = 5)

    private val def = BossDefinition(
        id = "test", nom = "Wyrm", titre = "Le test", monstreBase = "Wyrm", presentation = "Il arrive.", defaite = "Il tombe.",
        resistancesLegendaires = 2,
        actionsLegendaires = listOf(
            ActionBoss("Coup de queue", "Fouette.", FormeActionBoss.ATTAQUE, bonusToucher = 30, degats = "1d4", typeDegats = "contondants", portee = Distance.M4_5.metres),
        ),
        phases = listOf(
            PhaseBoss("Calme", 100, "a", profil = ProfilIA.TACTICIEN),
            PhaseBoss("Colère", 66, "b", renforts = listOf(LigneCompositionCombat("Loup", 1))),
            PhaseBoss("Fureur", 33, "c", profil = ProfilIA.BERSERKER, modificateurCa = -2, bonusPointsLegendaires = 1),
        ),
        attaqueAnnoncee = AttaqueAnnoncee("Souffle", "Il inspire…", "Interrompu !"),
        seuilDesequilibrePct = 20,
    )

    private fun pj(nom: String, init: Int = 20) =
        Combattant(id = nom, nom = nom, estMonstre = false, ca = 14, pvMax = 30, pv = 30, bonusInitiative = 0, initiative = init)

    private fun boss(attaques: List<AttaqueMonstre> = listOf(griffe), init: Int = 5) =
        Combattant(
            id = "boss", nom = "Wyrm", estMonstre = true, monstreNom = "Wyrm", ca = 17, pvMax = 100, pv = 100, bonusInitiative = 0,
            initiative = init, attaques = attaques, profilIA = ProfilIA.TACTICIEN, bossId = "test",
        )

    private fun pilier() = Combattant(id = "pilier", nom = "Pilier", estMonstre = true, ca = 10, pvMax = 10, pv = 10, bonusInitiative = 0, initiative = 0, inerte = true)

    private fun prepare(b: Combattant = boss(), pointsFaibles: List<Combattant> = emptyList()) = BossPrepare(
        b.id, listOf(b) + pointsFaibles,
        EtatBoss(
            definition = def, caBase = b.ca, pointsMaxBase = 3, pointsFaiblesIds = pointsFaibles.map { it.id },
            renforts = mapOf(1 to listOf(Combattant(id = "loup", nom = "Loup (renfort)", estMonstre = true, ca = 13, pvMax = 11, pv = 11, bonusInitiative = 2, attaques = listOf(griffe)))),
        ),
    )

    @After
    fun fin() {
        CombatSession.fermer()
        CombatSession.aleatoire = Random
    }

    @Test
    fun `phases declenchees par les PV`() {
        CombatSession.demarrer("Boss", listOf(pj("Aria")), listOf(prepare()))
        CombatSession.commencer()
        CombatSession.appliquerPv("boss", 40)
        var etat = CombatSession.etat.value!!
        assertEquals(1, etat.boss.getValue("boss").phase)
        assertTrue("renfort de la phase 2", etat.combattants.any { it.id == "loup" })
        CombatSession.appliquerPv("boss", 30)
        etat = CombatSession.etat.value!!
        val b = etat.boss.getValue("boss")
        assertEquals(2, b.phase)
        assertEquals(15, etat.combattants.first { it.id == "boss" }.ca)
        assertEquals(ProfilIA.BERSERKER, etat.combattants.first { it.id == "boss" }.profilIA)
        assertEquals(4, b.pointsMax)
        assertTrue(b.annonces.any { it.texte.contains("Fureur") })
    }

    @Test
    fun `les points faibles n'empechent pas la victoire et ne jouent pas`() {
        CombatSession.demarrer("Boss", listOf(pj("Aria")), listOf(prepare(pointsFaibles = listOf(pilier()))))
        CombatSession.commencer()
        val etat = CombatSession.etat.value!!
        assertNull("un point faible ne joue pas", etat.declarations["pilier"])
        assertFalse(etat.actif?.id == "pilier")
        CombatSession.appliquerPv("boss", 200)
        val fin = CombatSession.etat.value!!
        assertTrue(fin.victoire)
        assertTrue(fin.termine)
        assertTrue(fin.boss.getValue("boss").vaincu)
    }

    @Test
    fun `resistance legendaire transforme un JS rate couteux en reussite`() {
        CombatSession.aleatoire = Random(1)
        CombatSession.demarrer("Boss", listOf(pj("Aria")), listOf(prepare()))
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aria", "Boule de feu", detail = "JS Dextérité DD 40 (moitié si réussi) · 8d6 feu"))
        val jet = JetCombat(combattantId = "Aria", round = 1, libelle = "Dégâts", formule = "8d6", des = listOf(6), bonus = 0, total = 30, manuel = true)
        CombatSession.enregistrerJet(jet)
        CombatSession.appliquerJetZone(jet.id, listOf("boss"))
        val etat = CombatSession.etat.value!!
        val b = etat.boss.getValue("boss")
        assertEquals(1, b.resistances)
        assertEquals("moitié des dégâts grâce à la résistance", 85, etat.combattants.first { it.id == "boss" }.pv)
    }

    @Test
    fun `attaque annoncee puis interrompue par assez de degats avant son tour`() {
        CombatSession.aleatoire = Random(3)
        CombatSession.demarrer("Boss", listOf(pj("Aria")), listOf(prepare(boss(attaques = listOf(griffe, souffle)))))
        CombatSession.commencer()
        var etat = CombatSession.etat.value!!
        assertEquals("Souffle", etat.declarations["boss"]?.attaque?.nom)
        assertTrue(etat.boss.getValue("boss").prepare)
        assertTrue(etat.versJoueur(null).boss.single().prepare)
        // 20 % de 100 PV avant son tour : le souffle avorte.
        CombatSession.appliquerPv("boss", 20)
        etat = CombatSession.etat.value!!
        val b = etat.boss.getValue("boss")
        assertFalse(b.prepare)
        assertTrue(b.desequilibre)
        assertEquals(1, b.interruptions)
        assertTrue(etat.declarations["boss"]!!.libelle.contains("interrompu"))
    }

    @Test
    fun `action legendaire apres le tour d'un adversaire`() {
        CombatSession.aleatoire = Random(7)
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20)), listOf(prepare(boss(init = 5))))
        CombatSession.definirDistance("boss", "Aria", Distance.CONTACT)
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aria", "Esquiver"))
        // Entrée en scène lue : sinon le tour suivant reste bloqué.
        CombatSession.marquerAnnoncesLues("boss")
        var etat = CombatSession.etat.value!!
        assertEquals(PhaseCombat.EN_COURS, etat.phase)
        assertEquals("Aria", etat.actif?.id)
        CombatSession.tourSuivant()
        etat = CombatSession.etat.value!!
        // Son tour commence aussitôt : ses points sont déjà rechargés.
        assertEquals("boss", etat.actif?.id)
        assertEquals(3, etat.boss.getValue("boss").points)
        assertNotNull(etat.journal.firstOrNull { it.contains("⚡") && it.contains("Coup de queue") })
        assertTrue("+30 au toucher : touché", etat.combattants.first { it.id == "Aria" }.pv < 30)
    }

    @Test
    fun `controle attenue au debut de son tour`() {
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20)), listOf(prepare(boss(init = 5))))
        CombatSession.commencer()
        CombatSession.appliquerCondition("boss", ConditionCombat.ETOURDI)
        CombatSession.declarer(DeclarationAction("Aria", "Esquiver"))
        CombatSession.marquerAnnoncesLues("boss")
        CombatSession.tourSuivant()
        val etat = CombatSession.etat.value!!
        assertEquals("boss", etat.actif?.id)
        assertFalse(ConditionCombat.ETOURDI in etat.combattants.first { it.id == "boss" }.conditions)
        assertEquals(2, etat.boss.getValue("boss").points)
    }

    @Test
    fun `un boss s'enregistre en JSON et se relit a l'identique`() {
        val json = Json { ignoreUnknownKeys = true }
        val texte = json.encodeToString(listOf(BossCatalogue.CRYOVAIN))
        assertEquals(BossCatalogue.CRYOVAIN, json.decodeFromString<List<BossDefinition>>(texte).single())
    }

    @Test
    fun `boss integre modifie puis reinitialise`() {
        BossCatalogue.enregistrer(BossCatalogue.CRYOVAIN.copy(titre = "Autre titre"))
        assertEquals("Autre titre", BossCatalogue.parNom("cryovain")?.titre)
        assertTrue(BossCatalogue.estModifie("cryovain"))
        BossCatalogue.supprimerOuReinitialiser("cryovain")
        assertEquals(BossCatalogue.CRYOVAIN, BossCatalogue.parNom("Cryovain"))
        val cree = BossCatalogue.CRYOVAIN.copy(id = "autre", nom = "Givrecroc")
        BossCatalogue.enregistrer(cree)
        assertEquals(cree, BossCatalogue.parNom("givrecroc"))
        BossCatalogue.supprimerOuReinitialiser("autre")
        assertEquals(null, BossCatalogue.parNom("givrecroc"))
    }

    @Test
    fun `ajustement au nombre de personnages`() {
        assertEquals(123, BossCatalogue.pvAjustes(BossCatalogue.CRYOVAIN, 123, 4))
        assertEquals(92, BossCatalogue.pvAjustes(BossCatalogue.CRYOVAIN, 123, 3))
        assertEquals(2, BossCatalogue.pointsAjustes(BossCatalogue.CRYOVAIN, 2))
        assertEquals(4, BossCatalogue.pointsAjustes(BossCatalogue.CRYOVAIN, 6))
        assertEquals(0, BossCatalogue.phasePour(BossCatalogue.CRYOVAIN, 100))
        assertEquals(1, BossCatalogue.phasePour(BossCatalogue.CRYOVAIN, 50))
        assertEquals(2, BossCatalogue.phasePour(BossCatalogue.CRYOVAIN, 10))
    }

    @Test
    fun `la difficulte change les PV, actions et resistances du boss`() {
        val def = BossCatalogue.CRYOVAIN
        val normal = BossCatalogue.pvAjustes(def, 123, 4)
        assertTrue(BossCatalogue.pvAjustes(def, 123, 4, EncounterDifficulty.FACILE) < normal)
        assertTrue(BossCatalogue.pvAjustes(def, 123, 4, EncounterDifficulty.MORTELLE) > normal)
        assertEquals(BossCatalogue.pointsAjustes(def, 4) + 1, BossCatalogue.pointsAjustes(def, 4, EncounterDifficulty.MORTELLE))
        assertEquals(BossCatalogue.pointsAjustes(def, 4) - 1, BossCatalogue.pointsAjustes(def, 4, EncounterDifficulty.FACILE))
        assertEquals(def.resistancesLegendaires + 1, BossCatalogue.resistancesAjustees(def, EncounterDifficulty.MORTELLE))
    }

    @Test
    fun `la mise en scene du boss reste au MJ, la table ne voit que les faits`() {
        CombatSession.demarrer("Boss", listOf(pj("Aria")), listOf(prepare()))
        CombatSession.commencer()
        CombatSession.appliquerPv("boss", 60)
        val etat = CombatSession.etat.value!!
        val b = etat.boss.getValue("boss")
        assertTrue("textes à lire pour le MJ", b.annonces.any { it.aLire && !it.lue })
        val vueJoueur = etat.versJoueur("c-Aria")
        assertTrue(vueJoueur.boss.single().annonces.isEmpty())
        assertTrue(vueJoueur.recit.none { it.texte.contains("Fureur") })
        assertTrue(vueJoueur.recit.toString(), vueJoueur.recit.any { it.texte.contains("phase") })
        CombatSession.marquerAnnoncesLues("boss")
        assertTrue(CombatSession.etat.value!!.boss.getValue("boss").annonces.all { it.lue })
    }

    @Test
    fun `annonce a lire non lue, le tour suivant attend`() {
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20)), listOf(prepare(boss(init = 5))))
        CombatSession.commencer()
        CombatSession.declarer(DeclarationAction("Aria", "Esquiver"))
        assertNotNull(CombatSession.blocageTourSuivant(CombatSession.etat.value!!))
        CombatSession.tourSuivant()
        assertEquals("Aria", CombatSession.etat.value!!.actif?.id)
        CombatSession.marquerAnnoncesLues("boss")
        CombatSession.tourSuivant()
        assertEquals("boss", CombatSession.etat.value!!.actif?.id)
    }

    @Test
    fun `jet contre la mort note par le MJ`() {
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20).copy(pv = 0)), listOf(prepare(boss(init = 5))))
        CombatSession.commencer()
        CombatSession.marquerAnnoncesLues("boss")
        var etat = CombatSession.etat.value!!
        assertEquals("Aria", etat.actif?.id)
        assertTrue("le jet attend le MJ", CombatSession.jetContreLaMortEnAttente(etat, etat.actif!!))
        assertNotNull(CombatSession.blocageTourSuivant(etat))
        CombatSession.noterJetContreLaMort("Aria", 15)
        etat = CombatSession.etat.value!!
        assertEquals(1, etat.combattants.first { it.id == "Aria" }.succesMort)
        assertNull(CombatSession.blocageTourSuivant(etat))
    }

    @Test
    fun `le MJ peut ne pas appliquer une action legendaire`() {
        CombatSession.aleatoire = Random(7)
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20)), listOf(prepare(boss(init = 5))))
        CombatSession.definirDistance("boss", "Aria", Distance.CONTACT)
        CombatSession.commencer()
        CombatSession.marquerAnnoncesLues("boss")
        CombatSession.declarer(DeclarationAction("Aria", "Esquiver"))
        CombatSession.tourSuivant()
        var etat = CombatSession.etat.value!!
        assertTrue(etat.combattants.first { it.id == "Aria" }.pv < 30)
        val index = etat.boss.getValue("boss").annonces.indexOfLast { it.avant.isNotEmpty() }
        assertTrue("action legendaire a lire avec son resultat", index >= 0)
        CombatSession.annulerAnnonce("boss", index)
        etat = CombatSession.etat.value!!
        assertEquals(30, etat.combattants.first { it.id == "Aria" }.pv)
        assertTrue(etat.boss.getValue("boss").annonces[index].annulee)
    }

    @Test
    fun `le MJ fixe les degats d une action legendaire`() {
        CombatSession.aleatoire = Random(7)
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20)), listOf(prepare(boss(init = 5))))
        CombatSession.definirDistance("boss", "Aria", Distance.CONTACT)
        CombatSession.commencer()
        CombatSession.marquerAnnoncesLues("boss")
        CombatSession.declarer(DeclarationAction("Aria", "Esquiver"))
        CombatSession.tourSuivant()
        val index = CombatSession.etat.value!!.boss.getValue("boss").annonces.indexOfLast { it.avant.isNotEmpty() }
        CombatSession.ajusterDegatsAnnonce("boss", index, mapOf("Aria" to 2))
        assertEquals(28, CombatSession.etat.value!!.combattants.first { it.id == "Aria" }.pv)
        CombatSession.ajusterDegatsAnnonce("boss", index, mapOf("Aria" to 0))
        assertEquals(30, CombatSession.etat.value!!.combattants.first { it.id == "Aria" }.pv)
    }

    @Test
    fun `un rate annonce a la table sans blessure reveille le repaire`() {
        val repaire = ActionBoss("Crevasse", "La glace craque.", FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Force", dd = 40, degats = "1d4", typeDegats = "froid")
        val avecRepaire = def.copy(actionsRepaire = listOf(repaire), phases = def.phases.map { it.copy(repaireActif = true) })
        val b = boss(init = 5)
        CombatSession.demarrer("Boss", listOf(pj("Aria", init = 20)), listOf(BossPrepare(b.id, listOf(b), EtatBoss(definition = avecRepaire, caBase = b.ca, pointsMaxBase = 3))))
        CombatSession.commencer()
        CombatSession.marquerAnnoncesLues("boss")
        CombatSession.resultatTable("Aria", "boss", ResultatTable.RATE)
        val etat = CombatSession.etat.value!!
        assertTrue("annonce du repaire", etat.boss.getValue("boss").annonces.any { it.texte.contains("Crevasse") && !it.lue })
        assertTrue("une fois par round", "Aria" in etat.boss.getValue("boss").repaireRatesDuRound)
    }
}
