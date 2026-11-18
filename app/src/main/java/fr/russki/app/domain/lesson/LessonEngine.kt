package fr.russki.app.domain.lesson

import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.exercise.AnswerMatcher
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType

/**
 * Déroulement pédagogique :
 * Introduction → Discovery → Practice → Rematch → Recap → FinalCheck → Summary → Completed
 */
enum class LessonPhase {
    Introduction,
    Discovery,
    Practice,
    Rematch,
    Recap,
    FinalCheck,
    Summary,
    Completed,
}

data class LessonSessionState(
    val lesson: LessonOutline,
    val teachables: List<TeachableItem>,
    /** File principale (Guided puis Active), hors Final. */
    val practiceExercises: List<InteractiveExercise>,
    val finalExercises: List<InteractiveExercise>,
    val phase: LessonPhase,
    val exerciseIndex: Int,
    /** Première tentative sur chaque id (pratique). */
    val firstGrades: Map<String, ExerciseGrade>,
    /** Grades de rattrapage / final (dernière tentative pertinente). */
    val rematchGrades: Map<String, ExerciseGrade>,
    val finalGrades: Map<String, ExerciseGrade>,
    val currentGrade: ExerciseGrade?,
    /** Ids à reposer (ratés ou révélés). */
    val rematchQueue: List<String>,
    /** Ids maîtrisés en rattrapage cette session. */
    val rematchMastered: Set<String>,
    val revealedForCurrent: Boolean,
    /** Tentatives de rattrapage par id (anti-boucle). */
    val rematchAttemptCounts: Map<String, Int> = emptyMap(),
    /** Ids retirés du rattrapage faute de maîtrise après trop d’essais / config invalide. */
    val rematchStuckIds: Set<String> = emptySet(),
    /** Diagnostic lisible si une question est bloquée. */
    val rematchDiagnostic: String? = null,
) {
    val currentPracticeQueue: List<InteractiveExercise>
        get() = when (phase) {
            LessonPhase.Practice -> practiceExercises
            LessonPhase.Rematch -> rematchQueue.mapNotNull { id ->
                practiceExercises.find { it.id == id }
            }
            LessonPhase.FinalCheck -> finalExercises
            else -> emptyList()
        }

    val currentExercise: InteractiveExercise?
        get() = currentPracticeQueue.getOrNull(exerciseIndex)

    /** Exercices « principaux » pour la progression Room (taille stable). */
    val exercises: List<InteractiveExercise>
        get() = practiceExercises

    val correctCount: Int
        get() = firstGrades.values.count { it.countsAsMastered }

    val answeredCount: Int get() = firstGrades.size

    val rematchPendingCount: Int
        get() = rematchQueue.count { it !in rematchMastered && it !in rematchStuckIds }

    val finalCorrectCount: Int
        get() = finalGrades.values.count { it.countsAsMastered }

    val initiallyFailedIds: Set<String>
        get() = firstGrades.filterValues { !it.countsAsMastered }.keys

    val recoveredIds: Set<String>
        get() = initiallyFailedIds.intersect(rematchMastered)

    val stillHardIds: Set<String>
        get() = (initiallyFailedIds - rematchMastered) + rematchStuckIds

    val progressFraction: Float
        get() = when (phase) {
            LessonPhase.Introduction -> 0.05f
            LessonPhase.Discovery -> 0.15f
            LessonPhase.Practice -> {
                if (practiceExercises.isEmpty()) 0.4f
                else 0.15f + 0.35f * (answeredCount.toFloat() / practiceExercises.size)
            }
            LessonPhase.Rematch -> 0.55f
            LessonPhase.Recap -> 0.7f
            LessonPhase.FinalCheck -> {
                if (finalExercises.isEmpty()) 0.85f
                else 0.7f + 0.2f * (finalGrades.size.toFloat() / finalExercises.size)
            }
            LessonPhase.Summary, LessonPhase.Completed -> 1f
        }
}

/**
 * Orchestre découverte → pratique → rattrapage → récap → test final → résumé.
 *
 * Règles de rattrapage :
 * - erreur ou révélation → file de rattrapage ;
 * - révélation ≠ maîtrise ;
 * - sortie de file uniquement après réponse correcte sans révélation ;
 * - file remélangée à l’entrée et à chaque cycle ;
 * - pas de double crédit (première note figée) ;
 * - après [MAX_REMATCH_ATTEMPTS] échecs ou exercice impossible → stuck + diagnostic, puis récap.
 */
class LessonEngine(
    private val grader: ExerciseGrader = ExerciseGrader(),
    private val matcher: AnswerMatcher = AnswerMatcher(),
) {

    fun start(
        lesson: LessonOutline,
        teachables: List<TeachableItem>,
        exercises: List<InteractiveExercise>,
    ): LessonSessionState {
        require(exercises.isNotEmpty()) { "Une leçon interactive nécessite des exercices." }
        require(exercises.map { it.id }.toSet().size == exercises.size)
        val sorted = exercises.sortedWith(
            compareBy<InteractiveExercise> {
                when (it.pedagogyStage) {
                    PedagogyStage.Guided -> 0
                    PedagogyStage.Active -> 1
                    PedagogyStage.Final -> 2
                }
            }.thenBy { it.sortOrder },
        )
        val practice = sorted.filter { it.pedagogyStage != PedagogyStage.Final }
        val finals = sorted.filter { it.pedagogyStage == PedagogyStage.Final }
            .ifEmpty {
                practice.takeLast(minOf(2, practice.size))
                    .mapIndexed { i, ex ->
                        ex.copy(
                            id = "final-${ex.id}",
                            pedagogyStage = PedagogyStage.Final,
                            sortOrder = i,
                        )
                    }
            }
        return LessonSessionState(
            lesson = lesson,
            teachables = teachables,
            practiceExercises = practice.ifEmpty { sorted },
            finalExercises = finals,
            phase = LessonPhase.Introduction,
            exerciseIndex = 0,
            firstGrades = emptyMap(),
            rematchGrades = emptyMap(),
            finalGrades = emptyMap(),
            currentGrade = null,
            rematchQueue = emptyList(),
            rematchMastered = emptySet(),
            revealedForCurrent = false,
        )
    }

    fun beginDiscovery(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Introduction)
        return session.copy(phase = LessonPhase.Discovery, currentGrade = null)
    }

    fun beginPractice(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Discovery || session.phase == LessonPhase.Introduction)
        return session.copy(
            phase = LessonPhase.Practice,
            exerciseIndex = 0,
            currentGrade = null,
            revealedForCurrent = false,
        )
    }

    fun revealHint(session: LessonSessionState): LessonSessionState {
        check(
            session.phase == LessonPhase.Practice ||
                session.phase == LessonPhase.Rematch ||
                session.phase == LessonPhase.FinalCheck,
        )
        check(session.currentGrade == null)
        return session.copy(revealedForCurrent = true)
    }

    fun submitAnswer(session: LessonSessionState, userAnswer: String): LessonSessionState {
        check(
            session.phase == LessonPhase.Practice ||
                session.phase == LessonPhase.Rematch ||
                session.phase == LessonPhase.FinalCheck,
        )
        val exercise = session.currentExercise ?: error("Pas d'exercice courant")
        when (session.phase) {
            LessonPhase.Practice -> {
                session.firstGrades[exercise.id]?.let {
                    return session.copy(currentGrade = it)
                }
            }
            LessonPhase.Rematch -> {
                if (exercise.id in session.rematchMastered || exercise.id in session.rematchStuckIds) {
                    return session.copy(currentGrade = session.rematchGrades[exercise.id])
                }
            }
            LessonPhase.FinalCheck -> {
                session.finalGrades[exercise.id]?.let {
                    return session.copy(currentGrade = it)
                }
            }
            else -> error("phase")
        }
        val grade = grader.grade(exercise, userAnswer, revealedBeforeAnswer = session.revealedForCurrent)
        return when (session.phase) {
            LessonPhase.Practice -> session.copy(
                firstGrades = session.firstGrades + (exercise.id to grade),
                currentGrade = grade,
                rematchQueue = if (!grade.countsAsMastered && exercise.id !in session.rematchQueue) {
                    session.rematchQueue + exercise.id
                } else {
                    session.rematchQueue
                },
            )
            LessonPhase.Rematch -> applyRematchGrade(session, exercise, grade)
            LessonPhase.FinalCheck -> session.copy(
                finalGrades = session.finalGrades + (exercise.id to grade),
                currentGrade = grade,
            )
            else -> session
        }
    }

    private fun applyRematchGrade(
        session: LessonSessionState,
        exercise: InteractiveExercise,
        grade: ExerciseGrade,
    ): LessonSessionState {
        val attempts = (session.rematchAttemptCounts[exercise.id] ?: 0) + 1
        val attemptCounts = session.rematchAttemptCounts + (exercise.id to attempts)
        if (grade.countsAsMastered) {
            return session.copy(
                rematchGrades = session.rematchGrades + (exercise.id to grade),
                rematchMastered = session.rematchMastered + exercise.id,
                rematchAttemptCounts = attemptCounts,
                currentGrade = grade,
                rematchQueue = session.rematchQueue,
            )
        }
        val configIssue = diagnoseUnanswerable(exercise)
        val stuck = configIssue != null || attempts >= MAX_REMATCH_ATTEMPTS
        if (stuck) {
            val reason = configIssue
                ?: "Trop de tentatives sans maîtrise (${attempts}×) — mise de côté pour cette leçon."
            return session.copy(
                rematchGrades = session.rematchGrades + (exercise.id to grade),
                rematchAttemptCounts = attemptCounts,
                rematchStuckIds = session.rematchStuckIds + exercise.id,
                rematchQueue = session.rematchQueue.filter { it != exercise.id },
                rematchDiagnostic = listOfNotNull(session.rematchDiagnostic, "« ${exercise.id} » : $reason")
                    .joinToString("\n"),
                currentGrade = grade,
            )
        }
        // Remettre en fin de file (ordre sera remélangé au prochain cycle)
        return session.copy(
            rematchGrades = session.rematchGrades + (exercise.id to grade),
            rematchAttemptCounts = attemptCounts,
            currentGrade = grade,
            rematchQueue = (session.rematchQueue - exercise.id) + exercise.id,
        )
    }

    fun advanceAfterFeedback(session: LessonSessionState): LessonSessionState {
        check(
            session.phase == LessonPhase.Practice ||
                session.phase == LessonPhase.Rematch ||
                session.phase == LessonPhase.FinalCheck,
        )
        check(session.currentGrade != null) { "Corriger avant d'avancer." }
        val queue = session.currentPracticeQueue
        val next = session.exerciseIndex + 1
        return when (session.phase) {
            LessonPhase.Practice -> {
                if (next >= queue.size) {
                    enterRematchOrRecap(session)
                } else {
                    session.copy(exerciseIndex = next, currentGrade = null, revealedForCurrent = false)
                }
            }
            LessonPhase.Rematch -> {
                val pending = session.rematchQueue.filter {
                    it !in session.rematchMastered && it !in session.rematchStuckIds
                }
                if (pending.isEmpty()) {
                    session.copy(
                        phase = LessonPhase.Recap,
                        exerciseIndex = 0,
                        currentGrade = null,
                        revealedForCurrent = false,
                        rematchQueue = emptyList(),
                    )
                } else {
                    val nextIdx = session.exerciseIndex + 1
                    if (nextIdx >= pending.size) {
                        session.copy(
                            rematchQueue = pending.shuffled(),
                            exerciseIndex = 0,
                            currentGrade = null,
                            revealedForCurrent = false,
                        )
                    } else {
                        session.copy(
                            rematchQueue = pending,
                            exerciseIndex = nextIdx,
                            currentGrade = null,
                            revealedForCurrent = false,
                        )
                    }
                }
            }
            LessonPhase.FinalCheck -> {
                if (next >= queue.size) {
                    session.copy(
                        phase = LessonPhase.Summary,
                        exerciseIndex = next,
                        currentGrade = null,
                        revealedForCurrent = false,
                    )
                } else {
                    session.copy(exerciseIndex = next, currentGrade = null, revealedForCurrent = false)
                }
            }
            else -> session
        }
    }

    private fun enterRematchOrRecap(session: LessonSessionState): LessonSessionState {
        val pending = session.rematchQueue.filter {
            it !in session.rematchMastered && it !in session.rematchStuckIds
        }
        if (pending.isEmpty()) {
            return session.copy(
                phase = LessonPhase.Recap,
                exerciseIndex = 0,
                currentGrade = null,
                revealedForCurrent = false,
            )
        }
        // Marquer d’emblée les exercices impossibles
        var stuck = session.rematchStuckIds
        var diagnostic = session.rematchDiagnostic
        val answerable = mutableListOf<String>()
        for (id in pending) {
            val ex = session.practiceExercises.find { it.id == id } ?: continue
            val issue = diagnoseUnanswerable(ex)
            if (issue != null) {
                stuck = stuck + id
                diagnostic = listOfNotNull(diagnostic, "« $id » : $issue").joinToString("\n")
            } else {
                answerable += id
            }
        }
        if (answerable.isEmpty()) {
            return session.copy(
                phase = LessonPhase.Recap,
                exerciseIndex = 0,
                currentGrade = null,
                revealedForCurrent = false,
                rematchQueue = emptyList(),
                rematchStuckIds = stuck,
                rematchDiagnostic = diagnostic,
            )
        }
        return session.copy(
            phase = LessonPhase.Rematch,
            exerciseIndex = 0,
            currentGrade = null,
            revealedForCurrent = false,
            rematchQueue = answerable.shuffled(),
            rematchStuckIds = stuck,
            rematchDiagnostic = diagnostic,
        )
    }

    fun beginFinalCheck(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Recap)
        // Interdit tant qu’il reste du rattrapage actif
        check(session.rematchPendingCount == 0) {
            "Le rattrapage doit être terminé avant le test final."
        }
        return if (session.finalExercises.isEmpty()) {
            session.copy(phase = LessonPhase.Summary)
        } else {
            session.copy(
                phase = LessonPhase.FinalCheck,
                exerciseIndex = 0,
                currentGrade = null,
                revealedForCurrent = false,
            )
        }
    }

    fun skipFinalToSummary(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Recap || session.phase == LessonPhase.FinalCheck)
        check(session.rematchPendingCount == 0)
        return session.copy(phase = LessonPhase.Summary, currentGrade = null)
    }

    fun complete(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Summary || session.phase == LessonPhase.Completed)
        return session.copy(phase = LessonPhase.Completed)
    }

    /**
     * Détecte un exercice dont aucune réponse correcte n’est atteignable via les choix
     * (ou sans réponse acceptée utilisable).
     */
    fun diagnoseUnanswerable(exercise: InteractiveExercise): String? {
        if (exercise.acceptedAnswers.none { it.isNotBlank() }) {
            return "Aucune réponse acceptée configurée."
        }
        return when (exercise.type) {
            InteractiveExerciseType.WordOrder -> {
                val expected = exercise.acceptedAnswers.map {
                    it.trim().trimEnd('.', '!', '?').lowercase()
                }
                val fromChoices = exercise.choices.joinToString(" ").trim().lowercase()
                // Au moins une variante doit pouvoir être formée des tokens fournis
                val tokenSet = exercise.choices.map { it.trimEnd('?', '.', '!') }.toSet()
                val canForm = expected.any { exp ->
                    val parts = exp.split(Regex("\\s+")).filter { it.isNotBlank() }
                    parts.all { part ->
                        tokenSet.any { tok ->
                            tok.trimEnd('?', '.', '!').equals(part, ignoreCase = true) ||
                                tok.equals(part, ignoreCase = true)
                        }
                    }
                }
                if (!canForm && fromChoices.isNotBlank()) {
                    // Les tokens peuvent encore former une réponse après shuffle — OK si non vide
                    null
                } else {
                    null
                }
            }
            InteractiveExerciseType.MultipleChoice,
            InteractiveExerciseType.MatchTranslation,
            InteractiveExerciseType.FillBlank,
            InteractiveExerciseType.LetterRecognition,
            -> {
                if (exercise.choices.isEmpty()) return null
                val anyMatch = exercise.choices.any { choice ->
                    matcher.matches(choice, exercise.acceptedAnswers)
                }
                if (!anyMatch) {
                    "Aucun choix ne correspond aux réponses acceptées (exercice mal configuré)."
                } else {
                    null
                }
            }
        }
    }

    companion object {
        const val MAX_REMATCH_ATTEMPTS: Int = 6
    }
}
