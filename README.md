# Russki

Application Android native (Kotlin + Jetpack Compose) pour apprendre le russe, entièrement en français — **offline-first**.

## État actuel

- Thème Material 3 clair / sombre
- Navigation : Accueil, Apprendre, Réviser, Profil
- **Contenu pédagogique v1.0.0** embarqué : alphabet (33 lettres), exercices, vocabulaire salutations, phrases
- **Room v2** : lettres, mots, phrases, leçons/exercices, métadonnées audio, packs, quiz, flashcards/SRS, historique, dates de révision, progression leçons
- Migrations explicites (pas de wipe destructif) ; seed idempotent
- **Quiz** + **Flashcards / SRS** 100 % locaux
- **Packs hors ligne** : Profil → Gérer les packs (taille, disponibilité, installation / suppression sans toucher la progression)

Voir [docs/OFFLINE.md](docs/OFFLINE.md) pour le détail du mode avion.

## Prérequis

- JDK 17+ (JDK 21 testé)
- Android SDK (`compileSdk` / `targetSdk` 37, build-tools 36+)
- `ANDROID_HOME` ou `local.properties` avec `sdk.dir=…`

## Compilation

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK : `app/build/outputs/apk/debug/app-debug.apk`

Stack : AGP 9.5.1, Kotlin 2.2.21, Compose BOM 2026.09.00, Room 2.8.5, Gradle Wrapper 9.6.0.

## Architecture

Présentation Compose → ViewModels / StateFlow → domain → data (Room, seed embarqué, packs locaux).  
Réseau / Retrofit / WorkManager : réservés aux packs distants futurs — non requis pour les fonctions essentielles.
