package com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model

/**
 * Tables de noms pré-remplies par espèce (Nain, Humain, Elfe, Gnome), 50 prénoms, 50 noms et
 * 50 pseudos chacune, créées une fois par monde (voir TableAleatoireDependencies.seedTablesNoms).
 * Les chances de vide reflètent l'espèce : beaucoup d'humains n'ont pas de nom de famille,
 * presque tous les gnomes ont un surnom, etc.
 */
object TablesNomsParEspece {

    fun tables(worldId: String): List<TableAleatoire> = listOf(nains, humains, elfes, gnomes).map { modele ->
        TableAleatoire(
            nom = modele.nom,
            worldId = worldId,
            type = TypeTable.NOMS,
            prenoms = modele.prenoms,
            noms = modele.noms,
            pseudos = modele.pseudos,
            chanceVidePrenom = modele.chanceVidePrenom,
            chanceVideNom = modele.chanceVideNom,
            chanceVidePseudo = modele.chanceVidePseudo
        )
    }

    private val nains = TableAleatoire(
        nom = "Noms — Nains",
        type = TypeTable.NOMS,
        prenoms = listOf(
            "Thorin", "Balin", "Dwalin", "Gimli", "Bruenor", "Harbek", "Kildrak", "Morgran", "Orsik", "Rurik",
            "Taklinn", "Thoradin", "Ulfgar", "Vondal", "Adrik", "Baern", "Darrak", "Eberk", "Fargrim", "Gardain",
            "Brottor", "Oskar", "Travok", "Veit", "Dain", "Grimnir", "Borin", "Thrain", "Durgan", "Kazrik",
            "Amber", "Artin", "Audhild", "Bardryn", "Dagnal", "Diesa", "Eldeth", "Falkrunn", "Gunnloda", "Gurdis",
            "Helja", "Hlin", "Kathra", "Kristryd", "Ilde", "Liftrasa", "Mardred", "Riswynn", "Sannl", "Torbera"
        ),
        noms = listOf(
            "Forgefer", "Barbe-de-Pierre", "Marteau-d'Or", "Brisenclume", "Fendroc", "Poing-de-Fer", "Cœur-de-Granite", "Battlehammer", "Gorunn", "Holderhek",
            "Loderr", "Lutgehr", "Rumnaheim", "Strakeln", "Torunn", "Ungart", "Balderk", "Dankil", "Fireforge", "Frostbeard",
            "Ironfist", "Rocheval", "Mine-Profonde", "Taillegemme", "Bouclier-d'Airain", "Hachenoire", "Pierre-Lourde", "Feu-de-Forge", "Roc-Brisé", "Barbegrise",
            "Charbonnoir", "Cuirassier", "Éclat-de-Quartz", "Fer-Trempé", "Forge-Ardente", "Granitier", "Hautmont", "Lame-de-Roche", "Mithrilien", "Noirefosse",
            "Orfèvre", "Pic-d'Argent", "Profondval", "Roche-Sombre", "Sabot-de-Fer", "Taillepierre", "Tonnelourd", "Veine-d'Or", "Vieille-Enclume", "Ventre-de-Montagne"
        ),
        pseudos = listOf(
            "Barbe-de-Fer", "le Têtu", "Brise-Crâne", "Bière-Sans-Fin", "le Rancunier", "Main-de-Pierre", "le Grognon", "Œil-de-Braise", "Casse-Cailloux", "le Vieux Roc",
            "Hache-Rouge", "Tonneau", "le Taciturne", "Poing-Lourd", "Fouille-Mine", "le Borgne", "Barbe-Tressée", "Gueule-de-Forge", "le Sourd", "Deux-Haches",
            "le Sage des Profondeurs", "Ronfle-Tonnerre", "le Fier", "Pied-de-Plomb", "Trouve-Filon", "le Balafré", "Chope-Pleine", "le Gardien", "Martèle-Tout", "Nez-Cassé",
            "le Juste", "Bouclier-Vivant", "Sang-de-Granite", "le Bavard", "Crâne-Dur", "le Joaillier", "Ventre-Creux", "Fumeur-de-Pipe", "le Brave", "Grogne-Matin",
            "Tueur-d'Orcs", "le Rouge", "Barbe-Grise", "l'Ancien", "Oreille-Fendue", "le Rieur", "Coup-de-Tonnerre", "Pas-de-Pierre", "le Dernier", "Cogne-Dur"
        ),
        chanceVidePrenom = 0,
        chanceVideNom = 10,
        chanceVidePseudo = 50
    )

    private val humains = TableAleatoire(
        nom = "Noms — Humains",
        type = TypeTable.NOMS,
        prenoms = listOf(
            "Aldric", "Bertrand", "Cédric", "Dorian", "Edmond", "Gaspard", "Hugues", "Jehan", "Lancelin", "Mathis",
            "Odon", "Perceval", "Renaud", "Thibault", "Tristan", "Valentin", "Amaury", "Baudouin", "Gauvain", "Enguerrand",
            "Guillaume", "Lucien", "Marius", "Roland", "Sigismond", "Agnès", "Aliénor", "Béatrice", "Blanche", "Clémence",
            "Constance", "Éléonore", "Ermengarde", "Gisèle", "Hélène", "Isabeau", "Jeanne", "Mahaut", "Margot", "Mélisande",
            "Perrine", "Rosalind", "Sibylle", "Yolande", "Adèle", "Brunehaut", "Cunégonde", "Elvire", "Ombeline", "Viviane"
        ),
        noms = listOf(
            "Boisclair", "Dufer", "Hautval", "Lamontagne", "Lerouge", "Marchebois", "Rochefort", "Valcourt", "Beaulieu", "Châteaubrun",
            "Delorme", "Dumoulin", "Fontaine", "Fournier", "Gautier", "Lebrun", "Lefèvre", "Mercier", "Montfort", "Morel",
            "Navarre", "Perrin", "Renard", "Roussel", "Tessier", "Vasseur", "Bonnefoy", "Chevalier", "Courtois", "Desmarais",
            "Ferrand", "Garnier", "Hardi", "Lacroix", "Lenoir", "Malval", "Noirmont", "Ombreval", "Prévost", "Rivière",
            "Sauvage", "Tournelle", "Vautrin", "Blancheterre", "Corbin", "Ducastel", "Fierbois", "Grandpré", "Longchamp", "Vieillecour"
        ),
        pseudos = listOf(
            "le Hardi", "la Fouine", "Main-Leste", "le Borgne", "la Pie", "Cœur-Vaillant", "le Sage", "Deux-Lames", "la Muette", "le Balafré",
            "Pied-Léger", "le Vieux", "le Boiteux", "la Rousse", "le Pieux", "Langue-d'Argent", "le Bègue", "la Belle", "Bras-de-Fer", "le Rat",
            "le Juste", "le Sans-Terre", "la Veuve", "le Grand", "le Petit", "Œil-de-Lynx", "le Chauve", "la Sorcière", "le Loup", "Sang-Froid",
            "le Pendu", "la Lame", "le Corbeau", "Mauvaise-Graine", "le Jovial", "la Discrète", "le Marin", "Tête-Brûlée", "le Fou", "Trois-Doigts",
            "la Brute", "le Bavard", "Court-Toujours", "le Noir", "la Couronne", "Fine-Lame", "le Gros", "la Rapide", "le Damné", "Sans-Peur"
        ),
        chanceVidePrenom = 0,
        chanceVideNom = 35,
        chanceVidePseudo = 60
    )

    private val elfes = TableAleatoire(
        nom = "Noms — Elfes",
        type = TypeTable.NOMS,
        prenoms = listOf(
            "Adran", "Aelar", "Aramil", "Arannis", "Aust", "Beiro", "Berrian", "Carric", "Enialis", "Erdan",
            "Erevan", "Galinndan", "Hadarai", "Heian", "Himo", "Immeral", "Ivellios", "Laucian", "Mindartis", "Paelias",
            "Peren", "Quarion", "Riardon", "Rolen", "Soveliss", "Thamior", "Tharivol", "Theren", "Varis", "Elrion",
            "Adrie", "Althaea", "Anastrianna", "Andraste", "Antinua", "Bethrynna", "Birel", "Caelynn", "Drusilia", "Enna",
            "Felosial", "Ielenia", "Jelenneth", "Keyleth", "Leshanna", "Lia", "Meriele", "Mialee", "Naivara", "Quelenna"
        ),
        noms = listOf(
            "Amakiir", "Amastacia", "Galanodel", "Holimion", "Ilphelkiir", "Liadon", "Meliamne", "Naïlo", "Siannodel", "Xiloscient",
            "Feuillargent", "Chantelune", "Brise-d'Étoile", "Rosée-d'Aube", "Ventsylve", "Ombrefeuille", "Clairsoleil", "Fleur-de-Lune", "Pluie-d'Argent", "Murmure-des-Bois",
            "Aelthas", "Caerdonel", "Elendil", "Faelivrin", "Galathil", "Ilmarë", "Lothrandir", "Maethor", "Nimrodel", "Orophin",
            "Silvanthil", "Tauredhel", "Thalanor", "Valandil", "Yavethil", "Aube-Éternelle", "Chêne-Ancien", "Étoile-du-Soir", "Lame-Lunaire", "Saule-Pleureur",
            "Ciel-d'Été", "Écorce-d'Argent", "Brume-Matinale", "Voile-d'Étoiles", "Cristalfeuille", "Sève-d'Or", "Chant-du-Vent", "Larme-d'Ambre", "Plume-de-Cygne", "Haute-Futaie"
        ),
        pseudos = listOf(
            "Chant-du-Matin", "l'Archer", "Pas-Silencieux", "Œil-de-Faucon", "la Lune", "le Rêveur", "Flèche-Vive", "le Mélancolique", "Voix-d'Or", "le Lointain",
            "Feuille-Morte", "l'Exilé", "la Brise", "Cœur-de-Chêne", "le Sans-Âge", "Lame-Dansante", "l'Étoilé", "le Veilleur", "Ombre-Verte", "la Pâle",
            "Fils-de-l'Aube", "le Poète", "Main-Légère", "l'Errant", "Murmure", "le Hautain", "la Sentinelle", "Rire-de-Source", "le Gardien des Bois", "Pied-de-Mousse",
            "Lumière-d'Argent", "le Songeur", "la Chasseresse", "Trait-d'Azur", "le Dernier-Né", "l'Ancien", "Sourire-Triste", "le Sage", "Danse-Vent", "la Fée",
            "Regard-Clair", "le Silencieux", "Larme-de-Lune", "le Fier", "la Rose", "Arc-Long", "le Mystique", "Nuit-Calme", "le Ménestrel", "Ailes-de-Papillon"
        ),
        chanceVidePrenom = 0,
        chanceVideNom = 15,
        chanceVidePseudo = 65
    )

    private val gnomes = TableAleatoire(
        nom = "Noms — Gnomes",
        type = TypeTable.NOMS,
        prenoms = listOf(
            "Alston", "Alvyn", "Boddynock", "Brocc", "Burgell", "Dimble", "Eldon", "Erky", "Fonkin", "Frug",
            "Gerbo", "Gimble", "Glim", "Jebeddo", "Kellen", "Namfoodle", "Orryn", "Roondar", "Seebo", "Sindri",
            "Warryn", "Wrenn", "Zook", "Fizwick", "Pipkin", "Bimble", "Tinkel", "Nackle", "Dabbledob", "Wobblepot",
            "Bimpnottin", "Breena", "Caramip", "Carlin", "Donella", "Duvamil", "Ella", "Ellyjobell", "Ellywick", "Lilli",
            "Loopmottin", "Lorilla", "Mardnab", "Nissa", "Nyx", "Oda", "Orla", "Roywyn", "Shamil", "Tana"
        ),
        noms = listOf(
            "Beren", "Daergel", "Folkor", "Garrick", "Nackle", "Murnig", "Ningel", "Raulnor", "Scheppen", "Timbers",
            "Turen", "Tournevis", "Rouage", "Cliquetis", "Boulon", "Ressort", "Engrenage", "Petitpois", "Champignon", "Gribouille",
            "Bricolo", "Pétille", "Mèchecourte", "Poudrevive", "Tintamarre", "Filigrane", "Clochette", "Grelot", "Noisette", "Fouinard",
            "Bidouille", "Écrou", "Loupiotte", "Malicorne", "Pignon", "Ritournelle", "Sifflotin", "Tirebouchon", "Vapeur", "Zigzag",
            "Bulle-de-Savon", "Caillou-Brillant", "Chapeau-Pointu", "Doigts-de-Fée", "Étincelle", "Fil-de-Cuivre", "Lanterne", "Mille-Rouages", "Pot-de-Colle", "Trotte-Menu"
        ),
        pseudos = listOf(
            "Badger", "Cloak", "Doublelock", "Filchbatter", "Fnipper", "Ku", "Nim", "Oneshoe", "Pock", "Sparklegem",
            "Stumbleduck", "Pétard", "Boule-de-Gomme", "Touche-à-Tout", "Nez-Rouge", "Lunettes", "Explose-Tout", "Bricole", "Fuse-Vite", "Barbe-Bleue",
            "le Distrait", "Chapeau-Fou", "Grince-Dent", "Rire-Aigu", "le Génie", "Petit-Tonnerre", "Pas-de-Souris", "Doigts-Brûlés", "le Curieux", "Mille-Questions",
            "Sourcils-Roussis", "Tic-Tac", "Fouine-Partout", "le Farceur", "Clignote", "Trop-Bavard", "Poche-Pleine", "Mèche-Folle", "le Savant", "Boum",
            "Cligne-l'Œil", "Chausse-Trappe", "Turlututu", "Zéro-Calcul", "l'Inventeur", "Plume-Agile", "Gobe-Mouche", "Sans-Souci", "Ressort", "Grand-Projet"
        ),
        chanceVidePrenom = 0,
        chanceVideNom = 10,
        chanceVidePseudo = 25
    )
}
