package de.badaix.snapcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // TODO
            }
            ACTION_START_SERVICE -> {
                // TODO
            }
            ACTION_STOP_SERVICE -> {
                // TODO
            }
        }
    }

    companion object {
        const val ACTION_START_SERVICE = "de.badaix.snapcast.START_SERVICE"
        const val ACTION_STOP_SERVICE = "de.badaix.snapcast.STOP_SERVICE"
    }
}
