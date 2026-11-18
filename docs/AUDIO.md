# Audio pédagogique local — Russki

## État actuel

**Aucun fichier audio redistribuable n’est embarqué dans ce dépôt.**

L’application expose l’infrastructure de lecture et d’entraînement oral, mais
refuse d’afficher une lecture tant que le fichier n’existe pas réellement
(`AssetAudioAvailability` / `AudioResolver`). L’UI affiche alors
« Audio indisponible » et bascule en mode texte.

Le pack `assets/packs/audio-demo-v1/` ne contient que des manifests / readme
placeholder — **pas** d’enregistrements.

## Architecture

| Composant | Rôle |
|-----------|------|
| `PedagogicalAudioCatalog` | Chemins *attendus* pour salutations, présentations, 3 dialogues A1 |
| `AssetAudioAvailability` | Vérifie la présence réelle (assets ou `filesDir`) |
| `LocalAudioPlayer` | Lecture / pause / reprise / répétition / libération |
| `LocalVoiceRecorder` | Enregistrement micro optionnel dans le **cache privé** |
| Mode écoute | Exercices QCM ; audio si présent, sinon stimulus texte |
| Entraînement oral | Répétition + réécoute locale ; **aucune** notation / ASR / envoi |

Convention de chemins : `assets/content/audio/ru/<slug>.ogg`

## Comment ajouter un fichier licencié

1. Vérifier la licence (CC0, CC BY, ou licence propriétaire autorisant la
   redistribution dans l’APK). Conserver la preuve (URL, auteur, date).
2. Convertir en Ogg Vorbis (recommandé) ou un format lu par `MediaPlayer`.
3. Placer le fichier sous `app/src/main/assets/content/audio/ru/` avec le
   **slug exact** du catalogue (voir table ci-dessous).
4. Mettre à jour la section **Inventaire** de ce document (source, licence,
   attribution).
5. Lancer l’app : le bouton « Écouter » n’apparaît que si le fichier est détecté.
6. Ne jamais committer un audio dont la licence n’est pas claire.

## Inventaire des fichiers embarqués

| Fichier | Contenu | Source | Licence | Attribution |
|---------|---------|--------|---------|-------------|
| — | — | — | — | **Aucun fichier audio n’est intégré à ce jour.** |

## Chemins attendus (catalogue)

Exemples (liste complète via `PedagogicalAudioCatalog.allExpectedPaths()`) :

- `content/audio/ru/privet.ogg` ← `vocab-greet-001`
- `content/audio/ru/zdravstvuyte.ogg` ← `vocab-greet-002`
- `content/audio/ru/dialogue-meet-l1.ogg` ← réplique `meet-l1`
- `content/audio/ru/menya-zovut.ogg` ← présentation

## Enregistrements utilisateur

- Permission `RECORD_AUDIO` demandée **au moment** d’enregistrer.
- Fichiers dans `cacheDir/oral_recordings/` uniquement.
- Supprimés en quittant l’écran, via « Effacer le cache oral », ou
  `LocalVoiceRecorder.clearSession()`.
- Jamais synchronisés, jamais notés automatiquement.

## Tests appareil réel

Les tests unitaires Robolectric couvrent absence de fichier, libération du
lecteur, erreurs d’enregistrement simulées et nettoyage du cache.

La permission micro réelle, le comportement MediaRecorder hardware et la
qualité audio doivent être validés sur **appareil Android physique** (ou
émulateur avec micro virtuel).
