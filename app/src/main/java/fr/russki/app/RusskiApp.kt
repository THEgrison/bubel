package fr.russki.app

import android.app.Application
import fr.russki.app.di.ServiceLocator

class RusskiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
