package de.badaix.snapcast

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SnapclientService : LifecycleService() {
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
