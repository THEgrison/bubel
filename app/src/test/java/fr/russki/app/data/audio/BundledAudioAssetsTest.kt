package fr.russki.app.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.data.content.seed.GreetingsVocabularySeed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Vérifie que chaque chemin déclaré « disponible » correspond à un asset réel,
 * et que les absents restent indisponibles (pas de faux positif).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundledAudioAssetsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val availability = AssetAudioAvailability(context)
    private val resolver = AudioResolver(availability)

    private val knownPresent = listOf(
        "content/audio/ru/privet.ogg",
        "content/audio/ru/zdravstvuyte.ogg",
        "content/audio/ru/dobroe-utro.ogg",
        "content/audio/ru/dobryy-vecher.ogg",
        "content/audio/ru/poka.ogg",
        "content/audio/ru/do-svidaniya.ogg",
        "content/audio/ru/spasibo.ogg",
        "content/audio/ru/pozhaluysta.ogg",
        "content/audio/ru/khorosho.ogg",
    )

    private val knownMissing = listOf(
        "content/audio/ru/dobryy-den.ogg",
        "content/audio/ru/spokoynoy-nochi.ogg",
        "content/audio/ru/izvinite.ogg",
        "content/audio/ru/kak-dela.ogg",
        "content/audio/ru/menya-zovut.ogg",
        "content/audio/ru/ochen-priyatno.ogg",
        "content/audio/ru/dialogue-meet-l2.ogg",
        "content/audio/ru/missing-never.ogg",
    )

    @Test
    fun bundledGreetingFiles_resolveAsAvailable() {
        knownPresent.forEach { path ->
            assertNotNull("Asset attendu manquant : $path", availability.isAvailable(path))
        }
    }

    @Test
    fun missingCatalogFiles_stayUnavailable() {
        knownMissing.forEach { path ->
            assertNull("Ne doit pas inventer $path", availability.isAvailable(path))
        }
    }

    @Test
    fun vocabularySeed_pathsMatchAvailability() {
        GreetingsVocabularySeed.module.entries.forEach { entry ->
            val catalog = PedagogicalAudioCatalog.forVocabulary(entry.id)
            assertNotNull(catalog)
            val fromSeed = availability.isAvailable(entry.audioAssetPath)
            val fromCatalog = resolver.resolveVocabulary(entry.id)
            if (entry.audioAssetPath != null) {
                assertNotNull(
                    "Seed déclare ${entry.audioAssetPath} mais fichier absent",
                    fromSeed,
                )
                assertNotNull(fromCatalog)
                assertTrue(entry.audioAssetPath!!.endsWith(".ogg"))
            } else {
                assertNull(fromSeed)
                assertNull(
                    "Catalogue résout ${entry.id} sans chemin seed — aligner seed ou retirer le fichier",
                    fromCatalog,
                )
            }
        }
    }

    @Test
    fun dialogueExactReuse_meetL1AndDrinkL6_available() {
        assertNotNull(resolver.resolveDialogueLine("meet-l1", null))
        assertNotNull(resolver.resolveDialogueLine("drink-l6", null))
        assertNull(resolver.resolveDialogueLine("meet-l2", null))
        assertNull(resolver.resolveDialogueLine("ask-l1", null))
    }

    @Test
    fun assetsFolder_listsOnlyDocumentedOggsPlusReadme() {
        val names = context.assets.list("content/audio/ru")?.toList().orEmpty()
        assertTrue(names.contains("README.txt"))
        val oggs = names.filter { it.endsWith(".ogg") }.sorted()
        assertEquals(
            knownPresent.map { it.substringAfterLast('/') }.sorted(),
            oggs,
        )
    }

    private fun assertEquals(expected: List<String>, actual: List<String>) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
