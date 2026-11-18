# Russki — configuration et déploiement en ligne

## Variables de configuration (client)

| Variable | Où | Description |
| --- | --- | --- |
| `RUSSKI_API_BASE_URL` | Gradle `-P` / `gradle.properties` | URL HTTPS publique de l’API v1. **Vide = online désactivé.** |
| `BuildConfig.RUSSKI_ONLINE_ENABLED` | généré | `true` seulement si l’URL commence par `https://` |

Exemple :

```bash
./gradlew :app:assembleDebug -PRUSSKI_API_BASE_URL=https://api.exemple.fr
```

**Interdit** : clés secrètes serveur, tokens de signature CDN, mots de passe — dans l’APK, `BuildConfig`, assets ou git.

Jetons de sync : obtenus à l’exécution après opt-in + flux d’auth **à déployer** ; stockés hors code source.

## Ce que fournit ce dépôt (client)

- Contrats API : [API_v1.md](API_v1.md)
- Architecture : [ONLINE_ARCHITECTURE.md](ONLINE_ARCHITECTURE.md)
- `RusskiOnlineApi` + `HttpRusskiOnlineApi` + `DisabledOnlineApi`
- Téléchargement packs : temp → SHA-256 → install ; WorkManager ; limiteur concurrent
- Sync progression : interface + refus explicite sans opt-in / sans jeton
- Tests : `OnlineApiTest` avec **MockWebServer** (double de test, pas un backend)

## Ce qu’il reste à déployer (serveur)

1. **API HTTPS v1** (`/v1/catalog`, `/v1/packs/.../manifest`, assets, `/v1/sync/progress`)
2. **CDN** pour les binaires audio + URLs signées (TTL court)
3. **Émission de jetons** sync (OAuth2 device / anonymous account) + stockage serveur des snapshots
4. **Manifestes packs** versionnés avec SHA-256 réels des fichiers
5. Observabilité, backups, politique de rétention (région EU recommandée pour la sync)

Tant que ces éléments ne sont pas en production, laissez `RUSSKI_API_BASE_URL` vide : l’app reste offline-first.

## Étapes de mise en service

1. Déployer l’API + CDN ; vérifier TLS (A+).
2. Publier un catalogue + un pack de test avec checksums corrects.
3. Rebuild l’app avec `-PRUSSKI_API_BASE_URL=https://…`.
4. Vérifier Profil → packs : catalogue distant visible ; téléchargement + validation.
5. Activer le flux d’auth sync ; ne proposer l’opt-in UI qu’avec information claire.
6. Tester mode avion : leçons / quiz / flashcards locaux inchangés.

## Compilation

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```
