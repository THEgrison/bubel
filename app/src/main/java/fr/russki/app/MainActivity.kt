package fr.russki.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.russki.app.core.navigation.RusskiAppShell
import fr.russki.app.core.ui.theme.RusskiTheme
import fr.russki.app.data.temporary.TemporaryPreferencesStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by TemporaryPreferencesStore.themePreference
                .collectAsStateWithLifecycle()
            RusskiTheme(preference = themePreference) {
                Surface(Modifier.fillMaxSize()) {
                    RusskiAppShell()
                }
            }
        }
    }
}
