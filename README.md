# tap-to-focus

An Android app that blocks the apps you choose until you scan an NFC tag.

- **Free mode**: nothing is blocked. Scan your tag to start work mode.
- **Work mode**: the blocked apps show a block screen. Scanning the tag unlocks them for 1, 5, 15 or a custom number of minutes. Settings stay locked until the next scan.
- **Several tags**: desk, key ring, at a friend's… Any associated tag unlocks.
- **Your own sounds and image**: choose them in ⚙ → *Sons et image* (a sound on scan, a sound and an image on the block screen). No media is shipped with the app.

The UI is in French.

## Privacy

The app has no Internet permission: nothing leaves the phone. The accessibility service only listens to window changes to know which app is in front, and never reads screen content (`canRetrieveWindowContent="false"`).

## Requirements

- Android 10 (API 29) or later, with NFC
- An NFC tag that can hold NDEF data (NTAG213/215/216 work)

## Build

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # scan and lock rules
```

JDK 17 to 21. Gradle 8.14 does not run on newer JDKs, so use Android Studio's bundled JBR if needed: `JAVA_HOME=/path/to/android-studio/jbr`.

## Setup on the phone

1. Install the APK, then enable the accessibility service: *Accessibility → Installed apps → Tap to Focus*. If the switch is greyed out: *App info → ⋮ → Allow restricted settings*.
2. Associate a tag: the app writes a small NDEF record to it, so scanning it later opens the app.
3. Choose the apps to block and the unlock duration.

## Licence

Code: [CeCILL-B](LICENSE), © 2026 Neveu Grégor.

Third-party assets:

- IBM Plex Sans and JetBrains Mono fonts: SIL Open Font License 1.1 (see [`licences-polices/`](licences-polices/))
- Icons: [Lucide](https://lucide.dev), ISC licence

## Contact

contact.neveugregor@proton.me
