# Audio pédagogique local — Russki

## État actuel

Des enregistrements russes **licenciés** sont embarqués pour une partie des
salutations et formules A1. L’UI n’affiche « Écouter » que si
`AssetAudioAvailability` trouve le fichier réel. Sinon : « Audio indisponible »
et mode texte.

Convention : `assets/content/audio/ru/<slug>.ogg` (Ogg Vorbis).

## Architecture

| Composant | Rôle |
|-----------|------|
| `PedagogicalAudioCatalog` | Chemins *attendus* (vocabulaire, dialogues, phrases) |
| `AssetAudioAvailability` | Présence réelle (assets ou `filesDir`) |
| `LocalAudioPlayer` | Lecture / pause / reprise / répétition / libération |
| `LocalVoiceRecorder` | Micro optionnel (cache privé, non noté) |

## Inventaire des fichiers embarqués

| Fichier | Texte | Source | Licence | Attribution |
|---------|-------|--------|---------|-------------|
| `privet.ogg` | привет | Shtooka `rus-balm-voc-sakhno` | [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/us/) | Sergueï Sakhno, Nicolas Vion |
| `zdravstvuyte.ogg` | здравствуйте! | Shtooka `rus-balm-voc` | [CC BY 2.0 FR](https://creativecommons.org/licenses/by/2.0/fr/) | Gulnara Streit, Eric Streit, Nicolas Vion |
| `dobroe-utro.ogg` | доброе утро | [Commons:Ru-Доброе утро.ogg](https://commons.wikimedia.org/wiki/File:Ru-%D0%94%D0%BE%D0%B1%D1%80%D0%BE%D0%B5_%D1%83%D1%82%D1%80%D0%BE.ogg) | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | Захаровсергеевич (Wikimedia Commons) |
| `dobryy-vecher.ogg` | добрый вечер | [Commons:Ru-добрый вечер.ogg](https://commons.wikimedia.org/wiki/File:Ru-%D0%B4%D0%BE%D0%B1%D1%80%D1%8B%D0%B9_%D0%B2%D0%B5%D1%87%D0%B5%D1%80.ogg) | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | Захаровсергеевич (Wikimedia Commons) |
| `poka.ogg` | пока | Shtooka `rus-balm-voc-sakhno` | CC BY 3.0 | Sergueï Sakhno, Nicolas Vion |
| `do-svidaniya.ogg` | до свидания | Shtooka `rus-balm-voc-sakhno` | CC BY 3.0 | Sergueï Sakhno, Nicolas Vion |
| `spasibo.ogg` | спасибо | Shtooka `rus-balm-voc-sakhno` | CC BY 3.0 | Sergueï Sakhno, Nicolas Vion |
| `pozhaluysta.ogg` | пожалуйста | Shtooka `rus-balm-voc-sakhno` | CC BY 3.0 | Sergueï Sakhno, Nicolas Vion |
| `khorosho.ogg` | хорошо | Shtooka `rus-balm-voc-sakhno` | CC BY 3.0 | Sergueï Sakhno, Nicolas Vion |

Packs Shtooka d’origine (téléchargement) :

- https://yojik.eu/audiocollections/archives/rus-balm-voc-sakhno_flac.tar.xz
- https://yojik.eu/audiocollections/archives/rus-balm-voc_flac.tar.xz  
  README licences : `detailled/rus-balm-voc(-sakhno)/readme.txt` sur yojik.eu

Les fichiers Commons ont été convertis en Ogg Vorbis (ffmpeg) pour la
convention du projet ; licence **CC BY-SA** → redistribution OK avec
attribution + ShareAlike sur les dérivés.

## Références couvertes

- Vocabulaire salutations : 001, 002, 003, 005, 007, 008, 009, 010, 013
- Dialogues (réutilisation **exacte** uniquement) :
  - `meet-l1` → `zdravstvuyte.ogg`
  - `drink-l6` → `spasibo.ogg`
- Mode écoute / oral : disponibles si la clé catalogue pointe vers un slug présent

## Références encore sans fichier

Slugs catalogue sans `.ogg` embarqué (l’UI reste en « Audio indisponible ») :

- `dobryy-den`, `spokoynoy-nochi`, `izvinite`, `kak-dela`, `menya-zovut`,
  `ochen-priyatno`, `kak-vas-zovut`, `ya-iz-frantsii`
- Répliques de dialogue longues (`dialogue-meet-l2`…, `dialogue-drink-l1`…,
  `dialogue-ask-*`) — aucun enregistrement licencié **de la phrase entière**
  n’a été trouvé/intégré
- Alphabet (33 lettres) : toujours `audioAssetPath = null`

Ne pas inventer d’audio partiel ni concaténer des mots pour « couvrir » une phrase.

## Comment ajouter un fichier licencié

1. Licence claire (CC0, CC BY, CC BY-SA, ou équivalent redistribuable dans l’APK).
2. Convertir en Ogg Vorbis ; placer sous `app/src/main/assets/content/audio/ru/<slug>.ogg`.
3. Mettre à jour **cette** table (source, licence, attribution).
4. Brancher `audioAssetPath` / catalogue si besoin.
5. Vérifier que « Écouter » apparaît uniquement pour ce fichier.

## Enregistrements utilisateur

- Permission `RECORD_AUDIO` à la demande ; cache `oral_recordings/` ; jamais sync/noté.

## Tests

- Unitaires : présence des assets déclarés disponibles, absence des manquants,
  non-régression progression.
- Appareil réel : qualité sonore, MediaPlayer, pause/reprise, pas de lectures
  concurrentes, micro.
