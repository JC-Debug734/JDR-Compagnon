# Espèces — SRD 5.2.1

Ce fichier recense les 9 espèces jouables du Document de Référence du Système (D&D 5e, SRD 5.2.1), au format structuré pour import dans JDR Compagnon.

<!--
BALISES (commentaires invisibles à l'affichage, lus par l'application) :

Sur un trait (ligne juste sous "#### Nom du trait") ou sous la ligne "Catégorie de taille" :
  id: identifiant unique dans l'espèce (obligatoire).
  choix: N            -> le joueur doit choisir N option(s) ; déclenche un choix à la création.
  effet: <cible>      -> où va la valeur choisie (voir cibles ci-dessous).
  options: ...        -> liste "A, B, C" ; "toutes" (les 18 compétences) ;
                         "dons-origines" (dons balisés "categorie: Origines" de dons_srd521.md) ;
                         "langues" (langues.md) ; "equipement: X" (objets de equipement_srd521.md
                         dont le nom commence par X ou dont la catégorie contient X) ;
                         plusieurs sources séparées par "|" ;
                         absent avec effet: option -> les lignes "option:" du trait.
  recommande: X       -> option mise en avant lors du choix.
  libelle: X          -> texte affiché sur la fiche devant la valeur choisie.
  remplace: X         -> le texte X de l'équipement de départ est remplacé par la valeur choisie.
  choix-en-jeu: X     -> choix fait pendant la partie (jamais demandé à la création),
                         rappelé sur la fiche sous le trait.
  niveau: N           -> trait qui ne s'applique qu'à partir du niveau de personnage N
                         (rappelé lors de la montée de niveau).
  Cibles automatiques (sans choix) : competences, resistances, sorts, taille, vitesse,
  pv-par-niveau (ex. "pv-par-niveau: 1").

Sur une option d'un choix "effet: option" (ligne sous la table ou la liste d'options) :
  option: Nom (doit correspondre à la 1re cellule de la table ou au nom en gras de la liste)
  resistances / sorts / vitesse : appliqués si l'option est choisie ;
  sorts-3, sorts-5... : sort appris au niveau de personnage indiqué.

Cibles : competences -> maîtrises de compétence ; resistances -> résistances aux dégâts ;
sorts -> sorts connus ; don -> dons ; taille -> catégorie de taille ; vitesse -> vitesse (m) ;
pv-par-niveau -> PV max (+N à la création puis à chaque niveau) ;
outils -> outils maîtrisés ; langues -> langues connues ; equipement -> objet du sac (avec remplace:) ;
competences-ou-outils -> compétence ou outil selon la valeur ;
incantation / note / option -> noté dans le trait (ou le don) sur la fiche.
Tout trait est aussi copié dans "Traits d'espèce" de la fiche, avec le choix et sa raison.

Les mêmes balises servent dans classes_srd521.md (sous un champ des Traits de base, de la
section Multiclassage ou dans une aptitude), historiques_srd521.md et dons_srd521.md
(sous "### Nom du don" ; "categorie: Origines" y marque un don d'origines).
-->

---

## Drakéide

<!-- id: drakeide -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 1,50 m et 2,10 m)
<!-- id: taille; taille: Moyenne -->
- Vitesse : 9 m

### Traits spéciaux

#### Ascendance draconique
<!-- id: ascendance-draconique; choix: 1; effet: option -->
Votre lignée remonte à un géniteur dragon. Choisissez le type de dragon dans la table « Ancêtres draconiques ». Ce choix affecte vos traits Souffle et Résistance aux dégâts, ainsi que votre apparence.

**Table : Ancêtres draconiques**

| Dragon | Type de dégâts | Dragon | Type de dégâts |
|---|---|---|---|
| Airain | Feu | Cuivre | Acide |
| Argent | Froid | Noir | Acide |
| Blanc | Froid | Or | Feu |
| Bleu | Foudre | Rouge | Feu |
| Bronze | Foudre | Vert | Poison |

<!-- option: Airain; resistances: Feu -->
<!-- option: Argent; resistances: Froid -->
<!-- option: Blanc; resistances: Froid -->
<!-- option: Bleu; resistances: Foudre -->
<!-- option: Bronze; resistances: Foudre -->
<!-- option: Cuivre; resistances: Acide -->
<!-- option: Noir; resistances: Acide -->
<!-- option: Or; resistances: Feu -->
<!-- option: Rouge; resistances: Feu -->
<!-- option: Vert; resistances: Poison -->

#### Résistance aux dégâts
<!-- id: resistance-degats -->
Vous bénéficiez de la résistance au type de dégâts déterminé par votre trait Ascendance draconique.

#### Souffle
<!-- id: souffle; choix-en-jeu: forme du Souffle (Cône de 4,50 m ou Ligne de 9 m) à chaque utilisation -->

Lorsque vous entreprenez l'action Attaque à votre tour, vous pouvez remplacer l'une de vos attaques par une expiration d'énergie magique sous la forme d'un Cône de 4,50 m ou d'une Ligne de 9 m de long et 1,50 m de large (choisissez la forme à chaque fois). Chaque créature prise dans la zone effectue un jet de sauvegarde de Dextérité (DD 8 + votre modificateur de Constitution + votre bonus de maîtrise). En cas d'échec, la créature subit 1d10 dégâts du type déterminé par votre trait Ascendance draconique. En cas de réussite, elle ne subit que la moitié de ces dégâts. Les dégâts augmentent de 1d10 lorsque vous atteignez les niveaux de personnage 5 (2d10), 11 (3d10) et 17 (4d10).

Vous pouvez recourir à ce Souffle autant de fois que votre bonus de maîtrise, et récupérez ce quota en terminant un Repos long.

#### Vision dans le noir
<!-- id: vision-noir -->
Vous disposez de la Vision dans le noir sur 18 m.

#### Vol draconique
<!-- id: vol-draconique; niveau: 5 -->
Lorsque vous atteignez le niveau de personnage 5, vous pouvez canaliser la magie draconique pour vous octroyer le vol de façon temporaire. Par une action Bonus, il vous pousse des ailes spectrales dans le dos qui persistent 10 minutes ou jusqu'à ce que vous rétractiez ces ailes (pas d'action requise) ou subissiez l'état Neutralisé. Pour toute la durée, vous disposez d'une Vitesse de vol égale à votre Vitesse.

Une fois ce trait utilisé, vous devez terminer un Repos long pour pouvoir y recourir de nouveau.

---

## Elfe

<!-- id: elfe -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 1,50 m et 1,80 m)
<!-- id: taille; taille: Moyenne -->
- Vitesse : 9 m

### Traits spéciaux

#### Ascendance féerique
<!-- id: ascendance-feerique -->
Vous avez l'Avantage aux jets de sauvegarde visant à éviter l'état Charmé ou à y mettre un terme.

#### Lignage elfique
<!-- id: lignage-elfique; choix: 1; effet: option -->
<!-- id: lignage-elfique-incantation; choix: 1; effet: incantation; options: Intelligence, Sagesse, Charisme -->
<!-- id: haut-elfe-sort; choix-en-jeu: Haut-elfe — remplacer le sort mineur par un sort mineur de Magicien après chaque Repos long -->
Vous appartenez à un lignage qui vous octroie des aptitudes surnaturelles. Choisissez-en un dans la table « Lignages elfiques ». Vous recevez le bénéfice du niveau 1 de ce lignage.

Lorsque vous atteignez les niveaux de personnage 3 et 5, vous apprenez un sort de niveau supérieur, comme indiqué sur la table. Ce sort est toujours considéré comme préparé pour vous. Vous pouvez le lancer une fois sans dépenser d'emplacement de sort et récupérez cette aptitude en terminant un Repos long. Vous pouvez également le lancer avec vos éventuels emplacements de sort de niveau adéquat.

L'Intelligence, la Sagesse ou le Charisme est votre caractéristique d'incantation pour les sorts que vous lancez avec ce trait (décidez de la caractéristique lors de la sélection du lignage).

**Table : Lignages elfiques**

| Lignage | Niveau 1 | Niveau 3 | Niveau 5 |
|---|---|---|---|
| Drow | La portée de votre Vision dans le noir passe à 36 m. Vous connaissez également le sort mineur lumières dansantes. | lueurs féeriques | ténèbres |
| Elfe sylvestre | Votre Vitesse passe à 10,50 m. Vous connaissez également le sort mineur druidisme. | grande foulée | passage sans trace |
| Haut-elfe | Vous connaissez le sort mineur prestidigitation. Chaque fois que vous terminez un Repos long, vous pouvez remplacer ce sort mineur par un autre sort mineur de la liste de sorts du Magicien. | détection de la magie | foulée brumeuse |

<!-- option: Drow; sorts: Lumières dansantes; sorts-3: Lueurs féeriques; sorts-5: Ténèbres -->
<!-- option: Elfe sylvestre; vitesse: 10,5; sorts: Druidisme; sorts-3: Grande foulée; sorts-5: Passage sans trace -->
<!-- option: Haut-elfe; sorts: Prestidigitation; sorts-3: Détection de la magie; sorts-5: Foulée brumeuse -->

#### Sens aiguisés
<!-- id: sens-aiguises; choix: 1; effet: competences; options: Intuition, Perception, Survie -->
Vous bénéficiez de la maîtrise de la compétence Intuition, Perception ou Survie au choix.

#### Transe
<!-- id: transe -->
Vous vous passez de sommeil et la magie ne peut pas vous endormir. Vous pouvez terminer un Repos long en 4 heures si vous les passez en transe méditative durant laquelle vous restez en éveil.

#### Vision dans le noir
<!-- id: vision-noir -->
Vous disposez de la Vision dans le noir sur 18 m.

---

## Gnome

<!-- id: gnome -->

- Type de créature : Humanoïde
- Catégorie de taille : P (petite, entre 90 cm et 1,20 m)
<!-- id: taille; taille: Petite -->
- Vitesse : 9 m

### Traits spéciaux

#### Lignage gnome
<!-- id: lignage-gnome; choix: 1; effet: option -->
<!-- id: lignage-gnome-incantation; choix: 1; effet: incantation; options: Intelligence, Sagesse, Charisme -->
<!-- id: appareil-gnome; choix-en-jeu: Gnome des roches — effet de prestidigitation de chaque appareil, fixé à sa création -->
Vous appartenez à un lignage qui vous octroie des aptitudes surnaturelles. Choisissez l'une des options ci-après ; quelle que soit celle que vous choisissez, l'Intelligence, la Sagesse ou le Charisme est votre caractéristique d'incantation pour les sorts que vous lancez avec ce trait (décidez de la caractéristique lors de la sélection du lignage) :

- **Gnome des forêts.** Vous connaissez le sort mineur illusion mineure. Le sort communication avec les animaux est en outre considéré pour vous comme toujours préparé. Vous pouvez le lancer sans emplacement de sort autant de fois que votre bonus de maîtrise, et récupérez ce quota en terminant un Repos long. Vous pouvez en outre dépenser les emplacements de sort adaptés dont vous disposez pour lancer ce sort.
- **Gnome des roches.** Vous connaissez les sorts mineurs prestidigitation et réparation. Vous pouvez en outre consacrer 10 minutes à lancer prestidigitation pour créer un appareil mécanique de taille TP (CA 5, 1 pv) tel qu'un jouet, un allume-feu ou une boîte à musique. Lorsque vous créez l'appareil, vous déterminez sa fonction en choisissant un effet parmi ceux de prestidigitation ; l'appareil produit cet effet chaque fois que vous-même ou une autre créature entreprenez une action Bonus pour l'activer par simple contact. Lorsque l'effet choisi comporte des options, vous devez en choisir une pour l'appareil à sa création. Vous pouvez avoir trois appareils actifs de ce type à la fois, et chacun tombe en morceaux 8 heures après sa création ou lorsque vous le démantelez par simple contact en entreprenant l'action Utilisation.

<!-- option: Gnome des forêts; sorts: Illusion mineure, Communication avec les animaux -->
<!-- option: Gnome des roches; sorts: Prestidigitation, Réparation -->

#### Ruse gnome
<!-- id: ruse-gnome -->
Vous avez l'Avantage aux jets de sauvegarde d'Intelligence, de Sagesse et de Charisme.

#### Vision dans le noir
<!-- id: vision-noir -->
Vous disposez de la Vision dans le noir sur 18 m.

---

## Goliath

<!-- id: goliath -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 2,10 m et 2,40 m)
<!-- id: taille; taille: Moyenne -->
- Vitesse : 10,50 m

### Traits spéciaux

#### Ascendance gigante
<!-- id: ascendance-gigante; choix: 1; effet: option -->
Vous êtes un lointain descendant des géants. Choisissez l'un des bénéfices ci-après, une faveur surnaturelle liée à vos ancêtres ; vous pouvez recourir à ce bénéfice autant de fois que votre bonus de maîtrise et récupérez ce quota en terminant un Repos long :

- **Brûlure ignée (géants du feu).** Lorsque vous touchez une cible avec un jet d'attaque et lui infligez des dégâts, vous pouvez infliger 1d10 dégâts de feu supplémentaires à cette cible.
- **Endurance de la pierre (géants des pierres).** Lorsque vous subissez des dégâts, vous pouvez jouer votre Réaction pour lancer 1d12. Ajoutez votre modificateur de Constitution au résultat du dé et réduisez les dégâts de la somme obtenue.
- **Froid mordant (géants du givre).** Lorsque vous touchez une cible avec un jet d'attaque et lui infligez des dégâts, vous pouvez infliger 1d6 dégâts de froid supplémentaires à cette cible et réduire sa vitesse de 3 m jusqu'au début de votre tour suivant.
- **Renversement des coteaux (géants des collines).** Lorsque vous touchez une créature de taille G ou inférieure avec un jet d'attaque et lui infligez des dégâts, vous pouvez faire subir à cette cible l'état À terre.
- **Saut des nuées (géants des nuages).** Par une action Bonus, vous pouvez vous téléporter magiquement d'un maximum de 9 m dans un espace inoccupé que vous voyez.
- **Tonnerre des cieux (géants des tempêtes).** Lorsque vous subissez des dégâts de la part d'une créature dans un rayon de 18 m, vous pouvez jouer votre Réaction pour lui infliger 1d8 dégâts de tonnerre.

<!-- option: Brûlure ignée -->
<!-- option: Endurance de la pierre -->
<!-- option: Froid mordant -->
<!-- option: Renversement des coteaux -->
<!-- option: Saut des nuées -->
<!-- option: Tonnerre des cieux -->

#### Forme de géant
<!-- id: forme-de-geant; niveau: 5 -->
À partir du niveau de personnage 5, vous pouvez changer de catégorie de taille et devenir de taille G par une action Bonus, à condition que l'espace que vous occupez soit suffisamment vaste. Cette transformation dure 10 minutes ou jusqu'à ce que vous y mettiez un terme (pas d'action requise). Pour toute la durée, vous avez l'Avantage aux tests de Force et votre Vitesse augmente de 3 m.

Une fois ce trait utilisé, vous devez terminer un Repos long pour pouvoir y recourir de nouveau.

#### Forte carrure
<!-- id: forte-carrure -->
Vous avez l'Avantage à tous les tests de caractéristique que vous effectuez pour mettre un terme à l'état Agrippé. Votre catégorie de taille est en outre considérée comme du cran immédiatement supérieur pour ce qui est de définir votre capacité de charge.

---

## Halfelin

<!-- id: halfelin -->

- Type de créature : Humanoïde
- Catégorie de taille : P (petite, entre 60 cm et 90 cm)
<!-- id: taille; taille: Petite -->
- Vitesse : 9 m

### Traits spéciaux

#### Agilité halfeline
<!-- id: agilite-halfeline -->
Vous pouvez vous déplacer dans l'espace de toute créature d'une catégorie de taille supérieure à la vôtre, mais ne pouvez pas vous arrêter dans le même espace que celle-ci.

#### Brave
<!-- id: brave -->
Vous avez l'Avantage aux jets de sauvegarde visant à éviter l'état Effrayé ou à y mettre un terme.

#### Chance
<!-- id: chance -->
Lorsque vous obtenez un 1 au d20 d'un Test d20, vous pouvez relancer le dé, mais devez utiliser le nouveau résultat.

#### Discrétion naturelle
<!-- id: discretion-naturelle -->
Il vous suffit d'être dans l'ombre d'une créature dont la catégorie de taille est d'au moins un cran supérieure à la vôtre pour pouvoir entreprendre l'action Furtivité.

---

## Humain

<!-- id: humain -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 1,20 m et 2,10 m) ou P (petite, de 60 cm à 1,20 m), à choisir lors de la sélection de l'espèce
<!-- id: taille; choix: 1; effet: taille; options: Moyenne, Petite -->
- Vitesse : 9 m

### Traits spéciaux

#### Compétent
<!-- id: competent; choix: 1; effet: competences; options: toutes -->
Vous recevez la maîtrise d'une compétence de votre choix.

#### Ingénieux
<!-- id: ingenieux -->
Vous recevez l'Inspiration héroïque chaque fois que vous terminez un Repos long.

#### Polyvalent
<!-- id: polyvalent; choix: 1; effet: don; options: dons-origines; recommande: Doué -->
Vous recevez le don d'origines de votre choix (cf. « Dons »). Doué est recommandé.

---

## Nain

<!-- id: nain -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 1,20 m et 1,50 m)
<!-- id: taille; taille: Moyenne -->
- Vitesse : 9 m

### Traits spéciaux

#### Connaissance de la pierre
<!-- id: connaissance-pierre -->
Par une action Bonus, vous recevez la Perception des vibrations à 18 m pendant 10 minutes. Cette Perception des vibrations nécessite que vous soyez sur une surface en pierre ou en contact physique avec une telle surface. Cette pierre peut être naturelle ou façonnée.

Vous pouvez recourir à cette action Bonus autant de fois que votre bonus de maîtrise, et récupérez ce quota en terminant un Repos long.

#### Résistance naine
<!-- id: resistance-naine; resistances: Poison -->
Vous bénéficiez de la Résistance aux dégâts de poison. Vous avez en outre l'Avantage aux jets de sauvegarde visant à éviter l'état Empoisonné ou à y mettre un terme sur vous-même.

#### Ténacité naine
<!-- id: tenacite-naine; pv-par-niveau: 1 -->
Votre maximum de points de vie augmente de 1, et augmente encore de 1 chaque fois que vous gagnez un niveau.

#### Vision dans le noir
<!-- id: vision-noir -->
Vous disposez de la Vision dans le noir sur 36 m.

---

## Orc

<!-- id: orc -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 1,80 m et 2,10 m)
<!-- id: taille; taille: Moyenne -->
- Vitesse : 9 m

### Traits spéciaux

#### Acharnement
<!-- id: acharnement -->
Si vous tombez à 0 point de vie sans être tué sur le coup, vous pouvez en fait vous retrouver à 1 point de vie. Une fois ce trait utilisé, vous devez terminer un Repos long pour pouvoir y recourir de nouveau.

#### Poussée d'adrénaline
<!-- id: poussee-adrenaline -->
Vous pouvez entreprendre l'action Pointe par une action Bonus. Quand vous faites ainsi, vous recevez un nombre de points de vie temporaires égal à votre bonus de maîtrise.

Vous pouvez recourir à ce trait autant de fois que votre bonus de maîtrise, et récupérez ce quota en terminant un Repos court ou long.

#### Vision dans le noir
<!-- id: vision-noir -->
Vous disposez de la Vision dans le noir sur 36 m.

---

## Tieffelin

<!-- id: tieffelin -->

- Type de créature : Humanoïde
- Catégorie de taille : M (moyenne, entre 1,20 m et 2,10 m) ou P (petite, de 90 cm à 1,20 m), à choisir lors de la sélection de l'espèce
<!-- id: taille; choix: 1; effet: taille; options: Moyenne, Petite -->
- Vitesse : 9 m

### Traits spéciaux

#### Héritage fiélon
<!-- id: heritage-fielon; choix: 1; effet: option -->
<!-- id: heritage-fielon-incantation; choix: 1; effet: incantation; options: Intelligence, Sagesse, Charisme -->
Votre héritage vous octroie des aptitudes surnaturelles. Choisissez un héritage dans la table « Héritages fiélons ». Vous recevez le bénéfice de niveau 1 de l'héritage choisi.

Lorsque vous atteignez les niveaux de personnage 3 et 5, vous apprenez un sort de niveau supérieur, comme indiqué sur la table. Ce sort est toujours considéré comme préparé pour vous. Vous pouvez le lancer une fois sans dépenser d'emplacement de sort et récupérez cette aptitude en terminant un Repos long. Vous pouvez également le lancer avec vos éventuels emplacements de sort de niveau adéquat.

L'Intelligence, la Sagesse ou le Charisme est votre caractéristique d'incantation pour les sorts que vous lancez avec ce trait (décidez de la caractéristique lors de la sélection de l'héritage).

**Table : Héritages fiélons**

| Héritage | Niveau 1 | Niveau 3 | Niveau 5 |
|---|---|---|---|
| Abyssal | Vous bénéficiez de la Résistance aux dégâts de poison. Vous connaissez en outre le sort mineur bouffée de poison. | rayon empoisonné | immobilisation de personne |
| Chtonien | Vous bénéficiez de la Résistance aux dégâts nécrotiques. Vous connaissez en outre le sort mineur contact glacial. | simulacre de vie | rayon affaiblissant |
| Infernal | Vous bénéficiez de la Résistance aux dégâts de feu. Vous connaissez en outre le sort mineur trait de feu. | représailles infernales | ténèbres |

<!-- option: Abyssal; resistances: Poison; sorts: Bouffée de poison; sorts-3: Rayon empoisonné; sorts-5: Immobilisation de personne -->
<!-- option: Chtonien; resistances: Nécrotique; sorts: Contact glacial; sorts-3: Simulacre de vie; sorts-5: Rayon affaiblissant -->
<!-- option: Infernal; resistances: Feu; sorts: Trait de feu; sorts-3: Représailles infernales; sorts-5: Ténèbres -->

#### Présence d'outre-monde
<!-- id: presence-outre-monde; sorts: Thaumaturgie -->
Vous connaissez le sort mineur thaumaturgie. Lorsque vous le lancez avec ce trait, le sort utilise la même caractéristique d'incantation que celle du trait Héritage fiélon.

#### Vision dans le noir
<!-- id: vision-noir -->
Vous disposez de la Vision dans le noir sur 18 m.
