package de.badaix.snapcast

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SnapcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In release builds, you might want to use a crash reporting tree
            // Timber.plant(CrashReportingTree())
        }
    }
}
