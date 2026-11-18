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
    fun catalog_coversDialoguesAndGreetings_andOnlyClaimsPresentFiles() {
        val availability = AssetAudioAvailability(
            ApplicationProvider.getApplicationContext(),
        )
        val resolver = AudioResolver(availability)

        A1DialoguesSeed.all.forEach { script ->
            script.lines.forEach { line ->
                assertNotNull(
                    "Catalogue manquant pour ${line.id}",
                    PedagogicalAudioCatalog.forDialogueLine(line.id),
                )
            }
        }

        // Réutilisations exactes embarquées
        assertNotNull(resolver.resolveDialogueLine("meet-l1", null))
        assertNotNull(resolver.resolveDialogueLine("drink-l6", null))
        // Phrase longue encore absente
        assertNull(resolver.resolveDialogueLine("meet-l2", null))

        assertNotNull(PedagogicalAudioCatalog.forVocabulary("vocab-greet-002"))
        assertNotNull(resolver.resolveVocabulary("vocab-greet-002"))
        assertNull(resolver.resolveVocabulary("vocab-greet-014")) // menya-zovut absent

        assertTrue(PedagogicalAudioCatalog.allExpectedPaths().isNotEmpty())
        assertFalse(
            PedagogicalAudioCatalog.allExpectedPaths().any {
                it.relativePath.isBlank()
            },
        )
    }
}
