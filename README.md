# Russki

Application Android native (Kotlin + Jetpack Compose) pour apprendre le russe, entièrement en français — **offline-first**, avec services en ligne **facultatifs**.

## État actuel

- Navigation : **Apprendre / Réviser / Parler / Profil**
- Parcours **débutant A1** enrichi (9 unités / 19 leçons en Room v3) — couverture progressive, non certifiante
- **Parler** : 3 dialogues A1 scriptés (texte + activités), audio optionnel non inventé
- Contenu v1 + Room : alphabet, quiz, flashcards/SRS, packs
- Plan plateforme : [docs/MIGRATION_PLAN_PLATEFORME.md](docs/MIGRATION_PLAN_PLATEFORME.md)
- Mode avion : [docs/OFFLINE.md](docs/OFFLINE.md) · Online : [docs/ONLINE_ARCHITECTURE.md](docs/ONLINE_ARCHITECTURE.md)
- Audit pré-v1 : [docs/AUDIT_REPORT.md](docs/AUDIT_REPORT.md)

Sans `RUSSKI_API_BASE_URL`, le client utilise `DisabledOnlineApi` : **aucun backend fictif n’est présenté comme opérationnel**.

Rapport d’audit pré-v1 (tests exécutés, APK, limites) : [docs/AUDIT_REPORT.md](docs/AUDIT_REPORT.md).

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
