package fr.russki.app.domain.pronunciation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianPronunciationTest {

    @Test
    fun knownGreetings_haveStressMarks() {
        val privet = RussianPronunciation.lookupOrTransliterate("привет")
        assertEquals("privét", privet.transliteration)
        assertFalse(privet.stressUnknown)

        val zdr = RussianPronunciation.lookupOrTransliterate("Здравствуйте!")
        assertTrue(zdr.transliteration.startsWith("zdrávstvuyte"))
        assertFalse(zdr.stressUnknown)
    }

    @Test
    fun unknownLemma_marksStressUnknown() {
        val r = RussianPronunciation.lookupOrTransliterate("космонавт")
        assertTrue(r.stressUnknown)
        assertTrue(r.transliteration.contains("kosmonavt") || r.transliteration.isNotBlank())
    }
}
