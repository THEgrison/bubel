# Convention de translittération pédagogique — Russki

Le **cyrillique** est toujours la forme de référence.

## Principes

1. Translittération **cohérente** dans toute l’app (`RussianPronunciation`).
2. Accent tonique marqué par un **accent aigu** sur la voyelle (ex. `privét`, `dóbroe útro`).
3. Ce n’est **pas** de l’IPA ni une approximation française trompeuse présentée comme exacte.
4. Si l’accent n’est pas dans la table A1 connue → translittération sans marque + `stressUnknown`.
5. `ё` → `yo` / `yó` (toujours accentué en russe standard).

## Table courte (extraits)

| Russe | Translittération |
|-------|------------------|
| привет | privét |
| здравствуйте | zdrávstvuyte |
| спасибо | spasíbo |
| пожалуйста | pozhalúysta |
| до свидания | do svidániya |

Voir le code : `domain/pronunciation/RussianPronunciation.kt`.
