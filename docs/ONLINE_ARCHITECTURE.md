# Russki — architecture en ligne facultative

Les fonctions essentielles restent **100 % locales**. Le réseau n’est qu’un canal optionnel pour catalogue distant, packs audio et synchronisation de progression **après consentement**.

## Architecture serveur minimale

```
┌─────────────┐     HTTPS      ┌──────────────────┐
│  App Russki │ ─────────────► │ API v1 (edge)    │  JSON catalogue,
│  (Android)  │                │ Workers / Cloud  │  manifests, sync
└──────┬──────┘                │ Run / Fly.io     │
       │                       └────────┬─────────┘
       │ Range GET                      │ signed URLs
       ▼                                ▼
┌─────────────┐                ┌──────────────────┐
│ Fichiers    │ ◄──────────────│ CDN objets       │
│ .partial →  │   audio packs  │ R2 / S3+CF       │
│ install     │                └──────────────────┘
└─────────────┘
```

### Composants

| Composant | Rôle | Pourquoi |
| --- | --- | --- |
| **API HTTPS légère** | Catalogue leçons/packs, manifests versionnés, endpoint sync opt-in | Surface d’attaque petite ; pas de logique pédagogique lourde côté serveur |
| **CDN objets** | Binaires audio (gros volumes) via URLs HTTPS (idéalement signées, TTL court) | Décharge l’API ; téléchargements Range / cache edge |
| **Pas de secret serveur dans l’APK** | L’app ne connaît que l’URL de base publique | Une clé API embarquée serait extractible ; l’auth sync utilisera un jeton **émis après opt-in**, stocké hors code |
| **Client Android** | OkHttp (HTTPS, timeouts), WorkManager (téléchargements différés), validation manifeste + SHA-256 | Reprise, file d’attente, isolation du mode avion |

### Ce qui n’est **pas** déployé dans ce dépôt

- Aucun backend live, aucun faux serveur « de prod » dans l’APK.
- L’interface `RusskiOnlineApi` + `DisabledOnlineApi` (URL vide) et les tests `MockWebServer` couvrent le contrat.
- **Reste à déployer** : API v1, CDN + objets audio, émission de jetons sync, monitoring, politique de rétention.

## Sécurité

1. **HTTPS uniquement** — `networkSecurityConfig` refuse le cleartext ; base URL doit commencer par `https://` (sauf host de test injecté).
2. **Aucun secret serveur dans l’APK** — pas de clé privée, pas de token hardcodé. Variables : `RUSSKI_API_BASE_URL` (publique).
3. **Validation stricte** — JSON parsé et contrôlé (champs requis, types, tailles, SHA-256 hex 64). Réponse invalide → erreur typée, pas d’install.
4. **Packs** — téléchargement vers fichier temporaire → vérif taille + checksum → install atomique ; pack indisponible tant que tout n’est pas validé.
5. **Sync progression** — désactivée par défaut ; nécessite opt-in UI + serveur configuré ; payload minimal (IDs leçons, scores, dates SRS), pas d’identité réelle sans mécanisme d’auth déployé.
6. **Erreurs / timeouts** — connect 10 s, read 30 s, write 30 s ; échec réseau → conserver Room local, message utilisateur clair.
7. **Téléchargements** — WorkManager + limiteur (max 2 simultanés) ; reprise HTTP `Range` ; nettoyage des `.partial` orphelins.

## Hébergement recommandé (cible)

- **API** : Cloudflare Workers ou Cloud Run (région EU si données de progression) — TLS géré, scaling à zéro.
- **Objets** : Cloudflare R2 ou S3 + CloudFront, bucket privé, URLs signées renvoyées par l’API.
- **Secrets** : uniquement côté serveur (signing keys CDN, clés de session). Rotation sans republier l’APK.
- **Observabilité** : logs structurés sans contenu pédagogique utilisateur ; pas de PII dans les métriques.

## Contrat client ↔ serveur

Voir [API_v1.md](API_v1.md). Déploiement : [DEPLOYMENT.md](DEPLOYMENT.md).
