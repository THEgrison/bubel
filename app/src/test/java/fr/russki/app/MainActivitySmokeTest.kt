package fr.russki.app

import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Smoke : l'activité principale se lance (Robolectric, pas instrumenté appareil). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RusskiApp::class)
class MainActivitySmokeTest {

    @Test
    fun mainActivity_launchesWithoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}
