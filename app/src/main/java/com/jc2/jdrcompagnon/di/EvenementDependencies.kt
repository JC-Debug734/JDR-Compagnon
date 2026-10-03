package com.jc2.jdrcompagnon.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepositoryImpl
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.ProfilEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.presentation.EvenementListViewModel
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ⚠️ Comme TableAleatoireDependencies : pas de Hilt/Koin dans le projet, réutilise la base Room
 * initialisée par BoutiqueDependencies.init (voir GameState.init), qui doit avoir été appelé
 * avant toute utilisation de cet objet.
 */
object EvenementDependencies {

    val repository: EvenementRepository by lazy {
        EvenementRepositoryImpl(BoutiqueDependencies.requireDatabase().evenementDao())
    }

    fun newListViewModel(): EvenementListViewModel = EvenementListViewModel(repository)

    /**
     * Résout un lien de scénario `#evenement:[Titre]` dans la bibliothèque du monde, sans tenir
     * compte de la casse. À titre égal, l'événement de la campagne [campagneId] passe avant les
     * événements communs.
     */
    suspend fun trouverEvenement(worldId: String, titre: String, campagneId: String? = null): Evenement? =
        repository.observerEvenements(worldId).first()
            .filter { it.titre.trim().equals(titre.trim(), ignoreCase = true) }
            .sortedBy { if (it.campagneId != null && it.campagneId == campagneId) 0 else 1 }
            .firstOrNull()

    /** Monde de la campagne [campagneId], à défaut le monde courant. */
    fun mondeDeCampagne(campagneId: String?): String =
        GameState.mjCampaigns.value.firstOrNull { it.id == campagneId }?.worldId?.takeIf { it.isNotBlank() }
            ?: GameState.currentWorldId()
            ?: "donjon_et_dragon"

    private val seedMutex = Mutex()

    /**
     * Pré-remplit la bibliothèque une seule fois par monde : les anciens événements génériques
     * de la carte (anciennement codés en dur), typés, et quelques exemples pour que chaque type
     * en ait au moins trois. Mémorisé par un indicateur plutôt que "bibliothèque vide" : la
     * conversion des anciennes tables la remplit déjà, et des exemples supprimés par le MJ ne
     * doivent pas réapparaître.
     */
    fun seedExamplesIfNeeded(context: Context, worldId: String) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch { seedSiNecessaire(appContext, worldId) }
    }

    suspend fun seedSiNecessaire(context: Context, worldId: String) = seedMutex.withLock {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cleExemples = "exemples_$worldId"
        // Version des exemples : à incrémenter quand leur contenu s'enrichit (attendu, issues,
        // profils...), pour compléter une fois les exemples déjà présents.
        val cleVersion = "exemples_version_$worldId"
        if (!prefs.getBoolean(cleExemples, false)) {
            repository.sauvegarderTous(exemples(worldId))
            prefs.edit().putBoolean(cleExemples, true).apply()
        }
        // Toujours après l'ajout des exemples : la bibliothèque d'un monde peut déjà contenir des
        // événements d'exemple venus d'ailleurs (anciennes tables converties) à compléter.
        if (prefs.getInt(cleVersion, 1) < VERSION_EXEMPLES) {
            completerExemplesExistants(worldId)
            prefs.edit().putInt(cleVersion, VERSION_EXEMPLES).apply()
        }
    }

    // 6 : repasse sur les mondes dont les événements de tables convertis avaient été sautés.
    // 7 : objectifs en liste et issues "mène au combat".
    private const val VERSION_EXEMPLES = 7

    /**
     * Mondes pré-remplis avant l'ajout de "ce qui est attendu", des issues et des profils :
     * complète les exemples (bibliothèque et tables d'exemple) restés tels quels — même titre,
     * même type, ni objectif, ni issue, ni profil, effets venant du modèle. Un exemple modifié
     * par le MJ n'est pas touché.
     */
    private suspend fun completerExemplesExistants(worldId: String) {
        val aEnregistrer = repository.observerEvenements(worldId).first().mapNotNull { existant ->
            enrichirDepuisModele(existant).takeIf { it != existant }
        }
        repository.sauvegarderTous(aEnregistrer)
    }

    // Modèles des exemples (bibliothèque et tables), par titre ; le monde n'importe pas ici.
    private val modelesParTitre: Map<String, Evenement> by lazy {
        (exemples("") + TableAleatoireDependencies.modelesEvenements()).associateBy { it.titre }
    }

    /**
     * Complète un événement d'exemple resté tel quel avec le contenu actuel de son modèle (même
     * titre, même type, ni objectif, ni issue, ni profil, effets venant du modèle), après
     * l'éventuelle correction de son titre (voir [CORRECTIONS_EXEMPLES]). Renvoie l'événement
     * inchangé sinon : un exemple modifié par le MJ n'est pas touché. Utilisé aussi par la
     * conversion des anciennes tables (TableAleatoireRepositoryImpl), pour que leurs événements
     * naissent complets quel que soit le moment où la table est lue.
     */
    fun enrichirDepuisModele(evenement: Evenement): Evenement {
        val corrige = corrigerExemple(evenement, modelesParTitre) ?: evenement
        val modele = modelesParTitre[corrige.titre] ?: return corrige
        val effetsDuModele = (modele.effets + modele.issues.flatMap { it.effets }).map { it.contenu() }
        val intact = corrige.type == modele.type && corrige.objectif.isBlank() && corrige.issues.isEmpty() &&
            corrige.profils.isEmpty() && corrige.effets.all { it.contenu() in effetsDuModele }
        if (intact) {
            return corrige.copy(objectif = modele.objectif, issues = modele.issues, effets = modele.effets, profils = modele.profils)
        }
        // Exemple déjà complété : ne suivre que les retouches du modèle sur ce que le MJ n'a pas
        // modifié — objectif reformulé en liste, issues marquées "mène au combat".
        var maj = corrige
        if (maj.objectif in ANCIENS_OBJECTIFS[modele.titre].orEmpty()) maj = maj.copy(objectif = modele.objectif)
        if (memesIssues(maj.issues, modele.issues)) {
            maj = maj.copy(issues = maj.issues.zip(modele.issues) { issue, duModele -> issue.copy(declencheCombat = duModele.declencheCombat) })
        }
        return maj
    }

    /** Mêmes issues, dans le même ordre, sans tenir compte des ids ni du marquage combat. */
    private fun memesIssues(a: List<IssueEvenement>, b: List<IssueEvenement>): Boolean =
        a.size == b.size && a.zip(b).all { (x, y) ->
            x.nature == y.nature && x.titre == y.titre && x.description == y.description &&
                x.effets.map { it.contenu() } == y.effets.map { it.contenu() }
        }

    /** Exemple remplacé depuis : ancien titre et description d'origine → modèle [nouveauTitre]. */
    private data class CorrectionExemple(
        val ancienTitre: String,
        val ancienneDescription: String,
        val nouveauTitre: String,
        val anciensProfils: List<ProfilEvenement> = emptyList(),
    )

    // La patrouille d'orques (puis d'orcs) devient une patrouille de hobgobelins : pas de fiche
    // d'orc dans le bestiaire SRD 5.2.1 pour ouvrir le profil et lancer le combat.
    private val CORRECTIONS_EXEMPLES = listOf(
        CorrectionExemple(
            "Patrouille d'orques", "Une patrouille d'orques bien armée surveille son territoire.",
            "Patrouille de hobgobelins", listOf(monstre("Berserker", 3))
        ),
        CorrectionExemple(
            "Patrouille d'orcs", "Une patrouille d'orcs bien armée surveille son territoire.",
            "Patrouille de hobgobelins", listOf(monstre("Berserker", 3))
        ),
    )

    /**
     * Remplace un exemple corrigé resté tel quel (titre et description d'origine) par son
     * nouveau modèle ; ses issues et profils aussi s'ils n'avaient pas été modifiés. Null =
     * aucune correction à appliquer.
     */
    private fun corrigerExemple(evenement: Evenement, modeles: Map<String, Evenement>): Evenement? {
        val correction = CORRECTIONS_EXEMPLES.firstOrNull {
            it.ancienTitre == evenement.titre && it.ancienneDescription == evenement.description
        } ?: return null
        val modele = modeles[correction.nouveauTitre] ?: return null
        val corrige = evenement.copy(titre = modele.titre, description = modele.description)
        return if (evenement.profils == correction.anciensProfils) {
            corrige.copy(profils = modele.profils, issues = modele.issues)
        } else {
            corrige
        }
    }

    private fun EffetEvenement.contenu() = listOf(categorie, gain, quantite, cible, description)

    private const val PREFS = "evenements_prefs"

    internal fun exemples(worldId: String): List<Evenement> {
        fun evt(
            type: TypeEvenement,
            titre: String,
            description: String,
            objectif: String,
            issues: List<IssueEvenement> = emptyList(),
            effets: List<EffetEvenement> = emptyList(),
            profils: List<ProfilEvenement> = emptyList(),
        ) = Evenement(
            worldId = worldId, type = type, titre = titre, description = description,
            objectif = objectif, issues = issues, effets = effets, profils = profils
        )

        return listOf(
            // Rencontres
            evt(
                TypeEvenement.RENCONTRE, "Marchand ambulant",
                "Le groupe croise un marchand itinérant proposant quelques objets à prix variable.",
                "Marchander : Charisme (Persuasion) DD 12.",
                listOf(
                    reussite("Prix réduits de 20 %, et il glisse une rumeur sur la route."),
                    echec("Prix habituels ; vexé, il refuse de racheter le butin du groupe.")
                ),
                listOf(info("Occasion d'achat/vente improvisée."))
            ),
            evt(
                TypeEvenement.RENCONTRE, "Patrouille locale",
                "Une patrouille (garde, milice, faction locale) croise le groupe et l'interroge.",
                "Répondre sans éveiller les soupçons : Charisme (Persuasion ou Tromperie) DD 13.",
                listOf(
                    reussite("La patrouille les laisse passer et leur indique un abri sûr."),
                    echec("Fouille des sacs : tout objet suspect est confisqué, ou le groupe est escorté au poste.", reputation("Garde locale", -5))
                ),
                listOf(info("Contrôle de routine, éventuel accroc selon la réponse du groupe.")),
                listOf(monstre("Garde", 4))
            ),
            evt(
                TypeEvenement.RENCONTRE, "Animal sauvage",
                "Une faune locale (pas nécessairement hostile) croise la route du groupe.",
                "Apaiser la bête : Sagesse (Dressage) DD 12\nL'éviter : Dextérité (Discrétion) DD 13",
                listOf(
                    reussite("La bête s'éloigne, ou se laisse approcher."),
                    echec("Elle attaque : combat contre un animal adapté au niveau (loup, sanglier, ours).", combat = true)
                ),
                listOf(info("Opportunité de rôle-play ou de petite rencontre.")),
                listOf(monstre("Loup", 2))
            ),
            // Découvertes
            evt(
                TypeEvenement.DECOUVERTE, "Embuscade évitée",
                "Des traces suspectes permettent au groupe d'éviter une embuscade de justesse.",
                "Repérer les traces : Sagesse (Perception) DD 13.",
                listOf(
                    reussite(
                        "Le groupe contourne l'embuscade, et peut même surprendre les brigands.",
                        info("Pas de combat, mais une menace identifiée sur la route.")
                    ),
                    echec("Les brigands attaquent avec l'avantage de la surprise.", combat = true)
                ),
                profils = listOf(monstre("Bandit", 4), monstre("Chef de bande"))
            ),
            evt(
                TypeEvenement.DECOUVERTE, "Raccourci découvert",
                "Un guide local ou un indice sur une carte révèle un raccourci praticable.",
                "Suivre le raccourci : Sagesse (Survie) DD 12.",
                listOf(
                    reussite("Le trajet est raccourci d'une demi-journée.", info("Un raccourci à noter sur la carte.")),
                    echec("Le groupe s'égare : une heure perdue, Constitution DD 10 ou un niveau d'épuisement.")
                )
            ),
            evt(
                TypeEvenement.DECOUVERTE, "Vestige oublié",
                "Le groupe repère les restes d'un campement ou d'une bataille ancienne en chemin.",
                "Fouiller les restes : Intelligence (Investigation) DD 12.",
                listOf(
                    reussite(
                        "Un objet abandonné et un indice sur ce qui s'est passé ici.",
                        objet("Potion de soins"),
                        info("Un indice ou un objet abandonné à décrire.")
                    ),
                    echec("Rien d'utile, seulement du temps perdu.")
                )
            ),
            // Rumeurs / indices
            evt(
                TypeEvenement.RUMEUR_INDICE, "Rumeur locale",
                "Un voyageur croisé sur la route partage une rumeur sur la région (trésor caché, danger imminent, personnage important).",
                "Gagner sa confiance : Charisme (Persuasion) DD 10.",
                listOf(
                    reussite("La rumeur est exacte et précise : un lieu, un nom, un danger."),
                    echec("La rumeur est déformée ou exagérée.")
                ),
                listOf(info("Une rumeur locale à exploiter en jeu."))
            ),
            evt(
                TypeEvenement.RUMEUR_INDICE, "Messager pressé",
                "Un messager croise le groupe, porteur d'une nouvelle qui peut concerner l'intrigue en cours.",
                "Le convaincre de parler : Charisme (Persuasion ou Intimidation) DD 13.",
                listOf(
                    reussite("Le message révèle un élément de l'intrigue.", info("Une nouvelle fraîche à intégrer au scénario.")),
                    echec("Il refuse et file au galop ; trop d'insistance peut alerter ceux qui l'envoient.")
                )
            ),
            evt(
                TypeEvenement.RUMEUR_INDICE, "Journal abandonné",
                "Un carnet détrempé gît près d'un sac éventré : les dernières pages évoquent une créature aperçue plus loin.",
                "Déchiffrer les pages : Intelligence (Investigation) DD 12.",
                listOf(
                    reussite("La créature est identifiée : sa nature, son repaire et une faiblesse."),
                    echec("Seule une vague direction reste lisible.")
                ),
                listOf(info("Annonce une menace à venir sur la route."))
            ),
            // Périls
            evt(
                TypeEvenement.PERIL, "Mauvais temps",
                "Une tempête ou une pluie battante ralentit la progression du groupe.",
                "Trouver un abri : Sagesse (Survie) DD 12.",
                listOf(
                    reussite("Abri trouvé : peu de retard."),
                    echec("Demi-journée perdue ; Constitution DD 10 ou un niveau d'épuisement.", info("Le trajet en cours prend plus de temps que prévu."))
                )
            ),
            evt(
                TypeEvenement.PERIL, "Gué en crue",
                "La rivière a monté pendant la nuit et le courant est violent.",
                "Traverser : Force (Athlétisme) DD 12, avantage avec une corde.",
                listOf(
                    reussite("Le groupe traverse sans encombre."),
                    echec("Emporté sur quelques mètres : 1d6 dégâts contondants, un objet peut être perdu.", info("Échec : emporté sur quelques mètres, 1d6 dégâts contondants."))
                )
            ),
            evt(
                TypeEvenement.PERIL, "Éboulement",
                "Des pierres dévalent la pente au passage du groupe.",
                "Jet de sauvegarde de Dextérité DD 13.",
                listOf(
                    reussite("Moitié des dégâts : 2d6 divisé par deux."),
                    echec("2d6 dégâts contondants, et le chemin est bloqué.", info("Échec : 2d6 dégâts contondants, moitié en cas de réussite."))
                )
            ),
            // Complications
            evt(
                TypeEvenement.COMPLICATION, "Incident maladroit",
                "Une maladresse ou un malentendu du groupe agace des habitants du coin.",
                "Apaiser les habitants : Charisme (Persuasion) DD 13\nDédommager : 5 po",
                listOf(
                    reussite("L'incident est vite oublié."),
                    echec("La nouvelle se répand dans le village.", reputation("Habitants locaux", -5))
                )
            ),
            evt(
                TypeEvenement.COMPLICATION, "Incident matériel",
                "Une roue, une sangle ou un équipement cède en cours de route.",
                "Réparer : outils adaptés et Intelligence ou Dextérité DD 12.",
                listOf(
                    reussite("Réparé en une heure."),
                    echec("Une demi-journée perdue, ou du matériel abandonné.", info("Réparation ou détour nécessaire."))
                )
            ),
            evt(
                TypeEvenement.COMPLICATION, "Monture blessée",
                "Une des montures boite après un faux pas.",
                "Soigner la monture : Sagesse (Médecine ou Dressage) DD 13.",
                listOf(
                    reussite("Elle peut repartir au pas."),
                    echec("Il faut ralentir jusqu'au prochain village, ou la laisser derrière.", info("Vitesse de voyage réduite jusqu'à soin ou remplacement."))
                )
            ),
            // Opportunités
            evt(
                TypeEvenement.OPPORTUNITE, "Aide à un PNJ",
                "Un villageois en difficulté demande l'aide du groupe (roue cassée, animal égaré, blessé léger).",
                "Roue cassée : Force (Athlétisme) DD 12\nAnimal égaré : Sagesse (Survie) DD 12\nBlessé léger : Sagesse (Médecine) DD 12",
                listOf(
                    reussite("Le villageois remercie le groupe et lui offre le gîte.", reputation("Habitants locaux", 5)),
                    echec("L'aide arrive trop tard, mais il apprécie l'effort."),
                    autre("Ignoré", "Le groupe passe son chemin ; on parlera au village d'étrangers indifférents.")
                )
            ),
            evt(
                TypeEvenement.OPPORTUNITE, "Marque de confiance",
                "Un notable local demande au groupe un service discret.",
                "Agir sans être vu : Dextérité (Discrétion) DD 13\nOu donner le change : Charisme (Tromperie) DD 13",
                listOf(
                    reussite("Le notable s'en souviendra.", reputation("Notables locaux", 10)),
                    echec("Le service est découvert : le notable nie tout lien avec le groupe.", reputation("Notables locaux", -5))
                )
            ),
            evt(
                TypeEvenement.OPPORTUNITE, "Troc improvisé",
                "Des voyageurs proposent un échange d'objets plutôt qu'une vente classique.",
                "Négocier : Charisme (Persuasion) DD 12\nRepérer l'arnaque : Sagesse (Perspicacité) DD 12",
                listOf(
                    reussite("Échange avantageux pour le groupe."),
                    echec("Le groupe se fait avoir sur la valeur des objets.")
                ),
                listOf(info("Occasion de troc à négocier."))
            ),
            // Répits
            evt(
                TypeEvenement.REPIT, "Faveur rendue",
                "Un PNJ déjà croisé auparavant reconnaît le groupe et lui rend service.",
                "Aucun jet : le groupe peut demander une aide précise (information, soins, abri).",
                effets = listOf(reputation("Alliés de voyage", 8))
            ),
            evt(
                TypeEvenement.REPIT, "Campement abrité",
                "Une grotte sèche ou une ruine solide offre un abri pour la nuit.",
                "Installer le camp : Sagesse (Survie) DD 10.",
                listOf(
                    reussite("Repos long sans risque de rencontre.", info("Repos long sans risque de rencontre.")),
                    echec("Repos long possible, mais une rencontre trouble la nuit.")
                )
            ),
            evt(
                TypeEvenement.REPIT, "Source claire",
                "Une source limpide permet de refaire les provisions et de soigner les plaies.",
                "Aucun jet : il suffit de prendre le temps.",
                effets = listOf(info("Chaque personnage peut dépenser un dé de vie supplémentaire lors du prochain repos court."))
            ),
        )
    }
}

class EvenementListViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EvenementDependencies.newListViewModel() as T
    }
}
