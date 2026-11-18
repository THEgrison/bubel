package fr.russki.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Unités CECRL du parcours (Room v3).
 * Séparé de `content_lessons` (catalogue alphabet/vocab legacy).
 */
@Entity(
    tableName = "curriculum_units",
    indices = [Index("cefrLevel"), Index("sortOrder")],
)
data class CurriculumUnitEntity(
    @PrimaryKey val id: String,
    val cefrLevel: String,
    val titleFr: String,
    val descriptionFr: String,
    val sortOrder: Int,
    val source: String,
)

@Entity(
    tableName = "curriculum_lessons",
    foreignKeys = [
        ForeignKey(
            entity = CurriculumUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("unitId"), Index(value = ["unitId", "sortOrder"])],
)
data class CurriculumLessonEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val titleFr: String,
    val objectiveFr: String,
    val introductionFr: String,
    val summaryFr: String,
    val sortOrder: Int,
    val exerciseCount: Int,
    val legacyRouteHint: String?,
    val source: String,
)

@Entity(
    tableName = "curriculum_exercises",
    foreignKeys = [
        ForeignKey(
            entity = CurriculumLessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lessonId"), Index(value = ["lessonId", "sortOrder"])],
)
data class CurriculumExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val type: String,
    val promptFr: String,
    val stimulus: String,
    /** JSON array de choix affichés. */
    val choicesJson: String,
    /** JSON array de réponses acceptées (AnswerMatcher). */
    val acceptedAnswersJson: String,
    val explanationFr: String,
    val sortOrder: Int,
)
