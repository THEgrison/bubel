# Plan de migration — plateforme d’apprentissage Russki

**Statut :** proposition — **en attente de validation** avant toute migration structurelle.  
**Base :** branche `cursor/russki-pre-release-audit-ad1c` / `main` GitHub `THEgrison/bubel`  
**Date :** 18 novembre 2026 (Europe/Paris)

---

## 1. Audit de l’existant (résumé)

### Architecture actuelle

```
Compose (feature/*) → ViewModels → ServiceLocator / AppContainer
                                 → domain (QuizEngine, SrsScheduler, modèles)
                                 → data (Room v2, seed, packs, online optionnel)
```

- **73 fichiers Kotlin** sous `fr.russki.app`
- DI manuelle (`ServiceLocator`) — pas de Hilt
- Navigation : Accueil / Apprendre / Réviser / Profil (+ routes leçon)
- Offline-first documenté (`docs/OFFLINE.md`, `docs/AUDIT_REPORT.md`)

### Ce qui fonctionne réellement (pas une maquette)

| Domaine | Preuve |
| --- | --- |
| Alphabet 33 lettres + exercices | Seed + UI + tests |
| Vocabulaire salutations | Seed + UI |
| Quiz 7 types + score + reprise | `QuizEngine` + Room + tests |
| Flashcards + SRS | `SrsScheduler` + Room + tests |
| Room v2 + migration 1→2 | Schéma exporté + tests |
| Packs locaux / download stack | Code + tests MockWebServer |
| Online désactivé sans URL | `DisabledOnlineApi` |

### Lacunes vs la plateforme visée

| Besoin | État |
| --- | --- |
| Nav Apprendre / Réviser / **Parler** / Profil | Accueil présent, **Parler absent** |
| Parcours CECRL A1→B2, unités thématiques | Niveaux Beginner… seulement ; 2 modules UI |
| Moteur de leçon (intro → exercices → résumé → save) | Absent ; `LessonProgressRepository` **non branché à l’UI** |
| Correction souple (variantes russes) | Comparaison souvent stricte |
| DataStore | Absent (prefs mémoire) |
| Media3 | Absent (`MediaPlayer` basique, non branché UI) |
| Enregistrement oral / ASR | Absent |
| Audio pédagogique embarqué | **Aucun fichier** — honnêteté UI déjà en place |

### Dépendances

**Présentes :** Compose M3, Navigation, Room, WorkManager, OkHttp/Retrofit, Coroutines.  
**Absentes (à ajouter seulement quand une tranche en a besoin) :** DataStore, Media3, Kotlin Serialization.

---

## 2. Principes de migration

1. **Réutiliser** `QuizEngine`, `SrsScheduler`, Room v2, seed, packs, online stubs.
2. **Étendre** le schéma Room (migrations explicites), jamais de wipe destructif.
3. **Ne pas** réécrire l’UI Compose « pour le plaisir ».
4. **Ne jamais** afficher audio / oral / sync comme prêts s’ils ne le sont pas.
5. Une **tranche** = compile + tests + résumé vérifiable avant la suivante.

---

## 3. Architecture cible (incrémentale)

### Navigation (jalon 1)

| Avant | Après |
| --- | --- |
| Accueil / Apprendre / Réviser / Profil | **Apprendre** / **Réviser** / **Parler** (placeholder honnête) / **Profil** |

- Start destination : **Apprendre** (parcours A1).
- Accueil : retiré de la barre ; raccourcis « continuer » intégrés dans Apprendre.
- Parler (jalon 1) : écran « Bientôt — enregistrements natifs et micro non encore disponibles » — **pas** de fausse démo vocale.

### Domaine pédagogique nouveau

```
domain/curriculum/
  CefrLevel (A1…B2)
  Unit, Chapter, LessonBlueprint
domain/lesson/
  LessonSession / LessonEngine   // orchestre les étapes
domain/exercise/
  ExerciseSpec, ExerciseType
  AnswerMatcher                  // variantes acceptées
  ExerciseGrader
```

Le `LessonEngine` **compose** les briques existantes (QCM alphabet, quiz types, SRS) au lieu de les remplacer.

### Contenu A1 (jalon 1 — embarqué)

Unité 1 « Premiers pas » (A1) :

1. Alphabet cyrillique (réutilise le module actuel)
2. Salutations et formules (réutilise vocab greetings)
3. Se présenter (nouveau contenu minimal + exercices)

Chaque leçon seedée : objectif, intro, étapes exercices (≥3 types), résumé, `user_lesson_progress`.

### Exercices jalon 1 (minimum 3, moteur)

- QCM / reconnaissance de lettre (existant)
- Association / traduction FR↔RU (existant via quiz)
- Remise en ordre **ou** texte à trous (existant)

Correction **explicative** + `AnswerMatcher` (casse, ё/е optionnel, espaces) — tests unitaires obligatoires.

### Hors jalon 1 (plus tard)

| Étape | Contenu |
| --- | --- |
| Jalon 2 | DataStore prefs ; brancher Media3 + lecture UI si fichiers licenciés |
| Jalon 3 | Unités A1 supplémentaires (nombres, famille, nourriture…) |
| Jalon 4 | Parler : RECORD_AUDIO, enregistrement, ASR optionnelle (prudente) |
| Jalon 5 | A2+ ; sync distante réelle |

---

## 4. Plan fichier par fichier — jalon 1

### À ajouter

- `domain/curriculum/*`, `domain/lesson/*`, `domain/exercise/AnswerMatcher.kt`
- `data/content/seed/A1Unit1Seed.kt` (parcours)
- `data/lesson/LessonRepository.kt` (Room + seed)
- `feature/learn/path/*` (écran parcours A1)
- `feature/lesson/*` (lecteur de leçon interactif)
- `feature/speak/SpeakScreen.kt` (placeholder honnête)
- Tests : `AnswerMatcherTest`, `LessonEngineTest`

### À modifier

- `RusskiDestination` / `RusskiNavHost` (4 onglets cibles)
- `ContentSeedInitializer` (unités / leçons A1 enrichies)
- `LessonProgressRepository` — **brancher** depuis le lecteur de leçon
- `LearnScreen` → devient hub parcours (plus liste temporaire seule)
- Migration Room **2→3** si tables curriculum nécessaires (sinon réutiliser `content_lessons` + meta)

### À conserver intact

- `QuizEngine`, `SrsScheduler`, flashcards, packs, online stack, thème M3

### Risques

| Risque | Mitigation |
| --- | --- |
| Routes hardcodées | Mettre à jour NavHost + tests smoke |
| Double source seed vs Room | LessonRepository lit Room après seed ; UI n’utilise plus seulement OfflineContentRepository pour le parcours |
| Régression quiz/SRS | Garder suites unitaires existantes au vert |

---

## 5. Critères d’acceptation jalon 1 (rappel)

Application compilable avec :

1. Navigation opérationnelle (Apprendre / Réviser / Parler / Profil)
2. Écran Apprendre présentant le parcours A1
3. Un chapitre A1 avec **plusieurs** leçons
4. Moteur de leçon interactif
5. ≥ 3 types d’exercices fonctionnels
6. Correction visible et explicative
7. Sauvegarde locale de la progression
8. Tests des règles de correction  
→ **uniquement après** `assembleDebug` + tests réellement exécutés.

---

## 6. Décisions à valider avant code

Merci de confirmer (oui / ajustements) :

1. **Retirer Accueil** de la barre au profit de **Parler** (placeholder) ?
2. **Jalon 1** limité à Unité 1 A1 (alphabet + salutations + se présenter) — OK ?
3. **Pas** d’ajout Media3 / DataStore / micro au jalon 1 (reportés) — OK ?
4. Nom du package / marque inchangé **Russki** — OK ?
5. Pousser le travail sur une nouvelle branche `cursor/russki-a1-platform-ad1c` puis GitHub `bubel` — OK ?

Dès validation, implémentation de la **tranche jalon 1** uniquement, avec compile + tests à chaque étape.
