# Prononciation pédagogique — Russki

Le **cyrillique** est toujours la forme de référence à lire et à mémoriser.

## Trois notions distinctes

| Terme | Rôle | Ce que ce n’est pas |
|-------|------|---------------------|
| **Cyrillique** | Forme écrite russe | — |
| **Aide de lecture / translittération** | Repère pour francophones (`RussianPronunciation`) | Pas de l’IPA, pas une « vraie » transcription phonétique |
| **Audio natif** | Fichier embarqué vérifié | Jamais inventé ni simulé |

L’UI affiche explicitement que la translittération est une **aide de lecture**, pas une transcription phonétique exacte.

## Accent tonique

- Marqué par un **accent aigu** sur la voyelle accentuée dans la table connue (ex. `privét`, `máma`).
- Si l’accent n’est **pas** vérifié dans la table → translittération sans marque + `stressUnknown = true`.
- `ё` / `Ё` → `yo` / `yó` : en russe standard, **toujours sous l’accent**.

## Réductions vocaliques (aperçu, pas dès la 1ʳᵉ leçon)

Hors accent, **о** et **а** se réduisent souvent (ex. approximatif « a » faible) — on le signale dans les fiches avancées / notes de lettres, sans surcharger les premiers groupes du parcours cyrillique.

## Sons difficiles pour francophones

- **ы** : pas d’équivalent stable en français.
- **ж / ш / щ / ч / ц** : approximations prudentes seulement.
- **ь / ъ** : pas des voyelles ; rôles orthographiques / de palatalisation.
- **Lookalikes** : В≠B, Н≠H, Р≠P, С≠C, У≠Y, Х≠X.

## Table courte (extraits)

| Russe | Aide de lecture |
|-------|-----------------|
| привет | privét |
| мама | máma |
| это | éto |
| сын | syn |
| ёлка | yólka |

Code : `domain/pronunciation/RussianPronunciation.kt` · parcours lettres : `CyrillicPathSeed`.
