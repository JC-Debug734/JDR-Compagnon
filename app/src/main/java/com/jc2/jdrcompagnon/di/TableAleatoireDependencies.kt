package com.jc2.jdrcompagnon.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.TableAleatoireRepository
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.TableAleatoireRepositoryImpl
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.ProfilEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.TirageTableUseCase
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.VerifierDeclenchementTableUseCase
import com.jc2.jdrcompagnon.feature_table_aleatoire.presentation.TableAleatoireDetailViewModel
import com.jc2.jdrcompagnon.feature_table_aleatoire.presentation.TableAleatoireListViewModel
import com.jc2.jdrcompagnon.ui.LectureScenarioState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ⚠️ Comme HistoriqueDependencies/CarteDependencies/EnvironmentDependencies : pas de Hilt/Koin
 * dans le projet, et pas de base Room propre — réutilise celle initialisée par
 * BoutiqueDependencies.init (voir GameState.init), qui doit avoir été appelé avant toute
 * utilisation de cet objet.
 */
object TableAleatoireDependencies {

    private val verifierDeclenchement = VerifierDeclenchementTableUseCase()

    /** Exposé publiquement : réutilisé pour le tirage rapide depuis ScenarioReaderContent. */
    val tirage = TirageTableUseCase()

    val repository: TableAleatoireRepository by lazy {
        TableAleatoireRepositoryImpl(BoutiqueDependencies.requireDatabase().tableAleatoireDao(), EvenementDependencies.repository, EvenementDependencies::enrichirDepuisModele)
    }

    fun newListViewModel(): TableAleatoireListViewModel = TableAleatoireListViewModel(
        repository = repository,
        verifierDeclenchement = verifierDeclenchement,
        minutesLecture = LectureScenarioState.minutesEcoulees,
    )

    fun newDetailViewModel(tableId: String): TableAleatoireDetailViewModel = TableAleatoireDetailViewModel(
        tableId = tableId,
        repository = repository,
        evenementRepository = EvenementDependencies.repository,
        tirage = tirage,
        verifierDeclenchement = verifierDeclenchement,
        minutesLecture = LectureScenarioState.minutesEcoulees,
    )

    /**
     * Pré-remplit des tables d'événements et de loot par palier de niveau de groupe (même
     * principe que EnvironmentDependencies.seedExamplesIfNeeded), la première fois qu'un monde
     * n'a aucune table, pour que l'outil ne s'ouvre jamais complètement vide. Les événements des
     * tables d'exemple sont créés dans la bibliothèque (feature_evenement), après ses propres
     * exemples, puis référencés par les tables.
     */
    fun seedExamplesIfNeeded(context: Context, worldId: String) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            EvenementDependencies.seedSiNecessaire(appContext, worldId)
            if (repository.compterPourMonde(worldId) > 0) return@launch
            val tables = paliers(worldId)
            EvenementDependencies.repository.sauvegarderTous(
                tables.flatMap { table -> table.entreesEvenements.mapNotNull { it.evenement?.copy(worldId = worldId) } }
            )
            tables.forEach { table -> repository.sauvegarder(table) }
        }
    }

    /**
     * Événements des tables d'exemple, sans monde : modèles servant à compléter ceux déjà créés
     * dans un monde (voir EvenementDependencies.completerExemplesExistants).
     */
    fun modelesEvenements(): List<Evenement> =
        paliers("").flatMap { table -> table.entreesEvenements.mapNotNull { it.evenement } }

    // L'événement est porté par l'entrée (déjà résolue) le temps de l'enregistrer dans la
    // bibliothèque ; son monde est fixé à ce moment-là (voir seedExamplesIfNeeded).
    private fun evenement(
        type: TypeEvenement,
        titre: String,
        description: String,
        poids: Int = 1,
        objectif: String = "",
        issues: List<IssueEvenement> = emptyList(),
        profils: List<ProfilEvenement> = emptyList(),
        effets: List<EffetEvenement> = emptyList(),
    ): EntreeEvenement {
        val evenement = Evenement(
            worldId = "", type = type, titre = titre, description = description,
            objectif = objectif, issues = issues, profils = profils, effets = effets
        )
        return EntreeEvenement(evenementId = evenement.id, poids = poids, evenement = evenement)
    }

    private fun loot(nom: String, poids: Int, min: Int, max: Int = min) =
        EntreeLoot(equipementNom = nom, poids = poids, quantiteMin = min, quantiteMax = max)

    private fun paliers(worldId: String): List<TableAleatoire> = listOf(
        TableAleatoire(
            nom = "Rencontres — Niveau 1-4 (Novice)",
            worldId = worldId,
            type = TypeTable.EVENEMENTS,
            intervalleHeures = 4,
            entreesEvenements = listOf(
                evenement(
                    TypeEvenement.RENCONTRE, "Bande de gobelins", "Un petit groupe de gobelins embusqués guette les voyageurs imprudents.", 3,
                    objectif = "Repérer l'embuscade : Sagesse (Perception) DD 12\n" +
                        "Négocier le passage : un objet brillant ou 10 po (les gobelins adorent les babioles)",
                    issues = listOf(
                        reussite("Le groupe attaque en premier, ou contourne les gobelins.", titre = "Embuscade repérée", combat = true),
                        partielle("Ils laissent passer contre un objet brillant ou 10 po : pas de combat.", or(-10), titre = "Négociation"),
                        echec("Les gobelins attaquent par surprise.", titre = "Pris par surprise", combat = true)
                    ),
                    profils = listOf(monstre("Sbire gobelin", 3), monstre("Combattant gobelin", 2))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Loups affamés", "Une meute de loups décharnés rôde, poussée par la faim.", 2,
                    objectif = "Effrayer la meute : feu ou nourriture jetée\nLa calmer : Sagesse (Dressage) DD 13",
                    issues = listOf(
                        reussite("La meute se détourne vers la nourriture et s'éloigne."),
                        echec("La meute attaque en cherchant à isoler le plus faible.", combat = true)
                    ),
                    profils = listOf(monstre("Loup", 3))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Bandits de grand chemin", "Des brigands exigent une bourse en échange du passage.", 3,
                    objectif = "Payer : 20 po\n" +
                        "Négocier : Charisme (Persuasion) DD 13\n" +
                        "Intimider : Charisme (Intimidation) DD 15\n" +
                        "Refus ou jet raté : combat",
                    issues = listOf(
                        autre("Accepter de payer", "Le groupe paie la bourse exigée et passe sans combat.", or(-20)),
                        partielle("Les bandits se contentent de la moitié : pas de combat.", or(-10), titre = "Négociation réussie"),
                        reussite("Les bandits s'écartent sans rien prendre.", titre = "Intimidation réussie"),
                        echec("Refus ou jet raté : les bandits attaquent ; le chef s'enfuit s'il est blessé.", titre = "Combat", combat = true)
                    ),
                    profils = listOf(monstre("Bandit", 4), monstre("Chef de bande"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Sanglier enragé", "Un sanglier blessé charge sans sommation.", 1,
                    objectif = "Esquiver la charge : Dextérité DD 12\nLe calmer : Sagesse (Dressage) DD 14",
                    issues = listOf(
                        reussite("Le sanglier passe et disparaît dans les fourrés, laissant une piste de sang."),
                        echec("La charge renverse un personnage (à terre, 2d6 contondants) et le combat s'engage.", combat = true)
                    ),
                    profils = listOf(monstre("Sanglier"))
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Camp abandonné", "Les cendres encore tièdes d'un feu de camp récemment déserté.", 2,
                    objectif = "Examiner le camp : Sagesse (Survie) ou Intelligence (Investigation) DD 12.",
                    issues = listOf(
                        reussite("Le groupe déduit qui campait (nombre, direction, depuis quand) et récupère des provisions.", objet("Rations de voyage", 2)),
                        echec("Rien de plus que des cendres.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Ruines couvertes de lierre", "Les vestiges d'une petite tour de guet envahie par la végétation.", 2,
                    objectif = "Dégager l'entrée et explorer : Force (Athlétisme) DD 12.",
                    issues = listOf(
                        reussite("Une cache intacte au sommet de la tour.", or(15)),
                        echec("Le plancher cède : 1d6 dégâts contondants, rien trouvé.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Source d'eau claire", "Une source naturelle, propice à une halte réparatrice.", 1,
                    objectif = "Aucun jet : profiter de la halte.",
                    effets = listOf(info("Repos court sans risque ; les outres sont remplies."))
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Moutons disparus", "Un fermier du coin se plaint de moutons disparus ces derniers jours.", 2,
                    objectif = "Suivre les traces : Sagesse (Survie) DD 12.",
                    issues = listOf(
                        reussite("Les traces mènent à une tanière de loups, à une heure de marche."),
                        echec("Les traces se perdent ; le fermier reste inquiet.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Traces de griffes", "De profondes traces de griffes sont visibles près d'un puits.", 2,
                    objectif = "Identifier la créature : Intelligence (Nature) DD 13.",
                    issues = listOf(
                        reussite("C'est un ours brun, et sa tanière est proche."),
                        echec("Impossible de dire ce qui a laissé ces marques.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Grotte maudite", "Un vieil ermite évoque une grotte voisine que plus personne n'ose approcher.", 1,
                    objectif = "Faire parler l'ermite : Charisme (Persuasion) DD 12.",
                    issues = listOf(
                        reussite("Il indique l'entrée et prévient d'un danger précis : des morts-vivants."),
                        echec("Il marmonne des avertissements confus.")
                    )
                ),
            )
        ),
        TableAleatoire(
            nom = "Butin — Niveau 1-4 (Novice)",
            worldId = worldId,
            type = TypeTable.LOOT,
            intervalleHeures = 4,
            entreesLoot = listOf(
                loot("Potion de soins", 3, 1, 2),
                loot("Pièces de cuivre", 4, 10, 40),
                loot("Dague", 2, 1),
                loot("Corde (15 mètres)", 2, 1),
                loot("Rations de voyage", 3, 1, 3),
            )
        ),
        TableAleatoire(
            nom = "Rencontres — Niveau 5-10 (Aventurier)",
            worldId = worldId,
            type = TypeTable.EVENEMENTS,
            intervalleHeures = 4,
            entreesEvenements = listOf(
                evenement(
                    TypeEvenement.RENCONTRE, "Patrouille de hobgobelins", "Une patrouille de hobgobelins disciplinés surveille son territoire.", 3,
                    objectif = "Se cacher : Dextérité (Discrétion) DD 14\nParlementer : Charisme (Intimidation) DD 15",
                    issues = listOf(
                        reussite("La patrouille passe sans voir le groupe."),
                        echec("Combat ; un soldat s'enfuit pour prévenir le camp.", combat = true)
                    ),
                    profils = listOf(monstre("Combattant hobgobelin", 3), monstre("Capitaine hobgobelin"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Ogre solitaire", "Un ogre affamé barre la route, massue à la main.", 2,
                    objectif = "Le berner : Charisme (Tromperie) DD 12\nL'amadouer : lui offrir de la nourriture",
                    issues = listOf(
                        reussite("L'ogre s'écarte, repu ou berné."),
                        echec("Il attaque en hurlant.", combat = true)
                    ),
                    profils = listOf(monstre("Ogre"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Nid d'araignées géantes", "Des toiles épaisses trahissent la présence d'araignées géantes.", 2,
                    objectif = "Traverser les toiles : Dextérité (Acrobaties) DD 13\nLes brûler : torche ou sort de feu",
                    issues = listOf(
                        reussite("Le groupe passe ; un cocon contient les affaires d'un ancien voyageur.", objet("Potion de soins")),
                        echec("Un personnage est entravé et les araignées descendent.", combat = true)
                    ),
                    profils = listOf(monstre("Araignée géante", 3))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Mercenaires en maraude", "Une compagnie de mercenaires sans scrupules rançonne la région.", 2,
                    objectif = "Payer : 50 po\nLes convaincre de passer leur chemin : Charisme (Persuasion) DD 15\nRefus ou jet raté : combat",
                    issues = listOf(
                        autre("Accepter de payer", "Les mercenaires empochent l'or et s'en vont.", or(-50)),
                        reussite("Ils changent de cible, voire proposent leurs services.", titre = "Persuasion réussie"),
                        echec("Refus ou jet raté : ils attaquent pour piller le groupe.", titre = "Combat", combat = true)
                    ),
                    profils = listOf(monstre("Vétéran", 2), monstre("Bandit", 3))
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Tour de guet abandonnée", "Une ancienne tour de guet, partiellement effondrée mais explorable.", 2,
                    objectif = "Repérer les zones fragiles : Sagesse (Perception) DD 13.",
                    issues = listOf(
                        reussite("Vue dégagée sur la région et une cache de vivres.", info("Le groupe repère la suite de son itinéraire.")),
                        echec("Effondrement : Dextérité DD 13 ou 3d6 dégâts contondants.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Autel oublié", "Un autel érodé dédié à une divinité oubliée.", 2,
                    objectif = "Identifier la divinité : Intelligence (Religion) DD 14.",
                    issues = listOf(
                        reussite("Une prière juste accorde une bénédiction : inspiration héroïque."),
                        echec("Une offrande maladroite attire une malédiction mineure : désavantage au prochain jet de sauvegarde.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Caverne de cristal", "Une caverne dont les parois scintillent de cristaux bruts.", 1,
                    objectif = "Extraire les cristaux : outils de mineur et Force DD 13.",
                    issues = listOf(
                        reussite("Des cristaux de belle qualité.", or(50)),
                        echec("Les cristaux se brisent et l'écho alerte une créature des profondeurs.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Caravane disparue", "Une caravane marchande a disparu sans laisser de trace sur la route du nord.", 2,
                    objectif = "Enquêter auprès des marchands : Intelligence (Investigation) DD 14.",
                    issues = listOf(
                        reussite("Des traces mènent à un repaire de bandits."),
                        echec("Fausse piste : une journée perdue.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Lumières étranges", "Des habitants rapportent des lumières inexpliquées la nuit en forêt.", 2,
                    objectif = "Observer les lumières : Intelligence (Arcanes) DD 14.",
                    issues = listOf(
                        reussite("Ce sont des feux follets qui attirent les voyageurs vers un marais."),
                        echec("Le groupe se laisse attirer hors du chemin.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Culte secret", "Un marchand nerveux évoque à mots couverts un culte agissant dans l'ombre.", 1,
                    objectif = "Le mettre en confiance : Sagesse (Perspicacité) DD 13\nLe faire parler : Charisme (Persuasion) DD 14",
                    issues = listOf(
                        reussite("Le lieu de réunion du culte et son mot de passe."),
                        echec("Le marchand prend peur et prévient le culte.")
                    )
                ),
            )
        ),
        TableAleatoire(
            nom = "Butin — Niveau 5-10 (Aventurier)",
            worldId = worldId,
            type = TypeTable.LOOT,
            intervalleHeures = 4,
            entreesLoot = listOf(
                loot("Potion de soins supérieure", 3, 1, 2),
                loot("Pièces d'argent", 4, 20, 80),
                loot("Épée courte +1", 1, 1),
                loot("Anneau protecteur", 1, 1),
                loot("Parchemin de sort (niveau 2)", 2, 1),
            )
        ),
        TableAleatoire(
            nom = "Rencontres — Niveau 11-16 (Champion)",
            worldId = worldId,
            type = TypeTable.EVENEMENTS,
            intervalleHeures = 4,
            entreesEvenements = listOf(
                evenement(
                    TypeEvenement.RENCONTRE, "Garde du corps d'un seigneur de guerre", "Une escorte redoutable protège un seigneur de guerre local.", 2,
                    objectif = "Obtenir une audience : Charisme (Persuasion) DD 16\nPasser inaperçu : Dextérité (Discrétion) DD 16",
                    issues = listOf(
                        reussite("Le seigneur de guerre accepte d'écouter le groupe."),
                        echec("L'escorte attaque, ou exige un lourd tribut.", combat = true)
                    ),
                    profils = listOf(monstre("Chevalier"), monstre("Vétéran", 4))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Chimère", "Une chimère fond sur le groupe depuis les hauteurs.", 2,
                    objectif = "La voir venir : Sagesse (Perception) DD 15\nS'abriter : trouver un couvert avant le premier souffle",
                    issues = listOf(
                        reussite("Le groupe n'est pas surpris et s'abrite du premier souffle.", combat = true),
                        echec("Souffle de feu avant que le groupe ne réagisse.", combat = true)
                    ),
                    profils = listOf(monstre("Chimère"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Élémentaire déchaîné", "Un élémentaire échappé de son invocation d'origine sème le chaos.", 2,
                    objectif = "Le renvoyer : Intelligence (Arcanes) DD 16 sur le cercle d'invocation\nLe vaincre : combat",
                    issues = listOf(
                        reussite("L'élémentaire retourne sur son plan."),
                        echec("Il ravage la zone et se retourne contre le groupe.", combat = true)
                    ),
                    profils = listOf(monstre("Élémentaire de la terre"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Assassins à la solde d'une guilde", "Des tueurs professionnels tendent une embuscade silencieuse.", 2,
                    objectif = "Déjouer l'embuscade : Sagesse (Perception) DD 16.",
                    issues = listOf(
                        reussite("Les assassins sont repérés avant de frapper.", combat = true),
                        echec("Attaque surprise contre le membre le plus important du groupe.", combat = true),
                        autre("Prisonnier", "Un assassin capturé livre le nom de son commanditaire.")
                    ),
                    profils = listOf(monstre("Assassin", 2), monstre("Espion", 2))
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Sanctuaire ancien scellé", "Un sanctuaire scellé depuis des siècles, gardé par des pièges anciens.", 2,
                    objectif = "Désamorcer les pièges : outils de voleur DD 17\nBriser le sceau : Intelligence (Arcanes) DD 17",
                    issues = listOf(
                        reussite("Le sanctuaire livre ses trésors.", objet("Gemme précieuse", 2)),
                        echec("Piège : 4d10 dégâts de foudre, Dextérité DD 17 pour moitié.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Portail dimensionnel instable", "Un portail crépitant s'ouvre et se referme par intermittence.", 1,
                    objectif = "Le stabiliser ou le refermer : Intelligence (Arcanes) DD 18.",
                    issues = listOf(
                        reussite("Le portail se referme, ou mène là où le groupe le souhaite."),
                        echec("Une créature d'un autre plan le traverse.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Bibliothèque engloutie", "Les vestiges d'une bibliothèque engloutie recèlent des savoirs perdus.", 1,
                    objectif = "Plonger : Constitution DD 14 (apnée)\nChercher : Intelligence (Investigation) DD 15",
                    issues = listOf(
                        reussite("Un grimoire préservé, ou une carte ancienne.", objet("Parchemin de sort (niveau 2)")),
                        echec("Un niveau d'épuisement, et les livres se désagrègent.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Complot d'un noble", "Un noble influent complote une trahison contre sa propre maison.", 2,
                    objectif = "Réunir des preuves : Intelligence (Investigation) DD 16.",
                    issues = listOf(
                        reussite("Des preuves irréfutables du complot."),
                        echec("Le noble apprend que le groupe enquête sur lui.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Prophétie d'un réveil", "Une prophétie ancienne parle d'un réveil imminent et funeste.", 1,
                    objectif = "Interpréter la prophétie : Intelligence (Histoire ou Religion) DD 16.",
                    issues = listOf(
                        reussite("Le lieu et le moment du réveil sont connus."),
                        echec("La prophétie reste obscure.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Disparitions dans la capitale", "Des disparitions inexpliquées inquiètent la population de la capitale.", 2,
                    objectif = "Interroger les témoins des bas quartiers : Sagesse (Perspicacité) DD 15.",
                    issues = listOf(
                        reussite("Une piste mène aux égouts."),
                        echec("Le groupe se fait remarquer par les ravisseurs.")
                    )
                ),
            )
        ),
        TableAleatoire(
            nom = "Butin — Niveau 11-16 (Champion)",
            worldId = worldId,
            type = TypeTable.LOOT,
            intervalleHeures = 4,
            entreesLoot = listOf(
                loot("Potion de soins suprême", 3, 1, 2),
                loot("Pièces d'or", 4, 50, 200),
                loot("Armure +2", 1, 1),
                loot("Bâton de sorcellerie", 1, 1),
                loot("Gemme précieuse", 2, 1, 3),
            )
        ),
        TableAleatoire(
            nom = "Rencontres — Niveau 17-20 (Légende)",
            worldId = worldId,
            type = TypeTable.EVENEMENTS,
            intervalleHeures = 4,
            entreesEvenements = listOf(
                evenement(
                    TypeEvenement.RENCONTRE, "Dragon adulte", "Un dragon adulte fond sur le groupe, toutes griffes dehors.", 1,
                    objectif = "Négocier : Charisme (Persuasion) DD 20\nFuir : Dextérité (Discrétion) DD 18\nSinon : combat",
                    issues = listOf(
                        reussite("Le dragon accepte un tribut, voire une alliance de circonstance."),
                        echec("Combat contre le dragon.", combat = true),
                        autre("Fuite", "Le groupe s'échappe, mais abandonne une partie de son matériel.")
                    ),
                    profils = listOf(monstre("Dragon rouge adulte"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Avatar d'une divinité mineure", "Un avatar divin se manifeste, sa volonté n'admet aucune contestation.", 1,
                    objectif = "Accepter l'épreuve : celle que fixe la divinité\nY résister : Sagesse DD 18",
                    issues = listOf(
                        reussite("La divinité accorde une faveur au groupe."),
                        echec("Le groupe se voit imposer un geas divin."),
                        autre("Défi", "Le groupe défie l'avatar : combat.", combat = true)
                    ),
                    profils = listOf(monstre("Planétar"))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Armée démoniaque", "Une brèche vomit une avant-garde de démons assoiffés de chaos.", 1,
                    objectif = "Refermer la brèche : Intelligence (Arcanes) DD 20\nTenir la ligne : combat contre l'avant-garde pendant ce temps",
                    issues = listOf(
                        reussite("La brèche se referme : l'avant-garde est coupée de ses renforts.", combat = true),
                        echec("La brèche s'élargit et un glabrezu la franchit.", combat = true)
                    ),
                    profils = listOf(monstre("Vrock", 2), monstre("Dretch", 6))
                ),
                evenement(
                    TypeEvenement.RENCONTRE, "Liche et ses serviteurs", "Une liche millénaire commande une horde de morts-vivants.", 1,
                    objectif = "Localiser le phylactère : Intelligence (Investigation) DD 20.",
                    issues = listOf(
                        reussite("Le phylactère est localisé : la liche peut être détruite pour de bon.", combat = true),
                        echec("La liche s'enfuit, et reviendra plus forte.")
                    ),
                    profils = listOf(monstre("Liche"), monstre("Squelette", 6), monstre("Zombi", 4))
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Cité perdue entre les plans", "Une cité entière semble flotter entre plusieurs plans d'existence.", 1,
                    objectif = "Trouver le chemin entre les plans : Intelligence (Arcanes) DD 18.",
                    issues = listOf(
                        reussite("Le groupe atteint le cœur de la cité et ses trésors."),
                        echec("Le groupe est projeté sur un plan inconnu.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Forge des dieux", "Une forge légendaire, encore chaude, façonnée par des mains divines.", 1,
                    objectif = "Utiliser la forge : outils de forgeron et Force DD 20.",
                    issues = listOf(
                        reussite("Une arme ou une armure du groupe devient magique (+1)."),
                        echec("La forge rejette le profane : 6d10 dégâts de feu.")
                    )
                ),
                evenement(
                    TypeEvenement.DECOUVERTE, "Faille vers un autre plan", "Une faille béante laisse entrevoir un plan d'existence étranger.", 1,
                    objectif = "Sceller la faille : Intelligence (Arcanes) DD 20\nOu consentir un sacrifice",
                    issues = listOf(
                        reussite("La faille se referme."),
                        echec("Des créatures d'un autre plan en surgissent.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Le voile s'amincit", "Le voile séparant les mondes s'amincit dangereusement.", 1,
                    objectif = "Trouver la cause : Intelligence (Arcanes) DD 18.",
                    issues = listOf(
                        reussite("Le groupe identifie la source du phénomène."),
                        echec("Des phénomènes surnaturels frappent la région.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Prophétie en marche", "Une ancienne prophétie semble enfin s'accomplir sous leurs yeux.", 1,
                    objectif = "Reconnaître les signes : Intelligence (Religion) DD 18.",
                    issues = listOf(
                        reussite("Le groupe comprend le rôle qu'il y joue."),
                        echec("Le groupe agit à l'aveugle.")
                    )
                ),
                evenement(
                    TypeEvenement.RUMEUR_INDICE, "Vengeance d'un dieu déchu", "Un dieu déchu chercherait à se venger de ceux qui l'ont renversé.", 1,
                    objectif = "Remonter jusqu'à ses fidèles : Intelligence (Religion) DD 18.",
                    issues = listOf(
                        reussite("Le nom du champion du dieu déchu est découvert."),
                        echec("Les fidèles du dieu repèrent le groupe.")
                    )
                ),
            )
        ),
        TableAleatoire(
            nom = "Butin — Niveau 17-20 (Légende)",
            worldId = worldId,
            type = TypeTable.LOOT,
            intervalleHeures = 4,
            entreesLoot = listOf(
                loot("Pierre de vie", 1, 1),
                loot("Pièces de platine", 3, 20, 100),
                loot("Artefact légendaire", 1, 1),
                loot("Élixir de vie éternelle", 1, 1),
                loot("Relique divine", 1, 1),
            )
        ),
    )
}

class TableAleatoireListViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TableAleatoireDependencies.newListViewModel() as T
    }
}

class TableAleatoireDetailViewModelFactory(private val tableId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TableAleatoireDependencies.newDetailViewModel(tableId) as T
    }
}
