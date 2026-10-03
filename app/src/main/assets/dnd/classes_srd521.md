# Classes — SRD 5.2.1

Ce fichier recense les 12 classes du Document de Référence du Système (D&D 5e, SRD 5.2.1), au format structuré pour import dans JDR Compagnon. Chaque classe comprend ses traits de base, sa table de progression, ses aptitudes niveau par niveau et une sous-classe (celle fournie par le SRD). Les listes de sorts complètes ne sont pas reproduites ici : elles sont gérées par le module Bibliothèque (sorts.md).

---

## Barbare

<!-- id: barbare -->

### Traits de base

- desclas : Guerrier tribal qui puise sa force dans une rage primale, encaissant les coups pour mieux les rendre.
- caracprinc : Force
- difficulte : Facile
- dv : d12 par niveau
- jssauv : Force et Constitution
- maitcompA : 2 au choix parmi Athlétisme, Dressage, Intimidation, Nature, Perception, Survie
- maitarme : Armes courantes et armes de guerre
- formarm : Armures légères, intermédiaires et boucliers
- equipdepA : Hache à deux mains, 4 hachettes, paquetage d'explorateur et 15 po
- equipdepB : 75 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Rages | Dégâts de Rage | Bottes d'arme |
|---|---|---|---|---|---|
| 1 | +2 | Rage, Bottes d'arme, Défense sans armure | 2 | +2 | 2 |
| 2 | +2 | Sens du danger, Témérité | 2 | +2 | 2 |
| 3 | +2 | Savoir primal, Sous-classe de Barbare | 3 | +2 | 2 |
| 4 | +2 | Amélioration de caractéristique | 3 | +2 | 3 |
| 5 | +3 | Attaque supplémentaire, Déplacement rapide | 3 | +2 | 3 |
| 6 | +3 | Aptitude de sous-classe | 4 | +2 | 3 |
| 7 | +3 | Bond instinctif, Instinct sauvage | 4 | +2 | 3 |
| 8 | +3 | Amélioration de caractéristique | 4 | +2 | 3 |
| 9 | +4 | Coup brutal | 4 | +3 | 3 |
| 10 | +4 | Aptitude de sous-classe | 4 | +3 | 4 |
| 11 | +4 | Rage implacable | 4 | +3 | 4 |
| 12 | +4 | Amélioration de caractéristique | 5 | +3 | 4 |
| 13 | +5 | Coup brutal amélioré | 5 | +3 | 4 |
| 14 | +5 | Aptitude de sous-classe | 5 | +3 | 4 |
| 15 | +5 | Rage persistante | 5 | +3 | 4 |
| 16 | +5 | Amélioration de caractéristique | 5 | +4 | 4 |
| 17 | +6 | Coup brutal amélioré | 6 | +4 | 4 |
| 18 | +6 | Puissance indomptable | 6 | +4 | 4 |
| 19 | +6 | Faveur épique | 6 | +4 | 4 |
| 20 | +6 | Champion primitif | 6 | +4 | 4 |

### Aptitudes de classe

#### Niveau 1 : Rage
<!-- id: rage; type: automatique -->
Par une action Bonus (si vous ne portez pas d'armure lourde), entrez en Rage : Résistance aux dégâts contondants, perforants et tranchants ; bonus aux dégâts d'attaques basées sur la Force (voir colonne Dégâts de Rage) ; Avantage aux tests et JS de Force ; impossibilité de maintenir la Concentration ou de lancer des sorts. La Rage dure jusqu'à la fin du tour suivant, prolongeable par une attaque, un JS imposé à un ennemi ou une action Bonus (max. 10 minutes). Elle prend fin si vous portez une armure lourde ou subissez l'état Neutralisé.

#### Niveau 1 : Défense sans armure
<!-- id: defense-sans-armure; type: automatique -->
Sans armure, CA = 10 + mod. Dextérité + mod. Constitution (cumulable avec un bouclier).

#### Niveau 1 : Bottes d'arme
<!-- id: bottes-d-arme; type: automatique -->
Vous pouvez recourir à la botte de deux types d'armes courantes/de guerre de corps à corps. Modifiable à chaque Repos long.

#### Niveau 2 : Sens du danger
<!-- id: sens-du-danger; type: automatique -->
Avantage aux JS de Dextérité (sauf si Neutralisé).

#### Niveau 2 : Témérité
<!-- id: temerite; type: automatique -->
Au premier jet d'attaque du tour, Avantage aux attaques basées sur la Force jusqu'au début du tour suivant, mais les attaques contre vous ont aussi l'Avantage.

#### Niveau 3 : Savoir primal
<!-- id: savoir-primal; type: automatique -->
Maîtrise d'une compétence supplémentaire. Tant que la Rage est active, vous pouvez utiliser la Force à la place de la caractéristique normale pour Acrobaties, Discrétion, Intimidation, Perception et Survie.

#### Niveau 3 : Sous-classe de Barbare
<!-- id: sous-classe-de-barbare; type: choix-sousclasse -->
Choix de la sous-classe (Voie du Berserker dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->
Don Amélioration de caractéristique ou autre don pour lequel les prérequis sont remplis.

#### Niveau 5 : Attaque supplémentaire
<!-- id: attaque-supplementaire; type: automatique -->
Deux attaques au lieu d'une avec l'action Attaque.

#### Niveau 5 : Déplacement rapide
<!-- id: deplacement-rapide; type: automatique -->
Vitesse +3 m sans armure lourde.

#### Niveau 7 : Instinct sauvage
<!-- id: instinct-sauvage; type: automatique -->
Avantage aux jets d'Initiative.

#### Niveau 7 : Bond instinctif
<!-- id: bond-instinctif; type: automatique -->
Déplacement d'une demi-Vitesse dans le cadre de l'action Bonus d'entrée en Rage.

#### Niveau 9 : Coup brutal
<!-- id: coup-brutal; type: choix-effet -->
En renonçant à l'Avantage de Témérité, infligez 1d10 dégâts supplémentaires et un effet au choix :
- **Coup appuyé** <!-- id: coup-appuye --> : repousse de 4,50 m.
- **Coup au jarret** <!-- id: coup-au-jarret --> : Vitesse réduite de 4,50 m.

#### Niveau 11 : Rage implacable
<!-- id: rage-implacable; type: automatique -->
Si vous tombez à 0 pv en Rage sans mourir sur le coup, JS Constitution DD 10 (croît de 5 à chaque réutilisation) : succès = pv égaux au double du niveau de Barbare.

#### Niveau 13, 17 : Coup brutal amélioré
<!-- id: coup-brutal-ameliore; type: automatique -->
Nouvelles options (Coup stupéfiant, Coup déchirant), dégâts supplémentaires accrus à 2d10 au niveau 17.

#### Niveau 15 : Rage persistante
<!-- id: rage-persistante; type: automatique -->
Récupération de toutes les utilisations de Rage à l'Initiative ; la Rage dure 10 minutes sans prolongation active.

#### Niveau 18 : Puissance indomptable
<!-- id: puissance-indomptable; type: automatique -->
Un test/JS de Force raté peut utiliser la valeur de Force brute à la place.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Don de Faveur épique (Faveur d'attaque irrésistible recommandé).

#### Niveau 20 : Champion primitif
<!-- id: champion-primitif; type: automatique -->
Force et Constitution +4 (max. 25).

### Multiclassage

- multiarmures : Boucliers
- multiarmes : Armes courantes et armes de guerre

### Sous-classe : Voie du Berserker

<!-- id: voie-du-berserker -->

- description : Guerrier furieux qui canalise sa Rage en violence brute : dégâts supplémentaires, immunité à la peur et au charme, et présence terrifiante.

#### Niveau 3 : Frénésie
<!-- id: frenesie; type: automatique -->
Pendant la Rage, en utilisant Témérité : dégâts supplémentaires (1d6 par point de bonus de Rage aux dégâts) sur la première cible touchée par une attaque utilisant la Force.

#### Niveau 6 : Rage aveugle
<!-- id: rage-aveugle; type: automatique -->
Immunité aux états Charmé et Effrayé pendant la Rage ; ces états prennent fin en entrant en Rage.

#### Niveau 10 : Représailles
<!-- id: represailles; type: automatique -->
Réaction après avoir subi des dégâts d'une créature à 1,50 m : attaque au corps à corps contre elle.

#### Niveau 14 : Présence intimidante
<!-- id: presence-intimidante; type: automatique -->
Action Bonus : JS de Sagesse (DD 8 + mod. Force + bonus de maîtrise) des créatures choisies dans une Émanation de 9 m, sinon Effrayées 1 minute. 1/Repos long (ou une utilisation de Rage).

---

## Barde

<!-- id: barde -->

### Traits de base

- desclas : Artiste itinérant qui tisse une magie inspirante à travers musique, récits et éloquence.
- caracprinc : Charisme
- difficulte : Difficile
- dv : d8 par niveau
- jssauv : Dextérité et Charisme
- maitcompA : 3 au choix
- maitarme : Armes courantes
- maitoutils : 3 instruments de musique au choix
<!-- id: barde-instruments; choix: 3; effet: outils; options: equipement: Instrument de musique -->
- formarm : Armures légères
- typeincantation : complet
- equipdepA : Armure de cuir, 2 dagues, instrument de musique au choix, paquetage d'artiste et 19 po
<!-- id: barde-instrument-equipement; choix: 1; effet: equipement; options: equipement: Instrument de musique; remplace: instrument de musique au choix; libelle: Instrument de l'équipement A -->

- equipdepB : 90 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Dé bardique | Sorts mineurs | Sorts préparés |
|---|---|---|---|---|---|
| 1 | +2 | Inspiration bardique, Sorts | d6 | 2 | 4 |
| 2 | +2 | Expertise, Touche-à-tout | d6 | 2 | 5 |
| 3 | +2 | Sous-classe de Barde | d6 | 2 | 6 |
| 4 | +2 | Amélioration de caractéristique | d6 | 3 | 7 |
| 5 | +3 | Source d'inspiration | d8 | 3 | 9 |
| 6 | +3 | Aptitude de sous-classe | d8 | 3 | 10 |
| 7 | +3 | Contre-charme | d8 | 3 | 11 |
| 8 | +3 | Amélioration de caractéristique | d8 | 3 | 12 |
| 9 | +4 | Expertise | d8 | 3 | 14 |
| 10 | +4 | Secrets magiques | d10 | 4 | 15 |
| 11 | +4 | — | d10 | 4 | 16 |
| 12 | +4 | Amélioration de caractéristique | d10 | 4 | 16 |
| 13 | +5 | — | d10 | 4 | 17 |
| 14 | +5 | Aptitude de sous-classe | d10 | 4 | 17 |
| 15 | +5 | — | d12 | 4 | 18 |
| 16 | +5 | Amélioration de caractéristique | d12 | 4 | 18 |
| 17 | +6 | — | d12 | 4 | 19 |
| 18 | +6 | Inspiration supérieure | d12 | 4 | 20 |
| 19 | +6 | Faveur épique | d12 | 4 | 21 |
| 20 | +6 | Verbe de la création | d12 | 4 | 22 |

### Aptitudes de classe

#### Niveau 1 : Inspiration bardique
<!-- id: inspiration-bardique; type: automatique -->
Par action Bonus, donnez un dé d'Inspiration bardique (d6, évoluant à d8 niv. 5, d10 niv. 10, d12 niv. 15) à une créature dans un rayon de 18 m. Une fois dans l'heure, elle peut l'ajouter à un Test d20 raté. Utilisations = mod. Charisme (min. 1), récupérées à un Repos long.

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
Caractéristique d'incantation : Charisme. Sorts mineurs et sorts préparés du 1er niveau et plus selon la table.

#### Niveau 2 : Expertise
<!-- id: expertise; type: choix-expertise-2 -->
Expertise dans deux maîtrises de compétence (deux de plus au niveau 9).

#### Niveau 2 : Touche-à-tout
<!-- id: touche-a-tout; type: automatique -->
Ajout de la moitié du bonus de maîtrise (arrondi à l'inférieur) à tout test de caractéristique sans maîtrise applicable.

#### Niveau 3 : Sous-classe de Barde
<!-- id: sous-classe-de-barde; type: choix-sousclasse -->
Choix de la sous-classe (Collège du Savoir dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Source d'inspiration
<!-- id: source-d-inspiration; type: automatique -->
Récupération totale de l'Inspiration bardique à un Repos court ou long ; possibilité de dépenser un emplacement de sort pour récupérer une utilisation.

#### Niveau 7 : Contre-charme
<!-- id: contre-charme; type: automatique -->
Réaction pour rejouer avec Avantage un JS raté contre Charmé/Effrayé (vous ou une créature dans un rayon de 9 m).

#### Niveau 9 : Expertise (bis)
<!-- id: expertise-bis; type: choix-expertise-2 -->
Deux compétences supplémentaires avec Expertise.

#### Niveau 10 : Secrets magiques
<!-- id: secrets-magiques; type: automatique -->
Accès à des sorts de Barde, Clerc, Druide ou Magicien lors de l'augmentation des sorts préparés.

#### Niveau 14 : Compétence hors pair
<!-- id: competence-hors-pair; type: automatique -->
Dépense d'Inspiration bardique pour rejouer un test/attaque raté.

#### Niveau 18 : Inspiration supérieure
<!-- id: inspiration-superieure; type: automatique -->
Récupération d'Inspiration bardique (jusqu'à 2) à l'Initiative.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur de mémoire magique recommandé.

#### Niveau 20 : Verbe de la création
<!-- id: verbe-de-la-creation; type: automatique -->
mot de pouvoir guérisseur et mot de pouvoir mortel toujours préparés ; ciblage d'une seconde créature dans un rayon de 3 m.

### Multiclassage

- multiarmures : Armures légères
- multioutils : 1 instrument de musique au choix
<!-- id: barde-multi-instrument; choix: 1; effet: outils; options: equipement: Instrument de musique -->
- maitcompB : 1 compétence au choix

### Sous-classe : Collège du Savoir

<!-- id: college-du-savoir -->

- description : Érudit polyvalent qui collectionne compétences et secrets magiques : maîtrises supplémentaires, sorts d'autres classes et mots qui déstabilisent l'ennemi.

#### Niveau 3 : Maîtrises supplémentaires
<!-- id: maitrises-supplementaires; type: automatique -->
<!-- id: maitrises-supplementaires-competences; choix: 3; effet: competences; options: toutes -->
Maîtrise de trois compétences au choix.

#### Niveau 3 : Mots cinglants
<!-- id: mots-cinglants; type: automatique -->
Réaction : dépense d'une Inspiration bardique pour soustraire le dé au jet de dégâts, au test de caractéristique ou au jet d'attaque d'une créature visible dans un rayon de 18 m.

#### Niveau 6 : Découvertes magiques
<!-- id: decouvertes-magiques; type: automatique -->
Deux sorts au choix des listes de Clerc, Druide ou Magicien (sorts mineurs ou d'un niveau pour lequel vous avez des emplacements), toujours préparés et comptant comme sorts de Barde.

#### Niveau 14 : Compétence hors pair
<!-- id: competence-hors-pair; type: automatique -->
Après un test de caractéristique ou un jet d'attaque raté, dépense d'une Inspiration bardique pour ajouter le dé au jet ; elle n'est pas perdue si le jet échoue toujours.

---

## Clerc

<!-- id: clerc -->

### Traits de base

- desclas : Intermédiaire entre le monde mortel et le divin, canalisant la puissance de son dieu pour soigner ou punir.
- caracprinc : Sagesse
- difficulte : Intermédiaire
- dv : d8 par niveau
- jssauv : Sagesse et Charisme
- maitcompA : 2 au choix parmi Histoire, Intuition, Médecine, Persuasion, Religion
- maitarme : Armes courantes
- formarm : Armures légères, intermédiaires et boucliers
- typeincantation : complet
- equipdepA : Chemise de mailles, bouclier, masse d'armes, symbole sacré, paquetage d'ecclésiastique et 7 po
- equipdepB : 110 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Conduit divin | Sorts mineurs | Sorts préparés |
|---|---|---|---|---|---|
| 1 | +2 | Sorts, Ordre divin | — | 3 | 4 |
| 2 | +2 | Conduit divin | 2 | 3 | 5 |
| 3 | +2 | Sous-classe de Clerc | 2 | 3 | 6 |
| 4 | +2 | Amélioration de caractéristique | 2 | 4 | 7 |
| 5 | +3 | Calcination de Mort-vivant | 2 | 4 | 9 |
| 6 | +3 | Aptitude de sous-classe | 3 | 4 | 10 |
| 7 | +3 | Impacts bénis | 3 | 4 | 11 |
| 8 | +3 | Amélioration de caractéristique | 3 | 4 | 12 |
| 9 | +4 | — | 3 | 4 | 14 |
| 10 | +4 | Intervention divine | 3 | 5 | 15 |
| 11 | +4 | — | 3 | 5 | 16 |
| 12 | +4 | Amélioration de caractéristique | 3 | 5 | 16 |
| 13 | +5 | — | 3 | 5 | 17 |
| 14 | +5 | Impacts bénis améliorés | 3 | 5 | 17 |
| 15 | +5 | — | 3 | 5 | 18 |
| 16 | +5 | Amélioration de caractéristique | 3 | 5 | 18 |
| 17 | +6 | Aptitude de sous-classe | 3 | 5 | 19 |
| 18 | +6 | — | 4 | 5 | 20 |
| 19 | +6 | Faveur épique | 4 | 5 | 21 |
| 20 | +6 | Intervention divine suprême | 4 | 5 | 22 |

### Aptitudes de classe

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
Caractéristique d'incantation : Sagesse. Sorts mineurs et sorts préparés selon la table.

#### Niveau 1 : Ordre divin
<!-- id: ordre-divin; type: choix-effet -->
Choix entre deux ordres :
- **Protecteur** <!-- id: protecteur --> : maîtrise des armes de guerre, formation aux armures lourdes.
- **Thaumaturge** <!-- id: thaumaturge --> : sort mineur supplémentaire, bonus aux tests d'Intelligence (Arcanes/Religion) égal au mod. de Sagesse (min. +1).

#### Niveau 2 : Conduit divin
<!-- id: conduit-divin; type: automatique -->
2 utilisations (récupération : 1 à un Repos court, toutes à un Repos long). Effets de base :
- **Étincelle divine :** action Magie, 1d8 + mod. Sagesse en soins ou en dégâts nécrotiques/radiants (JS Constitution) à une créature à 9 m. Dé supplémentaire aux niveaux 7, 13, 18.
- **Renvoi des morts-vivants :** action Magie, JS Sagesse pour les Morts-vivants dans un rayon de 9 m (échec = Effrayé et Neutralisé 1 minute).

#### Niveau 3 : Sous-classe de Clerc
<!-- id: sous-classe-de-clerc; type: choix-sousclasse -->
Choix de la sous-classe (Domaine de la Vie dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Calcination de Mort-vivant
<!-- id: calcination-de-mort-vivant; type: automatique -->
Dégâts radiants supplémentaires (dés = mod. Sagesse) lors d'un Renvoi des morts-vivants.

#### Niveau 7 : Impacts bénis
<!-- id: impacts-benis; type: choix-effet -->
Choix entre deux effets :
- **Impact divin** <!-- id: impact-divin --> : 1d8 dégâts nécrotiques/radiants supplémentaires une fois par tour.
- **Incantation puissante** <!-- id: incantation-puissante-clerc --> : mod. Sagesse ajouté aux dégâts des sorts mineurs.

#### Niveau 10 : Intervention divine
<!-- id: intervention-divine; type: automatique -->
Action Magie : lancer un sort de Clerc du 5e niveau ou moins sans emplacement ni composante matérielle. Un Repos long requis pour réutiliser.

#### Niveau 14 : Impacts bénis améliorés
<!-- id: impacts-benis-ameliores; type: automatique -->
Impact divin passe à 2d8 ; ou Incantation puissante permet d'octroyer des pv temporaires (double du mod. Sagesse).

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur du destin recommandé.

#### Niveau 20 : Intervention divine suprême
<!-- id: intervention-divine-supreme; type: automatique -->
Intervention divine peut désormais choisir souhait (cooldown : 2d4 Repos longs).

### Multiclassage

- multiarmures : Armures légères, intermédiaires et boucliers

### Sous-classe : Domaine de la Vie

<!-- id: domaine-de-la-vie -->

- description : Soigneur par excellence : chaque sort de soin rend davantage de points de vie, jusqu'à des soins toujours maximaux.

| Niveau de Clerc | Sorts |
|---|---|
| 3 | Aide, Bénédiction, Soins, Restauration partielle |
| 5 | Mot de guérison de groupe, Retour à la vie |
| 7 | Aura de vie, Protection contre la mort |
| 9 | Restauration suprême, Soins de groupe |

#### Niveau 3 : Disciple de la vie
<!-- id: disciple-de-la-vie; type: automatique -->
Un sort qui rend des points de vie en rend 2 + le niveau de l'emplacement de sort en plus.

#### Niveau 3 : Sorts du Domaine de la Vie
<!-- id: sorts-domaine-de-la-vie; type: automatique -->
Sorts toujours préparés selon le niveau de Clerc (voir la table).

#### Niveau 3 : Préservation de la vie
<!-- id: preservation-de-la-vie; type: automatique -->
Conduit divin (action Magie) : répartit 5 × votre niveau de Clerc points de vie entre des créatures En péril d'une Émanation de 9 m, sans dépasser la moitié de leur maximum.

#### Niveau 6 : Guérisseur béni
<!-- id: guerisseur-beni; type: automatique -->
Quand un de vos sorts soigne une autre créature, vous récupérez 2 + le niveau de l'emplacement de sort points de vie.

#### Niveau 17 : Guérison suprême
<!-- id: guerison-supreme; type: automatique -->
Les dés de soins de vos sorts et du Conduit divin donnent automatiquement leur valeur maximale.

---

## Druide

<!-- id: druide -->

### Traits de base

- desclas : Gardien de la nature, capable de se transformer en animal et de manier les éléments sauvages.
- caracprinc : Sagesse
- difficulte : Difficile
- dv : d8 par niveau
- jssauv : Intelligence et Sagesse
- maitcompA : 2 au choix parmi Arcanes, Dressage, Intuition, Médecine, Nature, Perception, Religion, Survie
- maitarme : Armes courantes
- maitoutils : Matériel d'herboriste
- formarm : Armures légères et boucliers
- typeincantation : complet
- equipdepA : Armure de cuir, bouclier, serpe, focaliseur druidique (bâton de combat), paquetage d'explorateur, matériel d'herboriste et 9 po
- equipdepB : 50 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Forme sauvage | Sorts mineurs | Sorts préparés |
|---|---|---|---|---|---|
| 1 | +2 | Sorts, Druidique, Ordre primitif | — | 2 | 4 |
| 2 | +2 | Compagnon sauvage, Forme sauvage | 2 | 2 | 5 |
| 3 | +2 | Sous-classe de Druide | 2 | 2 | 6 |
| 4 | +2 | Amélioration de caractéristique | 2 | 3 | 7 |
| 5 | +3 | Regain sauvage | 2 | 3 | 9 |
| 6 | +3 | Aptitude de sous-classe | 3 | 3 | 10 |
| 7 | +3 | Fureur élémentaire | 3 | 3 | 11 |
| 8 | +3 | Amélioration de caractéristique | 3 | 3 | 12 |
| 9 | +4 | — | 3 | 3 | 14 |
| 10 | +4 | Aptitude de sous-classe | 3 | 4 | 15 |
| 11 | +4 | — | 3 | 4 | 16 |
| 12 | +4 | Amélioration de caractéristique | 3 | 4 | 16 |
| 13 | +5 | — | 3 | 4 | 17 |
| 14 | +5 | Aptitude de sous-classe | 3 | 4 | 17 |
| 15 | +5 | Fureur élémentaire améliorée | 3 | 4 | 18 |
| 16 | +5 | Amélioration de caractéristique | 3 | 4 | 18 |
| 17 | +6 | — | 4 | 4 | 19 |
| 18 | +6 | Incantation animale | 4 | 4 | 20 |
| 19 | +6 | Faveur épique | 4 | 4 | 21 |
| 20 | +6 | Archidruide | 4 | 4 | 22 |

### Aptitudes de classe

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
Caractéristique d'incantation : Sagesse. Sorts mineurs et sorts préparés selon la table.

#### Niveau 1 : Druidique
<!-- id: druidique; type: automatique -->
Langue secrète du Druide ; communication avec les animaux toujours préparé.

#### Niveau 1 : Ordre primitif
<!-- id: ordre-primitif; type: choix-effet -->
Choix entre deux ordres :
- **Mage** <!-- id: mage --> : sort mineur supplémentaire, bonus Arcanes/Nature égal au mod. Sagesse (min. +1).
- **Gardien** <!-- id: gardien --> : maîtrise armes de guerre, formation armures intermédiaires.

#### Niveau 2 : Forme sauvage
<!-- id: forme-sauvage; type: automatique -->
Action Bonus : métamorphose en Bête (FP ≤ 1/4 au niveau 2, sans Vitesse de vol) pour un nombre d'heures égal à la moitié du niveau de Druide. 2 utilisations de base (voir colonne dédiée pour la progression). pv temporaires = niveau de Druide à la transformation ; profil remplacé sauf type, pv, DV, Int/Sag/Cha, aptitudes de classe, langues et dons.

#### Niveau 2 : Compagnon sauvage
<!-- id: compagnon-sauvage; type: automatique -->
Action Magie : dépense d'un emplacement ou d'une utilisation de Forme sauvage pour lancer appel de familier (Fée, disparaît à un Repos long).

#### Niveau 3 : Sous-classe de Druide
<!-- id: sous-classe-de-druide; type: choix-sousclasse -->
Choix de la sous-classe (Cercle de la Terre dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Regain sauvage
<!-- id: regain-sauvage; type: automatique -->
Dépense d'un emplacement de sort pour récupérer une utilisation de Forme sauvage ; ou dépense d'une utilisation de Forme sauvage pour un emplacement du 1er niveau (1/Repos long).

#### Niveau 7, 15 : Fureur élémentaire (améliorée)
<!-- id: fureur-elementaire-amelioree; type: choix-effet -->
Choix entre deux effets :
- **Incantation puissante** <!-- id: incantation-puissante-druide --> : mod. Sagesse ajouté aux dégâts des sorts mineurs, portée +90 m au niv. 15.
- **Attaques primitives** <!-- id: attaques-primitives --> : 1d8 dégâts élémentaires supplémentaires, 2d8 au niv. 15.

#### Niveau 18 : Incantation animale
<!-- id: incantation-animale; type: automatique -->
Possibilité de lancer des sorts sous Forme sauvage (hors composantes matérielles coûteuses/consommées).

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur de Déplacement dimensionnel recommandé.

#### Niveau 20 : Archidruide
<!-- id: archidruide; type: automatique -->
Récupération automatique d'une utilisation de Forme sauvage à l'Initiative ; conversion d'utilisations de Forme sauvage en emplacement de sort (2 niveaux par utilisation) ; longévité (vieillissement ralenti).

### Multiclassage

- multiarmures : Armures légères et boucliers

### Sous-classe : Cercle de la Terre

<!-- id: cercle-de-la-terre -->

- description : Druide lié à un type de terre (Aride, Polaire, Tempéré, Tropical) qui en tire ses sorts, récupère sa magie et devient un sanctuaire vivant.

#### Niveau 3 : Sorts du cercle
<!-- id: sorts-du-cercle; type: automatique -->
À chaque Repos long, choix d'un type de terre (Aride, Polaire, Tempéré ou Tropical) : ses sorts de cercle sont toujours préparés selon votre niveau de Druide.

#### Niveau 3 : Aide de la terre
<!-- id: aide-de-la-terre; type: automatique -->
Action Magie, une utilisation de Forme sauvage : sphère de 3 m de rayon à 18 m, 2d6 dégâts nécrotiques (JS Constitution) aux créatures choisies et 2d6 points de vie rendus à une créature. Les dés augmentent aux niveaux 10 et 14.

#### Niveau 6 : Récupération naturelle
<!-- id: recuperation-naturelle; type: automatique -->
Un sort de cercle lancé sans emplacement 1/Repos long ; à un Repos court, récupération d'emplacements de sort (somme ≤ moitié du niveau de Druide, aucun du 6e niveau ou plus) 1/Repos long.

#### Niveau 10 : Protection naturelle
<!-- id: protection-naturelle; type: automatique -->
Immunité à l'état Empoisonné et Résistance au type de dégâts associé à la terre choisie (Feu, Froid, Foudre ou Poison).

#### Niveau 14 : Sanctuaire naturel
<!-- id: sanctuaire-naturel; type: automatique -->
Action Magie, une utilisation de Forme sauvage : cube de 4,50 m d'arbres spectraux pendant 1 minute, abri partiel et Résistance du Druide pour les alliés qui s'y trouvent.

---

## Ensorceleur

<!-- id: ensorceleur -->

### Traits de base

- desclas : Lanceur de sorts dont la magie jaillit d'un don inné, hérité du sang ou d'un évènement surnaturel.
- caracprinc : Charisme
- difficulte : Intermédiaire
- dv : d6 par niveau
- jssauv : Constitution et Charisme
- maitcompA : 2 au choix parmi Arcanes, Intimidation, Intuition, Persuasion, Religion, Tromperie
- maitarme : Armes courantes
- formarm : Aucune
- typeincantation : complet
- equipdepA : Lance, 2 dagues, focaliseur arcanique (cristal), paquetage d'exploration souterraine et 28 po
- equipdepB : 50 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Points de Sorcellerie | Sorts mineurs | Sorts préparés |
|---|---|---|---|---|---|
| 1 | +2 | Sorts, Sorcellerie innée | — | 4 | 2 |
| 2 | +2 | Réserve arcanique, Métamagie | 2 | 4 | 3 |
| 3 | +2 | Sous-classe d'Ensorceleur | 3 | 4 | 6 |
| 4 | +2 | Amélioration de caractéristique | 4 | 5 | 7 |
| 5 | +3 | Restauration ensorcelée | 5 | 5 | 9 |
| 6 | +3 | Aptitude de sous-classe | 6 | 5 | 10 |
| 7 | +3 | Sorcellerie incarnée | 7 | 5 | 11 |
| 8 | +3 | Amélioration de caractéristique | 8 | 5 | 12 |
| 9 | +4 | — | 9 | 5 | 14 |
| 10 | +4 | Métamagie | 10 | 6 | 15 |
| 11 | +4 | — | 11 | 6 | 16 |
| 12 | +4 | Amélioration de caractéristique | 12 | 6 | 16 |
| 13 | +5 | — | 13 | 6 | 17 |
| 14 | +5 | Aptitude de sous-classe | 14 | 6 | 17 |
| 15 | +5 | — | 15 | 6 | 18 |
| 16 | +5 | Amélioration de caractéristique | 16 | 6 | 18 |
| 17 | +6 | Métamagie | 17 | 6 | 19 |
| 18 | +6 | Aptitude de sous-classe | 18 | 6 | 20 |
| 19 | +6 | Faveur épique | 19 | 6 | 21 |
| 20 | +6 | Apothéose arcanique | 20 | 6 | 22 |

### Aptitudes de classe

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
Caractéristique d'incantation : Charisme.

#### Niveau 1 : Sorcellerie innée
<!-- id: sorcellerie-innee; type: automatique -->
Action Bonus, 1 minute : DD de sauvegarde des sorts +1, Avantage aux jets d'attaque de sorts. 2 utilisations, récupérées à un Repos long.

#### Niveau 2 : Réserve arcanique (points de Sorcellerie)
<!-- id: reserve-arcanique-points-de-sorcellerie; type: automatique -->
Conversion emplacement → points de Sorcellerie (1 pt/niveau) ; conversion points → emplacement (coût croissant, max. 5e niveau).

#### Niveau 2, 10, 17 : Métamagie
<!-- id: metamagie; type: automatique -->
2 options choisies parmi la liste (Sort accéléré, Sort ample, Sort chercheur, Sort étendu, Sort intensifié, Sort jumeau, Sort prévenant, Sort renforcé, Sort subtil, Sort transmuté) ; 2 options de plus aux niveaux 10 et 17.

#### Niveau 3 : Sous-classe d'Ensorceleur
<!-- id: sous-classe-d-ensorceleur; type: choix-sousclasse -->
Choix de la sous-classe (Sorcellerie draconique dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Restauration ensorcelée
<!-- id: restauration-ensorcelee; type: automatique -->
Récupération de points de Sorcellerie (moitié du niveau, arrondi à l'inférieur) à un Repos court, 1/Repos long.

#### Niveau 7 : Sorcellerie incarnée
<!-- id: sorcellerie-incarnee; type: automatique -->
Réactivation de Sorcellerie innée pour 2 points de Sorcellerie ; jusqu'à 2 options de Métamagie par sort tant que l'aptitude est active.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur de Déplacement dimensionnel recommandé.

#### Niveau 20 : Apothéose arcanique
<!-- id: apotheose-arcanique; type: automatique -->
Une option de Métamagie gratuite par tour tant que Sorcellerie innée est active.

### Multiclassage

- multiarmures : (aucune)

### Sous-classe : Sorcellerie draconique

<!-- id: sorcellerie-draconique -->

- description : Ensorceleur au sang de dragon : peau écailleuse résistante, affinité avec un élément dévastateur, puis des ailes et un dragon allié.

| Niveau d'Ensorceleur | Sorts |
|---|---|
| 3 | Modification d’apparence, Orbe chromatique, Injonction, Souffle du dragon |
| 5 | Terreur, Vol |
| 7 | Œil du mage, Charme-monstre |
| 9 | Mythes et légendes, Convocation de dragon |

#### Niveau 3 : Résistance draconique
<!-- id: resistance-draconique; type: automatique -->
Points de vie maximum +3, puis +1 par niveau d'Ensorceleur ; sans armure, CA = 10 + mod. Dextérité + mod. Charisme.

#### Niveau 3 : Sorts draconiques
<!-- id: sorts-draconiques; type: automatique -->
Sorts toujours préparés selon le niveau d'Ensorceleur (voir la table).

#### Niveau 6 : Affinité élémentaire
<!-- id: affinite-elementaire; type: choix-effet -->
Choix d'un type de dégâts : Résistance à ce type, et mod. Charisme ajouté à un jet de dégâts d'un sort de ce type.
- **Acide** : Résistance à l'acide, mod. Charisme aux dégâts d'acide de vos sorts.
- **Feu** : Résistance au feu, mod. Charisme aux dégâts de feu de vos sorts.
- **Foudre** : Résistance à la foudre, mod. Charisme aux dégâts de foudre de vos sorts.
- **Froid** : Résistance au froid, mod. Charisme aux dégâts de froid de vos sorts.
- **Poison** : Résistance au poison, mod. Charisme aux dégâts de poison de vos sorts.

#### Niveau 14 : Ailes draconiques
<!-- id: ailes-draconiques; type: automatique -->
Action Bonus : Vitesse de vol de 18 m pendant 1 heure. 1/Repos long (ou 3 points de sorcellerie).

#### Niveau 18 : Compagnon draconique
<!-- id: compagnon-draconique; type: automatique -->
Convocation de dragon sans composante matérielle, une fois sans emplacement par Repos long ; durée portée à 1 minute sans Concentration.

---

## Guerrier

<!-- id: guerrier -->

### Traits de base

- desclas : Combattant polyvalent maîtrisant un large éventail d'armes et de tactiques martiales.
- caracprinc : Force ou Dextérité
- difficulte : Facile
- dv : d10 par niveau
- jssauv : Force et Constitution
- maitcompA : 2 au choix parmi Acrobaties, Athlétisme, Dressage, Histoire, Intuition, Intimidation, Perception, Persuasion, Survie
- maitarme : Armes courantes et armes de guerre
- formarm : Armures légères, intermédiaires et lourdes, boucliers
- equipdepA : Cotte de mailles, épée à deux mains, fléau d'armes, 8 javelines, paquetage d'exploration souterraine et 4 po
- equipdepB : Armure de cuir clouté, cimeterre, épée courte, arc long, 20 flèches, carquois, paquetage d'exploration souterraine et 11 po
- equipdepC : 155 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Second souffle | Bottes d'arme |
|---|---|---|---|---|
| 1 | +2 | Bottes d'arme, Second souffle, Style de combat | 2 | 3 |
| 2 | +2 | Fougue (1 utilisation), Sens tactique | 2 | 3 |
| 3 | +2 | Sous-classe de Guerrier | 2 | 3 |
| 4 | +2 | Amélioration de caractéristique | 3 | 4 |
| 5 | +3 | Attaque supplémentaire, Décalage tactique | 3 | 4 |
| 6 | +3 | Amélioration de caractéristique | 3 | 4 |
| 7 | +3 | Aptitude de sous-classe | 3 | 4 |
| 8 | +3 | Amélioration de caractéristique | 3 | 4 |
| 9 | +4 | Botte tactique, Inflexible (1 utilisation) | 3 | 4 |
| 10 | +4 | Aptitude de sous-classe | 4 | 5 |
| 11 | +4 | Double attaque supplémentaire | 4 | 5 |
| 12 | +4 | Amélioration de caractéristique | 4 | 5 |
| 13 | +5 | Attaques avisées, Inflexible (2 utilisations) | 4 | 5 |
| 14 | +5 | Amélioration de caractéristique | 4 | 5 |
| 15 | +5 | Aptitude de sous-classe | 4 | 5 |
| 16 | +5 | Amélioration de caractéristique | 4 | 6 |
| 17 | +6 | Fougue (2 utilisations), Inflexible (3 utilisations) | 4 | 6 |
| 18 | +6 | Aptitude de sous-classe | 4 | 6 |
| 19 | +6 | Faveur épique | 4 | 6 |
| 20 | +6 | Triple attaque supplémentaire | 4 | 6 |

### Aptitudes de classe

#### Niveau 1 : Style de combat
<!-- id: style-de-combat; type: automatique -->
Don de Style de combat (Défense recommandé), remplaçable à chaque niveau de Guerrier.

#### Niveau 1 : Second souffle
<!-- id: second-souffle; type: automatique -->
Action Bonus : récupération de 1d10 + niveau de Guerrier pv. 2 utilisations (1 récupérée à un Repos court, toutes à un Repos long).

#### Niveau 1 : Bottes d'arme
<!-- id: bottes-d-arme; type: automatique -->
Botte de 3 types d'armes au choix, modifiable à chaque Repos long.

#### Niveau 2 : Fougue
<!-- id: fougue; type: automatique -->
Action supplémentaire (hors action Magie) à son tour. 1 utilisation (2 au niveau 17), récupérée à un Repos court ou long.

#### Niveau 2 : Sens tactique
<!-- id: sens-tactique; type: automatique -->
Dépense de Second souffle pour rejouer un test de caractéristique raté (1d10 ajouté).

#### Niveau 3 : Sous-classe de Guerrier
<!-- id: sous-classe-de-guerrier; type: choix-sousclasse -->
Choix de la sous-classe (Champion dans le SRD).

#### Niveau 4, 6, 8, 12, 14, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Attaque supplémentaire
<!-- id: attaque-supplementaire; type: automatique -->
Deux attaques au lieu d'une.

#### Niveau 5 : Décalage tactique
<!-- id: decalage-tactique; type: automatique -->
Déplacement d'une demi-Vitesse sans attaque d'Opportunité en activant Second souffle.

#### Niveau 9 : Inflexible
<!-- id: inflexible; type: automatique -->
Rejouer un JS raté avec bonus égal au niveau de Guerrier. 1 utilisation (2 au niveau 13, 3 au niveau 17), 1/Repos long.

#### Niveau 9 : Botte tactique
<!-- id: botte-tactique; type: automatique -->
Remplacement d'une botte par Poussée, Ralentissement ou Sape.

#### Niveau 11 : Double attaque supplémentaire
<!-- id: double-attaque-supplementaire; type: automatique -->
Trois attaques au lieu d'une.

#### Niveau 13 : Attaques avisées
<!-- id: attaques-avisees; type: automatique -->
Avantage à l'attaque suivante contre une cible ratée.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur de prouesse martiale recommandé.

#### Niveau 20 : Triple attaque supplémentaire
<!-- id: triple-attaque-supplementaire; type: automatique -->
Quatre attaques au lieu d'une.

### Multiclassage

- multiarmures : Armures légères, intermédiaires et boucliers
- multiarmes : Armes courantes et armes de guerre

### Sous-classe : Champion

<!-- id: champion -->

- description : Combattant simple et redoutable qui mise sur les coups critiques, l'endurance et la polyvalence martiale.

#### Niveau 3 : Critique amélioré
<!-- id: critique-ameliore; type: automatique -->
Coup critique sur 19-20 aux jets d'attaque avec une arme ou à mains nues.

#### Niveau 3 : Athlète accompli
<!-- id: athlete-accompli; type: automatique -->
Avantage à l'Initiative et aux tests de Force (Athlétisme) ; après un Coup critique, déplacement de la moitié de votre Vitesse sans provoquer d'attaque d'Opportunité.

#### Niveau 7 : Style de combat supplémentaire
<!-- id: style-de-combat-supplementaire; type: automatique -->
Un don de Style de combat supplémentaire au choix.

#### Niveau 10 : Guerrier héroïque
<!-- id: guerrier-heroique; type: automatique -->
En combat, Inspiration héroïque au début de votre tour si vous n'en avez pas.

#### Niveau 15 : Critique supérieur
<!-- id: critique-superieur; type: automatique -->
Coup critique sur 18-20 aux jets d'attaque avec une arme ou à mains nues.

#### Niveau 18 : Survivant
<!-- id: survivant; type: automatique -->
Avantage aux JS contre la mort (18-20 = 20) ; En péril au début de votre tour, récupération de 5 + mod. Constitution points de vie.

---

## Magicien

<!-- id: magicien -->

### Traits de base

- desclas : Érudit de l'arcane qui étudie et inscrit ses sorts dans un grimoire pour façonner la réalité.
- caracprinc : Intelligence
- difficulte : Difficile
- dv : d6 par niveau
- jssauv : Intelligence et Sagesse
- maitcompA : 2 au choix parmi Arcanes, Histoire, Intuition, Investigation, Médecine, Nature, Religion
- maitarme : Armes courantes
- formarm : Aucune
- typeincantation : complet
- equipdepA : 2 dagues, focaliseur arcanique (bâton de combat), robe, grimoire, paquetage d'érudit et 5 po
- equipdepB : 55 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Sorts mineurs | Sorts préparés |
|---|---|---|---|---|
| 1 | +2 | Sorts, Savoir rituel, Restauration magique | 3 | 4 |
| 2 | +2 | Érudition | 3 | 5 |
| 3 | +2 | Sous-classe de Magicien | 3 | 6 |
| 4 | +2 | Amélioration de caractéristique | 4 | 7 |
| 5 | +3 | Mémorisation de sort | 4 | 9 |
| 6 | +3 | Aptitude de sous-classe | 4 | 10 |
| 7 | +3 | — | 4 | 11 |
| 8 | +3 | Amélioration de caractéristique | 4 | 12 |
| 9 | +4 | — | 4 | 14 |
| 10 | +4 | Aptitude de sous-classe | 5 | 15 |
| 11 | +4 | — | 5 | 16 |
| 12 | +4 | Amélioration de caractéristique | 5 | 16 |
| 13 | +5 | — | 5 | 17 |
| 14 | +5 | Aptitude de sous-classe | 5 | 18 |
| 15 | +5 | — | 5 | 19 |
| 16 | +5 | Amélioration de caractéristique | 5 | 21 |
| 17 | +6 | — | 5 | 22 |
| 18 | +6 | Maîtrise des sorts | 5 | 23 |
| 19 | +6 | Faveur épique | 5 | 24 |
| 20 | +6 | Sorts de prédilection | 5 | 25 |

### Aptitudes de classe

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
<!-- id: sorts-grimoire; grimoire-depart: 6; grimoire-par-niveau: 2 -->
Caractéristique d'incantation : Intelligence. Grimoire de départ : 6 sorts du 1er niveau ; +2 sorts par niveau de Magicien gagné.

#### Niveau 1 : Savoir rituel
<!-- id: savoir-rituel; type: automatique -->
Incantation en rituel des sorts « rituel » de votre grimoire sans les avoir préparés.

#### Niveau 1 : Restauration magique
<!-- id: restauration-magique; type: automatique -->
À un Repos court, récupération d'emplacements de sort (somme des niveaux ≤ moitié du niveau de Magicien arrondi au-dessus, aucun du 6e niveau ou plus). 1/Repos long.

#### Niveau 2 : Érudition
<!-- id: erudition; type: choix-expertise-1 -->
<!-- id: erudition-competences; competences: Arcanes, Histoire, Intuition, Investigation, Médecine, Nature, Religion -->
Expertise dans une compétence maîtrisée (Arcanes, Histoire, Intuition, Investigation, Médecine, Nature ou Religion).

#### Niveau 3 : Sous-classe de Magicien
<!-- id: sous-classe-de-magicien; type: choix-sousclasse -->
Choix de la sous-classe (Évocateur dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Mémorisation de sort
<!-- id: memorisation-de-sort; type: automatique -->
À un Repos court, remplacement d'un sort préparé par un autre du grimoire.

#### Niveau 18 : Maîtrise des sorts
<!-- id: maitrise-des-sorts; type: automatique -->
<!-- id: maitrise-des-sorts-choix; sorts-speciaux: a-volonte; niveaux: 1, 2; incantation: Action -->
Un sort du 1er niveau et un du 2e niveau du grimoire (temps d'incantation : action), toujours préparés et lançables à volonté à leur niveau sans emplacement.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->

#### Niveau 20 : Sorts de prédilection
<!-- id: sorts-de-predilection; type: automatique -->
<!-- id: sorts-de-predilection-choix; sorts-speciaux: predilection; niveaux: 3, 3 -->
Deux sorts du 3e niveau du grimoire, toujours préparés, lançables chacun une fois au 3e niveau sans emplacement (récupéré à un Repos court ou long).

### Multiclassage

- multiarmures : (aucune)

### Sous-classe : Évocateur

<!-- id: evocateur -->

- description : Artilleur magique spécialiste de l'Évocation : sorts de dégâts plus puissants qui épargnent les alliés.

#### Niveau 3 : Savant en évocation
<!-- id: savant-en-evocation; type: automatique -->
<!-- id: savant-en-evocation-sorts; choix: 2; effet: sorts; ecole: Évocation; niveau-max: 2; libelle: Sorts d'Évocation ajoutés au grimoire -->
Deux sorts de Magicien de l'école d'Évocation (niveau 2 au plus) ajoutés gratuitement au grimoire. Ensuite, un sort d'Évocation gratuit à chaque nouveau niveau d'emplacement de sort de Magicien.

#### Niveau 3 : Sort mineur appuyé
<!-- id: sort-mineur-appuye; type: automatique -->
<!-- id: sort-mineur-appuye-combat; combat: demi-degats-mineur -->
Demi-dégâts même en cas d'échec de l'attaque ou de JS réussi de la cible pour vos sorts mineurs.

#### Niveau 6 : Façonneur de sorts
<!-- id: faconneur-de-sorts; type: automatique -->
<!-- id: faconneur-de-sorts-combat; combat: protection-allies; ecole: Évocation -->
Protection d'alliés dans la zone d'un sort d'Évocation : ils réussissent automatiquement leur JS et ne subissent aucun dégât.

#### Niveau 10 : Évocation améliorée
<!-- id: evocation-amelioree; type: automatique -->
<!-- id: evocation-amelioree-combat; combat: bonus-degats-carac; ecole: Évocation; carac: Intelligence -->
Mod. Intelligence ajouté à un jet de dégâts d'un sort d'Évocation de Magicien.

#### Niveau 14 : Surcharge magique
<!-- id: surcharge-magique; type: automatique -->
Dégâts maximaux garantis pour un sort de Magicien (niveaux 1 à 5), avec contrecoup nécrotique en cas de réutilisation avant un Repos long.

---

## Moine

<!-- id: moine -->

### Traits de base

- desclas : Adepte de la perfection martiale et spirituelle, canalisant une énergie intérieure dans ses coups.
- caracprinc : Dextérité et Sagesse
- difficulte : Difficile
- dv : d8 par niveau
- jssauv : Force et Dextérité
- maitcompA : 2 au choix parmi Acrobaties, Athlétisme, Discrétion, Histoire, Intuition, Religion
- maitarme : Armes courantes ; armes de guerre dotées de la propriété Légère
- maitoutils : un type d'outils d'artisan ou d'instrument de musique
<!-- id: moine-outil; choix: 1; effet: outils; options: equipement: Artisan | equipement: Instrument de musique; remplace: outils d'artisan/instrument de musique choisi -->
- formarm : Aucune
- equipdepA : Lance, 5 dagues, outils d'artisan/instrument de musique choisi, paquetage d'explorateur et 11 po
- equipdepB : 50 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Arts martiaux | Points de Credo | Déplacement sans armure |
|---|---|---|---|---|---|
| 1 | +2 | Arts martiaux, Défense sans armure | 1d6 | — | — |
| 2 | +2 | Déplacement sans armure, Credo du Moine, Métabolisme surnaturel | 1d6 | 2 | +3 m |
| 3 | +2 | Déviation d'assaut, Sous-classe de Moine | 1d6 | 3 | +3 m |
| 4 | +2 | Amélioration de caractéristique, Chute ralentie | 1d6 | 4 | +3 m |
| 5 | +3 | Attaque supplémentaire, Frappe étourdissante | 1d8 | 5 | +3 m |
| 6 | +3 | Aptitude de sous-classe, Frappes renforcées | 1d8 | 6 | +4,50 m |
| 7 | +3 | Esquive totale | 1d8 | 7 | +4,50 m |
| 8 | +3 | Amélioration de caractéristique | 1d8 | 8 | +4,50 m |
| 9 | +4 | Mouvement acrobatique | 1d8 | 9 | +4,50 m |
| 10 | +4 | Autosubsistance, Credo accru | 1d8 | 10 | +6 m |
| 11 | +4 | Aptitude de sous-classe | 1d10 | 11 | +6 m |
| 12 | +4 | Amélioration de caractéristique | 1d10 | 12 | +6 m |
| 13 | +5 | Parade énergétique | 1d10 | 13 | +6 m |
| 14 | +5 | Survivant discipliné | 1d10 | 14 | +7,50 m |
| 15 | +5 | Credo parachevé | 1d10 | 15 | +7,50 m |
| 16 | +5 | Amélioration de caractéristique | 1d10 | 16 | +7,50 m |
| 17 | +6 | Aptitude de sous-classe | 1d12 | 17 | +7,50 m |
| 18 | +6 | Défense supérieure | 1d12 | 18 | +9 m |
| 19 | +6 | Faveur épique | 1d12 | 19 | +9 m |
| 20 | +6 | Esprit et corps | 1d12 | 20 | +9 m |

### Aptitudes de classe

#### Niveau 1 : Arts martiaux
<!-- id: arts-martiaux; type: automatique -->
Sans armure ni bouclier, en combattant à mains nues ou avec des armes de Moine : attaque à mains nues supplémentaire (action Bonus), dé d'Arts martiaux (remplace les dégâts normaux, évolue avec le niveau), Attaques lestes (mod. Dextérité à la place de Force).

#### Niveau 1 : Défense sans armure
<!-- id: defense-sans-armure; type: automatique -->
CA = 10 + mod. Dextérité + mod. Sagesse (sans armure ni bouclier).

#### Niveau 2 : Credo du Moine (points de Credo)
<!-- id: credo-du-moine-points-de-credo; type: automatique -->
Trois aptitudes de base : **Déluge de coups** (1 pt, deux attaques à mains nues en action Bonus), **Patience défensive** (Désengagement en action Bonus, +1 pt pour aussi Esquiver), **Porté par le vent** (Pointe en action Bonus, +1 pt pour Désengagement + Pointe et doubler le saut). DD de sauvegarde = 8 + mod. Sagesse + bonus de maîtrise.

#### Niveau 2 : Déplacement sans armure
<!-- id: deplacement-sans-armure; type: automatique -->
Vitesse +3 m (évolutif) sans armure ni bouclier.

#### Niveau 2 : Métabolisme surnaturel
<!-- id: metabolisme-surnaturel; type: automatique -->
À l'Initiative, récupération des points de Credo dépensés + pv (dé d'Arts martiaux + niveau de Moine). 1/Repos long.

#### Niveau 3 : Déviation d'assaut
<!-- id: deviation-d-assaut; type: automatique -->
Réaction pour réduire les dégâts (1d10 + mod. Dextérité + niveau) d'une attaque contondante/perforante/tranchante ; si réduits à 0, possibilité de dévier les dégâts (2×dé d'Arts martiaux + mod. Dextérité) sur une autre créature pour 1 point de Credo.

#### Niveau 3 : Sous-classe de Moine
<!-- id: sous-classe-de-moine; type: choix-sousclasse -->
Choix de la sous-classe (Credo de la Paume dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 4 : Chute ralentie
<!-- id: chute-ralentie; type: automatique -->
Réaction : réduction des dégâts de chute (×5 le niveau de Moine).

#### Niveau 5 : Attaque supplémentaire
<!-- id: attaque-supplementaire; type: automatique -->

#### Niveau 5 : Frappe étourdissante
<!-- id: frappe-etourdissante; type: automatique -->
1 pt de Credo, JS Constitution : échec = Étourdi jusqu'au tour suivant ; réussite = Vitesse réduite de moitié et prochaine attaque avec Avantage.

#### Niveau 6 : Frappes renforcées
<!-- id: frappes-renforcees; type: automatique -->
Dégâts à mains nues convertibles en dégâts de force.

#### Niveau 7 : Esquive totale
<!-- id: esquive-totale; type: automatique -->
0 dégât en cas de réussite à un JS Dextérité pour demi-dégâts, demi-dégâts en cas d'échec (sauf si Neutralisé).

#### Niveau 9 : Mouvement acrobatique
<!-- id: mouvement-acrobatique; type: automatique -->
Déplacement le long de surfaces verticales et sur liquides sans armure ni bouclier.

#### Niveau 10 : Autosubsistance
<!-- id: autosubsistance; type: automatique -->
Suppression de Charmé/Effrayé/Empoisonné à la fin d'un tour ; pas d'Épuisement pour absence de nourriture/eau.

#### Niveau 10 : Credo accru
<!-- id: credo-accru; type: automatique -->
Déluge de coups (3 attaques pour 1 pt), Patience défensive (pv temporaires), Porté par le vent (déplace une créature consentante).

#### Niveau 13 : Parade énergétique
<!-- id: parade-energetique; type: automatique -->
Déviation d'assaut applicable à tout type de dégâts.

#### Niveau 14 : Survivant discipliné
<!-- id: survivant-discipline; type: automatique -->
Maîtrise de tous les JS ; rejeu d'un JS raté pour 1 pt de Credo.

#### Niveau 15 : Credo parachevé
<!-- id: credo-paracheve; type: automatique -->
Récupération de points de Credo jusqu'à 4 à l'Initiative (si moins de 4 et Métabolisme surnaturel non utilisé).

#### Niveau 18 : Défense supérieure
<!-- id: defense-superieure; type: automatique -->
3 pts de Credo : Résistance à tous les dégâts sauf force pendant 1 minute.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur d'attaque irrésistible recommandé.

#### Niveau 20 : Esprit et corps
<!-- id: esprit-et-corps; type: automatique -->
Dextérité et Sagesse +4 (max. 25).

### Multiclassage

- multiarmes : Armes courantes, épées courtes

### Sous-classe : Credo de la Paume

<!-- id: credo-de-la-paume -->

- description : Maître des arts martiaux à mains nues : chaque Déluge de coups repousse, renverse ou désarçonne, jusqu'à la paume vibratoire mortelle.

#### Niveau 3 : Technique de la Paume
<!-- id: technique-de-la-paume; type: automatique -->
Chaque attaque de Déluge de coups qui touche impose un effet : Hésitation (pas de Réaction), Bourrade (JS Force ou poussée de 4,50 m) ou Renversement (JS Dextérité ou À terre).

#### Niveau 6 : Plénitude physique
<!-- id: plenitude-physique; type: automatique -->
Action Bonus : récupération d'un dé d'Arts martiaux + mod. Sagesse points de vie, nombre d'utilisations = mod. Sagesse (min. 1)/Repos long.

#### Niveau 11 : Foulée preste
<!-- id: foulee-preste; type: automatique -->
Après une action Bonus autre que Porté par le vent, utilisation immédiate de Porté par le vent.

#### Niveau 17 : Paume vibratoire
<!-- id: paume-vibratoire; type: automatique -->
4 points de Credo en touchant : vibrations déclenchées plus tard (action) pour 10d12 dégâts de force, JS Constitution pour moitié.

---

## Occultiste

<!-- id: occultiste -->

### Traits de base

- desclas : Lanceur de sorts lié par un pacte à une entité surnaturelle qui lui accorde son pouvoir.
- caracprinc : Charisme
- difficulte : Intermédiaire
- dv : d8 par niveau
- jssauv : Sagesse et Charisme
- maitcompA : 2 au choix parmi Arcanes, Histoire, Intimidation, Investigation, Nature, Religion, Tromperie
- maitarme : Armes courantes
- formarm : Armures légères
- typeincantation : pacte
- equipdepA : Armure de cuir, serpe, 2 dagues, focaliseur arcanique (orbe), livre (savoir occulte), paquetage d'érudit et 15 po
- equipdepB : 100 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Manifestations occultes | Sorts mineurs | Sorts préparés | Emplacements de sort | Niveau des emplacements |
|---|---|---|---|---|---|---|---|
| 1 | +2 | Manifestations occultes, Magie de pacte | 1 | 2 | 2 | 1 | 1 |
| 2 | +2 | Rouerie magique | 3 | 2 | 3 | 2 | 1 |
| 3 | +2 | Sous-classe d'Occultiste | 3 | 2 | 4 | 2 | 2 |
| 4 | +2 | Amélioration de caractéristique | 3 | 3 | 5 | 2 | 2 |
| 5 | +3 | — | 5 | 3 | 6 | 2 | 3 |
| 6 | +3 | Aptitude de sous-classe | 5 | 3 | 7 | 2 | 3 |
| 7 | +3 | — | 6 | 3 | 8 | 2 | 4 |
| 8 | +3 | Amélioration de caractéristique | 6 | 3 | 9 | 2 | 4 |
| 9 | +4 | Communication avec le protecteur | 7 | 3 | 10 | 2 | 5 |
| 10 | +4 | Aptitude de sous-classe | 7 | 4 | 10 | 2 | 5 |
| 11 | +4 | Arcanum mystique (sort du 6e niveau) | 7 | 4 | 11 | 3 | 5 |
| 12 | +4 | Amélioration de caractéristique | 8 | 4 | 11 | 3 | 5 |
| 13 | +5 | Arcanum mystique (sort du 7e niveau) | 8 | 4 | 12 | 3 | 5 |
| 14 | +5 | Aptitude de sous-classe | 8 | 4 | 12 | 3 | 5 |
| 15 | +5 | Arcanum mystique (sort du 8e niveau) | 9 | 4 | 13 | 3 | 5 |
| 16 | +5 | Amélioration de caractéristique | 9 | 4 | 13 | 3 | 5 |
| 17 | +6 | Arcanum mystique (sort du 9e niveau) | 9 | 4 | 14 | 4 | 5 |
| 18 | +6 | — | 10 | 4 | 14 | 4 | 5 |
| 19 | +6 | Faveur épique | 10 | 4 | 15 | 4 | 5 |
| 20 | +6 | Maître de l'occulte | 10 | 4 | 15 | 4 | 5 |

### Aptitudes de classe

#### Niveau 1 : Manifestations occultes
<!-- id: manifestations-occultes; type: automatique -->
Choix d'une Manifestation (ex. Pacte du grimoire, Pacte de la lame, Pacte de la chaîne) ; d'autres reçues aux niveaux indiqués dans la table. Remplaçables à chaque niveau (sauf prérequis d'une autre manifestation).

#### Niveau 1 : Magie de pacte
<!-- id: magie-de-pacte; type: automatique -->
Caractéristique d'incantation : Charisme. Tous les emplacements de sort de Magie de pacte sont du même niveau (voir colonne Niveau des emplacements) et se récupèrent à un Repos court ou long.

#### Niveau 2 : Rouerie magique
<!-- id: rouerie-magique; type: automatique -->
1 minute de rite : récupération de la moitié (arrondi au-dessus) des emplacements de Magie de pacte dépensés. 1/Repos long.

#### Niveau 3 : Sous-classe d'Occultiste
<!-- id: sous-classe-d-occultiste; type: choix-sousclasse -->
Choix de la sous-classe (Protecteur Fiélon dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 9 : Communication avec le protecteur
<!-- id: communication-avec-le-protecteur; type: automatique -->
contact avec les plans toujours préparé, lançable sans emplacement (réussite automatique du JS). 1/Repos long.

#### Niveau 11, 13, 15, 17 : Arcanum mystique
<!-- id: arcanum-mystique; type: automatique -->
Sorts de haut niveau (6e à 9e) lançables une fois sans emplacement, 1/Repos long chacun.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur du destin recommandé.

#### Niveau 20 : Maître de l'occulte
<!-- id: maitre-de-l-occulte; type: automatique -->
Rouerie magique récupère désormais tous les emplacements de Magie de pacte dépensés.

### Multiclassage

- multiarmures : Armures légères
- multiarmes : Armes courantes

### Sous-classe : Protecteur Fiélon

<!-- id: protecteur-fielon -->

- description : Occultiste lié à un fiélon : sorts de feu et de destruction, points de vie temporaires à chaque ennemi abattu et chance infernale.

| Niveau d'Occultiste | Sorts |
|---|---|
| 3 | Mains brûlantes, Injonction, Rayon ardent, Suggestion |
| 5 | Boule de feu, Nuage nauséabond |
| 7 | Bouclier de feu, Mur de feu |
| 9 | Quête, Fléau d’insectes |

#### Niveau 3 : Bénédiction du ténébreux
<!-- id: benediction-du-tenebreux; type: automatique -->
Quand un ennemi tombe à 0 pv près de vous (ou d'un allié à 3 m), points de vie temporaires = mod. Charisme + niveau d'Occultiste (min. 1).

#### Niveau 3 : Sorts du Fiélon
<!-- id: sorts-du-fielon; type: automatique -->
Sorts toujours préparés selon le niveau d'Occultiste (voir la table).

#### Niveau 6 : Chance du ténébreux
<!-- id: chance-du-tenebreux; type: automatique -->
+1d10 à un test de caractéristique ou un JS, nombre d'utilisations = mod. Charisme (min. 1)/Repos long, une fois par jet.

#### Niveau 10 : Résistance fiélonne
<!-- id: resistance-fielonne; type: automatique -->
À chaque Repos court ou long, Résistance à un type de dégâts choisi (hors force) jusqu'au prochain changement.

#### Niveau 14 : Traversée des enfers
<!-- id: traversee-des-enfers; type: automatique -->
Après avoir touché une créature : JS Charisme, sinon projetée dans les Plans Inférieurs jusqu'à la fin de votre prochain tour et 8d10 dégâts psychiques. 1/Repos long.

---

## Paladin

<!-- id: paladin -->

### Traits de base

- desclas : Guerrier sacré lié par un serment, mêlant combat et magie divine au service d'une cause.
- caracprinc : Force et Charisme
- difficulte : Intermédiaire
- dv : d10 par niveau
- jssauv : Sagesse et Charisme
- maitcompA : 2 au choix parmi Athlétisme, Intimidation, Intuition, Médecine, Persuasion, Religion
- maitarme : Armes courantes et armes de guerre
- formarm : Armures légères, intermédiaires et lourdes, boucliers
- typeincantation : demi
- equipdepA : Cotte de mailles, bouclier, épée longue, 6 javelines, symbole sacré, paquetage d'ecclésiastique et 9 po
- equipdepB : 150 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Conduit divin | Sorts préparés |
|---|---|---|---|---|
| 1 | +2 | Bottes d'arme, Imposition des mains, Sorts | — | 2 |
| 2 | +2 | Châtiment de paladin, Style de combat | — | 3 |
| 3 | +2 | Conduit divin, Sous-classe de Paladin | 2 | 4 |
| 4 | +2 | Amélioration de caractéristique | 2 | 5 |
| 5 | +3 | Attaque supplémentaire, Fidèle destrier | 2 | 6 |
| 6 | +3 | Aura de protection | 2 | 6 |
| 7 | +3 | Aptitude de sous-classe | 2 | 7 |
| 8 | +3 | Amélioration de caractéristique | 2 | 7 |
| 9 | +4 | Abjuration d'ennemis | 2 | 9 |
| 10 | +4 | Aura de courage | 2 | 9 |
| 11 | +4 | Frappes radiantes | 3 | 10 |
| 12 | +4 | Amélioration de caractéristique | 3 | 10 |
| 13 | +5 | — | 3 | 11 |
| 14 | +5 | Toucher réparateur | 3 | 11 |
| 15 | +5 | Aptitude de sous-classe | 3 | 12 |
| 16 | +5 | Amélioration de caractéristique | 3 | 12 |
| 17 | +6 | — | 3 | 14 |
| 18 | +6 | Aura étendue | 3 | 14 |
| 19 | +6 | Faveur épique | 3 | 15 |
| 20 | +6 | Aptitude de sous-classe | 3 | 15 |

### Aptitudes de classe

#### Niveau 1 : Imposition des mains
<!-- id: imposition-des-mains; type: automatique -->
Réserve de soins = 5 × niveau de Paladin, récupérée à un Repos long. Action Bonus, contact : restitution de pv (ou dépense de 5 pv pour supprimer Empoisonné).

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
Caractéristique d'incantation : Charisme.

#### Niveau 1 : Bottes d'arme
<!-- id: bottes-d-arme; type: automatique -->
Botte de deux types d'armes maîtrisées, modifiable à chaque Repos long.

#### Niveau 2 : Châtiment de paladin
<!-- id: chatiment-de-paladin; type: automatique -->
châtiment divin toujours préparé, lançable une fois sans emplacement (1/Repos long).

#### Niveau 2 : Style de combat
<!-- id: style-de-combat; type: choix-effet -->
Choix entre deux options :
- **Style de combat** <!-- id: style-de-combat-don --> : Don de Style de combat, remplaçable à chaque niveau de Paladin.
- **Combattant béni** <!-- id: combattant-beni --> : deux sorts mineurs de Clerc.

#### Niveau 3 : Conduit divin
<!-- id: conduit-divin; type: automatique -->
2 utilisations (1 récupérée à un Repos court, toutes à un Repos long ; +1 utilisation au niveau 11). Effet de base : **Perception divine** (détection de Célestes/Fiélons/Morts-vivants à 18 m pendant 10 minutes).

#### Niveau 3 : Sous-classe de Paladin
<!-- id: sous-classe-de-paladin; type: choix-sousclasse -->
Choix de la sous-classe (Serment de Dévotion dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Attaque supplémentaire
<!-- id: attaque-supplementaire; type: automatique -->

#### Niveau 5 : Fidèle destrier
<!-- id: fidele-destrier; type: automatique -->
Monture spirituelle invocable.

#### Niveau 6 : Aura de protection
<!-- id: aura-de-protection; type: automatique -->
Bonus aux JS (vous et alliés proches) égal au mod. Charisme.

#### Niveau 9 : Abjuration d'ennemis
<!-- id: abjuration-d-ennemis; type: automatique -->
JS Sagesse imposant Effrayé à des créatures choisies (nombre = mod. Charisme, min. 1) dans un rayon de 18 m.

#### Niveau 10 : Aura de courage
<!-- id: aura-de-courage; type: automatique -->
Immunité à Effrayé dans l'Aura de protection.

#### Niveau 11 : Frappes radiantes
<!-- id: frappes-radiantes; type: automatique -->
1d8 dégâts radiants supplémentaires sur les attaques au corps à corps/mains nues.

#### Niveau 14 : Toucher réparateur
<!-- id: toucher-reparateur; type: automatique -->
Imposition des mains peut aussi retirer Assourdi, Aveuglé, Charmé, Effrayé, Étourdi ou Paralysé (5 pv de réserve par état).

#### Niveau 18 : Aura étendue
<!-- id: aura-etendue; type: automatique -->
Aura de protection = Émanation de 9 m.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur de Vision lucide recommandé.

### Multiclassage

- multiarmures : Armures légères, intermédiaires et boucliers
- multiarmes : Armes courantes et armes de guerre

### Sous-classe : Serment de Dévotion

<!-- id: serment-de-devotion -->

- description : Paladin idéaliste, champion de la justice et de l'ordre : arme sacrée, aura protectrice contre le charme et lumière radieuse.

| Niveau de Paladin | Sorts |
|---|---|
| 3 | Protection contre le mal et le bien, Bouclier de la foi |
| 5 | Aide, Zone de vérité |
| 9 | Lueur d’espoir, Dissipation de la magie |
| 13 | Liberté de mouvement, Gardien de la foi |
| 17 | Communion, Colonne de flamme |

#### Niveau 3 : Sorts du Serment de Dévotion
<!-- id: sorts-serment-de-devotion; type: automatique -->
Sorts toujours préparés selon le niveau de Paladin (voir la table).

#### Niveau 3 : Arme sacrée
<!-- id: arme-sacree; type: automatique -->
Conduit divin en attaquant : pendant 10 minutes, mod. Charisme ajouté aux attaques avec l'arme, qui émet de la lumière et inflige au choix des dégâts radiants.

#### Niveau 7 : Aura de dévotion
<!-- id: aura-de-devotion; type: automatique -->
Vous et vos alliés dans votre Aura de protection êtes immunisés contre l'état Charmé.

#### Niveau 15 : Châtiment protecteur
<!-- id: chatiment-protecteur; type: automatique -->
En lançant Châtiment divin, abri important pour vous et vos alliés dans votre aura jusqu'au début de votre prochain tour.

#### Niveau 20 : Nimbe sacré
<!-- id: nimbe-sacre; type: automatique -->
Action Bonus, 10 minutes : dégâts radiants (mod. Charisme + bonus de maîtrise) aux ennemis dans l'aura, lumière du soleil, Avantage aux JS contre les Fiélons et Morts-vivants. 1/Repos long.

---

## Rôdeur

<!-- id: rodeur -->

### Traits de base

- desclas : Chasseur et pisteur à l'aise en terrain sauvage, combinant maîtrise du combat et magie de la nature.
- caracprinc : Dextérité et Sagesse
- difficulte : Intermédiaire
- dv : d10 par niveau
- jssauv : Force et Dextérité
- maitcompA : 3 au choix parmi Athlétisme, Discrétion, Dressage, Intuition, Investigation, Nature, Perception, Survie
- maitarme : Armes courantes et armes de guerre
- formarm : Armures légères, intermédiaires et boucliers
- typeincantation : demi
- equipdepA : Armure de cuir clouté, cimeterre, épée courte, arc long, 20 flèches, carquois, focaliseur druidique (branche de gui), paquetage d'explorateur et 7 po
- equipdepB : 150 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Ennemi juré | Sorts préparés |
|---|---|---|---|---|
| 1 | +2 | Sorts, Ennemi juré, Bottes d'arme | 2 | 2 |
| 2 | +2 | Fin explorateur, Style de combat | 2 | 3 |
| 3 | +2 | Sous-classe de Rôdeur | 2 | 4 |
| 4 | +2 | Amélioration de caractéristique | 2 | 5 |
| 5 | +3 | Attaque supplémentaire | 3 | 6 |
| 6 | +3 | Arpenteur | 3 | 6 |
| 7 | +3 | Aptitude de sous-classe | 3 | 7 |
| 8 | +3 | Amélioration de caractéristique | 3 | 7 |
| 9 | +4 | Expertise | 4 | 9 |
| 10 | +4 | Infatigable | 4 | 9 |
| 11 | +4 | Aptitude de sous-classe | 4 | 10 |
| 12 | +4 | Amélioration de caractéristique | 4 | 10 |
| 13 | +5 | Chasseur implacable | 5 | 11 |
| 14 | +5 | Voile de la nature | 5 | 11 |
| 15 | +5 | Aptitude de sous-classe | 5 | 12 |
| 16 | +5 | Amélioration de caractéristique | 5 | 12 |
| 17 | +6 | Chasseur précis | 6 | 14 |
| 18 | +6 | Sens sauvages | 6 | 14 |
| 19 | +6 | Faveur épique | 6 | 15 |
| 20 | +6 | Tueur implacable | 6 | 15 |

### Aptitudes de classe

#### Niveau 1 : Sorts
<!-- id: sorts; type: automatique -->
Caractéristique d'incantation : Sagesse.

#### Niveau 1 : Ennemi juré
<!-- id: ennemi-jure; type: automatique -->
marque du chasseur toujours préparé, lançable 2 fois sans emplacement (récupération à un Repos long, progression selon la table).

#### Niveau 1 : Bottes d'arme
<!-- id: bottes-d-arme; type: automatique -->
Botte de deux types d'armes maîtrisées, modifiable à chaque Repos long.

#### Niveau 2 : Fin explorateur
<!-- id: fin-explorateur; type: choix-expertise-1 -->
Expertise dans une compétence maîtrisée ; deux langues supplémentaires.

#### Niveau 2 : Style de combat
<!-- id: style-de-combat; type: choix-effet -->
Choix entre deux options :
- **Style de combat** <!-- id: style-de-combat-don --> : Don de Style de combat, remplaçable à chaque niveau de Rôdeur.
- **Combattant druidique** <!-- id: combattant-druidique --> : deux sorts mineurs de Druide.

#### Niveau 3 : Sous-classe de Rôdeur
<!-- id: sous-classe-de-rodeur; type: choix-sousclasse -->
Choix de la sous-classe (Chasseur dans le SRD).

#### Niveau 4, 8, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Attaque supplémentaire
<!-- id: attaque-supplementaire; type: automatique -->

#### Niveau 6 : Arpenteur
<!-- id: arpenteur; type: automatique -->
Vitesse +3 m sans armure lourde ; Vitesse d'escalade et de nage égales à la Vitesse.

#### Niveau 9 : Expertise
<!-- id: expertise; type: choix-expertise-2 -->
Expertise dans deux compétences maîtrisées.

#### Niveau 10 : Infatigable
<!-- id: infatigable; type: automatique -->
Action Magie pour pv temporaires (1d8 + mod. Sagesse, min. +1) ; réduction de l'Épuisement à chaque Repos court.

#### Niveau 13 : Chasseur implacable
<!-- id: chasseur-implacable; type: automatique -->
Les dégâts ne peuvent pas briser la Concentration sur marque du chasseur.

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->

### Multiclassage

- multiarmures : Armures légères, intermédiaires et boucliers
- multiarmes : Armes courantes et armes de guerre
- maitcompB : 1 compétence au choix parmi celles du Rôdeur

### Sous-classe : Chasseur

<!-- id: chasseur -->

- description : Traqueur de monstres qui étudie ses proies et adapte ses tactiques face aux hordes ou aux créatures redoutables.

#### Niveau 3 : Savoir du chasseur
<!-- id: savoir-du-chasseur; type: automatique -->
Connaissance des Immunités, Résistances et Vulnérabilités de la créature marquée par Marque du chasseur.

#### Niveau 3 : Proie du chasseur
<!-- id: proie-du-chasseur; type: choix-effet -->
Choix d'une technique (modifiable à chaque Repos court ou long) :
- **Briseur de hordes** : une fois par tour, attaque supplémentaire contre une autre créature à 1,50 m de la cible, hors de portée de vous.
- **Tueur de colosses** : une fois par tour, 1d8 dégâts supplémentaires contre une cible qui n'a pas tous ses points de vie.

#### Niveau 7 : Tactiques défensives
<!-- id: tactiques-defensives; type: choix-effet -->
Choix d'une tactique (modifiable à chaque Repos court ou long) :
- **Échapper à la horde** : les attaques d'Opportunité contre vous subissent le Désavantage.
- **Défense contre les attaques multiples** : après avoir été touché par une créature, les autres attaques de celle-ci ce tour subissent le Désavantage.

#### Niveau 11 : Proie du chasseur supérieure
<!-- id: proie-du-chasseur-superieure; type: automatique -->
Une fois par tour, les dégâts de Marque du chasseur s'appliquent aussi à une autre créature à 9 m ou moins de la cible.

#### Niveau 15 : Défense de chasseur supérieure
<!-- id: defense-de-chasseur-superieure; type: automatique -->
Réaction après avoir subi des dégâts : Résistance à ce type de dégâts jusqu'à la fin du tour.

---

## Roublard

<!-- id: roublard -->

### Traits de base

- desclas : Expert de la discrétion et de la ruse, frappant avec précision là où l'adversaire est vulnérable.
- caracprinc : Dextérité
- difficulte : Facile
- dv : d8 par niveau
- jssauv : Dextérité et Intelligence
- maitcompA : 4 au choix parmi Acrobaties, Athlétisme, Discrétion, Escamotage, Intimidation, Intuition, Investigation, Perception, Persuasion, Tromperie
- maitarme : Armes courantes et armes de guerre dotées de la propriété Finesse ou Légère
- maitoutils : Outils de voleur
- formarm : Armures légères
- equipdepA : Armure de cuir, 2 dagues, épée courte, arc court, 20 flèches, carquois, outils de voleur, paquetage de cambrioleur et 8 po
- equipdepB : 100 po

### Table de progression

| Niveau | Bonus de maîtrise | Aptitudes de classe | Attaque sournoise |
|---|---|---|---|
| 1 | +2 | Argot des voleurs, Attaque sournoise, Bottes d'arme, Expertise | 1d6 |
| 2 | +2 | Ruse | 1d6 |
| 3 | +2 | Sous-classe de Roublard, Visée appliquée | 2d6 |
| 4 | +2 | Amélioration de caractéristique | 2d6 |
| 5 | +3 | Frappe malicieuse, Esquive instinctive | 3d6 |
| 6 | +3 | Expertise | 3d6 |
| 7 | +3 | Esquive totale, Savoir-faire | 4d6 |
| 8 | +3 | Amélioration de caractéristique | 4d6 |
| 9 | +4 | Aptitude de sous-classe | 5d6 |
| 10 | +4 | Amélioration de caractéristique | 5d6 |
| 11 | +4 | Frappe malicieuse améliorée | 6d6 |
| 12 | +4 | Amélioration de caractéristique | 6d6 |
| 13 | +5 | Aptitude de sous-classe | 7d6 |
| 14 | +5 | Coups vicieux | 7d6 |
| 15 | +5 | Esprit fuyant | 8d6 |
| 16 | +5 | Amélioration de caractéristique | 8d6 |
| 17 | +6 | Aptitude de sous-classe | 9d6 |
| 18 | +6 | Insaisissable | 9d6 |
| 19 | +6 | Faveur épique | 10d6 |
| 20 | +6 | Coup de chance | 10d6 |

### Aptitudes de classe

#### Niveau 1 : Expertise
<!-- id: expertise; type: choix-expertise-2 -->
Expertise dans deux compétences maîtrisées (deux de plus au niveau 6).

#### Niveau 1 : Attaque sournoise
<!-- id: attaque-sournoise; type: automatique -->
1/tour, avec une arme Finesse/À distance et l'Avantage (ou un allié adjacent à la cible) : dégâts supplémentaires (1d6, progression selon la table).

#### Niveau 1 : Argot des voleurs
<!-- id: argot-des-voleurs; type: automatique -->
<!-- id: argot-langue-auto; langues: Argot des voleurs -->
<!-- id: argot-langue; choix: 1; effet: langues; options: langues -->
Argot des voleurs + une langue au choix.

#### Niveau 1 : Bottes d'arme
<!-- id: bottes-d-arme; type: automatique -->
Botte de deux types d'armes maîtrisées, modifiable à chaque Repos long.

#### Niveau 2 : Ruse
<!-- id: ruse; type: automatique -->
Action Bonus : Désengagement, Furtivité ou Pointe.

#### Niveau 3 : Sous-classe de Roublard
<!-- id: sous-classe-de-roublard; type: choix-sousclasse -->
Choix de la sous-classe (Voleur dans le SRD).

#### Niveau 3 : Visée appliquée
<!-- id: visee-appliquee; type: automatique -->
Action Bonus (sans déplacement ce tour) : Avantage à la prochaine attaque, Vitesse réduite à 0.

#### Niveau 4, 8, 10, 12, 16 : Amélioration de caractéristique
<!-- id: amelioration-de-caracteristique; type: choix-generique -->

#### Niveau 5 : Frappe malicieuse
<!-- id: frappe-malicieuse; type: automatique -->
Effets ajoutables à l'Attaque sournoise en sacrifiant des dés : Croc-en-jambe (1d6, À terre), Poison (1d6, Empoisonné), Repli (1d6, déplacement sans Opportunité).

#### Niveau 5 : Esquive instinctive
<!-- id: esquive-instinctive; type: automatique -->
Réaction : demi-dégâts d'une attaque touchée.

#### Niveau 6 : Expertise (bis)
<!-- id: expertise-bis; type: choix-expertise-2 -->

#### Niveau 7 : Esquive totale
<!-- id: esquive-totale; type: automatique -->
0 dégât si JS Dextérité réussi (demi-dégâts), demi-dégâts si raté (sauf Neutralisé).

#### Niveau 7 : Savoir-faire
<!-- id: savoir-faire; type: automatique -->
Un résultat ≤9 au d20 compte comme 10 pour un test avec compétence/outil maîtrisé.

#### Niveau 11 : Frappe malicieuse améliorée
<!-- id: frappe-malicieuse-amelioree; type: automatique -->
Jusqu'à deux effets de Frappe malicieuse combinés.

#### Niveau 14 : Coups vicieux
<!-- id: coups-vicieux; type: automatique -->
Nouveaux effets : Assommer (6d6, Inconscient), Aveugler (3d6, Aveuglé), Hébéter (2d6, action limitée).

#### Niveau 15 : Esprit fuyant
<!-- id: esprit-fuyant; type: automatique -->
Maîtrise des JS de Sagesse et Charisme.

#### Niveau 18 : Insaisissable
<!-- id: insaisissable; type: automatique -->
Aucune attaque ne peut avoir l'Avantage contre vous (sauf Neutralisé).

#### Niveau 19 : Faveur épique
<!-- id: faveur-epique; type: automatique -->
<!-- id: faveur-epique-don; don-categorie: Faveur épique -->
Faveur de l'esprit nocturne recommandé.

#### Niveau 20 : Coup de chance
<!-- id: coup-de-chance; type: automatique -->
Conversion d'un Test d20 raté en 20. 1/Repos court ou long.

### Multiclassage

- multiarmures : Armures légères
- multioutils : Outils de voleur
- maitcompB : 1 compétence au choix parmi celles du Roublard

### Sous-classe : Voleur

<!-- id: voleur -->

- description : Cambrioleur agile et rusé : mains rapides, escalade aisée, discrétion absolue et maniement de tout objet magique.

#### Niveau 3 : Mains lestes
<!-- id: mains-lestes; type: automatique -->
Action Bonus : test de Dextérité (Escamotage), crochetage, désamorçage d'un piège, action Utiliser un objet ou Utilisation d'un objet magique.

#### Niveau 3 : Monte-en-l'air
<!-- id: monte-en-l-air; type: automatique -->
Vitesse d'escalade égale à votre Vitesse ; distance de saut déterminée par la Dextérité.

#### Niveau 9 : Furtivité suprême
<!-- id: furtivite-supreme; type: automatique -->
Option d'Attaque sournoise Frappe discrète (1d6) : l'attaque ne met pas fin à l'état Invisible obtenu par l'action Se cacher.

#### Niveau 13 : Utilisation d'objets magiques
<!-- id: utilisation-d-objets-magiques; type: automatique -->
Harmonisation avec 4 objets magiques, chance de ne pas dépenser de charge (1d6 : 6), utilisation de tout parchemin de sort (test d'Intelligence (Arcanes) au besoin).

#### Niveau 17 : Réflexes de voleur
<!-- id: reflexes-de-voleur; type: automatique -->
Deux tours au premier round de chaque combat : le second à votre Initiative − 10.

---

## Emplacements de sort (lanceurs complets)

<!-- id: emplacements-sort -->

Table universelle (règle du multiclassage, `progression.md`) donnant le nombre
d'emplacements de sort par niveau de sort (1er à 9e), en fonction du **niveau de lanceur
effectif** : le niveau de classe pour un lanceur complet (Barde, Clerc, Druide,
Ensorceleur, Magicien — champ `typeincantation : complet`), la moitié arrondie au
supérieur du niveau de classe pour un demi-lanceur (Paladin, Rôdeur — `typeincantation :
demi`), ou la somme de ces valeurs pour un personnage multiclassé. L'Occultiste
(`typeincantation : pacte`) suit sa propre table Magie de pacte, déjà dans sa table de
progression (colonnes « Emplacements de sort » / « Niveau des emplacements »), non comprise
ici. Les classes non listées (`typeincantation : aucun` ou absent) n'ont pas d'emplacement.

| Niveau | 1er | 2e | 3e | 4e | 5e | 6e | 7e | 8e | 9e |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 2 | 3 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 3 | 4 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 4 | 4 | 3 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 5 | 4 | 3 | 2 | 0 | 0 | 0 | 0 | 0 | 0 |
| 6 | 4 | 3 | 3 | 0 | 0 | 0 | 0 | 0 | 0 |
| 7 | 4 | 3 | 3 | 1 | 0 | 0 | 0 | 0 | 0 |
| 8 | 4 | 3 | 3 | 2 | 0 | 0 | 0 | 0 | 0 |
| 9 | 4 | 3 | 3 | 3 | 1 | 0 | 0 | 0 | 0 |
| 10 | 4 | 3 | 3 | 3 | 2 | 0 | 0 | 0 | 0 |
| 11 | 4 | 3 | 3 | 3 | 2 | 1 | 0 | 0 | 0 |
| 12 | 4 | 3 | 3 | 3 | 2 | 1 | 0 | 0 | 0 |
| 13 | 4 | 3 | 3 | 3 | 2 | 1 | 1 | 0 | 0 |
| 14 | 4 | 3 | 3 | 3 | 2 | 1 | 1 | 0 | 0 |
| 15 | 4 | 3 | 3 | 3 | 2 | 1 | 1 | 1 | 0 |
| 16 | 4 | 3 | 3 | 3 | 2 | 1 | 1 | 1 | 0 |
| 17 | 4 | 3 | 3 | 3 | 2 | 1 | 1 | 1 | 1 |
| 18 | 4 | 3 | 3 | 3 | 3 | 1 | 1 | 1 | 1 |
| 19 | 4 | 3 | 3 | 3 | 3 | 2 | 1 | 1 | 1 |
| 20 | 4 | 3 | 3 | 3 | 3 | 2 | 2 | 1 | 1 |

---

## Tableau récapitulatif

| Classe | Caractéristique principale | Dé de vie | Complexité |
|---|---|---|---|
| Barbare | Force | d12 | Moyenne |
| Barde | Charisme | d8 | Élevée |
| Clerc | Sagesse | d8 | Moyenne |
| Druide | Sagesse | d8 | Élevée |
| Ensorceleur | Charisme | d6 | Élevée |
| Guerrier | Force ou Dextérité | d10 | Basse |
| Magicien | Intelligence | d6 | Moyenne |
| Moine | Dextérité et Sagesse | d8 | Élevée |
| Occultiste | Charisme | d8 | Élevée |
| Paladin | Force et Charisme | d10 | Moyenne |
| Rôdeur | Dextérité et Sagesse | d10 | Moyenne |
| Roublard | Dextérité | d8 | Basse |
