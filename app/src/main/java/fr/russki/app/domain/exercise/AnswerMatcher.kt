package fr.russki.app.domain.exercise

/**
 * Comparaison souple des réponses textuelles russes / françaises.
 * Ne remplace pas une vraie morphologie : règles explicites et testables.
 */
class AnswerMatcher(
    private val treatYoAsYe: Boolean = true,
) {

    /**
     * @param acceptedAnswers variantes explicitement acceptées (déjà normalisées ou brutes)
     */
    fun matches(userAnswer: String, acceptedAnswers: Collection<String>): Boolean {
        val user = normalize(userAnswer)
        if (user.isEmpty()) return false
        return acceptedAnswers.any { normalize(it) == user }
    }

    fun matchesExpected(userAnswer: String, expected: String): Boolean =
        matches(userAnswer, listOf(expected))

    fun normalize(value: String): String {
        var s = value.trim().lowercase()
        s = s.replace(Regex("\\s+"), " ")
        s = s.replace('\u00a0', ' ')
        // Guillemets / ponctuation périphérique courante
        s = s.trim('«', '»', '"', '\'', '.', '!', '?', ',', ';', ':')
        s = s.replace(Regex("\\s+"), " ").trim()
        if (treatYoAsYe) {
            s = s.replace('ё', 'е')
        }
        return s
    }
}
