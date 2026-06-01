# Calcul Mental

Jeu Android de calcul mental développé avec Android Studio.

Le principe est simple : des opérations apparaissent à l’écran et le joueur doit entrer la bonne réponse avant la fin du temps imparti. Le jeu propose plusieurs modes avec une difficulté progressive, des boss, des miniboss, des vies, un système de scores et des récompenses sous forme d’étoiles.

## Concept

Le joueur doit résoudre des calculs mentaux le plus rapidement possible.  
Les opérations peuvent être des additions, soustractions, multiplications ou divisions.

Selon le mode choisi, le joueur joue avec un nombre limité de vies ou avec un temps global à gérer. Plus la partie avance, plus les calculs deviennent difficiles.

## Fonctionnalités principales

- Calculs aléatoires
- Additions, soustractions, multiplications et divisions
- Divisions toujours entières
- Système de vies
- Système de timer
- Boss
- Modes de jeu classiques et contre-la-montre
- Sauvegarde des meilleurs scores avec SQLite
- Tableau des 10 meilleurs scores
- Étoiles de progression sur le profil
- Musique de jeu
- Interface multilingue (français/allemand)

## Menu principal

Le menu principal permet d’accéder aux modes de jeu basés sur les vies.

### Mode classique

Le mode classique contient 30 niveaux.

Le joueur dispose de 3 vies au départ. La difficulté augmente progressivement pendant la partie.

Des boss apparaissent aux niveaux suivants :

- Niveau 10
- Niveau 20
- Niveau 30

Terminer le mode classique débloque une étoile de profil et donne accès au mode difficile.

### Mode difficile

Le mode difficile est débloqué après avoir terminé le mode classique.

Dans ce mode :

- le joueur n’a qu’une seule vie
- les 4 types d’opérations sont disponibles dès le début
- le temps est réduit
- le fond devient rouge clair
- les boss apparaissent aux niveaux 10, 20 et 30
- les boss sont aléatoires
- vaincre un boss ne redonne pas de cœur

Terminer le mode difficile débloque une étoile.

### Mode endless

Le mode endless est un mode infini.

Le joueur avance tant qu’il réussit à survivre. Des boss apparaissent régulièrement tous les 16 niveaux.

Vaincre un boss permet de récupérer un cœur supplémentaire, avec un maximum de 4 cœurs.

Si le joueur atteint un score de 48 ou plus, il débloque une étoile.

## Menu contre-la-montre

Un second menu est accessible depuis le menu principal avec une flèche vers la droite.

Ce menu contient les modes de jeu basés sur un temps global plutôt que sur des vies.

Dans ces modes, le timer est commun à toute la partie :

- réussir une opération normale ajoute 7 secondes
- rater une opération normale retire 5 secondes et fait passer au niveau suivant
- réussir un boss ajoute 15 secondes
- rater un boss retire 5 secondes, mais le joueur garde le même boss

Le timer est mis en pause pendant les transitions entre les niveaux.

### Contre Fredbear

Le mode Contre Fredbear contient 50 niveaux.

Le joueur commence avec 1 minute.

La difficulté augmente tous les 10 niveaux. Des boss aléatoires apparaissent aux niveaux :

- 16
- 33
- 50

Terminer ce mode débloque une étoile et permet d’accéder au mode Contre Nightmare.

### Contre Nightmare

Le mode Contre Nightmare est débloqué après avoir terminé Contre Fredbear.

Dans ce mode :

- le joueur commence avec 45 secondes
- la difficulté est au maximum dès le début
- le fond est noir
- les boss sont aléatoires aux niveaux 16, 33 et 50

Terminer ce mode débloque une étoile.

### Endless chrono

Le mode Endless chrono reprend le fonctionnement de Contre Fredbear, mais sans limite de niveaux.

Les boss apparaissent tous les 16 niveaux.

Si le joueur atteint un score de 80 ou plus, il débloque une étoile.

## Boss

Le jeu contient plusieurs types de boss.

### Trouver l’inconnu

Le joueur doit trouver la valeur manquante.

Exemple :

```text
? + 65 = 202
```

### X + Y

Le joueur doit résoudre deux petites opérations avant de donner le résultat final.

Exemple :

```text
x + y = ?
x = 10 + 78
y = 12 × 3
```

### Longue opération

Le joueur doit résoudre une opération plus longue.

Exemple :

```text
100 + 45 - 12 × 2 = ?
```

## Highscores

Le jeu possède un tableau des scores.

Deux classements sont disponibles :

- un classement pour les modes avec vies
- un classement indépendant pour les modes contre-la-montre

Chaque classement affiche les 10 meilleurs scores enregistrés.

Les scores sont sauvegardés localement.

## Système d’étoiles

Le joueur peut débloquer plusieurs étoiles selon ses réussites.


## Technologies utilisées

- Android Studio
- Java
- XML
- SQLite
- MediaPlayer
- VideoView

## Installation

1. Cloner le projet :

```bash
git clone https://github.com/Joyenfeu/Projet-Android.git
```

2. Ouvrir le projet avec Android Studio.

3. Laisser Android Studio synchroniser Gradle.

4. Lancer l’application sur un émulateur ou un téléphone Android.
