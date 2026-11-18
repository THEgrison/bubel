# Répétition espacée locale (Russki)

Algorithme **déterministe et simple**, inspiré d’approches type Anki.
Ce n’est **pas** un modèle scientifiquement optimal.

## Déblocage

Seules les notions liées à une leçon ou un dialogue **terminé**
(`completedAtEpochMs` non nul) entrent dans la file (`ReviewableCatalog`).

Identifiants stables : `rv-vocab-*`, `rv-letter-*`, `rv-ex-*`, `rv-dlg-*`.

## Planification (`SrsScheduler`)

| Situation | Effet |
|-----------|--------|
| Nouvelle carte | Due immédiatement |
| Erreur (Again) en apprentissage | Relearning, ~10 min, lapses++ |
| Erreur en revue | Relearning, ~10 min, ease −0.2 (min 1.3) |
| Hard | Intervalle × 1.2 (revue) ou ~30 min (apprentissage) |
| Good (apprentissage → revue) | Intervalle 1 jour |
| Good (revue) | Intervalle × ease (défaut 2.5) |
| Easy | Intervalle plus long, ease +0.15 (max 3.0) |

Horloge injectable (epoch ms). Changement de jour / fuseau : les échéances
restent en epoch absolu ; « due » = `nextReviewEpochMs <= now`.

## Persistance

Table Room existante `srs_cards` + `flashcard_review_history` (v3).
**Aucune migration** ajoutée. Aucune écriture dans `user_lesson_progress`.
