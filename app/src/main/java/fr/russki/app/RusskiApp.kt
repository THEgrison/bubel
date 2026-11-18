package fr.russki.app

import android.app.Application
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.data.content.OfflineContentRepository

class RusskiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OfflineContentRepository.install(
            OfflineContentRepository(
                audioAvailability = AssetAudioAvailability(this),
            ),
        )
    }
}
