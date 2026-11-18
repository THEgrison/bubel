package fr.russki.app.domain.cyrillic

import fr.russki.app.data.content.ContentValidator
import fr.russki.app.data.content.seed.AlphabetLettersSeed
import fr.russki.app.data.content.seed.CyrillicPathSeed
import fr.russki.app.domain.pronunciation.RussianPronunciation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit pédagogique final des 33 lettres + prérequis de lecture.
 * Pas de validation sur appareil.
 */
class CyrillicPedagogyAuditTest {

    @Test
    fun nineLessons_inDeclaredPedagogicalOrder() {
        val ids = CyrillicPathSeed.unit.lessons.map { it.id }
        assertEquals(
            listOf(
                CyrillicPathSeed.LESSON_G1,
                CyrillicPathSeed.LESSON_G2,
                CyrillicPathSeed.LESSON_G3,
                CyrillicPathSeed.LESSON_SYLLABLES,
                CyrillicPathSeed.LESSON_G4,
                CyrillicPathSeed.LESSON_G5,
                CyrillicPathSeed.LESSON_WORDS,
                CyrillicPathSeed.LESSON_G6,
                CyrillicPathSeed.LESSON_FINAL,
            ),
            ids,
        )
        assertEquals((1..9).toList(), CyrillicPathSeed.unit.lessons.map { it.sortOrder })
    }

    @Test
    fun noSyllableOrWord_usesLetterNotYetTaught() {
        CyrillicPathSeed.syllables.forEach { item ->
            val taught = CyrillicPathSeed.letterIdsTaughtUpTo(CyrillicPathSeed.LESSON_SYLLABLES)
            assertTrue(
                "Syllabe ${item.textRu} lettres manquantes=${CyrillicReadingGate.missingLetters(item, taught)}",
                CyrillicReadingGate.canPresent(item, taught),
            )
        }
        CyrillicPathSeed.words.forEach { item ->
            val taught = CyrillicPathSeed.letterIdsTaughtUpTo(CyrillicPathSeed.LESSON_WORDS)
            assertTrue(
                "Mot ${item.textRu} lettres manquantes=${CyrillicReadingGate.missingLetters(item, taught)}",
                CyrillicReadingGate.canPresent(item, taught),
            )
        }
    }

    @Test
    fun letters_distinguishNameSoundReadingAid_noFakeAudio_noExactClaim() {
        AlphabetLettersSeed.letters.forEach { letter ->
            assertTrue(letter.nameRu.isNotBlank())
            assertTrue(letter.soundInWordsFr.isNotBlank())
            assertTrue(letter.pronunciationGuideFr.isNotBlank())
            assertFalse(ContentValidator.presentsApproximationAsExact(letter.pronunciationGuideFr))
            assertFalse(ContentValidator.presentsApproximationAsExact(letter.soundInWordsFr))
            assertNull("Audio lettre inventé: ${letter.id}", letter.audioAssetPath)
            assertTrue(!letter.exampleReadingAid.isNullOrBlank())
            // L’aide de lecture du mot d’exemple ne doit pas être présentée comme IPA
            assertFalse(letter.exampleReadingAid!!.contains("IPA", ignoreCase = true))
            val formOk = letter.uppercase.length == 1 && letter.lowercase.length == 1 &&
                letter.uppercase != letter.lowercase
            assertTrue("Casse ${letter.id}", formOk || letter.uppercase == "Ё")
        }
    }

    @Test
    fun yoAndSoftHard_haveExplicitPedagogy() {
        val yo = AlphabetLettersSeed.byUppercase("Ё")!!
        val e = AlphabetLettersSeed.byUppercase("Е")!!
        val soft = AlphabetLettersSeed.byUppercase("Ь")!!
        val hard = AlphabetLettersSeed.byUppercase("Ъ")!!
        assertTrue(yo.confusionFr!!.contains("accent") || yo.soundInWordsFr.contains("accent"))
        assertTrue(e.confusionFr!!.contains("Ё"))
        assertTrue(soft.confusionFr!!.contains("pas une voyelle"))
        assertTrue(hard.confusionFr!!.contains("pas une voyelle"))
        assertTrue(soft.nameRu.contains("мягкий"))
        assertTrue(hard.nameRu.contains("твёрдый"))
        assertTrue(
            CyrillicPathSeed.exercisesFor(CyrillicPathSeed.LESSON_FINAL)
                .any { "Ё" in it.choices || "Ё" in it.acceptedAnswers },
        )
        assertTrue(
            CyrillicPathSeed.exercisesFor(CyrillicPathSeed.LESSON_FINAL)
                .any { it.stimulus.contains("Ь") || it.promptFr.contains("mou") },
        )
        assertTrue(
            CyrillicPathSeed.exercisesFor(CyrillicPathSeed.LESSON_FINAL)
                .any { it.stimulus.contains("Ъ") || it.promptFr.contains("dur") },
        )
    }

    @Test
    fun lookalikes_coveredInGroup2AndFinal() {
        val g2 = CyrillicPathSeed.lettersForLesson(CyrillicPathSeed.LESSON_G2).map { it.uppercase }.toSet()
        assertTrue(g2.containsAll(listOf("В", "Н", "Р", "С")))
        val finalPrompts = CyrillicPathSeed.exercisesFor(CyrillicPathSeed.LESSON_FINAL)
            .joinToString { it.promptFr + it.stimulus }
        assertTrue(finalPrompts.contains("В") || finalPrompts.contains("latin"))
        assertTrue(finalPrompts.contains("Н"))
    }

    @Test
    fun exampleWords_haveConsistentReadingAidWhenStressedKnown() {
        AlphabetLettersSeed.letters.forEach { letter ->
            val lookup = RussianPronunciation.lookupOrTransliterate(letter.exampleWordRu)
            if (!lookup.stressUnknown) {
                assertEquals(
                    "Accent/exemple ${letter.exampleWordRu}",
                    lookup.transliteration,
                    letter.exampleReadingAid,
                )
            }
        }
    }
}
