package fr.russki.app.feature.speak

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH

/**
 * Espace « Parler » — placeholder honnête.
 * Aucune simulation d'enregistrement ou de reconnaissance vocale.
 */
@Composable
fun SpeakRoute() {
    SpeakScreen()
}

@Composable
fun SpeakScreen() {
    ScreenScaffold(
        title = "Parler",
        subtitle = "Expression orale — pas encore disponible.",
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SoftPanel {
                SectionLabel("État actuel")
                Text(
                    text = "Les exercices oraux (micro, répétition, reconnaissance vocale) " +
                        "seront ajoutés lorsqu'ils seront réellement implémentés, " +
                        "avec des enregistrements natifs sous licence compatible.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                SpacerH(12)
                Text(
                    text = "En attendant : utilisez Apprendre (leçons A1) et Réviser " +
                        "(flashcards / quiz). Aucun bouton d'enregistrement factice n'est affiché.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SpacerH(16)
        }
    }
}
