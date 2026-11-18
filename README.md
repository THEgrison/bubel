# Russki

Application Android native (Kotlin + Jetpack Compose) pour apprendre le russe, entièrement en français — **offline-first**, avec services en ligne **facultatifs**.

## État actuel

- Contenu pédagogique v1.0.0 embarqué + Room v2 (leçons, quiz, flashcards/SRS, packs)
- Mode avion : leçons, quiz, flashcards, progression — documenté dans [docs/OFFLINE.md](docs/OFFLINE.md)
- En ligne optionnel : catalogue distant, packs audio (checksum, reprise, WorkManager), sync progression opt-in
- Architecture / sécurité : [docs/ONLINE_ARCHITECTURE.md](docs/ONLINE_ARCHITECTURE.md)
- Contrats API : [docs/API_v1.md](docs/API_v1.md)
- Déploiement : [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)

Sans `RUSSKI_API_BASE_URL`, le client utilise `DisabledOnlineApi` : **aucun backend fictif n’est présenté comme opérationnel**.

## Prérequis

- JDK 17+
- Android SDK (`compileSdk` / `targetSdk` 37)
- `ANDROID_HOME` ou `local.properties`

## Compilation

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest

# Avec API distante (HTTPS uniquement) :
./gradlew :app:assembleDebug -PRUSSKI_API_BASE_URL=https://api.exemple.fr
```

Stack : AGP 9.5.1, Kotlin 2.2.21, Compose BOM 2026.09.00, Room 2.8.5, OkHttp 4.12, WorkManager 2.10.5.
