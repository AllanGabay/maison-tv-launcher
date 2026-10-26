# Maison TV

Un accueil Android TV natif, sobre et chaleureux : vos applications, une maison illustrée, l'heure et la date. Écrit en Java avec les composants Android standards, sans WebView, publicité, connexion Internet ni service permanent.

![Fond d'accueil Maison TV](assets/wallpaper.png)

## Ce que contient le projet

- Un dock pour Netflix, YouTube, CANAL+, Prime Video et Disney+.
- Un panneau pour toutes les applications installées, pilotable à la télécommande.
- Des accès aux réglages Android et à la télévision classique.
- Une horloge et une date en français.
- Un lancement unique après démarrage, uniquement si Maison TV est toujours l'accueil par défaut.
- Un fond générique sans information familiale. Les icônes des applications sont chargées depuis les applications installées sur la TV.

La version personnalisée à l'origine du projet a été testée sur un Xiaomi MiTV-MSSP3 sous Android TV 11 : navigation à la télécommande, ouverture des cinq applications, réglages, TV, bouton Maison, redémarrage et sortie de veille. Chromecast a été vérifié par sa présence et sa joignabilité sur le réseau ; la lecture vidéo, le son et un cast depuis un téléphone ne font pas partie de ces vérifications. Le lanceur n'implémente pas Chromecast : il conserve le récepteur déjà présent sur la TV.

## Compiler sous Windows

Prérequis : Python 3.10 ou plus récent, une connexion Internet pour le premier téléchargement, et l'acceptation des licences des outils Android et Java utilisés. Le script récupère les outils portables auprès de Google et d'Eclipse Adoptium et vérifie leurs empreintes.

```powershell
python setup_build_tools.py
python build_launcher.py
```

Résultat : `launcher/build/maison-tv.apk`. Une clé de signature de développement est générée localement dans `tools/signing/` ; elle n'est jamais à publier. Conserver cette clé pour pouvoir mettre à jour la même installation. Le mot de passe de développement par défaut est `android` ; il peut être changé avant la génération de la clé via `MAISON_TV_DEBUG_PASSWORD`. Ce mécanisme local n'est pas une chaîne de publication de production.

Pour utiliser des outils déjà présents ou un autre fond :

```powershell
python build_launcher.py --tools-root CHEMIN_BUILD_ENV --wallpaper assets/local/mon-fond.png
```

Placer les fonds personnels dans `assets/local/`, exclu de Git. La version publique utilise un identifiant d'application distinct de la version familiale d'origine.

## Installer et choisir l'accueil

Installer les [Android SDK Platform Tools officiels](https://developer.android.com/tools/releases/platform-tools), activer le débogage ADB sur la TV puis accepter l'ordinateur à l'écran.

```powershell
adb connect TV_IP:5555
adb -s TV_IP:5555 install -r launcher/build/maison-tv.apk
adb -s TV_IP:5555 shell am start -n fr.maisontv.launcher/.HomeActivity
adb -s TV_IP:5555 shell cmd package set-home-activity --user 0 fr.maisontv.launcher/.HomeActivity
adb -s TV_IP:5555 shell input keyevent 3
```

Remplacer `TV_IP` par l'adresse de votre TV. Tester les réglages et les applications avant de modifier l'ancien accueil. Sur certains Android TV, l'accueil constructeur garde la priorité malgré `set-home-activity` : voir les [notes de nettoyage et de restauration Xiaomi](docs/xiaomi-cleanup.md), applicables au modèle documenté uniquement.

## Utilisation

Flèches et **OK** pour naviguer ; **Maison** pour revenir à l'accueil ; **Menu** pour ouvrir toutes les applications ; **Retour** pour fermer ce panneau. Les autres applications restent installées et accessibles. Après un redémarrage complet, le firmware Xiaomi peut afficher la télévision avant de distribuer l'événement de fin de démarrage ; le nouvel accueil s'ouvre ensuite. Une veille profonde peut couper le réseau et nécessiter un réveil à la télécommande.

## Personnaliser

Dans `launcher/src/fr/maisontv/launcher/HomeActivity.java`, modifier `packages` et `labels` pour changer les cinq raccourcis. Le bouton Télévision cible le composant MediaTek/Xiaomi testé : adapter son paquet pour un autre constructeur. L'apparence, les libellés et la langue de la date sont définis dans ce même fichier.

L'interface est conçue pour une TV 16:9 et a été vérifiée en 1920 × 1080. La permission `RECEIVE_BOOT_COMPLETED` sert uniquement au lancement après démarrage. Aucun accès réseau, compte, stockage externe ou service d'accessibilité n'est demandé.

## Contenu public

Les journaux de diagnostic, adresses réseau, comptes, captures personnelles, fonds familiaux, clés de signature, APK générés et outils téléchargés sont exclus du dépôt. Le fond générique a été produit avec l'outil de génération d'images intégré ; sa description est dans [assets/PROMPT.txt](assets/PROMPT.txt).
