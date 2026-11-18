package fr.russki.app.di

import android.content.Context
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.flashcard.FlashcardRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.online.OnlineConfig
import fr.russki.app.data.online.api.DisabledOnlineApi
import fr.russki.app.data.online.api.HttpRusskiOnlineApi
import fr.russki.app.data.online.api.RusskiOnlineApi
import fr.russki.app.data.online.catalog.RemoteCatalogRepository
import fr.russki.app.data.online.download.DownloadLimiter
import fr.russki.app.data.online.download.PackDownloadEngine
import fr.russki.app.data.online.sync.ProgressSyncGateway
import fr.russki.app.data.online.sync.SyncPreferences
import fr.russki.app.data.pack.PackCoordinator
import fr.russki.app.data.pack.PackRepository
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.quiz.QuizRepository
import fr.russki.app.domain.quiz.QuizEngine
import fr.russki.app.domain.srs.SrsScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(
    context: Context,
    onlineConfig: OnlineConfig = OnlineConfig.fromBuildConfig(),
    onlineApiOverride: RusskiOnlineApi? = null,
) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val onlineConfig: OnlineConfig = onlineConfig

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

    val onlineApi: RusskiOnlineApi = onlineApiOverride ?: if (onlineConfig.isConfigured) {
        HttpRusskiOnlineApi(onlineConfig)
    } else {
        DisabledOnlineApi()
    }

    val downloadLimiter = DownloadLimiter(onlineConfig.maxConcurrentDownloads)

    val packDownloadEngine: PackDownloadEngine = PackDownloadEngine(
        context = appContext,
        db = database,
        api = onlineApi,
        limiter = downloadLimiter,
    )

    val remoteCatalogRepository: RemoteCatalogRepository =
        RemoteCatalogRepository(appContext, onlineApi)

    val packCoordinator: PackCoordinator = PackCoordinator(
        context = appContext,
        local = packRepository,
        remoteCatalog = remoteCatalogRepository,
        downloadEngine = packDownloadEngine,
        preferOfflineOnly = {
            // Lecture best-effort : si le profil n’est pas encore chargé, false.
            runCatching {
                // Préférence session via TemporaryContentSource / profil — défaut false.
                false
            }.getOrDefault(false)
        },
    )

    val syncPreferences = SyncPreferences()

    val progressSyncGateway: ProgressSyncGateway = ProgressSyncGateway(
        api = onlineApi,
        db = database,
        preferences = syncPreferences,
    )

    /** Seed idempotent au démarrage — aucune attente réseau. */
    fun warmLocalData() {
        appScope.launch {
            seedInitializer.ensureSeeded()
            flashcardRepository.ensureDeckSeeded()
            packDownloadEngine.cleanupAllOrphans()
        }
    }
}

object ServiceLocator {
    @Volatile
    private var container: AppContainer? = null

    fun init(context: Context, onlineConfig: OnlineConfig = OnlineConfig.fromBuildConfig()) {
        if (container == null) {
            synchronized(this) {
                if (container == null) {
                    container = AppContainer(context, onlineConfig)
                    OfflineContentRepository.install(container!!.contentRepository)
                    container!!.warmLocalData()
                }
            }
        }
    }

    /** Remplace le conteneur (tests uniquement). */
    fun replaceForTests(container: AppContainer) {
        this.container = container
        OfflineContentRepository.install(container.contentRepository)
    }

    fun require(): AppContainer =
        checkNotNull(container) { "ServiceLocator non initialisé." }
}
