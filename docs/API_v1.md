# API Russki v1 — contrats

Base : `{RUSSKI_API_BASE_URL}` (HTTPS).  
Content-Type : `application/json; charset=utf-8` sauf téléchargement binaire.

Toutes les réponses d’erreur :

```json
{ "error": { "code": "string", "message": "string" } }
```

## `GET /v1/catalog`

Catalogue distant (leçons + packs). Ne remplace pas le contenu embarqué.

**200**

```json
{
  "apiVersion": "1",
  "catalogVersion": "2026.11.18",
  "generatedAtEpochMs": 1731920000000,
  "lessons": [
    {
      "id": "lesson-travel-01",
      "titleFr": "Voyage — bases",
      "descriptionFr": "…",
      "kind": "vocabulary",
      "moduleId": "vocab-travel",
      "sortOrder": 10,
      "minAppVersionCode": 1,
      "packIds": ["audio-travel-v1"]
    }
  ],
  "packs": [
    {
      "id": "audio-travel-v1",
      "titleFr": "Audio voyage",
      "descriptionFr": "…",
      "version": "1.0.0",
      "totalBytes": 5242880,
      "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      "isOptional": true,
      "manifestPath": "/v1/packs/audio-travel-v1/manifest"
    }
  ]
}
```

## `GET /v1/packs/{packId}/manifest`

Manifeste versionné d’un pack audio.

**200**

```json
{
  "packId": "audio-travel-v1",
  "version": "1.0.0",
  "titleFr": "Audio voyage",
  "totalBytes": 5242880,
  "sha256OfManifest": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
  "assets": [
    {
      "id": "audio-travel-001",
      "relativePath": "ogg/travel-001.ogg",
      "byteSize": 120000,
      "sha256": "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
      "downloadPath": "/v1/packs/audio-travel-v1/assets/audio-travel-001"
    }
  ]
}
```

- `id` d’asset : **stable** entre versions tant que le contenu audio est le même.
- Le client vérifie chaque `byteSize` + `sha256` après téléchargement.

## `GET /v1/packs/{packId}/assets/{assetId}`

Corps binaire (audio) ou `302` vers CDN signé.

En-têtes supportés :

- Requête : `Range: bytes=start-`
- Réponse : `200` / `206`, `Content-Length`, `Accept-Ranges: bytes`, `ETag` (optionnel)

## `POST /v1/sync/progress` (opt-in)

**Headers** : `Authorization: Bearer <jeton>` (jeton **jamais** dans l’APK ; obtenu après flux d’auth non encore déployé).

**Body** (minimal, pas de nom/e-mail) :

```json
{
  "schemaVersion": 1,
  "deviceInstallId": "uuid-local-anonyme",
  "exportedAtEpochMs": 1731920000000,
  "lessonProgress": [
    { "lessonId": "lesson-alphabet", "completedExerciseCount": 3, "completedAtEpochMs": null }
  ],
  "srsCards": [
    {
      "cardId": "fc-vocab-greet-001",
      "nextReviewEpochMs": 1732000000000,
      "intervalDays": 3.0,
      "easeFactor": 2.5,
      "repetitionCount": 2,
      "lapseCount": 0,
      "learningState": "Review"
    }
  ]
}
```

**200** : `{ "accepted": true, "serverRevision": "…" }`  
Sans jeton / sans déploiement : le client doit exposer `SyncUnavailable` et **ne pas** inventer un succès.

## `GET /v1/sync/progress`

Même auth. Retourne le dernier snapshot accepté ou `404`.

## Codes d’erreur client (app)

| Code app | Signification |
| --- | --- |
| `ONLINE_DISABLED` | `RUSSKI_API_BASE_URL` vide |
| `NETWORK` | timeout, DNS, TLS, HTTP 5xx |
| `INVALID_PAYLOAD` | JSON / champs / checksum invalides |
| `NOT_FOUND` | 404 |
| `SYNC_OPTED_OUT` | utilisateur n’a pas consenti |
| `SYNC_UNAUTHORIZED` | pas de jeton / 401 |
