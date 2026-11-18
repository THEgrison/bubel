package fr.russki.app.data.temporary

import fr.russki.app.domain.model.ContinueLesson
import fr.russki.app.domain.model.HomeContent
import fr.russki.app.domain.model.LearningModule
import fr.russki.app.domain.model.LearningProgress
import fr.russki.app.domain.model.ModuleKind
import fr.russki.app.domain.model.ProfileSettings
import fr.russki.app.domain.model.ReviewSummary
import fr.russki.app.domain.model.ThemePreference
import kotlinx.coroutines.delay

/**
 * Source de contenu **temporaire** pour la phase UI.
 *
 * Remplacer par des repositories Room / DataStore / réseau
 * (`ContentRepository`, `ProgressRepository`, …) sans toucher aux ViewModels
 * au-delà de l'injection.
 */
object TemporaryContentSource {

    const val SOURCE_TAG: String = "temporary-preview"

    suspend fun loadHome(forceError: Boolean = false): HomeContent {
        delay(280)
        if (forceError) {
            error("Impossible de charger l'accueil (source temporaire).")
        }
        return HomeContent.Dashboard(
            progress = LearningProgress(
                completedLessons = 3,
                totalLessonsInPath = 24,
                streakDays = 0,
                wordsSeen = 42,
            ),
            continueLesson = ContinueLesson(
                id = "lesson-alphabet-04",
                moduleTitle = "Alphabet",
                lessonTitle = "Lettres ж, з, и",
                cyrillicPreview = "Ж ж · З з · И и",
                progressFraction = 0.35f,
            ),
            review = ReviewSummary(
                dueCount = 0,
                nextReviewLabel = "Aucune carte due pour l'instant",
            ),
        )
    }

    suspend fun loadModules(forceError: Boolean = false): List<LearningModule> {
        delay(220)
        if (forceError) {
            error("Impossible de charger les modules (source temporaire).")
        }
        return listOf(
            LearningModule(
                id = "alphabet",
                title = "Alphabet",
                subtitle = "33 lettres, sons et écriture",
                cyrillicSample = "А Б В Г Д",
                lessonCount = 8,
                completedLessonCount = 3,
                kind = ModuleKind.Alphabet,
            ),
            LearningModule(
                id = "vocab-basics",
                title = "Vocabulaire de base",
                subtitle = "Salutations, famille, objets du quotidien",
                cyrillicSample = "привет · семья",
                lessonCount = 10,
                completedLessonCount = 0,
                kind = ModuleKind.Vocabulary,
            ),
            LearningModule(
                id = "phrases",
                title = "Phrases utiles",
                subtitle = "Se présenter et demander son chemin",
                cyrillicSample = "Как вас зовут?",
                lessonCount = 6,
                completedLessonCount = 0,
                kind = ModuleKind.Phrases,
            ),
        )
    }

    suspend fun loadReviewDueCount(): Int {
        delay(120)
        // État vide volontaire pour cette phase : aucune révision due.
        return 0
    }

    suspend fun loadProfile(): ProfileSettings {
        delay(100)
        return ProfileSettings(
            displayNamePlaceholder = "Apprenant",
            dailyGoalMinutes = 10,
            soundEnabled = true,
            offlinePacksOnly = true,
            darkThemePreference = ThemePreference.System,
        )
    }
}
