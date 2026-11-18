# Russki

Application Android native (Kotlin + Jetpack Compose) pour apprendre le russe, entièrement en français.

## État actuel

- Thème Material 3 clair / sombre, accent unique
- Navigation inférieure : Accueil, Apprendre, Réviser, Profil
- **Contenu pédagogique v1.0.0** (hors ligne) : alphabet russe (33 lettres), exercices, vocabulaire « salutations »
- **Quiz** : moteur indépendant de l'UI, 7 types de questions, sessions Room (reprise / anti double-comptage)
- **Flashcards + SRS** : notation À revoir / Difficile / Bien / Facile, dates de révision testables
- Données séparées de l'UI : `domain/content` + `data/content/seed` + manifeste `assets/content/v1/`
- Audio : chemins `null` tant qu'aucun enregistrement natif n'est embarqué ; l'UI l'indique clairement

## Prérequis

- JDK 17+ (JDK 21 testé)
- Android SDK (`compileSdk` / `targetSdk` 37, build-tools 36+)
- Variable `ANDROID_HOME` ou `local.properties` avec `sdk.dir=…`

## Compilation

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK : `app/build/outputs/apk/debug/app-debug.apk`

Stack Gradle : AGP 9.5.1, Kotlin intégré AGP, Compose BOM 2026.09.00, Gradle Wrapper 9.6.0.

## Architecture prévue

Présentation Compose → ViewModels / StateFlow → domain → data (Room, DataStore, Media3, WorkManager, Retrofit online optionnel).
