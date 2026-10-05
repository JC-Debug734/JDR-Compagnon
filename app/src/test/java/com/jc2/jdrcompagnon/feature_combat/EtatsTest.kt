package com.jc2.jdrcompagnon.feature_combat

import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsMonstreParser
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.EffetEtat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.IaMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ModeJet
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.LivreEtats
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterParser
import java.io.File
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EtatsTest {

    private val morsure = AttaqueMonstre("Morsure", TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = 5, formuleDegats = "1d10 + 3", degatsMoyens = 8)

    private fun pj(nom: String, conditions: Set<ConditionCombat> = emptySet()) =
        Combattant(id = nom, nom = nom, estMonstre = false, characterId = "c-$nom", ca = 14, pvMax = 20, pv = 20, bonusInitiative = 0, initiative = 10, conditions = conditions)

    private fun monstre(nom: String, conditions: Set<ConditionCombat> = emptySet(), attaques: List<AttaqueMonstre> = listOf(morsure)) =
        Combattant(id = nom, nom = nom, estMonstre = true, monstreNom = nom, ca = 12, pvMax = 20, pv = 20, bonusInitiative = 0,
            initiative = 15, attaques = attaques, profilIA = ProfilIA.BRUTE, conditions = conditions)

    @After
    fun fin() {
        CombatSession.fermer()
        CombatSession.aleatoire = Random
    }

    @Test
    fun `tous les etats des regles sont au catalogue`() {
        val officiels = Etats.fiches.filter { it.officiel }.map { it.condition.label }.toSet()
        assertEquals(
            setOf("Aveuglé", "Charmé", "Assourdi", "Effrayé", "Agrippé", "Neutralisé", "Invisible", "Paralysé",
                "Pétrifié", "Empoisonné", "À terre", "Entravé", "Étourdi", "Inconscient"),
            officiels
        )
        // L'épuisement (à niveaux) est suivi par le compteur de fatigue, présent dans le livre.
        assertTrue(Etats.reglesEpuisement.isNotEmpty())
    }

    @Test
    fun `paralyse ne peut ni agir ni bouger ni parler`() {
        val paralyse = setOf(ConditionCombat.PARALYSE)
        assertFalse(Etats.peutAgir(paralyse))
        assertEquals(ConditionCombat.PARALYSE, Etats.immobilisant(paralyse))
        assertTrue(EffetEtat.MUET in Etats.effets(paralyse))
        assertTrue(Etats.echecAutomatiqueJs(paralyse, "Dextérité"))
        assertFalse(Etats.echecAutomatiqueJs(paralyse, "Sagesse"))
        assertEquals(ConditionCombat.PARALYSE, Etats.bloquant(paralyse))
        // Empoisonné n'empêche pas d'agir.
        assertTrue(Etats.peutAgir(setOf(ConditionCombat.EMPOISONNE)))
    }

    @Test
    fun `champ condition d'une fiche a plusieurs etats`() {
        val texte = "Paralysée, empoisonné, Maudit, à terre"
        assertEquals(setOf(ConditionCombat.PARALYSE, ConditionCombat.EMPOISONNE, ConditionCombat.A_TERRE), Etats.lire(texte))
        assertEquals(listOf("Maudit"), Etats.autres(texte))
        assertEquals("À terre, Empoisonné, Paralysé, Maudit", Etats.ecrire(texte, Etats.lire(texte)))
        assertEquals("Maudit", Etats.ecrire(texte, emptySet()))
        assertEquals(ConditionCombat.INCAPABLE_D_AGIR, Etats.reconnaitre("Incapable d'agir"))
        assertEquals(ConditionCombat.EMPOISONNE, Etats.reconnaitre("Empoisonnées"))
    }

    @Test
    fun `avantage et desavantage selon les etats`() {
        assertEquals(ModeJet.AVANTAGE, Etats.modeAttaque(emptySet(), setOf(ConditionCombat.PARALYSE), auContact = false).first)
        assertEquals(ModeJet.DESAVANTAGE, Etats.modeAttaque(setOf(ConditionCombat.EMPOISONNE), emptySet(), auContact = true).first)
        assertEquals(ModeJet.AVANTAGE, Etats.modeAttaque(emptySet(), setOf(ConditionCombat.A_TERRE), auContact = true).first)
        assertEquals(ModeJet.DESAVANTAGE, Etats.modeAttaque(emptySet(), setOf(ConditionCombat.A_TERRE), auContact = false).first)
        // Les deux s'annulent.
        assertEquals(ModeJet.NORMAL, Etats.modeAttaque(setOf(ConditionCombat.EMPOISONNE), setOf(ConditionCombat.ENTRAVE), true).first)
        assertTrue(Etats.critiqueAuContact(setOf(ConditionCombat.INCONSCIENT), auContact = true))
        assertFalse(Etats.critiqueAuContact(setOf(ConditionCombat.INCONSCIENT), auContact = false))
    }

    @Test
    fun `attaque avec avantage garde le meilleur de deux d20 et critique au contact`() {
        val r = lancerAttaqueMonstre(morsure, 30, caCible = 10, aleatoire = Random(5), mode = ModeJet.AVANTAGE, critiqueSiTouche = true)
        assertTrue(r.all { it.d20Ecarte != null && it.d20!! >= it.d20Ecarte!! })
        assertTrue(r.filter { it.touche }.all { it.critique })
    }

    @Test
    fun `etats infliges lus dans les actions des monstres`() {
        val actions = ActionsMonstreParser.parser(
            """
            ## Actions
            Morsure. Corps à corps : +5, allonge 1,50 m. Touché : 10 (2d6 + 3) dégâts tranchants. Si la cible est une créature de taille G ou inférieure, elle subit l’état Agrippé (évasion DD 13).
            Griffe. Corps à corps : +5, allonge 1,50 m. Touché : 10 (2d6 + 3) dégâts tranchants. Si la cible est une créature autre qu’un Mort-vivant, elle est soumise à l’effet suivant. JS Constitution : DD 10. Échec : la cible subit l’état Paralysé jusqu’à la fin de son tour suivant.
            Engloutissement. JS Dextérité : DD 15, une créature. Échec : la cible subit les états Aveuglé et Entravé.
            """.trimIndent()
        )
        val (morsureLue, griffe, engloutir) = actions.attaques
        assertEquals(ConditionCombat.AGRIPPE, morsureLue.conditions.single().condition)
        assertEquals(13, morsureLue.conditions.single().evasionDd)
        assertFalse(morsureLue.conditions.single().surEchecJs)
        val paralysie = griffe.conditions.single()
        assertEquals(ConditionCombat.PARALYSE, paralysie.condition)
        assertEquals("Constitution", paralysie.sauvegarde)
        assertEquals(10, paralysie.dd)
        assertEquals(listOf(ConditionCombat.AVEUGLE, ConditionCombat.ENTRAVE), engloutir.conditions.map { it.condition })
        assertTrue(engloutir.conditions.all { it.dd == 15 })
    }

    @Test
    fun `le livre des etats ne liste plus les monstres`() {
        val livre = LivreEtats.construire()
        assertEquals("Les états", livre.first().name)
        assertTrue(livre.any { it.name == "Paralysé" })
        assertTrue(livre.none { it.rawMarkdown.contains("Monstres qui l'infligent") })
        assertTrue(livre.any { it.name == Etats.NOM_EPUISEMENT })
    }

    @Test
    fun `tournures du bestiaire reconnues et negations ignorees`() {
        assertEquals(
            listOf(ConditionCombat.AGRIPPE, ConditionCombat.ENTRAVE),
            Etats.infligesPar("La cible subit l’état Agrippé (évasion DD 16), ainsi que l’état Entravé tant qu’elle est Agrippée.").map { it.condition }
        )
        assertEquals(
            listOf(ConditionCombat.EMPOISONNE, ConditionCombat.PARALYSE),
            Etats.infligesPar("Échec : la cible subit l’état Empoisonné pendant 1 heure. Tant qu’elle est ainsi Empoisonnée, la cible subit en outre l’état Paralysé.").map { it.condition }
        )
        assertEquals(ConditionCombat.AGRIPPE, Etats.infligesPar("le tapis peut lui imposer l’état Agrippé (évasion DD 13)").single().condition)
        assertTrue(Etats.infligesPar("les créatures englouties ne subissent plus l’état Entravé").isEmpty())
        assertTrue(Etats.infligesPar("La cible ne subit pas l’état Charmé.").isEmpty())
        assertTrue(Etats.infligesPar("Échec : la cible est engloutie, et l’état Agrippé prend fin.").isEmpty())
    }

    @Test
    fun `les attaques du bestiaire embarque portent leurs etats en combat`() {
        val monstres = MonsterParser.parse(File("src/main/assets/dnd/monster_srd521.md").readText())
        fun attaque(monstre: String, nom: String) =
            ProfilCombatMonstre.depuisFiche(monstres.first { it.name == monstre }.rawMarkdown).actions.attaques.first { it.nom == nom }
        val tentacule = attaque("Aboleth", "Tentacule").conditions.single()
        assertEquals(ConditionCombat.AGRIPPE, tentacule.condition)
        assertEquals(14, tentacule.evasionDd)
        assertFalse(tentacule.surEchecJs)
        // Toute mention « subit l’état X » du bestiaire est reconnue (aucune tournure oubliée).
        val labels = Etats.fiches.filter { it.officiel }.joinToString("|") { Regex.escape(it.condition.label) }
        val simple = Regex("""subit\s+(?:aussi\s+|en outre\s+)?l[’']état\s+($labels)""")
        val nonReconnues = monstres.flatMap { m -> m.rawMarkdown.lines().map { m.name to it } }
            .flatMap { (nom, ligne) -> simple.findAll(ligne).map { Triple(nom, it.groupValues[1], ligne) } }
            .filter { (_, label, ligne) -> Etats.infligesPar(ligne).none { it.condition.label == label } }
            .map { (nom, label, _) -> "$nom : $label" }
        assertTrue("non reconnues : $nonReconnues", nonReconnues.isEmpty())
        val avecEtats = monstres.sumOf { m -> ProfilCombatMonstre.depuisFiche(m.rawMarkdown).actions.attaques.count { it.conditions.isNotEmpty() } }
        assertTrue("actions avec état : $avecEtats", avecEtats >= 100)
    }

    @Test
    fun `l'IA d'un monstre immobilise ne se deplace pas`() {
        val agrippe = monstre("Ogre", setOf(ConditionCombat.AGRIPPE))
        val loin = IaMonstre.decider(agrippe, listOf(pj("Aldric")), distances = mapOf("Aldric" to Distance.COURTE))
        assertTrue(loin.deplacements.isEmpty())
        assertNull(loin.attaque)
        val proche = IaMonstre.decider(agrippe, listOf(pj("Aldric")), distances = mapOf("Aldric" to Distance.CONTACT))
        assertEquals("Morsure", proche.attaque?.nom)
        assertTrue(proche.deplacements.isEmpty())
    }

    @Test
    fun `un joueur paralyse recoit Aucune action et ne peut pas en declarer`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric", setOf(ConditionCombat.PARALYSE)), pj("Brune"), monstre("Loup")))
        CombatSession.commencer()
        var etat = CombatSession.etat.value!!
        assertEquals("Aucune action", etat.declarations["Aldric"]?.libelle)
        CombatSession.declarer(DeclarationAction("Aldric", "Attaquer : Épée", actionId = "attaquer"))
        etat = CombatSession.etat.value!!
        assertEquals("Aucune action", etat.declarations["Aldric"]?.libelle)
        // Libéré pendant la déclaration : il peut de nouveau choisir.
        CombatSession.basculerCondition("Aldric", ConditionCombat.PARALYSE)
        etat = CombatSession.etat.value!!
        assertNull(etat.declarations["Aldric"])
        assertEquals(PhaseCombat.DECLARATION, etat.phase)
    }

    @Test
    fun `devenir neutralise rompt la concentration et le deplacement est annule si immobile`() {
        CombatSession.demarrer("Test", listOf(pj("Aldric", setOf(ConditionCombat.CONCENTRATION)), pj("Brune", setOf(ConditionCombat.ENTRAVE)), monstre("Loup")))
        CombatSession.commencer()
        CombatSession.basculerCondition("Aldric", ConditionCombat.ETOURDI)
        assertFalse(ConditionCombat.CONCENTRATION in CombatSession.etat.value!!.combattants.first { it.id == "Aldric" }.conditions)
        CombatSession.declarer(DeclarationAction("Brune", "Se précipiter", actionId = "dash", deplacements = mapOf("Loup" to Distance.CONTACT)))
        val brune = CombatSession.etat.value!!.declarations["Brune"]!!
        assertTrue(brune.deplacements.isEmpty())
        assertTrue(brune.detail!!.contains("vitesse 0"))
    }

    @Test
    fun `une attaque qui agrippe applique l'etat et un petrifie resiste aux degats`() {
        val tentacule = morsure.copy(nom = "Tentacule", bonusToucher = 30, conditions = ActionsMonstreParser.parser(
            "## Actions\nTentacule. Corps à corps : +30, allonge 1,50 m. Touché : 12 (2d6 + 5) dégâts contondants, et la cible subit l’état Agrippé (évasion DD 14)."
        ).attaques.single().conditions)
        CombatSession.aleatoire = Random(2)
        CombatSession.demarrer("Test", listOf(pj("Aldric"), monstre("Aboleth", attaques = listOf(tentacule))))
        CombatSession.commencer()
        CombatSession.definirDistancesMonstre("Aboleth", Distance.CONTACT)
        val d = CombatSession.etat.value!!.declarations["Aboleth"]!!
        if (d.resultats?.any { it.touche } == true) {
            CombatSession.remplacerJets("Aboleth", "Aldric", d.resultats!!)
            CombatSession.appliquerJets("Aboleth")
            assertTrue(ConditionCombat.AGRIPPE in CombatSession.etat.value!!.combattants.first { it.id == "Aldric" }.conditions)
        }
        CombatSession.basculerCondition("Aldric", ConditionCombat.PETRIFIE)
        CombatSession.appliquerPv("Aldric", -100)
        val avant = CombatSession.etat.value!!.combattants.first { it.id == "Aldric" }.pv
        CombatSession.appliquerPv("Aldric", 10)
        assertEquals(avant - 5, CombatSession.etat.value!!.combattants.first { it.id == "Aldric" }.pv)
    }
}
