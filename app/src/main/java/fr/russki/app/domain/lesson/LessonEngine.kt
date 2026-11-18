package fr.russki.app.domain.lesson

import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.exercise.InteractiveExercise

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
        get() = rematchQueue.count { it !in rematchMastered }

    val finalCorrectCount: Int
        get() = finalGrades.values.count { it.countsAsMastered }

    val initiallyFailedIds: Set<String>
        get() = firstGrades.filterValues { !it.countsAsMastered }.keys

    val recoveredIds: Set<String>
        get() = initiallyFailedIds.intersect(rematchMastered)

    val stillHardIds: Set<String>
        get() = initiallyFailedIds - rematchMastered

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
 */
class LessonEngine(
    private val grader: ExerciseGrader = ExerciseGrader(),
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
                // Pas de test final dédié : reprendre 1–2 actifs / guidés distincts
                practice.takeLast(minOf(2, practice.size))
                    .mapIndexed { i, ex -> ex.copy(id = "final-${ex.id}", pedagogyStage = PedagogyStage.Final, sortOrder = i) }
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
        return session.copy(phase = LessonPhase.Practice, exerciseIndex = 0, currentGrade = null, revealedForCurrent = false)
    }

    fun revealHint(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Practice || session.phase == LessonPhase.Rematch || session.phase == LessonPhase.FinalCheck)
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
                if (exercise.id in session.rematchMastered) {
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
            LessonPhase.Rematch -> {
                val mastered = if (grade.countsAsMastered) {
                    session.rematchMastered + exercise.id
                } else {
                    session.rematchMastered
                }
                session.copy(
                    rematchGrades = session.rematchGrades + (exercise.id to grade),
                    rematchMastered = mastered,
                    currentGrade = grade,
                    // Si encore raté / révélé : remettre en fin de file
                    rematchQueue = if (grade.countsAsMastered) {
                        session.rematchQueue
                    } else {
                        (session.rematchQueue - exercise.id) + exercise.id
                    },
                )
            }
            LessonPhase.FinalCheck -> session.copy(
                finalGrades = session.finalGrades + (exercise.id to grade),
                currentGrade = grade,
            )
            else -> session
        }
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
                    if (session.rematchQueue.any { it !in session.rematchMastered }) {
                        session.copy(
                            phase = LessonPhase.Rematch,
                            exerciseIndex = 0,
                            currentGrade = null,
                            revealedForCurrent = false,
                            rematchQueue = session.rematchQueue.filter { it !in session.rematchMastered },
                        )
                    } else {
                        session.copy(
                            phase = LessonPhase.Recap,
                            exerciseIndex = 0,
                            currentGrade = null,
                            revealedForCurrent = false,
                        )
                    }
                } else {
                    session.copy(exerciseIndex = next, currentGrade = null, revealedForCurrent = false)
                }
            }
            LessonPhase.Rematch -> {
                val pending = session.rematchQueue.filter { it !in session.rematchMastered }
                if (pending.isEmpty()) {
                    session.copy(
                        phase = LessonPhase.Recap,
                        exerciseIndex = 0,
                        currentGrade = null,
                        revealedForCurrent = false,
                        rematchQueue = emptyList(),
                    )
                } else {
                    // Avancer dans la file actuelle ; si fin, recommencer sur les restants
                    val rematchList = pending.mapNotNull { id ->
                        session.practiceExercises.find { it.id == id }
                    }
                    val nextIdx = session.exerciseIndex + 1
                    if (nextIdx >= rematchList.size) {
                        session.copy(
                            rematchQueue = pending,
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

    fun beginFinalCheck(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Recap)
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
        return session.copy(phase = LessonPhase.Summary, currentGrade = null)
    }

    fun complete(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Summary || session.phase == LessonPhase.Completed)
        return session.copy(phase = LessonPhase.Completed)
    }
}
