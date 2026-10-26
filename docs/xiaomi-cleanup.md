# Nettoyage réversible : notes pour Xiaomi Android TV 11

Ces observations proviennent d'un Xiaomi MiTV-MSSP3 (`machuca`, Android 11, environ 2 Go de RAM). Elles ne constituent pas une liste universelle de services à désactiver.

## Commencer par un état initial

Avec les Platform Tools Android officiels et le débogage ADB activé sur votre TV :

```powershell
adb connect TV_IP:5555
adb -s TV_IP:5555 shell getprop ro.product.model
adb -s TV_IP:5555 shell df -h /data
adb -s TV_IP:5555 shell dumpsys meminfo
adb -s TV_IP:5555 shell pm list packages -d
```

Remplacer `TV_IP` par l'adresse de la TV. Conserver les résultats et l'état d'origine de chaque paquet localement.

## Caches

```powershell
adb -s TV_IP:5555 shell pm trim-caches 1073741824 internal
```

Cette commande demande à Android de libérer des caches pour atteindre 1 Gio libre si possible. Les caches se reconstruisent ; elle ne garantit pas un gain durable et n'efface pas les données avec `pm clear`.

## Paquets examinés sur ce modèle

| Paquet | Fonction |
| --- | --- |
| `com.miui.tv.analytics` | Statistiques Xiaomi |
| `com.xiaomi.statistic` | Statistiques Xiaomi |
| `com.xiaomi.floatingframe` | Module de démonstration |
| `fusion.android.tv.demo` | Démonstration en magasin |
| `com.google.android.tvrecommendations` | Recommandations de l'ancien accueil |
| `com.mitv.tvhome.atv` | PatchWall |
| `com.mitv.tvhome.michannel` | Mi Channel |
| `com.mitv.tvhome.mitvplus` | MiTV+ |
| `com.google.android.tvlauncher` | Ancien accueil Google |

Pour un paquet présent, dont la fonction est comprise et qui n'est pas utilisé :

```powershell
adb -s TV_IP:5555 shell dumpsys package PACKAGE_NAME
adb -s TV_IP:5555 shell pm disable-user --user 0 PACKAGE_NAME
```

Le rétablissement dépend de son état initial : `pm default-state --user 0 PACKAGE_NAME` si cet état était `enabled=0`, ou `pm enable --user 0 PACKAGE_NAME` s'il était explicitement activé. Ne pas supprimer les données du paquet.

Pour l'ancien accueil, installer et tester le nouvel accueil **avant** toute désactivation. Les composants réseau, Bluetooth, DRM, Google Play Services, Chromecast (`com.google.android.apps.mediashell`), télécommande et entrées TV/HDMI doivent rester disponibles.

La restauration de l'accueil Google sur ce modèle, si son état initial était celui par défaut :

```powershell
adb -s TV_IP:5555 shell pm default-state --user 0 com.google.android.tvlauncher
adb -s TV_IP:5555 shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
adb -s TV_IP:5555 shell input keyevent 3
```

Les mesures du prototype ont montré environ 180 Mio de PSS pour l'ancien accueil et 22 à 64 Mio pour le nouvel accueil suivant son état. Elles ne prédisent pas la fluidité de lecture vidéo sur une autre TV.
