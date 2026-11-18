package fr.russki.app.data.audio

import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.data.content.seed.A1DialoguesSeed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PedagogicalAudioCatalogTest {

    @Test
    fun catalog_coversDialoguesAndGreetings_withoutClaimingAvailability() {
        val availability = AssetAudioAvailability(
            ApplicationProvider.getApplicationContext(),
        )
        val resolver = AudioResolver(availability)

        A1DialoguesSeed.all.forEach { script ->
            script.lines.forEach { line ->
                val expected = PedagogicalAudioCatalog.forDialogueLine(line.id)
                assertNotNull("Catalogue manquant pour ${line.id}", expected)
                // Aucun fichier embarqué → indisponible
                assertNull(resolver.resolveDialogueLine(line.id, line.audioAssetPath))
            }
        }

        assertNotNull(PedagogicalAudioCatalog.forVocabulary("vocab-greet-002"))
        assertNull(resolver.resolveVocabulary("vocab-greet-002"))
        assertTrue(PedagogicalAudioCatalog.allExpectedPaths().isNotEmpty())
        assertFalse(
            PedagogicalAudioCatalog.allExpectedPaths().any {
                it.relativePath.isBlank()
            },
        )
    }
}
