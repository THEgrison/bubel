package fr.russki.app.data.curriculum

import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.local.entity.CurriculumExerciseEntity
import fr.russki.app.data.local.entity.CurriculumLessonEntity
import fr.russki.app.data.local.entity.CurriculumUnitEntity
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.progress.ProgressContentIds
import fr.russki.app.domain.curriculum.CefrLevel
import fr.russki.app.domain.curriculum.CourseUnit
import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.curriculum.LessonProgressSummary
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class UnitWithProgress(
    val unit: CourseUnit,
    val lessonProgress: Map<String, LessonProgressSummary>,
)

/**
 * Parcours CECRL — lit le curriculum **Room** (seedé), pas le seed Kotlin en mémoire.
 * La progression reste dans `user_lesson_progress`.
 */
class CurriculumRepository(
    private val db: RusskiDatabase,
    private val progressRepository: LessonProgressRepository,
    private val ensureSeeded: suspend () -> Unit = {},
) {

    suspend fun unitsForLevel(level: CefrLevel): List<CourseUnit> = withContext(Dispatchers.IO) {
        ensureSeeded()
        db.curriculumUnitDao().byCefrLevel(level.name).map { loadUnit(it) }
    }

    suspend fun a1Units(): List<CourseUnit> = unitsForLevel(CefrLevel.A1)

    suspend fun lesson(lessonId: String): LessonOutline? = withContext(Dispatchers.IO) {
        ensureSeeded()
        db.curriculumLessonDao().get(lessonId)?.toOutline()
    }

    suspend fun exercises(lessonId: String): List<InteractiveExercise> = withContext(Dispatchers.IO) {
        ensureSeeded()
        db.curriculumExerciseDao().forLesson(lessonId).map { it.toDomain() }
    }

    suspend fun loadA1Path(): UnitWithProgress = withContext(Dispatchers.IO) {
        ensureSeeded()
        val units = db.curriculumUnitDao().byCefrLevel(CefrLevel.A1.name)
        val unitEntity = units.firstOrNull()
            ?: error("Curriculum A1 absent — seed Room requis.")
        val unit = loadUnit(unitEntity)
        // Ignore les lignes dialogue-* : elles ne sont pas des leçons du parcours.
        val stored = progressRepository.getAll()
            .filter { ProgressContentIds.isCurriculumLesson(it.lessonId) }
            .associateBy { it.lessonId }
        val progress = unit.lessons.associate { lesson ->
            ProgressContentIds.requireCurriculumLessonId(lesson.id)
            val row = stored[lesson.id]
            lesson.id to LessonProgressSummary(
                lessonId = lesson.id,
                completedExerciseCount = row?.completedExerciseCount ?: 0,
                totalExerciseCount = lesson.exerciseCount,
                completedAtEpochMs = row?.completedAtEpochMs,
            )
        }
        UnitWithProgress(unit = unit, lessonProgress = progress)
    }

    suspend fun markLessonOpened(lessonId: String) {
        progressRepository.markOpened(ProgressContentIds.requireCurriculumLessonId(lessonId))
    }

    suspend fun recordExercise(lessonId: String, totalExercises: Int) {
        progressRepository.recordExerciseCompleted(
            ProgressContentIds.requireCurriculumLessonId(lessonId),
            totalExercises,
        )
    }

    suspend fun markLessonCompleted(lessonId: String, totalExercises: Int) {
        progressRepository.completeLesson(
            ProgressContentIds.requireCurriculumLessonId(lessonId),
            totalExercises,
        )
    }

    private suspend fun loadUnit(entity: CurriculumUnitEntity): CourseUnit {
        val lessons = db.curriculumLessonDao().forUnit(entity.id).map { it.toOutline() }
        return CourseUnit(
            id = entity.id,
            cefrLevel = CefrLevel.valueOf(entity.cefrLevel),
            titleFr = entity.titleFr,
            descriptionFr = entity.descriptionFr,
            sortOrder = entity.sortOrder,
            lessons = lessons,
        )
    }

    private fun CurriculumLessonEntity.toOutline(): LessonOutline =
        LessonOutline(
            id = id,
            unitId = unitId,
            titleFr = titleFr,
            objectiveFr = objectiveFr,
            introductionFr = introductionFr,
            summaryFr = summaryFr,
            sortOrder = sortOrder,
            exerciseCount = exerciseCount,
            legacyRouteHint = legacyRouteHint,
        )

    private fun CurriculumExerciseEntity.toDomain(): InteractiveExercise =
        InteractiveExercise(
            id = id,
            type = InteractiveExerciseType.valueOf(type),
            promptFr = promptFr,
            stimulus = stimulus,
            choices = jsonStringList(choicesJson),
            acceptedAnswers = jsonStringList(acceptedAnswersJson),
            explanationFr = explanationFr,
            sortOrder = sortOrder,
        )

    private fun jsonStringList(raw: String): List<String> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                add(array.getString(i))
            }
        }
    }
}
