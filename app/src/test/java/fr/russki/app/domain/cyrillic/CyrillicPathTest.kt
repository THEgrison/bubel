package fr.russki.app.domain.cyrillic

import fr.russki.app.data.content.seed.AlphabetLettersSeed
import fr.russki.app.data.content.seed.CyrillicPathSeed
import fr.russki.app.data.content.seed.LessonTeachCatalog
import fr.russki.app.data.review.ReviewableCatalog
import fr.russki.app.domain.lesson.LessonEngine
import fr.russki.app.domain.lesson.LessonPhase
import fr.russki.app.domain.lesson.PedagogyStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyrillicPathTest {

    @Test
    fun partition_coversExactly33Letters_onceEach() {
        assertTrue(CyrillicPathSeed.assertPartitionCovers33())
        assertEquals(33, AlphabetLettersSeed.letters.size)
    }

    @Test
    fun letters_haveUpperLowerNameExampleAndGuides() {
        AlphabetLettersSeed.letters.forEach { letter ->
            assertEquals(1, letter.uppercase.length)
            assertEquals(1, letter.lowercase.length)
            assertTrue(letter.nameRu.isNotBlank())
            assertTrue(letter.exampleWordRu.isNotBlank())
            assertTrue(letter.exampleWordFr.isNotBlank())
            assertTrue(letter.pronunciationGuideFr.isNotBlank())
            assertTrue(letter.soundInWordsFr.isNotBlank())
            assertNull(letter.audioAssetPath)
            assertFalse(
                "Ne pas présenter l'approximation comme exacte: ${letter.id}",
                letter.pronunciationGuideFr.contains("transcription phonétique exacte") &&
                    !letter.pronunciationGuideFr.contains("pas"),
            )
        }
    }

    @Test
    fun lookalikes_haveConfusionNotes() {
        listOf("В", "Н", "Р", "С", "Ь", "Ъ", "Ё", "Е").forEach { u ->
            val letter = AlphabetLettersSeed.letters.first { it.uppercase == u }
            assertTrue("$u sans confusionFr", !letter.confusionFr.isNullOrBlank())
        }
    }

    @Test
    fun eachGroupLesson_hasTeachablesAndExercises() {
        CyrillicPathSeed.groups.forEach { group ->
            val teach = CyrillicPathSeed.teachablesFor(group.lessonId)
            val ex = CyrillicPathSeed.exercisesFor(group.lessonId)
            assertEquals(group.letterIds.size, teach.size)
            assertTrue(ex.size >= 4)
            assertTrue(ex.any { it.pedagogyStage == PedagogyStage.Final })
            assertTrue(ex.all { it.audioAssetPath == null })
        }
    }

    @Test
    fun syllablesAndWords_requireTaughtLetters() {
        val beforeSyllables = CyrillicPathSeed.letterIdsTaughtUpTo(CyrillicPathSeed.LESSON_G2)
        CyrillicPathSeed.syllables.forEach { syl ->
            val okEarly = CyrillicReadingGate.canPresent(syl, beforeSyllables)
            // Certaines syllabes (на, не…) demandent le groupe 2 ; ма demande g1
            if (syl.requiredLetterIds.all { it in beforeSyllables }) {
                assertTrue(okEarly)
            }
        }
        val atSyllables = CyrillicPathSeed.letterIdsTaughtUpTo(CyrillicPathSeed.LESSON_SYLLABLES)
        CyrillicPathSeed.syllables.forEach { syl ->
            assertTrue(
                "Syllabe ${syl.textRu} avant lettres: ${CyrillicReadingGate.missingLetters(syl, atSyllables)}",
                CyrillicReadingGate.canPresent(syl, atSyllables),
            )
        }
        val atWords = CyrillicPathSeed.letterIdsTaughtUpTo(CyrillicPathSeed.LESSON_WORDS)
        CyrillicPathSeed.words.forEach { w ->
            assertTrue(
                "Mot ${w.textRu} lettres manquantes: ${CyrillicReadingGate.missingLetters(w, atWords)}",
                CyrillicReadingGate.canPresent(w, atWords),
            )
        }
    }

    @Test
    fun readingAids_andTranslations_present() {
        CyrillicPathSeed.syllables.forEach {
            assertTrue(it.readingAid.isNotBlank())
            assertTrue(it.translationFr.isNotBlank())
        }
        CyrillicPathSeed.words.forEach {
            assertTrue(it.readingAid.isNotBlank())
            assertTrue(it.translationFr.isNotBlank())
        }
        CyrillicPathSeed.exercisesFor(CyrillicPathSeed.LESSON_WORDS).forEach { ex ->
            assertTrue(!ex.translationFr.isNullOrBlank() || ex.acceptedAnswers.isNotEmpty())
        }
    }

    @Test
    fun catalog_exposesCyrillicTeachables() {
        CyrillicPathSeed.allLessonIds().forEach { id ->
            assertTrue(id in LessonTeachCatalog.coveredLessonIds())
            assertTrue(LessonTeachCatalog.forLesson(id).isNotEmpty())
        }
    }

    @Test
    fun reviewCards_onlyAfterLessonCompleted_noLetterDuplicates() {
        assertTrue(ReviewableCatalog.itemsForCompleted(emptySet()).isEmpty())
        val g1 = ReviewableCatalog.itemsForCompleted(setOf(CyrillicPathSeed.LESSON_G1))
        assertTrue(g1.any { it.contentRefId.startsWith("letter-") })
        val letterIds = g1.filter { it.id.startsWith("rv-letter-") }.map { it.id }
        assertEquals(letterIds.size, letterIds.toSet().size)

        val both = ReviewableCatalog.itemsForCompleted(
            setOf(CyrillicPathSeed.LESSON_G1, CyrillicPathSeed.LESSON_G2),
        )
        val allLetterCardIds = both.filter { it.id.startsWith("rv-letter-") }.map { it.id }
        assertEquals(allLetterCardIds.size, allLetterCardIds.toSet().size)
    }

    @Test
    fun rematch_noDoubleCredit_onCyrillicLesson() {
        val lesson = CyrillicPathSeed.unit.lessons.first { it.id == CyrillicPathSeed.LESSON_G1 }
        val engine = LessonEngine()
        var session = engine.start(
            lesson,
            CyrillicPathSeed.teachablesFor(lesson.id),
            CyrillicPathSeed.exercisesFor(lesson.id),
        )
        session = engine.beginPractice(engine.beginDiscovery(session))
        val first = session.currentExercise!!
        session = engine.submitAnswer(session, "mauvais")
        assertFalse(session.currentGrade!!.countsAsMastered)
        assertEquals(0, session.correctCount)
        session = engine.submitAnswer(session, first.acceptedAnswers.first())
        assertEquals(0, session.correctCount) // première note figée
        session = engine.advanceAfterFeedback(session)
        while (session.phase == LessonPhase.Practice) {
            val ex = session.currentExercise!!
            session = engine.submitAnswer(session, ex.acceptedAnswers.first())
            session = engine.advanceAfterFeedback(session)
        }
        assertTrue(
            session.phase == LessonPhase.Rematch || session.phase == LessonPhase.Recap,
        )
    }
}
