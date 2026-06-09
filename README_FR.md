# LunaBeat

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

- LunaBeat souhaite offrir une expérience de musique locale plus pure et plus immersive, comme une musique nocturne, entre mélodie et paroles.
---
- LunaBeat est un lecteur mobile axé sur la musique locale et l'expérience des paroles, combinant lecture musicale, édition des paroles et synchronisation mot par mot.
- Il prend en charge la lecture audio locale, la création de paroles mot par mot avec duos et voix de fond, l'aperçu et l'édition des paroles, l'export en `.lrc` / `.elrc` / `.ttml`, ainsi que le traitement par lots des métadonnées audio et des paroles.
- De l'importation à l'édition, puis de la lecture à l'export, LunaBeat vise une expérience locale de musique et de paroles plus complète et immersive.

## Pourquoi LunaBeat

- Ce n'est pas seulement un éditeur de paroles : il peut aussi servir de lecteur de musique locale au quotidien.
- Créez des paroles synchronisées mot par mot de niveau professionnel directement sur mobile.
- Importez et exportez plusieurs formats adaptés aux flux de travail courants autour des paroles.
- La bibliothèque musicale et les outils de métadonnées intégrés réduisent les allers-retours entre applications.
- Le traitement par lots intégré convient à l'organisation de grandes collections de chansons.

## Aperçu de l'Interface

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/mainPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/searchPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricEditPage.jpg" width="30%" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage2.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/musicEditPage.jpg" width="30%" />
</p>

## Vue d'Ensemble des Fonctions

### 1. Synchronisation et Édition des Paroles
- Synchronisation en trois boutons : Début / Continuer / Fin
- Retour / avance rapide, lecture / pause, vitesse de 0.25x à 3x, saut vers un temps précis
- Annuler / rétablir pour les temps, les paroles et les opérations sur les lignes
- Touchez le temps de lecture pour entrer en mode suivi et localiser les paroles sélectionnées
- Touchez une unité de paroles pour la sélectionner, double-touchez pour revenir à son début et lancer la lecture

### 2. Importation
- Paroles en texte brut
- LRC (ligne par ligne / mot par mot)
- LRC enrichi (ELRC)
- TTML
- Recherche de paroles en streaming
- Importation de fichiers audio

### 3. Exportation
- LRC (ligne par ligne / mot par mot)
- TTML (prise en charge des duos et voix de fond)

### 4. Aperçu et Traitement par Lots
- Aperçu de l'effet de lecture mot par mot
- Segmentation des mots en un clic
- Décalage des horodatages
- Fusion des paroles en un clic
- Conversion du chinois traditionnel vers le chinois simplifié
- Suppression des lignes vides en un clic

### 5. Opérations sur la Structure des Paroles (Appui Long)
- Opérations sur les paroles : éditer, ajouter, diviser, fusionner, définir le temps, supprimer
- Opérations sur les lignes : ajouter, fusionner, diviser, déplacer, ajouter/modifier une traduction, supprimer

### 6. Lecteur
- Lecture de musique locale avec lecture / pause, précédent / suivant et déplacement dans la progression
- Lecture séquentielle, aléatoire et répétition d'un seul titre
- Gestion de la file en cours, avec saut, suppression et réordonnancement par glisser-déposer
- Intégration du mini lecteur de la bibliothèque avec le lecteur complet
- Lecture en arrière-plan, notifications média système et commandes par touches média
- Minuteur de sommeil, avec arrêt par compte à rebours ou après la chanson en cours
- Intégration avec l'aperçu des paroles pour vérifier l'effet mot par mot pendant la lecture
- Prise en charge des formats audio locaux courants et gestion de compatibilité de lecture pour ALAC

### 7. Bibliothèque Musicale
- Analyse et filtrage de l'audio local
- Playlists, chansons favorites et chansons cinq étoiles
- Import / export de playlists
- Ajouter à la file / lire ensuite
- Correspondance par lots des tags (pochette, titre, artiste, etc.)
- Correspondance par lots des paroles (mot par mot / ligne par ligne / traduction)
- Renommage par lots
- Édition des tags par lots

### 8. Paramètres
- Mode sombre personnalisé
- Durée personnalisée d'avance / retour rapide pour la synchronisation des paroles
- Paramètres de la bibliothèque musicale
- Style et arrière-plan de la barre de lecture
- Affichage des paroles, paroles de bureau, paroles dans la barre d'état et paroles Bluetooth voiture
- Paramètres de taille de pochette des métadonnées audio
- Paramètres de région des métadonnées Apple Music

## Démarrage Rapide en 5 Minutes (Ordre Recommandé)

### Étape 1 : Importer l'Audio et les Paroles
1. Importez d'abord le fichier audio.
2. Choisissez ensuite la méthode selon la source des paroles : texte brut / LRC / ELRC / TTML / recherche.

### Étape 2 : Lire de la Musique Locale
1. Ouvrez la bibliothèque musicale et analysez l'audio local.
2. Touchez une chanson pour lancer la lecture ; le mini lecteur en bas affiche l'état actuel.
3. Dans le lecteur complet, changez de mode de lecture, gérez la file, ouvrez l'aperçu des paroles ou définissez un minuteur.

### Étape 3 : Prétraiter les Paroles
1. Utilisez la segmentation en un clic pour obtenir des unités mot par mot.
2. Si besoin, supprimez les lignes vides, fusionnez les paroles ou convertissez le chinois traditionnel en simplifié.
3. Si la synchronisation existante est globalement décalée, appliquez d'abord un décalage d'horodatage avant l'ajustement fin.

### Étape 4 : Commencer la Synchronisation (Trois Boutons Clés)
- Début : définit le début de l'unité actuelle sans sauter.
- Continuer : définit la fin de l'unité actuelle, passe automatiquement à la suivante et écrit son début.
- Fin : définit la fin de l'unité actuelle et passe à la suivante, sans écrire le début suivant.

Rythme recommandé : `Début -> Continuer -> Continuer -> ... -> Fin`, phrase par phrase.

### Étape 5 : Affiner le Temps et le Texte
1. Activez le mode suivi pour localiser automatiquement l'unité actuelle pendant la lecture.
2. Double-touchez les paroles pour revenir rapidement à leur début et réécouter.
3. Utilisez annuler / rétablir pour comparer rapidement avant et après modification.
4. Appuyez longuement sur les paroles ou les lignes pour diviser, fusionner, déplacer, traduire ou corriger la structure.

### Étape 6 : Définir Duos et Traductions (Facultatif)
- Types de duo : duo gauche (voix principale), duo droit (voix secondaire), fond (voix de fond).
- Les traductions peuvent être importées ou modifiées manuellement.
- Remarque : les types de duo ne s'appliquent que dans le flux TTML.

### Étape 7 : Prévisualiser et Exporter
1. Vérifiez l'effet mot par mot dans la page d'aperçu des paroles.
2. Exportez selon l'usage :
   - Pour les lecteurs généraux, privilégiez `LRC`
   - Si des informations de duo / voix de fond sont nécessaires, privilégiez `TTML`

## Interface de Synchronisation

- Colonnes : début / paroles / fin / traduction
- Localisation : permet d'aller aux paroles sélectionnées
- Saut temporel : touchez le temps dans la zone de synchronisation pour aller à un point personnalisé
- Titre : touchez le titre pour obtenir le chemin audio

## Flux Conseillé pour la Bibliothèque Musicale

1. Analysez l'audio local, puis vérifiez et filtrez les chansons cibles.
2. Organisez les contenus souvent écoutés par album, playlist, favoris ou cinq étoiles.
3. Lisez les chansons et gérez la file actuelle ; si besoin, définissez lire ensuite ou un minuteur.
4. Faites correspondre les tags par lots pour compléter pochette / titre / artiste et autres métadonnées.
5. Faites correspondre les paroles par lots pour compléter versions mot par mot / ligne par ligne et traductions.
6. Finalisez avec le renommage et l'édition par lots.

## FAQ

### 1. Quand exporter en LRC, et quand en TTML ?
- Si vous avez seulement besoin d'un affichage classique des paroles : choisissez LRC.
- Si vous avez besoin de duos, de voix de fond ou d'une structure plus complète : choisissez TTML.

### 2. Pourquoi l'effet de duo ne s'affiche-t-il pas après configuration ?
Les types de duo ne sont pris en charge qu'en TTML. Vérifiez que vous utilisez le flux TTML et que vous exportez en TTML.

### 3. Les paroles sont globalement trop en avance ou en retard. Comment corriger vite ?
Utilisez d'abord le décalage des horodatages pour l'alignement global, puis revenez à la page de synchronisation pour les ajustements locaux.

### 4. Quelle est l'erreur la plus courante chez les débutants ?
Souvent, synchroniser avant de segmenter les mots. Il est recommandé de terminer la segmentation et le nettoyage du texte avant de commencer la synchronisation.

## Special Thanks

Inspired by:
- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [Music Tag](https://www.cnblogs.com/vinlxc/p/11932130.html)

Technical support:

- [TagLib](https://github.com/taglib/taglib)
- [Lyricon](https://github.com/tomakino/lyricon)
- [SongSync](https://github.com/Lambada10/SongSync)
- [LDDC](https://github.com/chenmozhijin/LDDC)
- [OpenCC](https://github.com/BYVoid/OpenCC)
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)
- [163Music2Tag](https://gitee.com/Wangs-official/163Music2Tag)
- [TagLib (Kyant0)](https://github.com/Kyant0/taglib)
- [uCrop](https://github.com/Yalantis/uCrop)
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)
- [EdgeTranslucent](https://github.com/qinci/EdgeTranslucent)
- [lottie-android](https://github.com/airbnb/lottie-android)
- [ICU Transliterator](https://unicode-org.github.io/icu/userguide/transforms/general/)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)

## Communauté et Retours

- [Rejoindre le groupe Telegram LunaBeat](https://t.me/+qXs6mjKqwhw3Zjll)
- [Rejoindre le groupe QQ LunaBeat](https://qm.qq.com/q/N0fBvuWKOY)  ID du groupe : 964680520
