package fr.russki.app.feature.speak

import androidx.compose.runtime.Composable
import fr.russki.app.feature.dialogue.DialogueCatalogRoute

/**
 * Onglet « Parler » — catalogue de dialogues scriptés A1.
 * Pas de micro, pas d'ASR, pas d'IA.
 */
@Composable
fun SpeakRoute(
    onOpenDialogue: (String) -> Unit,
) {
    DialogueCatalogRoute(onOpenDialogue = onOpenDialogue)
}
