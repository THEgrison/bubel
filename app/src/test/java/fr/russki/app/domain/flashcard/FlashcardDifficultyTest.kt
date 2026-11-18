package fr.russki.app.domain.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FlashcardDifficultyTest {

    private val withAudio = Flashcard(
        id = "c1",
        contentRefId = "x",
        frontRu = "привет",
        backFr = "salut",
        transliteration = "privét",
        stressedRu = null,
        exampleRu = null,
        exampleFr = null,
        audioAssetPath = "content/audio/ru/privet.ogg",
    )

    private val noAudio = withAudio.copy(id = "c2", audioAssetPath = null)

    @Test
    fun easy_isRecognize() {
        assertEquals(
            FlashcardPromptMode.RecognizeRuToFr,
            FlashcardDifficulty.Easy.promptModeFor(withAudio, 0),
        )
    }

    @Test
    fun normal_isRecall() {
        assertEquals(
            FlashcardPromptMode.RecallFrToRu,
            FlashcardDifficulty.Normal.promptModeFor(withAudio, 0),
        )
    }

    @Test
    fun hard_prefersListenWhenAudioExists() {
        assertEquals(
            FlashcardPromptMode.ListenOrRecall,
            FlashcardDifficulty.Hard.promptModeFor(withAudio, 0),
        )
        assertEquals(
            FlashcardPromptMode.RecallFrToRu,
            FlashcardDifficulty.Hard.promptModeFor(noAudio, 0),
        )
    }

    @Test
    fun all_cyclesModes() {
        val m0 = FlashcardDifficulty.All.promptModeFor(withAudio, 0)
        val m1 = FlashcardDifficulty.All.promptModeFor(withAudio, 1)
        val m2 = FlashcardDifficulty.All.promptModeFor(withAudio, 2)
        assertEquals(FlashcardPromptMode.RecognizeRuToFr, m0)
        assertEquals(FlashcardPromptMode.RecallFrToRu, m1)
        assertEquals(FlashcardPromptMode.ListenOrRecall, m2)
        assertNotEquals(m0, m1)
    }
}
