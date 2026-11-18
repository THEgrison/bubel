package fr.russki.app.feature.speak

import androidx.compose.runtime.Composable
import fr.russki.app.feature.dialogue.DialogueCatalogRoute

/**
 * Onglet « Parler » — dialogues A1 + accès écoute / oral locaux.
 * Pas d'ASR, pas d'IA, pas d'envoi de voix.
 */
@Composable
fun SpeakRoute(
    onOpenDialogue: (String) -> Unit,
    onOpenListening: () -> Unit = {},
    onOpenOral: () -> Unit = {},
) {
    DialogueCatalogRoute(
        onOpenDialogue = onOpenDialogue,
        onOpenListening = onOpenListening,
        onOpenOral = onOpenOral,
    )
}
