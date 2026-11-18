package fr.russki.app.data.content

/** Origine d'une entrée pédagogique — jamais confondue avec la progression. */
object ContentSource {
    const val BUNDLED = "bundled"
    const val DOWNLOADED = "downloaded"
}

object ContentMetaKeys {
    const val BUNDLED_VERSION = "bundled_content_version"
    const val SEED_COMPLETED_AT = "seed_completed_at_epoch_ms"
    /** Version du parcours CECRL (unités / leçons / exercices interactifs). */
    const val CURRICULUM_VERSION = "curriculum_version"
}

object CurriculumVersions {
    /** Ancienne meta (unité 1 seule) — conservée pour lecture historique. */
    const val A1_UNIT1 = "a1-unit1-1.0.0"
    /** Parcours A1 enrichi + unité cyrillique progressive — seed INSERT IGNORE, progression intacte. */
    const val A1_PATH = "a1-path-1.2.0"
}

object PackIds {
    const val CORE_BUNDLED = "core-v1"
    const val AUDIO_DEMO_OPTIONAL = "audio-demo-v1"
}

object InstalledPackState {
    const val NOT_INSTALLED = "NotInstalled"
    const val DOWNLOADING = "Downloading"
    const val INSTALLED = "Installed"
    const val FAILED = "Failed"
}
