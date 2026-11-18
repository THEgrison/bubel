package fr.russki.app.data.stats

import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.progress.ProgressContentIds
import fr.russki.app.domain.stats.DailyGoalStat
import fr.russki.app.domain.stats.DialogueProgressStat
import fr.russki.app.domain.stats.LearningStatsSnapshot
import fr.russki.app.domain.stats.PathProgressStat
import fr.russki.app.domain.stats.RegularityStat
import fr.russki.app.domain.stats.StreakCalculator
import fr.russki.app.domain.stats.UnitProgressStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatsRepository(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: LessonProgressRepository,
    private val db: RusskiDatabase,
    private val localStore: LocalStatsStore,
) {

    fun dailyGoalMinutes(): Int = localStore.dailyGoalMinutes()

    fun setDailyGoalMinutes(minutes: Int) {
        localStore.setDailyGoalMinutes(minutes)
    }

    fun markPedagogicalActivity() {
        localStore.markActiveToday()
    }

    suspend fun loadSnapshot(): LearningStatsSnapshot = withContext(Dispatchers.IO) {
        val path = curriculumRepository.loadA1Path()
        val progressRows = progressRepository.getAll()
        val lessonRows = progressRows.filter { ProgressContentIds.isCurriculumLesson(it.lessonId) }
        val dialogueRows = progressRows.filter { ProgressContentIds.isDialogue(it.lessonId) }

        val completedLessonIds = lessonRows
            .filter { it.completedAtEpochMs != null }
            .map { it.lessonId }
            .toSet()

        val unitStats = path.units.map { block ->
            val total = block.unit.lessons.size
            val done = block.unit.lessons.count { it.id in completedLessonIds }
            UnitProgressStat(
                unitId = block.unit.id,
                titleFr = block.unit.titleFr,
                completedLessons = done,
                totalLessons = total,
            )
        }
        val totalLessons = unitStats.sumOf { it.totalLessons }
        val completedLessons = unitStats.sumOf { it.completedLessons }

        val dialogueOutlines = A1DialoguesSeed.outlines()
        val completedDialogues = dialogueOutlines.count { outline ->
            dialogueRows.any { it.lessonId == outline.id && it.completedAtEpochMs != null }
        }

        val exerciseCompletions = lessonRows.sumOf { it.completedExerciseCount }
        val quizTotal = db.quizSessionDao().countAnswers()
        val quizCorrect = db.quizSessionDao().countCorrectAnswers()

        val today = localStore.today()
        val activeDays = localStore.activeDays()
        val studiedMin = localStore.studiedMinutesToday()
        val goal = localStore.dailyGoalMinutes()

        LearningStatsSnapshot(
            path = PathProgressStat(
                completedLessons = completedLessons,
                totalLessons = totalLessons,
                units = unitStats,
            ),
            dialogues = DialogueProgressStat(
                completedDialogues = completedDialogues,
                totalDialogues = dialogueOutlines.size,
            ),
            daily = DailyGoalStat(
                goalMinutes = goal,
                studiedMinutesToday = studiedMin,
                goalReached = studiedMin >= goal,
            ),
            regularity = RegularityStat(
                activeDaysCount = activeDays.size,
                currentStreak = StreakCalculator.currentStreak(activeDays, today),
                bestStreak = StreakCalculator.bestStreak(activeDays),
                activeToday = StreakCalculator.isActiveOn(activeDays, today),
                recentActiveDays = activeDays.sorted().takeLast(14),
            ),
            lessonExerciseCompletionsRecorded = exerciseCompletions,
            quizAnswersRecorded = quizTotal,
            quizCorrectAnswers = quizCorrect,
        )
    }
}
