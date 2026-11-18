package fr.russki.app.data.content

import fr.russki.app.data.content.seed.AlphabetExercisesSeed
import fr.russki.app.data.content.seed.GreetingsVocabularySeed
import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.domain.content.AlphabetExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class AlphabetContentTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun validatePackAtStartup() {
            ContentValidator.requireValid(SeedContentPack.pack)
        }
    }

    @Test
    fun alphabet_hasExactly33Letters() {
        assertEquals(33, SeedContentPack.pack.alphabet.letters.size)
        assertEquals(SeedContentPack.EXPECTED_LETTER_COUNT, SeedContentPack.pack.alphabet.letters.size)
    }

    @Test
    fun alphabet_isInCanonicalOrder() {
        val letters = SeedContentPack.pack.alphabet.letters
        val expected = listOf(
            "Аа", "Бб", "Вв", "Гг", "Дд", "Ее", "Ёё", "Жж", "Зз", "Ии",
            "Йй", "Кк", "Лл", "Мм", "Нн", "Оо", "Пп", "Рр", "Сс", "Тт",
            "Уу", "Фф", "Хх", "Цц", "Чч", "Шш", "Щщ", "Ъъ", "Ыы", "Ьь",
            "Ээ", "Юю", "Яя",
        )
        assertEquals(expected, letters.map { it.uppercase + it.lowercase })
        assertEquals((1..33).toList(), letters.map { it.order })
    }

    @Test
    fun letterIds_areStableAndUnique() {
        val ids = SeedContentPack.pack.alphabet.letters.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
        assertTrue(ids.all { it.startsWith("letter-") })
    }

    @Test
    fun letters_haveNoEmptyTranslationsOrGuides() {
        SeedContentPack.pack.alphabet.letters.forEach { letter ->
            assertTrue(letter.exampleWordFr.isNotBlank())
            assertTrue(letter.pronunciationGuideFr.isNotBlank())
            assertTrue(letter.soundInWordsFr.isNotBlank())
            assertTrue(letter.nameRu.isNotBlank())
        }
    }

    @Test
    fun letters_audioPathsAreNullUntilRecordingsExist() {
        SeedContentPack.pack.alphabet.letters.forEach { letter ->
            assertNull(
                "Aucun fichier audio n'est embarqué pour ${letter.id}",
                letter.audioAssetPath,
            )
        }
    }

    @Test
    fun pronunciation_neverClaimsExactFrenchEquivalence() {
        SeedContentPack.pack.alphabet.letters.forEach { letter ->
            assertFalse(ContentValidator.presentsApproximationAsExact(letter.pronunciationGuideFr))
            assertFalse(ContentValidator.presentsApproximationAsExact(letter.soundInWordsFr))
        }
    }

    @Test
    fun exercises_coverAllRequiredTypes() {
        val types = AlphabetExercisesSeed.exercises.map { it.type }.toSet()
        assertEquals(AlphabetExerciseType.entries.toSet(), types)
    }

    @Test
    fun greetingsModule_hasVerifiedNonEmptyTranslations() {
        val module = GreetingsVocabularySeed.module
        assertTrue(module.entries.isNotEmpty())
        module.entries.forEach { entry ->
            assertTrue(entry.translationFr.isNotBlank())
            assertTrue(entry.textRu.isNotBlank())
            assertTrue(entry.category.isNotBlank())
            entry.audioAssetPath?.let { path ->
                assertTrue(path.startsWith("content/audio/ru/"))
                assertTrue(path.endsWith(".ogg"))
            }
        }
        val ids = module.entries.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun repository_resolvesNullAudioAsUnavailable() {
        val repo = OfflineContentRepository(audioAvailability = NoOpAudioAvailability)
        val letter = repo.letters().first()
        assertNull(repo.resolveAudio(letter.audioAssetPath))
        assertNull(repo.resolveAudio("audio/missing.mp3"))
    }

    @Test
    fun contentPack_versionIsSet() {
        assertEquals("1.0.0", SeedContentPack.VERSION)
        assertEquals("1.0.0", SeedContentPack.pack.version)
    }
}
