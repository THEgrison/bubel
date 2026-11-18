package fr.russki.app.data.quiz

import fr.russki.app.domain.quiz.QuizDifficulty
import fr.russki.app.domain.quiz.QuizQuestion
import fr.russki.app.domain.quiz.QuizQuestionType
import org.json.JSONArray
import org.json.JSONObject

/** Sérialisation légère sans dépendance supplémentaire. */
object QuizQuestionJson {

    fun encodeList(questions: List<QuizQuestion>): String {
        val array = JSONArray()
        questions.forEach { array.put(encode(it)) }
        return array.toString()
    }

    fun decodeList(raw: String): List<QuizQuestion> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                add(decode(array.getJSONObject(i)))
            }
        }
    }

    private fun encode(q: QuizQuestion): JSONObject =
        JSONObject()
            .put("id", q.id)
            .put("type", q.type.name)
            .put("contentRefId", q.contentRefId)
            .put("promptFr", q.promptFr)
            .put("stimulus", q.stimulus)
            .put("choices", JSONArray(q.choices))
            .put("expectedAnswer", q.expectedAnswer)
            .put("explanationFr", q.explanationFr)
            .put("difficulty", q.difficulty.name)
            .put("audioAssetPath", q.audioAssetPath)
            .put("shuffledTokens", JSONArray(q.shuffledTokens))
            .put("hideAnswerUntilGraded", q.hideAnswerUntilGraded)

    private fun decode(obj: JSONObject): QuizQuestion {
        val choices = obj.getJSONArray("choices").toStringList()
        val tokens = obj.optJSONArray("shuffledTokens")?.toStringList().orEmpty()
        return QuizQuestion(
            id = obj.getString("id"),
            type = QuizQuestionType.valueOf(obj.getString("type")),
            contentRefId = obj.getString("contentRefId"),
            promptFr = obj.getString("promptFr"),
            stimulus = obj.getString("stimulus"),
            choices = choices,
            expectedAnswer = obj.getString("expectedAnswer"),
            explanationFr = obj.getString("explanationFr"),
            difficulty = QuizDifficulty.valueOf(obj.getString("difficulty")),
            audioAssetPath = if (obj.isNull("audioAssetPath")) {
                null
            } else {
                obj.optString("audioAssetPath").takeUnless { it.isBlank() || it == "null" }
            },
            shuffledTokens = tokens,
            hideAnswerUntilGraded = obj.optBoolean("hideAnswerUntilGraded", false),
        )
    }

    private fun JSONArray.toStringList(): List<String> =
        buildList {
            for (i in 0 until length()) add(getString(i))
        }
}
