package com.jc2.jdrcompagnon.feature_environnement.domain.usecase

import com.jc2.jdrcompagnon.feature_environnement.domain.model.CapaciteEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DifficulteRelative.DIFFICILE
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DifficulteRelative.FACILE
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DifficulteRelative.MOYEN
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DureeEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import com.jc2.jdrcompagnon.feature_environnement.domain.model.GraviteDegats.DANGEREUX
import com.jc2.jdrcompagnon.feature_environnement.domain.model.GraviteDegats.REVERS
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeCapacite.ACTION
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeCapacite.PASSIVE
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeCapacite.REACTION
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeEpreuve

/**
 * Catalogue d'épreuves environnementales fournies par l'app : le MJ les ajoute à ses
 * environnements (outil Environnement) puis les adapte. Les exemples d'environnements pré-remplis
 * en reçoivent déjà. Valeurs relatives uniquement, résolues au lancement selon le groupe.
 *
 * Chaque épreuve propose plusieurs Réactions (réponse immédiate à un échec précis) et Actions
 * (initiative du MJ après un échec) de natures variées — dégâts, états, perte de Progrès,
 * dilemme, adversaires — pour que deux échecs successifs ne se ressemblent pas.
 */
val catalogueEpreuves: List<EpreuveEnvironnementale> = listOf(
    EpreuveEnvironnementale(
        nom = "Tempête sur le col",
        type = TypeEpreuve.TRAVERSEE,
        description = "Un blizzard s'abat sur le passage de montagne. Le vent hurle, la neige efface le sentier et le froid mord la chair.",
        pulsions = listOf("Isoler le groupe", "Épuiser les corps", "Ensevelir"),
        competences = listOf("Survie (SAG)", "Athlétisme (FOR)", "Perception (SAG)", "Dressage (SAG)"),
        capacites = listOf(
            CapaciteEpreuve("Froid mordant", PASSIVE, "Sans vêtements chauds, chaque étape demande un jet de sauvegarde ; en cas d'échec, 1 niveau d'Épuisement.", sauvegarde = "CON", difficulte = MOYEN),
            CapaciteEpreuve("Congère", REACTION, "Sur un échec d'Athlétisme, le personnage s'enfonce dans la neige : entravé jusqu'à la fin de son prochain tour, et subit les dégâts s'il rate la sauvegarde.", sauvegarde = "DEX", difficulte = MOYEN, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Hors du sentier", REACTION, "Sur un échec de Survie, le groupe s'est trompé de versant : il perd 1 point de Progrès en revenant sur ses pas."),
            CapaciteEpreuve("Monture paniquée", REACTION, "Sur un échec de Dressage, une monture rue et s'enfuit dans la tempête avec une partie du paquetage.", question = "Qu'y avait-il d'irremplaçable dans ces sacoches ?"),
            CapaciteEpreuve("Voile blanc", ACTION, "La visibilité tombe à zéro : désavantage aux tests de Perception jusqu'au prochain succès du groupe."),
            CapaciteEpreuve("Avalanche", ACTION, "Un pan de neige se détache au-dessus du groupe.", sauvegarde = "DEX", difficulte = DIFFICILE, degats = DANGEREUX, typeDegats = "contondant", question = "Qui a entendu le grondement en premier ?"),
            CapaciteEpreuve("Rafale glaciale", ACTION, "Une bourrasque projette tout le monde au sol ; en cas d'échec, à terre et dégâts.", sauvegarde = "FOR", difficulte = MOYEN, degats = REVERS, typeDegats = "froid"),
            CapaciteEpreuve("Pont de glace", ACTION, "Le seul passage est une corniche gelée au-dessus du vide : chaque personnage doit la franchir ou trouver un détour (Progrès -1).", sauvegarde = "DEX", difficulte = MOYEN, degats = DANGEREUX, typeDegats = "contondant"),
            CapaciteEpreuve("Traces dans la neige", ACTION, "D'énormes empreintes fraîches croisent le chemin : quelque chose chasse par ce temps.", question = "Pourquoi la créature ne semble-t-elle pas gênée par la tempête ?")
        ),
        adversaires = listOf("Loup", "Yéti")
    ),
    EpreuveEnvironnementale(
        nom = "Marais fétide",
        type = TypeEpreuve.TRAVERSEE,
        description = "Eaux stagnantes, brume basse et lumières trompeuses. Chaque pas peut être le dernier sur un sol ferme.",
        pulsions = listOf("Égarer", "Engloutir", "Empoisonner lentement"),
        competences = listOf("Survie (SAG)", "Nature (INT)", "Athlétisme (FOR)", "Médecine (SAG)"),
        capacites = listOf(
            CapaciteEpreuve("Eaux croupies", PASSIVE, "Boire ou se blesser dans l'eau du marais impose un jet de sauvegarde ; en cas d'échec, empoisonné pendant 1 heure.", sauvegarde = "CON", difficulte = FACILE),
            CapaciteEpreuve("Sables mouvants", REACTION, "Sur un échec, un personnage s'enlise : entravé, il doit réussir un jet pour se dégager (un allié peut l'aider).", sauvegarde = "FOR", difficulte = MOYEN),
            CapaciteEpreuve("Sangsues", REACTION, "Sur un échec d'Athlétisme en eau profonde, des sangsues s'accrochent : dégâts à chaque étape jusqu'à ce qu'on les retire (Médecine).", degats = REVERS, typeDegats = "perforant"),
            CapaciteEpreuve("Matériel trempé", REACTION, "Sur un échec, un sac tombe à l'eau : parchemins, rations ou poudre sont gâchés."),
            CapaciteEpreuve("Nuée d'insectes", ACTION, "Un essaim de moustiques s'abat sur le groupe.", sauvegarde = "CON", difficulte = MOYEN, degats = REVERS, typeDegats = "perforant"),
            CapaciteEpreuve("Feux follets", ACTION, "Des lueurs attirent le groupe hors du sentier : sur un échec, le groupe perd 1 point de Progrès.", sauvegarde = "SAG", difficulte = MOYEN, question = "Quelle voix familière semble appeler depuis la brume ?"),
            CapaciteEpreuve("Gaz des marais", ACTION, "Une poche de gaz remonte à la surface ; une flamme nue la fait exploser.", sauvegarde = "CON", difficulte = MOYEN, degats = DANGEREUX, typeDegats = "poison"),
            CapaciteEpreuve("Fièvre des marais", ACTION, "Un personnage piqué plus tôt frissonne : en cas d'échec, désavantage aux jets de CON jusqu'à un repos long.", sauvegarde = "CON", difficulte = DIFFICILE),
            CapaciteEpreuve("Quelque chose sous l'eau", ACTION, "Une forme passe sous la barque ou entre les jambes. Un adversaire attaque par surprise.")
        ),
        adversaires = listOf("Crocodile", "Feu follet", "Sangsue géante")
    ),
    EpreuveEnvironnementale(
        nom = "Forêt labyrinthique",
        type = TypeEpreuve.EXPLORATION,
        description = "Les sentiers semblent se déplacer, les arbres se ressemblent tous et une magie ancienne brouille les repères.",
        pulsions = listOf("Désorienter", "Séparer le groupe", "Protéger son cœur"),
        competences = listOf("Survie (SAG)", "Nature (INT)", "Arcanes (INT)", "Perception (SAG)"),
        duree = DureeEpreuve.LONGUE,
        capacites = listOf(
            CapaciteEpreuve("Sentiers changeants", PASSIVE, "Un succès sans lien avec l'orientation (Survie, Nature, Arcanes) ne rapporte jamais plus d'1 point de Progrès."),
            CapaciteEpreuve("Racines traîtresses", REACTION, "Sur un échec, une racine happe une cheville.", sauvegarde = "DEX", difficulte = FACILE, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Tourner en rond", REACTION, "Sur un échec de Survie, le groupe retombe sur son propre campement de la veille : Progrès -1.", question = "Quel détail prouve qu'ils sont déjà passés ici ?"),
            CapaciteEpreuve("Égaré", REACTION, "Sur un échec de Perception, le personnage le plus en arrière perd le groupe de vue et doit le retrouver seul."),
            CapaciteEpreuve("Brume ensorcelée", ACTION, "Une brume argentée envahit le sous-bois : sur un échec, charmé et marche droit vers le cœur de la forêt.", sauvegarde = "SAG", difficulte = DIFFICILE, question = "Que voit le personnage charmé dans la brume ?"),
            CapaciteEpreuve("Prédateur à l'affût", ACTION, "Une créature suit le groupe depuis un moment et attaque le plus isolé."),
            CapaciteEpreuve("Ronces vivantes", ACTION, "Les buissons se referment sur le passage : il faut tailler (Athlétisme) ou contourner (Progrès -1).", sauvegarde = "DEX", difficulte = MOYEN, degats = REVERS, typeDegats = "tranchant"),
            CapaciteEpreuve("Gardien sylvestre", ACTION, "Un esprit de la forêt barre la route et exige une offrande ou une promesse.", question = "Que réclame-t-il, et qu'arrive-t-il si le groupe ment ?"),
            CapaciteEpreuve("Chant envoûtant", ACTION, "Une mélodie douce endort les sens.", sauvegarde = "SAG", difficulte = MOYEN)
        ),
        adversaires = listOf("Loup sanguinaire", "Araignée géante", "Dryade")
    ),
    EpreuveEnvironnementale(
        nom = "Incendie",
        type = TypeEpreuve.EVENEMENT,
        description = "Le bâtiment s'embrase. La fumée envahit les pièces et des gens sont encore à l'intérieur.",
        pulsions = listOf("Tout consumer", "Étouffer", "Couper les issues"),
        competences = listOf("Athlétisme (FOR)", "Acrobaties (DEX)", "Perception (SAG)", "Médecine (SAG)"),
        duree = DureeEpreuve.COURTE,
        menaceMax = 3,
        capacites = listOf(
            CapaciteEpreuve("Fumée étouffante", PASSIVE, "Chaque tour passé à l'intérieur sans protection impose un jet ; en cas d'échec, empoisonné jusqu'à ce qu'il respire de l'air frais.", sauvegarde = "CON", difficulte = MOYEN),
            CapaciteEpreuve("Poutre qui cède", REACTION, "Sur un échec, le plafond s'effondre en partie.", sauvegarde = "DEX", difficulte = MOYEN, degats = DANGEREUX, typeDegats = "feu"),
            CapaciteEpreuve("Brûlure", REACTION, "Sur un échec d'Acrobaties, le personnage traverse les flammes : dégâts et vêtements en feu jusqu'à ce qu'il les éteigne.", degats = REVERS, typeDegats = "feu"),
            CapaciteEpreuve("Victime paniquée", REACTION, "Sur un échec de Persuasion ou de Médecine, la personne secourue se débat et retarde la sortie (Progrès -1)."),
            CapaciteEpreuve("Embrasement", ACTION, "Une pièce entière s'enflamme d'un coup.", sauvegarde = "DEX", difficulte = DIFFICILE, degats = DANGEREUX, typeDegats = "feu"),
            CapaciteEpreuve("Appel à l'aide", ACTION, "Un cri s'élève d'une autre pièce : quelqu'un est piégé.", question = "Qui est coincé là-haut, et pourquoi compte-t-il pour le groupe ?"),
            CapaciteEpreuve("Escalier effondré", ACTION, "La seule descente disparaît : il faut sauter, grimper ou trouver une fenêtre.", sauvegarde = "DEX", difficulte = MOYEN, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Explosion", ACTION, "Un tonneau d'huile ou de poudre prend feu.", sauvegarde = "DEX", difficulte = DIFFICILE, degats = DANGEREUX, typeDegats = "feu"),
            CapaciteEpreuve("Pillards", ACTION, "Des opportunistes profitent de la confusion pour piller… et ne veulent pas de témoins.")
        ),
        adversaires = listOf("Bandit", "Méphite de magma")
    ),
    EpreuveEnvironnementale(
        nom = "Crue soudaine",
        type = TypeEpreuve.EVENEMENT,
        description = "Un grondement en amont, puis le torrent déborde : l'eau emporte tout sur son passage.",
        pulsions = listOf("Emporter", "Séparer", "Détruire le matériel"),
        competences = listOf("Athlétisme (FOR)", "Survie (SAG)", "Dressage (SAG)", "Perception (SAG)"),
        duree = DureeEpreuve.COURTE,
        capacites = listOf(
            CapaciteEpreuve("Eau glaciale", PASSIVE, "Les personnages immergés ont une vitesse réduite de moitié."),
            CapaciteEpreuve("Emporté par le courant", REACTION, "Sur un échec, le personnage est entraîné sur 6 m.", sauvegarde = "FOR", difficulte = MOYEN, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Tête sous l'eau", REACTION, "Sur un échec d'Athlétisme, le personnage boit la tasse : 1 niveau d'Épuisement s'il rate la sauvegarde.", sauvegarde = "CON", difficulte = MOYEN),
            CapaciteEpreuve("Chariot renversé", REACTION, "Sur un échec de Dressage, l'attelage verse dans le flot : provisions perdues ou à repêcher."),
            CapaciteEpreuve("Débris flottants", ACTION, "Un tronc dévale le courant vers le groupe.", sauvegarde = "DEX", difficulte = MOYEN, degats = DANGEREUX, typeDegats = "contondant"),
            CapaciteEpreuve("Rive qui s'effondre", ACTION, "Le sol cède sous les montures ou le chariot : un sac ou une monture est emporté si personne ne réagit.", question = "Quel objet précieux flotte déjà hors de portée ?"),
            CapaciteEpreuve("Tourbillon", ACTION, "Un remous aspire le personnage le plus exposé vers le fond.", sauvegarde = "FOR", difficulte = DIFFICILE, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Village en aval", ACTION, "Des cris montent de la vallée : des habitants sont menacés. Aider coûte du temps (Progrès -1), fuir coûte autre chose.", question = "Qui, dans ce village, se souviendra du choix du groupe ?"),
            CapaciteEpreuve("Seconde vague", ACTION, "Un nouveau grondement : le niveau monte encore. Menace -1 si le groupe n'a pas atteint un point haut.")
        ),
        adversaires = listOf("Crocodile", "Élémentaire de l'eau")
    ),
    EpreuveEnvironnementale(
        nom = "Émeute en ville",
        type = TypeEpreuve.SOCIALE,
        description = "La colère gronde sur la place : la foule s'agite, les pavés volent et la garde se prépare à charger.",
        pulsions = listOf("Désigner un coupable", "Tout casser", "Emporter les indécis"),
        competences = listOf("Persuasion (CHA)", "Intimidation (CHA)", "Perspicacité (SAG)", "Athlétisme (FOR)", "Tromperie (CHA)"),
        capacites = listOf(
            CapaciteEpreuve("Foule compacte", PASSIVE, "La place est un terrain difficile ; impossible de courir sans bousculer quelqu'un."),
            CapaciteEpreuve("Bousculade", REACTION, "Sur un échec, le personnage est renversé et piétiné.", sauvegarde = "DEX", difficulte = FACILE, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Parole mal prise", REACTION, "Sur un échec de Persuasion, la foule comprend l'inverse : le prochain test social se fait avec désavantage."),
            CapaciteEpreuve("Pickpocket", REACTION, "Sur un échec de Perspicacité ou de Perception, une bourse ou un objet disparaît dans la cohue."),
            CapaciteEpreuve("Meneur enragé", ACTION, "Un agitateur prend le groupe pour cible et retourne la foule contre lui : le prochain test social se fait avec désavantage.", question = "Que reproche-t-il au groupe, et a-t-il raison ?"),
            CapaciteEpreuve("La garde charge", ACTION, "Les gardes avancent en ligne, matraques levées, sans distinguer émeutiers et innocents.", sauvegarde = "DEX", difficulte = MOYEN, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Pluie de pavés", ACTION, "Des projectiles volent depuis les toits.", sauvegarde = "DEX", difficulte = MOYEN, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Enfant perdu", ACTION, "Un enfant pleure au milieu de la place, sur le point d'être écrasé.", question = "L'aider fait perdre du temps : qui s'en charge ?"),
            CapaciteEpreuve("Incendie déclenché", ACTION, "Une échoppe prend feu : la panique redouble (Menace -1 si personne n'intervient).")
        ),
        adversaires = listOf("Garde", "Bandit", "Roturier", "Chef bandit")
    ),
    EpreuveEnvironnementale(
        nom = "Poursuite dans les ruelles",
        type = TypeEpreuve.TRAVERSEE,
        description = "La cible file entre les étals, saute sur un toit, disparaît dans une venelle. Il faut la rattraper avant qu'elle ne se volatilise.",
        pulsions = listOf("Semer les poursuivants", "Multiplier les obstacles", "Attirer dans un piège"),
        competences = listOf("Athlétisme (FOR)", "Acrobaties (DEX)", "Perception (SAG)", "Discrétion (DEX)", "Investigation (INT)"),
        duree = DureeEpreuve.COURTE,
        menaceMax = 3,
        capacites = listOf(
            CapaciteEpreuve("Ville vivante", PASSIVE, "Chaque sprint au milieu de la foule impose un choix : ralentir, ou bousculer quelqu'un qui s'en souviendra."),
            CapaciteEpreuve("Obstacle", REACTION, "Sur un échec, un chariot ou un étal renversé barre le passage.", sauvegarde = "DEX", difficulte = MOYEN, degats = REVERS, typeDegats = "contondant"),
            CapaciteEpreuve("Point de côté", REACTION, "Sur un échec d'Athlétisme, le personnage s'essouffle : 1 niveau d'Épuisement s'il rate la sauvegarde.", sauvegarde = "CON", difficulte = FACILE),
            CapaciteEpreuve("Fausse piste", REACTION, "Sur un échec de Perception ou d'Investigation, le groupe suit un sosie de la cible : Progrès -1."),
            CapaciteEpreuve("Toits glissants", ACTION, "Les tuiles cèdent sous les pas du poursuivant de tête.", sauvegarde = "DEX", difficulte = DIFFICILE, degats = DANGEREUX, typeDegats = "contondant"),
            CapaciteEpreuve("Complice", ACTION, "Un complice de la cible surgit d'une porte pour ralentir le groupe.", question = "Qui aide la cible, et que lui a-t-elle promis ?"),
            CapaciteEpreuve("Garde zélé", ACTION, "Un garde prend le groupe pour les fautifs et exige des explications (Persuasion ou fuite)."),
            CapaciteEpreuve("Impasse", ACTION, "La ruelle est sans issue… ou c'est un piège : des silhouettes bloquent l'entrée derrière le groupe."),
            CapaciteEpreuve("La cible lâche quelque chose", ACTION, "Un objet tombe de sa poche : le ramasser coûte du temps (Progrès -1), l'ignorer peut coûter plus cher.", question = "Qu'est-ce que c'est, et pourquoi la cible y tenait-elle ?")
        ),
        adversaires = listOf("Espion", "Bandit", "Garde")
    ),
    EpreuveEnvironnementale(
        nom = "Galeries effondrées",
        type = TypeEpreuve.EXPLORATION,
        description = "Un réseau de tunnels à moitié écroulés. L'air est lourd, la roche craque et quelque chose gratte au loin.",
        pulsions = listOf("Enterrer vivant", "Étouffer", "Éveiller ce qui dort"),
        competences = listOf("Athlétisme (FOR)", "Investigation (INT)", "Perception (SAG)", "Survie (SAG)"),
        duree = DureeEpreuve.LONGUE,
        capacites = listOf(
            CapaciteEpreuve("Obscurité totale", PASSIVE, "Sans source de lumière ni vision dans le noir, tous les tests de Perception visuelle échouent automatiquement."),
            CapaciteEpreuve("Éboulement", REACTION, "Sur un échec, la voûte s'effondre.", sauvegarde = "DEX", difficulte = MOYEN, degats = DANGEREUX, typeDegats = "contondant"),
            CapaciteEpreuve("Boyau étroit", REACTION, "Sur un échec d'Athlétisme ou d'Acrobaties, un personnage reste coincé : entravé jusqu'à ce qu'on le dégage."),
            CapaciteEpreuve("Torche soufflée", REACTION, "Sur un échec, un courant d'air éteint la lumière : aveuglé jusqu'à la rallumer."),
            CapaciteEpreuve("Poche de gaz", ACTION, "Un gaz nauséabond s'échappe d'une fissure.", sauvegarde = "CON", difficulte = DIFFICILE, degats = REVERS, typeDegats = "poison"),
            CapaciteEpreuve("Grondement", ACTION, "Le vacarme a attiré des habitants des profondeurs.", question = "Quelle trace montre qu'ils sont là depuis longtemps ?"),
            CapaciteEpreuve("Galerie inondée", ACTION, "Le seul passage est sous l'eau noire : il faut retenir son souffle ou trouver un autre chemin (Progrès -1).", sauvegarde = "CON", difficulte = MOYEN),
            CapaciteEpreuve("Sol qui cède", ACTION, "Le plancher de la galerie s'ouvre sur un puits.", sauvegarde = "DEX", difficulte = DIFFICILE, degats = DANGEREUX, typeDegats = "contondant"),
            CapaciteEpreuve("Air vicié", ACTION, "L'oxygène se raréfie : 1 niveau d'Épuisement pour chacun en cas d'échec.", sauvegarde = "CON", difficulte = MOYEN)
        ),
        adversaires = listOf("Gobelin", "Ankheg", "Enlaceur")
    )
)
