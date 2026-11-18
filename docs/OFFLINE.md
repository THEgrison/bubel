# Russki — fonctionnement hors ligne

Russki est **offline-first** : les fonctions essentielles ne dépendent d'aucune réponse réseau.

## Ce qui fonctionne en mode avion

| Fonction | Source locale |
| --- | --- |
| Consulter les leçons installées | Catalogue Room + seed embarqué `SeedContentPack` |
| Écouter les audios installés | Fichiers assets / `filesDir/packs/…` ; l'UI n'expose la lecture que si le fichier existe |
| Quiz | `QuizRepository` + tables `quiz_sessions` / `quiz_answers` |
| Flashcards + SRS | `FlashcardRepository` + `srs_cards` + `flashcard_review_history` |
| Progression & dates de révision | `user_lesson_progress` (leçons `a1-lesson-*` et dialogues `dialogue-*` séparés par préfixe), `nextReviewEpochMs` |

Aucune de ces actions n'appelle Retrofit, WorkManager ni une API distante.

## Trois espaces de données

1. **Contenu pédagogique livré** (`source = bundled`)  
   Lettres, mots, phrases, leçons, exercices, métadonnées audio, catalogue de packs.  
   Initialisé par `ContentSeedInitializer` (INSERT IGNORE, idempotent).

2. **Contenu téléchargé / packs optionnels** (`source = downloaded`, table `installed_packs`)  
   Pack démo `audio-demo-v1` installable depuis `assets/packs/` sans réseau.  
   Suppression d'un pack optionnel = fichiers + état d'installation uniquement.

3. **Progression utilisateur**  
   Quiz, cartes SRS, historique de révision, progression de leçons.  
   **Jamais** écrasée par le seed ni par la suppression d'un pack optionnel.

## Migrations

- Room **v1** : quiz + SRS  
- Room **v2** : catalogue pédagogique + packs + historique + progression leçons  
- Room **v3** : curriculum CECRL (`curriculum_units` / `curriculum_lessons` / `curriculum_exercises`)  
- `RusskiMigrations` 1→2 et 2→3 — pas de `fallbackToDestructiveMigration` en production  
- Schémas exportés sous `app/schemas/`

## Packs (Profil → Gérer les packs hors ligne)

Pour chaque pack : taille, disponibilité hors ligne, état (non installé / installation / installé / échec), version.  
Le pack de base `core-v1` est toujours installé. Un pack optionnel peut être installé ou supprimé sans toucher à la progression.

## Scénarios couverts par les tests

`OfflinePersistenceTest` (Robolectric) :

- première installation (seed)
- redémarrage (idempotence, pas de doublon)
- « mise à jour » (meta version + progression conservée)
- absence de réseau (quiz / flashcards / leçons)
- base déjà peuplée (progression + SRS conservés après seed)
- migration schéma 1 → 2 (quiz / SRS conservés, catalogue seedable)
