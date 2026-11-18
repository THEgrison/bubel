package fr.russki.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Métadonnées du catalogue pédagogique embarqué / importé. */
@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(
    tableName = "content_letters",
    indices = [Index(value = ["sortOrder"], unique = true)],
)
data class ContentLetterEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val uppercase: String,
    val lowercase: String,
    val nameRu: String,
    val pronunciationGuideFr: String,
    val soundInWordsFr: String,
    val exampleWordRu: String,
    val exampleWordFr: String,
    val audioAssetId: String?,
    /** bundled | downloaded */
    val source: String,
)

@Entity(
    tableName = "content_words",
    indices = [
        Index("category"),
        Index("level"),
    ],
)
data class ContentWordEntity(
    @PrimaryKey val id: String,
    val textRu: String,
    val translationFr: String,
    val category: String,
    val level: String,
    val exampleRu: String?,
    val exampleFr: String?,
    val audioAssetId: String?,
    val source: String,
)

@Entity(
    tableName = "content_phrases",
    indices = [Index("category")],
)
data class ContentPhraseEntity(
    @PrimaryKey val id: String,
    val textRu: String,
    val translationFr: String,
    val category: String,
    val level: String,
    val audioAssetId: String?,
    val source: String,
)

@Entity(
    tableName = "content_lessons",
    indices = [Index("moduleId"), Index("sortOrder")],
)
data class ContentLessonEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val titleFr: String,
    val descriptionFr: String,
    val kind: String,
    val sortOrder: Int,
    val source: String,
)

@Entity(
    tableName = "content_lesson_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ContentLessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lessonId"), Index(value = ["lessonId", "sortOrder"])],
)
data class ContentLessonExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val type: String,
    val promptFr: String,
    val stimulus: String,
    val optionsJson: String,
    val correctIndex: Int,
    val explanationFr: String,
    val relatedContentId: String?,
    val sortOrder: Int,
)

@Entity(tableName = "audio_assets")
data class AudioAssetEntity(
    @PrimaryKey val id: String,
    val relativePath: String,
    val titleFr: String,
    val byteSize: Long,
    val durationMs: Long?,
    val contentRefId: String?,
    val requiredPackId: String?,
)

@Entity(tableName = "content_packs")
data class ContentPackEntity(
    @PrimaryKey val id: String,
    val titleFr: String,
    val descriptionFr: String,
    val isOptional: Boolean,
    val isBundled: Boolean,
    val totalBytes: Long,
    val version: String,
)

@Entity(
    tableName = "content_pack_members",
    primaryKeys = ["packId", "memberType", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = ContentPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packId")],
)
data class ContentPackMemberEntity(
    val packId: String,
    /** letter | word | phrase | lesson | audio */
    val memberType: String,
    val memberId: String,
)
