# tap-to-focus

*[English](README.md)*

Une appli Android qui bloque les applis de ton choix jusqu'à ce que tu scannes un tag NFC.

- **Mode libre** : rien n'est bloqué. Scanne ton tag pour passer en mode travail.
- **Mode travail** : les applis bloquées affichent un écran de blocage. Un scan du tag les débloque pour 1, 5 ou 15 minutes, ou une durée libre. Les réglages restent verrouillés jusqu'au scan suivant.
- **Plusieurs tags** : bureau, porte-clés, chez un ami… N'importe quel tag associé débloque.
- **Tes propres sons et image** : à choisir dans ⚙ → *Sons et image* (un son au scan, un son et une image sur l'écran de blocage). Aucun média n'est fourni avec l'appli.

L'interface est en français et en anglais, selon la langue du téléphone.

## Confidentialité

L'appli n'a pas la permission Internet : rien ne sort du téléphone. Le service d'accessibilité écoute seulement les changements de fenêtre pour savoir quelle appli est au premier plan, et ne lit jamais le contenu de l'écran (`canRetrieveWindowContent="false"`).

## Prérequis

- Android 10 (API 29) ou plus récent, avec NFC
- Un tag NFC capable de stocker des données NDEF (les NTAG213/215/216 conviennent)

## Compilation

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # règles de scan et de verrouillage
```

JDK 17 à 21. Gradle 8.14 ne tourne pas sur les JDK plus récents : au besoin, utilise le JBR fourni avec Android Studio : `JAVA_HOME=/chemin/vers/android-studio/jbr`.

## Installation sur le téléphone

1. Installe l'APK, puis active le service d'accessibilité : *Accessibilité → Applications installées → Tap to Focus*. Si l'interrupteur est grisé : *Infos sur l'appli → ⋮ → Autoriser les paramètres restreints*.
2. Associe un tag : l'appli y écrit un petit enregistrement NDEF, pour que le scanner ouvre ensuite l'appli.
3. Choisis les applis à bloquer et la durée de déblocage.

## Licence

Code : [CeCILL-B](LICENSE), © 2026 Neveu Grégor.

Ressources tierces :

- Polices IBM Plex Sans et JetBrains Mono : SIL Open Font License 1.1 (voir [`licences-polices/`](licences-polices/))
- Icônes : [Lucide](https://lucide.dev), licence ISC

## Contact

contact.neveugregor@proton.me
