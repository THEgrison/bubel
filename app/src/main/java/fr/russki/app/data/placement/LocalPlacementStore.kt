package fr.russki.app.data.placement

import android.content.Context
import android.content.SharedPreferences
import fr.russki.app.domain.placement.PlacementRecommendation
import fr.russki.app.domain.placement.PlacementResult
import fr.russki.app.domain.placement.PlacementSkill
import fr.russki.app.domain.placement.PlacementStoredResults
import fr.russki.app.domain.placement.SkillScore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistance locale du test de placement (SharedPreferences).
 * **Pas de migration Room** : résultats indépendants de `user_lesson_progress`.
 *
 * Stratégie de nouveau passage : le résultat précédent est conservé dans
 * `previous` ; `latest` devient le nouveau — aucun écrasement silencieux.
 */
class LocalPlacementStore(
    context: Context,
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
) {

    fun load(): PlacementStoredResults {
        val latest = prefs.getString(KEY_LATEST, null)?.let { parseResult(it) }
        val previous = prefs.getString(KEY_PREVIOUS, null)?.let { parseResult(it) }
        return PlacementStoredResults(latest = latest, previous = previous)
    }

    /**
     * Enregistre un nouveau résultat terminé.
     * Si un `latest` existait, il est déplacé vers `previous`.
     */
    fun saveCompleted(result: PlacementResult) {
        val current = load()
        val editor = prefs.edit()
            .putString(KEY_LATEST, serialize(result))
        if (current.latest != null) {
            editor.putString(KEY_PREVIOUS, serialize(current.latest))
        }
        editor.apply()
    }

    fun clearForTests() {
        prefs.edit().clear().apply()
    }

    private fun serialize(result: PlacementResult): String {
        val skills = JSONArray()
        result.skillScores.forEach { score ->
            skills.put(
                JSONObject()
                    .put("skill", score.skill.name)
                    .put("correct", score.correct)
                    .put("answered", score.answered),
            )
        }
        val rec = result.recommendation
        return JSONObject()
            .put("completedAtEpochMs", result.completedAtEpochMs)
            .put("correctCount", result.correctCount)
            .put("totalQuestions", result.totalQuestions)
            .put("answeredCount", result.answeredCount)
            .put("skills", skills)
            .put(
                "recommendation",
                JSONObject()
                    .put("lessonId", rec.lessonId)
                    .put("lessonTitleFr", rec.lessonTitleFr)
                    .put("rationaleFr", rec.rationaleFr)
                    .put("isFallbackStart", rec.isFallbackStart),
            )
            .toString()
    }

    private fun parseResult(raw: String): PlacementResult? = runCatching {
        val json = JSONObject(raw)
        val skillsJson = json.optJSONArray("skills") ?: JSONArray()
        val skills = buildList {
            for (i in 0 until skillsJson.length()) {
                val item = skillsJson.getJSONObject(i)
                val skill = PlacementSkill.valueOf(item.getString("skill"))
                add(
                    SkillScore(
                        skill = skill,
                        correct = item.getInt("correct"),
                        answered = item.getInt("answered"),
                    ),
                )
            }
        }
        val recJson = json.getJSONObject("recommendation")
        PlacementResult(
            completedAtEpochMs = json.getLong("completedAtEpochMs"),
            correctCount = json.getInt("correctCount"),
            totalQuestions = json.getInt("totalQuestions"),
            answeredCount = json.getInt("answeredCount"),
            skillScores = skills,
            recommendation = PlacementRecommendation(
                lessonId = recJson.getString("lessonId"),
                lessonTitleFr = recJson.getString("lessonTitleFr"),
                rationaleFr = recJson.getString("rationaleFr"),
                isFallbackStart = recJson.optBoolean("isFallbackStart", false),
            ),
        )
    }.getOrNull()

    companion object {
        const val PREFS_NAME = "russki_placement"
        private const val KEY_LATEST = "latest_result_json"
        private const val KEY_PREVIOUS = "previous_result_json"
    }
}
