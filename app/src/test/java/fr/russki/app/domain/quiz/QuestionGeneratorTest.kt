package fr.russki.app.domain.quiz

import fr.russki.app.data.content.seed.SeedContentPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionGeneratorTest {

    private val generator = QuestionGenerator(random = Random(42))

    @Test
    fun generatesRequestedCount_withUniqueIds() {
        val questions = generator.generate(SeedContentPack.pack, count = 10)
        assertEquals(10, questions.size)
        assertEquals(questions.size, questions.map { it.id }.toSet().size)
    }

    @Test
    fun distractorsNeverEqualExpected() {
        val questions = generator.generate(SeedContentPack.pack, count = 40)
        questions.forEach { q ->
            q.choices.forEach { choice ->
                if (choice != q.expectedAnswer) {
                    assertFalse(
                        choice.trim().equals(q.expectedAnswer.trim(), ignoreCase = true) &&
                            choice !== q.expectedAnswer,
                    )
                }
            }
            assertFalse(
                q.choices.count {
                    it.trim().equals(q.expectedAnswer.trim(), ignoreCase = true)
                } > 1,
            )
            if (q.choices.isNotEmpty()) {
                assertTrue(q.choices.any { it == q.expectedAnswer })
            }
        }
    }

    @Test
    fun coversAllQuestionTypesWhenPoolAllows() {
        val questions = generator.generate(SeedContentPack.pack, count = 200)
        val types = questions.map { it.type }.toSet()
        QuizQuestionType.entries.forEach { type ->
            assertTrue("type manquant: $type", type in types)
        }
    }
}
