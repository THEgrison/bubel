# Audit lecture seule — plateforme pédagogique Russki (sans IA)

**Date :** 18 novembre 2026 (Europe/Paris)  
**Branche auditée :** `cursor/russki-a1-platform-ad1c`  
**Contrainte produit confirmée :** **aucune IA** (pas de LLM, pas d’API d’IA, pas d’Ollama, pas de dialogue généré, pas de service à quota).

Ce document est un **audit en lecture seule** suivi d’un **plan par étapes**. Aucune modification produit n’est engagée tant que ce plan n’est pas validé pour la prochaine tranche.

---

## 1. Architecture

```
Compose (feature/*)
  → ViewModels
  → ServiceLocator / AppContainer (DI manuelle, pas de Hilt)
  → domain/  (purs : QuizEngine, LessonEngine, AnswerMatcher, SrsScheduler, curriculum)
  → data/    (Room v2, seed, packs, online optionnel, audio local)
```

| Couche | État | Commentaire |
| --- | --- | --- |
| UI Compose M3 | Opérationnel | Thème + composants `core/ui` |
| Navigation Compose | Opérationnel | 4 onglets + routes leçon / legacy |
| Domain | Solide | Moteurs testables, sans Android |
| Data Room v2 | Solide | Migrations 1→2, schéma exporté |
| Online | Optionnel | `DisabledOnlineApi` si pas d’URL HTTPS |
| IA | **Absente** | Scan deps + sources : aucun Ollama / OpenAI / LLM |

**~95 fichiers Kotlin** sous `fr.russki.app`. Réécriture totale **non justifiée**.

---

## 2. Navigation et écrans

### Barre principale

| Route | Écran | Rôle réel |
| --- | --- | --- |
| `learn` (start) | `LearnScreen` | Parcours A1 « Premiers pas » + progression |
| `review` | `ReviewScreen` | Entrées quiz + flashcards/SRS |
| `speak` | `SpeakScreen` | **Placeholder honnête** (pas de micro factice) |
| `profile` | `ProfileScreen` | Préférences, objectif UI, packs, sync opt-in |

### Routes secondaires

Alphabet, lettre, exercices alphabet, salutations vocab, quiz, flashcards, packs, leçon interactive `lesson/run/{id}`.

`HomeScreen` existe encore dans le code mais **n’est plus** dans la barre (retiré au jalon A1).

---

## 3. Dépendances

| Présent | Absent (volontaire / reporté) |
| --- | --- |
| Compose BOM, Navigation, Room 2.8.5, WorkManager, OkHttp, Retrofit, Coroutines | DataStore |
| Material icons extended | Media3 |
| Robolectric / MockWebServer (tests) | Toute lib IA / speech cloud |
| `LocalAudioPlayer` (MediaPlayer) | RECORD_AUDIO / ASR |

`compileSdk` / `targetSdk` 37, `minSdk` 26, `versionName` 0.1.0.

---

## 4. Stockage et contenu

### Trois espaces (déjà documentés dans `OFFLINE.md`)

1. **Contenu livré** — seed Room (`SeedContentPack` : alphabet 33, salutations, phrases, leçons/exercices catalogue).
2. **Packs optionnels** — `installed_packs` + assets `packs/audio-demo-v1` (placeholder, **aucun fichier son**).
3. **Progression utilisateur** — quiz, SRS, historique, `user_lesson_progress` — jamais écrasée par le seed.

### Double source pédagogique (point d’attention)

| Source | Contenu | Persistance |
| --- | --- | --- |
| `ContentSeedInitializer` → Room | Alphabet, vocab, phrases, leçons seed | Room v2 |
| `A1Unit1Seed` → `CurriculumRepository` | Unité A1 + 9 exercices interactifs | **En mémoire Kotlin** ; progression seule en Room |

Le parcours A1 n’est **pas encore** unifié dans les tables `content_*`. Risque de divergence si on enrichit les deux sans migration claire.

### Préférences

`TemporaryPreferencesStore` : **mémoire process** (thème, priorité offline). Objectif quotidien Profil : UI seule, **non persisté**, non mesuré contre l’activité réelle.

---

## 5. Moteurs pédagogiques

| Moteur | Rôle | Couverture |
| --- | --- | --- |
| `QuizEngine` + `QuestionGenerator` | Quiz 7 types, score, reprise | Tests unitaires |
| `LessonEngine` + `ExerciseGrader` | Intro → exercices → résumé | Tests + UI leçon |
| `AnswerMatcher` | Casse, espaces, ё/е | Tests |
| `SrsScheduler` | Répétition espacée flashcards | Tests + Room |

Types interactifs jalon A1 : MultipleChoice, MatchTranslation, FillBlank, WordOrder, LetterRecognition.

**Absent :** dialogues scriptés à branches, test de placement, stats d’activité, objectifs quotidiens mesurés.

---

## 6. Audio

| Élément | État |
| --- | --- |
| `LocalAudioPlayer` | Prêt (asset / fichier, pause cycle de vie) |
| `AssetAudioAvailability` | Ne signale lecture que si fichier présent |
| Fichiers `.ogg` / `.mp3` pédagogiques | **0** dans le dépôt |
| Pack démo | Manifeste + `readme.txt` placeholder |
| UI leçon / Parler | Pas de bouton play factice |

Intégration locuteurs natifs = **bloquée par licence + fichiers**, pas par architecture lecteur.

---

## 7. Progression et révision

| Fonction | État |
| --- | --- |
| Progression leçon A1 | Room via `LessonProgressRepository.completeLesson` |
| Quiz sessions / réponses | Room |
| Flashcards + SRS + historique | Room + `SrsScheduler` |
| Sync distante | Opt-in + refuse sans serveur / sans jeton |
| Streak / minutes / stats | **Absents** |
| Test de niveau | **Absent** |

---

## 8. Mode avion / online

- Offline-first documenté : leçons seed, quiz, SRS, progression **sans** réseau.
- Sans `RUSSKI_API_BASE_URL` HTTPS → `DisabledOnlineApi` (pas de faux backend).
- WorkManager / téléchargements packs distants uniquement si API configurée.

---

## 9. Tests (état code)

Suites unitaires présentes notamment : alphabet, offline persistence, online/path safety, audio manquant, quiz, SRS, AnswerMatcher, LessonEngine, smoke MainActivity (Robolectric).

Instrumentés : squelette `MainActivityInstrumentedTest` — **non jouable** ici sans `adb`/émulateur.

Dernière build connue sur cette branche : `assembleDebug` + `testDebugUnitTest` verts (jalon A1).

---

## 10. Écart vs plateforme type Busuu (principes, pas copie UI)

| Besoin | État actuel |
| --- | --- |
| Parcours niveaux / unités / leçons | A1 unité 1 seulement ; A2–B2 = enums sans contenu |
| Moteur d’exercices réutilisable | Oui (Quiz + Lesson + Matcher) — à étendre, pas remplacer |
| Dialogues prédéfinis + branches + explications | **Non** |
| Audio natifs licenciés | **Non** (infra lecteur OK) |
| Progression locale + SRS | **Oui** (SRS flashcards ; leçons A1 branchées) |
| Stats, objectifs quotidiens mesurés, test de niveau | Objectif UI cosmétique ; reste **non** |
| Hors ligne pour contenu installé | **Oui** pour ce qui est embarqué |
| Zéro IA | **Oui** — à verrouiller dans chaque tranche |

---

## 11. Plan de modification par étapes

Principes transverses :

1. **Aucune IA** à chaque PR (checklist + scan deps).
2. **Réutiliser** QuizEngine / LessonEngine / SRS / Room / packs.
3. Tranche = petit lot + `assembleDebug` + tests pertinents + bilan « ça marche / reste / erreurs ».
4. Contenu pédagogique **embarqué ou pack local** ; online reste optionnel.
5. Ne pas afficher audio / oral / sync comme prêts s’ils ne le sont pas.

### Étape 0 — Gel audit

Livré. Validation reçue → étape 1.

### Étape 1 — Unifier le curriculum en Room — **livré**

- Tables `curriculum_units` / `curriculum_lessons` / `curriculum_exercises` + migration Room **2→3**.
- Seed A1 écrit dans Room (INSERT IGNORE) ; meta `curriculum_version`.
- `CurriculumRepository` lit Room ; progression `user_lesson_progress` inchangée.
- Tests : `CurriculumRoomTest` + migration 1→3 mise à jour.

### Étape 2 — Étendre le moteur d’exercices

- Type `DialogueTurn` / `DialogueChoice` (données **fixes**, pas générées).
- Intégrer dans `LessonEngine` (phase dialogue optionnelle) ou sous-moteur dédié.
- Explications pédagogiques FR sur chaque branche.
- Tests : graphe de dialogue, branche incorrecte → feedback, fin de dialogue.

### Étape 3 — Contenu A1 enrichi (hors IA)

- Unité 2 A1 (ex. nombres / famille) en seed local.
- Au moins un dialogue scripté dans une leçon.
- Nav Apprendre : liste multi-unités.

### Étape 4 — Audio natifs **si et seulement si** fichiers + licences

- Pack local avec OGG + manifeste licence.
- Brancher lecture UI uniquement quand `AssetAudioAvailability` = true.
- Sinon : message existant « Pas d’enregistrement natif » — **pas de Media3 obligatoire** tant qu’un MediaPlayer suffit.
- Tests : play avec fichier fixture ; absence → pas de crash.

### Étape 5 — Stats locales + objectif quotidien réel

- DataStore (ou table Room prefs) pour thème / objectif / offline-only.
- Compteur minutes / leçons du jour (local, hors ligne).
- Profil : stats réelles (leçons terminées, cartes dues, streak simple).
- Tests : persistance après redémarrage process (Robolectric).

### Étape 6 — Test de niveau (placement) sans IA

- Questionnaire **fixe** A1→… (seuils déterministes).
- Résultat → recommandation d’unité (pas de score « magique »).
- Stockage résultat local.
- Tests : barème et orientation.

### Étape 7 — Parler (sans ASR cloud)

- Dialogues à voix haute **guidés** (répéter après natif) quand audio présent.
- Micro / enregistrement local optionnel **plus tard**, comparaison pédagogique simple (pas d’API cloud).
- Tant que non prêt : garder le placeholder honnête.

### Étape 8 — Qualification offline / nav

- Suite mode avion étendue (curriculum Room + dialogues + audio pack).
- Smoke navigation (Robolectric / instrumenté si device).
- Mise à jour `OFFLINE.md` + README.

---

## 12. Hors scope explicite

- Toute IA / LLM / Ollama / conversation générée.
- Copie UI ou contenu propriétaire Busuu.
- Backend payant requis pour le cœur pédagogique.
- Réécriture Compose « pour le plaisir ».

---

## 13. Décision demandée

Confirmer pour enchaîner **uniquement l’étape 1** (curriculum Room unifié + migration), ou ajuster l’ordre (ex. dialogues avant migration Room).
