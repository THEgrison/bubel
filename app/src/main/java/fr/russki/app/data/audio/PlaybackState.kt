package fr.russki.app.data.audio

/** État du lecteur local — pas de streaming distant. */
enum class PlaybackState {
    Idle,
    Playing,
    Paused,
    Error,
}
