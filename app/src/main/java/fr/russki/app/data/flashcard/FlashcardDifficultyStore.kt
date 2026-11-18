package fr.russki.app.data.flashcard

import android.content.Context
import fr.russki.app.domain.flashcard.FlashcardDifficulty

/**
 * Préférence locale (SharedPreferences) — pas de migration Room.
 */
class FlashcardDifficultyStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(): FlashcardDifficulty {
        val raw = prefs.getString(KEY, FlashcardDifficulty.Normal.name) ?: FlashcardDifficulty.Normal.name
        return runCatching { FlashcardDifficulty.valueOf(raw) }.getOrDefault(FlashcardDifficulty.Normal)
    }

    fun set(value: FlashcardDifficulty) {
        prefs.edit().putString(KEY, value.name).apply()
    }

    companion object {
        private const val PREFS = "russki_flashcard_prefs"
        private const val KEY = "difficulty"
    }
}
