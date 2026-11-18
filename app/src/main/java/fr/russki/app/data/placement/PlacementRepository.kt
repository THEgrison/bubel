package fr.russki.app.data.placement

import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.placement.PlacementQuestion
import fr.russki.app.domain.placement.PlacementRecommender
import fr.russki.app.domain.placement.PlacementResult
import fr.russki.app.domain.placement.PlacementStoredResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Accès placement — ne lit/écrit **pas** la progression leçons/dialogues.
 */
class PlacementRepository(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: LessonProgressRepository,
    private val store: LocalPlacementStore,
) {

    fun loadStored(): PlacementStoredResults = store.load()

    suspend fun loadQuestions(): List<PlacementQuestion> = withContext(Dispatchers.IO) {
        val path = curriculumRepository.loadA1Path()
        val available = path.units
            .flatMap { it.unit.lessons }
            .map { it.id }
            .toSet()
            .ifEmpty { A1CurriculumSeed.allLessonIds().toSet() }
        PlacementQuestionBank.filterForAvailableLessons(
            questions = PlacementQuestionBank.buildDefault(),
            availableLessonIds = available,
        )
    }

    suspend fun lessonTitles(): Map<String, String> = withContext(Dispatchers.IO) {
        val path = curriculumRepository.loadA1Path()
        path.units
            .flatMap { it.unit.lessons }
            .associate { it.id to it.titleFr }
    }

    suspend fun availableLessonIds(): Set<String> = withContext(Dispatchers.IO) {
        curriculumRepository.loadA1Path().units
            .flatMap { it.unit.lessons }
            .map { it.id }
            .toSet()
    }

    suspend fun completeAndPersist(
        questions: List<PlacementQuestion>,
        grades: Map<String, ExerciseGrade>,
        completedAtEpochMs: Long = System.currentTimeMillis(),
    ): PlacementResult = withContext(Dispatchers.IO) {
        // Isolation : snapshot du nombre de progressions avant/après (invariant).
        val before = progressRepository.getAll().size
        val result = PlacementRecommender.buildResult(
            questions = questions,
            grades = grades,
            availableLessonIds = availableLessonIds(),
            lessonTitles = lessonTitles(),
            completedAtEpochMs = completedAtEpochMs,
        )
        store.saveCompleted(result)
        val after = progressRepository.getAll().size
        check(before == after) {
            "Le test de placement ne doit pas modifier user_lesson_progress ($before → $after)"
        }
        result
    }
}
