---
title: Document de référence du système 5.1
documentclass: book
fontsize: 12pt
geometry: "left=3cm,right=2cm,top=2cm,bottom=2cm"
output: pdf_document
toc-title: "Table des Matières"
header-includes:
   - \usepackage{fontspec}
   - \defaultfontfeatures{Mapping=tex-text,Scale=MatchLowercase}
   - \usepackage{libertine}
   - \addfontfeatures{RawFeature=+dlig;+pnum;+onum;+hlig;+liga;+clig;+alt}
   - \usepackage{setspace}
#   - \linespread{1.5}
toc: true
---

# Informations légales


> Ce travail comprend des éléments tirés du Document de référence du
> système 5.1 (\"SRD 5.1\") de Wizards of the Coast LLC, disponible sur
> https://dnd.wizards.com/resources/systems-reference-document. Le SRD
> 5.1 est sous licence Creative Commons Attribution 4.0 International
> License disponible sur
> https://creativecommons.org/licenses/by/4.0/legalcode.

# Utilisation des valeurs de caractéristique

Six capacités permettent de décrire rapidement les caractéristiques
physiques et mentales de chaque créature :

-   **Force,** mesure de la puissance physique
-   **Dextérité,** mesure de l'agilité
-   **Constitution,** mesure de l'endurance
-   **Intelligence,** mesure du raisonnement et de la mémoire
-   **Sagesse,** mesure de la perception et de l'intuition
-   **charisme,** mesure de la personnalité

Un personnage est-il musclé et perspicace ? Brillant et charmant ? Agile
et robuste ? Les valeurs de caractéristiques définissent ces qualités -
les atouts et les faiblesses d'une créature.

Les trois principaux jets du jeu - le test de capacité, le jet de
sauvegarde et le jet d'attaque - reposent sur les six scores de
capacité. L'introduction du livre décrit la règle de base de ces jets :
lancer un d20, ajouter un modificateur d'aptitude dérivé de l'un des
six scores d'aptitude, et comparer le total à un nombre cible.


## Valeurs de caractéristiques et modificateurs de capacités

Chaque capacité d'une créature possède un score, un nombre qui définit
l'ampleur de cette capacité. Une valeur de caractéristique n'est pas
seulement une mesure des capacités innées, mais englobe également
l'entraînement et la compétence d'une créature dans les activités
liées à cette capacité.

Un score de 10 ou 11 est la moyenne humaine normale, mais les
aventuriers et de nombreux monstres sont bien au-dessus de la moyenne
dans la plupart des capacités. Un score de 18 est le plus élevé qu'une
personne puisse atteindre en général. Les aventuriers peuvent avoir des
scores allant jusqu'à 20, et les monstres et les êtres divins peuvent
avoir des scores allant jusqu'à 30.

Chaque aptitude possède également un modificateur, dérivé du score et
allant de -5 (pour un score d'aptitude de 1) à +10 (pour un score de
30). Le tableau des scores et des modificateurs de capacité indique les
modificateurs de capacité pour la gamme des scores de capacité
possibles, de 1 à 30.

  ----------------------
   Score   Modificateur
  ------- --------------
     1          -5

    2-3         -4

    4-5         -3

    6-7         -2

    8-9         -1

   10-11        +0

   12-13        +1

   14-15        +2

   16-17        +3

   18-19        +4

   20-21        +5

   22-23        +6

   24-25        +7

   26-27        +8

   28-29        +9

    30         +10
  ----------------------

  : Valeurs de caractéristiques et modificateurs de capacités

Pour déterminer un modificateur d'aptitude sans consulter la table,
soustrayez 10 au score d'aptitude, puis divisez le total par 2
(arrondir à l'inférieur).

Parce que les modificateurs d'aptitude affectent presque tous les jets
d'attaque, les contrôles d'aptitude et les jets de sauvegarde, les
modificateurs d'aptitude apparaissent plus souvent en jeu que les
scores qui leur sont associés.



## Avantage et inconvénient

Parfois, une capacité spéciale ou un sort vous indique que vous avez un
avantage ou un désavantage sur un test de capacité, un jet de sauvegarde
ou un jet d'attaque. Dans ce cas, vous lancez un deuxième d20 lorsque
vous effectuez le jet. Utilisez le plus élevé des deux jets si vous avez
un avantage, et utilisez le plus faible des jets si vous avez un
désavantage. Par exemple, si vous avez un désavantage et que vous
obtenez un 17 et un 5, vous utilisez le 5. Si, au contraire, vous avez
un avantage et que vous obtenez ces chiffres, vous utilisez le 17.

Si plusieurs situations affectent un jet et que chacune d'entre elles
lui confère un avantage ou lui impose un désavantage, vous ne lancez pas
plus d'un d20 supplémentaire. Si deux situations favorables confèrent
un avantage, par exemple, vous ne lancez toujours qu'un seul d20
supplémentaire.

Si les circonstances font qu'un jet est à la fois avantageux et
désavantageux, on considère que vous n'avez ni l'un ni l'autre, et
vous lancez un d20. Ceci est vrai même si plusieurs circonstances
imposent un désavantage et qu'une seule accorde un avantage ou vice
versa. Dans une telle situation, vous n'avez ni avantage ni
désavantage.

Lorsque vous avez un avantage ou un désavantage et que quelque chose
dans le jeu, comme le trait Chanceux du halfelin, vous permet de
relancer le d20, vous ne pouvez relancer qu'un seul des dés. Vous
choisissez lequel. Par exemple, si un halfelin a un avantage ou un
désavantage sur un test de capacité et qu'il obtient un 1 et un 13, il
peut utiliser le trait Chanceux pour relancer le 1.

On obtient généralement un avantage ou un désavantage en utilisant des
capacités spéciales, des actions ou des sorts. L'inspiration peut
également donner un avantage à un personnage. Le MJ peut également
décider que les circonstances influencent un jet dans un sens ou dans
l'autre et accorder un avantage ou imposer un désavantage en
conséquence.



## Bonus de maîtrise supplémentaire

Les personnages ont un bonus de maîtrise déterminé par le niveau. Les
monstres ont également ce bonus, qui est incorporé dans leurs blocs de
statistiques. Le bonus est utilisé dans les règles sur les tests de
caractéristiques, les jets de sauvegarde et les jets d'attaque.

Votre bonus de maîtrise ne peut pas être ajouté plus d'une fois à un
seul jet de dé ou à un autre nombre. Par exemple, si deux règles
différentes stipulent que vous pouvez ajouter votre bonus de maîtrise à
un jet de sauvegarde de Sagesse, vous n'ajoutez néanmoins le bonus
qu'une seule fois lorsque vous effectuez la sauvegarde.

Parfois, votre bonus de maîtrise peut être multiplié ou divisé (doublé
ou divisé par deux, par exemple) avant que vous ne l'appliquiez. Par
exemple, la caractéristique Expertise du roublard double le bonus de
maîtrise pour certains tests de capacité. Si une circonstance suggère
que votre bonus de maîtrise s'applique plus d'une fois au même jet,
vous ne l'ajoutez qu'une fois et ne le multipliez ou le divisez
qu'une seule fois.

De même, si une caractéristique ou un effet vous permet de multiplier
votre bonus de maîtrise lors d'un test d'aptitude qui ne bénéficierait
pas normalement de votre bonus de maîtrise, vous n'ajoutez toujours pas
le bonus au test. Pour ce test, votre bonus de maîtrise est de 0, étant
donné que la multiplication de 0 par n'importe quel nombre donne
toujours 0. Par exemple, si vous ne maîtrisez pas la compétence
Histoire, vous ne tirez aucun avantage d'une caractéristique qui vous
permet de doubler votre bonus de maîtrise lorsque vous effectuez des
tests d'Intelligence (Histoire).

En général, vous ne multipliez pas votre bonus de maîtrise pour les jets
d'attaque ou de sauvegarde. Si une caractéristique ou un effet vous
permet de le faire, ces mêmes règles s'appliquent.



## Jets de caractéristiques

Un jet de caractéristique teste le talent et l'entraînement innés d'un
personnage ou d'un monstre dans le but de surmonter un défi. Le MJ
demande un test d'aptitude lorsqu'un personnage ou un monstre tente
une action (autre qu'une attaque) qui a une chance d'échouer. Lorsque
l'issue est incertaine, ce sont les dés qui déterminent les résultats.

Pour chaque test de capacité, le MJ décide laquelle des six capacités
est pertinente pour la tâche à accomplir et la difficulté de la tâche,
représentée par une classe de difficulté. Plus une tâche est difficile,
plus son DC est élevé. Le tableau des Classes de Difficulté typiques
montre les DCs les plus communs.

  -----------------------
  Difficulté de la    DC
  tâche              
  ------------------ ----
  Très facile         5

  Facile              10

  Moyen               15

  Hard                20

  Très difficile      25

  Presque impossible  30
  -----------------------

  : Classes de difficultés typiques

Pour effectuer un test de capacité, lancez un d20 et ajoutez le
modificateur de capacité approprié. Comme pour les autres jets de d20,
appliquez les bonus et les pénalités, et comparez le total au DC. Si le
total est égal ou supérieur au DC, le test d'aptitude est une réussite
: la créature surmonte le défi qui lui est posé. Dans le cas contraire,
c'est un échec, ce qui signifie que le personnage ou le monstre ne
progresse pas vers l'objectif ou progresse avec un revers déterminé par
le MJ.


### Concours

Parfois, les efforts d'un personnage ou d'un monstre sont directement
opposés à ceux d'un autre. Cela peut se produire lorsque les deux
essaient de faire la même chose et que seul l'un d'entre eux peut
réussir, par exemple lorsqu'ils tentent d'attraper un anneau magique
qui est tombé par terre. Cette situation s'applique également lorsque
l'un d'entre eux essaie d'empêcher l'autre d'accomplir un
objectif - par exemple, lorsqu'un monstre tente de forcer une porte
qu'un aventurier tient fermée. Dans ce genre de situation, l'issue est
déterminée par une forme spéciale de jet de caractéristique, appelée
concours.

Les deux participants à un concours effectuent des tests de capacité
adaptés à leurs efforts. Ils appliquent tous les bonus et pénalités
appropriés, mais au lieu de comparer le total à un DC, ils comparent les
totaux de leurs deux tests. Le participant dont le total des tests est
le plus élevé remporte le concours. Ce personnage ou monstre réussit
l'action ou empêche l'autre de réussir.

Si le concours aboutit à une égalité, la situation reste la même
qu'avant le concours. Ainsi, un des participants peut gagner le
concours par défaut. Si deux personnages sont à égalité dans un concours
pour arracher un anneau du sol, aucun d'entre eux ne l'attrape. Dans
un concours entre un monstre qui essaie d'ouvrir une porte et un
aventurier qui essaie de garder la porte fermée, une égalité signifie
que la porte reste fermée.



### Compétences

Chaque aptitude couvre un large éventail de capacités, y compris les
compétences dans lesquelles un personnage ou un monstre peut être
compétent. Une compétence représente un aspect spécifique d'un score de
capacité, et la maîtrise d'une compétence par un individu démontre
qu'il se concentre sur cet aspect. (Les compétences de départ d'un
personnage sont déterminées à la création du personnage, et les
compétences d'un monstre apparaissent dans le bloc de statistiques du
monstre).

Par exemple, un test de Dextérité peut refléter la tentative d'un
personnage de réaliser une acrobatie, de tenir un objet dans sa main ou
de rester caché. Chacun de ces aspects de la Dextérité a une compétence
associée : Acrobaties, Habileté de la main, et Discrétion,
respectivement. Ainsi, un personnage qui maîtrise la compétence
Discrétion est particulièrement doué pour les contrôles de Dextérité
liés à la furtivité et à la dissimulation.

Les compétences liées à chaque valeur de caractéristique sont indiquées
dans la liste suivante. (Aucune compétence n'est liée à la
Constitution.) Voir la description d'une compétence pour des exemples
d'utilisation d'une compétence associée à une compétence.


#### Force

-   Athlétisme



#### Dextérité

-   Acrobaties
-   Escamotage
-   Discrétion



#### Intelligence

-   Mystère
-   Histoire
-   Investigation
-   Nature
-   Religion



#### Sagesse

-   Dressage d'animaux
-   Intuition
-   Médecine
-   Perception
-   Survie



#### Charisme

-   Supercherie
-   Intimidation
-   Représentation
-   Persuasion

Parfois, le MJ peut demander un test de capacité utilisant une
compétence spécifique - par exemple, \"Faites un test de Sagesse
(Perception)\". Dans d'autres cas, un joueur peut demander au MJ si la
maîtrise d'une compétence particulière s'applique à un test. Dans les
deux cas, la maîtrise d'une compétence signifie qu'une personne peut
ajouter son bonus de maîtrise aux tests de capacité qui impliquent cette
compétence. Si le joueur ne maîtrise pas la compétence, il effectue un
test d'aptitude normal.

Par exemple, si un personnage tente d'escalader une falaise dangereuse,
le MJ peut demander un test de Force (Athlétisme). Si le personnage est
compétent en Athlétisme, le bonus de compétence du personnage est ajouté
au test de Force. Si le personnage ne possède pas cette compétence, il
effectue simplement un test de Force.



#### Variante : Compétences avec différentes aptitudes

Normalement, votre maîtrise d'une compétence ne s'applique qu'à un
type spécifique de test d'aptitude. La maîtrise de l'Athlétisme, par
exemple, s'applique généralement aux tests de Force. Dans certaines
situations, cependant, votre maîtrise peut raisonnablement s'appliquer
à un autre type de test. Dans ce cas, le MJ peut demander un test
utilisant une combinaison inhabituelle de capacité et de compétence, ou
vous pouvez demander à votre MJ si vous pouvez appliquer une compétence
à un autre test. Par exemple, si vous devez nager d'une île au large
vers le continent, votre MJ peut demander un test de Constitution pour
voir si vous avez l'endurance nécessaire pour parcourir cette distance.
Dans ce cas, votre MJ pourrait vous permettre d'appliquer votre
maîtrise de l'Athlétisme et vous demander un test de Constitution
(Athlétisme). Ainsi, si vous êtes compétent en Athlétisme, vous
appliquez votre bonus de compétence au test de Constitution comme vous
le feriez normalement pour un test de Force (Athlétisme). De même,
lorsque votre barbare demi-orc utilise une démonstration de force brute
pour intimider un ennemi, votre MJ peut demander un test de Force
(Intimidation), même si l'Intimidation est normalement associée au
Charisme.




### Chèques passifs

Un test passif est un type particulier de test de capacité qui
n'implique aucun jet de dé. Un tel test peut représenter le résultat
moyen d'une tâche effectuée de manière répétée, comme chercher des
portes secrètes encore et encore, ou peut être utilisé lorsque le MJ
veut déterminer secrètement si les personnages réussissent quelque chose
sans lancer de dé, comme remarquer un monstre caché.

Voici comment déterminer le total d'un personnage pour un test passif :

  -----------------------------------------------------------------------------
   10 + tous les modificateurs qui s'appliquent normalement à la vérification
  -----------------------------------------------------------------------------

Si le personnage a un avantage sur le test, ajoutez 5. Pour un
désavantage, soustrayez 5. Le jeu se réfère au total d'un test passif
comme un **score**.

Par exemple, si un personnage de 1er niveau a une Sagesse de 15 et une
maîtrise de la Perception, il a un score passif de Sagesse (Perception)
de 14.

Les règles de dissimulation de la section \"Dextérité\" ci-dessous
reposent sur des contrôles passifs, tout comme les règles
d'exploration.



### Travailler ensemble

Parfois, deux personnages ou plus s'associent pour tenter une tâche. Le
personnage qui dirige l'effort - ou celui qui a le modificateur
d'aptitude le plus élevé - peut faire un jet de caractéristique avec
avantage, reflétant l'aide fournie par les autres personnages. En
combat, cela nécessite l'action Aider.

Un personnage ne peut apporter son aide que si la tâche est une tâche
qu'il pourrait tenter seul. Par exemple, pour essayer d'ouvrir une
serrure, il faut avoir la maîtrise des outils de voleur, donc un
personnage qui n'a pas cette maîtrise ne peut pas aider un autre
personnage dans cette tâche. De plus, un personnage ne peut aider que si
deux ou plusieurs personnes travaillant ensemble sont réellement
productives. Certaines tâches, comme enfiler une aiguille, ne sont pas
plus faciles avec de l'aide.


#### Contrôles de groupe

Lorsqu'un certain nombre d'individus essaient d'accomplir quelque
chose en groupe, le MJ peut demander un test de capacité de groupe. Dans
une telle situation, les personnages qui sont doués pour une tâche
particulière aident à couvrir ceux qui ne le sont pas. Pour faire un
test de capacité de groupe, tout le monde dans le groupe fait le test de
capacité. Si au moins la moitié du groupe réussit, le groupe entier
réussit. Sinon, le groupe échoue. Les tests de groupe ne sont pas très
fréquents, et ils sont surtout utiles lorsque tous les personnages
réussissent ou échouent en tant que groupe. Par exemple, lorsque les
aventuriers naviguent dans un marais, le MJ peut demander un test de
Sagesse (Survie) de groupe pour voir si les personnages peuvent éviter
les sables mouvants, les gouffres et autres dangers naturels de
l'environnement. Si au moins la moitié du groupe réussit, les
personnages qui ont réussi sont capables de guider leurs compagnons hors
de danger. Sinon, le groupe trébuche sur l'un de ces dangers.





## Utiliser chaque capacité

Chaque tâche qu'un personnage ou un monstre peut tenter dans le jeu est
couverte par l'une des six capacités. Cette section explique plus en
détail la signification de ces capacités et la façon dont elles sont
utilisées dans le jeu.


### Force

La force mesure la puissance corporelle, l'entraînement athlétique et
la mesure dans laquelle vous pouvez exercer une force physique brute.


#### Contrôles de Force

Un test de Force peut modéliser toute tentative de soulever, pousser,
tirer ou briser quelque chose, de forcer votre corps à travers un
espace, ou d'appliquer la force brute à une situation. La compétence
Athlétisme reflète l'aptitude à effectuer certains types de tests de
Force.

***Athlétisme.*** Votre test de Force (Athlétisme) couvre les situations
difficiles que vous rencontrez lorsque vous grimpez, sautez ou nagez.
Les exemples incluent les activités suivantes :

-   Vous tentez d'escalader une falaise abrupte ou glissante, d'éviter
    les dangers en escaladant un mur ou de vous accrocher à une surface
    alors que quelque chose essaie de vous faire tomber.
-   Vous essayez de sauter sur une distance inhabituellement longue ou
    de réaliser une acrobatie en plein saut.
-   Vous luttez pour nager ou rester à flot dans les courants traîtres,
    les vagues de tempête ou les zones d'algues épaisses. Ou bien une
    autre créature essaie de vous pousser ou de vous tirer sous l'eau
    ou d'interférer de toute autre manière avec votre nage.

***Autres tests de Force.*** Le MJ peut également demander un test de
Force lorsque vous essayez d'accomplir des tâches telles que les
suivantes :

-   Forcer une porte bloquée, verrouillée ou barrée.
-   Libérez-vous de vos liens
-   Pousser dans un tunnel trop petit
-   S'accrocher à un chariot tout en étant traîné derrière lui.
-   Renverser une statue
-   Empêcher un rocher de rouler



#### Jets d'attaque et dégâts

Vous ajoutez votre modificateur de Force à votre jet d'attaque et à
votre jet de dégâts lorsque vous attaquez avec une arme de mêlée comme
une masse, une hache de guerre ou une javeline. Vous utilisez les armes
de mêlée pour effectuer des attaques de mêlée au corps à corps, et
certaines d'entre elles peuvent être lancées pour effectuer une attaque
à distance.



#### Levage et portage

Votre score de Force détermine la quantité de poids que vous pouvez
supporter. Les termes suivants définissent ce que vous pouvez soulever
ou porter.

***Capacité de charge.*** Votre capacité de charge correspond à votre
score de Force multiplié par 15. Il s'agit du poids (en livres) que
vous pouvez porter, qui est suffisamment élevé pour que la plupart des
personnages n'aient généralement pas à s'en soucier.

***Pousser, traîner ou soulever.*** Vous pouvez pousser, traîner ou
soulever un poids en livres jusqu'à deux fois votre capacité de charge
(ou 30 fois votre score de Force). Lorsque vous poussez ou traînez un
poids supérieur à votre capacité de charge, votre vitesse est réduite à
5 pieds.

***Taille et Force.*** Les créatures plus grandes peuvent supporter plus
de poids, tandis que les créatures Très petites peuvent en supporter
moins. Pour chaque catégorie de taille supérieure à la taille moyenne,
la capacité de charge de la créature et la quantité qu'elle peut
pousser, traîner ou soulever sont doublées. Pour une créature Très
petite (TP), ces poids sont divisés par deux.



#### Variante : Encumbrance

Les règles de soulèvement et de transport sont intentionnellement
simples. Voici une variante si vous cherchez des règles plus détaillées
pour déterminer comment un personnage est gêné par le poids de son
équipement. Lorsque vous utilisez cette variante, ignorez la colonne
Force de la table d'armure.

Si vous portez un poids supérieur à 5 fois votre score de Force, vous
êtes **encombré**, ce qui signifie que votre vitesse diminue de 10
pieds.

Si vous portez un poids supérieur à 10 fois votre valeur de
caractéristique, jusqu'à concurrence de votre capacité de charge
maximale, vous êtes **lourdement encombré**, ce qui signifie que votre
vitesse diminue de 20 pieds et que vous avez un désavantage sur les
tests de capacité, les jets d'attaque et les jets de sauvegarde qui
utilisent la Force, la Dextérité ou la Constitution.




### Dextérité

La Dextérité mesure l'agilité, les réflexes et l'équilibre.


#### Contrôles de Dextérité

Un test de Dextérité peut modéliser toute tentative de se déplacer de
manière agile, rapide ou silencieuse, ou d'éviter de tomber sur un pied
délicat. Les compétences Acrobaties, Escamotage et Discrétion reflètent
l'aptitude à effectuer certains types de tests de Dextérité.

***Acrobaties.*** Votre test de Dextérité (Acrobaties) couvre votre
tentative de rester sur vos pieds dans une situation délicate, comme
lorsque vous essayez de courir sur une plaque de glace, de tenir en
équilibre sur une corde raide, ou de rester droit sur le pont d'un
bateau qui tangue. Le MJ peut également demander un test de Dextérité
(Acrobaties) pour voir si vous pouvez effectuer des acrobaties, y
compris des plongeons, des roulades, des sauts périlleux et des flips.

***Escamotage.*** Chaque fois que vous tentez un acte de ligue ou de
ruse manuelle, comme planter quelque chose sur quelqu'un d'autre ou
dissimuler un objet sur votre personne, faites un test de Dextérité
(Escamotage). Le MJ peut également demander un test de Dextérité
(Escamotage) pour déterminer si vous pouvez soulever un porte-monnaie
d'une autre personne ou glisser quelque chose de la poche d'une autre
personne.

***Discrétion.*** Effectuez un test de Dextérité (Discrétion) lorsque
vous tentez de vous cacher de vos ennemis, de vous faufiler entre les
gardes, de vous éclipser sans être remarqué ou de vous approcher
furtivement de quelqu'un sans être vu ou entendu.

***Autres tests de Dextérité.*** Le MJ peut demander un test de
Dextérité lorsque vous essayez d'accomplir des tâches telles que les
suivantes :

-   Contrôlez un chariot lourdement chargé dans une descente abrupte.
-   Diriger un char dans un virage serré
-   Crocheter une serrure
-   Désactiver un piège
-   Attachez solidement un prisonnier
-   S'affranchir des liens
-   Jouer d'un instrument à cordes
-   Réaliser un objet petit ou détaillé



#### Jets d'attaque et dégâts

Vous ajoutez votre modificateur de Dextérité à votre jet d'attaque et à
votre jet de dégâts lorsque vous attaquez avec une arme à distance,
comme une fronde ou un arc long. Vous pouvez également ajouter votre
modificateur de Dextérité à votre jet d'attaque et à votre jet de
dégâts lorsque vous attaquez avec une arme de mêlée qui a la propriété
de finesse, comme une dague ou une rapière.



#### Classe d'armure

Selon l'armure que vous portez, vous pouvez ajouter une partie ou la
totalité de votre modificateur de Dextérité à votre classe d'armure.



#### Initiative

Au début de chaque combat, vous lancez un jet d'initiative en faisant
un test de Dextérité. L'initiative détermine l'ordre des tours des
créatures en combat.

> #### Cacher {#hiding}
>
> Le MJ décide quand les circonstances sont appropriées pour se cacher.
> Lorsque vous tentez de vous cacher, effectuez un test de Dextérité
> (Discrétion). Jusqu'à ce que vous soyez découvert ou que vous cessiez
> de vous cacher, le total de ce test est contesté par le test de
> Sagesse (Perception) de toute créature qui recherche activement des
> signes de votre présence.
>
> Vous ne pouvez pas vous cacher d'une créature qui vous voit
> clairement, et vous dévoilez votre position si vous faites du bruit,
> par exemple en criant un avertissement ou en renversant un vase.
>
> Une créature invisible peut toujours essayer de se cacher. Les signes
> de son passage peuvent toujours être remarqués, et elle doit rester
> silencieuse.
>
> En combat, la plupart des créatures restent vigilantes aux signes de
> danger tout autour, donc si vous sortez de votre cachette et vous
> approchez d'une créature, elle vous voit généralement. Cependant,
> dans certaines circonstances, le MJ peut vous autoriser à rester caché
> lorsque vous vous approchez d'une créature distraite, vous permettant
> ainsi d'obtenir un avantage sur un jet d'attaque avant d'être vu.
>
> ***Perception passive.*** Lorsque vous vous cachez, il y a une chance
> que quelqu'un vous remarque même s'il ne cherche pas. Pour
> déterminer si une telle créature vous remarque, le MJ compare votre
> test de Dextérité (Discrétion) avec le score de Sagesse passive
> (Perception) de cette créature, qui est égal à 10 + le modificateur de
> Sagesse de la créature, ainsi que tout autre bonus ou malus. Si la
> créature a un avantage, ajoutez 5. Pour un désavantage, soustrayez 5.
> Par exemple, si un personnage de 1er niveau (avec un bonus de maîtrise
> de +2) a une Sagesse de 15 (avec un modificateur de +2) et une
> compétence en Perception, il a une Sagesse passive (Perception) de 14.
>
> ***Que pouvez-vous voir ?*** L'un des principaux facteurs qui
> déterminent si vous pouvez trouver une créature ou un objet caché est
> votre capacité à voir dans une zone, qui peut être légèrement ou
> fortement obscurcie .




### Constitution

La Constitution mesure la santé, l'endurance et la force vitale.


#### Contrôles de la Constitution

Les tests de Constitution sont rares, et aucune compétence ne
s'applique aux tests de Constitution, parce que l'endurance que cette
capacité représente est largement passive plutôt que d'impliquer un
effort spécifique de la part d'un personnage ou d'un monstre. Un test
de Constitution peut cependant modéliser votre tentative de dépasser les
limites normales.

Le MJ peut demander un test de Constitution lorsque vous essayez
d'accomplir des tâches comme celles-ci :

-   Retenez votre souffle
-   Marche ou travail pendant des heures sans repos
-   Se passer de sommeil
-   Survivre sans nourriture ni eau
-   Dégustez une chope entière de bière en une seule fois.



#### Points de vie

Votre modificateur de Constitution contribue à vos points de vie. En
général, vous ajoutez votre modificateur de Constitution à chaque dé de
réussite que vous obtenez pour vos points de vie.

Si votre modificateur de Constitution change, votre maximum de points de
vie change également, comme si vous aviez le nouveau modificateur depuis
le 1er niveau. Par exemple, si vous augmentez votre score de
Constitution lorsque vous atteignez le 4ème niveau et que votre
modificateur de Constitution passe de +1 à +2, vous ajustez votre
maximum de points de vie comme si le modificateur avait toujours été de
+2. Vous ajoutez donc 3 points de vie pour vos trois premiers niveaux,
puis vous calculez vos points de vie pour le 4e niveau en utilisant
votre nouveau modificateur. Ou si vous êtes au 7ème niveau et qu'un
effet abaisse votre score de Constitution de façon à réduire votre
modificateur de Constitution de 1, votre maximum de points de vie est
réduit de 7.




### Intelligence

L'intelligence mesure l'acuité mentale, la précision de la mémoire et
la capacité de raisonnement.


#### Contrôles d'Intelligence

Un test d'Intelligence entre en jeu lorsque vous devez faire appel à la
logique, à l'éducation, à la mémoire ou au raisonnement déductif. Les
compétences en Arcane, Histoire, Investigation, Nature et Religion
reflètent l'aptitude à effectuer certains types de tests
d'Intelligence.

***Mystères.*** Votre test d'Intelligence (Arcane) mesure votre
capacité à vous souvenir de connaissances sur les sorts, les objets
magiques, les symboles eldritch, les traditions magiques, les plans
d'existence et les habitants de ces plans.

***Histoire.*** Votre test d'Intelligence (Histoire) mesure votre
capacité à vous souvenir d'événements historiques, de personnages
légendaires, de royaumes anciens, de conflits passés, de guerres
récentes et de civilisations disparues.

***Investigation.*** Lorsque vous cherchez des indices et que vous
faites des déductions à partir de ces indices, vous effectuez un test
d'Intelligence (Investigation). Vous pouvez déduire l'emplacement
d'un objet caché, discerner d'après l'apparence d'une blessure le
type d'arme qui l'a infligée, ou déterminer le point le plus faible
d'un tunnel qui pourrait le faire s'effondrer. L'exploration de
parchemins anciens à la recherche d'un fragment de savoir caché peut
également nécessiter un test d'Intelligence (Investigation).

***Nature.*** Votre test d'Intelligence (Nature) mesure votre capacité
à vous souvenir des connaissances sur le terrain, les plantes et les
animaux, le temps et les cycles naturels.

***Religion.*** Votre test d'Intelligence (Religion) permet de mesurer
votre capacité à vous souvenir de connaissances sur les divinités, les
rites et les prières, les hiérarchies religieuses, les symboles sacrés
et les pratiques des cultes secrets.

***Autres tests d'Intelligence.*** Le MJ peut demander un test
d'Intelligence lorsque vous essayez d'accomplir des tâches telles que
les suivantes :

-   Communiquer avec une créature sans utiliser de mots
-   Estimer la valeur d'un objet précieux
-   Préparez un déguisement pour passer pour un garde municipal.
-   Falsifier un document
-   Rappeler les traditions d'un métier ou d'une profession
-   Gagner un jeu de compétence



#### Caractéristique d'Incantation

Les magiciens utilisent l'Intelligence comme caractéristique
d'incantation, ce qui permet de déterminer les DC de jet de sauvegarde
des sorts qu'ils lancent.




### Sagesse

La Sagesse reflète votre degré de sensibilité au monde qui vous entoure
et représente la perspicacité et l'intuition.


#### Tests de Sagesse

Un test de Sagesse peut refléter un effort pour lire le langage
corporel, comprendre les sentiments de quelqu'un, remarquer des choses
sur l'environnement, ou soigner une personne blessée. Les compétences
Dressage, Intuition, Médecine, Perception et Survie reflètent
l'aptitude à effectuer certains types de tests de Sagesse.

***Dressage d'animaux.*** Lorsqu'on se demande si vous pouvez calmer
un animal domestique, empêcher une monture de s'effrayer ou deviner les
intentions d'un animal, le MJ peut demander un test de Sagesse
(Dressage). Vous effectuez également un test de Sagesse (Dressage
d'animaux) pour contrôler votre monture lorsque vous tentez une
manœuvre risquée.

***Intuition.*** Votre test de Sagesse (Intuition) détermine si vous
pouvez déterminer les véritables intentions d'une créature, par exemple
lorsque vous cherchez un mensonge ou que vous prédisez le prochain
mouvement de quelqu'un. Pour ce faire, vous devez glaner des indices
dans le langage corporel, les habitudes d'élocution et les changements
de comportement.

***Médecine.*** Un test de Sagesse (Médecine) vous permet d'essayer de
stabiliser un compagnon mourant ou de diagnostiquer une maladie.

***Perception.*** Votre test de Sagesse (Perception) vous permet de
repérer, d'entendre ou de détecter la présence de quelque chose. Il
mesure votre conscience générale de votre environnement et l'acuité de
vos sens. Par exemple, vous pouvez essayer d'entendre une conversation
à travers une porte fermée, écouter aux portes sous une fenêtre ouverte,
ou entendre des monstres se déplacer furtivement dans la forêt. Vous
pouvez aussi essayer de repérer des choses qui sont obscures ou faciles
à manquer, qu'il s'agisse d'orcs en embuscade sur une route, de
malfrats cachés dans l'ombre d'une ruelle ou de la lumière d'une
bougie sous une porte secrète fermée.

***Survie.*** Le MJ peut vous demander de faire un test de Sagesse
(Survie) pour suivre des pistes, chasser du gibier, guider votre groupe
à travers des terres gelées, identifier des signes indiquant que des
Ours-hibous vivent à proximité, prédire le temps, ou éviter les sables
mouvants et autres dangers naturels.

***Autres tests de Sagesse.*** Le MJ peut demander un test de Sagesse
lorsque vous essayez d'accomplir des tâches telles que les suivantes :

-   Avoir une intuition sur la ligne de conduite à suivre
-   Discerner si une créature apparemment morte ou vivante est
    morte-vivante.



#### Caractéristique d'Incantation

Les clercs, les druides et les rôdeurs utilisent la Sagesse comme
caractéristique d'incantation, ce qui permet de déterminer les jets de
sauvegarde des sorts qu'ils lancent.




### Charisme

Le charisme mesure votre capacité à interagir efficacement avec les
autres. Il comprend des facteurs tels que la confiance et l'éloquence,
et peut représenter une personnalité charmante ou autoritaire.


#### Contrôles du Charisme

Un test de Charisme peut survenir lorsque vous essayez d'influencer ou
de divertir les autres, lorsque vous essayez de faire une impression ou
de dire un mensonge convaincant, ou lorsque vous naviguez dans une
situation sociale délicate. Les compétences en Supercherie,
Intimidation, Représentation et Persuasion reflètent l'aptitude à
effectuer certains types de tests de Charisme.

***Supercherie.*** Votre test de Charisme (Supercherie) détermine si
vous pouvez cacher la vérité de manière convaincante, que ce soit
verbalement ou par vos actions. Cette supercherie peut aller de la
tromperie par l'ambiguïté au mensonge pur et simple. Les situations
typiques sont les suivantes : essayer de parler rapidement à un garde,
escroquer un marchand, gagner de l'argent en jouant, se faire passer
pour un déguisement, apaiser les soupçons de quelqu'un avec de fausses
assurances, ou garder un visage impassible tout en racontant un mensonge
flagrant.

***Intimidation.*** Lorsque vous tentez d'influencer quelqu'un par des
menaces ouvertes, des actions hostiles et la violence physique, le MJ
peut vous demander de faire un test de Charisme (Intimidation). Par
exemple, vous pouvez essayer de soutirer des informations à un
prisonnier, convaincre des malfrats de la rue de renoncer à une
confrontation, ou utiliser le bord d'une bouteille cassée pour
convaincre un vizir narquois de reconsidérer une décision.

***Représentation.*** Votre test de Charisme (Représentation) détermine
votre capacité à enchanter un public avec de la musique, de la danse, du
théâtre, des contes ou toute autre forme de divertissement.

***Persuasion.*** Lorsque vous tentez d'influencer quelqu'un ou un
groupe de personnes avec du tact, des grâces sociales ou une bonne
nature, le MJ peut vous demander de faire un test de Charisme
(Persuasion). Généralement, vous utilisez la Persuasion lorsque vous
agissez de bonne foi, pour favoriser les amitiés, faire des demandes
cordiales, ou faire preuve d'une étiquette correcte. Par exemple, vous
pouvez convaincre un chambellan de laisser votre groupe voir le roi,
négocier la paix entre des tribus en guerre ou inspirer une foule de
citadins.

***Autres tests de Charisme.*** Le MJ peut demander un test de Charisme
lorsque vous essayez d'accomplir des tâches telles que les suivantes :

-   Trouvez la meilleure personne à qui parler pour les nouvelles, les
    rumeurs et les potins.
-   Se fondre dans la foule pour avoir une idée des principaux sujets de
    conversation



#### Caractéristique d'Incantation

Les bardes, paladins, sorciers et sorciers utilisent le Charisme comme
caractéristique d'incantation, ce qui aide à déterminer les jets de
sauvegarde des sorts qu'ils lancent.





## Jets de sauvegarde

Un jet de sauvegarde - également appelé sauvegarde - représente une
tentative de résistance à un sort, un piège, un poison, une maladie ou
une menace similaire. Normalement, vous ne décidez pas de faire un jet
de sauvegarde ; vous y êtes contraint parce que votre personnage ou
votre monstre risque d'être blessé.

Pour effectuer un jet de sauvegarde, lancez un d20 et ajoutez le
modificateur de capacité approprié. Par exemple, vous utilisez votre
modificateur de Dextérité pour un jet de sauvegarde de Dextérité.

Un jet de sauvegarde peut être modifié par un bonus ou une pénalité
situationnelle et peut être affecté par un avantage ou un désavantage,
comme déterminé par le MJ.

Chaque classe donne la maîtrise d'au moins deux jets de sauvegarde. Le
magicien, par exemple, est compétent en jet de sauvegarde
d'Intelligence. Comme pour les compétences, la maîtrise d'un jet de
sauvegarde permet au personnage d'ajouter son bonus de compétence aux
jets de sauvegarde effectués en utilisant une valeur de caractéristique
particulière. Certains monstres ont également des compétences en jet de
sauvegarde.

La classe de difficulté d'un jet de sauvegarde est déterminée par
l'effet qui le provoque. Par exemple, le DC d'un jet de sauvegarde
autorisé par un sort est déterminé par la capacité d'incantation et le
bonus de maîtrise du lanceur de sorts.

Le résultat d'un jet de sauvegarde réussi ou raté est également
détaillé dans l'effet qui permet la sauvegarde. En général, un jet de
sauvegarde réussi signifie qu'une créature ne subit aucun dommage, ou
un dommage réduit, à cause d'un effet.



# Aventures


## Temps

Dans les situations où il est important de garder la trace du passage du
temps, le MJ détermine le temps que nécessite une tâche. Le MJ peut
utiliser une échelle de temps différente selon le contexte de la
situation. Dans un donjon, les déplacements des aventuriers se font à
l'échelle des **minutes**. Il leur faut environ une minute pour se
faufiler dans un long couloir, une autre minute pour vérifier l'absence
de pièges sur la porte au bout du couloir, et une bonne dizaine de
minutes pour fouiller la chambre au-delà à la recherche de quelque chose
d'intéressant ou de précieux.

Dans une ville ou une région sauvage, une échelle d'**heures** est
souvent plus appropriée. Les aventuriers désireux d'atteindre la tour
solitaire au cœur de la forêt se dépêchent de traverser ces quinze miles
en un peu moins de quatre heures.

Pour les longs voyages, une échelle de **jours** fonctionne mieux. En
suivant la route d'une nation à l'autre, les aventuriers passent
quatre jours sans histoire avant qu'une embuscade de Gobelins
n'interrompe leur voyage.

En combat et dans d'autres situations rapides, le jeu repose sur les
**rounds**, un laps de temps de 6 secondes.



## Mouvement

Traverser à la nage une rivière tumultueuse, se faufiler dans le couloir
d'un donjon, escalader le flanc d'une montagne dangereuse, toutes
sortes de mouvements jouent un rôle essentiel dans les aventures de jeux
fantastiques.

Le MJ peut résumer le déplacement des aventuriers sans calculer les
distances ou les temps de parcours exacts : \"Vous traversez la forêt et
trouvez l'entrée du donjon tard dans la soirée du troisième jour.\"
Même dans un donjon, en particulier un grand donjon ou un réseau de
grottes, le MJ peut résumer les déplacements entre les rencontres :
\"Après avoir tué le gardien à l'entrée de l'ancienne forteresse
naine, vous consultez votre carte, qui vous conduit à travers des
kilomètres de couloirs résonnants jusqu'à un gouffre enjambé par une
étroite arche de pierre.\"

Mais il est parfois important de savoir combien de temps il faut pour
aller d'un endroit à un autre, que la réponse soit en jours, en heures
ou en minutes. Les règles permettant de déterminer le temps de
déplacement dépendent de deux facteurs : la vitesse et le rythme de
déplacement des créatures qui se déplacent et le terrain sur lequel
elles se déplacent.


### Vitesse

Chaque personnage et monstre a une vitesse, qui est la distance en pieds
que le personnage ou le monstre peut parcourir en 1 round. Ce nombre
suppose de courtes bouffées de mouvement énergique au milieu d'une
situation de danger de mort.

Les règles suivantes déterminent la distance que peut parcourir un
personnage ou un monstre en une minute, une heure ou un jour.


#### Rythme de voyage

Lors d'un voyage, un groupe d'aventuriers peut se déplacer à un rythme
normal, rapide ou lent, comme indiqué sur la table de rythme de voyage.
Cette table indique la distance que le groupe peut parcourir en un
certain temps et si le rythme a un effet. Un rythme rapide rend les
personnages moins perceptifs, tandis qu'un rythme lent permet de se
faufiler et de fouiller une zone plus attentivement.

***Marche forcée.*** La table de rythme de voyage suppose que les
personnages voyagent pendant 8 heures par jour. Ils peuvent aller
au-delà de cette limite, au risque de s'épuiser.

Pour chaque heure de voyage supplémentaire au-delà de 8 heures, les
personnages parcourent la distance indiquée dans la colonne Heure pour
leur rythme, et chaque personnage doit effectuer un jet de sauvegarde de
Constitution à la fin de l'heure. Le jet de sauvegarde est de 10 + 1
pour chaque heure au-delà de 8 heures. En cas d'échec au jet de
sauvegarde, le personnage subit un niveau d'épuisement.

***Montures et véhicules.*** Pour de courtes durées (jusqu'à une
heure), de nombreux animaux se déplacent beaucoup plus vite que les
humanoïdes. Un personnage monté peut chevaucher au galop pendant environ
une heure, couvrant ainsi deux fois la distance habituelle pour un
rythme rapide. Si de nouvelles montures sont disponibles tous les 8 à 10
miles, les personnages peuvent couvrir de plus grandes distances à ce
rythme, mais cela est très rare, sauf dans les zones densément peuplées.

Les personnages dans les chariots, les calèches ou autres véhicules
terrestres choisissent une allure comme d'habitude. Les personnages
dans un navire sont limités à la vitesse du navire, et ils ne subissent
pas de pénalités pour un rythme rapide ou ne gagnent pas d'avantages
pour un rythme lent. Selon le navire et la taille de l'équipage, les
navires peuvent être capables de voyager jusqu'à 24 heures par jour.

Certaines montures spéciales, comme un pégase ou un griffon, ou des
véhicules spéciaux, comme un tapis volant, vous permettent de voyager
plus rapidement.

  ----------------------------------------------------------------------------------
  Pace          Distance\   Voyagé\   par\...\   \
                 Minute      Heure     Journée   Effet
  ------------ ----------- --------- ----------- -----------------------------------
  Rapidement    400 pieds   4 miles   30 miles   -Pénalité de 5 points aux scores
                                                 passifs de Sagesse (Perception).

  Normal        300 pieds   3 miles   24 miles   \-

  Lenteur       200 pieds   2 miles   18 miles   Capable d'utiliser la Discrétion
  ----------------------------------------------------------------------------------

  : Rythme de voyage



#### Terrain difficile

Les vitesses de déplacement indiquées dans la table de vitesse de
déplacement supposent un terrain relativement simple : routes, plaines
ouvertes ou couloirs de donjon dégagés. Mais les aventuriers sont
souvent confrontés à des forêts denses, des marais profonds, des ruines
remplies de décombres, des montagnes escarpées et des terrains
recouverts de glace - tous considérés comme des terrains difficiles.

Vous vous déplacez à la moitié de la vitesse en terrain difficile - se
déplacer de 1 pied en terrain difficile coûte 2 pieds de vitesse - de
sorte que vous pouvez couvrir seulement la moitié de la distance normale
en une minute, une heure ou un jour.




### Types de mouvements spéciaux

Pour se déplacer dans des donjons dangereux ou des zones sauvages, il ne
suffit souvent pas de marcher. Les aventuriers doivent parfois grimper,
ramper, nager ou sauter pour arriver à destination.


#### Grimper, nager et ramper

Lors de l'escalade ou de la natation, chaque pied de mouvement coûte 1
pied supplémentaire (2 pieds supplémentaires en terrain difficile), sauf
si une créature possède une vitesse d'escalade ou de natation. Au choix
du MJ, l'escalade d'une surface verticale glissante ou avec peu de
prises nécessite un test de Force (Athlétisme) réussi. De même, gagner
une certaine distance dans des eaux agitées peut nécessiter la réussite
d'un test de Force (Athlétisme).



#### Sauts

Votre Force détermine la distance à laquelle vous pouvez sauter.

***Saut en longueur.*** Lorsque vous effectuez un saut en longueur, vous
parcourez un nombre de pieds égal à votre score de Force si vous vous
déplacez d'au moins 3 mètres à pied juste avant le saut. Lorsque vous
faites un saut en longueur debout, vous ne pouvez sauter que sur la
moitié de cette distance. Dans les deux cas, chaque pied que vous
franchissez lors du saut vous coûte un pied de mouvement.

Cette règle suppose que la hauteur de votre saut n'a pas d'importance,
comme un saut à travers un ruisseau ou un gouffre. Au choix de votre MJ,
vous devez réussir un test de Force (Athlétisme) DC 10 pour franchir un
obstacle bas (pas plus haut qu'un quart de la distance du saut), comme
une haie ou un muret. Sinon, vous le heurtez.

Lorsque vous atterrissez en terrain difficile, vous devez réussir un
test de Dextérité (Acrobaties) DC 10 pour retomber sur vos pieds. Sinon,
vous atterrissez sur le ventre.

***Saut en hauteur.*** Lorsque vous effectuez un saut en hauteur, vous
sautez en l'air d'un nombre de pieds égal à 3 + votre modificateur de
Force si vous vous déplacez d'au moins 3 mètres à pied juste avant le
saut. Lorsque vous faites un saut en hauteur debout, vous ne pouvez
sauter que sur la moitié de cette distance. Dans tous les cas, chaque
pied que vous franchissez lors du saut vous coûte un pied de
déplacement. Dans certaines circonstances, votre MJ peut vous autoriser
à effectuer un test de Force (Athlétisme) pour sauter plus haut que vous
ne le pouvez normalement.

Vous pouvez étendre vos bras à la moitié de votre hauteur au-dessus de
vous pendant le saut. Ainsi, vous pouvez tendre les bras au-dessus de
vous sur une distance égale à la hauteur du saut plus 1 1/2 fois votre
taille.





## Environnement

Par nature, l'aventure implique de plonger dans des endroits sombres,
dangereux et pleins de mystères à explorer. Les règles de cette section
abritent quelques-uns des moyens les plus importants par lesquels les
aventuriers interagissent avec l'environnement dans de tels endroits.


### Falling

Une chute de grande hauteur est l'un des dangers les plus communs
auxquels est confronté un aventurier. À la fin d'une chute, une
créature subit 1d6 points de dégâts contondants par tranche de 10 pieds
de chute, jusqu'à un maximum de 20d6. La créature atterrit à plat
ventre, à moins qu'elle n'évite de subir les dégâts de la chute.



### Suffocant

Une créature peut retenir sa respiration pendant un nombre de minutes
égal à 1 + son modificateur de Constitution (minimum de 30 secondes).

Lorsqu'une créature manque de souffle ou s'étouffe, elle peut survivre
pendant un nombre de rounds égal à son modificateur de Constitution
(minimum de 1 round). Au début de son prochain tour, elle tombe à 0
point de vie et est en train de mourir, et elle ne peut pas regagner de
points de vie ou être stabilisée avant de pouvoir respirer à nouveau.

Par exemple, une créature avec une Constitution de 14 peut retenir sa
respiration pendant 3 minutes. Si elle commence à suffoquer, elle a 2
rounds pour atteindre l'air avant de tomber à 0 points de vie.



### Vision et lumière

Les tâches les plus fondamentales de l'aventure - remarquer un danger,
trouver des objets cachés, frapper un ennemi au combat et cibler un
sort, pour n'en citer que quelques-unes - dépendent fortement de la
capacité de vision du personnage. Les Ténèbres et autres effets qui
obscurcissent la vision peuvent s'avérer un obstacle important.

Une zone donnée peut être légèrement ou fortement obscurcie. Dans une
zone **faiblement obscurcie**, comme une lumière chétive, un brouillard
parsemé ou un feuillage modéré, les créatures ont un désavantage aux
tests de Sagesse (Perception) qui reposent sur la vue.

Une zone **fortement obscurcie** - comme les ténèbres, un brouillard
opaque ou un feuillage dense - bloque entièrement la vision. Une
créature souffre effectivement de l'état d'aveuglement  lorsqu'elle essaie de voir
quelque chose dans cette zone.

La présence ou l'absence de lumière dans un environnement crée trois
catégories d'éclairement : la lumière vive, la lumière chétive et les
ténèbres.

La**lumière vive** permet à la plupart des créatures de voir
normalement. Même les jours sombres fournissent une lumière vive, tout
comme les torches, les lanternes, les feux et autres sources
d'éclairage dans un rayon spécifique.

La**lumière chétive**, également appelée ombre, crée une zone légèrement
obscurcie. Une zone de lumière chétive est généralement une frontière
entre une source de lumière vive, telle qu'une torche, et l'obscurité
environnante. La lumière douce du crépuscule et de l'aube est également
considérée comme une lumière faible. Une pleine lune particulièrement
brillante peut baigner la terre d'une lumière faible.

Les**Ténèbres** créent une zone fortement obscurcie. Les personnages
sont confrontés aux Ténèbres en extérieur la nuit (même la plupart des
nuits de lune), dans les limites d'un donjon non éclairé ou d'une
voûte souterraine, ou dans une zone d'obscurité magique.


#### Vision aveugle

Une créature dotée de vision aveugle peut percevoir son environnement
sans se fier à la vue, dans un rayon spécifique. Les créatures sans
yeux, comme les oozes, et les créatures dotées d'écholocalisation ou de
sens exacerbés, comme les chauves-souris et les vrais dragons, possèdent
ce sens.



#### Vision dans le noir

De nombreuses créatures des mondes de jeux fantastiques, en particulier
celles qui vivent sous terre, ont une vision dans le noir. Dans une
portée donnée, une créature dotée de la vision dans le noir peut voir
dans l'obscurité comme si l'obscurité était une lumière chétive, de
sorte que les zones d'obscurité ne sont que légèrement obscurcies en ce
qui concerne cette créature. Cependant, la créature ne peut pas
discerner les couleurs dans l'obscurité, seulement les nuances de gris.



#### Vision véritable

Une créature dotée d'une vision véritable peut, jusqu'à une certaine
portée, voir dans les ténèbres normales et magiques, voir les créatures
et les objets invisibles, détecter automatiquement les illusions
visuelles et réussir ses jets de sauvegarde contre elles, et percevoir
la forme originale d'un métamorphe ou d'une créature transformée par
la magie. De plus, la créature peut voir dans le plan éthéré.




### Nourriture et eau

Les personnages qui ne mangent pas ou ne boivent pas subissent les
effets de l'épuisement .
L'épuisement causé par le manque de nourriture ou d'eau ne peut pas
être éliminé tant que le personnage n'a pas mangé et bu toute la
quantité requise.


#### Alimentation

Un personnage a besoin d'une livre de nourriture par jour et peut faire
durer la nourriture plus longtemps en subsistant avec des demi-rations.
Manger une demi-livre de nourriture dans une journée compte comme une
demi-journée sans nourriture.

Un personnage peut rester sans nourriture pendant un nombre de jours
égal à 3 + son modificateur de Constitution (minimum 1). À la fin de
chaque jour au-delà de cette limite, le personnage subit automatiquement
un niveau d'épuisement.

Une journée normale d'alimentation remet à zéro le nombre de jours sans
nourriture.



#### Eau

Un personnage a besoin d'un gallon d'eau par jour, ou de deux gallons
par jour si le temps est chaud. Un personnage qui ne boit que la moitié
de cette quantité d'eau doit réussir un jet de sauvegarde de
Constitution DC 15 ou subir un niveau d'épuisement à la fin de la
journée. Un personnage ayant accès à encore moins d'eau subit
automatiquement un niveau d'épuisement à la fin de la journée.

Si le personnage a déjà un ou plusieurs niveaux d'épuisement, il prend
deux niveaux dans les deux cas.





## Objets

L'interaction d'un personnage avec les objets d'un environnement est
souvent simple à résoudre dans le jeu. Le joueur dit au MJ que son
personnage fait quelque chose, comme déplacer un levier, et le MJ décrit
ce qui se passe, le cas échéant.

Par exemple, un personnage peut décider de tirer un levier qui, à son
tour, peut lever une herse, inonder une pièce ou ouvrir une porte
secrète dans un mur voisin. Mais si le levier est rouillé en position,
le personnage devra peut-être le forcer. Dans ce cas, le MJ peut
demander un test de Force pour voir si le personnage peut mettre le
levier en place. Le MJ fixe le DC de ce test en fonction de la
difficulté de la tâche.


### Briser des objets

Les personnages peuvent également endommager les objets avec leurs armes
et leurs sorts. Les objets sont immunisés contre le poison et les dégâts
psychiques, mais sinon ils peuvent être affectés par des attaques
physiques et magiques tout comme les créatures. Le MJ détermine la
classe d'armure et les points de vie d'un objet, et peut décider que
certains objets ont une résistance ou une immunité à certains types
d'attaques. (Il est difficile de couper une corde avec un gourdin, par
exemple.) Les objets échouent toujours aux jets de sauvegarde de Force
et de Dextérité, et ils sont immunisés contre les effets qui nécessitent
d'autres sauvegardes. Lorsqu'un objet tombe à 0 point de vie, il se
brise.

Un personnage peut également tenter un test de Force pour briser un
objet. Le MJ fixe le DC de ce test.

Lorsque les personnages doivent scier des cordes, briser une vitre ou
fracasser le cercueil d'un vampire, la seule règle absolue est la
suivante : avec suffisamment de temps et les bons outils, les
personnages peuvent détruire n'importe quel objet destructible.

Faites preuve de bon sens pour déterminer le succès d'un personnage à
endommager un objet. Un guerrier peut-il traverser une section d'un mur
de pierre avec une épée ? Non, l'épée risque de se briser avant le mur.

Aux fins de ces règles, un objet est un élément discret et inanimé comme
une fenêtre, une porte, une épée, un livre, une table, une chaise ou une
pierre, et non un bâtiment ou un véhicule composé de nombreux autres
objets.


#### Statistiques pour les objets

Lorsque le temps est un facteur, vous pouvez attribuer une classe
d'armure et des points de vie à un objet destructible. Vous pouvez
également lui conférer des immunités, des résistances et des
vulnérabilités à des types de dégâts spécifiques.

  -------------------------
  Substance             AC
  -------------------- ----
  Tissu, papier, corde  11

  Cristal, verre,       13
  glace                

  Bois, os              15

  Pierre                17

  Fer, acier            19

  Mithral               21

  Adamantine            23
  -------------------------

  : Classe d'armure de l'objet

  --------------------------------------------------------------
  Taille                                  Fragile    Résiliente
  -------------------------------------- ---------- ------------
  Très petite (bouteille, serrure)        2 (1d4)     5 (2d4)

  Petit (poitrine, luth)                  3 (1d6)     10 (3d6)

  Moyen (tonneau, lustre)                 4 (1d8)     18 (4d8)

  Grand (chariot, fenêtre de 10 pieds     5 (1d10)   27 (5d10)
  sur 10 pieds)                                     
  --------------------------------------------------------------

  : Points de vie des objets

***Classe d'armure.*** La classe d'armure d'un objet est une mesure
de la difficulté d'infliger des dégâts à l'objet lorsqu'il est frappé
(parce que l'objet n'a aucune chance d'esquiver). Le tableau Classe
d'armure des objets fournit des valeurs de CA suggérées pour diverses
substances.

***Objets Très grands et Gargantuesques.*** Les armes normales sont peu
utiles contre de nombreux objets Très grands et Gargantuesques, tels
qu'une statue colossale, une colonne de pierre imposante ou un rocher
massif. Cela dit, une torche peut brûler une Très grande tapisserie, et
un sort de *tremblement de terre* peut réduire un colosse
en ruines. Vous pouvez suivre les points de vie d'un objet Très grand
ou Gargantuesque si vous le souhaitez, ou vous pouvez simplement décider
de la durée pendant laquelle l'objet peut résister à l'arme ou à la
force qui agit sur lui. Si vous comptabilisez les points de vie de
l'objet, divisez-le en Grandes ou en petites sections, et comptabilisez
les points de vie de chaque section séparément. Détruire l'une de ces
sections peut ruiner l'objet tout entier. Par exemple, une statue
Gargantuesque d'un humain peut basculer si l'une de ses Grandes jambes
est réduite à 0 point de vie.

***Objets et types de dégâts.*** Les objets sont immunisés contre le
poison et les dégâts psychiques. Vous pouvez décider que certains types
de dégâts sont plus efficaces que d'autres contre un objet ou une
substance particulière. Par exemple, les dégâts contondants sont
efficaces pour briser des objets, mais pas pour couper une corde ou du
cuir. Les objets en papier ou en tissu peuvent être vulnérables aux
dégâts du feu et de la foudre. Un pic peut ébrécher une pierre mais ne
peut pas couper efficacement un arbre. Comme toujours, faites preuve de
discernement.

***Seuil de dégâts.*** Les gros objets tels que les murs des châteaux
ont souvent une résilience supplémentaire représentée par un seuil de
dégâts. Un objet avec un seuil de dégâts est immunisé contre tous les
dégâts, sauf s'il subit une quantité de dégâts d'une seule attaque ou
d'un seul effet égale ou supérieure à son seuil de dégâts, auquel cas
il subit des dégâts normalement. Tout dommage qui n'atteint ou ne
dépasse pas le seuil de dégâts de l'objet est considéré comme
superficiel et ne rapetisse pas les points de vie de l'objet. Points de
vie. Les points de vie d'un objet mesurent la quantité de dégâts qu'il
peut subir avant de perdre son intégrité structurelle. Les objets
résilients ont plus de points de vie que les objets fragiles. Les grands
objets ont également tendance à avoir plus de points de vie que les
petits, à moins que briser une petite partie de l'objet soit aussi
efficace que de le briser en entier. La table des points de vie des
objets fournit des suggestions de points de vie pour les objets fragiles
et résilients qui sont Grands ou plus petits.





## Reposez-vous

Aussi héroïques qu'ils puissent être, les aventuriers ne peuvent pas
passer toutes les heures de la journée dans l'exploration, les
interactions sociales et les combats. Ils ont besoin de temps de repos
pour dormir et manger, soigner leurs blessures, se rafraîchir l'esprit
et l'âme pour l'Incantation, et se consolider pour la suite de
l'aventure. Les aventuriers peuvent prendre de courts repos au milieu
d'une journée d'aventure et un long repos pour terminer la journée.


### Repos court

Un repos court est une période d'immobilisation d'au moins une heure
pendant laquelle le personnage ne fait rien de plus fatiguant que de
manger, boire, lire et soigner ses blessures.

Un personnage peut dépenser un ou plusieurs dés de coups à la fin d'un
repos court, dans la limite du nombre maximal de dés de coups du
personnage, qui est égal au niveau du personnage. Pour chaque dé de
points de vie dépensé de cette manière, le joueur lance le dé et y
ajoute le modificateur de Constitution du personnage. Le personnage
regagne un nombre de points de vie égal au total. Le joueur peut décider
de dépenser un Dé de Vie supplémentaire après chaque jet. Un personnage
regagne des dés de points de vie dépensés à la fin d'un long repos,
comme expliqué ci-dessous.



### Repos long

Un repos long est une période d'immobilisation prolongée, d'au moins 8
heures, pendant laquelle un personnage dort ou exerce une activité
légère : lire, parler, manger ou monter la garde pendant 2 heures
maximum. Si le repos est interrompu par une période d'activité intense
(au moins 1 heure de marche, de combat, de lancement de sorts ou
d'activités d'aventure similaires), les personnages doivent
recommencer le repos pour en tirer un quelconque avantage.

À la fin d'un repos long, un personnage regagne tous les points de vie
perdus. Le personnage regagne également les dés de coups dépensés,
jusqu'à un nombre de dés égal à la moitié du nombre total de dés de
coups du personnage (minimum d'un dé). Par exemple, si un personnage a
huit dés de points de vie, il peut en regagner quatre dépensés à la fin
d'un long repos.

Un personnage ne peut pas bénéficier de plus d'un repos long par
période de 24 heures, et il doit avoir au moins 1 point de vie au début
du repos pour en tirer les bénéfices.




## Entre deux aventures

Entre les voyages dans les donjons et les batailles contre les maux
anciens, les aventuriers ont besoin de temps pour se reposer, récupérer
et se préparer pour leur prochaine aventure.

De nombreux aventuriers utilisent également ce temps pour effectuer
d'autres tâches, comme la fabrication d'armes et d'armures, la
recherche ou la dépense de leur or durement gagné.

Dans certains cas, le passage du temps est quelque chose qui se produit
avec peu de fanfare ou de description. Au début d'une nouvelle
aventure, le MJ peut simplement déclarer qu'un certain temps s'est
écoulé et vous permettre de décrire en termes généraux ce que votre
personnage a fait. A d'autres moments, le MJ peut vouloir garder une
trace du temps qui passe, alors que des événements hors de votre
perception sont en mouvement.


### Dépenses liées au mode de vie

Entre deux aventures, vous choisissez une qualité de vie particulière et
payez le coût du maintien de ce mode de vie.

Le fait de mener un style de vie particulier n'a pas un effet énorme
sur votre personnage, mais votre style de vie peut affecter la façon
dont les autres individus et groupes réagissent à votre égard. Par
exemple, si vous menez un style de vie aristocratique, il vous sera
peut-être plus facile d'influencer les nobles de la ville que si vous
vivez dans la pauvreté.



### Activités en temps d'arrêt

Entre deux aventures, le MJ peut vous demander ce que votre personnage
fait pendant ses temps morts. Les périodes d'indisponibilité peuvent
varier en durée, mais chaque activité d'indisponibilité nécessite un
certain nombre de jours pour être achevée avant d'en tirer un
quelconque avantage, et au moins 8 heures de chaque jour doivent être
bénies à l'activité d'indisponibilité pour que le jour compte. Il
n'est pas nécessaire que les jours soient consécutifs. Si vous avez
plus que le nombre minimum de jours à passer, vous pouvez continuer à
faire la même chose pendant une période plus longue, ou passer à une
nouvelle activité d'arrêt.

Des activités de temps d'arrêt autres que celles présentées ci-dessous
sont possibles. Si vous souhaitez que votre personnage passe son temps
libre à effectuer une activité qui n'est pas couverte ici, discutez-en
avec votre MJ.


#### Artisanat

Vous pouvez fabriquer des objets non magiques, y compris des équipements
d'aventure et des œuvres d'art. Vous devez avoir une bonne maîtrise
des outils liés à l'objet que vous essayez de créer (généralement des
outils d'artisan). Vous pouvez également avoir besoin d'accéder à des
matériaux ou des lieux particuliers pour le créer. Par exemple, une
personne maîtrisant les outils du forgeron a besoin d'une forge pour
fabriquer une épée ou une armure.

Pour chaque jour d'arrêt que vous passez à fabriquer, vous pouvez
fabriquer un ou plusieurs objets dont la valeur marchande totale ne
dépasse pas 5 gp, et vous devez dépenser des matières premières valant
la moitié de la valeur marchande totale. Si l'objet que vous souhaitez
fabriquer a une valeur marchande supérieure à 5 gp, vous progressez
chaque jour par tranches de 5 gp jusqu'à ce que vous atteigniez la
valeur marchande de l'objet. Par exemple, il faut 300 jours pour
fabriquer soi-même une armure en plaques (valeur marchande 1 500 gp).

Plusieurs personnages peuvent combiner leurs efforts pour fabriquer un
seul objet, à condition qu'ils maîtrisent tous les outils nécessaires
et qu'ils travaillent ensemble au même endroit.

Chaque personnage fournit 5 gp d'effort pour chaque jour passé à
participer à la fabrication de l'objet. Par exemple, trois personnages
ayant la maîtrise des outils requis et les installations adéquates
peuvent fabriquer une armure harnois en 100 jours, pour un coût total de
750 gp.

Pendant l'artisanat, vous pouvez maintenir un mode de vie modeste sans
avoir à payer 1 gp par jour, ou un mode de vie confortable à la moitié
du coût normal.



#### Exercer une profession

Vous pouvez travailler entre deux aventures, ce qui vous permet de
maintenir un mode de vie modeste sans avoir à payer 1 gp par jour. Cet
avantage dure aussi longtemps que vous continuez à exercer votre
profession.

Si vous êtes membre d'une organisation susceptible de vous fournir un
emploi rémunéré, comme un temple ou une guilde de voleurs, vous gagnez
suffisamment pour mener une vie confortable.

Si vous avez la maîtrise de la compétence Représentation et que vous
mettez à profit cette compétence pendant vos temps morts, vous gagnez
suffisamment pour mener un style de vie aisé.



#### Récupérer

Vous pouvez utiliser les temps d'arrêt entre les aventures pour
récupérer d'une blessure, d'une maladie ou d'un poison débilitant.

Après trois jours d'arrêt passés à récupérer, vous pouvez effectuer un
jet de sauvegarde de Constitution DC 15. En cas de sauvegarde réussie,
vous pouvez choisir l'un des résultats suivants :

-   Mettez fin à un effet qui vous empêche de regagner des points de
    vie.
-   Pendant les 24 prochaines heures, vous bénéficiez d'un avantage aux
    jets de sauvegarde contre une maladie ou un poison qui vous affecte.



#### Chercheur

Le temps qui s'écoule entre les aventures est une excellente occasion
de faire des recherches et de mieux comprendre les mystères qui se sont
révélés au cours de la campagne. Les recherches peuvent consister à
parcourir des tomes poussiéreux et des parchemins en ruine dans une
bibliothèque ou à payer des boissons aux habitants pour leur arracher
des rumeurs et des commérages.

Lorsque vous commencez vos recherches, le MJ détermine si les
informations sont disponibles, combien de jours d'arrêt seront
nécessaires pour les trouver, et s'il y a des restrictions à vos
recherches (comme la nécessité de chercher un individu, un tome ou un
lieu spécifique). Le MJ peut également vous demander d'effectuer un ou
plusieurs tests de capacité, comme un test d'Intelligence
(Investigation) pour trouver des indices menant aux informations que
vous recherchez, ou un test de Charisme (Persuasion) pour obtenir
l'aide de quelqu'un. Une fois ces étâts remplis, vous apprenez
l'information si elle est disponible.

Pour chaque jour de recherche, vous devez dépenser 1 gp pour couvrir vos
dépenses. Ce coût s'ajoute aux dépenses normales de votre mode de vie.



#### Formation

Vous pouvez passer du temps entre deux aventures à apprendre une
nouvelle langue ou à vous entraîner avec un ensemble d'outils. Votre MJ
peut autoriser des options de formation supplémentaires.

Tout d'abord, vous devez trouver un instructeur prêt à vous enseigner.
Le MJ détermine le temps que cela prend, et si un ou plusieurs tests
d'aptitude sont nécessaires.

La formation dure 250 jours et coûte 1 gp par jour. Après avoir dépensé
la quantité de temps et d'argent requise, vous apprenez la nouvelle
langue ou gagnez la maîtrise du nouvel outil.





## États

Les états modifient les capacités d'une créature de diverses manières
et peuvent résulter d'un sort, d'une caractéristique de classe, de
l'attaque d'un monstre ou d'un autre effet. La plupart des
conditions, comme la cécité, sont des handicaps, mais certaines, comme
l'invisibilité, peuvent être avantageuses.

Un état dure soit jusqu'à ce qu'il soit contré (l'état allongé est
contré en se levant, par exemple), soit pour une durée spécifiée par
l'effet qui a imposé l'état.

Si plusieurs effets imposent le même étât à une créature, chaque
instance de l'étât a sa propre durée, mais les effets de l'étât ne
s'aggravent pas.

Une créature est soumise à un étât ou ne l'est pas. Les définitions
suivantes précisent ce qui arrive à une créature lorsqu'elle est
soumise à un étât.


#### Cécité

-   Une créature aveuglée ne peut pas voir et échoue automatiquement à
    tout test de capacité nécessitant la vue.
-   Les jets d'attaque contre la créature ont un avantage, et les jets
    d'attaque de la créature ont un désavantage.



#### Charmé

-   Une créature charmée ne peut pas attaquer le charmeur ou le cibler
    avec des capacités nuisibles ou des effets magiques.
-   Le charmeur a un avantage sur tout test de caractéristique pour
    interagir socialement avec la créature.



#### Assourdi

-   Une créature assourdie ne peut pas entendre et échoue
    automatiquement à tout test de capacité nécessitant l'audition.



#### Épuisement

Certaines capacités spéciales et certains risques environnementaux,
comme la famine et les effets à long terme des températures glaciales ou
brûlantes, peuvent entraîner un état spécial appelé épuisement.
L'épuisement se mesure en six niveaux. Un effet peut donner à une
créature un ou plusieurs niveaux d'épuisement, comme spécifié dans la
description de l'effet.

  --------------------------------------------------
   Niveau  Effet
  -------- -----------------------------------------
     1     Désavantage sur les contrôles
           d'aptitude.

     2     Vitesse réduite de moitié

     3     Désavantage aux jets d'attaque et aux
           jets de sauvegarde.

     4     Réduction de moitié du nombre maximum de
           points de vie

     5     Vitesse réduite à 0

     6     Décès
  --------------------------------------------------

Si une créature déjà épuisée subit un autre effet qui provoque
l'épuisement, son niveau d'épuisement actuel augmente de la quantité
spécifiée dans la description de l'effet.

Une créature subit l'effet de son niveau d'épuisement actuel ainsi que
de tous les niveaux inférieurs. Par exemple, une créature souffrant
d'épuisement de niveau 2 voit sa vitesse réduite de moitié et a un
désavantage aux tests d'aptitude.

Un effet qui supprime l'épuisement réduit son niveau comme indiqué dans
la description de l'effet, tous les effets d'épuisement prenant fin si
le niveau d'épuisement d'une créature est réduit en dessous de 1.

La fin d'un repos long réduit le niveau d'épuisement d'une créature
de 1, à condition qu'elle ait également ingéré de la nourriture et des
boissons.



#### Effrayé

-   Une créature effrayée a un désavantage sur les tests d'aptitude et
    les jets d'attaque lorsque la source de sa peur est dans sa ligne
    de vue.
-   La créature ne peut pas se rapprocher volontairement de la source de
    sa peur.



#### Agrippé

-   La vitesse d'une créature agrippée devient 0, et elle ne peut
    bénéficier d'aucun bonus à sa vitesse.
-   L'étât prend fin si le grappler est frappé d'incapacité.
-   La condition prend également fin si un effet retire la créature
    agrippée de la portée du lutteur ou de l'effet du grappin, comme
    lorsqu'une créature est projetée au loin par le sort Vague
    *tonnante*.



#### Invalidité

-   Une créature frappée d'incapacité ne peut pas effectuer d'actions
    ou de réactions.



#### Invisibilité

-   Une créature invisible est impossible à voir sans l'aide de la
    magie ou d'un sens particulier. Pour se cacher, la créature est
    fortement obscurcie. L'emplacement de la créature peut être détecté
    par tout bruit qu'elle fait ou toute trace qu'elle laisse.
-   Les jets d'attaque contre la créature ont un désavantage, et les
    jets d'attaque de la créature ont un avantage.



#### Paralysés

-   Une créature paralysée est frappée d'incapacité  et
    ne peut ni bouger ni parler.
-   La créature rate automatiquement les jets de sauvegarde de Force et
    de Dextérité.
-   Les jets d'attaque contre la créature ont un avantage.
-   Toute attaque qui touche la créature est un coup critique si
    l'attaquant se trouve à moins de 1,5 mètre de la créature.



#### Pétrifié

-   Une créature pétrifiée est transformée, ainsi que tout objet non
    magique qu'elle porte ou transporte, en une substance inanimée
    solide (généralement de la pierre). Son poids augmente d'un facteur
    dix, et elle cesse de vieillir.
-   La créature est frappée d'incapacité , ne peut ni
    bouger ni parler, et n'a pas conscience de son environnement.
-   Les jets d'attaque contre la créature ont un avantage.
-   La créature rate automatiquement les jets de sauvegarde de Force et
    de Dextérité.
-   La créature possède une résistance à tous les dégâts.
-   La créature est immunisée contre le poison et les maladies, bien
    qu'un poison ou une maladie déjà présents dans son système soient
    suspendus, mais pas neutralisés.



#### Empoisonné

-   Une créature empoisonnée a un désavantage sur les jets d'attaque et
    les tests de capacité.



#### À terre

-   La seule option de mouvement d'une créature couchée est de ramper,
    à moins qu'elle ne se lève et mette ainsi fin à l'étât.
-   La créature a un désavantage sur les jets d'attaque.
-   Un jet d'attaque contre la créature a un avantage si l'attaquant
    se trouve à moins de 1,5 m de la créature. Sinon, le jet d'attaque
    est désavantagé.



#### Entravé

-   La vitesse d'une créature entravée devient 0, et elle ne peut
    bénéficier d'aucun bonus à sa vitesse.
-   Les jets d'attaque contre la créature ont un avantage, et les jets
    d'attaque de la créature ont un désavantage.
-   La créature a un désavantage aux jets de sauvegarde de Dextérité.



#### Étourdi

-   Une créature étourdie est frappée d'incapacité, ne
    peut pas bouger et ne peut parler que de façon hésitante.
-   La créature rate automatiquement les jets de sauvegarde de Force et
    de Dextérité.
-   Les jets d'attaque contre la créature ont un avantage.



#### inconscient

-   Une créature inconsciente est frappée d'incapacité ,
    ne peut ni bouger ni parler, et n'a pas conscience de son
    environnement.
-   La créature laisse tomber ce qu'elle tient et tombe à plat ventre.
-   La créature rate automatiquement les jets de sauvegarde de Force et
    de Dextérité.
-   Les jets d'attaque contre la créature ont un avantage.
-   Toute attaque qui touche la créature est un coup critique si
    l'attaquant se trouve à moins de 1,5 mètre de la créature.




## Poisons

Étant donné leur nature insidieuse et mortelle, les poisons sont
illégaux dans la plupart des sociétés, mais sont un outil de
prédilection pour les assassins, les Drow et autres créatures
maléfiques.

Les poisons se présentent sous les quatre formes suivantes.

***Contact.*** Le poison de contact peut être étalé sur un objet et
reste puissant jusqu'à ce qu'il soit touché ou lavé. Une créature qui
touche le poison de contact avec la peau exposée en subit les effets.

***Ingéré.*** Une créature doit avaler une dose entière de poison ingéré
pour en subir les effets. La dose peut être délivrée dans de la
nourriture ou un liquide. Vous pouvez décider qu'une dose partielle a
un effet réduit, par exemple en accordant un avantage sur le jet de
sauvegarde ou en n'infligeant que la moitié des dégâts en cas d'échec.

***Inhalés.*** Ces poisons sont des poudres ou des gaz qui font effet
lorsqu'ils sont inhalés. Souffler la poudre ou libérer le gaz soumet à
son effet les créatures situées dans un cube de 1,5 m de côté. Le nuage
qui en résulte se dissipe immédiatement après. Retenir sa respiration
est inefficace contre les poisons inhalés, car ils affectent les
membranes nasales, les canaux lacrymaux et d'autres parties du corps.

***Blessure.*** Le poison de blessure peut être appliqué aux armes,
munitions, composants de pièges et autres objets qui infligent des
dégâts perforants ou tranchants et reste puissant jusqu'à ce qu'il
soit délivré par une blessure ou lavé. Une créature qui subit des dégâts
perforants ou tranchants d'un objet enduit de ce poison est exposée à
ses effets.

  ----------------------------------------------
  Article             Type         Prix par dose
  ------------------- ------------ -------------
  Le sang d'un       Ingéré       150 gp
  Assassinat                       

  Fumées d'othur     Inhalation   500 gp
  brûlé                            

  Mucus de chenille   Contact      200 gp

  Drow poison         Blessure     200 gp

  Essence d'éther    Inhalation   300 gp

  Malice              Inhalation   250 gp

  Larmes de minuit    Ingéré       1 500 gp

  Huile de taggit     Contact      400 gp

  Teinture pâle       Ingéré       250 gp

  Empoisonnement du   Blessure     2 000 gp
  ver violet                       

  Venin de serpent    Blessure     200 gp

  Torpeur             Ingéré       600 gp

  Le sérum de vérité  Ingéré       150 gp

  Wiverne poison      Blessure     1 200 gp
  ----------------------------------------------

  : Poisons


### Echantillon de poisons

Chaque type de poison a ses propres effets débilitants.

***Sang d'Assassinat (ingéré).*** Une créature soumise à ce poison doit
effectuer un jet de sauvegarde de Constitution DC 10. En cas d'échec,
elle subit 6 (1d12) dégâts de poison et est empoisonnée pendant 24
heures. En cas de sauvegarde réussie, la créature subit la moitié des
dégâts et n'est pas empoisonnée.

***Fumées d'Othur brûlées (inhalées).*** Une créature soumise à ce
poison doit réussir un jet de sauvegarde de Constitution DC 13 ou subir
10 (3d6) dégâts de poison, et doit répéter le jet de sauvegarde au début
de chacun de ses tours. À chaque échec successif du jet de sauvegarde,
le personnage subit 3 (1d6) dégâts de poison. Après trois sauvegardes
réussies, le poison prend fin.

***Mucus de chenille (Contact).*** Ce poison doit être récolté sur une
chenille morte ou incapacitée. Une créature soumise à ce poison doit
réussir un jet de sauvegarde de Constitution DC 13 ou être empoisonnée
pendant 1 minute. La créature empoisonnée est paralysée. La créature
peut répéter le jet de sauvegarde à la fin de chacun de ses tours,
mettant fin à l'effet sur elle-même en cas de réussite.

***Poison Drow (blessure).*** Ce poison n'est typiquement fabriqué que
par le Drow, et uniquement dans un endroit éloigné de la lumière du
soleil. Une créature soumise à ce poison doit réussir un jet de
sauvegarde de Constitution DC 13 ou être empoisonnée pendant 1 heure. Si
le jet de sauvegarde échoue par 5 ou plus, la créature est également
inconsciente lorsqu'elle est empoisonnée de cette manière. La créature
se réveille si elle subit des dégâts ou si une autre créature entreprend
une action pour la secouer.

***Essence d'éther (Inhalation).*** Une créature soumise à ce poison
doit réussir un jet de sauvegarde de Constitution DC 15 ou
s'empoisonner pendant 8 heures. La créature empoisonnée est
inconsciente. La créature se réveille si elle subit des dégâts ou si une
autre créature entreprend une action pour la secouer.

***Malice (Inhalé).*** Une créature soumise à ce poison doit réussir un
jet de sauvegarde de Constitution DC 15 ou s'empoisonner pendant 1
heure. La créature empoisonnée est aveuglée.

***Larmes de minuit (ingéré).*** Une créature qui ingère ce poison ne
subit aucun effet jusqu'aux douze coups de minuit. Si le poison n'a
pas été neutralisé avant cela, la créature doit réussir un jet de
sauvegarde de Constitution DC 17, subissant 31 (9d6) dégâts de poison en
cas d'échec, ou la moitié des dégâts en cas de réussite.

***Huile de Taggit (Contact).*** Une créature soumise à ce poison doit
réussir un jet de sauvegarde de Constitution DC 13 ou s'empoisonner
pendant 24 heures. La créature empoisonnée est inconsciente. La créature
se réveille si elle subit des dégâts.

***Teinture pâle (ingérée).*** Une créature soumise à ce poison doit
réussir un jet de sauvegarde de Constitution DC 16 ou subir 3 (1d6)
dégâts de poison et s'empoisonner. La créature empoisonnée doit répéter
le jet de sauvegarde toutes les 24 heures, subissant 3 (1d6) dégâts de
poison en cas d'échec. Jusqu'à la fin de ce poison, les dégâts qu'il
inflige ne peuvent être soignés par aucun moyen. Après sept jets de
sauvegarde réussis, l'effet prend fin et la créature peut se soigner
normalement.

***Poison de ver pourpre (blessure).*** Ce poison doit être récolté sur
un ver pourpre mort ou frappé d'incapacité. Une créature soumise à ce
poison doit effectuer un jet de sauvegarde de Constitution DC 19,
subissant 42 (12d6) dégâts de poison en cas d'échec, ou la moitié des
dégâts en cas de réussite.

***Venin de serpent (blessure).*** Ce poison doit être récolté sur un
serpent venimeux géant mort ou frappé d'incapacité. Une créature
soumise à ce poison doit réussir un jet de sauvegarde de Constitution DC
11, subissant 10 (3d6) dégâts de poison en cas d'échec, ou la moitié
des dégâts en cas de réussite.

***Torpeur (ingéré).*** Une créature soumise à ce poison doit réussir un
jet de sauvegarde de Constitution DC 15 ou s'empoisonner pendant 4d6
heures. La créature empoisonnée est frappée d'incapacité.

***Sérum de vérité (ingéré).*** Une créature soumise à ce poison doit
réussir un jet de sauvegarde de Constitution DC 11 ou s'empoisonner
pendant 1 heure. La créature empoisonnée ne peut pas sciemment dire un
mensonge, comme si elle était sous l'effet d'un sort de *zone de
vérité*.

***Poison de wiverne (blessure).*** Ce poison doit être récolté sur une
wyvern morte ou incapacitée. Une créature soumise à ce poison doit
effectuer un jet de sauvegarde de Constitution DC 15, subissant 24 (7d6)
dégâts de poison en cas d'échec, ou la moitié des dégâts en cas de
réussite.




## Maladies

Une peste ravage le royaume, mettant les aventuriers en quête d'un
remède. Une aventurière émerge d'une tombe ancienne, non ouverte depuis
des siècles, et se retrouve bientôt atteinte d'une maladie mortelle. Un
sorcier offense une puissance obscure et contracte une étrange
affliction qui se propage chaque fois qu'il jette des sorts.

Une simple épidémie peut se résumer à une petite ponction sur les
ressources du groupe, qui peut être soignée par un jet de *restauration
partielle*. Une épidémie plus complexe peut
constituer la base d'une ou plusieurs aventures, les personnages
cherchant un remède, arrêtant la propagation de la maladie et gérant les
conséquences.

Une maladie qui fait plus qu'infecter quelques membres du groupe est
avant tout un élément de l'intrigue. Les règles aident à décrire les
effets de la maladie et la façon dont elle peut être soignée, mais les
spécificités du fonctionnement d'une maladie ne sont pas liées à un
ensemble commun de règles. Les maladies peuvent affecter n'importe
quelle créature, et une maladie donnée peut ou non passer d'une origine ou
d'un type de créature à un autre. Un fléau peut n'affecter que les
constructions ou les morts-vivants, ou balayer un quartier de halfelins
mais laisser les autres origines intactes. Ce qui compte, c'est
l'histoire que vous voulez raconter.


### Exemples de maladies

Les maladies présentées ici illustrent les différentes façons dont les
maladies peuvent fonctionner dans le jeu. N'hésitez pas à modifier les
jets de sauvegarde, les temps d'incubation, les symptômes et les autres
caractéristiques de ces maladies pour les adapter à votre campagne.


#### La fièvre du caquetage

Cette maladie cible les humanoïdes, bien que les
gnomes soient étrangement immunisés. Lorsqu'elles
sont en proie à cette maladie, les victimes succombent fréquemment à des
crises de fou rire, ce qui a donné à la maladie son nom commun et son
surnom morbide : \"les cris\".

Les symptômes se manifestent 1d4 heures après l'infection et
comprennent la fièvre et la désorientation. La créature infectée gagne
un niveau d'épuisement qui ne peut être supprimé tant que la maladie
n'est pas guérie.

Tout événement qui cause un grand stress à la créature infectée - y
compris le fait de participer à un combat, de subir des dégâts, d'avoir
peur ou de faire un cauchemar - force la créature à effectuer un jet de
sauvegarde de Constitution DC 13. En cas d'échec, la créature subit 5
(1d10) dégâts psychiques et devient incapable de rire pendant 1 minute.
La créature peut répéter le jet de sauvegarde à la fin de chacun de ses
tours, mettant fin au fou rire et à l'état d'incapacité en cas de
réussite.

Toute créature humanoïde qui commence son tour à moins de 3 mètres
d'une créature infectée en proie à un fou rire doit réussir un jet de
sauvegarde de Constitution DC 10 ou être également infectée par la
maladie. Lorsqu'une créature réussit ce jet de sauvegarde, elle est
immunisée contre le fou rire de cette créature infectée particulière
pendant 24 heures.

À la fin de chaque repos long, une créature infectée peut effectuer un
jet de sauvegarde de Constitution DC 13. En cas de sauvegarde réussie,
le DC de cette sauvegarde et de la sauvegarde pour éviter une attaque de
fou rire diminue de 1d6. Lorsque le jet de sauvegarde tombe à 0, la
créature se remet de la maladie. Une créature qui échoue à trois de ces
jets de sauvegarde gagne une forme de folie
indéfinie déterminée au hasard.



#### Peste des égouts

La peste des égouts est un terme générique pour une large catégorie de
maladies qui incubent dans les égouts, les tas d'ordures et les
marécages stagnants, et qui sont parfois transmises par des créatures
qui vivent dans ces zones, comme les rats et les
otyughs.

Lorsqu'une créature humanoïde est mordue par une créature porteuse de
la maladie, ou lorsqu'elle entre en contact avec des immondices ou des
abats contaminés par la maladie, elle doit réussir un jet de sauvegarde
de Constitution DC 11 ou être infectée.

Il faut 1d4 jours pour que les symptômes de la peste des égouts se
manifestent chez une créature infectée. Les symptômes comprennent la
fatigue et les crampes. La créature infectée souffre d'un niveau
d'épuisement, et elle ne récupère que la moitié du nombre normal de
points de vie en dépensant des dés de points de vie, et aucun point de
vie en terminant un repos long.

À la fin de chaque repos long, une créature infectée doit effectuer un
jet de sauvegarde de Constitution DC 11. En cas d'échec, le personnage
gagne un niveau d'épuisement. En cas de sauvegarde réussie, le niveau
d'épuisement du personnage diminue d'un niveau. Si un jet de
sauvegarde réussi réduit le niveau d'épuisement de la créature infectée
en dessous de 1, la créature se remet de la maladie.



#### Rotation de la vue

Cette infection douloureuse provoque des saignements des yeux et finit
par rendre la victime aveugle.

Une bête ou un humanoïde qui boit de l'eau souillée par la pourriture
de la vue doit réussir un jet de sauvegarde de Constitution DC 15 ou
être infecté. Un jour après l'infection, la vision de la créature
commence à se troubler. La créature subit un malus de -1 aux jets
d'attaque et aux tests de capacité qui dépendent de la vue. À la fin de
chaque repos long après l'apparition des symptômes, la pénalité
s'aggrave de 1. Lorsqu'elle atteint -5, la victime est aveuglée
jusqu'à ce que sa vue soit restaurée par une magie telle que
Restauration *partielle*.

La pourriture de la vue peut être soignée à l'aide d'une fleur rare,
l'euphorbe, qui pousse dans certains marais. En une heure, un
personnage ayant la maîtrise d'un kit d'herboristerie peut transformer
la fleur en une dose d'onguent. Appliquée sur les yeux avant un long
repos, une dose de cette pommade empêche la maladie de s'aggraver après
ce repos. Après trois doses, l'onguent guérit entièrement la maladie.





## La folie

Dans une campagne typique, les personnages ne sont pas rendus fous par
les horreurs qu'ils affrontent et le carnage qu'ils infligent jour
après jour, mais parfois le stress d'être un aventurier peut être trop
lourd à supporter. Si votre campagne a un thème d'horreur fort, vous
pouvez utiliser la folie pour renforcer ce thème, en soulignant la
nature extraordinairement horrible des menaces auxquelles les
aventuriers sont confrontés.


### Devenir fou

Divers effets magiques peuvent infliger la folie à un esprit autrement
stable. Certains sorts, comme Contact avec *autre
plan*, peuvent provoquer
la folie, et vous pouvez utiliser les règles de folie ici au lieu des
effets de ces sorts. Les maladies, les poisons et les effets planaires
tels que le vent psychique ou les vents hurlants d'un plan chaotique
peuvent tous infliger la folie. Certains artefacts peuvent également
briser la psyché d'un personnage qui les utilise ou qui y est lié.

Résister à un effet provoquant la folie nécessite généralement un jet de
sauvegarde de Sagesse ou de Charisme.



### Effets de la folie

La folie peut être à court terme, à long terme ou indéfinie. La plupart
des effets relativement banals imposent une folie à court terme, qui ne
dure que quelques minutes. Des effets plus horribles ou des effets
cumulatifs peuvent entraîner une folie à long terme ou indéfinie.

Un personnage affligé de **folie à court terme** est soumis à un effet
de la table de folie à court terme pendant 1d10 minutes.

  ------------------------------------------------------------------------
    d100   Effet (dure 1d10 minutes)
  -------- ---------------------------------------------------------------
   01-20   Le personnage se replie dans son esprit et devient paralysé.
           L'effet prend fin si le personnage subit des dégâts.

   21-30   Le personnage est frappé d'incapacité et passe la durée à
           crier, rire ou pleurer.

   31-40   Le personnage est effrayé et doit utiliser son action et son
           mouvement chaque round pour fuir la source de la peur.

   41-50   Le personnage se met à bafouiller et est incapable de parler
           normalement ou de lancer des incantations.

   51-60   Le personnage doit utiliser son action chaque round pour
           attaquer la créature la plus proche.

   61-70   Le personnage a de vives hallucinations et a un désavantage sur
           les tests de capacité.

   71-75   Le personnage fait tout ce qu'on lui dit de faire et qui
           n'est pas manifestement autodestructeur.

   76-80   Le personnage ressent une envie irrésistible de manger quelque
           chose d'étrange comme de la terre, de la bave ou des abats.

   81-90   Le personnage est étourdi.

   91-100  Le personnage tombe inconscient.
  ------------------------------------------------------------------------

  : La folie du court terme

Un personnage atteint de **folie à long terme** est soumis à un effet de
la table Folie à long terme pendant 1d10 × 10 heures.

  ------------------------------------------------------------------------
    d100   Effet (dure 1d10 × 10 heures)
  -------- ---------------------------------------------------------------
   01-10   Le personnage se sent obligé de répéter sans cesse une activité
           spécifique, comme se laver les mains, toucher des objets, prier
           ou compter des pièces.

   11-20   Le personnage a de vives hallucinations et a un désavantage sur
           les tests de capacité.

   21-30   Le personnage souffre d'une paranoïa extrême. Le personnage a
           un désavantage aux tests de Sagesse et de Charisme.

   31-40   Le personnage considère quelque chose (généralement la source
           de la folie) avec une répulsion intense, comme s'il était
           affecté par l'effet d'*antipathie* du sort
           *Aversion/sympathie*.

   41-45   Le personnage fait l'expérience d'un puissant délire.
           Choisissez une potion. Le personnage s'imagine qu'il est sous
           ses effets.

   46-55   Le personnage s'attache à un \"porte-bonheur\", comme une
           personne ou un objet, et a un désavantage aux jets d'attaque,
           aux tests de capacité et aux jets de sauvegarde lorsqu'il se
           trouve à plus de 10 mètres de lui.

   56-65   Le personnage est aveuglé (25%) ou assourdi (75%).

   66-75   Le personnage subit des tremblements ou des tics
           incontrôlables, qui lui imposent un désavantage aux jets
           d'attaque, aux tests de capacité et aux jets de sauvegarde qui
           impliquent la Force ou la Dextérité.

   76-85   Le personnage souffre d'une amnésie partielle. Le personnage
           sait qui il est et conserve ses traits raciaux et ses capacités
           de classe, mais il ne reconnaît pas les autres personnes et ne
           se souvient pas de ce qui s'est passé avant que la folie ne
           fasse effet.

   86-90   Chaque fois que le personnage subit des dégâts, il doit réussir
           un jet de sauvegarde de Sagesse DC 15 ou être affecté comme
           s'il avait raté un jet de sauvegarde contre le sort de
           *confusion*. L'effet de confusion dure 1 minute.

   91-95   Le personnage perd la capacité de parler.

   96-100  Le personnage tombe inconscient. Aucune bousculade ou dommage
           ne peut réveiller le personnage.
  ------------------------------------------------------------------------

  : La folie à long terme

Un personnage atteint de **folie indéfinie** gagne un nouveau défaut de
caractère de la table Folie indéfinie qui dure jusqu'à sa guérison.

  ------------------------------------------------------------------------
    d100   Défaut (dure jusqu'à ce qu'il soit corrigé)
  -------- ---------------------------------------------------------------
   01-15   \"Etre saoul me permet de rester sain d'esprit.\"

   16-25   \"Je garde tout ce que je trouve.\"

   26-30   \"J'essaie de ressembler davantage à une personne que je
           connais, en adoptant son style vestimentaire, ses manières et
           son nom.\"

   31-35   \"Je dois déformer la vérité, exagérer ou mentir carrément pour
           être intéressant aux yeux des autres.\"

   36-45   \"Atteindre mon objectif est la seule chose qui m'intéresse,
           et j'ignorerai tout le reste pour le poursuivre.\"

   46-50   \"J'ai du mal à me soucier de ce qui se passe autour de moi.\"

   51-55   \"Je n'aime pas la façon dont les gens me jugent tout le
           temps.\"

   56-70   \"Je suis la personne la plus intelligente, la plus sage, la
           plus forte, la plus rapide et la plus belle que je connaisse.\"

   71-80   \"Je suis convaincu que de puissants ennemis me traquent, et
           que leurs agents sont partout où je vais. Je suis sûr qu'ils
           me surveillent en permanence.\"

   81-85   \"Il n'y a qu'une seule personne en qui je peux avoir
           confiance. Et je suis le seul à pouvoir voir cet ami spécial.\"

   86-95   \"Je ne peux rien prendre au sérieux. Plus la situation est
           sérieuse, plus je la trouve drôle.\"

   96-100  \"J'ai découvert que j'aime vraiment tuer des gens.\"
  ------------------------------------------------------------------------

  : La folie indéfinie



### Guérir la folie

Un sort de *calme des émotions* peut supprimer les
effets de la folie, tandis qu'un sort de *restauration
partielle* peut débarrasser un personnage d'une
folie à court ou à long terme. En fonction de la source de la folie, les
sorts Délivrance des *malédictions* ou *Dissipation du
mal* peuvent également s'avérer efficaces. Un
sort de *restauration supérieure* ou une magie
plus puissante est nécessaire pour débarrasser un personnage d'une
folie de durée indéterminée.




## Pièges

Les pièges peuvent être trouvés presque partout. Un faux pas dans une
tombe ancienne peut déclencher une série de lames de faucille qui
transpercent les armures et les os. Les vignes apparemment inoffensives
qui pendent à l'entrée d'une grotte peuvent saisir et étouffer
quiconque les traverse. Un filet caché parmi les arbres peut tomber sur
les voyageurs qui passent en dessous. Dans un jeu fantastique, les
aventuriers imprudents peuvent faire une chute mortelle, être brûlés
vifs ou tomber sous une fusillade de fléchettes empoisonnées.

Un piège peut être de nature mécanique ou magique. Les **pièges
mécaniques** comprennent les fosses, les pièges à flèches, les blocs qui
tombent, les pièces remplies d'eau, les lames tournoyantes et tout ce
qui dépend d'un mécanisme pour fonctionner. Les **pièges magiques**
sont soit des pièges à dispositifs magiques, soit des pièges à sorts.
Les pièges magiques déclenchent les effets des sorts lorsqu'ils sont
activés. Les pièges à sortilèges sont des sorts tels que
*Glyphe* qui
fonctionnent comme des pièges.


### Les pièges en jeu

Lorsque les aventuriers rencontrent un piège, vous devez savoir comment
le piège se déclenche et ce qu'il fait, ainsi que la possibilité pour
les personnages de détecter le piège et de le désactiver ou de
l'éviter.


#### Déclencher un piège

La plupart des pièges sont déclenchés lorsqu'une créature va quelque
part ou touche quelque chose que le créateur du piège a voulu protéger.
Les déclencheurs communs comprennent le fait de marcher sur une plaque
de pression ou une fausse section de plancher, de tirer un fil de
déclenchement, de tourner une poignée de porte et d'utiliser la
mauvaise clé dans une serrure. Les pièges magiques sont souvent réglés
pour se déclencher lorsqu'une créature pénètre dans une zone ou touche
un objet. Certains pièges magiques (comme le sort *Glyphe de
protection* ont des conditions de déclenchement
plus compliquées, notamment un mot de passe qui empêche le piège de
s'activer.



#### Détection et désactivation d'un piège

En général, certains éléments d'un piège sont visibles lors d'une
inspection minutieuse. Les personnages peuvent remarquer une dalle
irrégulière qui cache une plaque de pression, repérer la lueur d'un
fil-piège, remarquer les petits trous dans les murs d'où jailliront des
flammes, ou détecter quelque chose qui indique la présence d'un piège.

La description d'un piège précise les tests et les DC nécessaires pour
le détecter, le désactiver, ou les deux. Un personnage qui cherche
activement un piège peut tenter un test de Sagesse (Perception) contre
le DC du piège. Vous pouvez également comparer le DC de détection du
piège avec le score passif de Sagesse (Perception) de chaque personnage
pour déterminer si un membre du groupe remarque le piège au passage. Si
les aventuriers détectent un piège avant de le déclencher, ils peuvent
être en mesure de le désarmer, soit de façon permanente, soit
suffisamment longtemps pour le contourner. Vous pouvez demander un test
d'Intelligence (Investigation) à un personnage pour déduire ce qui doit
être fait, suivi d'un test de Dextérité en utilisant des outils de
voleur pour effectuer le sabotage nécessaire.

Tout personnage peut tenter un test d'Intelligence (Arcane) pour
détecter ou désarmer un piège magique, en plus de tout autre test
indiqué dans la description du piège. Les DC sont les mêmes, quel que
soit le test utilisé. De plus, *Dissipation de la
magie* a une chance de désactiver la plupart des pièges
magiques. La description d'un piège magique fournit le DC pour le test
de capacité effectué lorsque vous utilisez *Dissipation de la
magie*.

Dans la plupart des cas, la description d'un piège est suffisamment
claire pour que vous puissiez déterminer si les actions d'un personnage
permettent de localiser ou de déjouer le piège. Comme dans de nombreuses
situations, vous ne devez pas laisser le jet de dé prendre le pas sur un
jeu intelligent et une bonne planification. Faites appel à votre sens
commun, en vous appuyant sur la description du piège pour déterminer ce
qui se passe. Aucun piège ne peut anticiper toutes les actions possibles
que les personnages pourraient tenter.

Vous devriez permettre à un personnage de découvrir un piège sans faire
de test de capacité si une action révélerait clairement la présence du
piège. Par exemple, si un personnage soulève un tapis qui dissimule une
plaque de pression, le personnage a trouvé le déclencheur et aucun test
n'est nécessaire.

Déjouer les pièges peut être un peu plus compliqué. Prenons l'exemple
d'un coffre au trésor piégé. Si le coffre est ouvert sans avoir tiré
sur les deux poignées fixées sur les côtés, un mécanisme à l'intérieur
envoie une grêle d'aiguilles empoisonnées vers quiconque se trouve
devant lui. Après avoir inspecté le coffre et effectué quelques
vérifications, les personnages ne sont toujours pas sûrs qu'il soit
piégé. Plutôt que d'ouvrir simplement le coffre, ils placent un
bouclier devant et poussent le coffre à distance avec une barre de fer.
Dans ce cas, le piège se déclenche toujours, mais la grêle d'aiguilles
tire inoffensivement sur le bouclier.

Les pièges sont souvent conçus avec des mécanismes qui leur permettent
d'être désarmés ou contournés. Les monstres intelligents qui placent
des pièges dans ou autour de leurs repaires ont besoin de moyens pour
passer ces pièges sans se blesser. Ces pièges peuvent avoir des leviers
cachés qui désactivent leurs déclencheurs, ou une porte secrète peut
cacher un passage qui contourne le piège.



#### Effets de piège

Les effets des pièges peuvent aller du désagrément à la mort, en
utilisant des éléments tels que des flèches, des pointes, des lames, du
poison, des gaz toxiques, des explosions de feu et des fosses profondes.
Les pièges les plus mortels combinent plusieurs éléments pour tuer,
blesser, contenir ou chasser toute créature assez malheureuse pour les
déclencher. La description d'un piège précise ce qui se passe
lorsqu'il est déclenché.

Le bonus d'attaque d'un piège, les points de sauvegarde pour résister
à ses effets et les dégâts qu'il inflige peuvent varier en fonction de
la gravité du piège. Utilisez le tableau des points de sauvegarde et des
bonus d'attaque des pièges et le tableau de la gravité des dégâts par
niveau pour obtenir des suggestions basées sur trois niveaux de gravité
des pièges.

Un piège destiné à être un **revers** a peu de chances de tuer ou de
blesser sérieusement les personnages des niveaux indiqués, alors qu'un
piège **dangereux** est susceptible de blesser sérieusement (et
potentiellement de tuer) les personnages des niveaux indiqués. Un piège
**mortel** est susceptible de tuer les personnages des niveaux indiqués.

  ------------------------------------
  Piège Danger  Save DC      Bonus
                          d'attaque
  ------------ --------- -------------
  Marge de       10-11      +3 à +5
  recul                  

  Dangerous      12-15      +6 à +8

  Deadly         16-20     +9 à +12
  ------------------------------------

  : Les DC de sauvegarde et les bonus d'attaque des pièges

  ------------------------------------------------
      Niveau de     Marge de   Dangerous   Deadly
      caractère       recul               
  ----------------- --------- ----------- --------
      1er-4ème        1d10       4d10       4d10

     5ème-10ème       2d10       10d10     10d10

     11ème-16ème      2d10       10d10     18d10

     17ème-20ème      4d10       18d10     24d10
  ------------------------------------------------

  : Gravité des dommages par niveau



#### Pièges complexes

Les pièges complexes fonctionnent comme les pièges standard, sauf
qu'une fois activés, ils exécutent une série d'actions à chaque round.
Un piège complexe transforme le processus de traitement d'un piège en
quelque chose qui ressemble plus à une rencontre de combat.

Lorsqu'un piège complexe s'active, il effectue un jet d'initiative.
La description du piège inclut un bonus d'initiative. A son tour, le
piège s'active à nouveau, souvent en effectuant une action. Il peut
effectuer des attaques successives contre des intrus, créer un effet qui
change avec le temps, ou produire un défi dynamique. Sinon, le piège
complexe peut être détecté et désactivé ou contourné de la manière
habituelle.

Par exemple, un piège qui provoque une lente inondation d'une pièce
fonctionne mieux comme un piège complexe. Au tour du piège, le niveau de
l'eau augmente. Après plusieurs tours, la pièce est complètement
inondée.




### Pièges à échantillons

Les pièges magiques et mécaniques présentés ici varient en termes de
dangerosité et sont présentés par ordre alphabétique.


#### Toit qui s'effondre

*Piège mécanique*

Ce piège utilise un fil déclencheur pour faire s'effondrer les supports
qui maintiennent en place une section instable d'un plafond.

Le fil déclencheur se trouve à 10 cm du sol et s'étend entre deux
poutres de soutien. Le DC pour repérer le fil-piège est de 10. Un test
de Dextérité DC 15 réussi à l'aide d'outils de voleurs désactive le
fil-piège sans danger. Un personnage sans outils de voleur peut tenter
ce test avec un désavantage en utilisant n'importe quelle arme
tranchante ou outil tranchant. En cas d'échec, le piège se déclenche.

Toute personne qui inspecte les poutres peut facilement déterminer
qu'elles sont simplement calées en place. En tant qu'action, un
personnage peut débloquer une poutre, provoquant le déclenchement du
piège.

Le plafond au-dessus du fil-piège est en mauvais état, et quiconque peut
le voir peut dire qu'il risque de s'effondrer.

Lorsque le piège est déclenché, le plafond instable s'effondre. Toute
créature se trouvant dans la zone située sous la section instable doit
réussir un jet de sauvegarde de Dextérité DC 15, subissant 22 (4d10)
dégâts de matraquage en cas d'échec, ou la moitié des dégâts en cas de
réussite. Une fois le piège déclenché, le sol de la zone est rempli de
gravats et devient un terrain difficile.



#### Filet tombant

*Piège mécanique*

Ce piège utilise un fil de déclenchement pour libérer un filet suspendu
au plafond.

Le fil de détente est à 10 cm du sol et s'étend entre deux colonnes ou
arbres. Le filet est caché par des toiles d'araignée ou du feuillage.
Le DC pour repérer le fil-piège et le filet est de 10. Un test de
Dextérité réussi (DC 15) à l'aide d'outils de voleurs permet de briser
le fil-piège sans danger. Un personnage sans outils de voleur peut
tenter ce test avec un désavantage en utilisant n'importe quelle arme
tranchante ou outil tranchant. En cas d'échec, le piège se déclenche.

Lorsque le piège est déclenché, le filet est libéré, couvrant une zone
de 3 mètres carrés. Les personnes se trouvant dans la zone sont piégées
sous le filet et entravées, et celles qui échouent à un jet de
sauvegarde de Force DC 10 sont également projetées en position couchée.
Une créature peut utiliser son action pour faire un test de Force DC 10,
se libérant ou libérant une autre créature à sa portée en cas de succès.
Le filet a une CA de 10 et 20 points de vie. Si vous infligez 5 points
de dégâts tranchants au filet (AC 10), vous en détruisez une section de
5 pieds carrés, libérant ainsi toute créature piégée dans cette section.



#### Statue crachant du feu

*Piège magique*

Ce piège est activé lorsqu'un intrus marche sur une plaque de pression
cachée, libérant une goutte de flamme magique d'une statue proche. La
statue peut représenter n'importe quoi, y compris un dragon ou un
magicien lançant un sort.

Le DC est de 15 pour repérer la plaque de pression, ainsi que de légères
marques de brûlure sur le sol et les murs. Un sort ou un autre effet
permettant de détecter la présence de magie, tel que *détecter la
magie*, révèle une aura de magie d'évocation autour de
la statue.

Le piège s'active lorsque plus de 10 kg sont placés sur la plaque de
pression, ce qui fait que la statue libère un cône de feu de 10 m de
haut. Chaque créature dans le feu doit effectuer un jet de sauvegarde de
Dextérité DC 13, subissant 22 (4d10) dégâts de feu en cas d'échec, ou
la moitié des dégâts en cas de réussite.

Coincer une pointe de fer ou un autre objet sous le plateau de pression
empêche le piège de s'activer. Un jet réussi de *Dissipation de la
magie* sur la statue détruit le piège.



#### Fosses

*Piège mécanique*

Quatre pièges à fosse de base sont présentés ici.

***Simple Pit.*** Un piège à fosse simple est un trou creusé dans le
sol. Le trou est recouvert d'un grand tissu ancré sur le bord de la
fosse et camouflé par de la terre et des débris.

Le DC pour repérer la fosse est de 10. Toute personne marchant sur le
tissu tombe au travers et entraîne le tissu dans la fosse, subissant des
dégâts en fonction de la profondeur de la fosse (généralement 3 mètres,
mais certaines fosses sont plus profondes).

***Fosse cachée.*** Cette fosse a un abri construit à partir d'un
matériau identique à celui du sol qui l'entoure.

Un test de Sagesse (Perception) DC 15 réussi permet de discerner une
absence de circulation piétonne sur la section du sol qui constitue le
couvercle de la fosse. Un test d'Intelligence (Investigation) DC 15
réussi est nécessaire pour confirmer que la section de sol piégée est en
fait l'abri d'une fosse.

Lorsqu'une créature marche sur l'abri, il s'ouvre comme une trappe et
l'intrus se jette dans la fosse en dessous. La fosse a généralement une
profondeur de 10 ou 20 pieds, mais elle peut être plus profonde.

Une fois le piège détecté, une pointe de fer ou un objet similaire peut
être coincé entre le couvercle de la fosse et le sol environnant de
manière à empêcher le couvercle de s'ouvrir, ce qui permet de traverser
en toute sécurité. Le couvercle peut également être maintenu fermé par
magie à l'aide du sort *Verrou occ* ulte ou d'un sort
similaire.

***Fosse verrouillée.*** Ce piège est identique à un piège à fosse
cachée, à une exception près : la trappe qui recouvre la fosse est
montée sur ressort. Lorsqu'une créature tombe dans la fosse, le
couvercle se referme pour emprisonner sa victime à l'intérieur.

Il faut réussir un test de Force DC 20 pour faire levier et ouvrir
l'abri. L'abri peut également être ouvert en le fracassant. Un
personnage dans la fosse peut également tenter de désactiver le
mécanisme à ressort de l'intérieur avec un test de Dextérité DC 15 en
utilisant des outils de voleur, à condition que le mécanisme puisse être
atteint et que le personnage puisse voir. Dans certains cas, un
mécanisme (généralement caché derrière une porte secrète à proximité)
permet d'ouvrir la fosse.

***Fosse à pointes.*** Ce piège à fosse est un piège simple, caché ou
verrouillé, avec des pointes de bois ou de fer aiguisées au fond. Une
créature tombant dans la fosse subit 11 (2d10) dégâts perforants dus aux
pointes, en plus des dégâts de chute. Les versions encore plus méchantes
ont du poison étalé sur les pointes. Dans ce cas, toute personne
subissant des dégâts perforants dus aux pointes doit également effectuer
un jet de sauvegarde de Constitution DC 13, subissant 22 (4d10) dégâts
de poison en cas d'échec, ou la moitié des dégâts en cas de réussite.



#### Fléchettes empoisonnées

*Piège mécanique*

Lorsqu'une créature marche sur une plaque de pression cachée, des
fléchettes empoisonnées jaillissent de tubes à ressort ou pressurisés
habilement encastrés dans les murs environnants. Une zone peut
comprendre plusieurs plaques de pression, chacune étant équipée de son
propre jeu de fléchettes.

Les très petits trous dans les murs sont cachés par la poussière et les
toiles d'araignée, ou habilement dissimulés parmi les bas-reliefs, les
peintures murales ou les fresques qui ornent les murs. Le DC pour les
repérer est de 15. Avec un test d'Intelligence (Investigation) DC 15
réussi, un personnage peut déduire la présence du harnois à partir des
variations du mortier et de la pierre utilisés pour le créer, par
rapport au sol environnant. Coincer une pointe de fer ou un autre objet
sous la plaque de pression empêche le piège de s'activer. Remplir les
trous de tissu ou de cire empêche les fléchettes qu'ils contiennent de
se lancer.

Le piège s'active lorsqu'un poids de plus de 10 kg est placé sur la
plaque de pression, libérant quatre fléchettes. Chaque fléchette
effectue une attaque à distance avec un bonus de +8 contre une cible
aléatoire située à moins de 3 mètres de la plaque de pression (la vision
n'est pas prise en compte pour ce jet d'attaque). (Si aucune cible ne
se trouve dans la zone, les fléchettes ne touchent rien.) Une cible
touchée subit 2 (1d4) dégâts perforants et doit réussir un jet de
sauvegarde de Constitution DC 15, subissant 11 (2d10) dégâts de poison
en cas d'échec, ou la moitié des dégâts en cas de réussite.



#### Aiguille empoisonnée

*Piège mécanique*

Une aiguille empoisonnée est cachée dans la serrure d'un coffre à
trésor, ou dans un autre objet qu'une créature pourrait ouvrir. Si
l'on ouvre le coffre sans la bonne clé, l'aiguille sort, délivrant une
dose de poison.

Lorsque le piège est déclenché, l'aiguille sort de la serrure de 5 cm.
Une créature à portée subit 1 dégât perforant et 11 (2d10) dégâts de
poison, et doit réussir un jet de sauvegarde de Constitution DC 15 ou
être empoisonnée pendant 1 heure.

Un test d'Intelligence (Investigation) DC 20 réussi permet au
personnage de déduire la présence du piège à partir des modifications
apportées à la serrure pour accueillir l'aiguille. Un test de Dextérité
DC 15 réussi en utilisant des outils de voleurs désarme le piège, en
retirant l'aiguille de la serrure. Une tentative infructueuse de
crochetage de la serrure déclenche le piège.



#### Sphère roulante

*Piège mécanique*

Lorsqu'une pression de 10 kg ou plus est exercée sur la plaque de
pression de ce piège, une trappe cachée dans le plafond s'ouvre,
libérant une sphère de pierre solide de 3 m de diamètre.

En réussissant un test de Sagesse (Perception) DC 15, un personnage peut
repérer la trappe et la plaque de pression. Une fouille du sol
accompagnée d'un test d'Intelligence (Investigation) DC 15 réussi
révèle des variations dans le mortier et la pierre qui trahissent la
présence de la plaque de pression. Le même test effectué lors de
l'inspection du plafond note des variations dans la maçonnerie qui
révèlent la trappe. Coincer une pointe de fer ou un autre objet sous la
plaque de pression empêche le piège de s'activer.

L'activation de la sphère nécessite un jet d'initiative de la part de
toutes les créatures présentes. La sphère effectue un jet d'initiative
avec un bonus de +8. À son tour, elle se déplace de 60 pieds en ligne
droite. La sphère peut se déplacer à travers les espaces des créatures,
et les créatures peuvent se déplacer à travers son espace, en le
traitant comme un terrain difficile. Chaque fois que la sphère entre
dans l'espace d'une créature ou qu'une créature entre dans son espace
pendant qu'elle roule, cette créature doit réussir un jet de sauvegarde
de Dextérité DC 15 ou subir 55 (10d10) dégâts de contondant et être mise
à terre.

La sphère s'arrête lorsqu'elle heurte un mur ou une barrière
similaire. Elle ne peut pas contourner les coins, mais les constructeurs
de donjons intelligents intègrent des virages en douceur dans les
passages proches, ce qui permet à la sphère de continuer à se déplacer.

Comme une action, une créature dans un rayon de 1,5 mètre de la sphère
peut tenter de la ralentir avec un test de Force DC 20. Si le test est
réussi, la vitesse de la sphère est réduite de 15 pieds. Si la vitesse
de la sphère tombe à 0, elle cesse de se déplacer et n'est plus une
menace.



#### Sphère d'annihilation

*Piège magique*

Des ténèbres magiques et impénétrables remplissent la bouche béante
d'un visage de pierre taillé dans un mur. La bouche fait 2 pieds de
diamètre et est grossièrement circulaire. Aucun son n'en sort, aucune
lumière ne peut en illuminer l'intérieur et toute matière qui y pénètre
est instantanément oblitérée.

Un test d'Intelligence (Mystères) réussi à DC 20 révèle que la bouche
contient une sphère *d'annihilation*
qui ne peut être ni contrôlée ni déplacée. Elle est par ailleurs
identique à une sphère *d'annihilation* normale.

Certaines versions du piège comprennent un enchantement placé sur la
face de la pierre, de sorte que les créatures spécifiées ressentent une
envie irrésistible de s'en approcher et de ramper dans sa bouche. Cet
effet est semblable à l'aspect *sympathie* du sort
*Aversion/attirance*. Une réussite de
*Dissipation de la magie* supprime cet
enchantement.



# Combat


## L'ordre de combat

Un combat typique est un affrontement entre deux camps, un déluge de
coups d'armes, de feintes, de parades, de jeux de jambes et
d'incantations. Le jeu organise le chaos du combat en un cycle de
rounds et de tours. Un round représente environ 6 secondes dans le monde
du jeu. Au cours d'un round, chaque participant à une bataille prend un
tour. L'ordre des tours est déterminé au début d'une rencontre de
combat, lorsque tout le monde fait un jet d'initiative. Une fois que
tout le monde a eu son tour, le combat se poursuit au tour suivant si
aucun camp n'a vaincu l'autre.

> #### Le combat étape par étape {#combat-step-by-step}
>
> 1.  **Déterminer la surprise.** Le MJ détermine si toute personne
>     impliquée dans la rencontre de combat est surprise.
>
> 2.  **Établir les positions.** Le MJ décide de l'emplacement de tous
>     les personnages et monstres. En fonction de l'ordre de marche des
>     aventuriers ou de leur position dans la pièce ou dans un autre
>     lieu, le MJ détermine où se trouvent les adversaires, à quelle
>     distance et dans quelle direction.
>
> 3.  **Lancer l'initiative.** Toutes les personnes impliquées dans la
>     rencontre de combat font un jet d'initiative, déterminant
>     l'ordre des tours des combattants.
>
> 4.  **Chacun son tour.** Chaque participant à la bataille prend son
>     tour dans l'ordre d'initiative.
>
> 5.  **Commencez le tour suivant.** Lorsque toutes les personnes
>     impliquées dans le combat ont eu leur tour, le round se termine.
>     Répétez l'étape 4 jusqu'à ce que le combat s'arrête.


### Surprise

Une bande d'aventuriers s'approche furtivement d'un camp de bandits,
surgissant des arbres pour les attaquer. Un cube gélatineux glisse dans
un passage du donjon, sans être remarqué par les aventuriers jusqu'à ce
qu'il engloutisse l'un d'entre eux. Dans ces situations, un côté de
la bataille gagne la surprise sur l'autre.

Le MJ détermine qui peut être surpris. Si aucun des camps n'essaie
d'être furtif, ils se remarquent automatiquement. Sinon, le MJ compare
les tests de Dextérité (Discrétion) de quiconque se cache avec le score
de Sagesse passive (Perception) de chaque créature du camp adverse. Tout
personnage ou monstre qui ne remarque pas une menace est surpris au
début de la rencontre.

Si vous êtes surpris, vous ne pouvez pas bouger ou faire une action lors
de votre premier tour de combat, et vous ne pouvez pas faire de réaction
avant la fin de ce tour. Un membre d'un groupe peut être surpris même
si les autres membres ne le sont pas.



### Initiative

L'initiative détermine l'ordre des tours pendant le combat. Au début
du combat, chaque participant effectue un test de Dextérité pour
déterminer sa place dans l'ordre d'initiative. Le MJ effectue un seul
jet pour un groupe entier de créatures identiques, de sorte que chaque
membre du groupe agit en même temps.

Le MJ classe les combattants dans l'ordre, de celui qui a le plus haut
total de tests de Dextérité à celui qui a le plus bas. C'est dans cet
ordre (appelé ordre d'initiative) qu'ils agissent à chaque tour.
L'ordre d'initiative reste le même d'un round à l'autre.

En cas d'égalité, le MJ décide de l'ordre entre les créatures
contrôlées par le MJ qui sont à égalité, et les joueurs décident de
l'ordre entre leurs personnages à égalité. Le MJ peut décider de
l'ordre si l'égalité est entre un monstre et un personnage joueur. En
option, le MJ peut demander aux personnages et monstres liés de lancer
un d20 pour déterminer l'ordre, le résultat le plus élevé étant le
premier.



### Votre tour

À votre tour, vous pouvez **vous déplacer** d'une distance égale à
votre vitesse et **effectuer une action**. Vous décidez de vous déplacer
ou d'effectuer votre action en premier. Votre vitesse - parfois appelée
vitesse de marche - est notée sur votre feuille de personnage.

Les actions les plus communes que vous pouvez entreprendre sont décrites
dans la section \"Actions en combat\". De
nombreuses caractéristiques de classe et autres capacités offrent des
options supplémentaires pour votre action.

La section \"Mouvement et position\"
donne les règles de votre mouvement.

Vous pouvez renoncer à vous déplacer, à entreprendre une action ou à
faire quoi que ce soit pendant votre tour. Si vous n'arrivez pas à
décider quoi faire à votre tour, envisagez de faire une action Esquive
ou Se tenir prêt, comme décrit dans \"Actions en
combat\".


#### Actions bonus

Diverses caractéristiques de classe, sorts et autres capacités vous
permettent d'effectuer une action supplémentaire à votre tour, appelée
action bonus. La capacité Geste sournois, par exemple, permet à un
roublard d'effectuer une action bonus. Vous ne pouvez effectuer une
action bonus que si une capacité spéciale, un sort ou une autre
caractéristique du jeu indique que vous pouvez faire quelque chose en
tant qu'action bonus. Sinon, vous n'avez pas d'action bonus à faire.
Vous ne pouvez prendre qu'une seule action bonus à votre tour, vous
devez donc choisir quelle action bonus utiliser lorsque vous en avez
plusieurs de disponibles. Vous choisissez quand prendre une action bonus
pendant votre tour, sauf si le moment de l'action bonus est spécifié,
et tout ce qui vous prive de votre capacité à prendre des actions vous
empêche également de prendre une action bonus.



#### Autre activité à votre tour

Votre tour peut inclure une variété de fioritures qui ne nécessitent ni
votre action ni votre déplacement.

Vous pouvez communiquer comme vous le pouvez, par des énoncés et des
gestes brefs, au fur et à mesure que vous prenez votre tour.

Vous pouvez également interagir avec un objet ou une caractéristique de
l'environnement gratuitement, pendant votre déplacement ou votre
action. Par exemple, vous pouvez ouvrir une porte pendant votre
déplacement vers un ennemi, ou dégainer votre arme dans le cadre de la
même action que vous utilisez pour attaquer.

Si vous voulez interagir avec un deuxième objet, vous devez utiliser
votre action. Certains objets magiques et autres objets spéciaux
nécessitent toujours une action pour être utilisés, comme indiqué dans
leur description.

Le MJ peut vous demander d'utiliser une action pour l'une de ces
activités lorsqu'elle nécessite un soin particulier ou lorsqu'elle
présente un obstacle inhabituel. Par exemple, le MJ peut raisonnablement
s'attendre à ce que vous utilisiez une action pour ouvrir une porte
bloquée ou tourner une manivelle pour abaisser un pont-levis.




### Réactions

Certaines capacités spéciales, certains sorts et certaines situations
vous permettent d'effectuer une action spéciale appelée réaction. Une
réaction est une réponse instantanée à un déclencheur quelconque, qui
peut se produire à votre tour ou à celui d'un autre joueur. L'attaque
d'opportunité est le type de réaction le plus
commun.

Lorsque vous faites une réaction, vous ne pouvez pas en faire une autre
avant le début de votre prochain tour. Si la réaction interrompt le tour
d'une autre créature, cette dernière peut continuer son tour juste
après la réaction.




## Mouvement et position

En combat, les personnages et les monstres sont en mouvement constant,
utilisant souvent le déplacement et la position pour prendre le dessus.

A votre tour, vous pouvez vous déplacer sur une distance égale à votre
vitesse. Vous pouvez utiliser autant ou aussi peu de votre vitesse que
vous le souhaitez à votre tour, en suivant les règles ici.

Votre mouvement peut inclure le saut, l'escalade et la natation. Ces
différents modes de déplacement peuvent être combinés avec la marche, ou
bien ils peuvent constituer l'intégralité de votre déplacement. Quelle
que soit la façon dont vous vous déplacez, vous déduisez la distance de
chaque partie de votre mouvement de votre vitesse jusqu'à ce qu'elle
soit épuisée ou que vous ayez fini de vous déplacer.


### La rupture de votre déménagement

Vous pouvez fractionner votre mouvement à votre tour, en utilisant une
partie de votre vitesse avant et après votre action. Par exemple, si
vous avez une vitesse de 30 pieds, vous pouvez vous déplacer de 10
pieds, faire votre action, puis vous déplacer de 20 pieds.


#### Se déplacer entre les attaques

Si vous entreprenez une action qui comprend plus d'une attaque d'arme,
vous pouvez fractionner encore plus votre mouvement en vous déplaçant
entre ces attaques. Par exemple, un guerrier qui peut effectuer deux
attaques avec la capacité Attaque supplémentaire et qui a une vitesse de
25 pieds peut se déplacer de 10 pieds, effectuer une attaque, se
déplacer de 15 pieds, puis attaquer à nouveau.



#### Utilisation de différentes vitesses

Si vous avez plus d'une vitesse, comme votre vitesse de marche et une
vitesse de vol, vous pouvez passer d'une vitesse à l'autre pendant
votre déplacement. À chaque fois que vous changez de vitesse, soustrayez
la distance que vous avez déjà parcourue de la nouvelle vitesse. Le
résultat détermine la distance que vous pouvez parcourir. Si le résultat
est inférieur ou égal à 0, vous ne pouvez pas utiliser la nouvelle
vitesse pendant le déplacement en cours.

Par exemple, si vous avez une vitesse de 30 et une vitesse de vol de 60
parce qu'un magicien vous a lancé le sort *Vol*, vous pouvez
voler sur 20 pieds, puis marcher sur 10 pieds, et enfin sauter dans les
airs pour voler sur 30 pieds de plus.




### Terrain difficile

Les combats se déroulent rarement dans des pièces nues ou sur des
plaines sans relief. Les cavernes jonchées de rochers, les forêts
couvertes de ronces, les escaliers traîtres - le cadre d'un combat
typique contient un terrain difficile.

Chaque pied de mouvement en terrain difficile coûte 1 pied
supplémentaire. Cette règle est vraie même si plusieurs choses dans un
espace comptent comme terrain difficile.

Les meubles bas, les gravats, les broussailles, les escaliers abrupts,
la neige et les tourbières peu profondes sont des exemples de terrain
difficile. L'espace d'une autre créature, qu'elle soit hostile ou
non, compte également comme un terrain difficile.



### Être à terre

Les combattants se retrouvent souvent allongés sur le sol, soit parce
qu'ils sont déboulés, soit parce qu'ils se jettent à terre. Dans le
jeu, ils sont étourdis .

Vous pouvez **vous mettre à plat ventre** sans utiliser votre vitesse.
**Se mettre debout** demande plus d'efforts ; cela coûte une quantité
de mouvement égale à la moitié de votre vitesse. Par exemple, si votre
vitesse est de 30 pieds, vous devez dépenser 15 pieds de mouvement pour
vous mettre debout. Vous ne pouvez pas vous mettre debout si vous
n'avez plus assez de mouvement ou si votre vitesse est nulle.

Pour vous déplacer lorsque vous êtes à terre, vous devez **ramper** ou
utiliser une magie telle que la téléportation. Chaque pied de mouvement
en rampant coûte 1 pied supplémentaire. Ramper de 1 pied en terrain
difficile coûte donc 3 pieds de mouvement.

> #### Interagir avec les objets qui vous entourent {#interacting-with-objects-around-you}
>
> Voici quelques exemples du genre de choses que vous pouvez faire en
> tandem avec votre mouvement et votre action :
>
> -   dégainer ou rengainer une épée
> -   ouvrir ou fermer une porte
> -   retirer une potion de votre sac à dos
> -   ramasser une hache abandonnée
> -   prendre une babiole sur une table
> -   retirer une bague de votre doigt
> -   mettez de la nourriture dans votre bouche
> -   planter une bannière dans le sol
> -   sortir quelques pièces de votre pochette de ceinture
> -   boire toute l'ale dans un flacon
> -   actionner un levier ou un interrupteur
> -   tirer une torche d'une applique
> -   prendre un livre sur une étagère que vous pouvez atteindre
> -   éteindre une petite flamme
> -   mettre un masque
> -   tirez le capuchon de votre cape vers le haut et sur votre tête.
> -   mettez votre oreille sur une porte
> -   donner un coup de pied à une petite pierre
> -   tourner une clé dans une serrure
> -   taper le sol avec une perche de 3 mètres
> -   remettre un objet à un autre personnage



### Se déplacer autour d'autres créatures

Vous pouvez vous déplacer dans l'espace d'une créature non hostile. En
revanche, vous ne pouvez vous déplacer dans l'espace d'une créature
hostile que si celle-ci est au moins deux tailles plus grande ou plus
petite que vous. N'oubliez pas que l'espace d'une autre créature est
un terrain difficile pour vous.

Qu'une créature soit amie ou ennemie, vous ne pouvez pas volontairement
terminer votre mouvement dans son espace.

Si vous quittez la portée d'une créature hostile pendant votre
déplacement, vous provoquez une attaque
d'opportunité.



### Mouvement Volant

Les créatures volantes bénéficient des nombreux avantages de la
mobilité, mais elles doivent aussi faire face au danger de la chute. Si
une créature volante est mise au tapis, que sa vitesse est réduite à 0
ou qu'elle est privée de la possibilité de se déplacer, elle tombe, à
moins qu'elle ne soit capable de planer ou qu'elle soit maintenue en
l'air par la magie, comme par le sort *Vol*.



### Taille de la créature

Chaque créature occupe une quantité différente d'espace. Le tableau des
catégories de taille indique la quantité d'espace qu'une créature
d'une taille donnée contrôle en combat. Les objets utilisent parfois
les mêmes catégories de taille.

  --------------------------------------
  Taille          Espace
  --------------- ----------------------
  Très petite     2-1/2 par 2-1/2 pieds.
  (TP)            

  Petit           5 par 5 pieds

  Moyen           5 par 5 pieds

  Grand           10 par 10 pieds.

  Très grand      15 par 15 pieds.

  Gargantuesque   20 par 20 pieds ou
                  plus
  --------------------------------------

  : Catégories de taille


#### Espace

L'espace d'une créature est la zone en pieds qu'elle contrôle
effectivement en combat, et non l'expression de ses dimensions
physiques. Une créature moyenne typique ne fait pas 1,5 mètre de large,
par exemple, mais elle contrôle un espace de cette largeur. Si un
hobgobelin de taille moyenne se tient dans une porte de 1,5 m de large,
les autres créatures ne peuvent pas passer à moins que le hobgobelin ne
les laisse faire.

L'espace d'une créature reflète également la zone dont elle a besoin
pour combattre efficacement. Pour cette raison, il y a une limite au
nombre de créatures qui peuvent entourer une autre créature en combat.
En supposant des combattants de taille moyenne, huit créatures peuvent
tenir dans un rayon de 1,5 m autour d'une autre.

Comme les créatures plus grandes prennent plus de place, elles sont
moins nombreuses à pouvoir entourer une créature. Si cinq créatures
Grandes s'entassent autour d'une créature Moyenne ou plus petite, il y
a peu de place pour les autres. En revanche, jusqu'à vingt créatures
moyennes peuvent entourer une créature Gargantuesque.



#### S'adapter à un espace plus restreint

Une créature peut se faufiler dans un espace qui est suffisamment grand
pour une créature d'une taille inférieure à la sienne. Ainsi, une
créature Grand peut se faufiler dans un passage de seulement 1,5 m de
large. Lorsqu'elle se faufile dans un espace, une créature doit
dépenser 1 pied supplémentaire pour chaque pied qu'elle y déplace, et
elle est désavantagée aux jets d'attaque et aux jets de sauvegarde de
Dextérité. Les jets d'attaque contre la créature ont un avantage tant
qu'elle se trouve dans l'espace réduit.





## Actions en combat

Lorsque vous effectuez votre action à votre tour, vous pouvez prendre
une des actions présentées ici, une action que vous avez obtenue grâce à
votre classe ou à une caractéristique spéciale, ou une action que vous
improvisez. De nombreux monstres ont leurs propres options d'action
dans leurs blocs de statuts.

Lorsque vous décrivez une action qui n'est pas détaillée ailleurs dans
les règles, le MJ vous indique si cette action est possible et quel type
de jet vous devez effectuer, le cas échéant, pour déterminer la réussite
ou l'échec.


### Attaque

L'action la plus commune en combat est l'action Attaquer, qu'il
s'agisse de brandir une épée, de tirer une flèche d'un arc ou de se
battre avec les poings.

Avec cette action, vous effectuez une attaque de mêlée ou à distance.
Consultez la section \"Attaquer\" pour
connaître les règles qui régissent les attaques.

Certaines capacités, comme la caractéristique Attaque supplémentaire du
guerrier, vous permettent d'effectuer plus d'une attaque avec cette
action.



### Lancer un sort

Les lanceurs de sorts tels que les magiciens et les clercs, ainsi que de
nombreux monstres, ont accès à des incantations et peuvent les utiliser
à bon escient en combat. Chaque sort a un temps d'incantation, qui
précise si le lanceur doit utiliser une action, une réaction, des
minutes ou même des heures pour lancer le sort. Lancer un sort n'est
donc pas nécessairement une action. La plupart des sorts ont un temps
d'incantation d'une action, de sorte que le lanceur de sorts utilise
souvent son action en combat pour lancer un tel sort.



### Dash

Lorsque vous effectuez l'action Foncer, vous gagnez un mouvement
supplémentaire pour le tour en cours. L'augmentation est égale à votre
vitesse, après application de tout modificateur. Avec une vitesse de 30
pieds, par exemple, vous pouvez vous déplacer jusqu'à 60 pieds pendant
votre tour si vous vous précipitez.

Toute augmentation ou diminution de votre vitesse modifie ce mouvement
supplémentaire de la même quantité. Si votre vitesse de 30 pieds est
réduite à 15 pieds, par exemple, vous pouvez vous déplacer de 30 pieds
au cours de ce tour en vous élançant.



### Désengager

Si vous effectuez l'action Se désangager, votre mouvement ne provoque
pas d'attaques d'opportunité pour le reste du tour.



### Dodge

Lorsque vous effectuez l'action Esquive, vous vous concentrez
entièrement sur l'évitement des attaques. Jusqu'au début de votre
prochain tour, tout jet d'attaque effectué contre vous est désavantagé
si vous pouvez voir l'attaquant, et vous effectuez des jets de
sauvegarde de Dextérité avec avantage. Vous perdez cet avantage si vous
êtes frappé d'incapacité  ou si
votre vitesse tombe à 0.



### Aide

Vous pouvez prêter votre aide à une autre créature dans
l'accomplissement d'une tâche. Lorsque vous effectuez l'action Aider,
la créature que vous aidez gagne un avantage sur le prochain test de
capacité qu'elle effectue pour accomplir la tâche pour laquelle vous
l'aidez, à condition qu'elle effectue ce test avant le début de votre
prochain tour.

Vous pouvez également aider une créature amie à attaquer une créature
située à moins de 1,5 m de vous. Vous feintez, distrayez la cible ou
faites équipe d'une manière ou d'une autre pour rendre l'attaque de
votre allié plus efficace. Si votre allié attaque la cible avant votre
prochain tour, le premier jet d'attaque est effectué avec avantage.



### Cacher {#cacher}

Lorsque vous effectuez l'action Se cacher, vous effectuez un test de
Dextérité (Discrétion) pour tenter de vous cacher, en suivant les règles
de dissimulation. Si vous réussissez, vous gagnez certains avantages,
comme décrit dans la section \"Attaquants et cibles
invisibles\".



### Prêt

Parfois, vous voulez prendre de l'avance sur un ennemi ou attendre une
circonstance particulière avant d'agir. Pour cela, vous pouvez faire
l'action Se tenir prêt à votre tour, ce qui vous permet d'agir en
utilisant votre réaction avant le début de votre prochain tour.

D'abord, vous décidez quelle circonstance perceptible va déclencher
votre réaction. Ensuite, vous choisissez l'action que vous allez
entreprendre en réponse à ce déclencheur, ou vous choisissez de vous
déplacer à votre vitesse en réponse à ce déclencheur. Par exemple : \"Si
le cultiste marche sur la trappe, je tire le levier qui l'ouvre\" et
\"Si le gobelin marche à côté de moi, je m'éloigne\".

Lorsque le déclencheur se produit, vous pouvez soit prendre votre
réaction juste après la fin du déclencheur, soit ignorer le déclencheur.
N'oubliez pas que vous ne pouvez effectuer qu'une seule réaction par
round.

Lorsque vous préparez un sort, vous le lancez comme d'habitude mais
vous retenez son énergie, que vous libérez avec votre réaction lorsque
le déclencheur se produit. Pour être préparé, un sort doit avoir un
temps d'incantation de 1 action, et retenir la magie du sort demande de
la concentration. Si votre concentration est rompue, le sort se dissipe
sans faire d'effet. Par exemple, si vous vous concentrez sur le sort
*toile* et que vous préparez *missile
magique* se termine, et si
vous subissez des dégâts avant de libérer *missile
magique* avec votre réaction, votre concentration
risque d'être rompue.



### Recherche

Lorsque vous effectuez l'action Chercher, vous consacrez votre
attention à trouver quelque chose. Selon la nature de votre recherche,
le MJ peut vous demander de faire un test de Sagesse (Perception) ou un
test d'Intelligence (Investigation).



### Utiliser un objet

Vous interagissez normalement avec un objet tout en faisant autre chose,
comme lorsque vous dégainez une épée dans le cadre d'une attaque.
Lorsqu'un objet nécessite votre action pour être utilisé, vous
effectuez l'action Utiliser un objet. Cette action est également utile
lorsque vous souhaitez interagir avec plus d'un objet pendant votre
tour.




## Faire une attaque

Que vous frappiez avec une arme de mêlée, que vous tiriez une arme à
distance ou que vous fassiez un jet d'attaque dans le cadre d'un sort,
une attaque a une structure simple.

1.  **Choisissez une cible.** Choisissez une cible dans la portée de
    votre attaque : une créature, un objet ou un lieu.
2.  **Déterminez les modificateurs.** Le MJ détermine si la cible est à
    couvert et si vous avez un avantage ou un désavantage contre elle.
    De plus, les sorts, les capacités spéciales et d'autres effets
    peuvent appliquer des pénalités ou des bonus à votre jet d'attaque.
3.  **Résolvez l'attaque.** Vous effectuez le jet d'attaque. En cas de
    succès, vous effectuez un jet de dégâts, sauf si l'attaque en
    question a des règles qui spécifient le contraire. Certaines
    attaques provoquent des effets spéciaux en plus ou à la place des
    dégâts.

Si la question se pose de savoir si ce que vous faites compte comme une
attaque, la règle est simple : si vous faites un jet d'attaque, vous
faites une attaque.


### Jets d'attaque

Lorsque vous effectuez une attaque, votre jet d'attaque détermine si
l'attaque touche ou rate. Pour effectuer un jet d'attaque, lancez un
d20 et ajoutez les modificateurs appropriés. Si le total du jet et des
modificateurs est égal ou supérieur à la classe d'armure (CA) de la
cible, l'attaque réussit. La CA d'un personnage est déterminée lors de
la création du personnage, alors que la CA d'un monstre se trouve dans
son bloc de statistiques.


#### Modificateurs du jet

Lorsqu'un personnage effectue un jet d'attaque, les deux modificateurs
les plus communs sont un modificateur de capacité et le bonus de
maîtrise du personnage. Lorsqu'un monstre effectue un jet d'attaque,
il utilise le modificateur indiqué dans son bloc de statistiques.

***Modificateur d'aptitude.*** Le modificateur de capacité utilisé pour
une attaque avec une arme de corps-à-corps est la Force, et le
modificateur de capacité utilisé pour une attaque avec une arme à
distance est la Dextérité. Les armes qui ont la propriété finesse ou
lancée enfreignent cette règle.

Certains sorts nécessitent également un jet d'attaque. Le modificateur
d'aptitude utilisé pour une attaque de sort dépend de la
caractéristique d'incantation du lanceur de sorts.

***Bonus de maîtrise.*** Vous ajoutez votre bonus de maîtrise à votre
jet d'attaque lorsque vous attaquez avec une arme que vous maîtrisez,
ainsi que lorsque vous attaquez avec un sort.



#### Rouler 1 ou 20

Il arrive que le destin bénisse ou maudisse un combattant, faisant que
le novice touche et que le vétéran rate.

Si le jet de d20 pour une attaque est un 20, l'attaque touche sans
tenir compte des modificateurs ou de la CA de la cible. C'est ce qu'on
appelle un coup critique.

Si le jet de d20 pour une attaque est un 1, l'attaque rate sans tenir
compte des modificateurs ou de la CA de la cible.




### Attaquants et cibles invisibles

Les combattants tentent souvent d'échapper à la vigilance de leurs
ennemis en se cachant, en lançant le sort
d'*invisibilité* ou en se tapissant dans les ténèbres.

Lorsque vous attaquez une cible que vous ne pouvez pas voir, vous avez
un désavantage sur le jet d'attaque. Ceci est vrai que vous deviniez
l'emplacement de la cible ou que vous visiez une créature que vous
pouvez entendre mais pas voir. Si la cible ne se trouve pas à l'endroit
que vous avez ciblé, vous ratez automatiquement votre coup, mais le MJ
se contente généralement de dire que l'attaque a raté, sans préciser si
vous avez deviné correctement l'emplacement de la cible.

Lorsqu'une créature ne peut pas vous voir, vous avez un avantage sur
les jets d'attaque contre elle. Si vous êtes caché - à la fois
invisible et inaudible - lorsque vous effectuez une attaque, vous donnez
votre emplacement lorsque l'attaque touche ou rate.



### Attaques à distance

Lorsque vous effectuez une attaque à distance, vous tirez avec un arc ou
une arbalète, lancez une hachette ou envoyez des projectiles pour
frapper un ennemi à distance. Un monstre peut tirer des épines de sa
queue. De nombreux sorts impliquent également une attaque à distance.


#### Portée

Vous ne pouvez effectuer des attaques à distance que contre des cibles
situées dans une portée donnée. Si une attaque à distance, comme celle
effectuée avec un sort, a une seule portée, vous ne pouvez pas attaquer
une cible au-delà de cette portée.

Certaines attaques à distance, comme celles effectuées avec un arc long
ou un arc court, ont deux portées. Le plus petit chiffre est la portée
normale, et le plus grand chiffre est la portée longue. Votre jet
d'attaque est désavantagé lorsque votre cible est au-delà de la portée
normale, et vous ne pouvez pas attaquer une cible au-delà de la longue
portée.




### Attaques à distance en combat rapproché

Viser une attaque à distance est plus difficile lorsqu'un ennemi est à
côté de vous. Lorsque vous effectuez une attaque à distance avec une
arme, un sort ou tout autre moyen, vous avez un désavantage au jet
d'attaque si vous vous trouvez à moins de 1,5 m d'une créature hostile
qui peut vous voir et qui n'est pas frappée d'incapacité.



### Attaques de mêlée

Utilisée en combat au corps à corps, une attaque de mêlée vous permet
d'attaquer un ennemi à votre portée. Une attaque de mêlée utilise
généralement une arme de poing telle qu'une épée, un marteau de guerre
ou une hache. Un monstre typique effectue une attaque de mêlée
lorsqu'il frappe avec ses griffes, ses cornes, ses dents, ses
tentacules ou une autre partie de son corps. Quelques sorts impliquent
également une attaque de mêlée.

La plupart des créatures ont une **portée** de 1,5 m et peuvent donc
attaquer des cibles situées à 1,5 m d'elles lorsqu'elles effectuent
une attaque de mêlée. Certaines créatures (généralement celles de taille
supérieure à Medium) ont des attaques de mêlée d'une portée supérieure
à 5 pieds, comme indiqué dans leur description.

Au lieu d'utiliser une arme pour effectuer une attaque de mêlée, vous
pouvez utiliser une **frappe sans arme :** un coup de poing, un coup de
pied, un coup de tête ou un coup de force similaire (qui ne comptent pas
comme des armes). En cas de succès, une attaque à mains nues inflige des
dégâts contondants égaux à 1 + votre modificateur de Force. Vous êtes
compétent en matière de frappes à mains nues.


#### Attaques d'opportunité

Dans un combat, tout le monde est constamment à l'affût d'une occasion
de frapper un ennemi qui s'enfuit ou qui passe par là. Une telle frappe
s'appelle une attaque d'opportunité.

Vous pouvez effectuer une attaque d'opportunité lorsqu'une créature
hostile que vous pouvez voir se déplace hors de votre portée. Pour
effectuer cette attaque d'opportunité, vous utilisez votre réaction
pour effectuer une attaque de mêlée contre la créature qui vous
provoque. L'attaque a lieu juste avant que la créature ne quitte votre
portée.

Vous pouvez éviter de provoquer une attaque d'opportunité en effectuant
l'action Se désengager. Vous ne provoquez pas non plus d'attaque
d'opportunité lorsque vous vous téléportez ou lorsque quelqu'un ou
quelque chose vous déplace sans utiliser votre mouvement, action ou
réaction. Par exemple, vous ne provoquez pas d'attaque d'opportunité
si une explosion vous projette hors de portée d'un ennemi ou si la
gravité vous fait tomber devant un ennemi.



#### Combat à deux armes

Lorsque vous effectuez l'action Attaquer et attaquez avec une arme de
mêlée légère que vous tenez dans une main, vous pouvez utiliser une
action bonus pour attaquer avec une autre arme de mêlée légère que vous
tenez dans l'autre main. Vous n'ajoutez pas votre modificateur de
capacité aux dégâts de l'attaque bonus, sauf si ce modificateur est
négatif.

Si l'une ou l'autre des armes possède la propriété lancée, vous pouvez
lancer l'arme, au lieu d'effectuer une attaque de mêlée avec elle.



#### Agrippés

Lorsque vous voulez saisir une créature ou lutter avec elle, vous pouvez
utiliser l'action d'attaque pour effectuer une attaque de mêlée
spéciale, un grappin. Si vous pouvez effectuer plusieurs attaques avec
l'action Attaquer, cette attaque remplace l'une d'entre elles.

La cible de votre grappin ne doit pas avoir plus d'une taille de plus
que vous et doit être à votre portée. En utilisant au moins une main
libre, vous essayez de saisir la cible en effectuant un test
d'agrippement au lieu d'un jet d'attaque : un test de Force
(Athlétisme) contesté par un test de Force (Athlétisme) ou de Dextérité
(Acrobaties) de la cible (la cible choisit la capacité à utiliser). Si
vous réussissez, vous soumettez la cible à la condition agrippée . L'état spécifie les éléments
qui y mettent fin, et vous pouvez libérer la cible quand vous le
souhaitez (aucune action requise).

***Echapper à un grappin.*** Une créature agrippée peut utiliser son
action pour s'échapper. Pour ce faire, elle doit réussir un test de
Force (Athlétisme) ou de Dextérité (Acrobaties) contesté par votre test
de Force (Athlétisme).

***Déplacer une créature agrippée.*** Lorsque vous vous déplacez, vous
pouvez traîner ou transporter la créature agrippée avec vous, mais votre
vitesse est réduite de moitié, sauf si la créature est plus petite que
vous de deux tailles ou plus.

> #### Concours de combat {#contests-in-combat}
>
> La bataille consiste souvent à opposer vos prouesses à celles de votre
> adversaire. Un tel défi est représenté par un concours. Cette section
> comprend les concours les plus communs qui nécessitent une action en
> combat : agripper et bousculer une créature. Le MJ peut utiliser ces
> concours comme modèles pour en improviser d'autres.



#### Bousculer une créature

En utilisant l'action Attaquer, vous pouvez faire une attaque de mêlée
spéciale pour pousser une créature, soit pour la mettre au sol, soit
pour la repousser loin de vous. Si vous pouvez effectuer plusieurs
attaques avec l'action Attaquer, cette attaque remplace l'une d'entre
elles.

La cible ne doit pas avoir plus d'une taille de plus que vous et doit
être à votre portée. Au lieu de faire un jet d'attaque, vous effectuez
un test de Force (Athlétisme) contesté par le test de Force (Athlétisme)
ou de Dextérité (Acrobaties) de la cible (la cible choisit la capacité à
utiliser). Si vous remportez le concours, vous placez la cible à plat
ventre ou la poussez à 1,5 m de vous.





## Abri

Les murs, les arbres, les créatures et autres obstacles peuvent fournir
un abri pendant le combat, rendant une cible plus difficile à atteindre.
Une cible ne peut bénéficier d'un abri que si une attaque ou un autre
effet provient du côté opposé de l'abri.

Il existe trois degrés d'abri. Si une cible se trouve derrière
plusieurs sources d'abri, seul le degré d'abri le plus protecteur
s'applique ; les degrés ne sont pas additionnés. Par exemple, si une
cible se trouve derrière une créature qui lui donne un demi-abri et un
tronc d'arbre qui lui donne trois-quarts d'abri, elle a trois-quarts
d'abri.

Une cible à **demi-abri** bénéficie d'un bonus de +2 à la CA et aux
jets de sauvegarde de Dextérité. Une cible est à demi-abri si un
obstacle bloque au moins la moitié de son corps. L'obstacle peut être
un muret, un grand meuble, un tronc d'arbre étroit ou une créature, que
cette dernière soit un ennemi ou un ami.

Une cible **couverte aux trois quarts** bénéficie d'un bonus de +5 à la
CA et aux jets de sauvegarde de Dextérité. Une cible est à couvert aux
trois quarts si elle est couverte aux trois quarts par un obstacle.
L'obstacle peut être une herse, une fente pour les flèches ou un tronc
d'arbre épais.

Une cible à **couvert total** ne peut pas être visée directement par une
attaque ou un sort, bien que certains sorts puissent atteindre une telle
cible en l'incluant dans une zone d'effet. Une cible bénéficie d'un
abri total si elle est complètement dissimulée par un obstacle.



## Dégâts et Guérison

Les blessures et le risque de mort sont les compagnons constants de ceux
qui explorent les univers de jeux fantastiques. Un coup d'épée, une
flèche bien placée ou un jet de flamme d'un sort de *boule de
feu* peuvent endommager, voire tuer, les créatures les plus
robustes.


### Points de vie

Les points de vie représentent une combinaison d'endurance physique et
mentale, de volonté de vivre et de chance. Les créatures ayant plus de
points de vie sont plus difficiles à tuer. Celles qui ont moins de
points de vie sont plus fragiles.

Les points de vie actuels d'une créature (généralement appelés points
de vie) peuvent être n'importe quel nombre entre le maximum de points
de vie de la créature et 0. Ce nombre change fréquemment lorsque la
créature subit des dégâts ou reçoit des soins.

Chaque fois qu'une créature subit des dégâts, ceux-ci sont soustraits
de ses points de vie. La perte de points de vie n'a aucun effet sur les
capacités d'une créature jusqu'à ce qu'elle tombe à 0 points de vie.



### Roulements de dommages

Chaque arme, sort et capacité de monstre nuisible précise les dégâts
qu'il inflige. Vous lancez le ou les dés de dégâts, ajoutez les
modificateurs éventuels, et appliquez les dégâts à votre cible. Les
armes magiques, les capacités spéciales et d'autres facteurs peuvent
accorder un bonus aux dégâts. Avec une pénalité, il est possible
d'infliger 0 dégât, mais jamais de dégât négatif.

Lorsque vous attaquez avec une **arme,** vous ajoutez votre modificateur
de capacité - le même modificateur que celui utilisé pour le jet
d'attaque - aux dégâts. Un **sort** vous indique quels dés lancer pour
les dégâts et si vous devez ajouter des modificateurs.

Si un sort ou un autre effet inflige des dégâts à **plusieurs cibles**
en même temps, lancez le dé une seule fois pour chacune d'entre elles.
Par exemple, lorsqu'un magicien lance une *boule de feu*
ou qu'un clerc lance Colonne *de flamme*, les dégâts
du sort sont lancés une fois pour toutes les créatures prises dans
l'explosion.


#### Coups critiques

Lorsque vous obtenez un succès critique, vous pouvez lancer des dés
supplémentaires pour les dégâts de l'attaque contre la cible. Lancez
deux fois tous les dés de dégâts de l'attaque et additionnez-les.
Ajoutez ensuite tout modificateur pertinent comme d'habitude. Pour
accélérer le jeu, vous pouvez lancer tous les dés de dégâts en une seule
fois.

Par exemple, si vous obtenez un coup critique avec une dague, lancez 2d4
pour les dégâts, au lieu de 1d4, puis ajoutez votre modificateur
d'aptitude pertinent. Si l'attaque implique d'autres dés de dégâts,
comme ceux de la caractéristique Attaque sournoise du roublard, vous
lancez ces dés deux fois également.



#### Types de dommages

Différentes attaques, sorts dommageables et autres effets néfastes
infligent différents types de dégâts. Les types de dégâts n'ont pas de
règles propres, mais d'autres règles, comme la résistance aux dégâts,
reposent sur les types.

Les types de dégâts suivent, avec des exemples pour aider un MJ à
attribuer un type de dégâts à un nouvel effet.

***Acide.*** Le jet corrosif du souffle d'un dragon noir et les enzymes
dissolvantes sécrétées par un pouding noir infligent des dégâts acides.

***Contondant.*** Les attaques par force contondante - marteaux, chutes,
constrictions, etc. - infligent des dégâts de contondant.

***Froid.*** Le froid infernal qui irradie de la lance d'un diable de
glace et le souffle glacial d'un dragon blanc infligent des dégâts de
froid.

***Feu.*** Les dragons rouges crachent du feu, et de nombreux sorts
conjurent des flammes pour infliger des dégâts de feu.

***La Force.*** La force est une énergie magique pure concentrée sous
une forme dommageable. La plupart des effets qui infligent des dégâts de
force sont des sorts, notamment *Projectile magique*
et *Arme spirituelle*.

***Foudre.*** Un sort de *foudre* et le souffle d'un
dragon bleu infligent des dégâts de foudre.

***Nécrotique.*** Les dégâts nécrotiques, infligés par certains
morts-vivants et un sort tel que Contact *glacial*,
flétrissent la matière et même l'âme.

***Perforant .*** Les attaques perforantes et empalantes, y compris les
lances et les morsures de monstres, infligent des dégâts perforants.

***Empoisonné.*** Les piqûres venimeuses et le gaz toxique de l'haleine
d'un dragon vert infligent des dégâts de poison.

***Psychique.*** Les capacités mentales, telles que l'explosion
psionique, infligent des dégâts psychiques.

***Radiant.*** Les dégâts radiants, infligés par le sort Colonne de
*flamme* d'un clerc ou par l'arme d'un ange, brûlent
la chair comme le feu et surchargent l'esprit de puissance.

***Tranchant.*** Les épées, les haches et les griffes des monstres
infligent des dégâts tranchants.

***Tonnerre.*** Une explosion de sons, comme l'effet du sort *Vague
tonnante*, inflige des dégâts de tonnerre.




### Résistance aux dégâts et vulnérabilité

Certaines créatures et objets sont excessivement difficiles ou
exceptionnellement faciles à blesser avec certains types de dégâts.

Si une créature ou un objet possède une **résistance** à un type de
dégâts, les dégâts de ce type sont réduits de moitié. Si une créature ou
un objet est **vulnérable à** un type de dommage, les dommages de ce
type sont doublés.

La résistance puis la vulnérabilité sont appliquées après tous les
autres modificateurs de dégâts. Par exemple, une créature possède une
résistance aux dégâts de matraquage et est touchée par une attaque qui
lui inflige 25 points de dégâts de matraquage. La créature se trouve
également dans une aura magique qui réduit tous les dégâts de 5. Les 25
dégâts sont d'abord réduits de 5, puis divisés par deux, de sorte que
la créature subit 10 dégâts.

Les instances multiples de résistance ou de vulnérabilité qui affectent
le même type de dégâts ne comptent que pour une seule instance. Par
exemple, si une créature possède une résistance aux dégâts de feu ainsi
qu'une résistance à tous les dégâts non magiques, les dégâts d'un feu
non magique sont réduits de moitié contre la créature, et non réduits de
trois quarts.



### Guérison

A moins qu'ils n'entraînent la mort, les dommages ne sont pas
permanents. Même la mort est réversible grâce à une magie puissante. Le
repos peut restaurer les points de vie d'une créature, et des méthodes
magiques telles qu'un sort de *soins* ou une *potion de
guérison* peuvent supprimer les dégâts en un instant.

Lorsqu'une créature reçoit des soins, quels qu'ils soient, les points
de vie récupérés sont ajoutés à ses points de vie actuels. Les points de
vie d'une créature ne peuvent pas dépasser son maximum de points de
vie, donc tout point de vie récupéré au-delà de ce nombre est perdu. Par
exemple, un druide accorde à un rôdeur 8 points de vie de guérison. Si
le rôdeur a 14 points de vie actuels et que son maximum de points de vie
est de 20, le rôdeur récupère 6 points de vie auprès du druide, et non
8.

Une créature morte ne peut pas regagner de points de vie tant qu'une
magie telle que le sort Retour *à la vie* ne l'a pas
ramenée à la vie.



### Chute à 0 points de vie

Lorsque vous tombez à 0 point de vie, vous mourrez ou tombez
inconscient, comme expliqué dans les sections suivantes.


#### Mort instantanée

Les dégâts massifs peuvent vous tuer instantanément. Lorsque les dégâts
vous réduisent à 0 point de vie et qu'il en reste, vous mourez si les
dégâts restants sont égaux ou supérieurs à votre maximum de points de
vie.

Par exemple, un clerc avec un maximum de 12 points de vie a actuellement
6 points de vie. S'il subit 18 points de dégâts lors d'une attaque, il
est réduit à 0 point de vie, mais il lui reste 12 points de dégâts.
Comme les dégâts restants sont égaux à son maximum de points de vie, le
clerc meurt.



#### Tomber inconscient

Si les dégâts vous ramènent à 0 points de vie et ne parviennent pas à
vous tuer, vous tombez inconscient. Cette inconscience prend fin si
vous regagnez des points de vie.



#### Jets de sauvegarde contre la mort

Chaque fois que vous commencez votre tour avec 0 points de vie, vous
devez effectuer un jet de sauvegarde spécial, appelé jet de sauvegarde
contre la mort, pour déterminer si vous vous rapprochez de la mort ou si
vous vous accrochez à la vie. Contrairement aux autres jets de
sauvegarde, celui-ci n'est pas lié à un score de capacité. Vous êtes
maintenant entre les mains du destin, aidé uniquement par des sorts et
des caractéristiques qui améliorent vos chances de réussir un jet de
sauvegarde.

Lancez un d20. Si le résultat est de 10 ou plus, vous réussissez. Sinon,
vous échouez. Un succès ou un échec n'a aucun effet en soi. À votre
troisième réussite, vous devenez stable (voir ci-dessous). Lors de votre
troisième échec, vous mourrez. Les succès et les échecs n'ont pas
besoin d'être consécutifs ; gardez la trace des deux jusqu'à ce que
vous obteniez trois cartes identiques. Le nombre des deux est remis à
zéro lorsque vous regagnez des points de vie ou devenez stable.

***Jet de 1 ou 20.*** Lorsque vous effectuez un jet de sauvegarde contre
la mort et obtenez un 1 sur le d20, cela compte comme deux échecs. Si
vous obtenez un 20 sur le d20, vous regagnez 1 point de vie.

***Dégâts à 0 points de vie.*** Si vous subissez des dégâts alors que
vous avez 0 points de vie, vous subissez un échec au jet de sauvegarde
contre la mort. Si les dégâts sont dus à un coup critique, vous subissez
deux échecs à la place. Si les dégâts sont égaux ou supérieurs à votre
maximum de points de vie, vous subissez une mort instantanée.



#### Stabiliser une créature

La meilleure façon de sauver une créature avec 0 points de vie est de la
guérir. Si la guérison n'est pas possible, la créature peut au moins
être stabilisée afin de ne pas être tuée par un jet de sauvegarde contre
la mort raté.

Vous pouvez utiliser votre action pour administrer les premiers soins à
une créature inconsciente et tenter de la stabiliser, ce qui nécessite
un test de Sagesse (Médecine) DC 10 réussi.

Une créature **stable** ne fait pas de jet de sauvegarde contre la mort,
même si elle a 0 points de vie, mais elle reste inconsciente. La
créature cesse d'être stable, et doit recommencer à faire des jets de
sauvegarde contre la mort, si elle subit des dégâts. Une créature stable
qui n'est pas guérie regagne 1 point de vie après 1d4 heures.



#### Les monstres et la mort

La plupart des MJ font mourir un monstre à l'instant où il tombe à 0
points de vie, plutôt que de le faire tomber inconscient et faire des
jets de sauvegarde contre la mort.

Les méchants puissants et les personnages spéciaux non joueurs sont des
exceptions communes ; le MJ peut les faire tomber inconscients et suivre
les mêmes règles que les personnages joueurs.




### Déblocage d'une créature

Parfois, un attaquant souhaite neutraliser un ennemi, plutôt que de lui
porter un coup mortel. Lorsqu'un attaquant réduit une créature à 0
points de vie avec une attaque de mêlée, il peut la débloquer.
L'attaquant peut faire ce choix au moment où les dégâts sont infligés.
La créature tombe inconsciente et est stable.



### Points de vie temporaires

Certains sorts et capacités spéciales confèrent des points de vie
temporaires à une créature. Les points de vie temporaires ne sont pas
des points de vie réels ; ils constituent un tampon contre les dégâts,
une réserve de points de vie qui vous protège des blessures.

Lorsque vous avez des points de vie temporaires et que vous subissez des
dégâts, les points de vie temporaires sont perdus en premier, et les
dégâts restants sont reportés sur vos points de vie normaux. Par
exemple, si vous avez 5 points de vie temporaires et que vous subissez 7
dégâts, vous perdez les points de vie temporaires et subissez ensuite 2
dégâts.

Comme les points de vie temporaires sont distincts de vos points de vie
réels, ils peuvent dépasser votre maximum de points de vie. Un
personnage peut donc avoir un maximum de points de vie et recevoir des
points de vie temporaires.

La Guérison ne peut pas restaurer les points de vie temporaires, et ils
ne peuvent pas être additionnés. Si vous avez des points de vie
temporaires et que vous en recevez d'autres, vous décidez de garder
ceux que vous avez ou de gagner les nouveaux. Par exemple, si un sort
vous accorde 12 points de vie temporaires alors que vous en avez déjà
10, vous pouvez avoir 12 ou 10, mais pas 22.

Si vous avez 0 points de vie, recevoir des points de vie temporaires ne
vous rend pas votre conscience et ne vous stabilise pas. Ils peuvent
toujours absorber les dégâts qui vous sont infligés pendant que vous
êtes dans cet état, mais seule une véritable guérison peut vous sauver.

À moins qu'une caractéristique qui vous accorde des points de vie
temporaires n'ait une durée, ils durent jusqu'à ce qu'ils soient
épuisés ou que vous terminiez un repos long.




## Combattant monté

Un chevalier qui se lance dans la bataille sur un cheval de guerre, un
magicien qui lance des sorts depuis le dos d'un griffon ou un clerc qui
s'envole dans le ciel sur un pégase profitent tous des avantages de la
vitesse et de la mobilité qu'offre une monture.

Une créature volontaire d'au moins une taille de plus que vous et dotée
d'une anatomie appropriée peut servir de monture, selon les règles
suivantes.


### Montage et démontage

Une fois pendant votre déplacement, vous pouvez monter une créature qui
se trouve à moins de 1,5 m de vous ou descendre de cheval. Cela vous
coûte une quantité de mouvement égale à la moitié de votre vitesse. Par
exemple, si votre vitesse est de 30 pieds, vous devez dépenser 15 pieds
de mouvement pour monter un cheval. Par conséquent, vous ne pouvez pas
le monter si vous n'avez plus 15 pieds de mouvement ou si votre vitesse
est de 0.

Si un effet déplace votre monture contre sa volonté alors que vous êtes
dessus, vous devez réussir un jet de sauvegarde de Dextérité DC 10 ou
tomber de la monture, atterrissant à plat ventre dans un espace situé à
moins de 1,5 m de celle-ci. Si vous êtes débloqué alors que vous êtes à
cheval, vous devez effectuer le même jet de sauvegarde.

Si votre monture est mise au sol, vous pouvez utiliser votre réaction
pour en descendre au moment où elle tombe et atterrir sur vos pieds.
Sinon, vous êtes mis à pied et vous tombez à plat ventre dans un espace
situé à moins de 1,5 m de celle-ci.



### Contrôle d'un support

Lorsque vous êtes monté, vous avez deux options. Vous pouvez soit
contrôler la monture, soit lui permettre d'agir indépendamment. Les
créatures intelligentes, comme les dragons, agissent indépendamment.

Vous ne pouvez contrôler une monture que si elle a été entraînée à
accepter un cavalier. Les chevaux domestiques, les ânes et les créatures
similaires sont supposés avoir reçu un tel entraînement. L'initiative
d'une monture contrôlée change pour correspondre à la vôtre lorsque
vous la montez. Elle se déplace selon vos instructions et n'a que trois
possibilités d'action : Sprint, Désengagement, et Esquive. Une monture
contrôlée peut se déplacer et agir même pendant le tour où vous la
montez.

Une monture indépendante conserve sa place dans l'ordre d'initiative.
Le fait de porter un cavalier n'impose aucune restriction sur les
actions que la monture peut entreprendre, et elle se déplace et agit
comme elle le souhaite. Elle peut fuir le combat, se précipiter pour
attaquer et dévorer un ennemi gravement blessé, ou agir autrement contre
vos souhaits.

Dans les deux cas, si la monture provoque une attaque d'opportunité
alors que vous êtes dessus, l'attaquant peut vous cibler ou cibler la
monture.




## Combat sous-marin

Lorsque les aventuriers poursuivent les Sahuagin jusqu'à leurs maisons
sous-marines, combattent les requins dans une ancienne épave ou se
retrouvent dans une salle de donjon inondée, ils doivent se battre dans
un environnement difficile. Sous l'eau, les règles suivantes
s'appliquent.

Lors d'une **attaque avec une arme de mêlée**, une créature qui n'a
pas de vitesse de nage (naturelle ou accordée par la magie) a un
désavantage au jet d'attaque, sauf si l'arme est une dague, un
javelot, une épée courte, une lance ou un trident.

Une attaque avec une **arme à distance** rate automatiquement une cible
située au-delà de la portée normale de l'arme. Même contre une cible à
portée normale, le jet d'attaque a un désavantage, sauf si l'arme est
une arbalète, un filet ou une arme lancée comme un javelot (y compris
une lance, un trident ou une fléchette).

Les créatures et les objets qui sont entièrement immergés dans l'eau
ont une résistance aux dégâts du feu.


