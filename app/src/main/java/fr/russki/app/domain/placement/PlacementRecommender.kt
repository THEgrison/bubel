package fr.russki.app.domain.placement

import fr.russki.app.domain.exercise.ExerciseGrade

/**
 * Règles de recommandation — déterministes et testables.
 *
 * - Estimation indicative, **pas** une certification CECRL.
 * - Si données insuffisantes ou curriculum vide → début du parcours disponible.
 * - Sinon : première compétence (ordre curriculum) dont le ratio de réussite
 *   sur les réponses données est &lt; [SKILL_PASS_RATIO].
 * - Si toutes les compétences évaluées passent → leçon de révision vocabulaire
 *   (toujours une leçon réelle du parcours).
 */
object PlacementRecommender {

    /** Ratio correct/répondu en dessous duquel la compétence est jugée fragile. */
    const val SKILL_PASS_RATIO: Float = 0.5f

    /**
     * Au moins la moitié des questions du test doivent être répondues
     * pour aller au-delà du début de parcours.
     */
    const val MIN_ANSWERED_FRACTION: Float = 0.5f

    const val FALLBACK_LESSON_ID: String = "a1-lesson-alphabet"
    const val STRONG_LESSON_ID: String = "a1-lesson-review-vocab"

    fun scoreSkills(
        questions: List<PlacementQuestion>,
        grades: Map<String, ExerciseGrade>,
    ): List<SkillScore> {
        return PlacementSkill.entries
            .sortedBy { it.order }
            .mapNotNull { skill ->
                val qs = questions.filter { it.skill == skill }
                if (qs.isEmpty()) return@mapNotNull null
                val answered = qs.count { it.id in grades }
                val correct = qs.count { grades[it.id]?.isCorrect == true }
                SkillScore(skill = skill, correct = correct, answered = answered)
            }
    }

    fun recommend(
        questions: List<PlacementQuestion>,
        grades: Map<String, ExerciseGrade>,
        availableLessonIds: Set<String>,
        lessonTitles: Map<String, String> = emptyMap(),
    ): PlacementRecommendation {
        val startId = resolveAvailable(
            preferred = FALLBACK_LESSON_ID,
            available = availableLessonIds,
        )
        if (questions.isEmpty() || availableLessonIds.isEmpty()) {
            return PlacementRecommendation(
                lessonId = startId.orEmpty(),
                lessonTitleFr = titleOf(startId.orEmpty(), lessonTitles, "Début du parcours"),
                rationaleFr = "Données insuffisantes : commencez au début du parcours disponible.",
                isFallbackStart = true,
            )
        }

        val minAnswered = (questions.size * MIN_ANSWERED_FRACTION).toInt().coerceAtLeast(1)
        if (grades.size < minAnswered) {
            return PlacementRecommendation(
                lessonId = startId ?: availableLessonIds.first(),
                lessonTitleFr = titleOf(
                    startId ?: availableLessonIds.first(),
                    lessonTitles,
                    "Début du parcours",
                ),
                rationaleFr = "Trop peu de réponses pour estimer un point de départ : " +
                    "recommandation par défaut = début du parcours.",
                isFallbackStart = true,
            )
        }

        val skillScores = scoreSkills(questions, grades)
        for (score in skillScores) {
            if (score.answered == 0) continue
            if (score.ratio < SKILL_PASS_RATIO) {
                val lessonId = resolveAvailable(
                    preferred = score.skill.recommendedLessonId,
                    available = availableLessonIds,
                ) ?: startId ?: availableLessonIds.first()
                return PlacementRecommendation(
                    lessonId = lessonId,
                    lessonTitleFr = titleOf(
                        lessonId,
                        lessonTitles,
                        score.skill.titleFr,
                    ),
                    rationaleFr = "Estimation indicative : reprise suggérée sur « ${score.skill.titleFr} » " +
                        "(réussite ${score.correct}/${score.answered} sur ce thème).",
                    isFallbackStart = lessonId == FALLBACK_LESSON_ID &&
                        score.skill == PlacementSkill.Alphabet,
                )
            }
        }

        val strongId = resolveAvailable(STRONG_LESSON_ID, availableLessonIds)
            ?: availableLessonIds.maxByOrNull { it }
            ?: startId
            ?: ""
        return PlacementRecommendation(
            lessonId = strongId,
            lessonTitleFr = titleOf(strongId, lessonTitles, "Révisions"),
            rationaleFr = "Estimation indicative : les thèmes évalués semblent acquis. " +
                "Vous pouvez poursuivre vers les révisions du parcours — " +
                "ce n'est pas un niveau CECRL officiel.",
            isFallbackStart = false,
        )
    }

    fun buildResult(
        questions: List<PlacementQuestion>,
        grades: Map<String, ExerciseGrade>,
        availableLessonIds: Set<String>,
        lessonTitles: Map<String, String>,
        completedAtEpochMs: Long,
    ): PlacementResult {
        val recommendation = recommend(questions, grades, availableLessonIds, lessonTitles)
        return PlacementResult(
            completedAtEpochMs = completedAtEpochMs,
            correctCount = grades.values.count { it.isCorrect },
            totalQuestions = questions.size,
            answeredCount = grades.size,
            skillScores = scoreSkills(questions, grades),
            recommendation = recommendation,
        )
    }

    private fun resolveAvailable(preferred: String, available: Set<String>): String? {
        if (preferred in available) return preferred
        // Première leçon « a1-lesson-* » triée, sinon premier id
        return available.filter { it.startsWith("a1-lesson-") }.minOrNull()
            ?: available.minOrNull()
    }

    private fun titleOf(
        lessonId: String,
        titles: Map<String, String>,
        fallback: String,
    ): String = titles[lessonId]?.takeIf { it.isNotBlank() } ?: fallback
}
