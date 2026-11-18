package fr.russki.app.data.online

import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.online.api.DisabledOnlineApi
import fr.russki.app.data.online.api.HttpRusskiOnlineApi
import fr.russki.app.data.online.api.RemoteJson
import fr.russki.app.data.online.catalog.RemoteCatalogRepository
import fr.russki.app.data.online.download.Checksums
import fr.russki.app.data.online.download.DownloadLimiter
import fr.russki.app.data.online.download.PackDownloadEngine
import fr.russki.app.data.online.sync.ProgressSyncGateway
import fr.russki.app.data.online.sync.SyncPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnlineApiTest {

    private lateinit var server: MockWebServer
    private lateinit var db: RusskiDatabase
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        db = RusskiDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    private fun api(): HttpRusskiOnlineApi =
        HttpRusskiOnlineApi(
            OnlineConfig(
                apiBaseUrl = server.url("/").toString().trimEnd('/'),
                allowCleartextForTests = true,
            ),
        )

    @Test
    fun disabledApi_doesNotCallNetwork_andLessonsStayLocal() = runBlocking {
        ContentSeedInitializer(db).ensureSeeded()
        val offline = OfflineContentRepository()
        assertEquals(33, offline.letters().size)

        val disabled = DisabledOnlineApi()
        assertFalse(disabled.isConfigured)
        val catalog = disabled.fetchCatalog()
        assertTrue(catalog is OnlineResult.Err)
        assertEquals(OnlineError.ONLINE_DISABLED, (catalog as OnlineResult.Err).error.code)
        assertEquals(33, offline.letters().size)
        assertTrue(db.contentLessonDao().getAll().isNotEmpty())
    }

    @Test
    fun fetchCatalog_validatesPayload() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "apiVersion":"1",
                  "catalogVersion":"2026.11.18",
                  "generatedAtEpochMs":1731920000000,
                  "lessons":[{
                    "id":"lesson-travel-01",
                    "titleFr":"Voyage",
                    "descriptionFr":"Bases",
                    "kind":"vocabulary",
                    "moduleId":"vocab-travel",
                    "sortOrder":10,
                    "minAppVersionCode":1,
                    "packIds":["audio-travel-v1"]
                  }],
                  "packs":[{
                    "id":"audio-travel-v1",
                    "titleFr":"Audio voyage",
                    "descriptionFr":"Pack test",
                    "version":"1.0.0",
                    "totalBytes":5,
                    "sha256":"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    "isOptional":true,
                    "manifestPath":"/v1/packs/audio-travel-v1/manifest"
                  }]
                }
                """.trimIndent(),
            ),
        )
        val result = api().fetchCatalog()
        assertTrue(result is OnlineResult.Ok)
        val catalog = (result as OnlineResult.Ok).value
        assertEquals("2026.11.18", catalog.catalogVersion)
        assertEquals(1, catalog.packs.size)
    }

    @Test
    fun fetchCatalog_rejectsInvalidSha() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "apiVersion":"1",
                  "catalogVersion":"x",
                  "generatedAtEpochMs":1,
                  "lessons":[],
                  "packs":[{
                    "id":"p",
                    "titleFr":"t",
                    "descriptionFr":"d",
                    "version":"1",
                    "totalBytes":1,
                    "sha256":"not-a-sha",
                    "isOptional":true,
                    "manifestPath":"/m"
                  }]
                }
                """.trimIndent(),
            ),
        )
        val result = api().fetchCatalog()
        assertTrue(result is OnlineResult.Err)
        assertEquals(OnlineError.INVALID_PAYLOAD, (result as OnlineResult.Err).error.code)
    }

    @Test
    fun download_resumesWithRange_andVerifiesChecksum() = runBlocking {
        val payload = "hello"
        val sha = Checksums.sha256Hex(payload.byteInputStream())
        // known: sha256("hello") =
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha)

        val partial = File(context.cacheDir, "hello.partial")
        partial.writeBytes(payload.take(2).toByteArray()) // "he"

        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 2-4/5")
                .setBody(Buffer().writeString("llo", Charsets.UTF_8)),
        )

        val result = api().downloadAssetResumable(
            downloadPath = "/v1/packs/p/assets/a",
            destinationPartial = partial,
            expectedBytes = 5L,
        )
        assertTrue(result is OnlineResult.Ok)
        assertEquals(payload, partial.readText())
        assertTrue(Checksums.matches(partial, sha, 5L))
    }

    @Test
    fun packDownloadEngine_installsOnlyAfterFullValidation() = runBlocking {
        ContentSeedInitializer(db).ensureSeeded()
        val bytes = "hello".toByteArray()
        val sha = Checksums.sha256Hex(bytes.inputStream())

        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "packId":"audio-test-v1",
                  "version":"1.0.0",
                  "titleFr":"Test audio",
                  "totalBytes":5,
                  "assets":[{
                    "id":"audio-test-001",
                    "relativePath":"ogg/test.ogg",
                    "byteSize":5,
                    "sha256":"$sha",
                    "downloadPath":"/v1/packs/audio-test-v1/assets/audio-test-001"
                  }]
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(
            MockResponse().setBody(Buffer().write(bytes)),
        )

        val engine = PackDownloadEngine(
            context = context,
            db = db,
            api = api(),
            limiter = DownloadLimiter(2),
        )
        val outcome = engine.downloadAndInstall(
            packId = "audio-test-v1",
            manifestPath = "/v1/packs/audio-test-v1/manifest",
            packTitleFr = "Test audio",
            packDescriptionFr = "desc",
            packVersion = "1.0.0",
        )
        assertTrue(outcome is OnlineResult.Ok)
        val installed = File(context.filesDir, "packs/audio-test-v1/ogg/test.ogg")
        assertTrue(installed.exists())
        assertEquals(
            fr.russki.app.data.content.InstalledPackState.INSTALLED,
            db.installedPackDao().get("audio-test-v1")?.state,
        )
        // leçons locales intactes
        assertEquals(33, db.contentLetterDao().count())
    }

    @Test
    fun packDownloadEngine_rejectsBadChecksum_keepsPackUnavailable() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "packId":"audio-bad-v1",
                  "version":"1.0.0",
                  "titleFr":"Bad",
                  "totalBytes":5,
                  "assets":[{
                    "id":"a1",
                    "relativePath":"x.ogg",
                    "byteSize":5,
                    "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "downloadPath":"/asset"
                  }]
                }
                """.trimIndent(),
            ),
        )
        server.enqueue(MockResponse().setBody(Buffer().writeString("hello", Charsets.UTF_8)))

        val engine = PackDownloadEngine(context, db, api(), DownloadLimiter(1))
        val outcome = engine.downloadAndInstall(
            packId = "audio-bad-v1",
            manifestPath = "/manifest",
            packTitleFr = "Bad",
            packDescriptionFr = "",
            packVersion = "1.0.0",
        )
        assertTrue(outcome is OnlineResult.Err)
        assertEquals(OnlineError.CHECKSUM, (outcome as OnlineResult.Err).error.code)
        assertFalse(File(context.filesDir, "packs/audio-bad-v1").exists())
        assertEquals(
            fr.russki.app.data.content.InstalledPackState.FAILED,
            db.installedPackDao().get("audio-bad-v1")?.state,
        )
    }

    @Test
    fun remoteCatalog_fallsBackToCache_whenServerDown() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "apiVersion":"1",
                  "catalogVersion":"cached-1",
                  "generatedAtEpochMs":10,
                  "lessons":[],
                  "packs":[]
                }
                """.trimIndent(),
            ),
        )
        val repo = RemoteCatalogRepository(context, api())
        assertTrue(repo.refresh() is OnlineResult.Ok)

        server.enqueue(MockResponse().setResponseCode(500))
        val second = repo.refresh()
        assertTrue(second is OnlineResult.Ok)
        assertEquals("cached-1", (second as OnlineResult.Ok).value.catalogVersion)
    }

    @Test
    fun progressSync_requiresOptIn_andNeverFakesSuccess() = runBlocking {
        ContentSeedInitializer(db).ensureSeeded()
        val prefs = SyncPreferences()
        val gateway = ProgressSyncGateway(api(), db, prefs)
        val optedOut = gateway.pushIfAllowed()
        assertTrue(optedOut is OnlineResult.Err)
        assertEquals(OnlineError.SYNC_OPTED_OUT, (optedOut as OnlineResult.Err).error.code)

        prefs.syncOptIn = true
        val noToken = gateway.pushIfAllowed()
        assertTrue(noToken is OnlineResult.Err)
        assertEquals(OnlineError.SYNC_UNAUTHORIZED, (noToken as OnlineResult.Err).error.code)

        // Données locales toujours présentes
        assertTrue(db.contentLetterDao().count() >= 33)
    }

    @Test
    fun remoteJson_manifestRejectsSizeMismatch() {
        val raw = """
            {
              "packId":"p",
              "version":"1",
              "titleFr":"t",
              "totalBytes":10,
              "assets":[{
                "id":"a",
                "relativePath":"a.ogg",
                "byteSize":5,
                "sha256":"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                "downloadPath":"/a"
              }]
            }
        """.trimIndent()
        val parsed = RemoteJson.parseManifest(raw)
        assertTrue(parsed is OnlineResult.Err)
    }
}
