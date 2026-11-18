package fr.russki.app.data.content

import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.PhrasesSeed
import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.local.entity.AudioAssetEntity
import fr.russki.app.data.local.entity.ContentLetterEntity
import fr.russki.app.data.local.entity.ContentLessonEntity
import fr.russki.app.data.local.entity.ContentLessonExerciseEntity
import fr.russki.app.data.local.entity.ContentMetaEntity
import fr.russki.app.data.local.entity.ContentPackEntity
import fr.russki.app.data.local.entity.ContentPackMemberEntity
import fr.russki.app.data.local.entity.ContentPhraseEntity
import fr.russki.app.data.local.entity.ContentWordEntity
import fr.russki.app.data.local.entity.CurriculumExerciseEntity
import fr.russki.app.data.local.entity.CurriculumLessonEntity
import fr.russki.app.data.local.entity.CurriculumUnitEntity
import fr.russki.app.data.local.entity.InstalledPackEntity
import org.json.JSONArray

/**
 * Initialise le catalogue pédagogique embarqué de façon **idempotente**.
 *
 * - INSERT IGNORE : ne duplique pas, n'écrase pas le contenu déjà présent
 * - ne touche jamais quiz / SRS / historique / progression leçon
 * - sépare packs embarqués, packs optionnels et état d'installation
 */
class ContentSeedInitializer(
    private val db: RusskiDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    data class Result(
        val alreadySeeded: Boolean,
        val bundledVersion: String,
        val letterCount: Int,
        val wordCount: Int,
        val phraseCount: Int,
        val lessonCount: Int,
        val curriculumUnitCount: Int,
        val curriculumLessonCount: Int,
        val curriculumExerciseCount: Int,
    )

    suspend fun ensureSeeded(): Result {
        val meta = db.contentMetaDao()
        val current = meta.get(ContentMetaKeys.BUNDLED_VERSION)
        val curriculumVersion = meta.get(ContentMetaKeys.CURRICULUM_VERSION)
        val pack = SeedContentPack.pack
        ContentValidator.requireValid(pack)

        // Toujours réinsérer en IGNORE (ajout de nouvelles entrées sur mise à jour)
        // sans REPLACE — la progression utilisateur n'est jamais dans ces tables.
        seedLetters()
        seedWords()
        seedPhrases()
        seedLessonsAndExercises()
        seedCurriculumA1()
        seedAudioMetadata()
        seedPackCatalog()
        ensureCorePackInstalled()

        if (current != SeedContentPack.VERSION) {
            meta.upsert(
                ContentMetaEntity(
                    key = ContentMetaKeys.BUNDLED_VERSION,
                    value = SeedContentPack.VERSION,
                ),
            )
            meta.upsert(
                ContentMetaEntity(
                    key = ContentMetaKeys.SEED_COMPLETED_AT,
                    value = clock().toString(),
                ),
            )
        }
        if (curriculumVersion != CurriculumVersions.A1_PATH) {
            meta.upsert(
                ContentMetaEntity(
                    key = ContentMetaKeys.CURRICULUM_VERSION,
                    value = CurriculumVersions.A1_PATH,
                ),
            )
        }

        return Result(
            alreadySeeded = current == SeedContentPack.VERSION &&
                curriculumVersion == CurriculumVersions.A1_PATH,
            bundledVersion = SeedContentPack.VERSION,
            letterCount = db.contentLetterDao().count(),
            wordCount = db.contentWordDao().count(),
            phraseCount = db.contentPhraseDao().count(),
            lessonCount = db.contentLessonDao().count(),
            curriculumUnitCount = db.curriculumUnitDao().count(),
            curriculumLessonCount = db.curriculumLessonDao().count(),
            curriculumExerciseCount = db.curriculumExerciseDao().count(),
        )
    }

    private suspend fun seedCurriculumA1() {
        val units = A1CurriculumSeed.units
        db.curriculumUnitDao().insertIgnore(
            units.map { unit ->
                CurriculumUnitEntity(
                    id = unit.id,
                    cefrLevel = unit.cefrLevel.name,
                    titleFr = unit.titleFr,
                    descriptionFr = unit.descriptionFr,
                    sortOrder = unit.sortOrder,
                    source = ContentSource.BUNDLED,
                )
            },
        )
        db.curriculumLessonDao().insertIgnore(
            units.flatMap { unit ->
                unit.lessons.map { lesson ->
                    CurriculumLessonEntity(
                        id = lesson.id,
                        unitId = lesson.unitId,
                        titleFr = lesson.titleFr,
                        objectiveFr = lesson.objectiveFr,
                        introductionFr = lesson.introductionFr,
                        summaryFr = lesson.summaryFr,
                        sortOrder = lesson.sortOrder,
                        exerciseCount = lesson.exerciseCount,
                        legacyRouteHint = lesson.legacyRouteHint,
                        source = ContentSource.BUNDLED,
                    )
                }
            },
        )
        val exercises = units.flatMap { unit ->
            unit.lessons.flatMap { lesson ->
                A1CurriculumSeed.exercisesFor(lesson.id).map { ex ->
                    CurriculumExerciseEntity(
                        id = ex.id,
                        lessonId = lesson.id,
                        type = ex.type.name,
                        promptFr = ex.promptFr,
                        stimulus = ex.stimulus,
                        choicesJson = JSONArray(ex.choices).toString(),
                        acceptedAnswersJson = JSONArray(ex.acceptedAnswers).toString(),
                        explanationFr = ex.explanationFr,
                        sortOrder = ex.sortOrder,
                    )
                }
            }
        }
        db.curriculumExerciseDao().insertIgnore(exercises)
    }

    private suspend fun seedLetters() {
        val entities = SeedContentPack.pack.alphabet.letters.map { letter ->
            ContentLetterEntity(
                id = letter.id,
                sortOrder = letter.order,
                uppercase = letter.uppercase,
                lowercase = letter.lowercase,
                nameRu = letter.nameRu,
                pronunciationGuideFr = letter.pronunciationGuideFr,
                soundInWordsFr = letter.soundInWordsFr,
                exampleWordRu = letter.exampleWordRu,
                exampleWordFr = letter.exampleWordFr,
                audioAssetId = letter.audioAssetPath,
                source = ContentSource.BUNDLED,
            )
        }
        db.contentLetterDao().insertIgnore(entities)
    }

    private suspend fun seedWords() {
        val entities = SeedContentPack.pack.vocabularyModules.flatMap { module ->
            module.entries.map { entry ->
                ContentWordEntity(
                    id = entry.id,
                    textRu = entry.textRu,
                    translationFr = entry.translationFr,
                    category = entry.category,
                    level = entry.level.name,
                    exampleRu = entry.exampleRu,
                    exampleFr = entry.exampleFr,
                    audioAssetId = entry.audioAssetPath,
                    source = ContentSource.BUNDLED,
                )
            }
        }
        db.contentWordDao().insertIgnore(entities)
    }

    private suspend fun seedPhrases() {
        val entities = PhrasesSeed.phrases.map { phrase ->
            ContentPhraseEntity(
                id = phrase.id,
                textRu = phrase.textRu,
                translationFr = phrase.translationFr,
                category = phrase.category,
                level = phrase.level,
                audioAssetId = phrase.audioAssetId,
                source = ContentSource.BUNDLED,
            )
        }
        db.contentPhraseDao().insertIgnore(entities)
    }

    private suspend fun seedLessonsAndExercises() {
        val alphabet = SeedContentPack.pack.alphabet
        val greetings = SeedContentPack.pack.vocabularyModules.first()
        val lessons = listOf(
            ContentLessonEntity(
                id = "lesson-alphabet",
                moduleId = alphabet.id,
                titleFr = alphabet.titleFr,
                descriptionFr = alphabet.descriptionFr,
                kind = "alphabet",
                sortOrder = 1,
                source = ContentSource.BUNDLED,
            ),
            ContentLessonEntity(
                id = "lesson-greetings",
                moduleId = greetings.id,
                titleFr = greetings.titleFr,
                descriptionFr = greetings.descriptionFr,
                kind = "vocabulary",
                sortOrder = 2,
                source = ContentSource.BUNDLED,
            ),
            ContentLessonEntity(
                id = "lesson-phrases",
                moduleId = "phrases-beginner",
                titleFr = "Premières phrases",
                descriptionFr = "Formules utiles pour se présenter et demander de répéter.",
                kind = "phrases",
                sortOrder = 3,
                source = ContentSource.BUNDLED,
            ),
        )
        db.contentLessonDao().insertIgnore(lessons)

        val exercises = alphabet.exercises.mapIndexed { index, exercise ->
            ContentLessonExerciseEntity(
                id = exercise.id,
                lessonId = "lesson-alphabet",
                type = exercise.type.name,
                promptFr = exercise.promptFr,
                stimulus = exercise.stimulus,
                optionsJson = JSONArray(exercise.options).toString(),
                correctIndex = exercise.correctIndex,
                explanationFr = exercise.explanationFr,
                relatedContentId = exercise.relatedLetterId,
                sortOrder = index + 1,
            )
        }
        db.contentLessonExerciseDao().insertIgnore(exercises)
    }

    private suspend fun seedAudioMetadata() {
        // Métadonnées uniquement — aucun fichier audio embarqué pour l'instant.
        // Les chemins restent déclaratifs ; l'UI n'expose la lecture que si le fichier existe.
        val assets = listOf(
            AudioAssetEntity(
                id = "audio-placeholder-core",
                relativePath = "audio/core/placeholder.ogg",
                titleFr = "Placeholder audio noyau (non embarqué)",
                byteSize = 0L,
                durationMs = null,
                contentRefId = null,
                requiredPackId = PackIds.CORE_BUNDLED,
            ),
            AudioAssetEntity(
                id = "audio-demo-readme",
                relativePath = "packs/audio-demo-v1/readme.txt",
                titleFr = "Fichier démo pack audio optionnel",
                byteSize = 128L,
                durationMs = null,
                contentRefId = null,
                requiredPackId = PackIds.AUDIO_DEMO_OPTIONAL,
            ),
        )
        db.audioAssetDao().insertIgnore(assets)
    }

    private suspend fun seedPackCatalog() {
        val core = ContentPackEntity(
            id = PackIds.CORE_BUNDLED,
            titleFr = "Contenu de base",
            descriptionFr = "Alphabet, vocabulaire salutations, phrases et exercices — livré avec l'application.",
            isOptional = false,
            isBundled = true,
            totalBytes = 0L,
            version = SeedContentPack.VERSION,
        )
        val audioDemo = ContentPackEntity(
            id = PackIds.AUDIO_DEMO_OPTIONAL,
            titleFr = "Démo audio (optionnel)",
            descriptionFr = "Petit pack local pour tester installation / suppression sans réseau. " +
                "La suppression n'efface pas quiz, flashcards ni progression.",
            isOptional = true,
            isBundled = false,
            totalBytes = 128L,
            version = "1.0.0",
        )
        db.contentPackDao().insertPackWithMembers(
            pack = core,
            members = listOf(
                ContentPackMemberEntity(PackIds.CORE_BUNDLED, "lesson", "lesson-alphabet"),
                ContentPackMemberEntity(PackIds.CORE_BUNDLED, "lesson", "lesson-greetings"),
                ContentPackMemberEntity(PackIds.CORE_BUNDLED, "lesson", "lesson-phrases"),
            ),
        )
        db.contentPackDao().insertPackWithMembers(
            pack = audioDemo,
            members = listOf(
                ContentPackMemberEntity(PackIds.AUDIO_DEMO_OPTIONAL, "audio", "audio-demo-readme"),
            ),
        )
    }

    private suspend fun ensureCorePackInstalled() {
        val installed = db.installedPackDao()
        if (installed.get(PackIds.CORE_BUNDLED) == null) {
            installed.upsert(
                InstalledPackEntity(
                    packId = PackIds.CORE_BUNDLED,
                    state = InstalledPackState.INSTALLED,
                    downloadedBytes = 0L,
                    installedAtEpochMs = clock(),
                    localDir = "bundled",
                    lastError = null,
                ),
            )
        }
        if (installed.get(PackIds.AUDIO_DEMO_OPTIONAL) == null) {
            installed.upsert(
                InstalledPackEntity(
                    packId = PackIds.AUDIO_DEMO_OPTIONAL,
                    state = InstalledPackState.NOT_INSTALLED,
                    downloadedBytes = 0L,
                    installedAtEpochMs = null,
                    localDir = null,
                    lastError = null,
                ),
            )
        }
    }
}
