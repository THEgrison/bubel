package fr.russki.app.di

import android.content.Context
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.flashcard.FlashcardRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.pack.PackRepository
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.quiz.QuizRepository
import fr.russki.app.domain.quiz.QuizEngine
import fr.russki.app.domain.srs.SrsScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: RusskiDatabase = RusskiDatabase.get(appContext)

    val contentRepository: OfflineContentRepository =
        OfflineContentRepository(audioAvailability = AssetAudioAvailability(appContext))

    val seedInitializer: ContentSeedInitializer = ContentSeedInitializer(database)

    val packRepository: PackRepository = PackRepository(appContext, database)

    val lessonProgressRepository: LessonProgressRepository =
        LessonProgressRepository(database.userLessonProgressDao())

    val quizRepository: QuizRepository = QuizRepository(
        dao = database.quizSessionDao(),
        contentRepository = contentRepository,
        engine = QuizEngine(),
    )

    val flashcardRepository: FlashcardRepository = FlashcardRepository(
        dao = database.srsCardDao(),
        historyDao = database.flashcardHistoryDao(),
        contentRepository = contentRepository,
        scheduler = SrsScheduler(),
    )

    /** Seed idempotent au démarrage — aucune attente réseau. */
    fun warmLocalData() {
        appScope.launch {
            seedInitializer.ensureSeeded()
            flashcardRepository.ensureDeckSeeded()
        }
    }
}

object ServiceLocator {
    @Volatile
    private var container: AppContainer? = null

    fun init(context: Context) {
        if (container == null) {
            synchronized(this) {
                if (container == null) {
                    container = AppContainer(context)
                    OfflineContentRepository.install(container!!.contentRepository)
                    container!!.warmLocalData()
                }
            }
        }
    }

    fun require(): AppContainer =
        checkNotNull(container) { "ServiceLocator non initialisé." }
}
