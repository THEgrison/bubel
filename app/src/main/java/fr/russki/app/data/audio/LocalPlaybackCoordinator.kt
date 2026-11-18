package fr.russki.app.data.audio

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Garantit qu'un seul [LocalAudioPlayer] lit à la fois dans l'app
 * (vocabulaire, dialogues, écoute, oral). Hors ligne, sans service distant.
 */
object LocalPlaybackCoordinator {

    private val players = CopyOnWriteArrayList<LocalAudioPlayer>()

    fun register(player: LocalAudioPlayer) {
        if (!players.contains(player)) {
            players += player
        }
    }

    fun unregister(player: LocalAudioPlayer) {
        players.remove(player)
    }

    /** Arrête tous les autres lecteurs avant de démarrer [player]. */
    fun requestExclusive(player: LocalAudioPlayer) {
        players.forEach { other ->
            if (other !== player) {
                other.stopForHandoff()
            }
        }
    }

    /** Arrête tous les lecteurs enregistrés (tests / nettoyage). */
    fun stopAll() {
        players.forEach { it.stopForHandoff() }
    }

    /** Visible pour les tests. */
    internal fun registeredCount(): Int = players.size

    internal fun clearForTests() {
        players.clear()
    }
}
