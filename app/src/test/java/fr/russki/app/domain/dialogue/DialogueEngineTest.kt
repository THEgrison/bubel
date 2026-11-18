package fr.russki.app.domain.dialogue

import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.domain.curriculum.CefrLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueEngineTest {

    private val engine = DialogueEngine()

    @Test
    fun seed_hasThreeStableA1Dialogues() {
        val ids = A1DialoguesSeed.all.map { it.outline.id }
        assertEquals(
            listOf(A1DialoguesSeed.ID_MEET, A1DialoguesSeed.ID_DRINK, A1DialoguesSeed.ID_ASK),
            ids,
        )
        assertEquals(
            listOf("Faire connaissance", "Commander une boisson", "Demander son chemin"),
            A1DialoguesSeed.all.map { it.outline.titleFr },
        )
        A1DialoguesSeed.all.forEach { script ->
            assertEquals(CefrLevel.A1, script.outline.cefrLevel)
            assertEquals(script.lines.size, script.outline.lineCount)
            assertEquals(script.activities.size, script.outline.activityCount)
            assertTrue(script.activities.size >= 3)
            val types = script.activities.map { it.type }.toSet()
            assertTrue(types.contains(DialogueActivityType.ChooseTranslation))
            assertTrue(types.contains(DialogueActivityType.ChooseReply))
            assertTrue(types.contains(DialogueActivityType.FillBlank))
            assertTrue(script.outline.id.startsWith("dialogue-"))
        }
    }

    @Test
    fun lines_areStrictlyOrderedBySortOrder() {
        A1DialoguesSeed.all.forEach { script ->
            val orders = script.lines.map { it.sortOrder }
            assertEquals(orders.sorted(), orders)
            assertEquals((0 until script.lines.size).toList(), orders)
        }
    }

    @Test
    fun session_lineThenActivity_withCorrection() {
        val script = A1DialoguesSeed.byId(A1DialoguesSeed.ID_MEET)!!
        var session = engine.start(script)
        assertEquals(DialoguePhase.Introduction, session.phase)

        session = engine.begin(session)
        assertEquals(DialoguePhase.Line, session.phase)
        assertEquals(0, session.lineIndex)
        assertEquals("Здравствуйте!", session.currentLine!!.textRu)

        session = engine.revealTranslation(session)
        assertTrue(session.translationRevealed)

        session = engine.advanceFromLine(session)
        assertEquals(DialoguePhase.Activity, session.phase)
        val activity = session.currentActivity!!
        assertEquals(DialogueActivityType.ChooseTranslation, activity.type)

        session = engine.submitAnswer(session, "mauvaise")
        assertFalse(session.currentGrade!!.isCorrect)
        assertTrue(session.currentGrade!!.explanationFr.isNotBlank())

        session = engine.advanceAfterFeedback(session)
        // Après meet-a1, pas d'autre activité sur la ligne 0 → réplique suivante
        assertEquals(DialoguePhase.Line, session.phase)
        assertEquals(1, session.lineIndex)
    }

    @Test
    fun chooseReply_acceptsCorrectVariant() {
        val script = A1DialoguesSeed.byId(A1DialoguesSeed.ID_DRINK)!!
        var session = engine.start(script)
        session = engine.begin(session)
        // Avancer jusqu'à l'activité ChooseReply (après ligne 2)
        while (session.phase != DialoguePhase.Activity ||
            session.currentActivity?.type != DialogueActivityType.ChooseReply
        ) {
            session = when (session.phase) {
                DialoguePhase.Line -> engine.advanceFromLine(session)
                DialoguePhase.Activity -> {
                    val act = session.currentActivity!!
                    val answer = act.acceptedAnswers.first()
                    var s = engine.submitAnswer(session, answer)
                    s = engine.advanceAfterFeedback(s)
                    s
                }
                else -> error("phase inattendue ${session.phase}")
            }
        }
        session = engine.submitAnswer(session, "Без сахара, пожалуйста.")
        assertTrue(session.currentGrade!!.isCorrect)
    }

    @Test
    fun audioPaths_areNullOrNotPretendedPresent() {
        A1DialoguesSeed.all.forEach { script ->
            script.lines.forEach { line ->
                assertNull(
                    "Aucun audio inventé pour ${line.id}",
                    line.audioAssetPath,
                )
            }
        }
    }
}
